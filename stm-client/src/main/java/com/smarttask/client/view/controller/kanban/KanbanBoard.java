package com.smarttask.client.view.controller.kanban;

import com.smarttask.model.*;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Consumer;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * KANBAN BOARD - Professional Task Management View
 * FIXED: Proper hover effects and submenu behavior
 * ════════════════════════════════════════════════════════════════════════════════
 */
public class KanbanBoard extends HBox {

    private static final Map<Status, String> COLUMN_COLORS = Map.of(
            Status.TODO, "#6366f1",
            Status.IN_PROGRESS, "#f59e0b",
            Status.BLOCKED, "#ef4444",
            Status.COMPLETED, "#10b981"
    );

    private static final Map<Status, String> COLUMN_TITLES = Map.of(
            Status.TODO, "📋 To Do",
            Status.IN_PROGRESS, "🔄 In Progress",
            Status.BLOCKED, "🚫 Blocked",
            Status.COMPLETED, "✅ Completed"
    );

    private final Map<Status, KanbanColumn> columns = new LinkedHashMap<>();

    private Consumer<Task> onTaskUpdated;
    private Consumer<Task> onTaskClicked;
    private Consumer<Task> onTaskDeleted;
    private User currentUser;
    private Task draggedTask;
    private VBox draggedCard;

    public KanbanBoard() {
        initializeBoard();
    }

    private void initializeBoard() {
        setSpacing(16);
        setPadding(new Insets(20));
        setStyle("-fx-background-color: linear-gradient(to bottom, #f8fafc, #f1f5f9);");

        for (Status status : List.of(Status.TODO, Status.IN_PROGRESS, Status.BLOCKED, Status.COMPLETED)) {
            KanbanColumn column = new KanbanColumn(status, COLUMN_TITLES.get(status), COLUMN_COLORS.get(status));
            columns.put(status, column);
            HBox.setHgrow(column, Priority.ALWAYS);
            getChildren().add(column);
            setupColumnDropTarget(column, status);
        }
    }

    public void loadTasks(List<Task> tasks) {
        columns.values().forEach(KanbanColumn::clearTasks);

        tasks.stream()
                .sorted(Comparator
                        .comparing((Task t) -> getPriorityOrdinal(t.getPriority())).reversed()
                        .thenComparing(t -> t.getDueDate() != null ? t.getDueDate() : java.time.LocalDateTime.MAX))
                .forEach(task -> {
                    Status status = task.getStatus() != null ? task.getStatus() : Status.TODO;
                    KanbanColumn column = columns.get(status);
                    if (column != null) {
                        VBox card = createTaskCard(task);
                        column.addTaskCard(card);
                    }
                });

        columns.values().forEach(KanbanColumn::updateCount);
    }

