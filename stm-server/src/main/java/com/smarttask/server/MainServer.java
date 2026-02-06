package com.smarttask.server;

import com.smarttask.server.config.DatabaseConnection;
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
    private static final int PORT = 8090;

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
            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

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

