# 📚 Online Book Store — REST API
# feature/UC1-REST API

A clean, minimal, and extensible **Online Book Store REST API** built with **Spring Boot**.
Follows strict **layered architecture** and **best practices** so that features like
JWT Security, Pagination, Caching, and Payment Integration can be added easily in future.

---

## 🧰 Tech Stack

| Technology        | Purpose                       |
|-------------------|-------------------------------|
| Java 17           | Core programming language     |
| Spring Boot 3.2.0 | Application framework         |
| Spring Web        | REST API layer                |
| Spring Data JPA   | Database abstraction layer    |
| Hibernate         | ORM / SQL generation          |
| MySQL             | Relational database           |
| Lombok            | Boilerplate code reduction    |
| Bean Validation   | Request input validation      |
| Maven             | Build and dependency tool     |

---

## 🏗️ Architecture

This project follows a strict **Layered Architecture**.
```
Client Request
      ↓
Controller       → Receives HTTP request, validates input, returns response
      ↓
Service          → Contains all business logic
      ↓
Repository       → Talks to the database via Spring Data JPA
      ↓
Database         → MySQL stores all data
```

### Package Structure
```
com.bookstore
├── controller        → REST controllers (HTTP layer)
├── service           → Business logic
├── repository        → Database access (JPA)
├── model             → JPA Entities + Enums
├── dto               → Request and Response DTOs
└── exception         → Custom exceptions + Global handler
```

---

## 📁 Complete File Structure
```
bookstore-api/
├── pom.xml
└── src/main/
    ├── java/com/bookstore/
    │   ├── BookstoreApiApplication.java
    │   ├── controller/
    │   │   ├── BookController.java
    │   │   ├── UserController.java
    │   │   └── OrderController.java
    │   ├── service/
    │   │   ├── BookService.java
    │   │   ├── UserService.java
    │   │   └── OrderService.java
    │   ├── repository/
    │   │   ├── BookRepository.java
    │   │   ├── UserRepository.java
    │   │   ├── OrderRepository.java
    │   │   └── OrderItemRepository.java
    │   ├── model/
    │   │   ├── Book.java
    │   │   ├── User.java
    │   │   ├── Order.java
    │   │   ├── OrderItem.java
    │   │   ├── Role.java
    │   │   └── OrderStatus.java
    │   ├── dto/
    │   │   ├── BookRequest.java
    │   │   ├── BookResponse.java
    │   │   ├── BookUpdateRequest.java
    │   │   ├── UserRegisterRequest.java
    │   │   ├── UserResponse.java
    │   │   ├── OrderRequest.java
    │   │   ├── OrderItemRequest.java
    │   │   ├── OrderResponse.java
    │   │   └── OrderItemResponse.java
    │   └── exception/
    │       ├── BookNotFoundException.java
    │       ├── UserNotFoundException.java
    │       ├── OrderNotFoundException.java
    │       ├── ErrorResponse.java
    │       └── GlobalExceptionHandler.java
    └── resources/
        └── application.properties
```

---

## ⚙️ Getting Started

### Prerequisites

Make sure you have the following installed on your machine:

- Java 17+
- Maven 3.8+
- MySQL 8.0+
- Postman (for testing)
- SPringToolSuite(STS) (recommended)

---

### Step 1 — Clone the Repository
```bash
git clone https://github.com/vikash8058/online-book-store.git
cd online-book-store
```

---

### Step 2 — Create MySQL Database

Open your MySQL client and run:
```sql
CREATE DATABASE bookstore_db;
```

---

### Step 3 — Configure Database

Open `src/main/resources/application.properties` and update your credentials:
```properties
# Server
server.port=8080

# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/bookstore_db
spring.datasource.username=root
spring.datasource.password=yourpassword
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# JPA / Hibernate
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
```

---

### Step 4 — Run the Application
```bash
mvn clean install
mvn spring-boot:run
```

Server starts at:
```
http://localhost:8080
```

---

## 🗄️ Database Schema

