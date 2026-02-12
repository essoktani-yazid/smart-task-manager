package com.smarttask.server.dao;

import com.smarttask.model.Project;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProjectDAO {

    public String save(Project project) {
        String sql = "INSERT INTO projects (id, name, description, user_id, color, created_at, updated_at, is_active) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String id = java.util.UUID.randomUUID().toString();

        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            pstmt.setString(2, project.getName());
            pstmt.setString(3, project.getDescription());
            pstmt.setString(4, project.getUserId());
            pstmt.setString(5, project.getColor());
            pstmt.setTimestamp(6, project.getCreatedAt() != null ? Timestamp.valueOf(project.getCreatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setTimestamp(7, project.getUpdatedAt() != null ? Timestamp.valueOf(project.getUpdatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setBoolean(8, project.isActive());

            int rows = pstmt.executeUpdate();
            if (rows == 0) throw new SQLException("Creating project failed");
            project.setId(id);
            return id;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public Optional<Project> findById(String id) {
        String sql = "SELECT * FROM projects WHERE id = ?";
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

    public List<Project> findByUserId(String userId) {
        String sql = "SELECT * FROM projects WHERE user_id = ? ORDER BY created_at DESC";
        List<Project> list = new ArrayList<>();
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

    public List<Project> findAll() {
        String sql = "SELECT * FROM projects ORDER BY created_at DESC";
        List<Project> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql); ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    public List<Project> getProjectsByUserId(String userId) {
        String sql = "SELECT * FROM projects WHERE user_id = ? AND is_active = TRUE ORDER BY name";
        List<Project> projects = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                Project project = new Project();
                project.setId(rs.getString("id"));
                project.setName(rs.getString("name"));
                project.setDescription(rs.getString("description"));
                project.setColor(rs.getString("color"));
                projects.add(project);
            }

        } catch (SQLException e) {
            System.err.println("Error fetching projects: " + e.getMessage());
            e.printStackTrace();
        }

        return projects;
    }

    public void update(Project project) {
        String sql = "UPDATE projects SET name = ?, description = ?, color = ?, updated_at = ?, is_active = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, project.getName());
            pstmt.setString(2, project.getDescription());
            pstmt.setString(3, project.getColor());
            pstmt.setTimestamp(4, project.getUpdatedAt() != null ? Timestamp.valueOf(project.getUpdatedAt()) : Timestamp.valueOf(LocalDateTime.now()));
            pstmt.setBoolean(5, project.isActive());
            pstmt.setString(6, project.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    public boolean deleteById(String id) {
        String sql = "DELETE FROM projects WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            int affected = pstmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    private Project map(ResultSet rs) throws SQLException {
        Project p = new Project();
        p.setId(rs.getString("id"));
        p.setName(rs.getString("name"));
        p.setDescription(rs.getString("description"));
        p.setUserId(rs.getString("user_id"));
        p.setColor(rs.getString("color"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) p.setUpdatedAt(ua.toLocalDateTime());
        p.setActive(rs.getBoolean("is_active"));
        return p;
    }
}
