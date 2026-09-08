# Subscription & Billing Fraud / Churn-Abuse Detector

JFSD Team Project — Project Documentation

## 1. Overview

This project detects abuse in a subscription-based business — the kind of thing streaming, SaaS, or membership apps deal with constantly. It looks for patterns like:

- Free-trial abuse — the same card or device signing up for new accounts repeatedly
- Promo-code stacking — the same promo code being reused across many accounts
- Chargeback-after-cancellation — a customer cancels, then disputes a charge shortly after

A scoring engine watches signups and account activity, flags suspicious accounts, and a support/ops dashboard lets a human review and act (freeze or ban an account). It intentionally has a smaller data model than a full payments-fraud system, which keeps it realistic to build well in 2-3 weeks.

## 2. Why this problem

It touches all the skills a Java Full Stack project should show — a rules-based scoring engine, event-driven microservices, a live dashboard, and a human-in-the-loop review workflow — without needing a huge data model or a live payment processor. It is also easy to demo: create a few fake accounts on the same device, watch them get flagged, then show an analyst reviewing and freezing one.

## 3. How it works — end to end flow

The system never bans anyone automatically without a clear reason. It is a three-tier response: no action, human review, or auto-restrict — and a human always has the final say for anything not obvious.

New signup or subscription event (cancel, chargeback)
↓
Abuse Scoring Service checks rules (card reuse, device reuse, promo reuse) and produces a risk score
↓
Low score → no action | Medium score → case queued for review | Very high score → account auto-frozen + case logged
↓
Ops analyst opens the case in the dashboard, sees why it was flagged
↓
Analyst decision: Dismiss (false positive) or Freeze / Ban the account

Scoring is simple and explainable rather than machine-learned: each rule adds points (e.g. same card used 3 times in a day = 40 points, same device across accounts = 35 points). Points are stored in a config table so the team can tune sensitivity without redeploying. This is a deliberate, defensible choice for a 2-3 week project — explainable beats opaque when the goal is trust.

## 4. Tech stack

| Area | Technology |
|------|-----------|
| Frontend | React — ops dashboard for reviewing and actioning flagged accounts |
| Backend | Java, Spring Boot (REST services), Spring WebFlux (scoring service), Spring Cloud Gateway |
| Messaging | Kafka — carries signup, subscription, and flagged-account events between services |
| Databases | PostgreSQL (users, subscriptions, cases — source of truth), MongoDB (fast abuse-signal log), Redis (real-time counters for scoring) |
| Auth | JWT-based login for the ops dashboard |
| Cloud | AWS — Docker Compose locally for Kafka/Postgres/Mongo/Redis; ECS Fargate or EC2 for services; S3 + CloudFront for the frontend |
| Other | Docker for local development and packaging every service |

## 5. The 5 services (one owner each)

- Signup & Onboarding Service — handles new signups, records card/device fingerprints
- Abuse Scoring Service — applies rules, checks Redis counters, computes a risk score
- Subscription & Billing Service — manages subscriptions, promo codes, cancellations, chargebacks
- Case & Action Service — creates review cases, freezes or bans accounts
- Gateway + Auth + React Dashboard — login, routing, and the analyst-facing UI

## 6. Scale of the project

Kept intentionally small so it is realistic for 5 people at 1-2 hours a day, 5 days a week, over 2-3 weeks:

- 5 microservices, one clear owner each — minimal overlap, easy to parallelize
- 3 databases, each with a small number of tables/collections — no heavy data modeling
- No real payments or real customer data — everything is simulated/synthetic
- No Kubernetes — Docker Compose locally, simple AWS hosting (EC2/Fargate) for the demo
- No machine learning — rules-based scoring only, with ML explicitly called out as a future improvement

## 7. Suggested 3-week plan

| Week | Focus |
|------|-------|
| Week 1 | Service skeletons, database schemas, Kafka topics wired end-to-end with dummy data, Docker Compose running locally |
| Week 2 | Real scoring rules, case workflow, freeze/ban actions, services talking to each other properly |
| Week 3 | React dashboard polish, AWS deployment, testing, demo preparation |
