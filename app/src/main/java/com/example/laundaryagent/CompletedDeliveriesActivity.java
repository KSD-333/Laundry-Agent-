package com.example.laundaryagent;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.PopupMenu;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;

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
    }

    private void showFilterMenu() {
        PopupMenu popupMenu = new PopupMenu(this, btnFilter);
        popupMenu.getMenu().add("All Societies");
        popupMenu.getMenu().add("Amanora Park Town");
        popupMenu.getMenu().add("Magarpatta City");
        popupMenu.getMenu().add("Kalyani Nagar");
        popupMenu.getMenu().add("Viman Nagar");

        popupMenu.setOnMenuItemClickListener(item -> {
            tvCurrentFilter.setText(item.getTitle().toString());
            // Here you would filter the list
            return true;
        });
        popupMenu.show();
    }
}
