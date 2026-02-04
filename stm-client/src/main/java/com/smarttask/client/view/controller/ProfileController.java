package com.smarttask.client.view.controller;

import com.smarttask.client.service.AuthService;
import com.smarttask.client.service.UserService;
import com.smarttask.model.User;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.util.Duration;

public class ProfileController {

    // --- EN-TÊTE ---
    @FXML private Text avatarText;
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;

    // --- INFOS PERSO ---
    @FXML private TextField usernameField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private Label profileStatusLabel;

    // --- SÉCURITÉ : VÉRIFICATION ---
    @FXML private PasswordField currentPasswordField;
    @FXML private TextField currentPasswordText; // Le champ visible
    @FXML private Button verifyPasswordButton;
    @FXML private Label verifyStatusLabel;

    // --- SÉCURITÉ : NOUVEAU MOT DE PASSE ---
    @FXML private VBox newPasswordSection;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordText; // Le champ visible
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button changePasswordButton;
    @FXML private Label passwordStatusLabel;

    // --- SERVICES ---
    private User currentUser;
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();
    private boolean passwordVerified = false;

    @FXML
    public void initialize() {
        resetPasswordSection();

        // 1. Validation en temps réel de l'email
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                clearFieldValidation(emailField);
            } else if (isValidEmail(newVal)) {
                setFieldValidation(emailField, true);
            } else {
                setFieldValidation(emailField, false);
            }
        });

        // 2. Synchronisation pour le Toggle Password (Vérification)
        setupPasswordSync(currentPasswordField, currentPasswordText);

        // 3. Synchronisation pour le Toggle Password (Nouveau)
        setupPasswordSync(newPasswordField, newPasswordText);
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (currentUser != null) {
            populateFields();
        }
    }

    private void populateFields() {
        usernameField.setText(currentUser.getUsername());
        firstNameField.setText(currentUser.getFirstName() != null ? currentUser.getFirstName() : "");
        lastNameField.setText(currentUser.getLastName() != null ? currentUser.getLastName() : "");
        emailField.setText(currentUser.getEmail());

        // Initiale Avatar
        if (avatarText != null) {
            String initial = (currentUser.getUsername() != null && !currentUser.getUsername().isEmpty())
                    ? currentUser.getUsername().substring(0, 1).toUpperCase()
                    : "?";
            avatarText.setText(initial);
        }
        if (usernameLabel != null) usernameLabel.setText(currentUser.getFirstName() + " " + currentUser.getLastName());

        resetPasswordSection();
    }

    // --- ACTIONS PROFIL ---

    @FXML
    private void handleSaveProfile() {
        if (currentUser == null) return;

        String email = emailField.getText();
        if (!isValidEmail(email)) {
            showStatus(profileStatusLabel, false, "Format d'email invalide");
            setFieldValidation(emailField, false);
            return;
        }

        currentUser.setFirstName(firstNameField.getText());
        currentUser.setLastName(lastNameField.getText());
        currentUser.setEmail(email);

        boolean success = userService.updateUser(currentUser);
        showStatus(profileStatusLabel, success, success ? "✓ Profil mis à jour" : "✗ Erreur de mise à jour");
    }

    @FXML
    private void handleEditAvatar() {
        // Pour l'instant, simple log ou feedback visuel
        showStatus(profileStatusLabel, true, "ℹ️ Fonctionnalité d'upload bientôt disponible");
    }

    // --- ACTIONS MOT DE PASSE ---

    @FXML
    private void handleVerifyPassword() {
        String input = currentPasswordField.getText(); // La valeur est sync avec currentPasswordText

        if (input == null || input.isEmpty()) {
            showStatus(verifyStatusLabel, false, "Entrez votre mot de passe");
            return;
        }

        boolean verified = authService.verifyPassword(currentUser.getUsername(), input);

        if (verified) {
            passwordVerified = true;
            showStatus(verifyStatusLabel, true, "✓ Identité vérifiée");
            enablePasswordChange();
        } else {
            passwordVerified = false;
            showStatus(verifyStatusLabel, false, "✗ Mot de passe incorrect");
        }
    }

    // Toggle Visibility : Champ Actuel
    @FXML
    private void toggleCurrentPassword() {
        toggleVisibility(currentPasswordField, currentPasswordText);
    }

    // Toggle Visibility : Nouveau Champ
    @FXML
    private void toggleNewPassword() {
        toggleVisibility(newPasswordField, newPasswordText);
    }

    @FXML
    private void handleChangePassword() {
        if (!passwordVerified) return;

        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass == null || newPass.length() < 6) {
            showStatus(passwordStatusLabel, false, "Minimum 6 caractères requis");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showStatus(passwordStatusLabel, false, "Les mots de passe ne correspondent pas");
            return;
        }

        currentUser.setPassword(newPass);
        boolean success = userService.updateUser(currentUser);

        if (success) {
            showStatus(passwordStatusLabel, true, "Mot de passe modifié !");
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> resetPasswordSection());
            delay.play();
        } else {
            showStatus(passwordStatusLabel, false, "Erreur serveur");
        }
    }

    // --- LOGIQUE INTERNE ---

    private void setupPasswordSync(PasswordField pass, TextField text) {
        // Binding bidirectionnel : ce qu'on tape dans l'un va dans l'autre
        text.textProperty().bindBidirectional(pass.textProperty());
    }

    private void toggleVisibility(PasswordField pass, TextField text) {
        // Si le TextField est visible, on le cache et on montre le PasswordField
        boolean isTextVisible = text.isVisible();

        if (isTextVisible) {
            text.setVisible(false);
            text.setManaged(false);
            pass.setVisible(true);
            pass.setManaged(true);
        } else {
            text.setVisible(true);
            text.setManaged(true);
            pass.setVisible(false);
            pass.setManaged(false);
        }
    }

    private void enablePasswordChange() {
        newPasswordSection.setDisable(false);
        newPasswordSection.setOpacity(1.0);
        verifyPasswordButton.setDisable(true);
        currentPasswordField.setDisable(true);
        currentPasswordText.setDisable(true); // Bloquer aussi le champ visible si ouvert
        newPasswordField.requestFocus();
    }

    private void resetPasswordSection() {
        passwordVerified = false;

        currentPasswordField.setText("");
        newPasswordField.setText("");
        confirmPasswordField.setText("");

        // Reset visibilité (remettre en mode caché par défaut)
        currentPasswordText.setVisible(false); currentPasswordText.setManaged(false);
        currentPasswordField.setVisible(true); currentPasswordField.setManaged(true);

        newPasswordText.setVisible(false); newPasswordText.setManaged(false);
        newPasswordField.setVisible(true); newPasswordField.setManaged(true);

        currentPasswordField.setDisable(false);
        currentPasswordText.setDisable(false);
        verifyPasswordButton.setDisable(false);

        newPasswordSection.setDisable(true);
        newPasswordSection.setOpacity(0.5);

        verifyStatusLabel.setVisible(false);
        passwordStatusLabel.setVisible(false);
    }

    private void showStatus(Label label, boolean success, String message) {
        label.setText(message);
        label.setVisible(true);
        label.getStyleClass().removeAll("status-success", "status-error");
        label.getStyleClass().add(success ? "status-success" : "status-error");

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> label.setVisible(false));
        pause.play();
    }

    // --- VALIDATION VISUELLE ---

    private boolean isValidEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private void setFieldValidation(TextField field, boolean isValid) {
        field.getStyleClass().removeAll("field-error", "field-success");
        field.getStyleClass().add(isValid ? "field-success" : "field-error");
    }

    private void clearFieldValidation(TextField field) {
        field.getStyleClass().removeAll("field-error", "field-success");
    }
}
