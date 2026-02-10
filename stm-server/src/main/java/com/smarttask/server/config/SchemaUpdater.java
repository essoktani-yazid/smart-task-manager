package com.smarttask.server.config;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Handles database schema updates.
 */
public class SchemaUpdater {

    public static void checkAndUpdateSchema() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            
            // Check if users table exists and update if needed
            checkAndCreateUsersTable(conn, meta);

            // Check if tasks table has parent_task_id
            checkAndAddParentTaskId(conn, meta);

            // Create attachments table
            createAttachmentsTable(conn, meta);

            // Create time_tracking table
            createTimeTrackingTable(conn, meta);

        } catch (SQLException e) {
            System.err.println("Error updating schema: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void checkAndCreateUsersTable(Connection conn, DatabaseMetaData meta) throws SQLException {
        ResultSet tables = meta.getTables(null, null, "users", null);
        if (tables.next()) {
            ResultSet columns = meta.getColumns(null, null, "users", "first_name");
            if (!columns.next()) {
                System.out.println("Updating database schema: Adding first_name and last_name columns...");
                try (Statement stmt = conn.createStatement()) {
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN first_name VARCHAR(255)");
                    stmt.executeUpdate("ALTER TABLE users ADD COLUMN last_name VARCHAR(255)");
                    System.out.println("Schema updated successfully.");
                }
            }
        } else {
            System.out.println("Creating users table...");
            try (Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE users (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "username VARCHAR(255) NOT NULL UNIQUE," +
                    "password_hash VARCHAR(255) NOT NULL," +
                    "email VARCHAR(255)," +
                    "first_name VARCHAR(255)," +
                    "last_name VARCHAR(255)" +
                    ")";
                stmt.executeUpdate(sql);
                System.out.println("Table users created.");
            }
        }
    }

    private static void checkAndAddParentTaskId(Connection conn, DatabaseMetaData meta) throws SQLException {
        ResultSet columns = meta.getColumns(null, null, "tasks", "parent_task_id");
        if (!columns.next()) {
            System.out.println("Updating database schema: Adding parent_task_id to tasks...");
            try (Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("ALTER TABLE tasks ADD COLUMN parent_task_id VARCHAR(36)");
                stmt.executeUpdate("ALTER TABLE tasks ADD FOREIGN KEY (parent_task_id) REFERENCES tasks(id)");
                System.out.println("Added parent_task_id to tasks.");
            }
        }
    }

    private static void createAttachmentsTable(Connection conn, DatabaseMetaData meta) throws SQLException {
        ResultSet tables = meta.getTables(null, null, "attachments", null);
        if (!tables.next()) {
            System.out.println("Creating attachments table...");
            try (Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE attachments (" +
                        "id VARCHAR(36) PRIMARY KEY," +
                        "task_id VARCHAR(36) NOT NULL," +
                        "file_name VARCHAR(255) NOT NULL," +
                        "file_type VARCHAR(100)," +
                        "file_path VARCHAR(500) NOT NULL," +
                        "file_size BIGINT," +
                        "uploaded_at DATETIME," +
                        "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE" +
                        ")";
                stmt.executeUpdate(sql);
                System.out.println("Table attachments created.");
            }
        }
    }

    private static void createTimeTrackingTable(Connection conn, DatabaseMetaData meta) throws SQLException {
        ResultSet tables = meta.getTables(null, null, "time_tracking", null);
        if (!tables.next()) {
            System.out.println("Creating time_tracking table...");
            try (Statement stmt = conn.createStatement()) {
                String sql = "CREATE TABLE time_tracking (" +
                        "id VARCHAR(36) PRIMARY KEY," +
                        "task_id VARCHAR(36) NOT NULL," +
                        "user_id VARCHAR(36) NOT NULL," +
                        "start_time DATETIME NOT NULL," +
                        "end_time DATETIME," +
                        "duration_ms BIGINT," +
                        "notes TEXT," +
                        "FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE," +
                        "FOREIGN KEY (user_id) REFERENCES users(id)" +
                        ")";
                stmt.executeUpdate(sql);
                System.out.println("Table time_tracking created.");
            }
        }
    }
}
