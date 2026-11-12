# SOS Videos — API

REST API for a subscription-based video content platform. Subscribers get secure, time-limited access to daily video content hosted on Google Drive, with downloads served directly from Google's CDN — the VPS only issues the token (~1 KB), so the video bytes never pass through the server.

## Problem it solves

The client sold daily video content to subscribers and needed a way to:

- Gate access behind an active subscription (integrated with Kiwify webhooks)
- Deliver large video files without saturating the VPS bandwidth
- Let subscribers download all videos for a given day in one click (desktop loop or Drive folder link on mobile)
- Automatically expire subscriptions past their end date
- Give admins a panel to manage subscribers, videos, and terms of service

## Tech Stack

- **Java 21** · Spring Boot 3
- **Spring Security** + JWT (stateless)
- **PostgreSQL** + Flyway migrations
- **Google Drive API** (service account) — CDN redirect for downloads, temporary folder links for mobile batch
- **Kiwify webhooks** — auto-activate / cancel / expire subscriptions
- **Springdoc OpenAPI** — Swagger UI at `/swagger-ui.html`
- **Testcontainers** — integration tests with a real PostgreSQL instance

## Features

- Token-based download system (60s single-use for individual files, 5-min for Drive folder links)
- Subscription visibility window: subscribers see videos from up to 2 days before their start date
- Daily scheduler (midnight, São Paulo timezone) auto-expires subscriptions past `endDate`
- Role-based access: `ADMIN`, `SUBSCRIBER`, `SUPPORT`
- Terms of service gate — subscribers must accept the active term before accessing content

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- A Google Cloud service account with Drive API access (`credentials.json`)

### Setup

1. **Clone the repo**

```bash
git clone https://github.com/ocainadev/sos-videos-api.git
cd sos-videos-api
```

2. **Create the database**

```sql
CREATE DATABASE sosvideos;
```

3. **Configure environment variables**

Copy `.env.example` to `.env` and fill in your values:

```bash
cp .env.example .env
```

| Variable | Description |
|---|---|
| `DB_URL` | JDBC URL, e.g. `jdbc:postgresql://localhost:5432/sosvideos` |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Base64 secret — generate with `openssl rand -base64 32` |
| `JWT_EXPIRATION` | Token TTL in ms (default `86400000` = 24 h) |
| `G_CREDENTIALS` | Path to your `credentials.json` service account file |
| `G_TOKEN` | Directory where Drive OAuth tokens are stored |
| `ADMIN_E` | Admin e-mail (seeded on first boot) |
| `ADMIN_P` | Admin password (seeded on first boot) |
| `KIWIFY_WEBHOOK_SECRET` | Secret configured in the Kiwify dashboard |
| `CORS_ALLOWED` | Comma-separated allowed origins |

4. **Place your Google credentials**

Put your service account `credentials.json` in the project root (it is gitignored).

5. **Run**

```bash
./mvnw spring-boot:run
```

The API starts on `http://localhost:8080`. Swagger UI: `http://localhost:8080/swagger-ui.html`.

### Run with Docker Compose

```bash
docker compose up --build
```

### Run tests

```bash
./mvnw test
```

Tests use Testcontainers and spin up a real PostgreSQL instance automatically — no local database required.

## Project Structure

```
src/main/java/com/sosvideos/
├── admin/          # Admin endpoints (subscribers, CSVimport)
├── config/         # Security, CORS, app properties, DB seeder
├── common/         # AppConstants (timezone, visibility window)
├── download/       # Token generation and CDN redirect
├── drivefolder/    # Temporary Drive folder link endpoint
├── kiwify/         # Webhook handler (order, subscription events)
├── subscription/   # Entity, service, scheduler, repository
├── terms/          # Terms of service and acceptance tracking
├── tutorial/       # Tutorial content management
├── user/           # User entity and admin user management
└── video/          # Video entity, visibility logic, admin CRUD
```

## Deployment

A sample Nginx config is provided at `deploy/nginx-host.conf`. Replace `yourdomain.com` with your actual domain. A `docker-compose.yml` is included for containerised deployments.
