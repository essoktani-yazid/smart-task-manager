package com.smarttask.client.view.controller;

import com.smarttask.client.service.TaskService;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Contrôleur pour la vue du dashboard (liste des tâches).
 */
public class DashboardController {
    @FXML
    private Label userLabel;

    @FXML
    private Button logoutButton;

    @FXML
    private TextField titleField;

    @FXML
    private TextField descriptionField;

    @FXML
    private ComboBox<Priority> priorityComboBox;

    @FXML
    private Button addTaskButton;

    @FXML
    private TableView<Task> tasksTable;

    @FXML
    private TableColumn<Task, String> titleColumn;

    @FXML
    private TableColumn<Task, String> descriptionColumn;

    @FXML
    private TableColumn<Task, Priority> priorityColumn;

    @FXML
    private TableColumn<Task, Status> statusColumn;

    @FXML
    private TableColumn<Task, String> dueDateColumn;

    @FXML
    private TableColumn<Task, String> actionsColumn;

    private User currentUser;
    private final TaskService taskService = new TaskService();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();

    @FXML
    private void initialize() {
        // Initialiser la ComboBox de priorité
        priorityComboBox.setItems(FXCollections.observableArrayList(Priority.values()));
        priorityComboBox.setValue(Priority.MEDIUM);

        // Configurer les colonnes du tableau
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dueDate = cellData.getValue().getDueDate();
            return new javafx.beans.property.SimpleStringProperty(
                    dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : ""
            );
        });

        // Colonne des actions
        actionsColumn.setCellFactory(column -> new TableCell<Task, String>() {
            private final Button deleteButton = new Button("Supprimer");
            private final Button editButton = new Button("Modifier");

            {
                deleteButton.setOnAction(event -> {
                    Task task = getTableView().getItems().get(getIndex());
                    handleDeleteTask(task);
                });

                editButton.setOnAction(event -> {
                    Task task = getTableView().getItems().get(getIndex());
                    handleEditTask(task);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editButton, deleteButton);
                    setGraphic(buttons);
                }
            }
        });

        tasksTable.setItems(tasks);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        userLabel.setText("Bienvenue, " + user.getUsername());
        loadTasks();
    }

    @FXML
    private void handleAddTask() {
        String title = titleField.getText();
        String description = descriptionField.getText();
        Priority priority = priorityComboBox.getValue();

        if (title.isEmpty()) {
            showAlert("Erreur", "Le titre est obligatoire");
            return;
        }

        Task task = taskService.createTask(title, description, priority, currentUser);
        if (task != null) {
            tasks.add(task);
            titleField.clear();
            descriptionField.clear();
        } else {
            showAlert("Erreur", "Impossible de créer la tâche");
        }
    }

    @FXML
    private void handleDeleteTask(Task task) {
        boolean deleted = taskService.deleteTask(task.getId());
        if (deleted) {
            tasks.remove(task);
        } else {
            showAlert("Erreur", "Impossible de supprimer la tâche");
        }
    }

    @FXML
    private void handleEditTask(Task task) {
        // Ouvrir une boîte de dialogue pour modifier la tâche
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Modifier la tâche");
        dialog.setHeaderText("Modifiez les informations de la tâche");

        TextField titleField = new TextField(task.getTitle());
        TextField descriptionField = new TextField(task.getDescription());
        ComboBox<Priority> priorityCombo = new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
        priorityCombo.setValue(task.getPriority());
        ComboBox<Status> statusCombo = new ComboBox<>(FXCollections.observableArrayList(Status.values()));
        statusCombo.setValue(task.getStatus());

        dialog.getDialogPane().setContent(new javafx.scene.layout.VBox(10,
                new Label("Titre:"), titleField,
                new Label("Description:"), descriptionField,
                new Label("Priorité:"), priorityCombo,
                new Label("Statut:"), statusCombo));

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                task.setTitle(titleField.getText());
                task.setDescription(descriptionField.getText());
                task.setPriority(priorityCombo.getValue());
                task.setStatus(statusCombo.getValue());
                return task;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedTask -> {
            Task savedTask = taskService.updateTask(updatedTask);
            if (savedTask != null) {
                int index = tasks.indexOf(task);
                tasks.set(index, savedTask);
            } else {
                showAlert("Erreur", "Impossible de mettre à jour la tâche");
            }
        });
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("SmartTaskManager - Connexion");
            stage.setWidth(400);
            stage.setHeight(500);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadTasks() {
        if (currentUser != null) {
            java.util.List<Task> userTasks = taskService.getTasksByUser(currentUser.getId());
            if (userTasks != null) {
                tasks.setAll(userTasks);
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

