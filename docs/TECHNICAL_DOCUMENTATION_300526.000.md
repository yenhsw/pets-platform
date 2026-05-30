# TÀI LIỆU KỸ THUẬT - PETS PLATFORM
## Ngày tạo: 30/05/2026
## Phiên bản: 1.0

---

# MỤC LỤC
1. [Tổng quan kiến trúc](#1-tổng-quan-kiến-trúc)
2. [Cấu trúc Package Enterprise](#2-cấu-trúc-package-enterprise)
3. [Dependencies Configuration](#3-dependencies-configuration)
4. [Application Configuration](#4-application-configuration)
5. [Liên kết giữa các thành phần](#5-liên-kết-giữa-các-thành-phần)
6. [Cách sử dụng](#6-cách-sử-dụng)

---

# 1. TỔNG QUAN KIẾN TRÚC

## 1.1 Thông tin Project

| Thông tin | Giá trị |
|-----------|---------|
| Tên project | pets-platform |
| Package gốc | com.petsplatform |
| Spring Boot | 3.5.14 |
| Java Version | 21 |
| Build Tool | Maven |
| Database | PostgreSQL |

## 1.2 Kiến trúc áp dụng

**Modular Monolith Architecture**

```
┌─────────────────────────────────────────────────────────────┐
│                    APPLICATION LAYER                        │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐       │
│  │ Common  │  │  Config │  │ Modules │  │ Shared  │       │
│  └─────────┘  └─────────┘  └─────────┘  └─────────┘       │
├─────────────────────────────────────────────────────────────┤
│                    INFRASTRUCTURE LAYER                     │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐  ┌─────────┐  ┌─────────┐                    │
│  │Database │  │  Cache  │  │External │                    │
│  └─────────┘  └─────────┘  └─────────┘                    │
└─────────────────────────────────────────────────────────────┘
```

---

# 2. CẤU TRÚC PACKAGE ENTERPRISE

## 2.1 Sơ đồ Package

```
src/main/java/com/petsplatform/
│
├── common/                      # Thành phần chia sẻ ở mức application-wide
│   ├── constants/               # Hằng số ứng dụng
│   ├── enums/                  # Enum dùng chung
│   └── utils/                  # Utility classes
│
├── config/                      # Spring Configuration Classes
│   └── ConfigPackage.java
│
├── infrastructure/              # Thành phần hạ tầng kỹ thuật
│   ├── database/               # JPA entities, repositories, migrations
│   ├── cache/                  # Caching configuration
│   └── external/               # External API clients
│
├── shared/                     # Cross-cutting concerns
│   ├── response/              # API response wrapper
│   ├── exception/             # Exception handlers
│   ├── logging/              # Logging utilities
│   ├── validation/           # Custom validators
│   ├── pagination/           # Pagination utilities
│   ├── auditing/             # Audit trail support
│   ├── base/                 # Base classes (Entity, Repository, Service, Controller)
│   └── SharedPackage.java
│
├── modules/                    # Business modules (tương lai)
│   ├── domain/               # Entities, Value Objects
│   ├── application/         # Use Cases, DTOs
│   ├── infrastructure/      # Repositories, Adapters
│   └── api/                 # REST Controllers
│
└── PetsPlatformApplication.java
```

## 2.2 Giải thích chi tiết từng Package

### 2.2.1 COMMON PACKAGE
**Vị trí:** `src/main/java/com/petsplatform/common/`

| Package | Mục đích | Ví dụ |
|---------|----------|-------|
| `constants/` | Hằng số ứng dụng | Message keys, Error codes, Config keys |
| `enums/` | Enum dùng chung | Status enum, Type enum |
| `utils/` | Utility classes | DateUtils, StringUtils, ValidationUtils |

### 2.2.2 CONFIG PACKAGE
**Vị trí:** `src/main/java/com/petsplatform/config/`

**Mục đích:** Chứa các Spring Configuration classes

**Ví dụ sử dụng:**
```java
@Configuration
public class WebMvcConfig { }
```

### 2.2.3 INFRASTRUCTURE PACKAGE
**Vị trí:** `src/main/java/com/petsplatform/infrastructure/`

| Package | Mục đích |
|---------|----------|
| `database/` | JPA entities, Spring Data repositories, DB migrations |
| `cache/` | Redis, EhCache configuration |
| `external/` | REST clients, SOAP clients, third-party API adapters |

### 2.2.4 SHARED PACKAGE
**Vị trí:** `src/main/java/com/petsplatform/shared/`

| Package | Mục đích | Chi tiết |
|---------|----------|----------|
| `response/` | API response wrapper | ApiResponse<T>, PageResponse<T> |
| `exception/` | Exception handling | GlobalExceptionHandler, BusinessException |
| `logging/` | Logging utilities | AuditLogger, PerformanceLogger |
| `validation/` | Custom validators | @PhoneNumber, @StrongPassword |
| `pagination/` | Pagination utilities | PagedResult, PageRequest |
| `auditing/` | Audit trail | @CreatedBy, @LastModifiedBy |
| `base/` | Base classes | BaseEntity, BaseRepository, BaseService, BaseController |

### 2.2.5 MODULES PACKAGE
**Vị trí:** `src/main/java/com/petsplatform/modules/`

**Mục đích:** Chứa các business modules độc lập

**Cấu trúc mỗi module:**
```
modules/{module-name}/
├── domain/           # Business logic
│   ├── entity/       # JPA Entities
│   ├── vo/          # Value Objects
│   └── event/       # Domain Events
├── application/      # Application layer
│   ├── dto/         # Data Transfer Objects
│   ├── service/     # Application Services
│   └── usecase/     # Use Cases
├── infrastructure/   # Infrastructure layer
│   ├── repository/  # Repository implementations
│   └── adapter/     # External adapters
└── api/             # API layer
    ├── controller/  # REST Controllers
    └── dto/         # Request/Response DTOs
```

---

# 3. DEPENDENCIES CONFIGURATION

## 3.1 File cấu hình
**Vị trí:** `pom.xml`

## 3.2 Dependencies đã thêm

### 3.2.1 Spring Boot Starter Web
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```
**Mục đích:** REST API, MVC pattern, Tomcat embedded server

### 3.2.2 Spring Data JPA
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```
**Mục đích:** JPA/Hibernate ORM, Repository pattern, Transaction management

### 3.2.3 PostgreSQL Driver
```xml
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
    <scope>runtime</scope>
</dependency>
```
**Mục đích:** JDBC driver cho PostgreSQL database

### 3.2.4 Validation
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>
```
**Mục đích:** Bean Validation (@Valid, @NotNull, @NotBlank, @Email...)

### 3.2.5 Lombok
```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <optional>true</optional>
</dependency>
```
**Mục đích:** Giảm boilerplate code (@Data, @Getter, @Setter, @Builder...)

### 3.2.6 Spring Boot Test
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```
**Mục đích:** Testing framework

---

# 4. APPLICATION CONFIGURATION

## 4.1 Cấu trúc File Configuration

```
src/main/resources/
├── application.yml              # Base configuration (luôn load)
├── application-dev.yml          # Development profile
├── application-staging.yml      # Staging profile
└── application-prod.yml         # Production profile
```

## 4.2 application.yml (Base Configuration)

**Vị trí:** `src/main/resources/application.yml`

```yaml
spring:
  application:
    name: pets-platform          # Tên ứng dụng
  profiles:
    active: default              # Profile mặc định
```

### Các thành phần chính:

#### 4.2.1 Server Configuration
```yaml
server:
  port: 8080                     # Port chạy ứng dụng
  servlet:
    context-path: /api           # Context path cho API
  compression:
    enabled: true               # Nén HTTP response
```

#### 4.2.2 JPA/Hibernate Configuration
```yaml
spring:
  jpa:
    open-in-view: false          # Tránh LazyInitializationException
    hibernate:
      ddl-auto: validate         # Kiểm tra schema (không auto tạo)
    show-sql: false             # Không hiển thị SQL
    defer-datasource-initialization: true
    properties:
      hibernate:
        format_sql: true         # Format SQL đẹp
        use_sql_comments: false  # Không comment SQL
        jdbc:
          batch_size: 25         # Batch size cho insert/update
          batch_versioned_data: true
        order_inserts: true      # Sắp xếp insert theo thứ tự
        order_updates: true      # Sắp xếp update theo thứ tự
        default_batch_fetch_size: 25  # Giảm N+1 query
```

#### 4.2.3 Logging Configuration
```yaml
logging:
  level:
    root: INFO                   # Level cho root logger
    com.petsplatform: INFO       # Level cho package petsplatform
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/pets-platform.log # Đường dẫn file log
```

## 4.3 application-dev.yml (Development Profile)

**Vị trí:** `src/main/resources/application-dev.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/pets_platform_dev
    username: postgres
    password: 123456
    driver-class-name: org.postgresql.Driver
    hikari:
      maximum-pool-size: 10
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000

  jpa:
    hibernate:
      ddl-auto: update           # Auto update schema (dev only)
    show-sql: true               # Hiển thị SQL

logging:
  level:
    root: DEBUG
    com.petsplatform: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.springframework.web: DEBUG
```

## 4.4 application-staging.yml (Staging Profile)

**Vị trí:** `src/main/resources/application-staging.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://staging-db.example.com:5432/pets_platform_staging
    username: ${DB_USERNAME}      # Từ environment variable
    password: ${DB_PASSWORD}      # Từ environment variable
    hikari:
      maximum-pool-size: 20      # Pool lớn hơn dev
      minimum-idle: 10

  jpa:
    hibernate:
      ddl-auto: validate         # Chỉ validate, không auto
    show-sql: false              # Không hiển thị SQL
```

## 4.5 application-prod.yml (Production Profile)

**Vị trí:** `src/main/resources/application-prod.yml`

```yaml
spring:
  datasource:
    url: jdbc:postgresql://prod-db.example.com:5432/pets_platform_prod
    username: ${DB_USERNAME}      # Environment variable
    password: ${DB_PASSWORD}      # Environment variable
    hikari:
      maximum-pool-size: 50      # Pool lớn nhất
      minimum-idle: 10

  jpa:
    hibernate:
      ddl-auto: validate         # Nghiêm ngặt nhất

server:
  compression:
    enabled: true               # Nén response

logging:
  level:
    root: WARN
    com.petsplatform: INFO
  file:
    name: /var/log/pets-platform/application.log
```

---

# 5. LIÊN KẾT GIỮA CÁC THÀNH PHẦN

## 5.1 Sơ đồ liên kết

```
┌─────────────────────────────────────────────────────────────────┐
│                        pom.xml                                   │
│  ┌─────────────┬─────────────┬─────────────┬─────────────┐     │
│  │spring-boot- │spring-boot- │ postgresql  │ lombok      │     │
│  │starter-web  │data-jpa     │             │             │     │
│  └──────┬──────┴──────┬──────┴──────┬──────┴──────┬──────┘     │
└─────────┼────────────┼─────────────┼─────────────┼────────────┘
          │            │             │             │
          ▼            ▼             ▼             │
┌─────────────────────────────────────────────────────────────────┐
│                   application.yml                                │
│  ┌─────────────────────────────────────────────────────────┐    │
│  │ spring:                                                  │    │
│  │   jpa: { hibernate.properties }                        │    │
│  │   datasource: { url, username, password }              │    │
│  │   profiles: { active: default }                        │    │
│  └─────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────┘
          │
          ▼
┌─────────────────────────────────────────────────────────────────┐
│              Spring Boot Application Context                     │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐          │
│  │ DataSource   │  │ EntityManager│  │ Web Server   │          │
│  │ (HikariCP)   │  │ (Hibernate)  │  │ (Tomcat)     │          │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘          │
│         │                 │                  │                   │
└─────────┼─────────────────┼──────────────────┼──────────────────┘
          │                 │                  │
          ▼                 ▼                  ▼
┌─────────────────────────────────────────────────────────────────┐
│              Package Structure (com.petsplatform)                │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │ common/  │  │ config/  │  │infrastructure│ │ shared/  │        │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘        │
└─────────────────────────────────────────────────────────────────┘
```

## 5.2 Luồng hoạt động

### 5.2.1 Khi ứng dụng khởi động

```
1. Maven build → Load pom.xml
                    │
                    ▼
2. Spring Boot auto-configuration
                    │
                    ▼
3. Load application.yml (base config)
                    │
                    ▼
4. Load application-{profile}.yml (nếu có profile active)
                    │
                    ▼
5. Tạo DataSource từ datasource config
                    │
                    ▼
6. Khởi tạo JPA/Hibernate EntityManager
                    │
                    ▼
7. Scan packages → Load beans
                    │
                    ▼
8. Start embedded Tomcat server
```

### 5.2.2 Khi có request API

```
1. Request → Tomcat (port 8080)
                  │
                  ▼
2. DispatcherServlet (Spring MVC)
                  │
                  ▼
3. Controller (@RestController)
                  │
                  ▼
4. Service (@Service)
                  │
                  ▼
5. Repository (JPA)
                  │
                  ▼
6. Database (PostgreSQL)
```

## 5.3 Liên kết giữa các lớp

### Controller → Service → Repository → Entity → Database

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Controller  │────▶│  Service    │────▶│ Repository   │
│ (API Layer) │     │ (Business)   │     │ (Data Access)│
└─────────────┘     └─────────────┘     └──────┬──────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │   Entity    │
                                        │ (JPA Model) │
                                        └──────┬──────┘
                                               │
                                               ▼
                                        ┌─────────────┐
                                        │  Database   │
                                        │(PostgreSQL) │
                                        └─────────────┘
```

---

# 6. CÁCH SỬ DỤNG

## 6.1 Chạy ứng dụng

### Development
```bash
# Sử dụng profile dev (có debug logging)
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Production
```bash
# Sử dụng profile prod
mvn spring-boot:run -Dspring.profiles.active=prod
```

## 6.2 Active profile trong code

**Trong application.yml:**
```yaml
spring:
  profiles:
    active: dev  # Đổi thành default nếu muốn tắt debug
```

## 6.3 Reload Maven dependencies

**IntelliJ IDEA:**
- Maven Tool Window → Click Reload All Maven Projects

**Terminal:**
```bash
mvn clean compile
```

## 6.4 Verify project

```bash
# Build và test
mvn clean verify

# Chạy ứng dụng
mvn spring-boot:run

# Chạy unit test
mvn test
```

## 6.5 Kiểm tra dependencies

```bash
mvn dependency:tree
```

---

# PHỤ LỤC

## A. HikariCP Configuration

| Property | Dev | Staging | Prod |
|----------|-----|---------|------|
| maximum-pool-size | 10 | 20 | 50 |
| minimum-idle | 5 | 10 | 10 |
| connection-timeout | 30000 | 30000 | 30000 |
| idle-timeout | 600000 | 600000 | 600000 |
| max-lifetime | 1800000 | 1800000 | 1800000 |

## B. JPA Best Practices đã áp dụng

1. **open-in-view: false** - Tránh LazyInitializationException
2. **ddl-auto: validate** (prod) - Chỉ validate schema
3. **batch_size: 25** - Batch insert/update performance
4. **order_inserts/order_updates: true** - Optimize batch operations
5. **default_batch_fetch_size: 25** - Giảm N+1 query

## C. Profile Usage Matrix

| Profile | Mục đích | Debug | SQL Log | Auto DDL |
|---------|----------|-------|---------|----------|
| default | Chạy bình thường | OFF | OFF | OFF |
| dev | Development | ON | ON | ON (update) |
| staging | Pre-production | OFF | OFF | OFF |
| prod | Production | OFF | OFF | OFF |

---

# TÀI LIỆU LIÊN QUAN

1. Spring Boot Documentation: https://docs.spring.io/spring-boot/docs/
2. Spring Data JPA: https://docs.spring.io/spring-data/jpa/docs/
3. Hibernate Documentation: https://docs.jboss.org/hibernate/orm/

---

**Người tạo:** Senior Java Enterprise Architect
**Ngày cập nhật:** 30/05/2026
**Phiên bản:** 1.0
