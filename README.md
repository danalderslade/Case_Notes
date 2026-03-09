# Case Notes Module

A production-ready microservice module for adding, viewing, and soft-deleting case notes within a case management system. Built for multi-country deployment with per-country data isolation using ISO 3166-1 alpha-2 codes.

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    Kubernetes Cluster                        │
│                                                              │
│  ┌──────────────┐    ┌───────────────────┐    ┌──────────┐  │
│  │  React 18    │───▶│  Spring Boot 3.3  │───▶│ PostgreSQL│  │
│  │  (TipTap)    │    │  REST API         │    │  16       │  │
│  │  Port 3000   │    │  Port 8080        │    │  Port 5432│  │
│  └──────────────┘    └───────────────────┘    └──────────┘  │
│         │                    │                               │
│    nginx ingress         Swagger UI                          │
│    TLS termination       /api/swagger-ui.html                │
└─────────────────────────────────────────────────────────────┘
```

### Technology Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Frontend | React + TipTap rich text | 18.x / 2.x |
| Backend | Spring Boot | 3.3.x (Java 21) |
| Database | PostgreSQL | 16 |
| API Docs | SpringDoc OpenAPI (Swagger) | 2.5 |
| Container | Docker + Kubernetes | - |
| DB Migrations | Flyway | bundled |

---

## Data Model

### Per-Country Table Design

Each country gets its own isolated notes table, named `case_notes_{iso_alpha2}` (e.g. `case_notes_gb`, `case_notes_us`). This provides:

- **Regulatory isolation** — data residency requirements (GDPR, etc.) are met by country
- **Performance** — queries never cross country boundaries
- **Auditability** — table-level access controls per jurisdiction

New countries are provisioned via:
1. Insert into `ref_countries`
2. Call `SELECT create_country_notes_table('XX')` or use the API `POST /v1/countries/{code}/provision`

### Note Record Schema

| Column | Type | Description |
|--------|------|-------------|
| `id` | UUID (PK) | Auto-generated UUID v4 |
| `case_id` | VARCHAR(100) | Reference to the external case |
| `staff_id` | VARCHAR(100) | Author — maps to HR/auth system |
| `country_code` | CHAR(2) | ISO 3166-1 alpha-2 (enforced by CHECK constraint) |
| `note_content` | TEXT | **Rich text as HTML** (TipTap output) |
| `note_plain_text` | TEXT | Auto-extracted plain text (for FTS) |
| `is_deleted` | BOOLEAN | **Soft delete marker** (never physically deleted) |
| `deleted_at` | TIMESTAMPTZ | When soft-deleted |
| `deleted_by` | VARCHAR(100) | Who deleted it |
| `delete_reason` | VARCHAR(500) | Optional reason |
| `created_at` | TIMESTAMPTZ | Auto-set on insert |
| `updated_at` | TIMESTAMPTZ | Auto-updated by trigger |
| `version` | INTEGER | Optimistic locking counter |

---

## API Reference

Swagger UI is available at: `http://localhost:8080/api/swagger-ui.html`
OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

### Endpoints

#### Notes

| Method | URL | Description |
|--------|-----|-------------|
| `POST` | `/v1/countries/{cc}/notes` | Create a note |
| `GET` | `/v1/countries/{cc}/notes?caseId=X` | List notes for a case (paginated) |
| `GET` | `/v1/countries/{cc}/notes/{id}` | Get single note |
| `GET` | `/v1/countries/{cc}/notes/search?q=text` | Full-text search |
| `PUT` | `/v1/countries/{cc}/notes/{id}` | Update note content |
| `DELETE` | `/v1/countries/{cc}/notes/{id}` | Soft delete note |

#### Countries

| Method | URL | Description |
|--------|-----|-------------|
| `GET` | `/v1/countries` | List active ISO countries |
| `POST` | `/v1/countries/{cc}/provision` | Provision a new country table |

### Example Requests

**Create a note (GB)**
```bash
curl -X POST http://localhost:8080/api/v1/countries/GB/notes \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "caseId": "CASE-2024-00001",
    "staffId": "STAFF-001",
    "countryCode": "GB",
    "noteContent": "<p>Initial contact made. Client confirmed address.</p>"
  }'
```

**List notes for a case**
```bash
curl "http://localhost:8080/api/v1/countries/GB/notes?caseId=CASE-2024-00001&page=0&size=20" \
  -H "Authorization: Bearer <token>"
```

**Soft delete a note**
```bash
curl -X DELETE http://localhost:8080/api/v1/countries/GB/notes/{uuid} \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{
    "deletedBy": "STAFF-002",
    "deleteReason": "Note added to incorrect case"
  }'
```

---

## Running Locally

### Prerequisites
- Docker & Docker Compose
- Java 21 (for local backend dev)
- Node.js 22 (for local frontend dev)

