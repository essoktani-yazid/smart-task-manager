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
        String sql = "SELECT DISTINCT p.* FROM projects p " +
                    "LEFT JOIN team_projects tp ON p.id = tp.project_id " +
                    "LEFT JOIN team_members tm ON tp.team_id = tm.team_id " +
                    "WHERE p.user_id = ? OR tm.user_id = ? " +
                    "ORDER BY p.created_at DESC";
        
        List<Project> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            pstmt.setString(2, userId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
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

    public void addTeamToProject(String projectId, String teamId) {
        String sql = "INSERT IGNORE INTO team_projects (project_id, team_id) VALUES (?, ?)";
        try (Connection conn = DatabaseConnection.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, projectId);
            pstmt.setString(2, teamId);
            int affected = pstmt.executeUpdate();

            if (affected > 0) {
                TeamDAO teamDAO = new TeamDAO();
                String projectName = getProjectNameById(projectId);
                List<String> memberIds = teamDAO.getTeamMemberIds(teamId);
                
                NotificationDAO notifDAO = new NotificationDAO();
                for (String memberId : memberIds) {
                    notifDAO.createAndSend(
                        memberId,
                        "TASK_ASSIGNED", 
                        "Nouveau Projet d'Équipe", 
                        "Votre équipe a été assignée au projet : " + projectName
                    );
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error linking team to project", e);
        }
    }

    public String getProjectNameById(String projectId) {
        String sql = "SELECT name FROM projects WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, projectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Projet inconnu";
    }

    public void removeTeamFromProject(String projectId, String teamId) {
        String sql = "DELETE FROM team_projects WHERE project_id = ? AND team_id = ?";
        try (Connection conn = DatabaseConnection.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectId);
            pstmt.setString(2, teamId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error removing team from project", e);
        }
    }

    public List<com.smarttask.model.Team> findTeamsByProjectId(String projectId) {
        String sql = "SELECT t.* FROM teams t " +
                    "JOIN team_projects tp ON t.id = tp.team_id " +
                    "WHERE tp.project_id = ?";
        List<com.smarttask.model.Team> teams = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection(); 
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, projectId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    com.smarttask.model.Team team = new com.smarttask.model.Team();
                    team.setId(rs.getString("id"));
                    team.setName(rs.getString("name"));
                    team.setDescription(rs.getString("description"));
                    team.setColor(rs.getString("color"));
                    team.setOwnerId(rs.getString("owner_id"));
                    team.setActive(rs.getBoolean("is_active"));
                    // map the rest
                    teams.add(team);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
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
