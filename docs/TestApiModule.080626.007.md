# TestApi Module — Hướng dẫn test

**Module:** `com.petsplatform.module.testapi`
**Table:** `test_apis`

---

## 1. Cấu trúc module

```
src/main/java/com/petsplatform/module/testapi/
├── domain/
│   ├── TestApi.java              ← Entity (12 fields)
│   ├── TestApiRepository.java    ← Repository (18 methods: JPA + native)
│   └── TestApiSpecifications.java← Dynamic query builder
├── dto/
│   ├── TestApiDto.java            ← CreateRequest, UpdateRequest, SearchRequest, Response
│   └── TestApiMapper.java        ← Entity ↔ DTO mapping
├── TestApiService.java            ← Business logic (22 methods)
└── TestApiController.java         ← REST API (19 endpoints)

src/main/resources/db/migration/
├── V1__create_test_apis_table.sql ← Schema
└── V2__seed_test_data.sql        ← 10 rows seed data
```

---

## 2. Cách chạy

### 2.1 Chạy app (dev)

```bash
# Đảm bảo PostgreSQL đang chạy
# Database: pets_platform_dev phải tồn tại

# Chạy với dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# Hoặc
java -jar target/pets-platform.jar --spring.profiles.active=dev
```

### 2.2 Chạy tests

```bash
# Tạo database test trước
# psql -U postgres -c "CREATE DATABASE pets_platform_test;"

mvn test -Dspring.profiles.active=test
```

### 2.3 Chạy API thủ công

```bash
# Tạo user
curl -X POST http://localhost:8080/api/test-apis \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Nguyen Van A",
    "email": "test@example.com",
    "phone": "+84-901-234-567",
    "age": 25,
    "gender": "MALE",
    "salary": 15000000,
    "birthDate": "1999-03-15",
    "bio": "Developer",
    "status": "ACTIVE",
    "score": 8.5
  }'
```

---

## 3. Tất cả API endpoints

### 3.1 CRUD

| Method | Endpoint | Description | Demo |
|--------|----------|-------------|------|
| POST | `/api/test-apis` | Tạo mới | ✅ |
| PUT | `/api/test-apis/{id}` | Cập nhật | ✅ |
| DELETE | `/api/test-apis/{id}` | Soft delete | ✅ |
| GET | `/api/test-apis/{id}` | Tìm theo ID | ✅ |
| GET | `/api/test-apis/by-email/{email}` | Tìm theo email | ✅ |

### 3.2 JPA — Method Name Queries

| Method | Endpoint | Description | Demo |
|--------|----------|-------------|------|
| GET | `/api/test-apis` | List all (paginated) | ✅ |
| GET | `/api/test-apis/by-status/{status}` | Lọc theo status (paginated) | ✅ |
| GET | `/api/test-apis/by-gender/{gender}` | Lọc theo giới tính | ✅ |
| GET | `/api/test-apis/by-age?minAge=20&maxAge=30` | Lọc theo tuổi | ✅ |
| GET | `/api/test-apis/by-name?name=ABC` | Tìm theo tên (like) | ✅ |
| GET | `/api/test-apis/birth-after?date=2000-01-01` | Sinh sau ngày | ✅ |
| GET | `/api/test-apis/score-above?minScore=7.0` | Điểm từ | ✅ |

### 3.3 JPA — @Query JPQL

| Method | Endpoint | Description | Demo |
|--------|----------|-------------|------|
| GET | `/api/test-apis/search/by-name-status?name=ABC&status=ACTIVE` | Tìm theo tên + status | ✅ |
| GET | `/api/test-apis/search/advanced` | Tìm nâng cao (nhiều params) | ✅ |
| GET | `/api/test-apis/count/by-status/{status}` | Đếm theo status | ✅ |
| GET | `/api/test-apis/avg-age/by-status/{status}` | Tuổi trung bình | ✅ |

### 3.4 JPA — Specification (Dynamic)

| Method | Endpoint | Description | Demo |
|--------|----------|-------------|------|
| GET | `/api/test-apis/search/dynamic` | Tìm động (tất cả fields) | ✅ |

Params: `name`, `status`, `gender`, `minAge`, `maxAge`, `minScore`, `page`, `size`

### 3.5 JPA — Native SQL

| Method | Endpoint | Description | Demo |
|--------|----------|-------------|------|
| GET | `/api/test-apis/recent?since=2025-01-01T00:00:00Z&status=ACTIVE&limit=20` | User gần đây | ✅ |
| GET | `/api/test-apis/statistics` | Thống kê theo status | ✅ |

---

## 4. Ví dụ API calls

### 4.1 Tạo record

```bash
curl -X POST http://localhost:8080/api/test-apis \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Tran Thi B",
    "email": "tranb@example.com",
    "age": 28,
    "gender": "FEMALE",
    "salary": 20000000,
    "birthDate": "1996-01-15",
    "status": "ACTIVE",
    "score": 9.0
  }'
```

