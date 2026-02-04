package com.smarttask.server.dao;

import com.smarttask.model.Attachment;
import com.smarttask.model.Task;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AttachmentDAO {

    public String save(Attachment attachment) {
        String sql = "INSERT INTO attachments (id, task_id, file_name, file_type, file_path, file_size, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String id = UUID.randomUUID().toString();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, attachment.getTask().getId());
            pstmt.setString(3, attachment.getFileName());
            pstmt.setString(4, attachment.getFileType());
            pstmt.setString(5, attachment.getFilePath());
            pstmt.setLong(6, attachment.getFileSize());
            pstmt.setTimestamp(7, Timestamp.valueOf(attachment.getUploadedAt()));
            
            pstmt.executeUpdate();
            attachment.setId(id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error saving attachment", e);
        }
    }

    public List<Attachment> findByTask(String taskId) {
        String sql = "SELECT * FROM attachments WHERE task_id = ? ORDER BY uploaded_at DESC";
        List<Attachment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, taskId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Attachment a = new Attachment();
                    a.setId(rs.getString("id"));
                    Task t = new Task(); t.setId(taskId);
                    a.setTask(t);
                    a.setFileName(rs.getString("file_name"));
                    a.setFileType(rs.getString("file_type"));
                    a.setFilePath(rs.getString("file_path"));
                    a.setFileSize(rs.getLong("file_size"));
                    Timestamp ts = rs.getTimestamp("uploaded_at");
                    if (ts != null) a.setUploadedAt(ts.toLocalDateTime());
                    list.add(a);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean delete(String id) {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
