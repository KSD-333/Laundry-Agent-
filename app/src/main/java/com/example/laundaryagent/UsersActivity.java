package com.example.laundaryagent;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.firebase.firestore.ListenerRegistration;

public class UsersActivity extends AppCompatActivity {

    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filteredUsers = new ArrayList<>();
    private final List<java.util.Map<String, Object>> allUserDocs = new ArrayList<>();
    private final List<java.util.Map<String, Object>> allOrderDocs = new ArrayList<>();

    private UserAdapter adapter;
    private TextView tvUserCount;
    private TextView tvCurrentFilter;
    private View btnFilter;
    private ListenerRegistration userListenerReg;
    private ListenerRegistration orderListenerReg;
    private String franchiseId = "";

    private String currentSocietyFilter = "All Societies";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Get franchiseId from intent (passed by AdminDashboardActivity)
        if (getIntent().hasExtra("franchise_id")) {
            franchiseId = getIntent().getStringExtra("franchise_id");
        } else {
            franchiseId = getSharedPreferences("LaundryPrefs", MODE_PRIVATE)
                    .getString("franchise_id", "");
        }

        tvUserCount = findViewById(R.id.tv_user_count);
        tvCurrentFilter = findViewById(R.id.tv_current_filter);
        btnFilter = findViewById(R.id.btn_filter);

        if (btnFilter != null) {
            btnFilter.setOnClickListener(this::showFilterMenu);
        }

