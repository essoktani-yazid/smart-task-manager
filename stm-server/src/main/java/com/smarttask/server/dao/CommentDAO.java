package com.smarttask.server.dao;

import com.smarttask.model.Comment;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommentDAO {

    public String save(Comment comment) {
        String sql = "INSERT INTO comments (id, task_id, user_id, content, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        String id = java.util.UUID.randomUUID().toString();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, comment.getTask().getId());
            pstmt.setString(3, comment.getUser().getId());
            pstmt.setString(4, comment.getContent());
            pstmt.setTimestamp(5, new Timestamp(comment.getCreatedAt().getTime()));
            pstmt.setTimestamp(6, new Timestamp(comment.getUpdatedAt().getTime()));
            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new SQLException("Failed to insert comment");
            comment.setId(id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Optional<Comment> findById(String id) {
        String sql = "SELECT c.*, u.username, u.email FROM comments c INNER JOIN users u ON c.user_id = u.id WHERE c.id = ?";
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

    public List<Comment> findByTaskId(String taskId) {
        String sql = "SELECT c.*, u.id as user_id, u.username, u.email FROM comments c INNER JOIN users u ON c.user_id = u.id WHERE c.task_id = ? ORDER BY c.created_at DESC";
        List<Comment> list = new ArrayList<>();
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

    public void update(Comment comment) {
        String sql = "UPDATE comments SET content = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, comment.getContent());
            pstmt.setTimestamp(2, new Timestamp(new java.util.Date().getTime()));
            pstmt.setString(3, comment.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean deleteById(String id) {
        String sql = "DELETE FROM comments WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Comment map(ResultSet rs) throws SQLException {
        Comment c = new Comment();
        c.setId(rs.getString("id"));
        c.setContent(rs.getString("content"));
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) c.setCreatedAt(new java.util.Date(ct.getTime()));
        Timestamp ut = rs.getTimestamp("updated_at");
        if (ut != null) c.setUpdatedAt(new java.util.Date(ut.getTime()));
        
        User user = new User();
        user.setId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        c.setUser(user);
        
        return c;
    }
}
