## UC6 — Session Based Security (Spring Security Phase 1)

### What Was Added
- Spring Security dependency
- `SecurityConfig.java` — public and protected endpoints + session management
- `CustomUserDetailsService.java` — loads user from DB for Spring Security
- `AuthController.java` — login, logout, current user endpoints
- `LoginRequest.java` — DTO for login request
- `LoginResponse.java` — DTO for login response
- BCrypt password encoding on user registration
- Session based authentication — login once, session stored on server

---

## Core Concepts

### What is Authentication vs Authorization
```
Authentication → WHO are you? (login)
Authorization  → WHAT can you do? (permissions)

UC6 covers Authentication only
Role based Authorization comes in JWT phase
```

### What is Session Based Security
```
Step 1 — Register
→ User registers with email + BCrypt password
→ Password never stored as plain text

Step 2 — Login
→ User sends email + password
→ Spring Security verifies credentials
→ Server creates a SESSION
→ Returns JSESSIONID cookie to client

Step 3 — Protected Request
→ Client sends JSESSIONID cookie automatically
→ Server checks session → valid → request proceeds

Step 4 — Logout
→ Server destroys session
→ JSESSIONID becomes invalid
→ Next request → 401 Unauthorized
```

### What is BCrypt
```
Plain password  → "secure123"
        ↓
BCryptPasswordEncoder.encode()
        ↓
Stored in DB    → "$2a$10$xyzxyzxyz..."

If DB is hacked → passwords are safe ✅
BCrypt is one way → cannot be decoded ✅
```

---

## How Authentication Works Internally
```
POST /api/auth/login
Body: { "email": "john@example.com", "password": "secure123" }
        ↓
AuthenticationManager.authenticate()
        ↓
DaoAuthenticationProvider
        ↓
CustomUserDetailsService.loadUserByUsername(email)
        ↓
Fetches User from DB by email
        ↓
BCryptPasswordEncoder.matches(rawPassword, encodedPassword)
        ↓
match    → Authentication success ✅
no match → BadCredentialsException ❌ → 401 Unauthorized
```

---

## New Endpoints Added

| Method | Endpoint | Description | Auth Required |
|---|---|---|---|
| `POST` | `/api/auth/login` | Login with email and password | No |
| `POST` | `/api/auth/logout` | Logout and destroy session | Yes |
| `GET` | `/api/auth/me` | Get current logged in user info | Yes |

---

## Public vs Protected Endpoints
```
PUBLIC — no login required:
✅ POST /api/auth/login          → login
✅ POST /api/users/register      → register
✅ GET  /api/books/get/**        → view books
✅ GET  /api/books/search        → search books

PROTECTED — login required:
🔒 POST   /api/orders/{userId}   → create order
🔒 GET    /api/orders            → get all orders
🔒 GET    /api/orders/{id}       → get order by id
🔒 DELETE /api/orders/{id}       → delete order
🔒 PATCH  /api/orders/{id}/status → update order status
🔒 GET    /api/users             → get all users
🔒 GET    /api/users/{id}        → get user by id
🔒 DELETE /api/users/{id}        → delete user
🔒 POST   /api/books             → create book
🔒 PUT    /api/books/{id}        → full update book
🔒 PATCH  /api/books/{id}        → partial update book
🔒 DELETE /api/books/{id}        → delete book
🔒 GET    /api/auth/me           → current user info
🔒 POST   /api/auth/logout       → logout
```

---

## Sample Requests and Responses

### Register User
```
POST http://localhost:8080/api/users/register
Body:
{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "secure123"
}

Response 201 Created:
{
  "id": 1,
  "name": "John Doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

---

### Login — Success
```
POST http://localhost:8080/api/auth/login
Body:
{
  "email": "john@example.com",
  "password": "secure123"
}

Response 200 OK:
{
  "message": "Login successful",
  "email": "john@example.com",
  "role": "ROLE_CUSTOMER"
}
Cookie set automatically: JSESSIONID=abc123xyz
```

---

### Login — Wrong Credentials
```
POST http://localhost:8080/api/auth/login
Body:
{
  "email": "john@example.com",
  "password": "wrongpassword"
}

