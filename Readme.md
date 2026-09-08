# Software Design Document

## Subscription & Billing Fraud / Churn-Abuse Detector

**JFSD Team Project**

| Field | Value |
|-------|-------|
| Document Type | Software Design Document (SDD) |
| Project | Subscription & Billing Fraud / Churn-Abuse Detector |
| Version | 1.0 |
| Status | Draft for team review |
| Date | September 8, 2026 |
| Duration | 3 weeks (1-2 hours/day, 5 days/week) |

---

## 1. Introduction

### 1.1 Purpose

This document defines the technical design for the Subscription & Billing Fraud Detector system. It outlines architecture, component responsibilities, database design, interfaces, and deployment decisions to enable parallel development.

### 1.2 Scope

**In Scope**
- Detection of three abuse patterns: free-trial abuse, promo-code stacking, chargeback-after-cancellation
- Rules-based, explainable scoring engine with tunable weights
- Event-driven microservices over Kafka
- Case management workflow with human-in-the-loop review
- React ops dashboard with JWT authentication
- Local Docker Compose setup and AWS deployment

**Out of Scope**
- Real payment processing
- Real customer PII (synthetic data only)
- Machine learning models
- Kubernetes orchestration
- Multi-region deployment
- Mobile or end-customer UI

### 1.3 Key Terms

| Term | Definition |
|------|-----------|
| Abuse Signal | A single observed fact contributing to risk (e.g., card used on 3 accounts) |
| Risk Score | Integer 0-100 from summing rule weights |
| Case | Review record created when score crosses review threshold |
| Card Fingerprint | SHA-256 hash of card identifier (no raw card stored) |
| Device Fingerprint | Hash of browser/device attributes |
| Freeze | Reversible account restriction (login allowed, billing blocked) |
| Ban | Terminal account restriction (login blocked, permanent) |
| Velocity Counter | Redis key counting occurrences in a time window |

---

## 2. System Overview

### 2.1 What It Does

The system observes subscription lifecycle events (signups, subscriptions, cancellations, chargebacks) and evaluates each against abuse rules. Every evaluation produces a risk score and human-readable reasons.

**Three-Tier Response**

| Score | Response |
|-------|----------|
| 0-39 (Low) | No action — signals logged for audit only |
| 40-79 (Medium) | Case created for analyst review — account stays active |
| 80-100 (High) | Account auto-frozen immediately — case logged for confirmation |

Auto-freeze is reversible. Only a human can issue a permanent ban.

### 2.2 Design Goals

- **Explainability** — Every score shows the exact rules that fired and points each contributed
- **Tunable without redeploy** — Rule weights live in a config table, cached in Redis
- **Parallel buildability** — Five services with disjoint ownership
- **Human final say** — No permanent, irreversible action taken automatically
- **Auditability** — Every decision is logged with actor, timestamp, and reason
- **Demoability** — Flag → review → freeze cycle in under two minutes

### 2.3 Architecture Summary

Event-driven microservices on Spring Boot + Kafka. Services communicate asynchronously over Kafka for domain events and synchronously over REST for dashboard reads and analyst commands. The scoring service uses Spring WebFlux (reactive) for high throughput on Redis lookups.

---

## 3. System Architecture

### 3.1 Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND TIER                           │
│                                                                 │
│                    React Ops Dashboard                          │
│              (Case Queue, Case Detail, Audit Log)              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             │ HTTPS / JWT Token
                             │
