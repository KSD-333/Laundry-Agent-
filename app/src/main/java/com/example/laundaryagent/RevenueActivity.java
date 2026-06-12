package com.example.laundaryagent;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.LinearLayout;
import android.view.Gravity;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.PopupWindow;
import android.graphics.drawable.ColorDrawable;
import android.view.ViewGroup;
import android.os.Handler;
import android.os.Looper;

import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.firebase.firestore.ListenerRegistration;

import androidx.core.util.Pair;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RevenueActivity extends AppCompatActivity {
    
    private TextView tvOverallRevenue, tvFilteredTotal, tvFilteredTotalLabel;
    private TextView tvOverallOrders, tvFilteredOrders, tvFilteredOrdersLabel;
    private TextView yLabelTop, yLabelMid;
    private TextView tvCurrentFilter;
    private TextView tvService1Amount, tvService2Amount, tvService3Amount;
    private TextView tvService1Pct, tvService2Pct, tvService3Pct;
    private ProgressBar progressWash, progressDry, progressIron;
    private LinearLayout chartScrollContainer;
    
    private MaterialButton btnFilter;
    private final List<ListenerRegistration> listeners = new ArrayList<>();
    private final List<java.util.Map<String, Object>> allOrderDocs = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue);

        // Header & Filter
        tvCurrentFilter = findViewById(R.id.tv_current_filter);
        btnFilter = findViewById(R.id.btn_filter);
        
        // Revenue Quick Stats
        tvOverallRevenue = findViewById(R.id.tv_overall_revenue);
        tvFilteredTotal = findViewById(R.id.tv_filtered_total);
        tvFilteredTotalLabel = findViewById(R.id.tv_filtered_total_label);
        tvOverallRevenue.setText("Loading...");
        
        // Orders Quick Stats
        tvOverallOrders = findViewById(R.id.tv_overall_orders);
        tvFilteredOrders = findViewById(R.id.tv_filtered_orders);
        tvFilteredOrdersLabel = findViewById(R.id.tv_filtered_orders_label);
        tvOverallOrders.setText("...");
        
        // Chart Containers
        yLabelTop = findViewById(R.id.y_label_top);
        yLabelMid = findViewById(R.id.y_label_mid);
        chartScrollContainer = findViewById(R.id.chart_scroll_container);

        // Service Breakdown
        tvService1Amount = findViewById(R.id.tv_service_1_amount);
        tvService2Amount = findViewById(R.id.tv_service_2_amount);
        tvService3Amount = findViewById(R.id.tv_service_3_amount);
        tvService1Pct = findViewById(R.id.tv_service_1_pct);
        tvService2Pct = findViewById(R.id.tv_service_2_pct);
        tvService3Pct = findViewById(R.id.tv_service_3_pct);
        progressWash = findViewById(R.id.progress_wash);
        progressDry = findViewById(R.id.progress_dry);
        progressIron = findViewById(R.id.progress_iron);

        android.content.SharedPreferences prefs = getSharedPreferences("LaundryPrefs", MODE_PRIVATE);
        String franchiseId = prefs.getString("franchise_id", "");

        listeners.add(FirebaseRepository.getInstance().listenFranchiseOrders(franchiseId, docs -> {
            runOnUiThread(() -> {
                allOrderDocs.clear();
                allOrderDocs.addAll(docs);
                recalculateOverallStats();
                updateGraphData(tvCurrentFilter.getText().toString());
            });
        }));

        setupClickListeners();
    }

    private void setupClickListeners() {
        btnFilter.setOnClickListener(v -> showFilterMenu());
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void showFilterMenu() {
        View popupView = getLayoutInflater().inflate(R.layout.layout_custom_dropdown, null);
        PopupWindow popupWindow = new PopupWindow(popupView, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(20);

        android.widget.LinearLayout optionsLayout = popupView.findViewById(R.id.ll_dropdown_options);

        String[] ranges = {"Past 1 Day", "Past 1 Week", "Past 1 Month", "Past 3 Months", "Past 6 Months", "Past 1 Year", "Custom Date Range"};
        int[] icons = {R.drawable.ic_clock, R.drawable.ic_nav_reports, R.drawable.ic_nav_reports, R.drawable.ic_nav_reports, R.drawable.ic_nav_reports, R.drawable.ic_nav_reports, R.drawable.ic_calendar};

        for (int i = 0; i < ranges.length; i++) {
            final String range = ranges[i];
            View itemView = getLayoutInflater().inflate(R.layout.item_filter_option, null);
            ((TextView) itemView.findViewById(R.id.tv_option_text)).setText(range);
            ((android.widget.ImageView) itemView.findViewById(R.id.iv_option_icon)).setImageResource(icons[i]);
            
            if (tvCurrentFilter.getText().toString().equals(range)) {
                itemView.findViewById(R.id.iv_check).setVisibility(View.VISIBLE);
                ((com.google.android.material.card.MaterialCardView) itemView.findViewById(R.id.card_option_icon)).setCardBackgroundColor(0xFFE0F2FE);
                ((android.widget.ImageView) itemView.findViewById(R.id.iv_option_icon)).setColorFilter(0xFF0EA5E9);
            }

            itemView.setOnClickListener(v -> {
                popupWindow.dismiss();
                if (range.equals("Custom Date Range")) {
                    showCustomDatePicker();
                } else {
                    tvCurrentFilter.setText(range);
                    btnFilter.setText(range);
                    updateGraphData(range);
                }
            });
            optionsLayout.addView(itemView);
        }

        popupWindow.showAsDropDown(btnFilter, 0, 10);
    }

    private void updateGraphData(String range) {
        if (tvFilteredTotalLabel != null) {
            tvFilteredTotalLabel.setText("Revenue (" + range + ")");
        }
        if (tvFilteredOrdersLabel != null) {
            tvFilteredOrdersLabel.setText("Orders (" + range + ")");
        }
        if (range.equals("Past 1 Day")) calculateGraphForRange(1, 6, range);
        else if (range.equals("Past 1 Week")) calculateGraphForRange(7, 7, range);
        else if (range.equals("Past 1 Month")) calculateGraphForRange(30, 4, range);
        else if (range.equals("Past 3 Months")) calculateGraphForRange(90, 3, range);
        else if (range.equals("Past 6 Months")) calculateGraphForRange(180, 6, range);
        else if (range.equals("Past 1 Year")) calculateGraphForRange(365, 12, range);
        else calculateGraphForRange(30, 4, range); // fallback for custom
    }

    private void recalculateOverallStats() {
        long totalRev = 0;
        int totalOrders = 0;
        for (java.util.Map<String, Object> doc : allOrderDocs) {
            String status = FirebaseRepository.str(doc, "status").toLowerCase();
            if (status.equals("completed") || status.equals("delivered")) {
                totalOrders++;
                Long amount = (Long) doc.get("totalAmount");
                if (amount != null) totalRev += amount;
            }
        }
        tvOverallOrders.setText(String.valueOf(totalOrders));
        tvOverallRevenue.setText("₹ " + String.format(Locale.US, "%,d", totalRev));
    }

    private void calculateGraphForRange(int days, int numBars, String rangeStr) {
        long[] revBars = new long[numBars];
        String[] labels = new String[numBars];
        long totalRev = 0;
        int totalOrders = 0;
        long washRev = 0, dryRev = 0, ironRev = 0;
        
        long now = System.currentTimeMillis();
        long interval = (days * 24L * 60 * 60 * 1000) / numBars;
        
        for (int i = 0; i < numBars; i++) {
            long barEnd = now - (numBars - 1 - i) * interval;
            if (days == 1) labels[i] = new SimpleDateFormat("ha", Locale.US).format(new Date(barEnd));
            else if (days <= 7) labels[i] = new SimpleDateFormat("EEE", Locale.US).format(new Date(barEnd));
            else if (days <= 30) labels[i] = "W" + (i+1);
            else labels[i] = new SimpleDateFormat("MMM", Locale.US).format(new Date(barEnd));
        }

        for (java.util.Map<String, Object> doc : allOrderDocs) {
            String status = FirebaseRepository.str(doc, "status").toLowerCase();
            if (!(status.equals("completed") || status.equals("delivered"))) continue;
            
            long orderTime = now; // fallback
            Object timeObj = doc.get("createdAt");
            if (timeObj == null) timeObj = doc.get("updatedAt");
            if (timeObj instanceof com.google.firebase.Timestamp) {
                orderTime = ((com.google.firebase.Timestamp) timeObj).toDate().getTime();
            }
            
            long timeDiff = now - orderTime;
            if (timeDiff <= days * 24L * 60 * 60 * 1000 && timeDiff >= 0) {
                totalOrders++;
                Long amount = (Long) doc.get("totalAmount");
                long amt = amount != null ? amount : 0;
                totalRev += amt;
                
                int barIdx = numBars - 1 - (int)(timeDiff / interval);
                if (barIdx >= 0 && barIdx < numBars) {
                    revBars[barIdx] += amt;
                }

                // mock service breakdown proportionally for now
                washRev += (long)(amt * 0.5);
                dryRev += (long)(amt * 0.3);
                ironRev += (long)(amt * 0.2);
            }
        }
        
        int[] intBars = new int[numBars];
        long maxBar = 0;
        for (int i = 0; i < numBars; i++) {
            intBars[i] = (int) revBars[i];
            if (revBars[i] > maxBar) maxBar = revBars[i];
        }
        
        String yTop = "₹" + (maxBar > 0 ? (maxBar + (maxBar/5)) : 1000);
        String yMid = "₹" + (maxBar > 0 ? (maxBar/2) : 500);

        renderView("₹ " + String.format(Locale.US, "%,d", totalRev), String.valueOf(totalOrders), yTop, yMid,
                intBars, labels, new int[]{(int)washRev, (int)dryRev, (int)ironRev});
    }

    private void renderView(String rev, String orders, String yTop, String yMid, int[] barData, String[] xLabels, int[] serviceData) {
        tvFilteredTotal.setText(rev);
        tvFilteredOrders.setText(orders);
        yLabelTop.setText(yTop);
        yLabelMid.setText(yMid);
        
        setupDynamicBars(barData, xLabels, yTop);
        updateServiceBreakdown(serviceData[0], serviceData[1], serviceData[2]);
    }

    private void setupDynamicBars(int[] data, String[] labels, String yTopStr) {
        chartScrollContainer.removeAllViews();
        
        float density = getResources().getDisplayMetrics().density;
        int maxVal = parseYValue(yTopStr);
        int barWidth = (int) (36 * density);
        int barMargin = (int) (12 * density);
        
        if (data.length > 7) {
            barWidth = (int) (28 * density);
            barMargin = (int) (6 * density);
        }

        for (int i = 0; i < data.length; i++) {
            final int value = data[i];
            
            // Create a vertical unit for each data point
            LinearLayout unit = new LinearLayout(this);
            unit.setOrientation(LinearLayout.VERTICAL);
            unit.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams unitParams = new LinearLayout.LayoutParams(barWidth + (barMargin * 2), ViewGroup.LayoutParams.MATCH_PARENT);
            unit.setLayoutParams(unitParams);

            // 1. Tooltip Value (Hidden initially)
            TextView tvValue = new TextView(this);
            tvValue.setText("₹" + String.format("%,d", value));
            tvValue.setTextSize(10);
            tvValue.setTextColor(0xFF0EA5E9);
            tvValue.setGravity(Gravity.CENTER);
            tvValue.setVisibility(View.INVISIBLE);
            unit.addView(tvValue);

            // 2. The Bar
            View bar = new View(this);
            int barHeight = (int) (value * 140 / maxVal * density); // Scaled height
            LinearLayout.LayoutParams barParams = new LinearLayout.LayoutParams(barWidth, Math.max(barHeight, (int)(4*density)));
            barParams.setMargins(0, 4, 0, 8);
            bar.setLayoutParams(barParams);
            bar.setBackgroundResource(R.drawable.bg_revenue_bar);
            if (i % 2 != 0) bar.setAlpha(0.85f);
            unit.addView(bar);

            // 3. X-Axis Label
            TextView tvLabel = new TextView(this);
            tvLabel.setText(labels[i]);
            tvLabel.setTextSize(10);
            tvLabel.setTextColor(0xFF64748B);
            tvLabel.setGravity(Gravity.CENTER);
            unit.addView(tvLabel);

            // Interaction
            bar.setOnClickListener(v -> {
                tvValue.setVisibility(View.VISIBLE);
                new Handler(Looper.getMainLooper()).postDelayed(() -> tvValue.setVisibility(View.INVISIBLE), 2500);
            });

            chartScrollContainer.addView(unit);
        }
    }

    private int parseYValue(String yStr) {
        try {
            String clean = yStr.replace("₹", "").replace("k", "000").replace("M", "000000").trim();
            return Integer.parseInt(clean);
        } catch (Exception e) {
            return 100000;
        }
    }

    private void updateServiceBreakdown(int wash, int dry, int iron) {
        int total = wash + dry + iron;
        if (total == 0) return;
        
        tvService1Amount.setText("₹" + String.format("%,d", wash));
        tvService2Amount.setText("₹" + String.format("%,d", dry));
        tvService3Amount.setText("₹" + String.format("%,d", iron));
        
        int p1 = (wash * 100) / total;
        int p2 = (dry * 100) / total;
        int p3 = 100 - p1 - p2;
        
        tvService1Pct.setText(p1 + "% of total revenue");
        tvService2Pct.setText(p2 + "% of total revenue");
        tvService3Pct.setText(p3 + "% of total revenue");
        
        progressWash.setProgress(p1);
        progressDry.setProgress(p2);
        progressIron.setProgress(p3);
    }

    private void showCustomDatePicker() {
        MaterialDatePicker<Pair<Long, Long>> dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Select Dates")
            .build();

        dateRangePicker.addOnPositiveButtonClickListener(selection -> {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd", Locale.getDefault());
            String startDate = sdf.format(new Date(selection.first));
            String endDate = sdf.format(new Date(selection.second));
            
            tvCurrentFilter.setText(startDate + " - " + endDate);
            btnFilter.setText(startDate + " - " + endDate);
            tvFilteredTotal.setText("₹ --,---");
            tvFilteredOrders.setText("--");
            yLabelTop.setText("₹--"); yLabelMid.setText("₹--");
            chartScrollContainer.removeAllViews();
            Toast.makeText(this, "Fetching data for " + startDate + " to " + endDate, Toast.LENGTH_SHORT).show();
        });

        dateRangePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }

    @Override
    protected void onDestroy() {
        for (com.google.firebase.firestore.ListenerRegistration reg : listeners) reg.remove();
        super.onDestroy();
    }
}
