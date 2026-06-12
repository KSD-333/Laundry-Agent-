package com.example.laundaryagent;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.view.LayoutInflater;
import android.content.Intent;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderStatus;
import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import androidx.viewpager2.widget.ViewPager2;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompletedDeliveriesActivity extends AppCompatActivity {
    
    private TextView tvCurrentFilter;
    private MaterialButton btnFilter;
    private TextView tvTotalDeliveriesCount, tvPendingDeliveriesCount;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private final List<OrderItem> allOrders = new ArrayList<>();
    private final List<OrderItem> filteredOrders = new ArrayList<>();
    private final List<OrderItem> pendingFilteredOrders = new ArrayList<>();
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_deliveries);

        tvCurrentFilter = findViewById(R.id.tv_current_filter);
        btnFilter = findViewById(R.id.btn_filter);
        tvTotalDeliveriesCount = findViewById(R.id.tv_total_deliveries_count);
        tvPendingDeliveriesCount = findViewById(R.id.tv_pending_deliveries_count);
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);

        if (findViewById(R.id.btn_back) != null) {
            findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        }

        btnFilter.setOnClickListener(v -> showFilterMenu());
        initViewPager();
        loadData();
    }

    private void initViewPager() {
        viewPager.setAdapter(new DeliveriesPagerAdapter());
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                viewPager.setCurrentItem(tab.getPosition());
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                TabLayout.Tab tab = tabLayout.getTabAt(position);
                if (tab != null) tab.select();
            }
        });
    }

    private void loadData() {
        String franchiseId = "";
        if (getIntent().hasExtra("franchise_id")) {
            franchiseId = getIntent().getStringExtra("franchise_id");
        } else {
            franchiseId = getSharedPreferences("LaundryPrefs", MODE_PRIVATE).getString("franchise_id", "");
        }

        listenerReg = FirebaseRepository.getInstance().listenFranchiseOrders(franchiseId, docs -> {
            runOnUiThread(() -> {
                allOrders.clear();
                for (Map<String, Object> doc : docs) {
                    OrderItem item = mapToOrderItem(doc);
                    String status = FirebaseRepository.str(doc, "status").toLowerCase();
                    // Deliveries include anything past pickup: ready, out for delivery, completed/delivered
                    if (status.equals("ready") || status.equals("out for delivery") || status.equals("out_for_delivery") || status.equals("completed") || status.equals("delivered") || status.equals("pickup done") || status.equals("pickup_done") || status.equals("washing") || status.equals("ironing")) {
                        allOrders.add(item);
                        
                        if (item.getCustomerName().equals(item.getPhone())) {
                            FirebaseRepository.getInstance().fetchNameForPhone(item.getPhone(), name -> {
                                if (!name.equals("Unknown")) {
                                    runOnUiThread(() -> {
                                        for (int i = 0; i < allOrders.size(); i++) {
                                            if (allOrders.get(i).getPhone().equals(item.getPhone())) {
                                                allOrders.set(i, allOrders.get(i).copyWithName(name));
                                            }
                                        }
                                        applyFilter();
                                    });
                                }
                            });
                        }
                    }
                }
                applyFilter();
            });
        });
    }

    private OrderItem mapToOrderItem(Map<String, Object> doc) {
        String id      = FirebaseRepository.str(doc, "id");
        String phone   = FirebaseRepository.str(doc, "customerPhone");
        if (phone.isEmpty()) phone = FirebaseRepository.str(doc, "phone");
        
        String name    = FirebaseRepository.str(doc, "customerName");
        if (name.isEmpty()) name = FirebaseRepository.str(doc, "name");
        
        String address = FirebaseRepository.str(doc, "address");
        String soc     = FirebaseRepository.str(doc, "society");
        String path    = FirebaseRepository.str(doc, "__path");

        if (name.isEmpty()) name = phone.isEmpty() ? "Unknown" : phone;
        if (soc.isEmpty()) soc = address;

        String statusStr = FirebaseRepository.str(doc, "status").toLowerCase();
        OrderStatus status;
        if (statusStr.equals("pending") || statusStr.equals("picking pending")) status = OrderStatus.PENDING;
        else if (statusStr.equals("pickup done") || statusStr.equals("washing") || statusStr.equals("ironing") || statusStr.equals("pickup_done")) status = OrderStatus.PICKUP_DONE;
        else if (statusStr.equals("ready")) status = OrderStatus.READY;
        else if (statusStr.equals("out for delivery") || statusStr.equals("out_for_delivery")) status = OrderStatus.OUT_FOR_DELIVERY;
        else if (statusStr.equals("delivered") || statusStr.equals("completed")) status = OrderStatus.COMPLETED;
        else if (statusStr.equals("incomplete")) status = OrderStatus.INCOMPLETE;
        else status = OrderStatus.PENDING;

        String reason = FirebaseRepository.str(doc, "incompleteReason");
        return new OrderItem(id, name, address, soc, phone, "Now", path, status, reason);
    }

    private void onOrderClick(OrderItem order) {
        showDeliveryDetailsDialog(order.getId(), order.getCustomerName(), "Agent", "₹ 0", order.getAddress(), "Recent");
    }

    private void applyFilter() {
        String filter = tvCurrentFilter.getText().toString();
        filteredOrders.clear();
        pendingFilteredOrders.clear();
        for (OrderItem o : allOrders) {
            if (filter.equals("All Societies") || o.getSociety().contains(filter)) {
                filteredOrders.add(o);
                // Pending deliveries are ready, out for delivery, washing, ironing, or pickup done but not completed/delivered
                if (o.getStatus() == OrderStatus.PICKUP_DONE || o.getStatus() == OrderStatus.READY || o.getStatus() == OrderStatus.OUT_FOR_DELIVERY) {
                    pendingFilteredOrders.add(o);
                }
            }
        }

        // Sort completed/done tasks to the bottom/last
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION.SDK_INT) {
            filteredOrders.sort((a, b) -> {
                boolean aDone = (a.getStatus() == OrderStatus.COMPLETED || a.getStatus() == OrderStatus.DELIVERED);
                boolean bDone = (b.getStatus() == OrderStatus.COMPLETED || b.getStatus() == OrderStatus.DELIVERED);
                if (aDone && !bDone) return 1;
                if (!aDone && bDone) return -1;
                return 0;
            });
        }
        
        if (tvTotalDeliveriesCount != null) {
            tvTotalDeliveriesCount.setText(String.valueOf(filteredOrders.size()));
        }
        if (tvPendingDeliveriesCount != null) {
            tvPendingDeliveriesCount.setText(String.valueOf(pendingFilteredOrders.size()));
        }

        if (viewPager != null && viewPager.getAdapter() != null) {
            viewPager.getAdapter().notifyDataSetChanged();
        }
    }

    private void showDeliveryDetailsDialog(String orderId, String userName, String agent, String amount, String address, String time) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delivery_details, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView).create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvOrderId = dialogView.findViewById(R.id.tv_delivery_order_id);
        if (tvOrderId != null) tvOrderId.setText("#" + (orderId.length() > 8 ? orderId.substring(0, 8) + "..." : orderId));
        
        TextView tvUser = dialogView.findViewById(R.id.tv_delivery_user_name);
        if (tvUser != null) tvUser.setText(userName);
        
        TextView tvAgent = dialogView.findViewById(R.id.tv_delivery_agent_name);
        if (tvAgent != null) tvAgent.setText(agent);
        
        TextView tvAmount = dialogView.findViewById(R.id.tv_delivery_amount);
        if (tvAmount != null) tvAmount.setText(amount);
        
        TextView tvAddress = dialogView.findViewById(R.id.tv_delivery_address);
        if (tvAddress != null) tvAddress.setText(address);
        
        TextView tvDate = dialogView.findViewById(R.id.tv_delivery_complete_date);
        if (tvDate != null) tvDate.setText(time);

        View btnDismiss = dialogView.findViewById(R.id.btn_delivery_dismiss);
        if (btnDismiss != null) btnDismiss.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    private void showFilterMenu() {
        View popupView = getLayoutInflater().inflate(R.layout.layout_custom_dropdown, null);
        PopupWindow popupWindow = new PopupWindow(popupView, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(20);

        android.widget.LinearLayout optionsLayout = popupView.findViewById(R.id.ll_dropdown_options);

        List<String> societies = new ArrayList<>();
        societies.add("All Societies");
        for (OrderItem o : allOrders) {
            if (o.getSociety() != null && !o.getSociety().isEmpty() && !societies.contains(o.getSociety())) {
                societies.add(o.getSociety());
            }
        }
        
        for (String society : societies) {
            View itemView = getLayoutInflater().inflate(R.layout.item_filter_option, null);
            ((TextView) itemView.findViewById(R.id.tv_option_text)).setText(society);
            ((android.widget.ImageView) itemView.findViewById(R.id.iv_option_icon)).setImageResource(R.drawable.ic_pin);
            
            if (tvCurrentFilter.getText().toString().equals(society)) {
                itemView.findViewById(R.id.iv_check).setVisibility(View.VISIBLE);
                ((com.google.android.material.card.MaterialCardView) itemView.findViewById(R.id.card_option_icon)).setCardBackgroundColor(0xFFE0F2FE);
                ((android.widget.ImageView) itemView.findViewById(R.id.iv_option_icon)).setColorFilter(0xFF0EA5E9);
            }

            itemView.setOnClickListener(v -> {
                popupWindow.dismiss();
                tvCurrentFilter.setText(society);
                btnFilter.setText(society);
                applyFilter();
            });
            optionsLayout.addView(itemView);
        }

        popupWindow.showAsDropDown(btnFilter, 0, 10);
    }

    @Override
    protected void onDestroy() {
        if (listenerReg != null) listenerReg.remove();
        super.onDestroy();
    }

    // ── Adapter ────────────────────────────────────────────────────────────

    private class DeliveriesPagerAdapter extends RecyclerView.Adapter<DeliveriesPagerAdapter.PageViewHolder> {
        @Override
        public PageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            RecyclerView rv = new RecyclerView(parent.getContext());
            rv.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.MATCH_PARENT));
            rv.setLayoutManager(new LinearLayoutManager(parent.getContext()));
            rv.setPadding(40, 16, 40, 16);
            rv.setClipToPadding(false);
            return new PageViewHolder(rv);
        }

        @Override
        public void onBindViewHolder(PageViewHolder holder, int position) {
            OrderAdapter pageAdapter = new OrderAdapter(position == 0 ? filteredOrders : pendingFilteredOrders, 
                true,
                order -> onOrderClick(order));
            holder.recyclerView.setAdapter(pageAdapter);
        }

        @Override
        public int getItemCount() { return 2; }

        class PageViewHolder extends RecyclerView.ViewHolder {
            RecyclerView recyclerView;
            PageViewHolder(View itemView) {
                super(itemView);
                recyclerView = (RecyclerView) itemView;
            }
        }
    }
}
