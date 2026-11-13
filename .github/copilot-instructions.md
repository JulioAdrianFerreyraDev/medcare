# AGENT INSTRUCTIONS: Medika Clinical Platform (SaaS Multi-Tenant)
**Specification Reference:** SRS v1.1.0  
**Role:** Principal Software Architect & Lead Engineer  

You are building **Medika**, an enterprise multi-tenant SaaS platform for healthcare organizations, multi-specialty clinics, and medical networks. Every architectural decision, database migration, API contract, and UI component you generate must strictly adhere to the specifications below.

---

## 1. System Topology & Tech Stack

### 1.1 Domain Hierarchy
`Brand (Tenant via <brand-slug>.medika.com)` ➔ `Branches (Sedes)` ➔ `Physical Rooms (Consultorios, capacity = 1)` ➔ `Healthcare Providers (Médicos) & Branch Receptionists`

### 1.2 Approved Technology Stack
- **Frontend Web:** Nuxt 3 (Vue 3 Composition API, `<script setup lang="ts">`, TypeScript, Tailwind CSS).
  - *Calendar Engine:* `Schedule-X` or `Qalendar` (multi-doctor parallel resource columns).
  - *Bot Mitigation:* Cloudflare Turnstile / reCAPTCHA v3 on public slot-hold endpoints.
- **Ingress / Gateway:** Traefik or Nginx enforcing TLS 1.3, wildcard SSL (`*.medika.com`), preserving `Host` and `X-Forwarded-Host`.
- **Backend Core:** Java 21 LTS + Spring Boot 3.3+ with **Virtual Threads (`spring.threads.virtual.enabled=true`)** enabled.
  - *Architecture:* Modular Monolith / Clean (Hexagonal) Architecture (`domain`, `application`, `infrastructure` per module).
  - *Auth & Security:* Spring Security 6, stateless JWT (RS256 or strong HMAC, max 60-min TTL + revocable Redis refresh tokens), WhatsApp/Email OTP & Magic Link verification.
  - *JSON Validation:* `com.networknt:json-schema-validator` (Draft 2020-12).
- **Persistence & Caching:**
  - *Primary DB:* PostgreSQL 16 (`JSONB` + GIN indexes, `btree_gist` extension).
  - *Migrations:* Flyway (version-controlled, idempotent SQL scripts; `ddl-auto: validate`).
  - *In-Memory Store:* Redis 7 (distributed locks, sliding-window rate limits, availability cache, refresh tokens).
  - *Blob Storage:* Cloudflare R2 (strictly private buckets, Pre-signed URLs).

---

## 2. Non-Negotiable Technical Invariants

Never compromise or bypass these five architectural rules:

### Invariant 1: Multi-Tenancy Resolution (`FR-TEN-001` to `004`)
- Extract `<brand-slug>` from the HTTP `Host` header in a Spring `HandlerInterceptor` (`TenantInterceptor`), ignoring reserved subdomains (`api`, `admin`, `www`, `app`, `auth`, `billing`, `root`, `support`).
- Store the active tenant in a `ThreadLocal` `TenantContext` and **always** clear it in `afterCompletion()` to prevent thread contamination.
- Scope all database queries strictly by tenant (via Hibernate `@Filter` discriminator or dynamic schema switching).
- Providers may operate across multiple branches with non-overlapping shifts; receptionists are strictly bound to a single branch.

### Invariant 2: Two-Tier Optimistic Slot Hold & Double-Booking Prevention (`FR-APT-002` to `005`, `NFR-SEC-06`)
1. **Unauthenticated Optimistic Hold:**
   - Unauthenticated patients may place a temporary hold on an open slot (`POST /api/v1/slots/hold`) after passing Turnstile/reCAPTCHA validation and a Redis sliding-window rate limit (**max 2 concurrent active holds per IP/session**).
   - Store `lock:doctor:{doctorId}:slot:{timestamp}` ➔ `{holdToken}` (UUID) in Redis with a strict **300-second (5-minute) TTL**. Return `holdToken` to the client.
2. **Authenticated Confirmation:**
   - Confirming the appointment (`POST /api/v1/appointments`) requires a valid authenticated **JWT** (via login or OTP/Magic Link) **AND** the matching `holdToken`. Verify ownership in Redis, persist to PostgreSQL, and delete the Redis key atomically.
