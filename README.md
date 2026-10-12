# Todo List A1

A responsive, single-user to-do list web application powered by **Spring Boot 4.1.1** (Spring Framework 7, Hibernate 7, Jackson 3) on Java 17/21.

## Tech Stack & Architecture

- **Backend:** Spring Boot REST API (`/api/todos`) with Spring Data JPA, Bean Validation, and Lombok.
- **Frontend:** Responsive, mobile-first static web UI (`index.html`, `styles.css`, `app.js`) served directly from Spring Boot (`src/main/resources/static`).
- **Database:**
  - **H2 in-memory (default):** Zero-setup development database (resets on restart).
  - **PostgreSQL 18 (profile `postgres`):** Local persistent database using database `todo_db`.
- **API Standard:** JSON with RFC 9457 `ProblemDetail` error formats.

## Prerequisites

- **Java JDK 17+** (JDK 21 tested and recommended)
- Maven is not required globally; use the included wrapper (`mvnw.cmd` on Windows, `./mvnw` on Linux/macOS).
- *(Optional)* PostgreSQL 18 if running with the `postgres` profile.

## Quick Start (PowerShell / Windows)

```powershell
# Build and run all tests (H2 in-memory)
.\mvnw.cmd clean test

# Run the app locally on http://localhost:8080 (H2)
.\mvnw.cmd spring-boot:run

# Run against PostgreSQL
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=postgres"
```

Once running, access the web app at [http://localhost:8080](http://localhost:8080).

## Phone Access Over Local Network (LAN)

Spring Boot binds to all interfaces (`0.0.0.0`) by default:
1. Ensure your Windows network connection profile is set to **Private**.
2. Allow incoming traffic on port 8080 (PowerShell as Administrator):
   ```powershell
   New-NetFirewallRule -DisplayName "Todo dev 8080" -Direction Inbound -Protocol TCP -LocalPort 8080 -Action Allow -Profile Private
   ```
3. Locate your machine's local IP address:
   ```powershell
   Get-NetIPAddress -AddressFamily IPv4 | Where-Object { $_.IPAddress -notlike '169.*' -and $_.IPAddress -ne '127.0.0.1' }
   ```
4. On your mobile device connected to the same Wi-Fi network, navigate to `http://<your-ip>:8080`.
5. Remove firewall rule when finished:
   ```powershell
   Remove-NetFirewallRule -DisplayName "Todo dev 8080"
   ```

## Project Documentation

- [`AGENTS.md`](AGENTS.md) — Coding conventions, Spring Boot 4 notes, and agent workflow guidelines.
- [`PLANS.md`](PLANS.md) — Project milestones, detailed specifications, and progress tracking.
