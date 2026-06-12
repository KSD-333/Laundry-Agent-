package com.example.laundaryagent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Personal Information
        view.findViewById(R.id.menu_personal_info).setOnClickListener(v -> {
            animateClick(v);
            startActivity(new Intent(getActivity(), PersonalInfoActivity.class));
        });

        // Help Center
        view.findViewById(R.id.menu_help_center).setOnClickListener(v -> {
            animateClick(v);
            startActivity(new Intent(getActivity(), HelpCenterActivity.class));
        });

        // Logout
        view.findViewById(R.id.logout_button_card).setOnClickListener(v -> {
            animateClick(v);
            handleLogout();
        });

        // Set up Assigned Societies RecyclerView
        RecyclerView rvSocieties = view.findViewById(R.id.rv_assigned_societies);
        TextView tvLoading = view.findViewById(R.id.tv_loading_societies);
        rvSocieties.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<String> societiesList = new ArrayList<>();
        SocietyAdapter adapter = new SocietyAdapter(societiesList);
        rvSocieties.setAdapter(adapter);

        SharedPreferences prefs = getActivity().getSharedPreferences("LaundryPrefs", Context.MODE_PRIVATE);
        String storedPhone = prefs.getString("agent_phone", "");
        if (storedPhone.isEmpty()) {
            String identity = prefs.getString("user_identity", "");
            if (identity.matches("\\d{10}")) storedPhone = identity;
        }
        final String agentPhone = storedPhone;

        TextView tvName = view.findViewById(R.id.tv_profile_name);
        TextView tvRole = view.findViewById(R.id.tv_profile_role);
        TextView tvInitials = view.findViewById(R.id.tv_avatar_initials);

        // Pre-fill from SharedPreferences while loading from Firebase
        String cachedName = prefs.getString("user_identity", "Agent");
        if (tvName != null) tvName.setText(cachedName);
        if (tvInitials != null && cachedName.length() > 0) {
            tvInitials.setText(String.valueOf(cachedName.charAt(0)).toUpperCase());
        }
        String cachedRole = prefs.getString("user_role", "Delivery Agent");
        if (tvRole != null) tvRole.setText(cachedRole);

        if (!agentPhone.isEmpty()) {
            com.example.laundaryagent.data.repository.FirebaseRepository.getInstance()
                .getAgentByPhone(agentPhone,
                    err -> { /* Ignore error, keep cached data */ },
                    data -> {
                        if (getActivity() == null) return;
                        getActivity().runOnUiThread(() -> {
                            String realName = com.example.laundaryagent.data.repository.FirebaseRepository.str(data, "name");
                            if (realName.isEmpty()) realName = com.example.laundaryagent.data.repository.FirebaseRepository.str(data, "agentName");
                            if (!realName.isEmpty()) {
                                if (tvName != null) tvName.setText(realName);
                                if (tvInitials != null && realName.length() > 0) {
                                    tvInitials.setText(String.valueOf(realName.charAt(0)).toUpperCase());
                                }
                            }
                            // Also check if they have a specific role assigned in Firebase
                            String specificRole = com.example.laundaryagent.data.repository.FirebaseRepository.str(data, "role");
                            if (!specificRole.isEmpty() && tvRole != null) {
                                tvRole.setText(specificRole);
                            }
                        });
                    });
        }

        if (!agentPhone.isEmpty()) {
            TaskViewModel viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
            viewModel.getSocieties(getContext(), agentPhone).observe(getViewLifecycleOwner(), societies -> {
                tvLoading.setVisibility(View.GONE);
                societiesList.clear();
                // Filter out "All Societies" if it is present
                for (String s : societies) {
                    if (!s.equals("All Societies")) societiesList.add(s);
                }
                
                if (societiesList.isEmpty()) {
                    tvLoading.setText("No assigned societies");
                    tvLoading.setVisibility(View.VISIBLE);
                } else {
                    rvSocieties.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            });
        } else {
            tvLoading.setText("Agent phone not found");
        }

        return view;
    }

    private void animateClick(View v) {
        v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                .withEndAction(() ->
                        v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                .start();
    }

    private void handleLogout() {
        if (getActivity() == null) return;
        SharedPreferences prefs = getActivity()
                .getSharedPreferences("LaundryPrefs", Context.MODE_PRIVATE);
        prefs.edit().clear().apply();
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(getActivity(), LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        getActivity().finish();
    }

    private static class SocietyAdapter extends RecyclerView.Adapter<SocietyAdapter.ViewHolder> {
        private final List<String> items;

        SocietyAdapter(List<String> items) {
            this.items = items;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_society_picker, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.text.setText(items.get(position));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View v) {
                super(v);
                text = v.findViewById(R.id.society_item_name);
            }
        }
    }
}
