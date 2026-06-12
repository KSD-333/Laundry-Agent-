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

public class PendingPickupsActivity extends AppCompatActivity {
    
    private TextView tvCurrentFilter;
    private MaterialButton btnFilter;
    private TextView tvTotalPickupsCount, tvPendingPickupsCount;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;

    private final List<OrderItem> allOrders = new ArrayList<>();
    private final List<OrderItem> filteredOrders = new ArrayList<>();
    private final List<OrderItem> pendingFilteredOrders = new ArrayList<>();
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_pickups);

        tvCurrentFilter = findViewById(R.id.tv_current_filter);
        btnFilter = findViewById(R.id.btn_filter);
        tvTotalPickupsCount = findViewById(R.id.tv_total_pickups_count);
        tvPendingPickupsCount = findViewById(R.id.tv_pending_pickups_count);
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
        viewPager.setAdapter(new PickupsPagerAdapter());
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
                    if (status.equals("pending") || status.equals("picking pending") || status.equals("pickup_done") || status.equals("pickup done") || status.equals("completed") || status.equals("delivered") || status.equals("washing") || status.equals("ironing")) {
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
        showPickupDetailsDialog(order.getId(), order.getCustomerName(), order.getAddress());
    }

    private void applyFilter() {
        String filter = tvCurrentFilter.getText().toString();
        filteredOrders.clear();
        pendingFilteredOrders.clear();
        for (OrderItem o : allOrders) {
            if (filter.equals("All Societies") || o.getSociety().contains(filter)) {
                filteredOrders.add(o);
                if (o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.PICKING_PENDING) {
                    pendingFilteredOrders.add(o);
                }
            }
        }

        // Sort completed/done tasks to the bottom/last (anything not PENDING or PICKING_PENDING is done)
        java.util.Collections.sort(filteredOrders, (a, b) -> {
            boolean aDone = (a.getStatus() != OrderStatus.PENDING && a.getStatus() != OrderStatus.PICKING_PENDING);
            boolean bDone = (b.getStatus() != OrderStatus.PENDING && b.getStatus() != OrderStatus.PICKING_PENDING);
            if (aDone && !bDone) return 1;
            if (!aDone && bDone) return -1;
            return 0;
        });
        
        if (tvTotalPickupsCount != null) {
            tvTotalPickupsCount.setText(String.valueOf(filteredOrders.size()));
        }
        if (tvPendingPickupsCount != null) {
            tvPendingPickupsCount.setText(String.valueOf(pendingFilteredOrders.size()));
        }

        if (viewPager != null && viewPager.getAdapter() != null) {
            viewPager.getAdapter().notifyDataSetChanged();
        }
    }

    private void showPickupDetailsDialog(String orderId, String userName, String address) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_pickup_details, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView orderIdText = dialogView.findViewById(R.id.tv_pickup_order_id);
        String shortId = orderId.length() > 8 ? orderId.substring(0, 8) + "..." : orderId;
        orderIdText.setText("#" + shortId);
        orderIdText.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Order ID", orderId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Order ID copied", Toast.LENGTH_SHORT).show();
        });
        ((TextView) dialogView.findViewById(R.id.tv_pickup_user_name)).setText(userName);
        ((TextView) dialogView.findViewById(R.id.tv_pickup_address)).setText(address);

        dialogView.findViewById(R.id.btn_pickup_dismiss).setOnClickListener(v -> dialog.dismiss());
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

    private class PickupsPagerAdapter extends RecyclerView.Adapter<PickupsPagerAdapter.PageViewHolder> {
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
                false,
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
