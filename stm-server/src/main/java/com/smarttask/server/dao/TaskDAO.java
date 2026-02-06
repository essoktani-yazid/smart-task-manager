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
import java.util.UUID;
/**
 * ✅ FULLY FIXED: Data Access Object for the Task entity
 * - Removed all u.password references
 * - Fixed findByUserId to include username and email
 * - Added enum validation with fallback
 */
public class TaskDAO {

    /**
     * Saves a new task to the database.
     */
    public void save(Task task) throws SQLException {
        String sql = "INSERT INTO tasks (id, title, description, priority, status, due_date, user_id, project_id, task_type, parent_task_id, created_at, recurrence_type, recurrence_interval) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        if (task.getId() == null) task.setId(UUID.randomUUID().toString());

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, task.getId());
            stmt.setString(2, task.getTitle());
            stmt.setString(3, task.getDescription());
            stmt.setString(4, task.getPriority().name());
            stmt.setString(5, task.getStatus().name());
            stmt.setTimestamp(6, task.getDueDate() != null ? Timestamp.valueOf(task.getDueDate()) : null);

            // --- ICI : Extraction de l'ID utilisateur ---
            if (task.getUser() != null) {
                stmt.setString(7, task.getUser().getId());
            } else {
                throw new SQLException("Une tâche doit avoir un utilisateur assigné");
            }
            // Ajout du project_id
            stmt.setString(8, task.getProjectId());
            // --------------------------------------------

            stmt.setString(9, task.getParentTask() != null ? "SUB" : "MAIN");
            stmt.setString(10, task.getParentTask() != null ? task.getParentTask().getId() : null);
            stmt.setTimestamp(11, Timestamp.valueOf(java.time.LocalDateTime.now()));
            
            // Add recurrence fields
            stmt.setString(12, task.getRecurrenceType() != null ? task.getRecurrenceType() : "NONE");
            stmt.setInt(13, task.getRecurrenceInterval() != null ? task.getRecurrenceInterval() : 0);

