package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.model.Team;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.scene.paint.Color;

import java.util.function.Consumer;

public class EditTeamDialogController {

    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private ColorPicker colorPicker;

    private Stage dialogStage;
    private Team team;
    private User currentUser;
    private final TeamService teamService = new TeamService();
    private Consumer<Void> onTeamUpdated; // callback

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setTeam(Team team) {
        this.team = team;
        populateFields();
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void setOnTeamUpdated(Consumer<Void> callback) {
        this.onTeamUpdated = callback;
    }

    private void populateFields() {
        if (team == null) return;
        nameField.setText(team.getName());
        descriptionField.setText(team.getDescription());
        colorPicker.setValue(Color.web(team.getColor() != null ? team.getColor() : "#3788d8"));
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleSave() {
        if (team == null) return;

        team.setName(nameField.getText());
        team.setDescription(descriptionField.getText());
        team.setColor(String.format("#%02X%02X%02X",
                (int)(colorPicker.getValue().getRed()*255),
                (int)(colorPicker.getValue().getGreen()*255),
                (int)(colorPicker.getValue().getBlue()*255)));

        teamService.updateTeam(team); // à implémenter côté service
        if (onTeamUpdated != null) onTeamUpdated.accept(null);
        dialogStage.close();
    }
}

