package com.smarttask.client.view.controller.projects;

import java.awt.Desktop;

import com.smarttask.client.service.ProjectService;
import com.smarttask.client.service.TaskService;
import com.smarttask.client.service.TaskTagService;
import com.smarttask.client.service.CommentService;
import com.smarttask.client.service.TimeTrackingService;
import com.smarttask.client.service.CommentAttachmentService;
import com.smarttask.client.service.TaskDependencyService;
import com.smarttask.client.service.SharedTaskService;

import com.smarttask.client.view.controller.MainLayoutController;
import com.smarttask.model.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;

public class ProjectViewController {

    // --- TABLEAU ARBORESCENT (Tâches & Sous-tâches) ---
    @FXML
    private TreeTableView<Task> tasksTreeTable;
    @FXML
    private TreeTableColumn<Task, Task> colTitle;
    @FXML
    private TreeTableColumn<Task, String> colStatus;
    @FXML
    private TreeTableColumn<Task, String> colPriority;
    @FXML
    private TreeTableColumn<Task, String> colDueDate;
    @FXML
    private TreeTableColumn<Task, String> colAssignee;
    @FXML
    private TreeTableColumn<Task, Void> colActions;

    @FXML
    private TextField searchField;

    // --- DÉTAILS ---
    @FXML
    private ListView<TaskTag> tagsList;
    @FXML
    private ListView<Comment> commentsList;
    @FXML
    private TextField commentField;
    // @FXML private Label timeTrackingLabel; // Removed from FXML
    @FXML
    private Button addSubtaskBtn;

    // --- SERVICES ---
    private final TaskService taskService = new TaskService();
    private final ProjectService projectService = new ProjectService();
    private final TaskTagService tagService = new TaskTagService();
    private final CommentService commentService = new CommentService();
    private final TimeTrackingService timeTrackingService = new TimeTrackingService();
    private final CommentAttachmentService attachmentService = new CommentAttachmentService();
    private final TaskDependencyService dependencyService = new TaskDependencyService();
    private final SharedTaskService sharedTaskService = new SharedTaskService();

    // --- SUIVI DE TEMPS ---
    private LocalDateTime timeTrackingStart = null;

    // --- DONNÉES ---
    private Project project = null;
    private ObservableList<Task> tasks = FXCollections.observableArrayList();
    private ObservableList<TaskTag> tags = FXCollections.observableArrayList();
    private ObservableList<TaskDependency> taskDependencies = FXCollections.observableArrayList();
    private ObservableList<Comment> comments = FXCollections.observableArrayList();
    private java.util.Map<String, Integer> dependencyCounts = new java.util.HashMap<>();
    private java.util.Map<String, List<CommentAttachment>> commentAttachments = new java.util.HashMap<>();

    private User currentUser;
    private Task selectedTask = null;
    private java.io.File pendingAttachment = null;

    public void setProject(Project p) {
        project = p;
        if (project != null) {
            loadTasks(project);
            refreshTeamsList();
            System.out.println("[DEBUG] Données chargées pour le projet: " + project.getName());
        }
    }

    @FXML
    private TableView<Team> teamsTable;
    @FXML
    private TableColumn<Team, String> colTeamName;
    @FXML
    private TableColumn<Team, String> colTeamDescription;

    @FXML
    private Button attachBtn;

