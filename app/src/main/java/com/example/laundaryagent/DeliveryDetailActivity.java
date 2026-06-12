package com.example.laundaryagent;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderServices;
import com.example.laundaryagent.data.model.ServiceItem;
import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.example.laundaryagent.data.repository.LaundryRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;
import java.util.Map;

public class DeliveryDetailActivity extends AppCompatActivity {

    // SMS credentials (same as LoginActivity)
    private static final String SMS_USER     = "Experts";
    private static final String SMS_AUTHKEY  = "ba9dcdcdfcXX";
    private static final String SMS_SENDER   = "EXTSKL";
    private static final String SMS_ACCUSAGE = "1";

    private String orderId;
    private String customerPhone;
    private String generatedOtp;
    private boolean otpSent = false;
    private boolean isFirebaseOrder = false; // true when loaded from Firestore

    // For legacy local orders
    private OrderItem currentOrder;

    // OTP inputs
    private EditText[] otpBoxes = new EditText[6];
    private RequestQueue requestQueue;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_detail);

        orderId = getIntent().getStringExtra("order_id");
        boolean isReadOnly = getIntent().getBooleanExtra("read_only", false);
        requestQueue = Volley.newRequestQueue(this);

        // Try to load from Firebase first; fall back to local repo
        loadOrderData(isReadOnly);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
    }

    private void loadOrderData(boolean isReadOnly) {
        // Check if Firebase order or legacy local order
        if (orderId != null && (orderId.startsWith("D") || orderId.length() < 4)) {
            // Legacy local order
            currentOrder = LaundryRepository.getInstance().getOrderById(orderId);
            if (currentOrder != null) {
                customerPhone = currentOrder.getPhone();
                populateHeaderFromLocal();
                setupActions(isReadOnly, true);
            }
        } else {
            // Firebase order — orderId is the full Firestore path
            isFirebaseOrder = true;
            FirebaseRepository.getInstance().getOrderByPath(orderId,
                err -> runOnUiThread(() ->
                    Toast.makeText(this, "Error: " + err, Toast.LENGTH_SHORT).show()),
                orders -> runOnUiThread(() -> {
                    if (!orders.isEmpty()) {
                        Map<String, Object> data = orders.get(0);
                        customerPhone = FirebaseRepository.str(data, "customerPhone");
                        if (customerPhone.isEmpty()) customerPhone = FirebaseRepository.str(data, "phone");
                        
                        populateHeaderFromFirebase(data);
                        boolean readOnly = FirebaseRepository.STATUS_COMPLETED.equals(
                                FirebaseRepository.str(data, "status"));
                        setupActions(readOnly, false);

                        // Fetch user data for complete info
                        String path = FirebaseRepository.str(data, "__path");
                        if (path.contains("/orders/")) {
                            String userPath = path.substring(0, path.indexOf("/orders/"));
                            FirebaseRepository.getInstance().getDocumentByPath(userPath,
                                e -> Log.e("DeliveryDetail", "User fetch error: " + e),
                                userMap -> runOnUiThread(() -> updateUIWithUserData(userMap))
                            );
                        }
                    }
                })
            );
        }
    }

    private void populateHeaderFromLocal() {
        String name = currentOrder.getCustomerName();
        ((TextView) findViewById(R.id.customer_name)).setText(name);
        ((TextView) findViewById(R.id.society_name)).setText(currentOrder.getSociety());
        ((TextView) findViewById(R.id.order_id_text)).setText("#" + currentOrder.getId());
        if (name != null && !name.isEmpty())
            ((TextView) findViewById(R.id.customer_initial))
                    .setText(String.valueOf(name.charAt(0)).toUpperCase());

        String address  = currentOrder.getAddress();
        String society  = currentOrder.getSociety();
        String[] parts  = address != null ? address.split(",") : new String[]{};
        String flat     = parts.length > 1 ? parts[1].trim() : (address != null ? address : "");
        String building = parts.length > 0 ? parts[0].trim() : "";

        ((TextView) findViewById(R.id.addr_flat)).setText(flat);
        ((TextView) findViewById(R.id.addr_building)).setText(building);
        ((TextView) findViewById(R.id.addr_society)).setText(society);
        ((TextView) findViewById(R.id.addr_area)).setText("Hadapsar, Pune");
        ((TextView) findViewById(R.id.addr_full)).setText(building + ", " + flat + ", " + society);

        String phone = currentOrder.getPhone();
        ((TextView) findViewById(R.id.tv_customer_phone)).setText(
                "+91 " + phone.substring(0, 5) + " " + phone.substring(5));
    }

    private void populateHeaderFromFirebase(Map<String, Object> data) {
        String name    = FirebaseRepository.str(data, "customerName");
        if (name.isEmpty()) name = FirebaseRepository.str(data, "name");
        String address = FirebaseRepository.str(data, "address");
        String society = FirebaseRepository.str(data, "society");
        String phone   = FirebaseRepository.str(data, "customerPhone");
        if (phone.isEmpty()) phone = FirebaseRepository.str(data, "phone");
        String docId   = FirebaseRepository.str(data, "id");
        String path    = FirebaseRepository.str(data, "__path");

        if (name.isEmpty()) {
            name = phone.isEmpty() ? "Unknown Customer" : phone;
        }

        currentOrder = new OrderItem(docId, name, address, society, phone, "", path);
        
        // Parse items array
        Object itemsObj = data.get("items");
        if (itemsObj instanceof List) {
            currentOrder.setServices(FirebaseRepository.getInstance()
                    .parseOrderServices((List<Map<String, Object>>) itemsObj));
        }

        ((TextView) findViewById(R.id.customer_name)).setText(name);
        ((TextView) findViewById(R.id.society_name)).setText(society.isEmpty() ? "Residence" : society);
        
        TextView orderIdView = findViewById(R.id.order_id_text);
        String shortId = docId.length() > 8 ? docId.substring(0, 8) + "..." : docId;
        orderIdView.setText("#" + shortId);
        orderIdView.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Order ID", docId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Order ID copied", Toast.LENGTH_SHORT).show();
        });
        
        if (!name.isEmpty()) {
            ((TextView) findViewById(R.id.customer_initial))
                    .setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        // Try to fetch real name if it's currently a phone number or unknown
        final String searchPhone = phone;
        if (name.equals(searchPhone) || name.equals("Unknown Customer")) {
            FirebaseRepository.getInstance().fetchNameForPhone(searchPhone, realName -> {
                if (!realName.equals("Unknown") && !realName.equals(searchPhone)) {
                    runOnUiThread(() -> {
                        ((TextView) findViewById(R.id.customer_name)).setText(realName);
                        ((TextView) findViewById(R.id.customer_initial))
                                .setText(String.valueOf(realName.charAt(0)).toUpperCase());
                        if (currentOrder != null) currentOrder = currentOrder.copyWithName(realName);
                    });
                }
            });
        }

        ((TextView) findViewById(R.id.addr_full)).setText(address);
        ((TextView) findViewById(R.id.addr_society)).setText(society.isEmpty() ? "Residential Area" : society);
        
        if (phone.length() >= 10)
            ((TextView) findViewById(R.id.tv_customer_phone)).setText(
                    "+91 " + phone.substring(0, 5) + " " + phone.substring(5));
        else
            ((TextView) findViewById(R.id.tv_customer_phone)).setText(phone);
    }

    private void setupActions(boolean isReadOnly, boolean isLocal) {
        // Quick action buttons
        findViewById(R.id.btn_order_details).setOnClickListener(v -> showOrderDetails(isLocal));
        findViewById(R.id.btn_call_customer).setOnClickListener(v -> callCustomer());
        findViewById(R.id.btn_locate_customer).setOnClickListener(v -> locateCustomer());
        if (findViewById(R.id.btn_navigate) != null)
            findViewById(R.id.btn_navigate).setOnClickListener(v -> locateCustomer());

        if (isReadOnly) {
            applyReadOnlyMode();
        } else {
            // OTP flow
            otpBoxes[0] = findViewById(R.id.otp_1);
            otpBoxes[1] = findViewById(R.id.otp_2);
            otpBoxes[2] = findViewById(R.id.otp_3);
            otpBoxes[3] = findViewById(R.id.otp_4);
            otpBoxes[4] = findViewById(R.id.otp_5);
            otpBoxes[5] = findViewById(R.id.otp_6);
            wireOtpBoxes();
            findViewById(R.id.btn_send_otp).setOnClickListener(v -> sendDeliveryOtp());
            findViewById(R.id.btn_complete_delivery).setOnClickListener(v -> verifyAndComplete());
            if (findViewById(R.id.btn_take_photo) != null)
                findViewById(R.id.btn_take_photo).setOnClickListener(v ->
                        Toast.makeText(this, "Delivery photo captured", Toast.LENGTH_SHORT).show());
            if (findViewById(R.id.btn_report_issue) != null)
                findViewById(R.id.btn_report_issue).setOnClickListener(v -> reportIssue());
        }
    }

    // ── OTP ───────────────────────────────────────────────────────────────

    private void wireOtpBoxes() {
        for (int i = 0; i < 6; i++) {
            final int idx = i;
            if (otpBoxes[i] == null) continue;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && idx < 5 && otpBoxes[idx + 1] != null)
                        otpBoxes[idx + 1].requestFocus();
                    if (s.length() == 0 && idx > 0 && otpBoxes[idx - 1] != null)
                        otpBoxes[idx - 1].requestFocus();
                }
            });
        }
    }

    /**
     * Generates a 6-digit OTP, saves it to Firebase (for the order doc),
     * and sends it to the customer via SMS.
     */
    private void sendDeliveryOtp() {
        if (customerPhone == null || customerPhone.isEmpty()) {
            Toast.makeText(this, "Customer phone not available", Toast.LENGTH_SHORT).show();
            return;
        }

        generatedOtp = String.valueOf((int)(Math.random() * 900000) + 100000);

        // If Firebase order, persist the OTP there first
        if (isFirebaseOrder) {
            FirebaseRepository.getInstance().saveDeliveryOtp(orderId, generatedOtp,
                new FirebaseRepository.ActionCallback() {
                    @Override public void onSuccess() {
                        runOnUiThread(() -> dispatchSmsOtp(generatedOtp));
                    }
                    @Override public void onFailure(String error) {
                        runOnUiThread(() -> Toast.makeText(DeliveryDetailActivity.this,
                                "Failed to save OTP: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
        } else {
            // Local order: just send SMS directly
            dispatchSmsOtp(generatedOtp);
        }
    }

    private void dispatchSmsOtp(String otp) {
        otpSent = true;
        showOtpSection();

        String message = "Your Verification Code for login is " + otp +
                ". - Expertskill Technology.";
        String url = "https://mobicomm.dove-sms.com//submitsms.jsp?"
                + "user=" + SMS_USER
                + "&key=" + SMS_AUTHKEY
                + "&mobile=+91" + customerPhone
                + "&message=" + message.replace(" ", "%20")
                + "&accusage=" + SMS_ACCUSAGE
                + "&senderid=" + SMS_SENDER;

        StringRequest req = new StringRequest(Request.Method.GET, url,
                response -> runOnUiThread(() ->
                    Toast.makeText(this, "OTP sent to customer's number", Toast.LENGTH_SHORT).show()),
                error -> runOnUiThread(() ->
                    Toast.makeText(this, "SMS failed — OTP: " + otp, Toast.LENGTH_SHORT).show()));

        requestQueue.add(req);
    }

    private void showOtpSection() {
        View otpSection = findViewById(R.id.otp_input_section);
        if (otpSection != null) {
            otpSection.setVisibility(View.VISIBLE);
            otpSection.animate().alpha(0f).setDuration(0)
                    .withEndAction(() -> otpSection.animate().alpha(1f).setDuration(300).start()).start();
            if (otpBoxes[0] != null) otpBoxes[0].requestFocus();
        }
        // Change Send OTP button text
        View sendBtn = findViewById(R.id.btn_send_otp);
        if (sendBtn instanceof LinearLayout) {
            for (int i = 0; i < ((LinearLayout) sendBtn).getChildCount(); i++) {
                View child = ((LinearLayout) sendBtn).getChildAt(i);
                if (child instanceof TextView) ((TextView) child).setText("Resend");
            }
        }
    }

    private void verifyAndComplete() {
        if (!otpSent) {
            Toast.makeText(this, "Please send OTP to customer first", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder entered = new StringBuilder();
        for (EditText box : otpBoxes) {
            if (box != null) entered.append(box.getText().toString().trim());
        }

        if (entered.length() < 6) {
            Toast.makeText(this, "Please enter the 6-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (isFirebaseOrder) {
            // Verify against Firestore-stored OTP
            FirebaseRepository.getInstance().verifyDeliveryOtp(orderId, entered.toString(),
                new FirebaseRepository.ActionCallback() {
                    @Override public void onSuccess() {
                        runOnUiThread(() -> showSuccessAndFinish());
                    }
                    @Override public void onFailure(String error) {
                        runOnUiThread(() -> {
                            if ("Invalid OTP".equals(error)) {
                                shakeOtpBoxes();
                                Toast.makeText(DeliveryDetailActivity.this,
                                        "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(DeliveryDetailActivity.this,
                                        "Error: " + error, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                });
        } else {
            // Local verification
            if (entered.toString().equals(generatedOtp)) {
                if (currentOrder != null)
                    LaundryRepository.getInstance().markDeliveryComplete(currentOrder.getId());
                showSuccessAndFinish();
            } else {
                shakeOtpBoxes();
                Toast.makeText(this, "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void shakeOtpBoxes() {
        for (EditText box : otpBoxes) {
            if (box == null) continue;
            box.animate().translationX(10f).setDuration(50)
                    .withEndAction(() -> box.animate().translationX(-10f).setDuration(50)
                            .withEndAction(() -> box.animate().translationX(0f).setDuration(50).start())
                            .start())
                    .start();
        }
    }

    private void showSuccessAndFinish() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dv = LayoutInflater.from(this).inflate(R.layout.dialog_delivery_success, null);
        dialog.setContentView(dv);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int)(getResources().getDisplayMetrics().widthPixels * 0.88f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dv.findViewById(R.id.dialog_done_btn).setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        dialog.setCancelable(false);
        dialog.show();
    }

    private void applyReadOnlyMode() {
        View completeBtn = findViewById(R.id.btn_complete_delivery);
        completeBtn.setAlpha(0.45f);
        completeBtn.setClickable(false);
        if (completeBtn instanceof LinearLayout) {
            for (int i = 0; i < ((LinearLayout) completeBtn).getChildCount(); i++) {
                View child = ((LinearLayout) completeBtn).getChildAt(i);
                if (child instanceof TextView) ((TextView) child).setText("Delivery Completed");
            }
        }
        View takePhotoBtn = findViewById(R.id.btn_take_photo);
        if (takePhotoBtn != null) { takePhotoBtn.setAlpha(0.4f); takePhotoBtn.setClickable(false); }
        View reportBtn = findViewById(R.id.btn_report_issue);
        if (reportBtn != null) { reportBtn.setAlpha(0.4f); reportBtn.setClickable(false); }
        View sendOtpBtn = findViewById(R.id.btn_send_otp);
        if (sendOtpBtn != null) { sendOtpBtn.setAlpha(0.4f); sendOtpBtn.setClickable(false); }
    }

    // ── Order Details ─────────────────────────────────────────────────────

    private void showOrderDetails(boolean isLocal) {
        if (currentOrder == null) return;
        
        OrderServices services = currentOrder.getServices();
        if (services == null) {
            // Fallback to repository
            services = LaundryRepository.getInstance().getServicesForOrder(currentOrder.getId());
        }

        if (services == null) {
            Toast.makeText(this, "No order details available", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_order_details, null);
        sheet.setContentView(v);

        TextView idView = v.findViewById(R.id.dialog_order_id);
        String fullId = currentOrder.getId();
        String shortId = fullId.length() > 8 ? fullId.substring(0, 8) + "..." : fullId;
        idView.setText("#" + shortId + " · " + currentOrder.getCustomerName());
        
        idView.setOnClickListener(v1 -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Order ID", fullId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Order ID copied to clipboard", Toast.LENGTH_SHORT).show();
        });

        ((TextView) v.findViewById(R.id.dialog_total_items))
                .setText(String.valueOf(services.totalItems()));

        populateServiceCard(v, R.id.laundry_items_container, R.id.laundry_count,
                services.getLaundry(), "#4361EE");
        populateServiceCard(v, R.id.ironing_items_container, R.id.ironing_count,
                services.getIroning(), "#7209B7");
        populateServiceCard(v, R.id.dryclean_items_container, R.id.dryclean_count,
                services.getDryCleaning(), "#06D6A0");
        populateServiceCard(v, R.id.shoecare_items_container, R.id.shoecare_count,
                services.getShoeCare(), "#FF9F1C");

        sheet.show();
    }

    private void populateServiceCard(View root, int containerId, int countId, List<ServiceItem> items, String color) {
        LinearLayout container = root.findViewById(containerId);
        TextView countView = root.findViewById(countId);
        int total = 0;
        for (ServiceItem s : items) total += s.getQuantity();
        if (total == 0) { ((View) container.getParent()).setVisibility(View.GONE); return; }
        countView.setText(total + " item" + (total > 1 ? "s" : ""));
        container.removeAllViews();
        float dp = getResources().getDisplayMetrics().density;
        for (ServiceItem item : items) {
            if (item.getQuantity() == 0) continue;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int)(6 * dp);
            row.setLayoutParams(lp);
            row.setGravity(Gravity.CENTER_VERTICAL);
            View dot = new View(this);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams((int)(8*dp),(int)(8*dp));
            dotLp.rightMargin = (int)(10*dp);
            dot.setLayoutParams(dotLp);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(color));
            dot.setBackground(gd);
            row.addView(dot);
            TextView name = new TextView(this);
            name.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setText(item.getName());
            name.setTextColor(Color.parseColor("#475569"));
            name.setTextSize(13f);
            row.addView(name);
            TextView qty = new TextView(this);
            qty.setText("× " + item.getQuantity());
            qty.setTextColor(Color.parseColor(color));
            qty.setTextSize(13f);
            qty.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(qty);
            container.addView(row);
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────

    private void callCustomer() {
        String phone = customerPhone != null ? customerPhone :
                (currentOrder != null ? currentOrder.getPhone() : "");
        if (!phone.isEmpty())
            startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
    }

    private void locateCustomer() {
        String society = currentOrder != null ? currentOrder.getSociety() : "";
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(society));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null)
            startActivity(mapIntent);
        else
            Toast.makeText(this, "Maps app not found", Toast.LENGTH_SHORT).show();
    }

    private void reportIssue() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View v = getLayoutInflater().inflate(R.layout.dialog_report_issue, null);
        dialog.setContentView(v);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.9f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        android.widget.RadioGroup group = v.findViewById(R.id.issue_radio_group);
        android.widget.EditText etCustomIssue = v.findViewById(R.id.et_custom_issue);
        View btnReschedule = v.findViewById(R.id.btn_reschedule);
        View btnCancel = v.findViewById(R.id.btn_cancel_order);

        group.setOnCheckedChangeListener((g, checkedId) -> {
            btnReschedule.setAlpha(1.0f);
            btnReschedule.setClickable(true);
            btnCancel.setAlpha(1.0f);
            btnCancel.setClickable(true);
            
            if (checkedId == R.id.radio_other_issue) {
                etCustomIssue.setVisibility(View.VISIBLE);
                etCustomIssue.requestFocus();
            } else {
                etCustomIssue.setVisibility(View.GONE);
            }
        });

        btnReschedule.setOnClickListener(view -> {
            int selectedId = group.getCheckedRadioButtonId();
            if (selectedId == -1) return;

            String reason;
            if (selectedId == R.id.radio_other_issue) {
                reason = etCustomIssue.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                android.widget.RadioButton selected = v.findViewById(selectedId);
                reason = selected != null ? selected.getText().toString() : "Unknown Issue";
            }

            FirebaseRepository.getInstance().rescheduleOrder(orderId, reason, new FirebaseRepository.ActionCallback() {
                @Override public void onSuccess() {
                    Toast.makeText(DeliveryDetailActivity.this, "Delivery rescheduled: " + reason, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    finish();
                }
                @Override public void onFailure(String error) {
                    Toast.makeText(DeliveryDetailActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        btnCancel.setOnClickListener(view -> {
            int selectedId = group.getCheckedRadioButtonId();
            if (selectedId == -1) return;

            String reason;
            if (selectedId == R.id.radio_other_issue) {
                reason = etCustomIssue.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "Please describe the issue", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                android.widget.RadioButton selected = v.findViewById(selectedId);
                reason = selected != null ? selected.getText().toString() : "Unknown Issue";
            }

            FirebaseRepository.getInstance().markOrderIncomplete(orderId, reason, new FirebaseRepository.ActionCallback() {
                @Override public void onSuccess() {
                    Toast.makeText(DeliveryDetailActivity.this, "Order cancelled: " + reason, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    finish();
                }
                @Override public void onFailure(String error) {
                    Toast.makeText(DeliveryDetailActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    private void updateUIWithUserData(Map<String, Object> userMap) {
        if (userMap == null) return;
        String name = FirebaseRepository.str(userMap, "name");
        String address = FirebaseRepository.str(userMap, "address");
        String society = FirebaseRepository.str(userMap, "society");
        if (society.isEmpty() && !address.isEmpty()) {
            String[] parts = address.split(",");
            society = parts.length > 2 ? parts[2].trim() : (parts.length > 0 ? parts[0].trim() : "Residence");
        }

        if (!name.isEmpty()) {
            ((TextView) findViewById(R.id.customer_name)).setText(name);
            ((TextView) findViewById(R.id.customer_initial))
                    .setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
        if (!society.isEmpty()) {
            ((TextView) findViewById(R.id.society_name)).setText(society);
            ((TextView) findViewById(R.id.addr_society)).setText(society);
        }
        if (!address.isEmpty()) {
            ((TextView) findViewById(R.id.addr_full)).setText(address);
        }
    }
}

