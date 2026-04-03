# UC7 — JWT Based Security

## Overview
UC7 replaces session based security (UC6) with stateless JWT
(JSON Web Token) based authentication. Every protected endpoint
now requires a valid Bearer token in the Authorization header.

## Tech Used
- Spring Boot 3.2.0
- Spring Security 6.2.0
- jjwt 0.12.3 (JSON Web Token library)
- BCryptPasswordEncoder
- MySQL
- Lombok

## What changed from UC6
| UC6 (Session) | UC7 (JWT) |
|---|---|
| Server stores session in memory | Server stores nothing — stateless |
| Cookie based auth | Bearer token in Authorization header |
| /login returns session | /login returns JWT token |
| Sessions expire server side | Token expires via exp claim |

## New Files
| File | Purpose |
|---|---|
| `filter/JwtAuthFilter.java` | Intercepts every request, validates JWT |
| `service/JwtService.java` | Generate, validate, extract claims from token |
| `dto/request/LoginRequest.java` | Request DTO for /auth/login |
| `dto/response/AuthResponse.java` | Response DTO — returns JWT token |

## Modified Files
| File | What changed |
|---|---|
| `model/User.java` | Implements UserDetails — Spring Security uses it directly |
| `service/CustomUserDetailsService.java` | Returns User directly instead of Spring's User builder |
| `controller/AuthController.java` | /register returns message, /login returns JWT token |
| `controller/UserController.java` | Removed /register endpoint — moved to AuthController |
| `service/UserService.java` | Removed registerUser() method |
| `config/SecurityConfig.java` | Full rewrite — JWT filter chain, STATELESS, endpoint rules |
| `application.properties` | Added jwt.secret and jwt.expiration |
| `pom.xml` | Added jjwt-api, jjwt-impl, jjwt-jackson dependencies |

## JWT Configuration
```properties
jwt.secret=5367566B59703373367639792F423F4528482B4D6251655468576D5A71347437
jwt.expiration=86400000
# expiration = 86400000ms = 24 hours
```

## Admin Setup
Admin user is inserted directly into MySQL via data.sql on startup.
Nobody can register as ADMIN via API — registration always creates CUSTOMER.
```sql
INSERT INTO users (name, email, password, role)
VALUES (
    'Admin',
    'admin2026@bookstore.com',
    '$2a$10$UyVBg8YTlj/XzlUgFUh5Ou/uudQrOLZyPq.ur787xxwQnjxuKzuBC',
    'ADMIN'
);
```

Admin credentials:
```
email    : admin2026@bookstore.com
password : Admin2026@
```

## API Endpoints

### POST /auth/register (public)
Request:
```json
{
    "name"     : "Vikash",
    "email"    : "vikash@gmail.com",
    "password" : "Vikash123@"
}
```
Response (200 OK):
```
"User registered successfully. Please login to get your token."
```

### POST /auth/login (public)
Request:
```json
{
    "email"    : "vikash@gmail.com",
    "password" : "Vikash123@"
}
```
Response (200 OK):
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9.xxxxx"
}
```

## Endpoint Security Rules
| Endpoint | Access |
|---|---|
| POST /auth/register | Public |
| POST /auth/login | Public |
| GET /api/books/get/** | ADMIN + CUSTOMER |
| GET /api/books/search | ADMIN + CUSTOMER |
| GET /api/books/author | ADMIN + CUSTOMER |
| POST /api/books/create | ADMIN only |
| PUT /api/books/update/** | ADMIN only |
| PATCH /api/books/partialUpdate/** | ADMIN only |
| DELETE /api/books/delete/** | ADMIN only |
| POST /api/orders/create/** | CUSTOMER only |
| GET /api/orders/get | ADMIN only |
| GET /api/orders/get/** | ADMIN + CUSTOMER |
| GET /api/orders/user/** | ADMIN + CUSTOMER |
| PATCH /api/orders/status/** | ADMIN only |
| DELETE /api/orders/delete/** | ADMIN only |
| GET /api/users/** | ADMIN only |

## How to use JWT token in Postman
```
Authorization tab
→ Type: Bearer Token
→ Token: paste token here (no quotes, no spaces)
```

## Token Structure
```
eyJhbGciOiJIUzI1NiJ9          ← Header (algorithm)
.eyJzdWIiOiJ2aWthc2gifQ        ← Payload (sub, iat, exp)
.SflKxwRJSMeKKF2QT4fwpMeJf    ← Signature (HMACSHA256)
```

## Error Responses
| Scenario | Status |
|---|---|
| No token | 401 Unauthorized |
| Expired token | 401 Unauthorized |
| Tampered token | 401 Unauthorized |
| Wrong role | 403 Forbidden |
| Duplicate email | 409 Conflict |
| Wrong password | 401 Unauthorized |

## Previous UC
UC6 — Session Based Security

## Next UC
UC8 — OTP Email Verification for Registration