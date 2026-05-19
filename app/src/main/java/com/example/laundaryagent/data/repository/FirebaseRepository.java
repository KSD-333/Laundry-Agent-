package com.example.laundaryagent.data.repository;

import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.laundaryagent.data.model.OrderServices;
import com.example.laundaryagent.data.model.ServiceItem;

/**
 * FirebaseRepository — updated for the latest Firestore structure:
 *
 *   app_data (collection)
 *     └─ [doc]
 *          └─ franchises (collection)
 *               └─ f1 admin (document)
 *                    └─ users (collection)
 *                         └─ {phone} (document)
 *                              ├─ address, name, phone, walletBalance ...
 *                              └─ orders (collection)
 *                                   └─ {orderId} (document)
 *                                        ├─ address, status, totalAmount, items[]
 *                                        └─ items: [ { name, description, quantity, emoji, pricePerUnit } ]
 */
public class FirebaseRepository {

    // ── Firestore path constants ───────────────────────────────────────────
    private static final String COL_APP_DATA = "app_data";
    private static final String DOC_USERS_ROOT = "users"; // document inside app_data? No — it IS the sub-col name
    // Real path: app_data (col) → users (col) — wait, from screenshot:
    // root > app_data (col) > users (sub-col) > {phone} (doc)
    // Firestore doesn't support col > col directly; app_data must be a doc ID.
    // Actual structure: collection "app_data", document (implicit), sub-collection "users"
    // But the screenshot shows: freshfold > app_data > users > 9156270816
    // This means: collection "app_data" → document "users" → subcollection? No.
    // Reading the breadcrumb carefully: app_data is a COLLECTION, users is a COLLECTION inside it?
    // In Firestore you can't have col > col. So "app_data" must be a doc path segment.
    // Most likely: collection = "app_data", document ID not shown, subcollection = "users"
    // OR: the top-level collection IS "users" inside a collection called "app_data" meaning
    // they named a top-level collection "app_data" and "users" is another top-level collection.
    // From breadcrumb "freshfold > app_data > users > 9156270816":
    //   freshfold = project, app_data = collection, users = document, 9156270816 = subcollection? No.
    // Actually in Firebase Console breadcrumb alternates: collection / document / collection / document
    // So: app_data (col) / [some doc] / users (col) / 9156270816 (doc)
    // But since "app_data" panel shows "services" and "users" as items WITH arrow icons,
    // those are SUBCOLLECTIONS of a document inside app_data.
    // The document ID inside app_data is likely the same as the app or is auto.
    // SIMPLEST INTERPRETATION that matches the UI: there's a doc in app_data, and users is a subcollection.
    // We'll use the path: app_data/{appDoc}/users/{phone}/orders/{orderId}
    // where appDoc is a fixed document — let's try "data" as convention, or check if it's just "users" top-level.
    //
    // FINAL DECISION: treat "app_data" as the collection name and "users" as document name inside it,
    // making "users" a document, and phone numbers its subcollection named by phone.
    // Path: db.collection("app_data").document("users").collection(phone)
    // BUT that makes phone a collection which doesn't match — phone should be a document.
    //
    // CORRECT READING: app_data(col) > [implicit root doc] > users(subcol) > 9156270816(doc)
    // In Firestore console this shows as: app_data > users > 9156270816
    // where app_data is the collection, "users" is a subcollection OF the first document in app_data.
    // The first document in app_data has no visible ID in the screenshot.
    //
    // PRAGMATIC FIX: The screenshot left panel shows TWO items: "services" and "users" under app_data.
    // These are subcollections. The document that owns them is the first (unlabeled) document.
    // We'll query: db.collection("app_data").document("users") won't work.
    // Let's go with: collectionGroup("users") for counting,
    // and collectionGroup("orders") for all orders.

    public static final String STATUS_PENDING     = "pending";
    public static final String STATUS_PICKING_PENDING = "picking pending";
    public static final String STATUS_PICKUP_DONE = "pickup_done";
    public static final String STATUS_COMPLETED   = "completed";
    public static final String STATUS_INCOMPLETE  = "incomplete";

    // The top-level users path based on screenshot (app_data col → doc → users subcol)
    // We'll store the users collection reference via the known path pattern
    private static final String USERS_COLLECTION = "users";   // sub-collection name
    private static final String ORDERS_COLLECTION = "orders"; // sub-collection name inside each user doc

