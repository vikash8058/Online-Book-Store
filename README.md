# UC13 — Address Management, Payment Integration & Invoice Generation

## Branch
`feature/UC13-address-payment-invoice`

## Commit Messages
```
[Vikash Prajapati]: UC13a - Add address management with multiple addresses and default support
[Vikash Prajapati]: UC13b - Integrate Razorpay online payment and COD with checkout flow
[Vikash Prajapati]: UC13c - Add PDF invoice generation and download
```

---

## Overview

UC13 adds three connected features to the Online Book Store:

```
UC13a → Address Management   (user can save multiple delivery addresses)
UC13b → Payment Integration  (Razorpay online + Cash on Delivery)
UC13c → Invoice Generation   (PDF invoice download after order)
```

### Old Flow (before UC13)
```
Add to Cart → Place Order directly → Done
```

### New Flow (UC13)
```
Add to Cart
  → Proceed to Checkout
  → Select Delivery Address (or Add New)
  → Select Payment Method (COD or Online)
  → COD: Order created immediately
  → Online: Razorpay popup → Pay → Verify → Order created
  → Download PDF Invoice
```

---

## New Dependencies Added to pom.xml

```xml
<!-- Razorpay Java SDK -->
<dependency>
    <groupId>com.razorpay</groupId>
    <artifactId>razorpay-java</artifactId>
    <version>1.4.5</version>
</dependency>

<!-- iText PDF — invoice generation -->
<dependency>
    <groupId>com.itextpdf</groupId>
    <artifactId>itextpdf</artifactId>
    <version>5.5.13.3</version>
</dependency>
```

---

## New Environment Variables

### Backend — application.properties
```properties
# Razorpay (get from razorpay.com dashboard → Settings → API Keys → Test Mode)
razorpay.key.id=${RAZORPAY_KEY_ID}
razorpay.key.secret=${RAZORPAY_KEY_SECRET}
```

### Windows — set via PowerShell
```
setx RAZORPAY_KEY_ID "rzp_test_xxxxxxxxxx"
setx RAZORPAY_KEY_SECRET "your_secret_here"
```

### Frontend — .env (in React project root, same level as package.json)
```
REACT_APP_RAZORPAY_KEY_ID=rzp_test_xxxxxxxxxx
```

> Never put RAZORPAY_KEY_SECRET in frontend. Only Key ID goes in .env.
> Add .env to .gitignore so it never goes to GitHub.

### public/index.html — add Razorpay script in head tag
```html
<script src="https://checkout.razorpay.com/v1/checkout.js"></script>
```

---

## New Database Tables

### addresses
```
+------------------+-------------+------------------------------------------+
| Column           | Type        | Notes                                    |
+------------------+-------------+------------------------------------------+
| id               | BIGINT PK   | Auto-generated                           |
| user_id          | BIGINT FK   | References users.id                      |
| full_name        | VARCHAR     | Not null                                 |
| phone            | VARCHAR     | 10 digits, not null                      |
| address_line     | VARCHAR     | House no, street, area                   |
| city             | VARCHAR     | Not null                                 |
| state            | VARCHAR     | Not null                                 |
| pincode          | VARCHAR     | 6 digits, not null                       |
| is_default       | BOOLEAN     | true = default address for this user     |
| created_at       | DATETIME    | Auto-set                                 |
+------------------+-------------+------------------------------------------+
```

### payments
```
+----------------------+-------------+----------------------------------------+
| Column               | Type        | Notes                                  |
+----------------------+-------------+----------------------------------------+
| id                   | BIGINT PK   | Auto-generated                         |
| order_id             | BIGINT FK   | References orders.id — unique          |
| razorpay_order_id    | VARCHAR     | Null for COD                           |
| razorpay_payment_id  | VARCHAR     | Null for COD, filled after verify      |
| amount               | DOUBLE      | In INR                                 |
| payment_mode         | ENUM        | ONLINE or COD                          |
| status               | ENUM        | PENDING / SUCCESS / FAILED             |
| paid_at              | DATETIME    | Null for COD                           |
| created_at           | DATETIME    | Auto-set                               |
+----------------------+-------------+----------------------------------------+
```

