package com.smarttask.server.dao;

import com.smarttask.model.User;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the User entity.
 * Manages all CRUD operations on users using JDBC.
 */
public class UserDAO {

    /**
     * Saves a new user to the database.
     */
    public String save(User user) {
        String sql = "INSERT INTO users (id, username, password_hash, email, first_name, last_name) VALUES (?, ?, ?, ?, ?, ?)";
        String id = java.util.UUID.randomUUID().toString();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, user.getUsername());
            pstmt.setString(3, user.getPassword()); 
            pstmt.setString(4, user.getEmail());
            pstmt.setString(5, user.getFirstName());
            pstmt.setString(6, user.getLastName());

            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating user failed, no rows affected.");
            }

            user.setId(id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving user", e);
        }
    }

    /**
     * Finds a user by ID.
     */
    public Optional<User> findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Finds a user by username.
     */
    public Optional<User> findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Finds a user by email.
     */
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    /**
     * Retrieves all users.
     */
    // Dans UserDAO.java
    public java.util.List<com.smarttask.model.User> findAll() {
        java.util.List<com.smarttask.model.User> users = new java.util.ArrayList<>();
        String sql = "SELECT id, username, email FROM users"; // On ne récupère pas le mot de passe !

        try (java.sql.Connection conn = com.smarttask.server.config.DatabaseConnection.getConnection();
             java.sql.PreparedStatement stmt = conn.prepareStatement(sql);
             java.sql.ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                com.smarttask.model.User u = new com.smarttask.model.User();
                u.setId(rs.getString("id"));
                u.setUsername(rs.getString("username"));
                u.setEmail(rs.getString("email"));
                users.add(u);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return users;
    }

    /**
     * Updates an existing user.
     */
    public void update(User user) {
        // If password is null, don't update it
        String sql;
        if (user.getPassword() == null || user.getPassword().isEmpty()) {
            sql = "UPDATE users SET username = ?, email = ?, first_name = ?, last_name = ? WHERE id = ?";
        } else {
            sql = "UPDATE users SET username = ?, password_hash = ?, email = ?, first_name = ?, last_name = ? WHERE id = ?";
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (user.getPassword() == null || user.getPassword().isEmpty()) {
                // Update without password
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getEmail());
                pstmt.setString(3, user.getFirstName());
                pstmt.setString(4, user.getLastName());
                pstmt.setString(5, user.getId());
            } else {
                // Update with password
                pstmt.setString(1, user.getUsername());
                pstmt.setString(2, user.getPassword()); // hash
                pstmt.setString(3, user.getEmail());
                pstmt.setString(4, user.getFirstName());
                pstmt.setString(5, user.getLastName());
                pstmt.setString(6, user.getId());
            }

            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error updating user", e);
        }
    }

    /**
     * Deletes a user by ID.
     */
    public void deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting user", e);
        }
    }

    /**
     * Checks if a user exists with the given username.
     */
    public boolean existsByUsername(String username) {
        String sql = "SELECT COUNT(*) FROM users WHERE username = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    /**
     * Maps a ResultSet row to a User object.
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password_hash"));
        user.setEmail(rs.getString("email"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        return user;
    }
}