┌────────────────────────────▼────────────────────────────────────┐
│                      GATEWAY & AUTH TIER                        │
│                                                                 │
│    Spring Cloud Gateway + JWT Auth                             │
│    (Routing, Token validation, Rate limiting)                  │
└────────────┬─────────────┬─────────────┬─────────────┬──────────┘
             │             │             │             │
    REST / JSON over HTTP/1.1
             │             │             │             │
    ┌────────▼──────┐  ┌──▼──────────┐  ┌──▼───────┐  ┌──▼────────────┐
    │   Signup &    │  │   Abuse     │  │Subscr. &│  │  Case &       │
    │  Onboarding   │  │   Scoring   │  │ Billing │  │  Action       │
    │   Service     │  │   Service   │  │Service  │  │  Service      │
    │               │  │ (WebFlux)   │  │         │  │               │
    └────────┬──────┘  └──┬──────────┘  └──┬─────┘  └──┬────────────┘
             │            │                │           │
             └────────────┼────────────────┼───────────┘
                          │
                    ┌─────▼─────┐
                    │   Kafka   │
                    │  Broker   │
                    │ (4 topics)│
                    └─────┬─────┘
                          │
        ┌─────────────┬───┴───────┬──────────────┐
        │             │           │              │
    ┌───▼────┐  ┌────▼────┐  ┌──▼─────┐  ┌────▼─────┐
    │   PG   │  │  Redis  │  │ MongoDB│  │ (DLQ)   │
    │   SQL  │  │Counters │  │Signals │  │        │
    │        │  │ & Rules │  │ Log    │  │        │
    └────────┘  └─────────┘  └────────┘  └────────┘
```

### 3.2 Technology Stack

| Component | Technology | Why |
|-----------|-----------|-----|
| Frontend | React 18, Axios | Lightweight, case queue is read-heavy |
| Gateway | Spring Cloud Gateway | Single ingress, JWT validation |
| Services | Java 17, Spring Boot 3.x | Team's primary stack |
| Scoring | Spring WebFlux | Async, I/O-bound on Redis calls |
| Messaging | Apache Kafka | Decouples producers from scorer |
| Primary DB | PostgreSQL 15 | Source of truth for entities, cases |
| Auth | JWT (HS256) | Stateless, simple |
| Containers | Docker, Docker Compose | One-command local bring-up |
| Cloud | AWS (ECS Fargate, RDS, EC2) | Managed compute, self-hosted data layer |

### 3.3 Event Topology

| Producer | Topic | Consumer | Carries |
|----------|-------|----------|---------|
| Signup Service | signup.events | Abuse Scoring | New account with fingerprints |
| Subscription Service | subscription.events | Abuse Scoring | Subscription, promo, chargeback events |
| Abuse Scoring | abuse.scored | Case & Action | Risk score, band, fired rules |
| Case & Action | account.actions | Signup, Subscription | Freeze, unfreeze, ban commands |

All topics partitioned by `accountId` for per-account ordering guarantee.

---

## 4. Component Design

### 4.1 Signup & Onboarding Service

**Responsibility:** Account creation, fingerprint capture, account status enforcement

**Endpoints:**
- `POST /api/signups` — Create account
- `GET /api/accounts/{id}` — Account detail
- `GET /api/accounts?fingerprint={hash}` — Sibling accounts

**Key Logic:**
- Hashes card and device on receipt, discards raw token
- Publishes `SignupEvent` to Kafka
- Account status machine: ACTIVE ↔ FROZEN → BANNED
- Uses transactional outbox pattern for guaranteed event publish

---

### 4.2 Abuse Scoring Service

**Responsibility:** Rule evaluation, velocity counting, score generation

**Endpoints:**
- `GET /api/scores/{accountId}` — Latest score and reasons
- `GET /api/rules` — Current rule config
- `PUT /api/rules/{ruleId}` — Update weight or enable/disable

**Scoring Rules (Tunable Weights)**

| Rule ID | Condition | Points | Pattern |
|---------|-----------|--------|---------|
| CARD_VELOCITY_24H | Same card on 3+ accounts in 24h | 40 | Free-trial |
| DEVICE_REUSE | Same device on 2+ accounts | 35 | Free-trial |
| IP_VELOCITY_1H | 5+ signups from 1 IP in 1h | 20 | Free-trial |
| PROMO_REUSE | Same promo by 3+ accounts | 30 | Stacking |
| CHARGEBACK_AFTER_CANCEL | Chargeback within 7 days of cancel | 50 | Chargeback |
| RAPID_CANCEL | Cancel within 24h of trial, twice | 20 | Trial cycle |

**Thresholds:**
- Review Threshold: 40 points
- Auto-Freeze Threshold: 80 points
- Max Score: 100

**Key Features:**
- Reactive (WebFlux) for non-blocking Redis calls
- Idempotent consumption via `eventId` dedup key
- Degraded mode if Redis unavailable
- Poison messages → DLQ after 3 retries

---

### 4.3 Subscription & Billing Service

**Responsibility:** Subscription lifecycle, promo redemption, simulated payments

**Endpoints:**
- `POST /api/subscriptions` — Start subscription
- `POST /api/subscriptions/{id}/cancel` — Cancel
- `POST /api/subscriptions/{id}/promo` — Apply promo code
- `POST /api/chargebacks` — File simulated dispute

**Key Logic:**
- Promo codes guarded by unique constraint (no double-apply at DB)
- Subscription status: TRIALING → ACTIVE → CANCELLED, SUSPENDED when account frozen
- Stacking is permitted at API level (scoring service detects abuse)

---

### 4.4 Case & Action Service

**Responsibility:** Convert scores into analyst work, execute analyst decisions

**Endpoints:**
- `GET /api/cases` — Paged queue by status, band, assignee
- `GET /api/cases/{id}` — Full case with sibling accounts and billing history
- `POST /api/cases/{id}/assign` — Claim case
- `POST /api/cases/{id}/decision` — DISMISS, FREEZE, BAN, UNFREEZE + note

**Key Logic:**
- HIGH-band events auto-freeze first, then open case
- Case dedup: multiple scores for same account append to existing case
- Optimistic locking prevents two analysts resolving same case concurrently
- Case status: OPEN → UNDER_REVIEW → RESOLVED (immutable)

---

### 4.5 Gateway + Auth + React Dashboard

**Gateway:**
- Validates JWT, injects analyst identity
- Rate limiting: 100 req/min per token
- Routes to five backend services

**Auth:**
- `POST /api/auth/login` — BCrypt-hashed credentials, HS256 JWT
- Access token: 30 minutes
- Refresh token: 7 days, stored server-side for revocation

**Roles:**
- ANALYST: view, assign, dismiss, freeze, unfreeze
- ADMIN: all analyst + ban, edit rules, view audit log

**Dashboard Screens:**
- **Case Queue:** Sortable table (account, score, band, reason, age, assignee) with 15-second auto-refresh
- **Case Detail:** Score breakdown bar per rule, sibling accounts, billing timeline, decision panel
- **Account View:** Status, truncated fingerprints, subscription/promo history
- **Rule Config (Admin):** Weight sliders, enable toggles, threshold fields
- **Audit Log:** Immutable, filterable list of all decisions

---

## 5. Database Design

### 5.1 PostgreSQL Schema

```
accounts
├─ id (PK)
├─ email (UNIQUE)
├─ card_fingerprint
├─ device_fingerprint
├─ ip_address
├─ status (ACTIVE | FROZEN | BANNED)
└─ created_at

