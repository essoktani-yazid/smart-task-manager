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
 */
public class TaskDAO {

    /**
     * Saves a new task to the database.
     */
    public void save(Task task) throws SQLException {
        // Assurez-vous que la colonne task_type existe dans votre BDD, sinon retirez-la de la requête
        String sql = "INSERT INTO tasks (id, title, description, priority, status, due_date, user_id, project_id, parent_task_id, created_at, recurrence_type, recurrence_interval) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        if (task.getId() == null) task.setId(UUID.randomUUID().toString());

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, task.getId());
            stmt.setString(2, task.getTitle());
            stmt.setString(3, task.getDescription());
            stmt.setString(4, task.getPriority().name());
            stmt.setString(5, task.getStatus().name());
            stmt.setTimestamp(6, task.getDueDate() != null ? Timestamp.valueOf(task.getDueDate()) : null);

            if (task.getUser() != null) {
                stmt.setString(7, task.getUser().getId());
            } else {
                throw new SQLException("Une tâche doit avoir un utilisateur assigné");
            }

            stmt.setString(8, task.getProjectId());
            
            // task_type : MAIN ou SUB selon s'il y a un parent
            stmt.setString(9, task.getParentTask() != null ? task.getParentTask().getId() : null);
            stmt.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            
            // Recurrence fields
            stmt.setString(11, task.getRecurrenceType() != null ? task.getRecurrenceType() : "NONE");
            stmt.setInt(12, task.getRecurrenceInterval());

