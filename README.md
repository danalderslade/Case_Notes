
# Case Notes

Case Notes is a case-focused notes application built with React, Spring Boot, and PostgreSQL. It supports creating, listing, searching, updating, and soft-deleting notes while keeping data isolated by country through per-country database tables.

## What This System Does

- Stores notes against a `caseId`
- Uses `noteId` for single-note operations
- Uses `countryCode` as a required routing/filter input for data isolation
- Stores rich text as HTML and derives plain text for search indexing
- Supports soft delete rather than hard delete
- Uses optimistic locking for updates via a `version` field in the API contract

## Architecture

### Runtime shape

```text
Browser
  |
  v
React 18 + Vite frontend
served by Nginx on :3000
  |
  v
Spring Boot API on :8080/api
  |
  v
PostgreSQL 16 on :5432
```

### Backend design

- Spring Boot exposes REST endpoints under `/api/v1`
- Controllers are identifier-first:
  - collection routes are case-based
  - single-resource routes are note-based
- `CaseNoteService` contains the core business logic
- `CaseNoteRepository` and `CountryRepository` use Spring JDBC, not JPA
- Country isolation is implemented by selecting a physical table named `case_notes_{countryCode}`

### Frontend design

- React 18 application using TipTap for note editing
- Axios client configured with `VITE_API_BASE_URL` and defaulting to `/api`
- Main workflow:
  - choose a country
  - enter a case identifier
  - list notes
  - search notes
  - create, edit, or soft-delete notes

## Technology Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite 5, TipTap 2, Tailwind CSS |
| Backend | Spring Boot 3.3.5, Java 21 target |
| Data access | Spring JDBC |
| Database | PostgreSQL 16 |
| API docs | SpringDoc OpenAPI / Swagger UI |
| Local runtime | Docker Compose |
| Deployment manifests | Kubernetes YAML |
| Testing | JUnit 5, Mockito, Spring MVC test |

## Data Model

Each active country has its own notes table, for example:

- `case_notes_gb`
- `case_notes_us`

This allows country-level data isolation while keeping the API contract uniform.

### Note fields

| Field | Meaning |
|---|---|
| `id` | UUID for the note |
| `case_id` | External case identifier |
| `staff_id` | Author identifier |
| `country_code` | ISO 3166-1 alpha-2 country code |
| `note_content` | Rich text HTML |
| `note_plain_text` | Plain text extracted from HTML for search |
| `is_deleted` | Soft delete flag |
| `deleted_at` | Soft delete timestamp |
| `deleted_by` | User who deleted the note |
| `delete_reason` | Optional delete reason |
| `created_at` | Creation timestamp |
| `updated_at` | Update timestamp |
| `version` | Optimistic locking counter |

## API Overview

Base URL locally:

- API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api/v3/api-docs`

### Notes endpoints

| Method | Path | Purpose |
|---|---|---|
| `POST` | `/v1/cases/{caseId}/notes?countryCode=CC` | Create a note for a case |
| `GET` | `/v1/cases/{caseId}/notes?countryCode=CC&page=0&size=20` | List notes for a case |
| `GET` | `/v1/notes/{noteId}?countryCode=CC&includeDeleted=false` | Fetch one note |
| `GET` | `/v1/notes/search?countryCode=CC&q=text&page=0&size=20&includeDeleted=false` | Search notes within a country |
| `PUT` | `/v1/notes/{noteId}?countryCode=CC` | Update note content |
| `DELETE` | `/v1/notes/{noteId}?countryCode=CC` | Soft-delete a note |

### Countries endpoints

| Method | Path | Purpose |
|---|---|---|
| `GET` | `/v1/countries` | List active countries |
| `POST` | `/v1/countries/{countryCode}/provision` | Provision a country notes table |

### Request examples

Create a note:

```bash
curl -X POST "http://localhost:8080/api/v1/cases/CASE-2024-00001/notes?countryCode=GB" \
  -H "Content-Type: application/json" \
  -d '{
    "staffId": "STAFF-001",
    "noteContent": "<p>Initial contact made. Client confirmed address.</p>"
  }'
