# Distributed Job Processing Platform

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-brightgreen)](https://spring.io/projects/spring-boot)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build](https://img.shields.io/github/actions/workflow/status/SrivastavSudiksha/<repo-name>/ci.yml?branch=main)](../../actions)

A production-style distributed job processing system built with **Java, Spring Boot, Redis, and PostgreSQL** for reliable asynchronous task execution — with automated testing, CI/CD, and real-time observability via Prometheus and Grafana.

Clients submit jobs through a REST API. Jobs are queued, picked up by a concurrent worker pool, executed, retried on failure, and routed to a Dead Letter Queue (DLQ) if they exhaust their retry budget — all while every stage is tracked in Postgres and surfaced live on a Grafana dashboard.

## Why this project

Most portfolio projects stop at CRUD. This one is built around the problems a real backend service actually has to solve: decoupling submission from execution, handling partial failure gracefully, avoiding duplicate work under concurrency, and giving whoever owns the service in production enough visibility to trust it.

**Core path:** async processing → message queue → concurrent workers → fault tolerance → persistence → observability → CI/CD.

## Features

- **Async job processing** — Redis-backed queue decouples submission from execution
- **Concurrent worker pool** — multiple workers process jobs in parallel with safe job locking to prevent duplicate execution
- **Retries + Dead Letter Queue** — configurable retry limits; permanently failed jobs are isolated for later inspection/reprocessing
- **Persistent job tracking** — PostgreSQL stores status, timestamps, retry counts, results, and full lifecycle history
- **REST API** — submit jobs, check status, view history
- **Observability** — Prometheus metrics + Grafana dashboards for queue depth, throughput, latency percentiles, and worker health
- **Automated testing** — unit, integration, and load tests (Locust/k6)
- **CI/CD** — GitHub Actions runs tests and generates coverage reports (JaCoCo) on every push

## Architecture

```
Client
  │
  ▼
Spring Boot REST API
  │
  ▼
Redis Job Queue
  │
  ├──► Worker 1 ──┐
  ├──► Worker 2 ──┤──► PostgreSQL (job state, results, history)
  └──► Worker N ──┘
  │
  ▼
Prometheus ──► Grafana
```

**Request flow:**

1. Client submits a job via the REST API
2. API validates the request and persists job metadata
3. Job is pushed to the Redis queue
4. An available worker claims and locks the job
5. Worker executes the task and updates status
6. Failed jobs are retried per the configured policy
7. Jobs exceeding the retry limit move to the DLQ
8. Prometheus scrapes runtime metrics; Grafana visualizes them in real time

**Job lifecycle:**

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

| Category | Technology |
|---|---|
| Language | Java 17 |
| Backend | Spring Boot |
| Queue | Redis |
| Database | PostgreSQL |
| Testing | JUnit, Locust / k6 |
| Coverage | JaCoCo |
| CI/CD | GitHub Actions |
| Monitoring | Prometheus, Grafana |
| Containerization | Docker, Docker Compose |

## Project structure

```
src/
├── main/
│   ├── java/
│   │   └── ...
│   └── resources/
│       └── application.yml
├── test/
│   └── ...
├── docker/
│   ├── prometheus/
│   └── grafana/
├── loadtest/
│   └── ...
├── docker-compose.yml
├── pom.xml
└── README.md
```

## Getting started

### Prerequisites

- Java 17+
- Maven
- Docker & Docker Compose

### 1. Clone the repository

```bash
git clone https://github.com/SrivastavSudiksha/<repo-name>.git
cd <repo-name>
```

### 2. Configure environment

Copy the example config and adjust as needed:

```bash
cp .env.example .env
```

| Variable | Description | Default |
|---|---|---|
| `DB_URL` | PostgreSQL connection string | `jdbc:postgresql://localhost:5432/jobs` |
| `REDIS_HOST` | Redis host | `localhost` |
| `REDIS_PORT` | Redis port | `6379` |
| `MAX_RETRIES` | Retry attempts before DLQ | `3` |
| `WORKER_POOL_SIZE` | Number of concurrent workers | `4` |

### 3. Start infrastructure

```bash
docker compose up -d
```

This starts Redis, PostgreSQL, Prometheus, and Grafana.

### 4. Run the application

```bash
mvn spring-boot:run
```

API available at `http://localhost:8080`.

## API examples

**Submit a job**

```bash
curl -X POST http://localhost:8080/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "type": "resize_image",
    "payload": { "url": "https://example.com/image.jpg" }
  }'
```

**Check job status**

```bash
curl http://localhost:8080/jobs/{jobId}
```

**List job history**

```bash
curl http://localhost:8080/jobs?status=COMPLETED
```

## Testing

```bash
mvn test                 # unit + integration tests
locust -f loadtest.py    # load testing (or: k6 run loadtest.js)
```

Coverage reports are generated via JaCoCo and published as part of the CI pipeline.

## Observability

Prometheus tracks queue depth, jobs processed, success/failure rate, retry count, processing latency, and worker health/heartbeat.

Grafana (`http://localhost:3000`) visualizes throughput, p50/p95/p99 latency, queue backlog, failure rate, retry activity, and worker availability in real time.

## CI/CD

```
Push → GitHub Actions → Build → Tests → Coverage → Build Verification
```

On every push, GitHub Actions builds the app, runs the full test suite, generates JaCoCo coverage reports, and verifies the build.

## Reliability patterns used

- Asynchronous processing to keep long-running jobs off the request path
- Worker concurrency for parallel execution
- Job locking to prevent duplicate processing
- Automatic retries for transient failures
- Dead Letter Queue for permanently failed jobs
- Persistent job state for full lifecycle tracking
- Health monitoring for worker availability

## Roadmap

- [ ] Kafka-based queue as an alternative to Redis
- [ ] Horizontal worker autoscaling based on queue depth
- [ ] Exponential backoff for retries
- [ ] Idempotency keys for duplicate request protection
- [ ] Distributed tracing with OpenTelemetry
- [ ] Slack / email alerting
- [ ] Admin dashboard for job requeue and cancellation
- [ ] Kubernetes deployment

## Contributing

This is currently a personal/learning project, but issues and suggestions are welcome — feel free to open an issue if you spot a bug or have an idea for improvement.

## License

MIT — see [LICENSE](LICENSE) for details.
