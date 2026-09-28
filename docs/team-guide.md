# Team Guide

## Project goal

Build a demo-ready subscription fraud and churn-abuse detection platform with explainable scoring, team review workflow, and analyst dashboard.

## Milestones

### Milestone 1: Foundation
- repo structure and build setup
- service skeletons
- Docker Compose basics
- frontend stub

### Milestone 2: Core business flows
- account creation and fingerprinting
- subscription lifecycle management
- promo-code rules
- event publishing

### Milestone 3: Scoring and review
- rule engine and weighting
- case creation and queue
- freeze/ban decision workflow
- audit logging

### Milestone 4: Dashboard and polish
- case detail view
- rule config UI
- ops analytics
- demo readiness

## Working agreement

- Everyone is responsible for one service or one feature track.
- Keep interfaces documented in the service README or API docs.
- Update the shared docs when changing request/response contracts.
- Prefer simple domain modeling over over-engineered abstractions early on.

## Suggested ownership split

- Backend architecture: 1 lead
- Auth and gateway: 1 contributor
- Signup and subscription services: 1 or 2 contributors
- Scoring and case services: 1 or 2 contributors
- Frontend/dashboard: 1 contributor
- DevOps and Docker: 1 contributor

## Good PR format

- Title
- Summary
- Testing steps
- Screenshots or output if UI-related

## Important reminder

This project is intentionally designed as a staged build. The goal is to create a solid, testable system in layers, not to force every feature in a single commit.
