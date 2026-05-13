package com.example.laundaryagent.data.model;

public class ServiceItem {
    private final String name;
    private int quantity;

    public ServiceItem(String name, int quantity) {
        this.name     = name;
        this.quantity = quantity;
    }

    public String getName()     { return name; }
    public int    getQuantity() { return quantity; }
    public void   setQuantity(int q) { this.quantity = q; }
}
