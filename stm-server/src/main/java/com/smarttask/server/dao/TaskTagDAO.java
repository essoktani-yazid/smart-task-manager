package com.smarttask.server.dao;

import com.smarttask.model.Task;
import com.smarttask.model.TaskTag;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TaskTagDAO {

    public String save(TaskTag tag) {
        String sql = "INSERT INTO task_tags (task_id, tag_name, created_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String taskId = tag.getTask().getId();
            pstmt.setString(1, taskId);
            pstmt.setString(2, tag.getTagName());
            pstmt.setTimestamp(3, new Timestamp(new java.util.Date().getTime()));
            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new SQLException("Failed to insert tag");
            return taskId + "::" + tag.getTagName();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public List<TaskTag> findByTaskId(String taskId) {
        String sql = "SELECT * FROM task_tags WHERE task_id = ? ORDER BY created_at DESC";
        List<TaskTag> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    TaskTag t = new TaskTag();
                    Task task = new Task(); task.setId(rs.getString("task_id"));
                    t.setTask(task);
                    t.setTagName(rs.getString("tag_name"));
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean delete(String taskId, String tagName) {
        String sql = "DELETE FROM task_tags WHERE task_id = ? AND tag_name = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, taskId);
            pstmt.setString(2, tagName);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
