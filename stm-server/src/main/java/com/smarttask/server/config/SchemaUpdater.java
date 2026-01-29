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
            
            // Check if users table exists
            ResultSet tables = meta.getTables(null, null, "users", null);
            if (tables.next()) {
                // Check if first_name column exists
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
                // Create table if it doesn't exist (optional, but good practice)
                System.out.println("Creating users table...");
                try (Statement stmt = conn.createStatement()) {
                    String sql = "CREATE TABLE users (" +
                            "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                            "username VARCHAR(255) NOT NULL UNIQUE," +
                            "password VARCHAR(255) NOT NULL," +
                            "email VARCHAR(255)," +
                            "first_name VARCHAR(255)," +
                            "last_name VARCHAR(255)" +
                            ")";
                    stmt.executeUpdate(sql);
                    System.out.println("Table users created.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error updating schema: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
