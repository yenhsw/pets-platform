# Database Foundation Architecture

**File:** `080626.006.md`
**Module:** `com.petsplatform.infrastructure.database`
**Version:** 1.0.0

---

## Mục lục

1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Khi nào dùng JPA / Native Query / Procedure](#2-khi-nào-dùng-jpa--native-query--procedure)
3. [Scalable cho CQRS sau này](#3-scalable-cho-cqrs-sau-này)
4. [Files đã tạo](#4-files-đã-tạo)
5. [JpaDatabaseConfig — naming strategy + transaction](#5-jpadatabaseconfig--naming-strategy--transaction)
6. [BaseRepository — base interface cho mọi repository](#6-baserepository--base-interface-cho-mọi-repository)
7. [AbstractBaseRepositoryImpl — soft delete tự động](#7-abstractbaserepositoryimpl--soft-delete-tự-động)
8. [ExampleUserRepository — Spring Data JPA patterns](#8-exampleuserrepository--spring-data-jpa-patterns)
9. [ExampleNativeQueryRepository — EntityManager patterns](#9-examplenativequeryrepository--entitymanager-patterns)
10. [ExampleStoredProcedureRepository — PostgreSQL procedure patterns](#10-examplestoredprocedurerepository--postgresql-procedure-patterns)
11. [Naming strategy chi tiết](#11-naming-strategy-chi-tiết)
12. [HikariCP configuration](#12-hikaricp-configuration)
13. [SQL logging configuration](#13-sql-logging-configuration)
14. [File index](#14-file-index)

---

## 1. Tổng quan kiến trúc

```
┌──────────────────────────────────────────────────────────────┐
│                      PetsPlatformApplication                    │
│                           @SpringBootApplication                 │
│                                │                                │
│    ┌───────────────────────────┼─────────────────────────────┐ │
│    │                           │                             │ │
│    ▼                           ▼                             │ │
│ JpaDatabaseConfig    JpaAuditingConfig                        │ │
│  @EnableJpaRepositories   @EnableJpaAuditing                 │ │
│  repositoryBaseClass       AuditorAware                       │ │
│    =AbstractBaseRepositoryImpl                                 │ │
└────┼───────────────────────────────────────────────────────┘ │
     │                                                            │
     ▼                                                            │
┌──────────────────────────────────────────────────────────────┐
│                    Repository Layer                              │
│                                                               │
│  BaseRepository<E, UUID>  ← JpaRepository + JpaSpecificationExecutor
│         │                                                        │
│         ├── ExampleUserRepository       ← Spring Data JPA
│         │     ├── Method name queries
│         │     ├── @Query (JPQL)
│         │     ├── @Query (native)
│         │     └── Specification
│         │                                                        │
│         ├── ExampleNativeQueryRepository ← EntityManager
│         │     ├── CTE + Window functions
│         │     ├── Batch operations
│         │     └── Scalar results (Map)
│         │                                                        │
│         └── ExampleStoredProcedureRepository ← StoredProcedureQuery
│               ├── IN/OUT parameters
│               ├── Scalar return
│               └── Result set return
└──────────────────────────────────────────────────────────────┘
```

### Database config hiện có

| Component | Config location | Description |
|-----------|----------------|-------------|
| HikariCP | `application.yml` | Pool size, timeout, leak detection |
| JPA/Hibernate | `application.yml` | DDL auto, batch size, naming |
| Auditing | `JpaAuditingConfig` | @CreatedDate, @CreatedBy |
| Naming | `application.yml` | Snake_case physical strategy |

---

## 2. Khi nào dùng JPA / Native Query / Procedure

### 2.1 Decision matrix

| Tiêu chí | JPA (@Query / Method name) | Native Query | Procedure / Function |
|----------|---------------------------|-------------|---------------------|
| CRUD cơ bản | ✅ Luôn dùng | ❌ Thừa | ❌ Thừa |
| Aggregation đơn giản | ✅ SUM, COUNT, AVG | ✅ Phức tạp hơn | ❌ Thừa |
| JOIN phức tạp (5+ bảng) | ✅ Nếu rõ ràng | ✅ Nếu phức tạp | ❌ |
| CTE / Window function | ❌ Không hỗ trợ | ✅ CTE, RANK, PARTITION | ✅ Có thể dùng |
| Paging với offset lớn | ✅ Page/slice | ✅ Tối ưu hơn | ✅ |
| Business logic phức tạp | ❌ Nên ở service | ❌ Nên ở procedure | ✅ |
| Performance tối ưu | Trung bình | ✅ Tốt | ✅ Tốt nhất |
| Database-agnostic | ✅ JPQL portable | ❌ PostgreSQL-specific | ❌ DB-specific |
| Testability | ✅ Unit test | ✅ Integration test | ❌ Cần DB |
| Migration complexity | ✅ Không cần | ✅ Có thể | ❌ Phức tạp |

### 2.2 Quy tắc thực tế

```
Luôn bắt đầu với JPA.
→ Nếu JPA không làm được → xuống Native Query.
→ Nếu Native Query quá phức tạp hoặc cần reuse → xuống Procedure.

Số % trong thực tế:
  JPA:           ~80% queries
  Native Query:  ~15% queries
  Procedure:      ~5% queries
```

### 2.3 Ví dụ minh họa

```java
// ✅ JPA — CRUD đơn giản
User user = userRepository.findByEmail(email);
userRepository.existsByEmail(email);
userRepository.findByStatus(ACTIVE, pageable);

// ✅ JPA @Query — điều kiện phức tạp nhưng vẫn portable
@Query("SELECT u FROM User u WHERE u.email LIKE %:name% AND u.deleted = false")
List<User> searchByName(@Param("name") String name);

// ✅ Native Query — CTE + Window function (JPA không làm được)
@Query(value = """
    WITH ranked AS (
        SELECT *, ROW_NUMBER() OVER (ORDER BY created_at DESC) AS rn
        FROM users WHERE deleted = false
    )
    SELECT * FROM ranked WHERE rn <= :limit
    """, nativeQuery = true)
List<User> findTopRecent(@Param("limit") int limit);

// ❌ Native Query — KHÔNG dùng cho CRUD đơn giản
@Query(value = "SELECT * FROM users WHERE id = :id", nativeQuery = true)
Optional<User> findByIdRaw(@Param("id") UUID id);
// → Dùng JPA: userRepository.findById(id)

// ✅ Procedure — business logic phức tạp, cần transaction bên trong DB
// Hoặc khi nhiều service gọi cùng logic (reuse)
public int archiveRecordsBefore(Instant before) { ... }
```

---

## 3. Scalable cho CQRS sau này

### 3.1 Current: Single database (Modular Monolith)

```
┌────────────────────────────────────────┐
│          PetsPlatform (Monolith)        │
│                                        │
│  Command Side (Write)                  │
│    ExampleUserRepository               │
│    ExampleNativeQueryRepository        │
│    ExampleStoredProcedureRepository    │
│                                        │
│  Query Side (Read)                     │
│    (Đọc từ cùng database)             │
└────────────────────────────────────────┘
```

### 3.2 Future: CQRS với Read replicas

```
┌────────────────────────────────────────┐
│          PetsPlatform                    │
│                                          │
│  Write path ──► PostgreSQL (Primary)    │
│                    │                     │
│                    └──► CDC / Event ────► Read DB
│                                              │
│  Read path ◄── PostgreSQL (Read Replica) ◄┘
│      └── Không cần thay đổi query code
│      └── Chỉ thay đổi datasource config
```

### 3.3 Future: CQRS với Event Sourcing

```
Command Side:
  Command → Aggregate → Domain Event → Event Store (PostgreSQL / Kafka)
  ExampleStoredProcedureRepository có thể wrap transaction logic

Query Side:
  Read Model → Projector → Elasticsearch / PostgreSQL (materialized view)
  ExampleNativeQueryRepository chỉ cần thay đổi table name
```

### 3.4 Thêm module mới không cần thay đổi gì

```
Module Order thêm:
  OrderRepository extends BaseRepository<Order, UUID>
    → Tự động có soft delete, auditing, pagination
    → Tự động có Specification support
    → KHÔNG cần thay đổi JpaDatabaseConfig
    → KHÔNG cần thay đổi AbstractBaseRepositoryImpl
```

---

## 4. Files đã tạo

```
src/main/java/com/petsplatform/infrastructure/database/
├── JpaDatabaseConfig.java              ← @EnableJpaRepositories + @EnableTransactionManagement
├── BaseRepository.java                 ← Generic interface: JpaRepository + JpaSpecificationExecutor
├── AbstractBaseRepositoryImpl.java     ← Custom base: auto soft-delete in findById
├── DbUtils.java                        ← Pagination helpers, constants
└── examples/
    ├── ExampleUser.java                ← Demo entity
    ├── ExampleUserRepository.java       ← Spring Data JPA patterns
    ├── ExampleUserSpecifications.java  ← Dynamic query builder
    ├── ExampleNativeQueryRepository.java← EntityManager patterns
    └── ExampleStoredProcedureRepository.java ← Procedure/function patterns

src/main/resources/
└── application.yml                      ← Updated: naming strategy + SQL logging
```

---

## 5. JpaDatabaseConfig — Naming Strategy + Transaction

**File:** `src/main/java/com/petsplatform/infrastructure/database/JpaDatabaseConfig.java`

### 5.1 Annotations

```java
@Configuration
@EnableJpaAuditing          // Kích hoạt @CreatedDate, @CreatedBy, @LastModifiedDate, @LastModifiedBy
@EnableJpaRepositories(
    basePackages = "com.petsplatform",
    repositoryBaseClass = AbstractBaseRepositoryImpl.class  // Custom base cho mọi repository
)
@EnableTransactionManagement // Kích hoạt @Transactional annotation
public class JpaDatabaseConfig { }
```

### 5.2 Transaction defaults

| Setting | Value | Ý nghĩa |
|---------|-------|---------|
| Rollback | Unchecked exceptions | RuntimeException, DataAccessException → tự động rollback |
| No rollback | Checked exceptions | SQLException → KHÔNG rollback (cần explicit) |
| Isolation | Database default | Thường là READ_COMMITTED |
| Propagation | REQUIRED | Nếu có transaction thì reuse, không thì tạo mới |

### 5.3 Naming strategy config (trong application.yml)

```yaml
spring:
  jpa:
    properties:
      hibernate:
        naming:
          physical-strategy: PhysicalNamingStrategySnakeCaseImpl
          implicit-strategy: ImplicitNamingStrategyJpaCompliantImpl
```

---

## 6. BaseRepository — Base Interface cho mọi Repository

**File:** `src/main/java/com/petsplatform/infrastructure/database/BaseRepository.java`

```java
@NoRepositoryBean
public interface BaseRepository<E extends BaseEntity, ID extends UUID>
    extends JpaRepository<E, ID>, JpaSpecificationExecutor<E> {

    Optional<E> findByIdAndDeletedFalse(ID id);  // Tự động filter soft-delete
    boolean existsByIdAndDeletedFalse(ID id);
}
```

### 6.1 Sử dụng

```java
// Tất cả entity repositories kế thừa BaseRepository
public interface UserRepository extends BaseRepository<User, UUID> { }
public interface ProductRepository extends BaseRepository<Product, UUID> { }
public interface OrderRepository extends BaseRepository<Order, UUID> { }
```

---

## 7. AbstractBaseRepositoryImpl — Soft Delete tự động

**File:** `src/main/java/com/petsplatform/infrastructure/database/AbstractBaseRepositoryImpl.java`

### 7.1 Key feature: Override findById

```java
@Override
public Optional<E> findById(ID id) {
    return findByIdAndDeletedFalse(id);  // Tự động filter deleted=true
}
```

### 7.2 Tại sao dùng @Where trên BaseEntity + override findById

```
BaseEntity có:
  @Where(clause = "deleted = false")
  
→ Tự động thêm WHERE deleted = false cho mọi JPQL query
→ NHƯNG findById(ID id) dùng entityManager.find() → bypass @Where
→ → Override findById trong AbstractBaseRepositoryImpl để cover cả trường hợp này
```

### 7.3 Result

```
Tất cả queries tự động exclude soft-deleted records:
  userRepository.findById(id)       → WHERE deleted = false ✓
  userRepository.findAll()         → WHERE deleted = false ✓
  userRepository.existsById(id)   → WHERE deleted = false ✓
  specExecutor.findAll(spec)       → WHERE deleted = false ✓
  entityManager.find(...)          → WHERE deleted = false ✓ (nhờ @Where)
```

---

## 8. ExampleUserRepository — Spring Data JPA Patterns

**File:** `src/main/java/com/petsplatform/infrastructure/database/examples/ExampleUserRepository.java`

### 8.1 Method name queries

```java
// Tìm theo email — Spring tự generate SQL
Optional<User> findByEmail(String email);

// AND/OR logic
Optional<User> findByEmailAndStatus(String email, UserStatus status);

// Pattern matching
List<User> findByFullNameContainingIgnoreCase(String name);  // ILIKE '%name%'

// Count
boolean existsByEmail(String email);

// Pagination
Page<User> findByStatus(UserStatus status, Pageable pageable);
```

### 8.2 @Query — JPQL

```java
@Query("SELECT u FROM User u WHERE u.fullName LIKE %:name% AND u.deleted = false")
List<User> searchByName(@Param("name") String name);

@Query("SELECT u FROM User u WHERE u.email = :email AND u.status = :status AND u.deleted = false")
Optional<User> findByEmailAndStatusJpql(@Param("email") String email, @Param("status") UserStatus status);

@Query("SELECT COUNT(u) FROM User u WHERE u.status = :status AND u.deleted = false")
long countByStatus(@Param("status") UserStatus status);
```

### 8.3 @Query — Native SQL

```java
@Query(
    value = """
        SELECT * FROM example_users
        WHERE deleted = false AND created_at >= :since
        ORDER BY created_at DESC LIMIT :limit
        """,
    nativeQuery = true
)
List<User> findRecentUsers(@Param("since") Timestamp since, @Param("limit") int limit);

// Projection — không cần entity, trả về interface
@Query(
    value = """
        SELECT full_name, email, COUNT(*) as count
        FROM example_users WHERE deleted = false AND status = :status
        GROUP BY full_name, email
        """,
    nativeQuery = true
)
List<EmailCountProjection> countByStatusGroupedByEmail(@Param("status") String status);

interface EmailCountProjection {
    String getFullName();
    String getEmail();
    Long getCount();
}
```

### 8.4 Specification — Dynamic queries

```java
// Kết hợp nhiều điều kiện động
Specification<User> spec = ExampleUserSpecifications
    .withName("John")
    .and(ExampleUserSpecifications.withStatus(ACTIVE))
    .and(ExampleUserSpecifications.withMinAge(18));

Page<User> result = userRepository.findAll(spec, pageable);
```

---

## 9. ExampleNativeQueryRepository — EntityManager Patterns

**File:** `src/main/java/com/petsplatform/infrastructure/database/examples/ExampleNativeQueryRepository.java`

### 9.1 Scalar results (Map)

```java
// Aggregation không cần entity
List<Map<String, Object>> stats = nativeRepo.findUserStatsByStatus("ACTIVE");
// [{ status: "ACTIVE", totalCount: 42, avgAge: 28.5 }, ...]
```

### 9.2 CTE + Window function

```java
// Ranking với ROW_NUMBER() — JPA không làm được
List<Map<String, Object>> ranked = nativeRepo.rankUsersByActivity(10);
// [{ id: uuid, fullName: "John", rank: 1 }, ...]
```

### 9.3 Batch operations

```java
// Batch update
int affected = nativeRepo.batchUpdateStatus(List.of(id1, id2), "INACTIVE");

// Batch insert
int inserted = nativeRepo.batchInsertUsers(users);
```

---

## 10. ExampleStoredProcedureRepository — PostgreSQL Procedure Patterns

**File:** `src/main/java/com/petsplatform/infrastructure/database/examples/ExampleStoredProcedureRepository.java`

### 10.1 PostgreSQL functions (trong DB)

```sql
-- Scalar function
CREATE OR REPLACE FUNCTION fn_calculate_total_spending(p_user_id UUID)
RETURNS DECIMAL AS $$
DECLARE total DECIMAL;
BEGIN
    SELECT COALESCE(SUM(amount), 0) INTO total
    FROM orders WHERE user_id = p_user_id AND deleted = false;
    RETURN total;
END;
$$ LANGUAGE plpgsql;

-- Function returning SETOF
CREATE OR REPLACE FUNCTION fn_get_active_users()
RETURNS SETOF example_users AS $$
BEGIN
    RETURN QUERY SELECT * FROM example_users
    WHERE status = 'ACTIVE' AND deleted = false;
END;
$$ LANGUAGE plpgsql;

-- Procedure với OUT parameter
CREATE OR REPLACE PROCEDURE sp_archive_records(
    IN p_before_date TIMESTAMP,
    OUT p_archived_count INT
) AS $$
BEGIN
    UPDATE example_users
    SET deleted = true, updated_at = NOW()
    WHERE created_at < p_before_date AND deleted = false;
    GET DIAGNOSTICS p_archived_count = ROW_COUNT;
END;
$$ LANGUAGE plpgsql;
```

### 10.2 Gọi từ Java

```java
// Function — scalar return
BigDecimal total = procedureRepo.calculateTotalSpending(userId);

// Function — SETOF entity
List<ExampleUser> active = procedureRepo.getActiveUsers();

// Procedure — IN/OUT parameters
int archived = procedureRepo.archiveRecordsBefore(Instant.now().minus(30, ChronoUnit.DAYS));
```

### 10.3 Khi nào dùng procedure

| Scenario | Dùng Procedure? |
|----------|----------------|
| Complex transaction logic cần ACID | ✅ Có |
| Nhiều service reuse cùng logic | ✅ Có |
| Performance-critical batch | ✅ Có |
| Business logic đơn giản | ❌ Không |
| Cần unit test | ❌ Không |
| Cần database-agnostic | ❌ Không |

---

## 11. Naming Strategy chi tiết

### 11.1 Snake_case mapping

| Java field | Column name | Table name |
|-----------|------------|------------|
| `firstName` | `first_name` | - |
| `userId` | `user_id` | - |
| `orderDetails` | `order_details` | - |
| `URL` | `url` | - |
| `Entity class` | - | `example_user` (auto from Entity name) |
| `Entity @Table(name="users")` | - | `users` (override) |

### 11.2 Java code vs Database column

```java
@Entity
@Table(name = "example_users")     // Table name
public class ExampleUser extends BaseEntity {

    @Column(name = "full_name")    // Override column name
    private String fullName;        // → full_name (snake_case auto)

    @Column(name = "created_at", updatable = false)
    private Instant createdAt;       // → created_at

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private ExampleStatus status;    // → status (enum as string)
}
```

---

## 12. HikariCP Configuration

### 12.1 Dev vs Prod

| Setting | Dev | Prod | Ý nghĩa |
|---------|-----|------|---------|
| `maximum-pool-size` | 10 | 50 | Số connections tối đa |
| `minimum-idle` | 5 | 10 | Số connections luôn mở |
| `idle-timeout` | 5 phút | 5 phút | Connection idle quá lâu → đóng |
| `connection-timeout` | 20s | 20s | Chờ connection timeout |
| `max-lifetime` | 20 phút | 30 phút | Connection sống tối đa bao lâu |
| `leak-detection-threshold` | 0 (off) | 60s | Phát hiện leak |

### 12.2 Connection pool sizing formula

```
minimum connections = ((core_count * 2) + effective_spindle_count)
maximum connections = core_count * 5

Ví dụ: 4 core, SSD → min=10, max=20
Ví dụ: 8 core, SSD → min=18, max=40
```

---

## 13. SQL Logging Configuration

### 13.1 Dev environment

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG           # Ghi SQL statements
    org.hibernate.orm.jdbc.bind: TRACE  # Ghi parameter values
    org.springframework.jdbc.core: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %X{traceId:-} %logger{36} - %msg%n"
```

**Output:**
```
Hibernate:
  select u.id, u.created_at, ... from example_users u where u.id=?
  binding parameter [1] as [BINARY] = [a1b2c3...]
```

### 13.2 Prod environment

```yaml
logging:
  level:
    org.hibernate.SQL: ERROR  # Chỉ ghi khi có lỗi
  pattern:
    console: "%d{...} [%thread] %-5level %X{traceId:-} ... - %msg%n"
```

---

## 14. File Index

| File | Package | Lines | Mục đích |
|------|---------|-------|----------|
| `JpaDatabaseConfig.java` | `infrastructure.database` | 45 | @EnableJpaRepositories + @EnableTransactionManagement |
| `BaseRepository.java` | `infrastructure.database` | 39 | Generic interface (JpaRepository + JpaSpecificationExecutor) |
| `AbstractBaseRepositoryImpl.java` | `infrastructure.database` | 73 | Auto soft-delete in findById |
| `DbUtils.java` | `infrastructure.database` | 46 | Pagination helpers, constants |
| `ExampleUser.java` | `infrastructure.database.examples` | 52 | Demo entity (do not use as template) |
| `ExampleUserRepository.java` | `infrastructure.database.examples` | 138 | JPA patterns: method name, @Query, native, Specification |
| `ExampleUserSpecifications.java` | `infrastructure.database.examples` | 96 | Dynamic query builder |
| `ExampleNativeQueryRepository.java` | `infrastructure.database.examples` | 215 | EntityManager patterns: CTE, batch, pagination |
| `ExampleStoredProcedureRepository.java` | `infrastructure.database.examples` | 178 | PostgreSQL procedure/function patterns |

---

## Checklist trước khi merge

- [ ] Mọi domain entity đều `extends BaseEntity` và `implements BaseRepository<E, UUID>`
- [ ] `@Where(clause = "deleted = false")` đã thêm vào BaseEntity
- [ ] `AbstractBaseRepositoryImpl.findById(ID)` override tự động filter soft-delete
- [ ] Business logic dùng JPA (`@Query`) trước, chỉ dùng native query khi cần
- [ ] Procedure chỉ dùng cho complex transaction hoặc reusable logic
- [ ] Table migration đã thêm cột `deleted` (BOOLEAN DEFAULT FALSE) cho các bảng hiện có
- [ ] Naming strategy snake_case đã set trong application.yml
- [ ] HikariCP pool size phù hợp với môi trường
- [ ] SQL logging chỉ bật DEBUG/TRACE trong dev
- [ ] Khi thêm module mới: KHÔNG cần thay đổi JpaDatabaseConfig
