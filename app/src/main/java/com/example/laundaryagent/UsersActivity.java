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
    private UserAdapter adapter;
    private TextView tvUserCount;
    private ListenerRegistration listenerReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        tvUserCount = findViewById(R.id.tv_user_count);
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
        // Uses collectionGroup("users") to match path: app_data/[doc]/users/{phone}
        listenerReg = FirebaseRepository.getInstance().listenAllUsers(userDocs ->
            runOnUiThread(() -> {
                allUsers.clear();
                for (java.util.Map<String, Object> doc : userDocs) {
                    String phone   = FirebaseRepository.str(doc, "id");   // doc ID = phone
                    String name    = FirebaseRepository.str(doc, "name");
                    if (name.isEmpty()) name = phone; // fallback to phone
                    String email   = FirebaseRepository.str(doc, "email");
                    String address = FirebaseRepository.str(doc, "address");
                    allUsers.add(new User(name, phone, email, address));
                }
                if (tvUserCount != null)
                    tvUserCount.setText(allUsers.size() + " Users");
                EditText et = findViewById(R.id.et_search);
                filter(et != null ? et.getText().toString() : "");
            })
        );
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.et_search);
        if (etSearch == null) return;
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        filteredUsers.clear();
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {
            String lq = query.toLowerCase();
            for (User user : allUsers) {
                if (user.name.toLowerCase().contains(lq) ||
                    user.phone.toLowerCase().contains(lq) ||
                    user.email.toLowerCase().contains(lq)) {
                    filteredUsers.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        if (listenerReg != null) listenerReg.remove();
        super.onDestroy();
    }

    // ── Model ──────────────────────────────────────────────────────────────

    private static class User {
        String name, phone, email, address;
        User(String n, String p, String e, String a) {
            name = n; phone = p; email = e; address = a;
        }
    }

    // ── Adapter ────────────────────────────────────────────────────────────

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private final List<User> users;
        UserAdapter(List<User> users) { this.users = users; }

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

        @Override public int getItemCount() { return users.size(); }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone;
            UserViewHolder(View itemView) {
                super(itemView);
                tvName  = itemView.findViewById(R.id.tv_user_name);
                tvPhone = itemView.findViewById(R.id.tv_user_phone);
            }
        }
    }
}
