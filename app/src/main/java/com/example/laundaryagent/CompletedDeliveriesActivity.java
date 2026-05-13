package com.example.laundaryagent;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;

import com.google.android.material.button.MaterialButton;

public class CompletedDeliveriesActivity extends AppCompatActivity {

    private TextView tvCurrentFilter;
    private MaterialButton btnFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_completed_deliveries);

        tvCurrentFilter = findViewById(R.id.tv_current_filter);
        btnFilter = findViewById(R.id.btn_filter);

        btnFilter.setOnClickListener(v -> showFilterMenu());

        findViewById(R.id.card_delivery_1).setOnClickListener(v -> showDeliveryDetailsDialog("Order #1982", "Vikas Joshi", "Raj Kumar", "₹ 450.00", "C-201, Sapphire Heights, Pune", "12 May, 2024"));
        findViewById(R.id.card_delivery_2).setOnClickListener(v -> showDeliveryDetailsDialog("Order #1980", "Mrs. Mehta", "Amit Singh", "₹ 1,200.00", "Villa 44, Magarpatta City, Pune", "11 May, 2024"));
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
                // Filter logic here
            });
            optionsLayout.addView(itemView);
        }

        popupWindow.showAsDropDown(btnFilter, 0, 10);
    }
}
