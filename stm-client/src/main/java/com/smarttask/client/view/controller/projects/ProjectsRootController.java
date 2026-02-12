package com.smarttask.client.view.controller.projects;

import com.smarttask.client.service.ProjectService;
import com.smarttask.client.service.TaskService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.client.view.controller.MainLayoutController;
import com.smarttask.model.Project;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import java.util.List;

public class ProjectsRootController {
	@FXML private VBox projectsContainer;

	private final ProjectService projectService = new ProjectService();

	@FXML
	private void initialize() {
		loadProjects();
	}

	private void loadProjects() {
		User current = SessionManager.getInstance().getCurrentUser();
		if (current == null) return;

		List<Project> projects = projectService.getProjectsByUser(current.getId());
		projectsContainer.getChildren().clear();
		if (projects == null) return;

		projectsContainer.setSpacing(15);

		for (Project p : projects) {
			HBox card = new HBox(20);
			card.getStyleClass().add("project-card-row");
			card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

			StackPane iconContainer = new StackPane();
			iconContainer.getStyleClass().add("project-icon-container");

			String pColor = (p.getColor() != null && !p.getColor().isEmpty()) ? p.getColor() : "#4f46e5";

			iconContainer.setStyle(
				"-fx-background-color: " + pColor + "26;" + 
				"-fx-border-color: " + pColor + ";" +
				"-fx-background-radius: 10; -fx-border-radius: 10; -fx-border-width: 1.5;"
			);
			iconContainer.setPrefSize(54, 54);
			iconContainer.setMinSize(54, 54);

			javafx.scene.shape.SVGPath svgIcon = new javafx.scene.shape.SVGPath();
			svgIcon.setContent("M71.6784 84.1883H15.0422C10.0597 84.1883 6.02056 79.4034 6.02056 73.5008L6.02056 22.6875C6.02056 16.785 10.0597 12 15.0422 12H39.8706C42.5572 12 45.104 13.4185 46.818 15.8694L49.559 19.7891C51.2182 22.1618 53.9176 23.5952 56.8127 23.616C57.6296 23.6219 58.4436 23.6258 59.149 23.6258H82.3148M15.0273 84.1883H73.604C77.8306 84.1883 81.4906 80.7123 82.4098 75.8251L89.7804 36.6375C91.036 29.9615 86.7483 23.6257 80.9746 23.6257H25.3316C21.6506 23.6257 18.3387 26.275 16.9633 30.3199L14.2906 38.1801L6.36349 70.5206C4.68758 77.3578 9.01734 84.1883 15.0273 84.1883Z");

			svgIcon.setStyle("-fx-fill: transparent; -fx-stroke: " + pColor + "; -fx-stroke-width: 3;");

			svgIcon.setScaleX(0.35);
			svgIcon.setScaleY(0.35);

			iconContainer.getChildren().add(svgIcon);

			VBox vInfo = new VBox(5);
			Label lblName = new Label(p.getName());
			lblName.getStyleClass().add("project-title-label");

			Label lblDesc = new Label(p.getDescription() != null ? p.getDescription() : "Aucune description");
			lblDesc.getStyleClass().add("project-desc-label");
			lblDesc.setWrapText(true);
			lblDesc.setMaxWidth(400);

			vInfo.getChildren().addAll(lblName, lblDesc);


			Region spacer = new Region();
			HBox.setHgrow(spacer, Priority.ALWAYS);

			HBox actions = new HBox(10);
			actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

			Button btnEdit = new Button("Modifier");
			btnEdit.getStyleClass().add("action-btn-secondary");
			btnEdit.setOnAction(e -> { e.consume(); showEditDialog(p); });

			Button btnDelete = new Button("Supprimer");
			btnDelete.getStyleClass().add("danger-button");
			btnDelete.setOnAction(e -> { 
				e.consume(); 
				boolean ok = projectService.deleteProject(p.getId());
				if (ok) loadProjects();
			});

			Button btnOpen = new Button("Voir");
			btnOpen.getStyleClass().add("primary-action-button");
			btnOpen.setOnAction(e -> showTasksForProject(p));

			actions.getChildren().addAll(btnEdit, btnDelete, btnOpen);

			card.getChildren().addAll(iconContainer, vInfo, spacer, actions);

			card.setOnMouseClicked(e -> showTasksForProject(p));

			projectsContainer.getChildren().add(card);
		}
	}

	@FXML
	private void handleCreateProject() {
		User current = SessionManager.getInstance().getCurrentUser();
		if (current == null) return;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/projects/project-form.fxml"));
			Parent root = loader.load();
			ProjectFormController controller = loader.getController();
			controller.setMode(ProjectFormController.Mode.CREATE);
			controller.setProject(null);

			Stage stage = new Stage();
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("Nouveau Projet");
			stage.setScene(new javafx.scene.Scene(root));
			controller.setStage(stage);
			stage.showAndWait();

			Project result = controller.getProject();
			if (result != null) {
				result.setUserId(current.getId());
				Project created = projectService.createProject(result);
				if (created != null) loadProjects();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void showEditDialog(Project p) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/projects/project-form.fxml"));
			Parent root = loader.load();
			ProjectFormController controller = loader.getController();
			
			controller.setMode(ProjectFormController.Mode.EDIT);
			controller.setProject(p);

			Stage stage = new Stage();
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("Modifier Projet");
			stage.setScene(new javafx.scene.Scene(root));
			controller.setStage(stage);
			stage.showAndWait();

			Project result = controller.getProject();
			if (result != null) {
				result.setId(p.getId());
				projectService.updateProject(result);
				loadProjects();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void showTasksForProject(Project project) {
		MainLayoutController.getInstance().navigateTo("/fxml/projects/project-view.fxml", project);
	}
}


