package com.smarttask.client.view.controller.projects;

import com.smarttask.client.service.*;
import com.smarttask.model.*;
import com.smarttask.client.util.GsonUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.smarttask.client.config.AppConfig;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class TaskDetailController {

    @FXML private BorderPane mainPane;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Label priorityLabel;
    @FXML private Label descriptionLabel;

    // TableView pour sous-tâches
    @FXML private TableView<Task> subtasksTable;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> statusColumn;
    @FXML private TableColumn<Task, String> priorityColumn;
    @FXML private TableColumn<Task, LocalDate> dueDateColumn;
    @FXML private TableColumn<Task, Void> actionsColumn;
    
    @FXML private ListView<TaskTag> tagsList;
    @FXML private ListView<TaskDependency> dependenciesList;
    @FXML private ListView<Comment> commentsList;
    @FXML private TextArea newCommentArea;
    @FXML private Button attachFileBtn;
    @FXML private Label attachmentInfoLabel;
    @FXML private Label attachmentStatusLabel;
    @FXML private ListView<Attachment> attachmentsList;
    @FXML private ListView<TimeTracking> timeLogsList;
    @FXML private Button startTimerBtn;
    @FXML private Button stopTimerBtn;
    @FXML private Label timerLabel;

    // Services
    private final String BASE_URL = AppConfig.API_URL;
    private final TaskService taskService = new TaskService();
    private final TaskTagService tagService = new TaskTagService(BASE_URL);
    private final CommentService commentService = new CommentService();
    private final AttachmentService attachmentService = new AttachmentService(BASE_URL);
    private final TimeTrackingService timeTrackingService = new TimeTrackingService(BASE_URL);
    private final DependencyService dependencyService = new DependencyService();

    private Task currentTask;
    private TimeTracking activeTimeLog;
    private File selectedAttachmentFile;

    @FXML
    public void initialize() {
        setupSubtasksTable();
        setupListViews();
    }
    
    private void setupSubtasksTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        statusColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatus() != null ? cellData.getValue().getStatus().name() : ""
            )
        );
        priorityColumn.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPriority() != null ? cellData.getValue().getPriority().name() : ""
            )
        );
        dueDateColumn.setCellValueFactory(cellData -> {
            Task task = cellData.getValue();
            if (task.getDueDate() != null) {
                return new javafx.beans.property.SimpleObjectProperty<>(task.getDueDate().toLocalDate());
            }
            return new javafx.beans.property.SimpleObjectProperty<>(null);
        });
        
        // Format des dates
        dueDateColumn.setCellFactory(column -> new TableCell<Task, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText("");
                } else {
                    setText(item.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                }
            }
        });
        
        // Actions Column with Edit and Delete buttons
        actionsColumn.setCellFactory(col -> new TableCell<Task, Void>() {
            private final Button editBtn = new Button("✏️ Modifier");
            private final Button deleteBtn = new Button("🗑️ Supprimer");
            private final javafx.scene.layout.HBox hbox = new javafx.scene.layout.HBox(5);
            
            {
                editBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 11; -fx-background-color: #3b82f6; -fx-text-fill: white; -fx-cursor: hand;");
                deleteBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 11; -fx-background-color: #ef4444; -fx-text-fill: white; -fx-cursor: hand;");
                
                editBtn.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    handleEditSubtask(task);
                });
                
                deleteBtn.setOnAction(e -> {
                    Task task = getTableView().getItems().get(getIndex());
                    handleDeleteSubtask(task);
                });
                
                hbox.setAlignment(javafx.geometry.Pos.CENTER);
                hbox.getChildren().addAll(editBtn, deleteBtn);
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    private void setupListViews() {

        // --- 1. COMMENTAIRES (Style "Chat" Pro) ---
        commentsList.setCellFactory(lv -> new ListCell<>() {
            private final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd MMM à HH:mm");

            @Override
            protected void updateItem(Comment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    // Structure globale
                    javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(4);

                    // En-tête : Avatar + Nom + Date
                    javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
                    header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // Avatar (Cercle avec initiale)
                    String username = (item.getUser() != null) ? item.getUser().getUsername() : "?";
                    String initial = (username != null && !username.isEmpty()) ? username.substring(0, 1).toUpperCase() : "?";
                    Label avatar = new Label(initial);
                    avatar.getStyleClass().add("comment-avatar");

                    // Nom de l'auteur
                    Label authorLabel = new Label(username);
                    authorLabel.getStyleClass().add("comment-author");

                    // Date
                    String dateStr = (item.getCreatedAt() != null) ? sdf.format(item.getCreatedAt()) : "";
                    Label dateLabel = new Label(dateStr);
                    dateLabel.getStyleClass().add("comment-date");

                    header.getChildren().addAll(avatar, authorLabel, dateLabel);

                    // Corps du commentaire
                    Label contentLabel = new Label(item.getContent());
                    contentLabel.setWrapText(true);
                    contentLabel.getStyleClass().add("comment-content");

                    container.getChildren().addAll(header, contentLabel);
                    container.getStyleClass().add("comment-cell-container");

                    setGraphic(container);
                    setText(null);
                }
            }
        });

        // --- 2. TAGS (Style "Badge/Pill") ---
        tagsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TaskTag item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    Label tagLabel = new Label(item.getTagName());
                    tagLabel.getStyleClass().add("tag-badge");

                    // Centrer le badge dans la cellule
                    javafx.scene.layout.StackPane pane = new javafx.scene.layout.StackPane(tagLabel);
                    pane.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    setGraphic(pane);
                    setText(null);
                }
            }
        });

        // --- 3. SOUS-TÂCHES (Style Checklist) ---
        // Note: ListView remplacée par TableView dans la vue FXML
        // La configuration sera mise à jour après que le FXML soit synchronisé
        /*
        subtasksList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Task item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    // CORRECTION: Utilisation de Status.COMPLETED (selon votre modèle)
                    boolean isCompleted = (item.getStatus() == Status.COMPLETED);

                    Label statusIcon = new Label(isCompleted ? "☑" : "☐");
                    statusIcon.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (isCompleted ? "#10b981" : "#6b7280") + ";");

                    Label titleLabel = new Label(item.getTitle());
                    titleLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");
                    if (isCompleted) {
                        titleLabel.setStyle("-fx-strikethrough: true; -fx-text-fill: #9ca3af;");
                    }

                    // Gestion sécurité priorité null
                    String priorityName = (item.getPriority() != null) ? item.getPriority().name() : "LOW";
                    Label priorityBadge = new Label(priorityName.substring(0, 1));
                    priorityBadge.getStyleClass().add("mini-badge-" + priorityName.toLowerCase());

                    row.getChildren().addAll(statusIcon, titleLabel, priorityBadge);
                    setGraphic(row);
                    setText(null);
                }
            }
        });
        */

        // --- 4. PIÈCES JOINTES (Style Fichier) ---
        attachmentsList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Attachment item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label icon = new Label("📎");
                    icon.setStyle("-fx-font-size: 16px; -fx-text-fill: #6366f1;");

                    javafx.scene.layout.VBox textContainer = new javafx.scene.layout.VBox(0);
                    Label nameLabel = new Label(item.getFileName());
                    nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151;");

                    // Conversion taille (bytes -> KB/MB)
                    long size = item.getFileSize();
                    String sizeStr = size > 1024 * 1024 ? (size / (1024 * 1024)) + " MB" : (size / 1024) + " KB";
                    Label sizeLabel = new Label(sizeStr);
                    sizeLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #9ca3af;");

                    textContainer.getChildren().addAll(nameLabel, sizeLabel);
                    row.getChildren().addAll(icon, textContainer);

                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // --- 5. LOGS DE TEMPS (Style Timer) ---
        timeLogsList.setCellFactory(lv -> new ListCell<>() {
            private final java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm");

            @Override
            protected void updateItem(TimeTracking item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(15);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label icon = new Label("⏱");

                    String startStr = item.getStartTime() != null ? timeFormat.format(item.getStartTime()) : "--:--";
                    String endStr = item.getEndTime() != null ? timeFormat.format(item.getEndTime()) : "En cours...";

                    Label rangeLabel = new Label(startStr + " ➜ " + endStr);
                    rangeLabel.setStyle("-fx-text-fill: #374151;");

                    // Durée en minutes
                    String durationStr = "";
                    if (item.getDurationMs() != null) {
                        long mins = item.getDurationMs() / (1000 * 60);
                        durationStr = mins + " min";
                    }
                    Label durLabel = new Label(durationStr);
                    durLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4f46e5;");

                    row.getChildren().addAll(icon, rangeLabel, durLabel);
                    setGraphic(row);
                    setText(null);
                }
            }
        });

        // --- 6. DÉPENDANCES (Style Lien) ---
        dependenciesList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(TaskDependency item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(10);
                    row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                    Label icon = new Label("🔗"); // Link icon

                    // CORRECTION: Utilisation de getPredecessor() au lieu de getPredecessorTask()
                    String text = "Dépend de: " + (item.getPredecessor() != null ? item.getPredecessor().getTitle() : "?");
                    Label lbl = new Label(text);
                    lbl.setStyle("-fx-text-fill: #4b5563; -fx-font-style: italic;");

                    row.getChildren().addAll(icon, lbl);
                    setGraphic(row);
                    setText(null);
                }
            }
        });
    }

    public void setTask(Task task) {
        this.currentTask = task;
        if (task == null) return;

        titleLabel.setText(task.getTitle());
        statusLabel.setText(task.getStatus().name());
        priorityLabel.setText(task.getPriority().name());
        descriptionLabel.setText(task.getDescription());

        refreshAll();
    }
    private void refreshAll() {
        if (currentTask == null) return;
        String tId = currentTask.getId(); // currentTask.getId() returns String

        // Load Subtasks (Note: TreeView ou ListView à utiliser temporairement)
        // TableView sera implémentée après synchronisation du FXML
        /*
        new Thread(() -> {
            try {
                List<Task> all = taskService.getTasksByUser(currentTask.getUser().getId());
                List<Task> subs = all.stream()
                    .filter(t -> t.getParentTask() != null && tId.equals(t.getParentTask().getId()))
                    .toList();
                Platform.runLater(() -> subtasksTable.setItems(FXCollections.observableArrayList(subs)));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
        */

        // Load Tags
        new Thread(() -> {
            try {
                List<TaskTag> list = tagService.listByTaskId(tId);
                Platform.runLater(() -> tagsList.setItems(FXCollections.observableArrayList(list)));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // Load Comments
        new Thread(() -> {
            try {
                List<Comment> list = commentService.getTaskComments(tId);
                Platform.runLater(() -> commentsList.setItems(FXCollections.observableArrayList(list)));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // Load Attachments
        new Thread(() -> {
            try {
                List<Attachment> list = attachmentService.getAttachments(tId);
                Platform.runLater(() -> attachmentsList.setItems(FXCollections.observableArrayList(list)));
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        // Load Time Logs
        new Thread(() -> {
            try {
                List<TimeTracking> list = timeTrackingService.getTimeLogs(tId);
                Platform.runLater(() -> {
                    timeLogsList.setItems(FXCollections.observableArrayList(list));
                    // Check if any is running
                    Optional<TimeTracking> running = list.stream().filter(tt -> tt.getEndTime() == null).findFirst();
                    if (running.isPresent()) {
                        activeTimeLog = running.get();
                        startTimerBtn.setDisable(true);
                        stopTimerBtn.setDisable(false);
                        timerLabel.setText("Tracking...");
                    } else {
                        activeTimeLog = null;
                        startTimerBtn.setDisable(false);
                        stopTimerBtn.setDisable(true);
                        timerLabel.setText("00:00:00");
                    }
                });
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
        
        // TODO: Load Dependencies
    }

    @FXML
    private void handleAddSubtask() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Subtask");
        dialog.setHeaderText("Add a subtask");
        dialog.setContentText("Title:");
        dialog.showAndWait().ifPresent(title -> {
            Task sub = new Task();
            sub.setTitle(title);
            sub.setDescription("");
            sub.setUser(currentTask.getUser()); // Inherit user
            sub.setParentTask(currentTask);
            sub.setProjectId(currentTask.getProjectId());
            
            try {
                taskService.createTask(sub);
                refreshAll();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }
    
    private void handleEditSubtask(Task subtask) {
        TextInputDialog dialog = new TextInputDialog(subtask.getTitle());
        dialog.setTitle("Modifier la sous-tâche");
        dialog.setHeaderText("Modification du titre");
        dialog.setContentText("Titre:");
        dialog.showAndWait().ifPresent(newTitle -> {
            subtask.setTitle(newTitle);
            try {
                taskService.updateTask(subtask);
                refreshAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
    
    private void handleDeleteSubtask(Task subtask) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmer la suppression");
        alert.setHeaderText("Êtes-vous sûr?");
        alert.setContentText("Voulez-vous supprimer la sous-tâche \"" + subtask.getTitle() + "\" ?");
        
        Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            try {
                taskService.deleteTask(subtask.getId());
                refreshAll();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    
    @FXML
    private void handleAddTag() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Add Tag");
        dialog.setContentText("Tag Name:");
        dialog.showAndWait().ifPresent(name -> {
            TaskTag t = new TaskTag(currentTask, name);
            try {
                tagService.add(t);
                refreshAll();
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    @FXML
    private void handleAttachFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier à joindre");
        File file = fileChooser.showOpenDialog(attachFileBtn.getScene().getWindow());
        if (file != null) {
            selectedAttachmentFile = file;
            String sizeStr = file.length() > 1024 * 1024 ? 
                (file.length() / (1024 * 1024)) + " MB" : 
                (file.length() / 1024) + " KB";
            attachmentInfoLabel.setText("📎 " + file.getName() + " (" + sizeStr + ")");
        }
    }

    @FXML
    private void handlePostComment() {
        String text = newCommentArea.getText();
        if ((text == null || text.trim().isEmpty()) && selectedAttachmentFile == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Veuillez ajouter du texte ou un fichier", ButtonType.OK);
            alert.setTitle("Commentaire vide");
            alert.showAndWait();
            return;
        }
        if (currentTask == null) return;
        
        // Créer le commentaire texte s'il y a du texte
        if (text != null && !text.trim().isEmpty()) {
            Comment c = new Comment();
            c.setContent(text);
            c.setTask(currentTask);
            if (currentTask.getUser() == null || currentTask.getUser().getId() == null) {
                Alert a = new Alert(Alert.AlertType.INFORMATION, "Utilisateur non défini pour la tâche", ButtonType.OK);
                a.setTitle("Info");
                a.showAndWait();
                return;
            }
            c.setUser(currentTask.getUser());
            try {
                commentService.addComment(c);
                newCommentArea.clear();
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR, e.getMessage() == null ? "Impossible d'ajouter le commentaire" : e.getMessage(), ButtonType.OK);
                a.setTitle("Erreur");
                a.showAndWait();
                return;
            }
        }
        
        // Créer un attachement s'il y a un fichier sélectionné
        if (selectedAttachmentFile != null) {
            try {
                Attachment att = new Attachment();
                att.setTask(currentTask);
                att.setFileName(selectedAttachmentFile.getName());
                att.setFilePath(selectedAttachmentFile.getAbsolutePath());
                att.setFileSize(selectedAttachmentFile.length());
                
                attachmentService.addAttachment(att);
                selectedAttachmentFile = null;
                attachmentInfoLabel.setText("");
                attachmentStatusLabel.setText("✓ Fichier attaché avec succès");
                
                // Masquer le message après 3 secondes
                new Thread(() -> {
                    try {
                        Thread.sleep(3000);
                        Platform.runLater(() -> attachmentStatusLabel.setText(""));
                    } catch (InterruptedException e) { /* Ignorer */ }
                }).start();
                
                refreshAll();
            } catch (Exception e) {
                Alert a = new Alert(Alert.AlertType.ERROR, "Erreur lors de l'upload: " + e.getMessage(), ButtonType.OK);
                a.setTitle("Erreur");
                a.showAndWait();
            }
        } else {
            refreshAll();
        }
    }

    @FXML
    private void handleUploadAttachment() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Attachment");
        File file = fileChooser.showOpenDialog(mainPane.getScene().getWindow());
        if (file != null) {
            Attachment a = new Attachment();
            a.setTask(currentTask);
            a.setFileName(file.getName());
            a.setFilePath(file.getAbsolutePath()); // In real app, upload file content
            a.setFileSize(file.length());
            
            try {
                attachmentService.addAttachment(a);
                refreshAll();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @FXML
    private void handleStartTimer() {
        TimeTracking tt = new TimeTracking(currentTask, currentTask.getUser()); // Should be logged user
        try {
            activeTimeLog = timeTrackingService.startTracking(tt);
            startTimerBtn.setDisable(true);
            stopTimerBtn.setDisable(false);
            refreshAll();
        } catch (Exception e) { e.printStackTrace(); }
    }

    @FXML
    private void handleStopTimer() {
        if (activeTimeLog != null) {
            activeTimeLog.stop();
            try {
                timeTrackingService.updateTracking(activeTimeLog);
                activeTimeLog = null;
                startTimerBtn.setDisable(false);
                stopTimerBtn.setDisable(true);
                refreshAll();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }
    
    @FXML private void handleEditTask() {
        // Implementation for editing task could open a dialog similar to TaskForm
    }

    @FXML private void handleDeleteTask() {
        if (currentTask == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete this task and all its subtasks?", ButtonType.YES, ButtonType.NO);
        alert.showAndWait().ifPresent(bt -> {
            if (bt == ButtonType.YES) {
                try {
                    if (taskService.deleteTask(currentTask.getId())) {
                        titleLabel.getScene().getWindow().hide(); 
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
        });
    }
    @FXML private void handleAddDependency() {}
}
