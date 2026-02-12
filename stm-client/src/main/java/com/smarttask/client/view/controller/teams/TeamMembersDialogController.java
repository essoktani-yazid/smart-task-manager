package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.Team;
import com.smarttask.model.TeamMember;
import com.smarttask.model.User;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.List;

public class TeamMembersDialogController {

    @FXML private TableView<TeamMember> membersTable;
    @FXML private TableColumn<TeamMember, String> colName;
    @FXML private TableColumn<TeamMember, String> colEmail;
    @FXML private TableColumn<TeamMember, Void> colAction;
	@FXML private TextField usernameField;

    private Team team;
    private Stage dialogStage;
    private final TeamService teamService = new TeamService();

    public void setDialogStage(Stage stage) {
        this.dialogStage = stage;
    }

    public void setTeam(Team team) {
        this.team = team;
        loadMembers();
    }

    private void loadMembers() {
        if (team == null) return;

        User currentUser = SessionManager.getInstance().getCurrentUser();

        boolean isOwner = currentUser != null && currentUser.getId().equals(team.getOwnerId());

        usernameField.setDisable(!isOwner);
        usernameField.setPromptText(isOwner ? "username..." : "Seul les admins peuvent ajouter des membres");

        List<TeamMember> members = teamService.getMembers(team.getId());
        membersTable.setItems(FXCollections.observableArrayList(members));

        colName.setCellValueFactory(data -> new ReadOnlyStringWrapper(
            data.getValue().getUser() != null ? data.getValue().getUser().getUsername() : "Unknown"));
        colEmail.setCellValueFactory(data -> new ReadOnlyStringWrapper(
            data.getValue().getUser() != null ? data.getValue().getUser().getEmail() : "-"));

        colAction.setCellFactory(param -> new TableCell<>() {
            private final Button removeBtn = new Button("Remove");
            {
                removeBtn.setStyle("-fx-background-color:#FF6B6B; -fx-text-fill:white; -fx-background-radius:6;");
                removeBtn.setOnAction(event -> {
                    TeamMember member = getTableView().getItems().get(getIndex());
                    if (teamService.removeMember(team.getId(), member.getUserId())) {
                        getTableView().getItems().remove(member);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    TeamMember member = getTableView().getItems().get(getIndex());

                    boolean isRowOwner = member.getUserId().equals(team.getOwnerId());
                    
                    if (isOwner && !isRowOwner) {
                        setGraphic(removeBtn);
                    } else {
                        setGraphic(null); 
                    }
                }
            }
        });
    }

	@FXML
	private void handleAddMember() {
		String username = usernameField.getText();

		if (username == null || username.isBlank()) {
			showAlert("Error", "Username cannot be empty.");
			return;
		}

		try {
			boolean success = teamService.addMemberByUsername(team.getId(), username);

			if (success) {
				usernameField.clear();
				loadMembers();
			} else {
				showAlert("Error", "User not found.");
			}

		} catch (Exception e) {
			e.printStackTrace();
			showAlert("Error", "Failed to add member.");
		}
	}

	private void showAlert(String title, String message) {
		Alert alert = new Alert(Alert.AlertType.ERROR);
		alert.setTitle(title);
		alert.setHeaderText(null);
		alert.setContentText(message);
		alert.showAndWait();
	}

    @FXML
    private void handleClose() {
        dialogStage.close();
    }
}

