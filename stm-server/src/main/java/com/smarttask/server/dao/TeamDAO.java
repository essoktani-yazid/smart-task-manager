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

        String ownerSql = """
            INSERT INTO team_members (team_id, user_id, role)
            VALUES (?, ?, 'OWNER')
        """;

        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement teamStmt = conn.prepareStatement(teamSql);
                 PreparedStatement ownerStmt = conn.prepareStatement(ownerSql)) {

                teamStmt.setString(1, teamId);
                teamStmt.setString(2, team.getName());
                teamStmt.setString(3, team.getDescription());
                teamStmt.setString(4, team.getColor());
                teamStmt.setString(5, team.getOwnerId());
                teamStmt.executeUpdate();

                ownerStmt.setString(1, teamId);
                ownerStmt.setString(2, team.getOwnerId());
                ownerStmt.executeUpdate();

                conn.commit();
                team.setId(teamId);
                return teamId;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
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
                    return Optional.of(mapTeam(rs));
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
            JOIN team_members tm ON t.id = tm.team_id
            WHERE tm.user_id = ?
        """;

        List<Team> teams = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, userId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    teams.add(mapTeam(rs));
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
            SET name = ?, description = ?, color = ?
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
            INSERT INTO team_members (team_id, user_id)
            VALUES (?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);
            stmt.setString(2, userId);

            stmt.executeUpdate();
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

    public List<TeamMember> findMembers(String teamId) {
        String sql = """
            SELECT tm.*, 
                u.username, u.full_name, u.email,
                t.name AS team_name, t.description AS team_desc, t.color AS team_color, t.owner_id AS team_owner
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
        member.setTeamId(rs.getString("team_id"));
        member.setUserId(rs.getString("user_id"));
        member.setJoinedAt(rs.getTimestamp("joined_at").toLocalDateTime());

        User user = mapUser(rs);
        member.setUser(user);

        Team team = mapTeam(rs);
        member.setTeam(team);

        return member;
    }

    private Team mapTeam(ResultSet rs) throws SQLException {
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

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getString("user_id"));
        user.setUsername(rs.getString("username"));
        user.setFirstName(rs.getString("first_name"));
        user.setLastName(rs.getString("last_name"));
        user.setEmail(rs.getString("email"));
        return user;
    }
}

