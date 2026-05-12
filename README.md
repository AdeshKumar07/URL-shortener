# URL Shortener — DevOps Project

A deployment-ready URL shortener built with **Spring Boot 3**, **H2**, **Docker**, **Jenkins**, **GitHub Actions**, and a responsive **Tailwind CSS** UI.

---

## Project Structure

```text
url-shortener/
├── src/main/java/com/devops/urlshortener/
│   ├── UrlShortenerApplication.java   # Entry point
│   ├── UrlController.java             # REST endpoints
│   ├── UrlMapping.java                # JPA entity
│   └── UrlRepository.java            # Spring Data repository
├── src/main/resources/
│   ├── application.properties         # All config (env-overridable)
│   └── static/index.html             # Tailwind CSS UI
├── src/test/java/com/devops/urlshortener/
│   └── UrlControllerTest.java         # Integration tests
├── .github/workflows/ci.yml           # GitHub Actions pipeline
├── Dockerfile                         # Multi-stage production image
├── docker-compose.yml                 # Local compose with H2 volume
├── Jenkinsfile                        # Jenkins declarative pipeline
└── pom.xml                            # Maven build + dependencies
```

---

## Prerequisites

- Java 17+
- Maven 3.9+
- (Optional) Docker + Docker Compose v2

If you open the UI from Spring Boot (recommended), you do **not** need Node.js.

---

## API Endpoints

| Method | Endpoint   | Description              |
|--------|------------|--------------------------|
| GET    | `/`        | Tailwind CSS web UI      |
| GET    | `/index.html` | Tailwind CSS web UI   |
| POST   | `/shorten` | Shorten a URL            |
| GET    | `/{code}`  | Redirect to original URL |
| GET    | `/health`  | Health check             |
| GET    | `/api`     | API endpoint summary     |

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url": "https://www.google.com"}'
```

---

## Quick Start (Local)

1) Start the Spring Boot app:

```bash
mvn spring-boot:run
```

2) Open the UI (served by Spring Boot):

- http://localhost:8080/

3) Paste a URL (with or without `https://`) and click **Shorten**.

### Important: How to open the UI

This project includes a static UI at `src/main/resources/static/index.html`.

- Recommended: open it from Spring Boot (`http://localhost:8080/`) so the UI and API are **same-origin**.
- If you open the file using VS Code Live Server (usually `http://127.0.0.1:5500/.../index.html`), the UI will call the backend at `http://localhost:8080` using CORS.

### Verify API quickly (curl)

Health check:

```bash
curl http://localhost:8080/health
```

Shorten:

```bash
curl -X POST http://localhost:8080/shorten \
  -H "Content-Type: application/json" \
  -d '{"url":"https://www.linkedin.com"}'
```

---

## Run With Docker Compose

```bash
docker compose up --build
```

Open:

- http://localhost:8080/

H2 data is stored in the `url-shortener-data` volume — links survive container restarts.

---

## Build and Test

```bash
mvn test
```

Build a runnable JAR:

```bash
mvn clean package
```

Run the built JAR:

```bash
java -jar target/*.jar
```

---

## Configuration (Environment Variables)

All key settings can be overridden by environment variables:

- `SERVER_PORT` (default `8080`)
- `SPRING_DATASOURCE_URL` (default in-memory H2)
- `SPRING_JPA_HIBERNATE_DDL_AUTO` (default `update`)

Docker Compose already sets a file-based H2 URL so data persists in a volume.

---

## GitHub Actions Setup

Secrets required in your repository (`Settings → Secrets → Actions`):

| Secret            | Value                     |
|-------------------|---------------------------|
| `DOCKER_USERNAME` | Your Docker Hub username  |
| `DOCKER_PASSWORD` | Docker Hub access token   |

`GITHUB_TOKEN` is provided automatically by GitHub Actions.

The pipeline:
1. Builds and tests with Maven
2. Uploads surefire test results as an artifact
3. Builds the Docker image once with Buildx
4. Pushes to both Docker Hub and GHCR in a single step

---

## Jenkins Setup

