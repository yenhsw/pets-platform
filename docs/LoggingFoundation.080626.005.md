# Logging Foundation System

**File:** `080626.005.md`
**Module:** `com.petsplatform.shared.logging`
**Version:** 1.0.0

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Filter vs Interceptor — tại sao dùng Filter](#2-filter-vs-interceptor--tại-sao-dùng-filter)
3. [Kiến trúc scalable — giải thích](#3-kiến-trúc-scalable--giải-thích)
4. [Files tạo trong session](#4-files-tạo-trong-session)
5. [Trace ID flow](#5-trace-id-flow)
6. [MDC — Mapped Diagnostic Context](#6-mdc--mapped-diagnostic-context)
7. [RequestLoggingFilter chi tiết](#7-requestloggingfilter-chi-tiết)
8. [SlowApiWarningFilter chi tiết](#8-slowapiwarningfilter-chi-tiết)
9. [Sensitive field masking](#9-sensitive-field-masking)
10. [Logging format & ELK compatibility](#10-logging-format--elk-compatibility)
11. [Logging configuration](#11-logging-configuration)
12. [Cách dùng trong code](#12-cách-dùng-trong-code)
13. [ELK Stack integration](#13-elk-stack-integration)
14. [File index](#14-file-index)

---

## 1. Tổng quan kiến trúc

```
Request
   │
   ▼
┌─────────────────────┐
│ RequestLoggingFilter │  Order: HIGHEST_PRECEDENCE + 1
│  • Generate traceId │  ✅ Chạy ĐẦU TIÊN (sau security)
│  • MDC.put(traceId) │  ✅ Ghi request + response body (masked)
│  • Log request      │  ✅ Log mọi request/response
│  • Timing           │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│ SlowApiWarningFilter│  Order: HIGHEST_PRECEDENCE + 2
│  • Time request     │  ✅ Kiểm tra sau khi response trả về
│  • WARN nếu > threshold  │  ✅ Chỉ log khi slow
│  • Read MDC traceId  │
└──────────┬──────────┘
           │
           ▼
    Controller / Service
           │
           ▼
    MdcUtils.put("userId", userId)  ← Trong service/auth layer
    log.info("Processing order")     ← Tự động có traceId trong log
           │
           ▼
┌─────────────────────┐
│    MDC Context      │
│  traceId: "abc123"  │  ← Mọi log trong request thread đều thấy
│  userId: "user_42"  │
└─────────────────────┘
           │
           ▼
    Log Output (console / file)
    "2026-06-08 09:47:00.123 [http-nio-1] INFO abc123 RequestLoggingFilter - ..."
```

---

## 2. Filter vs Interceptor — Tại sao dùng Filter

### 2.1 So sánh Filter và HandlerInterceptor

| Tiêu chí | Filter (Servlet Filter) | HandlerInterceptor |
|----------|------------------------|-------------------|
| **Level** | Servlet container (before Spring MVC) | Spring MVC (DispatcherServlet) |
| **Request body** | Đọc được (cần wrapper) | Không đọc được (đã consumed) |
| **Response body** | Đọc được (cần wrapper) | Không đọc được (đã committed) |
| **Timing** | Toàn bộ request-to-response | Chỉ controller method |
| **Coverage** | Bao gồm static resources, actuators | Chỉ controller endpoints |
| **Servlet API** | Full servlet API | Spring abstraction |
| **Use case** | Logging, security headers, CORS, metrics | Controller-level prep/cleanup |

### 2.2 Tại sao dùng Filter cho logging

```
Filter (chúng ta dùng):
  RequestLoggingFilter
    └─ ✅ Log request body (POST/PUT) — cần đọc body
    └─ ✅ Log response body (khi error) — cần wrapper
    └─ ✅ Timing toàn bộ request-to-response
    └─ ✅ Ghi cả timing của Spring Security nếu cần
    └─ ✅ Hoạt động với mọi endpoint (static, actuator, API)

Interceptor (nếu dùng):
  LoggingInterceptor
    ├─ ❌ Không đọc được request body (đã consumed)
    ├─ ❌ Không đọc được response body
    ├─ ❌ Timing không tính Spring Security filter chain
    └─ ❌ Không cover static resources

→ KẾT LUẬN: Dùng Filter cho logging toàn diện
```

### 2.3 Khi nào dùng Interceptor

| Use case | Dùng |
|----------|-------|
| Log request/response bodies | Filter |
| Timing toàn request | Filter |
| Set MDC context | Filter |
| Pre/post controller logic (auth check, view model) | Interceptor |
| Performance monitoring (chỉ method) | Interceptor |
| Global data binding | Interceptor |

---

## 3. Kiến trúc Scalable — Giải thích

### 3.1 Tại sao kiến trúc này scalable

```
Nguyên tắc 1: Separation of Concerns
─────────────────────────────────────
RequestLoggingFilter  → Chỉ log request/response, không business logic
SlowApiWarningFilter → Chỉ phát hiện slow API, không log body
MdcUtils             → Chỉ quản lý context, không biết logging
PetsPlatformApplication → Không cần import filter (Spring auto-registers)

→ Thêm filter mới = thêm class, không sửa code hiện có

Nguyên tắc 2: Single Responsibility cho mỗi filter
────────────────────────────────────────────────────
Filter A: Logging
Filter B: Slow API warning
Filter C: Security
...

→ Mỗi filter có 1 mục đích duy nhất
→ Test từng filter riêng biệt
→ Disable filter bằng config mà không cần sửa code

Nguyên tắc 3: MDC — thread-safe, non-blocking
─────────────────────────────────────────────────
MDC.put("traceId", ...) → ThreadLocal under the hood
MDC.get("traceId")      → O(1) lookup
MDC.clear()             → Trong finally block

→ Zero performance overhead khi không logging
→ Thread-safe vì mỗi request = 1 thread
→ Không block các request khác

Nguyên tắc 4: ELK-ready từ đầu
─────────────────────────────────
traceId là field riêng (%X{traceId}) → Elasticsearch parse được
JSON format có thể enable bằng dependency swap
Không cần thay đổi code khi migrate sang ELK
```

### 3.2 Thêm module mới

```
Module User thêm endpoint mới:
  → RequestLoggingFilter tự động log
  → SlowApiWarningFilter tự động check
  → Trace ID tự động propagate
  → KHÔNG cần thay đổi gì trong logging system
```

### 3.3 Microservices

```
Service A (User) có:
  └── RequestLoggingFilter → traceId = "abc123"

Service B (Order) gọi Service A:
  └── Pass X-Trace-Id header → cùng traceId trong distributed system

→ Dùng OpenTelemetry/Zipkin sau này để aggregate
→ Mỗi service chỉ cần generate/forward traceId
```

---

## 4. Files tạo trong session

```
src/main/java/com/petsplatform/shared/logging/
├── MdcUtils.java                       ← MDC utility class
└── filter/
    ├── RequestLoggingFilter.java       ← Core request/response logging
    ├── SlowApiWarningFilter.java       ← Slow API detection
    └── CachedBodyHttpServletRequest.java ← Request body caching wrapper

src/main/resources/
├── application.yml                     ← Updated: traceId pattern + slow-api config
└── application-prod.yml                ← Updated: prod logging levels
```

---

## 5. Trace ID Flow

```
Request tới: POST /api/v1/users
Headers: X-Trace-Id: my-custom-id   (optional)

Step 1: RequestLoggingFilter.doFilter()
  ↓
Step 2: resolveTraceId(request)
  ├─ Header có X-Trace-Id? → Dùng giá trị đó
  └─ Không có? → Tạo mới: UUID.randomUUID()
  ↓
Step 3: MDC.put("traceId", traceId)
  ↓
Step 4: chain.doFilter() → Controller → Service
  ├─ log.info("Processing") → Tự động có traceId
  └─ MdcUtils.put("userId", userId) → Thêm user context
  ↓
Step 5: finally block
  ├─ Log request completed
  ├─ MDC.remove("traceId") → Không leak
  └─ Response với X-Trace-Id header
```

---

## 6. MDC — Mapped Diagnostic Context

### 6.1 MDC là gì

```
MDC = ThreadLocal-based key-value store cho log context

Thread A (Request 1):
  MDC: { traceId: "abc", userId: "user_1" }
  → log.info("Processing") → "Processing traceId=abc userId=user_1"

Thread B (Request 2):
  MDC: { traceId: "def", userId: "user_2" }
  → log.info("Processing") → "Processing traceId=def userId=user_2"

→ Mỗi request thread có context riêng
→ Không ảnh hưởng request khác
```

### 6.2 MdcUtils API

```java
// Set trace ID (tự động trong RequestLoggingFilter)
MdcUtils.putTraceId("abc123");

// Set user ID (trong auth/service layer)
MdcUtils.putUserId("user_42");

// Set custom key
MdcUtils.put("sessionId", sessionId);

// Get trace ID
String traceId = MdcUtils.getTraceId();  // "abc123"

// Clear all (trong finally block)
MdcUtils.clear();
```

### 6.3 Tại sao dùng MDC thay vì global variable

| Phương án | Vấn đề |
|-----------|---------|
| Global static variable | Race condition — nhiều request cùng lúc |
| Request attribute | Cần pass qua mọi method call |
| ThreadLocal (manual) | Boilerplate, dễ quên clear() |
| **MDC** | ThreadLocal wrapper của SLF4J, tích hợp log pattern sẵn |

---

## 7. RequestLoggingFilter chi tiết

### 7.1 Khi nào log

| HTTP Method | Log request body | Log response body |
|-------------|-----------------|-------------------|
| GET | Không | Chỉ khi status >= 400 |
| POST | ✅ Masked | Chỉ khi status >= 400 |
| PUT | ✅ Masked | Chỉ khi status >= 400 |
| PATCH | ✅ Masked | Chỉ khi status >= 400 |
| DELETE | ✅ Masked | Chỉ khi status >= 400 |

### 7.2 Log output format

```
2026-06-08 09:47:00.123 [http-nio-8080-exec-1] INFO  abc123-def456 c.p.s.l.f.RequestLoggingFilter -
  traceId="abc123-def456" requestId="a1b2c3d4" method="POST" uri="/api/v1/users" status=201 duration=45ms body={"name":"John","email":"john@example.com","password":"[MASKED]"}
```

### 7.3 CachedBodyHttpServletRequest

```
Vấn đề:
  InputStream chỉ đọc được MỘT LẦN
  Filter đọc body → Controller không đọc được

Giải pháp:
  CachedBodyHttpServletRequest
    ├─ Đọc body vào byte[] buffer
    ├─ Lần sau đọc từ buffer
    └─ Wrapper implement ServletInputStream
```

### 7.4 Configuration options

```yaml
logging:
  request:
    enabled: true          # Bật/tắt request logging
    max-body-length: 2000 # Giới hạn body log
```

---

## 8. SlowApiWarningFilter chi tiết

### 8.1 Configuration

```yaml
logging:
  slow-api:
    enabled: true
    threshold-ms: 1000       # Cảnh báo nếu > 1 giây
    paths-to-ignore: /actuator/health,/health,/actuator/info
```

### 8.2 Log output

```
2026-06-08 09:47:05 [http-nio-8080-exec-3] WARN  abc123 c.p.s.l.f.SlowApiWarningFilter -
  [SLOW_API] traceId="abc123" GET /api/v1/reports/export status=200 duration=3521ms threshold=1000ms
```

### 8.3 Threshold guidelines

| Environment | Threshold | Lý do |
|-------------|-----------|-------|
| Development | 5000ms | Máy yếu, debug |
| Staging | 2000ms | Gần production |
| Production | 500ms | User expectation |

---

## 9. Sensitive Field Masking

### 9.1 Body fields được mask

```json
// Request body (trước mask)
{ "email": "john@example.com", "password": "MyP@ss1!" }

// Request body (sau mask)
{ "email": "john@example.com", "password": "[MASKED]" }
```

### 9.2 Fields được mask

```
Body JSON:
  password, pwd, passwd
  oldPassword, newPassword, confirmPassword, currentPassword
  token, accessToken, refreshToken, idToken
  secret, apiKey, privateKey, credential
  cardNumber, cvv, cvc, pan
  ssn, socialSecurityNumber

Headers:
  password, pwd, passwd
  authorization, bearer, token
  x-auth-token, x-api-key
  secret, refresh-token, access-token
  private-key, credential
  x-csrf-token, csrf-token

Query params:
  password, token, apiKey
```

### 9.3 Masking implementation

```java
// Regex-based JSON field masking
// Pattern: "fieldName"\s*:\s*"[value]"
// Replacement: "fieldName":"[MASKED]"
private static String maskJsonField(String json, String fieldName) {
    String[] patterns = {
        "\"" + fieldName + "\"\\s*:\\s*\"[^\"]*\"",
        "\"" + fieldName + "\"\\s*:\\s*\\[[^\\]]*\\]"
    };
    // Case-insensitive replaceAll
}
```

---

## 10. Logging Format & ELK Compatibility

### 10.1 Current pattern (console/file)

```
%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %X{traceId:-} %logger{36} - %msg%n

↓ Giải thích
2026-06-08 09:47:00.123 [http-nio-1] INFO  abc123-def  c.p.s.l.f.RequestLoggingFilter - traceId="abc123" method="POST" uri="/api/v1/users" status=201 duration=45ms
```

| Token | Giá trị | Ý nghĩa |
|-------|---------|---------|
| `%d{yyyy-MM-dd HH:mm:ss.SSS}` | 2026-06-08 09:47:00.123 | Timestamp milliseconds |
| `[%thread]` | [http-nio-1] | Thread name |
| `%-5level` | INFO | Log level (5 chars) |
| `%X{traceId:-}` | abc123-def | MDC trace ID (- = default nếu null) |
| `%logger{36}` | c.p.s.l.f.RequestLoggingFilter | Logger name (36 = full) |
| `%msg` | traceId="abc123"... | Message |
| `%n` | newline | Platform newline |

### 10.2 ELK-ready format

| ELK Component | Integration |
|---------------|-------------|
| **Elasticsearch** | traceId là field, có thể filter/search theo traceId |
| **Logstash** | Pattern-based parsing, traceId extracted thành field |
| **Kibana** | Dashboard theo traceId, duration, status code |
| **OpenTelemetry** | traceId có thể map sang span ID |

### 10.3 ELK-ready JSON format (optional)

Khi cần JSON format cho Logstash, thêm dependency vào `pom.xml`:

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>8.0</version>
</dependency>
```

Sau đó tạo `logback-spring.xml` với JSON appender:

```xml
<appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
    <encoder class="net.logstash.logback.encoder.LogstashEncoder">
        <includeMdcKeyName>traceId</includeMdcKeyName>
        <includeMdcKeyName>userId</includeMdcKeyName>
    </encoder>
</appender>
```

Output JSON:
```json
{
  "@timestamp": "2026-06-08T09:47:00.123Z",
  "level": "INFO",
  "traceId": "abc123-def456",
  "userId": "user_42",
  "logger": "c.p.s.l.f.RequestLoggingFilter",
  "message": "traceId=\"abc123\" method=\"POST\" uri=\"/api/v1/users\""
}
```

---

## 11. Logging Configuration

### 11.1 application.yml (base — tất cả environments)

```yaml
logging:
  slow-api:
    enabled: true
    threshold-ms: 1000
    paths-to-ignore: /actuator/health,/health,/actuator/info

  request:
    enabled: true
    max-body-length: 2000

  level:
    root: INFO
    com.petsplatform: INFO
    org.hibernate.SQL: WARN
    org.springframework.web: INFO

  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %X{traceId:-} %logger{36} - %msg%n"

  file:
    name: logs/pets-platform.log
    max-size: 50MB
    max-history: 14
```

### 11.2 application-dev.yml (development)

```yaml
logging:
  level:
    com.petsplatform: DEBUG
    org.hibernate.SQL: DEBUG
    org.springframework.web: DEBUG
```

### 11.3 application-prod.yml (production)

```yaml
logging:
  slow-api:
    enabled: true
    threshold-ms: 500      # Stricter threshold in prod
    paths-to-ignore: /actuator/health,/health

  level:
    root: WARN
    com.petsplatform: INFO
    com.petsplatform.shared.logging.filter: INFO

  file:
    name: /var/log/pets-platform/pets-platform.log
    max-size: 100MB
    max-history: 30
```

### 11.4 Log level guidelines

| Package | DEV | STAGING | PROD |
|---------|-----|---------|------|
| `com.petsplatform` | DEBUG | INFO | INFO |
| `org.hibernate.SQL` | DEBUG | WARN | ERROR |
| `org.springframework.web` | DEBUG | INFO | WARN |
| `RequestLoggingFilter` | DEBUG | INFO | INFO |
| `SlowApiWarningFilter` | WARN | WARN | WARN |
| Root | INFO | INFO | WARN |

---

## 12. Cách dùng trong code

### 12.1 Trong service — thêm user context

```java
@Service
@RequiredArgsConstructor
public class UserService {

    public void createUser(CreateUserDto dto) {
        // Thêm userId vào MDC — tự động xuất hiện trong mọi log
        MdcUtils.putUserId(getCurrentUserId());
        try {
            log.info("Creating user with email: {}", dto.getEmail());
            // Business logic
            log.info("User created successfully: {}", user.getId());
        } finally {
            MdcUtils.remove(MdcUtils.USER_ID);
        }
    }

    public void processPayment(Long orderId) {
        MdcUtils.put("orderId", orderId.toString());
        MdcUtils.put("transactionType", "PAYMENT");
        try {
            log.info("Processing payment for order");
        } finally {
            MdcUtils.remove("orderId");
            MdcUtils.remove("transactionType");
        }
    }
}
```

### 12.2 Trong scheduled job

```java
@Scheduled(cron = "0 0 2 * * ?")
public void cleanupExpiredSessions() {
    MdcUtils.putTraceId("SCHEDULED_CLEANUP");
    MdcUtils.put("jobName", "session-cleanup");
    try {
        log.info("Starting session cleanup job");
        sessionRepository.deleteExpired(LocalDateTime.now());
        log.info("Session cleanup completed");
    } finally {
        MdcUtils.clear();
    }
}
```

### 12.3 Trong security/auth layer

```java
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain chain)
            throws ServletException, IOException {
        try {
            String userId = extractUserId(jwt);
            MdcUtils.putUserId(userId);
            MdcUtils.put("authType", "JWT");
            chain.doFilter(request, response);
        } finally {
            MdcUtils.clear();
        }
    }
}
```

---

## 13. ELK Stack Integration

### 13.1 Overview

```
┌─────────────┐    ┌─────────────┐    ┌──────────────┐    ┌──────────┐
│ PetsPlatform│───►│  Logstash   │───►│Elasticsearch │───►│  Kibana  │
│  (app.log)  │    │(parse json) │    │ (index logs) │    │(dashboard│
└─────────────┘    └─────────────┘    └──────────────┘    └──────────┘
```

### 13.2 Elasticsearch index mapping

```json
{
  "mappings": {
    "properties": {
      "@timestamp": { "type": "date" },
      "traceId":   { "type": "keyword" },
      "userId":    { "type": "keyword" },
      "method":    { "type": "keyword" },
      "uri":       { "type": "keyword" },
      "status":    { "type": "integer" },
      "duration":  { "type": "long" },
      "level":     { "type": "keyword" },
      "message":   { "type": "text" }
    }
  }
}
```

### 13.3 Kibana dashboards

| Dashboard | Metrics |
|-----------|---------|
| Request volume | Số request/giờ theo endpoint |
| Slow API | Top 10 slowest endpoints |
| Error rate | % requests với status >= 400 |
| Trace analysis | Tìm all logs với cùng traceId |
| User activity | Logs theo userId |

---

## 14. File Index

| File | Package | Lines | Mục đích |
|------|---------|-------|----------|
| `MdcUtils.java` | `shared.logging` | 67 | MDC utility (put/get/clear) |
| `RequestLoggingFilter.java` | `shared.logging.filter` | 197 | Core request/response logging + masking |
| `SlowApiWarningFilter.java` | `shared.logging.filter` | 68 | Slow API detection |
| `CachedBodyHttpServletRequest.java` | `shared.logging.filter` | 74 | Request body caching wrapper |
| `application.yml` | resources | updated | Logging pattern + slow-api config |
| `application-prod.yml` | resources | updated | Prod logging levels |

---

## Checklist trước khi merge

- [ ] `MdcUtils.clear()` được gọi trong `finally` block của mọi filter
- [ ] Sensitive fields (password, token) được mask trong logs
- [ ] `X-Trace-Id` header được forward giữa services
- [ ] Slow API threshold phù hợp với từng environment
- [ ] Log level đã set đúng cho prod (INFO cho app, WARN cho root)
- [ ] File log rotation đã configure (`max-size`, `max-history`)
- [ ] Actuator health endpoint được ignore trong slow API filter
- [ ] Khi thêm ELK: `traceId` là keyword field trong Elasticsearch index
- [ ] Không log sensitive data trong production
- [ ] Thêm service mới: KHÔNG cần thay đổi gì trong logging system
