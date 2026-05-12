package com.example.laundaryagent;

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

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderStatus;
import com.example.laundaryagent.data.repository.LaundryRepository;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.List;

public class ReportsFragment extends Fragment {

    private LaundryRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_reports, container, false);

        repository = LaundryRepository.getInstance();

        RecyclerView pickupsRecycler = view.findViewById(R.id.report_pickups_recycler);
        pickupsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        List<OrderItem> pickups = repository.getPickupOrders();
        pickupsRecycler.setAdapter(new OrderAdapter(pickups, order -> {}));

        RecyclerView deliveriesRecycler = view.findViewById(R.id.report_deliveries_recycler);
        deliveriesRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        List<OrderItem> deliveries = repository.getDeliveryOrders();
        deliveriesRecycler.setAdapter(new OrderAdapter(deliveries, order -> {}));

        // Summary
        int total = pickups.size() + deliveries.size();
        int completed = 0;
        for (OrderItem o : pickups) {
            if (o.getStatus() == OrderStatus.COMPLETED) completed++;
        }
        for (OrderItem o : deliveries) {
            if (o.getStatus() == OrderStatus.COMPLETED) completed++;
        }

        CircularProgressIndicator progress = view.findViewById(R.id.report_circular_progress);
        TextView percentageText = view.findViewById(R.id.report_percentage_text);
        TextView summaryText = view.findViewById(R.id.report_tasks_summary);

        if (total > 0) {
            int percent = (int) ((completed * 100) / total);
            progress.setProgress(percent);
            percentageText.setText(percent + "%");
        } else {
            progress.setProgress(0);
            percentageText.setText("0%");
        }
        summaryText.setText(completed + " tasks completed");

        return view;
    }
}
