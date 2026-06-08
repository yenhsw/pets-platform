# Validation Foundation System

**File:** `080626.004.md`
**Module:** `com.petsplatform.shared.validation`
**Version:** 1.0.0

---

## Mục lục

1. [Tổng quan hệ thống](#1-tổng-quan-hệ-thống)
2. [Tại sao centralized validation quan trọng](#2-tại-sao-centralized-validation-quan-trọng)
3. [Best practice validate DTO](#3-best-practice-validate-dto)
4. [Kiến trúc validation system](#4-kiến-trúc-validation-system)
5. [ValidationMessages.properties — Custom messages](#5-validationmessagesproperties--custom-messages)
6. [BeanValidationConfig](#6-beanvalidationconfig)
7. [GlobalExceptionHandler integration](#7-globalexceptionhandler-integration)
8. [Built-in constraint annotations](#8-built-in-constraint-annotations)
9. [Custom constraint annotations](#9-custom-constraint-annotations)
10. [ValidationHelper — Service layer validation](#10-validationhelper--service-layer-validation)
11. [Validation flow hoàn chỉnh](#11-validation-flow-hoàn-chỉnh)
12. [Error response format](#12-error-response-format)
13. [Scalability](#13-scalability)
14. [File index](#14-file-index)

---

## 1. Tổng quan hệ thống

Hệ thống validation của Pets Platform gồm 3 lớp làm việc cùng nhau:

```
┌──────────────────────────────────────────────────────────────────┐
│                     Controller Layer                              │
│  @Validated @RestController                                        │
│      │                                                            │
│      ▼ (@Valid @RequestBody DTO)                                  │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │       Bean Validation (JSR-380 / Hibernate Validator)       │  │
│  │  @NotBlank, @Email, @Size, @Pattern, @StrongPassword...    │  │
│  └──────────────────────────┬────────────────────────────────────┘  │
│                            │ Validated OK                          │
│                            ▼                                        │
│                     Service Layer                                  │
│                 (business logic)                                   │
│                            │                                       │
│                  ValidationHelper                                  │
│           (manual cross-field checks)                              │
│                            │                                       │
│                            ▼                                       │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │              ValidationException (business)                   │  │
│  └──────────────────────────┬────────────────────────────────────┘  │
└─────────────────────────────┼──────────────────────────────────────┘
                              │
                              ▼ (@ExceptionHandler)
┌──────────────────────────────────────────────────────────────────┐
│                   GlobalExceptionHandler                            │
│                                                                      │
│  MethodArgumentNotValidException      → 422 fieldErrors map          │
│  ConstraintViolationException         → 422 fieldErrors map          │
│  HandlerMethodValidationException     → 422 fieldErrors map          │
│  ValidationException                  → 422 fieldErrors map          │
│                                                                      │
│                            ▼                                        │
│                   ApiResponse<Map<String,String>>                     │
│                  { success: false, data: { field: msg } }          │
└──────────────────────────────────────────────────────────────────┘
```

**Tổng cộng files tạo trong session này:**

```
src/main/resources/
└── ValidationMessages.properties          ← Custom validation messages

src/main/java/com/petsplatform/
├── config/
│   └── BeanValidationConfig.java        ← Validator factory config
└── shared/validation/
    ├── ValidationHelper.java            ← Service-layer validation helper
    └── constraints/
        ├── StrongPassword.java          ← Custom @StrongPassword
        ├── StrongPasswordValidator.java
        ├── PhoneNumber.java             ← Custom @PhoneNumber
        ├── PhoneNumberValidator.java
        ├── EnumValue.java               ← Custom @EnumValue
        └── EnumValueValidator.java
```

---

## 2. Tại sao Centralized Validation quan trọng

### 2.1 Vấn đề khi không có centralized validation

```
Module A viết:
  if (email == null) throw new RuntimeException("Email required");
  if (!email.contains("@")) throw new RuntimeException("Invalid email");

Module B viết:
  if (email == null) return "Email là bắt buộc";
  if (!email.matches(".*@.*")) return "Email không hợp lệ";

Module C viết:
  throw new IllegalArgumentException("email: must not be blank");

→ Frontend nhận 3 format error khác nhau
→ Không thể reuse validation logic
→ Khi requirement thay đổi phải sửa N chỗ
→ Inconsistent giữa các module
```

### 2.2 Giải pháp với centralized system

| Vấn đề | Giải pháp |
|---------|------------|
| Error format không đồng nhất | `ApiResponse<Map<String, String>>` — 1 format cho mọi lỗi |
| Validation logic lặp lại | `@NotBlank`, `@Email` — khai báo, không lặp code |
| Hardcode message | `ValidationMessages.properties` — 1 chỗ sửa, propagate toàn app |
| Custom validation khó reuse | `@EnumValue`, `@StrongPassword` — khai báo annotation |
| Service-layer validation rải rác | `ValidationHelper` — 1 interface nhất quán |
| Xử lý error phải viết lại | `GlobalExceptionHandler` — catch tất cả validation exceptions |

### 2.3 So sánh các approaches

| Approach | Pros | Cons |
|----------|------|------|
| Manual if-else in controller | Simple | Inconsistent, repeated, verbose |
| Manual if-else in service | Reuse được trong service | Vẫn rải rác, không centralized |
| Hibernate Validator + JSR-380 | Standard, declarative, powerful | Chỉ cho DTO/Request |
| Spring `Validator` interface | Pluggable | Verbose, requires manual wiring |
| Custom `ValidationHelper` (chúng ta) | Centralized, consistent, Spring-wired | Extra layer |

---

## 3. Best Practice Validate DTO

### 3.1 DTO Design Principles

```
Good DTO:
├── Group related fields (CreateUserDto vs UpdateUserDto vs UserResponseDto)
├── Separate input DTO from output DTO (Create vs Response)
├── Immutable fields (final + constructor, hoặc record)
└── Only expose fields that client needs

Bad DTO:
├── 1 DTO cho tất cả operations (create/update/view)
├── Entity references exposed as nested objects
├── Password trong response DTO
└── Too many fields (> 20 → nên tách)
```

### 3.2 Layered Validation Strategy

```
                    Controller                    Service                    Repository
                         │                          │                            │
  ┌─────────────────────┴───────────────────────┐  │                            │
  │  @Valid @RequestBody CreateUserDto           │  │                            │
  │  ──────────────────────────────────────────  │  │                            │
  │  @NotBlank name      ←── Basic format       │  │                            │
  │  @Email email        ←── Basic format       │  │                            │
  │  @Size(min=8) password ←── Basic format     │  │                            │
  │  @StrongPassword password ←── Business     │  │                            │
  │  @EnumValue Role     ←── Business enum      │  │                            │
  └─────────────────────────────────────────────┘  │                            │
                                                   │                            │
                                        ┌──────────┴──────────┐                │
                                        │  ValidationHelper  │                │
                                        │  ─────────────────  │                │
                                        │  requireTrue(...)  │ ←── Cross-field│
                                        │  requireNonNull()  │ ←── DB check  │
                                        │  validate(object)  │ ←── Cascade   │
                                        └─────────────────────┘                │
                                                                               │
                                                              userRepository.existsByEmail(dto.getEmail())
```

### 3.3 Checklist khi viết DTO validation

- [ ] `@NotNull` cho fields bắt buộc (không phải `@NotBlank` — khác nhau!)
- [ ] `@NotBlank` cho String bắt buộc
- [ ] `@Email` cho email format
- [ ] `@Size` cho String length
- [ ] `@Pattern` cho custom format (regex)
- [ ] `@StrongPassword` cho password fields
- [ ] `@PhoneNumber` cho phone number
- [ ] `@EnumValue(MyEnum.class)` cho enum fields
- [ ] `@Valid` cho nested DTOs
- [ ] `@DecimalMin`/`@DecimalMax` cho numeric ranges
- [ ] `@Past`/`@Future` cho date fields
- [ ] **KHÔNG** validate business rules trong DTO annotation (để service layer)

### 3.4 Common mistakes

```java
// ❌ SAI: Dùng @NotBlank cho numeric field
@NotBlank
private Integer age; // Integer không bao giờ là "blank" — là null hoặc số

// ✅ ĐÚNG: Dùng @NotNull cho numeric
@NotNull(message = "Age is required")
@Min(value = 0)
@Max(value = 150)
private Integer age;

// ❌ SAI: Dùng @NotBlank cho optional field
@NotBlank
private String nickname; // nickname là optional, không cần validate

// ✅ ĐÚNG: Không annotation cho optional
private String nickname; // Chỉ validate khi có giá trị
// Hoặc nếu cần validate format khi có:
// @Size(min = 2, max = 50)
private String nickname;

// ❌ SAI: Validate business rule trong DTO
@AssertTrue(message = "Start date must be before end date")
private boolean isValidDateRange() { ... } // Nên ở service layer

// ✅ ĐÚNG: Business rule ở service layer
public void createEvent(CreateEventDto dto) {
    validationHelper.requireTrue(
        dto.getStartDate().isBefore(dto.getEndDate()),
        "dateRange",
        "Start date must be before end date"
    );
}
```

---

## 4. Kiến trúc Validation System

```
src/main/
├── resources/
│   └── ValidationMessages.properties    ← Centralized messages (i18n-ready)
│
├── java/com/petsplatform/
│   ├── config/
│   │   └── BeanValidationConfig.java      ← Validator factory + MethodValidationPostProcessor
│   │
│   └── shared/validation/
│       ├── ValidationHelper.java           ← Programmatic validation in services
│       └── constraints/
│           ├── StrongPassword.java          ← @StrongPassword annotation
│           ├── StrongPasswordValidator.java
│           ├── PhoneNumber.java             ← @PhoneNumber annotation
│           ├── PhoneNumberValidator.java
│           ├── EnumValue.java               ← @EnumValue annotation
│           └── EnumValueValidator.java
```

### Dependencies với hệ thống khác

```
BeanValidationConfig
    └── LocalValidatorFactoryBean
            └── ValidationMessages.properties
                    └── GlobalExceptionHandler
                            └── ValidationException
                                    └── ApiResponse<Map<String,String>>
                                            └── ApiError.VALIDATION_FAILED
```

---

## 5. ValidationMessages.properties — Custom Messages

**File:** `src/main/resources/ValidationMessages.properties`

### 5.1 Message resolution order

```
1. {constraint}.{fully.qualified.Class.field}
2. {constraint}.{fully.qualified.Class}
3. {constraint}
4. Hibernate Validator default message
```

### 5.2 Message parameters

| Format | Meaning | Example |
|--------|---------|---------|
| `{min}` | Minimum value/size | `Độ dài phải từ {min} đến {max} ký tự` |
| `{max}` | Maximum value/size | `Độ dài phải từ {min} đến {max} ký tự` |
| `{value}` | Exact value | `Giá trị phải lớn hơn hoặc bằng {value}` |
| `{integer}` | Integer digits | `Tối đa {integer} chữ số nguyên` |
| `{fraction}` | Fraction digits | `{fraction} chữ số thập phân` |
| `{enumClass}` | Enum class name | `Giá trị phải là một trong các giá trị cho phép` |

### 5.3 Message override examples

```properties
# Override Hibernate default
jakarta.validation.constraints.Size.message = Độ dài phải từ {min} đến {max} ký tự

# Override cho field cụ thể (theo FQCN)
# com.petsplatform.modules.user.dto.CreateUserDto.password
com.petsplatform.modules.user.dto.CreateUserDto.password.Password.message = Mật khẩu của bạn quá yếu
```

---

## 6. BeanValidationConfig

**File:** `src/main/java/com/petsplatform/config/BeanValidationConfig.java`

### 6.1 Các beans được configure

| Bean | Mục đích |
|------|----------|
| `LocalValidatorFactoryBean` | Tích hợp `ValidationMessages.properties`, message interpolation |
| `MethodValidationPostProcessor` | Enable `@Validated` trên class/controller/service level |

### 6.2 Kích hoạt method-level validation

```java
// Trong controller class — BẮT BUỘC để HandlerMethodValidationException hoạt động
@Validated
@RestController
public class UserController {
    @GetMapping("/{id}")
    public ApiResponse<User> getById(
        @PathVariable @NotNull Long id) { ... }
}
```

---

## 7. GlobalExceptionHandler Integration

**File:** `src/main/java/com/petsplatform/shared/exception/GlobalExceptionHandler.java`

### 7.1 Validation handlers đã được cấu hình

| Exception | Nguồn | HTTP Status |
|-----------|-------|-------------|
| `MethodArgumentNotValidException` | `@Valid` + `@RequestBody` | 422 |
| `HandlerMethodValidationException` | `@Validated` + `@RequestParam`/`@PathVariable` | 422 |
| `ConstraintViolationException` | JSR-303 on method parameters | 422 |
| `ValidationException` | Business validation trong service | 422 |

### 7.2 Cơ chế mapping

```java
// GlobalExceptionHandler.java (lines 168-179)
private ResponseEntity<ApiResponse<Map<String, String>>> buildValidationResponse(
        Map<String, String> fieldErrors) {
    String message = "Validation failed for " + fieldErrors.size() + " field(s)";
    ApiResponse<Map<String, String>> response = ApiResponse
        .<Map<String, String>>builder()
        .success(false)
        .message(message)
        .data(fieldErrors)          // ← Field errors map
        .error(ApiError.VALIDATION_FAILED)  // ← ERR_422
        .build();
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
}
```

---

## 8. Built-in Constraint Annotations

### 8.1 Thumbnail reference

| Annotation | Khi nào dùng | Ví dụ |
|-----------|--------------|-------|
| `@NotNull` | Bắt buộc, mọi type | `@NotNull Long userId` |
| `@NotBlank` | String bắt buộc | `@NotBlank String name` |
| `@NotEmpty` | Collection/Array bắt buộc | `@NotEmpty List<String> tags` |
| `@Email` | Email format | `@Email String email` |
| `@Size(min, max)` | String/Collection length | `@Size(min=2, max=100) String name` |
| `@Length(min, max)` | Hibernate-specific (tương đương @Size) | `@Length(min=2, max=50)` |
| `@Min` / `@Max` | Numeric range | `@Min(0) @Max(150) Integer age` |
| `@DecimalMin` / `@DecimalMax` | BigDecimal comparison | `@DecimalMin("0.01") BigDecimal price` |
| `@Pattern(regexp)` | Regex match | `@Pattern(regexp="^[A-Z]{2}$") String code` |
| `@Past` / `@Future` | Date validation | `@Future LocalDateTime expiresAt` |
| `@Valid` | Nested object validation | `@Valid Address address` |
| `@AssertTrue` / `@AssertFalse` | Boolean condition | `@AssertTrue boolean agreed` |

### 8.2 Chi tiết các annotation quan trọng

```java
public class CreateUserDto {

    // @NotBlank — String bắt buộc, reject "", "   ", null
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 100)
    private String name;

    // @Email — email format, accept null
    @NotBlank
    @Email(message = "Invalid email format")
    private String email;

    // @Pattern — custom regex
    @Pattern(
        regexp = "^\\+?[0-9]{10,15}$",
        message = "Phone must be 10-15 digits"
    )
    private String phone;

    // @Size — String length / Collection size
    @Size(min = 8, max = 128, message = "Password must be 8-128 characters")
    private String password;

    // @Min/@Max — integer range
    @NotNull
    @Min(0)
    @Max(120)
    private Integer age;

    // @DecimalMin/@DecimalMax — BigDecimal comparison
    @NotNull
    @DecimalMin(value = "0.01", inclusive = true)
    private BigDecimal price;

    // @Past/@Future — date validation
    @Future(message = "Expiry date must be in the future")
    private LocalDateTime expiresAt;

    // @Valid — nested object validation
    @NotNull
    @Valid
    private AddressDto address;
}
```

### 8.3 @NotNull vs @NotBlank vs @NotEmpty

```
@NotNull   → value != null                          (dùng cho mọi type)
@NotBlank  → value != null && !value.isBlank()      (chỉ String)
@NotEmpty  → value != null && value.size() > 0      (Collection, Array, Map, String)

 Ví dụ:    null    ""     "   "    "a"
@NotNull:   ❌      ✅      ✅      ✅
@NotBlank:  ❌      ❌      ❌       ✅
@NotEmpty:  ❌      ❌       ❌      ✅
```

---

## 9. Custom Constraint Annotations

### 9.1 @StrongPassword

**File:** `src/main/java/com/petsplatform/shared/validation/constraints/StrongPassword.java`

```java
@StrongPassword
private String password;
// Regex: ^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=\[\]{}|;:',.<>?/~`]).{8,}$
// Yêu cầu: lowercase + uppercase + digit + special char + 8+ chars
```

**Valid passwords:**
- `MyP@ssw0rd!` ✅
- `Abc123!@#` ✅

**Invalid passwords:**
- `password123` ❌ (không có uppercase)
- `PASSWORD123!` ❌ (không có lowercase)
- `Password!` ❌ (dưới 8 ký tự)
- `Password1234` ❌ (không có special char)

### 9.2 @PhoneNumber

**File:** `src/main/java/com/petsplatform/shared/validation/constraints/PhoneNumber.java`

```java
@PhoneNumber
private String phone;
// Regex: ^\+?[0-9\s\-()]{10,20}$
// Accepts: +84 123 456 789, 0912345678, (123) 456-7890
```

### 9.3 @EnumValue

**File:** `src/main/java/com/petsplatform/shared/validation/constraints/EnumValue.java`

```java
// Enum
public enum UserRole { ADMIN, USER, MODERATOR }

// DTO
public class CreateUserDto {
    @EnumValue(UserRole.class)
    private String role;  // "ADMIN" ✅, "admin" ✅ (case-insensitive)
}
```

**Ưu điểm so với @Enum:*** (Hibernate-specific):
- Dùng trực tiếp với enum class — không cần liệt kê giá trị
- Thêm enum constant → validation tự cập nhật
- Standard JSR-380, không phụ thuộc Hibernate

### 9.4 Tạo custom constraint mới

```java
// 1. Tạo annotation
@Documented
@Constraint(validatedBy = MyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface MyConstraint {
    String message() default "{com.petsplatform.shared.validation.constraints.MyConstraint.message}";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int minLength() default 0;
}

// 2. Tạo validator
public class MyValidator implements ConstraintValidator<MyConstraint, String> {
    private int minLength;
    @Override
    public void initialize(MyConstraint annotation) {
        this.minLength = annotation.minLength();
    }
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value != null && value.length() >= minLength;
    }
}

// 3. Thêm message vào ValidationMessages.properties
com.petsplatform.shared.validation.constraints.MyConstraint.message = Giá trị quá ngắn
```

---

## 10. ValidationHelper — Service Layer Validation

**File:** `src/main/java/com/petsplatform/shared/validation/ValidationHelper.java`

### 10.1 Khi nào dùng ValidationHelper

| Scenario | Dùng annotation | Dùng ValidationHelper |
|----------|----------------|-----------------------|
| Single field format | `@Email`, `@Size` | Không cần |
| Required field | `@NotNull`, `@NotBlank` | Không cần |
| Nested DTO | `@Valid` | Không cần |
| Cross-field check | Không làm được | ✅ `requireTrue()` |
| Database-dependent check | Không làm được | ✅ `validateWithMessage()` |
| Complex business rule | Không làm được | ✅ `requireTrue()` |
| Service-to-service validation | Không làm được | ✅ `validate(object)` |

### 10.2 Các methods

```java
// Validate DTO với annotation, throw nếu fail
validationHelper.validate(dto);

// Validate với message tùy chỉnh
validationHelper.validateWithMessage(dto, "Dữ liệu người dùng không hợp lệ");

// Lấy errors map (không throw)
Map<String, String> errors = validationHelper.validateAndGetErrors(dto);
if (!errors.isEmpty()) throw new ValidationException(errors);

// Check boolean condition
validationHelper.requireTrue(
    startDate.isBefore(endDate),
    "dateRange",
    "Ngày bắt đầu phải trước ngày kết thúc"
);

// Check null
validationHelper.requireNonNull(userId, "userId");

// Check errors map
validationHelper.requireValid(fieldErrors);
```

### 10.3 Ví dụ trong Service

```java
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ValidationHelper validationHelper;

    public ApiResponse<Order> createOrder(CreateOrderDto dto) {
        // 1. DTO validation đã done ở controller (@Valid)

        // 2. Service-layer cross-field validation
        validationHelper.requireTrue(
            dto.getStartDate().isBefore(dto.getEndDate()),
            "dateRange",
            "Ngày bắt đầu phải trước ngày kết thúc"
        );

        validationHelper.requireTrue(
            dto.getQuantity() > 0 && dto.getQuantity() <= MAX_QUANTITY,
            "quantity",
            "Quantity must be between 1 and " + MAX_QUANTITY
        );

        // 3. Database-dependent validation
        if (orderRepository.existsByUserAndStatus(dto.getUserId(), OrderStatus.PENDING)) {
            throw new ValidationException("userId",
                "Bạn đã có một đơn hàng đang chờ xử lý");
        }

        // 4. Proceed with creation
        Order order = mapper.toEntity(dto);
        return ResponseHelper.successMsgData("Tạo đơn hàng thành công",
            orderRepository.save(order));
    }
}
```

---

## 11. Validation Flow hoàn chỉnh

### 11.1 Happy path — POST /api/v1/users

```
POST /api/v1/users
{
  "name": "John",
  "email": "invalid-email"
}

Step 1: Spring MVC binds DTO
Step 2: @Valid triggers Bean Validation
Step 3: @Email fails → MethodArgumentNotValidException thrown
Step 4: GlobalExceptionHandler.handleMethodArgumentNotValid() catches it
Step 5: fieldErrors = { "email": "Invalid email format" }
Step 6: buildValidationResponse(fieldErrors)
Step 7: ResponseEntity 422 + JSON
```

### 11.2 Validation error response

```json
{
  "success": false,
  "message": "Validation failed for 2 field(s)",
  "data": {
    "name": "Trường không được để trống",
    "email": "Email không hợp lệ",
    "password": "Mật khẩu phải từ 8 ký tự trở lên, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt",
    "role": "Giá trị phải là một trong các giá trị cho phép"
  },
  "timestamp": "2026-06-08T09:25:00Z",
  "error": {
    "code": "ERR_422",
    "message": "Validation failed"
  }
}
```

### 11.3 Validation OK — happy path

```json
POST /api/v1/users
{ "name": "John", "email": "john@example.com", "password": "MyP@ss1!" }

→ 201 Created
{
  "success": true,
  "message": "Success",
  "data": { "id": "...", "name": "John" },
  "timestamp": "...",
  "error": null
}
```

---

## 12. Error Response Format

### 12.1 Validation error (HTTP 422)

```json
{
  "success": false,
  "message": "Validation failed for 3 field(s)",
  "data": {
    "fieldName": "Error message"
  },
  "timestamp": "2026-06-08T09:25:00Z",
  "error": {
    "code": "ERR_422",
    "message": "Validation failed"
  }
}
```

### 12.2 Các loại error khác để so sánh

| Type | HTTP | success | data | error.code |
|------|------|---------|------|-----------|
| Validation | 422 | false | `{"field": "msg"}` | `ERR_422` |
| Not Found | 404 | false | null | `ERR_404` |
| Conflict | 409 | false | null | `ERR_409` |
| Bad Request | 400 | false | null | `ERR_400` |
| Internal Error | 500 | false | null | `ERR_500` |

### 12.3 Frontend handling

```typescript
// TypeScript interface
interface ValidationErrorResponse {
  success: false;
  message: string;
  data: Record<string, string>;  // field -> error message
  error: { code: string; message: string };
}

// Usage
const response = await api.post('/users', data);
if (!response.success && response.error.code === 'ERR_422') {
  // Show inline field errors
  Object.entries(response.data).forEach(([field, message]) => {
    form.setFieldError(field, message);
  });
}
```

---

## 13. Scalability

### 13.1 Thêm module mới

```
Module User có DTO:
  CreateUserDto
    @NotBlank name
    @Email email
    @StrongPassword password

Module Order có DTO:
  CreateOrderDto
    @NotNull userId
    @DecimalMin("0.01") amount

→ Validation infrastructure KHÔNG cần thay đổi
→ Chỉ cần dùng annotations trong DTO
→ GlobalExceptionHandler tự bắt mọi exception
```

### 13.2 i18n expansion

```properties
# Hiện tại: validation message tiếng Anh
jakarta.validation.constraints.NotBlank.message = Field cannot be blank

# Tương lai: thêm tiếng Việt
jakarta.validation.constraints.NotBlank.message = Trường không được để trống

# Hoặc per-field
com.petsplatform.modules.user.dto.CreateUserDto.name.NotBlank.message = Tên không được để trống
```

### 13.3 Performance

| Factor | Impact | Mitigation |
|--------|--------|------------|
| Constraint evaluation | O(n) per field | Không đáng kể |
| Regex in @Pattern | O(m) per validation | Compile regex pattern static |
| Custom validators | O(1) per call | Stateless validators |
| Message interpolation | String concat | Chỉ khi error xảy ra |

---

## 14. File Index

| File | Package | Lines | Mục đích |
|------|---------|-------|----------|
| `ValidationMessages.properties` | resources | 55 | Centralized validation messages |
| `BeanValidationConfig.java` | `config` | 49 | Validator factory + method validation |
| `ValidationHelper.java` | `shared.validation` | 117 | Programmatic validation in services |
| `StrongPassword.java` | `shared.validation.constraints` | 42 | Custom @StrongPassword annotation |
| `StrongPasswordValidator.java` | `shared.validation.constraints` | 20 | Validator for StrongPassword |
| `PhoneNumber.java` | `shared.validation.constraints` | 42 | Custom @PhoneNumber annotation |
| `PhoneNumberValidator.java` | `shared.validation.constraints` | 18 | Validator for PhoneNumber |
| `EnumValue.java` | `shared.validation.constraints` | 48 | Custom @EnumValue annotation |
| `EnumValueValidator.java` | `shared.validation.constraints` | 26 | Validator for EnumValue |
| `GlobalExceptionHandler.java` | `shared.exception` | 180 | Validation exception handlers |
| `ValidationException.java` | `shared.exception` | 34 | Business validation exception |

---

## Checklist trước khi merge

- [ ] DTO đã có `@Valid` cho nested objects
- [ ] `@NotNull` cho required fields, không dùng `@NotBlank` cho non-String
- [ ] `@NotBlank` cho String required, không dùng `@NotNull`
- [ ] `@StrongPassword` hoặc `@Size(min=8)` cho password fields
- [ ] `@EnumValue(MyEnum.class)` cho String fields mapped từ enum
- [ ] Custom validators đã register trong `ValidationMessages.properties`
- [ ] Business validation dùng `ValidationHelper` ở service layer
- [ ] Cross-field validation dùng `requireTrue()` trong service
- [ ] GlobalExceptionHandler đã handle đủ 4 loại validation exceptions
- [ ] Frontend đã đồng bộ error response format
