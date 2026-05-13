# Project Synopsis — URL Shortener (DevOps)

## 1) Project Title and Problem Statement

**Project Title:** URL Shortener with CI/CD using Docker, Maven, Jenkins & GitHub Actions

**Problem Statement:**
Long URLs are hard to share, remember, and manage. The goal of this project is to build a small, production-style URL shortener service with a clean UI and a REST API, and to demonstrate **DevOps automation** by packaging the application in Docker and implementing **CI/CD pipelines** that build, test, and publish container images reliably.

**Objectives**
- Provide REST endpoints to shorten a URL and redirect using a 6‑character code.
- Ensure correctness via automated tests (`mvn test / mvn clean package`).
- Containerize the Spring Boot app using a multi‑stage Docker build.
- Automate build/test/image publication using Jenkins and GitHub Actions.
- Provide a health endpoint (`/health`) to support Docker health checks and pipeline verification.

---

## 2) Tools / Technologies Used (as per submission options)

**Selected (implemented in this repository):**
- **Docker + Maven + Jenkins** (Jenkins pipeline builds/tests and pushes Docker image)
- **Docker + Maven + GitHub Actions** (Actions workflow builds/tests and pushes Docker image)

**Technology stack (project components)**

| Area | Tools / Tech | Purpose in this project |
|---|---|---|
| Backend | Java 17, Spring Boot 3 (Web, Data JPA) | REST API: shorten + redirect + health |
| Database | H2 (in‑memory by default; file‑based in Docker Compose) | Store shortCode → originalUrl mappings |
| Build & Test | Maven, JUnit (Surefire) | Build executable JAR and run tests |
| Containerization | Docker (multi-stage Dockerfile) | Produce lightweight runtime image |
| Orchestration | Docker Compose | Local run with persistent H2 volume |
| CI/CD | Jenkins (Jenkinsfile), GitHub Actions (`.github/workflows/ci.yml`) | Automated build/test + image push |
| UI | Static HTML + Tailwind (CDN) | Simple frontend to call the API |

---

## 3) Architecture / Workflow Diagram (CI/CD Pipeline)

The repository contains **two CI/CD implementations** (Jenkins and GitHub Actions). Both follow the same DevOps workflow: **checkout → build/test → docker build → push image**.

```mermaid
flowchart TB
  dev[Developer] -->|Push / PR| repo[GitHub Repository]

  %% ───────────────── GitHub Actions ─────────────────
  repo --> gha[GitHub Actions: CI Pipeline]
  subgraph GHA[GitHub Actions Workflow]
    gha1[Checkout] --> gha2[Setup JDK 17 + Maven cache]
    gha2 --> gha3[mvn clean package\n(run tests)]
    gha3 --> gha4[Upload surefire test reports]
    gha4 --> gha5[Setup Docker Buildx]
    gha5 --> gha6[Docker login\nDocker Hub + GHCR]
    gha6 --> gha7[Build image once\nPush tags to Docker Hub + GHCR\n(push only, not PR)]
  end

  %% ───────────────── Jenkins ─────────────────
  repo --> jn[Jenkins: Declarative Pipeline]
  subgraph JENKINS[Jenkins Pipeline]
    jn1[Checkout SCM] --> jn2[mvn clean package\n(run tests)]
    jn2 --> jn3[Publish JUnit reports]
    jn3 --> jn4[docker build + tag latest]
    jn4 --> jn5[Docker Hub login\n(Jenkins credentials)]
    jn5 --> jn6[docker push\nversion + latest]
  end

  %% ───────────────── Delivery ─────────────────
  gha7 --> reg[Container Registries\nDocker Hub / GHCR]
  jn6  --> reg
  reg --> deploy[Run container (Docker / Compose)]
  deploy --> health[/GET \/health returns UP/]
```

**Pipeline inputs/outputs**
- **Input:** Source code (Spring Boot + Maven project)
- **Quality gate:** Maven build + automated tests
- **Output artifact:** Docker image (`url-shortener:latest` and versioned tags)
- **Observability hook:** `/health` used by Docker health checks and smoke checks

---

## 4) Expected Results and Conclusion

**Expected Results**
- The application provides a working URL shortener:
  - `POST /shorten` returns `{ code, short, original }`
  - `GET /{code}` performs an HTTP **302 redirect** to the original URL
  - `GET /health` returns JSON with `status = UP`
- Build and tests run consistently across local and CI environments:
  - Maven produces an executable JAR
  - Automated tests execute in both Jenkins and GitHub Actions
- Dockerization is reproducible and deployment-ready:
  - Multi-stage Docker build creates a smaller runtime image
  - Docker Compose can run the service and persist data via a volume
- CI/CD produces publishable images:
  - Jenkins pushes to Docker Hub
  - GitHub Actions pushes to Docker Hub and GHCR (on `push`, not on PR)

**Conclusion**
This project demonstrates an end-to-end DevOps workflow for a real Spring Boot service: **code → test → containerize → publish → run**. By implementing CI/CD in both **Jenkins** and **GitHub Actions**, the project shows portable automation patterns while keeping the runtime deployment simple through Docker and health checks.

---

### Quick Commands (for viva / demo)
- Local run (no Docker): `mvn spring-boot:run`
- Run tests: `mvn test`
- Build JAR: `mvn clean package`
- Docker Compose: `docker compose up --build`
