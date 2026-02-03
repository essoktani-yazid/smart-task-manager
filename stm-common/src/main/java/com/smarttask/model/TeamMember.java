package com.smarttask.model;

import java.time.LocalDateTime;

/**
 * Model representing a team member.
 */
public class TeamMember {

    private String teamId;
    private String userId;
    private String role;
	private LocalDateTime joinedAt;

    public TeamMember() {
    }

    public TeamMember(String teamId, String userId, String role) {
        this.teamId = teamId;
        this.userId = userId;
        this.role = role;
        this.joinedAt = LocalDateTime.now();
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public LocalDateTime getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(LocalDateTime joinedAt) {
        this.joinedAt = joinedAt;
    }

    @Override
    public String toString() {
        return "TeamMember{" +
                "teamId='" + teamId + '\'' +
                ", userId='" + userId + '\'' +
                ", role='" + role + '\'' +
                ", joinedAt=" + joinedAt +
                '}';
    }
}

