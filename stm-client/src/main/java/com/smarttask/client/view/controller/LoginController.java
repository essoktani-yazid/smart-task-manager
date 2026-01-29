package com.smarttask.client.view.controller;

import com.smarttask.client.service.AuthService;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Contrôleur pour la vue de connexion/inscription.
 */
public class LoginController {
    // Login form fields
    @FXML
    private TextField loginUsernameField;

    @FXML
    private PasswordField loginPasswordField;

    @FXML
    private Label loginErrorLabel;

    // Register form fields
    @FXML
    private TextField registerUsernameField;

    @FXML
    private TextField registerEmailField;

    @FXML
    private PasswordField registerPasswordField;

    @FXML
    private Label registerErrorLabel;

    // Forms
    @FXML
    private VBox loginForm;

    @FXML
    private VBox registerForm;

    // Tab buttons
    @FXML
    private Button loginTab;

    @FXML
    private Button registerTab;

    @FXML
    private Button loginButton;

    @FXML
    private Button registerButton;

    private final AuthService authService = new AuthService();

    @FXML
    private void initialize() {
        // Show login form by default
        showLoginForm();
    }

    @FXML
    private void switchToLogin() {
        showLoginForm();
    }

    @FXML
    private void switchToRegister() {
        showRegisterForm();
    }

    private void showLoginForm() {
        loginForm.setVisible(true);
        loginForm.setManaged(true);
        registerForm.setVisible(false);
        registerForm.setManaged(false);

        loginTab.getStyleClass().add("tab-active");
        registerTab.getStyleClass().remove("tab-active");

        clearErrors();
    }

    private void showRegisterForm() {
        loginForm.setVisible(false);
        loginForm.setManaged(false);
        registerForm.setVisible(true);
        registerForm.setManaged(true);

        registerTab.getStyleClass().add("tab-active");
        loginTab.getStyleClass().remove("tab-active");

        clearErrors();
    }

    @FXML
    private void handleLogin() {
        String username = loginUsernameField.getText();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showLoginError("Veuillez remplir tous les champs");
            return;
        }

        User user = authService.login(username, password);
        if (user != null) {
            navigateToDashboard(user);
        } else {
            showLoginError("Nom d'utilisateur ou mot de passe incorrect");
        }
    }

    @FXML
    private void handleRegister() {
        String username = registerUsernameField.getText();
        String email = registerEmailField.getText();
        String password = registerPasswordField.getText();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showRegisterError("Veuillez remplir tous les champs");
            return;
        }

        if (!email.contains("@")) {
            showRegisterError("Veuillez entrer une adresse email valide");
            return;
        }

        User user = authService.register(username, password, email);
        if (user != null) {
            navigateToDashboard(user);
        } else {
            showRegisterError("Erreur lors de l'inscription. Le nom d'utilisateur existe peut-être déjà.");
        }
    }

    private void navigateToDashboard(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-layout.fxml"));
            Parent root = loader.load();

            MainLayoutController mainController = loader.getController();
            mainController.setCurrentUser(user);

            Stage stage = (Stage) loginButton.getScene().getWindow();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("SmartTaskManager - Intelligent Task Management");
            stage.setWidth(1400);
            stage.setHeight(900);
            stage.setResizable(true);
            stage.centerOnScreen();
        } catch (IOException e) {
            e.printStackTrace();
            showLoginError("Erreur lors du chargement de l'application");
        }
    }

    private void showLoginError(String message) {
        loginErrorLabel.setText(message);
        loginErrorLabel.setVisible(true);
    }

    private void showRegisterError(String message) {
        registerErrorLabel.setText(message);
        registerErrorLabel.setVisible(true);
    }

    private void clearErrors() {
        loginErrorLabel.setVisible(false);
        registerErrorLabel.setVisible(false);
    }
}
