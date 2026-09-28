# Local Development Instructions

This guide explains how to set up, run, and develop the **Subscription & Billing Fraud Detector** system locally.

---

## Architecture & Service Map

| Component | Type | Local Port | Role |
|---|---|---|---|
| **Frontend** | React 18 + Vite | `5173` | Analyst Ops Dashboard |
| **API Gateway** | Spring Cloud Gateway | `8080` | Ingress router for `/api/**` |
| **Auth Service** | Spring Boot | `8081` | Authentication & JWT handling |
| **Signup Service** | Spring Boot | `8082` | Account creation & fingerprinting |
| **Subscription Service** | Spring Boot | `8083` | Subscriptions, promo codes, chargebacks |
| **Scoring Service** | Spring WebFlux (Reactive) | `8084` | Abuse scoring engine & rules |
| **Case Service** | Spring Boot | `8085` | Analyst case queue & auto-freeze actions |
| **PostgreSQL 15** | Docker Container | `5432` | Relational database (`frauddb`) |
| **Redis 7** | Docker Container | `6379` | Velocity counters & rule cache |
| **MongoDB 7** | Docker Container | `27017` | Append-only abuse signals log |
| **Apache Kafka 3.8** | Docker Container | `9092` | Event messaging broker (KRaft mode) |

---

## Prerequisites

Ensure you have the following installed on your machine:
- **Java 21+** (check with `java -version`)
- **Maven 3.8+** (check with `mvn -version`)
- **Node.js 18+ & npm** (check with `node -v` and `npm -v`)
- **Docker Desktop** (check with `docker --version`)

---

## Quick Start: Local Development (Recommended)

In local development mode, you run the **datastores and Kafka in Docker**, while running your **Spring Boot services and React dashboard natively on your host machine** (enabling live reload, hot-reloading, and IDE debugging).

### Step 1: Start Infrastructure Containers

Run the following command from the project root:

```bash
docker compose up -d postgres redis mongo kafka
```

Verify that all 4 containers are running:
```bash
docker compose ps
```

---

### Step 2: Run Backend Microservices

You can run each service directly from your IDE (e.g. IntelliJ / VS Code by running each service's `*Application.java` file) or via Maven in separate terminal windows:

```bash
# 1. API Gateway
mvn -pl gateway spring-boot:run

# 2. Auth Service
mvn -pl services/auth-service spring-boot:run

# 3. Signup Service
mvn -pl services/signup-service spring-boot:run

# 4. Subscription Service
mvn -pl services/subscription-service spring-boot:run

# 5. Scoring Service
mvn -pl services/scoring-service spring-boot:run

# 6. Case Service
mvn -pl services/case-service spring-boot:run
```

---

### Step 3: Run the React Ops Dashboard

In a new terminal window:

```bash
cd frontend
npm install
npm run dev
```

Open your browser at **[http://localhost:5173](http://localhost:5173)**.

---

## Alternative: Run Everything in Docker

To build and run the entire system (all 6 microservices + frontend + datastores) inside Docker containers:

1. **Package the backend JARs:**
   ```bash
   mvn clean package -DskipTests
   ```

2. **Start all containers:**
   ```bash
   docker compose up --build
   ```

3. **Access endpoints:**
   - **Frontend:** [http://localhost:5173](http://localhost:5173)
   - **Gateway:** [http://localhost:8080](http://localhost:8080)

4. **Stop all containers:**
   ```bash
   docker compose down
   ```

---

## Datastore Connection Details

Use these connection details to connect via database GUIs (DBeaver, TablePlus, MongoDB Compass, Redis Insight):

| Datastore | Host | Port | Database / Auth |
|---|---|---|---|
| **PostgreSQL** | `localhost` | `5432` | **DB:** `frauddb`<br>**User:** `fraud`<br>**Password:** `fraudpass` |
| **Redis** | `localhost` | `6379` | No password |
| **MongoDB** | `localhost` | `27017` | `mongodb://localhost:27017` |
| **Kafka Broker** | `localhost` | `9092` | Plaintext |

---

## Useful Commands

### Reset Local Test Data
To wipe all container data (PostgreSQL volumes, MongoDB volumes) and start completely fresh:
```bash
docker compose down -v
docker compose up -d postgres redis mongo kafka
```

### Stop Infrastructure
```bash
docker compose stop
```

### View Container Logs
```bash
# View all infrastructure logs
docker compose logs -f

# View logs for a specific service (e.g. Kafka or PostgreSQL)
docker compose logs -f kafka
docker compose logs -f postgres
```

### Build & Validate Repository
```bash
# Build all backend and frontend modules
mvn clean install -DskipTests

# Run frontend build checks
cd frontend
npm run build
```
