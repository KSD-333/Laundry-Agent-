package com.example.laundaryagent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class AdminDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        MaterialButton btnLogout = findViewById(R.id.btn_logout_admin);
        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("LaundryPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Setup navigation for stat cards
        findViewById(R.id.card_revenue).setOnClickListener(v -> 
            startActivity(new Intent(this, RevenueActivity.class)));
            
        findViewById(R.id.card_total_users).setOnClickListener(v -> 
            startActivity(new Intent(this, UsersActivity.class)));
            
        findViewById(R.id.card_pending_pickups).setOnClickListener(v -> 
            startActivity(new Intent(this, PendingPickupsActivity.class)));

        findViewById(R.id.card_completed_deliveries).setOnClickListener(v -> 
            startActivity(new Intent(this, CompletedDeliveriesActivity.class)));

        // Setup history navigation
        findViewById(R.id.tv_view_all_history).setOnClickListener(v -> 
            startActivity(new Intent(this, OrderHistoryActivity.class)));
            
        findViewById(R.id.card_history_shortcut).setOnClickListener(v -> 
            startActivity(new Intent(this, OrderHistoryActivity.class)));
    }
}
