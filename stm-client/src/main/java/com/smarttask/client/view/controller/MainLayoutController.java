package com.smarttask.client.view.controller;

import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

import java.io.IOException;

/**
 * Main controller for the layout with navigation sidebar.
 */
public class MainLayoutController {
    @FXML
    private StackPane contentPane;

    @FXML
    private Label userLabel;

    @FXML
    private Button tasksButton;

    @FXML
    private Button calendarButton;

    @FXML
    private Button aiButton;

    @FXML
    private Button collaborationButton;

    @FXML
    private Button analyticsButton;

    @FXML
    private Button logoutButton;

    private User currentUser;
    private Button currentActiveButton;

    @FXML
    private void initialize() {
        currentActiveButton = tasksButton;
        tasksButton.getStyleClass().add("active");
        
        showTasks();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            userLabel.setText(user.getUsername());
        }
    }

    @FXML
    private void showTasks() {
        setActiveButton(tasksButton);
        loadView("/fxml/dashboard.fxml");
    }

    @FXML
    private void showCalendar() {
        setActiveButton(calendarButton);
        loadView("/fxml/calendar-view.fxml");
    }

    @FXML
    private void showAI() {
        setActiveButton(aiButton);
        loadView("/fxml/ai-view.fxml");
    }

    @FXML
    private void showCollaboration() {
        setActiveButton(collaborationButton);
        loadView("/fxml/collaboration-view.fxml");
    }

    @FXML
    private void showAnalytics() {
        setActiveButton(analyticsButton);
        loadView("/fxml/analytics-view.fxml");
    }

    @FXML
    private void handleLogout() {
        System.out.println("Déconnexion...");
    }

    private void loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Si c'est le dashboard, on passe l'utilisateur au contrôleur
            if (fxmlPath.equals("/fxml/dashboard.fxml")) {
                Object controller = loader.getController();
                if (controller instanceof DashboardController && currentUser != null) {
                    ((DashboardController) controller).setCurrentUser(currentUser);
                }
            }

            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue : " + fxmlPath);
            e.printStackTrace();
        }
    }

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
}