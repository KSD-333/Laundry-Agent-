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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderStatus;
import com.example.laundaryagent.data.repository.LaundryRepository;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TasksFragment extends Fragment {

    private ViewPager2 viewPager;
    private LaundryRepository repository;
    private String selectedSociety = "All Societies";
    
    private TextView societyText;
    private TextView doneCountText;
    private TextView pendingCountText;

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

        repository = LaundryRepository.getInstance();
        societyText = view.findViewById(R.id.society_text);
        doneCountText = view.findViewById(R.id.done_count);
        pendingCountText = view.findViewById(R.id.pending_count);
        TextView agentName = view.findViewById(R.id.tv_agent_name);

        android.content.SharedPreferences prefs = getActivity().getSharedPreferences("LaundryPrefs", android.content.Context.MODE_PRIVATE);
        String name = prefs.getString("user_identity", "Agent John");
        agentName.setText(name);

        viewPager = view.findViewById(R.id.tasks_view_pager);
        TaskPagerAdapter pagerAdapter = new TaskPagerAdapter(this);
        viewPager.setAdapter(pagerAdapter);

        TabLayout tabLayout = view.findViewById(R.id.tab_layout);
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "Pickup Tasks" : "Delivery Tasks");
        }).attach();

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateCounts();
            }
        });

        // Hook up Society Filter Dropdown
        view.findViewById(R.id.society_dropdown_card).setOnClickListener(v -> showSocietyPicker());

        // Hook up Map Icon
        view.findViewById(R.id.map_location_button).setOnClickListener(v -> openSocietyMap());

        updateCounts();
        return view;
    }

    private void showSocietyPicker() {
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View picker = getLayoutInflater().inflate(R.layout.layout_society_picker, null);
        
        EditText searchEdit = picker.findViewById(R.id.society_search_edit);
        RecyclerView pickerRecycler = picker.findViewById(R.id.society_recycler);
        pickerRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        
        List<String> displayList = new ArrayList<>(ALL_SOCIETIES);
        SocietyPickerAdapter pickerAdapter = new SocietyPickerAdapter(displayList, s -> {
            setSociety(s);
            dialog.dismiss();
        });
        pickerRecycler.setAdapter(pickerAdapter);
        
        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                List<String> filtered = new ArrayList<>();
                for (String item : ALL_SOCIETIES) {
                    if (item.toLowerCase().contains(s.toString().toLowerCase())) {
                        filtered.add(item);
                    }
                }
                pickerAdapter.update(filtered);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        dialog.setContentView(picker);
        dialog.show();
    }

    private void setSociety(String society) {
        this.selectedSociety = society;
        societyText.setText(society);
        // Refresh the ViewPager content
        viewPager.setAdapter(new TaskPagerAdapter(this));
        updateCounts();
    }

    private void openSocietyMap() {
        if (selectedSociety.equals("All Societies")) {
            Toast.makeText(getContext(), "Please select a specific society to view on map", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(selectedSociety));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        if (mapIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            Toast.makeText(getContext(), "Maps app not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateCounts() {
        if (viewPager == null || doneCountText == null || pendingCountText == null) return;
        
        int currentTab = viewPager.getCurrentItem();
        List<OrderItem> orders;
        if (currentTab == 0) {
            orders = repository.getFilteredPickups(selectedSociety);
        } else {
            orders = repository.getFilteredDeliveries(selectedSociety);
        }

        int done = 0;
        int pending = 0;
        for (OrderItem order : orders) {
            if (order.getStatus() == OrderStatus.COMPLETED) {
                done++;
            } else {
                pending++;
            }
        }
        doneCountText.setText(String.valueOf(done));
        pendingCountText.setText(String.valueOf(pending));
    }

    // Pager Adapter
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
        public int getItemCount() {
            return 2;
        }
    }

    // Inner Fragment for the list page
    public static class TaskListPageFragment extends Fragment {
        private int tabType;
        private String society;

        public static TaskListPageFragment newInstance(int type, String society) {
            TaskListPageFragment fragment = new TaskListPageFragment();
            Bundle args = new Bundle();
            args.putInt("type", type);
            args.putString("society", society);
            fragment.setArguments(args);
            return fragment;
        }

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            Bundle args = getArguments();
            tabType = args != null ? args.getInt("type") : 0;
            society = args != null ? args.getString("society") : "All Societies";
            if (society == null) society = "All Societies";

            RecyclerView rv = new RecyclerView(getContext());
            rv.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            rv.setLayoutManager(new LinearLayoutManager(getContext()));
            rv.setPadding(40, 20, 40, 200);
            rv.setClipToPadding(false);

            List<OrderItem> orders;
            if (tabType == 0) {
                orders = LaundryRepository.getInstance().getFilteredPickups(society);
            } else {
                orders = LaundryRepository.getInstance().getFilteredDeliveries(society);
            }

            OrderAdapter adapter = new OrderAdapter(orders, order -> {
                Intent intent;
                if (tabType == 0) {
                    intent = new Intent(getActivity(), PickupDetailActivity.class);
                } else {
                    intent = new Intent(getActivity(), DeliveryDetailActivity.class);
                }
                intent.putExtra("order_id", order.getId());
                startActivity(intent);
            });
            rv.setAdapter(adapter);

            return rv;
        }
    }

    // Inner class for Society Picker Adapter (already static)
    private static class SocietyPickerAdapter extends RecyclerView.Adapter<SocietyPickerAdapter.ViewHolder> {
        private List<String> items;
        private final OnItemClickListener listener;

        interface OnItemClickListener {
            void onItemClick(String s);
        }

        SocietyPickerAdapter(List<String> items, OnItemClickListener listener) {
            this.items = items;
            this.listener = listener;
        }

        void update(List<String> newItems) {
            this.items = newItems;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_society_picker, parent, false);
            return new ViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            String s = items.get(position);
            holder.text.setText(s);
            holder.itemView.setOnClickListener(v -> listener.onItemClick(s));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView text;
            ViewHolder(View v) {
                super(v);
                text = v.findViewById(R.id.society_item_name);
            }
        }
    }
}