### orders table — new columns added
```
+-------------------------+----------+------------------------------------------+
| Column                  | Type     | Notes                                    |
+-------------------------+----------+------------------------------------------+
| delivery_full_name      | VARCHAR  | Snapshotted from address at order time   |
| delivery_phone          | VARCHAR  | Snapshotted                              |
| delivery_address_line   | VARCHAR  | Snapshotted                              |
| delivery_city           | VARCHAR  | Snapshotted                              |
| delivery_state          | VARCHAR  | Snapshotted                              |
| delivery_pincode        | VARCHAR  | Snapshotted                              |
+-------------------------+----------+------------------------------------------+
```

> Why snapshot address on Order instead of storing address_id as FK?
> If user deletes their address later, order history would break.
> By copying address fields directly onto the order, history is preserved forever.

---

## New Enums

### PaymentMode.java
```java
public enum PaymentMode {
    ONLINE,
    COD
}
```

### PaymentStatus.java
```java
public enum PaymentStatus {
    PENDING,    // COD starts here / ONLINE before payment
    SUCCESS,    // ONLINE after Razorpay verification
    FAILED      // ONLINE if payment fails
}
```

---

## New Files — Backend

```
model/
  Address.java
  Payment.java
  PaymentMode.java
  PaymentStatus.java

repository/
  AddressRepository.java
  PaymentRepository.java

dto/request/
  AddressRequest.java
  PlaceOrderRequest.java
  PaymentVerifyRequest.java

dto/response/
  AddressResponse.java
  PaymentResponse.java

service/
  AddressService.java
  PaymentService.java
  InvoiceService.java

controller/
  AddressController.java
  PaymentController.java
  InvoiceController.java
```

## Modified Files — Backend

```
model/Order.java               → added 6 delivery address fields + payment OneToOne
model/OrderStatus.java         → no change
dto/response/OrderResponse.java → added delivery address fields + paymentMode + paymentStatus
service/OrderService.java      → toResponse() made public + includes payment info
SecurityConfig.java            → added address, payment, invoice endpoint permissions
pom.xml                        → added Razorpay + iText dependencies
application.properties         → added Razorpay keys
```

---

## API Endpoints — UC13

### Address Endpoints (CUSTOMER only)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/address/my` | Get all my addresses |
| POST | `/api/address/add` | Add new address |
| PATCH | `/api/address/update/{id}` | Update an address |
| DELETE | `/api/address/delete/{id}` | Delete an address |
| PATCH | `/api/address/default/{id}` | Set as default address |

### Payment Endpoints (CUSTOMER only)

| Method | URL | Description |
|--------|-----|-------------|
| POST | `/api/payment/initiate` | COD: creates order. ONLINE: creates Razorpay order |
| POST | `/api/payment/verify?addressId={id}` | Verify Razorpay signature and create order |

### Invoice Endpoint (CUSTOMER only)

| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/orders/{id}/invoice` | Download PDF invoice |

---

## SecurityConfig.java — new lines added

```java
// Add in CUSTOMER section
.requestMatchers("/api/address/**").hasRole("CUSTOMER")
.requestMatchers("/api/payment/**").hasRole("CUSTOMER")
.requestMatchers("/api/orders/*/invoice").hasRole("CUSTOMER")
```

---

## Request / Response Examples

### POST /api/address/add

Request:
```json
{
  "fullName": "Vikash Prajapati",
  "phone": "9876543210",
  "addressLine": "123 MG Road, Vijay Nagar",
  "city": "Indore",
  "state": "Madhya Pradesh",
  "pincode": "452010",
  "isDefault": true
}
```

Response:
```json
{
  "id": 1,
  "fullName": "Vikash Prajapati",
  "phone": "9876543210",
  "addressLine": "123 MG Road, Vijay Nagar",
  "city": "Indore",
  "state": "Madhya Pradesh",
  "pincode": "452010",
  "isDefault": true
}
```

---

### POST /api/payment/initiate — COD

Request:
```json
{
  "addressId": 1,
  "paymentMode": "COD"
}
```

Response (OrderResponse directly):
```json
{
  "id": 5,
  "status": "PENDING",
  "totalAmount": 998.0,
  "paymentMode": "COD",
  "paymentStatus": "PENDING",
  "deliveryFullName": "Vikash Prajapati",
  "deliveryCity": "Indore",
  ...
}
```

---

### POST /api/payment/initiate — ONLINE

Request:
```json
{
  "addressId": 1,
  "paymentMode": "ONLINE"
}
```

Response (PaymentResponse — NOT order yet):
```json
{
  "razorpayOrderId": "order_Pxxxxxxxxxxxxxxx",
  "amount": 998.0,
  "paymentMode": "ONLINE",
  "status": "PENDING"
}
```

> Frontend receives razorpayOrderId and opens Razorpay popup.
> After payment success, Razorpay returns 3 values to frontend handler.

---

### POST /api/payment/verify?addressId=1

Request:
```json
{
  "razorpayOrderId": "order_Pxxxxxxxxxxxxxxx",
  "razorpayPaymentId": "pay_Pxxxxxxxxxxxxxxx",
  "razorpaySignature": "abc123def456..."
}
```

Response (OrderResponse — order created now):
```json
{
  "id": 6,
  "status": "PENDING",
  "totalAmount": 998.0,
  "paymentMode": "ONLINE",
  "paymentStatus": "SUCCESS",
  ...
}
```

---

## How Razorpay Signature Verification Works

```
1. Razorpay sends: razorpay_order_id, razorpay_payment_id, razorpay_signature
2. Backend computes: HMAC-SHA256(razorpayOrderId + "|" + razorpayPaymentId, KEY_SECRET)
3. If computed signature == razorpay_signature → payment is genuine
4. If not equal → reject (someone tampered with the request)
```

This is the standard Razorpay security verification. Never skip this step.

---

## New Files — Frontend

```
src/
  api/
    addressApi.js       → all address API calls
    paymentApi.js       → initiatePayment + verifyPayment
  pages/
    AddressPage.js      → manage addresses (add, edit, delete, set default)
    CheckoutPage.js     → select address + payment method + place order
  styles/
    Address.css
    Checkout.css
```

## Modified Files — Frontend

```
src/
  pages/
    CartPage.js         → "Place Order" button now navigates to /checkout
    OrdersPage.js       → shows delivery address + payment mode + invoice button
  App.js                → added /checkout and /address routes
  Navbar.js             → added "My Addresses" link in CUSTOMER sidebar
  public/index.html     → added Razorpay checkout script in head
  .env                  → added REACT_APP_RAZORPAY_KEY_ID
```

---

## Address Logic — Key Rules

| Situation | What happens |
|-----------|-------------|
| User adds first address | Auto-set as default |
| User adds second address with isDefault=true | Old default loses default, new one gets it |
| User deletes default address | Next remaining address auto-becomes default |
| User has no addresses on checkout | Redirected to /address to add one |
| Checkout loads | Default address auto-selected |

---

## Payment Flow Diagrams

### COD Flow
```
CheckoutPage
  → User selects COD
  → POST /api/payment/initiate { addressId, paymentMode: "COD" }
  → Backend: buildOrderFromCart() → save order → save payment (COD/PENDING) → clear cart
  → Returns: OrderResponse
  → Frontend: shows success → navigate to /orders
