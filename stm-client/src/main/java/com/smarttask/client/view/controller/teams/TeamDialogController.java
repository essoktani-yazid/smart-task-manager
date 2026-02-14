package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.Team;
import com.smarttask.model.TeamMember;
import com.smarttask.model.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TeamDialogController {

    @FXML private Label dialogTitle, dialogSubtitle;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private ColorPicker colorPicker;
    @FXML private ComboBox<User> userSelectionCombo;
    @FXML private FlowPane selectedMembersPane;
    @FXML private Button submitButton;

    private Stage dialogStage;
    private Runnable onSaveSuccess;
    private final TeamService teamService = new TeamService();
    
    // Lists for member management
    private final List<User> selectedUsers = new ArrayList<>();
    private List<User> initialUsers = new ArrayList<>(); // Initial state for comparison
    
    private Team editingTeam; // Null = Creation, Non-null = Edition

    @FXML
    public void initialize() {
        colorPicker.setValue(Color.web("#6a7cff"));
        setupUserCombo();
        loadAllUsers();
    }

    /**
     * Configure the edit mode (called from outside before display)
     */
    public void setTeam(Team team) {
        this.editingTeam = team;
        
        // Adapt the UI
        dialogTitle.setText("Edit team");
        dialogSubtitle.setText("Update information for " + team.getName());
        submitButton.setText("Save changes");

        // Pre-filling
        nameField.setText(team.getName());
        descriptionField.setText(team.getDescription());
        if (team.getColor() != null) {
            try { colorPicker.setValue(Color.valueOf(team.getColor())); } catch (Exception ignored) {}
        }

        loadCurrentMembers(team.getId());
    }

    private void loadCurrentMembers(String teamId) {
        new Thread(() -> {
            List<TeamMember> members = teamService.getTeamMembers(teamId);
            List<User> allUsers = teamService.getAllUsers();
            
            // Filter to get complete User objects of current members
            List<User> currentUsers = allUsers.stream()
                    .filter(u -> members.stream().anyMatch(m -> m.getUserId().equals(u.getId())))
                    .collect(Collectors.toList());

            Platform.runLater(() -> {
                initialUsers = new ArrayList<>(currentUsers);
                currentUsers.forEach(this::addMemberToUI);
            });
        }).start();
    }

    private void loadAllUsers() {
        new Thread(() -> {
            List<User> allUsers = teamService.getAllUsers();
            Platform.runLater(() -> userSelectionCombo.getItems().setAll(allUsers));
        }).start();
    }

    @FXML
    public void handleAddMember() {
        User selected = userSelectionCombo.getSelectionModel().getSelectedItem();
        if (selected != null && selectedUsers.stream().noneMatch(u -> u.getId().equals(selected.getId()))) {
            addMemberToUI(selected);
            userSelectionCombo.getSelectionModel().clearSelection();
        }
    }

    private void addMemberToUI(User user) {
        selectedUsers.add(user);
        
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

    @FXML
    public void handleSubmit() {
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) return;

        if (editingTeam != null) {
            updateExistingTeam();
        } else {
            createNewTeam();
        }
    }

    private void updateExistingTeam() {
        editingTeam.setName(nameField.getText());
        editingTeam.setDescription(descriptionField.getText());
        editingTeam.setColor(colorPicker.getValue().toString());

        new Thread(() -> {
            // 1. Update basic information
            boolean success = teamService.updateTeam(editingTeam);
            if (success) {
                // 2. Synchronize members (additions/removals)
                syncMembers(editingTeam.getId());
                Platform.runLater(this::closeAndNotify);
            }
        }).start();
    }

    private void createNewTeam() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        Team team = new Team(nameField.getText(), descriptionField.getText(), 
                             colorPicker.getValue().toString(), currentUser.getId());

        new Thread(() -> {
            String teamId = teamService.createTeam(team);
            if (teamId != null) {
                // Simple addition of selected members
                for (User user : selectedUsers) {
                    teamService.addMember(teamId, user.getId(), "MEMBER");
                }
                Platform.runLater(this::closeAndNotify);
            }
        }).start();
    }

    private void syncMembers(String teamId) {
        // Determine the deltas
        List<User> toAdd = selectedUsers.stream()
                .filter(u -> initialUsers.stream().noneMatch(i -> i.getId().equals(u.getId())))
                .collect(Collectors.toList());

        List<User> toRemove = initialUsers.stream()
                .filter(u -> selectedUsers.stream().noneMatch(s -> s.getId().equals(u.getId())))
                .collect(Collectors.toList());

        // API calls
        toAdd.forEach(u -> teamService.addMember(teamId, u.getId(), "MEMBER"));
        // toRemove.forEach(u -> teamService.removeMember(teamId, u.getId())); // If implemented
    }

    private void closeAndNotify() {
        if (onSaveSuccess != null) onSaveSuccess.run();
        dialogStage.close();
    }

    private void setupUserCombo() {
        userSelectionCombo.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getUsername());
            }
        });
        userSelectionCombo.setButtonCell(userSelectionCombo.getCellFactory().call(null));
    }

    public void setDialogStage(Stage dialogStage) { this.dialogStage = dialogStage; }
    public void setOnSaveSuccess(Runnable callback) { this.onSaveSuccess = callback; }
    @FXML public void handleCancel() { dialogStage.close(); }
}
