<div align="center" width="100%">

  <h1>🧠 SmartTask Manager</h1>
  
  <p>
    An intelligent productivity platform merging <b>Modern UI</b> with <b>Generative AI</b>.
    <br>
    <i>"Stop managing tasks, start executing them."</i>
  </p>

  <a href="https://www.java.com">
    <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java" />
  </a>
  <a href="https://openjfx.io/">
    <img src="https://img.shields.io/badge/Frontend-JavaFX-007396?style=for-the-badge&logo=java&logoColor=white" alt="JavaFX" />
  </a>
  <a href="https://fastapi.tiangolo.com/">
    <img src="https://img.shields.io/badge/Backend-FastAPI-009688?style=for-the-badge&logo=fastapi&logoColor=white" alt="FastAPI" />
  </a>
  <a href="#">
    <img src="https://img.shields.io/badge/AI-Mistral_7B-purple?style=for-the-badge&logo=openai&logoColor=white" alt="AI" />
  </a>

<br /> <br />

  <table align="center" style="margin-left: auto; margin-right: auto; border-collapse: collapse; width: 100%;">
    <tr>
        <td align="center" width="25%"><b>🎓 School</b></td>
        <td align="center" width="25%"><b>📚 Module</b></td>
        <td align="center" width="25%"><b>👩‍🏫 Supervisor</b></td>
    </tr>
    <tr>
        <td align="center">ENSET Mohammedia (Master SDIA)</td>
        <td align="center">Object-Oriented Programming</td>
        <td align="center">Prof. Loubna Aminou</td>
    </tr>
  </table>

  <h3>👨‍💻 The Development Team</h3>
  
  **Yazid ESSOKTANI** • **Loubna MAHRACH** • **Rayane TOKO** • **Mohamed Amin BOUSSAID**

</div>

<hr>

## 📝 Project Overview

**SmartTask Manager** is not just another To-Do list application. It is a hybrid system designed to demonstrate how **Java Object-Oriented architecture** can seamlessly interact with a **Python Microservice for AI**.

The user can manage tasks via a beautiful Glassmorphism UI or simply **chat with the AI** to perform complex database operations using natural language.

---

## 📋 Table of Contents

