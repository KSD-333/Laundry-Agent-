package com.example.laundaryagent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderStatus;
import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;

public class TaskViewModel extends ViewModel {
    // Raw data replaced by cached OrderItems
    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();
    private ListenerRegistration listener;

    public TaskViewModel() {
        // Initialize with real today
        selectedDate.setValue(new java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
    }

    public MutableLiveData<String> getSelectedDate() { return selectedDate; }
    public void setSelectedDate(String date) { selectedDate.setValue(date); }

    private final MutableLiveData<List<OrderItem>> orderItems = new MutableLiveData<>(new java.util.ArrayList<>());

    public LiveData<List<OrderItem>> getOrderItems(android.content.Context context) {
        if (listener == null) {
            listener = FirebaseRepository.getInstance().listenAllOrdersForTasks(
                list -> {
                    List<OrderItem> mapped = new java.util.ArrayList<>();
                    for (Map<String, Object> doc : list) {
                        mapped.add(mapToOrderItem(doc));
                    }
                    orderItems.setValue(mapped);
                },
                err -> {
                    if (context != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            android.widget.Toast.makeText(context, "Firestore Error: " + err, android.widget.Toast.LENGTH_LONG).show());
                    }
                }
            );
        }
        return orderItems;
    }

    private OrderItem mapToOrderItem(Map<String, Object> doc) {
        String id      = FirebaseRepository.str(doc, "id");
        String phone   = FirebaseRepository.str(doc, "customerPhone");
        if (phone.isEmpty()) phone = FirebaseRepository.str(doc, "phone");
        
        String name    = FirebaseRepository.str(doc, "customerName");
        if (name.isEmpty()) name = FirebaseRepository.str(doc, "name");
        
        String address = FirebaseRepository.str(doc, "address");
        String soc     = FirebaseRepository.str(doc, "society");
        String status  = FirebaseRepository.str(doc, "status");
        String path    = FirebaseRepository.str(doc, "__path");
        
        String pDate = FirebaseRepository.str(doc, "pickupDate");
        if (pDate.isEmpty()) pDate = FirebaseRepository.str(doc, "pickup_date");
        if (pDate.isEmpty()) pDate = FirebaseRepository.str(doc, "date");
        if (pDate.isEmpty()) pDate = FirebaseRepository.str(doc, "orderDate");

        String dDate = FirebaseRepository.str(doc, "deliveryDate");
        if (dDate.isEmpty()) dDate = FirebaseRepository.str(doc, "delivery_date");

        String reason = FirebaseRepository.str(doc, "incompleteReason");

        if (name.isEmpty()) name = phone.isEmpty() ? "Unknown Customer" : phone;
        
        if (soc.isEmpty() || soc.equals("All Societies")) {
            if (!address.isEmpty()) {
                String[] parts = address.split(",");
                soc = parts.length > 0 ? parts[0].trim() : "Residence";
            } else {
                soc = "Residence";
            }
        }

        String time = "—";
        Object createdAt = doc.get("createdAt");
        if (createdAt instanceof com.google.firebase.Timestamp) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM, hh:mm a", java.util.Locale.US);
            time = sdf.format(((com.google.firebase.Timestamp) createdAt).toDate());
        }
        if (!pDate.isEmpty()) time = pDate;
        else if (!dDate.isEmpty()) time = dDate;

        OrderStatus orderStatus;
        String s = status.toLowerCase().replace("_", " ").replace("-", " ").trim();
        if (s.equals("pending") || s.equals("picking pending")) orderStatus = OrderStatus.PENDING;
        else if (s.equals("pickup done") || s.equals("washing") || s.equals("ironing") || s.equals("pickup_done")) orderStatus = OrderStatus.PICKUP_DONE;
        else if (s.equals("ready")) orderStatus = OrderStatus.READY;
        else if (s.equals("out for delivery") || s.equals("out_for_delivery")) orderStatus = OrderStatus.OUT_FOR_DELIVERY;
        else if (s.equals("delivered") || s.equals("completed")) orderStatus = OrderStatus.COMPLETED;
        else if (s.equals("incomplete")) orderStatus = OrderStatus.INCOMPLETE;
        else orderStatus = OrderStatus.PENDING;

        OrderItem item = new OrderItem(id, name, address, soc, phone, time, path, orderStatus, reason);
        item.setPickupDate(pDate);
        item.setDeliveryDate(dDate);
        return item;
    }

    @Override
    protected void onCleared() {
        if (listener != null) {
            listener.remove();
            listener = null;
        }
        super.onCleared();
    }
}
