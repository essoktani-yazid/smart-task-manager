package com.smarttask.client.view.controller.projects;

import com.smarttask.client.service.ProjectService;
import com.smarttask.client.service.TaskService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.Project;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

import java.util.List;

public class ProjectsController {
	@FXML private FlowPane projectsFlow;

	private final ProjectService projectService = new ProjectService();
	private final TaskService taskService = new TaskService();

	@FXML
	private void initialize() {
		loadProjects();
	}

	private void loadProjects() {
		User current = SessionManager.getInstance().getCurrentUser();
		if (current == null) return;

		List<Project> projects = projectService.getProjectsByUser(current.getId());
		projectsFlow.getChildren().clear();
		if (projects == null) return;

		for (Project p : projects) {
			VBox card = new VBox();
			card.getStyleClass().add("project-card");
			Label name = new Label(p.getName());
			name.getStyleClass().add("project-name");
			Label desc = new Label(p.getDescription() != null ? p.getDescription() : "");
			HBox actions = new HBox(8);
			Button edit = new Button("Edit");
			Button del = new Button("Delete");
			Button tasks = new Button("Tasks");

			edit.setOnAction(e -> showEditDialog(p));
			del.setOnAction(e -> {
				boolean ok = projectService.deleteProject(p.getId());
				if (ok) loadProjects();
			});

			tasks.setOnAction(e -> showTasksForProject(p));

			actions.getChildren().addAll(edit, del, tasks);
			card.getChildren().addAll(name, desc, actions);
			projectsFlow.getChildren().add(card);
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
			controller.setProject(new Project());

			Stage stage = new Stage();
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("Create Project");
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
			controller.setProject(p);

			Stage stage = new Stage();
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("Edit Project");
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
		User current = SessionManager.getInstance().getCurrentUser();
		if (current == null) return;
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/projects/task-form.fxml"));
			Parent root = loader.load();
			TaskFormController controller = loader.getController();

			// create a simple list view dialog where user can create a task for the project
			Stage stage = new Stage();
			stage.initModality(Modality.APPLICATION_MODAL);
			stage.setTitle("Tasks for " + project.getName());
			stage.setScene(new javafx.scene.Scene(root));
			controller.setTask(null);
			controller.setStage(stage);
			stage.showAndWait();

			Task created = controller.getTask();
			if (created != null) {
				created.setUser(current);
				created.setProjectId(project.getId());
				Task sent = taskService.createTask(created);
				if (sent == null) {
					// optionally show alert to user
				}
				// reload tasks display by re-opening dialog or updating UI (simplified)
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}

