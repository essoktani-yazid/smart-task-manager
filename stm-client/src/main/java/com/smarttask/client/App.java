package com.smarttask.client;

import com.smarttask.client.view.controller.MainLayoutController;
import com.smarttask.model.User;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * JavaFX application entry point.
 * Launches the SmartTaskManager client user interface.
 */
public class App extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load login view
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
        // FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-layout.fxml"));
        Parent root = loader.load();

        primaryStage.setTitle("SmartTaskManager ");
        Scene scene = new Scene(root, 800, 600);
        
        // Add CSS if exists (login.fxml might not supply it directly if not linked)
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMaximized(true);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


