package com.smarttask.server;

import com.smarttask.server.config.DatabaseConnection;
import com.smarttask.server.socket.NotificationWebSocketServer;
import com.smarttask.server.handler.AuthHandler;
import com.smarttask.server.handler.TaskHandler;
import com.smarttask.server.handler.TeamHandler;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * HTTP server entry point.
 * Launches the server on port 8080 and configures routes.
 */
public class MainServer {
    private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
    private static final int WS_PORT = Integer.parseInt(System.getenv().getOrDefault("WS_PORT", "8887"));

    public static void main(String[] args) {
        try {
            // Test database connection
            try (Connection conn = DatabaseConnection.getConnection()) {
                System.out.println("Database connection successful!");
                com.smarttask.server.config.SchemaUpdater.checkAndUpdateSchema();
            } catch (SQLException e) {
                System.err.println("Database connection failed: " + e.getMessage());
                System.err.println("Please check your database configuration in database.properties");
                return;
            }

            // Create HTTP server
            // HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);
            HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

            // Configure routes
            server.createContext("/api/auth", new AuthHandler());
            server.createContext("/api/tasks", new TaskHandler());
            server.createContext("/api/users", new com.smarttask.server.handler.UserHandler());
            server.createContext("/api/projects", new com.smarttask.server.handler.ProjectsHandler());
            server.createContext("/api/comments", new com.smarttask.server.handler.CommentHandler());
            server.createContext("/api/comment-attachments", new com.smarttask.server.handler.CommentAttachmentHandler());
            server.createContext("/api/tags", new com.smarttask.server.handler.TaskTagHandler());
            server.createContext("/api/dependencies", new com.smarttask.server.handler.DependencyHandler());
            server.createContext("/api/shared-tasks", new com.smarttask.server.handler.SharedTaskHandler());
            server.createContext("/api/notifications", new com.smarttask.server.handler.NotificationHandler());
            server.createContext("/api/attachments", new com.smarttask.server.handler.AttachmentHandler());
            server.createContext("/api/timetracking", new com.smarttask.server.handler.TimeTrackingHandler());
            server.createContext("/api/teams", new TeamHandler());

            // Start server
            server.setExecutor(null); // Uses default thread pool
            server.start();

            NotificationWebSocketServer wsServer = new NotificationWebSocketServer(WS_PORT);
            wsServer.start();

            System.out.println("=== SmartTaskManager Server ===");
            System.out.println("Server started on port " + PORT);
            System.out.println("API available at: http://localhost:" + PORT);
            System.out.println("Endpoints:");
            System.out.println("  POST /api/auth/register - Registration");
            System.out.println("  POST /api/auth/login - Login");
            System.out.println("  GET  /api/tasks - List all tasks");
            System.out.println("  GET  /api/tasks/{id} - Task details");
            System.out.println("  GET  /api/tasks/{id}/user - User's tasks");
            System.out.println("  POST /api/tasks - Create a task");
            System.out.println("  PUT  /api/tasks/{id} - Update a task");
            System.out.println("  DELETE /api/tasks/{id} - Delete a task");
            System.out.println("  GET  /api/projects - List all projects");
            System.out.println("  GET  /api/projects/{id} - Project details");
            System.out.println("  GET  /api/projects/{id}/user - User's projects");
            System.out.println("  POST /api/projects - Create a project");
            System.out.println("  PUT  /api/projects/{id} - Update a project");
            System.out.println("  DELETE /api/projects/{id} - Delete a project");
            System.out.println("  GET  /api/comments/task/{id} - Task comments");
            System.out.println("  POST /api/comments - Add comment");
            System.out.println("  PUT  /api/comments/{id} - Update comment");
            System.out.println("  DELETE /api/comments/{id} - Delete comment");
            System.out.println("  GET  /api/dependencies/successor/{id} - Predecessor tasks");
            System.out.println("  GET  /api/dependencies/predecessor/{id} - Successor tasks");
            System.out.println("  POST /api/dependencies - Add dependency");
            System.out.println("  DELETE /api/dependencies/{id} - Remove dependency");
            System.out.println("  POST /api/shared-tasks - Share task");
            System.out.println("  GET  /api/shared-tasks/task/{id} - Task shares");
            System.out.println("  GET  /api/shared-tasks/user/{id} - User shared tasks");
            System.out.println("  PUT  /api/shared-tasks/{taskId}/{userId} - Update permission");
            System.out.println("  DELETE /api/shared-tasks/{taskId}/{userId} - Revoke sharing");
            System.out.println("  GET  /api/notifications/user/{id} - User notifications");
            System.out.println("  GET  /api/notifications/user/{id}/unread - Unread notifications");
            System.out.println("  POST /api/notifications - Create notification");
            System.out.println("  PUT  /api/notifications/{id}/read - Mark as read");
            System.out.println("  PUT  /api/notifications/read-all - Mark all as read");
            System.out.println("  DELETE /api/notifications/{id} - Delete notification");

            // Add shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\nShutting down server...");
                server.stop(0);
                System.out.println("Server stopped.");
            }));

        } catch (IOException e) {
            System.err.println("Error starting server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


// package com.smarttask.server;

// import com.smarttask.server.config.DatabaseConnection;
// import com.smarttask.server.config.SchemaUpdater; // Assurez-vous d'importer ça
// import com.smarttask.server.handler.*; // Importe tous les handlers d'un coup
// import com.sun.net.httpserver.HttpServer;

// import java.io.IOException;
// import java.net.InetSocketAddress;
// import java.sql.Connection;
// import java.sql.SQLException;
// import java.util.concurrent.Executors; // Import pour le multi-threading

// /**
//  * HTTP server entry point.
//  * Launches the server on port 8080 (or Env PORT) and configures routes.
//  */
// public class MainServer {

//     // Récupération dynamique du port (Vital pour Railway)
//     private static final int PORT = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

//     public static void main(String[] args) {
//         try {
//             // 1. Test database connection & Update Schema
//             System.out.println(">>> Initializing Database...");
//             try (Connection conn = DatabaseConnection.getConnection()) {
//                 System.out.println(">>> Database connection successful!");
//                 // Création automatique des tables si elles n'existent pas
//                 SchemaUpdater.checkAndUpdateSchema();
//             } catch (SQLException e) {
//                 System.err.println(">>> CRITICAL DB ERROR: " + e.getMessage());
//                 System.err.println(">>> Server will start, but DB features will fail.");
//             }

//             // 2. Create HTTP server
//             // "0.0.0.0" est OBLIGATOIRE pour Docker/Railway
//             HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

//             // 3. Configure routes
//             server.createContext("/api/auth", new AuthHandler());
//             server.createContext("/api/tasks", new TaskHandler());
//             server.createContext("/api/users", new UserHandler());
//             server.createContext("/api/projects", new ProjectsHandler());
//             server.createContext("/api/comments", new CommentHandler());
//             server.createContext("/api/comment-attachments", new CommentAttachmentHandler());
//             server.createContext("/api/tags", new TaskTagHandler());
//             server.createContext("/api/dependencies", new DependencyHandler());
//             server.createContext("/api/shared-tasks", new SharedTaskHandler());
//             server.createContext("/api/notifications", new NotificationHandler());
//             server.createContext("/api/attachments", new AttachmentHandler());
//             server.createContext("/api/timetracking", new TimeTrackingHandler());
//             server.createContext("/api/teams", new TeamHandler());

//             // 4. Start server with Multi-threading
//             // Permet de gérer plusieurs requêtes simultanées (Important pour la vitesse)
//             server.setExecutor(Executors.newCachedThreadPool());
            
//             server.start();

//             // Logs de confirmation
//             System.out.println("=== SmartTaskManager Server ===");
//             System.out.println("🚀 Server started on port " + PORT);
//             System.out.println("📡 Listening on 0.0.0.0 (Accessible externally)");
//             System.out.println("✅ Ready to accept requests!");

//             // 5. Add shutdown hook (Arrêt propre)
//             Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//                 System.out.println("\nShutting down server...");
//                 server.stop(0);
//                 System.out.println("Server stopped.");
//             }));

//         } catch (IOException e) {
//             System.err.println("Error starting server: " + e.getMessage());
//             e.printStackTrace();
//         }
//     }
// }
