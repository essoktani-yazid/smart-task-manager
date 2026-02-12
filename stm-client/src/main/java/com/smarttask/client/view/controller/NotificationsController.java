package com.smarttask.client.view.controller;

import com.smarttask.client.service.NotificationService;
import com.smarttask.model.Notification;
import com.smarttask.model.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.List;

public class NotificationsController {

    @FXML private ListView<Notification> notificationsListView;
    
    private User currentUser;
    private final NotificationService notificationService = new NotificationService();

    public void setCurrentUser(User user) {
        this.currentUser = user;
        loadNotifications();
    }

    private void loadNotifications() {
        if (currentUser == null) return;

        new Thread(() -> {
            List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId());
            Platform.runLater(() -> {
                if (notifications != null) {
                    notificationsListView.setItems(FXCollections.observableArrayList(notifications));
                    setupListView();
                }
            });
        }).start();
    }

    private void setupListView() {
        notificationsListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Notification notif, boolean empty) {
                super.updateItem(notif, empty);
                if (empty || notif == null) {
                    setGraphic(null);
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    renderNotificationCell(this, notif);
                }
            }
        });
    }

    private void renderNotificationCell(ListCell<Notification> cell, Notification notif) {
        VBox container = new VBox(5);
        container.setPadding(new javafx.geometry.Insets(10));
        
        // Style de la carte
        String bg = notif.getIsRead() ? "white" : "#EEF2FF";
        container.setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 10; -fx-border-color: #E2E8F0; -fx-border-radius: 10;");

        HBox header = new HBox(10);
        Label title = new Label(notif.getTitle());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1E293B;");
        
        // Indicateur de non-lu
        if (!notif.getIsRead()) {
            Circle dot = new Circle(4, Color.web("#6366F1"));
            header.getChildren().add(dot);
        }
        header.getChildren().add(title);

        Label message = new Label(notif.getMessage());
        message.setWrapText(true);
        message.setStyle("-fx-text-fill: #64748B;");

        HBox actions = new HBox(10);
        actions.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #EF4444; -fx-font-size: 11; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDelete(notif));

        if (!notif.getIsRead()) {
            Button readBtn = new Button("Marquer comme lu");
            readBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #6366F1; -fx-font-size: 11; -fx-cursor: hand;");
            readBtn.setOnAction(e -> handleMarkRead(notif));
            actions.getChildren().add(readBtn);
        }
        
        actions.getChildren().add(deleteBtn);
        container.getChildren().addAll(header, message, actions);
        cell.setGraphic(container);
    }

    private void handleMarkRead(Notification notif) {
        if (notificationService.markAsRead(notif.getId())) {
            loadNotifications();
        }
    }

    @FXML
    private void handleMarkAllAsRead() {
        if (currentUser != null && notificationService.markAllAsRead(currentUser.getId())) {
            loadNotifications();
        }
    }

    private void handleDelete(Notification notif) {
        if (notificationService.deleteNotification(notif.getId())) {
            loadNotifications();
        }
    }

    public void refresh() {
        loadNotifications();
    }
}