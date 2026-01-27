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
        // Load main layout with sidebar
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main-layout.fxml"));
        Parent root = loader.load();

        // Create default user for development
        User defaultUser = new User();
        defaultUser.setId(1L);
        defaultUser.setUsername("ESSOKTANI Yazid");
        defaultUser.setEmail("yazid.essoktani@esprit.tn");

        // Configure main controller with default user
        MainLayoutController controller = loader.getController();
        controller.setCurrentUser(defaultUser);

        primaryStage.setTitle("SmartTaskManager - Intelligent Task Management");
        Scene scene = new Scene(root, 1400, 900);
        
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        
        primaryStage.setScene(scene);
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(1200);
        primaryStage.setMinHeight(700);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}


