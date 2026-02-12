package com.smarttask.server.dao;

import com.smarttask.model.Team;
import com.smarttask.model.TeamMember;
import com.smarttask.model.User;
import com.smarttask.server.config.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Data Access Object for Team & TeamMember entities.
 */
public class TeamDAO {

    /**
     * Create a new team and assign OWNER as first member.
     */
    public String create(Team team) {
        String teamId = UUID.randomUUID().toString();

        String teamSql = """
                    INSERT INTO teams (id, name, description, color, owner_id, is_active)
                    VALUES (?, ?, ?, ?, ?, TRUE)
                """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement teamStmt = conn.prepareStatement(teamSql)) {

                teamStmt.setString(1, teamId);
                teamStmt.setString(2, team.getName());
                teamStmt.setString(3, team.getDescription());
                teamStmt.setString(4, team.getColor());
                teamStmt.setString(5, team.getOwnerId());
                teamStmt.executeUpdate();

                team.setId(teamId);
                return teamId;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error creating team", e);
        }
    }

    /**
     * Find a team by ID.
     */
    public Optional<Team> findById(String teamId) {
        String sql = "SELECT * FROM teams WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapTeam(rs, "id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    /**
     * Get all teams where the user is member or owner.
     */
    public List<Team> findByUser(String userId) {
        String sql = """
                    SELECT DISTINCT t.*
                    FROM teams t
                    LEFT JOIN team_members tm ON t.id = tm.team_id
                    WHERE (tm.user_id = ? OR t.owner_id = ?) AND t.is_active = TRUE
                """;

        List<Team> teams = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);
            stmt.setString(2, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    teams.add(mapTeam(rs, "id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return teams;
    }

    /**
     * Update team info.
     */
    public void update(Team team) {
        String sql = """
                    UPDATE teams
                    SET name = ?, description = ?, color = ?, is_active = ?
                    WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, team.getName());
            stmt.setString(2, team.getDescription());
            stmt.setString(3, team.getColor());
            stmt.setString(4, team.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error updating team", e);
        }
    }

    /**
     * Soft delete a team.
     */
    public void delete(String teamId) {
        String sql = "DELETE FROM teams WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows == 0) {
                System.out.println("[WARN] Aucune équipe trouvée avec l'ID: " + teamId);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Erreur SQL lors de la suppression : " + e.getMessage(), e);
        }
    }

    public void addMember(String teamId, String userId) {
        String sql = """
                    INSERT INTO team_members (team_id, user_id, role)
                    VALUES (?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);
            stmt.setString(2, userId);

            stmt.executeUpdate();

            String teamName = getTeamNameById(teamId); 
        
            NotificationDAO notifDAO = new NotificationDAO();
            notifDAO.createAndSend(
                userId, 
                "TASK_ASSIGNED",
                "Nouvelle Équipe !", 
                "Vous avez été ajouté à l'équipe : " + teamName
            );
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error adding member", e);
        }
    }

    public void removeMember(String teamId, String userId) {
        String sql = "DELETE FROM team_members WHERE team_id = ? AND user_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);
            stmt.setString(2, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateMemberRole(String teamId, String userId, String role) {
        String sql = """
                    UPDATE team_members
                    SET role = ?
                    WHERE team_id = ? AND user_id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, role);
            stmt.setString(2, teamId);
            stmt.setString(3, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<TeamMember> findMembers(String teamId) {
        String sql = """
            SELECT 
                tm.team_id AS tm_team_id, 
                tm.user_id AS tm_user_id, 
                joined_at,
                u.username, u.email,
                t.name AS team_name, t.description AS team_desc, t.color AS team_color, t.owner_id AS team_owner, t.created_at AS team_created_at
            FROM team_members tm
            JOIN users u ON tm.user_id = u.id
            JOIN teams t ON tm.team_id = t.id
            WHERE tm.team_id = ?
        """;
        List<TeamMember> members = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(mapUserAndTeam(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
    }

    private TeamMember mapUserAndTeam(ResultSet rs) throws SQLException {
        TeamMember member = new TeamMember();
        member.setTeamId(rs.getString("tm_team_id"));
        member.setUserId(rs.getString("tm_user_id"));
        member.setJoinedAt(rs.getTimestamp("joined_at").toLocalDateTime());

        User user = new User();
        user.setId(rs.getString("tm_user_id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        
        Team team = new Team();
        team.setId(rs.getString("tm_team_id"));
        team.setName(rs.getString("team_name"));
        team.setDescription(rs.getString("team_desc"));
        team.setColor(rs.getString("team_color"));
        team.setOwnerId(rs.getString("team_owner"));
        team.setCreatedAt(rs.getTimestamp("team_created_at").toLocalDateTime());
    
        member.setUser(user);
        member.setTeam(team);

        return member;
    }

    private Team mapTeam(ResultSet rs, String idColumn) throws SQLException {
        Team team = new Team();

        team.setId(rs.getString("id"));
        team.setName(rs.getString("name"));
        team.setDescription(rs.getString("description"));
        team.setColor(rs.getString("color"));
        team.setOwnerId(rs.getString("owner_id"));
        team.setActive(rs.getBoolean("is_active"));
        team.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return team;
    }

    public List<String> getTeamMemberIds(String teamId) throws SQLException {
        List<String> ids = new ArrayList<>();
        String sql = "SELECT user_id FROM team_members WHERE team_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, teamId);
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) ids.add(rs.getString("user_id"));
        }
        return ids;
    }

    public String getTeamNameById(String teamId) {
        String sql = "SELECT name FROM teams WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, teamId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "Équipe inconnue";
    }
}
