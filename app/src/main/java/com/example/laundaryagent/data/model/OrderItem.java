package com.example.laundaryagent.data.model;

public class OrderItem {
    private final String id;
    private final String customerName;
    private final String address;
    private final String society;
    private final String phone;
    private final String time;
    private OrderStatus status;
    private String incompleteReason;

    public OrderItem(String id, String customerName, String address, String society, String phone, String time) {
        this.id = id;
        this.customerName = customerName;
        this.address = address;
        this.society = society;
        this.phone = phone;
        this.time = time;
        this.status = OrderStatus.PENDING;
        this.incompleteReason = null;
    }

    public OrderItem(String id, String customerName, String address, String society, String phone, String time, OrderStatus status, String incompleteReason) {
        this.id = id;
        this.customerName = customerName;
        this.address = address;
        this.society = society;
        this.phone = phone;
        this.time = time;
        this.status = status;
        this.incompleteReason = incompleteReason;
    }

    // Getters
    public String getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getAddress() { return address; }
    public String getSociety() { return society; }
    public String getPhone() { return phone; }
    public String getTime() { return time; }
    public OrderStatus getStatus() { return status; }
    public String getIncompleteReason() { return incompleteReason; }

    // Setters
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setIncompleteReason(String incompleteReason) { this.incompleteReason = incompleteReason; }

    // Copy method for convenience (to match Kotlin's copy)
    public OrderItem copyWithStatus(OrderStatus newStatus) {
        return new OrderItem(id, customerName, address, society, phone, time, newStatus, incompleteReason);
    }

    public OrderItem copyWithIncompleteReason(String reason) {
        return new OrderItem(id, customerName, address, society, phone, time, OrderStatus.INCOMPLETE, reason);
    }
}
