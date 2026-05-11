# DocuVault — SaaS Document Management System

> A cloud-native, RESTful document management platform with automatic versioning, isolated virtual workspaces, and a full observability stack.

---

## Table of Contents

1. [Team](#team)
2. [Project Description](#project-description)
3. [API Documentation](#api-documentation)
4. [Architecture](#architecture)
5. [Branching Strategy](#branching-strategy)
6. [Testing Strategy](#testing-strategy)
7. [CI/CD Pipeline](#cicd-pipeline)
8. [Observability](#observability)
9. [Contributing](#contributing)
10. [Prerequisites & Running the Project](#prerequisites--running-the-project)

---

## Team

- **Team Name:** Cloud 9

| Member | Role |
|---|---|
| Enache-Preoteasa David | Identity & Workspace Manager — delegation of virtual workspaces, document sharing system with access permissions |
| Bunescu Robert | Document Operations Core & API Design & Versioning — CRUD operations for documents, version history system |

---

## Project Description

DocuVault is a **Software-as-a-Service (SaaS)** application that exposes a RESTful API for the secure and efficient management of cloud documents. The system gives users isolated virtual workspaces, where they can create, read, update and delete documents, and have full control over their personal data.

A central component of the business logic is the **automatic versioning system**. Unlike a simple storage system, when a user updates the content or metadata of a document, DocuVault does not overwrite the old information, but instead automatically generates a new version of the file, keeping the complete change history for traceability and recovery.

The project architecture is modular, separating the isolation responsibilities, core operations on files (CRUD), and the versioning engine. This degree of decoupling, supported by a NoSQL persistent database (MongoDB), allows rigorous testing of the access rules and data flows.

### Key Features

| Feature | Description |
|---|---|
| **Isolated Virtual Workspaces** | Per user virtual workspaces, guaranteeing the isolation of data and granular access permissions |
| **Document CRUD & Export** | Full operations on virtual files stored in MongoDB, with support for client download |
| **Restful API** | Resource-oriented endpoints for secure document management inside isolated user workspaces |
| **Automated Versioning** | Automatic creation of new versions (v1 → v2 → v3) for each update, without overwriting the history |



### Technical Stack

| Layer | Technology |
|---|---|
| **Backend** | Spring Boot 3.4.0 (Java 21) |
| **Database** | MongoDB 6.0 |
| **API Style** | RESTful|
| **Unit & Integration Testing** | JUnit 5, Mockito, Testcontainers |
| **Coverage** | JaCoCo |
| **Performance Testing** | Apache JMeter, wrk |
| **Monitoring** | Prometheus, Grafana, Loki, AlertManager |
| **Containerisation** | Docker, Docker Compose |
| **CI/CD** | Jenkins |
| **Orchestration** | Kubernetes |

---

## API Documentation

Base URL: `http://localhost:8080`

### Documents (`/api/documents`)

All mutating document endpoints require the `X-User-Id` header for ownership and permission checks.

| Method | Endpoint | Headers | Description | Request Body |
|--------|----------|---------|-------------|--------------|
| `POST` | `/api/documents` | `X-User-Id` | Create a new document (v1) | `{ "title", "content", "workspaceId", "viewers", "editors" }` |
| `GET` | `/api/documents/{groupId}` | — | Get latest version by group ID | — |
| `PUT` | `/api/documents/{groupId}` | `X-User-Id` | Update document (creates new version) | `{ "title", "content", "viewers", "editors" }` |
| `PUT` | `/api/documents/add-viewer` | `X-User-Id` | Add viewer to document | `{ "userId", "documentGroupId" }` |
| `DELETE` | `/api/documents/{groupId}` | `X-User-Id` | Delete all versions (owner only) | — |
| `GET` | `/api/documents/workspace/{workspaceId}` | — | Get all documents in a workspace | — |
| `GET` | `/api/documents/owner/{ownerId}` | — | Get all documents by owner | — |

### Document Access Control

| Role | Delete | Add Viewers/Editors | Edit | View |
|---|---|---|---|---|
| **Owner** | ✅ | ✅ | ✅ | ✅ |
| **Workspace Member** | ❌ | ❌ | ✅ | ✅ |
| **Editor** | ❌ | ❌ | ✅ | ✅ |
| **Viewer** | ❌ | ❌ | ❌ | ✅ |

### Workspaces (`/api/workspaces`)

| Method | Path | Headers | Description | Request Body |
|-|-|-|-|-|
| `GET` | `/statistics/{id}` | — | Get workspace statistics | — |
| `POST` | `/` | — | Create workspace | `{ "name", "userId" }` |
| `POST` | `/add-user` | — | Add user to workspace | `{ "userId", "workspaceId" }` |

---

## Architecture

### 1. Application Architecture — DocuVault Internals

This diagram shows the internal structure of the Spring Boot application, mapping exactly to the source code packages under `ro.unibuc.prodeng`.

```mermaid
graph TD
    subgraph HTTP["REST API — HTTP Layer"]
        REQ_H["X-User-Id Header\n(ownership context)"]
    end

    subgraph Controllers["Controller Layer — @RestController"]
        DC["DocumentController\n/api/documents"]
        WC["WorkspaceController\n/api/workspaces"]
        UC["UserController\n/api/users"]
        TC["TodoController\n/api/todos"]
    end

    subgraph RequestDTOs["Request DTOs — validation layer"]
        DCR["DocumentCreateRequest\n{title, content, workspaceId,\nviewers, editors}"]
        DUR["DocumentUpdateRequest\n{title, content,\nviewers, editors}"]
        DAVR["DocumentAddViewerRequest\n{userId, documentGroupId}"]
        CWR["CreateWorkspaceRequest\n{name, userId}"]
        AUWR["AddUserToWorkspaceRequest\n{userId, workspaceId}"]
    end

    subgraph Services["Service Layer — @Service (Business Logic)"]
        DS["DocumentService\n• createDocument()\n• getLatestByGroupId()\n• updateDocument()  ← auto-versioning\n• deleteAllVersions() ← owner-only\n• addViewer() ← owner-only\n• getHistory()"]
        WS["WorkspaceService\n• createWorkspace()\n• addUserToWorkspace()\n• getWorkspaceStatistics()"]
        US["UserService"]
        TS["TodoService"]
    end

    subgraph ACL["Access Control — Permission Engine"]
        direction LR
        CHK["checkEditPermission()\n1. Is Owner?\n2. Is Editor?\n3. Same Workspace?"]
    end

    subgraph Repos["Repository Layer — MongoRepository"]
        DR["DocumentRepository\n• findTopBy...OrderByVersionDesc()\n• findByDocumentGroupId...()\n• findByOwnerId()\n• findByWorkspaceId()"]
        WR["WorkspaceRepository"]
        UR["UserRepository"]
        TR["TodoRepository"]
    end

    subgraph Entities["MongoDB Document Entities — @Document"]
        DE["DocumentEntity\ncollection: files\n{id, documentGroupId, version,\nownerId, title, content,\nworkspaceId, viewers[],\neditors[], createdAt}"]
        WE["WorkspaceEntity\ncollection: workspaces\n{id, name, users[]}"]
        UE["UserEntity\ncollection: users\n{id, name, email,\nworkspaces[]}"]
    end

    subgraph ResponseDTOs["Response DTOs — HATEOAS"]
        DRR["DocumentResponse"]
        WRR["WorkspaceResponse"]
        WSR["WorkspaceStatisticsResponse"]
    end

    subgraph ErrHandler["Global Error Handling — @RestControllerAdvice"]
        GEH["GlobalExceptionHandler\n• 400 → IllegalArgumentException\n• 403 → AccessDeniedException\n• 404 → EntityNotFoundException"]
    end

    HTTP --> Controllers
    REQ_H -.->|"injected via @RequestHeader"| DC

    DC --> DCR & DUR & DAVR
    WC --> CWR & AUWR

    DC --> DS
    WC --> WS
    UC --> US
    TC --> TS

    DS --> CHK
    DS --> DR
    DS --> UR
    WS --> WR
    WS --> UR
    WS --> DS
    US --> UR
    TS --> TR

    DR --> DE
    WR --> WE
    UR --> UE

    DS --> DRR
    WS --> WRR & WSR

    Controllers -.->|"exceptions"| GEH
```

#### Auto-Versioning Flow

The core business logic — automatic document versioning — works as follows:

```mermaid
sequenceDiagram
    actor Client
    participant DC as DocumentController
    participant DS as DocumentService
    participant ACL as checkEditPermission()
    participant DR as DocumentRepository
    participant MongoDB

    Client->>DC: PUT /api/documents/{groupId}<br/>X-User-Id: user-42
    DC->>DS: updateDocument(groupId, request, "user-42")
    DS->>DR: findTopByDocumentGroupIdOrderByVersionDesc(groupId)
    DR->>MongoDB: db.files.find({documentGroupId: groupId}).sort({version: -1}).limit(1)
    MongoDB-->>DR: DocumentEntity (version: 3)
    DR-->>DS: latest entity (v3)

    DS->>ACL: checkEditPermission("user-42", entity)
    Note over ACL: 1. Is owner? ❌<br/>2. Is editor? ✅
    ACL-->>DS: access granted

    DS->>DS: Clone entity → set version = 4,<br/>merge request fields (title, content, viewers, editors)
    DS->>DR: save(newVersion)
    DR->>MongoDB: db.files.insertOne({...version: 4...})
    MongoDB-->>DR: saved entity (v4)
    DR-->>DS: DocumentEntity (v4)
    DS->>DS: toResponse(entity) → DocumentResponse
    DS-->>DC: DocumentResponse (v4)
    DC-->>Client: 200 OK + DocumentResponse (v4)

    Note over MongoDB: v1, v2, v3 remain<br/>untouched in the<br/>files collection
```

---

### 2. Infrastructure & DevOps Architecture

This diagram shows the full deployment topology — application containers, database, monitoring stack, CI/CD pipeline, and orchestration.

```mermaid
graph TB
    subgraph Client["Client Layer"]
        HTTP["HTTP Client\n(requests.http / curl / JMeter)"]
    end

    subgraph App["Application Layer — Spring Boot (port 8080)"]
        direction TB
        WC["WorkspaceController\n/api/workspaces"]
        DC["DocumentController\n/api/documents"]
        ACT["Actuator\n/actuator/prometheus"]

        WS["WorkspaceService"]
        DS["DocumentService"]
        MS["AppMetricsService\n(Micrometer)"]

        WC --> WS
        DC --> DS
        DC --> MS
    end

    subgraph DB["Persistence Layer"]
        MONGO[("MongoDB 6.0\nport 27017")]
        MONGOUI["Mongo Express\n(Admin UI — port 8090)"]
        MONGO --- MONGOUI
    end

    subgraph Observability["Observability Stack"]
        PROM["Prometheus\n(port 9090)"]
        GRAF["Grafana\n(port 3000)"]
        LOKI["Loki\n(port 3100)"]
        AM["AlertManager\n(port 9093)"]
        CADV["cAdvisor\n(Container Metrics — port 8081)"]
        MDBE["mongodb-exporter\n(port 9216)"]

        PROM --> GRAF
        PROM --> AM
        LOKI --> GRAF
        CADV --> PROM
        MDBE --> PROM
    end

    subgraph CI["CI/CD Layer"]
        JEN["Jenkins\n(port 8082)"]
        REG["Docker Registry\n(DockerHub)"]
        K8S["Kubernetes Cluster\n(prod-eng Deployment)"]
        JEN --> REG
        JEN --> K8S
    end

    HTTP --> WC
    HTTP --> DC
    App --> MONGO
    ACT --> PROM
    App --> LOKI
```

### Component Interaction — Technical Description

The system is composed of four clearly separated layers:

#### 1. Client Layer
Any HTTP client (browser, curl, IntelliJ `.http` files, or JMeter) communicates with the Spring Boot application over port `8080`. All mutating operations on documents must include the `X-User-Id` header, which drives the ownership and permission evaluation logic.

#### 2. Application Layer (Spring Boot)
The application follows a classic **Controller → Service → Repository** layering:

- **`WorkspaceController` / `WorkspaceService`** — responsible for creating and managing virtual workspaces. Each workspace is a first-class entity in MongoDB and acts as the isolation boundary. When a document is created, it must be anchored to a workspace; the service enforces that only members of that workspace (or holders of explicit `viewer`/`editor` grants) can access its documents.

- **`DocumentController` / `DocumentService`** — handles document CRUD and the application’s document-version persistence rules. Rather than exposing a separate `VersioningEngine`, the service works directly with the persistence layer to create and retrieve document versions stored as separate MongoDB records grouped by `groupId`. API payloads are returned through `DocumentResponse`, which is a plain response model and does not include HATEOAS-style hypermedia links.

- **`AppMetricsService`** — a Micrometer-backed service wired into controllers and exception handlers to record custom business metrics (e.g., `prod_eng_info_count_total`, request latencies, error rates). These are exposed via `/actuator/prometheus` for Prometheus scraping.

#### 3. Persistence Layer (MongoDB)
MongoDB stores three primary collections:
- `workspaces` — workspace entities with member lists.
- `documents` — each document version is stored as a separate document, grouped by a shared `groupId` field. Querying by `groupId` and sorting by `version` descending returns the latest version.
- Administrative access is available via **Mongo Express** at `http://localhost:8090`; authentication credentials are configured via the Docker/environment settings and should be overridden as needed instead of being documented here.

#### 4. Observability Stack
The entire stack is scraped and visualised through:
- **Prometheus** pulls metrics every `30s` from three targets: the Spring Boot actuator endpoint, **cAdvisor** (container-level CPU/memory), and the **mongodb-exporter** (database internals).
- **Loki** aggregates logs from Docker containers via the Loki logging driver.
- **Grafana** renders unified dashboards combining Prometheus metrics and Loki logs.
- **AlertManager** receives rule violations from Prometheus and routes email notifications.

---

## Branching Strategy

The project uses a **multi-branch Git workflow** where each long-lived branch has a dedicated, non-overlapping responsibility:

### Branch Descriptions

| Branch | Purpose | Key Contents |
|---|---|---|
| `main` | **Production-ready application code.** The single source of truth for the Spring Boot service. All feature branches are cut from here and merged back via Pull Request after peer review. | `src/`, `build.gradle`, `Dockerfile`, `docker-compose.yml` |
| `monitoring` | **Infrastructure-as-Code for the observability stack.** Contains all configuration files for the monitoring layer. Changes here do not affect application logic, only how the system is observed. | `infrastructure/prometheus/`, `infrastructure/grafana/`, `infrastructure/alertmanager/`, `infrastructure/loki/` |
| `jenkins` | **CI/CD pipeline definitions.** Houses the `Jenkinsfile` and any Jenkins-specific configuration. Separating this from `main` means pipeline changes can be tested and reviewed independently without risking the production codebase. | `infrastructure/Jenkinsfile`, `jenkins_config/` |

### Feature Branch Workflow (from `main`)

```
main
 └── feature/document-versioning   ← cut from main
      └── (develop → commit → push)
           └── Pull Request → code review → merge to main
```

> All team members follow trunk-based development with short-lived feature branches. No direct commits to `main`, `monitoring`, or `jenkins`.

---

## Testing Strategy

The project implements a **two-tier testing strategy** enforced via Gradle tasks and tagged JUnit 5 test suites.

```mermaid
graph TD
    INT["🔶 Integration Tests\nTestcontainers + real MongoDB\n./gradlew testIT"]
    UNIT["🟩 Unit Tests  (largest)\nJUnit 5 + Mockito · JaCoCo coverage\n./gradlew test"]

    INT --> UNIT
```

### Unit Testing

- **Tools:** JUnit 5 (`junit-jupiter-api:5.11.4`), Mockito (via `spring-boot-starter-test`), Spring MockMvc
- **Scope:** Service-layer business logic (`DocumentService`, `WorkspaceService`, `AppMetricsService`) and controller request/response mapping.
- **Coverage:** Measured by **JaCoCo** (v0.8.9). Config, exception, model, repository, and request/response classes are excluded from coverage metrics so that the report reflects only meaningful business logic.
- **Run command:**
  ```bash
  ./gradlew test
  # Coverage report generated at: build/reports/jacoco/test/html/index.html
  ```
- **Gradle tag:** Tests **without** `@Tag("IntegrationTest")` or `@Tag("E2E")` are included by default.

### Integration Testing

- **Tools:** **Testcontainers** (`testcontainers:2.0.3`, `testcontainers-mongodb:2.0.3`) — spins up a real, ephemeral MongoDB container during the test run, eliminating in-memory fakes.
- **Scope:** Full Spring context loaded (`@SpringBootTest`). Tests exercise the complete Controller → Service → Repository → MongoDB flow to validate data persistence, versioning correctness, and workspace access-control rules.
- **Base class:** `IntegrationTestBase.java` bootstraps the Testcontainers MongoDB instance and wires the `MONGODB_CONECTION_URL` property before the application context starts.
- **Run command:**
  ```bash
  ./gradlew testIT
  ```
- **Gradle tag:** Tests annotated `@Tag("IntegrationTest")`.

### Performance Testing (Apache JMeter)

All performance test plans are located in the `jmeter/` directory and were created with **Apache JMeter 5.6.3**. The plans are organized into two categories: **DocuVault-specific** (Documents endpoint) and **legacy reference** (Todos endpoint, from the original service template).

#### 1. Documents Endpoint Test Plan (`jmeter/Test plan for Documents endpoint.jmx`)

This is the primary performance test plan — a full CRUD load test against the `/api/documents` API.

| Parameter | Value |
|---|---|
| **Threads (virtual users)** | 50 |
| **Ramp-up period** | 60 seconds |
| **Loop count** | 5 per thread |
| **Total requests** | 50 × 5 × 4 samplers = **1,000 requests** |
| **Think time** | 500ms constant timer between requests |
| **On error** | Continue |

**HTTP Samplers (executed in sequence per loop):**

| # | Sampler | Method | Endpoint | Description |
|---|---|---|---|---|
| 1 | `POST Document Create` | `POST` | `/api/documents` | Creates a document with randomised title (`${__Random(1,10000)}`), per-thread owner (`owner-${__threadNum}`), in workspace `ws-1` |
| 2 | `GET Document` | `GET` | `/api/documents/${currentDocId}` | Reads back the created document using `documentGroupId` extracted via JSON Post-Processor |
| 3 | `PUT UPDATE DOCUMENT` | `PUT` | `/api/documents/${currentDocId}` | Updates the document (triggers auto-versioning), adds `editor-1` to editors list |
| 4 | `DELETE document` | `DELETE` | `/api/documents/${currentDocId}` | Deletes all versions of the document |

> A **JSON Post-Processor** (`$.documentGroupId`) on the POST sampler chains the `currentDocId` variable across subsequent samplers, creating a realistic correlated CRUD flow.

**Listeners (result collectors):**

| Listener | Purpose |
|---|---|
| **View Results Tree** | Per-request detail inspection — request/response headers, body, timing |
| **Summary Report** | Aggregate statistics table: avg/min/max, throughput, error % |
| **Graph Results** | Real-time visualization of throughput and response time trends |

#### Test Results (Documents Endpoint — 50 threads, 5 loops)

Run environment: `localhost`, Docker Compose (`mongo` + `prod-eng-service` profiles), macOS (Apple Silicon), JMeter 5.6.3 CLI mode.

**Aggregate Summary:**

| Metric | Value |
|---|---|
| **Total Requests** | 1,000 |
| **Duration** | ~69 seconds |
| **Throughput** | **14.4 req/s** |
| **Avg Response Time** | **16 ms** |
| **Min Response Time** | 2 ms |
| **Max Response Time** | 50 ms |
| **Error Rate** | **0.00%** |

**Per-Sampler Breakdown:**

| Sampler | Requests | Avg (ms) | Min (ms) | Max (ms) | Error % |
|---|---|---|---|---|---|
| `POST Document Create` | 250 | 16 | 3 | 44 | 0.0% |
| `GET Document` | 250 | 15 | 3 | 44 | 0.0% |
| `PUT UPDATE DOCUMENT` | 250 | 18 | 2 | 48 | 0.0% |
| `DELETE document` | 250 | 18 | 2 | 50 | 0.0% |

> **Key takeaways:** All 1,000 requests completed successfully with **zero errors**. Average latency stayed consistently under **20ms** across all four CRUD operations even under concurrent load (50 virtual users). The auto-versioning logic on `PUT` adds no measurable overhead compared to `POST`.

#### 2. Workspaces Test Plan (`jmeter/Workspaces Test Plan.jmx`)

This performance test plan performs load tests against the `/api/workspaces` API.

| Parameter | Value |
|---|---|
| **Threads (virtual users)** | 10 |
| **Ramp-up period** | 10 seconds |
| **Loop count** | 5 per thread |
| **Total requests** | 10 × 5 × 4 samplers = **200 requests** |
| **Think time** | 0-2000ms constant timers between requests (depending on the sampler) |
| **On error** | Continue |

**HTTP Samplers (executed in sequence per loop):**

| # | Sampler | Method | Endpoint | Description |
|---|---|---|---|---|
| 1 | `Get Workspace` | `GET` | `/api/workspaces/${workspaceId}` | Reads a workspace using the list of workspace IDs provided in a CSV file |
| 2 | `Create User` | `POST` | `/api/users` | Creates a user that will be used by the next sampler for creating a workspace |
| 3 | `Create Workspace` | `POST` | `/api/workspaces` | Creates a workspace |
| 4 | `Get All Workspaces` | `GET` | `/api/workspaces` | Gets all workspaces |

> A **JSON Post-Processor** (`Extract userId`) on the POST sampler chains the `userId` variable across subsequent samplers, creating a realistic correlated CRUD flow.

**Listeners (result collectors):**

| Listener | Purpose |
|---|---|
| **View Results Tree** | Per-request detail inspection — request/response headers, body, timing |
| **Summary Report** | Aggregate statistics table: avg/min/max, throughput, error % |
| **Aggregate Report** | Median/P90 latency |
| **Graph Results** | Real-time visualization of throughput and response time trends |

#### Test Results (Workspaces Endpoint — 10 threads, 5 loops)

Run environment: `localhost`, Docker Compose (`mongo` + `prod-eng-service` profiles), Windows (Intel Core i3-1215U), JMeter 5.6.3 GUI mode.

**Aggregate Summary:**

| Metric | Value |
|---|---|
| **Total Requests** | 200 |
| **Duration** | 19 seconds |
| **Throughput** | **10.0 req/s** |
| **Avg Response Time** | **86 ms** |
| **Min Response Time** | 55 ms |
| **Max Response Time** | 247 ms |
| **Error Rate** | **0.00%** |

**Per-Sampler Breakdown:**

| Sampler | Requests | Avg (ms) | Min (ms) | Max (ms) | Error % |
|---|---|---|---|---|---|
| `Get Workspace` | 50 | 82 | 55 | 247 | 0.0% |
| `Create User` | 50 | 90 | 59 | 203 | 0.0% |
| `Create Workspace` | 50 | 84 | 57 | 173 | 0.0% |
| `Get All Workspaces` | 50 | 88 | 58 | 198 | 0.0% |

> **Key takeaways:** All 200 requests completed successfully with **zero errors**. Average latency stayed consistently under **90ms** across all four operations even under concurrent load (10 virtual users).

#### 3. Todos Test Plans (reference/legacy)

These plans target the `/api/todos` endpoint from the original service template and serve as reference for running JMeter against GitHub Codespaces:

| Test Plan | Target | Threads | Loops | Notes |
|---|---|---|---|---|
| `jmeter/todo_test_plan_local.jmx` | `localhost:8080` | 10 | 5 | Local execution, `Content-Type: application/json` header |
| `jmeter/todo_test_plan_codespace.jmx` | `[codespace]-8080.app.github.dev` | 10 | 5 | Codespace execution, requires `X-Github-Token` header |

#### 4. Standalone Listener Fragments

Reusable JMeter listener fragments that can be imported into any test plan:

| File | Listener Type |
|---|---|
| `jmeter/Graph Results_latest.jmx` | Graph Results visualizer |
| `jmeter/Summary Report_latest.jmx` | Summary Report aggregator |
| `jmeter/View Results Tree_latest.jmx` | View Results Tree inspector |

#### Running JMeter Tests

```bash
# GUI mode (interactive, for developing/debugging test plans)
jmeter -t jmeter/"Test plan for Documents endpoint.jmx"

# CLI mode (headless, for CI/unattended runs)
jmeter -n -t jmeter/"Test plan for Documents endpoint.jmx" \
       -l results.jtl \
       -e -o jmeter-report/
```

#### wrk Injectors (Docker Compose `perf` profile)

In addition to JMeter, lightweight sustained-load and stress tests are available via Docker Compose:

| Service | Command | Purpose |
|---|---|---|
| `wrk-injector-prod-eng-functional` | `wrk -t4 -c10 -d300s /api/users` | Low-concurrency sustained load (10 connections, 5 min) |
| `wrk-injector-info-perf` | `wrk -t4 -c1000 -d5m --latency /info` | High-concurrency stress test (1000 connections, latency histogram) |

```bash
# Start the wrk injectors alongside the monitoring stack
docker compose --profile monitoring --profile perf up -d
```

- **Metrics measured:** Requests/sec (throughput), P50/P95/P99 latency, error rate, and container CPU/memory via cAdvisor during the load run.

---

## CI/CD Pipeline

DocuVault uses **Jenkins** (running as a Docker container on port `8082`) for Continuous Integration and **Kubernetes** manifests for Continuous Delivery.

### Pipeline Architecture

```mermaid
flowchart LR
    DEV["👨‍💻 Developer\nPushes to GitHub"]
    GH["GitHub\nRepository"]
    JEN["Jenkins\n(port 8082)"]
    BUILD["Stage: Build & Test\n./gradlew clean build"]
    PKG["Docker Image\nBuilt & Tagged"]
    REG["DockerHub Registry\nrobertpoziumschi/hello-img-pipeline"]
    K8S["Kubernetes Cluster\nprod-eng Deployment"]

    DEV --> GH
    GH -->|"Webhook / Poll SCM"| JEN
    JEN --> BUILD
    BUILD -->|"✅ Tests pass"| PKG
    PKG -->|"docker push"| REG
    REG -->|"kubectl apply"| K8S
```

### Continuous Integration — Jenkins Pipeline Stages

The `Jenkinsfile` (located at `infrastructure/Jenkinsfile`) defines the following pipeline:

```groovy
pipeline {
    agent any
    environment {
        DOCKER_PASSWORD = credentials("docker_password")
    }
    stages {
        stage('Build & Test') {
            steps {
                sh './gradlew clean build'
            }
        }
    }
}
```

| Stage | Tool | What happens |
|---|---|---|
| **Checkout** | Jenkins SCM | Jenkins clones the repository from GitHub at the configured branch. |
| **Build & Test** | `./gradlew clean build` | Compiles the Java 21 source, runs all **unit tests** (tags `IntegrationTest` and `E2E` are excluded by default), and generates the JaCoCo coverage report. |
| **Package** | `docker build` | The Spring Boot fat-JAR (produced in `build/libs/`) is packaged into a Docker image using the project `Dockerfile`. The image is tagged with the build version. |
| **Push** | `docker push` | The tagged image is pushed to DockerHub using the `docker_password` Jenkins credential (`DOCKER_PASSWORD`). |
| **Deploy** | `kubectl apply` | Kubernetes manifests in `infrastructure/kubernetes/` are applied to the cluster, rolling out the new image with zero-downtime. |

> Jenkins is mounted with `/var/run/docker.sock` and the host Docker binary so it can build and push images without a Docker-in-Docker sidecar.

### Continuous Delivery — Kubernetes

The application is deployed to Kubernetes using raw manifests (no Helm charts at this time). The manifests live in `infrastructure/kubernetes/`.

#### `prod-eng-service.yaml`

```yaml
# Deployment — 1 replica of the Spring Boot service
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prod-eng
spec:
  replicas: 1
  selector:
    matchLabels:
      prod-eng-service: prod-eng
  template:
    spec:
      containers:
        - name: prod-eng
          image: robertpoziumschi/hello-img-pipeline:v1.2.0
          ports:
            - containerPort: 8080
          env:
            - name: ENVIRONMENT_NAME
              value: local
            - name: MONGODB_CONECTION_URL
              value: mongodb://root:example@mongo:27017/
---
# Service — LoadBalancer exposing port 8080
apiVersion: v1
kind: Service
metadata:
  name: prod-eng
spec:
  type: LoadBalancer
  ports:
    - port: 8080
      targetPort: 8080
```

| Resource | Kind | Purpose |
|---|---|---|
| `prod-eng` | `Deployment` | Runs 1 replica of the DocuVault Spring Boot container. `restartPolicy: Always` ensures automatic recovery on crash. |
| `prod-eng` | `Service (LoadBalancer)` | Exposes port `8080` externally via a cloud load balancer (or `NodePort` locally with minikube). |
| `mongo` | `Deployment` (separate) | MongoDB instance reachable at `mongo:27017` inside the cluster. |

---

## Observability

The observability stack is started with the `monitoring` Docker Compose profile and is fully pre-configured — no manual dashboard or datasource setup required.

```bash
# Start monitoring stack (Prometheus + Grafana + Loki + AlertManager + cAdvisor + mongodb-exporter)
./start_with_monitoring.sh

# Or directly:
docker compose --profile mongo --profile prod-eng-service --profile monitoring up -d
```

### Stack Overview

```mermaid
graph LR
    subgraph Sources["Metric & Log Sources"]
        APP["Spring Boot\n/actuator/prometheus\n(port 8080)"]
        CADV["cAdvisor\n/metrics\n(port 8080→8081)"]
        MDBE["mongodb-exporter\n/metrics\n(port 9216)"]
        LOKI_DRV["Docker Loki\nLogging Driver\n(containers → Loki)"]
    end

    subgraph Aggregation["Aggregation"]
        PROM["Prometheus\nscrape_interval: 30s\n(port 9090)"]
        LOKI["Loki\n(port 3100)"]
    end

    subgraph Visualisation["Visualisation & Alerting"]
        GRAF["Grafana\n(port 3000)"]
        AM["AlertManager\n(port 9093)"]
        EMAIL["📧 Email\n(Gmail SMTP)"]
    end

    APP -->|"pull"| PROM
    CADV -->|"pull"| PROM
    MDBE -->|"pull"| PROM
    LOKI_DRV -->|"push"| LOKI
    PROM -->|"query"| GRAF
    LOKI -->|"query"| GRAF
    PROM -->|"fire alerts"| AM
    AM -->|"smtp:587"| EMAIL
```

### Prometheus — Metrics Scraping

Prometheus is configured via `infrastructure/prometheus/prometheus.yml` with a global `scrape_interval` of **30 seconds**.

| Job Name | Target | Metrics path | What it measures |
|---|---|---|---|
| `prometheus` | `localhost:9090` | `/metrics` | Prometheus self-monitoring |
| `loki` | `loki:3100` | `/metrics` | Loki internal metrics |
| `cadvisor` | `cadvisor:8080` | `/metrics` | Per-container CPU, memory, network I/O |
| `mongodb` | `mongodb-exporter:9216` | `/metrics` | MongoDB connections, opcounters, replication lag |
| `spring-prod-eng-app` | `prod-eng:8080` | `/actuator/prometheus` | JVM heap, HTTP request rates, custom business metrics |

The `cadvisor` job uses a `metric_relabel_config` to extract short container names from full Docker container IDs, making dashboards readable.

Custom business metrics exposed by `AppMetricsService` include:

| Metric | Type | Description |
|---|---|---|
| `prod_eng_info_count_total` | Counter | Total number of `/info` endpoint calls — used as a canary signal |
| `http_server_requests_seconds` | Timer | Latency histogram for all HTTP endpoints (auto-instrumented by Micrometer) |
| JVM metrics (`jvm_memory_*`, `jvm_gc_*`) | Gauge/Counter | Heap usage, GC pause times, thread counts |

### Grafana — Dashboards

Grafana (port `3000`) starts in **anonymous admin mode** (no login required) with dashboards and datasources provisioned automatically from:

```
infrastructure/grafana/
├── provisioning/
│   ├── datasources/   ← Prometheus & Loki datasource configs
│   └── dashboards/    ← Dashboard provider config
└── dashboards/        ← Dashboard JSON files
```

| Dashboard | Datasource | Key panels |
|---|---|---|
| **DocuVault App Metrics** | Prometheus | Request rate, error rate, P95 latency, custom counters |
| **JVM Overview** | Prometheus | Heap used/committed, GC pause time, thread count, class loading |
| **Container Resources** | Prometheus (cAdvisor) | Per-container CPU %, memory usage, network Rx/Tx |
| **MongoDB Overview** | Prometheus (mongodb-exporter) | Active connections, operations/sec, document counts |
| **Logs Explorer** | Loki | Full-text log search across all containers |

### AlertManager — Alert Routing

AlertManager (`infrastructure/alertmanager/alertmanager.yml`) receives fired alerts from Prometheus and routes them to an **email receiver** via Gmail SMTP (port `587`, TLS).

```yaml
route:
  group_by: ['alertname']
  group_wait: 10s        # Wait before sending first notification
  group_interval: 10s    # Interval between grouped notifications
  repeat_interval: 1m    # Re-notify if alert is still firing after 1 min
  receiver: 'email'
```

#### Configured Alert Rules

The following table reflects the alert rules currently configured and loaded by Prometheus from `infrastructure/prometheus/`:

| File | Group | Alert Name | Condition | Severity |
|---|---|---|---|---|
| `app-alerts.yml` | `AppAlerts` | `WARNING-HighThroughput` | `rate(prod_eng_info_count_total[1m]) > 10` for 10s | ⚠️ warning |
| `app-alerts.yml` | `AppAlerts` | `CRITICAL-HighThroughput` | `rate(prod_eng_info_count_total[1m]) > 50` for 10s | 🔴 critical |
| `canary-alerts.yml` | `CanaryAlerts` | `WARNING-NoThroughout` | `rate(prod_eng_info_count_total[1m]) == 0` for 10s | ⚠️ warning |
| `container-alerts.yml` | `ContainerAlerts` | `WARNING-ApplicationContainerDown` | Container not seen for > 20s | ⚠️ warning |
| `container-alerts.yml` | `ContainerAlerts` | `CRITICAL-ApplicationContainerDown` | Container not seen for > 60s | 🔴 critical |

#### Alert Lifecycle

```mermaid
stateDiagram-v2
    [*] --> Inactive : Rule defined
    Inactive --> Pending : Condition first becomes true
    Pending --> Firing : Condition holds for 'for' duration
    Firing --> AlertManager : Alert dispatched
    AlertManager --> Email : Routed to Gmail SMTP
    Firing --> Inactive : Condition resolves
    AlertManager --> Resolved : resolve_timeout = 1m
```

> **Email setup:** Replace the placeholder values in `infrastructure/alertmanager/alertmanager.yml` with your Gmail address and an [App Password](https://security.google.com/settings/security/apppasswords) before starting the monitoring stack.

---


## Contributing

All team members follow trunk-based development:
1. Create feature branch from `main`
2. Make changes and commit with clear messages
3. Create PR and request review
4. Address feedback
5. Merge after approval

# Prerequisites

For using Github Codespaces, no prerequisites are mandatory.
Follow the [./PREREQUISITES.md](./PREREQUISITES.md) instructions to configure a local virtual machine with Ubuntu, Docker, IntelliJ.

# Access the code

* Fork the code GitHub repository under your Organization
  * https://github.com/UNIBUC-PROD-ENGINEERING/service
* Clone the code repository:
  * git@github.com:YOUR_ORG_NAME/service.git

# Run code in Github Codespaces

* Make sure that the Github repository is forked under your account / Organization
* Create a new Codespace from your forked repository
* Wait for the Codespace to be up and running
* Make sure that Docker service has been started
    * ```docker ps``` should return no error
* For running / debugging directly in Visual Studio Code
  * Start the MongoDB related services
    * ```./start_mongo_only.sh```
  * Build and run the Spring Boot service
    * ```./gradlew build```
    * ```./gradlew bootRun```
* For running all services in Docker:
    * Build the Docker image of the prod-eng service
        * ```make build```
    * Start all the service containers
        * ```./start.sh```
* Use [requests.http](requests.http) to test API endpoints
* Navigation between methods (e.g. 'Go to Definition') may require:
  * ```./gradlew build``` 

NOTE: for a live demo, please check out [this youtube video](https://youtu.be/-9ePlxz03kg)

# Run/debug code in IntelliJ
* Build the code
    * IntelliJ will build it automatically
    * If you want to build it from command line and also run unit tests, run: ```./gradlew build```
* Create an IntelliJ run configuration for a Jar application
    * Add in the configuration the JAR path to the build folder `./build/libs/prod-eng-0.0.1-SNAPSHOT.jar`
* Start the MongoDB container using Docker Compose
    * ```./start_mongo_only.sh```
* Run/debug your IntelliJ run configuration
* Open in your browser:
    * http://localhost:8080/api/users

# Deploy and run the code locally as Docker instance

* Build the Docker image of the prod-eng service
    * ```make build```
* Start all the containers
    * ```./start.sh```

* Verify that all containers started, by running
  ```
  service git:(master) ✗  $ docker ps
  CONTAINER ID   IMAGE             COMMAND                  CREATED         STATUS         PORTS                                                                                          NAMES
  d0a14d57ade1   jenkins/jenkins   "/usr/bin/tini -- /u…"   5 seconds ago   Up 4 seconds   0.0.0.0:50000->50000/tcp, [::]:50000->50000/tcp, 0.0.0.0:8082->8080/tcp, [::]:8082->8080/tcp   service-jenkins-1
  d9465565ebc9   mongo-express     "/sbin/tini -- /dock…"   5 seconds ago   Up 4 seconds   0.0.0.0:8090->8081/tcp, [::]:8090->8081/tcp                                                    service-mongo-admin-ui-1
  304c29bb39ea   mongo:6.0.20      "docker-entrypoint.s…"   5 seconds ago   Up 4 seconds   0.0.0.0:27017->27017/tcp, [::]:27017->27017/tcp                                                service-mongo-1
  a74b4cb2fb58   prod-eng-img      "java -jar /prod-eng…"   5 seconds ago   Up 4 seconds   0.0.0.0:5005->5005/tcp, [::]:5005->5005/tcp, 0.0.0.0:8080->8080/tcp, [::]:8080->8080/tcp       service-prod-eng-1
  ```
* Open in your browser:
    * http://localhost:8080/api/users
* You can test other API endpoints using [requests.http](requests.http)
* You can access the MongoDB Admin UI at:
  * http://localhost:8090
  * default credentials: username `unibuc`, password `adobe`
  * database `test` contains application entities
