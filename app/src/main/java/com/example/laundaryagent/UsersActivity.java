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

public class UsersActivity extends AppCompatActivity {

    private List<User> allUsers = new ArrayList<>();
    private List<User> filteredUsers = new ArrayList<>();
    private UserAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        initViews();
        loadMockData();
        setupSearch();
    }

    private void initViews() {
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());
        RecyclerView rvUsers = findViewById(R.id.rv_users);
        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserAdapter(filteredUsers);
        rvUsers.setAdapter(adapter);
    }

    private void loadMockData() {
        allUsers.add(new User("Rahul Sharma", "+91 98234 56789", "rahul.s@email.com", "Flat 402, Sapphire Heights, Indore"));
        allUsers.add(new User("Priya Patel", "+91 87654 32109", "priya.p@email.com", "House 12, Lotus Colony, Indore"));
        allUsers.add(new User("Amit Verma", "+91 99887 76655", "amit.v@email.com", "B-201, Green Valley, Vijay Nagar"));
        allUsers.add(new User("Sneha Gupta", "+91 77665 54433", "sneha.g@email.com", "55, Silver Palace, Rajendra Nagar"));
        allUsers.add(new User("Vikram Singh", "+91 91223 34455", "vikram.s@email.com", "78-A, Scheme No. 54, Indore"));
        allUsers.add(new User("Anjali Rao", "+91 88776 65544", "anjali.r@email.com", "M-14, Sukhliya Main Road, Indore"));
        
        filteredUsers.addAll(allUsers);
        adapter.notifyDataSetChanged();
    }

    private void setupSearch() {
        EditText etSearch = findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filter(String query) {
        filteredUsers.clear();
        if (query.isEmpty()) {
            filteredUsers.addAll(allUsers);
        } else {
            String lowerQuery = query.toLowerCase();
            for (User user : allUsers) {
                if (user.name.toLowerCase().contains(lowerQuery) || 
                    user.phone.toLowerCase().contains(lowerQuery) ||
                    user.email.toLowerCase().contains(lowerQuery)) {
                    filteredUsers.add(user);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private static class User {
        String name;
        String phone;
        String email;
        String address;
        User(String name, String phone, String email, String address) {
            this.name = name;
            this.phone = phone;
            this.email = email;
            this.address = address;
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> users;

        UserAdapter(List<User> users) {
            this.users = users;
        }

        @Override
        public UserViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(UserViewHolder holder, int position) {
            User user = users.get(position);
            holder.tvName.setText(user.name);
            holder.tvPhone.setText(user.phone);

            holder.itemView.setOnClickListener(v -> showUserDetailsDialog(user));
        }

        private void showUserDetailsDialog(User user) {
            View dialogView = LayoutInflater.from(UsersActivity.this).inflate(R.layout.dialog_user_details, null);
            androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(UsersActivity.this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .create();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            }

            ((TextView) dialogView.findViewById(R.id.tv_dialog_user_name)).setText(user.name);
            ((TextView) dialogView.findViewById(R.id.tv_dialog_user_phone)).setText(user.phone);
            ((TextView) dialogView.findViewById(R.id.tv_dialog_user_email)).setText(user.email);
            ((TextView) dialogView.findViewById(R.id.tv_dialog_user_address)).setText(user.address);

            dialogView.findViewById(R.id.btn_close_user_dialog).setOnClickListener(view -> dialog.dismiss());
            dialog.show();
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            TextView tvName, tvPhone;
            UserViewHolder(View itemView) {
                super(itemView);
                tvName = itemView.findViewById(R.id.tv_user_name);
                tvPhone = itemView.findViewById(R.id.tv_user_phone);
            }
        }
    }
}
