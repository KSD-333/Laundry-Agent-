package com.example.laundaryagent;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private TextInputEditText mobileInput, otpInput, franchiseEmail, franchisePassword;
    private TextInputLayout   otpLayout;
    private View              actionButton, franchiseLoginButton;
    private View              agentContainer, franchiseContainer;
    private View              logoCard, loginCard;
    private TextView          titleText, btnActionText;
    private View              resendOtpRow;
    private TextView          btnResendOtp;
    private CountDownTimer    resendTimer;

    private boolean isOtpSent = false;
    private String  serverOtp = "";
    private RequestQueue requestQueue;

    private final String username = "Experts";
    private final String authkey  = "ba9dcdcdfcXX";
    private final String senderId = "EXTSKL";
    private final String accusage = "1";

    private final List<String> BYPASS_NUMBERS = Arrays.asList(
            "9999999999","8888888888","7777777777","0000000000",
            "9898989898","1111111111","2222222222","3333333333",
            "4444444444","5555555555","6666666666"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        initializeViews();
        animateEntrance();
        setupListeners();
        checkLoginStatus();
    }

    // ── Views ─────────────────────────────────────────────────────────────

    private void initializeViews() {
        mobileInput          = findViewById(R.id.mobile_input);
        otpInput             = findViewById(R.id.otp_input);
        otpLayout            = findViewById(R.id.otp_layout);
        actionButton         = findViewById(R.id.btn_action);
        btnActionText        = actionButton.findViewById(R.id.btn_action_text);

        franchiseEmail       = findViewById(R.id.franchise_email);
        franchisePassword    = findViewById(R.id.franchise_password);
        franchiseLoginButton = findViewById(R.id.btn_franchise_login);

        agentContainer       = findViewById(R.id.agent_login_container);
        franchiseContainer   = findViewById(R.id.franchise_login_container);
        logoCard             = findViewById(R.id.logo_card);
        loginCard            = findViewById(R.id.login_card);
        titleText            = findViewById(R.id.tv_login_title);

        resendOtpRow         = findViewById(R.id.resend_otp_row);
        btnResendOtp         = findViewById(R.id.btn_resend_otp);

        requestQueue = Volley.newRequestQueue(this);
    }

    // ── Entrance animation ────────────────────────────────────────────────

    private void animateEntrance() {
        // Logo bounces in from scale 0
        logoCard.setScaleX(0f);
        logoCard.setScaleY(0f);
        logoCard.setAlpha(0f);
        logoCard.animate()
                .scaleX(1f).scaleY(1f).alpha(1f)
                .setDuration(600)
                .setInterpolator(new OvershootInterpolator(1.2f))
                .start();

        // White card slides up
        loginCard.setTranslationY(60f);
        loginCard.setAlpha(0f);
        loginCard.animate()
                .translationY(0f).alpha(1f)
                .setDuration(500)
                .setStartDelay(250)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    // ── Listeners ─────────────────────────────────────────────────────────

    private void setupListeners() {
        // OTP / Verify button
        actionButton.setOnClickListener(v -> {
            animatePress(v);
            if (!isOtpSent) handleSendOtp();
            else            handleVerifyOtp();
        });

        // Franchise login button
        franchiseLoginButton.setOnClickListener(v -> {
            animatePress(v);
            handleFranchiseLogin();
        });

        // Long-press logo → toggle franchise mode (hidden from agents)
        logoCard.setOnLongClickListener(v -> {
            toggleFranchiseLogin();
            return true;
        });

        // Resend OTP (active only after 30s countdown)
        btnResendOtp.setOnClickListener(v -> {
            if (btnResendOtp.isClickable()) {
                isOtpSent = false;
                otpInput.setText("");
                handleSendOtp();
            }
        });
    }

    private void animatePress(View v) {
        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                .withEndAction(() ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                .start();
    }

    // ── Toggle franchise (long-press only) ────────────────────────────────

    private void toggleFranchiseLogin() {
        if (agentContainer.getVisibility() == View.VISIBLE) {
            // Switch to franchise
            agentContainer.setVisibility(View.GONE);
            franchiseContainer.setVisibility(View.VISIBLE);
            franchiseContainer.setAlpha(0f);
            franchiseContainer.setTranslationY(20f);
            franchiseContainer.animate().alpha(1f).translationY(0f)
                    .setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
            if (titleText != null) titleText.setText("Franchise Portal");
            Toast.makeText(this, "Admin Mode Activated", Toast.LENGTH_SHORT).show();
        } else {
            // Switch back to agent
            franchiseContainer.setVisibility(View.GONE);
            agentContainer.setVisibility(View.VISIBLE);
            agentContainer.setAlpha(0f);
            agentContainer.setTranslationY(20f);
            agentContainer.animate().alpha(1f).translationY(0f)
                    .setDuration(300).setInterpolator(new DecelerateInterpolator()).start();
            if (titleText != null) titleText.setText("Welcome back");
        }
    }

    // ── Login logic (unchanged) ───────────────────────────────────────────

    private void handleSendOtp() {
        String mobile = mobileInput.getText().toString().trim();
        if (mobile.length() != 10) {
            mobileInput.setError("Enter valid 10-digit number");
            return;
        }

        if (BYPASS_NUMBERS.contains(mobile)) {
            Toast.makeText(this, "Admin Access: Bypassing OTP", Toast.LENGTH_SHORT).show();
            saveLoginAndNavigate(mobile, "Agent (Admin)");
            return;
        }

        serverOtp = String.valueOf((int)(Math.random() * 900000) + 100000);
        String message = "Your Verification Code for login is " + serverOtp
                + ". - Expertskill Technology.";
        String url = "https://mobicomm.dove-sms.com//submitsms.jsp?"
                + "user=" + username
                + "&key=" + authkey
                + "&mobile=+91" + mobile
                + "&message=" + message.replace(" ", "%20")
                + "&accusage=" + accusage
                + "&senderid=" + senderId;

        actionButton.setEnabled(false);
        if (btnActionText != null) btnActionText.setText("Sending...");

        StringRequest request = new StringRequest(Request.Method.GET, url,
                response -> {
                    actionButton.setEnabled(true);
                    if (response.contains("success")) {
                        isOtpSent = true;
                        // Reveal OTP field with animation
                        otpLayout.setVisibility(View.VISIBLE);
                        otpLayout.setAlpha(0f);
                        otpLayout.setTranslationY(10f);
                        otpLayout.animate().alpha(1f).translationY(0f)
                                .setDuration(300).start();
                        if (btnActionText != null) btnActionText.setText("Verify & Login");
                        // Show resend row and start countdown
                        resendOtpRow.setVisibility(View.VISIBLE);
                        resendOtpRow.setAlpha(0f);
                        resendOtpRow.animate().alpha(1f).setDuration(300).start();
                        startResendCountdown();
                        Toast.makeText(this, "OTP sent successfully", Toast.LENGTH_SHORT).show();
                        Log.d("Login", "Sent OTP: " + serverOtp);
                    } else {
                        if (btnActionText != null) btnActionText.setText("Get OTP");
                        Toast.makeText(this, "Failed to send OTP", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    actionButton.setEnabled(true);
                    if (btnActionText != null) btnActionText.setText("Get OTP");
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
            // Shake the OTP field
            ObjectAnimator shake = ObjectAnimator.ofFloat(
                    otpLayout, "translationX", 0f, 14f, -14f, 10f, -10f, 0f);
            shake.setDuration(400);
            shake.start();
        }
    }

    private void handleFranchiseLogin() {
        String email    = franchiseEmail.getText().toString().trim();
        String password = franchisePassword.getText().toString().trim();

        if (!email.isEmpty() && !password.isEmpty()) {
            saveLoginAndNavigate(email, "Franchise Admin");
        } else {
            Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show();
        }
    }

    private void startResendCountdown() {
        if (resendTimer != null) resendTimer.cancel();

        btnResendOtp.setClickable(false);
        btnResendOtp.setAlpha(0.5f);

        resendTimer = new CountDownTimer(30000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                btnResendOtp.setText("Resend in " + (millisUntilFinished / 1000) + "s");
            }

            @Override
            public void onFinish() {
                btnResendOtp.setText("Resend OTP");
                btnResendOtp.setClickable(true);
                btnResendOtp.setAlpha(1f);
            }
        }.start();
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

    @Override
    protected void onDestroy() {
        if (resendTimer != null) resendTimer.cancel();
        super.onDestroy();
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
