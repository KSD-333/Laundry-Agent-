package com.example.laundaryagent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        View logoutBtn = view.findViewById(R.id.logout_button_card);
        logoutBtn.setOnClickListener(v -> {
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                    .withEndAction(() ->
                            v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                    .start();
            handleLogout();
        });

        // Animate menu rows in
        animateChildren(view);

        return view;
    }

    private void animateChildren(View root) {
        int[] ids = {
                R.id.logout_button_card
        };
        // Animate the whole surface content
        View surface = root.findViewWithTag("surface");
        if (surface == null) return;
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
}
