package com.smarttask.client.view.controller;

import com.smarttask.model.User;
import com.smarttask.client.view.controller.projects.ProjectsViewController;
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

    private User currentUser;
    private Button currentActiveButton;

    @FXML
    private void initialize() {
        // Vue par défaut au démarrage
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

            // 2. Extraire et afficher la première lettre pour l'avatar
            if (displayName != null && !displayName.isEmpty()) {
                userInitialLabel.setText(displayName.substring(0, 1).toUpperCase());
            } else {
                userInitialLabel.setText("?");
            }
            
            // Recharger la vue actuelle pour injecter le nouvel utilisateur si nécessaire
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

    @FXML 
    private void showProjects() {
        if (loadView("/fxml/projects/projects-view.fxml")) setActiveButton(projectsButton);
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

            // Injection automatique de l'utilisateur dans les contrôleurs enfants
            Object controller = loader.getController();
            
            if (controller instanceof DashboardController && currentUser != null) {
                ((DashboardController) controller).setCurrentUser(currentUser);
            } else if (controller instanceof ProjectsViewController && currentUser != null) {
                ((ProjectsViewController) controller).setCurrentUser(currentUser);
            }
            // Ajoutez d'autres contrôleurs ici si nécessaire (ProfileController, etc.)

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
}