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
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PendingPickupsActivity extends AppCompatActivity {
    private String package_name = "com.example.laundaryagent";
    
    private TextView tvCurrentFilter;
    private MaterialButton btnFilter;
    private RecyclerView rvPickups;
    private OrderAdapter adapter;
    private final List<OrderItem> allOrders = new ArrayList<>();
    private final List<OrderItem> filteredOrders = new ArrayList<>();
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_pickups);

        tvCurrentFilter = findViewById(R.id.tv_current_filter);
        btnFilter = findViewById(R.id.btn_filter);
        rvPickups = findViewById(R.id.rv_pending_pickups);

        initRecyclerView();
        btnFilter.setOnClickListener(v -> showFilterMenu());
        loadData();
    }

    private void initRecyclerView() {
        if (rvPickups == null) return;
        rvPickups.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(filteredOrders, this::onOrderClick);
        rvPickups.setAdapter(adapter);
    }

    private void loadData() {
        listenerReg = FirebaseRepository.getInstance().listenAllPendingPickups(docs -> {
            runOnUiThread(() -> {
                allOrders.clear();
                for (Map<String, Object> doc : docs) {
                    OrderItem item = mapToOrderItem(doc);
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
        if (soc.isEmpty()) {
            if (!address.isEmpty()) {
                String[] parts = address.split(",");
                soc = parts.length > 2 ? parts[2].trim() : (parts.length > 0 ? parts[0].trim() : "Residence");
            } else {
                soc = "Residence";
            }
        }

        return new OrderItem(id, name, address, soc, phone, "Now", path, OrderStatus.PENDING, null);
    }


    private void onOrderClick(OrderItem order) {
        showPickupDetailsDialog(order.getId(), order.getCustomerName(), order.getAddress());
    }

    private void applyFilter() {
        String filter = tvCurrentFilter.getText().toString();
        filteredOrders.clear();
        for (OrderItem o : allOrders) {
            if (filter.equals("All Societies") || o.getSociety().contains(filter)) {
                filteredOrders.add(o);
            }
        }
        adapter.notifyDataSetChanged();
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

        String[] societies = {"All Societies", "Amanora Park Town", "Magarpatta City", "Kalyani Nagar", "Viman Nagar"};
        
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
}
