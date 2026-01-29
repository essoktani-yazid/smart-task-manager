package com.smarttask.server.dao;

import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for the Task entity.
 * Manages all CRUD operations on tasks using JDBC.
 */
public class TaskDAO {

    /**
     * Saves a new task to the database.
     */
    public String save(Task task) {
        String sql = "INSERT INTO tasks (id, title, description, priority, status, due_date, created_at, user_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String id = java.util.UUID.randomUUID().toString();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            pstmt.setString(2, task.getTitle());
            pstmt.setString(3, task.getDescription());
            pstmt.setString(4, task.getPriority().name());
            pstmt.setString(5, task.getStatus().name());
            pstmt.setTimestamp(6, task.getDueDate() != null ? Timestamp.valueOf(task.getDueDate()) : null);
            pstmt.setTimestamp(7, task.getCreatedAt() != null ? Timestamp.valueOf(task.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setString(8, task.getUser().getId());
            
            int affectedRows = pstmt.executeUpdate();
            
            if (affectedRows == 0) {
                throw new SQLException("Creating task failed, no rows affected.");
            }
            
            task.setId(id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving task", e);
        }
    }

    /**
     * Finds a task by ID.
     */
    public Optional<Task> findById(String id) {
        String sql = "SELECT t.*, u.id as user_id, u.username, u.email, u.password as user_password " +
                     "FROM tasks t " +
                     "INNER JOIN users u ON t.user_id = u.id " +
                     "WHERE t.id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return Optional.empty();
    }

    /**
     * Retrieves all tasks for a user.
     */
    public List<Task> findByUser(User user) {
        return findByUserId(user.getId());
    }

    /**
     * Retrieves all tasks for a user by user ID.
     */
    public List<Task> findByUserId(String userId) {
        String sql = "SELECT t.*, u.id as user_id, u.username, u.email, u.password as user_password " +
                     "FROM tasks t " +
                     "INNER JOIN users u ON t.user_id = u.id " +
                     "WHERE t.user_id = ? " +
                     "ORDER BY t.created_at DESC";
        
        List<Task> tasks = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return tasks;
    }

    /**
     * Retrieves all tasks.
     */
    public List<Task> findAll() {
        String sql = "SELECT t.*, u.id as user_id, u.username, u.email, u.password as user_password " +
                     "FROM tasks t " +
                     "INNER JOIN users u ON t.user_id = u.id " +
                     "ORDER BY t.created_at DESC";
        
        List<Task> tasks = new ArrayList<>();
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                tasks.add(mapResultSetToTask(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return tasks;
    }

    /**
     * Updates an existing task.
     */
    public void update(Task task) {
        String sql = "UPDATE tasks SET title = ?, description = ?, priority = ?, status = ?, due_date = ? WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, task.getTitle());
            pstmt.setString(2, task.getDescription());
            pstmt.setString(3, task.getPriority().name());
            pstmt.setString(4, task.getStatus().name());
            pstmt.setTimestamp(5, task.getDueDate() != null ? Timestamp.valueOf(task.getDueDate()) : null);
            pstmt.setString(6, task.getId());
            
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error updating task", e);
        }
    }

    /**
     * Deletes a task from the database.
     */
    public void delete(Task task) {
        deleteById(task.getId());
    }

    /**
     * Deletes a task by ID.
     */
    public boolean deleteById(String id) {
        String sql = "DELETE FROM tasks WHERE id = ?";
        
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, id);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error deleting task", e);
        }
    }

    /**
     * Maps a ResultSet row to a Task object.
     */
    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task task = new Task();
        task.setId(rs.getString("id"));
        task.setTitle(rs.getString("title"));
        task.setDescription(rs.getString("description"));
        task.setPriority(Priority.valueOf(rs.getString("priority")));
        task.setStatus(Status.valueOf(rs.getString("status")));
        
        Timestamp dueDate = rs.getTimestamp("due_date");
        if (dueDate != null) {
            task.setDueDate(dueDate.toLocalDateTime());
        }
        
        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            task.setCreatedAt(createdAt.toLocalDateTime());
        }
        
        // Map user
        User user = new User();
        user.setId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("user_password"));
        task.setUser(user);
        
        return task;
    }
}
