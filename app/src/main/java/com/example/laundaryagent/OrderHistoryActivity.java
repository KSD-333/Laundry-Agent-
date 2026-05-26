package com.example.laundaryagent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import java.util.ArrayList;
import java.util.List;

public class OrderHistoryActivity extends AppCompatActivity {

    private List<OrderHistory> allHistory = new ArrayList<>();
    private List<OrderHistory> filteredHistory = new ArrayList<>();
    private HistoryAdapter adapter;
    private TextView filterAll, filterCompleted, filterPending;
    private TextView tvFilterSocietyText, tvFilterMonthsText;
    private String currentSocietyFilter = "All Societies";
    private String currentMonthFilter = "All Months";
    private String currentStatusFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_history);

        initViews();
        setupFilters();
        loadMockData();
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        
        filterAll = findViewById(R.id.filter_all);
        filterCompleted = findViewById(R.id.filter_completed);
        filterPending = findViewById(R.id.filter_pending);

        tvFilterSocietyText = findViewById(R.id.tv_filter_society_text);
        tvFilterMonthsText = findViewById(R.id.tv_filter_months_text);

        findViewById(R.id.btn_filter_society).setOnClickListener(this::showSocietyFilterMenu);
        findViewById(R.id.btn_filter_months).setOnClickListener(this::showMonthsFilterMenu);

        RecyclerView rvHistory = findViewById(R.id.rv_history);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredHistory);
        rvHistory.setAdapter(adapter);
    }

    private void setupFilters() {
        filterAll.setOnClickListener(v -> applyFilter("All"));
        filterCompleted.setOnClickListener(v -> applyFilter("Completed"));
        filterPending.setOnClickListener(v -> applyFilter("Pending"));
    }

    private void showSocietyFilterMenu(View v) {
        View popupView = getLayoutInflater().inflate(R.layout.layout_custom_dropdown, null);
        PopupWindow popupWindow = new PopupWindow(popupView, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(20);

        LinearLayout optionsLayout = popupView.findViewById(R.id.ll_dropdown_options);

        // Dynamically extract unique societies from all history
        List<String> societies = new ArrayList<>();
        societies.add("All Societies");
        for (OrderHistory order : allHistory) {
            if (order.society != null && !order.society.isEmpty() && !societies.contains(order.society)) {
                societies.add(order.society);
            }
        }

        for (String society : societies) {
            View itemView = getLayoutInflater().inflate(R.layout.item_filter_option, null);
            ((TextView) itemView.findViewById(R.id.tv_option_text)).setText(society);
            ((ImageView) itemView.findViewById(R.id.iv_option_icon)).setImageResource(R.drawable.ic_pin);
            
            if (currentSocietyFilter.equals(society)) {
                itemView.findViewById(R.id.iv_check).setVisibility(View.VISIBLE);
                ((com.google.android.material.card.MaterialCardView) itemView.findViewById(R.id.card_option_icon)).setCardBackgroundColor(0xFFE0F2FE);
                ((ImageView) itemView.findViewById(R.id.iv_option_icon)).setColorFilter(0xFF0EA5E9);
            }

            itemView.setOnClickListener(click -> {
                popupWindow.dismiss();
                currentSocietyFilter = society;
                if (tvFilterSocietyText != null) {
                    tvFilterSocietyText.setText(society);
                }
                applyFilter(null);
            });
            optionsLayout.addView(itemView);
        }

        popupWindow.showAsDropDown(v, 0, 10);
    }

    private void showMonthsFilterMenu(View v) {
        View popupView = getLayoutInflater().inflate(R.layout.layout_custom_dropdown, null);
        PopupWindow popupWindow = new PopupWindow(popupView, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(20);

        LinearLayout optionsLayout = popupView.findViewById(R.id.ll_dropdown_options);

        String[] timeframes = {"All Months", "May", "April", "March"};
        int[] icons = {R.drawable.ic_clock, R.drawable.ic_calendar, R.drawable.ic_calendar, R.drawable.ic_calendar};

        for (int i = 0; i < timeframes.length; i++) {
            final String month = timeframes[i];
            View itemView = getLayoutInflater().inflate(R.layout.item_filter_option, null);
            ((TextView) itemView.findViewById(R.id.tv_option_text)).setText(month);
            ((ImageView) itemView.findViewById(R.id.iv_option_icon)).setImageResource(icons[i]);
            
            if (currentMonthFilter.equals(month)) {
                itemView.findViewById(R.id.iv_check).setVisibility(View.VISIBLE);
                ((com.google.android.material.card.MaterialCardView) itemView.findViewById(R.id.card_option_icon)).setCardBackgroundColor(0xFFE0F2FE);
                ((ImageView) itemView.findViewById(R.id.iv_option_icon)).setColorFilter(0xFF0EA5E9);
            }

            itemView.setOnClickListener(click -> {
                popupWindow.dismiss();
                currentMonthFilter = month;
                if (tvFilterMonthsText != null) {
                    tvFilterMonthsText.setText(month);
                }
                applyFilter(null);
            });
            optionsLayout.addView(itemView);
        }

        popupWindow.showAsDropDown(v, 0, 10);
    }

    private void applyFilter(String status) {
        if (status != null) {
            currentStatusFilter = status;
        }

        // Reset chip styles
        resetFilterStyles();

        // Highlight selected chip
        if (currentStatusFilter.equals("All")) {
            filterAll.setBackgroundResource(R.drawable.bg_chip_selected);
            filterAll.setTextColor(getResources().getColor(android.R.color.white));
        } else if (currentStatusFilter.equals("Completed")) {
            filterCompleted.setBackgroundResource(R.drawable.bg_chip_selected);
            filterCompleted.setTextColor(getResources().getColor(android.R.color.white));
        } else if (currentStatusFilter.equals("Pending")) {
            filterPending.setBackgroundResource(R.drawable.bg_chip_selected);
            filterPending.setTextColor(getResources().getColor(android.R.color.white));
        }

        // Filter data by status AND society AND month
        filteredHistory.clear();
        for (OrderHistory order : allHistory) {
            boolean matchesStatus = currentStatusFilter.equals("All") || order.status.equalsIgnoreCase(currentStatusFilter);
            
            boolean matchesSociety = currentSocietyFilter.equals("All Societies") || 
                (order.society != null && order.society.equalsIgnoreCase(currentSocietyFilter));
            
            boolean matchesMonth = currentMonthFilter.equals("All Months") || 
                (order.month != null && order.month.equalsIgnoreCase(currentMonthFilter));

            if (matchesStatus && matchesSociety && matchesMonth) {
                filteredHistory.add(order);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void resetFilterStyles() {
        int unselectedColor = 0xFF64748B;
        filterAll.setBackgroundResource(R.drawable.bg_chip_unselected);
        filterAll.setTextColor(unselectedColor);
        filterCompleted.setBackgroundResource(R.drawable.bg_chip_unselected);
        filterCompleted.setTextColor(unselectedColor);
        filterPending.setBackgroundResource(R.drawable.bg_chip_unselected);
        filterPending.setTextColor(unselectedColor);
    }

    private void loadMockData() {
        allHistory.add(new OrderHistory("Rahul Sharma", "₹ 450.00", "12:30 PM, 13 May", "Completed", "Amanora Park Town", "May"));
        allHistory.add(new OrderHistory("Priya Patel", "₹ 320.00", "11:15 AM, 13 May", "Completed", "Blue Ridge Town", "May"));
        allHistory.add(new OrderHistory("Amit Verma", "₹ 890.00", "09:45 AM, 13 May", "Pending", "Magarpatta City", "May"));
        allHistory.add(new OrderHistory("Sneha Gupta", "₹ 210.00", "06:20 PM, 12 April", "Completed", "Amanora Park Town", "April"));
        allHistory.add(new OrderHistory("Vikram Singh", "₹ 550.00", "04:10 PM, 12 April", "Canceled", "Blue Ridge Town", "April"));
        allHistory.add(new OrderHistory("Anjali Rao", "₹ 1,200.00", "02:30 PM, 12 March", "Completed", "Magarpatta City", "March"));
        allHistory.add(new OrderHistory("Karan Mehra", "₹ 670.00", "10:00 AM, 12 March", "Pending", "Amanora Park Town", "March"));

        filteredHistory.addAll(allHistory);
        adapter.notifyDataSetChanged();
    }

    private static class OrderHistory {
        String userName;
        String amount;
        String time;
        String status;
        String society;
        String month;

        OrderHistory(String userName, String amount, String time, String status, String society, String month) {
            this.userName = userName;
            this.amount = amount;
            this.time = time;
            this.status = status;
            this.society = society;
            this.month = month;
        }
    }

    private class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {
        private List<OrderHistory> historyList;

        HistoryAdapter(List<OrderHistory> historyList) {
            this.historyList = historyList;
        }

        @Override
        public HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_order, parent, false);
            return new HistoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(HistoryViewHolder holder, int position) {
            OrderHistory order = historyList.get(position);
            holder.tvUser.setText(order.userName);
            holder.tvAmount.setText(order.amount);
            holder.tvTime.setText(order.time);
            holder.tvStatus.setText(order.status);
            holder.tvOrderId.setText("Order #" + (1024 + position) + " · " + order.society);
            
            if (order.status.equalsIgnoreCase("Completed")) {
                holder.cardStatus.setCardBackgroundColor(0xFFD1FAE5);
                holder.tvStatus.setTextColor(0xFF065F46);
            } else if (order.status.equalsIgnoreCase("Canceled")) {
                holder.cardStatus.setCardBackgroundColor(0xFFFEE2E2);
                holder.tvStatus.setTextColor(0xFF991B1B);
            } else {
                holder.cardStatus.setCardBackgroundColor(0xFFFEF3C7);
                holder.tvStatus.setTextColor(0xFF92400E);
            }

            holder.itemView.setOnClickListener(v -> showOrderDetailsDialog(order, position));
        }

        private void showOrderDetailsDialog(OrderHistory order, int position) {
            View dialogView = LayoutInflater.from(OrderHistoryActivity.this).inflate(R.layout.dialog_admin_order_details, null);
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(OrderHistoryActivity.this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            TextView tvOrderId = dialogView.findViewById(R.id.tv_dialog_order_id);
            TextView tvStatus = dialogView.findViewById(R.id.tv_dialog_status);
            com.google.android.material.card.MaterialCardView cardStatus = dialogView.findViewById(R.id.card_dialog_status);
            
            tvOrderId.setText("#ORD-" + (7742 + position) + " · " + order.userName + " (" + order.society + ")");
            tvStatus.setText(order.status);
            ((TextView) dialogView.findViewById(R.id.tv_dialog_amount)).setText(order.amount);
            
            if (order.status.equalsIgnoreCase("Completed")) {
                cardStatus.setCardBackgroundColor(0xFFD1FAE5);
                tvStatus.setTextColor(0xFF065F46);
            } else if (order.status.equalsIgnoreCase("Canceled")) {
                cardStatus.setCardBackgroundColor(0xFFFEE2E2);
                tvStatus.setTextColor(0xFF991B1B);
            } else {
                cardStatus.setCardBackgroundColor(0xFFFEF3C7);
                tvStatus.setTextColor(0xFF92400E);
            }

            dialogView.findViewById(R.id.btn_close_dialog).setOnClickListener(view -> dialog.dismiss());
            dialog.show();
        }

        @Override
        public int getItemCount() {
            return historyList.size();
        }

        class HistoryViewHolder extends RecyclerView.ViewHolder {
            TextView tvUser, tvAmount, tvTime, tvStatus, tvOrderId;
            MaterialCardView cardStatus;

            HistoryViewHolder(View itemView) {
                super(itemView);
                tvUser = itemView.findViewById(R.id.tv_order_user);
                tvOrderId = itemView.findViewById(R.id.tv_order_id);
                tvAmount = itemView.findViewById(R.id.tv_order_amount);
                tvTime = itemView.findViewById(R.id.tv_order_time);
                tvStatus = itemView.findViewById(R.id.tv_order_status);
                cardStatus = itemView.findViewById(R.id.card_status);
            }
        }
    }
}
