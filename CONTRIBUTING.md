# Contributing to Subscription Fraud Detector

This repository is a collaborative monorepo for the Subscription & Billing Fraud / Churn-Abuse Detector.

## Team workflow

1. Create a branch from main.
2. Keep changes focused to one feature or service.
3. Add tests for behavior you change.
4. Run the relevant validation before opening a PR.
5. Keep documentation in sync with code changes.

## Branch naming

- feature/<short-name>
- fix/<short-name>
- chore/<short-name>
- docs/<short-name>

## Responsibilities by module

- gateway: routing, auth enforcement, ingress
- auth-service: login, JWT issuance, role checks
- signup-service: account creation and fingerprints
- subscription-service: billing lifecycle and promo handling
- scoring-service: rules, velocity checks, score computation
- case-service: analyst queue, decisions, actions
- frontend: dashboard UX and analyst workflows

## Local setup

### Java backend

```bash
mvn clean install -DskipTests
```

### Frontend

Install Node LTS first, then:

```bash
cd frontend
npm install
npm run build
```

## Run services locally

```bash
docker compose up --build
```

or run individual Spring Boot apps with their service ports.

## PR checklist

- Build completes
- No unrelated files are changed
- Service-specific docs are updated when API or behavior changes
- Sample payloads or examples are included when relevant
- Clear summary of what changed and why

## Communication

Use short, concrete updates in the pull request description:

- What changed
- Why it changed
- What to validate
- Risks or follow-up work

The goal is to keep the monorepo understandable and easy for multiple contributors to extend.