Hibernate auto-creates all tables on startup via `ddl-auto=update`.
```
books
├── id            BIGINT (PK, AUTO_INCREMENT)
├── title         VARCHAR (NOT NULL)
├── author_name   VARCHAR (NOT NULL)
├── price         DOUBLE  (NOT NULL)
└── stock         INTEGER (NOT NULL)

users
├── id            BIGINT (PK, AUTO_INCREMENT)
├── name          VARCHAR (NOT NULL)
├── email         VARCHAR (NOT NULL, UNIQUE)
├── password      VARCHAR (NOT NULL)
└── role          VARCHAR (NOT NULL) → ADMIN | CUSTOMER

orders
├── id            BIGINT   (PK, AUTO_INCREMENT)
├── order_date    DATETIME (NOT NULL)
├── total_amount  DOUBLE   (NOT NULL)
├── status        VARCHAR  (NOT NULL) → PENDING | CONFIRMED | SHIPPED | DELIVERED | CANCELLED
└── user_id       BIGINT   (FK → users.id)

order_items
├── id            BIGINT  (PK, AUTO_INCREMENT)
├── quantity      INTEGER (NOT NULL)
├── price         DOUBLE  (NOT NULL)
├── order_id      BIGINT  (FK → orders.id)
└── book_id       BIGINT  (FK → books.id)
```

---

## 🔑 Enums

### Role
```
ADMIN       → Admin of the system
CUSTOMER    → Registered user (assigned by default on registration)
```

### OrderStatus
```
PENDING     → Order just placed
CONFIRMED   → Order confirmed
SHIPPED     → Order shipped
DELIVERED   → Order delivered
CANCELLED   → Order cancelled
```

---

## 🌐 API Endpoints

### 📖 Book APIs

| Method   | Endpoint                          | Description                | Body Required |
|----------|-----------------------------------|----------------------------|---------------|
| `POST`   | `/api/books`                      | Create a new book          | Yes           |
| `GET`    | `/api/books`                      | Get all books              | No            |
| `GET`    | `/api/books/{id}`                 | Get book by ID             | No            |
| `PUT`    | `/api/books/{id}`                 | Full update (all fields)   | Yes           |
| `PATCH`  | `/api/books/{id}`                 | Partial update (any field) | Yes           |
| `DELETE` | `/api/books/{id}`                 | Delete book by ID          | No            |
| `GET`    | `/api/books/search?title={title}` | Search books by title      | No            |

---

### 👤 User APIs

| Method   | Endpoint              | Description         | Body Required |
|----------|-----------------------|---------------------|---------------|
| `POST`   | `/api/users/register` | Register a new user | Yes           |
| `GET`    | `/api/users`          | Get all users       | No            |
| `GET`    | `/api/users/{id}`     | Get user by ID      | No            |
| `DELETE` | `/api/users/{id}`     | Delete user by ID   | No            |

---

### 🛒 Order APIs

| Method   | Endpoint               | Description             | Body Required |
|----------|------------------------|-------------------------|---------------|
| `POST`   | `/api/orders/{userId}` | Create order for a user | Yes           |
| `GET`    | `/api/orders`          | Get all orders          | No            |
| `GET`    | `/api/orders/{id}`     | Get order by ID         | No            |
| `DELETE` | `/api/orders/{id}`     | Delete order by ID      | No            |

---

## 📦 Sample Requests and Responses

### Create a Book
**POST** `/api/books`
```json
// Request
{
  "title": "Clean Code",
  "authorName": "Robert C. Martin",
  "price": 29.99,
  "stock": 50
}

// Response 201 Created
{
  "id": 1,
  "title": "Clean Code",
  "authorName": "Robert C. Martin",
  "price": 29.99,
  "stock": 50
}
```

---

### Partial Update a Book
**PATCH** `/api/books/1`
```json
// Request — send only the fields you want to change
{
  "price": 19.99
}

// Response 200 OK — only price changed, rest stays same
{
  "id": 1,
  "title": "Clean Code",
  "authorName": "Robert C. Martin",
  "price": 19.99,
  "stock": 50
}
```

---

### Register a User
**POST** `/api/users/register`
```json
// Request
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secure123"
}

// Response 201 Created
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

---

### Create an Order
**POST** `/api/orders/1`
```json
// Request
{
  "items": [
    { "bookId": 1, "quantity": 2 },
    { "bookId": 2, "quantity": 1 }
  ]
}