1. [Overview](#-overview)
2. [Architecture](#-architecture)
3. [Project Structure](#-project-structure)
4. [Modules](#-modules)
5. [Configuration](#-configuration)
6. [Build and Run](#-build-and-run)
7. [REST API](#-rest-api)

---

## 🎯 Overview

SmartTaskManager is a task management application divided into **four main components**:

| Module             | Role                                                                 |
| ------------------ | -------------------------------------------------------------------- |
| **stm-common**     | Shared Data Models (POJOs)                                           |
| **stm-server**     | HTTP Backend with REST API and JDBC persistence                      |
| **stm-client**     | JavaFX Client (Login, Dashboard, Calendar, AI, Profile)              |
| **stm-ai-service** | AI Microservice: Natural language → SQL via LLM (Mistral/OpenRouter) |

---

## 🏗️ Architecture

### Global Data Flow

The application offers **two modes** of interaction with the database:

#### Case 1: Direct connection via the server (Classic CRUD)

For standard operations (create, edit, delete tasks via the interface), the client communicates with the server which accesses MySQL directly.

```
┌─────────────────┐     HTTP/JSON      ┌─────────────────┐       JDBC       ┌───────────────┐
│   Client        │ ──────────────────> │   Server        │ ────────────────> │   MySQL DB    │
│   (JavaFX)      │ <────────────────── │   (HttpServer)  │ <──────────────── │ (smarttask_db)│
└─────────────────┘   Dashboard, CRUD   └─────────────────┘                   └───────────────┘
```

#### Case 2: Natural Language via AI Service

To work more easily with the interface, you can **write in natural language** in a prompt: the application understands your request and executes the operations on the database.

_Example: "Delete overdue tasks" or "Show my priority tasks for the week"_

```
┌─────────────────┐     WebSocket      ┌───────────────┐     LLM + SQL      ┌───────────────┐
│   Client        │ ──────────────────> │ stm-ai-service│ ─────────────────> │   MySQL DB    │
│   (JavaFX)      │   "Prompts" text    │  (FastAPI)    │   Analyze + exec   │ (smarttask_db)│
└─────────────────┘ <────────────────── └───────────────┘ <───────────────── └───────────────┘
                       Response / Confirm
```

### AI Microservice Architecture (stm-ai-service)

The AI service acts as an intelligent bridge between the JavaFX client and the database: it translates natural language into secure SQL operations via an LLM (Mistral via OpenRouter).

**Read-Execute / Write-Confirm Security:**

- **READ**: Immediate execution (SELECT)
- **WRITE** (INSERT/UPDATE/DELETE): No modification without **double validation** — the AI generates the SQL, the system simulates the impact, and the user confirms in the interface.

**Real-time Feedback (WebSocket):**

> _Analyzing intent..._ → _Reading database..._ → _Summarizing..._

---

## 📁 Project Structure

```
SmartTaskManager/
├── pom.xml                          # Maven Parent POM
│
├── stm-common/                      # Shared Models
│   └── src/main/java/com/smarttask/model/
│       ├── Task.java, MainTask.java, SubTask.java
│       ├── User.java, Priority.java, Status.java
│       ├── CalendarEvent.java, TaskDependency.java
│       ├── Comment.java, SharedTask.java, TimeTracking.java
│       └── ...
│
├── stm-server/                      # REST Backend
│   ├── src/main/java/com/smarttask/server/
│   │   ├── MainServer.java          # Entry Point (HttpServer, port 8080)
│   │   ├── config/                  # DatabaseConnection, SchemaUpdater
│   │   ├── dao/                     # TaskDAO, UserDAO, CalendarEventDAO
│   │   └── handler/                 # AuthHandler, TaskHandler, UserHandler
│   └── src/main/resources/
│       ├── database.properties      # DB Config (⚠️ do not commit)
│       └── schema.sql               # Complete Schema + Test Data
│
├── stm-client/                      # JavaFX Interface
│   ├── src/main/java/com/smarttask/client/
│   │   ├── App.java
│   │   ├── service/                 # AuthService, TaskService, UserService, PlannerManager
│   │   └── view/controller/         # Login, Dashboard, Calendar, AI, Profile
│   └── src/main/resources/
│       ├── fxml/                    # login, dashboard, calendar, ai-view, profile, etc.
│       └── css/
│
└── stm-ai-service/                  # AI Microservice (Python)
    ├── app/
    │   ├── main.py                  # FastAPI + WebSocket /ai/stream
    │   ├── database.py              # MySQL Connection
    │   ├── core/config.py           # Environment Variables
    │   ├── rag/prompts.py           # System Prompts (SQL Security)
    │   ├── services/
    │   │   ├── ai_service.py        # OpenRouter Communication (LLM)
    │   │   └── query_service.py     # Logic: Intent → SQL → Confirmation
    │   └── utils/logger.py
    ├── .env                         # OPENROUTER_API_KEY, DB_*
    └── requirements.txt
```

---

## 📦 Modules

### stm-common

POJO models shared between client and server (Task, User, CalendarEvent, etc.) to ensure data consistency.

### stm-server

- **MainServer**: Launches the `HttpServer` on port **8080**
- **config/DatabaseConnection**: JDBC connection management via `database.properties`
- **dao/**: CRUD for tasks, users, calendar events
- **handler/**: Routes `/api/auth`, `/api/tasks`, `/api/users`

### stm-client

- **App.java**: JavaFX entry point
- **Controllers**: Login, Dashboard, Calendar, AI, Profile, MainLayout
- **Services**: HTTP communication with the server and WebSocket with the AI service

### stm-ai-service (Python)

- **main.py**: FastAPI, WebSocket `/ai/stream`, routing (prompts vs confirmations)
- **query_service.py**: Intent analysis, READ/WRITE decision, real-time status
- **prompts.py**: System prompts to ensure SQL compliance with the schema and filtering by `user_id`

---

## ⚙️ Configuration

### Prerequisites

- **Java 21** and Maven 3.6+
- **MySQL 8.0+**
- **Python 3.10+** (for stm-ai-service)
- **OpenRouter API Key** (for the AI service)

### 1. Database

Create the database and tables:

```bash
mysql -u root -p < stm-server/src/main/resources/schema.sql
```

The schema creates the `smarttask_db` database with tables: users, tasks, task_tags, task_dependencies, comments, calendar_event, time_tracking, shared_tasks, notifications, productivity_insights, etc.

### 2. Server Configuration (database.properties)

File `stm-server/src/main/resources/database.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/smarttask_db?useSSL=false&serverTimezone=UTC
db.username=root
db.password=YOUR_PASSWORD
db.driver=com.mysql.cj.jdbc.Driver
```

### 3. AI Service Configuration (.env)

File `stm-ai-service/.env`:

```env
OPENROUTER_API_KEY=sk-or-your-key...
DB_HOST=localhost
DB_USER=root
DB_PASSWORD=your_password
DB_NAME=smarttask_db
```

---

## 🚀 Build and Run

### Compilation (Java)

```bash
mvn clean install
```

### Start Server

```bash
mvn exec:java -pl stm-server
```

The server listens on `http://localhost:8080`.

### Start Client

```bash
mvn javafx:run -pl stm-client
```

### Start AI Service

```bash
cd stm-ai-service
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

WebSocket available at `ws://localhost:8000/ai/stream`.

### Development with Nodemon (Optional)

To automatically reload on changes:

```bash
# Server
nodemon --watch stm-server/src --watch stm-common/src -e "*" --exec "mvn compile exec:java -pl stm-server"

# Client
nodemon --watch stm-client/src --watch stm-common/src -e "*" --exec "mvn compile javafx:run -pl stm-client"
```

---

## 🌐 REST API

### Authentication

| Method | Endpoint             | Description |
| ------ | -------------------- | ----------- |
| POST   | `/api/auth/register` | Register    |
| POST   | `/api/auth/login`    | Login       |

### Tasks

| Method | Endpoint               | Description   |
| ------ | ---------------------- | ------------- |
| GET    | `/api/tasks`           | List tasks    |
| GET    | `/api/tasks/{id}`      | Task details  |
| GET    | `/api/tasks/{id}/user` | User's tasks  |
| POST   | `/api/tasks`           | Create a task |
| PUT    | `/api/tasks/{id}`      | Update a task |
| DELETE | `/api/tasks/{id}`      | Delete a task |

---

## 📝 Important Notes

1. **Security**: `database.properties` and `.env` contain secrets. Do not commit them. Use environment variables in production.
2. **Passwords**: Currently in plain text. In production: hashing (BCrypt, Argon2).
3. **CORS**: `Access-Control-Allow-Origin: *` in handlers. Restrict in production.
4. **Connection Pool**: For production, consider HikariCP or equivalent.
