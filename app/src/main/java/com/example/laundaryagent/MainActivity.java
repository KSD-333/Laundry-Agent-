package com.example.laundaryagent;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Fragment tasksFragment, reportsFragment, profileFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initial fragment
        loadFragment(new TasksFragment());

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_tasks) {
                loadFragment(new TasksFragment());
                return true;
            } else if (id == R.id.nav_reports) {
                loadFragment(new ReportsFragment());
                return true;
            } else if (id == R.id.nav_profile) {
                loadFragment(new ProfileFragment());
                return true;
            }
            return false;
        });

        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_tasks);
        badge.setBackgroundColor(Color.RED);
        badge.setBadgeTextColor(Color.WHITE);

        TaskViewModel viewModel = new androidx.lifecycle.ViewModelProvider(this).get(TaskViewModel.class);
        
        // Observe both the date and the items to update the bottom navigation badge
        androidx.lifecycle.Observer<Object> updateBadgeObserver = obj -> {
            String targetDate = viewModel.getSelectedDate().getValue();
            java.util.List<com.example.laundaryagent.data.model.OrderItem> items = viewModel.getOrderItems(this).getValue();
            
            if (targetDate == null || items == null) return;
            String normalizedTarget = normalizeDateStr(targetDate);
            
            int pendingCount = 0;
            for (com.example.laundaryagent.data.model.OrderItem item : items) {
                String pDate = normalizeDateStr(item.getPickupDate());
                if (pDate.equals(normalizedTarget) && 
                    (item.getStatus() == com.example.laundaryagent.data.model.OrderStatus.PENDING || 
                     item.getStatus() == com.example.laundaryagent.data.model.OrderStatus.PICKING_PENDING)) {
                    pendingCount++;
                }
            }
            
            if (pendingCount > 0) {
                badge.setVisible(true);
                badge.setNumber(pendingCount);
            } else {
                badge.setVisible(false);
            }
        };

        viewModel.getSelectedDate().observe(this, updateBadgeObserver);
        viewModel.getOrderItems(this).observe(this, updateBadgeObserver);
    }

    private String normalizeDateStr(String date) {
        if (date == null) return "";
        String s = date.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
        if (s.startsWith("0") && s.length() > 1 && Character.isDigit(s.charAt(1))) {
            s = s.substring(1);
        }
        return s;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}