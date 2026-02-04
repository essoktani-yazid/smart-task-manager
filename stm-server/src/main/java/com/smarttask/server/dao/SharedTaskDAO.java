package com.smarttask.server.dao;

import com.smarttask.model.SharedTask;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SharedTaskDAO {

    public void save(SharedTask shared) {
        String sql = "INSERT INTO shared_tasks (task_id, user_id, permission_level, shared_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, shared.getTask().getId());
            pstmt.setString(2, shared.getUser().getId());
            pstmt.setString(3, shared.getPermissionLevel().name());
            pstmt.setTimestamp(4, new Timestamp(shared.getSharedAt().getTime()));
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<SharedTask> findByTaskId(String taskId) {
        String sql = "SELECT s.*, u.id as user_id, u.username, u.email FROM shared_tasks s INNER JOIN users u ON s.user_id = u.id WHERE s.task_id = ?";
        List<SharedTask> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<SharedTask> findByUserId(String userId) {
        String sql = "SELECT s.*, u.id as user_id, u.username, u.email FROM shared_tasks s INNER JOIN users u ON s.user_id = u.id WHERE s.user_id = ?";
        List<SharedTask> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean delete(String taskId, String userId) {
        String sql = "DELETE FROM shared_tasks WHERE task_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, taskId);
            pstmt.setString(2, userId);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void updatePermission(String taskId, String userId, String permissionLevel) {
        String sql = "UPDATE shared_tasks SET permission_level = ? WHERE task_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, permissionLevel);
            pstmt.setString(2, taskId);
            pstmt.setString(3, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private SharedTask map(ResultSet rs) throws SQLException {
        SharedTask st = new SharedTask();
        
        Task task = new Task();
        task.setId(rs.getString("task_id"));
        st.setTask(task);
        
        User user = new User();
        user.setId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        st.setUser(user);
        
        String permStr = rs.getString("permission_level");
        try {
            st.setPermissionLevel(SharedTask.PermissionLevel.valueOf(permStr));
        } catch (IllegalArgumentException e) {
            st.setPermissionLevel(SharedTask.PermissionLevel.READ);
        }
        
        Timestamp ts = rs.getTimestamp("shared_at");
        if (ts != null) st.setSharedAt(new java.util.Date(ts.getTime()));
        
        return st;
    }
}
