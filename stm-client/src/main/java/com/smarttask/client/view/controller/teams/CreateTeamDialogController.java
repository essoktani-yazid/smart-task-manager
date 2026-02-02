package com.smarttask.client.view.controller.teams;

import javafx.fxml.FXML;
import javafx.scene.control.ColorPicker;
import javafx.scene.paint.Color;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class CreateTeamDialogController {

    @FXML
    private TextField nameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private ColorPicker colorPicker;

    private Stage dialogStage;

	@FXML
	private void initialize() {
		colorPicker.setValue(Color.web("#FF0000"));
	}

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }

    @FXML
    private void handleCreate() {
        String name = nameField.getText();
        String description = descriptionField.getText();
        String color = colorPicker.getValue().toString();

        System.out.println("Create team:");
        System.out.println("Name = " + name);
        System.out.println("Desc = " + description);
        System.out.println("Color = " + color);

        // TODO : créer la team + ajouter la card
        dialogStage.close();
    }
}

