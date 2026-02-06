package com.smarttask.client.view.controller;

import com.smarttask.client.service.GoogleCalendarService;
import com.smarttask.client.service.GoogleCalendarService.SyncStatus;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 * GOOGLE CALENDAR SETTINGS PANEL
 * 
 * UI Component for managing Google Calendar sync settings
 * Add this to your Calendar sidebar or settings
 * ═══════════════════════════════════════════════════════════════════════════════════════════════
 */
public class GoogleCalendarSettingsPanel extends VBox implements GoogleCalendarService.GoogleCalendarSyncListener {

    // UI Components
    private Circle statusIndicator;
    private Label statusLabel;
    private Button connectButton;
    private Button disconnectButton;
    private Button syncAllButton;
    private Label lastSyncLabel;
    private ProgressIndicator syncProgress;
    
    // Service reference
    private final GoogleCalendarService googleService;
    private Runnable onSyncAction;

    public GoogleCalendarSettingsPanel(GoogleCalendarService service) {
        this.googleService = service;
        
        // Register as sync listener
        googleService.addSyncListener(this);
        
        buildUI();
        updateUI();
    }

    private void buildUI() {
        setSpacing(12);
        setPadding(new Insets(16));
        setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 12; " +
            "-fx-border-color: #e5e7eb; " +
            "-fx-border-radius: 12; " +
            "-fx-border-width: 1;"
        );

        // ════════════════════════════════════════════════════════════════════════════
        // HEADER
        // ════════════════════════════════════════════════════════════════════════════
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Google icon (using emoji as placeholder)
        Label googleIcon = new Label("📅");
        googleIcon.setStyle("-fx-font-size: 24;");

        VBox headerText = new VBox(2);
        Label title = new Label("Google Calendar");
        title.setStyle("-fx-font-size: 15; -fx-font-weight: 700; -fx-text-fill: #1e293b;");
        
        Label subtitle = new Label("Sync your tasks and events");
        subtitle.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
        
        headerText.getChildren().addAll(title, subtitle);
        header.getChildren().addAll(googleIcon, headerText);

        // ════════════════════════════════════════════════════════════════════════════
        // STATUS ROW
        // ════════════════════════════════════════════════════════════════════════════
        HBox statusRow = new HBox(8);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setPadding(new Insets(10, 12, 10, 12));
        statusRow.setStyle(
            "-fx-background-color: #f8fafc; " +
            "-fx-background-radius: 8;"
        );

        statusIndicator = new Circle(5);
        statusIndicator.setFill(Color.web("#9ca3af")); // Gray = disconnected

        statusLabel = new Label("Not connected");
        statusLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: #475569;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        syncProgress = new ProgressIndicator();
        syncProgress.setMaxSize(16, 16);
        syncProgress.setVisible(false);

        statusRow.getChildren().addAll(statusIndicator, statusLabel, spacer, syncProgress);

        // ════════════════════════════════════════════════════════════════════════════
        // BUTTONS
        // ════════════════════════════════════════════════════════════════════════════
        HBox buttonRow = new HBox(8);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        connectButton = new Button("Connect");
        connectButton.setStyle(
            "-fx-background-color: #4285f4; " +
            "-fx-text-fill: white; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 6; " +
            "-fx-cursor: hand;"
        );
        connectButton.setOnAction(e -> handleConnect());

        disconnectButton = new Button("Disconnect");
        disconnectButton.setStyle(
            "-fx-background-color: white; " +
            "-fx-text-fill: #dc2626; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #fecaca; " +
            "-fx-border-radius: 6; " +
            "-fx-cursor: hand;"
        );
        disconnectButton.setOnAction(e -> handleDisconnect());
        disconnectButton.setVisible(false);

        syncAllButton = new Button("Sync All");
        syncAllButton.setStyle(
            "-fx-background-color: white; " +
            "-fx-text-fill: #059669; " +
            "-fx-font-size: 12; " +
            "-fx-font-weight: 600; " +
            "-fx-padding: 8 16; " +
            "-fx-background-radius: 6; " +
            "-fx-border-color: #a7f3d0; " +
            "-fx-border-radius: 6; " +
            "-fx-cursor: hand;"
        );
        syncAllButton.setOnAction(e -> handleSyncAll());
        syncAllButton.setVisible(false);

        buttonRow.getChildren().addAll(connectButton, disconnectButton, syncAllButton);