        initViews();
        loadFromFirebase();
        setupSearch();
    }

    private void initViews() {
        if (findViewById(R.id.btn_back) != null)
            findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        RecyclerView rvUsers = findViewById(R.id.rv_users);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(filteredUsers);
        rvUsers.setAdapter(adapter);
    }

    private void loadFromFirebase() {
        FirebaseRepository fb = FirebaseRepository.getInstance();

        // 1. Listen to users (fetch all from root users path and filter client-side)
        userListenerReg = fb.listenAllUsers(userDocs ->
            runOnUiThread(() -> {
                allUserDocs.clear();
                if (userDocs != null) {
                    if (franchiseId != null && !franchiseId.isEmpty()) {
                        for (java.util.Map<String, Object> doc : userDocs) {
                            String fId = FirebaseRepository.str(doc, "franchiseId").trim();
                            if (franchiseId.trim().equals(fId)) {
                                allUserDocs.add(doc);
                            }
                        }
                    } else {
                        allUserDocs.addAll(userDocs);
                    }
                }
                recalculateUsers();
            })
        );

        // 2. Orders filtered by franchise
        orderListenerReg = fb.listenFranchiseOrders(franchiseId, orderDocs ->
            runOnUiThread(() -> {
                allOrderDocs.clear();
                if (orderDocs != null) allOrderDocs.addAll(orderDocs);
                recalculateUsers();
            })
        );
    }

    private void recalculateUsers() {
        allUsers.clear();
        long oneMonthAgoMs = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        for (java.util.Map<String, Object> doc : allUserDocs) {
            String phone   = FirebaseRepository.str(doc, "id");   // doc ID = phone
            String name    = FirebaseRepository.str(doc, "name");
            if (name.isEmpty()) name = phone; // fallback to phone
            String email   = FirebaseRepository.str(doc, "email");
            String address = FirebaseRepository.str(doc, "address");
            String society = FirebaseRepository.str(doc, "society");
            if (society.isEmpty()) {
                // Try to extract from address
                String[] parts = address.split(",");
                society = parts.length > 2 ? parts[2].trim() : (parts.length > 0 ? parts[0].trim() : "");
            }

            // Check if active based on last one month purchases
            boolean active = false;
            for (java.util.Map<String, Object> order : allOrderDocs) {
                String customerPhone = FirebaseRepository.str(order, "customerPhone");
                if (customerPhone.equals(phone)) {
                    long orderTime = getOrderTime(order);
                    if (orderTime >= oneMonthAgoMs) {
                        active = true;
                        break;
                    }
                }
            }

            allUsers.add(new User(name, phone, email, address, society, active));
        }

        if (tvUserCount != null) {
            tvUserCount.setText(allUsers.size() + " Users");
        }

        if (tvCurrentFilter != null) {
            if (currentSocietyFilter.equals("All Societies")) {
                tvCurrentFilter.setText("All Societies (" + allUsers.size() + ")");
            } else {
                int count = 0;
                for (User u : allUsers) {
                    if (u.society != null && u.society.equalsIgnoreCase(currentSocietyFilter)) {
                        count++;
                    }
                }
                tvCurrentFilter.setText(currentSocietyFilter + " (" + count + ")");
            }
        }

        applyFilter();
    }

    private long getOrderTime(java.util.Map<String, Object> orderDoc) {
        Object completedAt = orderDoc.get("completedAt");
        if (completedAt instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) completedAt).toDate().getTime();
        }
        Object updatedAt = orderDoc.get("updatedAt");
        if (updatedAt instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) updatedAt).toDate().getTime();
        }
        Object pickedAt = orderDoc.get("pickedAt");
        if (pickedAt instanceof com.google.firebase.Timestamp) {
            return ((com.google.firebase.Timestamp) pickedAt).toDate().getTime();
        }
        return System.currentTimeMillis();
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.et_search);
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilter();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilter() {
        EditText et = findViewById(R.id.et_search);
        String query = et != null ? et.getText().toString() : "";
        filter(query);
    }

    private void filter(String query) {
        filteredUsers.clear();
        String lq = query.toLowerCase();
        for (User user : allUsers) {
            boolean matchesSearch = query.isEmpty() ||
                user.name.toLowerCase().contains(lq) ||
                user.phone.toLowerCase().contains(lq) ||
                user.email.toLowerCase().contains(lq);

            boolean matchesSociety = currentSocietyFilter.equals("All Societies") ||
                (user.society != null && user.society.equalsIgnoreCase(currentSocietyFilter));

            if (matchesSearch && matchesSociety) {
                filteredUsers.add(user);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void showFilterMenu(View v) {
        View popupView = getLayoutInflater().inflate(R.layout.layout_custom_dropdown, null);
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(popupView, 
            ViewGroup.LayoutParams.WRAP_CONTENT, 
            ViewGroup.LayoutParams.WRAP_CONTENT, true);

        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        popupWindow.setElevation(20);

        android.widget.LinearLayout optionsLayout = popupView.findViewById(R.id.ll_dropdown_options);

        populateFilterMenu(optionsLayout, popupWindow, false);

        popupWindow.showAsDropDown(v, 0, 10);
    }

    private void populateFilterMenu(android.widget.LinearLayout optionsLayout, android.widget.PopupWindow popupWindow, boolean showAll) {
        optionsLayout.removeAllViews();

        // Dynamically extract unique societies
        List<String> societies = new ArrayList<>();
        societies.add("All Societies");
        for (User u : allUsers) {
            if (u.society != null && !u.society.isEmpty() && !societies.contains(u.society)) {
                societies.add(u.society);
            }
        }

        // Calculate counts
        java.util.Map<String, Integer> societyCounts = new java.util.HashMap<>();
        for (User u : allUsers) {
            if (u.society != null && !u.society.isEmpty()) {
                societyCounts.put(u.society, societyCounts.getOrDefault(u.society, 0) + 1);
            }
        }

        int limit = 4;
        boolean hasMore = societies.size() > limit;
        int itemsToShow = (hasMore && !showAll) ? limit : societies.size();

        for (int i = 0; i < itemsToShow; i++) {
            final String society = societies.get(i);
            View itemView = getLayoutInflater().inflate(R.layout.item_filter_option, null);
            
            // Format text with user count
            final String displayName = society.equals("All Societies")
                ? "All Societies (" + allUsers.size() + ")"
                : society + " (" + societyCounts.getOrDefault(society, 0) + ")";

            ((TextView) itemView.findViewById(R.id.tv_option_text)).setText(displayName);
            ((android.widget.ImageView) itemView.findViewById(R.id.iv_option_icon)).setImageResource(R.drawable.ic_pin);
            
            if (currentSocietyFilter.equals(society)) {
                itemView.findViewById(R.id.iv_check).setVisibility(View.VISIBLE);
                ((com.google.android.material.card.MaterialCardView) itemView.findViewById(R.id.card_option_icon)).setCardBackgroundColor(0xFFE0F2FE);
                ((android.widget.ImageView) itemView.findViewById(R.id.iv_option_icon)).setColorFilter(0xFF0EA5E9);
            }

            itemView.setOnClickListener(click -> {
                popupWindow.dismiss();
                currentSocietyFilter = society;
                if (tvCurrentFilter != null) tvCurrentFilter.setText(displayName);
                applyFilter();
            });
            optionsLayout.addView(itemView);
        }

        // Show More option if not expanded and there are more items
        if (hasMore && !showAll) {
            View moreView = getLayoutInflater().inflate(R.layout.item_filter_option, null);
            ((TextView) moreView.findViewById(R.id.tv_option_text)).setText("Show More (" + (societies.size() - limit) + ")...");
            ((android.widget.ImageView) moreView.findViewById(R.id.iv_option_icon)).setImageResource(R.drawable.ic_clock);
            ((android.widget.ImageView) moreView.findViewById(R.id.iv_option_icon)).setColorFilter(0xFF64748B);
            moreView.findViewById(R.id.iv_check).setVisibility(View.GONE);
            
            moreView.setOnClickListener(click -> {
                // Re-populate showing all items
                populateFilterMenu(optionsLayout, popupWindow, true);
            });
            optionsLayout.addView(moreView);
        }
    }

    @Override
    protected void onDestroy() {
        if (userListenerReg != null) userListenerReg.remove();
        if (orderListenerReg != null) orderListenerReg.remove();
        super.onDestroy();
    }

    // ── Model ──────────────────────────────────────────────────────────────

    private static class User {
        String name, phone, email, address, society;
        boolean isActive;
        User(String n, String p, String e, String a, String s, boolean active) {
            name = n; phone = p; email = e; address = a; society = s; isActive = active;
        }
    }

    // ── Adapter ────────────────────────────────────────────────────────────

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<User> users;

        UserAdapter(List<User> users) {
            this.users = users;
        }

        @Override
        public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            User user = users.get(position);
            holder.tvName.setText(user.name);
            holder.tvPhone.setText(user.phone.isEmpty() ? "No phone" : user.phone);
            holder.tvSociety.setText(user.society.isEmpty() ? "Residence" : user.society);

            if (user.isActive) {
                holder.cardStatus.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFD1FAE5));
                holder.tvStatus.setText("Active");
                holder.tvStatus.setTextColor(0xFF065F46);
            } else {
                holder.cardStatus.setCardBackgroundColor(android.content.res.ColorStateList.valueOf(0xFFF1F5F9));
                holder.tvStatus.setText("Inactive");
                holder.tvStatus.setTextColor(0xFF475569);
            }

            holder.itemView.setOnClickListener(v -> showUserDetailsDialog(user));
        }

        private void showUserDetailsDialog(User user) {
            View dialogView = LayoutInflater.from(UsersActivity.this)
                    .inflate(R.layout.dialog_user_details, null);
            androidx.appcompat.app.AlertDialog dialog =
                    new androidx.appcompat.app.AlertDialog.Builder(
                            UsersActivity.this, R.style.CustomDialogTheme)
                            .setView(dialogView).create();
            if (dialog.getWindow() != null)
                dialog.getWindow().setBackgroundDrawable(
                        new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            ((TextView) dialogView.findViewById(R.id.tv_dialog_user_name)).setText(user.name);
            ((TextView) dialogView.findViewById(R.id.tv_dialog_user_phone)).setText(user.phone);
            ((TextView) dialogView.findViewById(R.id.tv_dialog_user_email)).setText(user.email);
            ((TextView) dialogView.findViewById(R.id.tv_dialog_user_address)).setText(user.address);
            dialogView.findViewById(R.id.btn_close_user_dialog).setOnClickListener(v -> dialog.dismiss());
            dialog.show();
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone, tvSociety, tvStatus;
            com.google.android.material.card.MaterialCardView cardStatus;

            UserViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_user_name);
                tvPhone = itemView.findViewById(R.id.tv_user_phone);
                tvSociety = itemView.findViewById(R.id.tv_user_society);
                tvStatus = itemView.findViewById(R.id.tv_user_status);
                cardStatus = itemView.findViewById(R.id.card_user_status);
            }
        }
    }
}
