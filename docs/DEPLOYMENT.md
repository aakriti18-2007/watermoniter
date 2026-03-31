# Deployment Guide

## Local development

The app now supports two profiles:

- `local`: file-backed H2 database for easy development
- `postgres`: PostgreSQL-backed setup for production-style deployment

Run locally with the default H2 profile:

```powershell
cd C:\Users\Dell\Downloads\watermoniter\watermoniter
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=9090"
```

Open `http://localhost:9090`.

## Run with PostgreSQL locally

1. Copy `.env.example` to `.env`
2. Set:
   - `SPRING_PROFILES_ACTIVE=postgres`
   - `DB_URL=jdbc:postgresql://localhost:5432/watermoniter`
   - `DB_USERNAME=postgres`
   - `DB_PASSWORD=your_password`
3. Start the app:

```powershell
cd C:\Users\Dell\Downloads\watermoniter\watermoniter
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:DB_URL="jdbc:postgresql://localhost:5432/watermoniter"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.jvmArguments=-Dserver.port=9090"
```

## Docker only

```powershell
docker build -t watermoniter .
docker run -p 8080:8080 --env-file .env watermoniter
```

Open `http://localhost:8080`.

## Docker Compose with PostgreSQL

Create a `.env` file from `.env.example`, then run:

```powershell
docker compose up --build
```

This starts:

- `postgres` on port `5432`
- `watermoniter-app` on port `8080`

Open `http://localhost:8080`.

## Render or Railway

1. Push this project to GitHub.
2. Create a PostgreSQL database on the platform.
3. Create a Web Service using the Dockerfile or Maven build.
4. Set these environment variables:
   - `SPRING_PROFILES_ACTIVE=postgres`
   - `DB_URL`
   - `DB_USERNAME`
   - `DB_PASSWORD`
   - `OPENAI_API_KEY`
   - notification variables from `.env.example` if needed
5. Expose port `8080`.

## Final production checklist

1. Use PostgreSQL instead of H2.
2. Store secrets in the platform secret manager.
3. Enable real SMTP and Twilio credentials.
4. Run behind HTTPS.
5. Restrict admin passwords and rotate them after first deployment.
