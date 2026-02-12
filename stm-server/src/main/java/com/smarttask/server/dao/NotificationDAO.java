package com.smarttask.server.dao;

import com.smarttask.model.Notification;
import com.smarttask.model.User;
import com.smarttask.server.config.DatabaseConnection;
import com.smarttask.server.socket.NotificationWebSocketServer;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NotificationDAO {

    public String save(Notification notif) {
        String sql = "INSERT INTO notifications (id, user_id, type, title, message, is_read, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String id = java.util.UUID.randomUUID().toString();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, notif.getUser().getId());
            pstmt.setString(3, notif.getType());
            pstmt.setString(4, notif.getTitle());
            pstmt.setString(5, notif.getMessage());
            pstmt.setBoolean(6, notif.getIsRead());
            pstmt.setTimestamp(7, new Timestamp(notif.getCreatedAt().getTime()));
            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new SQLException("Failed to insert notification");
            notif.setId(id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Optional<Notification> findById(String id) {
        String sql = "SELECT n.*, u.id as user_id, u.username, u.email FROM notifications n INNER JOIN users u ON n.user_id = u.id WHERE n.id = ?";
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

    public List<Notification> findByUserId(String userId) {
        String sql = "SELECT n.*, u.id as user_id, u.username, u.email FROM notifications n INNER JOIN users u ON n.user_id = u.id WHERE n.user_id = ? ORDER BY n.created_at DESC";
        List<Notification> list = new ArrayList<>();
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

    public List<Notification> findUnreadByUserId(String userId) {
        String sql = "SELECT n.*, u.id as user_id, u.username, u.email FROM notifications n INNER JOIN users u ON n.user_id = u.id WHERE n.user_id = ? AND n.is_read = FALSE ORDER BY n.created_at DESC";
        List<Notification> list = new ArrayList<>();
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

    public void markAsRead(String id) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void markAllAsRead(String userId) {
        String sql = "UPDATE notifications SET is_read = TRUE WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean deleteById(String id) {
        String sql = "DELETE FROM notifications WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public void createAndSend(String userId, String type, String title, String message) {
        Notification notif = new Notification();
        User recipient = new User();
        recipient.setId(userId);

        notif.setUser(recipient);
        notif.setType(type);
        notif.setTitle(title);
        notif.setMessage(message);
        notif.setIsRead(false);

        this.saveAndNotify(notif); 
    }

    private String saveAndNotify(Notification notif) {
        String id = save(notif);

        NotificationWebSocketServer.sendToUser(notif.getUser().getId(), notif);
        
        return id;
    }

    private Notification map(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setId(rs.getString("id"));
        n.setType(rs.getString("type"));
        n.setTitle(rs.getString("title"));
        n.setMessage(rs.getString("message"));
        n.setIsRead(rs.getBoolean("is_read"));
        
        User user = new User();
        user.setId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        n.setUser(user);
        
        Timestamp ct = rs.getTimestamp("created_at");
        if (ct != null) n.setCreatedAt(new java.util.Date(ct.getTime()));
        
        return n;
    }
}
