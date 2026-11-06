# Distributed Job Processing Platform

A production-style distributed job processing system built with **Java 17**, **Spring Boot**, **Redis**, and **PostgreSQL** for reliable asynchronous task execution — with automated testing, CI/CD, and real-time observability via Prometheus and Grafana.

Clients submit jobs through a REST API. Jobs are queued, picked up by a concurrent worker pool, executed, retried on failure (with exponential backoff), and routed to a Dead Letter Queue (DLQ) if they exhaust their retry budget — all while every stage is tracked in PostgreSQL and surfaced live on a Grafana dashboard.

## Why this project

Most portfolio projects stop at CRUD. This one is built around the problems a real backend service actually has to solve:

- Decoupling submission from execution
- Handling partial failure gracefully
- Avoiding duplicate work under concurrency
- Giving whoever owns the service in production enough visibility to trust it

**Core path:** async processing → message queue → concurrent workers → fault tolerance → persistence → observability → CI/CD

## Features

- **Async job processing** — Redis-backed queue decouples submission from execution
- **Concurrent worker pool** — configurable number of workers process jobs in parallel
- **Job locking** — Redis-based distributed lock + database status check prevents duplicate execution
- **Retries + Dead Letter Queue** — configurable max retries with exponential backoff; permanently failed jobs move to DLQ
- **Persistent job tracking** — PostgreSQL stores status, timestamps, retry counts, results, error details, and full lifecycle history
- **REST API** — submit jobs, check status, list history, inspect & requeue DLQ jobs
- **Observability** — Prometheus metrics + Grafana dashboards (queue depth, throughput, latency percentiles, worker health)
- **Health endpoints** — Spring Actuator (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`)
- **Automated testing** — unit, integration, and load tests
- **CI/CD** — GitHub Actions runs tests + JaCoCo coverage on every push

## Architecture

```
Client
  │
  ▼
Spring Boot REST API
  │
  ▼
Redis Job Queue (+ Distributed Lock)
  │
  ├──► Worker 1 ──┐
  ├──► Worker 2 ──┤──► PostgreSQL (job state, results, history, DLQ)
  └──► Worker N ──┘
  │
  ▼
Prometheus ──► Grafana
```

### Request flow

1. Client submits a job via the REST API
2. API validates the request and persists job metadata (`PENDING`)
3. Job is pushed to the Redis queue and status becomes `QUEUED`
4. An available worker claims the job using a distributed lock
5. Worker executes the task and updates status (`PROCESSING` → `COMPLETED` / `FAILED`)
6. Failed jobs are retried according to the configured policy (exponential backoff)
7. Jobs that exhaust retries move to the Dead Letter Queue
8. Prometheus scrapes runtime metrics; Grafana visualizes them in real time

### Job lifecycle

```
PENDING → QUEUED → PROCESSING → COMPLETED
                       │
                       ▼ (on failure)
                    FAILED → RETRYING → PROCESSING
                       │
                       ▼ (retries exhausted)
                  DEAD_LETTER_QUEUE
```

## Tech stack

| Category          | Technology                       |
|-------------------|----------------------------------|
| Language          | Java 17                          |
| Backend           | Spring Boot 3.2                  |
| Queue + Locking   | Redis                            |
| Database          | PostgreSQL                       |
| Testing           | JUnit 5, MockMvc, Locust / k6    |
| Coverage          | JaCoCo                           |
| CI/CD             | GitHub Actions                   |
| Monitoring        | Prometheus, Grafana              |
| Containerization  | Docker, Docker Compose           |
| Health / Metrics  | Spring Actuator + Micrometer     |

## Project structure

```
src/
├── main/
│   ├── java/com/example/jobprocessor/
│   │   ├── config/          # Redis, Worker, App properties
│   │   ├── controller/      # REST endpoints + exception handler
│   │   ├── dto/             # Request / Response DTOs
│   │   ├── entity/          # Job, JobHistory, JobStatus
│   │   ├── repository/      # Spring Data JPA
│   │   ├── service/         # JobService, WorkerService, History
│   │   ├── worker/          # (logic lives in WorkerService)
│   │   ├── queue/           # Redis queue + distributed lock
│   │   ├── handler/         # Pluggable job type handlers
│   │   └── metrics/         # Prometheus custom metrics
│   └── resources/
│       ├── application.yml
│       └── application-test.yml
├── test/
│   └── ...
├── docker/
│   ├── prometheus/
│   └── grafana/
├── loadtest/
│   ├── locustfile.py
│   └── k6-script.js
├── .github/workflows/
│   └── ci.yml
├── docker-compose.yml
├── Dockerfile
├── pom.xml
├── .env.example
└── README.md
```

## Getting started

### Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/SrivastavSudiksha/<your-repo-name>.git
cd <your-repo-name>
```

### 2. Configure environment

```bash
cp .env.example .env
```

| Variable              | Description                              | Default                                   |
|-----------------------|------------------------------------------|-------------------------------------------|
| `DB_URL`              | PostgreSQL JDBC URL                      | `jdbc:postgresql://localhost:5432/jobs`   |
| `DB_USERNAME`         | Database username                        | `postgres`                                |
| `DB_PASSWORD`         | Database password                        | `postgres`                                |
| `REDIS_HOST`          | Redis host                               | `localhost`                               |
| `REDIS_PORT`          | Redis port                               | `6379`                                    |
| `MAX_RETRIES`         | Max retry attempts before DLQ            | `3`                                       |
| `RETRY_BASE_DELAY_MS` | Base delay for exponential backoff (ms)  | `1000`                                    |
| `WORKER_POOL_SIZE`    | Number of concurrent workers             | `4`                                       |
| `JOB_LOCK_TTL_MS`     | Distributed lock TTL                     | `30000`                                   |
| `POLL_INTERVAL_MS`    | Queue poll interval                      | `500`                                     |

### 3. Start infrastructure

```bash
docker compose up -d
```

This starts **Redis**, **PostgreSQL**, **Prometheus**, and **Grafana**.

### 4. Run the application

```bash
mvn spring-boot:run
```

| Service     | URL                          |
|-------------|------------------------------|
| API         | http://localhost:8080        |
| Actuator    | http://localhost:8080/actuator/health |
| Prometheus  | http://localhost:9090        |
| Grafana     | http://localhost:3000 (admin/admin) |

---

## API Reference

### Submit a job

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "resize_image",
    "payload": {
      "url": "https://example.com/image.jpg",
      "width": 800,
      "height": 600
    },
    "priority": "NORMAL"
  }'
