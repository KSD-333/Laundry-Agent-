package com.example.laundaryagent;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
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
import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.Map;

public class PickupDetailActivity extends AppCompatActivity {

    private static final int MAX_PHOTOS = 5;

    // Request codes for each slot
    private static final int REQ_BASE = 100; // REQ_BASE + slot = request code

    private OrderItem currentOrder;
    private int activeSlot = 0;

    private final boolean[] photoTaken = new boolean[MAX_PHOTOS];
    private final ImageView[] thumbs   = new ImageView[MAX_PHOTOS];
    private final View[]      badges   = new View[MAX_PHOTOS];
    private final ImageView[] checks   = new ImageView[MAX_PHOTOS];
    private TextView photoCountText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup_detail);

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

            // Address breakdown
            String address = currentOrder.getAddress();
            String society = currentOrder.getSociety();
            String[] parts = address != null ? address.split(",") : new String[]{};
            String flat     = parts.length > 1 ? parts[1].trim() : (address != null ? address : "");
            String building = parts.length > 0 ? parts[0].trim() : "";
            String area     = "Hadapsar, Pune";

            ((TextView) findViewById(R.id.addr_flat)).setText(flat);
            ((TextView) findViewById(R.id.addr_building)).setText(building);
            ((TextView) findViewById(R.id.addr_society)).setText(society);
            ((TextView) findViewById(R.id.addr_area)).setText(area);
            ((TextView) findViewById(R.id.addr_full)).setText(
                    building + ", " + flat + ", " + society + ", " + area);
        }

        // Wire photo slots
        photoCountText = findViewById(R.id.photo_count_text);
        photoCountText.setText("0 / " + MAX_PHOTOS);

        int[] slotIds  = {R.id.photo_slot_1, R.id.photo_slot_2, R.id.photo_slot_3,
                          R.id.photo_slot_4, R.id.photo_slot_5};
        int[] thumbIds = {R.id.photo_thumb_1, R.id.photo_thumb_2, R.id.photo_thumb_3,
                          R.id.photo_thumb_4, R.id.photo_thumb_5};
        int[] badgeIds = {R.id.photo_taken_badge_1, R.id.photo_taken_badge_2,
                          R.id.photo_taken_badge_3, R.id.photo_taken_badge_4,
                          R.id.photo_taken_badge_5};
        int[] checkIds = {R.id.photo_check_1, R.id.photo_check_2, R.id.photo_check_3,
                          R.id.photo_check_4, R.id.photo_check_5};

        for (int i = 0; i < MAX_PHOTOS; i++) {
            thumbs[i]  = findViewById(thumbIds[i]);
            badges[i]  = findViewById(badgeIds[i]);
            checks[i]  = findViewById(checkIds[i]);
            final int slot = i + 1;
            findViewById(slotIds[i]).setOnClickListener(v -> launchCamera(slot));
        }

        // Quick actions
        findViewById(R.id.btn_call_customer).setOnClickListener(v -> callCustomer());
        findViewById(R.id.btn_locate_customer).setOnClickListener(v -> locateCustomer());
        findViewById(R.id.btn_navigate).setOnClickListener(v -> locateCustomer());

        // Order details
        findViewById(R.id.btn_order_details).setOnClickListener(v -> showOrderDetails());

        // Dynamic fetch from Firebase if path is available
        String orderPath = getIntent().getStringExtra("order_path");
        if (orderPath != null && !orderPath.isEmpty()) {
            FirebaseRepository.getInstance().getOrderByPath(orderPath,
                err -> runOnUiThread(() ->
                    Toast.makeText(this, "Error: " + err, Toast.LENGTH_SHORT).show()),
                orders -> runOnUiThread(() -> {
                    if (!orders.isEmpty()) {
                        Map<String, Object> orderMap = orders.get(0);
                        updateUIFromMap(orderMap);

                        // Also fetch user data for complete info (name, address from user doc)
                        String path = FirebaseRepository.str(orderMap, "__path");
                        if (path.contains("/orders/")) {
                            String userPath = path.substring(0, path.indexOf("/orders/"));
                            FirebaseRepository.getInstance().getDocumentByPath(userPath,
                                e -> Log.e("PickupDetail", "User fetch error: " + e),
                                userMap -> runOnUiThread(() -> {
                                    // Merge user data into UI (priority to user doc for name/address if order doc is empty)
                                    updateUIWithUserData(userMap);
                                })
                            );
                        }
                    }
                })
            );
        }

        if (isReadOnly) {
            // ── Completed / read-only mode ────────────────────────────────
            applyReadOnlyMode();
        } else {
            // ── Active mode ───────────────────────────────────────────────
            // Bottom buttons
            findViewById(R.id.btn_complete_pickup).setOnClickListener(v -> completePickup());
            findViewById(R.id.btn_take_photo).setOnClickListener(v -> launchNextEmptySlot());
            findViewById(R.id.btn_report_issue).setOnClickListener(v -> reportIssue());
        }
    }

    private void applyReadOnlyMode() {
        // Show completed banner on the Complete button
        View completeBtn = findViewById(R.id.btn_complete_pickup);
        completeBtn.setAlpha(0.45f);
        completeBtn.setClickable(false);
        completeBtn.setFocusable(false);
        // Change label
        TextView btnLabel = completeBtn.findViewWithTag("complete_label");
        // Find the TextView inside the button LinearLayout
        if (completeBtn instanceof android.widget.LinearLayout) {
            for (int i = 0; i < ((android.widget.LinearLayout) completeBtn).getChildCount(); i++) {
                android.view.View child = ((android.widget.LinearLayout) completeBtn).getChildAt(i);
                if (child instanceof TextView) {
                    ((TextView) child).setText("Pickup Completed");
                }
            }
        }

        // Disable photo + issue buttons
        View takePhotoBtn = findViewById(R.id.btn_take_photo);
        takePhotoBtn.setAlpha(0.4f);
        takePhotoBtn.setClickable(false);
        takePhotoBtn.setFocusable(false);

        View reportBtn = findViewById(R.id.btn_report_issue);
        reportBtn.setAlpha(0.4f);
        reportBtn.setClickable(false);
        reportBtn.setFocusable(false);

        // Disable photo slots
        int[] slotIds = {R.id.photo_slot_1, R.id.photo_slot_2, R.id.photo_slot_3,
                         R.id.photo_slot_4, R.id.photo_slot_5};
        for (int id : slotIds) {
            View slot = findViewById(id);
            if (slot != null) {
                slot.setClickable(false);
                slot.setFocusable(false);
            }
        }
    }

    // ── Camera ────────────────────────────────────────────────────────────

    private void launchCamera(int slot) {
        if (photoTaken[slot - 1]) {
            // Already taken — ask to retake
            new AlertDialog.Builder(this)
                    .setTitle("Retake Photo " + slot + "?")
                    .setMessage("This will replace the existing photo.")
                    .setPositiveButton("Retake", (d, w) -> openCamera(slot))
                    .setNegativeButton("Cancel", null)
                    .show();
            return;
        }
        openCamera(slot);
    }

    private void openCamera(int slot) {
        activeSlot = slot;
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQ_BASE + slot);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void launchNextEmptySlot() {
        for (int i = 0; i < MAX_PHOTOS; i++) {
            if (!photoTaken[i]) {
                openCamera(i + 1);
                return;
            }
        }
        Toast.makeText(this, "All " + MAX_PHOTOS + " photos captured", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK || data == null) return;

        int slot = requestCode - REQ_BASE;
        if (slot < 1 || slot > MAX_PHOTOS) return;

        Bitmap bmp = (Bitmap) data.getExtras().get("data");
        if (bmp == null) return;

        // Show thumbnail
        int idx = slot - 1;
        photoTaken[idx] = true;
        thumbs[idx].setImageBitmap(bmp);
        thumbs[idx].setVisibility(View.VISIBLE);
        badges[idx].setVisibility(View.VISIBLE);
        checks[idx].setVisibility(View.VISIBLE);

        // Update counter
        int count = 0;
        for (boolean b : photoTaken) if (b) count++;
        photoCountText.setText(count + " / " + MAX_PHOTOS);

        Toast.makeText(this, "Photo " + slot + " captured ✓", Toast.LENGTH_SHORT).show();
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
        View btnReschedule = v.findViewById(R.id.btn_reschedule);
        View btnCancel = v.findViewById(R.id.btn_cancel_order);

        group.setOnCheckedChangeListener((g, checkedId) -> {
            btnReschedule.setAlpha(1.0f);
            btnReschedule.setClickable(true);
            btnCancel.setAlpha(1.0f);
            btnCancel.setClickable(true);
        });

        btnReschedule.setOnClickListener(view -> {
            int selectedId = group.getCheckedRadioButtonId();
            if (selectedId == -1 || currentOrder == null) return;

            android.widget.RadioButton selected = v.findViewById(selectedId);
            String reason = selected != null ? selected.getText().toString() : "Unknown Issue";

            com.example.laundaryagent.data.repository.FirebaseRepository.getInstance()
                    .rescheduleOrder(currentOrder.getFullPath(), reason, new com.example.laundaryagent.data.repository.FirebaseRepository.ActionCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(PickupDetailActivity.this, "Order rescheduled: " + reason, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
                        }
                        @Override public void onFailure(String error) {
                            Toast.makeText(PickupDetailActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        btnCancel.setOnClickListener(view -> {
            int selectedId = group.getCheckedRadioButtonId();
            if (selectedId == -1 || currentOrder == null) return;

            android.widget.RadioButton selected = v.findViewById(selectedId);
            String reason = selected != null ? selected.getText().toString() : "Unknown Issue";

            com.example.laundaryagent.data.repository.FirebaseRepository.getInstance()
                    .markOrderIncomplete(currentOrder.getFullPath(), reason, new com.example.laundaryagent.data.repository.FirebaseRepository.ActionCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(PickupDetailActivity.this, "Order cancelled: " + reason, Toast.LENGTH_SHORT).show();
                            dialog.dismiss();
                            finish();
                        }
                        @Override public void onFailure(String error) {
                            Toast.makeText(PickupDetailActivity.this, "Failed: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        dialog.show();
    }

    private void completePickup() {
        int count = 0;
        for (boolean b : photoTaken) if (b) count++;

        if (count == 0) {
            showNoPhotoDialog();
        } else {
            finishPickup();
        }
    }

    private void showNoPhotoDialog() {
        android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_no_photo, null);
        dialog.setContentView(dialogView);

        // Rounded window background
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.88f),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        dialogView.findViewById(R.id.dialog_btn_take_photo).setOnClickListener(v -> {
            dialog.dismiss();
            launchNextEmptySlot();
        });

        dialogView.findViewById(R.id.dialog_btn_skip).setOnClickListener(v -> {
            dialog.dismiss();
            finishPickup();
        });

        dialog.show();
    }

    private void showOrderDetails() {
        if (currentOrder == null) return;
        
        OrderServices services = currentOrder.getServices();
        if (services == null) {
            // Fallback to repository if Firestore items not yet loaded
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

    private void populateServiceCard(View root, int containerId, int countId,
                                     java.util.List<ServiceItem> items, String color) {
        LinearLayout container = root.findViewById(containerId);
        TextView countView     = root.findViewById(countId);

        int total = 0;
        for (ServiceItem s : items) total += s.getQuantity();

        if (total == 0) {
            // Hide the whole parent card
            ((View) container.getParent()).setVisibility(View.GONE);
            return;
        }

        countView.setText(total + " item" + (total > 1 ? "s" : ""));
        container.removeAllViews();

        for (ServiceItem item : items) {
            if (item.getQuantity() == 0) continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = (int) (6 * getResources().getDisplayMetrics().density);
            row.setLayoutParams(lp);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Dot
            View dot = new View(this);
            LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                    (int)(8 * getResources().getDisplayMetrics().density),
                    (int)(8 * getResources().getDisplayMetrics().density));
            dotLp.rightMargin = (int)(10 * getResources().getDisplayMetrics().density);
            dot.setLayoutParams(dotLp);
            dot.setBackgroundColor(Color.parseColor(color));
            // Make dot circular
            dot.post(() -> {
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                gd.setColor(Color.parseColor(color));
                dot.setBackground(gd);
            });
            row.addView(dot);

            // Item name
            TextView name = new TextView(this);
            LinearLayout.LayoutParams nameLp = new LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            name.setLayoutParams(nameLp);
            name.setText(item.getName());
            name.setTextColor(Color.parseColor("#475569"));
            name.setTextSize(13f);
            row.addView(name);

            // Quantity badge
            TextView qty = new TextView(this);
            qty.setText("× " + item.getQuantity());
            qty.setTextColor(Color.parseColor(color));
            qty.setTextSize(13f);
            qty.setTypeface(null, android.graphics.Typeface.BOLD);
            row.addView(qty);

            container.addView(row);
        }
    }

    private void finishPickup() {
        if (currentOrder != null) {
            com.example.laundaryagent.data.repository.FirebaseRepository.getInstance()
                    .markOrderPickupDone(currentOrder.getFullPath(), new com.example.laundaryagent.data.repository.FirebaseRepository.ActionCallback() {
                        @Override public void onSuccess() {
                            Toast.makeText(PickupDetailActivity.this, "Pickup completed successfully!", Toast.LENGTH_LONG).show();
                            finish();
                        }
                        @Override public void onFailure(String error) {
                            Toast.makeText(PickupDetailActivity.this, "Failed to complete pickup: " + error, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateUIFromMap(Map<String, Object> map) {
        String id      = FirebaseRepository.str(map, "id");
        String name    = FirebaseRepository.str(map, "customerName");
        if (name.isEmpty()) name = FirebaseRepository.str(map, "name");
        String society = FirebaseRepository.str(map, "society");
        String address = FirebaseRepository.str(map, "address");
        String phone   = FirebaseRepository.str(map, "customerPhone");
        if (phone.isEmpty()) phone = FirebaseRepository.str(map, "phone"); // match screenshot
        if (phone.isEmpty()) phone = FirebaseRepository.str(map, "userPhone");
        String path    = FirebaseRepository.str(map, "__path");

        currentOrder = new OrderItem(id, name, address, society, phone, "", path);
        
        // Parse items array from Firestore
        Object itemsObj = map.get("items");
        if (itemsObj instanceof java.util.List) {
            currentOrder.setServices(FirebaseRepository.getInstance()
                    .parseOrderServices((java.util.List<Map<String, Object>>) itemsObj));
        }

        ((TextView) findViewById(R.id.customer_name)).setText(name);
        ((TextView) findViewById(R.id.society_name)).setText(society.isEmpty() ? "Residence" : society);
        
        TextView orderIdView = findViewById(R.id.order_id_text);
        String fullId = id;
        String shortId = fullId.length() > 8 ? fullId.substring(0, 8) + "..." : fullId;
        orderIdView.setText("#" + shortId);
        orderIdView.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Order ID", fullId);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Order ID copied", Toast.LENGTH_SHORT).show();
        });

        if (!name.isEmpty()) {
            ((TextView) findViewById(R.id.customer_initial))
                    .setText(String.valueOf(name.charAt(0)).toUpperCase());
        }

        // Update address views
        if (address != null && !address.isEmpty()) {
            String[] parts = address.split(",");
            String flat     = parts.length > 0 ? parts[0].trim() : address;
            String building = parts.length > 1 ? parts[1].trim() : "";
            String area     = parts.length > 2 ? parts[2].trim() : "Pune";

            ((TextView) findViewById(R.id.addr_flat)).setText(flat);
            ((TextView) findViewById(R.id.addr_building)).setText(building.isEmpty() ? society : building);
            ((TextView) findViewById(R.id.addr_society)).setText(society.isEmpty() ? "Residential Area" : society);
            ((TextView) findViewById(R.id.addr_area)).setText(area);
            ((TextView) findViewById(R.id.addr_full)).setText(address);
        }
    }

    private void updateUIWithUserData(Map<String, Object> userMap) {
        if (userMap == null) return;

        String name    = FirebaseRepository.str(userMap, "name");
        String address = FirebaseRepository.str(userMap, "address");
        String phone   = FirebaseRepository.str(userMap, "phone");
        
        // Society might be part of address or a separate field "society"
        String society = FirebaseRepository.str(userMap, "society");
        if (society.isEmpty()) {
            if (!address.isEmpty()) {
                String[] parts = address.split(",");
                // Guessing society from the 3rd part or 1st part
                society = parts.length > 2 ? parts[2].trim() : (parts.length > 0 ? parts[0].trim() : "Residence");
            } else {
                society = "Residence";
            }
        }

        // Only update if not already set by orderMap (or if orderMap had placeholders)
        if (!name.isEmpty()) {
            ((TextView) findViewById(R.id.customer_name)).setText(name);
            ((TextView) findViewById(R.id.customer_initial))
                    .setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
        
        if (!society.isEmpty()) {
            ((TextView) findViewById(R.id.society_name)).setText(society);
        }

        if (!address.isEmpty()) {
            String[] parts = address.split(",");
            String flat     = parts.length > 0 ? parts[0].trim() : address;
            String building = parts.length > 1 ? parts[1].trim() : "";
            String area     = parts.length > 2 ? parts[2].trim() : "Pune";

            ((TextView) findViewById(R.id.addr_flat)).setText(flat);
            ((TextView) findViewById(R.id.addr_building)).setText(building.isEmpty() ? society : building);
            ((TextView) findViewById(R.id.addr_society)).setText(society);
            ((TextView) findViewById(R.id.addr_area)).setText(area);
            ((TextView) findViewById(R.id.addr_full)).setText(address);
        }
    }
}

