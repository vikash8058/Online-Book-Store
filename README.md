# UC10 — Pagination + Swagger + CORS

## Overview
UC10 adds three production-level improvements:
1. Pagination on books endpoint
2. Swagger API documentation
3. CORS configuration for React frontend

## New Features

### Pagination
GET /api/books/paged supports:
- page → page number (0-based)
- size → items per page (default 10)
- sort → field and direction (e.g. price,asc)

### Swagger UI
http://localhost:8080/swagger-ui.index.html
- All endpoints listed with request/response details
- JWT authorization built in
- Test endpoints directly from browser

### CORS
Allows React frontend on:
- http://localhost:3000 (Create React App)
- http://localhost:5173 (Vite)

## New Endpoint
| Endpoint | Access | Description |
|---|---|---|
| GET /api/books/paged | ADMIN + CUSTOMER | Paginated books list |

## Modified Files
| File | What changed |
|---|---|
| SecurityConfig.java | Added CORS config + Swagger endpoints |
| BookService.java | Added getAllBooksPaginated() |
| BookController.java | Added GET /api/books/paged |
| application.properties | Added Swagger config |
| pom.xml | Added springdoc-openapi dependency |

## New Files
| File | Purpose |
|---|---|
| config/SwaggerConfig.java | Swagger/OpenAPI configuration |

## Pagination Example
Request:
GET /api/books/paged?page=0&size=2&sort=price,asc

Response:
```json
{
  "content": [...],
  "totalElements": 4,
  "totalPages": 2,
  "number": 0,
  "size": 2
}
```

## Previous UC
UC9 — OAuth2 Google Login
