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

    private TextView tvTotalUsers, tvPendingOrders, tvCompletedOrders, tvOverallOrders;
    private final List<ListenerRegistration> listeners = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        // KPI TextViews in cards
        tvTotalUsers      = findViewById(R.id.tv_stat_total_users);
        tvPendingOrders   = findViewById(R.id.tv_stat_pending_pickups);
        tvCompletedOrders = findViewById(R.id.tv_stat_completed_deliveries);
        tvOverallOrders   = findViewById(R.id.tv_stat_overall_orders);

        attachFirebaseListeners();
        setupNavigation();
    }

    private void attachFirebaseListeners() {
        FirebaseRepository fb = FirebaseRepository.getInstance();

        listeners.add(fb.listenTotalUsers(count -> runOnUiThread(() -> {
            if (tvTotalUsers != null) tvTotalUsers.setText(String.valueOf(count));
        })));

        listeners.add(fb.listenTotalOrders(count -> runOnUiThread(() -> {
            if (tvOverallOrders != null) tvOverallOrders.setText(String.valueOf(count));
        })));

        listeners.add(fb.listenPendingOrders(count -> runOnUiThread(() -> {
            if (tvPendingOrders != null) tvPendingOrders.setText(String.valueOf(count));
        })));

        listeners.add(fb.listenCompletedOrders(count -> runOnUiThread(() -> {
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

        findViewById(R.id.card_total_users).setOnClickListener(v ->
            startActivity(new Intent(this, UsersActivity.class)));

        findViewById(R.id.card_pending_pickups).setOnClickListener(v ->
            startActivity(new Intent(this, PendingPickupsActivity.class)));

        findViewById(R.id.card_completed_deliveries).setOnClickListener(v ->
            startActivity(new Intent(this, CompletedDeliveriesActivity.class)));

        if (findViewById(R.id.tv_view_all_history) != null)
            findViewById(R.id.tv_view_all_history).setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class)));

        if (findViewById(R.id.card_history_shortcut) != null)
            findViewById(R.id.card_history_shortcut).setOnClickListener(v ->
                startActivity(new Intent(this, OrderHistoryActivity.class)));
    }

    @Override
    protected void onDestroy() {
        // Clean up all real-time listeners to prevent memory leaks
        for (ListenerRegistration reg : listeners) reg.remove();
        super.onDestroy();
    }
}
