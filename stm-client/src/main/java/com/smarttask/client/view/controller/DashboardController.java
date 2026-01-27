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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the dashboard view (task list).
 */
public class DashboardController {

    @FXML
    private TextField titleField;

    @FXML
    private TextField descriptionField;

    @FXML
    private ComboBox<Priority> priorityComboBox;

    @FXML
    private DatePicker dueDatePicker;

    @FXML
    private Button addTaskButton;

    @FXML
    private Button refreshButton;

    @FXML
    private Button refreshButtonHeader;

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
        // Initialize priority ComboBox
        priorityComboBox.setItems(FXCollections.observableArrayList(Priority.values()));
        priorityComboBox.setValue(Priority.MEDIUM);

        // Configure table columns
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        // Format priority column
        priorityColumn.setCellValueFactory(new PropertyValueFactory<>("priority"));
        priorityColumn.setCellFactory(column -> new TableCell<Task, Priority>() {
            @Override
            protected void updateItem(Priority priority, boolean empty) {
                super.updateItem(priority, empty);
                if (empty || priority == null) {
                    setText(null);
                } else {
                    setText(priority.getLabel());
                }
            }
        });
        
        // Format status column
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setCellFactory(column -> new TableCell<Task, Status>() {
            @Override
            protected void updateItem(Status status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setText(null);
                } else {
                    setText(status.getLabel());
                }
            }
        });
        
        // Format due date column
        dueDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime dueDate = cellData.getValue().getDueDate();
            return new javafx.beans.property.SimpleStringProperty(
                    dueDate != null ? dueDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "Not set"
            );
        });

        // Actions column
        actionsColumn.setCellFactory(column -> new TableCell<Task, String>() {
            private final Button deleteButton = new Button("Delete");
            private final Button editButton = new Button("Edit");

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
        loadTasks();
    }

    @FXML
    private void handleAddTask() {
        String title = titleField.getText();
        String description = descriptionField.getText();
        Priority priority = priorityComboBox.getValue();
        LocalDate dueDate = dueDatePicker.getValue();

        if (title.isEmpty()) {
            showAlert("Error", "Title is required");
            return;
        }

        Task task = taskService.createTask(title, description, priority, currentUser);
        if (task != null) {
            // Set due date if it was selected
            if (dueDate != null) {
                task.setDueDate(dueDate.atStartOfDay());
                // Update the task with the due date
                Task updatedTask = taskService.updateTask(task);
                if (updatedTask != null) {
                    task = updatedTask;
                }
            }
            tasks.add(task);
            titleField.clear();
            descriptionField.clear();
            dueDatePicker.setValue(null);
            showSuccessMessage("Task created successfully");
        } else {
            showAlert("Error", "Unable to create task");
        }
    }

    @FXML
    private void handleDeleteTask(Task task) {
        // Ask for confirmation before deleting
        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Delete Confirmation");
        confirmAlert.setHeaderText("Delete Task");
        confirmAlert.setContentText("Are you sure you want to delete the task \"" + task.getTitle() + "\"?");

        confirmAlert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = taskService.deleteTask(task.getId());
                if (deleted) {
                    tasks.remove(task);
                    showSuccessMessage("Task deleted successfully");
                } else {
                    showAlert("Error", "Unable to delete task");
                }
            }
        });
    }

    @FXML
    private void handleEditTask(Task task) {
        // Open dialog to edit task
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Edit Task");
        dialog.setHeaderText("Modify task information");

        TextField titleField = new TextField(task.getTitle());
        TextField descriptionField = new TextField(task.getDescription());
        ComboBox<Priority> priorityCombo = new ComboBox<>(FXCollections.observableArrayList(Priority.values()));
        priorityCombo.setValue(task.getPriority());
        ComboBox<Status> statusCombo = new ComboBox<>(FXCollections.observableArrayList(Status.values()));
        statusCombo.setValue(task.getStatus());
        
        // DatePicker for due date
        DatePicker dueDatePicker = new DatePicker();
        if (task.getDueDate() != null) {
            dueDatePicker.setValue(task.getDueDate().toLocalDate());
        }

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(10);
        content.getChildren().addAll(
                new Label("Title:"), titleField,
                new Label("Description:"), descriptionField,
                new Label("Priority:"), priorityCombo,
                new Label("Status:"), statusCombo,
                new Label("Due Date:"), dueDatePicker
        );
        dialog.getDialogPane().setContent(content);

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                task.setTitle(titleField.getText());
                task.setDescription(descriptionField.getText());
                task.setPriority(priorityCombo.getValue());
                task.setStatus(statusCombo.getValue());
                // Update due date
                if (dueDatePicker.getValue() != null) {
                    task.setDueDate(dueDatePicker.getValue().atStartOfDay());
                } else {
                    task.setDueDate(null);
                }
                return task;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedTask -> {
            Task savedTask = taskService.updateTask(updatedTask);
            if (savedTask != null) {
                int index = tasks.indexOf(task);
                tasks.set(index, savedTask);
                showSuccessMessage("Task updated successfully");
            } else {
                showAlert("Error", "Unable to update task");
            }
        });
    }

    @FXML
    private void handleRefresh() {
        loadTasks();
        showSuccessMessage("Task list refreshed");
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

    private void showSuccessMessage(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

