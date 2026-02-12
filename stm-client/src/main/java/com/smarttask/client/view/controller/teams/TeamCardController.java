package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.model.Team;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Pane;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXMLLoader;

public class TeamCardController {

    @FXML
    private VBox cardRoot;

    @FXML
    private Label nameLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Rectangle colorRect;

    @FXML
    private Button btnViewMembers;

    @FXML
    private Button btnDeleteTeam;

	@FXML
    private Button btnEditTeam;

    private Team team;
    private User currentUser;
    private final TeamService teamService = new TeamService();

    public void setTeam(Team team) {
        this.team = team;
        updateUI();
    }

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        updateUI();
    }

    private void updateUI() {
        if (team == null) return;

		nameLabel.setText(team.getName());
		descriptionLabel.setText(team.getDescription());
		colorRect.setFill(Color.web(team.getColor() != null ? team.getColor() : "#3788d8"));

		boolean isOwner = currentUser != null && currentUser.getId().equals(team.getOwnerId());

		btnDeleteTeam.setVisible(isOwner);
		btnEditTeam.setVisible(isOwner);

		btnViewMembers.setVisible(true);
    }

	@FXML
	private void handleViewTeam() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teams/team-members-dialog.fxml"));
			Stage dialogStage = new Stage();
			dialogStage.initOwner(cardRoot.getScene().getWindow());
			dialogStage.initModality(Modality.APPLICATION_MODAL);

			Parent root = loader.load();
			dialogStage.setScene(new Scene(root));

			TeamMembersDialogController controller = loader.getController();
			controller.setDialogStage(dialogStage);
			controller.setTeam(team);

			dialogStage.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void handleEditTeam() {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teams/edit-team-dialog.fxml"));
			Stage dialogStage = new Stage();
			dialogStage.initOwner(cardRoot.getScene().getWindow());
			dialogStage.initModality(Modality.APPLICATION_MODAL);

			Parent root = loader.load();
			dialogStage.setScene(new Scene(root));

			EditTeamDialogController controller = loader.getController();
			controller.setDialogStage(dialogStage);
			controller.setTeam(team);
			controller.setCurrentUser(currentUser);
			controller.setOnTeamUpdated(v -> {
				updateUI();
			});

			dialogStage.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void handleDeleteTeam() {
		if (team == null || currentUser == null) return;

		if (!currentUser.getId().equals(team.getOwnerId())) return;

		Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
		confirmAlert.setTitle("Supprimer une équipe");
		confirmAlert.setHeaderText("Êtes vous sûre de vouloir supprimer cette équipe?");
		confirmAlert.setContentText("Équipe: " + team.getName());

		confirmAlert.showAndWait().ifPresent(response -> {
			if (response == ButtonType.OK) {
				boolean success = teamService.deleteTeam(team.getId());
				if (success) {
					Pane parent = (Pane) cardRoot.getParent();
					if (parent != null) {
						parent.getChildren().remove(cardRoot);
					}
				} else {
					Alert errorAlert = new Alert(Alert.AlertType.ERROR);
					errorAlert.setTitle("Échec lors de la suppression");
					errorAlert.setHeaderText("Impossible de supprimer l'équipe");
					errorAlert.setContentText("Veuillez réessayer plus tard.");
					errorAlert.showAndWait();
				}
			}
		});
	}
}