// Response 201 Created
{
  "id": 1,
  "orderDate": "2024-01-15T10:30:00",
  "totalAmount": 89.97,
  "status": "PENDING",
  "userId": 1,
  "items": [
    {
      "id": 1,
      "bookId": 1,
      "bookTitle": "Clean Code",
      "quantity": 2,
      "price": 29.99
    },
    {
      "id": 2,
      "bookId": 2,
      "bookTitle": "The Pragmatic Programmer",
      "quantity": 1,
      "price": 39.99
    }
  ]
}
```

---

## ✅ Validation Rules

### BookRequest — POST `/api/books`

| Field      | Rule                        |
|------------|-----------------------------|
| title      | Must not be blank           |
| authorName | Must not be blank           |
| price      | Must not be null, positive  |
| stock      | Must not be null, min 0     |

### BookUpdateRequest — PATCH `/api/books/{id}`

| Field      | Rule                               |
|------------|------------------------------------|
| title      | Optional, send only to update      |
| authorName | Optional, send only to update      |
| price      | Optional, must be positive if sent |
| stock      | Optional, must be min 0 if sent    |

### UserRegisterRequest — POST `/api/users/register`

| Field    | Rule                                  |
|----------|---------------------------------------|
| name     | Must not be blank                     |
| email    | Must not be blank, valid email format |
| password | Must not be blank, min 6, max 100     |

### OrderItemRequest

| Field    | Rule                    |
|----------|-------------------------|
| bookId   | Must not be null        |
| quantity | Must not be null, min 1 |

### OrderRequest

| Field | Rule                          |
|-------|-------------------------------|
| items | Must not be empty, min 1 item |

---

## 🚨 Error Responses

All errors return this structure:
```json
{
  "message": "Reason for error",
  "statusCode": 404,
  "timestamp": "2024-01-15T10:30:00"
}
```

### Error Scenarios

| Scenario             | Status | Message                       |
|----------------------|--------|-------------------------------|
| Book ID not found    | 404    | Book not found with id: {id}  |
| User ID not found    | 404    | User not found with id: {id}  |
| Order ID not found   | 404    | Order not found with id: {id} |
| Validation failure   | 400    | field: validation message     |
| Unexpected error     | 500    | An unexpected error occurred  |

---

## 🧪 Postman Testing Guide

### Setup
```
Base URL   : http://localhost:8080
Header Key : Content-Type
Header Val : application/json
```

---

### Book API Tests

#### Create Books (run these first)
```
POST http://localhost:8080/api/books
Body: { "title": "Clean Code", "authorName": "Robert C. Martin", "price": 29.99, "stock": 50 }

POST http://localhost:8080/api/books
Body: { "title": "The Pragmatic Programmer", "authorName": "Andrew Hunt", "price": 39.99, "stock": 30 }

POST http://localhost:8080/api/books
Body: { "title": "Spring Boot in Action", "authorName": "Craig Walls", "price": 49.99, "stock": 20 }
```

#### Read, Update, Delete
```
GET    http://localhost:8080/api/books           → get all books
GET    http://localhost:8080/api/books/1         → get book by id
GET    http://localhost:8080/api/books/999       → 404 not found
PUT    http://localhost:8080/api/books/1         → full update (all fields required)
PATCH  http://localhost:8080/api/books/1         → partial update (any field)
GET    http://localhost:8080/api/books/search?title=Clean Code
DELETE http://localhost:8080/api/books/3         → delete book
```

#### Validation Error Tests
```
POST http://localhost:8080/api/books
Body: { "title": "", "price": -10 }
Expected: 400 Bad Request
```

---

### User API Tests

#### Register Users (run these before orders)
```
POST http://localhost:8080/api/users/register
Body: { "name": "John Doe", "email": "john@example.com", "password": "secure123" }

POST http://localhost:8080/api/users/register
Body: { "name": "Jane Smith", "email": "jane@example.com", "password": "password123" }
```

#### Read, Delete
```
GET    http://localhost:8080/api/users           → get all users
GET    http://localhost:8080/api/users/1         → get user by id
GET    http://localhost:8080/api/users/999       → 404 not found
DELETE http://localhost:8080/api/users/2         → delete user
```

#### Validation Error Tests
```
POST http://localhost:8080/api/users/register
Body: { "name": "Test", "email": "not-valid-email", "password": "pass123" }
Expected: 400 Bad Request — invalid email

POST http://localhost:8080/api/users/register
Body: { "name": "Test", "email": "test@example.com", "password": "abc" }
Expected: 400 Bad Request — password too short
```

---

### Order API Tests

#### Create Orders (need books + users created first)
```
POST http://localhost:8080/api/orders/1
Body:
{
  "items": [
    { "bookId": 1, "quantity": 2 },
    { "bookId": 2, "quantity": 1 }
  ]
}
```

#### Read, Delete
```
GET    http://localhost:8080/api/orders          → get all orders
GET    http://localhost:8080/api/orders/1        → get order by id
GET    http://localhost:8080/api/orders/999      → 404 not found
DELETE http://localhost:8080/api/orders/1        → delete order
```

#### Exception + Validation Tests
```
POST http://localhost:8080/api/orders/999        → 404 user not found
Body: { "items": [{ "bookId": 1, "quantity": 1 }] }

