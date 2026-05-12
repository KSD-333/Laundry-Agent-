package com.example.laundaryagent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText mobileInput, otpInput, franchiseEmail, franchisePassword;
    private TextInputLayout otpLayout;
    private MaterialButton actionButton, franchiseLoginButton;
    private View agentContainer, franchiseContainer;
    private View logoCard;
    private TextView titleText;

    private boolean isOtpSent = false;
    private String serverOtp = "";
    private RequestQueue requestQueue;

    // SMS API credentials (as provided in user example)
    private final String username = "Experts";
    private final String authkey = "ba9dcdcdfcXX";
    private final String senderId = "EXTSKL";
    private final String accusage = "1";

    // Bypass numbers list
    private final List<String> BYPASS_NUMBERS = Arrays.asList(
            "9999999999",
            "8888888888",
            "7777777777",
            "0000000000",
            "9898989898",
            "1111111111",
            "2222222222",
            "3333333333",
            "4444444444",
            "5555555555",
            "6666666666"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        setupListeners();
        checkLoginStatus();
    }

    private void initializeViews() {
        mobileInput = findViewById(R.id.mobile_input);
        otpInput = findViewById(R.id.otp_input);
        otpLayout = findViewById(R.id.otp_layout);
        actionButton = findViewById(R.id.btn_action);
        
        franchiseEmail = findViewById(R.id.franchise_email);
        franchisePassword = findViewById(R.id.franchise_password);
        franchiseLoginButton = findViewById(R.id.btn_franchise_login);
        
        agentContainer = findViewById(R.id.agent_login_container);
        franchiseContainer = findViewById(R.id.franchise_login_container);
        logoCard = findViewById(R.id.logo_card);
        titleText = findViewById(R.id.tv_login_title);

        requestQueue = Volley.newRequestQueue(this);
    }

    private void setupListeners() {
        actionButton.setOnClickListener(v -> {
            if (!isOtpSent) {
                handleSendOtp();
            } else {
                handleVerifyOtp();
            }
        });

        franchiseLoginButton.setOnClickListener(v -> handleFranchiseLogin());

        logoCard.setOnLongClickListener(v -> {
            toggleFranchiseLogin();
            return true;
        });
    }

    private void handleSendOtp() {
        String mobile = mobileInput.getText().toString().trim();
        if (mobile.length() != 10) {
            mobileInput.setError("Enter valid 10-digit number");
            return;
        }

        // Check for Bypass numbers
        if (BYPASS_NUMBERS.contains(mobile)) {
            Toast.makeText(this, "Admin Access: Bypassing OTP", Toast.LENGTH_SHORT).show();
            saveLoginAndNavigate(mobile, "Agent (Admin)");
            return;
        }

        // Generate 6-digit OTP
        serverOtp = String.valueOf((int) (Math.random() * 900000) + 100000);
        String message = "Your Verification Code for login is " + serverOtp + ". - Expertskill Technology.";
        String url = "https://mobicomm.dove-sms.com//submitsms.jsp?" +
                "user=" + username +
                "&key=" + authkey +
                "&mobile=+91" + mobile +
                "&message=" + message.replace(" ", "%20") +
                "&accusage=" + accusage +
                "&senderid=" + senderId;

        actionButton.setEnabled(false);
        actionButton.setText("Sending...");

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    actionButton.setEnabled(true);
                    if (response.contains("success")) {
                        isOtpSent = true;
                        otpLayout.setVisibility(View.VISIBLE);
                        actionButton.setText("Verify & Login");
                        Toast.makeText(this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                        Log.d("Login", "Sent OTP: " + serverOtp);
                    } else {
                        Toast.makeText(this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    actionButton.setEnabled(true);
                    actionButton.setText("Get OTP");
                    Toast.makeText(this, "Network error. Check connection.", Toast.LENGTH_SHORT).show();
                });

        requestQueue.add(request);
    }

    private void handleVerifyOtp() {
        String inputOtp = otpInput.getText().toString().trim();
        if (inputOtp.equals(serverOtp)) {
            saveLoginAndNavigate(mobileInput.getText().toString(), "Agent");
        } else {
            otpInput.setError("Invalid OTP");
        }
    }

    private void handleFranchiseLogin() {
        String email = franchiseEmail.getText().toString().trim();
        String password = franchisePassword.getText().toString().trim();

        if (!email.isEmpty() && !password.isEmpty()) {
            // Allowing any non-empty credentials for franchise as requested
            saveLoginAndNavigate(email, "Franchise Admin");
        } else {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleFranchiseLogin() {
        if (agentContainer.getVisibility() == View.VISIBLE) {
            agentContainer.setVisibility(View.GONE);
            franchiseContainer.setVisibility(View.VISIBLE);
            titleText.setText("Franchise Portal");
            Toast.makeText(this, "Admin Mode Activated", Toast.LENGTH_SHORT).show();
        } else {
            agentContainer.setVisibility(View.VISIBLE);
            franchiseContainer.setVisibility(View.GONE);
            titleText.setText("Laundry Agent Login");
        }
    }

    private void saveLoginAndNavigate(String identity, String role) {
        SharedPreferences prefs = getSharedPreferences("LaundryPrefs", MODE_PRIVATE);
        prefs.edit()
                .putBoolean("isLoggedIn", true)
                .putString("user_identity", identity)
                .putString("user_role", role)
                .apply();

        Toast.makeText(this, "Access Granted: " + role, Toast.LENGTH_SHORT).show();
        
        if (role.equals("Franchise Admin")) {
            startActivity(new Intent(this, AdminDashboardActivity.class));
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }

    private void checkLoginStatus() {
        SharedPreferences prefs = getSharedPreferences("LaundryPrefs", MODE_PRIVATE);
        if (prefs.getBoolean("isLoggedIn", false)) {
            String role = prefs.getString("user_role", "Agent");
            if (role.equals("Franchise Admin")) {
                startActivity(new Intent(this, AdminDashboardActivity.class));
            } else {
                startActivity(new Intent(this, MainActivity.class));
            }
            finish();
        }
    }
}
