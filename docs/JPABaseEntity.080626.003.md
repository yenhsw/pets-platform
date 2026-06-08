# JPA Base Entity & Auditing Foundation

**File:** `080626.003.md`
**Module:** `com.petsplatform.shared.base`
**Version:** 1.0.0

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Lý do Auditing quan trọng](#2-lý-do-auditing-quan-trọng)
3. [Lý do BaseEntity giúp maintain dễ hơn](#3-lý-do-baseentity-giúp-maintain-dễ-hơn)
4. [Cấu trúc files](#4-cấu-trúc-files)
5. [BaseEntity chi tiết](#5-baseentity-chi-tiết)
6. [AuditorContext chi tiết](#6-auditorcontext-chi-tiết)
7. [JpaAuditingConfig chi tiết](#7-jpaauditingconfig-chi-tiết)
8. [Database schema](#8-database-schema)
9. [Cách sử dụng](#9-cách-sử-dụng)
10. [Cách populate AuditorContext](#10-cách-populate-auditorcontext)
11. [Soft delete pattern](#11-soft-delete-pattern)
12. [Best practice Java 21 + Lombok](#12-best-practice-java-21--lombok)
13. [Scalability & Multi-module](#13-scalability--multi-module)
14. [File index](#14-file-index)

---

## 1. Tổng quan kiến trúc

```
┌─────────────────────────────────────────────────────────────┐
│                    PetsPlatformApplication                  │
│                         @SpringBootApplication              │
│                              │                              │
│                              ▼                              │
│               ┌───────────────────────────────┐              │
│               │     JpaAuditingConfig         │              │
│               │     @EnableJpaAuditing        │              │
│               │     auditorProvider()         │              │
│               └───────────────┬───────────────┘              │
│                               │                              │
│                               │ AuditorAware<String>         │
│                               │ reads from                   │
│                               ▼                              │
│               ┌───────────────────────────────┐              │
│               │     AuditorContext            │              │
│               │     ThreadLocal<String>       │              │
│               └───────────────────────────────┘              │
└─────────────────────────────────────────────────────────────┘
                               │
                    setAuditor(username)
                               │
┌─────────────────────────────────────────────────────────────┐
│                      BaseEntity                             │
│  @MappedSuperclass                                         │
│  @EntityListeners(AuditingEntityListener.class)             │
│  ────────────────────────────────────────────────────────  │
│  id          : UUID         @Id @GeneratedValue(UUID)       │
│  createdAt   : Instant      @CreatedDate                    │
│  updatedAt   : Instant      @LastModifiedDate               │
│  createdBy   : String       @CreatedBy                      │
│  updatedBy   : String       @LastModifiedBy                 │
│  deleted     : Boolean      soft-delete flag                 │
└─────────────────────────────────────────────────────────────┘
                               │
                    extends
                               │
        ┌──────────────┬────────────────┬──────────────┐
        │              │                │              │
    ┌───▼────┐   ┌────▼───┐    ┌─────▼────┐  ┌─────▼─────┐
    │  User   │   │ Product │    │   Order   │  │  Payment  │
    └─────────┘   └────────┘    └───────────┘  └───────────┘
```

---

## 2. Lý do Auditing quan trọng

### 2.1 Không có auditing — vấn đề thực tế

| Vấn đề | Hậu quả |
|---------|---------|
| Không biết ai tạo record | Khi có bug, không truy vết được người tạo |
| Không biết khi nào update | Violation audit log, không prove compliance |
| Hard delete mất dữ liệu | Không recovery được khi user yêu cầu |
| Không có audit trail | Không đáp ứng GDPR, SOC2, ISO 27001 |
| Bug không reproduce được | Không biết trạng thái trước đó là gì |

### 2.2 Có auditing — lợi ích

| Lợi ích | Chi tiết |
|---------|----------|
| **Audit trail đầy đủ** | Ai tạo, khi nào, ai sửa, khi nào |
| **Bug investigation** | So sánh `createdAt`, `updatedAt` để xác định thời điểm thay đổi |
| **Compliance** | GDPR Article 5(1)(c): "adequate" data, Article 25: data protection by design |
| **Recovery** | Soft delete cho phép recovery dữ liệu |
| **Business intelligence** | Biết thời điểm user/tài khoản active |
| **Security forensics** | Biết ai đã sửa đổi dữ liệu khi có incident |

### 2.3 JPA Auditing vs Manual Approach

| Tiêu chí | Manual (trigger/procedure) | JPA Auditing |
|----------|--------------------------|-------------|
| Code consistency | Lặp lại ở mỗi entity | 1 lần trong BaseEntity |
| Business logic integration | Tách rời DB | Tích hợp Java layer |
| Testing | Cần DB test | Unit test được |
| Migration | Database migration phức tạp | Không cần migration |
| Flexibility | Cố định | Có thể customize |
| Framework coupling | Tightly coupled DB | Loose coupled |
| Performance | Native trigger execution | JVM overhead nhỏ |

---

## 3. Lý do BaseEntity giúp maintain dễ hơn

### 3.1 Không có BaseEntity

```java
// User.java — lặp lại 6 field + annotations cho mỗi entity
@Entity
public class User {
    @Id @GeneratedValue UUID id;
    Instant createdAt;
    Instant updatedAt;
    String createdBy;
    String updatedBy;
    Boolean deleted;

    // Getters/setters lặp lại 100 dòng
}

// Product.java — lặp lại Y CHÁNH XÁC
@Entity
public class Product {
    @Id @GeneratedValue UUID id;        // trùng
    Instant createdAt;                   // trùng
    Instant updatedAt;                   // trùng
    String createdBy;                    // trùng
    String updatedBy;                    // trùng
    Boolean deleted;                    // trùng
    // + Product-specific fields
}
```

**Vấn đề:**
- Copy-paste 10+ field + annotations mỗi entity → dễ miss
- Thêm 1 field mới phải sửa N files
- Không consistent — developer A quên `updatedBy`, developer B không
- Mỗi getter/setter lặp lại hàng chục dòng

### 3.2 Có BaseEntity

```java
// User.java — chỉ 1 dòng kế thừa
@Entity
public class User extends BaseEntity {
    private String name;
    private String email;
    // Chỉ User-specific fields
}

// Product.java — chỉ 1 dòng kế thừa
@Entity
public class Product extends BaseEntity {
    private String name;
    private BigDecimal price;
    // Chỉ Product-specific fields
}
```

**Lợi ích:**
- 1 lần định nghĩa → reuse mãi mãi
- Thêm field → sửa 1 file (`BaseEntity`)
- Tất cả entity TỰ ĐỘNG có đầy đủ auditing
- IDE autocomplete cho mọi entity

### 3.3 Thêm field mới — so sánh effort

| Thêm field `version` | Không BaseEntity | Có BaseEntity |
|-----------------------|------------------|---------------|
| Files cần sửa | N entity files + N test files | 1 file (`BaseEntity`) |
| Code thay đổi | 6 lines × N files | 1 place |
| Risk inconsistency | Cao — có thể miss file | Thấp — 1 chỗ |
| Time estimate | 30 phút × N files | 5 phút |

---

## 4. Cấu trúc files

```
src/main/java/com/petsplatform/
├── config/
│   └── JpaAuditingConfig.java        ← @EnableJpaAuditing + AuditorAware bean
│
└── shared/base/
    ├── BaseEntity.java               ← @MappedSuperclass + auditing fields
    └── AuditorContext.java           ← ThreadLocal auditor holder
```

### File responsibilities

| File | Mục đích | Dependency |
|------|----------|------------|
| `BaseEntity.java` | Mapping columns, JPA callbacks, soft delete | Không phụ thuộc gì |
| `AuditorContext.java` | Thread-safe auditor storage | Không phụ thuộc gì |
| `JpaAuditingConfig.java` | Enable auditing, wire AuditorAware | Phụ thuộc AuditorContext |
| `PetsPlatformApplication.java` | Spring Boot entry | Không cần @EnableJpaAuditing vì đã có trong config |

---

## 5. BaseEntity chi tiết

### 5.1 Fields overview

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;                          // UUID vì distributed-safe

    @CreatedDate
    @Column(name = "created_at", updatable = false, nullable = false)
    private Instant createdAt;               // Auto-set on INSERT

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;                // Auto-set on INSERT + UPDATE

    @CreatedBy
    @Column(name = "created_by", updatable = false, nullable = false, length = 100)
    private String createdBy;                // Set from AuditorContext

    @LastModifiedBy
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;                 // Set from AuditorContext

    @Setter
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;          // Soft delete flag
}
```

### 5.2 Design decisions

| Quyết định | Lý do |
|------------|-------|
| `UUID` thay vì `Long` | Distributed system safe — không conflict khi merge data giữa các service |
| `Instant` thay vì `LocalDateTime` | UTC, timezone-agnostic, Instant.ofEpochMilli() for comparison |
| `Boolean deleted` thay vì `boolean deleted` | Null-safe, `Boolean.TRUE.equals(deleted)` tránh NPE |
| `@Getter` (Lombok) | Không cần setter cho mọi field, chỉ `deleted` cần setter |
| `@Setter` riêng cho `deleted` | Chỉ `markDeleted()` được gọi trực tiếp |
| `abstract` class | Không cho phép instantiate trực tiếp, bắt buộc kế thừa |

### 5.3 Soft delete methods

```java
public boolean isDeleted() {
    return Boolean.TRUE.equals(deleted);
}

public void markDeleted() {
    this.deleted = true;
}
```

- `isDeleted()` — null-safe, trả về `false` nếu `deleted = null`
- `markDeleted()` — không gọi `em.remove()`, entity ở lại DB nhưng filtered

### 5.4 Annotations giải thích

```java
@MappedSuperclass
// → Không phải Entity, không có bảng riêng
// → Các field được KẾ THỪA vào mọi entity con như thể được định nghĩa ngay trong entity đó
// → Hibernate generate CREATE TABLE với các column này cho mỗi entity con

@EntityListeners(AuditingEntityListener.class)
// → Đăng ký JPA callback listener
// → Khi entity INSERT → gọi @CreatedDate, @CreatedBy
// → Khi entity UPDATE → gọi @LastModifiedDate, @LastModifiedBy
```

---

## 6. AuditorContext chi tiết

### 6.1 Implementation

```java
public final class AuditorContext {

    private static final ThreadLocal<String> AUDITOR = new ThreadLocal<>();

    public static void setAuditor(@Nullable String username) { ... }
    @Nullable public static String getAuditor() { ... }
    public static void clear() { ... }
}
```

### 6.2 Tại sao dùng ThreadLocal?

| Phương án | Ưu điểm | Nhược điểm |
|-----------|---------|-----------|
| `SecurityContextHolder` | Tích hợp Spring Security sẵn | Tightly coupled, không dùng được khi không có Security |
| `Principal` injection | Spring managed | Chỉ hoạt động trong request context |
| `ThreadLocal` | Decoupled, flexible | Cần `clear()` để tránh leak |
| `InheritableThreadLocal` | Truyền sang thread con | Leak risk cao hơn |

**Chọn `ThreadLocal`** vì:
- **Decoupled** — không phụ thuộc Spring Security, dùng được trong batch jobs
- **Explicit** — developer kiểm soát khi nào set/clear
- **Lightweight** — không overhead như InheritableThreadLocal

### 6.3 Tại sao dùng final class?

```java
public final class AuditorContext {
    private AuditorContext() {}
}
```

- **Utility class** — không có instance, chỉ có static methods
- **`final`** — không cho extend, tránh override không mong muốn
- **`private constructor`** — không thể tạo instance từ bên ngoài

---

## 7. JpaAuditingConfig chi tiết

### 7.1 Configuration

```java
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<String> auditorProvider() {
        return () -> Optional.ofNullable(AuditorContext.getAuditor());
    }
}
```

### 7.2 Tại sao dùng `Optional.ofNullable()`?

```java
// ❌ Khi không có auditor (batch job, SYSTEM process)
// → Trả về null
// → JPA ghi null vào cột created_by/updated_by
// → Database column NOT NULL → constraint violation!
auditorProvider() { return Optional.of(AuditorContext.getAuditor()); }

// ✅ Khi không có auditor
// → Trả về Optional.empty()
// → JPA bỏ qua không set field
// → Entity giữ giá trị mặc định (null hoặc database default)
auditorProvider() { return Optional.ofNullable(AuditorContext.getAuditor()); }
```

### 7.3 @EnableJpaAuditing placement

```java
// ❌ Đặt trên PetsPlatformApplication
@SpringBootApplication
@EnableJpaAuditing  // ← Hoạt động nhưng không clean
public class PetsPlatformApplication { ... }

// ✅ Đặt trên JpaAuditingConfig
@Configuration
@EnableJpaAuditing  // ← Tách biệt, dễ test, dễ disable
public class JpaAuditingConfig { ... }
```

**Lý do:**
- Separation of concerns — config riêng cho từng concern
- Testable — có thể disable auditing khi cần test
- Modular — có thể tách thành library riêng

---

## 8. Database schema

### 8.1 Generated DDL cho mỗi entity

```sql
CREATE TABLE users (
    id            UUID         NOT NULL PRIMARY KEY DEFAULT gen_random_uuid(),
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    created_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    updated_by    VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Entity-specific columns
    name          VARCHAR(255),
    email         VARCHAR(255),
    CONSTRAINT users_pkey PRIMARY KEY (id)
);

-- Tương tự cho products, orders, v.v. — mỗi bảng có cùng 6 cột auditing
CREATE TABLE products (
    id            UUID         NOT NULL PRIMARY KEY,
    created_at    TIMESTAMPTZ  NOT NULL,
    updated_at    TIMESTAMPTZ  NOT NULL,
    created_by    VARCHAR(100) NOT NULL,
    updated_by    VARCHAR(100) NOT NULL,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Product-specific columns
    name          VARCHAR(255),
    price         DECIMAL(10,2)
);
```

### 8.2 Index recommendations

```sql
-- Soft delete filter — thường dùng
CREATE INDEX idx_users_deleted ON users(deleted) WHERE deleted = FALSE;

-- Auditor queries — tìm records theo người tạo
CREATE INDEX idx_users_created_by ON users(created_by);

-- Timestamp queries — tìm records mới tạo
CREATE INDEX idx_users_created_at ON users(created_at DESC);
```

---

## 9. Cách sử dụng

### 9.1 Tạo entity mới

```java
@Entity
@Table(name = "users")
public class User extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    // KHÔNG cần định nghĩa id, createdAt, updatedAt, createdBy, updatedBy, deleted
    // — đã có từ BaseEntity
}
```

### 9.2 Soft delete — 2 cách

```java
// Cách 1: Gọi markDeleted()
userRepository.findById(id).ifPresent(user -> {
    user.markDeleted();
    userRepository.save(user);
});

// Cách 2: Custom repository query
public interface UserRepository extends JpaRepository<User, UUID> {
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deleted = false")
    Optional<User> findActiveById(@Param("id") UUID id);
}
```

### 9.3 Tạo bản ghi với auditor

```java
// Trong service — set auditor trước khi save
public void createUser(CreateUserDto dto) {
    AuditorContext.setAuditor(getCurrentUsername()); // hoặc "SYSTEM"
    try {
        User user = new User();
        user.setName(dto.getName());
        userRepository.save(user); // createdBy = current user
    } finally {
        AuditorContext.clear();
    }
}
```

### 9.4 Timestamps tự động

```java
// createdAt và updatedAt được JPA set TỰ ĐỘNG khi:
// - INSERT → createdAt = updatedAt = now()
// - UPDATE → updatedAt = now()

// Không cần làm gì thêm
User user = userRepository.findById(id);
user.setEmail("new@example.com");
userRepository.save(user);
// → updatedAt tự động = now()
```

---

## 10. Cách populate AuditorContext

### 10.1 Trong Security Filter

```java
@Component
public class AuditorFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                   HttpServletResponse response,
                                   FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String username = getCurrentUsername(request);
            AuditorContext.setAuditor(username);
            filterChain.doFilter(request, response);
        } finally {
            AuditorContext.clear();
        }
    }

    private String getCurrentUsername(HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            return auth.getName();
        }
        return "ANONYMOUS";
    }
}
```

### 10.2 Trong Service cho batch/scheduled jobs

```java
@Scheduled(cron = "0 0 2 * * ?")
public void cleanupExpiredTokens() {
    AuditorContext.setAuditor("SCHEDULER_CLEANUP");
    try {
        tokenRepository.deleteExpiredTokens(LocalDateTime.now());
    } finally {
        AuditorContext.clear();
    }
}
```

### 10.3 Trong Service cho CLI/admin tasks

```java
public void importUsersFromCsv(String csvFile) {
    AuditorContext.setAuditor("ADMIN_IMPORT:" + admin.getUsername());
    try {
        csvReader.readAll(csvFile).forEach(userRepository::save);
    } finally {
        AuditorContext.clear();
    }
}
```

---

## 11. Soft Delete Pattern

### 11.1 BaseEntity soft delete vs hard delete

```java
// ❌ Hard delete — MẤT DỮ LIỆU VĨNH VIỄN
userRepository.delete(user);
userRepository.flush();
// → DELETE FROM users WHERE id = ?

// ✅ Soft delete — Dữ liệu còn, không truy vấn thấy
user.markDeleted();
userRepository.save(user);
// → UPDATE users SET deleted = TRUE, updated_at = now() WHERE id = ?

// ✅ Soft delete qua repository
@Modifying
@Query("UPDATE User u SET u.deleted = true WHERE u.id = :id")
void softDelete(@Param("id") UUID id);
```

### 11.2 Transparent soft delete với @Where

Thêm vào BaseEntity:

```java
@Where(clause = "deleted = false")
// → Tự động thêm WHERE deleted = false cho mọi query trên entity này
// → Không cần thủ công thêm điều kiện ở mọi repository query
```

```java
// BaseEntity — thêm @Where annotation
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
@Where(clause = "deleted = false")  // ← Thêm dòng này
public abstract class BaseEntity { ... }
```

### 11.3 Khi nào dùng soft delete

| Dùng soft delete | Dùng hard delete |
|-------------------|------------------|
| User accounts | Temporary cache records |
| Orders/Transactions | Duplicate import logs |
| Reference data (categories, tags) | GDPR full erasure request |
| Anything with financial/legal implication | Data có TTL rõ ràng |

---

## 12. Best Practice Java 21 + Lombok

### 12.1 Lombok usage

| Annotation | Dùng ở | Lý do |
|-----------|---------|-------|
| `@Getter` | `BaseEntity` | Tất cả field cần đọc, chỉ 1 field cần setter |
| `@Setter` | Chỉ `deleted` | Các field khác chỉ JPA được set |
| `@NoArgsConstructor` | Không | `@MappedSuperclass` + JPA proxy cần protected, không public |
| `@AllArgsConstructor` | Không | Không cần |

### 12.2 Java 21 features

```java
// Switch expression với pattern matching (Java 21)
public String formatId(UUID id) {
    return switch (id) {
        case null -> "N/A";
        case UUID u when u.version() == 4 -> "v4-" + u.toString().substring(0, 8);
        default -> id.toString();
    };
}

// Record cho immutable DTO (Java 16+)
public record CreateUserCommand(String name, String email) {}

// Stream toList() (Java 16+) — immutable list
List<String> emails = users.stream()
    .map(User::getEmail)
    .toList();

// Null check với pattern matching (Java 21)
public void process(User user) {
    if (user instanceof User u && u.getEmail() != null) {
        sendEmail(u.getEmail());
    }
}
```

### 12.3 JPA/Hibernate best practices

```java
// ❌ Không override equals/hashCode bằng entity ID
// → ID là UUID, được generate SAU KHI persist
// → equals() trả về false khi entity chưa persist
public class User extends BaseEntity {
    @Override
    public boolean equals(Object o) {
        return this.id != null && this.id.equals(((User) o).id);
    }
}

// ✅ Hoặc dùng business key
@Override
public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof User other)) return false;
    return email != null && email.equals(other.email);
}
```

---

## 13. Scalability & Multi-module

### 13.1 Khi tách module

```
pets-platform/
├── module-common/                  # Chia sẻ giữa các module
│   └── shared/
│       ├── base/
│       │   ├── BaseEntity.java
│       │   └── AuditorContext.java
│       └── config/
│           └── JpaAuditingConfig.java
│
├── module-user/                   # Module User
│   └── src/main/java/.../
│       └── domain/
│           └── User.java          ← extends BaseEntity từ module-common
│
└── module-product/               # Module Product
    └── src/main/java/.../
        └── domain/
            └── Product.java       ← extends BaseEntity từ module-common
```

### 13.2 Mỗi module có BaseEntity riêng

Nếu muốn module độc lập hoàn toàn, mỗi module có thể có BaseEntity riêng. Kiến trúc này **không yêu cầu** module-common.

### 13.3 Microservices migration

```
Monolith → 2 Microservices (User Service + Order Service)

User Service có:
  ├── User extends BaseEntity       ← có đầy đủ auditing
  └── BaseEntity từ shared JAR

Order Service có:
  ├── Order extends BaseEntity      ← cũng có đầy đủ auditing
  └── BaseEntity từ shared JAR (cùng JAR)

→ Cùng cấu trúc bảng
→ Cùng error codes (ApiError)
→ Consistent giữa services
```

---

## 14. File Index

| File | Package | Mục đích |
|------|---------|----------|
| `BaseEntity.java` | `shared.base` | `@MappedSuperclass` — 6 field auditing + soft delete |
| `AuditorContext.java` | `shared.base` | ThreadLocal holder cho current auditor |
| `JpaAuditingConfig.java` | `config` | `@EnableJpaAuditing` + `AuditorAware` bean |

---

## Checklist trước khi merge

- [ ] Mọi domain entity đều `extends BaseEntity`
- [ ] Không có entity nào định nghĩa trùng `id`, `createdAt`, `updatedAt`, `createdBy`, `updatedBy`, `deleted`
- [ ] `AuditorContext.setAuditor()` được gọi trước mọi `save()` trong service
- [ ] `AuditorContext.clear()` được gọi trong `finally` block
- [ ] Security filter đã set auditor cho authenticated requests
- [ ] Scheduled jobs/batch đã set auditor là `"SYSTEM"` hoặc tên job
- [ ] Soft delete dùng `markDeleted()` hoặc repository query, không dùng `delete()`
- [ ] `@Where(clause = "deleted = false")` đã được thêm vào BaseEntity nếu muốn transparent filtering
- [ ] Database migration thêm `created_by`, `updated_by`, `deleted` columns cho các bảng hiện có
