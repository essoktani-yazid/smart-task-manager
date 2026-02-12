package com.smarttask.client.view.controller.projects;

import com.smarttask.model.Project;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class ProjectFormController {
    public enum Mode {
        CREATE,
        EDIT
    }

    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private ColorPicker colorPicker;
    @FXML private Region colorPreview;
    @FXML private Label colorCodeLabel;
    @FXML private Button saveBtn;
    @FXML private Button cancelBtn;
    @FXML private Label titleLabel;

    private Mode mode = Mode.CREATE;

    private Project project;
    private Stage stage;

    public void setStage(Stage stage) { this.stage = stage; }

    @FXML
    private void initialize() {
        // Écouter les changements de couleur et mettre à jour l'aperçu
        colorPicker.setOnAction(event -> updateColorPreview());
    }
    
    public void setMode(Mode mode) {
        this.mode = mode;
        updateFormMode();
    }
    private void updateFormMode() {

        if (mode == Mode.CREATE) {
            titleLabel.setText("Créer un nouveau projet");
            saveBtn.setText("Créer le projet");
        } else {
            titleLabel.setText("Modifier le projet");
            saveBtn.setText("Enregistrer les modifications");
        }
    }
    
    private void updateColorPreview() {
        Color selectedColor = colorPicker.getValue();
        if (selectedColor != null) {
            colorPreview.setStyle("-fx-background-color: " + toHex(selectedColor) + "; -fx-border-radius: 6; -fx-background-radius: 6; -fx-border-color: #d1d5db; -fx-border-width: 1;");
            colorCodeLabel.setText(toHex(selectedColor));
        }
    }

    public void setProject(Project project) {
        this.project = project;

        if (project == null) {
            this.project = new Project();
            setMode(Mode.CREATE);
        } else {
            setMode(Mode.EDIT);
        }

        nameField.setText(project.getName() != null ? project.getName() : "");
        descriptionField.setText(project.getDescription() != null ? project.getDescription() : "");
        try {
            Color c = Color.web(project.getColor() != null ? project.getColor() : "#3788d8");
            colorPicker.setValue(c);
        } catch (Exception e) {
            colorPicker.setValue(Color.web("#3788d8"));
        }
        updateColorPreview();
    }

    public Project getProject() {
        if (project == null) project = new Project();
        
        String name = nameField.getText();
        if (name == null || name.trim().isEmpty()) {
            showAlert("Erreur", "Le nom du projet est obligatoire.");
            return null;
        }
        
        project.setName(name);
        project.setDescription(descriptionField.getText());
        project.setColor(toHex(colorPicker.getValue()));
        return project;
    }

    private String toHex(Color c) {
        return String.format("#%02X%02X%02X",
                (int) (c.getRed() * 255),
                (int) (c.getGreen() * 255),
                (int) (c.getBlue() * 255));
    }

    @FXML
    private void onSave() {
        if (getProject() != null) {
            if (stage != null) stage.close();
        }
    }

    @FXML
    private void onCancel() {
        project = null;
        if (stage != null) stage.close();
    }
    
    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();    }
}