        // ════════════════════════════════════════════════════════════════════════════
        // LAST SYNC INFO
        // ════════════════════════════════════════════════════════════════════════════
        lastSyncLabel = new Label("");
        lastSyncLabel.setStyle("-fx-font-size: 10; -fx-text-fill: #94a3b8;");
        lastSyncLabel.setVisible(false);

        // ════════════════════════════════════════════════════════════════════════════
        // ASSEMBLE
        // ════════════════════════════════════════════════════════════════════════════
        getChildren().addAll(header, statusRow, buttonRow, lastSyncLabel);
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ACTIONS
    // ════════════════════════════════════════════════════════════════════════════

    private void handleConnect() {
        connectButton.setDisable(true);
        connectButton.setText("Connecting...");
        syncProgress.setVisible(true);
        
        // The actual connection is handled by PlannerManager.enableGoogleSync()
        // This will be called from CalendarController
        if (onConnectAction != null) {
            onConnectAction.run();
        }
    }

    private void handleDisconnect() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Disconnect Google Calendar");
        confirm.setHeaderText("Disconnect from Google Calendar?");
        confirm.setContentText("Your items will no longer sync to Google Calendar.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (onDisconnectAction != null) {
                    onDisconnectAction.run();
                }
            }
        });
    }

    private void handleSyncAll() {
        syncAllButton.setDisable(true);
        syncProgress.setVisible(true);
        
        if (onSyncAction != null) {
            onSyncAction.run();
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // SYNC LISTENER IMPLEMENTATION
    // ════════════════════════════════════════════════════════════════════════════

    @Override
    public void onSyncStatusChanged(SyncStatus status, String message) {
        Platform.runLater(() -> {
            switch (status) {
                case CONNECTED:
                    statusIndicator.setFill(Color.web("#22c55e")); // Green
                    statusLabel.setText("Connected");
                    connectButton.setVisible(false);
                    disconnectButton.setVisible(true);
                    syncAllButton.setVisible(true);
                    syncProgress.setVisible(false);
                    break;
                    
                case DISCONNECTED:
                    statusIndicator.setFill(Color.web("#9ca3af")); // Gray
                    statusLabel.setText("Not connected");
                    connectButton.setVisible(true);
                    connectButton.setDisable(false);
                    connectButton.setText("Connect");
                    disconnectButton.setVisible(false);
                    syncAllButton.setVisible(false);
                    syncProgress.setVisible(false);
                    lastSyncLabel.setVisible(false);
                    break;
                    
                case SYNCING:
                    statusIndicator.setFill(Color.web("#3b82f6")); // Blue
                    statusLabel.setText("Syncing...");
                    syncProgress.setVisible(true);
                    break;
                    
                case SYNCED:
                    statusIndicator.setFill(Color.web("#22c55e")); // Green
                    statusLabel.setText("Connected");
                    syncProgress.setVisible(false);
                    syncAllButton.setDisable(false);
                    lastSyncLabel.setText("Last sync: just now");
                    lastSyncLabel.setVisible(true);
                    break;
                    
                case ERROR:
                    statusIndicator.setFill(Color.web("#ef4444")); // Red
                    statusLabel.setText("Error: " + message);
                    syncProgress.setVisible(false);
                    connectButton.setDisable(false);
                    connectButton.setText("Retry");
                    syncAllButton.setDisable(false);
                    break;
            }
        });
    }

    private void updateUI() {
        if (googleService.isAuthenticated()) {
            onSyncStatusChanged(SyncStatus.CONNECTED, "");
        } else {
            onSyncStatusChanged(SyncStatus.DISCONNECTED, "");
        }
    }

    // ════════════════════════════════════════════════════════════════════════════
    // ACTION CALLBACKS
    // ════════════════════════════════════════════════════════════════════════════

    private Runnable onConnectAction;
    private Runnable onDisconnectAction;

    public void setOnConnectAction(Runnable action) {
        this.onConnectAction = action;
    }

    public void setOnDisconnectAction(Runnable action) {
        this.onDisconnectAction = action;
    }

    public void setOnSyncAction(Runnable action) {
        this.onSyncAction = action;
    }

    // ════════════════════════════════════════════════════════════════════════════
    // CLEANUP
    // ════════════════════════════════════════════════════════════════════════════

    public void cleanup() {
        googleService.removeSyncListener(this);
    }
}
