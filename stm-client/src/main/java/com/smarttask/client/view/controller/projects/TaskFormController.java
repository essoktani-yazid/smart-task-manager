package com.smarttask.client.view.controller.projects;

import com.smarttask.client.service.UserService;
import com.smarttask.client.service.TaskService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.User;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.time.LocalDate;
import java.util.List;

public class TaskFormController {

    @FXML private TextField titleField;
    @FXML private TextArea descriptionField;
    @FXML private DatePicker dueDatePicker;
    @FXML private ChoiceBox<Priority> priorityChoice;
    @FXML private ComboBox<Status> statusCombo;
    @FXML private ComboBox<User> assigneeCombo; // Le combo pour choisir l'user
    @FXML private ComboBox<String> recurrenceCombo;
    @FXML private ComboBox<Task> dependencyCombo;

    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;

    private Task task;
    private Stage stage;
    private final String[] projectId = {null}; // Utiliser un tableau pour pouvoir le modifier
    private final UserService userService = new UserService();
    private final TaskService taskService = new TaskService();

    public void setStage(Stage stage) { this.stage = stage; }
    public void setProjectId(String projId) { this.projectId[0] = projId; }

    @FXML
    private void initialize() {
        // Initialisation des listes
        priorityChoice.setItems(FXCollections.observableArrayList(Priority.values()));
        statusCombo.setItems(FXCollections.observableArrayList(Status.values()));
        
        // Récurrence options
        recurrenceCombo.setItems(FXCollections.observableArrayList(
            "Aucune", "Quotidienne", "Hebdomadaire", "Bimensuelle", "Mensuelle", "Annuelle"
        ));
        recurrenceCombo.setValue("Aucune");

        // Initialisation du combo Utilisateurs
        setupUserComboBox();
        
        // Charger les tâches pour dépendances
        setupDependencyCombo();
    }
    
    private void setupDependencyCombo() {
        // Convertisseur pour afficher le titre de la tâche
        dependencyCombo.setConverter(new StringConverter<Task>() {
            @Override
            public String toString(Task task) {
                return task != null ? task.getTitle() : "";
            }
            @Override
            public Task fromString(String string) { return null; }
        });

        // Charger les tâches existantes du projet
        if (projectId[0] != null) {
            final String pId = projectId[0]; // Créer une copie finale pour la lambda
            new Thread(() -> {
                try {
                    List<Task> tasks = taskService.getTasksByProject(pId);
                    // Exclure la tâche actuelle des dépendances
                    if (task != null && task.getId() != null) {
                        tasks = tasks.stream()
                            .filter(t -> !t.getId().equals(task.getId()))
                            .collect(java.util.stream.Collectors.toList());
                    }
                    final List<Task> finalTasks = tasks; // Créer une copie finale pour la deuxième lambda
                    javafx.application.Platform.runLater(() ->
                        dependencyCombo.setItems(FXCollections.observableArrayList(finalTasks))
                    );
                } catch (Exception e) {
                    System.err.println("Erreur chargement dépendances: " + e.getMessage());
                }
            }).start();
        }
    }

    private void setupUserComboBox() {
        // Convertisseur pour afficher le nom de l'utilisateur proprement
        assigneeCombo.setConverter(new StringConverter<User>() {
            @Override
            public String toString(User user) {
                return user != null ? user.getUsername() : "";
            }
            @Override
            public User fromString(String string) { return null; }
        });

        // Chargement des utilisateurs
        try {
            List<User> users = userService.findAll();
            assigneeCombo.setItems(FXCollections.observableArrayList(users));
        } catch (Exception e) {
            System.err.println("Erreur chargement users: " + e.getMessage());
            // Fallback: ajout de l'utilisateur courant si erreur
            User current = SessionManager.getInstance().getCurrentUser();
            if(current != null) assigneeCombo.getItems().add(current);
        }
    }

    public void setTask(Task task) {
        this.task = task;
        if (this.task == null) this.task = new Task();

        titleField.setText(this.task.getTitle() != null ? this.task.getTitle() : "");
        descriptionField.setText(this.task.getDescription() != null ? this.task.getDescription() : "");

        if (this.task.getDueDate() != null)
            dueDatePicker.setValue(this.task.getDueDate().toLocalDate());
        else
            dueDatePicker.setValue(LocalDate.now());

        priorityChoice.setValue(this.task.getPriority() != null ? this.task.getPriority() : Priority.MEDIUM);
        statusCombo.setValue(this.task.getStatus() != null ? this.task.getStatus() : Status.TODO);

        // Sélectionner l'utilisateur assigné dans la liste
        if (this.task.getUser() != null) {
            for (User u : assigneeCombo.getItems()) {
                if (u.getId().equals(this.task.getUser().getId())) {
                    assigneeCombo.setValue(u);
                    break;
                }
            }
        }
    }

    public Task getTask() {
        if (task == null) task = new Task();

        if (titleField.getText() == null || titleField.getText().trim().isEmpty()) {
            showAlert("Erreur", "Le titre est obligatoire.");
            return null;
        }

        task.setTitle(titleField.getText());
        task.setDescription(descriptionField.getText());

        LocalDate d = dueDatePicker.getValue();
        if (d != null) task.setDueDate(d.atStartOfDay());

        task.setPriority(priorityChoice.getValue());
        task.setStatus(statusCombo.getValue());

        // --- PARTIE CRITIQUE CORRIGÉE ---
        User selectedUser = assigneeCombo.getValue();
        if (selectedUser != null) {
            task.setUser(selectedUser); // On affecte l'objet User directement
        } else {
            // Si aucun user sélectionné, on met celui connecté par défaut
            try {
                task.setUser(SessionManager.getInstance().getCurrentUser());
            } catch (Exception e) { /* Ignorer */ }
        }
        
        // Recurrence configuration
        String recurrence = recurrenceCombo.getValue();
        if (recurrence != null && !recurrence.equals("Aucune")) {
            task.setRecurrenceType(recurrence.toUpperCase());
            task.setRecurrenceInterval(1); // Default interval of 1
        } else {
            task.setRecurrenceType("NONE");
            task.setRecurrenceInterval(0);
        }
        
        // Dependency configuration
        Task selectedDependency = dependencyCombo.getValue();
        if (selectedDependency != null) {
            task.setDependentTaskId(selectedDependency.getId());
            task.setDependentTask(selectedDependency);
        }
        // Le projectId reste inchangé (défini dans onAddTask)

        return task;
    }

    @FXML
    private void onSave() {
        if (getTask() != null) {
            if (stage != null) stage.close();
        }
    }

    @FXML
    private void onCancel() {
        task = null;
        if (stage != null) stage.close();
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}