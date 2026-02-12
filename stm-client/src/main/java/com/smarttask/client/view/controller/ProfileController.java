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

    // --- HEADER ---
    @FXML private Text avatarText;
    @FXML private Label usernameLabel;
    @FXML private Label roleLabel;

    // --- PERSONAL INFO ---
    @FXML private TextField usernameField;
    @FXML private TextField firstNameField;
    @FXML private TextField lastNameField;
    @FXML private TextField emailField;
    @FXML private Label profileStatusLabel;

    // --- SECURITY: VERIFICATION ---
    @FXML private PasswordField currentPasswordField;
    @FXML private TextField currentPasswordText;
    @FXML private Button verifyPasswordButton;
    @FXML private Label verifyStatusLabel;

    // --- SECURITY: NEW PASSWORD ---
    @FXML private VBox newPasswordSection;
    @FXML private PasswordField newPasswordField;
    @FXML private TextField newPasswordText;
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

        // 1. Real-time email validation
        emailField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isEmpty()) {
                clearFieldValidation(emailField);
            } else if (isValidEmail(newVal)) {
                setFieldValidation(emailField, true);
            } else {
                setFieldValidation(emailField, false);
            }
        });

        // 2. Setup Password Sync (Current & New)
        setupPasswordSync(currentPasswordField, currentPasswordText);
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

        // Avatar Initial
        if (avatarText != null) {
            String initial = (currentUser.getUsername() != null && !currentUser.getUsername().isEmpty())
                    ? currentUser.getUsername().substring(0, 1).toUpperCase()
                    : "?";
            avatarText.setText(initial);
        }

        // Display Name (Handling nulls)
        if (usernameLabel != null) {
            String fName = currentUser.getFirstName() != null ? currentUser.getFirstName() : "";
            String lName = currentUser.getLastName() != null ? currentUser.getLastName() : "";
            usernameLabel.setText((fName + " " + lName).trim().isEmpty() ? currentUser.getUsername() : (fName + " " + lName));
        }

        resetPasswordSection();
    }

    // --- PROFILE ACTIONS ---

    @FXML
    private void handleSaveProfile() {
        if (currentUser == null) return;

        String email = emailField.getText();
        if (!isValidEmail(email)) {
            showStatus(profileStatusLabel, false, "Invalid email format");
            setFieldValidation(emailField, false);
            return;
        }

        currentUser.setFirstName(firstNameField.getText());
        currentUser.setLastName(lastNameField.getText());
        currentUser.setEmail(email);

        boolean success = userService.updateUser(currentUser);
        showStatus(profileStatusLabel, success, success ? "✓ Profile updated" : "✗ Update failed");
    }

    @FXML
    private void handleEditAvatar() {
        showStatus(profileStatusLabel, true, "ℹ️ Upload feature coming soon");
    }

    // --- PASSWORD ACTIONS ---

    @FXML
    private void handleVerifyPassword() {
        String input = currentPasswordField.getText();

        if (input == null || input.isEmpty()) {
            showStatus(verifyStatusLabel, false, "Please enter your password");
            return;
        }

        // Verification via service
        boolean verified = authService.verifyPassword(currentUser.getUsername(), input);

        if (verified) {
            passwordVerified = true;
            showStatus(verifyStatusLabel, true, "✓ Identity verified");
            enablePasswordChange();
        } else {
            passwordVerified = false;
            showStatus(verifyStatusLabel, false, "✗ Incorrect password");
        }
    }

    @FXML private void toggleCurrentPassword() { toggleVisibility(currentPasswordField, currentPasswordText); }
    @FXML private void toggleNewPassword() { toggleVisibility(newPasswordField, newPasswordText); }

    @FXML
    private void handleChangePassword() {
        if (!passwordVerified) return;

        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass == null || newPass.length() < 6) {
            showStatus(passwordStatusLabel, false, "Minimum 6 characters required");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showStatus(passwordStatusLabel, false, "Passwords do not match");
            return;
        }

        currentUser.setPassword(newPass);
        boolean success = userService.updateUser(currentUser);

        if (success) {
            showStatus(passwordStatusLabel, true, "Password changed successfully!");
            PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
            delay.setOnFinished(e -> resetPasswordSection());
            delay.play();
        } else {
            showStatus(passwordStatusLabel, false, "Server error");
        }
    }

    // --- INTERNAL LOGIC ---

    private void setupPasswordSync(PasswordField pass, TextField text) {
        if (pass != null && text != null) {
            text.textProperty().bindBidirectional(pass.textProperty());
        }
    }

    private void toggleVisibility(PasswordField pass, TextField text) {
        boolean isTextVisible = text.isVisible();
        text.setVisible(!isTextVisible);
        text.setManaged(!isTextVisible);
        pass.setVisible(isTextVisible);
        pass.setManaged(isTextVisible);
        
        if (!isTextVisible) text.requestFocus(); else pass.requestFocus();
    }

    private void enablePasswordChange() {
        newPasswordSection.setDisable(false);
        newPasswordSection.setOpacity(1.0);
        verifyPasswordButton.setDisable(true);
        currentPasswordField.setDisable(true);
        currentPasswordText.setDisable(true);
        newPasswordField.requestFocus();
    }

    private void resetPasswordSection() {
        passwordVerified = false;

        if (currentPasswordField != null) currentPasswordField.setText("");
        if (newPasswordField != null) newPasswordField.setText("");
        if (confirmPasswordField != null) confirmPasswordField.setText("");

        // Set default visibility (hidden)
        if (currentPasswordText != null) { currentPasswordText.setVisible(false); currentPasswordText.setManaged(false); }
        if (currentPasswordField != null) { currentPasswordField.setVisible(true); currentPasswordField.setManaged(true); }

        if (newPasswordText != null) { newPasswordText.setVisible(false); newPasswordText.setManaged(false); }
        if (newPasswordField != null) { newPasswordField.setVisible(true); newPasswordField.setManaged(true); }

        if (verifyPasswordButton != null) verifyPasswordButton.setDisable(false);
        if (currentPasswordField != null) currentPasswordField.setDisable(false);
        if (currentPasswordText != null) currentPasswordText.setDisable(false);

        if (newPasswordSection != null) {
            newPasswordSection.setDisable(true);
            newPasswordSection.setOpacity(0.5);
        }

        if (verifyStatusLabel != null) verifyStatusLabel.setVisible(false);
        if (passwordStatusLabel != null) passwordStatusLabel.setVisible(false);
    }

    private void showStatus(Label label, boolean success, String message) {
        if (label == null) return;
        label.setText(message);
        label.setVisible(true);
        label.getStyleClass().removeAll("status-success", "status-error");
        label.getStyleClass().add(success ? "status-success" : "status-error");

        PauseTransition pause = new PauseTransition(Duration.seconds(3));
        pause.setOnFinished(e -> label.setVisible(false));
        pause.play();
    }

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