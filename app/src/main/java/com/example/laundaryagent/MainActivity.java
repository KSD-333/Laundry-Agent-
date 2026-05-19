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

        // Real-time badge from Firebase
        BadgeDrawable badge = bottomNav.getOrCreateBadge(R.id.nav_tasks);
        badge.setBackgroundColor(Color.RED);
        badge.setBadgeTextColor(Color.WHITE);

        com.example.laundaryagent.data.repository.FirebaseRepository.getInstance()
            .listenPendingOrders(count -> runOnUiThread(() -> {
                if (count > 0) {
                    badge.setVisible(true);
                    badge.setNumber((int) count);
                } else {
                    badge.setVisible(false);
                }
            }));
    }

    private void loadFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, fragment);
        transaction.commit();
    }
}