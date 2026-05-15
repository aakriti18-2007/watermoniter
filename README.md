# Team Task Manager

A full-stack Spring Boot web application for collaborative project and task management. Users can sign up, create projects, invite teammates, assign tasks, update task status, and track progress through a dashboard.

## Features

- Signup and login with Spring Security, BCrypt password hashing, and session authentication
- Project creation where the creator becomes the project Admin
- Project team management with Admin and Member access rules
- Task creation with title, description, due date, priority, status, and assignee
- Member permissions limited to viewing projects and updating their own assigned task status
- Dashboard with total tasks, tasks by status, tasks per user, overdue tasks, and assigned tasks
- REST APIs backed by JPA relationships for users, projects, memberships, and tasks
- H2 local database and PostgreSQL-ready Railway profile

## Tech Stack

- Java 17
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- H2 for local development
- PostgreSQL for Railway deployment
- Static HTML, CSS, and JavaScript frontend served by Spring Boot

## Local Setup

```powershell
./mvnw.cmd test
./mvnw.cmd spring-boot:run
```

Open [http://localhost:8080](http://localhost:8080).

Seeded demo accounts:

- Admin: `admin` / `admin123`
- Operator: `operator` / `operator123`
- Viewer: `viewer` / `viewer123`

New signup users are created as members. A user becomes a project Admin when they create a project.

## Railway Deployment

1. Push this repository to GitHub.
2. Create a new Railway project from the GitHub repository.
3. Add a PostgreSQL database service in Railway.
4. Set the application environment variables:

```text
SPRING_PROFILES_ACTIVE=postgres
```

The Postgres profile reads Railway's `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, and `PGPASSWORD` variables. If you prefer custom names, set:

```text
DB_URL=jdbc:postgresql://<host>:<port>/<database>
DB_USERNAME=<user>
DB_PASSWORD=<password>
```

Railway also provides `PORT`; the app already reads it through `server.port=${PORT:8080}`. Railway's current Spring Boot guide supports GitHub or CLI deploys, and Railway will use the included `Dockerfile` when it is present.

5. Deploy. The `Dockerfile` builds the Spring Boot jar and runs it on port 8080.

## API Summary

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{projectId}`
- `POST /api/projects/{projectId}/members`
- `DELETE /api/projects/{projectId}/members/{userId}`
- `POST /api/projects/{projectId}/tasks`
- `PUT /api/projects/{projectId}/tasks/{taskId}`
- `DELETE /api/projects/{projectId}/tasks/{taskId}`
- `GET /api/dashboard`

## Submission Checklist

- Live URL: add your Railway public URL here
- GitHub repo: add your repository URL here
- Demo video: record a 2-5 minute walkthrough covering signup, project creation, member invite, task assignment, status update, dashboard metrics, and Railway deployment
