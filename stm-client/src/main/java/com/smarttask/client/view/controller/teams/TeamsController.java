package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.model.Team;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class TeamsController {

    @FXML
    private FlowPane teamsFlow;

    private User currentUser;
    private final TeamService teamService = new TeamService();

    public void setCurrentUser(User currentUser) {
        this.currentUser = currentUser;
        loadTeams();
    }

    @FXML
    private void initialize() {
        System.out.println("teams view loaded");
    }

    @FXML
    private void handleCreateTeam() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/fxml/teams/create-team-dialog.fxml")
            );

            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle("Create team");

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

            CreateTeamDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setCurrentUser(currentUser); // injection de l'utilisateur courant
            controller.setOnTeamCreated(this::loadTeams); // callback pour rafraîchir la vue

            dialogStage.showAndWait();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Charge toutes les équipes de l'utilisateur courant et les affiche dans le FlowPane.
     */
    private void loadTeams() {
        if (currentUser == null) return;

        teamsFlow.getChildren().clear();

        List<Team> teams = teamService.getMyTeams(currentUser.getId());
        for (Team team : teams) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teams/team-card.fxml"));
                Parent card = loader.load();

                TeamCardController cardController = loader.getController();
                cardController.setTeam(team);
                cardController.setCurrentUser(currentUser);

                teamsFlow.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