subscriptions
├─ id (PK)
├─ account_id (FK → accounts)
├─ plan_code
├─ status (TRIALING | ACTIVE | CANCELLED | SUSPENDED)
├─ trial_ends_at
└─ cancelled_at

promo_codes
├─ id (PK)
├─ code (UNIQUE)
├─ discount_pct
├─ stackable (boolean)
└─ expires_at

promo_redemptions
├─ id (PK)
├─ promo_code_id (FK → promo_codes)
├─ account_id (FK → accounts)
├─ redeemed_at
└─ UNIQUE (promo_code_id, account_id)

payments
├─ id (PK)
├─ subscription_id (FK → subscriptions)
├─ amount_cents
├─ status (SUCCESS | FAILED)
└─ charged_at

chargebacks
├─ id (PK)
├─ payment_id (FK → payments)
├─ reason_code
└─ filed_at

cases
├─ id (PK)
├─ account_id (FK → accounts)
├─ risk_score (0-100)
├─ band (LOW | MEDIUM | HIGH)
├─ status (OPEN | UNDER_REVIEW | RESOLVED)
├─ auto_action_taken (FREEZE | NONE)
├─ score_reasons (JSONB)
├─ version (for optimistic locking)
└─ created_at

case_actions
├─ id (PK)
├─ case_id (FK → cases)
├─ ops_user_id (FK → ops_users)
├─ action (DISMISS | FREEZE | BAN | UNFREEZE)
├─ note
└─ acted_at

