package com.smarttask.client.view.controller.projects;

import com.smarttask.client.service.ProjectService;
import com.smarttask.client.service.TaskService;
import com.smarttask.client.service.TaskTagService;
import com.smarttask.client.service.CommentService;
import com.smarttask.client.service.TimeTrackingService;
import com.smarttask.client.service.CommentAttachmentService;
import com.smarttask.client.service.TaskDependencyService;
import com.smarttask.client.service.SharedTaskService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.client.view.controller.MainLayoutController;
import com.smarttask.model.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public class ProjectViewController {

    // --- TABLEAU PRINCIPAL (Tâches Parente) ---
    @FXML private TableView<Task> tasksTable;
    @FXML private TableColumn<Task, String> colTitle;
    @FXML private TableColumn<Task, String> colStatus;
    @FXML private TableColumn<Task, String> colPriority;
    @FXML private TableColumn<Task, String> colDueDate;
    @FXML private TableColumn<Task, String> colAssignee;
    @FXML private TableColumn<Task, Void> colActions;

    // --- TABLEAU DES SOUS-TÂCHES ---
    @FXML private TableView<Task> subTasksTable;
    @FXML private TableColumn<Task, String> subTitleCol;
    @FXML private TableColumn<Task, String> subStatusCol;
    @FXML private TableColumn<Task, String> subPriorityCol;
    @FXML private TableColumn<Task, String> subAssigneeCol;

    // --- DÉTAILS ---
    @FXML private ListView<TaskTag> tagsList;
    @FXML private ListView<Comment> commentsList;
    @FXML private TextField commentField;
    @FXML private Label timeTrackingLabel;
    @FXML private Button addSubtaskBtn;

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
    private ObservableList<Comment> comments = FXCollections.observableArrayList();

    private User currentUser;
    private Task selectedTask = null;

    public void setProject(Project p) {
        project = p;
        if (project != null) {
            loadTasks(project);
            refreshTeamsList();
            System.out.println("[DEBUG] Données chargées pour le projet: " + project.getName());
        }
    }

    @FXML private TableView<Team> teamsTable;
    @FXML private TableColumn<Team, String> colTeamName;
    @FXML private TableColumn<Team, String> colTeamDescription;

    @FXML
    public void initialize() {

        setupMainTable();
        setupSubTasksTable();
        setupTeamsTable();
        setupDetailLists();

        tasksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedTask = newSel;
                loadSubTasksFor(newSel);
                loadTags(newSel);
                loadComments(newSel);
            } else {
                subTasksTable.getItems().clear();
            }
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
        if (project == null) return;
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
            if (success) refreshTeamsList();
        }
    }

    @FXML
    private void onRemoveTeam() {
        Team selected = teamsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean confirm = showConfirmation("Voulez-vous retirer l'équipe " + selected.getName() + " de ce projet ?");
            if (confirm) {
                boolean success = projectService.removeTeamFromProject(project.getId(), selected.getId());
                if (success) refreshTeamsList();
            }
        }
    }

    private String showTeamSelectionDialog() {
        com.smarttask.client.service.TeamService teamService = new com.smarttask.client.service.TeamService();
        List<Team> userTeams = teamService.getMyTeams(currentUser.getId());

        if (userTeams == null || userTeams.isEmpty()) {
            showAlert("Information", "Vous ne faites partie d'aucune équipe.");
            return null;
        }

        ChoiceDialog<Team> dialog = new ChoiceDialog<>(userTeams.get(0), userTeams);
        dialog.setTitle("Ajouter une équipe");
        dialog.setHeaderText("Sélectionnez l'équipe à ajouter au projet " + project.getName());
        dialog.setContentText("Équipe :");

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

    private void setupMainTable() {
        tasksTable.setItems(tasks);
        colTitle.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        colPriority.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPriority() != null ? c.getValue().getPriority().name() : ""));
        colDueDate.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getDueDate() == null ? "" : c.getValue().getDueDate().toLocalDate().toString()));

        // Assigné à
        colAssignee.setCellValueFactory(c -> {
            if (c.getValue().getUser() != null) return new SimpleStringProperty(c.getValue().getUser().getUsername());
            return new SimpleStringProperty("-");
        });

        // Statut Modifiable
        colStatus.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        colStatus.setCellFactory(col -> new TableCell<Task, String>() {
            private final ComboBox<Status> comboBox = new ComboBox<>(FXCollections.observableArrayList(Status.values()));
            {
                comboBox.setOnAction(e -> {
                    Task t = getTableView().getItems().get(getIndex());
                    Status newStatus = comboBox.getValue();
                    if (t.getStatus() != newStatus) {
                        t.setStatus(newStatus);
                        updateTaskStatus(t);
                    }
                });
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); }
                else {
                    try { comboBox.setValue(Status.valueOf(item)); } catch(Exception e) {}
                    setGraphic(comboBox);
                }
            }
        });
    }

    private void setupSubTasksTable() {
        subTitleCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getTitle()));
        subStatusCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getStatus().name()));
        subPriorityCol.setCellValueFactory(c -> new SimpleStringProperty(c.getValue().getPriority().name()));
        subAssigneeCol.setCellValueFactory(c -> new SimpleStringProperty(
                c.getValue().getUser() != null ? c.getValue().getUser().getUsername() : "-"
        ));

        subTasksTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                selectedTask = newSel;
                loadTags(newSel);
                loadComments(newSel);
            }
        });
    }

    private void setupDetailLists() {
        tagsList.setItems(tags);
        tagsList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(TaskTag item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTagName());
            }
        });
        commentsList.setItems(comments);
        commentsList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Comment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    String authorName = item.getUser() != null ? item.getUser().getUsername() : "Anonyme";
                    String timestamp = item.getCreatedAt() != null ? " · " + item.getCreatedAt().toString() : "";
                    String text = authorName + timestamp + "\n" + item.getContent();
                    setText(text);
                    setStyle("-fx-padding: 5px; -fx-border-color: #ccc; -fx-border-radius: 3; -fx-wrap-text: true;");
                    setWrapText(true);
                }
            }
        });
    }

    // --- LOGIQUE CHARGEMENT ---

    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("[DEBUG] ProjectsViewController.setCurrentUser appelée avec: " + (user != null ? user.getUsername() : "NULL"));
    }

    private void loadTasks(Project project) {
        if (project == null) {
            System.out.println("[DEBUG] Tentative de chargement avec un projet NULL");
            return;
        }
        System.out.println("[DEBUG] Chargement tâches pour le projet: " + project.getName() + " (ID: " + project.getId() + ")");
        try {
            List<Task> projectTasks = taskService.getTasksByProject(project.getId());

            if (projectTasks == null) projectTasks = java.util.Collections.emptyList();

            System.out.println("[DEBUG] Tâches reçues pour ce projet: " + projectTasks.size());

            List<Task> mains = new java.util.ArrayList<>();

            for (Task t : projectTasks) {
                if (t.getParentTask() == null || t.getParentTask().getId() == null) {
                    mains.add(t);
                }
            }

            System.out.println("[DEBUG] Tâches principales: " + mains.size());
            tasks.setAll(mains);
            subTasksTable.getItems().clear();

            if (!tasks.isEmpty()) tasksTable.getSelectionModel().select(0);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ERROR] Exception dans loadTasks: " + e.getMessage());
            showAlert("Erreur", "Impossible de charger les tâches: " + e.getMessage());
        }
    }

    private void loadSubTasksFor(Task parentTask) {
        try {
            List<Task> subTasks = taskService.getSubTasks(parentTask.getId());
            subTasksTable.setItems(FXCollections.observableArrayList(subTasks));
        } catch (Exception e) {
            System.err.println("Erreur chargement sous-tâches: " + e.getMessage());
        }
    }

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
            if (c != null) comments.setAll(c);
        } catch (Exception e) {
            System.err.println("Warning: Commentaires non disponibles");
        }
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
        if (event.getClickCount() == 2 && tasksTable.getSelectionModel().getSelectedItem() != null) {
            onEditTask();
        }
    }

    @FXML
    private void onSubtaskClicked(MouseEvent event) {
        Task sel = subTasksTable.getSelectionModel().getSelectedItem();
        if (event.getClickCount() == 2 && sel != null) {
            handleEditSubtask(sel);
        }
    }

    @FXML
    private void onBackToProjects() {
        MainLayoutController.getInstance().navigateTo("/fxml/projects/root-view.fxml", project);
    }

    private void updateTaskStatus(Task task) {
        try {
            taskService.updateTask(task);
        } catch (Exception e) {
            showAlert("Erreur", "Echec maj statut: " + e.getMessage());
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
            stage.initOwner(tasksTable.getScene().getWindow());
            stage.showAndWait();

            Task result = ctrl.getTask();
            if (result != null) {
                System.out.println("[DEBUG] Tâche résultante: " + result.getTitle());
                // Création ou Mise à jour
                if (result.getId() == null) {
                    System.out.println("[DEBUG] Création nouvelle tâche");
                    if (currentUser != null && result.getUser() == null) result.setUser(currentUser);
                    Task created = taskService.createTask(result);
                    System.out.println("[DEBUG] Tâche créée avec ID: " + (created != null ? created.getId() : "NULL"));
                    if (project != null) loadTasks(project);
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
            System.err.println("[ERROR] Exception dans openTaskForm: " + e.getMessage());
            showAlert("Erreur", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    // --- BOUTONS ---

    @FXML
    private void onAddTask() {
        if (project == null) { showAlert("Info", "Sélectionnez un projet"); return; }
        Task t = new Task();
        t.setProjectId(project.getId());
        openTaskForm(t, false);
    }

    @FXML
    private void onEditTask() {
        Task t = tasksTable.getSelectionModel().getSelectedItem();
        if (t != null) openTaskForm(t, false);
    }

    @FXML
    private void onDeleteTask() {
        Task t = tasksTable.getSelectionModel().getSelectedItem();
        if (t != null && confirmAction("Supprimer la tâche ?")) {
            try {
                taskService.deleteTask(t.getId());
                loadTasks(project);
            } catch(Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void onAddSubtask() {
        Task parent = tasksTable.getSelectionModel().getSelectedItem();
        if (parent == null) { showAlert("Info", "Sélectionnez une tâche parente"); return; }
        Task t = new Task();
        t.setProjectId(parent.getProjectId());
        t.setParentTask(parent);
        openTaskForm(t, true);
    }

    private void handleEditSubtask(Task sub) {
        openTaskForm(sub, true);
    }

    // --- TAGS & COMMENTS ---
    @FXML
    private void onAddTag() {
        Task t = selectedTask != null ? selectedTask : tasksTable.getSelectionModel().getSelectedItem();
        if (t == null) { showAlert("Info", "Sélectionnez une tâche"); return; }
        
        TextInputDialog d = new TextInputDialog();
        d.setTitle("Ajouter un tag");
        d.setHeaderText("Entrez le nom du tag");
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
        if (tag == null) { showAlert("Info", "Sélectionnez un tag à supprimer"); return; }
        
        if (confirmAction("Supprimer ce tag?")) {
            try {
                Task t = selectedTask != null ? selectedTask : tasksTable.getSelectionModel().getSelectedItem();
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
        Task t = selectedTask != null ? selectedTask : tasksTable.getSelectionModel().getSelectedItem();
        if (t == null) { showAlert("Info", "Sélectionnez une tâche"); return; }
        
        String commentText = commentField.getText().trim();
        if (commentText.isEmpty()) { showAlert("Info", "Entrez un commentaire"); return; }
        
        try {
            Comment comment = new Comment();
            comment.setTask(t);
            comment.setUser(currentUser);
            comment.setContent(commentText);
            comment.setCreatedAt(new java.util.Date());
            commentService.addComment(comment);
            commentField.clear();
            loadComments(t);
            System.out.println("[DEBUG] Commentaire ajouté");
        } catch (Exception e) {
            System.err.println("[ERROR] Erreur ajout commentaire: " + e.getMessage());
        }
    }

    @FXML
    private void onStartTimeTracking() {
        Task t = selectedTask != null ? selectedTask : tasksTable.getSelectionModel().getSelectedItem();
        if (t == null) { showAlert("Info", "Sélectionnez une tâche"); return; }
        
        if (timeTrackingStart != null) {
            showAlert("Info", "Un suivi de temps est déjà en cours. Arrêtez-le d'abord.");
            return;
        }
        
        timeTrackingStart = LocalDateTime.now();
        System.out.println("[DEBUG] Suivi de temps démarré pour: " + t.getTitle());
        showAlert("Succès", "Suivi de temps démarré pour: " + t.getTitle());
    }

    @FXML
    private void onStopTimeTracking() {
        Task t = selectedTask != null ? selectedTask : tasksTable.getSelectionModel().getSelectedItem();
        if (t == null) { showAlert("Info", "Sélectionnez une tâche"); return; }
        
        if (timeTrackingStart == null) {
            showAlert("Info", "Aucun suivi de temps en cours");
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
            timeTrackingLabel.setText(hours + "h " + mins + "m");
            timeTrackingStart = null;
            
            System.out.println("[DEBUG] Suivi de temps arrêté: " + hours + "h " + mins + "m");
            showAlert("Succès", "Temps enregistré: " + hours + "h " + mins + "m");
        } catch (Exception e) {
            System.err.println("[ERROR] Erreur suivi temps: " + e.getMessage());
            showAlert("Erreur", "Impossible d'enregistrer le temps: " + e.getMessage());
        }
    }

    // --- GESTION DES DÉPENDANCES ENTRE TÂCHES ---
    @FXML
    private void onAddDependency() {
        Task currentTask = selectedTask != null ? selectedTask : tasksTable.getSelectionModel().getSelectedItem();
        if (currentTask == null) { showAlert("Info", "Sélectionnez une tâche"); return; }
        
        // Créer une boîte de dialogue pour sélectionner la tâche prédécesseur
        List<Task> allTasks = tasks;
        if (allTasks.isEmpty()) { showAlert("Info", "Aucune autre tâche disponible"); return; }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Ajouter une dépendance");
        dialog.setHeaderText("Sélectionnez la tâche dont dépend cette tâche:");
        
        ComboBox<Task> taskCombo = new ComboBox<>();
        taskCombo.setItems(FXCollections.observableArrayList(allTasks));
        taskCombo.setCellFactory(lv -> new ListCell<Task>() {
            @Override protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        taskCombo.setButtonCell(new ListCell<Task>() {
            @Override protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitle());
            }
        });
        
        VBox content = new VBox(10, new Label("Tâche prédécesseur:"), taskCombo);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK && taskCombo.getValue() != null) {
            try {
                Task predecessor = taskCombo.getValue();
                TaskDependency dep = new TaskDependency(predecessor, currentTask, TaskDependency.DependencyType.FINISH_TO_START);
                dependencyService.addDependency(dep);
                showAlert("Succès", "Dépendance ajoutée");
                System.out.println("[DEBUG] Dépendance ajoutée: " + predecessor.getTitle() + " -> " + currentTask.getTitle());
            } catch (Exception e) {
                System.err.println("[ERROR] Erreur ajout dépendance: " + e.getMessage());
                showAlert("Erreur", "Erreur lors de l'ajout de la dépendance");
            }
        }
    }

    // --- GESTION DES ATTACHEMENTS DE COMMENTAIRES ---
    @FXML
    private void onAttachFileToComment() {
        Comment selectedComment = commentsList.getSelectionModel().getSelectedItem();
        if (selectedComment == null) { showAlert("Info", "Sélectionnez un commentaire"); return; }
        
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Ajouter un fichier");
        java.io.File selectedFile = fileChooser.showOpenDialog(new Stage());
        
        if (selectedFile != null) {
            try {
                CommentAttachment attachment = new CommentAttachment(
                    selectedComment.getId(),
                    selectedFile.getName(),
                    getFileExtension(selectedFile.getName()),
                    selectedFile.length(),
                    selectedFile.getAbsolutePath()
                );
                attachmentService.addAttachment(attachment);
                showAlert("Succès", "Fichier attaché au commentaire");
                System.out.println("[DEBUG] Fichier attaché: " + selectedFile.getName());
            } catch (Exception e) {
                System.err.println("[ERROR] Erreur attachement: " + e.getMessage());
                showAlert("Erreur", "Erreur lors de l'attachement du fichier");
            }
        }
    }

    // --- GESTION DU PARTAGE DE TÂCHES ---
    @FXML
    private void onShareTask() {
        Task taskToShare = selectedTask != null ? selectedTask : tasksTable.getSelectionModel().getSelectedItem();
        if (taskToShare == null) { showAlert("Info", "Sélectionnez une tâche"); return; }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Partager la tâche");
        dialog.setHeaderText("Partagez cette tâche avec un utilisateur");
        
        TextField userIdField = new TextField();
        userIdField.setPromptText("ID utilisateur");
        ComboBox<String> permissionCombo = new ComboBox<>();
        permissionCombo.setItems(FXCollections.observableArrayList("READ", "WRITE", "ADMIN"));
        permissionCombo.setValue("READ");
        
        VBox content = new VBox(10, 
            new Label("ID utilisateur:"), userIdField,
            new Label("Niveau de permission:"), permissionCombo
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK && !userIdField.getText().isEmpty()) {
            try {
                String permLevel = permissionCombo.getValue();
                sharedTaskService.shareTask(taskToShare.getId(), userIdField.getText(), permLevel);
                showAlert("Succès", "Tâche partagée avec succès");
                System.out.println("[DEBUG] Tâche partagée avec: " + userIdField.getText());
            } catch (Exception e) {
                System.err.println("[ERROR] Erreur partage: " + e.getMessage());
                showAlert("Erreur", "Erreur lors du partage de la tâche");
            }
        }
    }

    // --- GESTION DE LA RÉCURRENCE ---
    @FXML
    private void onSetRecurrence() {
        Task task = selectedTask != null ? selectedTask : tasksTable.getSelectionModel().getSelectedItem();
        if (task == null) { showAlert("Info", "Sélectionnez une tâche"); return; }
        
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Définir une récurrence");
        dialog.setHeaderText("Choisissez le type de récurrence:");
        
        ComboBox<String> recurrenceCombo = new ComboBox<>();
        recurrenceCombo.setItems(FXCollections.observableArrayList("NONE", "DAILY", "WEEKLY", "MONTHLY", "YEARLY"));
        recurrenceCombo.setValue("NONE");
        
        Spinner<Integer> intervalSpinner = new Spinner<>(1, 365, 1);
        
        VBox content = new VBox(10,
            new Label("Type de récurrence:"), recurrenceCombo,
            new Label("Intervalle (jours/semaines/mois):"), intervalSpinner
        );
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                // TODO: Implémenter l'endpoint de mise à jour pour la récurrence
                showAlert("Succès", "Récurrence définie: " + recurrenceCombo.getValue());
                System.out.println("[DEBUG] Récurrence définie: " + recurrenceCombo.getValue() + ", intervalle: " + intervalSpinner.getValue());
            } catch (Exception e) {
                System.err.println("[ERROR] Erreur récurrence: " + e.getMessage());
                showAlert("Erreur", "Erreur lors de la définition de la récurrence");
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