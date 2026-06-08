# Global API Response Structure

Tài liệu này mô tả cấu trúc response chuẩn cho tất cả API trong Pets Platform. Mọi API endpoint đều trả về format thống nhất thông qua `ApiResponse<T>`.

---

## Mục lục

1. [Tổng quan](#1-tổng-quan)
2. [Cấu trúc JSON](#2-cấu-trúc-json)
3. [ApiResponse - Generic Wrapper](#3-apiresponse---generic-wrapper)
4. [ApiError - Mã lỗi chuẩn](#4-apierror---mã-lỗi-chuẩn)
5. [ApiResponseBuilder - Builder Pattern](#5-apiresponsebuilder---builder-pattern)
6. [ResponseHelper - Tiện ích](#6-responsehelper---tiện-ính)
7. [Pagination Response](#7-pagination-response)
8. [Exception Handler tích hợp](#8-exception-handler-tích-hợp)
9. [Ví dụ sử dụng](#9-ví-dụ-sử-dụng)

---

## 1. Tổng quan

### Vấn đề khi không có wrapper

Khi mỗi API trả về format khác nhau, frontend phải xử lý rất nhiều case:

```
/api/users     → { "id": 1, "name": "John" }
                { "status": "ok", "user": {...} }
                { "data": {...}, "code": 200 }
/api/products  → { "items": [...] }
                [...]
```

### Giải pháp

Tất cả API trả về cùng một JSON structure:

```
{
  "success": true|false,
  "message": "Mô tả",
  "data": { ... },
  "timestamp": "2026-06-08T08:52:00Z",
  "error": null | { "code": "...", "message": "..." }
}
```

### Lợi ích

| STT | Lợi ích | Mô tả |
|-----|---------|-------|
| 1 | Thống nhất | Frontend chỉ cần đọc 1 lần, reuse mọi API |
| 2 | Type-safe | Frontend định nghĩa `ApiResponse<T>` 1 lần |
| 3 | Dễ debug | Log trace dễ dàng qua `timestamp` và `error.code` |
| 4 | Mở rộng dễ | Thêm field mới (traceId, pagination) chỉ cần sửa 1 class |
| 5 | i18n | `error.code` dùng cho multi-language, `message` cho hiển thị |

---

## 2. Cấu trúc JSON

### Success Response

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "Buddy",
    "species": "Dog"
  },
  "timestamp": "2026-06-08T08:52:00Z",
  "error": null
}
```

### Error Response

```json
{
  "success": false,
  "message": "User not found",
  "data": null,
  "timestamp": "2026-06-08T08:52:00Z",
  "error": {
    "code": "ERR_404",
    "message": "Resource not found"
  }
}
```

### Collection Response

```json
{
  "success": true,
  "message": "Success",
  "data": [
    { "id": 1, "name": "Buddy" },
    { "id": 2, "name": "Whiskers" }
  ],
  "timestamp": "2026-06-08T08:52:00Z",
  "error": null
}
```

### Validation Error Response

```json
{
  "success": false,
  "message": "Validation failed for 2 field(s)",
  "data": {
    "email": "Invalid email format",
    "name": "Name is required"
  },
  "timestamp": "2026-06-08T08:52:00Z",
  "error": {
    "code": "ERR_422",
    "message": "Validation failed"
  }
}
```

### Quy tắc Serialization

- `null` fields **KHÔNG** serialize ra JSON (nhờ `@JsonInclude(NON_NULL)`)
- Trường hợp `success: true` → `error` luôn là `null`
- Trường hợp `success: false` → `data` thường là `null` (trừ validation error)

---

## 3. ApiResponse - Generic Wrapper

**File:** `src/main/java/com/petsplatform/shared/response/ApiResponse.java`

### Đặc điểm thiết kế

| Đặc điểm | Chi tiết |
|-----------|----------|
| Immutable | `final class`, tất cả field `final` |
| Generic | `ApiResponse<User>`, `ApiResponse<List<Product>>` |
| Package-private constructor | Chỉ `ApiResponseBuilder` cùng package được tạo |
| Không dùng Lombok | Tránh xung đột với generic + nested class |

### Các static factory methods

#### Success factories

```java
// Không có data
ApiResponse<User> r1 = ApiResponse.ok();

// Với data, message mặc định "Success"
ApiResponse<User> r2 = ApiResponse.ok(user);

// Chỉ message, không data
ApiResponse<User> r3 = ApiResponse.okMsg("User created");
```

#### Error factories

```java
// Error đơn giản
ApiResponse<User> r4 = ApiResponse.error("Something went wrong");

// Error với ApiError enum
ApiResponse<User> r5 = ApiResponse.error(ApiError.NOT_FOUND);

// Error với message override
ApiResponse<User> r6 = ApiResponse.error(ApiError.BAD_REQUEST, "Email is invalid");
```

#### Utility factories

```java
// Wrap một Supplier, tự bắt exception
ApiResponse<User> r7 = ApiResponse.of(() -> userRepository.findById(1L));

// Conditional
ApiResponse<User> r8 = ApiResponse.of(user, user != null);
```

### Các getter methods

| Method | Mô tả |
|--------|--------|
| `isSuccess()` | Trả về `true` nếu thành công |
| `getMessage()` | Lấy message mô tả |
| `getData()` | Lấy payload data |
| `getTimestamp()` | Lấy thời điểm tạo response |
| `getError()` | Lấy `ApiError` object (null nếu thành công) |

---

## 4. ApiError - Mã lỗi chuẩn

**File:** `src/main/java/com/petsplatform/shared/response/ApiError.java`

### HTTP Standard Errors (1xx-5xx)

| Enum | Code | Message | HTTP Status |
|------|------|---------|------------|
| `BAD_REQUEST` | `ERR_400` | Bad request | 400 |
| `UNAUTHORIZED` | `ERR_401` | Unauthorized | 401 |
| `FORBIDDEN` | `ERR_403` | Access denied | 403 |
| `NOT_FOUND` | `ERR_404` | Resource not found | 404 |
| `CONFLICT` | `ERR_409` | Resource conflict | 409 |
| `VALIDATION_FAILED` | `ERR_422` | Validation failed | 422 |
| `TOO_MANY_REQUESTS` | `ERR_429` | Too many requests | 429 |
| `INTERNAL_ERROR` | `ERR_500` | Internal server error | 500 |
| `SERVICE_UNAVAILABLE` | `ERR_503` | Service unavailable | 503 |
| `GATEWAY_TIMEOUT` | `ERR_504` | Gateway timeout | 504 |

### Application-Specific Errors (1001+)

| Enum | Code | Message |
|------|------|---------|
| `RESOURCE_ALREADY_EXISTS` | `ERR_1001` | Resource already exists |
| `RESOURCE_NOT_FOUND` | `ERR_1002` | Resource not found |
| `INVALID_CREDENTIALS` | `ERR_1003` | Invalid credentials |
| `TOKEN_EXPIRED` | `ERR_1004` | Token expired |
| `TOKEN_INVALID` | `ERR_1005` | Token invalid |
| `OPERATION_NOT_ALLOWED` | `ERR_1006` | Operation not allowed |
| `DATA_INTEGRITY_VIOLATION` | `ERR_1007` | Data integrity violation |
| `DATABASE_ERROR` | `ERR_2001` | Database error |
| `EXTERNAL_SERVICE_ERROR` | `ERR_3001` | External service error |
| `CACHE_ERROR` | `ERR_4001` | Cache error |

### Cách thêm error code mới

```java
// Trong ApiError.java, thêm vào enum:
RESOURCE_DELETED("ERR_1008", "Resource has been deleted"),
PAYMENT_FAILED("ERR_5001", "Payment processing failed"),
```

### Quy tắc đặt mã lỗi

| Prefix | Ý nghĩa |
|--------|----------|
| `ERR_4xx` | Lỗi từ phía client (bad request, unauthorized...) |
| `ERR_5xx` | Lỗi từ phía server (internal error, unavailable...) |
| `ERR_1xxx` | Lỗi nghiệp vụ (business logic) |
| `ERR_2xxx` | Lỗi database |
| `ERR_3xxx` | Lỗi external service |
| `ERR_4xxx` | Lỗi infrastructure (cache, queue...) |

---

## 5. ApiResponseBuilder - Builder Pattern

**File:** `src/main/java/com/petsplatform/shared/response/ApiResponseBuilder.java`

### Khi nào dùng Builder?

Dùng khi response có nhiều field tùy chỉnh hoặc khi cần build response phức tạp từ nhiều nguồn.

### Cách sử dụng

```java
// Cách 1: Tạo builder trống
ApiResponse<User> r1 = ApiResponse.<User>builder()
    .data(user)
    .message("User loaded successfully")
    .build();

// Cách 2: Tạo builder với data có sẵn
ApiResponse<User> r2 = ApiResponse.<User>builder(user)
    .message("User found")
    .build();

// Cách 3: Builder cho error response
ApiResponse<User> r3 = ApiResponse.<User>builder()
    .success(false)
    .message("Custom error message")
    .error(ApiError.VALIDATION_FAILED)
    .build();

// Cách 4: Builder cho error với message override
ApiResponse<User> r4 = ApiResponse.<User>builder()
    .success(false)
    .message("Email đã tồn tại trong hệ thống")
    .error(ApiError.CONFLICT, "Email already exists")
    .build();

// Cách 5: Static factory methods của Builder
ApiResponse<User> r5 = ApiResponseBuilder.<User>forData(user)
    .message("Loaded")
    .build();
```

### Builder methods chain

```
ApiResponse.<T>builder()
    ├── data(T)             → đặt payload
    ├── message(String)     → đặt message (mặc định "Success")
    ├── success(boolean)    → true=thành công, false=lỗi
    ├── error(ApiError)     → đặt mã lỗi (tự đặt success=false)
    ├── error(ApiError, String) → mã lỗi + override message
    ├── timestamp(Instant)  → đặt thời điểm (mặc định Instant.now())
    └── build()             → tạo ApiResponse
```

---

## 6. ResponseHelper - Tiện ích

**File:** `src/main/java/com/petsplatform/shared/response/ResponseHelper.java`

### Design philosophy

`ResponseHelper` cung cấp các **convenience methods** giúp viết code ở **service layer** ngắn gọn và tường minh hơn. Mỗi method trả về `ApiResponse<T>` sẵn sàng return về controller.

### Success shortcuts

```java
// Các shortcut cho success response
ResponseHelper.success()                           // ApiResponse.ok()
ResponseHelper.success("Thành công")               // ApiResponse.okMsg(...)
ResponseHelper.success(user)                       // ApiResponse.ok(user)
ResponseHelper.successMsgData("Loaded", user)      // ApiResponse.okMsgData(...)

// Collection
ResponseHelper.successCollection(List<User> users)

// Optional
ResponseHelper.successOptional(Optional<User> opt)
ResponseHelper.successOptional(Optional<User> opt, "Custom message")
```

### Error shortcuts

```java
// Generic error
ResponseHelper.error("Something went wrong")
ResponseHelper.error(ApiError.INTERNAL_ERROR)

// Error với HTTP status semantics
ResponseHelper.badRequest("Email is required")
ResponseHelper.notFound("User với id=99 không tồn tại")
ResponseHelper.unauthorized("Token has expired")
ResponseHelper.forbidden("Bạn không có quyền truy cập resource này")
ResponseHelper.conflict("Email đã được sử dụng")
ResponseHelper.serverError("Database connection failed")
```

### Null-safe factories (quan trọng)

```java
// Nếu data == null → tự động trả về NOT_FOUND
ResponseHelper.okOrNotFound(user, "User")
// Tương đương:
// if (user == null) return notFound("User not found");
// return success(user);

// Nếu đã tồn tại → conflict, ngược lại → success
ResponseHelper.createdOrConflict(newUser, exists, "User")
// Tương đương:
// if (exists) return conflict("User already exists");
// return success(newUser);

// Generic null check
ResponseHelper.of(value)                    // null → notFound("Resource not found")
ResponseHelper.of(value, "Custom message")   // null → notFound("Custom message")
```

### Sự khác biệt giữa ApiResponse và ResponseHelper

| Tiêu chí | ApiResponse | ResponseHelper |
|----------|-------------|----------------|
| Mức độ | Low-level, foundation | High-level, convenience |
| Nơi dùng | Trực tiếp khi cần kiểm soát chi tiết | Service layer thông thường |
| API | Static factories | Static shortcuts |
| Null-handling | Không tự động | Tự động (okOrNotFound, of) |

---

## 7. Pagination Response

### Tạo một PaginationResult class (đề xuất)

Tạo file mới `com.petsplatform.shared.response.PageResponse.java`:

```java
package com.petsplatform.shared.response;

import lombok.Getter;
import org.springframework.data.domain.Page;
import java.util.List;

@Getter
public final class PageResponse<T> {

    private final List<T> content;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean first;
    private final boolean last;

    private PageResponse(Page<T> page) {
        this.content = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.first = page.isFirst();
        this.last = page.isLast();
    }

    public static <T> ApiResponse<PageResponse<T>> from(Page<T> page) {
        return ApiResponse.ok(new PageResponse<>(page));
    }

    public static <T> ApiResponse<PageResponse<T>> from(Page<T> page, String message) {
        return ApiResponse.okMsgData(message, new PageResponse<>(page));
    }
}
```

### JSON Output

```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      { "id": 1, "name": "Buddy" },
      { "id": 2, "name": "Whiskers" }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 42,
    "totalPages": 5,
    "first": true,
    "last": false
  },
  "timestamp": "2026-06-08T08:52:00Z",
  "error": null
}
```

---

## 8. Exception Handler tích hợp

### Global Exception Handler

Tạo file `com.petsplatform.shared.exception.GlobalExceptionHandler.java`:

```java
package com.petsplatform.shared.exception;

import com.petsplatform.shared.response.ApiError;
import com.petsplatform.shared.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 404 Not Found
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.errorMsg(ex.getMessage(), ApiError.NOT_FOUND));
    }

    // 400 Bad Request
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.errorMsg(ex.getMessage(), ApiError.BAD_REQUEST));
    }

    // 422 Validation Error
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(err ->
            errors.put(err.getField(), err.getDefaultMessage())
        );
        ApiResponse<Map<String, String>> response = ApiResponse
            .<Map<String, String>>builder()
            .success(false)
            .message("Validation failed for " + errors.size() + " field(s)")
            .data(errors)
            .error(ApiError.VALIDATION_FAILED)
            .build();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // 409 Conflict
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(ResourceConflictException ex) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.errorMsg(ex.getMessage(), ApiError.CONFLICT));
    }

    // 500 Internal Error (catch-all)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.errorMsg("Internal server error", ApiError.INTERNAL_ERROR));
    }
}
```

### Custom Exception Classes

```java
// ResourceNotFoundException.java
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