ops_users
├─ id (PK)
├─ email (UNIQUE)
├─ password_hash
├─ role (ANALYST | ADMIN)
└─ created_at

rule_config
├─ rule_id (PK)
├─ weight (int)
├─ enabled (boolean)
├─ description
└─ updated_at
```

**Key Constraints:**
- `accounts.email` UNIQUE → prevent duplicate signup
- `promo_redemptions (promo_code_id, account_id)` UNIQUE → prevent double-apply at DB
- `cases.version` for optimistic locking

**Indexes:**
- `accounts(card_fingerprint)` — sibling lookup
- `accounts(device_fingerprint)` — sibling lookup
- `cases(status, created_at DESC)` — queue query
- `cases(account_id) WHERE status != 'RESOLVED'` — case dedup on HIGH
- `payments(subscription_id, charged_at DESC)` — billing timeline

---

### 5.2 MongoDB Collection

**abuse_signals** — append-only signal log

```json
{
  "_id": ObjectId("..."),
  "eventId": "3f9c-...",
  "accountId": "a71e-...",
  "eventType": "SIGNUP",
  "score": 75,
  "band": "MEDIUM",
  "reasons": [
    {
      "ruleId": "CARD_VELOCITY_24H",
      "weight": 40,
      "detail": "card seen on 3 accounts in 24h"
    },
    {
      "ruleId": "DEVICE_REUSE",
      "weight": 35,
      "detail": "device shared with 2 other accounts"
    }
  ],
  "counterSnapshot": {
    "card24h": 3,
    "device": 3,
    "ip1h": 1
  },
  "scoredAt": "2026-09-08T14:22:31Z"
}
```

**Indexes:**
- `{accountId: 1, scoredAt: -1}` — account signal history
- `{band: 1, scoredAt: -1}` — high-band searches
- TTL index: 90 days on `scoredAt`

---

### 5.3 Redis Keys

| Key | Type | TTL | Purpose |
|-----|------|-----|---------|
| `vel:card:{hash}:24h` | Counter | 24h | Card reuse velocity |
| `vel:device:{hash}` | Counter | 7d | Device reuse across accounts |
| `vel:ip:{addr}:1h` | Counter | 1h | Signup burst from IP |
| `vel:promo:{code}:24h` | Counter | 24h | Promo redemption velocity |
| `dedupe:event:{eventId}` | String | 24h | Idempotent consumption guard |
| `cfg:rules` | Hash | 60s | Cached rule weights/thresholds |
| `set:card:{hash}` | Set | 7d | Sibling account IDs for case context |

---

## 6. External Interfaces

### 6.1 REST Endpoints

All endpoints behind `/api/`, protected with JWT.

**Authentication:**
- `POST /api/auth/login` — Email + password → access + refresh token

**Signup Service:**
- `POST /api/signups` — Create account
- `GET /api/accounts/{id}` — Account detail
- `GET /api/accounts?fingerprint={hash}` — Sibling accounts

**Scoring Service:**
- `GET /api/scores/{accountId}` — Latest score and reasons
- `GET /api/rules` — Current rule config
- `PUT /api/rules/{ruleId}` — Update weight or enable/disable

**Subscription Service:**
- `POST /api/subscriptions` — Start subscription
- `POST /api/subscriptions/{id}/cancel` — Cancel
- `POST /api/subscriptions/{id}/promo` — Apply promo code

**Case Service:**
- `GET /api/cases` — Paged queue
- `GET /api/cases/{id}` — Full case detail
- `POST /api/cases/{id}/assign` — Claim case
- `POST /api/cases/{id}/decision` — Submit decision (DISMISS/FREEZE/BAN/UNFREEZE)

### 6.2 Kafka Topics

| Topic | Partitions | Key | Producer | Consumer |
|-------|-----------|-----|----------|----------|
| signup.events | 3 | accountId | Signup | Abuse Scoring |
| subscription.events | 3 | accountId | Subscription | Abuse Scoring |
| abuse.scored | 3 | accountId | Abuse Scoring | Case & Action |
| account.actions | 3 | accountId | Case & Action | Signup, Subscription |
| abuse.scoring.dlq | 1 | eventId | Abuse Scoring | (manual) |

**Per-account ordering guaranteed by `accountId` partition key.**

---

## 7. Security

### 7.1 Authentication & Authorization

- **Method:** JWT (HS256), BCrypt hashing (cost 12)
- **Access token:** 30 minutes
- **Refresh token:** 7 days, server-side revocable
- **Roles:**
  - ANALYST: review, freeze, unfreeze
  - ADMIN: analyst + ban, edit rules

### 7.2 Data Protection

- **Card data:** No raw card number ever stored or logged — hashed on receipt
- **Fingerprints:** Salted SHA-256, one-way, truncated on display
- **Transit:** TLS/HTTPS at edge (CloudFront)
- **Logs:** Email and IP scrubbed at INFO level; full values in audit table only

### 7.3 Audit Trail

Every automated decision and every analyst action is logged:
- Actor (analyst ID or "SYSTEM")
- Timestamp
- Action (FREEZE, BAN, DISMISS, etc.)
- Reason or note
- Immutable

---

## 8. Performance & Scalability

### 8.1 Expected Load

| Metric | Target |
|--------|--------|
| Signup events | 50/sec sustained (burst) |
| Scoring latency (p95) | <200 ms (event → abuse.scored) |
| Dashboard queue load (p95) | <500 ms for 50 rows |
| Concurrent analysts | 5 |
| Total seeded accounts | ~10,000 |
| Open cases at any time | <500 |

### 8.2 Caching

- **Rule config:** Cached in Redis (60-second TTL) — tuning takes effect within a minute
- **Velocity counters:** The cache; no fallback query to PostgreSQL
- **Dashboard:** Client-side caching via TanStack Query (15-second refresh)
- **Sibling lookups:** Redis set instead of self-join

### 8.3 Database Optimization

- **Keyset pagination:** `WHERE created_at < :cursor` for case queue (avoids OFFSET cost)
- **Partial indexes:** `cases(account_id) WHERE status != 'RESOLVED'` for dedup check
- **Connection pooling:** HikariCP, pool size 10

### 8.4 Scaling Strategy

- **Services:** Horizontal by replica count; scoring service is highest-throughput
- **Datastores:** Vertical scaling first; no sharding at this scale
- **Known bottleneck:** Redis single instance for all counters (documented, not solved)

---

## 9. Deployment

### 9.1 Environments

| Environment | Purpose | Infrastructure |
|-------------|---------|-----------------|
| Local | Development | Docker Compose (all services + infrastructure) |
| CI | Automated tests | GitHub Actions + Testcontainers |
| Demo (AWS) | Evaluation | ECS Fargate, RDS, EC2-hosted Kafka |

### 9.2 Infrastructure

| Layer | AWS Resource | Contains |
|-------|-------------|----------|
| CDN | CloudFront + S3 | React production build |
| Load Balance | ALB | Routes to gateway task |
| Compute | ECS Fargate | All five services |
| Database | RDS PostgreSQL | Accounts, subscriptions, cases, rules |
| Messaging | EC2 | Kafka + Zookeeper |
| Cache | EC2 | MongoDB + Redis (co-located) |
| Secrets | AWS Secrets Manager | JWT key, DB credentials |

### 9.3 CI/CD Pipeline

```
Push to main
    ↓
