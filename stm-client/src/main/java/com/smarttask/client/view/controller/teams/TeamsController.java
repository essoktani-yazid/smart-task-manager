package com.smarttask.client.view.controller.teams;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class TeamsController {

	@FXML
	private FlowPane teamsFlow;

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

			dialogStage.showAndWait();

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

