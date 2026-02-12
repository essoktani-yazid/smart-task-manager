package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.Team;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class CreateTeamDialogController {

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private ColorPicker colorPicker;

    @FXML
    private ComboBox<User> userSelectionCombo;

    @FXML
    private FlowPane selectedMembersPane;

    private Stage dialogStage;
    private Runnable onTeamCreated;
    private final TeamService teamService = new TeamService();
    private final List<User> selectedUsers = new ArrayList<>();
    private Team editingTeam;

    @FXML
    public void initialize() {
        System.out.println("CreateTeamDialogController initializing");
        colorPicker.setValue(Color.web("#6a7cff")); // Default project theme color
        loadUsers();
    }

    private void loadUsers() {
        new Thread(() -> {
            List<User> allUsers = teamService.getAllUsers();
            // Allow including everyone in the selection list

            javafx.application.Platform.runLater(() -> {
                userSelectionCombo.getItems().addAll(allUsers);
                userSelectionCombo.setCellFactory(lv -> new ListCell<User>() {
                    @Override
                    protected void updateItem(User item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? "" : item.getUsername());
                    }
                });
                userSelectionCombo.setButtonCell(new ListCell<User>() {
                    @Override
                    protected void updateItem(User item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(empty ? "" : item.getUsername());
                    }
                });
            });
        }).start();
    }

    @FXML
    public void handleAddMember() {
        User selected = userSelectionCombo.getSelectionModel().getSelectedItem();
        if (selected != null && !selectedUsers.contains(selected)) {
            selectedUsers.add(selected);
            addMemberChip(selected);
            userSelectionCombo.getSelectionModel().clearSelection();
        }
    }

    private void addMemberChip(User user) {
        HBox chip = new HBox(6);
        chip.getStyleClass().add("member-chip-tag");
        chip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label nameLabel = new Label(user.getUsername());
        Button removeBtn = new Button("×");
        removeBtn.getStyleClass().add("chip-remove-btn");
        removeBtn.setOnAction(e -> {
            selectedUsers.remove(user);
            selectedMembersPane.getChildren().remove(chip);
        });

        chip.getChildren().addAll(nameLabel, removeBtn);
        selectedMembersPane.getChildren().add(chip);
    }

    public void setEditMode(Team team) {
        this.editingTeam = team;
        nameField.setText(team.getName());
        descriptionField.setText(team.getDescription());
        if (team.getColor() != null) {
            try {
                colorPicker.setValue(Color.valueOf(team.getColor()));
            } catch (Exception e) {
                // Ignore
            }
        }
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }


    public void setOnTeamCreated(Runnable onTeamCreated) {
        this.onTeamCreated = onTeamCreated;
    }

    @FXML
    public void handleCancel() {
        dialogStage.close();
    }

    @FXML
    public void handleCreate() {
        String name = nameField.getText();
        String description = descriptionField.getText();
        String color = colorPicker.getValue().toString();

        if (name == null || name.isEmpty()) {
            return;
        }

        if (editingTeam != null) {
            editingTeam.setName(name);
            editingTeam.setDescription(description);
            editingTeam.setColor(color);

            boolean success = teamService.updateTeam(editingTeam);
            if (success) {
                if (onTeamCreated != null)
                    onTeamCreated.run();
                dialogStage.close();
            }
            return;
        }

        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null)
            return;

        Team team = new Team(name, description, color, currentUser.getId());

        String teamId = teamService.createTeam(team);
        if (teamId != null) {
            // Add selected members
            for (User user : selectedUsers) {
                teamService.addMember(teamId, user.getId(), "MEMBER");
            }

            if (onTeamCreated != null) {
                onTeamCreated.run();
            }
            dialogStage.close();
        }
    }
}