    @FXML
    public void initialize() {

        setupMainTreeTable();
        // setupSubTasksTable(); // Removed
        setupTeamsTable();
        setupDetailLists();

        tasksTreeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null && newSel.getValue() != null) {
                selectedTask = newSel.getValue();
                loadTags(selectedTask);
                loadComments(selectedTask);
                loadDependencies(selectedTask);
            }
        });

        // Search functionality
        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterTasks(newValue);
        });

        if (project != null) {
            loadTasks(project);
            refreshTeamsList();
        }
    }

    public void loadInitialData() {
        if (project != null) {
            loadTasks(project);
            refreshTeamsList();
        }
    }

    private void refreshTeamsList() {
        if (project == null)
            return;
        try {
            List<Team> teams = projectService.getTeamsByProject(project.getId());
            if (teams != null) {
                teamsTable.setItems(FXCollections.observableArrayList(teams));
            }
        } catch (Exception e) {
            System.err.println("[ERROR] Erreur lors du chargement des équipes: " + e.getMessage());
        }
    }

    private void setupTeamsTable() {
        colTeamName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colTeamDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
    }

    @FXML
    private void onAddTeam() {
        String teamId = showTeamSelectionDialog();
        if (teamId != null) {
            boolean success = projectService.addTeamToProject(project.getId(), teamId);
            if (success)
                refreshTeamsList();
        }
    }

    @FXML
    private void onRemoveTeam() {
        Team selected = teamsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean confirm = showConfirmation(
                    "Do you want to remove team " + selected.getName() + " from this project?");
            if (confirm) {
                boolean success = projectService.removeTeamFromProject(project.getId(), selected.getId());
                if (success)
                    refreshTeamsList();
            }
        }
    }

    private String showTeamSelectionDialog() {
        com.smarttask.client.service.TeamService teamService = new com.smarttask.client.service.TeamService();
        List<Team> userTeams = teamService.getMyTeams(currentUser.getId());

        if (userTeams == null || userTeams.isEmpty()) {
            showAlert("Information", "You are not part of any team.");
            return null;
        }

        ChoiceDialog<Team> dialog = new ChoiceDialog<>(userTeams.get(0), userTeams);
        dialog.setTitle("Add Team");
        dialog.setHeaderText("Select team to add to project " + project.getName());
        dialog.setContentText("Team:");

        javafx.util.StringConverter<Team> converter = new javafx.util.StringConverter<Team>() {
            @Override
            public String toString(Team team) {
                return (team == null) ? "" : team.getName();
            }

            @Override
            public Team fromString(String string) {
                return null;
            }
        };

        Control listView = (Control) dialog.getDialogPane().lookup(".combo-box");
        if (listView instanceof ComboBox) {
            ((ComboBox<Team>) listView).setConverter(converter);
        }

        Optional<Team> result = dialog.showAndWait();
        return result.map(Team::getId).orElse(null);
    }

    private boolean showConfirmation(String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void setupMainTreeTable() {
        // colTitle.setCellValueFactory(param -> new
        // SimpleStringProperty(param.getValue().getValue().getTitle()));
        colTitle.setCellFactory(col -> new TreeTableCell<Task, Task>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.getId() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label titleLabel = new Label(item.getTitle());
                    box.getChildren().add(titleLabel);

                    int count = dependencyCounts.getOrDefault(item.getId(), 0);
                    if (count > 0) {
                        Label badge = new Label(String.valueOf(count));
                        badge.getStyleClass().add("dependency-badge");
                        badge.setStyle("-fx-background-color: #d97706; -fx-text-fill: white; " +
                                "-fx-padding: 1 6; -fx-background-radius: 10; -fx-font-size: 10; -fx-cursor: hand;");

                        badge.setOnMouseClicked(e -> {
                            // Select this task and show dependencies popup
                            tasksTreeTable.getSelectionModel().select(getTreeTableRow().getIndex());
                            showDependenciesPopup(item);
                        });

                        box.getChildren().add(badge);
                    }

                    setGraphic(box);
                    setText(null);
                }
            }
        });
        colTitle.setCellValueFactory(
                param -> new javafx.beans.property.SimpleObjectProperty<Task>(param.getValue().getValue()));
        colPriority.setCellValueFactory(param -> new SimpleStringProperty(
                param.getValue().getValue().getPriority() != null ? param.getValue().getValue().getPriority().name()
                        : ""));
        colDueDate.setCellValueFactory(param -> new SimpleStringProperty(
                param.getValue().getValue().getDueDate() == null ? ""
                        : param.getValue().getValue().getDueDate().toLocalDate().toString()));

        // Assigné à
        colAssignee.setCellValueFactory(param -> {
            if (param.getValue().getValue().getUser() != null)
                return new SimpleStringProperty(param.getValue().getValue().getUser().getUsername());
            return new SimpleStringProperty("-");
        });

        // Statut Modifiable
        colStatus
                .setCellValueFactory(param -> new SimpleStringProperty(param.getValue().getValue().getStatus().name()));
        // Statut (Badge + ContextMenu)
        colStatus.setCellFactory(col -> new TreeTableCell<Task, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("status-badge");
                    if ("TODO".equals(item))
                        badge.getStyleClass().add("status-todo");
                    else if ("IN_PROGRESS".equals(item))
                        badge.getStyleClass().add("status-in-progress");
                    else if ("DONE".equals(item))
                        badge.getStyleClass().add("status-done");

                    // Context Menu for changing status
                    badge.setOnMouseClicked(e -> {
                        ContextMenu menu = new ContextMenu();
                        for (Status s : Status.values()) {
                            MenuItem mi = new MenuItem(s.name());
                            mi.setOnAction(ev -> {
                                Task t = getTreeTableRow().getItem();
                                if (t != null) {
                                    t.setStatus(s);
                                    updateTaskStatus(t);
                                }
                            });
                            menu.getItems().add(mi);
                        }
                        menu.show(badge, e.getScreenX(), e.getScreenY());
                    });

                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Priority (Badge)
        colPriority.setCellFactory(col -> new TreeTableCell<Task, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Label badge = new Label(item);
                    badge.getStyleClass().add("priority-badge");
                    if ("HIGH".equals(item))
                        badge.getStyleClass().add("priority-high");
                    else if ("MEDIUM".equals(item))
                        badge.getStyleClass().add("priority-medium");
                    else if ("LOW".equals(item))
                        badge.getStyleClass().add("priority-low");

                    setGraphic(badge);
                    setText(null);
                }
            }
        });

        // Actions Column (Custom Buttons)
        colActions.setCellFactory(col -> new TreeTableCell<Task, Void>() {
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Task task = getTreeTableRow().getItem();
                    if (task == null) {
                        setGraphic(null);
                        return;
                    }

                    HBox actionsBox = new HBox(5);
                    actionsBox.setAlignment(javafx.geometry.Pos.CENTER);

                    // Style helper for buttons
                    String baseStyle = "-fx-background-color: transparent; -fx-padding: 2 5; -fx-cursor: hand; -fx-font-size: 14px;";

                    // Edit Button (Common)
                    Button editBtn = new Button("✎");
                    editBtn.setTooltip(new Tooltip("Edit"));
                    editBtn.setStyle(baseStyle + "-fx-text-fill: #2563eb;"); // Blue
                    editBtn.setOnAction(e -> handleEditTask(task));

                    // Delete Button (Common)
                    Button deleteBtn = new Button("🗑");
                    deleteBtn.setTooltip(new Tooltip("Delete"));
                    deleteBtn.setStyle(baseStyle + "-fx-text-fill: #dc2626;"); // Red
                    deleteBtn.setOnAction(e -> {
                        // Ensure we delete the correct object
                        handleDeleteTask(task);
                    });

                    if (task.getParentTaskId() == null || task.getParentTaskId().isEmpty()) {
                        // --- MAIN TASK ACTIONS ---

                        // Add Subtask
                        Button addSubBtn = new Button("+");
                        addSubBtn.setTooltip(new Tooltip("Add subtask"));
                        addSubBtn.setStyle(baseStyle + "-fx-text-fill: #059669; -fx-font-weight: bold;"); // Green
                        addSubBtn.setOnAction(e -> {
                            tasksTreeTable.getSelectionModel().select(getTreeTableRow().getIndex());
                            onAddSubtask();
                        });

                        // Add Dependency
                        Button addDepBtn = new Button("🔗");
                        addDepBtn.setTooltip(new Tooltip("Add dependency"));
                        addDepBtn.setStyle(baseStyle + "-fx-text-fill: #d97706;"); // Amber
                        addDepBtn.setOnAction(e -> {
                            tasksTreeTable.getSelectionModel().select(getTreeTableRow().getIndex());
                            onAddDependency();
                        });

                        // Share
                        Button shareBtn = new Button("📤");
                        shareBtn.setTooltip(new Tooltip("Share"));
                        shareBtn.setStyle(baseStyle + "-fx-text-fill: #7c3aed;"); // Purple
                        shareBtn.setOnAction(e -> {
                            tasksTreeTable.getSelectionModel().select(getTreeTableRow().getIndex());
                            onShareTask();
                        });

                        actionsBox.getChildren().addAll(editBtn, addSubBtn, addDepBtn, shareBtn, deleteBtn);
                    } else {
                        // --- SUBTASK ACTIONS ---
                        // Only Edit and Delete
                        actionsBox.getChildren().addAll(editBtn, deleteBtn);
                    }

                    setGraphic(actionsBox);
                }
            }
        });
    }

    private void handleEditTask(Task t) {
        if (t != null)
            openTaskForm(t, false);
    }

    private void handleDeleteTask(Task t) {
        if (t != null && confirmAction("Delete task?")) {
            try {
                taskService.deleteTask(t.getId());
                loadTasks(project);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // setupSubTasksTable and subTasksTable related code removed

    private void setupDetailLists() {
        tagsList.setItems(tags);
        tagsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TaskTag item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    HBox chip = new HBox(8);
                    chip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    chip.getStyleClass().add("tag-chip");
                    
                    // Dynamic background color based on tag name
                    String tagName = item.getTagName();
                    String color = getTagColor(tagName);
                    chip.setStyle("-fx-background-color: " + color + "22; -fx-border-color: " + color + "55; -fx-background-radius: 16; -fx-border-radius: 16; -fx-border-width: 1; -fx-padding: 4 4 4 10;");

                    Label titleLbl = new Label(tagName);
                    titleLbl.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 11px;");

                    Button removeBtn = new Button("×");
                    removeBtn.getStyleClass().add("tag-remove-button");
                    removeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: " + color + "; -fx-font-size: 14px; -fx-padding: 0 6; -fx-cursor: hand;");
                    removeBtn.setOnAction(e -> {
                        if (confirmAction("Delete tag '" + item.getTagName() + "'?")) {
                            try {
                                tagService.delete(selectedTask.getId(), item.getTagName());
                                loadTags(selectedTask);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    chip.getChildren().addAll(titleLbl, removeBtn);
                    setGraphic(chip);
                    setText(null);
                    setStyle("-fx-background-color: transparent; -fx-padding: 4;");
                }
            }
        });

        commentsList.setItems(comments);
        commentsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Comment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    VBox container = new VBox(2);
                    container.setMaxWidth(Double.MAX_VALUE);
                    container.setPadding(new Insets(5, 5, 5, 5));

                    HBox header = new HBox(10);
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    String authorName = item.getUser() != null ? item.getUser().getUsername() : "Anonymous";
                    String initial = authorName.length() > 0 ? authorName.substring(0, 1).toUpperCase() : "?";

                    Label avatar = new Label(initial);
                    avatar.getStyleClass().add("user-avatar");
                    String avatarColor = getTagColor(authorName);
                    avatar.setStyle("-fx-background-color: " + avatarColor + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 18; -fx-min-width: 36; -fx-min-height: 36; -fx-alignment: center; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 2);");

                    VBox meta = new VBox(0);
                    Label nameLabel = new Label(authorName);
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b; -fx-font-size: 13px;");

                    Label dateLabel = new Label("");
                    if (item.getCreatedAt() != null) {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM, HH:mm",
                                    java.util.Locale.getDefault());
                            dateLabel.setText(sdf.format(item.getCreatedAt()));
                        } catch (Exception e) {
                            dateLabel.setText(item.getCreatedAt().toString());
                        }
                    }
                    dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8;");

                    meta.getChildren().addAll(nameLabel, dateLabel);
                    header.getChildren().addAll(avatar, meta);

                    Label contentLabel = new Label(item.getContent());
                    contentLabel.setWrapText(true);
                    contentLabel.setMaxWidth(300); // Prevent bubble from being too wide
                    contentLabel.setStyle("-fx-text-fill: #334155; -fx-font-size: 13px;");

                    VBox bubble = new VBox(5, contentLabel);
                    bubble.getStyleClass().add("comment-bubble");
                    bubble.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: 0 12 12 12; -fx-padding: 10 14; -fx-border-color: #e2e8f0; -fx-border-width: 1; -fx-border-radius: 0 12 12 12;");
                    
                    // Indent the bubble relative to the avatar
                    HBox bubblePaddingLayout = new HBox(bubble);
                    bubblePaddingLayout.setPadding(new Insets(0, 0, 0, 46));

                    // Affichage des pièces jointes
                    List<CommentAttachment> atts = commentAttachments.get(item.getId());
                    if (atts != null && !atts.isEmpty()) {
                        VBox attContainer = new VBox(5);
                        attContainer.setPadding(new Insets(5, 0, 0, 0));

                        for (CommentAttachment att : atts) {
                            HBox attRow = new HBox(8);
                            attRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                            attRow.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-padding: 8; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

                            Label icon = new Label("📎");
                            icon.setStyle("-fx-text-fill: #6366f1; -fx-font-size: 14px;");

                            VBox attInfo = new VBox(0);
                            Hyperlink link = new Hyperlink(att.getFileName());
                            link.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #4f46e5; -fx-padding: 0;");
                            link.setOnAction(e -> {
                                try {
                                    File file = new File(att.getFilePath());
                                    if (file.exists()) {
                                        Desktop.getDesktop().open(file);
                                    } else {
                                        showAlert("Error", "The file no longer exists on disk.");
                                    }
                                } catch (Exception ex) {
                                    showAlert("Error", "Cannot open file: " + ex.getMessage());
                                }
                            });
                            
                            Label sizeLbl = new Label(att.getFileType() + " • File");
                            sizeLbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #94a3b8;");

                            attInfo.getChildren().addAll(link, sizeLbl);
                            attRow.getChildren().addAll(icon, attInfo);

                            String type = att.getFileType().toLowerCase();
                            if (type.endsWith("png") || type.endsWith("jpg") || type.endsWith("jpeg") || type.endsWith("gif")) {
                                try {
                                    File file = new File(att.getFilePath());
                                    if (file.exists()) {
                                        Image img = new Image(file.toURI().toString(), 200, 200, true, true);
                                        ImageView iv = new ImageView(img);
                                        iv.setStyle("-fx-cursor: hand; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
                                        iv.setPreserveRatio(true);
                                        iv.setFitWidth(180);
                                        iv.setOnMouseClicked(e -> link.fire());
                                        
                                        VBox previewBox = new VBox(5, iv, attRow);
                                        attContainer.getChildren().add(previewBox);
                                    } else {
                                        attContainer.getChildren().add(attRow);
                                    }
                                } catch (Exception e) {
                                    attContainer.getChildren().add(attRow);
                                }
                            } else {
                                attContainer.getChildren().add(attRow);
                            }
                        }
                        bubble.getChildren().add(attContainer);
                    }

                    container.getChildren().addAll(header, bubblePaddingLayout);

                    setGraphic(container);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                }
            }
        });
    }

    private String getTagColor(String name) {
        if (name == null || name.isEmpty()) return "#64748b";
        int hash = name.hashCode();
        String[] colors = {
            "#ef4444", "#f59e0b", "#10b981", "#3b82f6", "#6366f1", "#8b5cf6", "#ec4899", "#06b6d4"
        };
        return colors[Math.abs(hash) % colors.length];
    }


    // --- LOGIQUE CHARGEMENT ---

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("[DEBUG] ProjectsViewController.setCurrentUser appelée avec: "
                + (user != null ? user.getUsername() : "NULL"));
    }

    private void loadTasks(Project project) {
        if (project == null)
            return;
        try {
            List<Task> allTasks = taskService.getTasksByProject(project.getId());
            if (allTasks == null)
                allTasks = new java.util.ArrayList<>();

            this.tasks.setAll(allTasks); // Update the observable list for other usages

            // Build Tree
            TreeItem<Task> root = new TreeItem<>(new Task()); // Dummy root
            root.setExpanded(true);

            // Separate parents and children
            List<Task> parents = new java.util.ArrayList<>();
            List<Task> children = new java.util.ArrayList<>();

            for (Task t : allTasks) {
                if (t.getParentTaskId() == null || t.getParentTaskId().isEmpty()) {
                    parents.add(t);
                } else {
                    children.add(t);
                }
            }

            for (Task parent : parents) {
                TreeItem<Task> parentItem = new TreeItem<>(parent);
                parentItem.setExpanded(true);

                // Find children for this parent
                for (Task child : children) {
                    if (parent.getId().equals(child.getParentTaskId())) {
                        parentItem.getChildren().add(new TreeItem<>(child));
                    }
                }
                root.getChildren().add(parentItem);
            }

            tasksTreeTable.setRoot(root);
            tasksTreeTable.setShowRoot(false);

            // Fetch and set dependency counts
            refreshDependencyCounts();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Cannot load tasks: " + e.getMessage());
        }
    }

    private void refreshDependencyCounts() {
        if (project == null)
            return;
        new Thread(() -> {
            try {
                List<TaskDependency> allDeps = dependencyService.getProjectDependencies(project.getId());
                java.util.Map<String, Integer> counts = new java.util.HashMap<>();
                for (TaskDependency d : allDeps) {
                    if (d.getSuccessor() != null) {
                        String succId = d.getSuccessor().getId();
                        counts.put(succId, counts.getOrDefault(succId, 0) + 1);
                    }
                }
                Platform.runLater(() -> {
                    dependencyCounts.clear();
                    dependencyCounts.putAll(counts);
                    tasksTreeTable.refresh();
                });
            } catch (Exception e) {
                System.err.println("Warning: Error refreshing dependency counts: " + e.getMessage());
            }
        }).start();
    }

    // Filter tasks based on search
    private void filterTasks(String query) {
        if (project == null)
            return;
        if (query == null || query.isEmpty()) {
            loadTasks(project);
            return;
        }

        // Simple reload and filter (could be optimized)
        try {
            List<Task> allTasks = taskService.getTasksByProject(project.getId());
            TreeItem<Task> root = new TreeItem<>(new Task());
            root.setExpanded(true);

            String lowerQuery = query.toLowerCase();

            for (Task t : allTasks) {
                // Only modify logic to show if matches or if child matches
                // For simplicity, just showing matching top level tasks and their children
                if (t.getParentTaskId() == null && t.getTitle().toLowerCase().contains(lowerQuery)) {
                    TreeItem<Task> item = new TreeItem<>(t);
                    // Add children regardless of match if parent matches? Or only matching
                    // children?
                    // Let's standard load children
                    for (Task child : allTasks) {
                        if (t.getId().equals(child.getParentTaskId())) {
                            item.getChildren().add(new TreeItem<>(child));
                        }
                    }
                    root.getChildren().add(item);
                }
            }
            tasksTreeTable.setRoot(root);
        } catch (Exception e) {
        }
    }

    // loadSubTasksFor removed

    private void loadTags(Task task) {
        try {
            List<TaskTag> t = tagService.listByTaskId(task.getId());
            tags.setAll(t);
        } catch (Exception e) {
            System.err.println("Warning: Tags non disponibles");
        }
    }

    private void loadComments(Task task) {
        try {
            List<Comment> c = commentService.getTaskComments(task.getId());
            if (c != null) {
                // Charger les attachments pour chaque commentaire
                commentAttachments.clear();
                for (Comment comment : c) {
                    List<CommentAttachment> atts = attachmentService.getAttachmentsByCommentId(comment.getId());
                    if (atts != null && !atts.isEmpty()) {
                        commentAttachments.put(comment.getId(), atts);
                    }
                }
                comments.setAll(c);
            }
        } catch (Exception e) {
            System.err.println("Warning: Commentaires non disponibles");
        }
    }

    private void loadDependencies(Task task) {
        if (task == null)
            return;
        try {
            // Successors relative to this task = the tasks it depends on
            List<TaskDependency> deps = dependencyService.getSuccessors(task.getId());
            taskDependencies.setAll(deps);
        } catch (Exception e) {
            System.err.println("Warning: Dépendances non disponibles");
        }
    }

    private void showDependenciesPopup(Task task) {
        if (task == null)
            return;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Dependencies for: " + task.getTitle());
        dialog.setHeaderText("This task depends on the following tasks:");

        ListView<TaskDependency> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(dependencyService.getSuccessors(task.getId())));
        listView.setPrefHeight(200);

        listView.setCellFactory(lv -> new ListCell<TaskDependency>() {
            @Override
            protected void updateItem(TaskDependency item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox row = new HBox(10);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label icon = new Label("🔗");
                    String predTitle = (item.getPredecessor() != null && item.getPredecessor().getTitle() != null)
                            ? item.getPredecessor().getTitle()
                            : "Unknown task";

                    VBox info = new VBox(2);
                    Label titleLbl = new Label(predTitle);
                    titleLbl.setStyle("-fx-font-weight: bold;");

                    Label detailLbl = new Label("Type: " + item.getType());
                    detailLbl.setStyle("-fx-font-size: 10; -fx-text-fill: #6b7280;");

                    info.getChildren().addAll(titleLbl, detailLbl);

                    Region spacer = new Region();
                    HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

                    Button deleteBtn = new Button("×");
                    deleteBtn.setStyle(
                            "-fx-text-fill: #dc2626; -fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 16;");
                    deleteBtn.setOnAction(e -> {
                        if (confirmAction("Delete this dependency?")) {
                            dependencyService.deleteDependency(item.getId());
                            listView.getItems().remove(item);
                            refreshDependencyCounts(); // Refresh counts on main view
                        }
                    });

                    row.getChildren().addAll(icon, info, spacer, deleteBtn);
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        VBox content = new VBox(10, listView);
        content.setPrefWidth(400);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        dialog.showAndWait();
    }

    // --- EVENEMENTS UI ---

    @FXML
    private void onProjectClicked(MouseEvent event) {
        // Le listener de sélection gère déjà le chargement des tâches
        // Cette méthode ne fait rien pour éviter les appels doubles
        System.out.println("[DEBUG] onProjectClicked appelée");
    }

    @FXML
    private void onTaskClicked(MouseEvent event) {
        // Handled by selection listener
    }

    // onSubtaskClicked removed

    @FXML
    private void onBackToProjects() {
        MainLayoutController.getInstance().navigateTo("/fxml/projects/root-view.fxml", project);
    }

    private void updateTaskStatus(Task task) {
        // Validation des dépendances : on ne peut pas terminer une tâche si ses
        // prédécesseurs ne sont pas finis
        if (task.getStatus() == Status.COMPLETED) {
            try {
                List<TaskDependency> dependencies = dependencyService.getSuccessors(task.getId());
                List<String> blockingTasks = new java.util.ArrayList<>();

                for (TaskDependency dep : dependencies) {
                    Task pred = dep.getPredecessor();
                    // On considère que le prédécesseur bloque s'il n'est pas COMPLETED
                    if (pred != null && pred.getStatus() != Status.COMPLETED) {
                        blockingTasks.add(pred.getTitle());
                    }
                }

                if (!blockingTasks.isEmpty()) {
                    showAlert("Action Blocked",
                            "This task cannot be completed because it depends on the following uncompleted tasks:\n- "
                                     + String.join("\n- ", blockingTasks));
                    // Recharger les tâches pour remettre l'ancien statut visuellement
                    if (project != null)
                        loadTasks(project);
                    return;
                }
            } catch (Exception e) {
                System.err.println("[WARNING] Impossible de vérifier les dépendances: " + e.getMessage());
            }
        }

        try {
            taskService.updateTask(task);
        } catch (Exception e) {
            showAlert("Error", "Status update failed: " + e.getMessage());
            if (project != null)
                loadTasks(project);
        }
    }

    // --- FORMULAIRES ---

    private void openTaskForm(Task task, boolean isSubtask) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/projects/task-form.fxml"));
            Parent root = loader.load();
            TaskFormController ctrl = loader.getController();

            Stage stage = new Stage();
            ctrl.setStage(stage);
            ctrl.setTask(task);

            stage.setScene(new Scene(root));
            stage.initOwner(tasksTreeTable.getScene().getWindow());
            stage.showAndWait();

            Task result = ctrl.getTask();
            if (result != null) {
                System.out.println("[DEBUG] Tâche résultante: " + result.getTitle());
                // Création ou Mise à jour
                if (result.getId() == null) {
                    System.out.println("[DEBUG] Création nouvelle tâche");
                    if (currentUser != null && result.getUser() == null)
                        result.setUser(currentUser);
                    Task created = taskService.createTask(result);
                    System.out.println("[DEBUG] Task created with ID: " + (created != null ? created.getId() : "NULL"));
                    if (created != null) {
                        if (project != null)
                            loadTasks(project);
                    } else {
                        showAlert("Error", "The task could not be created. Check server logs.");
                    }
                } else {
                    System.out.println("[DEBUG] Mise à jour tâche existante: " + result.getId());
                    taskService.updateTask(result);

                    if (project != null) {
                        loadTasks(project);
                        System.out.println("[DEBUG] Tâches rechargées après mise à jour");
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR] Exception in openTaskForm: " + e.getMessage());
            showAlert("Error", "Cannot open form: " + e.getMessage());
        }
    }

    // --- BOUTONS ---

    @FXML
    private void onAddTask() {
        if (project == null) {
            showAlert("Info", "Select a project");
            return;
        }
        Task t = new Task();
        t.setProjectId(project.getId());
        openTaskForm(t, false);
    }

    @FXML
    private void onEditTask() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null) {
            openTaskForm(selected.getValue(), false);
        }
    }

    @FXML
    private void onDeleteTask() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getValue() != null) {
            handleDeleteTask(selected.getValue());
        }
    }

    @FXML
    private void onAddSubtask() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getValue() == null) {
            showAlert("Info", "Select a parent task");
            return;
        }

        Task parent = selected.getValue();
        Task t = new Task();
        t.setProjectId(parent.getProjectId());
        t.setParentTask(parent);
        t.setParentTaskId(parent.getId());
        openTaskForm(t, true);
    }

    // --- TAGS & COMMENTS ---
    @FXML
    private void onAddTag() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        Task t = selected != null ? selected.getValue() : null;
        if (t == null) {
            showAlert("Info", "Select a task");
            return;
        }

        TextInputDialog d = new TextInputDialog();
        d.setTitle("Add a tag");
        d.setHeaderText("Enter tag name");
        d.showAndWait().ifPresent(tagName -> {
            if (!tagName.trim().isEmpty()) {
                try {
                    TaskTag tag = new TaskTag(t, tagName);
                    tagService.add(tag);
                    loadTags(t);
                    System.out.println("[DEBUG] Tag ajouté: " + tagName);
                } catch (Exception e) {
                    System.err.println("[ERROR] Erreur ajout tag: " + e.getMessage());
                }
            }
        });
    }

    @FXML
    private void onRemoveTag() {
        TaskTag tag = tagsList.getSelectionModel().getSelectedItem();
        if (tag == null) {
            showAlert("Info", "Select a tag to delete");
            return;
        }

        if (confirmAction("Delete this tag?")) {
            try {
                TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
                Task t = selected != null ? selected.getValue() : null;
                if (t != null && tag.getId() != null) {
                    String taskId = tag.getId().getTaskId();
                    String tagName = tag.getId().getTagName();
                    tagService.delete(taskId, tagName);
                    loadTags(t);
                    System.out.println("[DEBUG] Tag supprimé: " + tag.getTagName());
                }
            } catch (Exception e) {
                System.err.println("[ERROR] Erreur suppression tag: " + e.getMessage());
            }
        }
    }

    @FXML
    private void onAddComment() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        Task t = selected != null ? selected.getValue() : null;
        if (t == null) {
            showAlert("Info", "Select a task");
            return;
        }

        String commentText = commentField.getText().trim();
        if (commentText.isEmpty() && pendingAttachment == null) {
            showAlert("Info", "Enter a comment or attach a file");
            return;
        }

        try {
            Comment comment = new Comment();
            comment.setTask(t);
            comment.setUser(currentUser);
            comment.setContent(commentText.isEmpty() ? "[Attached file]" : commentText);
            comment.setCreatedAt(new java.util.Date());

            // Add comment and get ID (assuming void returns nothing, we need to fetch
            // latest or hope for object return)
            // If addComment returns void, we have a problem. Checking CommentService next.
            // Assuming addComment returns the comment with ID or void.
            // Let's assume for now we add comment, then refresh.
            // BUT to attach file we need ID.
            // I'll check CommentService return type first.

            Comment savedComment = commentService.addComment(comment);

            if (pendingAttachment != null && savedComment != null) {
                CommentAttachment attachment = new CommentAttachment(
                        savedComment.getId(),
                        pendingAttachment.getName(),
                        getFileExtension(pendingAttachment.getName()),
                        pendingAttachment.length(),
                        pendingAttachment.getAbsolutePath());
                try {
                    attachmentService.addAttachment(attachment);
                    System.out
                            .println("[DEBUG] Fichier attaché au nouveau commentaire: " + pendingAttachment.getName());
                } catch (Exception attEx) {
                    System.err.println("[ERROR] Attachment error: " + attEx.getMessage());
                    showAlert("Error",
                            "The comment was added but the file could not be attached. Verify that the database is up to date.");
                }
                pendingAttachment = null;
                if (attachBtn != null) {
                    attachBtn.setStyle("");
                    attachBtn.setText("📎");
                }
            }
            loadComments(t);

            commentField.clear();
            System.out.println("[DEBUG] Commentaire ajouté");
        } catch (Exception e) {
            System.err.println("[ERROR] Erreur ajout commentaire: " + e.getMessage());
        }
    }

    @FXML
    private void onStartTimeTracking() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        Task t = selected != null ? selected.getValue() : null;
        if (t == null) {
            showAlert("Info", "Select a task");
            return;
        }

        if (timeTrackingStart != null) {
            showAlert("Info", "A time tracking session is already in progress. Stop it first.");
            return;
        }

        timeTrackingStart = LocalDateTime.now();
        System.out.println("[DEBUG] Time tracking started for: " + t.getTitle());
        showAlert("Success", "Time tracking started for: " + t.getTitle());
    }

    @FXML
    private void onStopTimeTracking() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        Task t = selected != null ? selected.getValue() : null;
        if (t == null) {
            showAlert("Info", "Select a task");
            return;
        }

        if (timeTrackingStart == null) {
            showAlert("Info", "No time tracking session in progress");
            return;
        }

        try {
            LocalDateTime endTime = LocalDateTime.now();
            long minutes = java.time.temporal.ChronoUnit.MINUTES.between(timeTrackingStart, endTime);
            long milliseconds = minutes * 60 * 1000;

            TimeTracking tracking = new TimeTracking();
            tracking.setTask(t);
            tracking.setUser(currentUser);
            tracking.setStartTime(new java.util.Date(System.currentTimeMillis() - milliseconds));
            tracking.setEndTime(new java.util.Date());
            tracking.setDurationMs(milliseconds);

            // Créer le suivi de temps via le service
            timeTrackingService.updateTracking(tracking);

            long hours = minutes / 60;
            long mins = minutes % 60;
            // Label updated
            // timeTrackingLabel.setText(hours + "h " + mins + "m");
            timeTrackingStart = null;

            System.out.println("[DEBUG] Time tracking stopped: " + hours + "h " + mins + "m");
            showAlert("Success", "Time recorded: " + hours + "h " + mins + "m");
        } catch (Exception e) {
            System.err.println("[ERROR] Time tracking error: " + e.getMessage());
            showAlert("Error", "Cannot save time: " + e.getMessage());
        }
    }

    // --- GESTION DES DÉPENDANCES ENTRE TÂCHES ---
    @FXML
    private void onAddDependency() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        Task currentTask = selected != null ? selected.getValue() : null;
        if (currentTask == null) {
            showAlert("Info", "Select a task");
            return;
        }

        // Créer une boîte de dialogue pour sélectionner la tâche prédécesseur
        // Filter out current task and tasks that already have a dependency (simple loop
        // prevention for now)
        List<Task> availableTasks = tasks.stream()
                .filter(t -> !t.getId().equals(currentTask.getId()))
                .collect(java.util.stream.Collectors.toList());

        if (availableTasks.isEmpty()) {
            showAlert("Info", "No other tasks available");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Add a dependency");
        dialog.setHeaderText("Select the task that this task depends on:");

        ComboBox<Task> taskCombo = new ComboBox<>();
        taskCombo.setItems(FXCollections.observableArrayList(availableTasks));
        taskCombo.setCellFactory(lv -> new ListCell<Task>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        taskCombo.setButtonCell(new ListCell<Task>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });

        VBox content = new VBox(10, new Label("Predecessor task:"), taskCombo);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK && taskCombo.getValue() != null) {
            try {
                Task selectedPredecessor = taskCombo.getValue();

                // Create lightweight tasks to avoid serialization loops
                Task predecessorRef = new Task();
                predecessorRef.setId(selectedPredecessor.getId());

                Task currentTaskRef = new Task();
                currentTaskRef.setId(currentTask.getId());

                TaskDependency dep = new TaskDependency(predecessorRef, currentTaskRef,
                        TaskDependency.DependencyType.FINISH_TO_START);
                dependencyService.addDependency(dep);
                showAlert("Success", "Dependency added");
                loadDependencies(currentTask);
                refreshDependencyCounts();
                System.out.println(
                        "[DEBUG] Dépendance ajoutée: " + selectedPredecessor.getTitle() + " -> "
                                + currentTask.getTitle());
            } catch (Exception e) {
                System.err.println("[ERROR] Add dependency error: " + e.getMessage());
                showAlert("Error", "Error while adding dependency");
            }
        }
    }

    @FXML
    private void onManageDependencies() {
        onAddDependency();
    }

    // --- GESTION DES ATTACHEMENTS DE COMMENTAIRES ---
    @FXML
    private void onAttachFileToComment() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Add a file to comment");
        java.io.File selectedFile = fileChooser.showOpenDialog(new javafx.stage.Stage());

        if (selectedFile != null) {
            this.pendingAttachment = selectedFile;
            if (attachBtn != null) {
                attachBtn.setStyle("-fx-background-color: #dcfce7; -fx-border-color: #22c55e;");
                attachBtn.setText("📎 (" + selectedFile.getName() + ")");
            }
            // showAlert("Succès", "Fichier prêt à être envoyé : " +
            // selectedFile.getName());
        }
    }

    // --- GESTION DU PARTAGE DE TÂCHES ---
    @FXML
    private void onShareTask() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        Task taskToShare = selected != null ? selected.getValue() : null;
        if (taskToShare == null) {
            showAlert("Info", "Select a task");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Share task");
        dialog.setHeaderText("Share this task with a user");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Username");
        ComboBox<String> permissionCombo = new ComboBox<>();
        permissionCombo.setItems(FXCollections.observableArrayList("READ", "WRITE", "ADMIN"));
        permissionCombo.setValue("READ");

        VBox content = new VBox(10,
                new Label("Username:"), usernameField,
                new Label("Permission level:"), permissionCombo);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK && !usernameField.getText().isEmpty()) {
            try {
                String permLevel = permissionCombo.getValue();
                sharedTaskService.shareTask(taskToShare.getId(), usernameField.getText(), permLevel,
                        currentUser.getUsername());
                showAlert("Success", "Task shared successfully");
                System.out.println("[DEBUG] Task shared with: " + usernameField.getText());
            } catch (Exception e) {
                System.err.println("[ERROR] Share error: " + e.getMessage());
                showAlert("Error", "Error while sharing task");
            }
        }
    }

    // --- GESTION DE LA RÉCURRENCE ---
    @FXML
    private void onSetRecurrence() {
        TreeItem<Task> selected = tasksTreeTable.getSelectionModel().getSelectedItem();
        Task task = selected != null ? selected.getValue() : null;
        if (task == null) {
            showAlert("Info", "Select a task");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Set recurrence");
        dialog.setHeaderText("Choose recurrence type:");

        ComboBox<String> recurrenceCombo = new ComboBox<>();
        recurrenceCombo.setItems(FXCollections.observableArrayList("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"));
        recurrenceCombo.setValue("NONE");

        Spinner<Integer> intervalSpinner = new Spinner<>(1, 365, 1);

        VBox content = new VBox(10,
                new Label("Recurrence type:"), recurrenceCombo,
                new Label("Interval (days/weeks/months):"), intervalSpinner);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // TODO: Implémenter l'endpoint de mise à jour pour la récurrence
                showAlert("Success", "Recurrence set: " + recurrenceCombo.getValue());
                System.out.println("[DEBUG] Recurrence set: " + recurrenceCombo.getValue() + ", interval: "
                        + intervalSpinner.getValue());
            } catch (Exception e) {
                System.err.println("[ERROR] Recurrence error: " + e.getMessage());
                showAlert("Error", "Error while setting recurrence");
            }
        }
    }

    // --- UTILITAIRES ---
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot > 0) ? fileName.substring(lastDot + 1) : "";
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, msg, ButtonType.OK);
        a.setTitle(title);
        a.showAndWait();
    }

    private boolean confirmAction(String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        return a.showAndWait().orElse(ButtonType.NO) == ButtonType.YES;
    }
}