package com.smarttask.client.view.controller;

import com.smarttask.client.view.controller.teams.TeamsController;
import com.smarttask.model.Project;
import com.smarttask.model.User;
import com.google.gson.JsonObject;
import com.smarttask.client.view.controller.projects.ProjectViewController;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.shape.Circle;
import javafx.scene.paint.Color;

import java.io.IOException;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;


public class MainLayoutController {

    private final com.google.gson.Gson gson = com.smarttask.client.util.GsonUtils.getGson();
    private class GlobalWebSocketListener implements WebSocket.Listener {
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            String message = data.toString();
            JsonObject json = gson.fromJson(message, JsonObject.class);

            String type = json.get("type").getAsString();
            
            Platform.runLater(() -> {
                if ("NOTIFICATION".equals(type)) {
                    handleNewNotification(json.get("payload").getAsJsonObject());
                }
            });
            
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
    }

    @FXML private StackPane contentPane;
    
    // --- User Profile Components ---
    @FXML private Label userLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label userInitialLabel; // Label pour l'initiale dans le cercle

    // --- Navigation Buttons ---
    @FXML private Button tasksButton;
    @FXML private Button calendarButton;
    @FXML private Button aiButton;
    @FXML private Button teamsButton;
    @FXML private Button projectsButton;
    @FXML private Button analyticsButton;
    @FXML private Button logoutButton;
    @FXML private Button profileButton;
    @FXML private Button notificationButton;

    @FXML private Label notificationBadge;

    private User currentUser;
    private Button currentActiveButton;
    private static MainLayoutController instance;
    private Object currentController;
    private WebSocket webSocket;

    private int notificationCount = 0;

    private void handleNewNotification(JsonObject payload) {
        Platform.runLater(() -> {
            notificationCount++;
            notificationBadge.setText(String.valueOf(notificationCount));
            notificationBadge.setVisible(true);
            shakeElement(notificationButton);
            shakeElement(notificationBadge);

            if (currentController instanceof NotificationsController) {
                ((NotificationsController) currentController).refresh();

                notificationCount = 0;
                notificationBadge.setVisible(false);
            }
        });
    }

    private void shakeElement(javafx.scene.Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(0);
        tt.setByX(4);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);

        if (!notificationButton.getStyleClass().contains("has-notification")) {
            notificationButton.getStyleClass().add("has-notification");
        }

