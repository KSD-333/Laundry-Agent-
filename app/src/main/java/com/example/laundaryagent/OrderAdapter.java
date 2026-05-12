package com.example.laundaryagent;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
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
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        holder.bind(orders.get(position), listener);

        // Staggered slide-up entrance
        holder.itemView.setAlpha(0f);
        holder.itemView.setTranslationY(40f);
        holder.itemView.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(350)
                .setStartDelay(position * 70L)
                .setInterpolator(new DecelerateInterpolator(1.5f))
                .start();
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView customerName, societyName, time, statusText, initialsText;
        MaterialCardView statusBadgeContainer;
        View statusDotBg, accentBar;

        OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            customerName        = itemView.findViewById(R.id.customer_name);
            societyName         = itemView.findViewById(R.id.society_name_list);
            time                = itemView.findViewById(R.id.time);
            statusText          = itemView.findViewById(R.id.status_text);
            initialsText        = itemView.findViewById(R.id.initials_text);
            statusBadgeContainer = itemView.findViewById(R.id.status_badge_container);
            statusDotBg         = itemView.findViewById(R.id.status_dot_bg);
            accentBar           = itemView.findViewById(R.id.accent_bar);
        }

        void bind(OrderItem order, OnOrderClickListener listener) {
            customerName.setText(order.getCustomerName());
            societyName.setText(order.getSociety());
            time.setText(order.getTime());

            boolean isDone = order.getStatus() == OrderStatus.COMPLETED;

            // Accent bar color
            int accentColor = isDone
                    ? Color.parseColor("#06D6A0")   // teal-green
                    : Color.parseColor("#4361EE");  // indigo

            // Badge color
            int badgeColor = isDone
                    ? Color.parseColor("#06D6A0")
                    : Color.parseColor("#FF9F1C");  // amber

            // Apply accent bar
            if (accentBar != null) accentBar.setBackgroundColor(accentColor);

            // Avatar background
            if (statusDotBg != null) {
                statusDotBg.setBackgroundResource(
                        isDone ? R.drawable.bg_avatar_done : R.drawable.bg_avatar_pending);
            }

            // Initials
            initialsText.setTextColor(accentColor);
            if (order.getCustomerName() != null && !order.getCustomerName().isEmpty()) {
                initialsText.setText(
                        String.valueOf(order.getCustomerName().charAt(0)).toUpperCase());
            }

            // Status badge
            String label = isDone ? "Completed" : "Pending";
            statusText.setText(label);
            statusText.setTextColor(badgeColor);
            statusBadgeContainer.setCardBackgroundColor(withAlpha(badgeColor, 0.13f));

            // Time chip color
            time.setTextColor(accentColor);

            // Press animation
            itemView.setOnClickListener(v -> {
                v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(90)
                        .withEndAction(() ->
                                v.animate().scaleX(1f).scaleY(1f).setDuration(140).start())
                        .start();
                listener.onOrderClick(order);
            });
        }

        private int withAlpha(int color, float alpha) {
            return Color.argb(
                    Math.round(255 * alpha),
                    Color.red(color),
                    Color.green(color),
                    Color.blue(color));
        }
    }
}
