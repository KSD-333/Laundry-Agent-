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

import com.example.laundaryagent.data.model.NotificationItem;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.VH> {

    private final List<NotificationItem> items;

    public NotificationAdapter(List<NotificationItem> items) {
        this.items = items;
    }

    /** Mark every item as read and refresh */
    public void markAllRead() {
        for (NotificationItem n : items) n.markRead();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        h.bind(items.get(position));

        // Staggered entrance
        h.itemView.setAlpha(0f);
        h.itemView.setTranslationY(30f);
        h.itemView.animate()
                .alpha(1f).translationY(0f)
                .setDuration(320)
                .setStartDelay(position * 55L)
                .setInterpolator(new DecelerateInterpolator(1.4f))
                .start();
    }

    @Override
    public int getItemCount() { return items.size(); }

    // ── ViewHolder ────────────────────────────────────────────────────────
    static class VH extends RecyclerView.ViewHolder {

        final View       root, iconCircle, unreadDot;
        final ImageView  icon;
        final TextView   title, message, time;

        VH(@NonNull View v) {
            super(v);
            root       = v.findViewById(R.id.notif_root);
            iconCircle = v.findViewById(R.id.notif_icon_circle);
            icon       = v.findViewById(R.id.notif_icon);
            unreadDot  = v.findViewById(R.id.unread_dot);
            title      = v.findViewById(R.id.notif_title);
            message    = v.findViewById(R.id.notif_message);
            time       = v.findViewById(R.id.notif_time);
        }

        void bind(NotificationItem n) {
            title.setText(n.getTitle());
            message.setText(n.getMessage());
            time.setText(n.getTime());

            // Unread dot
            unreadDot.setVisibility(n.isRead() ? View.GONE : View.VISIBLE);

            // Card background
            root.setBackgroundResource(
                    n.isRead() ? R.drawable.bg_notif_card_read
                               : R.drawable.bg_notif_card_unread);

            // Title alpha
            title.setAlpha(n.isRead() ? 0.65f : 1f);

            // Icon + circle per type
            switch (n.getType()) {
                case ORDER:
                    iconCircle.setBackgroundResource(R.drawable.bg_notif_icon_blue);
                    icon.setImageResource(R.drawable.ic_notification_order);
                    break;
                case PICKUP:
                    iconCircle.setBackgroundResource(R.drawable.bg_notif_icon_green);
                    icon.setImageResource(R.drawable.ic_notification_pickup);
                    break;
                case ALERT:
                    iconCircle.setBackgroundResource(R.drawable.bg_notif_icon_orange);
                    icon.setImageResource(R.drawable.ic_notification_alert);
                    break;
                case DONE:
                    iconCircle.setBackgroundResource(R.drawable.bg_notif_icon_green);
                    icon.setImageResource(R.drawable.ic_notification_done);
                    break;
                case PROMO:
                    iconCircle.setBackgroundResource(R.drawable.bg_notif_icon_purple);
                    icon.setImageResource(R.drawable.ic_notification_promo);
                    break;
            }

            // Tap: mark read + scale feedback
            itemView.setOnClickListener(v -> {
                if (!n.isRead()) {
                    n.markRead();
                    unreadDot.setVisibility(View.GONE);
                    root.setBackgroundResource(R.drawable.bg_notif_card_read);
                    title.animate().alpha(0.65f).setDuration(200).start();
                }
                v.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80)
                        .withEndAction(() ->
                                v.animate().scaleX(1f).scaleY(1f).setDuration(120).start())
                        .start();
            });
        }
    }
}
