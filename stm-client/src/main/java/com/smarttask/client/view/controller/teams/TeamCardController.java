package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.model.Team;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

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

        // Seul le propriétaire peut supprimer
        btnDeleteTeam.setVisible(currentUser != null && currentUser.getId().equals(team.getOwnerId()));

        // TODO : activer / désactiver ViewMembers selon permissions si nécessaire
        btnViewMembers.setVisible(true);
    }

    @FXML
    private void handleViewMembers() {
        if (team == null) return;

        try {
            // Chargement du dialog des membres
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/fxml/teams/team-members-dialog.fxml")
            );
            HBox root = loader.load();

            TeamMembersController controller = loader.getController();
            controller.setTeam(team);
            controller.setCurrentUser(currentUser);

            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Members of " + team.getName());
            dialog.setScene(new Scene(root));
            dialog.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteTeam() {
        if (team == null || currentUser == null) return;

        boolean confirmed = ConfirmDialog.show(
                "Supprimer l'équipe",
                "Voulez-vous vraiment supprimer l'équipe " + team.getName() + " ?"
        );

        if (!confirmed) return;

        boolean success = teamService.deleteTeam(team.getId());
        if (success) {
            System.out.println("Team deleted: " + team.getName());
            cardRoot.getChildren().clear(); // retire la carte du FlowPane
        } else {
            System.err.println("Impossible de supprimer la team");
        }
    }
}

