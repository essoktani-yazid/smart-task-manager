package com.smarttask.client.view.controller;

import com.smarttask.client.service.AuthService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.User;
import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        
        // Les animations d'entrée et de flottement ont été supprimées ici
        // On s'assure que la carte est bien visible immédiatement
        mainCard.setOpacity(1.0);
        mainCard.setScaleX(1.0);
        mainCard.setScaleY(1.0);
    }

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
        // On garde cette animation car elle est fonctionnelle pour le changement d'onglet
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

    @FXML
    private void handleLogin() {
        String username = loginUsernameField.getText();
        String password = loginPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showError(loginErrorLabel, "Veuillez remplir tous les champs");
            return;
        }

        User user = authService.login(username, password);
        if (user != null){ SessionManager.getInstance().setCurrentUser(user);
            navigateToDashboard(user);}
        else showError(loginErrorLabel, "Identifiants incorrects");
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

        try {
            User user = authService.register(username, password, email);
            if (user != null) {
                navigateToDashboard(user);
            }
        } catch (Exception e) {
            showError(registerErrorLabel, e.getMessage());
        }
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
        // On peut garder la secousse d'erreur car elle aide l'utilisateur
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