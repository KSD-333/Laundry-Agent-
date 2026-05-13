package com.example.laundaryagent;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.laundaryagent.data.model.NotificationItem;
import com.example.laundaryagent.data.model.NotificationItem.Type;

import java.util.ArrayList;
import java.util.List;

public class NotificationsActivity extends AppCompatActivity {

    private NotificationAdapter todayAdapter;
    private NotificationAdapter earlierAdapter;
    private List<NotificationItem> todayList;
    private List<NotificationItem> earlierList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifications);

        // Back button
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        // Mark all read button
        findViewById(R.id.mark_all_btn).setOnClickListener(v -> {
            todayAdapter.markAllRead();
            earlierAdapter.markAllRead();
            updateUnreadCount();
        });

        // Build data
        todayList   = buildTodayNotifications();
        earlierList = buildEarlierNotifications();

        // Today recycler
        RecyclerView rvToday = findViewById(R.id.recycler_today);
        rvToday.setLayoutManager(new LinearLayoutManager(this));
        todayAdapter = new NotificationAdapter(todayList);
        rvToday.setAdapter(todayAdapter);

        // Earlier recycler
        RecyclerView rvEarlier = findViewById(R.id.recycler_earlier);
        rvEarlier.setLayoutManager(new LinearLayoutManager(this));
        earlierAdapter = new NotificationAdapter(earlierList);
        rvEarlier.setAdapter(earlierAdapter);

        updateUnreadCount();
    }

    private void updateUnreadCount() {
        int unread = 0;
        for (NotificationItem n : todayList)   if (!n.isRead()) unread++;
        for (NotificationItem n : earlierList) if (!n.isRead()) unread++;

        TextView tvUnread  = findViewById(R.id.tv_unread_count);
        TextView statUnread = findViewById(R.id.stat_unread);
        TextView statToday  = findViewById(R.id.stat_today);

        tvUnread.setText(unread > 0 ? unread + " unread" : "All caught up");
        statUnread.setText(String.valueOf(unread));
        statToday.setText(String.valueOf(todayList.size()));
    }

    // ── Sample data ───────────────────────────────────────────────────────

    private List<NotificationItem> buildTodayNotifications() {
        List<NotificationItem> list = new ArrayList<>();
        list.add(new NotificationItem(
                "New Pickup Assigned",
                "Rahul Sharma · Amanora Park Town · 09:30 AM",
                "2 min ago", Type.ORDER, false));
        list.add(new NotificationItem(
                "Pickup Reminder",
                "Anjali Deshmukh · Magarpatta City · 10:15 AM",
                "15 min ago", Type.PICKUP, false));
        list.add(new NotificationItem(
                "Route Updated",
                "Your pickup route has been optimised for today",
                "32 min ago", Type.ALERT, false));
        list.add(new NotificationItem(
                "Delivery Completed",
                "Priya Singh · Blue Ridge Town marked as done",
                "1 hr ago", Type.DONE, false));
        list.add(new NotificationItem(
                "New Delivery Assigned",
                "Sandeep Patil · Magarpatta City · 01:30 PM",
                "1 hr ago", Type.ORDER, false));
        list.add(new NotificationItem(
                "Bonus Unlocked",
                "Complete 3 more tasks today to earn ₹150 bonus",
                "2 hrs ago", Type.PROMO, false));
        list.add(new NotificationItem(
                "Pickup Overdue",
                "Vikram Malhotra · Blue Ridge Town · 08:00 AM",
                "3 hrs ago", Type.ALERT, false));
        list.add(new NotificationItem(
                "Delivery Reminder",
                "Megha Rao · Amanora Park Town · 04:00 PM",
                "3 hrs ago", Type.PICKUP, false));
        list.add(new NotificationItem(
                "New Pickup Assigned",
                "Sneha Kulkarni · Amanora Park Town · 02:00 PM",
                "4 hrs ago", Type.ORDER, false));
        return list;
    }

    private List<NotificationItem> buildEarlierNotifications() {
        List<NotificationItem> list = new ArrayList<>();
        list.add(new NotificationItem(
                "Weekly Performance",
                "You completed 42 tasks last week. Great work!",
                "Yesterday", Type.DONE, true));
        list.add(new NotificationItem(
                "Schedule Updated",
                "Your duty schedule for next week is now available",
                "Yesterday", Type.ALERT, true));
        list.add(new NotificationItem(
                "Incentive Credited",
                "₹320 incentive has been added to your account",
                "2 days ago", Type.PROMO, true));
        list.add(new NotificationItem(
                "New Area Assigned",
                "EON Waterfront has been added to your zone",
                "3 days ago", Type.ORDER, true));
        list.add(new NotificationItem(
                "App Update Available",
                "Version 2.1 is available with new features",
                "5 days ago", Type.ALERT, true));
        return list;
    }
}
