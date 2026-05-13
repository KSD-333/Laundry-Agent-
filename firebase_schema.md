# Laundry App Ecosystem: Firebase Database Structure

This document outlines the NoSQL (Firestore) database schema designed for the Laundry Delivery App ecosystem, which includes the Customer App, Delivery Agent App, and Franchise Admin Panel.

This schema is designed to be scalable, efficient for read/write operations, and similar to modern delivery apps like Blinkit, Swiggy, or Zomato.

---

## 1. `users` (Customers)
Stores all customer profile details. A subcollection `addresses` is used to allow a single customer to have multiple saved locations.

```json
Collection: "users"
Document: {user_uid}
{
  "firstName": "John",
  "lastName": "Doe",
  "phone": "+919876543210",
  "email": "john.doe@example.com",
  "profileImageUrl": "https://...",
  "walletBalance": 150.0,
  "fcmToken": "token_for_push_notifications",
  "createdAt": Timestamp(2026-05-13T10:00:00Z),
  
  // Subcollection: addresses
  "addresses": {
    Collection: "addresses"
    Document: {address_id}
    {
      "type": "Home",               // "Home", "Work", "Other"
      "addressLine1": "Flat 402, Building A",
      "addressLine2": "Main Street, Area Name",
      "society": "Green Valley Society",
      "landmark": "Near Apollo Pharmacy",
      "city": "Mumbai",
      "state": "Maharashtra",
      "pincode": "400001",
      "location": GeoPoint(19.0760, 72.8777), // Essential for Blinkit-like map tracking
      "isDefault": true
    }
  }
}
```

---

## 2. `agents` (Delivery & Pickup Agents)
Stores information about the delivery executives. It links them to specific franchises and tracks their live location.

```json
Collection: "agents"
Document: {agent_uid}
{
  "name": "Ramesh Kumar",
  "phone": "+919876543211",
  "email": "ramesh.agent@example.com",
  "profileImageUrl": "https://...",
  "vehicleNumber": "MH 12 AB 1234",
  "franchiseId": "franchise_001",    // Links agent to a specific franchise admin
  "status": "ONLINE",                // "ONLINE", "OFFLINE", "ON_DUTY"
  "currentLocation": GeoPoint(19.0765, 72.8780), // Updated via agent app periodically
  "totalEarnings": 4500.0,
  "fcmToken": "token_for_push_notifications",
  "createdAt": Timestamp(2026-05-01T10:00:00Z)
}
```

---

## 3. `franchises` (Admin Panel)
Stores the master data for each franchise branch that handles laundry processing.

```json
Collection: "franchises"
Document: {franchise_id}
{
  "name": "Aqua Flow Laundry - Andheri Branch",
  "ownerName": "Suresh Gupta",
  "phone": "+919876543212",
  "email": "andheri@aquaflow.com",
  "address": "Shop No 4, Main Market, Andheri East",
  "location": GeoPoint(19.1136, 72.8697),
  "coverageRadiusKm": 5.0,           // To filter which customers they serve
  "status": "ACTIVE",                // "ACTIVE", "INACTIVE"
  "operatingHours": {
    "open": "08:00 AM",
    "close": "09:00 PM"
  },
  "createdAt": Timestamp(2026-01-10T10:00:00Z)
}
```

---

## 4. `orders` (Core Transaction Data)
The most critical collection. It connects Customers, Agents, and Franchises. It stores snapshots of addresses so that if a customer updates their address later, historical orders aren't affected.

```json
Collection: "orders"
Document: {order_id}
{
  "orderId": "ORD-12345678",
  "customerId": "user_uid",
  "franchiseId": "franchise_001",
  
  // Assigned Agents
  "pickupAgentId": "agent_uid_1",
  "deliveryAgentId": "agent_uid_2", // Could be same or different
  
  // Order Status Flow
  "status": "PENDING_PICKUP", 
  // Statuses: PENDING_PICKUP -> PICKUP_IN_PROGRESS -> AT_FRANCHISE -> IN_WASHING -> READY_FOR_DELIVERY -> OUT_FOR_DELIVERY -> DELIVERED (or CANCELLED/INCOMPLETE)
  
  "incompleteReason": null, // E.g., "Customer not available", "Door locked"
  
  // Snapshot of Address
  "pickupAddress": {
    "addressLine": "Flat 402, Building A, Main Street",
    "society": "Green Valley Society",
    "phone": "+919876543210",
    "location": GeoPoint(19.0760, 72.8777)
  },
  "deliveryAddress": { ... }, // Same structure as pickup
  
  // Pricing and Items
  "totalAmount": 450.0,
  "paymentMethod": "CASH_ON_DELIVERY", // "ONLINE", "WALLET", "CASH_ON_DELIVERY"
  "paymentStatus": "PENDING",          // "PENDING", "COMPLETED"
  
  "items": [
    {
      "categoryName": "Dry Clean",
      "itemName": "Men's Suit",
      "quantity": 1,
      "pricePerUnit": 250.0,
      "totalPrice": 250.0
    },
    {
      "categoryName": "Wash & Iron",
      "itemName": "Shirt",
      "quantity": 2,
      "pricePerUnit": 100.0,
      "totalPrice": 200.0
    }
  ],
  
  // Timestamps
  "scheduledPickupTime": Timestamp(2026-05-14T10:00:00Z),
  "timestamps": {
    "placedAt": Timestamp(2026-05-13T10:00:00Z),
    "pickedUpAt": null,
    "deliveredAt": null
  }
}
```

---

## 5. `services` (Master Pricing List)
This data is fetched to show the catalog to the user in the Customer App.

```json
Collection: "services"
Document: {service_id}
{
  "name": "Dry Clean",
  "description": "Premium dry cleaning for delicate clothes.",
  "imageUrl": "https://...",
  "isActive": true,
  "orderIndex": 1,
  
  // Subcollection: items (Specific clothing items and their prices)
  "items": {
    Collection: "items"
    Document: {item_id}
    {
      "name": "Men's Suit (2 Pcs)",
      "price": 250.0,
      "iconUrl": "https://...",
      "isActive": true
    }
  }
}
```

---

## 6. `transactions` (Wallet & Payments)
Keeps track of wallet recharges and order payments for the customer and franchise accounting.

```json
Collection: "transactions"
Document: {transaction_id}
{
  "userId": "user_uid",
  "type": "WALLET_CREDIT",      // "WALLET_CREDIT", "ORDER_PAYMENT", "REFUND"
  "amount": 500.0,
  "status": "SUCCESS",          // "SUCCESS", "FAILED", "PENDING"
  "paymentGatewayRef": "PAY_123456",
  "relatedOrderId": null,       // If it's an order payment, link it here
  "timestamp": Timestamp(2026-05-13T10:05:00Z)
}
```

---

### Implementation Notes for Android/Firebase:
1. **GeoQueries:** Use `GeoFlutterFire` (if Flutter) or standard `GeoFire` (for Android Java/Kotlin) to query nearby franchises based on the user's selected address `GeoPoint`.
2. **References over Subcollections:** We use independent collections (`users`, `orders`, `agents`) rather than deep nesting. This makes querying globally much easier (e.g., "Find all PENDING orders for Franchise X").
3. **Data Duplication (Snapshotting):** Notice how `pickupAddress` is copied into the `order` document. This is a crucial NoSQL best practice. If the user deletes or changes their address in their profile next week, the old order receipt should still show the exact address where the delivery happened.
4. **Push Notifications:** The `fcmToken` in both User and Agent profiles allows Cloud Functions to trigger notifications when an order status changes (e.g., Agent gets notified when an order is placed, User gets notified when agent is "Out for delivery").
