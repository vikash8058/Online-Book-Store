## UC5 — Spring Security Phase 1 (Basic Authentication)

### What Was Added
- Spring Security dependency
- `SecurityConfig.java` — defines public and protected endpoints
- `CustomUserDetailsService.java` — loads user from DB for Spring Security
- BCrypt password encoding on user registration
- HTTP Basic Authentication
- Plain text password never stored in DB

---

## Core Concepts

### What is Spring Security
```
Spring Security is a framework that handles:
→ Authentication  — who are you? (login)
→ Authorization   — what can you do? (permissions)

Without Security:
Anyone can call any endpoint without login ❌

With Security:
Public endpoints  → anyone can access ✅
Protected endpoints → only logged in users ✅
```

### What is BCrypt
```
BCrypt is a password hashing algorithm

Without BCrypt:
password "secure123" stored as "secure123" in DB ❌
if DB is hacked → all passwords exposed ❌

With BCrypt:
password "secure123" stored as "$2a$10$xyz..." in DB ✅
if DB is hacked → passwords are safe ✅
BCrypt is one way — cannot be decoded back ✅
```

### What is HTTP Basic Auth
```
Client sends credentials with every request
Authorization: Basic base64(email:password)

Simple and easy to test in Postman
Not recommended for production (use JWT instead)
Good for Phase 1 learning ✅
```

---

## How It Works

### Registration Flow
```
POST /api/users/register
        ↓
UserService.registerUser()
        ↓
Check duplicate email
        ↓
BCryptPasswordEncoder.encode("secure123")
→ "$2a$10$xyzxyzxyz..."
        ↓
Save encoded password in DB ✅
Plain text password never stored ✅
```

### Login / Authentication Flow
```
Client sends request with credentials
Authorization: Basic am9obkBleGFtcGxlLmNvbTpzZWN1cmUxMjM=
        ↓
Spring Security intercepts request
        ↓
Calls CustomUserDetailsService.loadUserByUsername(email)
        ↓
Fetches user from DB by email
        ↓
BCryptPasswordEncoder.matches(rawPassword, encodedPassword)
        ↓
match    → request proceeds ✅  200 OK
no match → request blocked  ❌  401 Unauthorized
```

### Password Encoding Flow
```
Register:
plain password → "secure123"
        ↓
BCryptPasswordEncoder.encode()
        ↓
stored in DB → "$2a$10$xyzxyzxyz..."  ✅

Login:
client sends → "secure123"
        ↓
BCryptPasswordEncoder.matches()
compares with → "$2a$10$xyzxyzxyz..."
        ↓
match → login success ✅
```

---

## Public vs Protected Endpoints
```
PUBLIC — no login required:
✅ POST /api/users/register     → anyone can register
✅ GET  /api/books              → anyone can view all books
✅ GET  /api/books/**           → anyone can view book, search

PROTECTED — login required:
🔒 POST   /api/orders/{userId}  → must be logged in
🔒 GET    /api/orders           → must be logged in
🔒 GET    /api/orders/{id}      → must be logged in
🔒 DELETE /api/orders/{id}      → must be logged in
🔒 GET    /api/users            → must be logged in
🔒 GET    /api/users/{id}       → must be logged in
🔒 DELETE /api/users/{id}       → must be logged in
🔒 POST   /api/books            → must be logged in
🔒 PUT    /api/books/{id}       → must be logged in
🔒 PATCH  /api/books/{id}       → must be logged in
🔒 DELETE /api/books/{id}       → must be logged in
```

---

## New Files Added

### SecurityConfig.java
```
→ Disables CSRF (not needed for REST APIs)
→ Defines public and protected endpoints
→ Sets up HTTP Basic authentication
→ Creates BCryptPasswordEncoder bean
```

### CustomUserDetailsService.java
```
→ Implements UserDetailsService interface
→ Spring Security calls this during login
→ Loads user from DB by email
→ Returns UserDetails object to Spring Security
```

---

## Files Changed Summary

| File | Type | Change |
|---|---|---|
| `pom.xml` | Modified | Added Spring Security dependency |
| `SecurityConfig.java` | New | Security rules + BCrypt bean |
| `CustomUserDetailsService.java` | New | Load user from DB for Spring Security |
| `UserService.java` | Modified | Encode password before saving |

---

## Postman Testing — UC5

### Setup for Protected Endpoints
```
Postman → Authorization tab
Type     : Basic Auth
Username : john@example.com
Password : secure123
```

---

### Test 1 — Register User (Public)
```
POST http://localhost:8080/api/users/register
Body:
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secure123"
}
Expected : 201 Created
Auth     : not required ✅
```

---

### Test 2 — Get All Books (Public)
```
GET http://localhost:8080/api/books
Expected : 200 OK
Auth     : not required ✅
```

---

### Test 3 — Create Order Without Login
```
POST http://localhost:8080/api/orders/1
Body: { "items": [{ "bookId": 1, "quantity": 1 }] }
Expected : 401 Unauthorized ❌
Auth     : none
```

---

### Test 4 — Create Order With Login
```
POST http://localhost:8080/api/orders/1
Body: { "items": [{ "bookId": 1, "quantity": 1 }] }
Auth     : Basic Auth → john@example.com / secure123
Expected : 201 Created ✅
```

---

### Test 5 — Wrong Password
```
POST http://localhost:8080/api/orders/1
Auth     : Basic Auth → john@example.com / wrongpassword
Expected : 401 Unauthorized ❌
```

---

### Test 6 — Get All Users Without Login
```
GET http://localhost:8080/api/users
Expected : 401 Unauthorized ❌
Auth     : none
```

---

### Test 7 — Get All Users With Login
```
GET http://localhost:8080/api/users
Auth     : Basic Auth → john@example.com / secure123
Expected : 200 OK ✅
```

---

### Test 8 — Duplicate Email
```
POST http://localhost:8080/api/users/register
Body:
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secure123"
}
(same email registered again)
Expected : 409 Conflict ❌
Response :
{
  "message": "Email already registered: john@example.com",
  "statusCode": 409,
  "timestamp": "2024-01-15T10:30:00"
}
```

---

## Complete Testing Checklist — UC5
```
REGISTRATION
□ POST /api/users/register (new email)     → 201 Created
□ POST /api/users/register (same email)    → 409 Conflict
□ check DB → password stored as BCrypt hash not plain text

PUBLIC ENDPOINTS
□ GET /api/books (no auth)                 → 200 OK
□ GET /api/books/1 (no auth)               → 200 OK

PROTECTED WITHOUT LOGIN
□ POST   /api/orders/1 (no auth)           → 401 Unauthorized
□ GET    /api/orders   (no auth)           → 401 Unauthorized
□ GET    /api/users    (no auth)           → 401 Unauthorized
□ POST   /api/books    (no auth)           → 401 Unauthorized
□ DELETE /api/books/1  (no auth)           → 401 Unauthorized

PROTECTED WITH CORRECT LOGIN
□ POST   /api/orders/1 (correct auth)      → 201 Created
□ GET    /api/orders   (correct auth)      → 200 OK
□ GET    /api/users    (correct auth)      → 200 OK

PROTECTED WITH WRONG PASSWORD
□ POST /api/orders/1 (wrong password)      → 401 Unauthorized
□ GET  /api/users    (wrong password)      → 401 Unauthorized
```
---