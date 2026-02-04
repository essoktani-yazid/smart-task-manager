package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.model.Team;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.lang.Runnable;

public class CreateTeamDialogController {

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private ColorPicker colorPicker;

    private Stage dialogStage;
    private User currentUser;
    private final TeamService teamService = new TeamService();
    private Runnable onTeamCreated; // callback pour notifier TeamsController

    @FXML
    private void initialize() {
        colorPicker.setValue(Color.web("#3788d8"));
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
    }

    public void setOnTeamCreated(Runnable callback) {
        this.onTeamCreated = callback;
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleCreate() {
        if (currentUser == null) return;

        String name = nameField.getText().trim();
        String description = descriptionField.getText().trim();
        String color = colorPicker.getValue().toString();

        if (name.isEmpty()) {
            System.err.println("Le nom de l'équipe est requis");
            return;
        }
        Team team = new Team();
        team.setName(name);
        team.setDescription(description);
        team.setColor(color);
        team.setOwnerId(currentUser.getId());

        boolean success = teamService.createTeam(team);
        if (success) {
            System.out.println("Team created: " + name);
            if (onTeamCreated != null) {
                onTeamCreated.run();
            }
            dialogStage.close();
        } else {
            System.err.println("Impossible de créer la team");
        }
    }
}