POST http://localhost:8080/api/orders/1          → 404 book not found
Body: { "items": [{ "bookId": 999, "quantity": 1 }] }

POST http://localhost:8080/api/orders/1          → 400 empty items
Body: { "items": [] }
```

---

### Complete Testing Checklist
```
BOOKS
□ POST   /api/books              → Create Book 1
□ POST   /api/books              → Create Book 2
□ POST   /api/books              → Create Book 3
□ GET    /api/books              → Get all books
□ GET    /api/books/1            → Get book by ID
□ GET    /api/books/999          → 404 error
□ PUT    /api/books/1            → Full update
□ PATCH  /api/books/1            → Update price only
□ PATCH  /api/books/1            → Update stock only
□ GET    /api/books/search?title → Search by title
□ POST   /api/books (invalid)    → 400 validation error
□ DELETE /api/books/3            → Delete book

USERS
□ POST   /api/users/register     → Register User 1
□ POST   /api/users/register     → Register User 2
□ GET    /api/users              → Get all users
□ GET    /api/users/1            → Get user by ID
□ GET    /api/users/999          → 404 error
□ POST   /api/users (bad email)  → 400 validation error
□ POST   /api/users (short pass) → 400 validation error
□ DELETE /api/users/2            → Delete user

ORDERS
□ POST   /api/orders/1              → Create order (book 1 + book 2)
□ POST   /api/orders/999            → 404 user not found
□ POST   /api/orders/1 (bad bookId) → 404 book not found
□ POST   /api/orders/1 (empty list) → 400 validation error
□ GET    /api/orders                → Get all orders
□ GET    /api/orders/1              → Get order by ID
□ GET    /api/orders/999            → 404 error
□ DELETE /api/orders/1              → Delete order
```

---

## 💡 Key Design Decisions

| Decision | Reason |
|---|---|
| DTOs instead of Entities in controllers | Prevents exposing internal fields like password to client |
| `@Builder` instead of `@Data` on entities | `@Data` causes `equals/hashCode` and `toString` issues with JPA |
| Constructor injection instead of `@Autowired` | Easier to test, fields can be `final`, follows best practices |
| `Role` and `OrderStatus` as Enums | Type safety — prevents invalid values and typos |
| `@Enumerated(EnumType.STRING)` | Stores readable strings like `PENDING` instead of numbers in DB |
| `PUT` for full update | Replaces entire resource — all fields required |
| `PATCH` for partial update | Updates only sent fields — rest stay unchanged |
| `toResponse()` helper in each service | Single place to convert entity to DTO — clean and reusable |
| `GlobalExceptionHandler` | Centralized error handling — consistent JSON error response |
| `@Builder.Default` on `orderItems` | Prevents NullPointerException when list not set via builder |
| Price stored in `OrderItem` | Historical price preserved even if book price changes later |

---

## 🎯 Project Outcome

### What Was Built
- **14 REST endpoints** covering Books, Users, and Orders
- **Clean 6-layer architecture** with strict separation of concerns
- **DTO-based API** — entities never exposed directly to clients
- **Two Enums** — `Role` (ADMIN, CUSTOMER) and `OrderStatus` (PENDING, CONFIRMED, SHIPPED, DELIVERED, CANCELLED)
- **Full update via PUT** and **partial update via PATCH**
- **Transactional order creation** — fetches user and books, builds items, calculates total, saves everything
- **Global exception handling** — structured JSON error responses for all error cases
- **Bean Validation** — meaningful error messages on all invalid inputs

### What Can Be Added Next

| Feature              | How to Add                                             |
|----------------------|--------------------------------------------------------|
| JWT Security         | Spring Security + JWT filter + `UserDetailsService`    |
| Password Hashing     | `BCryptPasswordEncoder` in `UserService`               |
| Pagination           | `Pageable` in repository and service methods           |
| Caching              | `@EnableCaching` + `@Cacheable` on read endpoints      |
| Logging              | SLF4J + Logback or AOP-based logging                   |
| Payment Integration  | `PaymentService` + Stripe or Razorpay SDK              |
| Stock Management     | Deduct stock on order, validate before placing order   |
| Order Status Update  | `PATCH /api/orders/{id}/status` to update order status |
| Email Notification   | Spring Mail on order confirmation                      |
| API Documentation    | Springdoc OpenAPI (Swagger UI)                         |

---

## 👨‍💻 Author

**Your Name**
- GitHub: [Vikash Prajapati](https://github.com/vikash8058)
---
