package com.smarttask.client.view.controller;

import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

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

    @FXML
    private Button profileButton;

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
    private void showProfile() {
        setActiveButton(profileButton);
        loadView("/fxml/profile.fxml");
    }

    @FXML
    private void handleLogout() {
        try {
            // Load login page
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();

            // Get current stage
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            
            // Create new scene with login page
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            // Set scene and adjust window size for login page
            stage.setScene(scene);
            stage.setTitle("SmartTaskManager - Connexion");
            stage.setWidth(1200);
            stage.setHeight(700);
            stage.centerOnScreen();
            
            // Clear current user
            currentUser = null;
            
            System.out.println("Déconnexion réussie");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Erreur lors de la déconnexion");
        }
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
            } else if (fxmlPath.equals("/fxml/profile.fxml")) {
                Object controller = loader.getController();
                if (controller instanceof ProfileController && currentUser != null) {
                    ((ProfileController) controller).setCurrentUser(currentUser);
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