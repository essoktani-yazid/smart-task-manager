package com.smarttask.client.view.controller.kanban;

import com.smarttask.model.Status;
import com.smarttask.model.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;

import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * ════════════════════════════════════════════════════════════════════════════════
 * KANBAN COLUMN - Single column in the Kanban board
 * ════════════════════════════════════════════════════════════════════════════════
 */
public class KanbanColumn extends VBox {

    private final Status status;
    private final String title;
    private final String color;
    private final Label countLabel;
    private final VBox taskContainer;
    private final ScrollPane scrollPane;

    private String normalStyle;
    private String dragOverStyle;

    public KanbanColumn(Status status, String title, String color) {
        this.status = status;
        this.title = title;
        this.color = color;

        // Column styling
        setMinWidth(280);
        setPrefWidth(300);
        setMaxWidth(350);
        setSpacing(0);

        normalStyle = String.format(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 16; " +
                        "-fx-border-radius: 16; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 10, 0, 0, 4);"
        );

        dragOverStyle = String.format(
                "-fx-background-color: %s10; " +
                        "-fx-background-radius: 16; " +
                        "-fx-border-radius: 16; " +
                        "-fx-border-color: %s; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-style: dashed; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);",
                color, color
        );

        setStyle(normalStyle);

        // ════════════════════════════════════════════════════════════════════════════
        // HEADER
        // ════════════════════════════════════════════════════════════════════════════
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 16, 12, 16));
        header.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: #f1f5f9; " +
                        "-fx-border-width: 0 0 1 0;"
        );

        // Color indicator
        Region colorIndicator = new Region();
        colorIndicator.setMinSize(4, 20);
        colorIndicator.setMaxSize(4, 20);
        colorIndicator.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 2;",
                color
        ));

        // Title
        Label titleLabel = new Label(title);
        titleLabel.setStyle(
                "-fx-font-size: 14; " +
                        "-fx-font-weight: 700; " +
                        "-fx-text-fill: #1e293b;"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Count badge
        countLabel = new Label("0");
        countLabel.setStyle(String.format(
                "-fx-font-size: 12; " +
                        "-fx-font-weight: 700; " +
                        "-fx-padding: 4 10; " +
                        "-fx-background-radius: 12; " +
                        "-fx-background-color: %s20; " +
                        "-fx-text-fill: %s;",
                color, color
        ));

        header.getChildren().addAll(colorIndicator, titleLabel, spacer, countLabel);

        // ════════════════════════════════════════════════════════════════════════════
        // TASK CONTAINER (scrollable)
        // ════════════════════════════════════════════════════════════════════════════
        taskContainer = new VBox(10);
        taskContainer.setPadding(new Insets(12, 12, 12, 12));
        taskContainer.setStyle("-fx-background-color: transparent;");

        scrollPane = new ScrollPane(taskContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-background: transparent; " +
                        "-fx-border-width: 0;"
        );
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Style scrollbar
        scrollPane.getStyleClass().add("kanban-scroll");

        getChildren().addAll(header, scrollPane);
    }

    /**
     * Add a task card to this column
     */
    public void addTaskCard(VBox card) {
        taskContainer.getChildren().add(card);
        updateCount();
    }

    /**
     * Remove a task from this column
     */
    public void removeTask(Task task) {
        taskContainer.getChildren().removeIf(node -> {
            if (node instanceof VBox) {
                Object userData = node.getUserData();
                if (userData instanceof Task t) {
                    return t.getId().equals(task.getId());
                }
            }
            return false;
        });
        updateCount();
    }

    /**
     * Clear all tasks from this column
     */
    public void clearTasks() {
        taskContainer.getChildren().clear();
        updateCount();
    }

    /**
     * Update the task count badge
     */
    public void updateCount() {
        countLabel.setText(String.valueOf(taskContainer.getChildren().size()));
    }

    /**
     * Set drag over visual style
     */
    public void setDragOverStyle(boolean isDragOver) {
        setStyle(isDragOver ? dragOverStyle : normalStyle);
    }

    /**
     * Setup drop target on the task container
     */
    public void setupTaskContainerDrop(Status status, BiConsumer<Task, Status> moveTask, Supplier<Task> getDraggedTask) {
        taskContainer.setOnDragOver(e -> {
            Task draggedTask = getDraggedTask.get();
            if (e.getGestureSource() != taskContainer && draggedTask != null) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        taskContainer.setOnDragEntered(e -> {
            Task draggedTask = getDraggedTask.get();
            if (e.getGestureSource() != taskContainer && draggedTask != null) {
                setDragOverStyle(true);
            }
            e.consume();
        });

        taskContainer.setOnDragExited(e -> {
            setDragOverStyle(false);
            e.consume();
        });

        taskContainer.setOnDragDropped(e -> {
            Task draggedTask = getDraggedTask.get();
            boolean success = false;

            if (draggedTask != null && draggedTask.getStatus() != status) {
                moveTask.accept(draggedTask, status);
                success = true;
            }

            e.setDropCompleted(success);
            e.consume();
        });

        // Also handle drops on scrollpane
        scrollPane.setOnDragOver(e -> {
            Task draggedTask = getDraggedTask.get();
            if (e.getGestureSource() != scrollPane && draggedTask != null) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        scrollPane.setOnDragEntered(e -> {
            Task draggedTask = getDraggedTask.get();
            if (e.getGestureSource() != scrollPane && draggedTask != null) {
                setDragOverStyle(true);
            }
            e.consume();
        });

        scrollPane.setOnDragExited(e -> {
            setDragOverStyle(false);
            e.consume();
        });

        scrollPane.setOnDragDropped(e -> {
            Task draggedTask = getDraggedTask.get();
            boolean success = false;

            if (draggedTask != null && draggedTask.getStatus() != status) {
                moveTask.accept(draggedTask, status);
                success = true;
            }

            e.setDropCompleted(success);
            e.consume();
        });
    }

    public Status getStatus() {
        return status;
    }
}
