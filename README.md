# UC8 — OTP Based Email Verification for Registration

## Overview
UC8 adds OTP (One-Time Password) based email verification to the registration flow.
Before a user can register, they must verify their email address by entering a
6-digit OTP sent to their inbox via Gmail SMTP.

## Tech Used
- Spring Boot 3.2.0
- Spring Mail (JavaMailSender)
- Gmail SMTP
- MySQL (otp_verifications table)
- Lombok

## New Flow

### Before UC8
```
POST /auth/register { name, email, password }
→ Account created immediately
```

### After UC8
```
Step 1 → POST /auth/send-otp  { email }
         → OTP sent to email (valid 5 minutes)

Step 2 → POST /auth/register  { name, email, password, otp }
         → OTP verified → account created
```

## New Files
| File | Purpose |
|---|---|
| `model/OtpVerification.java` | JPA entity for otp_verifications table |
| `repository/OtpVerificationRepository.java` | DB operations for OTP |
| `service/OtpService.java` | Generate, save, verify OTP logic |
| `service/EmailService.java` | Send OTP email via Gmail SMTP |
| `dto/request/SendOtpRequest.java` | Request DTO for /auth/send-otp |
| `exception/OtpInvalidException.java` | Thrown when OTP is wrong or used |
| `exception/OtpExpiredException.java` | Thrown when OTP is expired |

## Modified Files
| File | What changed |
|---|---|
| `dto/request/RegisterRequest.java` | Added otp field |
| `controller/AuthController.java` | Added /send-otp endpoint, register now verifies OTP |
| `config/SecurityConfig.java` | Added /auth/send-otp to public endpoints |
| `exception/GlobalExceptionHandler.java` | Added OTP exception handlers |
| `application.properties` | Added Gmail SMTP configuration |
| `pom.xml` | Added spring-boot-starter-mail dependency |

## Database
New table created automatically by JPA on startup:
```sql
CREATE TABLE otp_verifications (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    otp         VARCHAR(6)   NOT NULL,
    expiry      DATETIME     NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL
);
```

## OTP Rules
| Rule | Detail |
|---|---|
| Length | 6 digits numeric |
| Expiry | 5 minutes from generation |
| One-time use | Marked used after successful verification |
| Resend | New OTP replaces old one for same email |
| Range | 100000 to 999999 (always 6 digits) |

## API Endpoints

### POST /auth/send-otp
Request:
```json
{
    "email": "vikash@gmail.com"
}
```
Response (200 OK):
```
"OTP sent to vikash@gmail.com. Valid for 5 minutes."
```
Error (409):
```json
{ "message": "Email already registered: vikash@gmail.com" }
```

### POST /auth/register
Request:
```json
{
    "name"     : "Vikash",
    "email"    : "vikash@gmail.com",
    "password" : "Vikash123@",
    "otp"      : "482910"
}
```
Response (200 OK):
```
"User registered successfully. Please login to get your token."
```
Errors:
```json
{ "message": "Invalid OTP. Please check and try again." }
{ "message": "OTP has expired. Please request a new OTP." }
{ "message": "OTP already used. Please request a new OTP." }
```

### POST /auth/login
No change from UC7.

## Error Responses
| Scenario | Status | Message |
|---|---|---|
| Wrong OTP | 400 | Invalid OTP. Please check and try again. |
| Expired OTP | 400 | OTP has expired. Please request a new OTP. |
| Already used OTP | 400 | OTP already used. Please request a new OTP. |
| No OTP requested | 400 | No OTP found for this email. Please request a new OTP. |
| Duplicate email | 409 | Email already registered: {email} |

## How to Test

### Step 1 — Send OTP
```
POST /auth/send-otp
{ "email": "your-email@gmail.com" }
```

### Step 2 — Check inbox for 6-digit OTP

### Step 3 — Register with OTP
```
POST /auth/register
{ "name": "...", "email": "...", "password": "...", "otp": "482910" }
```

### Step 4 — Login
```
POST /auth/login
{ "email": "...", "password": "..." }
```

## Gmail SMTP Config (application.properties)
```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=your-email@gmail.com
spring.mail.password=your-app-password
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
```

## Previous UC
UC7 — JWT Based Security