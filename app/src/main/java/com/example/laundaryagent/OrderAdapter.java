package com.example.laundaryagent;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
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
    private boolean isDeliveryView = false;

    public interface OnOrderClickListener {
        void onOrderClick(OrderItem order);
    }

    public OrderAdapter(List<OrderItem> orders, OnOrderClickListener listener) {
        this.orders = orders;
        this.listener = listener;
        this.isDeliveryView = false;
    }

    public OrderAdapter(List<OrderItem> orders, boolean isDeliveryView, OnOrderClickListener listener) {
        this.orders = orders;
        this.isDeliveryView = isDeliveryView;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position), isDeliveryView, listener);
    }

    @Override
    public int getItemCount() { return orders.size(); }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView customerName, societyName, statusText, initialsText;
        MaterialCardView statusBadgeContainer, cardRoot;
        View statusDotBg, accentBar;
        ImageView chevronIcon;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName         = itemView.findViewById(R.id.customer_name);
            societyName          = itemView.findViewById(R.id.society_name_list);
            statusText           = itemView.findViewById(R.id.status_text);
            initialsText         = itemView.findViewById(R.id.initials_text);
            // Removed order_id_list from constructor
            statusBadgeContainer = itemView.findViewById(R.id.status_badge_container);
            statusDotBg          = itemView.findViewById(R.id.status_dot_bg);
            accentBar            = itemView.findViewById(R.id.accent_bar);
            cardRoot             = itemView.findViewById(R.id.card_root);
            chevronIcon          = itemView.findViewById(R.id.chevron_icon);
        }

        void bind(OrderItem order, boolean isDeliveryView, OnOrderClickListener listener) {
            customerName.setText(order.getCustomerName());
            societyName.setText(order.getSociety());
            
            // Removed order_id_list binding to declutter UI per user request

            boolean isDone = isDeliveryView ? 
                (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.DELIVERED) : 
                (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.PICKING_PENDING);

            if (isDone) {
                // ── Completed: muted visual, but still navigable ──────────
                int grey = Color.parseColor("#06D6A0");

                if (accentBar != null) accentBar.setBackgroundColor(grey);
                if (statusDotBg != null) statusDotBg.setBackgroundResource(R.drawable.bg_avatar_done);
                initialsText.setTextColor(grey);

                statusText.setText(isDeliveryView ? "Completed" : "Picked Up");
                statusText.setTextColor(grey);
                statusBadgeContainer.setCardBackgroundColor(withAlpha(grey, 0.12f));

                // Dim the whole card
                if (cardRoot != null) {
                    cardRoot.setAlpha(0.6f);
                    cardRoot.setCardElevation(0f);
                }

                // Show chevron — still navigable (read-only view)
                if (chevronIcon != null) chevronIcon.setVisibility(View.VISIBLE);

                // Navigate but pass read-only flag
                itemView.setOnClickListener(v -> {
                    v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                            .withEndAction(() ->
                                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                            .start();
                    listener.onOrderClick(order);
                });

            } else {
                // ── Pending: full color, interactive ─────────────────────
                int accentColor = Color.parseColor("#4361EE");
                int badgeColor  = Color.parseColor("#FF9F1C");

                if (accentBar != null) accentBar.setBackgroundColor(accentColor);
                if (statusDotBg != null) statusDotBg.setBackgroundResource(R.drawable.bg_avatar_pending);
                initialsText.setTextColor(accentColor);

                statusText.setText("Pending");
                statusText.setTextColor(badgeColor);
                statusBadgeContainer.setCardBackgroundColor(withAlpha(badgeColor, 0.12f));

                // Full opacity
                if (cardRoot != null) {
                    cardRoot.setAlpha(1f);
                    cardRoot.setCardElevation(4f);
                }

                // Show chevron
                if (chevronIcon != null) chevronIcon.setVisibility(View.VISIBLE);

                // Press + navigate
                itemView.setOnClickListener(v -> {
                    v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80)
                            .withEndAction(() ->
                                    v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                            .start();
                    listener.onOrderClick(order);
                });
            }

            // Initials
            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                String firstChar = String.valueOf(order.getCustomerName().charAt(0));
                if (firstChar.matches("[0-9]")) {
                    initialsText.setText("U");
                } else {
                    initialsText.setText(firstChar.toUpperCase());
                }
            }
        }

        private int withAlpha(int color, float alpha) {
            return Color.argb(Math.round(255 * alpha),
                    Color.red(color), Color.green(color), Color.blue(color));
        }
    }
}
