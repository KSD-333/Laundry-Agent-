package com.example.laundaryagent;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderServices;
import com.example.laundaryagent.data.model.ServiceItem;
import com.example.laundaryagent.data.repository.LaundryRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.List;

public class DeliveryDetailActivity extends AppCompatActivity {

    private static final int MAX_PHOTOS = 5;
    private static final int REQ_BASE   = 100;
    private static final String DEMO_OTP = "1234";

    private OrderItem currentOrder;
    private boolean otpSent = false;

    // Delivery photos (taken at delivery time)
    private final boolean[] photoTaken = new boolean[MAX_PHOTOS];
    private final ImageView[] thumbs   = new ImageView[MAX_PHOTOS];
    private final View[]      badges   = new View[MAX_PHOTOS];
    private final ImageView[] checks   = new ImageView[MAX_PHOTOS];

    // OTP inputs
    private EditText[] otpBoxes = new EditText[4];

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_delivery_detail);

        String orderId = getIntent().getStringExtra("order_id");
        currentOrder = LaundryRepository.getInstance().getOrderById(orderId);

        // Back
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Read-only mode for completed orders
        boolean isReadOnly = getIntent().getBooleanExtra("read_only", false);

        // Populate header
        if (currentOrder != null) {
            String name = currentOrder.getCustomerName();
            ((TextView) findViewById(R.id.customer_name)).setText(name);
            ((TextView) findViewById(R.id.society_name)).setText(currentOrder.getSociety());
            ((TextView) findViewById(R.id.order_id_text)).setText("#" + currentOrder.getId());

            if (name != null && !name.isEmpty()) {
                ((TextView) findViewById(R.id.customer_initial))
                        .setText(String.valueOf(name.charAt(0)).toUpperCase());
            }

            // Address
            String address  = currentOrder.getAddress();
            String society  = currentOrder.getSociety();
            String[] parts  = address != null ? address.split(",") : new String[]{};
            String flat     = parts.length > 1 ? parts[1].trim() : (address != null ? address : "");
            String building = parts.length > 0 ? parts[0].trim() : "";
            String area     = "Hadapsar, Pune";

            ((TextView) findViewById(R.id.addr_flat)).setText(flat);
            ((TextView) findViewById(R.id.addr_building)).setText(building);
            ((TextView) findViewById(R.id.addr_society)).setText(society);
            ((TextView) findViewById(R.id.addr_area)).setText(area);
            ((TextView) findViewById(R.id.addr_full)).setText(
                    building + ", " + flat + ", " + society + ", " + area);

            // Phone
            String phone = currentOrder.getPhone();
            String formatted = "+91 " + phone.substring(0, 5) + " " + phone.substring(5);
            ((TextView) findViewById(R.id.tv_customer_phone)).setText(formatted);
        }

        // Pickup photo slots (read-only display — in real app these come from saved bitmaps)
        // For now they show as empty slots with labels
        // (In production you'd pass bitmap references via intent or a shared store)

        // Order details button
        findViewById(R.id.btn_order_details).setOnClickListener(v -> showOrderDetails());

        // Quick actions
        findViewById(R.id.btn_call_customer).setOnClickListener(v -> callCustomer());
        findViewById(R.id.btn_locate_customer).setOnClickListener(v -> locateCustomer());
        findViewById(R.id.btn_navigate).setOnClickListener(v -> locateCustomer());

        if (isReadOnly) {
            applyReadOnlyMode();
        } else {
            // OTP flow
            otpBoxes[0] = findViewById(R.id.otp_1);
            otpBoxes[1] = findViewById(R.id.otp_2);
            otpBoxes[2] = findViewById(R.id.otp_3);
            otpBoxes[3] = findViewById(R.id.otp_4);
            wireOtpBoxes();
            findViewById(R.id.btn_send_otp).setOnClickListener(v -> sendOtp());

            // Bottom buttons
            findViewById(R.id.btn_complete_delivery).setOnClickListener(v -> verifyAndComplete());
            findViewById(R.id.btn_take_photo).setOnClickListener(v ->
                    Toast.makeText(this, "Delivery photo captured", Toast.LENGTH_SHORT).show());
            findViewById(R.id.btn_report_issue).setOnClickListener(v -> reportIssue());
        }
    }

    private void applyReadOnlyMode() {
        // Disable Verify & Complete button
        View completeBtn = findViewById(R.id.btn_complete_delivery);
        completeBtn.setAlpha(0.45f);
        completeBtn.setClickable(false);
        completeBtn.setFocusable(false);
        if (completeBtn instanceof android.widget.LinearLayout) {
            for (int i = 0; i < ((android.widget.LinearLayout) completeBtn).getChildCount(); i++) {
                android.view.View child = ((android.widget.LinearLayout) completeBtn).getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText("Delivery Completed");
                }
            }
        }

        // Disable Take Photo
        View takePhotoBtn = findViewById(R.id.btn_take_photo);
        takePhotoBtn.setAlpha(0.4f);
        takePhotoBtn.setClickable(false);
        takePhotoBtn.setFocusable(false);

        // Disable Report Issue
        View reportBtn = findViewById(R.id.btn_report_issue);
        reportBtn.setAlpha(0.4f);
        reportBtn.setClickable(false);
        reportBtn.setFocusable(false);

        // Disable Send OTP button
        View sendOtpBtn = findViewById(R.id.btn_send_otp);
        sendOtpBtn.setAlpha(0.4f);
        sendOtpBtn.setClickable(false);
        sendOtpBtn.setFocusable(false);
    }

    // ── OTP ───────────────────────────────────────────────────────────────

    private void wireOtpBoxes() {
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            otpBoxes[i].addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
                @Override public void afterTextChanged(Editable s) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 1 && idx < 3) {
                        otpBoxes[idx + 1].requestFocus();
                    }
                    if (s.length() == 0 && idx > 0) {
                        otpBoxes[idx - 1].requestFocus();
                    }
                }
            });
        }
    }

    private void sendOtp() {
        otpSent = true;
        // Show OTP input section
        View otpSection = findViewById(R.id.otp_input_section);
        otpSection.setVisibility(View.VISIBLE);
        otpSection.animate().alpha(0f).setDuration(0).withEndAction(() ->
                otpSection.animate().alpha(1f).setDuration(300).start()).start();

        otpBoxes[0].requestFocus();

        // Change button text
        ((TextView) ((LinearLayout) findViewById(R.id.btn_send_otp))
                .getChildAt(1)).setText("Resend");

        Toast.makeText(this, "OTP sent to customer's number", Toast.LENGTH_SHORT).show();
    }

    private void verifyAndComplete() {
        if (!otpSent) {
            Toast.makeText(this, "Please send OTP to customer first", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder entered = new StringBuilder();
        for (EditText box : otpBoxes) entered.append(box.getText().toString().trim());

        if (entered.length() < 4) {
            Toast.makeText(this, "Please enter the 4-digit OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (entered.toString().equals(DEMO_OTP)) {
            showSuccessAndFinish();
        } else {
            // Shake animation on OTP boxes
            for (EditText box : otpBoxes) {
                box.setBackgroundResource(R.drawable.bg_otp_input);
                box.animate().translationX(10f).setDuration(50)
                        .withEndAction(() -> box.animate().translationX(-10f).setDuration(50)
                                .withEndAction(() -> box.animate().translationX(0f).setDuration(50).start())
                                .start())
                        .start();
            }
            Toast.makeText(this, "Incorrect OTP. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    private void showSuccessAndFinish() {
        if (currentOrder != null) {
            LaundryRepository.getInstance().markDeliveryComplete(currentOrder.getId());
        }

        // Custom success dialog
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

    // ── Order Details ─────────────────────────────────────────────────────

    private void showOrderDetails() {
        if (currentOrder == null) return;
        OrderServices services = LaundryRepository.getInstance()
                .getServicesForOrder(currentOrder.getId());
        if (services == null) {
            Toast.makeText(this, "No order details available", Toast.LENGTH_SHORT).show();
            return;
        }

        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View v = LayoutInflater.from(this).inflate(R.layout.dialog_order_details, null);
        sheet.setContentView(v);

        ((TextView) v.findViewById(R.id.dialog_order_id))
                .setText("#" + currentOrder.getId() + " · " + currentOrder.getCustomerName());
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

    private void populateServiceCard(View root, int containerId, int countId,
                                     List<ServiceItem> items, String color) {
        LinearLayout container = root.findViewById(containerId);
        TextView countView     = root.findViewById(countId);

        int total = 0;
        for (ServiceItem s : items) total += s.getQuantity();

        if (total == 0) {
            ((View) container.getParent()).setVisibility(View.GONE);
            return;
        }

        countView.setText(total + " item" + (total > 1 ? "s" : ""));
        container.removeAllViews();

        float dp = getResources().getDisplayMetrics().density;

        for (ServiceItem item : items) {
            if (item.getQuantity() == 0) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int)(6 * dp);
            row.setLayoutParams(lp);
            row.setGravity(Gravity.CENTER_VERTICAL);

            // Colored dot
            View dot = new View(this);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                    (int)(8 * dp), (int)(8 * dp));
            dotLp.rightMargin = (int)(10 * dp);
            dot.setLayoutParams(dotLp);
            android.graphics.drawable.GradientDrawable gd =
                    new android.graphics.drawable.GradientDrawable();
            gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            gd.setColor(Color.parseColor(color));
            dot.setBackground(gd);
            row.addView(dot);

            // Name
            TextView name = new TextView(this);
            name.setLayoutParams(new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            name.setText(item.getName());
            name.setTextColor(Color.parseColor("#475569"));
            name.setTextSize(13f);
            row.addView(name);

            // Qty
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
        String phone = currentOrder != null ? currentOrder.getPhone() : "1234567890";
        startActivity(new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phone)));
    }

    private void locateCustomer() {
        if (currentOrder == null) return;
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(currentOrder.getSociety()));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(this, "Maps app not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void reportIssue() {
        String[] issues = {
            "Payment Not Received", "Customer Not Available",
            "Incomplete Order Delivery", "Wrong Item Delivered",
            "Customer Refused Delivery", "Address Not Found", "Other Issue"
        };
        new AlertDialog.Builder(this)
                .setTitle("Report Delivery Issue")
                .setItems(issues, (d, w) ->
                        Toast.makeText(this, "Issue reported: " + issues[w],
                                Toast.LENGTH_SHORT).show())
                .show();
    }
}
