package com.smarttask.server.dao;

import com.smarttask.model.CommentAttachment;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommentAttachmentDAO {

    public String save(CommentAttachment attachment) {
        String sql = "INSERT INTO comment_attachments (id, comment_id, file_name, file_type, file_size, file_path, uploaded_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, attachment.getId());
            pstmt.setString(2, attachment.getCommentId());
            pstmt.setString(3, attachment.getFileName());
            pstmt.setString(4, attachment.getFileType());
            pstmt.setLong(5, attachment.getFileSize() != null ? attachment.getFileSize() : 0);
            pstmt.setString(6, attachment.getFilePath());
            pstmt.setTimestamp(7, new Timestamp(attachment.getUploadedAt().getTime()));
            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new SQLException("Failed to insert attachment");
            return attachment.getId();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Optional<CommentAttachment> findById(String id) {
        String sql = "SELECT * FROM comment_attachments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<CommentAttachment> findByCommentId(String commentId) {
        String sql = "SELECT * FROM comment_attachments WHERE comment_id = ?";
        List<CommentAttachment> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, commentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public boolean deleteById(String id) {
        String sql = "DELETE FROM comment_attachments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteByCommentId(String commentId) {
        String sql = "DELETE FROM comment_attachments WHERE comment_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, commentId);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private CommentAttachment map(ResultSet rs) throws SQLException {
        CommentAttachment attachment = new CommentAttachment();
        attachment.setId(rs.getString("id"));
        attachment.setCommentId(rs.getString("comment_id"));
        attachment.setFileName(rs.getString("file_name"));
        attachment.setFileType(rs.getString("file_type"));
        attachment.setFileSize(rs.getLong("file_size"));
        attachment.setFilePath(rs.getString("file_path"));
        attachment.setUploadedAt(new java.util.Date(rs.getTimestamp("uploaded_at").getTime()));
        return attachment;
    }
}
