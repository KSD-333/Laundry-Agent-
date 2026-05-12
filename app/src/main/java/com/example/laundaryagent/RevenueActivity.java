package com.example.laundaryagent;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.PopupMenu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import androidx.core.util.Pair;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RevenueActivity extends AppCompatActivity {
    
    private TextView tvRevenueAmount;
    private TextView yLabelTop, yLabelMid;
    private TextView xLabel1, xLabel2, xLabel3, xLabel4, xLabel5, xLabel6;
    private TextView tvCurrentFilter;
    
    private MaterialButton btnFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_revenue);

        tvRevenueAmount = findViewById(R.id.tv_revenue_amount);
        tvCurrentFilter = findViewById(R.id.tv_current_filter);
        
        yLabelTop = findViewById(R.id.y_label_top);
        yLabelMid = findViewById(R.id.y_label_mid);
        
        xLabel1 = findViewById(R.id.x_label_1);
        xLabel2 = findViewById(R.id.x_label_2);
        xLabel3 = findViewById(R.id.x_label_3);
        xLabel4 = findViewById(R.id.x_label_4);
        xLabel5 = findViewById(R.id.x_label_5);
        xLabel6 = findViewById(R.id.x_label_6);

        btnFilter = findViewById(R.id.btn_filter);

        setupClickListeners();
        
        // Default to 1 Week
        updateGraphData("1W");
    }

    private void setupClickListeners() {
        btnFilter.setOnClickListener(v -> showFilterMenu());
    }

    private void showFilterMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnFilter);
        popupMenu.getMenu().add("Past 1 Day");
        popupMenu.getMenu().add("Past 1 Week");
        popupMenu.getMenu().add("Past 1 Month");
        popupMenu.getMenu().add("Past 3 Months");
        popupMenu.getMenu().add("Past 6 Months");
        popupMenu.getMenu().add("Past 1 Year");
        popupMenu.getMenu().add("Custom Date Range");

        popupMenu.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Custom Date Range")) {
                showCustomDatePicker();
            } else {
                tvCurrentFilter.setText(title);
                if (title.equals("Past 1 Day")) updateGraphData("1D");
                else if (title.equals("Past 1 Week")) updateGraphData("1W");
                else if (title.equals("Past 1 Month")) updateGraphData("1M");
                else if (title.equals("Past 3 Months")) updateGraphData("3M");
                else if (title.equals("Past 6 Months")) updateGraphData("6M");
                else if (title.equals("Past 1 Year")) updateGraphData("1Y");
            }
            return true;
        });
        popupMenu.show();
    }

    private void updateGraphData(String range) {

        switch (range) {
            case "1D":
                tvRevenueAmount.setText("₹ 4,500");
                yLabelTop.setText("₹5k"); yLabelMid.setText("₹2.5k");
                xLabel1.setText("8AM"); xLabel2.setText("10AM"); xLabel3.setText("12PM");
                xLabel4.setText("2PM"); xLabel5.setText("4PM"); xLabel6.setText("6PM");
                break;
            case "1W":
                tvRevenueAmount.setText("₹ 32,400");
                yLabelTop.setText("₹40k"); yLabelMid.setText("₹20k");
                xLabel1.setText("Mon"); xLabel2.setText("Tue"); xLabel3.setText("Wed");
                xLabel4.setText("Thu"); xLabel5.setText("Fri"); xLabel6.setText("Sat");
                break;
            case "1M":
                tvRevenueAmount.setText("₹ 1,24,500");
                yLabelTop.setText("₹150k"); yLabelMid.setText("₹75k");
                xLabel1.setText("Wk1"); xLabel2.setText("Wk2"); xLabel3.setText("Wk3");
                xLabel4.setText("Wk4"); xLabel5.setText("Wk5"); xLabel6.setText("End");
                break;
            case "3M":
                tvRevenueAmount.setText("₹ 3,80,000");
                yLabelTop.setText("₹400k"); yLabelMid.setText("₹200k");
                xLabel1.setText("Jan"); xLabel2.setText("Feb"); xLabel3.setText("Mar");
                xLabel4.setText("Wk1"); xLabel5.setText("Wk2"); xLabel6.setText("Wk3");
                break;
            case "6M":
                tvRevenueAmount.setText("₹ 7,50,000");
                yLabelTop.setText("₹800k"); yLabelMid.setText("₹400k");
                xLabel1.setText("Jan"); xLabel2.setText("Feb"); xLabel3.setText("Mar");
                xLabel4.setText("Apr"); xLabel5.setText("May"); xLabel6.setText("Jun");
                break;
            case "1Y":
                tvRevenueAmount.setText("₹ 15,20,000");
                yLabelTop.setText("₹2M"); yLabelMid.setText("₹1M");
                xLabel1.setText("Jan-Feb"); xLabel2.setText("Mar-Apr"); xLabel3.setText("May-Jun");
                xLabel4.setText("Jul-Aug"); xLabel5.setText("Sep-Oct"); xLabel6.setText("Nov-Dec");
                break;
        }
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
            tvRevenueAmount.setText("₹ --,---"); // Mock loading or fetched amount
            yLabelTop.setText("₹--"); yLabelMid.setText("₹--");
            xLabel1.setText(startDate); xLabel2.setText("..."); xLabel3.setText("...");
            xLabel4.setText("..."); xLabel5.setText("..."); xLabel6.setText(endDate);
            Toast.makeText(this, "Fetching data for " + startDate + " to " + endDate, Toast.LENGTH_SHORT).show();
        });

        dateRangePicker.show(getSupportFragmentManager(), "DATE_PICKER");
    }
}