            stmt.executeUpdate();
        }
    }

    /**
     * Finds a task by ID.
     */
    public Optional<Task> findById(String id) {
        String sql = "SELECT t.*, u.username FROM tasks t LEFT JOIN users u ON t.user_id = u.id WHERE t.id = ?";

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
     * Retrieves all tasks for a user object.
     */
    public List<Task> findByUser(User user) {
        return findByUserId(user.getId());
    }

    /**
     * Retrieves all tasks for a user by user ID.
     */
    public List<Task> findByUserId(String userId) {
        // ⚡ OPTIMISÉ: Utilise l'index idx_user_created (user_id, created_at)
        // Pas de SELECT * - uniquement les colonnes nécessaires
        String sql = "SELECT t.id, t.title, t.description, t.priority, t.status, t.due_date, " +
                     "t.created_at, t.user_id, t.project_id, t.parent_task_id, " +
                     "t.recurrence_type, t.recurrence_interval, u.username, u.email " +
                     "FROM tasks t " +
                     "FORCE INDEX (idx_user_id) " +  // Force utilisation index
                     "LEFT JOIN users u ON t.user_id = u.id " +
                     "WHERE t.user_id = ? " +
                     "ORDER BY t.created_at DESC " +
                     "LIMIT 1000";  // Limite de sécurité

        List<Task> tasks = new ArrayList<>();
        long startTime = System.currentTimeMillis();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setFetchSize(100);  // ⚡ Fetch par batch de 100

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    try {
                        tasks.add(mapResultSetToTask(rs));
                    } catch (Exception e) {
                        System.err.println("❌ Failed to map task: " + e.getMessage());
                    }
                }
                long duration = System.currentTimeMillis() - startTime;
                System.out.println("⚡ Tasks loaded in " + duration + "ms for user " + userId + ": " + tasks.size());
            }
        } catch (SQLException e) {
            System.err.println("❌ SQL Error in findByUserId: " + e.getMessage());
            e.printStackTrace();
        }

        return tasks;
    }

    /**
     * Retrieves all MAIN tasks.
     */
    public List<Task> findAll() {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT t.*, u.username FROM tasks t LEFT JOIN users u ON t.user_id = u.id"; 

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
    public void update(Task task) throws SQLException {
        // Assurez-vous que l'ordre des ? correspond aux setString ci-dessous
        String sql = "UPDATE tasks SET title=?, description=?, priority=?, status=?, due_date=?, user_id=?, project_id=?, recurrence_type=?, recurrence_interval=? WHERE id=?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, task.getTitle());
            stmt.setString(2, task.getDescription());
            stmt.setString(3, task.getPriority().name());
            stmt.setString(4, task.getStatus().name());
            stmt.setTimestamp(5, task.getDueDate() != null ? Timestamp.valueOf(task.getDueDate()) : null);
            stmt.setString(6, task.getUser() != null ? task.getUser().getId() : null);

            if (task.getProjectId() != null) {
                stmt.setString(7, task.getProjectId());
            } else {
                stmt.setNull(7, java.sql.Types.VARCHAR);
            }

            stmt.setString(8, task.getRecurrenceType() != null ? task.getRecurrenceType() : "NONE");
            stmt.setInt(9, task.getRecurrenceInterval());

            stmt.setString(10, task.getId()); // WHERE id = ?

            stmt.executeUpdate();
        }
    }

    /**
     * Deletes a task by object.
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
     * Finds sub-tasks for a given parent task ID.
     */
    public List<Task> findSubTasks(String parentId) {
        List<Task> tasks = new ArrayList<>();
        String sql = "SELECT t.*, u.username FROM tasks t " +
                     "LEFT JOIN users u ON t.user_id = u.id " +
                     "WHERE t.parent_task_id = ? ORDER BY t.created_at DESC";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, parentId);

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

    // =================================================================================
    // PRIVATE HELPER METHODS
    // =================================================================================

    /**
     * Maps a ResultSet row to a Task object.
     */
    private Task mapResultSetToTask(ResultSet rs) throws SQLException {
        Task t = new Task();
        t.setId(rs.getString("id"));
        t.setTitle(rs.getString("title"));
        t.setDescription(rs.getString("description"));

        // Gestion Priority avec fallback
        try {
            String p = rs.getString("priority");
            t.setPriority(p != null ? Priority.valueOf(p) : Priority.MEDIUM);
        } catch (IllegalArgumentException e) {
            t.setPriority(Priority.MEDIUM);
        }

        // Gestion Status avec fallback
        try {
            String s = rs.getString("status");
            t.setStatus(s != null ? Status.valueOf(s) : Status.TODO);
        } catch (IllegalArgumentException e) {
            t.setStatus(Status.TODO);
        }

        // Project ID
        // Vérifier si la colonne existe dans le ResultSet pour éviter les erreurs
        try {
            t.setProjectId(rs.getString("project_id"));
        } catch (SQLException e) {
            // La colonne project_id n'est peut-être pas sélectionnée dans certaines requêtes
        }

        // Due Date
        Timestamp ts = rs.getTimestamp("due_date");
        if (ts != null) t.setDueDate(ts.toLocalDateTime());

        // Created At
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) t.setCreatedAt(created.toLocalDateTime());

        // User Mapping
        String userId = rs.getString("user_id");
        if (userId != null) {
            User u = new User();
            u.setId(userId);
            // On essaie de récupérer le username s'il est présent dans la jointure
            try { u.setUsername(rs.getString("username")); } catch (SQLException e) {}
            try { u.setEmail(rs.getString("email")); } catch (SQLException e) {}
            t.setUser(u);
        }

        // Parent Task Mapping
        String parentId = rs.getString("parent_task_id");
        if (parentId != null) {
            Task parent = new Task();
            parent.setId(parentId);
            t.setParentTask(parent);
        }

        // Recurrence Mapping
        try {
            String recurrenceType = rs.getString("recurrence_type");
            t.setRecurrenceType(recurrenceType != null ? recurrenceType : "NONE");
            t.setRecurrenceInterval(rs.getInt("recurrence_interval"));
        } catch (SQLException e) {
            // Colonnes manquantes dans la BDD
            t.setRecurrenceType("NONE");
            t.setRecurrenceInterval(0);
        }

        return t;
    }
}