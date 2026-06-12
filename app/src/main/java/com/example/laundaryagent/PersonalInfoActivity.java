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
    private TextView tvRole, tvZone, tvJoined;
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
        
        tvRole   = findViewById(R.id.tv_role);
        tvZone   = findViewById(R.id.tv_zone);
        tvJoined = findViewById(R.id.tv_joined_on);

        // Load saved values from preferences as fallback
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String savedName  = prefs.getString(KEY_NAME,  prefs.getString("user_identity", "Agent"));
        String savedPhone = prefs.getString(KEY_PHONE, prefs.getString("agent_phone", ""));

        tvName.setText(savedName);
        etName.setText(savedName);
        tvPhone.setText(savedPhone);
        etPhone.setText(savedPhone);
        
        String savedRole   = prefs.getString("user_role_display", prefs.getString("user_role", "Delivery Agent"));
        String savedZone   = prefs.getString("user_zone_display", "No assigned societies");
        String savedJoined = prefs.getString("user_joined_display", "N/A");

        tvRole.setText(savedRole);
        tvZone.setText(savedZone);
        tvJoined.setText(savedJoined);
        
        String agentPhone = prefs.getString("agent_phone", "");
        if (agentPhone.isEmpty()) agentPhone = prefs.getString("user_identity", "");
        
        if (!agentPhone.isEmpty()) {
            com.example.laundaryagent.data.repository.FirebaseRepository.getInstance()
                .getAgentByPhone(agentPhone,
                    err -> { /* handle err */ },
                    data -> {
                        runOnUiThread(() -> {
                            String realName = com.example.laundaryagent.data.repository.FirebaseRepository.str(data, "name");
                            if (realName.isEmpty()) realName = com.example.laundaryagent.data.repository.FirebaseRepository.str(data, "agentName");
                            if (!realName.isEmpty() && !isEditMode) {
                                tvName.setText(realName);
                                etName.setText(realName);
                            }
                            
                            String dbPhone = com.example.laundaryagent.data.repository.FirebaseRepository.str(data, "phone");
                            if (!dbPhone.isEmpty() && !isEditMode) {
                                tvPhone.setText(dbPhone);
                                etPhone.setText(dbPhone);
                            }
                            
                            String role = com.example.laundaryagent.data.repository.FirebaseRepository.str(data, "role");
                            String finalRole = role.isEmpty() ? savedRole : role;
                            tvRole.setText(finalRole);
                            
                            // Format createdAt to show only date
                            String finalJoined = savedJoined;
                            Object createdAtObj = data.get("createdAt");
                            if (createdAtObj instanceof com.google.firebase.Timestamp) {
                                java.util.Date d = ((com.google.firebase.Timestamp) createdAtObj).toDate();
                                finalJoined = new java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(d);
                            } else if (createdAtObj instanceof String) {
                                String dateStr = (String) createdAtObj;
                                if (dateStr.contains(",")) {
                                    finalJoined = dateStr.substring(0, dateStr.indexOf(",")).trim();
                                } else if (dateStr.contains(" ")) {
                                    String[] parts = dateStr.split(" ");
                                    if (parts.length >= 3) {
                                        finalJoined = parts[0] + " " + parts[1] + " " + parts[2];
                                    } else {
                                        finalJoined = dateStr;
                                    }
                                } else {
                                    finalJoined = dateStr;
                                }
                            } else if (createdAtObj instanceof Long) {
                                java.util.Date d = new java.util.Date((Long) createdAtObj);
                                finalJoined = new java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.getDefault()).format(d);
                            } else if (createdAtObj != null) {
                                finalJoined = String.valueOf(createdAtObj);
                            }
                            tvJoined.setText(finalJoined);
                            
                            String finalZone = "No assigned societies";
                            Object locObj = data.get("locations");
                            if (locObj instanceof java.util.List) {
                                java.util.List<String> locs = new java.util.ArrayList<>();
                                for (Object o : (java.util.List<?>) locObj) {
                                    String locStr = String.valueOf(o);
                                    if (locStr.contains("::")) {
                                        String[] parts = locStr.split("::");
                                        if (parts.length > 1) locs.add(parts[1].trim());
                                        else locs.add(locStr.trim());
                                    } else {
                                        locs.add(locStr.trim());
                                    }
                                }
                                if (!locs.isEmpty()) {
                                    finalZone = android.text.TextUtils.join(" · ", locs);
                                }
                            }
                            tvZone.setText(finalZone);
                            
                            // Save fetched values to SharedPreferences to prevent blinking on next load
                            prefs.edit()
                                .putString(KEY_NAME, tvName.getText().toString())
                                .putString(KEY_PHONE, tvPhone.getText().toString())
                                .putString("user_role_display", finalRole)
                                .putString("user_zone_display", finalZone)
                                .putString("user_joined_display", finalJoined)
                                .apply();
                        });
                    });
        }

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
