package com.example.laundaryagent;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderStatus;
import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class TasksFragment extends Fragment {

    private ViewPager2 viewPager;
    private String selectedSociety = "All Societies";
    private TextView societyText, doneCountText, pendingCountText, dateText;
    private TaskViewModel viewModel;
    private ListenerRegistration badgeListenerReg;
    private TextView notificationBadge;

    private final List<String> ALL_SOCIETIES = Arrays.asList(
            "All Societies",
            "Amanora Park Town",
            "Magarpatta City",
            "Blue Ridge Town",
            "EON Waterfront",
            "Riverdale Residences",
            "Godrej Infinity",
            "Yoo Pune"
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tasks, container, false);

        societyText    = view.findViewById(R.id.society_text);
        doneCountText  = view.findViewById(R.id.done_count);
        pendingCountText = view.findViewById(R.id.pending_count);
        dateText       = view.findViewById(R.id.current_date_text);
        notificationBadge = view.findViewById(R.id.notification_badge);

        TextView agentName = view.findViewById(R.id.tv_agent_name);
        android.content.SharedPreferences prefs = requireActivity()
                .getSharedPreferences("LaundryPrefs", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("user_identity", "Agent");
        if (agentName != null) agentName.setText(name);

        viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
        viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> {
            if (dateText != null) dateText.setText("Date: " + date);
            
            // Re-trigger global badge listener for the selected date
            if (badgeListenerReg != null) badgeListenerReg.remove();
            badgeListenerReg = FirebaseRepository.getInstance().listenPendingOrders(date, count -> {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    if (notificationBadge != null) {
                        notificationBadge.setText(String.valueOf(count));
                        notificationBadge.setVisibility(count > 0 ? View.VISIBLE : View.GONE);
                    }
                });
            });
        });

        viewPager = view.findViewById(R.id.tasks_view_pager);
        TaskPagerAdapter pagerAdapter = new TaskPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);
        viewPager.setOffscreenPageLimit(1); 

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
            tab.setText(position == 0 ? "Pickup Tasks" : "Delivery Tasks")
        ).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateHeaderCounts(0, 0);
            }
        });

        view.findViewById(R.id.society_dropdown_card).setOnClickListener(v -> showSocietyPicker());
        view.findViewById(R.id.btn_change_date).setOnClickListener(v -> showDatePicker());
        view.findViewById(R.id.map_location_button).setOnClickListener(v -> openSocietyMap());
        view.findViewById(R.id.notification_card).setOnClickListener(v ->
            startActivity(new Intent(getActivity(), NotificationsActivity.class)));

        return view;
    }

    private void showDatePicker() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        new android.app.DatePickerDialog(requireContext(), (view, year, month, dayOfMonth) -> {
            cal.set(year, month, dayOfMonth);
            // Force US locale for consistency with database "May" vs "may"
            String newDate = new java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.US).format(cal.getTime());
            viewModel.setSelectedDate(newDate);
        }, cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH), cal.get(java.util.Calendar.DAY_OF_MONTH)).show();
    }

    // Called by child fragments when their count changes
    public void updateHeaderCounts(int done, int pending) {
        if (doneCountText != null) doneCountText.setText(String.valueOf(done));
        if (pendingCountText != null) pendingCountText.setText(String.valueOf(pending));
    }

    private void showSocietyPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
        View picker = getLayoutInflater().inflate(R.layout.layout_society_picker, null);

        EditText searchEdit = picker.findViewById(R.id.society_search_edit);
        RecyclerView pickerRecycler = picker.findViewById(R.id.society_recycler);
        pickerRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        List<String> displayList = new ArrayList<>(ALL_SOCIETIES);
        SocietyPickerAdapter pickerAdapter = new SocietyPickerAdapter(displayList, s -> {
            selectedSociety = s;
            societyText.setText(s);
            viewPager.setAdapter(new TaskPagerAdapter(this));
            dialog.dismiss();
        });
        pickerRecycler.setAdapter(pickerAdapter);

        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> filtered = new ArrayList<>();
                for (String item : ALL_SOCIETIES) {
                    if (item.toLowerCase().contains(s.toString().toLowerCase())) filtered.add(item);
                }
                pickerAdapter.update(filtered);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        dialog.setContentView(picker);
        dialog.show();
    }

    private void openSocietyMap() {
        if (selectedSociety.equals("All Societies")) {
            Toast.makeText(getContext(), "Please select a specific society", Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = Uri.parse("geo:0,0?q=" + Uri.encode(selectedSociety));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null)
            startActivity(mapIntent);
        else
            Toast.makeText(getContext(), "Maps app not found", Toast.LENGTH_SHORT).show();
    }

    // ── Pager Adapter ──────────────────────────────────────────────────────

    private static class TaskPagerAdapter extends FragmentStateAdapter {
        private final TasksFragment parent;
        TaskPagerAdapter(TasksFragment fragment) {
            super(fragment);
            this.parent = fragment;
        }
        @NonNull
        @Override
        public Fragment createFragment(int position) {
            return TaskListPageFragment.newInstance(position, parent.selectedSociety);
        }
        @Override
        public int getItemCount() { return 2; }
    }

    // ── Task List Page Fragment (Firebase-backed) ──────────────────────────

    public static class TaskListPageFragment extends Fragment {
        private int tabType;      // 0 = Pickup Tasks, 1 = Delivery Tasks
        private String society;

        // Fired-order lists from Firebase
        private final List<OrderItem> allOrders   = new ArrayList<>();
        private final List<OrderItem> shownOrders = new ArrayList<>();
        private OrderAdapter adapter;
        private RecyclerView rv;
        private TaskViewModel viewModel;

        public static TaskListPageFragment newInstance(int type, String society) {
            TaskListPageFragment f = new TaskListPageFragment();
            Bundle args = new Bundle();
            args.putInt("type", type);
            args.putString("society", society);
            f.setArguments(args);
            return f;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                                 @Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            tabType = args != null ? args.getInt("type") : 0;
            society = args != null ? args.getString("society", "All Societies") : "All Societies";

            rv = new RecyclerView(requireContext());
            rv.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            rv.setLayoutManager(new LinearLayoutManager(requireContext()));
            rv.setPadding(48, 24, 48, 220);
            rv.setClipToPadding(false);

            adapter = new OrderAdapter(shownOrders, tabType == 1, this::onOrderClick);
            rv.setAdapter(adapter);

            viewModel = new ViewModelProvider(requireActivity()).get(TaskViewModel.class);
            viewModel.getOrderItems(getContext()).observe(getViewLifecycleOwner(), this::onOrdersReceived);
            viewModel.getSelectedDate().observe(getViewLifecycleOwner(), date -> applyFilter());
            
            return rv;
        }

        private void onOrdersReceived(List<OrderItem> items) {
            if (getActivity() == null) return;
            requireActivity().runOnUiThread(() -> {
                allOrders.clear();
                allOrders.addAll(items);
                applyFilter();
            });
        }

        private void applyFilter() {
            if (allOrders == null || allOrders.isEmpty()) {
                adapter.notifyDataSetChanged();
                return;
            }
            
            shownOrders.clear();
            int done = 0, pending = 0;

            String today = viewModel.getSelectedDate().getValue();
            if (today == null) today = "";

            for (OrderItem o : allOrders) {
                boolean matchesSociety = society.equals("All Societies") || society.equals(o.getSociety());
                if (!matchesSociety) continue;

                boolean isPickupTab = (tabType == 0);
                String rawDate = isPickupTab ? o.getPickupDate() : o.getDeliveryDate();
                
                // Aggressive normalization: "18 May 2026" -> "18 may 2026", "18/05/2026" -> "18 05 2026"
                String orderDate = rawDate.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
                String targetDate = today.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();

                // Strictly filter for selected date. If date is missing, skip it.
                if (orderDate.isEmpty() || !orderDate.equals(targetDate)) continue;

                if (isPickupTab) {
                    // Pickup Tab: show pending pickups OR completed pickups for today
                    boolean isPendingPickup = (o.getStatus() == OrderStatus.PENDING || o.getStatus() == OrderStatus.PICKING_PENDING);
                    boolean isDonePickup = (o.getStatus() != OrderStatus.PENDING && o.getStatus() != OrderStatus.PICKING_PENDING);
                    
                    if (isPendingPickup) pending++;
                    else if (isDonePickup) done++;
                    
                    shownOrders.add(o);
                } else {
                    // Delivery Tab: Show ALL orders that match the delivery date
                    boolean isDone = (o.getStatus() == OrderStatus.COMPLETED || o.getStatus() == OrderStatus.DELIVERED);
                    
                    if (isDone) done++;
                    else pending++;
                    
                    shownOrders.add(o);
                }
            }
            
            // Update the counts in the header
            if (getParentFragment() instanceof TasksFragment) {
                ((TasksFragment) getParentFragment()).updateHeaderCounts(done, pending);
            }
            
            // Sort: Active/Pending first, Completed at the bottom
            java.util.Collections.sort(shownOrders, (a, b) -> {
                boolean isPickupTab = (tabType == 0);
                boolean aDone = isPickupTab ? 
                    (a.getStatus() != OrderStatus.PENDING && a.getStatus() != OrderStatus.PICKING_PENDING) : 
                    (a.getStatus() == OrderStatus.COMPLETED || a.getStatus() == OrderStatus.DELIVERED);
                boolean bDone = isPickupTab ? 
                    (b.getStatus() != OrderStatus.PENDING && b.getStatus() != OrderStatus.PICKING_PENDING) : 
                    (b.getStatus() == OrderStatus.COMPLETED || b.getStatus() == OrderStatus.DELIVERED);
                if (aDone && !bDone) return 1;
                if (!aDone && bDone) return -1;
                return 0;
            });

            if (adapter != null) adapter.notifyDataSetChanged();
        }

        private void onOrderClick(OrderItem order) {
            Intent intent;
            if (tabType == 0) {
                intent = new Intent(getActivity(), PickupDetailActivity.class);
                intent.putExtra("order_path", order.getFullPath());
                intent.putExtra("order_id", order.getId());
            } else {
                intent = new Intent(getActivity(), DeliveryDetailActivity.class);
                // DeliveryDetailActivity expects the full path in "order_id" extra
                intent.putExtra("order_id", order.getFullPath());
            }
            intent.putExtra("order_phone", order.getPhone());
            intent.putExtra("read_only", order.getStatus() == OrderStatus.COMPLETED);
            startActivity(intent);
        }

        @Override
        public void onDestroyView() {
            // Listener cleanup handled by ViewModel
            super.onDestroyView();
        }
    }

    // ── Society Picker Adapter ─────────────────────────────────────────────

    private static class SocietyPickerAdapter extends RecyclerView.Adapter<SocietyPickerAdapter.ViewHolder> {
        private List<String> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener { void onItemClick(String s); }

        SocietyPickerAdapter(List<String> items, OnItemClickListener listener) {
            this.items = items; this.listener = listener;
        }

        void update(List<String> newItems) { this.items = newItems; notifyDataSetChanged(); }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_society_picker, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String s = items.get(position);
            holder.text.setText(s);
            holder.itemView.setOnClickListener(v -> listener.onItemClick(s));
        }

        @Override public int getItemCount() { return items.size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View v) {
                super(v);
                text = v.findViewById(R.id.society_item_name);
            }
        }
    }
}
