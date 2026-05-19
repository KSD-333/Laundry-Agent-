package com.example.laundaryagent.data.repository;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderServices;
import com.example.laundaryagent.data.model.OrderStatus;
import com.example.laundaryagent.data.model.ServiceItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LaundryRepository {
    private static LaundryRepository instance;

    private final List<OrderItem>          pickupOrders;
    private final List<OrderItem>          deliveryOrders;
    private final List<String>             societies;
    private final Map<String, OrderServices> servicesMap;

    private LaundryRepository() {
        pickupOrders = new ArrayList<>(Arrays.asList(
            new OrderItem("P1", "Rahul Sharma",   "Tower 4, Apt 1201",       "Amanora Park Town", "9876543210", "09:30 AM", ""),
            new OrderItem("P2", "Anjali Deshmukh","Iris, Flat 302",           "Magarpatta City",   "9876543211", "10:15 AM", ""),
            new OrderItem("P3", "Vikram Malhotra","Wing C, House 5",          "Blue Ridge Town",   "9876543212", "08:00 AM", ""),
            new OrderItem("P4", "Sneha Kulkarni", "Aspire, Penthouse 1",      "Amanora Park Town", "9876543215", "02:00 PM", ""),
            new OrderItem("P5", "Aditya Joshi",   "Seryne, Block B",          "Magarpatta City",   "9876543216", "03:45 PM", "")
        ));

        deliveryOrders = new ArrayList<>(Arrays.asList(
            new OrderItem("D1", "Priya Singh",    "T8, Flat 504",             "Blue Ridge Town",   "9876543213", "11:00 AM", ""),
            new OrderItem("D2", "Sandeep Patil",  "Heliconia, Apt 101",       "Magarpatta City",   "9876543214", "01:30 PM", ""),
            new OrderItem("D3", "Megha Rao",      "Gateway Towers, Apt 22",   "Amanora Park Town", "9876543217", "04:00 PM", ""),
            new OrderItem("D4", "Karan Mehra",    "Wing A, Floor 15",         "Blue Ridge Town",   "9876543218", "05:30 PM", "")
        ));

        societies = Arrays.asList("All Societies", "Amanora Park Town", "Magarpatta City", "Blue Ridge Town");

        // ── Service data per order ──────────────────────────────────────────
        servicesMap = new HashMap<>();

        servicesMap.put("P1", new OrderServices(
            Arrays.asList(new ServiceItem("Shirt", 3), new ServiceItem("T-Shirt", 2), new ServiceItem("Jeans", 1)),
            Arrays.asList(new ServiceItem("Trouser", 2), new ServiceItem("Kurta", 1)),
            Collections.emptyList(),
            Collections.emptyList()
        ));
        servicesMap.put("P2", new OrderServices(
            Arrays.asList(new ServiceItem("Shirt", 4), new ServiceItem("Saree", 2)),
            Arrays.asList(new ServiceItem("Shirt", 2), new ServiceItem("Pant", 3)),
            Arrays.asList(new ServiceItem("Blazer", 1), new ServiceItem("Coat", 1)),
            Collections.emptyList()
        ));
        servicesMap.put("P3", new OrderServices(
            Arrays.asList(new ServiceItem("T-Shirt", 5), new ServiceItem("Shorts", 3)),
            Collections.emptyList(),
            Collections.emptyList(),
            Arrays.asList(new ServiceItem("Sports Shoes", 2), new ServiceItem("Formal Shoes", 1))
        ));
        servicesMap.put("P4", new OrderServices(
            Arrays.asList(new ServiceItem("Kurti", 3), new ServiceItem("Dupatta", 2)),
            Arrays.asList(new ServiceItem("Saree", 2), new ServiceItem("Lehenga", 1)),
            Arrays.asList(new ServiceItem("Suit", 1)),
            Collections.emptyList()
        ));
        servicesMap.put("P5", new OrderServices(
            Arrays.asList(new ServiceItem("Shirt", 6), new ServiceItem("Pant", 4)),
            Arrays.asList(new ServiceItem("Shirt", 3)),
            Collections.emptyList(),
            Arrays.asList(new ServiceItem("Sneakers", 1))
        ));
        // Delivery orders share same service data
        servicesMap.put("D1", new OrderServices(
            Arrays.asList(new ServiceItem("Shirt", 2), new ServiceItem("Jeans", 2)),
            Arrays.asList(new ServiceItem("Trouser", 1)),
            Collections.emptyList(),
            Arrays.asList(new ServiceItem("Leather Shoes", 1))
        ));
        servicesMap.put("D2", new OrderServices(
            Arrays.asList(new ServiceItem("T-Shirt", 4), new ServiceItem("Shorts", 2)),
            Arrays.asList(new ServiceItem("Shirt", 2), new ServiceItem("Pant", 2)),
            Arrays.asList(new ServiceItem("Jacket", 1)),
            Collections.emptyList()
        ));
        servicesMap.put("D3", new OrderServices(
            Arrays.asList(new ServiceItem("Saree", 3), new ServiceItem("Blouse", 3)),
            Arrays.asList(new ServiceItem("Kurti", 2)),
            Arrays.asList(new ServiceItem("Lehenga", 1), new ServiceItem("Sherwani", 1)),
            Collections.emptyList()
        ));
        servicesMap.put("D4", new OrderServices(
            Arrays.asList(new ServiceItem("Shirt", 5), new ServiceItem("Pant", 3)),
            Arrays.asList(new ServiceItem("Suit", 1)),
            Collections.emptyList(),
            Arrays.asList(new ServiceItem("Formal Shoes", 2))
        ));
    }

    public static synchronized LaundryRepository getInstance() {
        if (instance == null) instance = new LaundryRepository();
        return instance;
    }

    public List<OrderItem>   getPickupOrders()   { return pickupOrders; }
    public List<OrderItem>   getDeliveryOrders() { return deliveryOrders; }
    public List<String>      getSocieties()      { return societies; }
    public OrderServices     getServicesForOrder(String orderId) { return servicesMap.get(orderId); }

    public List<OrderItem> getFilteredPickups(String society) {
        List<OrderItem> filtered = new ArrayList<>();
        if ("All Societies".equals(society)) {
            filtered.addAll(pickupOrders);
        } else {
            for (OrderItem o : pickupOrders) {
                if (o.getSociety().equals(society)) {
                    filtered.add(o);
                }
            }
        }
        
        Collections.sort(filtered, new Comparator<OrderItem>() {
            @Override
            public int compare(OrderItem o1, OrderItem o2) {
                return Integer.compare(o1.getStatus().ordinal(), o2.getStatus().ordinal());
            }
        });
        return filtered;
    }

    public List<OrderItem> getFilteredDeliveries(String society) {
        List<OrderItem> filtered = new ArrayList<>();
        if ("All Societies".equals(society)) {
            filtered.addAll(deliveryOrders);
        } else {
            for (OrderItem o : deliveryOrders) {
                if (o.getSociety().equals(society)) {
                    filtered.add(o);
                }
            }
        }
        
        Collections.sort(filtered, new Comparator<OrderItem>() {
            @Override
            public int compare(OrderItem o1, OrderItem o2) {
                return Integer.compare(o1.getStatus().ordinal(), o2.getStatus().ordinal());
            }
        });
        return filtered;
    }

    public void markPickupComplete(String id) {
        for (OrderItem item : pickupOrders) {
            if (item.getId().equals(id)) {
                item.setStatus(OrderStatus.COMPLETED);
                break;
            }
        }
    }

    public void markPickupIncomplete(String id, String reason) {
        for (OrderItem item : pickupOrders) {
            if (item.getId().equals(id)) {
                item.setStatus(OrderStatus.INCOMPLETE);
                item.setIncompleteReason(reason);
                break;
            }
        }
    }

    public void markDeliveryComplete(String id) {
        for (OrderItem item : deliveryOrders) {
            if (item.getId().equals(id)) {
                item.setStatus(OrderStatus.COMPLETED);
                break;
            }
        }
    }

    public void markDeliveryIncomplete(String id, String reason) {
        for (OrderItem item : deliveryOrders) {
            if (item.getId().equals(id)) {
                item.setStatus(OrderStatus.INCOMPLETE);
                item.setIncompleteReason(reason);
                break;
            }
        }
    }

    public OrderItem getOrderById(String id) {
        for (OrderItem item : pickupOrders) {
            if (item.getId().equals(id)) return item;
        }
        for (OrderItem item : deliveryOrders) {
            if (item.getId().equals(id)) return item;
        }
        return null;
    }
}
