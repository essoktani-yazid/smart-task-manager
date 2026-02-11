package com.smarttask.server.dao;

import com.smarttask.model.Team;
import com.smarttask.model.TeamMember;
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
        String sql = "SELECT * FROM teams WHERE id = ? AND is_active = TRUE";

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
                    SET name = ?, description = ?, color = ?, is_active = ?
                    WHERE id = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, team.getName());
            stmt.setString(2, team.getDescription());
            stmt.setString(3, team.getColor());
            stmt.setBoolean(4, team.isActive());
            stmt.setString(5, team.getId());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Error updating team", e);
        }
    }

    /**
     * Soft delete a team.
     */
    public void deactivate(String teamId) {
        String sql = "UPDATE teams SET is_active = FALSE WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addMember(String teamId, String userId, String role) {
        String sql = """
                    INSERT INTO team_members (team_id, user_id, role)
                    VALUES (?, ?, ?)
                """;

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);
            stmt.setString(2, userId);
            stmt.setString(3, role);

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
        String sql = "SELECT * FROM team_members WHERE team_id = ?";
        List<TeamMember> members = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, teamId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    members.add(mapMember(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return members;
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

    private TeamMember mapMember(ResultSet rs) throws SQLException {
        TeamMember member = new TeamMember();
        member.setTeamId(rs.getString("team_id"));
        member.setUserId(rs.getString("user_id"));
        member.setRole(rs.getString("role"));
        member.setJoinedAt(rs.getTimestamp("joined_at").toLocalDateTime());
        return member;
    }
}
