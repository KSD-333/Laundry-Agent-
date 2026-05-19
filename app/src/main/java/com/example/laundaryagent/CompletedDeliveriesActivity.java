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
import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CompletedDeliveriesActivity extends AppCompatActivity {

    private TextView tvCurrentFilter;
    private MaterialButton btnFilter;
    private RecyclerView rvDeliveries;
    private OrderAdapter adapter;
    private final List<OrderItem> allOrders = new ArrayList<>();
    private final List<OrderItem> filteredOrders = new ArrayList<>();
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_deliveries);

        tvCurrentFilter = findViewById(R.id.tv_current_filter);
        btnFilter = findViewById(R.id.btn_filter);
        rvDeliveries = findViewById(R.id.rv_completed_deliveries);

        initRecyclerView();
        btnFilter.setOnClickListener(v -> showFilterMenu());
        loadData();
    }

    private void initRecyclerView() {
        if (rvDeliveries == null) return;
        rvDeliveries.setLayoutManager(new LinearLayoutManager(this));
        adapter = new OrderAdapter(filteredOrders, this::onOrderClick);
        rvDeliveries.setAdapter(adapter);
    }

    private void loadData() {
        // We'll reuse listenTotalOrders logic but fetch the documents
        listenerReg = FirebaseRepository.getInstance().listenAllDeliveryOrders(docs -> {
            // Wait, listenAllDeliveryOrders is for PICKUP_DONE. 
            // Let's use a more generic one or just filter for COMPLETED.
            runOnUiThread(() -> {
                allOrders.clear();
                for (Map<String, Object> doc : docs) {
                    // Filter for COMPLETED here if needed, or add a new repo method
                    allOrders.add(mapToOrderItem(doc));
                }
                applyFilter();
            });
        });
        
        // Actually let's just create a quick method for completed orders in repo
    }

    private OrderItem mapToOrderItem(Map<String, Object> doc) {
        String id      = FirebaseRepository.str(doc, "id");
        String phone   = FirebaseRepository.str(doc, "customerPhone");
        String name    = FirebaseRepository.str(doc, "customerName");
        String address = FirebaseRepository.str(doc, "address");
        String soc     = FirebaseRepository.str(doc, "society");
        String path    = FirebaseRepository.str(doc, "__path");

        if (name.isEmpty()) name = phone;
        if (soc.isEmpty()) soc = address;

        return new OrderItem(id, name, address, soc, phone, "Done", path);
    }

    private void onOrderClick(OrderItem order) {
        showDeliveryDetailsDialog(order.getId(), order.getCustomerName(), "Agent", "₹ 0", order.getAddress(), "Recent");
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

    private void showDeliveryDetailsDialog(String orderId, String userName, String agentName, String amount, String address, String pickupDate) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_delivery_details, null);
        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomDialogTheme)
            .setView(dialogView)
            .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        ((TextView) dialogView.findViewById(R.id.tv_delivery_order_id)).setText(orderId);
        ((TextView) dialogView.findViewById(R.id.tv_delivery_user_name)).setText(userName);
        ((TextView) dialogView.findViewById(R.id.tv_delivery_agent_name)).setText(agentName);
        ((TextView) dialogView.findViewById(R.id.tv_delivery_amount)).setText(amount);
        ((TextView) dialogView.findViewById(R.id.tv_delivery_address)).setText(address);
        ((TextView) dialogView.findViewById(R.id.tv_delivery_pickup_date)).setText(pickupDate);

        dialogView.findViewById(R.id.btn_delivery_dismiss).setOnClickListener(v -> dialog.dismiss());
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
