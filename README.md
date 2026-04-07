# UC11 — Forgot Password + Update Password

## Overview
UC11 adds two password management features to the Book Store:
1. **Forgot Password** — user forgot password → get OTP on email → reset password
2. **Update Password** — logged in user → change current password to new one

Both features work only for LOCAL users.
GOOGLE users have no password — they use Google login.

## Tech Used
- Spring Boot 3.2.0
- Spring Mail (reused from UC8)
- OtpService (reused from UC8)
- BCryptPasswordEncoder
- JWT Security Context (@AuthenticationPrincipal)
- MySQL

## New Flow

### Forgot Password Flow
```
Step 1 → POST /auth/forgot-password { email }
         → OTP sent to email (valid 5 minutes)

Step 2 → POST /auth/reset-password { email, otp, newPassword }
         → OTP verified → password updated in DB
```

### Update Password Flow
```
POST /auth/update-password
Authorization: Bearer <JWT token>
Body: { currentPassword, newPassword }
→ Current password verified → new password saved
```

## New Files
| File | Purpose |
|---|---|
| `dto/request/ForgotPasswordRequest.java` | Request DTO for /auth/forgot-password |
| `dto/request/ResetPasswordRequest.java` | Request DTO for /auth/reset-password |
| `dto/request/UpdatePasswordRequest.java` | Request DTO for /auth/update-password |
| `service/PasswordService.java` | Core logic for all password operations |

## Modified Files
| File | What changed |
|---|---|
| `service/EmailService.java` | Added sendPasswordResetOtpEmail() method |
| `service/OtpService.java` | Added generateOtpAndSave() — returns OTP string |
| `controller/AuthController.java` | Added 3 new endpoints |
| `config/SecurityConfig.java` | Added /auth/forgot-password and /auth/reset-password as public |

## API Endpoints

### POST /auth/forgot-password (Public)
Request:
```json
{ "email": "vikash@gmail.com" }
```
Response (200 OK):
```
"If an account exists with this email, an OTP has been sent. Valid for 5 minutes."
```

### POST /auth/reset-password (Public)
Request:
```json
{
    "email"       : "vikash@gmail.com",
    "otp"         : "482910",
    "newPassword" : "NewPass123@"
}
```
Response (200 OK):
```
"Password reset successfully. Please login with your new password."
```

### POST /auth/update-password (JWT Required)
Request:
```
Authorization: Bearer <JWT token>
```
```json
{
    "currentPassword" : "OldPass123@",
    "newPassword"     : "NewPass123@"
}
```
Response (200 OK):
```
"Password updated successfully."
```

## Rules
| Rule | Detail |
|---|---|
| LOCAL users only | GOOGLE users have no password |
| OTP expiry | 5 minutes — same as registration OTP |
| OTP reuse | Cannot use same OTP twice |
| Same password | New password cannot be same as current |
| Security message | /forgot-password always returns same message — prevents email enumeration |
| @AuthenticationPrincipal | Email extracted from JWT — not passed in request body |

## OtpService changes — UC11
```
Before UC11:
generateAndSendOtp() → generates OTP + saves + sends REGISTRATION email

After UC11:
generateOtpAndSave()  → generates OTP + saves to DB → returns OTP string
                        caller decides which email to send

generateAndSendOtp() still works for UC8 registration
generateOtpAndSave() used by PasswordService for password reset email
```

## Error Responses
| Scenario | Status | Message |
|---|---|---|
| Email not registered | 200 | Same success message (security) |
| GOOGLE user on forgot-password | 400 | Google account — password reset not available |
| GOOGLE user on update-password | 400 | Google account — password update not available |
| Wrong OTP | 400 | Invalid OTP. Please check and try again. |
| Expired OTP | 400 | OTP has expired. Please request a new OTP. |
| Already used OTP | 400 | OTP already used. Please request a new OTP. |
| Wrong current password | 400 | Current password is incorrect. |
| Same as current password | 400 | New password cannot be same as current password. |
| No JWT token on update | 401 | Unauthorized |

## How to Test

### Forgot Password
```
Step 1 → POST /auth/forgot-password { "email": "vikash@gmail.com" }
Step 2 → Check Gmail inbox for OTP
Step 3 → POST /auth/reset-password { email, otp, newPassword }
Step 4 → POST /auth/login with new password → get JWT token
```

### Update Password
```
Step 1 → POST /auth/login → get JWT token
Step 2 → POST /auth/update-password
         Authorization: Bearer <token>
         { "currentPassword": "...", "newPassword": "..." }
```

## Previous UC
UC10 — Pagination + Swagger + CORS

## Next
React Frontend