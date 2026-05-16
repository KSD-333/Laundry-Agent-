# Laundry Agent App - Hierarchical Firebase Structure

This document defines a multi-tenant (Franchise-based) Firestore schema for a laundry service ecosystem involving Super Admins, Franchise Admins, Agents, and Customers.

---

## 1. `franchises` (Collection)
The core entity. All users, agents, and orders are scoped under a franchise.

| Field | Type | Description |
| :--- | :--- | :--- |
| `franchiseId` | `string` | Unique identifier (Document ID) |
| `name` | `string` | Name of the franchise/outlet |
| `ownerUid` | `string` | UID of the Franchise Admin |
| `location` | `map` | `city`, `area`, `geoPoint` |
| `status` | `string` | `active`, `suspended` |
| `settings` | `map` | `minOrderValue`, `operatingHours` |
| `createdAt` | `timestamp` | |

---

## 2. `users` (Collection)
Includes both Customers and Franchise Admins/Agents.

| Field | Type | Description |
| :--- | :--- | :--- |
| `uid` | `string` | Firebase Auth ID |
| `franchiseId` | `string` | **CRITICAL**: Scopes user to a specific outlet |
| `role` | `string` | `customer`, `franchise_admin`, `agent`, `super_admin` |
| `name` | `string` | Full name |
| `phone` | `string` | Contact number |
| `fcmToken` | `string` | For push notifications |
| `address` | `map` | `street`, `society`, `landmark`, `pincode`, `geoPoint` |

---

## 3. `orders` (Collection)
Structured to handle the multi-day lifecycle (Pickup -> Process -> Delivery).

| Field | Type | Description |
| :--- | :--- | :--- |
| `orderId` | `string` | Document ID |
| `franchiseId` | `string` | **CRITICAL**: Used by Admin to see only their orders |
| `customerId` | `string` | UID of the user |
| `agentId` | `string` | UID of the assigned agent (for pickup/delivery) |
| `status` | `string` | `PENDING`, `ASSIGNED`, `PICKED_UP`, `WASHING`, `READY`, `OUT_FOR_DELIVERY`, `COMPLETED` |
| `pickupDate` | `timestamp` | User-selected (e.g., Tomorrow/Next Day) |
| `deliveryDate`| `timestamp` | Usually 24-48 hours after pickup |
| `items` | `map` | Itemized list with qty and prices |
| `totalAmount` | `number` | Calculated bill |
| `paymentStatus`| `string` | `PENDING`, `PAID` |
| `createdAt` | `timestamp` | |
| `updatedAt` | `timestamp` | |

---

## 4. `agents_meta` (Collection)
Tracking specific agent data like availability and active tasks.

| Field | Type | Description |
| :--- | :--- | :--- |
| `agentId` | `string` | Reference to `users/{uid}` |
| `franchiseId` | `string` | The franchise they work for |
| `isOnline` | `boolean` | Current availability toggle |
| `currentOrder`| `string` | ID of the order currently being handled |
| `vehicleType` | `string` | `bike`, `scooter`, etc. |

---


## Role-Based Access Summary

### **Super Admin**
- **Access**: Global (`all franchises`, `all users`, `all orders`).
- **Capabilities**: Create new franchises, view global revenue, change any data.

### **Franchise Admin**
- **Access**: Scoped to their `franchiseId`.
- **Capabilities**: 
    - Add/Edit User addresses and Agent profiles.
    - Assign Agents to specific orders.
    - Monitor daily revenue for their branch.
    - Manage local pricing.

### **Agent**
- **Access**: Orders assigned to them within their `franchiseId`.
- **Capabilities**: 
    - View pickup list for "Tomorrow" or "Next Day".
    - Update order status to `PICKED_UP` or `COMPLETED`.
    - View customer location maps.

### **Customer (User)**
- **Access**: Their own orders and profile.
- **Capabilities**: 
    - Auto-assigned to nearest franchise based on location.
    - Select pickup/delivery slots.
    - Track real-time progress.

---

## 6. Logic Workflow (Order Lifecycle)

1.  **Placement**: Customer places order -> System attaches `franchiseId` based on user's location.
2.  **Assignment**: Franchise Admin sees the order -> Assigns an `agentId`.
3.  **Pickup**: Agent app shows order in "Upcoming Pickups" -> Agent visits on `pickupDate`.
4.  **Processing**: Laundry status changes to `WASHING` at the franchise center.
5.  **Delivery**: Once `READY`, Admin/System schedules delivery -> Agent delivers on `deliveryDate`.

---

## 7. Composite Indices (Required)
1.  `franchiseId` (ASC) + `status` (ASC) + `createdAt` (DESC) - *For Admin Dashboard*
2.  `agentId` (ASC) + `status` (ASC) + `pickupDate` (ASC) - *For Agent App*
3.  `customerId` (ASC) + `createdAt` (DESC) - *For User App history*
