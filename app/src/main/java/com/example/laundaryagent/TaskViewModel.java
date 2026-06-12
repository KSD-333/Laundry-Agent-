package com.example.laundaryagent;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.laundaryagent.data.model.OrderItem;
import com.example.laundaryagent.data.model.OrderStatus;
import com.example.laundaryagent.data.repository.FirebaseRepository;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class TaskViewModel extends ViewModel {
    // Raw data replaced by cached OrderItems
    private final MutableLiveData<String> selectedDate = new MutableLiveData<>();
    private ListenerRegistration listener;
    
    private final MutableLiveData<List<String>> societiesList = new MutableLiveData<>();
    private final MutableLiveData<List<OrderItem>> rawOrderItems = new MutableLiveData<>(new ArrayList<>());
    private final MediatorLiveData<List<OrderItem>> filteredOrderItems = new MediatorLiveData<>();

    public TaskViewModel() {
        // Initialize with real today
        selectedDate.setValue(new java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date()));
        
        filteredOrderItems.addSource(rawOrderItems, items -> applyFilter());
        filteredOrderItems.addSource(societiesList, socs -> applyFilter());
    }

    private void applyFilter() {
        List<OrderItem> raw = rawOrderItems.getValue();
        List<String> socs = societiesList.getValue();
        if (raw == null) return;
        List<OrderItem> filtered = new ArrayList<>();
        if (socs == null || socs.isEmpty() || (socs.size() == 1 && socs.get(0).startsWith("Error"))) {
            filteredOrderItems.setValue(filtered);
            return;
        }
        
        for (OrderItem item : raw) {
            boolean matches = false;
            for (String allowed : socs) {
                if (allowed.equalsIgnoreCase(item.getSociety())) {
                    matches = true;
                    break;
                }
            }
            if (matches) filtered.add(item);
        }
        filteredOrderItems.setValue(filtered);
    }

    public MutableLiveData<String> getSelectedDate() { return selectedDate; }
    public void setSelectedDate(String date) { selectedDate.setValue(date); }

    private final MutableLiveData<String> selectedSociety = new MutableLiveData<>("All Societies");
    public MutableLiveData<String> getSelectedSociety() { return selectedSociety; }
    public void setSelectedSociety(String society) { selectedSociety.setValue(society); }


    public LiveData<List<String>> getSocieties(android.content.Context context, String agentPhone) {
        if (societiesList.getValue() == null) {
            // First time loading
            FirebaseRepository.getInstance().loadSocietiesForAgent(agentPhone, list -> {
                new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                    societiesList.setValue(list);
                });
            });
        }
        return societiesList;
    }

    public LiveData<List<OrderItem>> getOrderItems(android.content.Context context) {
        if (listener == null) {
            String franchiseId = "";
            String agentPhone = "";
            if (context != null) {
                android.content.SharedPreferences prefs = context.getSharedPreferences("LaundryPrefs", android.content.Context.MODE_PRIVATE);
                franchiseId = prefs.getString("franchise_id", "");
                agentPhone = prefs.getString("agent_phone", "");
                if (agentPhone.isEmpty()) {
                    String identity = prefs.getString("user_identity", "");
                    if (identity.matches("\\d{10}")) agentPhone = identity;
                }
            }
            
            if (societiesList.getValue() == null && !agentPhone.isEmpty()) {
                getSocieties(context, agentPhone);
            }
            
            listener = FirebaseRepository.getInstance().listenAllOrdersForTasks(
                franchiseId,
                list -> {
                    List<OrderItem> mapped = new java.util.ArrayList<>();
                    for (Map<String, Object> doc : list) {
                        mapped.add(mapToOrderItem(doc));
                    }
                    rawOrderItems.setValue(mapped);
                },
                err -> {
                    if (context != null) {
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() ->
                            android.widget.Toast.makeText(context, "Firestore Error: " + err, android.widget.Toast.LENGTH_LONG).show());
                    }
                }
            );
        }
        return filteredOrderItems;
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
                if (parts.length >= 4) {
                    soc = parts[3].trim(); // Example: "5, 1st Floor, B, Amanora, Pune..." -> "Amanora"
                } else if (parts.length >= 3) {
                    soc = parts[2].trim();
                } else {
                    soc = parts[0].trim();
                }
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
        else if (s.equals("pickup done") || s.equals("washing") || s.equals("ironing") || s.equals("pickup_done") || s.equals("progress")) orderStatus = OrderStatus.PICKUP_DONE;
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
