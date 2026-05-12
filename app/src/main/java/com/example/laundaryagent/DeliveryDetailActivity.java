package com.example.laundaryagent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.repository.LaundryRepository;

public class DeliveryDetailActivity extends AppCompatActivity {

    private OrderItem currentOrder;
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pickup_detail);

        String orderId = getIntent().getStringExtra("order_id");
        currentOrder = LaundryRepository.getInstance().getOrderById(orderId);

        if (currentOrder != null) {
            ((TextView) findViewById(R.id.customer_name)).setText(currentOrder.getCustomerName());
            ((TextView) findViewById(R.id.society_name)).setText(currentOrder.getSociety());
            ((TextView) findViewById(R.id.order_id_text)).setText("#" + currentOrder.getId());
            
            // Safe update for delivery text
            TextView mainBtnText = findViewById(R.id.btn_main_action_text);
            if (mainBtnText != null) {
                mainBtnText.setText("Complete Delivery");
            }
        }

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        // Quick Actions
        findViewById(R.id.btn_call_customer).setOnClickListener(v -> callCustomer());
        findViewById(R.id.btn_locate_customer).setOnClickListener(v -> locateCustomer());
        findViewById(R.id.btn_add_item).setOnClickListener(v -> Toast.makeText(this, "Review items logic here", Toast.LENGTH_SHORT).show());

        // Protocol Actions
        findViewById(R.id.btn_complete_pickup).setOnClickListener(v -> completeDelivery());
        findViewById(R.id.btn_take_photo).setOnClickListener(v -> takePhoto());
        findViewById(R.id.btn_report_issue).setOnClickListener(v -> reportIssue());
    }

    private void callCustomer() {
        Intent intent = new Intent(Intent.ACTION_DIAL);
        intent.setData(Uri.parse("tel:1234567890"));
        startActivity(intent);
    }

    private void locateCustomer() {
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(currentOrder.getSociety()));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private void takePhoto() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Camera not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void reportIssue() {
        String[] issues = {
            "Payment Not Received",
            "Customer Not Available",
            "Incomplete Order Delivery",
            "Wrong Item Delivered",
            "Customer Refused Delivery",
            "Address Not Found",
            "Other Delivery Issue"
        };
        new AlertDialog.Builder(this)
                .setTitle("Report Delivery Issue")
                .setItems(issues, (dialog, which) -> {
                    Toast.makeText(this, "Issue reported: " + issues[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void completeDelivery() {
        Toast.makeText(this, "Delivery completed successfully!", Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Toast.makeText(this, "Delivery photo captured", Toast.LENGTH_SHORT).show();
        }
    }
}