            stmt.executeUpdate();
        }
    }

    /**
     * ✅ FIXED: Finds a task by ID - includes username and email
     */
    public Optional<Task> findById(String id) {
        String sql = "SELECT t.*, u.username FROM tasks t JOIN users u ON t.user_id = u.id WHERE t.task_type = 'MAIN';";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error finding task by ID: " + e.getMessage());
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
     * ✅ FIXED: Retrieves all tasks for a user by user ID
     * Now includes username and email in SELECT
     */
    public List<Task> findByUserId(String userId) {
        String sql = "SELECT t.*, u.id as user_id, u.username, u.email " +
                "FROM tasks t " +
                "INNER JOIN users u ON t.user_id = u.id " +
                "WHERE t.user_id = ? " +
                "ORDER BY t.created_at DESC";

        List<Task> tasks = new ArrayList<>();

        System.out.println("🔍 TaskDAO.findByUserId() called with userId: " + userId);

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    try {
                        Task task = mapResultSetToTask(rs);
                        tasks.add(task);
                        System.out.println("   ✅ Task #" + count + ": " + task.getTitle() +
                                " (Priority: " + task.getPriority() + ", Status: " + task.getStatus() + ")");
                    } catch (Exception e) {
                        System.err.println("   ❌ Failed to map task row #" + count + ": " + e.getMessage());
                        System.err.println("      Task ID: " + rs.getString("id"));
                        System.err.println("      Title: " + rs.getString("title"));
                        System.err.println("      Priority (DB): " + rs.getString("priority"));
                        System.err.println("      Status (DB): " + rs.getString("status"));
                        e.printStackTrace();
                    }
                }
                System.out.println("📊 Total tasks loaded for user " + userId + ": " + tasks.size());
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Error in findByUserId: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * ✅ FIXED: Retrieves all tasks - includes username and email
     */
    // Méthode de lecture optimisée avec jointure pour avoir le Username
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        // Jointure pour récupérer le nom de l'utilisateur directement
        String sql = "SELECT t.*, u.username FROM tasks t LEFT JOIN users u ON t.user_id = u.id WHERE t.task_type = 'MAIN'";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

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
    // Méthode UPDATE : Mise à jour d'une tâche
    public void update(Task task) throws SQLException {
    String sql = "UPDATE tasks SET title=?, description=?, priority=?, status=?, due_date=?, user_id=?, project_id=?, recurrence_type=?, recurrence_interval=? WHERE id=?";

    try (Connection conn = DatabaseConnection.getConnection();
         PreparedStatement stmt = conn.prepareStatement(sql)) {

        stmt.setString(1, task.getTitle());
        stmt.setString(2, task.getDescription());
        stmt.setString(3, task.getPriority().name());
        stmt.setString(4, task.getStatus().name());
        stmt.setTimestamp(5, task.getDueDate() != null ? Timestamp.valueOf(task.getDueDate()) : null);
        stmt.setString(6, task.getUser().getId());
        
        // --- MODIFICATION ICI ---
        // On s'assure d'envoyer NULL proprement si l'ID est vide, sinon la String
        if (task.getProjectId() != null) {
            stmt.setString(7, task.getProjectId());
        } else {
            stmt.setNull(7, java.sql.Types.VARCHAR);
        }
        // --- Add recurrence fields ---
        stmt.setString(8, task.getRecurrenceType() != null ? task.getRecurrenceType() : "NONE");
        stmt.setInt(9, task.getRecurrenceInterval() != null ? task.getRecurrenceInterval() : 0);
        // ------------------------

        stmt.setString(10, task.getId());

        stmt.executeUpdate();
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
     * Récupère les tâches liées à un projet spécifique.
     */
    public List<Task> findByProjectId(String projectId) {
        List<Task> tasks = new ArrayList<>();
        // On sélectionne les tâches qui ont ce project_id
        String sql = "SELECT t.*, u.username FROM tasks t " +
                "LEFT JOIN users u ON t.user_id = u.id " +
                "WHERE t.project_id = ? ORDER BY t.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, projectId);

            try (ResultSet rs = stmt.executeQuery()) {
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
     * ✅ FIXED: Maps a ResultSet row to a Task object
     * - No password column needed
     * - Validates priority/status enums with fallback
     */
    // Helper pour transformer le résultat SQL en Objet Task
    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
    Task t = new Task();
    t.setId(rs.getString("id"));
    t.setTitle(rs.getString("title"));
    t.setDescription(rs.getString("description"));
    t.setPriority(Priority.valueOf(rs.getString("priority")));
    t.setStatus(Status.valueOf(rs.getString("status")));

    // --- LIGNE MANQUANTE À AJOUTER ICI ---
    t.setProjectId(rs.getString("project_id")); 
    // -------------------------------------

    Timestamp ts = rs.getTimestamp("due_date");
    if (ts != null) t.setDueDate(ts.toLocalDateTime());

    // User
    String userId = rs.getString("user_id");
    if (userId != null) {
        User u = new User();
        u.setId(userId);
        try { u.setUsername(rs.getString("username")); } catch (Exception e) {}
        t.setUser(u);
    }

    // Parent
    String parentId = rs.getString("parent_task_id");
    if (parentId != null) {
        Task parent = new Task();
        parent.setId(parentId);
        t.setParentTask(parent);
    }
    
    // Recurrence
    try {
        String recurrenceType = rs.getString("recurrence_type");
        if (recurrenceType != null) {
            t.setRecurrenceType(recurrenceType);
        }
        int recurrenceInterval = rs.getInt("recurrence_interval");
        t.setRecurrenceInterval(recurrenceInterval);
    } catch (Exception e) {
        // Column might not exist in older database versions
        t.setRecurrenceType("NONE");
        t.setRecurrenceInterval(0);
    }

    return t;
}


    public List<Task> findSubTasks(String parentId) {
        List<Task> tasks = new ArrayList<>();
        // La requête SQL avec jointure pour avoir le nom de l'utilisateur
        String sql = "SELECT t.*, u.username FROM tasks t " +
                "LEFT JOIN users u ON t.user_id = u.id " +
                "WHERE t.parent_task_id = ? ORDER BY t.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, parentId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    // Utilise ta méthode existante pour convertir le résultat en objet Task
                    tasks.add(mapResultSetToTask(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return tasks;
    }
}