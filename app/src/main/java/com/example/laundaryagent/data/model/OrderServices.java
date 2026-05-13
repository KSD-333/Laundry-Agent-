package com.example.laundaryagent.data.model;

import java.util.List;

/**
 * Holds the four service categories for one order.
 * Each category has a list of ServiceItem (name + qty).
 */
public class OrderServices {

    private final List<ServiceItem> laundry;
    private final List<ServiceItem> ironing;
    private final List<ServiceItem> dryCleaning;
    private final List<ServiceItem> shoeCare;

    public OrderServices(List<ServiceItem> laundry,
                         List<ServiceItem> ironing,
                         List<ServiceItem> dryCleaning,
                         List<ServiceItem> shoeCare) {
        this.laundry     = laundry;
        this.ironing     = ironing;
        this.dryCleaning = dryCleaning;
        this.shoeCare    = shoeCare;
    }

    public List<ServiceItem> getLaundry()     { return laundry; }
    public List<ServiceItem> getIroning()     { return ironing; }
    public List<ServiceItem> getDryCleaning() { return dryCleaning; }
    public List<ServiceItem> getShoeCare()    { return shoeCare; }

    /** Total items across all services */
    public int totalItems() {
        int t = 0;
        for (ServiceItem s : laundry)     t += s.getQuantity();
        for (ServiceItem s : ironing)     t += s.getQuantity();
        for (ServiceItem s : dryCleaning) t += s.getQuantity();
        for (ServiceItem s : shoeCare)    t += s.getQuantity();
        return t;
    }
}
