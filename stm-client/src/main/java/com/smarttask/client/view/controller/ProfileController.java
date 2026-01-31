package com.smarttask.client.view.controller;

import com.smarttask.client.service.AuthService;
import com.smarttask.client.service.UserService;
import com.smarttask.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ProfileController {

    @FXML private TextField usernameField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    
    // Password verification section
    @FXML private PasswordField currentPasswordField;
    @FXML private Button verifyPasswordButton;
    @FXML private Label verifyStatusLabel;
    
    // New password section
    @FXML private VBox newPasswordSection;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Button changePasswordButton;
    @FXML private Label passwordStatusLabel;
    
    @FXML private Label profileStatusLabel;

    private User currentUser;
    private final UserService userService = new UserService();
    private final AuthService authService = new AuthService();
    private boolean passwordVerified = false;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        populateFields();
    }

    private void populateFields() {
        if (currentUser == null) return;
        usernameField.setText(currentUser.getUsername());
        firstNameField.setText(currentUser.getFirstName() != null ? currentUser.getFirstName() : "");
        lastNameField.setText(currentUser.getLastName() != null ? currentUser.getLastName() : "");
        emailField.setText(currentUser.getEmail());
        
        // Initialize password section state
        resetPasswordSection();
    }

    @FXML
    private void handleSaveProfile() {
        if (currentUser == null) return;

        currentUser.setFirstName(firstNameField.getText());
        currentUser.setLastName(lastNameField.getText());
        currentUser.setEmail(emailField.getText());

        boolean success = userService.updateUser(currentUser);

        showProfileStatus(success);
    }

    @FXML
    private void handleVerifyPassword() {
        String currentPassword = currentPasswordField.getText();
        
        if (currentPassword == null || currentPassword.isEmpty()) {
            showVerifyStatus(false, "Veuillez entrer votre mot de passe actuel");
            return;
        }

        boolean verified = authService.verifyPassword(currentUser.getUsername(), currentPassword);
        
        if (verified) {
            passwordVerified = true;
            showVerifyStatus(true, "✓ Mot de passe vérifié");
            enablePasswordChange();
        } else {
            passwordVerified = false;
            showVerifyStatus(false, "✗ Mot de passe incorrect");
        }
    }

    @FXML
    private void handleChangePassword() {
        if (!passwordVerified) {
            showPasswordStatus(false, "Veuillez d'abord vérifier votre mot de passe actuel");
            return;
        }

        String newPassword = newPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (newPassword == null || newPassword.isEmpty()) {
            showPasswordStatus(false, "Veuillez entrer un nouveau mot de passe");
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            showPasswordStatus(false, "Les mots de passe ne correspondent pas");
            return;
        }

        if (newPassword.length() < 6) {
            showPasswordStatus(false, "Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        // Update password
        currentUser.setPassword(newPassword);
        boolean success = userService.updateUser(currentUser);

        if (success) {
            showPasswordStatus(true, "Mot de passe modifié avec succès");
            resetPasswordSection();
        } else {
            showPasswordStatus(false, "Erreur lors de la modification du mot de passe");
        }
    }

    private void enablePasswordChange() {
        newPasswordSection.setDisable(false);
        newPasswordSection.setOpacity(1.0);
        verifyPasswordButton.setDisable(true);
        currentPasswordField.setDisable(true);
    }

    private void resetPasswordSection() {
        passwordVerified = false;
        currentPasswordField.setText("");
        newPasswordField.setText("");
        confirmPasswordField.setText("");
        currentPasswordField.setDisable(false);
        verifyPasswordButton.setDisable(false);
        newPasswordSection.setDisable(true);
        newPasswordSection.setOpacity(0.5);
        verifyStatusLabel.setVisible(false);
        passwordStatusLabel.setVisible(false);
    }

    private void showProfileStatus(boolean success) {
        profileStatusLabel.setVisible(true);
        if (success) {
            profileStatusLabel.setText("✓ Profil mis à jour avec succès");
            profileStatusLabel.getStyleClass().removeAll("error-label");
            profileStatusLabel.getStyleClass().add("success-label");
        } else {
            profileStatusLabel.setText("✗ Erreur lors de la mise à jour du profil");
            profileStatusLabel.getStyleClass().removeAll("success-label");
            profileStatusLabel.getStyleClass().add("error-label");
        }
    }

    private void showVerifyStatus(boolean success, String message) {
        verifyStatusLabel.setVisible(true);
        verifyStatusLabel.setText(message);
        if (success) {
            verifyStatusLabel.getStyleClass().removeAll("error-label");
            verifyStatusLabel.getStyleClass().add("success-label");
        } else {
            verifyStatusLabel.getStyleClass().removeAll("success-label");
            verifyStatusLabel.getStyleClass().add("error-label");
        }
    }

    private void showPasswordStatus(boolean success, String message) {
        passwordStatusLabel.setVisible(true);
        passwordStatusLabel.setText(message);
        if (success) {
            passwordStatusLabel.getStyleClass().removeAll("error-label");
            passwordStatusLabel.getStyleClass().add("success-label");
        } else {
            passwordStatusLabel.getStyleClass().removeAll("success-label");
            passwordStatusLabel.getStyleClass().add("error-label");
        }
    }
}
