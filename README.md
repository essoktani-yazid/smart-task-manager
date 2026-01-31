# SmartTaskManager

Task management application with client-server architecture in Java, using Maven, JDBC, MySQL and JavaFX.

## 📋 Table of Contents

1. [Overview](#overview)
2. [Project Architecture](#project-architecture)
3. [File Structure](#file-structure)
4. [Module Details](#module-details)
5. [Configuration](#configuration)
6. [Compilation and Execution](#compilation-and-execution)
7. [REST API](#rest-api)

---

## 🎯 Overview

SmartTaskManager is a task management application built with a Maven multi-module architecture. The project is divided into three main modules:

- **stm-common** : Shared module containing data models
- **stm-server** : Server module with custom HTTP API and JDBC persistence
- **stm-client** : Client module with JavaFX interface

---

## 🏗️ Project Architecture

The project follows a **client-server** architecture where:

- The **server** exposes a REST API via `HttpServer` (JDK) and uses JDBC for persistence
- The **client** communicates with the server via HTTP (Apache HttpClient) and displays a JavaFX interface
- **Models** are shared between both modules to avoid code duplication

### Data Flow

```
┌─────────────┐         HTTP/JSON          ┌─────────────┐
│   Client    │ ───────────────────────────> │   Server    │
│  (JavaFX)   │ <─────────────────────────── │  (HttpServer)│
└─────────────┘                              └─────────────┘
                                                      │
                                                      │ JDBC
                                                      ▼
                                              ┌─────────────┐
                                              │   MySQL DB  │
                                              └─────────────┘
```

---

## 📁 File Structure

```
SmartTaskManager/                    (Root Directory - "Parent")
│
├── pom.xml                          (Parent POM : manages common versions)
│
├── stm-common/                      (Shared Module : Transfer objects)
│   ├── pom.xml
│   └── src/main/java/com/smarttask/model/
│       ├── Task.java                (Plain POJO - no annotations)
│       ├── User.java                (Plain POJO - no annotations)
│       ├── Priority.java            (Enumeration : LOW, MEDIUM, HIGH, URGENT)
│       └── Status.java              (Enumeration : TODO, IN_PROGRESS, DONE, CANCELLED)
│
├── stm-server/                      (Backend Module : Custom HTTP API)
│   ├── pom.xml                      (Dependencies : MySQL, Gson, stm-common)
│   ├── src/main/resources/
│   │   ├── database.properties      (CONFIDENTIAL : Contains DB password)
│   │   └── schema.sql               (SQL script to create database and tables)
│   └── src/main/java/com/smarttask/server/
│       ├── MainServer.java          (Entry point : public static void main with HttpServer)
│       ├── config/
│       │   └── DatabaseConnection.java   (JDBC connection utility)
│       ├── dao/                     (Data Access Objects using JDBC)
│       │   ├── TaskDAO.java         (CRUD for tasks)
│       │   └── UserDAO.java         (CRUD for users)
│       └── handler/                 (HTTP Controllers that receive requests)
│           ├── AuthHandler.java     (Handles /api/auth/login and /api/auth/register)
│           └── TaskHandler.java     (Handles /api/tasks/* : Full CRUD)
│
└── stm-client/                      (Frontend Module : JavaFX Interface)
    ├── pom.xml                      (Dependencies : JavaFX, Gson, HttpClient, stm-common)
    ├── src/main/resources/
    │   ├── fxml/                    (FXML files for views)
    │   │   ├── login.fxml           (Login/registration view)
    │   │   └── dashboard.fxml       (Main view with task list)
    │   └── css/
    │       └── styles.css           (CSS styles for the interface)
    └── src/main/java/com/smarttask/client/
        ├── App.java                 (JavaFX entry point : extends Application)
        ├── view/
        │   └── controller/          (JavaFX UI Controllers)
        │       ├── LoginController.java    (Manages login view)
        │       └── DashboardController.java (Manages dashboard view)
        └── service/                 (Services that call the API via HTTP)
            ├── AuthService.java     (login() and register() methods)
            └── TaskService.java     (CRUD methods for tasks)
```

---

## 📦 Module Details

### A. The `stm-common` Module (Shared)

**Role** : Contains classes (models) used by both the client and the server.

**Why?** Avoids copying the `Task.java` class twice and ensures data consistency.

**Content** :

- `Task.java` : Plain Java object representing a task
- `User.java` : Plain Java object representing a user
- `Priority.java` : Enumeration for priority levels
- `Status.java` : Enumeration for task statuses

**Note** : These are plain POJOs without any framework annotations. They are simple data transfer objects.

---

### B. The `stm-server` Module (The Brain)

**Role** : Backend of the application. This is the only place where JDBC and MySQL Driver are present.

#### Structure

1. **`MainServer.java`** : Entry point
   - Launches the `HttpServer` on port **8080**
   - Configures routes `/api/auth` and `/api/tasks`
   - Tests database connection on startup

2. **`config/DatabaseConnection.java`** :
   - Utility class for managing JDBC connections
   - Loads database configuration from `database.properties`
   - Provides `getConnection()` and `closeConnection()` methods

3. **`dao/`** : Data Access Objects
   - `TaskDAO.java` : CRUD operations on tasks using JDBC
   - `UserDAO.java` : CRUD operations on users using JDBC
   - Use PreparedStatements for SQL queries and parameter binding

4. **`handler/`** : HTTP Controllers
   - `AuthHandler.java` : Handles authentication
     - `POST /api/auth/register` : Registration
     - `POST /api/auth/login` : Login
   - `TaskHandler.java` : Handles task operations
     - `GET /api/tasks` : List all tasks
     - `GET /api/tasks/{id}` : Task details
     - `GET /api/tasks/{id}/user` : User's tasks
     - `POST /api/tasks` : Create a task
     - `PUT /api/tasks/{id}` : Update a task
     - `DELETE /api/tasks/{id}` : Delete a task

**Security** : ⚠️ The `database.properties` file contains the database password. Never commit it to a public repository!

---

### C. The `stm-client` Module (The Face)

**Role** : Frontend of the application. This is the only place where JavaFX is present.

#### Structure

1. **`App.java`** : JavaFX entry point
   - Extends `Application`
   - Loads the `login.fxml` view on startup

2. **`view/controller/`** : UI Controllers
   - `LoginController.java` : Manages the login view
     - Methods : `handleLogin()`, `handleRegister()`
     - Navigation to dashboard after successful login
   - `DashboardController.java` : Manages the main view
     - Displays the task list in a `TableView`
     - Allows adding, editing, deleting tasks
     - Methods : `handleAddTask()`, `handleEditTask()`, `handleDeleteTask()`

3. **`service/`** : HTTP communication services
   - `AuthService.java` : Does **NOT** contain DAOs anymore
     - Uses `HttpClient` to make requests to `http://localhost:8080`
     - Methods : `login()`, `register()`
   - `TaskService.java` : Service for tasks
     - Methods : `getTasksByUser()`, `createTask()`, `updateTask()`, `deleteTask()`
     - Uses `HttpClient` to communicate with the API

4. **`resources/`** : UI Resources
   - `fxml/login.fxml` : Login interface
   - `fxml/dashboard.fxml` : Main interface
   - `css/styles.css` : CSS styles for the application

**Security** : ✅ This module contains **no** database passwords.

---

## ⚙️ Configuration

### 1. Database Configuration

Edit the file `stm-server/src/main/resources/database.properties` :

```properties
db.url=jdbc:mysql://localhost:3306/smarttaskdb?useSSL=false&serverTimezone=UTC
db.username=root
db.password=123456
db.driver=com.mysql.cj.jdbc.Driver
```

**Important** : Replace `123456` with your MySQL password.

### 2. Database Creation

Create the MySQL database and tables by running the SQL script :

```bash
mysql -u root -p < stm-server/src/main/resources/schema.sql
```

Or manually execute the SQL commands in `stm-server/src/main/resources/schema.sql` :

```sql
CREATE DATABASE IF NOT EXISTS smarttaskdb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE smarttaskdb;

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Tasks table
CREATE TABLE IF NOT EXISTS tasks (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority ENUM('LOW', 'MEDIUM', 'HIGH', 'URGENT') NOT NULL DEFAULT 'MEDIUM',
    status ENUM('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED') NOT NULL DEFAULT 'TODO',
    due_date DATETIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 3. Maven Configuration

The parent `pom.xml` manages common versions :

```xml
<properties>
    <mysql.version>8.0.33</mysql.version>
    <gson.version>2.10.1</gson.version>
    <javafx.version>17.0.2</javafx.version>
    <httpclient.version>4.5.14</httpclient.version>
</properties>
```

---

## 🚀 Compilation and Execution

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MySQL 8.0+
- MySQL running

### Compilation

At the project root :

```bash
mvn clean install
```

This command compiles all modules in order :

1. `stm-common`
2. `stm-server`
3. `stm-client`

### Server Execution

```bash
cd stm-server
mvn exec:java
```

Or directly :

```bash
mvn exec:java -pl stm-server
```

The server starts on `http://localhost:8080`.

### Client Execution

In another terminal :

```bash
cd stm-client
mvn javafx:run
```

Or directly from the root :

```bash
mvn javafx:run -pl stm-client
```

## 🚀 Seamless Development with Nodemon

To avoid manually restarting Maven compilation commands after every code change, you can use **Nodemon**. Although initially designed for Node.js, Nodemon can watch any type of file and execute custom commands.

### 1. Installation

Make sure you have Node.js installed, then install Nodemon globally:

```bash
sudo npm install -g nodemon
```

### 2. Usage

Open separate terminals for each service and use the following commands. These commands specify the watch directories and strict flags directly, eliminating the need for a config file.

**For the Server:**

```bash
nodemon --watch stm-server/src --watch stm-common/src -e "*" --exec "mvn compile exec:java -pl stm-server"
```

The server will restart automatically if you modify server code or shared models.

**For the Client (JavaFX):**

```bash
nodemon --watch stm-client/src --watch stm-common/src -e "*" --exec "mvn compile javafx:run -pl stm-client"
```

The GUI will relaunch automatically on every change to Java, FXML, or CSS files.

**For Python Scripts:**

```bash
nodemon --exec "python main.py"
```

### 💡 Why use Nodemon here?

- **Time saving**: Maven incremental compilation is triggered as soon as a file is saved.
- **Multi-module**: It watches changes in `stm-common` and propagates recompilation to dependent modules (server and client).
- **Flexibility**: Works for all file types specified in the command arguments.

## 🌐 REST API

### Authentication

#### Registration

```http
POST /api/auth/register
Content-Type: application/json

{
  "username": "john",
  "password": "secret",
  "email": "john@example.com"
}
```

**Response** (201 Created) :

```json
{
  "id": 1,
  "username": "john",
  "email": "john@example.com"
}
```

#### Login

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "john",
  "password": "secret"
}
```

**Response** (200 OK) :

```json
{
  "id": 1,
  "username": "john",
  "email": "john@example.com"
}
```

### Tasks

#### List all tasks

```http
GET /api/tasks
```

#### List user's tasks

```http
GET /api/tasks/{userId}/user
```

#### Get a task

```http
GET /api/tasks/{taskId}
```

#### Create a task

```http
POST /api/tasks
Content-Type: application/json

{
  "title": "Do the shopping",
  "description": "Buy milk and bread",
  "priority": "HIGH",
  "status": "TODO",
  "user": {
    "id": 1
  }
}
```

#### Update a task

```http
PUT /api/tasks/{taskId}
Content-Type: application/json

{
  "title": "Do the shopping",
  "description": "Buy milk, bread and eggs",
  "priority": "URGENT",
  "status": "IN_PROGRESS"
}
```

#### Delete a task

```http
DELETE /api/tasks/{taskId}
```

---

## 📝 Important Notes

1. **Security** : The database password is stored in plain text in `database.properties`. For production, use environment variables or an external configuration file.

2. **CORS** : HTTP handlers include `Access-Control-Allow-Origin: *` to allow cross-origin requests. For production, restrict this.

3. **Error Handling** : Errors are currently displayed in the console. For production, implement a logging system (Log4j, SLF4J).

4. **Validation** : Data validations are minimal. Add more robust validations for production.

5. **Password** : Passwords are stored in plain text. For production, use hashing (BCrypt, Argon2).

6. **Connection Management** : The current implementation creates a new connection for each database operation. For production, consider using a connection pool (HikariCP, C3P0).

---

## 🔧 Development

### Dependency Structure

```
SmartTaskManager (parent)
├── stm-common (no dependencies - plain POJOs)
├── stm-server (depends on stm-common, MySQL, Gson)
└── stm-client (depends on stm-common, JavaFX, Gson, HttpClient)
```

### Adding New Features

1. **New model** : Add it in `stm-common/src/main/java/com/smarttask/model/`
2. **New DAO** : Add it in `stm-server/src/main/java/com/smarttask/server/dao/` using JDBC
3. **New handler** : Add it in `stm-server/src/main/java/com/smarttask/server/handler/` and register it in `MainServer.java`
4. **New client service** : Add it in `stm-client/src/main/java/com/smarttask/client/service/`
5. **New view** : Create the FXML file in `stm-client/src/main/resources/fxml/` and the corresponding controller

### JDBC Best Practices

- Always use `PreparedStatement` to prevent SQL injection
- Always close connections, statements, and result sets in finally blocks or use try-with-resources
- Use transactions for operations that modify multiple tables
- Handle SQL exceptions appropriately
