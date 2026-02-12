package com.smarttask.client.view.controller.teams;

import com.smarttask.client.service.TeamService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.Team;
import com.smarttask.model.TeamMember;
import com.smarttask.model.User;
import com.smarttask.client.view.model.TeamDisplayWrapper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamsController {

    @FXML
    private FlowPane teamsFlow;

	@FXML
	private ScrollPane cardsScroll;

	@FXML
	private TableView<TeamDisplayWrapper> teamsTable;

	@FXML
	private TableColumn<TeamDisplayWrapper, String> nameColumn;

	@FXML
	private TableColumn<TeamDisplayWrapper, String> descColumn;

	@FXML
	private TableColumn<TeamDisplayWrapper, String> membersColumn;

	@FXML
	private TableColumn<TeamDisplayWrapper, String> dateColumn;

	@FXML
	private TableColumn<TeamDisplayWrapper, Void> actionsColumn;

	@FXML
	private ToggleGroup viewToggleGroup;

	private final TeamService teamService = new TeamService();
	private final ObservableList<TeamDisplayWrapper> tableData = FXCollections.observableArrayList();

	@FXML
	public void initialize() {
		setupTableColumns();
		loadTeams();
	}

	private void setupTableColumns() {
		nameColumn.setCellValueFactory(cellData -> cellData.getValue().nameProperty());
		descColumn.setCellValueFactory(cellData -> cellData.getValue().descriptionProperty());
		membersColumn.setCellValueFactory(cellData -> cellData.getValue().membersProperty());
		membersColumn.setCellFactory(column -> new TableCell<TeamDisplayWrapper, String>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || getTableRow() == null || getTableRow().getItem() == null) {
					setGraphic(null);
					setText(null);
				} else {
					TeamDisplayWrapper wrapper = getTableRow().getItem();
					if (wrapper.getMembersList().isEmpty()) {
						Label emptyLabel = new Label("No members yet");
						emptyLabel.getStyleClass().add("empty-member-label");
						setGraphic(emptyLabel);
					} else {
						FlowPane pane = new FlowPane(5, 5);
						pane.getStyleClass().add("members-flow-pane");

						for (User user : wrapper.getMembersList()) {
							Label chip = new Label(user.getUsername());
							chip.getStyleClass().add("table-member-chip");

							Tooltip tooltip = new Tooltip(user.getUsername() + " (" + user.getEmail() + ")");
							tooltip.setShowDelay(javafx.util.Duration.millis(200));
							chip.setTooltip(tooltip);

							pane.getChildren().add(chip);
						}
						setGraphic(pane);
					}
					setText(null);
				}
			}
		});
		dateColumn.setCellValueFactory(cellData -> cellData.getValue().createdAtProperty());

		setupActionsColumn();

		teamsTable.setItems(tableData);
	}

	private void setupActionsColumn() {
		actionsColumn.setCellFactory(param -> new TableCell<>() {
			private final Button editBtn = new Button("✎");
			private final Button deleteBtn = new Button("✕");
			private final HBox container = new HBox(12, editBtn, deleteBtn);

			{
				editBtn.getStyleClass().add("action-button-edit");
				deleteBtn.getStyleClass().add("action-button-delete");
				container.setAlignment(javafx.geometry.Pos.CENTER);

				editBtn.setOnAction(event -> {
					TeamDisplayWrapper wrapper = getTableView().getItems().get(getIndex());
					handleEditTeam(wrapper.getTeam());
				});

				deleteBtn.setOnAction(event -> {
					TeamDisplayWrapper wrapper = getTableView().getItems().get(getIndex());
					handleDeleteTeam(wrapper.getTeam());
				});
			}

			@Override
			protected void updateItem(Void item, boolean empty) {
				super.updateItem(item, empty);
				if (empty) {
					setGraphic(null);
				} else {
					setGraphic(container);
				}
			}
		});
	}

	@FXML
	private void handleViewChange() {
		ToggleButton selected = (ToggleButton) viewToggleGroup.getSelectedToggle();
		if (selected != null) {
			boolean isList = selected.getText().equals("List");
			teamsTable.setVisible(isList);
			teamsTable.setManaged(isList);
			cardsScroll.setVisible(!isList);
			cardsScroll.setManaged(!isList);
		}
	}

	private void loadTeams() {
		User currentUser = SessionManager.getInstance().getCurrentUser();
		if (currentUser == null)
			return;

		new Thread(() -> {
			List<Team> teams = teamService.getTeamsByUser(currentUser.getId());
			List<User> allUsers = teamService.getAllUsers();
			Map<String, User> userMap = allUsers.stream()
					.collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

			List<TeamDisplayWrapper> listData = new ArrayList<>();

			Platform.runLater(() -> {
				teamsFlow.getChildren().clear();
				tableData.clear();
			});

			for (Team team : teams) {
				List<TeamMember> members = teamService.getTeamMembers(team.getId());
				List<User> membersList = members.stream()
						.map(m -> userMap.get(m.getUserId()))
						.filter(java.util.Objects::nonNull)
						.collect(Collectors.toList());

				String memberNames = membersList.stream()
						.map(User::getUsername)
						.collect(Collectors.joining(", "));

				int memberCount = members.size();

				listData.add(new TeamDisplayWrapper(team, memberNames, membersList));

				Platform.runLater(() -> addTeamCard(team, memberCount));
			}

			Platform.runLater(() -> tableData.addAll(listData));
		}).start();
	}

	private void addTeamCard(Team team, int memberCount) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teams/team-card.fxml"));
			Node card = loader.load();

			TeamCardController controller = loader.getController();
			controller.setTeamData(team, memberCount);

			teamsFlow.getChildren().add(card);

			// Animation d'entrée
			animateCard(card, teamsFlow.getChildren().size() - 1);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void animateCard(Node node, int index) {
		node.setOpacity(0);
		node.setTranslateY(20);

		javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500),
				node);
		ft.setToValue(1.0);
		ft.setDelay(javafx.util.Duration.millis(index * 100));

		javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(
				javafx.util.Duration.millis(500), node);
		tt.setToY(0);
		tt.setDelay(javafx.util.Duration.millis(index * 100));

		ft.play();
		tt.play();
	}

	@FXML
	public void handleCreateTeam() {
		openTeamDialog(null);
	}

	private void handleEditTeam(Team team) {
		openTeamDialog(team);
	}

	private void handleDeleteTeam(Team team) {
		Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
		alert.setTitle("Delete Team");
		alert.setHeaderText("Delete Team: " + team.getName());
		alert.setContentText("Are you sure you want to delete this team?");

		if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
			new Thread(() -> {
				boolean success = teamService.deactivateTeam(team.getId());
				if (success) {
					Platform.runLater(this::loadTeams);
				}
			}).start();
		}
	}

	private void openTeamDialog(Team team) {
		try {
			java.net.URL fxmlLocation = getClass().getResource("/fxml/teams/create-team-dialog.fxml");
			if (fxmlLocation == null)
				return;

			FXMLLoader loader = new FXMLLoader(fxmlLocation);
			Parent root = loader.load();

			Stage dialogStage = new Stage();
			dialogStage.initModality(Modality.APPLICATION_MODAL);
			dialogStage.setTitle(team == null ? "Create team" : "Edit team");

			CreateTeamDialogController controller = loader.getController();
			if (controller != null) {
				controller.setDialogStage(dialogStage);
				if (team != null) {
					controller.setEditMode(team);
				}
				controller.setOnTeamCreated(this::loadTeams);
			}

            Scene scene = new Scene(root);
            dialogStage.setScene(scene);

			dialogStage.showAndWait();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
