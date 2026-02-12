package com.smarttask.model;



import java.time.LocalDateTime;

/**
 * Model representing a team member.
 */
public class TeamMember {

    private String teamId;
    private String userId;
    private User user;
	private Team team;
    private String role;
	private LocalDateTime joinedAt;

    public TeamMember() {
    }

    public TeamMember(String teamId, String userId) {
        this.teamId = teamId;
        this.userId = userId;
        this.joinedAt = LocalDateTime.now();
    }

	public TeamMember(Team team, User user, String joinedAt) {
		this();
        this.team = team;
        this.user = user;

        if (joinedAt != null && !joinedAt.isEmpty()) {
            try {
                this.joinedAt = LocalDateTime.parse(joinedAt);
            } catch (Exception e) {
                this.joinedAt = LocalDateTime.now();
            }
        } else {
            this.joinedAt = LocalDateTime.now();
        }
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team newTeam) {
        this.team = newTeam;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String newRole) {
        this.role = newRole;
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
                ", joinedAt=" + joinedAt +
                '}';
    }
}