```

### Online (Razorpay) Flow
```
CheckoutPage
  → User selects Online
  → POST /api/payment/initiate { addressId, paymentMode: "ONLINE" }
  → Backend: creates Razorpay order (payment intent only, NOT our order yet)
  → Returns: PaymentResponse { razorpayOrderId, amount }
  → Frontend: opens Razorpay popup with razorpayOrderId
  → User pays via UPI / Card / Net Banking
  → Razorpay calls handler with { razorpay_order_id, razorpay_payment_id, razorpay_signature }
  → POST /api/payment/verify?addressId=1 { razorpayOrderId, razorpayPaymentId, razorpaySignature }
  → Backend: verifies HMAC-SHA256 signature → builds order → saves payment (ONLINE/SUCCESS) → clears cart
  → Returns: OrderResponse
  → Frontend: shows success → navigate to /orders
```

---

## Invoice PDF — What It Contains

```
+------------------------------------------+
|  📚 Book Wala           Invoice #INV-00005 |
|  Your Online Book Store    Date: 09 Apr 2026|
|------------------------------------------|
|  Order Details     |  Payment Details    |
|  Order ID: #5      |  Mode: COD          |
|  Date: ...         |  Status: PENDING    |
|  Status: PENDING   |                     |
|------------------------------------------|
|  Delivery Address                        |
|  Vikash Prajapati                        |
|  123 MG Road, Indore, MP - 452010        |
|------------------------------------------|
|  Book Title        Qty  Unit Price Total |
|  Clean Code         2   Rs.499.00  Rs.998|
|  Spring Boot        1   Rs.299.00  Rs.299|
|------------------------------------------|
|                    Subtotal:   Rs.1297.00|
|                    Delivery:   FREE      |
|                    Grand Total:Rs.1297.00|
|------------------------------------------|
|  Thank you for shopping with Book Wala!  |
+------------------------------------------+
```

Generated using iText PDF library. Returned as byte array, downloaded as:
`invoice-order-{id}.pdf`

---

## Razorpay Test Mode

Use these test credentials to simulate payments (no real money):

| Field | Value |
|-------|-------|
| Test Card Number | 4111 1111 1111 1111 |
| Expiry | Any future date |
| CVV | Any 3 digits |
| OTP | 1234 (if asked) |

Test UPI: `success@razorpay`
Test UPI (failure): `failure@razorpay`

---

## Postman Test Cases — UC13

### Setup
```
Login as CUSTOMER → copy token → set as {{token}}
```

### Address Tests

**TC-01 — Add First Address (auto default)**
```
POST {{baseUrl}}/api/address/add
Authorization: Bearer {{token}}
Body:
{
  "fullName": "Vikash Prajapati",
  "phone": "9876543210",
  "addressLine": "123 MG Road",
  "city": "Indore",
  "state": "Madhya Pradesh",
  "pincode": "452010",
  "isDefault": false
}
Expected: 201 Created, isDefault = true (first address auto-default)
```

**TC-02 — Add Second Address with isDefault true**
```
POST {{baseUrl}}/api/address/add
Body: { ...same fields..., "isDefault": true }
Expected: 201, new address isDefault=true, first address isDefault=false
```

**TC-03 — Get All My Addresses**
```
GET {{baseUrl}}/api/address/my
Authorization: Bearer {{token}}
Expected: 200, array of addresses
```

**TC-04 — Set Different Address as Default**
```
PATCH {{baseUrl}}/api/address/default/1
Authorization: Bearer {{token}}
Expected: 200, address 1 isDefault=true, others false
```

**TC-05 — Delete Default Address**
```
DELETE {{baseUrl}}/api/address/delete/1
Authorization: Bearer {{token}}
Expected: 204, next address auto-becomes default
```

**ERROR — Invalid Phone (not 10 digits)**
```
POST /api/address/add with "phone": "123"
Expected: 400 Validation error
```

**ERROR — Invalid Pincode (not 6 digits)**
```
POST /api/address/add with "pincode": "1234"
Expected: 400 Validation error
```

---

### Payment Tests

**TC-06 — Place COD Order**
```
POST {{baseUrl}}/api/payment/initiate
Authorization: Bearer {{token}}
Body: { "addressId": 1, "paymentMode": "COD" }
Expected: 200 OrderResponse with paymentMode=COD, paymentStatus=PENDING
Verify: GET /api/cart → items should be empty (cart cleared)
Verify: GET /api/books/get/1 → stock reduced
```

**TC-07 — Initiate Online Payment**
```
POST {{baseUrl}}/api/payment/initiate
Authorization: Bearer {{token}}
Body: { "addressId": 1, "paymentMode": "ONLINE" }
Expected: 200 PaymentResponse with razorpayOrderId
Note: Use the razorpayOrderId in Razorpay frontend popup
```

**TC-08 — Verify Payment (after Razorpay success)**
```
POST {{baseUrl}}/api/payment/verify?addressId=1
Authorization: Bearer {{token}}
Body:
{
  "razorpayOrderId": "order_xxx",
  "razorpayPaymentId": "pay_xxx",
  "razorpaySignature": "abc123..."
}
Expected: 200 OrderResponse with paymentMode=ONLINE, paymentStatus=SUCCESS
```

**ERROR — Empty Cart on Initiate**
```
POST /api/payment/initiate (cart is empty)
Expected: 400 "Cart is empty. Add items before checkout."
```

**ERROR — Wrong Address ID**
```
POST /api/payment/initiate with addressId: 99999
Expected: 400 "Address not found"
```

**ERROR — Another User's Address**
```
POST /api/payment/initiate with addressId belonging to different user
Expected: 400 "Not your address"
```

---

### Invoice Tests

**TC-09 — Download Invoice**
```
GET {{baseUrl}}/api/orders/1/invoice
Authorization: Bearer {{token}}
Expected: 200, Content-Type: application/pdf
         File downloads as invoice-order-1.pdf