// ResourceConflictException.java
public class ResourceConflictException extends RuntimeException {
    public ResourceConflictException(String message) {
        super(message);
    }
}
```

### Flow xử lý exception

```
Request
  │
  ▼
Controller ──► Service ──► Repository
  ▲              │
  │              ▼
  │         Exception thrown
  │              │
  └────── GlobalExceptionHandler
              │
              ▼
         ApiResponse.error(...)
              │
              ▼
         HTTP Response (4xx/5xx)
```

---

## 9. Ví dụ sử dụng

### 9.1. Service Layer - CRUD Operations

```java
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public ApiResponse<User> getUserById(Long id) {
        return ResponseHelper.okOrNotFound(
            userRepository.findById(id),
            "User"
        );
    }

    public ApiResponse<User> createUser(CreateUserDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            return ResponseHelper.conflict("Email already exists");
        }
        User saved = userRepository.save(mapper.toEntity(dto));
        return ResponseHelper.successMsgData("User created", saved);
    }

    public ApiResponse<User> updateUser(Long id, UpdateUserDto dto) {
        User existing = userRepository.findById(id);
        if (existing == null) {
            return ResponseHelper.notFound("User not found");
        }
        User updated = mapper.merge(existing, dto);
        return ResponseHelper.successMsgData("User updated", userRepository.save(updated));
    }

    public ApiResponse<Void> deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseHelper.notFound("User not found");
        }
        userRepository.deleteById(id);
        return ResponseHelper.success("User deleted");
    }
}
```

### 9.2. Controller Layer

```java
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ApiResponse<User> getUser(@PathVariable Long id) {
        return userService.getUserById(id);
    }

    @PostMapping
    public ApiResponse<User> createUser(@Valid @RequestBody CreateUserDto dto) {
        return userService.createUser(dto);
    }

    @PutMapping("/{id}")
    public ApiResponse<User> updateUser(@PathVariable Long id,
                                        @Valid @RequestBody UpdateUserDto dto) {
        return userService.updateUser(id, dto);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteUser(@PathVariable Long id) {
        return userService.deleteUser(id);
    }
}
```

### 9.3. Pagination Service

```java
public ApiResponse<PageResponse<User>> getUsers(Pageable pageable) {
    Page<User> page = userRepository.findAll(pageable);
    return PageResponse.from(page, "Users retrieved");
}
```

### 9.4. Batch/Delete Operations

```java
public ApiResponse<List<Long>> deleteUsers(List<Long> ids) {
    List<Long> deletedIds = ids.stream()
        .filter(userRepository::existsById)
        .peek(userRepository::deleteById)
        .toList();
    return ResponseHelper.successMsgData(
        "Deleted " + deletedIds.size() + " user(s)",
        deletedIds
    );
}
```

### 9.5. Validation Error với Map

```java
public ApiResponse<Map<String, String>> validateObject(ObjectDto dto) {
    Map<String, String> errors = new HashMap<>();
    if (dto.getName() == null) errors.put("name", "Name is required");
    if (dto.getEmail() == null) errors.put("email", "Email is required");
    if (!errors.isEmpty()) {
        return ApiResponse.<Map<String, String>>builder()
            .success(false)
            .message("Validation failed for " + errors.size() + " field(s)")
            .data(errors)
            .error(ApiError.VALIDATION_FAILED)
            .build();
    }
    return ResponseHelper.success(errors);
}
```

### 9.6. Async Operations

```java
@Async
public CompletableFuture<ApiResponse<Report>> generateReport(Long userId) {
    return CompletableFuture.supplyAsync(() ->
        ResponseHelper.okOrNotFound(reportService.generate(userId), "Report")
    );
}
```

---

## Checklist cho Frontend Integration

### TypeScript Interface (tham khảo)

```typescript
interface ApiError {
  code: string;
  message: string;
}

interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T | null;
  timestamp: string;
  error: ApiError | null;
}

interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}
```

### Frontend Axios Interceptor (tham khảo)

```typescript
// Interceptor để handle response chuẩn
axios.interceptors.response.use(
  (response) => response,
  (error) => {
    const apiResponse = error.response?.data;
    if (apiResponse?.success === false) {
      // Frontend biết chắc đây là lỗi từ server
      showToast(apiResponse.message);
      if (apiResponse.error?.code === 'ERR_401') {
        redirectToLogin();
      }
    }
    return Promise.reject(error);
  }
);
```

---

## Checklist trước khi merge

- [ ] Tất cả API trả về `ApiResponse<T>`
- [ ] Controller chỉ return response, không throw exception trực tiếp
- [ ] Service trả về `ApiResponse<T>`, không trả về entity thô
- [ ] Error response luôn có `error.code` và `error.message`
- [ ] GlobalExceptionHandler được đăng ký
- [ ] Frontend đã đồng bộ interface `ApiResponse<T>`
- [ ] HTTP status code phù hợp với error type (400, 404, 409, 422, 500...)

---

## File Index

| File | Package | Mục đích |
|------|---------|----------|
| `ApiResponse.java` | `shared.response` | Generic immutable response wrapper |
| `ApiResponseBuilder.java` | `shared.response` | Builder pattern cho complex response |
| `ApiError.java` | `shared.response` | Enum mã lỗi chuẩn |
| `ResponseHelper.java` | `shared.response` | Convenience helpers cho service layer |
| `PageResponse.java` | `shared.response` | (Đề xuất) Wrapper cho paginated response |
| `GlobalExceptionHandler.java` | `shared.exception` | (Đề xuất) Centralized exception handling |
