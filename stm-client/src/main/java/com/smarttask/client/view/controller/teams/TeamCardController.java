package com.smarttask.client.view.controller.teams;

import com.smarttask.model.Team;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class TeamCardController {

    @FXML
    private StackPane colorBadge;

    @FXML
    private Label teamName, teamDescription, memberCountLabel;

    private Team team;
    private java.util.function.Consumer<Team> onEditRequested;

    @FXML
    public void initialize() {
        setupHoverEffects();
    }

    @FXML
    private void handleCardClick() {
        if (onEditRequested != null) onEditRequested.accept(team);
    }

    private void setupHoverEffects() {
        javafx.scene.Node parent = teamName.getParent().getParent().getParent();

        parent.setOnMouseEntered(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200),
                    parent);
            st.setToX(1.03);
            st.setToY(1.03);
            st.play();
        });

        parent.setOnMouseExited(e -> {
            javafx.animation.ScaleTransition st = new javafx.animation.ScaleTransition(javafx.util.Duration.millis(200),
                    parent);
            st.setToX(1.0);
            st.setToY(1.0);
            st.play();
        });
    }

    public void setTeamData(Team team, int memberCount, java.util.function.Consumer<Team> onEditRequested) {
        this.team = team;
        this.onEditRequested = onEditRequested;
        teamName.setText(team.getName());
        teamDescription.setText(team.getDescription());
        memberCountLabel.setText(memberCount + (memberCount > 1 ? " membres" : " membre"));

        if (team.getColor() != null) {
            String color = team.getColor().replace("0x", "#");
            colorBadge.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");
        }
    }

    public Team getTeam() {
        return team;
    }
}
