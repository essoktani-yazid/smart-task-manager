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
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TeamsController {

    @FXML private FlowPane teamsFlow;
    @FXML private ScrollPane cardsScroll;
    @FXML private TableView<TeamDisplayWrapper> teamsTable;
    @FXML private TableColumn<TeamDisplayWrapper, String> nameColumn;
    @FXML private TableColumn<TeamDisplayWrapper, String> descColumn;
    @FXML private TableColumn<TeamDisplayWrapper, String> membersColumn;
    @FXML private TableColumn<TeamDisplayWrapper, String> dateColumn;
    @FXML private TableColumn<TeamDisplayWrapper, Void> actionsColumn;
    @FXML private ToggleGroup viewToggleGroup;

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
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().createdAtProperty());

        membersColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    TeamDisplayWrapper wrapper = getTableRow().getItem();
                    if (wrapper.getMembersList().isEmpty()) {
                        Label emptyLabel = new Label("Aucun membre");
                        emptyLabel.getStyleClass().add("empty-member-label");
                        setGraphic(emptyLabel);
                    } else {
                        FlowPane pane = new FlowPane(5, 5);
                        pane.getStyleClass().add("members-flow-pane");
                        for (User user : wrapper.getMembersList()) {
                            Label chip = new Label(user.getUsername());
                            chip.getStyleClass().add("table-member-chip");
                            pane.getChildren().add(chip);
                        }
                        setGraphic(pane);
                    }
                }
            }
        });

		nameColumn.setCellFactory(column -> new TableCell<>() {
			@Override
			protected void updateItem(String item, boolean empty) {
				super.updateItem(item, empty);
				if (empty || getTableRow() == null || getTableRow().getItem() == null) {
					setGraphic(null);
					setText(null);
				} else {
					Team team = getTableRow().getItem().getTeam();
					HBox box = new HBox(10);
					box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

					StackPane iconBadge = new StackPane();
					iconBadge.setPrefSize(24, 24);
					String color = team.getColor().replace("0x", "#");
					iconBadge.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 4;");

					SVGPath icon = new SVGPath();
					icon.setContent("M26.41,18.1a3.28,3.28,0,1,0-3.91,0l-.43.17a8.33,8.33,0,0,0-3.89-2.45,4.41,4.41,0,1,0-4.61,0,8.32,8.32,0,0,0-3.81,2.36l-.24-.1a3.28,3.28,0,1,0-3.93,0A6.06,6.06,0,0,0,1.5,23.8v0a.5.5,0,0,0,.5.5H30a.5.5,0,0,0,.5-.5A6.06,6.06,0,0,0,26.41,18.1ZM22.17,15.5a2.28,2.28,0,1,1,2.28,2.28A2.28,2.28,0,0,1,22.17,15.5Zm2.28,3.28a5,5,0,0,1,5,4.55H24.17a8.26,8.26,0,0,0-1.46-4.23A4.89,4.89,0,0,1,24.45,18.77Zm-12-6.68a3.41,3.41,0,1,1,3.41,3.41A3.41,3.41,0,0,1,12.46,12.09ZM7.55,13.22A2.28,2.28,0,1,1,5.28,15.5,2.28,2.28,0,0,1,7.55,13.22Zm0,5.55A5,5,0,0,1,9.09,19a8.26,8.26,0,0,0-1.52,4.31h-5A5,5,0,0,1,7.55,18.77Zm5.55,4.55H8.57a7.31,7.31,0,0,1,14.59,0H13.1Z");
					icon.setFill(Color.WHITE);
					icon.setScaleX(0.6); icon.setScaleY(0.6);
					
					iconBadge.getChildren().add(icon);
					Label nameLabel = new Label(team.getName());
					nameLabel.setStyle("-fx-font-weight: bold;");
					
					box.getChildren().addAll(iconBadge, nameLabel);
					setGraphic(box);
				}
			}
		});

        setupActionsColumn();
        teamsTable.setItems(tableData);
    }

    private void setupActionsColumn() {
		actionsColumn.setCellFactory(param -> new TableCell<>() {
			private final String EDIT_PATH = "M3 17.25V21h3.75L17.81 9.94l-3.75-3.75L3 17.25zM20.71 7.04c.39-.39.39-1.02 0-1.41l-2.34-2.34c-.39-.39-1.02-.39-1.41 0l-1.83 1.83 3.75 3.75 1.83-1.83z";
			private final String DELETE_PATH = "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z";

			private final Button editBtn = new Button();
			private final Button deleteBtn = new Button();
			private final HBox container = new HBox(12, editBtn, deleteBtn);

			{
				SVGPath editIcon = new SVGPath();
				editIcon.setContent(EDIT_PATH);
				editIcon.setFill(Color.web("#4a90e2"));
				editBtn.setGraphic(editIcon);
				editBtn.getStyleClass().add("action-button-edit");
				Tooltip.install(editBtn, new Tooltip("Modifier l'équipe"));

				SVGPath deleteIcon = new SVGPath();
				deleteIcon.setContent(DELETE_PATH);
				deleteIcon.setFill(Color.web("#e74c3c"));
				deleteBtn.setGraphic(deleteIcon);
				deleteBtn.getStyleClass().add("action-button-delete");
				Tooltip.install(deleteBtn, new Tooltip("Supprimer l'équipe"));

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
        if (currentUser == null) return;

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

                listData.add(new TeamDisplayWrapper(team, memberNames, membersList));
                Platform.runLater(() -> addTeamCard(team, members.size()));
            }
            Platform.runLater(() -> tableData.addAll(listData));
        }).start();
    }

    private void addTeamCard(Team team, int memberCount) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teams/team-card.fxml"));
            Node card = loader.load();
            ((TeamCardController) loader.getController()).setTeamData(team, memberCount, this::handleEditTeam);
            teamsFlow.getChildren().add(card);
            animateCard(card, teamsFlow.getChildren().size() - 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void animateCard(Node node, int index) {
        node.setOpacity(0);
        node.setTranslateY(20);
        javafx.animation.FadeTransition ft = new javafx.animation.FadeTransition(javafx.util.Duration.millis(500), node);
        ft.setToValue(1.0);
        ft.setDelay(javafx.util.Duration.millis(index * 50));
        javafx.animation.TranslateTransition tt = new javafx.animation.TranslateTransition(javafx.util.Duration.millis(500), node);
        tt.setToY(0);
        tt.setDelay(javafx.util.Duration.millis(index * 50));
        ft.play(); tt.play();
    }

    @FXML
    public void handleCreateTeam() {
        openTeamDialog(null);
    }

    private void handleEditTeam(Team team) {
        openTeamDialog(team);
    }

    private void handleDeleteTeam(Team team) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous vraiment supprimer l'équipe " + team.getName() + " ?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Suppression d'équipe");
        if (alert.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            new Thread(() -> {
                if (teamService.deactivateTeam(team.getId())) {
                    Platform.runLater(this::loadTeams);
                }
            }).start();
        }
    }

    /**
     * Ouvre la boîte de dialogue générique pour la création ou l'édition
     */
    private void openTeamDialog(Team team) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/teams/team-dialog.fxml"));
            Parent root = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.initModality(Modality.APPLICATION_MODAL);
            dialogStage.setTitle(team == null ? "Créer une équipe" : "Modifier l'équipe");

            // Récupération du contrôleur mis à jour
            TeamDialogController controller = loader.getController();
            controller.setDialogStage(dialogStage);
            controller.setOnSaveSuccess(this::loadTeams); // Rafraîchir la liste après succès

            if (team != null) {
                controller.setTeam(team); // Active le mode édition
            }

            dialogStage.setScene(new Scene(root));
            dialogStage.showAndWait();
            
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur de chargement", "Impossible d'ouvrir le formulaire d'équipe.");
        }
    }

    private void showError(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