Maven build + JUnit tests
    ↓
Integration tests (Testcontainers)
    ↓
Build Docker images (tagged with commit SHA)
    ↓
Push to ECR
    ↓
Deploy to ECS Fargate
    ↓
Health check /actuator/health
    ↓
Build React app → S3 → CloudFront invalidation
```

**Merge requirements:** Green build + 1 review

### 9.4 Containerization

- Multi-stage Docker: Maven build → eclipse-temurin:17-jre-alpine runtime
- Non-root user
- Health check: `/actuator/health`
- Docker Compose: `docker-compose.yml` (all services + infrastructure)
- Docker Compose: `docker-compose.infra.yml` (infrastructure only, for IDE dev)

---

## 10. Testing Strategy

### 10.1 Unit Tests

- **Framework:** JUnit 5, Mockito
- **Target:** 70%+ line coverage on service/domain packages
- **Coverage:** Each ScoringRule tested for fires, doesn't-fire, and boundary cases
- **Frontend:** React Testing Library on decision panel and score breakdown

### 10.2 Integration Tests

- **Framework:** Testcontainers (real Kafka, PostgreSQL, MongoDB, Redis)
- **Scenarios:**
  - Signup → event published → outbox drained
  - Scoring consumer → counters incremented → correct score emitted
  - Duplicate eventId → exactly one signal document
  - Case dedup: five HIGH events → one case with five appended signals
  - Optimistic locking: two concurrent decisions → one succeeds, one gets 409

### 10.3 End-to-End Tests

**Scenario 1: Trial Abuse**
1. Seed four signups sharing device fingerprint
2. Assert fourth is auto-frozen
3. Analyst reviews case, sees three sibling accounts
4. Analyst confirms freeze

**Scenario 2: Chargeback Abuse (False Positive)**
1. Subscribe → cancel → chargeback (3 days later)
2. Assert MEDIUM case with CHARGEBACK_AFTER_CANCEL rule
3. Analyst dismisses as false positive
4. Account remains ACTIVE

**Tooling:** Postman/Newman for API, React Testing Library for UI

### 10.4 Quality Metrics

| Metric | Target |
|--------|--------|
| Line coverage (service layer) | 70%+ |
| Build time | <5 minutes |
| Static analysis issues | 0 critical/blocker |
| Scoring p95 latency | <200 ms |

---

## 11. Three-Week Plan

| Week | Focus | Exit Criteria |
|------|-------|---------------|
| **Week 1** | Service skeletons, schemas, Kafka topics, Docker Compose | Event travels Signup → Scoring → Case with dummy logic; hello-world deploy to Fargate |
| **Week 2** | Real scoring rules, case workflow, freeze/ban actions | All eight rules implemented; all three bands reachable on seeded data |
| **Week 3** | Dashboard polish, AWS deployment, testing | Both end-to-end scenarios run cleanly in <2 minutes |

---

## 12. Design Decisions & Trade-offs

| Decision | Alternative Rejected | Reasoning |
|----------|-------------------|-----------| 
| Rules-based scoring | ML classifier | No labeled data; explainability is a product requirement for human reviewers |
| Weights in config table | Weights in application.yml | Tuning without redeploy is the most persuasive demo moment |
| Kafka between all services | Synchronous REST throughout | Scorer must not fail customer signup; decoupling is correctness, not buzzword |
| WebFlux only for scoring | WebFlux everywhere | Reactive justified for I/O-bound, high-volume; costs debuggability elsewhere |
| Three datastores | PostgreSQL only | Each earns place: Redis for atomic TTL counters, MongoDB for heterogeneous signals, PG for transactional integrity |
| Auto-freeze, never auto-ban | Fully automated enforcement | Freeze is reversible; ban is not — irreversible actions require human |
| JSONB for score reasons | Normalized reasons table | Explanations must be immutable snapshots, not live joins against mutable rules |
| No Kubernetes | EKS | Five services do not justify operational surface in three weeks |

---

## 13. Known Risks & Mitigations

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Kafka setup delays | Everything slips | Wire all topics end-to-end with dummy data by day 3 |
| Contract drift between owners | Integration fails | Freeze schemas/DTOs by end of Week 1; changes require agreement |
| Rule weights produce all-or-nothing | Demo looks broken | Calibrate against seeded dataset in Week 2; confirm all bands reachable |
| AWS deployment consumes Week 3 | No time for polish | Containerize from day one; deploy hello-world to Fargate in Week 1 |

---

## 14. Future Enhancements

- ML model trained on analyst decisions, running in shadow mode
- Graph-based account linking (catch rings across card/device/IP)
- Sliding-window counters via Redis sorted sets
- Analyst feedback loop: per-rule false-positive rates drive weight tuning
- Real payment provider integration
- Webhook and email notifications on HIGH-band freezes

---

**Version 1.0 — September 8, 2026**
