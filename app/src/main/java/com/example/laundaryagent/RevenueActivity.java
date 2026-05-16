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

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import androidx.core.util.Pair;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RevenueActivity extends AppCompatActivity {
    
    private TextView tvOverallRevenue, tvFilteredTotal;
    private TextView tvOverallOrders, tvFilteredOrders;
    private TextView yLabelTop, yLabelMid;
    private TextView tvCurrentFilter;
    private TextView tvService1Amount, tvService2Amount, tvService3Amount;
    private TextView tvService1Pct, tvService2Pct, tvService3Pct;
    private ProgressBar progressWash, progressDry, progressIron;
    private LinearLayout chartScrollContainer;
    
    private MaterialButton btnFilter;
    private final String OVERALL_REV_VAL = "₹25,50,000";
    private final String OVERALL_ORD_VAL = "15,420";

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
        tvOverallRevenue.setText(OVERALL_REV_VAL);
        
        // Orders Quick Stats
        tvOverallOrders = findViewById(R.id.tv_overall_orders);
        tvFilteredOrders = findViewById(R.id.tv_filtered_orders);
        tvOverallOrders.setText(OVERALL_ORD_VAL);
        
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

        setupClickListeners();
        
        // Default to 1 Week
        updateGraphData("Past 1 Week");
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
        switch (range) {
            case "Past 1 Day":
                renderView("₹4,500", "24", "₹5k", "₹2.5k",
                    new int[]{1200, 2500, 1800, 3200, 4100, 2800}, 
                    new String[]{"8AM", "10AM", "12PM", "2PM", "4PM", "6PM"},
                    new int[]{2800, 1200, 500});
                break;
            case "Past 1 Week":
                renderView("₹32,400", "184", "₹10k", "₹5k",
                    new int[]{4200, 6800, 5100, 8400, 9200, 7100, 4500}, 
                    new String[]{"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"},
                    new int[]{21060, 8100, 3240});
                break;
            case "Past 1 Month":
                renderView("₹1,24,500", "742", "₹40k", "₹20k",
                    new int[]{28000, 35000, 21000, 42000}, 
                    new String[]{"Week 1", "Week 2", "Week 3", "Week 4"},
                    new int[]{80925, 31125, 12450});
                break;
            case "Past 3 Months":
                renderView("₹3,80,000", "2,140", "₹150k", "₹75k",
                    new int[]{110000, 145000, 125000}, 
                    new String[]{"Oct", "Nov", "Dec"},
                    new int[]{247000, 95000, 38000});
                break;
            case "Past 6 Months":
                renderView("₹7,50,000", "4,280", "₹150k", "₹75k",
                    new int[]{120000, 140000, 110000, 160000, 180000, 140000}, 
                    new String[]{"Jul", "Aug", "Sep", "Oct", "Nov", "Dec"},
                    new int[]{487500, 187500, 75000});
                break;
            case "Past 1 Year":
                renderView("₹15,20,000", "8,940", "₹200k", "₹100k",
                    new int[]{90000, 110000, 130000, 120000, 150000, 140000, 160000, 180000, 170000, 190000, 200000, 180000}, 
                    new String[]{"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"},
                    new int[]{988000, 380000, 152000});
                break;
        }
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
}