    private int getPriorityOrdinal(com.smarttask.model.Priority priority) {
        if (priority == null) return 0;
        return switch (priority) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH -> 2;
            case URGENT -> 3;
        };
    }

    private VBox createTaskCard(Task task) {
        VBox card = new VBox(8);
        card.setPadding(new Insets(14, 16, 14, 16));
        card.setMaxWidth(Double.MAX_VALUE);
        card.setUserData(task);

        String priorityColor = getPriorityColor(task.getPriority());
        boolean isCompleted = task.getStatus() == Status.COMPLETED;
        boolean isOverdue = isTaskOverdue(task);

        String cardStyle = String.format(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-width: 0 0 0 4; " +
                        "-fx-border-color: %s; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 8, 0, 0, 2); " +
                        "-fx-cursor: hand;",
                priorityColor
        );
        card.setStyle(cardStyle);

        // Header row
        HBox headerRow = new HBox(6);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        if (isOverdue && !isCompleted) {
            Label overdueBadge = new Label("⚠ Overdue");
            overdueBadge.setStyle(
                    "-fx-font-size: 9; -fx-font-weight: 700; -fx-padding: 3 8; " +
                            "-fx-background-radius: 10; -fx-background-color: #fef2f2; -fx-text-fill: #dc2626;"
            );
            headerRow.getChildren().add(overdueBadge);
        }

        Label priorityBadge = createPriorityBadge(task.getPriority());
        headerRow.getChildren().add(priorityBadge);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        headerRow.getChildren().add(spacer);

        Button menuBtn = createMenuButton(task, card);
        headerRow.getChildren().add(menuBtn);

        // Title
        Label titleLabel = new Label(task.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);

        if (isCompleted) {
            titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #94a3b8; -fx-strikethrough: true;");
        } else {
            titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: 600; -fx-text-fill: #1e293b;");
        }

        // Footer row
        HBox footerRow = new HBox(10);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footerRow.setPadding(new Insets(6, 0, 0, 0));

        if (task.getDueDate() != null) {
            HBox dateBox = new HBox(4);
            dateBox.setAlignment(Pos.CENTER_LEFT);
            Label dateIcon = new Label("📅");
            dateIcon.setStyle("-fx-font-size: 11;");
            Label dateLabel = new Label(formatDueDate(task.getDueDate().toLocalDate()));
            String dateColor = isOverdue && !isCompleted ? "#dc2626" : "#64748b";
            dateLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 500; -fx-text-fill: " + dateColor + ";");
            dateBox.getChildren().addAll(dateIcon, dateLabel);
            footerRow.getChildren().add(dateBox);
        }

        Region footerSpacer = new Region();
        HBox.setHgrow(footerSpacer, Priority.ALWAYS);
        footerRow.getChildren().add(footerSpacer);

        if (task.getUser() != null) {
            StackPane avatar = createUserAvatar(task.getUser());
            footerRow.getChildren().add(avatar);
        }

        card.getChildren().addAll(headerRow, titleLabel, footerRow);

        // Hover effect
        String hoverStyle = String.format(
                "-fx-background-color: #fafbfc; " +
                        "-fx-background-radius: 12; -fx-border-radius: 12; " +
                        "-fx-border-width: 0 0 0 4; -fx-border-color: %s; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 12, 0, 0, 4); -fx-cursor: hand;",
                priorityColor
        );

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(cardStyle));

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && onTaskClicked != null) {
                onTaskClicked.accept(task);
            }
        });

        setupDragSource(card, task);

        return card;
    }

    private Button createMenuButton(Task task, VBox card) {
        Button menuBtn = new Button("•••");
        menuBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; " +
                        "-fx-font-size: 14; -fx-font-weight: bold; " +
                        "-fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;"
        );

        menuBtn.setOnMouseEntered(e -> menuBtn.setStyle(
                "-fx-background-color: #f1f5f9; -fx-text-fill: #475569; " +
                        "-fx-font-size: 14; -fx-font-weight: bold; " +
                        "-fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;"
        ));

        menuBtn.setOnMouseExited(e -> menuBtn.setStyle(
                "-fx-background-color: transparent; -fx-text-fill: #94a3b8; " +
                        "-fx-font-size: 14; -fx-font-weight: bold; " +
                        "-fx-padding: 4 8; -fx-background-radius: 6; -fx-cursor: hand;"
        ));

        menuBtn.setOnAction(e -> showProfessionalContextMenu(menuBtn, task));

        return menuBtn;
    }

    /**
     * ════════════════════════════════════════════════════════════════════════════════
     * FIXED: Professional Context Menu with proper hover and submenu behavior
     * ════════════════════════════════════════════════════════════════════════════════
     */
    private void showProfessionalContextMenu(Node owner, Task task) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 8); " +
                        "-fx-padding: 6;"
        );

        // ════════════════════════════════════════════════════════════════════════════
        // EDIT TASK - Using standard MenuItem with proper styling
        // ════════════════════════════════════════════════════════════════════════════
        MenuItem editItem = new MenuItem("  Edit Task");
        editItem.setStyle("-fx-padding: 10 16;");
        editItem.setOnAction(e -> {
            if (onTaskClicked != null) onTaskClicked.accept(task);
        });

        // ════════════════════════════════════════════════════════════════════════════
        // MOVE TO - Submenu with status options
        // ════════════════════════════════════════════════════════════════════════════
        Menu moveToMenu = new Menu("  Move to");
        moveToMenu.setStyle("-fx-padding: 10 16;");

        for (Status status : List.of(Status.TODO, Status.IN_PROGRESS, Status.BLOCKED, Status.COMPLETED)) {
            if (status != task.getStatus()) {
                MenuItem statusItem = createStatusMenuItem(status, task, menu);
                moveToMenu.getItems().add(statusItem);
            }
        }

        // ════════════════════════════════════════════════════════════════════════════
        // SEPARATOR
        // ════════════════════════════════════════════════════════════════════════════
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // ════════════════════════════════════════════════════════════════════════════
        // DELETE TASK - Red styled
        // ════════════════════════════════════════════════════════════════════════════
        MenuItem deleteItem = new MenuItem("  Delete Task");
        deleteItem.setStyle("-fx-padding: 10 16; -fx-text-fill: #ef4444;");
        deleteItem.setOnAction(e -> showDeleteConfirmation(task));

        menu.getItems().addAll(editItem, moveToMenu, separator, deleteItem);

        // Apply custom styling via CSS lookup after showing
        menu.setOnShowing(e -> {
            Platform.runLater(() -> applyMenuStyling(menu));
        });

        menu.show(owner, Side.BOTTOM, 0, 5);
    }

    /**
     * Apply custom styling to menu items
     */
    private void applyMenuStyling(ContextMenu menu) {
        // Style the menu items using CSS
        menu.getScene().getRoot().setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12;"
        );
    }

    /**
     * Create status menu item with colored dot
     */
    private MenuItem createStatusMenuItem(Status status, Task task, ContextMenu parentMenu) {
        String color = COLUMN_COLORS.get(status);
        String title = getStatusDisplayName(status);

        // Create graphic with colored dot
        HBox graphic = new HBox(8);
        graphic.setAlignment(Pos.CENTER_LEFT);

        Circle dot = new Circle(5);
        dot.setFill(Color.web(color));

        Label label = new Label(title);
        label.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");

        graphic.getChildren().addAll(dot, label);

        MenuItem item = new MenuItem();
        item.setGraphic(graphic);
        item.setStyle("-fx-padding: 8 16;");

        item.setOnAction(e -> {
            parentMenu.hide();
            moveTaskToStatus(task, status);
        });

        return item;
    }

    private String getStatusDisplayName(Status status) {
        return switch (status) {
            case TODO -> "To Do";
            case IN_PROGRESS -> "In Progress";
            case BLOCKED -> "Blocked";
            case COMPLETED -> "Completed";
        };
    }

    private void showDeleteConfirmation(Task task) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Task");
        alert.setHeaderText("Delete \"" + task.getTitle() + "\"?");
        alert.setContentText("This action cannot be undone.");

        // Style the buttons
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        // Get the buttons and style them
        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

        if (okButton != null) {
            okButton.setText("Delete");
            okButton.setStyle(
                    "-fx-background-color: #ef4444; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 8 20; " +
                            "-fx-font-weight: 600; " +
                            "-fx-cursor: hand;"
            );
        }

        if (cancelButton != null) {
            cancelButton.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-text-fill: #374151; " +
                            "-fx-border-color: #d1d5db; " +
                            "-fx-border-radius: 8; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 8 20; " +
                            "-fx-font-weight: 600; " +
                            "-fx-cursor: hand;"
            );
        }

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK && onTaskDeleted != null) {
                onTaskDeleted.accept(task);
                columns.get(task.getStatus()).removeTask(task);
            }
        });
    }

    private void moveTaskToStatus(Task task, Status newStatus) {
        Status oldStatus = task.getStatus();
        task.setStatus(newStatus);

        if (onTaskUpdated != null) {
            onTaskUpdated.accept(task);
        }

        KanbanColumn oldColumn = columns.get(oldStatus);
        KanbanColumn newColumn = columns.get(newStatus);

        if (oldColumn != null) oldColumn.removeTask(task);
        if (newColumn != null) {
            VBox newCard = createTaskCard(task);
            newColumn.addTaskCard(newCard);
        }
    }

    private Label createPriorityBadge(com.smarttask.model.Priority priority) {
        String text = getPriorityText(priority);
        String bgColor = getPriorityBgColor(priority);
        String textColor = getPriorityColor(priority);

        Label badge = new Label(text);
        badge.setStyle(String.format(
                "-fx-font-size: 9; -fx-font-weight: 700; -fx-padding: 3 8; " +
                        "-fx-background-radius: 10; -fx-background-color: %s; -fx-text-fill: %s;",
                bgColor, textColor
        ));
        return badge;
    }

    private StackPane createUserAvatar(User user) {
        StackPane avatar = new StackPane();
        avatar.setMinSize(24, 24);
        avatar.setMaxSize(24, 24);
        avatar.setStyle("-fx-background-color: linear-gradient(to bottom right, #6366f1, #8b5cf6); -fx-background-radius: 12;");

        String initials = user.getUsername() != null && !user.getUsername().isEmpty()
                ? user.getUsername().substring(0, 1).toUpperCase() : "?";

        Label initialsLabel = new Label(initials);
        initialsLabel.setStyle("-fx-text-fill: white; -fx-font-size: 11; -fx-font-weight: 700;");
        avatar.getChildren().add(initialsLabel);

        Tooltip tooltip = new Tooltip(user.getUsername());
        Tooltip.install(avatar, tooltip);

        return avatar;
    }

    private void setupDragSource(VBox card, Task task) {
        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(task.getId());
            db.setContent(content);
            draggedTask = task;
            draggedCard = card;
            card.setOpacity(0.5);
            e.consume();
        });

        card.setOnDragDone(e -> {
            card.setOpacity(1.0);
            draggedTask = null;
            draggedCard = null;
            e.consume();
        });
    }

    private void setupColumnDropTarget(KanbanColumn column, Status status) {
        column.setOnDragOver(e -> {
            if (e.getGestureSource() != column && draggedTask != null) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        column.setOnDragEntered(e -> {
            if (e.getGestureSource() != column && draggedTask != null) {
                column.setDragOverStyle(true);
            }
            e.consume();
        });

        column.setOnDragExited(e -> {
            column.setDragOverStyle(false);
            e.consume();
        });

        column.setOnDragDropped(e -> {
            boolean success = false;
            if (draggedTask != null && draggedTask.getStatus() != status) {
                moveTaskToStatus(draggedTask, status);
                success = true;
            }
            e.setDropCompleted(success);
            e.consume();
        });

        column.setupTaskContainerDrop(status, this::moveTaskToStatus, () -> draggedTask);
    }

    private String getPriorityColor(com.smarttask.model.Priority priority) {
        if (priority == null) return "#94a3b8";
        return switch (priority) {
            case LOW -> "#94a3b8";
            case MEDIUM -> "#3b82f6";
            case HIGH -> "#f97316";
            case URGENT -> "#ef4444";
        };
    }

    private String getPriorityBgColor(com.smarttask.model.Priority priority) {
        if (priority == null) return "#f1f5f9";
        return switch (priority) {
            case LOW -> "#f1f5f9";
            case MEDIUM -> "#eff6ff";
            case HIGH -> "#fff7ed";
            case URGENT -> "#fef2f2";
        };
    }

    private String getPriorityText(com.smarttask.model.Priority priority) {
        if (priority == null) return "Low";
        return switch (priority) {
            case LOW -> "Low";
            case MEDIUM -> "Medium";
            case HIGH -> "High";
            case URGENT -> "Urgent";
        };
    }

    private boolean isTaskOverdue(Task task) {
        if (task.getDueDate() == null) return false;
        return task.getDueDate().toLocalDate().isBefore(LocalDate.now());
    }

    private String formatDueDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        if (date.equals(today)) return "Today";
        if (date.equals(today.plusDays(1))) return "Tomorrow";
        if (date.isBefore(today)) return "Overdue";
        return date.format(DateTimeFormatter.ofPattern("MMM d"));
    }

    public void setOnTaskUpdated(Consumer<Task> callback) { this.onTaskUpdated = callback; }
    public void setOnTaskClicked(Consumer<Task> callback) { this.onTaskClicked = callback; }
    public void setOnTaskDeleted(Consumer<Task> callback) { this.onTaskDeleted = callback; }
    public void setCurrentUser(User user) { this.currentUser = user; }
}