# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build
mvn clean package -DskipTests

# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=BloodPlatformModulithTests

# Run a single test method
mvn test -Dtest=BloodPlatformModulithTests#moduleStructureIsValid

# Start the application
mvn spring-boot:run
```

> Tests require no running infrastructure — they use H2 in-memory with Flyway disabled (see `application-test.yml`).

## Architecture

This is a **modular monolith** using Spring Modulith 1.2.4. All code lives in one deployable JAR but is split into four strictly-bounded modules under `com.blood`:

| Module | Package | Responsibility |
|---|---|---|
| `auth` | `com.blood.auth` | JWT auth, user registration/login, account lockout, admin user management |
| `donor` | `com.blood.donor` | Donor registration and listing |
| `hospital` | `com.blood.hospital` | Hospital registration, deactivation, status management |
| `inventory` | `com.blood.inventory` | Blood stock management per hospital, audit log |
| `notification` | `com.blood.notification` | Event-driven notifications |
| `search` | `com.blood.search` | Cross-hospital blood availability search |
| `transfer` | `com.blood.transfer` | Blood transfer workflow saga (request → approve → complete) |

### Module Boundaries (enforced by Spring Modulith)

Modules may only depend on each other through two mechanisms:

1. **Named interfaces** — `hospital::service` (used by `auth`, `inventory`, `transfer`), `inventory::search` (used by `search`), `inventory::transfer` (used by `transfer`), `transfer::events` (used by `notification`).
2. **Events** — `donor` exposes an `events` named interface; `notification` listens to `DonorRegisteredEvent` via `@ApplicationModuleListener`.

Direct cross-package imports outside these interfaces will break the `moduleStructureIsValid` test. Each module's allowed dependencies are declared in its `package-info.java`.

### Event Flow

```
DonorService  →  publishes DonorRegisteredEvent
                      ↓
               DonorRegisteredListener (notification module)
                      ↓
               creates Notification record
```

Other published events (`UserRegisteredEvent`, `AdminAuditEvent`, `HospitalRegisteredEvent`, `HospitalDeactivatedEvent`) are defined but have no listeners yet.

### Security Model

- JWT tokens are issued on login and validated by `JwtAuthenticationFilter` on every request (`Authorization: Bearer <token>`).
- Token claims include: `role`, `userId`, `name`. Expiry defaults to 24 hours.
- Public endpoints: `POST /api/auth/register`, `POST /api/auth/login`, `GET /actuator/health`.
- Admin-only endpoints (require `ROLE_ADMIN`): hospital create/deactivate, user approval/deactivation/role change.
- All other endpoints require authentication.
- Account lockout is tracked via `failedAttempts` and `lockedUntil` fields on `User`; managed by `AccountLockoutService`.

### Database

- PostgreSQL 16, database `blooddb`. Schema is managed exclusively by Flyway — `ddl-auto` is set to `validate`.
- Migrations live in `src/main/resources/db/migration/` (V1–V6). Always add new schema changes as a new `V{n}__description.sql` file; never edit existing migrations.
- JPA auditing (`@EnableJpaAuditing`) populates `createdAt`/`updatedAt` on entities automatically.

### Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/blooddb` | JDBC URL |
| `DB_USER` | `postgres` | DB username |
| `DB_PASSWORD` | `postgres` | DB password |
| `JWT_SECRET` | (hex string in yml) | HMAC-SHA256 signing key |
| `JWT_EXPIRATION_MS` | `86400000` | Token lifetime in ms |

### Running with Docker

```bash
docker compose up --build
```

The compose file starts PostgreSQL first (with a healthcheck) then the app. The app container waits for `service_healthy` before starting.