```

**ERROR — Invoice for Non-Existent Order**
```
GET {{baseUrl}}/api/orders/99999/invoice
Expected: 404 Order not found
```

---

## Database Verification Queries

After placing COD order:
```sql
SELECT * FROM addresses;
SELECT * FROM payments;
SELECT * FROM orders WHERE id = (SELECT MAX(id) FROM orders);
SELECT * FROM cart_items;   -- should be empty after order
SELECT stock FROM books WHERE id = 1;  -- should be reduced
```

---

## Frontend Checkout Flow Summary

```
/cart
  → "Proceed to Checkout" button → navigate('/checkout')

/checkout (CheckoutPage.js)
  → Loads addresses + cart on mount
  → If no address → redirect to /address
  → If cart empty → redirect to /cart
  → User selects address (default auto-selected)
  → User selects COD or Online
  → COD: calls initiatePayment(addressId, "COD")
       → order created → success notification → navigate('/orders')
  → Online: calls initiatePayment(addressId, "ONLINE")
          → gets razorpayOrderId → opens Razorpay popup
          → on payment success → calls verifyPayment(...)
          → order created → success notification → navigate('/orders')

/orders (OrdersPage.js)
  → Shows all orders with status, address, payment info
  → "Cancel Order" button for PENDING orders
  → "Download Invoice" button for all orders
```

---

## Key Design Decisions

**Why address snapshot on Order?**
Storing address fields directly on the order (not as FK) ensures order history is preserved even if user later deletes or updates their address. Amazon uses the same pattern.

**Why two-step payment for Online?**
Razorpay requires creating a payment order on backend first (to get razorpayOrderId), then the frontend opens the popup. The actual DB order is only created after payment is verified — this prevents creating orders for failed payments.

**Why HMAC-SHA256 signature verification?**
Without this check, anyone could send a fake verify request with a fake payment ID and get an order for free. Verifying the signature proves the payment was genuinely processed by Razorpay.

**Why COD payment status stays PENDING?**
For COD, money is collected at delivery. The payment status changes to SUCCESS only when Admin marks order as DELIVERED (can be added as enhancement later).

---

