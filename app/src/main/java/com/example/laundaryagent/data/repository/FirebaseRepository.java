package com.example.laundaryagent.data.repository;

import android.util.Log;

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
 * FirebaseRepository — Exact Firestore structure (confirmed from console screenshots):
 *
 *  FRANCHISES:
 *    app_data (collection)
 *      └─ franchises (document)
 *           └─ franchises (subcollection)
 *                └─ {franchiseId} (document)
 *                     fields: businessEmail, name, location[], address, id
 *
 *  USERS:
 *    app_data (collection)
 *      └─ users (document)
 *           └─ users (subcollection)
 *                └─ {phone} (document)
 *                     fields: franchiseId, name, city, email, phone, role, isApproved ...
 *
 *  ORDERS:
 *    app_data (collection)
 *      └─ users (document)
 *           └─ users (subcollection)
 *                └─ {phone} (document)
 *                     └─ address (subcollection)
 *                          └─ {addressId} (document)
 *                               └─ orders (subcollection)
 *                                    └─ {orderId} (document)
 *                                         fields: franchiseId, items[], address, deliveryDate, status
 */
public class FirebaseRepository {

    private static final String TAG = "FirebaseRepo";

    // Exact Firestore paths
    // Franchises:  app_data/franchises/franchises/{franchiseId}
    private static final String COL_APP_DATA      = "app_data";
    private static final String DOC_FRANCHISES    = "franchises";   // document ID inside app_data
    private static final String COL_FRANCHISES    = "franchises";   // subcollection of that document
    // Users:       app_data/users/users/{phone}
    private static final String DOC_USERS         = "users";        // document ID inside app_data
    private static final String COL_USERS         = "users";        // subcollection of that document
    // Orders:      app_data/users/users/{phone}/address/{addressId}/orders/{orderId}
    private static final String COL_ADDRESS       = "address";
    private static final String COL_ORDERS        = "orders";

    public static final String STATUS_PENDING         = "pending";
    public static final String STATUS_PICKING_PENDING = "picking pending";
    public static final String STATUS_PICKUP_DONE     = "pickup_done";
    public static final String STATUS_COMPLETED       = "completed";
    public static final String STATUS_INCOMPLETE      = "incomplete";
    public static final String STATUS_PROGRESS        = "progress";

    // Name cache
    private static final Map<String, String> nameCache = new java.util.concurrent.ConcurrentHashMap<>();

    private static FirebaseRepository instance;
    private final FirebaseFirestore db;

    private FirebaseRepository() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirebaseRepository getInstance() {
        if (instance == null) instance = new FirebaseRepository();
        return instance;
    }

    // ── Callbacks ──────────────────────────────────────────────────────────

    public interface ListCallback    { void onData(List<Map<String, Object>> list); }
    public interface ErrorCallback   { void onError(String error); }
    public interface SuccessCallback { void onSuccess(); }
    public interface FailureCallback { void onFailure(String error); }
    public interface ActionCallback  { void onSuccess(); void onFailure(String error); }
    public interface NameCallback    { void onName(String name); }
    public interface OrdersCallback  { void onOrders(List<Map<String, Object>> orders); }
    public interface CountCallback   { void onCount(long count); }
    public interface DataCallback    { void onData(Map<String, Object> data); }
    public interface StringsCallback { void onData(List<String> data); }

    // ── Path helpers ───────────────────────────────────────────────────────

    /** Reference to: freshfold/app_data/franchises */
    private com.google.firebase.firestore.CollectionReference franchisesRef() {
        return db.collection("freshfold").document("app_data").collection("franchises");
    }

    /** Reference to: app_data/users/users */
    private com.google.firebase.firestore.CollectionReference usersRef() {
        return db.collection(COL_APP_DATA).document(DOC_USERS).collection(COL_USERS);
    }

    // ── Franchise lookup ───────────────────────────────────────────────────

