package com.smarttask.client.view.controller.teams;

import com.smarttask.model.Team;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class TeamCardController {

    @FXML
    private StackPane colorBadge;

    @FXML
    private Label teamName;

    @FXML
    private Label teamDescription;

    @FXML
    private Label memberCountLabel;

    private Team team;

    @FXML
    public void initialize() {
        setupHoverEffects();
    }

    private void setupHoverEffects() {
        // We'll use scaling for a "pop" effect on hover
        javafx.scene.Node parent = teamName.getParent().getParent().getParent(); // The card itself

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

    public void setTeamData(Team team, int memberCount) {
        this.team = team;
        teamName.setText(team.getName());
        teamDescription.setText(team.getDescription());
        memberCountLabel.setText(memberCount + (memberCount > 1 ? " members" : " member"));

        // Appliquer la couleur de fond au badge
        if (team.getColor() != null && !team.getColor().isEmpty()) {
            String color = team.getColor();
            // Convertir de format hex optionnel 0x... en format CSS #...
            if (color.startsWith("0x")) {
                color = "#" + color.substring(2, 8);
            }
            colorBadge.setStyle("-fx-background-color: " + color + ";");
        }
    }

    public Team getTeam() {
        return team;
    }
}
