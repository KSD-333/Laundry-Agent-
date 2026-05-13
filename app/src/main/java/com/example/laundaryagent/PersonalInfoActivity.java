package com.example.laundaryagent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PersonalInfoActivity extends AppCompatActivity {

    private static final String PREFS = "LaundryPrefs";
    private static final String KEY_NAME  = "profile_name";
    private static final String KEY_PHONE = "profile_phone";

    private boolean isEditMode = false;

    private TextView tvName, tvPhone, tvEditSaveLabel;
    private EditText etName, etPhone;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_personal_info);

        tvName        = findViewById(R.id.tv_name);
        tvPhone       = findViewById(R.id.tv_phone);
        etName        = findViewById(R.id.et_name);
        etPhone       = findViewById(R.id.et_phone);
        tvEditSaveLabel = findViewById(R.id.tv_edit_save_label);

        // Load saved values
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedName  = prefs.getString(KEY_NAME,  "John Doe");
        String savedPhone = prefs.getString(KEY_PHONE, "+91 99999 99999");

        tvName.setText(savedName);
        tvPhone.setText(savedPhone);
        etName.setText(savedName);
        etPhone.setText(savedPhone);

        // Back
        findViewById(R.id.btn_back).setOnClickListener(v -> {
            if (isEditMode) {
                // Discard changes
                exitEditMode(false);
            } else {
                finish();
            }
        });

        // Edit / Save toggle
        findViewById(R.id.btn_edit_save).setOnClickListener(v -> {
            if (isEditMode) {
                saveChanges();
            } else {
                enterEditMode();
            }
        });
    }

    private void enterEditMode() {
        isEditMode = true;
        tvEditSaveLabel.setText("Save");

        // Show EditTexts, hide TextViews
        tvName.setVisibility(View.GONE);
        etName.setVisibility(View.VISIBLE);
        etName.requestFocus();

        tvPhone.setVisibility(View.GONE);
        etPhone.setVisibility(View.VISIBLE);

        // Show keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.showSoftInput(etName, InputMethodManager.SHOW_IMPLICIT);
    }

    private void saveChanges() {
        String newName  = etName.getText().toString().trim();
        String newPhone = etPhone.getText().toString().trim();

        if (newName.isEmpty()) {
            etName.setError("Name cannot be empty");
            return;
        }
        if (newPhone.isEmpty()) {
            etPhone.setError("Phone cannot be empty");
            return;
        }

        // Persist
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_NAME,  newName)
                .putString(KEY_PHONE, newPhone)
                .apply();

        exitEditMode(true);
        Toast.makeText(this, "Profile updated successfully", Toast.LENGTH_SHORT).show();
    }

    private void exitEditMode(boolean applyValues) {
        isEditMode = false;
        tvEditSaveLabel.setText("Edit");

        if (applyValues) {
            tvName.setText(etName.getText().toString().trim());
            tvPhone.setText(etPhone.getText().toString().trim());
        } else {
            // Restore original
            etName.setText(tvName.getText());
            etPhone.setText(tvPhone.getText());
        }

        tvName.setVisibility(View.VISIBLE);
        etName.setVisibility(View.GONE);
        tvPhone.setVisibility(View.VISIBLE);
        etPhone.setVisibility(View.GONE);

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(etName.getWindowToken(), 0);
    }

    @Override
    public void onBackPressed() {
        if (isEditMode) {
            exitEditMode(false);
        } else {
            super.onBackPressed();
        }
    }
}
