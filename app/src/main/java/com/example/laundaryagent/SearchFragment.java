package com.example.laundaryagent;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.repository.LaundryRepository;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private RecyclerView recyclerView;
    private OrderAdapter adapter;
    private LaundryRepository repository;
    private List<OrderItem> allOrders;
    private View emptyState;
    private TextView emptyText;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        repository = LaundryRepository.getInstance();
        allOrders = new ArrayList<>();
        allOrders.addAll(repository.getPickupOrders());
        allOrders.addAll(repository.getDeliveryOrders());

        recyclerView = view.findViewById(R.id.search_results_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        emptyState = view.findViewById(R.id.search_empty_state);
        emptyText = view.findViewById(R.id.search_empty_text);

        TextInputEditText searchInput = view.findViewById(R.id.search_input);
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Initialize with empty state
        recyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        
        return view;
    }

    private void filter(String query) {
        if (query.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyText.setText("Search for orders or customers");
            return;
        }

        String lowerQuery = query.toLowerCase();
        List<OrderItem> filtered = new ArrayList<>();
        for (OrderItem o : allOrders) {
            if (o.getCustomerName().toLowerCase().contains(lowerQuery) ||
                o.getAddress().toLowerCase().contains(lowerQuery) ||
                o.getSociety().toLowerCase().contains(lowerQuery)) {
                filtered.add(o);
            }
        }

        if (filtered.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
            emptyText.setText("No results found for \"" + query + "\"");
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
            adapter = new OrderAdapter(filtered, order -> {
                Intent intent;
                if (order.getId().startsWith("P")) {
                    intent = new Intent(getActivity(), PickupDetailActivity.class);
                } else {
                    intent = new Intent(getActivity(), DeliveryDetailActivity.class);
                }
                intent.putExtra("order_id", order.getId());
                startActivity(intent);
            });
            recyclerView.setAdapter(adapter);
        }
    }
}