```

List notes for a case:

```bash
curl "http://localhost:8080/api/v1/cases/CASE-2024-00001/notes?countryCode=GB&page=0&size=20"
```

Get one note:

```bash
curl "http://localhost:8080/api/v1/notes/{uuid}?countryCode=GB&includeDeleted=false"
```

Search notes:

```bash
curl "http://localhost:8080/api/v1/notes/search?countryCode=GB&q=address&page=0&size=20&includeDeleted=false"
```

Update a note:

```bash
curl -X PUT "http://localhost:8080/api/v1/notes/{uuid}?countryCode=GB" \
  -H "Content-Type: application/json" \
  -d '{
    "noteContent": "<p>Updated note text</p>",
    "version": 1
  }'
```

Soft delete a note:

```bash
curl -X DELETE "http://localhost:8080/api/v1/notes/{uuid}?countryCode=GB" \
  -H "Content-Type: application/json" \
  -d '{
    "deletedBy": "STAFF-002",
    "deleteReason": "Note added to incorrect case"
  }'
```

Provision a country table:

```bash
curl -X POST "http://localhost:8080/api/v1/countries/NZ/provision"
```

## Local Development

### Prerequisites

- Docker with Docker Compose support
- Java 21 for backend-only local execution
- Node.js 22 for frontend-only local execution

### Run everything with Docker Compose

From the repository root:

```bash
docker compose up -d
docker compose ps
```

Local URLs:

- Frontend: `http://localhost:3000`
- API: `http://localhost:8080/api`
- Swagger UI: `http://localhost:8080/api/swagger-ui.html`

### Backend-only local run

Start only Postgres from the repository root:

```bash
docker compose up postgres -d
```

Then start the backend:

```bash
cd backend
mvn spring-boot:run
```

### Frontend-only local run

```bash
cd frontend
npm install
VITE_API_BASE_URL=http://localhost:8080/api npm run dev
```

## Testing

### Backend tests included

- `CaseNoteServiceTest` covers the service layer
- `CaseNoteControllerTest` verifies the create-note controller contract

Run backend tests:

```bash
cd backend
mvn test
```

## Deployment Notes

- Dockerfiles exist for backend and frontend
- `docker-compose.yml` is the main local orchestration file
- `k8s/manifests.yaml` contains Kubernetes resources for deployment packaging

## Current Security Position

This codebase currently provides CORS configuration, but it does not yet enforce authentication or authorization in the application layer.

That means the following would still need to be added for production use:

- Spring Security integration
- JWT or OAuth2 resource server configuration
- role-based authorization for create, update, delete, and provisioning actions
- richer server-side HTML sanitization if user-supplied rich text is not fully trusted

## Project Structure

```text
Case_Notes/
├── backend/
│   ├── src/main/java/com/casemanagement/notes/
│   │   ├── CaseNotesApplication.java
│   │   ├── config/
│   │   │   ├── OpenApiConfig.java
│   │   │   └── WebConfig.java
│   │   ├── controller/
│   │   │   ├── CaseNoteController.java
│   │   │   └── CountryController.java
│   │   ├── dto/
│   │   │   └── CaseNoteDtos.java
│   │   ├── exception/
│   │   │   ├── BadRequestException.java
│   │   │   ├── ConflictException.java
│   │   │   ├── GlobalExceptionHandler.java
│   │   │   └── NotFoundException.java
│   │   ├── repository/
│   │   │   ├── CaseNoteRepository.java
│   │   │   └── CountryRepository.java
│   │   └── service/
│   │       └── CaseNoteService.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/V1__init.sql
│   ├── src/test/java/com/casemanagement/notes/
│   │   ├── controller/CaseNoteControllerTest.java
│   │   └── service/CaseNoteServiceTest.java
│   ├── src/test/resources/mockito-extensions/
│   │   └── org.mockito.plugins.MockMaker
│   ├── Dockerfile
│   └── pom.xml
├── frontend/
│   ├── src/
│   │   ├── App.tsx
│   │   ├── api/notesApi.ts
│   │   └── types/index.ts
│   ├── Dockerfile
│   ├── nginx.conf
│   └── package.json
├── db/
│   └── 01_schema.sql
├── k8s/
│   └── manifests.yaml
└── docker-compose.yml
```

## Known Repository Hygiene Issue

Build output directories such as `backend/target` and `frontend/dist` are currently present in the repository history and have been committed during recent changes. They are generated artifacts, not source-of-truth application code.

If you want this cleaned up next, the right follow-up is:

1. add ignore rules for generated build outputs
2. remove tracked generated artifacts from git
3. commit that cleanup separately