Response:
```json
{
  "success": true,
  "message": "Tạo thành công",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "name": "Tran Thi B",
    "email": "tranb@example.com",
    "age": 28,
    "gender": "FEMALE",
    "salary": 20000000.00",
    "birthDate": "1996-01-15",
    "bio": null,
    "status": "ACTIVE",
    "score": 9.0,
    "createdAt": "2026-06-08T10:30:00Z",
    "updatedAt": "2026-06-08T10:30:00Z",
    "createdBy": "api-user",
    "updatedBy": "api-user"
  }
}
```

### 4.2 Tìm theo email

```bash
curl http://localhost:8080/api/test-apis/by-email/tranb@example.com
```

### 4.3 List all (pagination)

```bash
curl "http://localhost:8080/api/test-apis?page=0&size=5&sortBy=createdAt&sortDir=DESC"
```

### 4.4 Tìm nâng cao

```bash
curl "http://localhost:8080/api/test-apis/search/advanced?name=Nguyen&status=ACTIVE&minAge=25&maxAge=35&page=0&size=10"
```

### 4.5 Tìm động (Specification)

```bash
curl "http://localhost:8080/api/test-apis/search/dynamic?gender=MALE&minScore=8.0&page=0&size=10"
```

### 4.6 Xem thống kê

```bash
curl http://localhost:8080/api/test-apis/statistics
```

### 4.7 Soft delete

```bash
# Xóa user có id cụ thể
curl -X DELETE http://localhost:8080/api/test-apis/550e8400-e29b-41d4-a716-446655440000
```

Sau khi soft delete, `findById` sẽ trả về `null` (hoặc 404).

---

## 5. Ví dụ SQL trực tiếp

### 5.1 Native query — CTE với window function

```sql
-- Ranking user theo score (JPA không làm được)
WITH ranked AS (
    SELECT id, name, score,
           ROW_NUMBER() OVER (ORDER BY score DESC) AS rn
    FROM test_apis
    WHERE deleted = false
)
SELECT * FROM ranked WHERE rn <= 5;
```

### 5.2 Thống kê group by

```sql
-- Đếm theo gender
SELECT gender, COUNT(*) FROM test_apis
WHERE deleted = false
GROUP BY gender;
```

### 5.3 Soft delete thủ công

```sql
-- Xóa mềm
UPDATE test_apis SET deleted = true, updated_at = NOW()
WHERE id = '550e8400-e29b-41d4-a716-446655440000';

-- Khôi phục
UPDATE test_apis SET deleted = false, updated_at = NOW()
WHERE id = '550e8400-e29b-41d4-a716-446655440000';
```

---

## 6. Unit test

```bash
# Chạy repository test
mvn test -Dtest=TestApiRepositoryTest

# Chạy tất cả tests
mvn test
```

Test file: `src/test/java/com/petsplatform/module/testapi/TestApiRepositoryTest.java`

Coverage:
- JPA method name queries (6 tests)
- @Query JPQL (4 tests)
- @Query Native SQL (2 tests)
- Soft delete (2 tests)
- Pagination (1 test)
- Auditing fields (1 test)

---

## 7. Database schema

```sql
CREATE TABLE test_apis (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100)    NOT NULL,
    email       VARCHAR(255)    NOT NULL UNIQUE,
    phone       VARCHAR(20),
    age         INTEGER,
    gender      VARCHAR(10),
    salary      DECIMAL(12, 2),
    birth_date  DATE,
    bio         VARCHAR(500),
    status      VARCHAR(20)     NOT NULL DEFAULT 'ACTIVE',
    score       DOUBLE PRECISION,
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by  VARCHAR(100)   NOT NULL DEFAULT 'system',
    updated_by  VARCHAR(100)    NOT NULL DEFAULT 'system',
    deleted     BOOLEAN        NOT NULL DEFAULT FALSE
);
```

---

## 8. Mapping JPA → API

| Entity field | Column | API field | Type |
|-------------|--------|-----------|------|
| `name` | `name` | `name` | String |
| `email` | `email` | `email` | String |
| `phone` | `phone` | `phone` | String |
| `age` | `age` | `age` | Integer |
| `gender` | `gender` | `gender` | Enum (MALE/FEMALE/OTHER) |
| `salary` | `salary` | `salary` | BigDecimal |
| `birthDate` | `birth_date` | `birthDate` | LocalDate |
| `bio` | `bio` | `bio` | String |
| `status` | `status` | `status` | Enum (ACTIVE/INACTIVE/SUSPENDED) |
| `score` | `score` | `score` | Double |
| `createdAt` | `created_at` | `createdAt` | Instant |
| `updatedAt` | `updated_at` | `updatedAt` | Instant |
| `createdBy` | `created_by` | `createdBy` | String |
| `updatedBy` | `updated_by` | `updatedBy` | String |
| `deleted` | `deleted` | (hidden) | Boolean |
| `id` | `id` | `id` | UUID |
