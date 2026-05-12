package com.example.laundaryagent;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderStatus;
import com.example.laundaryagent.data.repository.LaundryRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.List;

public class ReportsFragment extends Fragment {

    private LaundryRepository repository;
    private ViewPager2 viewPager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        repository = LaundryRepository.getInstance();

        List<OrderItem> pickups   = repository.getPickupOrders();
        List<OrderItem> deliveries = repository.getDeliveryOrders();

        // Header counts
        TextView reportPickupCount   = view.findViewById(R.id.report_pickup_count);
        TextView reportDeliveryCount = view.findViewById(R.id.report_delivery_count);
        reportPickupCount.setText(String.valueOf(pickups.size()));
        reportDeliveryCount.setText(String.valueOf(deliveries.size()));

        // Progress ring
        int total = pickups.size() + deliveries.size();
        int completed = 0;
        for (OrderItem o : pickups)    if (o.getStatus() == OrderStatus.COMPLETED) completed++;
        for (OrderItem o : deliveries) if (o.getStatus() == OrderStatus.COMPLETED) completed++;

        CircularProgressIndicator progress = view.findViewById(R.id.report_circular_progress);
        TextView percentageText = view.findViewById(R.id.report_percentage_text);
        TextView summaryText    = view.findViewById(R.id.report_tasks_summary);

        int percent = total > 0 ? (completed * 100) / total : 0;
        progress.setProgress(percent);
        percentageText.setText(percent + "%");
        summaryText.setText(completed + " of " + total + " completed");

        // ViewPager2 + TabLayout
        viewPager = view.findViewById(R.id.report_view_pager);
        viewPager.setAdapter(new ReportPagerAdapter(this));

        TabLayout tabLayout = view.findViewById(R.id.report_tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(position == 0 ? "Pickup Tasks" : "Delivery Tasks")
        ).attach();

        return view;
    }

    // ── Pager adapter ──────────────────────────────────────────────────────
    private static class ReportPagerAdapter extends FragmentStateAdapter {
        ReportPagerAdapter(ReportsFragment f) { super(f); }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return ReportPageFragment.newInstance(position);
        }

        @Override
        public int getItemCount() { return 2; }
    }

    // ── Inner page fragment ────────────────────────────────────────────────
    public static class ReportPageFragment extends Fragment {

        private static final String ARG_TYPE = "type";

        public static ReportPageFragment newInstance(int type) {
            ReportPageFragment f = new ReportPageFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_TYPE, type);
            f.setArguments(args);
            return f;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_report_page, container, false);

            int type = getArguments() != null ? getArguments().getInt(ARG_TYPE, 0) : 0;
            boolean isPickup = (type == 0);

            LaundryRepository repo = LaundryRepository.getInstance();
            List<OrderItem> orders = isPickup
                    ? repo.getPickupOrders()
                    : repo.getDeliveryOrders();

            // Section label
            TextView label  = view.findViewById(R.id.section_label);
            TextView badge  = view.findViewById(R.id.section_badge);
            View accentBar  = view.findViewById(R.id.section_accent_bar);
            MaterialCardView badgeCard = (MaterialCardView) badge.getParent();

            label.setText(isPickup ? "Pickups" : "Deliveries");
            badge.setText(String.valueOf(orders.size()));

            if (isPickup) {
                // teal-green accent
                accentBar.setBackgroundColor(Color.parseColor("#06D6A0"));
                badge.setTextColor(Color.parseColor("#4361EE"));
                badgeCard.setCardBackgroundColor(Color.parseColor("#EEF2FF"));
            } else {
                // amber accent
                accentBar.setBackgroundColor(Color.parseColor("#FF9F1C"));
                badge.setTextColor(Color.parseColor("#FF9F1C"));
                badgeCard.setCardBackgroundColor(Color.parseColor("#FFF7ED"));
            }

            // RecyclerView
            RecyclerView rv = view.findViewById(R.id.report_page_recycler);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setAdapter(new OrderAdapter(orders, order -> {}));

            return view;
        }
    }
}