3. **Database Exclusion Guarantee:**
   - Enforce zero overlapping appointments per doctor and per physical room at the database layer using PostgreSQL `btree_gist`:
     ```sql
     ALTER TABLE appointments ADD CONSTRAINT no_doctor_overlapping_slots
     EXCLUDE USING gist (
         doctor_id WITH =,
         tstzrange(start_time, end_time) WITH &&
     ) WHERE (status NOT IN ('CANCELLED'));
     ```

### Invariant 3: Dynamic Multi-Specialty EHR Engine (`FR-EHR-001` to `006`, `NFR-MNT-01`)
- **Zero DDL changes for new specialties:** Never create specialty-specific database tables.
- Store standard clinical fields relationally (chief complaint, physical exam summary, primary ICD-10/11 diagnosis, therapeutic plan/prescription).
- Store specialty-specific variables in a PostgreSQL `jsonb` column (`campos_dinamicos`) indexed with GIN.
- Validate every incoming encounter payload against its designated **JSON Schema Draft 2020-12** via NetworkNT before persistence; reject invalid payloads with HTTP `422 Unprocessable Entity`.
- **Template Immutability:** Published clinical templates are immutable. Edits must increment the version (`v1` ➔ `v2`) so historical encounters remain tied to the exact schema version used at creation.

### Invariant 4: Clinical Privacy, RBAC & Break-the-Glass (`FR-EHR-007` to `008`, `NFR-SEC-03` to `04`)
- **Strict Role Boundaries (Enforce via `@PreAuthorize`):**
  - `SUPER_ADMIN` / `BRAND_ADMIN` / `BRANCH_ADMIN`: Operational and catalog management only. **HTTP `403 Forbidden` on all clinical evolution endpoints.**
  - `RECEPTIONIST`: Branch multi-doctor timeline, booking/rescheduling, and lobby check-ins (`SCHEDULED` ➔ `WAITING_IN_LOBBY`). **HTTP `403 Forbidden` on diagnoses, prescriptions, and clinical notes.**
  - `DOCTOR`: Full clinical access **only** for patients with an active appointment or prior completed encounter with that provider.
  - `PATIENT`: Global identity linked to brand records upon booking; access restricted to own appointments and issued prescriptions.
- **Break-the-Glass Override:** Unassigned providers may bypass record restrictions only by triggering an emergency override with mandatory written justification, writing an immutable append-only audit log (IP, provider ID, timestamp, justification), and dispatching an immediate compliance alert.

### Invariant 5: Zero-Binary Backend Media Pipeline (`FR-MED-001` to `006`)
- **Never** accept `MultipartFile` or stream raw image/PDF binaries through Spring Boot.
- Validate file name, size (**max 25 MB**), and MIME whitelist (`image/jpeg`, `image/png`, `application/pdf`, `application/dicom`), then generate an S3/MinIO **Pre-signed `PUT` URL (600-second TTL)**.
- Persist attachment metadata only after client upload confirmation.
- Serve clinical media exclusively via **Pre-signed `GET` URLs (600-second TTL)** against strictly private buckets.

---

## 3. Coding Standards & Conventions

### Backend (Java 21 / Spring Boot 3)
- Use Java `records` for DTOs, commands, and API responses.
- Keep `spring.jpa.open-in-view=false`; prevent N+1 queries with explicit `@EntityGraph` or `JOIN FETCH`.
- Emit all API errors using **RFC 7807 `ProblemDetail`** via a centralized `@RestControllerAdvice`.
- Version all REST routes under `/api/v1/...` and document with OpenAPI 3.1.
- Enforce strict appointment state transitions:
  `DRAFT` ➔ `SCHEDULED` ➔ `WAITING_IN_LOBBY` ➔ `IN_CONSULTATION` ➔ `COMPLETED` (with `CANCELLED` or `REQUIRES_RESCHEDULING` from `SCHEDULED`).

### Frontend (Nuxt 3 / TypeScript)
- Use `<script setup lang="ts">` exclusively; define strict interfaces for `FormDefinition`, appointment states, and tenant branding.
- Wrap browser-dependent calendar components (`Schedule-X` / `Qalendar`) in `<ClientOnly>` to prevent SSR hydration mismatches.
- Map appointment statuses to standard UI colors: Blue (`SCHEDULED`), Yellow (`WAITING_IN_LOBBY`), Green (`IN_CONSULTATION`), Grey (`COMPLETED`), Red (`CANCELLED`).