    // Name cache to avoid redundant lookups
    private static final Map<String, String> nameCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static FirebaseRepository instance;
    private final FirebaseFirestore db;

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public interface ListCallback {
        void onData(List<Map<String, Object>> list);
    }

    public interface ErrorCallback {
        void onError(String error);
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) instance = new FirebaseRepository();
        return instance;
    }

    // ── Callbacks ──────────────────────────────────────────────────────────

    /** Single-method success callback — usable as a lambda. */
    public interface SuccessCallback {
        void onSuccess();
    }

    /** Single-method failure callback — usable as a lambda. */
    public interface FailureCallback {
        void onFailure(String error);
    }

    /** Combined callback — use anonymous class (not a functional interface). */
    public interface ActionCallback {
        void onSuccess();
        void onFailure(String error);
    }

    public interface NameCallback { void onName(String name); }

    public void fetchNameForPhone(String phone, NameCallback cb) {
        if (phone == null || phone.isEmpty()) {
            cb.onName("Unknown");
            return;
        }
        if (nameCache.containsKey(phone)) {
            cb.onName(nameCache.get(phone));
            return;
        }

        db.collectionGroup(USERS_COLLECTION)
                .whereEqualTo("phone", phone)
                .get()
                .addOnSuccessListener(snap -> {
                    if (!snap.isEmpty()) {
                        String name = snap.getDocuments().get(0).getString("name");
                        if (name != null) {
                            nameCache.put(phone, name);
                            cb.onName(name);
                            return;
                        }
                    }
                    cb.onName("Unknown");
                })
                .addOnFailureListener(e -> cb.onName("Unknown"));
    }

    public interface OrdersCallback {
        void onOrders(List<Map<String, Object>> orders);
    }

    public interface CountCallback {
        void onCount(long count);
    }

    public interface DataCallback {
        void onData(Map<String, Object> data);
    }

    // ── Internal path helpers ──────────────────────────────────────────────

    /**
     * Returns the users CollectionReference.
     * Path: app_data/users (top-level collection named "users" inside "app_data" document).
     * Based on screenshot breadcrumb: app_data [col] → users [col] → {phone} [doc]
     * In Firestore this means: collection("app_data") is wrong if users is a subcollection.
     *
     * ACTUAL Firestore Rule: col → doc → col → doc...
     * Breadcrumb "app_data > users > 9156270816" maps to:
     *   collection("app_data")           — this fails; there must be a doc between app_data and users.
     *
     * Most likely the project has a single document at app_data/{some_id}/users/{phone}.
     * We use collectionGroup("users") to get ALL user documents regardless of parent path.
     */
    private Query usersQuery() {
        return db.collectionGroup(USERS_COLLECTION);
    }

    // ── Dashboard Stats (real-time) ────────────────────────────────────────

    /**
     * Listen for total registered user count.
     * Uses collectionGroup("users") to count all docs in any "users" subcollection.
     */
    public ListenerRegistration listenTotalUsers(CountCallback cb) {
        return db.collectionGroup(USERS_COLLECTION)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("FirebaseRepo", "listenTotalUsers error: " + e.getMessage());
                        return;
                    }
                    if (snap != null) {
                        Log.d("FirebaseRepo", "Found total users: " + snap.size());
                        cb.onCount(snap.size());
                    }
                });
    }

    /**
     * Listen for total orders count (across all users).
     * Uses collectionGroup("orders") — requires Firestore index for this collection group.
     */
    public ListenerRegistration listenTotalOrders(CountCallback cb) {
        return db.collectionGroup(ORDERS_COLLECTION)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("FirebaseRepo", "listenTotalOrders error: " + e.getMessage());
                        return;
                    }
                    if (snap != null) {
                        Log.d("FirebaseRepo", "Total orders snap size: " + snap.size());
                        cb.onCount(snap.size());
                    }
                });
    }

    /** Listen for completed orders count. Robust version with client-side filter. */
    public ListenerRegistration listenCompletedOrders(CountCallback cb) {
        return db.collectionGroup(ORDERS_COLLECTION)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        int count = 0;
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            String s = str(doc.getData(), "status").toLowerCase();
                            if (s.equals(STATUS_COMPLETED)) count++;
                        }
                        cb.onCount(count);
                    }
                });
    }

    /** Listen for pending orders count. Robust version with client-side filter. */
    public ListenerRegistration listenPendingOrders(String targetDate, CountCallback cb) {
        return db.collectionGroup(ORDERS_COLLECTION)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) {
                        int count = 0;
                        for (DocumentSnapshot doc : snap.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            String s = str(data, "status").toLowerCase().replace("_", " ").replace("-", " ").trim();
                            String pDate = str(data, "pickupDate");
                            if (pDate.isEmpty()) pDate = str(data, "date");
                            if (pDate.isEmpty()) pDate = str(data, "orderDate");
                            String dDate = str(data, "deliveryDate");

                            // Aggressive normalization for comparison
                            String targetNorm = targetDate.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
                            String pDateNorm = pDate.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
                            String dDateNorm = dDate.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();

                            // Check if it's an active task for the target date
                            boolean matchesDate = targetNorm.equals(pDateNorm) || targetNorm.equals(dDateNorm);
                            if (matchesDate) {
                                boolean isPendingPickup = (s.isEmpty() || s.equals("pending") || s.equals("picking pending"));
                                boolean isPendingDelivery = (s.equals("pickup done") || s.equals("ready") || s.equals("out for delivery") || s.equals("washing") || s.equals("ironing"));
                                
                                if (isPendingPickup || isPendingDelivery) {
                                    count++;
                                }
                            }
                        }
                        cb.onCount(count);
                    }
                });
    }
    
    // Compatibility wrapper
    public ListenerRegistration listenPendingOrders(CountCallback cb) {
        String today = new java.text.SimpleDateFormat("d MMM yyyy", java.util.Locale.getDefault()).format(new java.util.Date());
        return listenPendingOrders(today, cb);
    }

    // ── Real-time order lists for agent ────────────────────────────────────

    /**
     * Listen for ALL orders across all users (for task list and reports).
     * Does not filter by status, allowing fragments to show both pending and completed.
     */
    public ListenerRegistration listenAllOrdersForTasks(ListCallback cb) {
        return listenAllOrdersForTasks(cb, null);
    }

    public ListenerRegistration listenAllOrdersForTasks(ListCallback cb, ErrorCallback errorCb) {
        return db.collectionGroup(ORDERS_COLLECTION)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("FirebaseRepo", "listenAllOrdersForTasks error: " + e.getMessage());
                        if (errorCb != null) errorCb.onError(e.getMessage());
                        return;
                    }
                    if (snap != null) {
                        List<Map<String, Object>> allDocs = docsToListWithPhone(snap);
                        Log.d("FirebaseRepo", "Fetched all tasks: " + allDocs.size());
                        cb.onData(allDocs);
                    }
                });
    }

    public ListenerRegistration listenAllPendingPickups(ListCallback cb) {
        return db.collectionGroup(ORDERS_COLLECTION)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("FirebaseRepo", "listenAllPendingPickups error: " + e.getMessage());
                        return;
                    }
                    if (snap != null) {
                        List<Map<String, Object>> allDocs = docsToListWithPhone(snap);
                        List<Map<String, Object>> pending = new ArrayList<>();
                        for (Map<String, Object> doc : allDocs) {
                            String s = str(doc, "status").toLowerCase();
                            // Support "pending", "Pending", or "Picking Pending"
                            if (s.isEmpty() || s.equals(STATUS_PENDING) || s.equals(STATUS_PICKING_PENDING)) {
                                pending.add(doc);
                            }
                        }
                        Log.d("FirebaseRepo", "Found pending orders: " + pending.size() + " out of " + allDocs.size());
                        cb.onData(pending);
                    }
                });
    }

    /**
     * Listen for ALL delivery-ready orders across all users (for agent delivery list).
     */
    public ListenerRegistration listenAllDeliveryOrders(ListCallback cb) {
        return db.collectionGroup(ORDERS_COLLECTION)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("FirebaseRepo", "listenAllDeliveryOrders error: " + e.getMessage());
                        return;
                    }
                    if (snap != null) {
                        List<Map<String, Object>> allDocs = docsToListWithPhone(snap);
                        List<Map<String, Object>> ready = new ArrayList<>();
                        for (Map<String, Object> doc : allDocs) {
                            String s = str(doc, "status").toLowerCase();
                            if (s.equals(STATUS_PICKUP_DONE)) {
                                ready.add(doc);
                            }
                        }
                        Log.d("FirebaseRepo", "Found delivery orders: " + ready.size());
                        cb.onData(ready);
                    }
                });
    }

    /** Listen for all completed orders (for admin completed deliveries screen). */
    public ListenerRegistration listenCompletedDeliveries(ListCallback cb) {
        return db.collectionGroup(ORDERS_COLLECTION)
                .whereEqualTo("status", STATUS_COMPLETED)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) cb.onData(docsToListWithPhone(snap));
                });
    }

    // ── Order Actions ──────────────────────────────────────────────────────

    /**
     * Mark an order as picked up. Stores delivery OTP in Firestore.
     * orderPath format: "app_data/{appId}/users/{phone}/orders/{orderId}"
     * We get the full path from the order map's "__path" field set by docsToListWithPhone.
     */
    public void markPickupDone(String orderFullPath, String deliveryOtp, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_PICKUP_DONE);
        updates.put("deliveryOtp", deliveryOtp);
        updates.put("pickedAt", com.google.firebase.Timestamp.now());

        db.document(orderFullPath)
                .update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    /** Mark an order's pickup as done without OTP (direct agent confirmation). */
    public void markOrderPickupDone(String orderFullPath, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_COMPLETED);
        updates.put("completedAt", com.google.firebase.Timestamp.now());
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        db.document(orderFullPath)
                .update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    /** Mark an order as COMPLETED. */
    public void markOrderCompleted(String orderFullPath, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_COMPLETED);
        updates.put("completedAt", com.google.firebase.Timestamp.now());

        db.document(orderFullPath)
                .update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    /** Mark an order as INCOMPLETE with a reason. */
    public void markOrderIncomplete(String orderFullPath, String reason, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_INCOMPLETE);
        updates.put("incompleteReason", reason);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        db.document(orderFullPath)
                .update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    /** Reschedule an order by adding a reason and keeping it in the current queue. */
    public void rescheduleOrder(String orderFullPath, String reason, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("rescheduleReason", reason);
        updates.put("rescheduledAt", com.google.firebase.Timestamp.now());
        updates.put("updatedAt", com.google.firebase.Timestamp.now());

        db.document(orderFullPath)
                .update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    /**
     * Verify OTP stored in Firestore for the given order path, then mark completed if correct.
     */
    public void verifyDeliveryOtp(String orderFullPath, String enteredOtp, ActionCallback cb) {
        db.document(orderFullPath).get()
                .addOnSuccessListener(doc -> {
                    String storedOtp = doc.getString("deliveryOtp");
                    if (enteredOtp.equals(storedOtp)) {
                        markOrderCompleted(orderFullPath, cb);
                    } else {
                        cb.onFailure("Invalid OTP");
                    }
                })
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    /**
     * Fetch a single order document by its full Firestore path.
     */
    public void getOrderByPath(String orderFullPath, FailureCallback errCb, OrdersCallback dataCb) {
        db.document(orderFullPath).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<Map<String, Object>> list = new ArrayList<>();
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId());
                            data.put("__path", doc.getReference().getPath());
                            // Extract customerPhone from parent path: .../users/{phone}/orders/{id}
                            String path = doc.getReference().getPath();
                            String[] parts = path.split("/");
                            // Find "users" segment and take next segment as phone
                            for (int i = 0; i < parts.length - 1; i++) {
                                if ("users".equals(parts[i])) {
                                    data.put("customerPhone", parts[i + 1]);
                                    break;
                                }
                            }
                            list.add(data);
                        }
                        dataCb.onOrders(list);
                    } else {
                        errCb.onFailure("Order not found");
                    }
                })
                .addOnFailureListener(err -> errCb.onFailure(err.getMessage()));
    }
    /**
     * Fetch a single document by its full Firestore path.
     */
    public void getDocumentByPath(String path, FailureCallback errCb, DataCallback dataCb) {
        db.document(path).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.getData() != null) {
                        Map<String, Object> data = doc.getData();
                        data.put("id", doc.getId());
                        data.put("__path", doc.getReference().getPath());
                        dataCb.onData(data);
                    } else {
                        errCb.onFailure("Document not found");
                    }
                })
                .addOnFailureListener(err -> errCb.onFailure(err.getMessage()));
    }

    // ── Revenue stats ──────────────────────────────────────────────────────

    /**
     * Sum totalAmount across all completed orders (collection group query).
     */
    public void getTotalRevenue(FailureCallback errCb, CountCallback cb) {
        db.collectionGroup(ORDERS_COLLECTION)
                .whereEqualTo("status", STATUS_COMPLETED)
                .get()
                .addOnSuccessListener(snap -> {
                    long total = 0;
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Long amount = doc.getLong("totalAmount");
                        if (amount != null) total += amount;
                    }
                    cb.onCount(total);
                })
                .addOnFailureListener(err -> errCb.onFailure(err.getMessage()));
    }

    // ── Users (for UsersActivity) ──────────────────────────────────────────

    /**
     * Listen for all user documents in real-time.
     * Returns maps with fields from each user doc + "id" = phone number.
     */
    public ListenerRegistration listenAllUsers(OrdersCallback cb) {
        return db.collectionGroup(USERS_COLLECTION)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        Log.e("FirebaseRepo", "listenAllUsers error: " + e.getMessage());
                        return;
                    }
                    if (snap != null) {
                        Log.d("FirebaseRepo", "Found users: " + snap.size());
                        cb.onOrders(docsToList(snap));
                    }
                });
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Convert a QuerySnapshot to a list of maps, adding "id" and "__path" to each. */
    private List<Map<String, Object>> docsToList(QuerySnapshot snap) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            Map<String, Object> data = doc.getData();
            if (data != null) {
                data.put("id", doc.getId());
                data.put("__path", doc.getReference().getPath());
                list.add(data);
            }
        }
        return list;
    }

    /**
     * Like docsToList but also extracts customerPhone from the parent "users/{phone}" path segment.
     * Adds "customerPhone" to the map for easy display in agent screens.
     */
    private List<Map<String, Object>> docsToListWithPhone(QuerySnapshot snap) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (DocumentSnapshot doc : snap.getDocuments()) {
            list.add(docToMapWithPhone(doc));
        }
        return list;
    }

    private Map<String, Object> docToMapWithPhone(DocumentSnapshot doc) {
        Map<String, Object> data = doc.getData();
        if (data == null) data = new HashMap<>();
        data.put("id", doc.getId());
        data.put("__path", doc.getReference().getPath());
        // Extract phone from path: .../users/{phone}/orders/{id}
        String path = doc.getReference().getPath();
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if ("users".equals(parts[i])) {
                String phone = parts[i + 1];
                data.put("customerPhone", phone);
                // Also set customerName from phone if not already set
                if (!data.containsKey("customerName") || data.get("customerName") == null) {
                    data.put("customerName", phone);
                }
                break;
            }
        }
        return data;
    }

    /**
     * Helper to parse the flat 'items' array from Firestore into categorized OrderServices.
     * Mapped based on the 'name' field in each item (which acts as the category).
     */
    public OrderServices parseOrderServices(List<Map<String, Object>> items) {
        List<ServiceItem> laundry = new ArrayList<>();
        List<ServiceItem> ironing = new ArrayList<>();
        List<ServiceItem> dryCleaning = new ArrayList<>();
        List<ServiceItem> shoeCare = new ArrayList<>();

        if (items == null) return new OrderServices(laundry, ironing, dryCleaning, shoeCare);

        for (Map<String, Object> itemMap : items) {
            String category = str(itemMap, "name").toLowerCase();
            String itemName = str(itemMap, "description");
            if (itemName.isEmpty()) itemName = str(itemMap, "itemName"); // alternative key
            
            Object qtyObj = itemMap.get("quantity");
            int qty = 0;
            if (qtyObj instanceof Long) qty = ((Long) qtyObj).intValue();
            else if (qtyObj instanceof Integer) qty = (Integer) qtyObj;
            else if (qtyObj instanceof String) {
                try { qty = Integer.parseInt((String) qtyObj); } catch (Exception ignored) {}
            }

            ServiceItem serviceItem = new ServiceItem(itemName, qty);

            if (category.contains("laundry") || category.contains("wash")) {
                laundry.add(serviceItem);
            } else if (category.contains("iron") || category.contains("press")) {
                ironing.add(serviceItem);
            } else if (category.contains("dry clean")) {
                dryCleaning.add(serviceItem);
            } else if (category.contains("shoe")) {
                shoeCare.add(serviceItem);
            } else {
                // Default to laundry if unknown
                laundry.add(serviceItem);
            }
        }

        return new OrderServices(laundry, ironing, dryCleaning, shoeCare);
    }

    /** Safely get a String field from a Firestore doc map. */
    public static String str(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key);
        if (val == null) {
            // Fallback for weird field names like "5" for address or society in user DB
            if ("address".equals(key) || "society".equals(key)) {
                val = map.get("5");
            }
        }
        return val != null ? val.toString() : "";
    }
}
