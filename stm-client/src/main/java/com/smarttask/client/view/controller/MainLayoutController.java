package com.smarttask.client.view.controller;

import com.smarttask.client.view.controller.teams.TeamsController;
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

public class MainLayoutController {
    @FXML private StackPane contentPane;
    @FXML private Label userLabel;
    @FXML private Label userEmailLabel;
    
    // Boutons de navigation
    @FXML private Button tasksButton;
    @FXML private Button calendarButton;
    @FXML private Button aiButton;
    @FXML private Button teamsButton;
    @FXML private Button projectsButton;
    @FXML private Button analyticsButton;
    @FXML private Button logoutButton;
    @FXML private Button profileButton;

    private User currentUser;
    private Button currentActiveButton;

    @FXML
    private void initialize() {
        // Définir le bouton par défaut
        setActiveButton(tasksButton);
        showTasks();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user != null) {
            String name = (user.getFirstName() != null && user.getLastName() != null) 
                          ? user.getFirstName() + " " + user.getLastName() 
                          : user.getUsername();
            userLabel.setText(name);
            userEmailLabel.setText(user.getEmail());
        }
    }

    // --- Navigation ---

    @FXML private void showTasks() {
        if(loadView("/fxml/dashboard.fxml")) setActiveButton(tasksButton);
    }

    @FXML private void showCalendar() {
        if(loadView("/fxml/calendar-view.fxml")) setActiveButton(calendarButton);
    }

    @FXML private void showAI() {
        if(loadView("/fxml/ai-view.fxml")) setActiveButton(aiButton);
    }

    @FXML private void showTeams() {
        if(loadView("/fxml/teams/root-view.fxml")) setActiveButton(teamsButton);
    }

	@FXML private void showProjects() {
        if(loadView("/fxml/projects/root-view.fxml")) setActiveButton(projectsButton);
    }

    @FXML private void showAnalytics() {
        if(loadView("/fxml/analytics-view.fxml")) setActiveButton(analyticsButton);
    }

    @FXML private void showProfile() {
        if(loadView("/fxml/profile.fxml")) setActiveButton(profileButton); // Optionnel: garder le dernier bouton actif ou non
    }

    @FXML
    private void handleLogout() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            
            Scene scene = new Scene(root);
            // Assurez-vous que le CSS login est chargé si nécessaire
            // scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("SmartTaskManager - Connexion");
            stage.centerOnScreen();
            
            currentUser = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean loadView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            // Injection de l'utilisateur dans les contrôleurs enfants
            Object controller = loader.getController();
            if (controller instanceof DashboardController && currentUser != null) {
                ((DashboardController) controller).setCurrentUser(currentUser);
            } else if (controller instanceof ProfileController && currentUser != null) {
                ((ProfileController) controller).setCurrentUser(currentUser);
            } else if (controller instanceof TeamsController && currentUser != null) {
                ((TeamsController) controller).setCurrentUser(currentUser);
            }

            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
            return true;
        } catch (IOException e) {
            System.err.println("Erreur chargement vue: " + fxmlPath);
            e.printStackTrace();
            return false;
        }
    }

    private void setActiveButton(Button button) {
        // Retirer la classe 'active' de l'ancien bouton
        if (currentActiveButton != null) {
            currentActiveButton.getStyleClass().remove("active");
        }
        
        // Ajouter la classe 'active' au nouveau bouton
        if (button != null) {
            if (!button.getStyleClass().contains("active")) {
                button.getStyleClass().add("active");
            }
            currentActiveButton = button;
        }
    }
}
