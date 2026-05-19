package com.example.laundaryagent.data.model;

public class OrderItem {
    private final String id;
    private final String customerName;
    private final String address;
    private final String society;
    private final String phone;
    private final String time;
    private final String fullPath; // Firestore full path
    private OrderStatus status;
    private String incompleteReason;
    private String pickupDate;
    private String deliveryDate;

    private OrderServices services;

    public OrderItem(String id, String customerName, String address, String society, String phone, String time, String fullPath) {
        this.id = id;
        this.customerName = customerName;
        this.address = address;
        this.society = society;
        this.phone = phone;
        this.time = time;
        this.fullPath = fullPath;
        this.status = OrderStatus.PENDING;
        this.incompleteReason = null;
        this.pickupDate = "";
        this.deliveryDate = "";
        this.services = null;
    }

    public OrderItem(String id, String customerName, String address, String society, String phone, String time, String fullPath, OrderStatus status, String incompleteReason) {
        this.id = id;
        this.customerName = customerName;
        this.address = address;
        this.society = society;
        this.phone = phone;
        this.time = time;
        this.fullPath = fullPath;
        this.status = status;
        this.incompleteReason = incompleteReason;
        this.pickupDate = "";
        this.deliveryDate = "";
        this.services = null;
    }

    // Getters
    public String getId() { return id; }
    public String getCustomerName() { return customerName; }
    public String getAddress() { return address; }
    public String getSociety() { return society; }
    public String getPhone() { return phone; }
    public String getTime() { return time; }
    public String getFullPath() { return fullPath; }
    public OrderStatus getStatus() { return status; }
    public String getIncompleteReason() { return incompleteReason; }
    public OrderServices getServices() { return services; }

    // Setters
    public void setStatus(OrderStatus status) { this.status = status; }
    public void setIncompleteReason(String incompleteReason) { this.incompleteReason = incompleteReason; }
    public void setServices(OrderServices services) { this.services = services; }

    public String getPickupDate() { return pickupDate; }
    public void setPickupDate(String pickupDate) { this.pickupDate = pickupDate; }
    public String getDeliveryDate() { return deliveryDate; }
    public void setDeliveryDate(String deliveryDate) { this.deliveryDate = deliveryDate; }

    // Copy method for convenience
    public OrderItem copyWithName(String newName) {
        OrderItem item = new OrderItem(id, newName, address, society, phone, time, fullPath, status, incompleteReason);
        item.setPickupDate(this.pickupDate);
        item.setDeliveryDate(this.deliveryDate);
        item.setServices(this.services);
        return item;
    }

    public OrderItem copyWithStatus(OrderStatus newStatus) {
        OrderItem item = new OrderItem(id, customerName, address, society, phone, time, fullPath, newStatus, incompleteReason);
        item.setPickupDate(this.pickupDate);
        item.setDeliveryDate(this.deliveryDate);
        item.setServices(this.services);
        return item;
    }

    public OrderItem copyWithIncompleteReason(String reason) {
        OrderItem item = new OrderItem(id, customerName, address, society, phone, time, fullPath, OrderStatus.INCOMPLETE, reason);
        item.setServices(this.services);
        return item;
    }
}

