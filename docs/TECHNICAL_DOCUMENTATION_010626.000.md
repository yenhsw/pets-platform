# TÀI LIỆU KỸ THUẬT - pets-platform
## Ngày: 01/06/2026
## Phiên bản: 1.0

---

# MỤC LỤC

1. [Tổng quan](#1-tổng-quan)
2. [API Response Structure](#2-api-response-structure)
3. [Exception Handling](#3-exception-handling)
4. [JPA Base Entity](#4-jpa-base-entity)
5. [Cách sử dụng chi tiết](#5-cách-sử-dụng-chi-tiết)
6. [Best Practices](#6-best-practices)

---

# 1. TỔNG QUAN

## 1.1 Mục tiêu

Thiết lập nền tảng foundation cho pets-platform với:

- **API Response chuẩn hóa** - Đảm bảo frontend luôn nhận response nhất quán
- **Exception Handling tập trung** - Xử lý lỗi nhất quán, dễ debug
- **JPA Base Entity** - Giảm boilerplate, audit trail tự động

## 1.2 Cấu trúc Package đã tạo

```
src/main/java/com/petsplatform/
├── common/
│   └── enums/
│       └── ErrorCode.java              ← Mã lỗi chuẩn hóa
│
├── shared/
│   ├── base/
│   │   ├── BaseEntity.java            ← Entity cơ sở
│   │   ├── SoftDeletableEntity.java   ← Entity có soft delete
│   │   └── AuditingConfig.java        ← Cấu hình auditing
│   │
│   ├── exception/
│   │   ├── BusinessException.java     ← Exception cơ sở
│   │   ├── ResourceNotFoundException.java
│   │   ├── ResourceAlreadyExistsException.java
│   │   ├── ValidationException.java
│   │   ├── ConflictException.java
│   │   └── GlobalExceptionHandler.java ← Xử lý exception tập trung
│   │
│   └── response/
│       ├── ApiResponse.java           ← Generic response wrapper
│       ├── ApiError.java             ← Error wrapper
│       ├── ErrorResponse.java         ← Error response structure
│       └── PageResponse.java         ← Pagination response
```

---

# 2. API RESPONSE STRUCTURE

## 2.1 Giới thiệu

Tất cả API responses được wrap trong `ApiResponse<T>` để đảm bảo **consistency**.

## 2.2 ApiResponse<T>

**Vị trí:** `src/main/java/com/petsplatform/shared/response/ApiResponse.java`

### Cấu trúc JSON

```json
{
  "success": true,
  "message": "Success",
  "data": { ... },
  "timestamp": "2026-06-01T15:30:00Z"
}
```

### Các methods tạo response

| Method | Mô tả | Ví dụ |
|--------|-------|-------|
| `success(data)` | Response thành công với data | `ApiResponse.success(pet)` |
| `success(message, data)` | Success với message tùy chỉnh | `ApiResponse.success("Created", pet)` |
| `success(message)` | Success chỉ có message (không data) | `ApiResponse.success("Deleted")` |
| `error(message)` | Response lỗi | `ApiResponse.error("Not found")` |
| `error(apiError)` | Lỗi với ApiError | `ApiResponse.error(ApiError.of(...))` |

### Ví dụ sử dụng

```java
// 1. Trả về data
return ApiResponse.success(pet);

// 2. Trả về với message tùy chỉnh
return ApiResponse.success("Pet created successfully", pet);

// 3. Trả về chỉ message (VD: delete)
return ApiResponse.success("Pet deleted successfully");

// 4. Trả về lỗi
return ApiResponse.error("Pet not found");
```

## 2.3 ApiError

**Vị trí:** `src/main/java/com/petsplatform/shared/response/ApiError.java`

Dùng cho error response với code chi tiết:

```java
ApiError error = ApiError.of("PET_001", "Pet not found", "ID: 123");
return ApiResponse.error(error);
```

### Output JSON

```json
{
  "success": false,
  "message": "Pet not found",
  "data": null,
  "timestamp": "2026-06-01T15:30:00Z"
}
```

## 2.4 PageResponse<T>

**Vị trí:** `src/main/java/com/petsplatform/shared/response/PageResponse.java`

Dùng cho API có phân trang:

### Cấu trúc JSON

```json
{
  "success": true,
  "message": "Success",
  "items": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "hasNext": true,
  "hasPrevious": false,
  "timestamp": "2026-06-01T15:30:00Z"
}
```

### Cách sử dụng

```java
public PageResponse<Pet> getPets(int page, int size, List<Pet> items, long total) {
    return PageResponse.of(items, page, size, total);
}
```

---

# 3. EXCEPTION HANDLING

## 3.1 Tại sao cần Exception Handling tập trung?

### Vấn đề khi không có centralized handler

```
Controller A → try/catch → { status: 500, msg: "Error" }
Controller B → try/catch → { error: true, message: "Failed" }
Controller C → uncaught → 500 Internal Server Error
```

→ Frontend phải xử lý N formats khác nhau ❌

### Giải pháp: GlobalExceptionHandler

```
Controller A → throw ResourceNotFoundException
Controller B → throw ValidationException
Controller C → throw BusinessException
                ↓
         GlobalExceptionHandler
                ↓
      { success: false, code: "...", message: "..." }
```

→ Frontend chỉ cần 1 interceptor cho tất cả ❌

## 3.2 ErrorCode Enum

**Vị trí:** `src/main/java/com/petsplatform/common/enums/ErrorCode.java`

Mã lỗi chuẩn hóa theo domain:

| Mã | Domain | Ví dụ |
|----|--------|-------|
| SYS_xxxx | System | SYS_0001: Unknown error |
| RES_2xxx | Resource | RES_2001: Not found |
| USR_3xxx | User | USR_3001: User not found |
| PET_4xxx | Pet | PET_4001: Pet not found |
| SHL_5xxx | Shelter | SHL_5001: Shelter not found |
| ADP_6xxx | Adoption | ADP_6001: Adoption not found |
| VAL_9xxx | Validation | VAL_9001: Field required |

### Cách thêm ErrorCode mới

```java
// Thêm vào ErrorCode.java
VACCINE_NOT_FOUND("VAC_7001", "Vaccine not found"),
VACCINE_EXPIRED("VAC_7002", "Vaccine has expired"),
```

## 3.3 Các Exception Classes

| Class | HTTP Status | Mục đích |
|-------|-------------|----------|
| `BusinessException` | Map theo ErrorCode | Base exception |
| `ResourceNotFoundException` | 404 | Resource không tìm thấy |
| `ResourceAlreadyExistsException` | 409 | Resource đã tồn tại |
| `ValidationException` | 400 | Validation thất bại |
| `ConflictException` | 409 | Xung đột dữ liệu |

### Cách sử dụng

```java
// 1. Không tìm thấy
throw new ResourceNotFoundException("Pet", "id", 123);
// → "Pet not found with id: '123'"

// 2. Đã tồn tại
throw new ResourceAlreadyExistsException("User", "email", "test@test.com");
// → "User already exists with email: 'test@test.com'"

// 3. Validation
throw new ValidationException("Validation failed");
```

## 3.4 GlobalExceptionHandler

**Vị trí:** `src/main/java/com/petsplatform/shared/exception/GlobalExceptionHandler.java`

Xử lý tất cả exceptions và trả về ErrorResponse chuẩn.

### ErrorResponse Format

```json
{
  "success": false,
  "code": "PET_4001",
  "message": "Pet not found with id: '123'",
  "details": null,
  "fieldErrors": null,
  "timestamp": "2026-06-01T15:30:00Z",
  "path": "/api/pets/123"
}
```

### Validation Error Format

```json
{
  "success": false,
  "code": "SYS_0002",
  "message": "Validation failed",
  "fieldErrors": {
    "email": "must be a valid email address",
    "name": "must not be blank"
  },
  "timestamp": "2026-06-01T15:30:00Z",
  "path": "/api/pets"
}
```

---

# 4. JPA BASE ENTITY

## 4.1 BaseEntity

**Vị trí:** `src/main/java/com/petsplatform/shared/base/BaseEntity.java`

### Các trường có sẵn

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `id` | String (UUID) | Khóa chính tự động |
| `createdAt` | Instant | Thời điểm tạo |
| `updatedAt` | Instant | Thời điểm cập nhật cuối |
| `createdBy` | String | Người tạo |
| `updatedBy` | String | Người cập nhật cuối |
| `deleted` | boolean | Cờ soft delete |

### Database Schema (tự động)

```sql
CREATE TABLE pets (
    id              VARCHAR(36) PRIMARY KEY,  -- UUID
    created_at      TIMESTAMP NOT NULL,
    updated_at      TIMESTAMP,
    created_by      VARCHAR(100),
    updated_by      VARCHAR(100),
    is_deleted      BOOLEAN NOT NULL DEFAULT FALSE,
    -- Các trường khác của entity
);
```

## 4.2 SoftDeletableEntity

**Vị trí:** `src/main/java/com/petsplatform/shared/base/SoftDeletableEntity.java`

Kế thừa BaseEntity + thêm:

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `deletedAt` | Instant | Thời điểm xóa |
| `deletedBy` | String | Người thực hiện xóa |

## 4.3 AuditingConfig

**Vị trí:** `src/main/java/com/petsplatform/shared/base/AuditingConfig.java`

Bật JPA Auditing để tự động điền:

- `createdAt` - Tự động khi tạo
- `updatedAt` - Tự động khi cập nhật
- `createdBy` - Từ `auditorProvider` (hiện trả về "system")
- `updatedBy` - Từ `auditorProvider`

---

# 5. CÁCH SỬ DỤNG CHI TIẾT

## 5.1 Tạo Entity mới

### Bước 1: Tạo class kế thừa

```java
package com.petsplatform.modules.pet.domain.entity;

import com.petsplatform.shared.base.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "pets")
@Getter
@Setter
public class Pet extends SoftDeletableEntity {

    @Column(nullable = false)
    private String name;

    @Column(length = 50)
    private String species;

    @Column
    private Integer age;

    @Column
    private String breed;
}
```

### Bước 2: Không cần khai báo gì thêm

- `id` đã có (UUID)
- `createdAt`, `updatedAt` tự động
- `deleted` flag đã có
- Soft delete đã có

## 5.2 Sử dụng trong Service

```java
@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;

    public Pet createPet(CreatePetRequest request) {
        // Kiểm tra trùng lặp
        if (petRepository.existsByName(request.getName())) {
            throw new ResourceAlreadyExistsException("Pet", "name", request.getName());
        }

        Pet pet = new Pet();
        pet.setName(request.getName());
        pet.setSpecies(request.getSpecies());

        return petRepository.save(pet);
    }

    public Pet findById(String id) {
        return petRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Pet", "id", id));
    }

    public void deletePet(String id) {
        Pet pet = findById(id);
        pet.setDeleted(true);  // Soft delete
        petRepository.save(pet);
    }
}
```

## 5.3 Sử dụng trong Controller

```java
@RestController
@RequestMapping("/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    @GetMapping("/{id}")
    public ApiResponse<PetResponse> getPet(@PathVariable String id) {
        Pet pet = petService.findById(id);
        PetResponse response = toResponse(pet);
        return ApiResponse.success(response);
    }

    @PostMapping
    public ApiResponse<PetResponse> createPet(@Valid @RequestBody CreatePetRequest request) {
        Pet pet = petService.createPet(request);
        PetResponse response = toResponse(pet);
        return ApiResponse.success("Pet created successfully", response);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deletePet(@PathVariable String id) {
        petService.deletePet(id);
        return ApiResponse.success("Pet deleted successfully");
    }
}
```

## 5.4 Xử lý Validation Errors

```java
// GlobalExceptionHandler tự động bắt @Valid errors
@PostMapping
public ApiResponse<PetResponse> createPet(@Valid @RequestBody CreatePetRequest request) {
    // Nếu request không hợp lệ, GlobalExceptionHandler xử lý
    // Trả về ErrorResponse với fieldErrors
}

// ErrorResponse nhận được:
{
  "success": false,
  "code": "SYS_0002",
  "message": "Validation failed",
  "fieldErrors": {
    "name": "must not be blank",
    "email": "must be a valid email address"
  }
}
```

## 5.5 Phân trang

```java
@GetMapping
public ApiResponse<PageResponse<PetResponse>> getPets(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {

    Page<Pet> petPage = petRepository.findAll(PageRequest.of(page, size));

    List<PetResponse> items = petPage.getContent().stream()
        .map(this::toResponse)
        .toList();

    PageResponse<PetResponse> response = PageResponse.of(
        items,
        page,
        size,
        petPage.getTotalElements()
    );

    return ApiResponse.success(response);
}
```

---

# 6. BEST PRACTICES

## 6.1 Response Wrapping

**NÊN:**
```java
return ApiResponse.success(pet);
return ApiResponse.success("Created", pet);
```

**KHÔNG NÊN:**
```java
return pet;  // Trả thẳng entity
return ResponseEntity.ok(pet);  // Dùng ResponseEntity
```

## 6.2 Exception Throwing

**NÊN:**
```java
throw new ResourceNotFoundException("Pet", "id", id);
throw new ResourceAlreadyExistsException("Pet", "name", name);
throw new ValidationException("Invalid input");
```

**KHÔNG NÊN:**
```java
throw new RuntimeException("Pet not found");  // Không có error code
return null;  // Không rõ ràng
```

## 6.3 Entity Design

**NÊN:**
```java
@Entity
public class Pet extends SoftDeletableEntity {
    // Chỉ khai báo business fields
}
```

**KHÔNG NÊN:**
```java
@Entity
public class Pet {
    @Id
    private String id;  // Lặp lại trong mọi entity
    private Instant createdAt;
    private Instant updatedAt;
    // ...
}
```

## 6.4 Thêm ErrorCode mới

Khi cần thêm error code cho domain mới:

```java
// 1. Thêm vào ErrorCode.java
VACCINE_NOT_FOUND("VAC_7001", "Vaccine not found"),

// 2. Thêm vào GlobalExceptionHandler nếu cần custom handler
@ExceptionHandler(VaccineException.class)
public ResponseEntity<ErrorResponse> handleVaccine(...) { }

// 3. Sử dụng
throw new BusinessException(ErrorCode.VACCINE_NOT_FOUND);
```

## 6.5 Frontend Integration

```typescript
// Tạo một interceptor cho tất cả responses
axios.interceptors.response.use(
  (response) => response.data,
  (error) => {
    const { code, message, fieldErrors } = error.response.data;

    if (fieldErrors) {
      // Xử lý validation errors
      Object.entries(fieldErrors).forEach(([field, msg]) => {
        showFieldError(field, msg);
      });
    } else {
      // Xử lý generic errors
      showToast(message);
    }

    return Promise.reject(error);
  }
);

// Sử dụng
const { data } = await api.getPet(id);
// data là Pet object, không cần extract từ response
```

---

# CHECKLIST

- [x] ApiResponse generic wrapper
- [x] ErrorCode enum chuẩn hóa
- [x] BusinessException hierarchy
- [x] GlobalExceptionHandler
- [x] BaseEntity với auditing
- [x] SoftDeletableEntity
- [x] AuditingConfig

---

**Ngày tạo:** 01/06/2026
**Người tạo:** Senior Java Enterprise Architect
**Phiên bản:** 1.0
