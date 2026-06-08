# Pets Platform - Spring Boot Configuration Guide

## Table of Contents

1. [Cấu trúc thư mục](#1-cấu-trúc-thư-mục)
2. [Quy trình hoạt động của Profile](#2-quy-trình-hoạt-động-của-profile)
3. [Chi tiết từng file config](#3-chi-tiết-từng-file-config)
4. [Hướng dẫn chạy ứng dụng](#4-hướng-dẫn-chạy-ứng-dụng)
5. [Chi tiết cấu hình datasource](#5-chi-tiết-cấu-hình-datasource)
6. [Chi tiết cấu hình JPA / Hibernate](#6-chi-tiết-cấu-hình-jpa--hibernate)
7. [Chi tiết cấu hình HikariCP](#7-chi-tiết-cấu-hình-hikaricp)
8. [Chi tiết cấu hình Logging](#8-chi-tiết-cấu-hình-logging)
9. [Lưu ý bảo mật](#9-lưu-ý-bảo-mật)
10. [FAQ](#10-faq)

---

## 1. Cấu trúc thư mục

```
src/main/resources/
├── application.yml              # Cấu hình BASE - kế thừa bởi mọi profile
├── application-dev.yml        # Profile DEV - local development
├── application-staging.yml    # Profile STAGING - pre-production
├── application-prod.yml       # Profile PROD - production
└── db/
    ├── schema-dev.sql         # Schema init (dev)
    └── data-dev.sql           # Seed data (dev)
```

---

## 2. Quy trình hoạt động của Profile

### Spring Boot Profile Loading Order

```
application.yml          ← BASE (luôn load đầu tiên)
       ↓
application-{profile}.yml  ← PROFILE OVERRIDE (merge chồng lên base)
       ↓
application-{profile}-{local}.yml  ← LOCAL OVERRIDE (override cuối cùng, gitignore)
```

Spring Boot merge properties từ base lên profile theo thứ tự. Profile-specific properties **chồng lên** base properties cùng key.

### Ví dụ merge

| Key | `application.yml` (base) | `application-dev.yml` (profile) | Kết quả cuối cùng |
|---|---|---|---|
| `spring.jpa.hibernate.ddl-auto` | `validate` | `update` | `update` |
| `spring.jpa.show-sql` | `false` | `true` | `true` |
| `spring.jpa.hibernate.jdbc.batch_size` | `25` | *(không có)* | `25` |

---

## 3. Chi tiết từng file config

### 3.1. `application.yml` — Base Configuration

**Vai trò:** Chứa config mặc định, chung cho mọi môi trường. Mọi profile đều kế thừa file này.

**Nội dung chính:**

| Nhóm | Key | Giá trị | Ý nghĩa |
|---|---|---|---|
| `spring.application.name` | pets-platform | Tên ứng dụng |
| `spring.datasource.*` | | Cấu hình kết nối DB default |
| `spring.jpa.*` | | Cấu hình JPA/Hibernate |
| `server.port` | 8080 | Port HTTP |
| `logging.*` | | Cấu hình log |

**Nguyên tắc:** Chỉ giữ giá trị **không đổi** giữa các môi trường.

---

### 3.2. `application-dev.yml` — Dev Profile

**Vai trò:** Cấu hình cho môi trường phát triển local.

**Kích hoạt:** `--spring.profiles.active=dev` hoặc set trong IDE run config.

**Khác biệt chính so với base:**

| Key | Giá trị | Ý nghĩa |
|---|---|---|
| `ddl-auto` | `update` | Hibernate tự tạo/update bảng khi entity thay đổi |
| `show-sql` | `true` | In SQL ra console để debug |
| `format_sql` | `true` | Format SQL cho dễ đọc |
| `sql.init.mode` | `always` | Chạy schema/data SQL khi start |
| `logging` | `DEBUG` | Log chi tiết |
| `server.error.*` | `always` | Hiện đầy đủ lỗi + stacktrace |
| `maximum-pool-size` | `10` | Pool nhỏ, phù hợp local |

**Datasource:** `localhost:5432/pets_platform_dev` — kết nối PostgreSQL local.

---

### 3.3. `application-staging.yml` — Staging Profile

**Vai trò:** Môi trường pre-production, giống production nhưng dùng test data.

**Kích hoạt:** `--spring.profiles.active=staging`

**Khác biệt chính:**

| Key | Giá trị | Ý nghĩa |
|---|---|---|
| `ddl-auto` | `validate` | Chỉ kiểm tra schema, không thay đổi |
| `show-sql` | `false` | Không in SQL |
| `sql.init.mode` | `never` | Không chạy init script |
| `maximum-pool-size` | `20` | Pool trung bình |
| Credentials | `${DB_USERNAME}`, `${DB_PASSWORD}` | Env variables thay vì hardcode |

**Datasource:** `staging-db:5432/petsplatform` — server staging.

---

### 3.4. `application-prod.yml` — Prod Profile

**Vai trò:** Môi trường production thực sự.

**Kích hoạt:** `--spring.profiles.active=prod`

**Khác biệt chính:**

| Key | Giá trị | Ý nghĩa |
|---|---|---|
| `ddl-auto` | `validate` | Schema cố định, không thay đổi |
| `show-sql` | `false` | Không in SQL |
| `format_sql` | `false` | Tối ưu performance |
| `maximum-pool-size` | `50` | Pool lớn, phục vụ nhiều request |
| `leak-detection-threshold` | `60000` | Phát hiện connection leak sau 60s |
| `logging.level.root` | `WARN` | Chỉ log warning/error |
| `logging.file.max-size` | `100MB` | Rotate log khi đạt 100MB |
| `logging.file.max-history` | `30` | Giữ 30 file log cũ |
| `server.error.*` | `never` | Không leak thông tin lỗi ra ngoài |
| Credentials | `${DB_USERNAME}`, `${DB_PASSWORD}` | Env variables |

**Datasource:** `prod-db:5432/petsplatform` — server production.

---

## 4. Hướng dẫn chạy ứng dụng

### 4.1. Chạy với profile

```bash
# Dev (mặc định)
mvn spring-boot:run

# Dev rõ ràng
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Staging
mvn spring-boot:run -Dspring-boot.run.profiles=staging

# Production
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 4.2. Chạy JAR

```bash
java -jar pets-platform.jar
java -jar pets-platform.jar --spring.profiles.active=prod
```

### 4.3. Set default profile trong IDE

Trong IntelliJ IDEA / VS Code, thêm VM argument:

```
-Dspring.profiles.active=dev
```

### 4.4. Chạy nhiều profile cùng lúc

```bash
--spring.profiles.active=dev,local
```

---

## 5. Chi tiết cấu hình Datasource

### 5.1. JDBC URL

```
jdbc:postgresql://{host}:{port}/{database}
```

| Môi trường | Host | Port | Database |
|---|---|---|---|
| Dev | localhost | 5432 | pets_platform_dev |
| Staging | staging-db | 5432 | petsplatform |
| Prod | prod-db | 5432 | petsplatform |

### 5.2. Driver

```
org.postgresql.Driver
```

Driver này cần có trong `pom.xml`:

```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```

---

## 6. Chi tiết cấu hình JPA / Hibernate

### 6.1. DDL Auto Mode

| Giá trị | Dev | Staging | Prod | Mô tả |
|---|---|---|---|---|
| `update` | Yes | No | No | Tự tạo/update bảng theo entity |
| `validate` | No | Yes | Yes | Chỉ kiểm tra schema, không thay đổi |
| `create` | No | No | No | Tạo mới bảng, xóa data cũ khi restart |
| `none` | No | No | No | Không làm gì |

### 6.2. Batch Processing (nằm trong base config)

```yaml
hibernate:
  jdbc:
    batch_size: 25              # Gom 25 câu SQL thành 1 batch
    batch_versioned_data: true  # Hỗ trợ optimistic lock với batch
  order_inserts: true           # Sắp xếp INSERT theo thứ tự để tối ưu batch
  order_updates: true           # Sắp xếp UPDATE theo thứ tự để tối ưu batch
```

### 6.3. SQL Formatting

```yaml
format_sql: true                # Format SQL đẹp, có indent (dev)
use_sql_comments: false         # Tắt comment trong SQL (giảm log noise)
```

### 6.4. Open-in-View

```yaml
open-in-view: false
```

> **Quan trọng:** Luôn đặt `false`. `true` dễ gây `LazyInitializationException` và kéo dài transaction không cần thiết.

---

## 7. Chi tiết cấu hình HikariCP

HikariCP là connection pool mặc định của Spring Boot.

### 7.1. Các tham số

| Tham số | Dev | Staging | Prod | Ý nghĩa |
|---|---|---|---|---|
| `maximum-pool-size` | 10 | 20 | 50 | Tối đa bao nhiêu connection |
| `minimum-idle` | 5 | 10 | 10 | Luôn giữ tối thiểu bao nhiêu connection |
| `idle-timeout` | 300000 | 300000 | 300000 | Connection idle quá lâu bị đóng (5 phút) |
| `connection-timeout` | 20000 | 20000 | 20000 | Timeout chờ lấy connection (20 giây) |
| `max-lifetime` | 1200000 | 1200000 | 1800000 | Connection sống tối đa bao lâu |
| `leak-detection-threshold` | — | — | 60000 | Phát hiện connection leak (prod only) |

### 7.2. Công thức tính pool size

```
maximum-pool-size = (core_count * 2) + effective_spindle_count
```

Ví dụ: server 4 core + SSD = `9` connection (thường set 10-20).

### 7.3. Dev nên dùng pool nhỏ

Dev local chỉ 1 user, pool 10 là đủ. Pool lớn tốn tài nguyên không cần thiết.

---

## 8. Chi tiết cấu hình Logging

### 8.1. Log Levels

| Level | Prod | Dev | Mô tả |
|---|---|---|---|
| `OFF` | — | — | Tắt log hoàn toàn |
| `ERROR` | Hibernate SQL | — | Chỉ lỗi nghiêm trọng |
| `WARN` | root | — | Cảnh báo |
| `INFO` | root, app | root, app | Thông tin chung |
| `DEBUG` | — | Tất cả | Chi tiết debug |
| `TRACE` | — | Hibernate binding | Chi tiết nhất |

### 8.2. Log Pattern

```
%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n
```

| Phần | Ý nghĩa |
|---|---|
| `%d{...}` | Timestamp |
| `[%thread]` | Tên thread |
| `%-5level` | Level (5 ký tự, left-pad) |
| `%logger{36}` | Logger name (max 36 ký tự) |
| `%msg` | Message |
| `%n` | Newline |

### 8.3. Log File Rotation (Prod)

```yaml
logging:
  file:
    name: /var/log/pets-platform/pets-platform.log
    max-size: 100MB    # Mỗi file tối đa 100MB
    max-history: 30    # Giữ 30 file log gần nhất
```

---

## 9. Lưu ý bảo mật

### 9.1. Không hardcode credentials

| Sai | Đúng |
|---|---|
| `password: 123456` | `password: ${DB_PASSWORD}` |
| `username: myuser` | `username: ${DB_USERNAME}` |

### 9.2. Không leak error chi tiết ở Prod

```yaml
# DEV
server.error.include-stacktrace: always
server.error.include-message: always

# PROD
server.error.include-stacktrace: never
server.error.include-message: never
```

### 9.3. Sensitive files

Thêm vào `.gitignore`:

```
# Credentials
.env
*.env
application-local.yml
application-local.yaml

# Logs
logs/
*.log
```

### 9.4. Khuyến nghị cải thiện

Sau này nên chuyển credentials sang:

```bash
# Set env variable trước khi chạy
export DB_USERNAME=your_user
export DB_PASSWORD=your_password
```

Hoặc dùng Spring Cloud Config / HashiCorp Vault để quản lý secrets tập trung.

---

## 10. FAQ

### Q: Tại sao có cả `.yml` và `.yaml`?
**A:** Cùng một định dạng YAML. Spring Boot ưu tiên `.yaml` trước `.yml`. Nên chỉ dùng **một trong hai** — khuyến nghị `.yml`.

### Q: Dev có cần staging profile không?
**A:** Nếu team nhỏ (< 5 người), có thể bỏ staging, chỉ dùng dev + prod. Staging hữu ích khi cần test trên môi trường giống prod trước khi deploy.

### Q: DDL Auto nên để gì?
**A:** `validate` cho staging và prod. `update` hoặc `create-drop` cho dev. **Không bao giờ** dùng `create` ở prod vì mất data.

### Q: Connection pool bao nhiêu là đủ?
**A:** Tùy server. Dev: 10. Staging: 20. Prod: 50-100 (điều chỉnh theo load thực tế).

### Q: App không chạy được, lỗi datasource?
**A:** Kiểm tra:
1. PostgreSQL có đang chạy không?
2. Database `pets_platform_dev` đã tồn tại chưa?
3. Username/password đúng không?
4. Đã set đúng profile chưa?
