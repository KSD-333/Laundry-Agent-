package com.example.laundaryagent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboardActivity extends AppCompatActivity {

    private TextView tvTotalUsers, tvPendingOrders, tvCompletedOrders, tvRevenueAnalytics;
    private final List<ListenerRegistration> listeners = new ArrayList<>();
    private String franchiseId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // Read franchise info saved at login time
        SharedPreferences prefs = getSharedPreferences("LaundryPrefs", MODE_PRIVATE);
        franchiseId  = prefs.getString("franchise_id", "");
        String fName = prefs.getString("franchise_name", "Franchise Admin");
        String fLocation = prefs.getString("franchise_location", "");

        // Show franchise name and location in header
        TextView tvAdminName    = findViewById(R.id.tv_admin_name);
        TextView tvAdminWelcome = findViewById(R.id.tv_admin_welcome);

        if (tvAdminName != null)    tvAdminName.setText(fName);
        if (tvAdminWelcome != null) {
            tvAdminWelcome.setText(fLocation.isEmpty() ? "Welcome Back," : fLocation);
        }

        // KPI TextViews in cards
        tvTotalUsers       = findViewById(R.id.tv_stat_total_users);
        tvPendingOrders    = findViewById(R.id.tv_stat_pending_pickups);
        tvCompletedOrders  = findViewById(R.id.tv_stat_completed_deliveries);
        tvRevenueAnalytics = findViewById(R.id.tv_stat_revenue_analytics);

        attachFirebaseListeners();
        setupNavigation();
    }

    private void attachFirebaseListeners() {
        FirebaseRepository fb = FirebaseRepository.getInstance();

        // Total customers for THIS franchise only (filtered by franchiseId)
        listeners.add(fb.listenFranchiseUsers(franchiseId, count -> runOnUiThread(() -> {
            if (tvTotalUsers != null) tvTotalUsers.setText(String.valueOf(count));
        })));

        // Revenue for THIS franchise only
        fb.getFranchiseRevenue(franchiseId,
            err -> runOnUiThread(() -> {
                if (tvRevenueAnalytics != null) tvRevenueAnalytics.setText("Rs.0");
            }),
            total -> runOnUiThread(() -> {
                if (tvRevenueAnalytics != null)
                    tvRevenueAnalytics.setText("Rs." + String.format("%,d", total));
            })
        );

        // Pending pickups for THIS franchise only
        listeners.add(fb.listenFranchisePendingOrders(franchiseId, count -> runOnUiThread(() -> {
            if (tvPendingOrders != null) tvPendingOrders.setText(String.valueOf(count));
        })));

        // Completed deliveries for THIS franchise only
        listeners.add(fb.listenFranchiseCompletedOrders(franchiseId, count -> runOnUiThread(() -> {
            if (tvCompletedOrders != null) tvCompletedOrders.setText(String.valueOf(count));
        })));
    }

    private void setupNavigation() {
        MaterialButton btnLogout = findViewById(R.id.btn_logout_admin);
        btnLogout.setOnClickListener(v -> {
            SharedPreferences prefs = getSharedPreferences("LaundryPrefs", MODE_PRIVATE);
            prefs.edit().clear().apply();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.card_revenue).setOnClickListener(v ->
            startActivity(new Intent(this, RevenueActivity.class)));

        // Pass franchiseId so UsersActivity can also filter
        findViewById(R.id.card_total_users).setOnClickListener(v -> {
            Intent i = new Intent(this, UsersActivity.class);
            i.putExtra("franchise_id", franchiseId);
            startActivity(i);
        });

        // Pass franchiseId so PendingPickupsActivity can also filter
        findViewById(R.id.card_pending_pickups).setOnClickListener(v -> {
            Intent i = new Intent(this, PendingPickupsActivity.class);
            i.putExtra("franchise_id", franchiseId);
            startActivity(i);
        });

        // Pass franchiseId so CompletedDeliveriesActivity can also filter
        findViewById(R.id.card_completed_deliveries).setOnClickListener(v -> {
            Intent i = new Intent(this, CompletedDeliveriesActivity.class);
            i.putExtra("franchise_id", franchiseId);
            startActivity(i);
        });

        if (findViewById(R.id.card_history_button) != null)
            findViewById(R.id.card_history_button).setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class)));
    }

    @Override
    protected void onDestroy() {
        for (ListenerRegistration reg : listeners) reg.remove();
        super.onDestroy();
    }
}
