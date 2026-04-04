# UC9 — OAuth2 Google Login

## Overview
UC9 adds Google OAuth2 login to the Book Store.
Users can now login using their existing Google account
instead of registering with email and password.
After successful Google login, our own JWT token is generated
and returned — all further requests use JWT exactly like UC7.

## Tech Used
- Spring Boot 3.2.0
- Spring Security OAuth2 Client
- Google OAuth2 / OpenID Connect
- JWT (same as UC7)
- MySQL

## What is OAuth2?
OAuth2 is the framework behind "Login with Google".
Instead of user creating a new account with password:
→ User logs in via Google
→ Google confirms their identity
→ Your app gets email and name from Google
→ Your app generates its own JWT token
→ User uses JWT for all further requests

## New Flow (UC9)
```
Step 1 → Browser hits /oauth2/authorization/google
Step 2 → Redirected to Google login page
Step 3 → User selects Google account + allows permissions
Step 4 → Google redirects back with authorization code
Step 5 → Spring exchanges code for Access Token + ID Token
Step 6 → CustomOAuth2UserService saves/updates user in DB
Step 7 → OAuth2SuccessHandler generates JWT token
Step 8 → JWT returned to client
Step 9 → Client uses JWT for all requests (same as UC7)
```

## Existing Flows (unchanged)
```
LOCAL registration → POST /auth/send-otp + POST /auth/register (UC8)
LOCAL login        → POST /auth/login (UC7)
```

## New Files
| File | Purpose |
|---|---|
| `model/AuthProvider.java` | Enum — LOCAL or GOOGLE |
| `service/CustomOAuth2UserService.java` | Loads/creates user from Google profile |
| `security/OAuth2AuthenticationSuccessHandler.java` | Generates JWT after Google login |

## Modified Files
| File | What changed |
|---|---|
| `model/User.java` | Added authProvider, googleId fields. Password now nullable |
| `config/SecurityConfig.java` | Added oauth2Login() config with custom service and handler |
| `service/CustomUserDetailsService.java` | Added check — Google users cannot use password login |
| `application.properties` | Added Google Client ID, Secret, scope |
| `pom.xml` | Added spring-boot-starter-oauth2-client dependency |

## Two Types of Users
| Type | How registered | Password | Login method |
|---|---|---|---|
| LOCAL | Email + OTP (UC8) | BCrypt hashed | POST /auth/login |
| GOOGLE | Google OAuth2 (UC9) | null | /oauth2/authorization/google |

## New DB Columns in users table
```sql
auth_provider VARCHAR(10) NOT NULL DEFAULT 'LOCAL'
google_id     VARCHAR(255) NULL
```

## Google Cloud Console Config
```
Project        : BookStore
Redirect URI   : http://localhost:8080/login/oauth2/code/google
Scopes         : email, profile
```

## application.properties
```properties
spring.security.oauth2.client.registration.google.client-id=YOUR_CLIENT_ID
spring.security.oauth2.client.registration.google.client-secret=YOUR_CLIENT_SECRET
spring.security.oauth2.client.registration.google.scope=email,profile
```

## OAuth2 Endpoints (public — no token needed)
| Endpoint | Purpose |
|---|---|
| GET /oauth2/authorization/google | Triggers Google login redirect |
| GET /login/oauth2/code/google | Google redirects here with auth code |

## How to Test

### Step 1 — Google Login
Open browser and hit:
```
http://localhost:8080/oauth2/authorization/google
```
→ Select Google account → Allow permissions
→ Response:
```json
{ "token": "eyJhbGciOiJIUzI1NiJ9.xxxxx" }
```

### Step 2 — Use token in Postman
```
GET /api/books/get
Authorization: Bearer <token from Step 1>
```

### Step 3 — Verify in MySQL
```sql
SELECT * FROM users WHERE auth_provider = 'GOOGLE';
```

## Error Scenarios
| Scenario | Result |
|---|---|
| Google user tries POST /auth/login | 401 — use Google login instead |
| Invalid/expired JWT after Google login | 401 Unauthorized |
| Google user accesses ADMIN endpoint | 403 Forbidden |

## Previous UC
UC8 — OTP Based Email Verification

## Next UC
UC10 — (upcoming)