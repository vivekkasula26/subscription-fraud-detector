# Agent Guide for This Repository

This repository is meant for collaborative implementation of the fraud detection platform described in the project design document.

## How to work in this repo

- Treat the design in [Readme.md](Readme.md) as the primary source of truth.
- Keep changes modular and aligned to the service boundaries.
- Prefer small, reviewable PRs.
- Do not add cross-cutting concerns to unrelated services.

## Service map

- gateway
- auth-service
- signup-service
- subscription-service
- scoring-service
- case-service
- frontend

## Validation commands

```bash
mvn clean install -DskipTests
```

For frontend checks:

```bash
cd frontend
npm install
npm run build
```

## Notes

- Java backend is the primary implementation target.
- The current environment needs Node/npm installed before the frontend build can pass.
- The project should evolve in incremental milestones instead of a huge single-step rewrite.
