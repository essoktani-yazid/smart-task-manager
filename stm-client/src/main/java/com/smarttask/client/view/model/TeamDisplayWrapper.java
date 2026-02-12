package com.smarttask.client.view.model;

import com.smarttask.model.Team;
import javafx.beans.property.*;
import java.time.format.DateTimeFormatter;

public class TeamDisplayWrapper {
    private final Team team;
    private final StringProperty name;
    private final StringProperty description;
    private final StringProperty members;
    private final StringProperty createdAt;
    private final java.util.List<com.smarttask.model.User> membersList;

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public TeamDisplayWrapper(Team team, String members, java.util.List<com.smarttask.model.User> membersList) {
        this.team = team;
        this.name = new SimpleStringProperty(team.getName());
        this.description = new SimpleStringProperty(team.getDescription());
        this.members = new SimpleStringProperty(members);
        this.createdAt = new SimpleStringProperty(
                team.getCreatedAt() != null ? team.getCreatedAt().format(formatter) : "N/A");
        this.membersList = membersList;
    }

    public String getName() {
        return name.get();
    }

    public StringProperty nameProperty() {
        return name;
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public String getMembers() {
        return members.get();
    }

    public StringProperty membersProperty() {
        return members;
    }

    public String getCreatedAt() {
        return createdAt.get();
    }

    public StringProperty createdAtProperty() {
        return createdAt;
    }

    public java.util.List<com.smarttask.model.User> getMembersList() {
        return membersList;
    }

    public Team getTeam() {
        return team;
    }
}
