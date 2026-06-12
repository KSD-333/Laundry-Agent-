package com.example.laundaryagent;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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
import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import androidx.lifecycle.ViewModelProvider;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ReportsFragment extends Fragment {

    private TaskViewModel viewModel;
    private ViewPager2 viewPager;
    
    private TextView reportPickupCount, reportDeliveryCount, percentageText, summaryText;
    private CircularProgressIndicator progress;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        reportPickupCount   = view.findViewById(R.id.report_pickup_count);
        reportDeliveryCount = view.findViewById(R.id.report_delivery_count);
        progress            = view.findViewById(R.id.report_circular_progress);
        percentageText      = view.findViewById(R.id.report_percentage_text);
        summaryText         = view.findViewById(R.id.report_tasks_summary);

        viewPager = view.findViewById(R.id.report_view_pager);
        viewPager.setAdapter(new ReportPagerAdapter(this));

        TabLayout tabLayout = view.findViewById(R.id.report_tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(position == 0 ? "Pickup Tasks" : "Delivery Tasks")
        ).attach();

        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        
        viewModel.getOrderItems(getContext()).observe(getViewLifecycleOwner(), items -> {
            updateHeaderStats(items);
        });
        
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            List<OrderItem> items = viewModel.getOrderItems(getContext()).getValue();
            if (items != null) {
                updateHeaderStats(items);
            }
        });

        return view;
    }

    private void updateHeaderStats(List<OrderItem> items) {
        int pickups = 0;
        int deliveries = 0;
        int completed = 0;
        int total = 0;
        
        String today = viewModel.getSelectedDate().getValue();
        String targetDate = normalizeDateStr(today);

        for (OrderItem item : items) {
            String pDate = normalizeDateStr(item.getPickupDate());
            String dDate = normalizeDateStr(item.getDeliveryDate());
            
            boolean matchesPickupDate = !pDate.isEmpty() && pDate.equals(targetDate);
            boolean matchesDeliveryDate = !dDate.isEmpty() && dDate.equals(targetDate);
            
            if (!matchesPickupDate && !matchesDeliveryDate) continue;
            total++;

            OrderStatus status = item.getStatus();
            
            // Pickup side
            if (matchesPickupDate && (status == OrderStatus.PENDING || status == OrderStatus.PICKING_PENDING || status == OrderStatus.PICKUP_DONE)) {
                pickups++;
            }
            // Delivery side
            if (matchesDeliveryDate && (status == OrderStatus.READY || status == OrderStatus.OUT_FOR_DELIVERY || status == OrderStatus.COMPLETED)) {
                deliveries++;
            }
            
            if ((matchesPickupDate || matchesDeliveryDate) && (status == OrderStatus.COMPLETED || status == OrderStatus.DELIVERED)) {
                completed++;
            }
        }

        if (reportPickupCount != null) reportPickupCount.setText(String.valueOf(pickups));
        if (reportDeliveryCount != null) reportDeliveryCount.setText(String.valueOf(deliveries));

        int percent = total > 0 ? (completed * 100) / total : 0;
        
        if (progress != null) progress.setProgress(percent, true);
        if (percentageText != null) percentageText.setText(percent + "%");
        if (summaryText != null) summaryText.setText(completed + " of " + total + " completed");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
    
    private static String normalizeDateStr(String date) {
        if (date == null) return "";
        String s = date.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
        if (s.startsWith("0") && s.length() > 1 && Character.isDigit(s.charAt(1))) {
            s = s.substring(1);
        }
        return s;
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
        private RecyclerView rv;
        private boolean isPickup;
        private ListenerRegistration pageListener;
        private final List<OrderItem> shownOrders = new ArrayList<>();
        private OrderAdapter adapter;

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
            isPickup = (type == 0);

            TextView label  = view.findViewById(R.id.section_label);
            TextView badge  = view.findViewById(R.id.section_badge);
            View accentBar  = view.findViewById(R.id.section_accent_bar);
            MaterialCardView badgeCard = (MaterialCardView) badge.getParent();

            label.setText(isPickup ? "Pickups" : "Deliveries");

            if (isPickup) {
                accentBar.setBackgroundColor(Color.parseColor("#06D6A0"));
                badge.setTextColor(Color.parseColor("#4361EE"));
                badgeCard.setCardBackgroundColor(Color.parseColor("#EEF2FF"));
            } else {
                accentBar.setBackgroundColor(Color.parseColor("#FF9F1C"));
                badge.setTextColor(Color.parseColor("#FF9F1C"));
                badgeCard.setCardBackgroundColor(Color.parseColor("#FFF7ED"));
            }

            rv = view.findViewById(R.id.report_page_recycler);
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            adapter = new OrderAdapter(shownOrders, !isPickup, this::onOrderClick);
            rv.setAdapter(adapter);

            attachFirebaseListener();
            return view;
        }

        private void attachFirebaseListener() {
            TaskViewModel viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
            
            viewModel.getOrderItems(getContext()).observe(getViewLifecycleOwner(), items -> {
                updateList(items, viewModel.getSelectedDate().getValue());
            });
            
            viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
                List<OrderItem> items = viewModel.getOrderItems(getContext()).getValue();
                if (items != null) updateList(items, date);
            });
        }
        
        private void updateList(List<OrderItem> items, String today) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                shownOrders.clear();
                
                String targetDate = normalizeDateStr(today);
                
                for (OrderItem item : items) {
                    String pDate = normalizeDateStr(item.getPickupDate());
                    String dDate = normalizeDateStr(item.getDeliveryDate());
                    
                    boolean matchesPickupDate = !pDate.isEmpty() && pDate.equals(targetDate);
                    boolean matchesDeliveryDate = !dDate.isEmpty() && dDate.equals(targetDate);
                    
                    if (isPickup) {
                        if (!matchesPickupDate) continue;
                    } else {
                        if (!matchesDeliveryDate) continue;
                    }
                    
                    // Resolve name if missing
                    if (item.getCustomerName().equals(item.getPhone())) {
                        FirebaseRepository.getInstance().fetchNameForPhone(item.getPhone(), name -> {
                            if (!name.equals("Unknown") && getActivity() != null) {
                                getActivity().runOnUiThread(() -> {
                                    for (int i = 0; i < shownOrders.size(); i++) {
                                        if (shownOrders.get(i).getPhone().equals(item.getPhone())) {
                                            shownOrders.set(i, shownOrders.get(i).copyWithName(name));
                                        }
                                    }
                                    if (adapter != null) adapter.notifyDataSetChanged();
                                });
                            }
                        });
                    }
                    
                    if (isPickup) {
                        shownOrders.add(item);
                    } else {
                        if (item.getStatus() == OrderStatus.READY || item.getStatus() == OrderStatus.OUT_FOR_DELIVERY 
                                || item.getStatus() == OrderStatus.COMPLETED || item.getStatus() == OrderStatus.PICKUP_DONE) {
                            shownOrders.add(item);
                        }
                    }
                }
                
                // Sort: Active/Pending first, Completed at the bottom
                java.util.Collections.sort(shownOrders, (a, b) -> {
                    boolean aDone = isPickup ? 
                        (a.getStatus() != OrderStatus.PENDING) : 
                        (a.getStatus() == OrderStatus.COMPLETED);
                    boolean bDone = isPickup ? 
                        (b.getStatus() != OrderStatus.PENDING) : 
                        (b.getStatus() == OrderStatus.COMPLETED);
                    if (aDone && !bDone) return 1;
                    if (!aDone && bDone) return -1;
                    return 0;
                });
                
                if (adapter != null) adapter.notifyDataSetChanged();
                TextView badge = getView() != null ? getView().findViewById(R.id.section_badge) : null;
                if (badge != null) badge.setText(String.valueOf(shownOrders.size()));
            });
        }

        private void onOrderClick(OrderItem order) {
            android.content.Intent intent;
            if (isPickup) {
                intent = new android.content.Intent(getActivity(), PickupDetailActivity.class);
                intent.putExtra("order_id", order.getId());
                intent.putExtra("order_path", order.getFullPath());
            } else {
                intent = new android.content.Intent(getActivity(), DeliveryDetailActivity.class);
                intent.putExtra("order_id", order.getFullPath());
                intent.putExtra("order_path", order.getFullPath());
            }
            intent.putExtra("read_only", order.getStatus() == OrderStatus.COMPLETED);
            startActivity(intent);
        }

        private OrderItem mapToOrderItem(Map<String, Object> doc) {
            String id      = FirebaseRepository.str(doc, "id");
            String phone   = FirebaseRepository.str(doc, "customerPhone");
            if (phone.isEmpty()) phone = FirebaseRepository.str(doc, "phone");
            String name    = FirebaseRepository.str(doc, "customerName");
            if (name.isEmpty()) name = FirebaseRepository.str(doc, "name");
            String address = FirebaseRepository.str(doc, "address");
            String soc     = FirebaseRepository.str(doc, "society");
            String statusStr = FirebaseRepository.str(doc, "status").toLowerCase();
            String path    = FirebaseRepository.str(doc, "__path");
            String incompleteReason = FirebaseRepository.str(doc, "incompleteReason");

            if (name.isEmpty()) name = phone.isEmpty() ? "Unknown Customer" : phone;
            if (soc.isEmpty() || soc.equals("All Societies")) {
                if (!address.isEmpty()) {
                    String[] parts = address.split(",");
                    soc = parts.length > 0 ? parts[0].trim() : "Residence";
                } else soc = "Residence";
            }

            OrderStatus status;
            if (statusStr.equals("pending") || statusStr.equals("picking pending")) status = OrderStatus.PENDING;
            else if (statusStr.equals("pickup_done") || statusStr.equals("washing") || statusStr.equals("ironing")) 
                status = OrderStatus.PICKUP_DONE;
            else if (statusStr.equals("ready")) status = OrderStatus.READY;
            else if (statusStr.equals("out_for_delivery")) status = OrderStatus.OUT_FOR_DELIVERY;
            else if (statusStr.equals("delivered") || statusStr.equals("completed")) 
                status = OrderStatus.COMPLETED;
            else if (statusStr.equals("incomplete")) status = OrderStatus.INCOMPLETE;
            else status = OrderStatus.PENDING;

            return new OrderItem(id, name, address, soc, phone, "", path, status, incompleteReason);
        }

        @Override
        public void onDestroyView() {
            if (pageListener != null) pageListener.remove();
            super.onDestroyView();
        }
    }
}
