package com.example.laundaryagent;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderStatus;
import com.google.android.material.card.MaterialCardView;

import java.util.List;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.OrderViewHolder> {

    private final List<OrderItem> orders;
    private final OnOrderClickListener listener;

    public interface OnOrderClickListener {
        void onOrderClick(OrderItem order);
    }

    public OrderAdapter(List<OrderItem> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView customerName;
        TextView societyName;
        TextView time;
        TextView statusText;
        TextView initialsText;
        MaterialCardView statusBadgeContainer;
        View statusDotBg;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName = itemView.findViewById(R.id.customer_name);
            societyName = itemView.findViewById(R.id.society_name_list);
            time = itemView.findViewById(R.id.time);
            statusText = itemView.findViewById(R.id.status_text);
            initialsText = itemView.findViewById(R.id.initials_text);
            statusBadgeContainer = itemView.findViewById(R.id.status_badge_container);
            statusDotBg = itemView.findViewById(R.id.status_dot_bg);
        }

        public void bind(OrderItem order, OnOrderClickListener listener) {
            customerName.setText(order.getCustomerName());
            societyName.setText(order.getSociety());
            time.setText(order.getTime());
            
            String statusLabel = "Pending";
            int color = Color.parseColor("#F97316"); // Orange for Pending
            
            if (order.getStatus() == OrderStatus.COMPLETED) {
                statusLabel = "Done";
                color = Color.parseColor("#10B981"); // Green for Done
            }

            statusText.setText(statusLabel);
            statusText.setTextColor(color);
            statusBadgeContainer.setCardBackgroundColor(adjustAlpha(color, 0.15f));

            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                initialsText.setText(String.valueOf(order.getCustomerName().charAt(0)).toUpperCase());
            }

            itemView.setOnClickListener(v -> listener.onOrderClick(order));
        }

        private int adjustAlpha(int color, float factor) {
            int alpha = Math.round(Color.alpha(color) * factor);
            int red = Color.red(color);
            int green = Color.green(color);
            int blue = Color.blue(color);
            return Color.argb(alpha, red, green, blue);
        }
    }
}
