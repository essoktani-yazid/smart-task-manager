package com.smarttask.client.view.controller;

import com.smarttask.client.service.GoogleCalendarService;
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
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DashboardController {

    // --- NAVIGATION ---
    @FXML private BorderPane mainBorderPane; // La racine
    @FXML private VBox tasksView;           // La vue "Tableau" par défaut
    
    // --- COMPOSANTS EXISTANTS ---
    @FXML private TextField titleField;
    @FXML private TextField descriptionField;
    @FXML private ComboBox<Priority> priorityComboBox;
    @FXML private DatePicker dueDatePicker;
    @FXML private TableView<Task> tasksTable;
    @FXML private TableColumn<Task, String> titleColumn;
    @FXML private TableColumn<Task, String> descriptionColumn;
    @FXML private TableColumn<Task, Priority> priorityColumn;
    @FXML private TableColumn<Task, Status> statusColumn;
    @FXML private TableColumn<Task, String> dueDateColumn;
    @FXML private TableColumn<Task, String> actionsColumn;

    private User currentUser;
    private final TaskService taskService = new TaskService();
    private final ObservableList<Task> tasks = FXCollections.observableArrayList();
    //private final GoogleCalendarService googleCalendarService ;

    // Cache pour la vue Analytics pour éviter de la recharger à chaque fois
    private Parent analyticsView;



    @FXML
    private void initialize() {
        // --- Initialisation de la Table des tâches ---
        setupTaskTable();
        
        // --- Sécurisation des ComboBox ---
        if (priorityComboBox != null) {
            priorityComboBox.setItems(FXCollections.observableArrayList(Priority.values()));
            priorityComboBox.setValue(Priority.MEDIUM);
        }
    }

    private void setupTaskTable() {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        // descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description")); // Si utilisé

        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Priority item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getLabel());
            }
        });

        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Status item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getLabel());
            }
        });

        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getDueDate();
            return new javafx.beans.property.SimpleStringProperty(
                    date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "-"
            );
        });

        // Colonne Actions (Edit/Delete)
        actionsColumn.setCellFactory(param -> new TableCell<>() {
            private final Button deleteBtn = new Button("Suppr.");
            private final Button editBtn = new Button("Edit");
            {
                deleteBtn.setStyle("-fx-background-color: #ff4d4d; -fx-text-fill: white; -fx-font-size: 10px;");
                editBtn.setStyle("-fx-background-color: #4d94ff; -fx-text-fill: white; -fx-font-size: 10px;");
                
                deleteBtn.setOnAction(e -> handleDeleteTask(getTableView().getItems().get(getIndex())));
                editBtn.setOnAction(e -> handleEditTask(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : new HBox(5, editBtn, deleteBtn));
            }
        });

        tasksTable.setItems(tasks);
    }

    // --- GESTION DE LA NAVIGATION ---

    @FXML
    private void handleShowTasks() {
        // Remet la VBox "tasksView" au centre
        if (mainBorderPane != null && tasksView != null) {
            mainBorderPane.setCenter(tasksView);
        }
    }

    @FXML
    private void handleShowAnalytics() {
        if (mainBorderPane == null) return;

        try {
            // Si la vue n'a pas encore été chargée, on la charge
            if (analyticsView == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/analytics-view.fxml"));
                analyticsView = loader.load();
            }
            // On remplace le centre par la vue Analytics
            mainBorderPane.setCenter(analyticsView);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les Analytics : " + e.getMessage());
        }
    }

    @FXML
    private void handleShowAI() {
        // Même logique si vous voulez intégrer l'IA ici
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ai-view.fxml"));
            mainBorderPane.setCenter(loader.load());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleLogout() {
        System.out.println("Déconnexion...");
        // Logique de retour au login
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
            mainBorderPane.getScene().setRoot(loginView);
            //googleCalendarService.disconnect();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // --- LOGIQUE METIER (Task CRUD) ---

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadTasks();
    }

    private void loadTasks() {
        if (currentUser != null) {
            tasks.setAll(taskService.getTasksByUser(currentUser.getId()));
        }
    }

    @FXML
    private void handleAddTask() {
        // (Votre code existant pour ajouter une tâche...)
        // Assurez-vous d'avoir les vérifications de nullité sur les champs
    }

    private void handleDeleteTask(Task task) {
        if (taskService.deleteTask(task.getId())) {
            tasks.remove(task);
        }
    }

    private void handleEditTask(Task task) {
        // (Votre code existant pour éditer...)
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}