# Exception Handling System

**File:** `080626.002.md`
**Module:** `com.petsplatform.shared.exception`
**Version:** 1.0.0

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Lý do centralized exception handling quan trọng](#2-lý-do-centralized-exception-handling-quan-trọng)
3. [Class diagram](#3-class-diagram)
4. [Exception classes](#4-exception-classes)
5. [GlobalExceptionHandler chi tiết](#5-globalexceptionhandler-chi-tiết)
6. [HTTP status mapping](#6-http-status-mapping)
7. [Exception flow](#7-exception-flow)
8. [Cách sử dụng trong service layer](#8-cách-sử-dụng-trong-service-layer)
9. [Mở rộng kiến trúc](#9-mở-rộng-kiến-trúc)
10. [Scalability analysis](#10-scalability-analysis)
11. [File index](#11-file-index)

---

## 1. Tổng quan kiến trúc

Hệ thống exception handling của Pets Platform gồm 4 class exception và 1 handler trung tâm:

```
┌─────────────────────────────────────────────────┐
│          GlobalExceptionHandler                  │
│          @RestControllerAdvice                   │
│          (1 file xử lý TẤT CẢ exception)        │
└──────────────┬──────────────────────────────────┘
               │ maps to
┌──────────────▼──────────────────────────────────┐
│            ApiResponse<Void>                     │
│            (consistent JSON structure)           │
└─────────────────────────────────────────────────┘

com.petsplatform.shared.exception/
├── BusinessException.java          # Base class
├── ResourceNotFoundException.java  # 404 Not Found
├── ResourceConflictException.java   # 409 Conflict
├── ValidationException.java        # 422 Validation Failed
└── GlobalExceptionHandler.java     # @RestControllerAdvice
```

---

## 2. Lý do Centralized Exception Handling quan trọng

### 2.1 Vấn đề khi KHÔNG có centralized handler

```
Khi người A viết: throw new RuntimeException("User not found")
Khi người B viết: return ResponseEntity.badRequest().body("User not found")
Khi người C viết: throw new NotFoundException("ERR_404", "User not found")

→ Frontend phải handle 3 format khác nhau
→ Mỗi lần thêm API mới phải copy-paste error handling
→ Quên try-catch → server crash
→ HTTP status code không đồng nhất
```

### 2.2 Giải pháp với centralized handler

| Vấn đề | Giải pháp |
|---------|------------|
| Error format không đồng nhất | Tất cả trả về `ApiResponse.error(...)` |
| Quên handle exception | `Exception.class` catch-all ở cuối |
| HTTP status lộn xộn | `ApiError.getHttpStatus()` tự động map |
| Validation error format khác nhau | `buildValidationResponse()` thống nhất |
| Thêm module phải viết lại handler | Dùng `BusinessException` base, handler có sẵn |
| Khó debug production | `timestamp` trong `ApiResponse` + centralized logging |

### 2.3 So sánh ở các mức độ

| Tiêu chí | Không centralized | Có centralized |
|----------|-------------------|-----------------|
| Số chỗ xử lý error | N × Controller methods | 1 file (GlobalExceptionHandler) |
| Consistency | Mỗi người viết 1 kiểu | 100% đồng nhất |
| Thêm error type mới | Sửa N files | Thêm 1 handler hoặc exception class |
| Memory leak risk | Có thể uncaught exception | Catch-all luôn có |
| Performance | Duplicate try-catch overhead | 1 central point |

---

## 3. Class diagram

```
                        ┌─────────────────────────┐
                        │    RuntimeException      │
                        │       (java.lang)       │
                        └───────────┬─────────────┘
                                    │
                        ┌───────────▼─────────────┐
                        │    BusinessException      │
                        │───────────────────────── │
                        │ - error: ApiError         │
                        │ - overrideMessage: String│
                        │───────────────────────── │
                        │ + getError()             │
                        │ + getDisplayMessage()    │
                        └───────────┬─────────────┘
                                    │
          ┌──────────────────────────┼──────────────────────────┐
          │                          │                          │
┌─────────▼──────────┐   ┌─────────▼──────────┐   ┌─────────▼──────────┐
│ResourceNotFoundException│  │ValidationException │   │ResourceConflictException│
│───────────────────── │   │────────────────── │   │────────────────── │
│ + (resourceName)     │   │- fieldErrors: Map │   │ + (resourceName)   │
│ + (resourceName, id) │   │+ getFieldErrors()│   │ + (resourceName,  │
│ + (resourceName,field,│   │                   │   │      field, value) │
│      value)          │   │                   │   │                   │
└─────────────────────┘   └───────────────────┘   └───────────────────┘

┌─────────────────────────────────────────────────────────────────────────┐
│                    GlobalExceptionHandler                                │
│                    @RestControllerAdvice                                 │
│─────────────────────────────────────────────────────────────────────────│
│ + handleNotFound(ResourceNotFoundException)         → 404               │
│ + handleConflict(ResourceConflictException)          → 409               │
│ + handleValidation(ValidationException)             → 422               │
│ + handleBusiness(BusinessException)                 → auto              │
│ + handleMethodArgumentNotValid(...)                 → 422               │
│ + handleHandlerMethodValidation(...)                → 422               │
│ + handleConstraintViolation(...)                    → 422               │
│ + handleMissingParam(...)                           → 400               │
│ + handleTypeMismatch(...)                           → 400               │
│ + handleMalformedJson(...)                          → 400               │
│ + handleNoResource(...)                             → 404               │
│ + handleUnknown(Exception)                          → 500 (catch-all)   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 4. Exception Classes

### 4.1 BusinessException — Base Class

**File:** `src/main/java/com/petsplatform/shared/exception/BusinessException.java`

```java
public class BusinessException extends RuntimeException {

    private final ApiError error;
    private final String overrideMessage;

    public BusinessException(ApiError error) { ... }
    public BusinessException(ApiError error, String overrideMessage) { ... }
    public ApiError getError() { ... }
    public String getDisplayMessage() { ... }
}
```

**Design decisions:**

| Quyết định | Lý do |
|------------|-------|
| `extends RuntimeException` | Không cần khai báo throws, dùng trong service layer tiện lợi |
| `ApiError` field | Handler tự map sang HttpStatus, không cần logic rẽ nhánh |
| `overrideMessage` | Cho phép custom message mà vẫn giữ nguyên error code |
| Immutable fields | `final` — thread-safe khi exception được reuse |

### 4.2 ResourceNotFoundException

**File:** `src/main/java/com/petsplatform/shared/exception/ResourceNotFoundException.java`

```java
// Constructor 1: Generic resource name
throw new ResourceNotFoundException("User");
// → "User not found"

// Constructor 2: Resource với id
throw new ResourceNotFoundException("User", 42L);
// → "User with id [42] not found"

// Constructor 3: Resource với field cụ thể
throw new ResourceNotFoundException("User", "email", "john@example.com");
// → "User with email [john@example.com] not found"
```

**Sử dụng trong Service:**

```java
public User getUser(Long id) {
    return userRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("User", id));
}

public User findByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
}
```

### 4.3 ValidationException

**File:** `src/main/java/com/petsplatform/shared/exception/ValidationException.java`

Khác với Spring `MethodArgumentNotValidException` — dùng cho **business validation** (sau khi đã qua bean validation).

```java
// Constructor 1: Simple message
throw new ValidationException("Invalid status transition");

// Constructor 2: Chi tiết từng field
Map<String, String> errors = new HashMap<>();
errors.put("email", "Email already registered");
errors.put("phone", "Phone number is invalid");
throw new ValidationException(errors);
// → HTTP 422, data = { "email": "...", "phone": "..." }

// Constructor 3: Một field cụ thể
throw new ValidationException("oldPassword", "Current password is incorrect");
```

**Khi nào dùng ValidationException vs MethodArgumentNotValidException:**

| Loại | Nguồn | Trigger | Dùng khi |
|------|-------|---------|-----------|
| `MethodArgumentNotValidException` | `@Valid` + `@RequestBody` | Bean validation fail | Input DTO validation |
| `ValidationException` | Manual check | Business rule fail | Cross-field, database check |

```java
// Ví dụ: Dùng ValidationException cho business validation
public void transferMoney(Account from, Account to, BigDecimal amount) {
    if (from.getBalance().compareTo(amount) < 0) {
        throw new ValidationException("balance",
            "Insufficient balance. Available: " + from.getBalance());
    }
    if (from.getId().equals(to.getId())) {
        throw new ValidationException("toAccount",
            "Cannot transfer to the same account");
    }
}
```

### 4.4 ResourceConflictException

**File:** `src/main/java/com/petsplatform/shared/exception/ResourceConflictException.java`

```java
throw new ResourceConflictException("User");
// → "User already exists"

throw new ResourceConflictException("User", "email", "john@example.com");
// → "User with email [john@example.com] already exists"

// Trong service
public User createUser(CreateUserDto dto) {
    if (userRepository.existsByEmail(dto.getEmail())) {
        throw new ResourceConflictException("User", "email", dto.getEmail());
    }
    return userRepository.save(mapper.toEntity(dto));
}
```

---

## 5. GlobalExceptionHandler chi tiết

**File:** `src/main/java/com/petsplatform/shared/exception/GlobalExceptionHandler.java`

### 5.1 Các nhóm handler

```
@RestControllerAdvice
└── GlobalExceptionHandler
    ├── Nhóm 1: Business Exceptions (4xx từ domain logic)
    │   ├── ResourceNotFoundException     → 404
    │   ├── ResourceConflictException    → 409
    │   ├── ValidationException          → 422
    │   └── BusinessException           → auto
    │
    ├── Nhóm 2: Spring Validation Exceptions
    │   ├── MethodArgumentNotValidException    → 422  (@Valid @RequestBody)
    │   ├── HandlerMethodValidationException   → 422  (@Validated @RequestParam)
    │   └── ConstraintViolationException       → 422  (JSR-303)
    │
    ├── Nhóm 3: HTTP Request Exceptions
    │   ├── MissingServletRequestParameterException  → 400
    │   ├── MethodArgumentTypeMismatchException     → 400
    │   ├── HttpMessageNotReadableException        → 400
    │   └── NoResourceFoundException                → 404
    │
    └── Nhóm 4: Catch-all (LUÔN LUÔN ở CUỐI)
        └── Exception.class                        → 500
```

### 5.2 Helper methods

```java
private ResponseEntity<ApiResponse<Void>> buildResponse(ApiError error, String message) {
    return ResponseEntity
        .status(error.getHttpStatus())
        .body(ApiResponse.error(error, message));
}

private ResponseEntity<ApiResponse<Map<String, String>>> buildValidationResponse(
        Map<String, String> fieldErrors) {
    String message = "Validation failed for " + fieldErrors.size() + " field(s)";
    ApiResponse<Map<String, String>> response = ApiResponse
        .<Map<String, String>>builder()
        .success(false)
        .message(message)
        .data(fieldErrors)
        .error(ApiError.VALIDATION_FAILED)
        .build();
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
}
```

### 5.3 Handler cho Spring Validation

**`MethodArgumentNotValidException`** — Dùng khi:

```java
// Controller
@PostMapping
public ApiResponse<User> create(@Valid @RequestBody CreateUserDto dto) { ... }

// GlobalExceptionHandler tự bắt
Map<String, String> fieldErrors = ex.getBindingResult()
    .getFieldErrors()
    .stream()
    .collect(Collectors.toMap(
        FieldError::getField,
        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
        (a, b) -> a  // keep first
    ));
// → HTTP 422, data = { "email": "must not be blank", ... }
```

**`HandlerMethodValidationException`** — Dùng khi:

```java
// Controller
@Validated
public class UserController {
    @GetMapping
    public ApiResponse<List<User>> search(
        @RequestParam @NotBlank String query,
        @RequestParam @Min(1) Integer page) { ... }
}
```

**`ConstraintViolationException`** — Dùng khi:

```java
// Controller
@Validated
public class UserController {
    @GetMapping("/{id}")
    public ApiResponse<User> getById(
        @PathVariable @NotNull Long id) { ... }
}
```

### 5.4 Lưu ý quan trọng về thứ tự handler

```java
// ❌ SAI: catch-all Exception.class ở trên BusinessException
// → BusinessException bị catch-all bắt trước
@ExceptionHandler(Exception.class) { ... }           // catch-all
@ExceptionHandler(BusinessException.class) { ... }    // KHÔNG BAO GIỜ chạy

// ✅ ĐÚNG: từ specific → general
@ExceptionHandler(ResourceNotFoundException.class) { ... }  // ✅ cụ thể
@ExceptionHandler(BusinessException.class) { ... }          // ✅ base class
@ExceptionHandler(Exception.class) { ... }                  // ✅ catch-all CUỐI CÙNG
```

---

## 6. HTTP Status Mapping

Mỗi `ApiError` đã được map sẵn với `HttpStatus`. Handler KHÔNG cần if-else.

### 6.1 Bảng mapping đầy đủ

| ApiError | HTTP Status | Khi nào |
|----------|------------|---------|
| `BAD_REQUEST` | 400 | Malformed request, wrong type |
| `UNAUTHORIZED` | 401 | Chưa authenticate |
| `FORBIDDEN` | 403 | Không có quyền |
| `NOT_FOUND` | 404 | Resource không tồn tại |
| `CONFLICT` | 409 | Resource đã tồn tại |
| `VALIDATION_FAILED` | 422 | Validation fail |
| `TOO_MANY_REQUESTS` | 429 | Rate limit |
| `INTERNAL_ERROR` | 500 | Unexpected error |
| `SERVICE_UNAVAILABLE` | 503 | External service down |
| `GATEWAY_TIMEOUT` | 504 | Gateway timeout |

### 6.2 Application error codes → HTTP Status

| ApiError | HTTP Status | Mã lỗi |
|----------|------------|---------|
| `RESOURCE_ALREADY_EXISTS` | 409 | ERR_1001 |
| `RESOURCE_NOT_FOUND` | 404 | ERR_1002 |
| `INVALID_CREDENTIALS` | 401 | ERR_1003 |
| `TOKEN_EXPIRED` | 401 | ERR_1004 |
| `TOKEN_INVALID` | 401 | ERR_1005 |
| `OPERATION_NOT_ALLOWED` | 403 | ERR_1006 |
| `DATA_INTEGRITY_VIOLATION` | 409 | ERR_1007 |
| `DATABASE_ERROR` | 500 | ERR_2001 |
| `EXTERNAL_SERVICE_ERROR` | 502 | ERR_3001 |
| `CACHE_ERROR` | 503 | ERR_4001 |

---

## 7. Exception Flow

### 7.1 Happy path

```
Request: GET /api/v1/users/99
    │
    ▼
UserController.getUser(99)
    │
    ▼
UserService.getUser(99)
    │
    ▼
userRepository.findById(99) → Optional.empty()
    │
    ▼ (throw)
ResourceNotFoundException("User", 99L)
    │
    ▼
Exception reaches DispatcherServlet
    │
    ▼
@RestControllerAdvice intercepts
    │
    ▼
GlobalExceptionHandler.handleNotFound(ex)
    │
    ▼
buildResponse(NOT_FOUND, "User with id [99] not found")
    │
    ▼
ApiResponse.error(NOT_FOUND, "User with id [99] not found")
    │
    ▼
HTTP 404 + JSON:
{
  "success": false,
  "message": "User with id [99] not found",
  "data": null,
  "timestamp": "2026-06-08T09:05:00Z",
  "error": { "code": "ERR_404", "message": "Resource not found" }
}
```

### 7.2 Validation error path

```
POST /api/v1/users
{
  "email": "invalid-email",
  "age": -5
}
    │
    ▼
@Valid → MethodArgumentNotValidException thrown
    │
    ▼
@RestControllerAdvice intercepts
    │
    ▼
handleMethodArgumentNotValid(ex)
    │
    ▼
buildValidationResponse({ "email": "...", "age": "..." })
    │
    ▼
HTTP 422 + JSON:
{
  "success": false,
  "message": "Validation failed for 2 field(s)",
  "data": { "email": "Invalid email format", "age": "Must be positive" },
  "timestamp": "...",
  "error": { "code": "ERR_422", "message": "Validation failed" }
}
```

---

## 8. Cách sử dụng trong Service Layer

### 8.1 Basic CRUD

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public ApiResponse<User> findById(Long id) {
        return ResponseHelper.okOrNotFound(
            userRepository.findById(id).orElse(null),
            "User"
        );
    }

    public ApiResponse<User> create(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new ResourceConflictException("User", "email", dto.getEmail());
        }
        User saved = userRepository.save(mapper.toEntity(dto));
        return ResponseHelper.successMsgData("User created", saved);
    }

    public ApiResponse<User> update(Long id, UpdateUserDto dto) {
        User existing = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", id));
        // merge and save
        return ResponseHelper.successMsgData("User updated", userRepository.save(existing));
    }

    public ApiResponse<Void> delete(Long id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User", id);
        }
        userRepository.deleteById(id);
        return ResponseHelper.success("User deleted");
    }
}
```

### 8.2 Business Validation với ValidationException

```java
public ApiResponse<Order> placeOrder(Long userId, OrderRequest request) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new ResourceNotFoundException("User", userId));

    // Business rule validation
    Map<String, String> errors = new HashMap<>();
    if (user.getStatus() != UserStatus.ACTIVE) {
        errors.put("userStatus", "User account is not active");
    }
    if (request.getAmount().compareTo(MIN_ORDER) < 0) {
        errors.put("amount", "Minimum order amount is " + MIN_ORDER);
    }
    if (!errors.isEmpty()) {
        throw new ValidationException(errors);
    }

    Order order = orderService.create(user, request);
    return ResponseHelper.successMsgData("Order placed", order);
}
```

### 8.3 Nested BusinessException

```java
// Tạo custom exception mới — KHÔNG cần sửa GlobalExceptionHandler
public class InsufficientBalanceException extends BusinessException {
    public InsufficientBalanceException(BigDecimal available, BigDecimal requested) {
        super(ApiError.OPERATION_NOT_ALLOWED,
              String.format("Insufficient balance. Available: %s, Requested: %s",
                  available, requested));
    }
}

// Dùng ngay — handler có sẵn
throw new InsufficientBalanceException(wallet.getBalance(), amount);
```

---

## 9. Mở rộng kiến trúc

### 9.1 Thêm custom exception mới

```java
// Bước 1: Tạo class mới
public class ExternalPaymentException extends BusinessException {
    public ExternalPaymentException(String provider, String reason) {
        super(ApiError.EXTERNAL_SERVICE_ERROR,
              "Payment provider [" + provider + "] error: " + reason);
    }
}

// Bước 2: Dùng ngay — KHÔNG cần sửa GlobalExceptionHandler
throw new ExternalPaymentException("Stripe", "Card declined");

// Output: HTTP 502 + { "code": "ERR_3001", "message": "Payment provider [Stripe] error: Card declined" }
```

### 9.2 Thêm error code mới vào ApiError

```java
// Thêm vào ApiError.java
SUBSCRIPTION_EXPIRED("ERR_1008", "Subscription expired", HttpStatus.PAYMENT_REQUIRED),
PAYMENT_REQUIRED("ERR_402", "Payment required", HttpStatus.PAYMENT_REQUIRED),
EMAIL_ALREADY_VERIFIED("ERR_1010", "Email already verified", HttpStatus.CONFLICT),

// KHÔNG cần sửa handler — getHttpStatus() tự động map
```

### 9.3 Thêm handler mới cho exception không phải BusinessException

```java
// Trong GlobalExceptionHandler, thêm handler mới:
@ExceptionHandler(DataAccessException.class)
public ResponseEntity<ApiResponse<Void>> handleDataAccess(DataAccessException ex) {
    // Log chi tiết, có thể inspect nguyên nhân gốc
    Throwable root = ex.getMostSpecificCause();
    return buildResponse(ApiError.DATABASE_ERROR,
        "Database operation failed: " + root.getMessage());
}
```

### 9.4 Logging nâng cao

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception ex,
        HttpServletRequest request,
        HttpServletResponse response) {
    String traceId = UUID.randomUUID().toString();
    log.error("Unhandled exception [traceId={}] [path={}] [method={}]",
        traceId, request.getRequestURI(), request.getMethod(), ex);
    return buildResponse(ApiError.INTERNAL_ERROR,
        "An unexpected error occurred. Reference: " + traceId);
}
```

### 9.5 Thêm field vào response

```java
// Muốn thêm traceId vào mọi response error?
// → Sửa 1 chỗ trong GlobalExceptionHandler.buildResponse()
// → Mọi API error response tự cập nhật
```

---

## 10. Scalability Analysis

### 10.1 Khi thêm module mới

```
Module cũ: User, Product, Order
Module mới: Payment, Notification

Không cần thay đổi gì trong exception system:
✓ GlobalExceptionHandler giữ nguyên
✓ ApiError giữ nguyên
✓ Service chỉ cần throw BusinessException hoặc subclasses

→ Zero changes to exception infrastructure
```

### 10.2 Khi chuyển sang Microservices

```
Monolith:                    Microservices:
UserService ──► DB           UserService (separate app)
                          → AuthService (separate app)
                          → PaymentService (separate app)

Mỗi service CÓ THỂ:
- Giữ nguyên exception classes (share JAR)
- Hoặc chỉ dùng ApiError enum + BusinessException base

→ Consistent error codes across ALL services
→ Frontend handle 1 error structure cho mọi service
```

### 10.3 Performance considerations

| Factor | Impact | Mitigation |
|--------|--------|------------|
| Exception instantiation | `new ResourceNotFoundException(...)` là cheap | Không vấn đề |
| Stack trace capture | Có overhead khi throw | Chỉ khi exception thực sự xảy ra |
| Handler lookup | Spring dùng concurrent HashMap | O(1) lookup |
| GC pressure | Exception objects ngắn hạn | Không vấn đề với modern GC |

### 10.4 Multi-module scalability

```
pets-platform/
├── module-common/          # shared.exception, shared.response
│   └── ApiError, BusinessException, GlobalExceptionHandler
│
├── module-user/            # user domain
├── module-product/         # product domain
├── module-order/           # order domain
└── module-payment/        # payment domain

Khi module-common thay đổi:
→ Compile lại module-common
→ Compile lại tất cả modules phụ thuộc
→ Deploy tất cả

Khi module-order thêm ValidationException mới:
→ Chỉ cần sửa module-order + module-common
→ Các module khác không cần thay đổi
```

---

## 11. File Index

| File | Package | Lines | Mục đích |
|------|---------|-------|----------|
| `BusinessException.java` | `shared.exception` | 33 | Base unchecked exception cho mọi business violation |
| `ResourceNotFoundException.java` | `shared.exception` | 22 | 404 — resource không tồn tại |
| `ResourceConflictException.java` | `shared.exception` | 17 | 409 — resource đã tồn tại |
| `ValidationException.java` | `shared.exception` | 34 | 422 — business validation fail |
| `GlobalExceptionHandler.java` | `shared.exception` | 180 | `@RestControllerAdvice` — 12 handlers |

---

## Checklist trước khi merge

- [ ] Tất cả service throw `BusinessException` hoặc subclass, không throw `RuntimeException` trực tiếp
- [ ] `ResourceNotFoundException` dùng đúng constructor (1, 2, hoặc 3 tham số)
- [ ] `ValidationException` dùng cho business validation, không phải bean validation
- [ ] Controller KHÔNG có try-catch xung quanh business logic
- [ ] Exception không chứa thông tin nhạy cảm (password, token) trong message
- [ ] Catch-all `Exception.class` LUÔN ở CUỐI trong `GlobalExceptionHandler`
- [ ] `ApiError` enum đã được sử dụng cho error code chuẩn
- [ ] Thêm custom exception mới phải extend `BusinessException`