Response 401 Unauthorized:
{
  "message": "Invalid email or password",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

---

### Get Current Logged In User
```
GET http://localhost:8080/api/auth/me
(JSESSIONID cookie sent automatically)

Response 200 OK:
{
  "message": "Currently logged in",
  "email": "john@example.com",
  "role": "ROLE_CUSTOMER"
}
```

---

### Get Current User — Not Logged In
```
GET http://localhost:8080/api/auth/me
(no session)

Response 401 Unauthorized:
{
  "message": "Not logged in"
}
```

---

### Logout
```
POST http://localhost:8080/api/auth/logout

Response 200 OK:
"Logged out successfully"
```

---

### Access Protected Endpoint Without Login
```
GET http://localhost:8080/api/orders

Response 401 Unauthorized
```

---

### Access Protected Endpoint With Login
```
GET http://localhost:8080/api/orders
(JSESSIONID cookie sent automatically)

Response 200 OK:
[...]
```

---

## Files Changed Summary

| File | Type | Change |
|---|---|---|
| `pom.xml` | Modified | Added Spring Security dependency |
| `SecurityConfig.java` | New | Security rules, session management, BCrypt bean |
| `CustomUserDetailsService.java` | New | Loads user from DB for Spring Security |
| `AuthController.java` | New | Login, logout, current user endpoints |
| `LoginRequest.java` | New | DTO for login request |
| `LoginResponse.java` | New | DTO for login response |
| `UserService.java` | Modified | Encode password with BCrypt before saving |

---

## Postman Testing — UC6

### Important Postman Setup
```
Postman → Settings → Cookies → Enable
Postman will automatically store and send JSESSIONID cookie
No manual work needed ✅
```

---

### Complete Testing Checklist
```
REGISTRATION
□ POST /api/users/register (new user)       → 201 Created
□ POST /api/users/register (same email)     → 409 Conflict
□ check DB → password stored as BCrypt hash ✅

LOGIN
□ POST /api/auth/login (correct credentials) → 200 OK + JSESSIONID cookie
□ POST /api/auth/login (wrong password)      → 401 Unauthorized
□ POST /api/auth/login (wrong email)         → 401 Unauthorized

PUBLIC ENDPOINTS (no login needed)
□ GET /api/books/get/1   (no session) → 200 OK ✅
□ GET /api/books/search  (no session) → 200 OK ✅

PROTECTED WITHOUT LOGIN
□ GET    /api/orders     (no session) → 401 Unauthorized ❌
□ GET    /api/users      (no session) → 401 Unauthorized ❌
□ POST   /api/books      (no session) → 401 Unauthorized ❌
□ DELETE /api/books/1    (no session) → 401 Unauthorized ❌

PROTECTED WITH LOGIN
□ GET    /api/orders     (with session) → 200 OK ✅
□ GET    /api/users      (with session) → 200 OK ✅
□ POST   /api/books      (with session) → 201 Created ✅
□ DELETE /api/books/1    (with session) → 204 No Content ✅

CURRENT USER
□ GET /api/auth/me (with session)    → 200 OK + user info ✅
□ GET /api/auth/me (without session) → 401 Not logged in ❌

LOGOUT
□ POST /api/auth/logout (with session)   → 200 OK ✅
□ GET  /api/orders (after logout)        → 401 Unauthorized ❌
```

---

## Session vs JWT — Why We Will Move to JWT Next
```
Session Based (UC6 — current):
→ Session stored on SERVER
→ Every request hits server to check session
→ Hard to scale (multiple servers = session sharing problem)
→ Not ideal for mobile apps and REST APIs
→ Good for learning and understanding ✅

JWT Based (UC7 — next):
→ Token stored on CLIENT
→ Server is STATELESS — no session storage
→ Easy to scale
→ Works perfectly for REST APIs and mobile apps
→ Token contains user info + expiry
→ Production ready ✅
```

---
