package com.smarttask.server;

import com.smarttask.server.config.DatabaseConnection;
import com.smarttask.server.handler.AuthHandler;
import com.smarttask.server.handler.TaskHandler;
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
            System.out.println("================================");

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