        tt.play();
    }

    public static MainLayoutController getInstance() {
        return instance;
    }

    @FXML
    private void initialize() {
        instance = this;

        setActiveButton(tasksButton);
        showTasks();
    }

    /**
     * Définit l'utilisateur actuel et met à jour les éléments de la barre latérale.
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            // 1. Déterminer le nom à afficher (Prénom + Nom ou Username)
            String displayName = user.getUsername();
            if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                displayName = user.getFirstName();
                if (user.getLastName() != null && !user.getLastName().isEmpty()) {
                    displayName += " " + user.getLastName();
                }
            }
            
            userLabel.setText(displayName);
            userEmailLabel.setText(user.getEmail());

            if (displayName != null && !displayName.isEmpty()) {
                userInitialLabel.setText(displayName.substring(0, 1).toUpperCase());
            } else {
                userInitialLabel.setText("?");
            }

            connectWebSocket();
            showTasks();
        }
    }

    // --- Navigation Actions ---

    @FXML 
    private void showTasks() {
        if (loadView("/fxml/dashboard.fxml")) setActiveButton(tasksButton);
    }

    @FXML 
    private void showCalendar() {
        if (loadView("/fxml/calendar-view.fxml")) setActiveButton(calendarButton);
    }

    @FXML 
    private void showAI() {
        if (loadView("/fxml/ai-view.fxml")) setActiveButton(aiButton);
    }

    @FXML 
    private void showTeams() {
        if (loadView("/fxml/teams/root-view.fxml")) setActiveButton(teamsButton);
    }

    @FXML private void showProjects() {
        if(loadView("/fxml/projects/root-view.fxml")) setActiveButton(projectsButton);
    }

    @FXML 
    private void showAnalytics() {
        if (loadView("/fxml/analytics-view.fxml")) setActiveButton(analyticsButton);
    }

    @FXML 
    private void showProfile() {
        // On charge la vue profil, mais on peut ne pas changer le bouton actif 
        // ou avoir un bouton dédié si vous le souhaitez.
        loadView("/fxml/profile.fxml");
    }

    @FXML
    private void showNotifications() {
        notificationCount = 0;
        notificationButton.getStyleClass().remove("has-notification");
        notificationBadge.setVisible(false);

        if (loadView("/fxml/notifications.fxml")) setActiveButton(notificationButton);
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            Scene scene = new Scene(root);
            
            stage.setScene(scene);
            stage.setTitle("SmartTask.AI - Login");
            stage.centerOnScreen();
            
            this.currentUser = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Charge une vue FXML dans le contentPane et injecte l'utilisateur courant.
     */
    private boolean loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            this.currentController = loader.getController();

            Object controller = loader.getController();
            
            if (controller instanceof DashboardController && currentUser != null) {
                ((DashboardController) controller).setCurrentUser(currentUser);
            } else if (controller instanceof ProfileController && currentUser != null) {
                ((ProfileController) controller).setCurrentUser(currentUser);
            } else if (controller instanceof ProjectViewController && currentUser != null) {
                ((ProjectViewController) controller).setCurrentUser(currentUser);
            } else if (controller instanceof NotificationsController && currentUser != null) {
                ((NotificationsController) controller).setCurrentUser(currentUser);
            } else if (controller instanceof CalendarController && currentUser != null) {
                ((CalendarController) controller).setCurrentUser(currentUser);
            }

            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
            return true;
        } catch (IOException e) {
            System.err.println("Error loading view: " + fxmlPath);
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gère l'aspect visuel des boutons (style .active défini dans le CSS).
     */
    private void setActiveButton(Button button) {
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active");
        }
        
        if (button != null) {
            if (!button.getStyleClass().contains("active")) {
                button.getStyleClass().add("active");
            }
            currentActiveButton = button;
        }
    }

    public void navigateTo(String fxmlPath, Object data) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();
            
            Object controller = loader.getController();

            this.currentController = loader.getController();

            if (controller instanceof ProjectViewController) {
                ProjectViewController pvc = (ProjectViewController) controller;
                pvc.setCurrentUser(currentUser);
                if (data instanceof Project) pvc.setProject((Project) data);
            } else if (controller instanceof ProfileController) {
                ProfileController pfc = (ProfileController) controller;
                pfc.setCurrentUser(currentUser);
            } else if (controller instanceof DashboardController) {
                DashboardController pvc = (DashboardController) controller;
                pvc.setCurrentUser(currentUser);
            } else if (currentController instanceof NotificationsController && currentUser != null) {
                ((NotificationsController) currentController).setCurrentUser(currentUser);
            }

            contentPane.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void connectWebSocket() {
        java.net.http.HttpClient.newHttpClient()
        .newWebSocketBuilder()
        .buildAsync(
            java.net.URI.create("ws://localhost:8887"),
            new WebSocket.Listener() {

                @Override
                public void onOpen(WebSocket webSocket) {
                    MainLayoutController.this.webSocket = webSocket;

                    System.out.println("WebSocket connected.");

                    if (currentUser != null) {
                        String authMessage = "AUTH:" + currentUser.getId();
                        webSocket.sendText(authMessage, true);
                        System.out.println("AUTH message sent: " + authMessage);
                    }

                    WebSocket.Listener.super.onOpen(webSocket);
                }

                @Override
                public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                    return new GlobalWebSocketListener().onText(webSocket, data, last);
                }
            }
        );
    }
}