```

**Success response (202 Accepted):**
```json
{
  "jobId": "550e8400-e29b-41d4-a716-446655440000",
  "status": "QUEUED",
  "createdAt": "2026-11-06T10:30:00Z"
}
```

### Check job status

```bash
curl http://localhost:8080/api/v1/jobs/{jobId}
```

### List jobs (with filters)

```bash
curl "http://localhost:8080/api/v1/jobs?status=COMPLETED&page=0&size=20"
```

### Job history

```bash
curl http://localhost:8080/api/v1/jobs/{jobId}/history
```

### Dead Letter Queue

```bash
# List DLQ jobs
curl http://localhost:8080/api/v1/dlq

# Requeue a DLQ job
curl -X POST http://localhost:8080/api/v1/dlq/{jobId}/requeue
```

### Supported job types

| Type              | Description                     | Required payload fields      |
|-------------------|---------------------------------|------------------------------|
| `resize_image`    | Simulate image resize           | `url`, `width`, `height`     |
| `send_email`      | Simulate sending an email       | `to`, `subject`, `body`      |
| `generate_report` | Simulate report generation      | `reportType`                 |

> New job types can be registered by implementing the `JobHandler` interface and annotating with `@Component`.

---

## Testing

```bash
# Unit + Integration tests
mvn test

# Generate coverage report
mvn jacoco:report
# Report → target/site/jacoco/index.html

# Load testing with Locust
locust -f loadtest/locustfile.py --host=http://localhost:8080

# Load testing with k6
k6 run loadtest/k6-script.js
```

## Observability

Prometheus scrapes (via `/actuator/prometheus`):

- `job_queue_depth` / `job_dlq_depth`
- `jobs_processed_total` (success / failure)
- `jobs_retries_total`
- `jobs_dlq_total`
- `job_processing_duration_seconds`
- `job_active_workers`
- `jobs_by_status`

Grafana (http://localhost:3000) is pre-configured with the Prometheus datasource.

## CI/CD

On every push / PR:

1. Build the project
2. Run unit + integration tests
3. Generate JaCoCo coverage report
4. Upload artifacts

Workflow file: `.github/workflows/ci.yml`

## Reliability patterns used

- Asynchronous processing (long-running work stays off the request path)
- Concurrent workers with distributed locking (no duplicate execution)
- Configurable retries + exponential backoff
- Dead Letter Queue for permanently failed jobs
- Full job lifecycle persistence + history table
- Health checks & metrics for operational visibility

## Roadmap

### Implemented
- Redis queue + worker pool + locking
- Retries + DLQ + requeue API
- PostgreSQL job tracking + history
- Prometheus + Grafana
- Basic CI with coverage
- Pluggable job handlers

### Planned
- [ ] Kafka as alternative queue backend
- [ ] Horizontal worker autoscaling based on queue depth
- [ ] Idempotency keys for duplicate request protection
- [ ] Distributed tracing (OpenTelemetry)
- [ ] Slack / email alerting on high failure rate or DLQ growth
- [ ] Admin UI for job inspection, requeue, and cancellation
- [ ] Kubernetes deployment manifests
- [ ] Authentication & rate limiting

## Contributing

This is currently a personal / learning project. Issues and suggestions are welcome — feel free to open an issue if you spot a bug or have an idea for improvement.

## License

MIT — see [LICENSE](LICENSE) for details.
