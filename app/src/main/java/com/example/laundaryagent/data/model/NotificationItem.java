package com.example.laundaryagent.data.model;

public class NotificationItem {

    public enum Type { ORDER, PICKUP, ALERT, DONE, PROMO }

    private final String title;
    private final String message;
    private final String time;
    private final Type type;
    private boolean read;

    public NotificationItem(String title, String message, String time, Type type, boolean read) {
        this.title   = title;
        this.message = message;
        this.time    = time;
        this.type    = type;
        this.read    = read;
    }

    public String getTitle()   { return title; }
    public String getMessage() { return message; }
    public String getTime()    { return time; }
    public Type   getType()    { return type; }
    public boolean isRead()    { return read; }
    public void markRead()     { this.read = true; }
}