1. Replace `YOUR_DOCKERHUB_USERNAME` in `Jenkinsfile` with your Docker Hub username.
2. Add Docker Hub credentials in Jenkins (`Manage Jenkins → Credentials`) with ID `docker-hub-creds`.
3. Configure JDK 17 as `JDK17` and Maven 3.9 as `Maven3.9` in Jenkins Global Tool Configuration.

---

## Troubleshooting

### UI shows “Offline”

- Check the backend is running: open http://localhost:8080/health (should return JSON with `"status":"UP"`).
- If you are using VS Code Live Server (`127.0.0.1:5500`), the UI calls the API on `http://localhost:8080`. Ensure Spring Boot is actually running on port 8080.

### “Unexpected end of JSON input” / “Could not shorten this URL”

This happens when the UI receives **HTML/empty text** instead of JSON (commonly because it called the wrong server).

- Recommended: open the UI from Spring Boot (http://localhost:8080/) not from Live Server.
- If you do use Live Server: keep Spring Boot running on `http://localhost:8080` and refresh the page.

### Live Server + backend on a different port

- Option 1: Run Spring Boot on 8080.
- Option 2: Update the fallback URL in the UI (search for `API_BASE_CANDIDATES` in `index.html`).

---

## Bugs Fixed

| # | File | Bug | Fix |
|---|------|-----|-----|
| 1 | `UrlControllerTest.java` | `forwardedUrl("index.html")` fails — Spring Boot serves static files directly, not via Servlet forward | Replaced with `status().isOk()` + content assertion on `/index.html` |
| 2 | `UrlControllerTest.java` | `GET /xxxxxx` bypasses the controller regex `[a-f0-9]{6}` (no-handler 404, not controller 404) | Changed to `GET /000000` — valid hex format, absent from DB |
| 3 | `UrlControllerTest.java` | Missing test for empty JSON body `{}` (null url field) | Added `missingUrlFieldReturnsBadRequest` test |
| 4 | `Dockerfile` | `eclipse-temurin:17-jre-alpine` does not include `wget`, breaking `HEALTHCHECK` | Added `apk add --no-cache wget` before the HEALTHCHECK |
| 5 | `docker-compose.yml` | Missing `platform: linux/amd64` causes image mismatch on ARM machines | Added `platform` declaration |
| 6 | `docker-compose.yml` | H2 file-mode URL missing `AUTO_RECONNECT=TRUE` | Added to `SPRING_DATASOURCE_URL` |
| 7 | `docker-compose.yml` | No log rotation — container logs can fill host disk | Added `json-file` driver with `max-size`/`max-file` |
| 8 | `ci.yml` | Docker image built from scratch twice (once per registry) | Merged into one `build-push-action` step with both tag sets |
| 9 | `ci.yml` | Missing `setup-buildx-action` — layer cache and multi-platform builds unavailable | Added `docker/setup-buildx-action@v3` + GHA cache |
| 10 | `ci.yml` | PRs also push images (secrets not available on fork PRs, causes pipeline failure) | Added `push: ${{ github.event_name != 'pull_request' }}` |
| 11 | `Jenkinsfile` | `always` block uses single-quoted string — `${env.BUILD_NUMBER}` printed literally | Changed to double-quoted Groovy GString |
| 12 | `application.properties` | H2 2.x treats `VALUE` as reserved keyword; schema generation can silently fail | Added `NON_KEYWORDS=VALUE` to JDBC URL |
| 13 | `.gitignore` | `.env` and `docker-compose.override.yml` not excluded — risk of secret leakage | Added exclusion rules |
| 14 | `index.html` | Opening UI via VS Code Live Server calls `/health` and `/shorten` on port 5500 (wrong server), causing “Offline” + JSON parse errors | Added API base fallback to `http://localhost:8080` + safer response parsing |
| 15 | `WebConfig.java` | Browser blocks Live Server → Spring Boot calls due to CORS | Added CORS mappings for `/shorten` and `/health` from `localhost:5500` |
| 16 | `GlobalExceptionHandler.java` | Some missing routes could be turned into 500 JSON errors | Added explicit 404 handler for MVC “not found” exceptions |