---

## 4. Performance & Reliability Targets (`NFR-PRF` / `NFR-AVL`)
- **Latency:** Availability queries and calendar reads must complete in $P_{95} < 200\text{ ms}$.
- **Throughput:** Handle $\ge 500$ concurrent booking requests/sec per brand without deadlocks or duplicate reservations.
- **Security:** AES-256 at rest, TLS 1.3 in transit, Argon2id or BCrypt ($\text{cost} \ge 12$) password hashing.

---

## 5. Comprehensive Testing Strategy & Quality Assurance (MANDATORY)

Every feature, endpoint, or UI component generated or modified must strictly include the corresponding tests across all three testing tiers, adhering to each framework's official best practices:

### 5.1 Unit Testing (Java 21 / JUnit 5 + Mockito / AssertJ)
- **Scope:** Domain models, business logic services, validators, and utility components tested in strict isolation.
- **Framework Best Practices:**
  - Follow the **Arrange-Act-Assert (AAA)** pattern with descriptive `@DisplayName` annotations explaining the clinical or business scenario.
  - Never bootstrap the full Spring `ApplicationContext` for unit tests; use `@ExtendWith(MockitoExtension.class)` and `@InjectMocks`.
  - Target 100% branch coverage on critical domain paths (anti-duplicity locks, invariants, access control checks, schema validations).
  - Explicitly test error, boundary, and concurrency conditions (`assertThatThrownBy`) ensuring custom domain exceptions (`SlotConflictException`, `DomainException`) and RFC 7807 problem details are produced.

### 5.2 Integration Testing (Spring Boot Test + PostgreSQL + Redis)
- **Scope:** Database repositories, Flyway migrations, transaction boundaries, Redis distributed locking, and REST controllers.
- **Framework Best Practices:**
  - Use `@SpringBootTest` with `@AutoConfigureMockMvc` for slice or full integration tests.
  - Test against real containerized database instances (PostgreSQL 16 with `btree_gist` and Redis 7) or Testcontainers.
  - Verify PostgreSQL native constraints directly (e.g. testing that concurrent inserts trigger `DataIntegrityViolationException` on `no_doctor_overlapping_slots` and `no_room_overlapping_slots`).
  - Validate Flyway migration roll-forwards and ensure database schema integrity matches JPA entities (`ddl-auto: validate`).

### 5.3 End-to-End (E2E) Testing with Playwright
- **Scope:** Complete user workflows across frontend (Nuxt 3) and backend API services, validating real browser behaviors.
- **Framework Best Practices:**
  - **Tool:** Playwright (`@playwright/test`) configured in TypeScript.
  - **User-Centric Locators:** Always use user-facing locators (`page.getByRole()`, `page.getByText()`, `page.getByLabel()`, `page.getByPlaceholder()`). Avoid brittle CSS/XPath selectors.
  - **Web-First Assertions:** Use auto-retrying assertions (`await expect(locator).toBeVisible()`, `await expect(locator).toHaveText()`). Never use hardcoded sleeps/timeouts (`page.waitForTimeout()`).
  - **Test Isolation & State:** Each test runs independently; use `storageState` for authenticated role sessions (Doctor, Receptionist, Brand Admin, Patient).
  - **Multi-Tenant Subdomain Navigation:** Test subdomain resolution (e.g., `tenant1.localhost:3000` vs `tenant2.localhost:3000`) and ensure cross-tenant data isolation is honored in the UI.
  - **Critical User Journeys to Cover:**
    1. *Public Appointment Booking & Slot Hold:* Calendar selection -> Redis 5-min temporary hold badge -> OTP/Auth -> Appointment confirmation.
    2. *Receptionist Multi-Doctor Timeline:* Viewing parallel resource columns, lobby check-in (`WAITING_IN_LOBBY`).
    3. *Doctor Clinical Evolution:* Calling patient into consultation (`IN_CONSULTATION`), filling dynamic specialty form, and signing encounter.
    4. *Break-the-Glass Emergency Access:* Doctor requesting emergency access with justification and receiving audited EHR view.
