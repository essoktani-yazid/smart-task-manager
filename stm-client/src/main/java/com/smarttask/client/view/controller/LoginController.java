package com.smarttask.client.view.controller;

import com.smarttask.client.service.AuthService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.User;
import javafx.animation.*;
import javafx.concurrent.Task; // Import nécessaire pour le Thread
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane; // Import StackPane
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.io.IOException;

public class LoginController {

    @FXML private TextField loginUsernameField, registerUsernameField, registerEmailField;
    @FXML private PasswordField loginPasswordField, registerPasswordField;
    @FXML private Label loginErrorLabel, registerErrorLabel;
    @FXML private VBox loginForm, registerForm;
    @FXML private Button loginTab, registerTab, loginButton, registerButton;
    @FXML private ImageView brandingImage;
    @FXML private HBox mainCard;
    
    // NOUVEAU : Le spinner
    @FXML private ProgressIndicator loadingSpinner;
    @FXML private StackPane formContainer; // Pour désactiver l'interaction

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        mainCard.setOpacity(1.0);
        mainCard.setScaleX(1.0);
        mainCard.setScaleY(1.0);
        
        // Cacher le spinner au démarrage
        loadingSpinner.setVisible(false);
    }

    // ... (Gardez switchToLogin, switchToRegister, animateFormSwitch, updateTabUI tels quels) ...

    @FXML
    private void handleLogin() {
        String username = loginUsernameField.getText();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError(loginErrorLabel, "Veuillez remplir tous les champs");
            return;
        }

        // 1. Activer le chargement
        toggleLoading(true);

        // 2. Créer une Tâche de fond (Thread)
        Task<User> loginTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                // Cette partie s'exécute en arrière-plan
                // Thread.sleep(1000); // Décommentez pour tester l'effet visuel si c'est trop rapide
                return authService.login(username, password);
            }
        };

        // 3. Succès
        loginTask.setOnSucceeded(e -> {
            toggleLoading(false);
            User user = loginTask.getValue();
            if (user != null) {
                SessionManager.getInstance().setCurrentUser(user);
                navigateToDashboard(user);
            } else {
                showError(loginErrorLabel, "Identifiants incorrects");
            }
        });

        // 4. Échec (Erreur technique)
        loginTask.setOnFailed(e -> {
            toggleLoading(false);
            Throwable error = loginTask.getException();
            showError(loginErrorLabel, "Erreur connexion: " + error.getMessage());
            error.printStackTrace();
        });

        // 5. Lancer le thread
        new Thread(loginTask).start();
    }

    @FXML
    private void handleRegister() {
        String username = registerUsernameField.getText();
        String email = registerEmailField.getText();
        String password = registerPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError(registerErrorLabel, "Veuillez tout remplir");
            return;
        }

        toggleLoading(true);

        Task<User> registerTask = new Task<>() {
            @Override
            protected User call() throws Exception {
                return authService.register(username, password, email);
            }
        };

        registerTask.setOnSucceeded(e -> {
            toggleLoading(false);
            User user = registerTask.getValue();
            if (user != null) {
                navigateToDashboard(user);
            }
        });

        registerTask.setOnFailed(e -> {
            toggleLoading(false);
            showError(registerErrorLabel, "Erreur: " + registerTask.getException().getMessage());
        });

        new Thread(registerTask).start();
    }

    // Méthode utilitaire pour gérer l'état de chargement
    private void toggleLoading(boolean isLoading) {
        loadingSpinner.setVisible(isLoading);
        formContainer.setDisable(isLoading); // Empêche de cliquer pendant le chargement
        loginButton.setDisable(isLoading);
        registerButton.setDisable(isLoading);
        clearErrors();
    }

    // ... (Gardez navigateToDashboard, showError, clearErrors tels quels) ...
    
    // Ajoutez ceci pour switchToLogin/Register pour s'assurer que c'est propre
    @FXML
    private void switchToLogin() {
        if (loginForm.isVisible()) return;
        animateFormSwitch(registerForm, loginForm);
        updateTabUI(loginTab, registerTab);
    }
    
    @FXML
    private void switchToRegister() {
        if (registerForm.isVisible()) return;
        animateFormSwitch(loginForm, registerForm);
        updateTabUI(registerTab, loginTab);
    }

    private void animateFormSwitch(VBox toHide, VBox toShow) {
        FadeTransition fadeOut = new FadeTransition(Duration.millis(200), toHide);
        fadeOut.setFromValue(1.0); fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            toHide.setVisible(false);
            toHide.setManaged(false);
            toShow.setVisible(true);
            toShow.setManaged(true);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(300), toShow);
            fadeIn.setFromValue(0.0); fadeIn.setToValue(1.0);
            fadeIn.play();
        });
        fadeOut.play();
    }
    
    private void updateTabUI(Button active, Button inactive) {
        active.getStyleClass().add("tab-active");
        inactive.getStyleClass().remove("tab-active");
        clearErrors();
    }
    
    private void navigateToDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-layout.fxml"));
            Parent root = loader.load();
            
            // Injecter l'utilisateur dans le contrôleur principal
            MainLayoutController controller = loader.getController();
            controller.setCurrentUser(user);
            
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
        } catch (IOException e) { e.printStackTrace(); }
    }

    private void showError(Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        TranslateTransition shake = new TranslateTransition(Duration.millis(50), label);
        shake.setByX(5);
        shake.setAutoReverse(true);
        shake.setCycleCount(4);
        shake.play();
    }

    private void clearErrors() {
        loginErrorLabel.setVisible(false);
        registerErrorLabel.setVisible(false);
    }
}