### Quick Start (Docker Compose)

```bash
# Clone and start everything
git clone <your-repo>
cd case-notes

# Start all services
docker-compose up -d

# Check services are running
docker-compose ps

# Access:
# Frontend:    http://localhost:3000
# API:         http://localhost:8080/api
# Swagger UI:  http://localhost:8080/api/swagger-ui.html
```

### Local Development

**Backend:**
```bash
cd backend
# Start only the DB
docker-compose up postgres -d
# Run Spring Boot
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

**Frontend:**
```bash
cd frontend
npm install
VITE_API_BASE_URL=http://localhost:8080/api npm run dev
# → http://localhost:5173
```

---

## Kubernetes Deployment

```bash
# Create namespace and apply all manifests
kubectl create namespace case-management
kubectl apply -f k8s/manifests.yaml -n case-management

# Check rollout
kubectl rollout status deployment/case-notes-api -n case-management
kubectl rollout status deployment/case-notes-frontend -n case-management

# View logs
kubectl logs -l app=case-notes-api -n case-management --tail=100 -f

# Port-forward for local testing
kubectl port-forward svc/case-notes-api-service 8080:8080 -n case-management
```

### Adding a New Country in K8s

```bash
# 1. Insert country into ref_countries (via DB migration or SQL)
# 2. Call the provision endpoint
curl -X POST http://your-domain/api/v1/countries/NZ/provision \
  -H "Authorization: Bearer <admin-token>"
```

---

## Security Considerations

1. **Authentication** — JWT bearer token is required for all endpoints (except Swagger UI). Wire your IdP via `spring-security-oauth2-resource-server` (configuration stub in `SecurityConfig`).
2. **Authorisation** — Add `@PreAuthorize` annotations to restrict which staff can delete vs. view notes.
3. **CORS** — Configured via `CORS_ORIGINS` env var.
4. **SQL Injection** — Country code is validated against a regex `^[a-z]{2}$` before use in dynamic table names. All other queries use parameterised statements.
5. **Rich Text Sanitisation** — Consider adding server-side HTML sanitisation using OWASP Java HTML Sanitizer before storing `noteContent` to prevent stored XSS.
6. **Secrets** — Replace `stringData` in K8s Secret with an External Secrets Operator + Vault/AWS Secrets Manager integration for production.
7. **Audit Trail** — `case_notes_audit` table captures all mutations with actor, timestamp, IP and old/new values.
8. **Network Policies** — K8s NetworkPolicies restrict database access to the API pod only; frontend can only reach the API.

---

## Things You May Want to Add

| Feature | Notes |
|---------|-------|
| **Role-based access** | Distinguish between read-only, write, and delete roles per country |
| **Note attachments** | Add file upload support (store in S3/blob, reference by note ID) |
| **Note templates** | Pre-defined note structures for common case actions |
| **@mentions** | Notify other staff members referenced in notes |
| **Note history/versioning** | Store previous versions for full edit history |
| **Export to PDF** | Generate PDF of all notes for a case |
| **Restore deleted notes** | Add a `PATCH /{id}/restore` endpoint |
| **Rate limiting** | Add Spring Boot rate limiter or API Gateway throttling |
| **Distributed tracing** | Integrate OpenTelemetry / Jaeger for tracing across services |
| **Caching** | Add Redis cache for frequent `GET` queries |

---

## Project Structure

```
case-notes/
├── backend/
│   ├── src/main/java/com/casemanagement/notes/
│   │   ├── CaseNotesApplication.java
│   │   ├── controller/
│   │   │   ├── CaseNoteController.java    # REST endpoints
│   │   │   └── CountryController.java     # Country management
│   │   ├── service/
│   │   │   └── CaseNoteService.java       # Business logic
│   │   ├── repository/
│   │   │   ├── CaseNoteRepository.java    # Dynamic table routing
│   │   │   └── CountryRepository.java     # JPA country repo
│   │   ├── model/
│   │   │   ├── CaseNote.java              # Domain model
│   │   │   └── Country.java               # JPA entity
│   │   ├── dto/
│   │   │   └── CaseNoteDtos.java          # All request/response DTOs
│   │   ├── config/
│   │   │   └── AppConfig.java             # OpenAPI + Security
│   │   └── exception/
│   │       └── GlobalExceptionHandler.java
│   ├── src/main/resources/application.yml
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── App.tsx                        # Main module UI
│   │   ├── api/notesApi.ts               # Axios API client
│   │   └── types/index.ts                # TypeScript interfaces
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── db/
│   └── 01_schema.sql                     # Full PostgreSQL schema
├── k8s/
│   └── manifests.yaml                    # All K8s resources
└── docker-compose.yml                    # Local development
```