    /**
     * Find a franchise document by businessEmail.
     * Uses exact path: app_data/franchises/franchises
     * Falls back to collectionGroup if not found.
     */
    public void getFranchiseByEmail(String email, FailureCallback errCb, DataCallback dataCb) {
        String searchEmail = email.trim().toLowerCase();
        Log.d(TAG, "Looking for franchise with email: " + searchEmail);

        // Primary: exact known path
        franchisesRef().get().addOnSuccessListener(snap -> {
            if (snap != null && !snap.isEmpty()) {
                for (DocumentSnapshot doc : snap.getDocuments()) {
                    Map<String, Object> data = doc.getData();
                    if (data == null) continue;
                    String bEmail = str(data, "businessEmail").trim().toLowerCase();
                    String eEmail = str(data, "email").trim().toLowerCase();
                    Log.d(TAG, "Franchise doc " + doc.getId() + " businessEmail=" + bEmail);
                    if (bEmail.equals(searchEmail) || eEmail.equals(searchEmail)) {
                        data.put("id", doc.getId());
                        Log.d(TAG, "Found franchise: " + doc.getId() + " name=" + str(data, "name"));
                        dataCb.onData(data);
                        return;
                    }
                }
            }
            // Fallback: collectionGroup (searches all collections named "franchises")
            Log.d(TAG, "Not found in primary path, trying collectionGroup...");
            db.collectionGroup(COL_FRANCHISES).get().addOnSuccessListener(snap2 -> {
                if (snap2 != null) {
                    for (DocumentSnapshot doc : snap2.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data == null) continue;
                        String bEmail = str(data, "businessEmail").trim().toLowerCase();
                        String eEmail = str(data, "email").trim().toLowerCase();
                        if (bEmail.equals(searchEmail) || eEmail.equals(searchEmail)) {
                            data.put("id", doc.getId());
                            dataCb.onData(data);
                            return;
                        }
                    }
                }
                errCb.onFailure("No franchise found for: " + searchEmail);
            }).addOnFailureListener(err -> errCb.onFailure(err.getMessage()));
        }).addOnFailureListener(err -> errCb.onFailure(err.getMessage()));
    }

    public void getAgentByPhone(String phone, FailureCallback errCb, DataCallback dataCb) {
        // Iterate over all franchises and query their "agents" subcollection directly
        franchisesRef().get().addOnSuccessListener(franchiseSnap -> {
            if (franchiseSnap == null || franchiseSnap.isEmpty()) {
                errCb.onFailure("No franchises found to search for agent.");
                return;
            }
            
            java.util.concurrent.atomic.AtomicInteger pendingQueries = new java.util.concurrent.atomic.AtomicInteger(franchiseSnap.size());
            java.util.concurrent.atomic.AtomicBoolean found = new java.util.concurrent.atomic.AtomicBoolean(false);

            for (DocumentSnapshot fDoc : franchiseSnap.getDocuments()) {
                fDoc.getReference().collection("agents")
                    .whereEqualTo("phone", phone)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (found.get()) return;

                        if (task.isSuccessful() && task.getResult() != null && !task.getResult().isEmpty()) {
                            if (found.compareAndSet(false, true)) {
                                Map<String, Object> data = task.getResult().getDocuments().get(0).getData();
                                if (data != null) {
                                    data.put("id", task.getResult().getDocuments().get(0).getId());
                                    if (!data.containsKey("franchiseId") || String.valueOf(data.get("franchiseId")).isEmpty()) {
                                        data.put("franchiseId", fDoc.getId());
                                    }
                                    dataCb.onData(data);
                                } else {
                                    errCb.onFailure("Agent data is empty");
                                }
                            }
                        } else {
                            fDoc.getReference().collection("agents")
                                .whereEqualTo("mobile", phone)
                                .get()
                                .addOnCompleteListener(task2 -> {
                                    if (found.get()) return;
                                    
                                    if (task2.isSuccessful() && task2.getResult() != null && !task2.getResult().isEmpty()) {
                                        if (found.compareAndSet(false, true)) {
                                            Map<String, Object> data = task2.getResult().getDocuments().get(0).getData();
                                            if (data != null) {
                                                data.put("id", task2.getResult().getDocuments().get(0).getId());
                                                if (!data.containsKey("franchiseId") || String.valueOf(data.get("franchiseId")).isEmpty()) {
                                                    data.put("franchiseId", fDoc.getId());
                                                }
                                                dataCb.onData(data);
                                            } else {
                                                errCb.onFailure("Agent data is empty");
                                            }
                                        }
                                    } else {
                                        if (pendingQueries.decrementAndGet() == 0 && !found.get()) {
                                            errCb.onFailure("Agent not found for phone: " + phone);
                                        }
                                    }
                                });
                        }
                    });
            }
        }).addOnFailureListener(e -> errCb.onFailure("Failed to fetch franchises: " + e.getMessage()));
    }

    public void getSocietiesForFranchise(String franchiseId, StringsCallback cb) {
        if (franchiseId == null || franchiseId.isEmpty()) {
            cb.onData(java.util.Arrays.asList("Error: empty franchiseId"));
            return;
        }

        franchisesRef().document(franchiseId).collection("areas")
            .get()
            .addOnSuccessListener(snap -> {
                List<String> names = new ArrayList<>();
                if (snap != null && !snap.isEmpty()) {
                    for (DocumentSnapshot doc : snap.getDocuments()) {
                        Object societiesObj = doc.get("societies");
                        if (societiesObj instanceof Map) {
                            Map<String, Object> societiesMap = (Map<String, Object>) societiesObj;
                            for (Object value : societiesMap.values()) {
                                if (value instanceof Map) {
                                    Map<String, Object> soc = (Map<String, Object>) value;
                                    String name = str(soc, "name");
                                    if (!name.isEmpty() && !names.contains(name)) names.add(name);
                                }
                            }
                        }
                    }
                }
                if (names.isEmpty()) {
                    names.add("Debug: 0 areas found or parsing failed for " + franchiseId + " snapSize=" + (snap != null ? snap.size() : "null"));
                }
                cb.onData(names);
            })
            .addOnFailureListener(e -> {
                cb.onData(java.util.Arrays.asList("Error querying areas: " + e.getMessage()));
            });
    }

    /**
     * One-shot method: given agent phone → fetch agent doc → get franchiseId
     * → query locations for that franchise → return all society names.
     */
    public void loadSocietiesForAgent(String phone, StringsCallback cb) {
        getAgentByPhone(phone,
            err -> {
                cb.onData(java.util.Arrays.asList("Agent fetch error: " + err));
            },
            data -> {
                Object locObj = data.get("locations");
                List<String> societies = new ArrayList<>();
                if (locObj instanceof List) {
                    for (Object o : (List<?>) locObj) {
                        String locStr = String.valueOf(o);
                        if (locStr.contains("::")) {
                            String[] parts = locStr.split("::");
                            if (parts.length > 1) {
                                societies.add(parts[1].trim());
                            } else {
                                societies.add(locStr.trim());
                            }
                        } else {
                            societies.add(locStr.trim());
                        }
                    }
                }
                
                if (societies.isEmpty()) {
                    cb.onData(java.util.Arrays.asList("No assigned societies"));
                } else {
                    cb.onData(societies);
                }
            });
    }

    // ── User name lookup ───────────────────────────────────────────────────

    public void fetchNameForPhone(String phone, NameCallback cb) {
        if (phone == null || phone.isEmpty()) { cb.onName("Unknown"); return; }
        if (nameCache.containsKey(phone))     { cb.onName(nameCache.get(phone)); return; }

        usersRef().document(phone).get().addOnSuccessListener(doc -> {
            String name = doc.getString("name");
            if (name != null && !name.isEmpty()) {
                nameCache.put(phone, name);
                cb.onName(name);
            } else {
                cb.onName(phone);
            }
        }).addOnFailureListener(e -> cb.onName(phone));
    }

    // ── Dashboard stats ────────────────────────────────────────────────────

    /**
     * Listen for total user count (all users, all franchises).
     * Path: app_data/users/users
     */
    public ListenerRegistration listenTotalUsers(CountCallback cb) {
        return db.collectionGroup(COL_USERS).addSnapshotListener((snap, e) -> {
            if (e != null) { Log.e(TAG, "listenTotalUsers: " + e.getMessage()); return; }
            if (snap != null) cb.onCount(snap.size());
        });
    }

    /** Total order count across all franchises — used by RevenueActivity. */
    public ListenerRegistration listenTotalOrders(CountCallback cb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (e != null) { Log.e(TAG, "listenTotalOrders: " + e.getMessage()); return; }
            if (snap != null) cb.onCount(snap.size());
        });
    }

    /**
     * Listen for user count for a specific franchise.
     * Fetches all users from app_data/users/users and filters client-side by franchiseId.
     */
    public ListenerRegistration listenFranchiseUsers(String franchiseId, CountCallback cb) {
        return db.collectionGroup(COL_USERS).addSnapshotListener((snap, e) -> {
            if (e != null) { Log.e(TAG, "listenFranchiseUsers: " + e.getMessage()); return; }
            if (snap == null) return;
            if (franchiseId == null || franchiseId.isEmpty()) {
                cb.onCount(snap.size());
                return;
            }
            int count = 0;
            String searchFranchise = franchiseId.trim();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                String fId = str(doc.getData(), "franchiseId").trim();
                if (searchFranchise.equals(fId)) count++;
            }
            Log.d(TAG, "Franchise users for " + searchFranchise + ": " + count + "/" + snap.size());
            cb.onCount(count);
        });
    }

    /**
     * Listen for pending pickup orders for a specific franchise.
     * Uses collectionGroup("orders") and filters client-side by franchiseId + status.
     */
    public ListenerRegistration listenFranchisePendingOrders(String franchiseId, CountCallback cb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (snap == null) return;
            int count = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (!matchesFranchise(data, franchiseId)) continue;
                String s = str(data, "status").toLowerCase().trim();
                if (s.isEmpty() || s.equals(STATUS_PENDING) || s.equals(STATUS_PICKING_PENDING)) count++;
            }
            cb.onCount(count);
        });
    }

    // Compatibility wrapper (no franchise filter)
    public ListenerRegistration listenPendingOrders(CountCallback cb) {
        return listenFranchisePendingOrders("", cb);
    }

    public ListenerRegistration listenPendingOrdersForFranchiseAndDate(String franchiseId, String date, CountCallback cb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (snap == null) return;
            int count = 0;
            String targetDate = date == null ? "" : date.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
            if (targetDate.startsWith("0") && targetDate.length() > 1 && Character.isDigit(targetDate.charAt(1))) {
                targetDate = targetDate.substring(1);
            }
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (!matchesFranchise(data, franchiseId)) continue;
                
                String pDate = str(data, "pickupDate");
                if (pDate.isEmpty()) pDate = str(data, "pickup_date");
                if (pDate.isEmpty()) pDate = str(data, "date");
                if (pDate.isEmpty()) pDate = str(data, "orderDate");
                String orderDate = pDate.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
                if (orderDate.startsWith("0") && orderDate.length() > 1 && Character.isDigit(orderDate.charAt(1))) {
                    orderDate = orderDate.substring(1);
                }
                
                if (!targetDate.isEmpty() && !orderDate.equals(targetDate)) continue;
                
                String s = str(data, "status").toLowerCase().trim();
                if (s.isEmpty() || s.equals(STATUS_PENDING) || s.equals(STATUS_PICKING_PENDING)) count++;
            }
            cb.onCount(count);
        });
    }

    /**
     * Listen for completed order count for a specific franchise.
     */
    public ListenerRegistration listenFranchiseCompletedOrders(String franchiseId, CountCallback cb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (snap == null) return;
            int count = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (!matchesFranchise(data, franchiseId)) continue;
                if (str(data, "status").toLowerCase().equals(STATUS_COMPLETED)) count++;
            }
            cb.onCount(count);
        });
    }

    // Compatibility wrapper (no franchise filter)
    public ListenerRegistration listenCompletedOrders(CountCallback cb) {
        return listenFranchiseCompletedOrders("", cb);
    }

    /**
     * Get total revenue for a specific franchise (one-shot).
     */
    public void getFranchiseRevenue(String franchiseId, FailureCallback errCb, CountCallback cb) {
        db.collectionGroup(COL_ORDERS).get().addOnSuccessListener(snap -> {
            long total = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = doc.getData();
                if (!matchesFranchise(data, franchiseId)) continue;
                if (!str(data, "status").toLowerCase().equals(STATUS_COMPLETED)) continue;
                Long amount = doc.getLong("totalAmount");
                if (amount != null) total += amount;
            }
            cb.onCount(total);
        }).addOnFailureListener(err -> errCb.onFailure(err.getMessage()));
    }

    // Compat
    public void getTotalRevenue(FailureCallback errCb, CountCallback cb) {
        getFranchiseRevenue("", errCb, cb);
    }

    /**
     * Listen for all orders for a specific franchise (real-time list).
     */
    public ListenerRegistration listenFranchiseOrders(String franchiseId, ListCallback cb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (e != null) { Log.e(TAG, "listenFranchiseOrders: " + e.getMessage()); return; }
            if (snap == null) return;
            List<Map<String, Object>> result = new ArrayList<>();
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = docToMapWithPhone(doc);
                if (matchesFranchise(data, franchiseId)) result.add(data);
            }
            cb.onData(result);
        });
    }

    // ── Real-time order lists for agents ───────────────────────────────────

    public ListenerRegistration listenAllOrdersForTasks(ListCallback cb) {
        return listenAllOrdersForTasks(null, cb, null);
    }

    public ListenerRegistration listenAllOrdersForTasks(String franchiseId, ListCallback cb, ErrorCallback errCb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (e != null) {
                Log.e(TAG, "listenAllOrdersForTasks: " + e.getMessage());
                if (errCb != null) errCb.onError(e.getMessage());
                return;
            }
            if (snap != null) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Map<String, Object> doc : docsToListWithPhone(snap)) {
                    if (matchesFranchise(doc, franchiseId)) {
                        result.add(doc);
                    }
                }
                cb.onData(result);
            }
        });
    }

    public ListenerRegistration listenAllPendingPickups(ListCallback cb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (snap == null) return;
            List<Map<String, Object>> pending = new ArrayList<>();
            for (Map<String, Object> doc : docsToListWithPhone(snap)) {
                String s = str(doc, "status").toLowerCase();
                if (s.isEmpty() || s.equals(STATUS_PENDING) || s.equals(STATUS_PICKING_PENDING))
                    pending.add(doc);
            }
            cb.onData(pending);
        });
    }

    public ListenerRegistration listenAllDeliveryOrders(ListCallback cb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (snap == null) return;
            List<Map<String, Object>> ready = new ArrayList<>();
            for (Map<String, Object> doc : docsToListWithPhone(snap)) {
                if (str(doc, "status").toLowerCase().equals(STATUS_PICKUP_DONE)) ready.add(doc);
            }
            cb.onData(ready);
        });
    }

    public ListenerRegistration listenCompletedDeliveries(ListCallback cb) {
        return db.collectionGroup(COL_ORDERS)
                .whereEqualTo("status", STATUS_COMPLETED)
                .addSnapshotListener((snap, e) -> {
                    if (snap != null) cb.onData(docsToListWithPhone(snap));
                });
    }

    // ── Listen all users (for UsersActivity) ──────────────────────────────

    public ListenerRegistration listenAllUsers(OrdersCallback cb) {
        return db.collectionGroup(COL_USERS).addSnapshotListener((snap, e) -> {
            if (e != null) { Log.e(TAG, "listenAllUsers: " + e.getMessage()); return; }
            if (snap != null) cb.onOrders(docsToList(snap));
        });
    }

    // ── Order actions ──────────────────────────────────────────────────────

    public void markPickupDone(String orderFullPath, String deliveryOtp, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_PICKUP_DONE);
        updates.put("deliveryOtp", deliveryOtp);
        updates.put("pickedAt", com.google.firebase.Timestamp.now());
        db.document(orderFullPath).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    public void saveDeliveryOtp(String orderFullPath, String deliveryOtp, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("deliveryOtp", deliveryOtp);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        db.document(orderFullPath).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    public void markOrderPickupDone(String orderFullPath, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_PROGRESS);
        updates.put("pickedAt", com.google.firebase.Timestamp.now());
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        db.document(orderFullPath).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    public void markOrderCompleted(String orderFullPath, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_COMPLETED);
        updates.put("completedAt", com.google.firebase.Timestamp.now());
        db.document(orderFullPath).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    public void markOrderIncomplete(String orderFullPath, String reason, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", STATUS_INCOMPLETE);
        updates.put("incompleteReason", reason);
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        db.document(orderFullPath).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    public void rescheduleOrder(String orderFullPath, String reason, ActionCallback cb) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("rescheduleReason", reason);
        updates.put("rescheduledAt", com.google.firebase.Timestamp.now());
        updates.put("updatedAt", com.google.firebase.Timestamp.now());
        db.document(orderFullPath).update(updates)
                .addOnSuccessListener(v -> cb.onSuccess())
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    public void verifyDeliveryOtp(String orderFullPath, String enteredOtp, ActionCallback cb) {
        db.document(orderFullPath).get()
                .addOnSuccessListener(doc -> {
                    String storedOtp = doc.getString("deliveryOtp");
                    if (enteredOtp.equals(storedOtp)) markOrderCompleted(orderFullPath, cb);
                    else cb.onFailure("Invalid OTP");
                })
                .addOnFailureListener(err -> cb.onFailure(err.getMessage()));
    }

    public void getOrderByPath(String orderFullPath, FailureCallback errCb, OrdersCallback dataCb) {
        db.document(orderFullPath).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        List<Map<String, Object>> list = new ArrayList<>();
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("id", doc.getId());
                            data.put("__path", doc.getReference().getPath());
                            String path = doc.getReference().getPath();
                            String[] parts = path.split("/");
                            for (int i = 0; i < parts.length - 1; i++) {
                                if (COL_USERS.equals(parts[i])) {
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

    // ── Compat: pending orders with date filter ────────────────────────────

    public ListenerRegistration listenPendingOrders(String targetDate, CountCallback cb) {
        return db.collectionGroup(COL_ORDERS).addSnapshotListener((snap, e) -> {
            if (snap == null) return;
            int count = 0;
            for (DocumentSnapshot doc : snap.getDocuments()) {
                Map<String, Object> data = doc.getData();
                String s = str(data, "status").toLowerCase().replace("_", " ").replace("-", " ").trim();
                String pDate = str(data, "pickupDate");
                if (pDate.isEmpty()) pDate = str(data, "date");
                if (pDate.isEmpty()) pDate = str(data, "orderDate");
                String dDate = str(data, "deliveryDate");
                String tNorm = targetDate.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
                String pNorm = pDate.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
                String dNorm = dDate.toLowerCase().replaceAll("[^a-z0-9]", " ").replaceAll("\\s+", " ").trim();
                boolean matchDate = tNorm.equals(pNorm) || tNorm.equals(dNorm);
                if (matchDate) {
                    boolean pending = s.isEmpty() || s.equals("pending") || s.equals("picking pending")
                            || s.equals("pickup done") || s.equals("ready") || s.equals("out for delivery")
                            || s.equals("washing") || s.equals("ironing");
                    if (pending) count++;
                }
            }
            cb.onCount(count);
        });
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Returns true if the order belongs to the given franchise (or franchiseId is empty = all). */
    private boolean matchesFranchise(Map<String, Object> data, String franchiseId) {
        if (franchiseId == null || franchiseId.isEmpty()) return true;
        return franchiseId.equals(str(data, "franchiseId"));
    }

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
        String path = doc.getReference().getPath();
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length - 1; i++) {
            if (COL_USERS.equals(parts[i])) {
                String phone = parts[i + 1];
                data.put("customerPhone", phone);
                if (!data.containsKey("customerName") || data.get("customerName") == null) {
                    data.put("customerName", phone);
                }
                break;
            }
        }
        return data;
    }

    public OrderServices parseOrderServices(List<Map<String, Object>> items) {
        List<ServiceItem> laundry = new ArrayList<>();
        List<ServiceItem> ironing = new ArrayList<>();
        List<ServiceItem> dryCleaning = new ArrayList<>();
        List<ServiceItem> shoeCare = new ArrayList<>();
        if (items == null) return new OrderServices(laundry, ironing, dryCleaning, shoeCare);
        for (Map<String, Object> itemMap : items) {
            String category = str(itemMap, "name").toLowerCase();
            String itemName = str(itemMap, "description");
            if (itemName.isEmpty()) itemName = str(itemMap, "itemName");
            Object qtyObj = itemMap.get("quantity");
            int qty = 0;
            if (qtyObj instanceof Long) qty = ((Long) qtyObj).intValue();
            else if (qtyObj instanceof Integer) qty = (Integer) qtyObj;
            else if (qtyObj instanceof String) { try { qty = Integer.parseInt((String) qtyObj); } catch (Exception ignored) {} }
            ServiceItem si = new ServiceItem(itemName, qty);
            if (category.contains("laundry") || category.contains("wash")) laundry.add(si);
            else if (category.contains("iron") || category.contains("press")) ironing.add(si);
            else if (category.contains("dry clean")) dryCleaning.add(si);
            else if (category.contains("shoe")) shoeCare.add(si);
            else laundry.add(si);
        }
        return new OrderServices(laundry, ironing, dryCleaning, shoeCare);
    }

    public static String str(Map<String, Object> map, String key) {
        if (map == null) return "";
        Object val = map.get(key);
        if (val == null && ("address".equals(key) || "society".equals(key))) val = map.get("5");
        return val != null ? val.toString() : "";
    }
}
