package com.smarttask.client.view.controller;

import com.smarttask.client.service.EmailNotificationService;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.*;
import com.smarttask.client.view.controller.kanban.*;

import com.smarttask.client.service.PlannerManager;
import com.smarttask.server.dao.ProjectDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.util.StringConverter;

import javafx.scene.layout.AnchorPane;
import javafx.scene.shape.Line;
import javafx.scene.paint.Color;
import java.util.stream.Collectors;

import java.net.URL;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;

/**
 * Professional Calendar Controller with Tasks and Events
 * NOW SUPPORTS: Tasks (private) AND Events (public)
 */
public class CalendarController implements Initializable {

    public enum ViewType {
        DAY, WEEK, MONTH, YEAR , KANBAN
    }
    // Primary Colors
    private static final String M3_PRIMARY = "#4f46e5";
    private static final String M3_PRIMARY_CONTAINER = "#eef2ff";
    private static final String M3_ON_PRIMARY = "#ffffff";

    // Surface Colors
    private static final String M3_SURFACE = "#ffffff";
    private static final String M3_SURFACE_CONTAINER = "#f8f9fe";
    private static final String M3_SURFACE_VARIANT = "#f1f5f9";

    // On Surface
    private static final String M3_ON_SURFACE = "#0f172a";
    private static final String M3_ON_SURFACE_VARIANT = "#475569";
    private static final String M3_OUTLINE = "#e2e8f0";

    // Priority Colors
    private static final String PRIORITY_URGENT = "#dc2626";
    private static final String PRIORITY_URGENT_CONTAINER = "#fef2f2";
    private static final String PRIORITY_IMPORTANT = "#ea580c";
    private static final String PRIORITY_IMPORTANT_CONTAINER = "#fff7ed";
    private static final String PRIORITY_STANDARD = "#4f46e5";
    private static final String PRIORITY_STANDARD_CONTAINER = "#eef2ff";
    private static final String PRIORITY_OPTIONAL = "#64748b";
    private static final String PRIORITY_OPTIONAL_CONTAINER = "#f1f5f9";

    // Type Colors
    private static final String TASK_COLOR = "#059669";
    private static final String TASK_CONTAINER = "#ecfdf5";
    private static final String EVENT_COLOR = "#4f46e5";
    private static final String EVENT_CONTAINER = "#eef2ff";

    // Corner Radii
    private static final int CORNER_XL = 28;
    private static final int CORNER_LG = 16;
    private static final int CORNER_MD = 12;
    private static final int CORNER_SM = 8;

    private static final int HOUR_HEIGHT = 60;

    private GoogleCalendarSettingsPanel googleCalendarPanel;

    // FXML Components
    @FXML private Label currentDateLabel;
    @FXML private Button prevButton;
    @FXML private Button nextButton;
    @FXML private Button todayButton;
    @FXML private ToggleGroup viewToggleGroup;
    @FXML private ToggleButton dayViewButton;
    @FXML private ToggleButton weekViewButton;
    @FXML private ToggleButton monthViewButton;
    @FXML private ToggleButton yearViewButton;
    @FXML private Button addTaskButton;   // NEW: Add Task button
    @FXML private Button addEventButton;  // Existing: Add Event button
    @FXML private StackPane calendarContainer;
    @FXML private ListView<PlannerItem> upcomingItemsList;  // CHANGED: now PlannerItem
    // NEW (add these):
    @FXML private CheckMenuItem filterOptional;
    @FXML private CheckMenuItem filterStandard;
    @FXML private CheckMenuItem filterImportant;
    @FXML private CheckMenuItem filterUrgent;
    @FXML private CheckMenuItem showCompleted;
    @FXML private CheckMenuItem showTasks;
    @FXML private CheckMenuItem showEvents;

    // Optional: MenuButton references if you want to show active filter indicators
    @FXML private MenuButton typeFilterBtn;
    @FXML private MenuButton priorityFilterBtn;
    @FXML private MenuButton statusFilterBtn;

    // NEW: Type filters
//    @FXML private CheckBox showTasks;
//    @FXML private CheckBox showEvents;

    @FXML private Label taskCountLabel;
    @FXML private Label eventCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private VBox googleCalendarContainer;

    @FXML private ToggleButton kanbanViewButton;  // Add this line


    // State
    private LocalDate currentDate = LocalDate.now();
    private ViewType currentView = ViewType.MONTH;
    private User currentUser; // Current logged-in user
    private PlannerManager plannerManager; // CHANGED: Unified manager

    // Drag and drop - CHANGED to PlannerItem
    private PlannerItem draggedItem = null;
    private VBox draggedItemCard = null;

    // Formatters
    private final DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("MMMM yyyy");
    private final DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private KanbanBoard kanbanBoard;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Get current user (TODO: Replace with your session management)
        this.currentUser = getCurrentUser();

        // Initialize planner manager
        this.plannerManager = new PlannerManager(currentUser);

        setupViewToggle();
        setupFilterListeners();
        setupUpcomingItemsList();
        updateFilterButtonLabels();
        setupPersistentFilterDropdowns();
        setupGoogleCalendarPanel();
        updateView();
    }

    @FXML
    private void handleClearFilters() {
        // Reset all filters to selected (show everything)
        if (showTasks != null) showTasks.setSelected(true);
        if (showEvents != null) showEvents.setSelected(true);
        if (filterUrgent != null) filterUrgent.setSelected(true);
        if (filterImportant != null) filterImportant.setSelected(true);
        if (filterStandard != null) filterStandard.setSelected(true);
        if (filterOptional != null) filterOptional.setSelected(true);
        if (showCompleted != null) showCompleted.setSelected(true);

        updateView();
    }

    /**
     * Get current logged-in user
     * TODO: Replace with your actual session management
     */
    private User getCurrentUser() {
        User user = SessionManager.getInstance().getCurrentUser();

        if (user == null) {
            System.err.println("⚠️ WARNING: No user in session!");
        } else {
            System.out.println("✅ Retrieved user from session:");
            System.out.println("   ID: " + user.getId());
            System.out.println("   Username: " + user.getUsername());
            System.out.println("   Email: " + user.getEmail());
        }

        return user;
    }

    /**
     * Public method to set current user (if needed)
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        SessionManager.getInstance().setCurrentUser(user);
        this.plannerManager = new PlannerManager(user);
        updateView();
    }

    /**
     * Setup Google Calendar integration panel
     */
    private void setupGoogleCalendarPanel() {
        if (googleCalendarContainer != null && plannerManager != null) {

            // Create the settings panel
            googleCalendarPanel = new GoogleCalendarSettingsPanel(
                    plannerManager.getGoogleCalendarService()
            );

            // Set up action callbacks
            googleCalendarPanel.setOnConnectAction(() -> {
                // Enable Google sync through PlannerManager
                plannerManager.enableGoogleSync();
            });

            googleCalendarPanel.setOnDisconnectAction(() -> {
                // Disable Google sync
                plannerManager.disableGoogleSync();
            });

            googleCalendarPanel.setOnSyncAction(() -> {
                // Sync all items
                plannerManager.syncAllToGoogle();
            });

            // Add to container
            googleCalendarContainer.getChildren().add(googleCalendarPanel);

            System.out.println("✅ Google Calendar panel initialized");
        }
    }


    //    **
//            * Create a small sync status indicator for the header
// */
    private HBox createSyncStatusIndicator() {
        HBox indicator = new HBox(6);
        indicator.setAlignment(Pos.CENTER_LEFT);
        indicator.setPadding(new Insets(4, 10, 4, 10));
        indicator.setStyle(
                "-fx-background-color: #f0fdf4; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-color: #bbf7d0; " +
                        "-fx-border-radius: 12;"
        );

        // Animated dot
        javafx.scene.shape.Circle dot = new javafx.scene.shape.Circle(4);
        dot.setFill(javafx.scene.paint.Color.web("#22c55e"));

        Label text = new Label("Synced");
        text.setStyle("-fx-font-size: 10; -fx-font-weight: 600; -fx-text-fill: #16a34a;");

        indicator.getChildren().addAll(dot, text);
        indicator.setVisible(false); // Hidden by default

        // Listen for sync status changes
        plannerManager.addGoogleSyncListener((status, message) -> {
            Platform.runLater(() -> {
                switch (status) {
                    case CONNECTED, SYNCED:
                        indicator.setVisible(true);
                        text.setText("Synced");
                        dot.setFill(javafx.scene.paint.Color.web("#22c55e"));
                        break;
                    case SYNCING:
                        indicator.setVisible(true);
                        text.setText("Syncing...");
                        dot.setFill(javafx.scene.paint.Color.web("#3b82f6"));
                        break;
                    case DISCONNECTED:
                        indicator.setVisible(false);
                        break;
                    case ERROR:
                        indicator.setVisible(true);
                        text.setText("Sync Error");
                        dot.setFill(javafx.scene.paint.Color.web("#ef4444"));
                        indicator.setStyle(
                                "-fx-background-color: #fef2f2; " +
                                        "-fx-background-radius: 12; " +
                                        "-fx-border-color: #fecaca; " +
                                        "-fx-border-radius: 12;"
                        );
                        break;
                }
            });
        });

        return indicator;
    }

    /**
     * HELPER METHOD: Check if item is completed
     * Handles both Tasks and Events
     */
    private boolean checkItemCompletion(PlannerItem item) {
        if (item.getItemType() == PlannerItemType.TASK) {
            TaskPlannerAdapter adapter = (TaskPlannerAdapter) item;
            return adapter.getTask().getStatus() == Status.COMPLETED;
        } else {
            EventPlannerAdapter adapter = (EventPlannerAdapter) item;
            return adapter.getEvent().isCompleted();
        }
    }

    private void setupViewToggle() {
        if (viewToggleGroup != null) {
            viewToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == dayViewButton) currentView = ViewType.DAY;
                else if (newVal == weekViewButton) currentView = ViewType.WEEK;
                else if (newVal == monthViewButton) currentView = ViewType.MONTH;
                else if (newVal == yearViewButton) currentView = ViewType.YEAR;
                else if (newVal == kanbanViewButton) currentView = ViewType.KANBAN;
                updateView();


            });


            // In setupViewToggle():
            if (weekViewButton != null) {
                weekViewButton.setSelected(true);  // Changed from monthViewButton
            }
        }
        initializeKanbanBoard();
    }

    private void setupPersistentFilterDropdowns() {
        // Make each MenuButton's dropdown stay open
        setupPersistentMenuButton(typeFilterBtn);
        setupPersistentMenuButton(priorityFilterBtn);
        setupPersistentMenuButton(statusFilterBtn);
    }

    private void initializeKanbanBoard() {
        kanbanBoard = new KanbanBoard();
        kanbanBoard.setCurrentUser(currentUser);

        // Set callbacks
        kanbanBoard.setOnTaskUpdated(task -> {
            // Save to database when task is moved
            plannerManager.updateTask(task);
            System.out.println("✅ Task status updated via Kanban: " + task.getTitle() + " → " + task.getStatus());
        });

        kanbanBoard.setOnTaskClicked(task -> {
            // Open edit dialog
            showTaskDialog(new TaskPlannerAdapter(task));
        });

        kanbanBoard.setOnTaskDeleted(task -> {
            // Delete from database
            plannerManager.removeTask(task);
            System.out.println("🗑️ Task deleted via Kanban: " + task.getTitle());
        });
    }

    /**
     * Configure a MenuButton to keep its dropdown open when items are selected
     */
    private void setupPersistentMenuButton(MenuButton menuButton) {
        if (menuButton == null) return;

        // For each CheckMenuItem, prevent the menu from closing when clicked
        for (MenuItem item : menuButton.getItems()) {
            if (item instanceof CheckMenuItem checkItem) {
                // Store the original handler
                checkItem.setOnAction(event -> {
                    // Toggle the selection (this happens automatically)
                    // But we need to consume the event to prevent menu from closing

                    // Re-show the menu to keep it open
                    Platform.runLater(() -> {
                        if (!menuButton.isShowing()) {
                            menuButton.show();
                        }
                    });

                    // Update the view
                    updateView();

                    // Consume the event
                    event.consume();
                });
            }
        }
    }

    private void updateSidebarStats() {
        if (plannerManager == null) return;

        // Get all tasks and events
        List<Task> allTasks = plannerManager.getAllTasks();
        List<CalendarEvent> allEvents = plannerManager.getAllEvents();

        // Count active (non-completed) items
        long activeTaskCount = allTasks.stream()
                .filter(t -> t.getStatus() != Status.COMPLETED)
                .count();

        long activeEventCount = allEvents.stream()
                .filter(e -> !e.isCompleted())
                .count();

        // Count completed items
        long completedTaskCount = allTasks.stream()
                .filter(t -> t.getStatus() == Status.COMPLETED)
                .count();

        long completedEventCount = allEvents.stream()
                .filter(CalendarEvent::isCompleted)
                .count();

        // Update labels (check for null to avoid errors)
        if (taskCountLabel != null) {
            taskCountLabel.setText(String.valueOf(activeTaskCount));
        }

        if (eventCountLabel != null) {
            eventCountLabel.setText(String.valueOf(activeEventCount));
        }

        if (completedCountLabel != null) {
            completedCountLabel.setText(String.valueOf(completedTaskCount + completedEventCount));
        }
    }

    private void setupFilterListeners() {
        // All CheckMenuItems - same API as CheckBox!
        CheckMenuItem[] filters = {
                filterOptional, filterStandard, filterImportant, filterUrgent,
                showCompleted, showTasks, showEvents
        };

        for (CheckMenuItem item : filters) {
            if (item != null) {
                item.selectedProperty().addListener((obs, oldVal, newVal) -> {
                    updateView();
                    updateFilterButtonLabels(); // Optional: update button text to show active count
                });
            }
        }
    }

    private void updateFilterButtonLabels() {
        // Type filter
        if (typeFilterBtn != null) {
            int typeCount = 0;
            if (showTasks != null && showTasks.isSelected()) typeCount++;
            if (showEvents != null && showEvents.isSelected()) typeCount++;

            if (typeCount == 2) {
                typeFilterBtn.setText("Type ▾");
            } else {
                typeFilterBtn.setText("Type (" + typeCount + ") ▾");
            }
        }

        // Priority filter
        if (priorityFilterBtn != null) {
            int priorityCount = 0;
            if (filterUrgent != null && filterUrgent.isSelected()) priorityCount++;
            if (filterImportant != null && filterImportant.isSelected()) priorityCount++;
            if (filterStandard != null && filterStandard.isSelected()) priorityCount++;
            if (filterOptional != null && filterOptional.isSelected()) priorityCount++;

            if (priorityCount == 4) {
                priorityFilterBtn.setText("Priority ▾");
            } else {
                priorityFilterBtn.setText("Priority (" + priorityCount + ") ▾");
            }
        }

        // Status filter
        if (statusFilterBtn != null) {
            if (showCompleted != null && showCompleted.isSelected()) {
                statusFilterBtn.setText("Status ▾");
            } else {
                statusFilterBtn.setText("Status (filtered) ▾");
            }
        }
    }


    /**
     * NEW: Create card for upcoming items (Tasks or Events)
     */
    /**
     * ENHANCED: Create beautiful card for upcoming items with settings menu
     */
    /**
     * ENHANCED: Create beautiful card for upcoming items with settings menu
     */
//    private VBox createEnhancedEventCard(PlannerItem item) {
//        VBox container = new VBox(8);  // Reduced from 10
//        container.setPadding(new Insets(12, 14, 12, 14));  // Reduced from 16, 18
//        container.setMinHeight(85);  // Reduced from 100
//        container.setMaxWidth(Double.MAX_VALUE);
//
//        // Get color based on priority
//        String baseColor = item.getColor();
//        String itemType = item.getItemType() == PlannerItemType.TASK ? "TASK" : "EVENT";
//
//        // Create BEAUTIFUL gradient background - more subtle and professional
//        String gradientStart = baseColor + "35"; // 21% opacity - visible but elegant
//        String gradientEnd = baseColor + "15";   // 8% opacity - subtle fade
//        String borderColor = baseColor;
//
//        // Different styles for tasks vs events
//        String borderStyle = item.getItemType() == PlannerItemType.TASK ? "dashed" : "solid";
//        String borderRadius = "10";  // Slightly smaller radius
//        String shadowColor = "rgba(0, 0, 0, 0.1)";  // Lighter shadow
//
//        container.setStyle(
//                "-fx-background-color: linear-gradient(to bottom right, " + gradientStart + ", " + gradientEnd + "); " +
//                        "-fx-background-radius: " + borderRadius + "; " +
//                        "-fx-border-radius: " + borderRadius + "; " +
//                        "-fx-border-color: " + borderColor + "; " +
//                        "-fx-border-width: 0 0 0 4; " +  // Reduced from 5
//                        "-fx-border-style: " + borderStyle + "; " +
//                        "-fx-effect: dropshadow(gaussian, " + shadowColor + ", 6, 0, 0, 2); " +  // Smaller shadow
//                        "-fx-cursor: hand;"
//        );
//
//        // ============= HEADER ROW =============
//        HBox headerRow = new HBox(6);  // Reduced from 8
//        headerRow.setAlignment(Pos.CENTER_LEFT);
//
//        // Type badge with icon - SMALLER
//        String typeIcon = item.getItemType() == PlannerItemType.TASK ? "📋" : "📅";
//        String typeText = item.getItemType() == PlannerItemType.TASK ? "Task" : "Event";
//        Label typeLabel = new Label(typeIcon + " " + typeText);
//        typeLabel.setStyle(
//                "-fx-text-fill: white; " +
//                        "-fx-font-size: 10; " +  // Reduced from 11
//                        "-fx-font-weight: bold; " +
//                        "-fx-background-color: " + borderColor + "; " +
//                        "-fx-padding: 3 10; " +  // Reduced from 5 12
//                        "-fx-background-radius: 12; " +  // Reduced from 15
//                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 2, 0, 0, 1);"  // Smaller shadow
//        );
//
//        // Private/Public badge - SMALLER
//        if (item.getItemType() == PlannerItemType.TASK) {
//            Label privateLabel = new Label("🔒 Private");
//            privateLabel.setStyle(
//                    "-fx-text-fill: #64748b; " +
//                            "-fx-font-size: 9; " +  // Reduced from 10
//                            "-fx-font-weight: 600; " +
//                            "-fx-background-color: #f1f5f9; " +
//                            "-fx-padding: 3 8; " +  // Reduced from 4 10
//                            "-fx-background-radius: 10;"  // Reduced from 12
//            );
//            headerRow.getChildren().add(privateLabel);
//        } else {
//            Label publicLabel = new Label("🌍 Public");
//            publicLabel.setStyle(
//                    "-fx-text-fill: #059669; " +
//                            "-fx-font-size: 9; " +  // Reduced from 10
//                            "-fx-font-weight: 600; " +
//                            "-fx-background-color: #d1fae5; " +
//                            "-fx-padding: 3 8; " +  // Reduced from 4 10
//                            "-fx-background-radius: 10;"  // Reduced from 12
//            );
//            headerRow.getChildren().add(publicLabel);
//        }
//
//        // Completion badge - SMALLER
//        boolean isCompleted = checkItemCompletion(item);
//        if (isCompleted) {
//            Label completedBadge = new Label("✓ Completed");
//            completedBadge.setStyle(
//                    "-fx-text-fill: #10b981; " +
//                            "-fx-font-size: 9; " +  // Reduced from 10
//                            "-fx-font-weight: bold; " +
//                            "-fx-background-color: #d1fae5; " +
//                            "-fx-padding: 3 8; " +  // Reduced from 4 10
//                            "-fx-background-radius: 10;"  // Reduced from 12
//            );
//            headerRow.getChildren().add(completedBadge);
//        }
//        // ✨ USER BADGE (only for tasks)
//        if (item.getItemType() == PlannerItemType.TASK) {
//            TaskPlannerAdapter adapter = (TaskPlannerAdapter) item;
//            String username = adapter.getTask().getUser() != null
//                    ? adapter.getTask().getUser().getUsername()
//                    : "Unknown";
//
//            Label userBadge = new Label("👤 " + username);
//            userBadge.setStyle(
//                    "-fx-text-fill: #0369a1; " +
//                            "-fx-font-size: 9; " +
//                            "-fx-font-weight: 600; " +
//                            "-fx-background-color: #e0f2fe; " +
//                            "-fx-padding: 3 8; " +
//                            "-fx-background-radius: 10;"
//            );
//            headerRow.getChildren().add(userBadge);
//        }
//
//        Region spacer = new Region();
//        HBox.setHgrow(spacer, Priority.ALWAYS);
//
//        // Settings menu button - SMALLER
//        Button settingsButton = createModernSettingsButton(item);
//
//        headerRow.getChildren().addAll(typeLabel, spacer, settingsButton);
//
//        // ============= TITLE ============= SMALLER
//        Label titleLabel = new Label(item.getTitle());
//        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 14));  // Reduced from 16
//        titleLabel.setStyle("-fx-text-fill: #1e293b;");
//        titleLabel.setWrapText(true);
//        if (isCompleted) {
//            titleLabel.setStyle(
//                    "-fx-text-fill: #94a3b8; " +
//                            "-fx-strikethrough: true; " +
//                            "-fx-opacity: 0.7;"
//            );
//        }
//
//        // ============= TIME AND DATE ROW ============= SMALLER & MORE COMPACT
//        HBox timeRow = new HBox(12);  // Reduced from 15
//        timeRow.setAlignment(Pos.CENTER_LEFT);
//        timeRow.setStyle("-fx-padding: 6 0 0 0;");  // Reduced from 8
//
//        // Date section - SMALLER
//        HBox dateSection = new HBox(5);  // Reduced from 6
//        dateSection.setAlignment(Pos.CENTER_LEFT);
//
//        Label dateIcon = new Label("📅");
//        dateIcon.setStyle("-fx-font-size: 11;");  // Reduced from 13
//
//        String dateText = item.getDate().format(DateTimeFormatter.ofPattern("EEE, MMM d, yyyy"));
//        Label dateLabel = new Label(dateText);
//        dateLabel.setStyle(
//                "-fx-text-fill: #475569; " +
//                        "-fx-font-size: 11; " +  // Reduced from 12
//                        "-fx-font-weight: 600;"
//        );
//
//        dateSection.getChildren().addAll(dateIcon, dateLabel);
//
//        // Time section - SMALLER
//        HBox timeSection = new HBox(5);  // Reduced from 6
//        timeSection.setAlignment(Pos.CENTER_LEFT);
//
//        Label timeIcon = new Label("🕐");
//        timeIcon.setStyle("-fx-font-size: 11;");  // Reduced from 13
//
//        String timeText = item.getStartTime().format(timeFormatter) +
//                " - " + item.getEndTime().format(timeFormatter);
//        Label timeLabel = new Label(timeText);
//        timeLabel.setStyle(
//                "-fx-text-fill: #475569; " +
//                        "-fx-font-size: 11; " +  // Reduced from 12
//                        "-fx-font-weight: 600;"
//        );
//
//        timeSection.getChildren().addAll(timeIcon, timeLabel);
//
//        timeRow.getChildren().addAll(dateSection, timeSection);
//
//        // ============= PRIORITY INDICATOR ============= SMALLER
//        HBox priorityRow = new HBox(6);  // Reduced from 8
//        priorityRow.setAlignment(Pos.CENTER_LEFT);
//        priorityRow.setStyle("-fx-padding: 3 0 0 0;");  // Reduced from 4
//
//        // Priority dot with glow effect - SMALLER
//        javafx.scene.shape.Circle priorityDot = new javafx.scene.shape.Circle(4);  // Reduced from 5
//        priorityDot.setFill(javafx.scene.paint.Color.web(borderColor));
//        priorityDot.setStyle("-fx-effect: dropshadow(gaussian, " + borderColor + "80, 3, 0, 0, 0);");  // Smaller glow
//
//        String priorityText = getPriorityText(item.getPriorityLevel());
//        Label priorityLabel = new Label(priorityText + " Priority");
//        priorityLabel.setStyle(
//                "-fx-text-fill: #64748b; " +
//                        "-fx-font-size: 10; " +  // Reduced from 11
//                        "-fx-font-weight: 700; " +
//                        "-fx-letter-spacing: 0.3px;"  // Reduced from 0.5px
//        );
//
//        priorityRow.getChildren().addAll(priorityDot, priorityLabel);
//
//        container.getChildren().addAll(headerRow, titleLabel, timeRow, priorityRow);
//
//        // ============= HOVER ANIMATION ============= MORE SUBTLE
//        final String defaultStyle = container.getStyle();
//        final String hoverStyle = defaultStyle.replace(
//                "dropshadow(gaussian, " + shadowColor + ", 6, 0, 0, 2)",
//                "dropshadow(gaussian, " + shadowColor + ", 8, 0, 0, 3)"  // Subtle increase
//        );
//
//        container.setOnMouseEntered(e -> {
//            container.setStyle(hoverStyle);
//        });
//
//        container.setOnMouseExited(e -> {
//            container.setStyle(defaultStyle);
//        });
//
//        return container;
//    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════════════════════
     * STREAMLINED UPCOMING CARD - Replace the createEnhancedEventCard method in CalendarController
     * ═══════════════════════════════════════════════════════════════════════════════════════════════
     *
     * This creates a cleaner, more professional card design with:
     * - Subtle left border accent (not heavy gradients)
     * - Minimal badge usage
     * - Clear visual hierarchy
     * - Smooth hover effects
     */

    /**
     * Create a streamlined, professional card for the sidebar
     */
    private VBox createStreamlinedCard(PlannerItem item) {
        VBox card = new VBox(0);
        card.setPadding(new Insets(16, 18, 16, 18));
        card.setMaxWidth(Double.MAX_VALUE);

        boolean isTask = item.getItemType() == PlannerItemType.TASK;
        boolean isCompleted = checkItemCompletion(item);
        String accentColor = getAccentColor(item);

        // Base card styling with left accent border
        String baseStyle = String.format(
                "-fx-background-color: #ffffff; " +
                        "-fx-background-radius: 16; " +
                        "-fx-border-radius: 16; " +
                        "-fx-border-width: 0 0 0 4; " +
                        "-fx-border-color: %s; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2); " +
                        "-fx-cursor: hand;",
                accentColor
        );
        card.setStyle(baseStyle);

        // ════════════════════════════════════════════════════════════════════════════
        // HEADER ROW: Type badge + Visibility + Settings
        // ════════════════════════════════════════════════════════════════════════════
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        // Type badge (Task/Event)
        Label typeBadge = new Label(isTask ? "Task" : "Event");
        typeBadge.setStyle(String.format(
                "-fx-font-size: 10; " +
                        "-fx-font-weight: 700; " +
                        "-fx-padding: 4 10; " +
                        "-fx-background-radius: 20; " +
                        "-fx-background-color: %s; " +
                        "-fx-text-fill: %s;",
                isTask ? "#d1fae5" : "#e0e7ff",
                isTask ? "#047857" : "#4338ca"
        ));
        headerRow.getChildren().add(typeBadge);

        // Visibility badge (only show if private - tasks)
        if (!isTask) {
            EventPlannerAdapter adapter = (EventPlannerAdapter) item;
            CalendarEvent event = adapter.getEvent();

            if (event.hasMeetingLink() && event.getMeetingLink() != null) {
                HBox meetingRow = new HBox(8);
                meetingRow.setAlignment(Pos.CENTER_LEFT);
                meetingRow.setPadding(new Insets(8, 0, 0, 0));

                Label meetingIcon = new Label("🎥");
                meetingIcon.setStyle("-fx-font-size: 14;");

                Button joinBtn = new Button("Join Meeting");
                joinBtn.setStyle(
                        "-fx-background-color: #10b981; " +
                                "-fx-text-fill: white; " +
                                "-fx-font-size: 11; " +
                                "-fx-font-weight: 600; " +
                                "-fx-padding: 6 12; " +
                                "-fx-background-radius: 12; " +
                                "-fx-cursor: hand;"
                );

                joinBtn.setOnAction(e -> {
                    try {
                        // Open link in default browser
                        java.awt.Desktop.getDesktop().browse(new java.net.URI(event.getMeetingLink()));
                    } catch (Exception ex) {
                        System.err.println("Failed to open meeting link: " + ex.getMessage());
                    }
                });

                meetingRow.getChildren().addAll(meetingIcon, joinBtn);
                card.getChildren().add(meetingRow);
            }

            // Show shared status
            if (event.getVisibility() == CalendarEvent.EventVisibility.SHARED) {
                HBox sharedRow = new HBox(6);
                sharedRow.setAlignment(Pos.CENTER_LEFT);
                sharedRow.setPadding(new Insets(4, 0, 0, 0));

                Label sharedIcon = new Label("👥");
                Label sharedText = new Label("Shared with " +
                        (event.getSharedWithUserIds().size() + event.getSharedWithEmails().size()) +
                        " people");
                sharedText.setStyle("-fx-font-size: 10; -fx-text-fill: #64748b;");

                sharedRow.getChildren().addAll(sharedIcon, sharedText);
                card.getChildren().add(sharedRow);
            }
        }

        // Completed badge (if completed)
        if (isCompleted) {
            Label completedBadge = new Label("✓ Done");
            completedBadge.setStyle(
                    "-fx-font-size: 10; " +
                            "-fx-font-weight: 600; " +
                            "-fx-padding: 4 8; " +
                            "-fx-background-radius: 20; " +
                            "-fx-background-color: #d1fae5; " +
                            "-fx-text-fill: #047857;"
            );
            headerRow.getChildren().add(completedBadge);
        }

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        headerRow.getChildren().add(headerSpacer);

        // Settings button
        Button settingsBtn = createMinimalSettingsButton(item);
        headerRow.getChildren().add(settingsBtn);

        // ════════════════════════════════════════════════════════════════════════════
        // TITLE
        // ════════════════════════════════════════════════════════════════════════════
        Label titleLabel = new Label(item.getTitle());
        titleLabel.setWrapText(true);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setPadding(new Insets(12, 0, 10, 0));

        if (isCompleted) {
            titleLabel.setStyle(
                    "-fx-font-size: 15; " +
                            "-fx-font-weight: 600; " +
                            "-fx-text-fill: #94a3b8; " +
                            "-fx-strikethrough: true;"
            );
        } else {
            titleLabel.setStyle(
                    "-fx-font-size: 15; " +
                            "-fx-font-weight: 600; " +
                            "-fx-text-fill: #1e293b;"
            );
        }

        // ════════════════════════════════════════════════════════════════════════════
        // META ROW: Date and Time
        // ════════════════════════════════════════════════════════════════════════════
        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        // Date
        HBox dateBox = new HBox(6);
        dateBox.setAlignment(Pos.CENTER_LEFT);
        Label dateIcon = new Label("📅");
        dateIcon.setStyle("-fx-font-size: 12;");
        Label dateText = new Label(formatDate(item.getDate()));
        dateText.setStyle("-fx-font-size: 12; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        dateBox.getChildren().addAll(dateIcon, dateText);

        // Time
        HBox timeBox = new HBox(6);
        timeBox.setAlignment(Pos.CENTER_LEFT);
        Label timeIcon = new Label("🕐");
        timeIcon.setStyle("-fx-font-size: 12;");
        Label timeText = new Label(
                item.getStartTime().format(timeFormatter) + " - " +
                        item.getEndTime().format(timeFormatter)
        );
        timeText.setStyle("-fx-font-size: 12; -fx-font-weight: 500; -fx-text-fill: #64748b;");
        timeBox.getChildren().addAll(timeIcon, timeText);

        metaRow.getChildren().addAll(dateBox, timeBox);

        // ════════════════════════════════════════════════════════════════════════════
        // FOOTER ROW: Priority indicator + User (for tasks)
        // ════════════════════════════════════════════════════════════════════════════
        HBox footerRow = new HBox(12);
        footerRow.setAlignment(Pos.CENTER_LEFT);
        footerRow.setPadding(new Insets(10, 0, 0, 0));

        // Priority indicator
        HBox priorityBox = new HBox(6);
        priorityBox.setAlignment(Pos.CENTER_LEFT);

        // Priority dot
        Region priorityDot = new Region();
        priorityDot.setMinSize(8, 8);
        priorityDot.setMaxSize(8, 8);
        priorityDot.setStyle(String.format(
                "-fx-background-color: %s; -fx-background-radius: 4;",
                accentColor
        ));

        Label priorityText = new Label(getPriorityText(item.getPriorityLevel()));
        priorityText.setStyle(
                "-fx-font-size: 11; " +
                        "-fx-font-weight: 600; " +
                        "-fx-text-fill: #94a3b8;"
        );
        priorityBox.getChildren().addAll(priorityDot, priorityText);
        footerRow.getChildren().add(priorityBox);

        // User badge (for tasks only)
        if (isTask) {
            TaskPlannerAdapter adapter = (TaskPlannerAdapter) item;
            String username = adapter.getTask().getUser() != null
                    ? adapter.getTask().getUser().getUsername()
                    : "Unknown";

            Label userBadge = new Label("👤 " + username);
            userBadge.setStyle(
                    "-fx-font-size: 11; " +
                            "-fx-font-weight: 500; " +
                            "-fx-padding: 3 8; " +
                            "-fx-background-radius: 12; " +
                            "-fx-background-color: #f0f9ff; " +
                            "-fx-text-fill: #0369a1;"
            );
            footerRow.getChildren().add(userBadge);
        }

        // ════════════════════════════════════════════════════════════════════════════
        // ASSEMBLE CARD
        // ════════════════════════════════════════════════════════════════════════════
        card.getChildren().addAll(headerRow, titleLabel, metaRow, footerRow);

        // ════════════════════════════════════════════════════════════════════════════
        // HOVER EFFECT
        // ════════════════════════════════════════════════════════════════════════════
        String hoverStyle = String.format(
                "-fx-background-color: #fafbfc; " +
                        "-fx-background-radius: 16; " +
                        "-fx-border-radius: 16; " +
                        "-fx-border-width: 0 0 0 4; " +
                        "-fx-border-color: %s; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4); " +
                        "-fx-cursor: hand;",
                accentColor
        );

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(baseStyle));

        // Double-click to edit
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (isTask) {
                    showTaskDialog((TaskPlannerAdapter) item);
                } else {
                    showEventDialog((EventPlannerAdapter) item);
                }
            }
        });

        return card;
    }

    /**
     * Get accent color based on priority
     */
    private String getAccentColor(PlannerItem item) {
        return switch (item.getPriorityLevel()) {
            case 3 -> "#ef4444"; // Urgent - Red
            case 2 -> "#f97316"; // Important - Orange
            case 1 -> "#3b82f6"; // Standard - Blue
            default -> "#94a3b8"; // Optional - Gray
        };
    }

    /**
     * Format date in a friendly way
     */
    private String formatDate(LocalDate date) {
        LocalDate today = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        if (date.equals(today)) {
            return "Today";
        } else if (date.equals(tomorrow)) {
            return "Tomorrow";
        } else if (date.isBefore(today.plusWeeks(1))) {
            return date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.getDefault());
        } else {
            return date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"));
        }
    }

    /**
     * Create minimal settings button
     */
    private Button createMinimalSettingsButton(PlannerItem item) {
        Button btn = new Button("•••");
        btn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #94a3b8; " +
                        "-fx-font-size: 14; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 4 8; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        );

        btn.setOnMouseEntered(e -> btn.setStyle(
                "-fx-background-color: #f1f5f9; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-font-size: 14; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 4 8; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        ));

        btn.setOnMouseExited(e -> btn.setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-text-fill: #94a3b8; " +
                        "-fx-font-size: 14; " +
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 4 8; " +
                        "-fx-background-radius: 6; " +
                        "-fx-cursor: hand;"
        ));

        btn.setOnAction(e -> showEnhancedItemMenu(btn, item));

        return btn;
    }

    /**
     * Update the setupUpcomingItemsList to use the new card style
     */
    private void setupUpcomingItemsList() {
        if (upcomingItemsList != null) {
            upcomingItemsList.setCellFactory(listView -> new ListCell<>() {
                @Override
                protected void updateItem(PlannerItem item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                        setStyle("-fx-background-color: transparent;");
                    } else {
                        VBox card = createStreamlinedCard(item);
                        setGraphic(card);
                        setStyle("-fx-background-color: transparent; -fx-padding: 0 0 8 0;");
                    }
                }
            });

            // Remove default selection styling
            upcomingItemsList.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-background-insets: 0; " +
                            "-fx-padding: 0;"
            );
        }
    }
    /**
     * ✨ NEW: Show add menu with specific time
     */
    private void showAddItemMenuAtTime(Node owner, LocalDate date, int hour, double x, double y) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 5); " +
                        "-fx-padding: 6;"
        );

        MenuItem addTaskItem = new MenuItem("📋 Add Task at " + String.format("%02d:00", hour));
        addTaskItem.setStyle("-fx-padding: 10 16; -fx-font-size: 13; -fx-font-weight: 500;");
        addTaskItem.setOnAction(e -> {
            Task newTask = new Task();
            newTask.setDueDate(date.atTime(hour, 0));
            newTask.setUser(currentUser);
            newTask.setStatus(Status.TODO);
            newTask.setPriority(com.smarttask.model.Priority.MEDIUM);
            showTaskDialog(new TaskPlannerAdapter(newTask));
        });

        MenuItem addEventItem = new MenuItem("📅 Add Event at " + String.format("%02d:00", hour));
        addEventItem.setStyle("-fx-padding: 10 16; -fx-font-size: 13; -fx-font-weight: 500;");
        addEventItem.setOnAction(e -> {
            CalendarEvent newEvent = new CalendarEvent();
            newEvent.setDate(date);
            newEvent.setStartTime(LocalTime.of(hour, 0));
            newEvent.setEndTime(LocalTime.of(hour + 1, 0));
            newEvent.setPriority(CalendarEvent.Priority.STANDARD);
            newEvent.setEventType(CalendarEvent.EventType.ONE_TIME_EVENT);
            showEventDialog(new EventPlannerAdapter(newEvent));
        });

        menu.getItems().addAll(addTaskItem, addEventItem);
        menu.show(owner, x, y);
    }

    /**
     * ✨ NEW: Create modern settings button - SMALLER & COMPACT
     */
    private Button createModernSettingsButton(PlannerItem item) {
        Button button = new Button("⚙");
        button.setStyle(
                "-fx-background-color: rgba(255, 255, 255, 0.9); " +
                        "-fx-text-fill: #000000; " +
                        "-fx-font-size: 14; " +  // Reduced from 16
                        "-fx-font-weight: bold; " +
                        "-fx-padding: 4 8; " +  // Reduced from 6 10
                        "-fx-cursor: hand; " +
                        "-fx-background-radius: 6; " +  // Reduced from 8
                        "-fx-border-radius: 6; " +
                        "-fx-border-color: rgba(0, 0, 0, 0.1); " +
                        "-fx-border-width: 1;"
        );

        button.setOnMouseEntered(e -> {
            button.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-text-fill: #000000; " +
                            "-fx-font-size: 14; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 4 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-radius: 6; " +
                            "-fx-border-color: rgba(0, 0, 0, 0.15); " +
                            "-fx-border-width: 1; " +
                            "-fx-effect: dropshadow(gaussian, rgba(0, 0, 0, 0.12), 3, 0, 0, 1);"  // Smaller shadow
            );
        });

        button.setOnMouseExited(e -> {
            button.setStyle(
                    "-fx-background-color: rgba(255, 255, 255, 0.9); " +
                            "-fx-text-fill: #1e293b; " +
                            "-fx-font-size: 14; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 4 8; " +
                            "-fx-cursor: hand; " +
                            "-fx-background-radius: 6; " +
                            "-fx-border-radius: 6; " +
                            "-fx-border-color: rgba(0, 0, 0, 0.1); " +
                            "-fx-border-width: 1;"
            );
        });

        button.setOnAction(e -> {
            showEnhancedItemMenu(button, item);
        });

        return button;
    }

    private void showEnhancedItemMenu(Node owner, PlannerItem item) {
        ContextMenu menu = new ContextMenu();

        // Modern menu styling
        menu.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-border-radius: 12; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-width: 1; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 20, 0, 0, 8); " +
                        "-fx-padding: 6;"
        );

        boolean isCompleted = checkItemCompletion(item);

        // ════════════════════════════════════════════════════════════════════════════
        // EDIT - Standard MenuItem (CSS handles hover)
        // ════════════════════════════════════════════════════════════════════════════
        MenuItem editItem = new MenuItem("Edit");
        editItem.setStyle("-fx-padding: 10 16;");
        editItem.setOnAction(e -> {
            if (item.getItemType() == PlannerItemType.TASK) {
                showTaskDialog((TaskPlannerAdapter) item);
            } else {
                showEventDialog((EventPlannerAdapter) item);
            }
        });

        // ════════════════════════════════════════════════════════════════════════════
        // MARK AS COMPLETE / INCOMPLETE
        // ════════════════════════════════════════════════════════════════════════════
        MenuItem toggleCompleteItem = new MenuItem(
                isCompleted ? "Mark as Incomplete" : "Mark as Complete"
        );
        toggleCompleteItem.setStyle("-fx-padding: 10 16;");
        toggleCompleteItem.setOnAction(e -> {
            if (item.getItemType() == PlannerItemType.TASK) {
                TaskPlannerAdapter adapter = (TaskPlannerAdapter) item;
                Task task = adapter.getTask();
                task.setStatus(task.getStatus() == Status.COMPLETED ? Status.TODO : Status.COMPLETED);
                plannerManager.updateTask(task);
            } else {
                EventPlannerAdapter adapter = (EventPlannerAdapter) item;
                CalendarEvent event = adapter.getEvent();
                event.toggleCompleted();
                plannerManager.updateEvent(event);
            }
            updateView();
        });

        // ════════════════════════════════════════════════════════════════════════════
        // LOCATE IN CALENDAR
        // ════════════════════════════════════════════════════════════════════════════
        MenuItem locateItem = new MenuItem("Locate in Calendar");
        locateItem.setStyle("-fx-padding: 10 16;");
        locateItem.setOnAction(e -> {
            currentDate = item.getDate();
            currentView = ViewType.DAY;
            dayViewButton.setSelected(true);
            updateView();
        });

        // ════════════════════════════════════════════════════════════════════════════
        // SEPARATOR
        // ════════════════════════════════════════════════════════════════════════════
        SeparatorMenuItem separator = new SeparatorMenuItem();

        // ════════════════════════════════════════════════════════════════════════════
        // DELETE - Red text via inline style
        // ════════════════════════════════════════════════════════════════════════════
        MenuItem deleteItem = new MenuItem("🗑  Delete");
        deleteItem.setStyle("-fx-padding: 10 16; -fx-text-fill: #ef4444;");
        deleteItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete " + (item.getItemType() == PlannerItemType.TASK ? "Task" : "Event"));
            alert.setHeaderText("Delete \"" + item.getTitle() + "\"?");
            alert.setContentText("This action cannot be undone.");

            // Style the dialog
            DialogPane dialogPane = alert.getDialogPane();
            dialogPane.setStyle("-fx-background-color: white;");

            Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
            Button cancelButton = (Button) dialogPane.lookupButton(ButtonType.CANCEL);

            if (okButton != null) {
                okButton.setText("Delete");
                okButton.setStyle(
                        "-fx-background-color: #ef4444; " +
                                "-fx-text-fill: white; " +
                                "-fx-background-radius: 8; " +
                                "-fx-padding: 8 20; " +
                                "-fx-font-weight: 600; " +
                                "-fx-cursor: hand;"
                );
            }

            if (cancelButton != null) {
                cancelButton.setStyle(
                        "-fx-background-color: white; " +
                                "-fx-text-fill: #374151; " +
                                "-fx-border-color: #d1d5db; " +
                                "-fx-border-radius: 8; " +
                                "-fx-background-radius: 8; " +
                                "-fx-padding: 8 20; " +
                                "-fx-font-weight: 600; " +
                                "-fx-cursor: hand;"
                );
            }

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (item.getItemType() == PlannerItemType.TASK) {
                        plannerManager.removeTask(((TaskPlannerAdapter) item).getTask());
                    } else {
                        plannerManager.removeEvent(((EventPlannerAdapter) item).getEvent());
                    }
                    updateView();
                }
            });
        });

        // Add all items to menu
        menu.getItems().addAll(editItem, toggleCompleteItem, locateItem, separator, deleteItem);

        // Show menu
        menu.show(owner, javafx.geometry.Side.BOTTOM, 0, 5);
    }
    /**
     * ✨ NEW: Create modern menu item with icon, text, and hover effect
     */
    private CustomMenuItem createModernMenuItem(String icon, String text, String accentColor, Runnable action) {
        // Create container for the menu item
        HBox container = new HBox(12);
        container.setPadding(new Insets(10, 16, 10, 16));
        container.setAlignment(Pos.CENTER_LEFT);
        container.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-cursor: hand;"
        );

        // Icon label
        Label iconLabel = new Label(icon);
        iconLabel.setStyle(
                "-fx-font-size: 16; " +
                        "-fx-min-width: 24; " +
                        "-fx-alignment: center;"
        );

        // Text label
        Label textLabel = new Label(text);
        textLabel.setStyle(
                "-fx-text-fill: #1f2937; " +
                        "-fx-font-size: 14; " +
                        "-fx-font-weight: 500;"
        );

        // Add hover effect based on accent color
        container.setOnMouseEntered(e -> {
            container.setStyle(
                    "-fx-background-color: " + accentColor + "20; " +  // ~8% opacity
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand;"

            );
            textLabel.setStyle(
                    "-fx-text-fill: " + accentColor + "; " +
                            "-fx-font-size: 14; " +
                            "-fx-font-weight: 600;"
            );
        });

        container.setOnMouseExited(e -> {
            container.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-background-radius: 8; " +
                            "-fx-cursor: hand;"
            );
            textLabel.setStyle(
                    "-fx-text-fill: #1f2937; " +
                            "-fx-font-size: 14; " +
                            "-fx-font-weight: 500;"
            );
        });

        // Add click action
        container.setOnMouseClicked(e -> {
            if (action != null) {
                action.run();
            }
        });

        container.getChildren().addAll(iconLabel, textLabel);

        // Create custom menu item
        CustomMenuItem menuItem = new CustomMenuItem(container);
        menuItem.setHideOnClick(false);  // We'll handle hiding manually

        return menuItem;
    }

    /**
     * ✨ NEW: Create styled menu item
     */
    private MenuItem createStyledMenuItem(String text, String accentColor) {
        MenuItem item = new MenuItem(text);
        item.setStyle(
                "-fx-padding: 10 16; " +
                        "-fx-font-size: 13; " +
                        "-fx-font-weight: 500;"
        );

        // Note: JavaFX MenuItem doesn't support setOnMouseEntered/Exited
        // Use CSS pseudo-classes instead via stylesheet

        return item;
    }

    private String getPriorityText(int level) {
        return switch (level) {
            case 0 -> "Optional";
            case 1 -> "Standard";
            case 2 -> "Important";
            case 3 -> "Urgent";
            default -> "Unknown";
        };
    }

//    private void setupUpcomingItemsList() {
//        if (upcomingItemsList != null) {
//            upcomingItemsList.setCellFactory(listView -> new ListCell<>() {
//                @Override
//                protected void updateItem(PlannerItem item, boolean empty) {
//                    super.updateItem(item, empty);
//                    if (empty || item == null) {
//                        setText(null);
//                        setGraphic(null);
//                    } else {
//                        VBox container = createEnhancedEventCard(item);
//                        setGraphic(container);
//                    }
//                }
//            });
//        }
//    }

    private void updateUpcomingItems() {
        if (upcomingItemsList != null) {
            List<PlannerItem> upcoming = plannerManager.getUpcomingItems(30);

            if (!isShowCompleted()) {
                upcoming = upcoming.stream()
                        .filter(i -> !checkItemCompletion(i))
                        .toList();
            }

            upcoming = upcoming.stream()
                    .filter(this::matchesFilters)
                    .filter(this::matchesTypeFilter)
                    .limit(20)
                    .toList();

            upcomingItemsList.setItems(FXCollections.observableArrayList(upcoming));
        }
    }

    private boolean matchesTypeFilter(PlannerItem item) {
        boolean tasksEnabled = showTasks == null || showTasks.isSelected();
        boolean eventsEnabled = showEvents == null || showEvents.isSelected();

        if (item.getItemType() == PlannerItemType.TASK) {
            return tasksEnabled;
        } else {
            return eventsEnabled;
        }
    }

    private boolean matchesFilters(PlannerItem item) {
        int priorityLevel = item.getPriorityLevel();

        boolean optionalChecked = filterOptional == null || filterOptional.isSelected();
        boolean standardChecked = filterStandard == null || filterStandard.isSelected();
        boolean importantChecked = filterImportant == null || filterImportant.isSelected();
        boolean urgentChecked = filterUrgent == null || filterUrgent.isSelected();

        return switch (priorityLevel) {
            case 0 -> optionalChecked;
            case 1 -> standardChecked;
            case 2 -> importantChecked;
            case 3 -> urgentChecked;
            default -> true;
        };
    }

    private boolean isShowCompleted() {
        return showCompleted != null && showCompleted.isSelected();
    }
    private void updateView() {
        if (calendarContainer == null) return;

        switch (currentView) {
            case DAY -> renderDayView();
            case WEEK -> renderWeekView();
            case MONTH -> renderMonthView();
            case YEAR -> renderYearView();
            case KANBAN -> renderKanbanView();
        }

        updateDateLabel();
        updateUpcomingItems();
        updateSidebarStats();
    }

    private void renderKanbanView() {
        System.out.println("🎯 renderKanbanView() STARTED");

        if (kanbanBoard == null) {
            System.err.println("⚠️ kanbanBoard is NULL, initializing...");
            initializeKanbanBoard();
        }

        System.out.println("✅ KanbanBoard initialized: " + kanbanBoard);

        // Load all tasks for current user
        List<Task> allTasks = plannerManager.getAllTasks();
        System.out.println("📋 Loaded " + allTasks.size() + " tasks");

        // Apply filters if needed
        List<Task> filteredTasks = allTasks.stream()
                .filter(task -> {
                    // Show completed filter
                    if (!isShowCompleted() && task.getStatus() == Status.COMPLETED) {
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        System.out.println("🔍 After filtering: " + filteredTasks.size() + " tasks");

        kanbanBoard.loadTasks(filteredTasks);

        // Display in container
        System.out.println("🖼️ Setting Kanban board in container...");
        calendarContainer.getChildren().setAll(kanbanBoard);
        System.out.println("✅ renderKanbanView() COMPLETED");
    }

    private void updateDateLabel() {
        if (currentDateLabel == null) return;

        switch (currentView) {
            case DAY -> currentDateLabel.setText(currentDate.format(dayFormatter));
            case WEEK -> {
                LocalDate weekStart = currentDate.minusDays(currentDate.getDayOfWeek().getValue() - 1);
                LocalDate weekEnd = weekStart.plusDays(6);
                currentDateLabel.setText(weekStart.format(DateTimeFormatter.ofPattern("MMM d")) +
                        " - " + weekEnd.format(DateTimeFormatter.ofPattern("MMM d, yyyy")));
            }
            case MONTH -> currentDateLabel.setText(currentDate.format(monthYearFormatter));
            case YEAR -> currentDateLabel.setText(String.valueOf(currentDate.getYear()));
            case KANBAN -> currentDateLabel.setText("📋 Kanban Board");
        }
    }

    @FXML
    private void handlePrevious() {
        switch (currentView) {
            case DAY -> currentDate = currentDate.minusDays(1);
            case WEEK -> currentDate = currentDate.minusWeeks(1);
            case MONTH -> currentDate = currentDate.minusMonths(1);
            case YEAR -> currentDate = currentDate.minusYears(1);
        }
        updateView();
    }

    @FXML
    private void handleNext() {
        switch (currentView) {
            case DAY -> currentDate = currentDate.plusDays(1);
            case WEEK -> currentDate = currentDate.plusWeeks(1);
            case MONTH -> currentDate = currentDate.plusMonths(1);
            case YEAR -> currentDate = currentDate.plusYears(1);
        }
        updateView();
    }

    @FXML
    private void handleToday() {
        currentDate = LocalDate.now();
        updateView();
    }

    @FXML
    private void handleAddTask() {
        showTaskDialog(null);
    }

    @FXML
    private void handleAddEvent() {
        showEventDialog(null);
    }

    // VIEW RENDERING METHODS (keeping existing implementations)
//    private void renderDayView() {
//        ScrollPane scrollPane = new ScrollPane();
//        scrollPane.setFitToWidth(true);
//        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
//        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
//
//        VBox dayContainer = new VBox();
//        dayContainer.setStyle("-fx-background-color: white;");
//
//        GridPane timeGrid = new GridPane();
//        timeGrid.setVgap(0);
//        timeGrid.setHgap(0);
//
//        for (int hour = 0; hour < 24; hour++) {
//            HBox hourRow = createDayViewHourRow(hour, currentDate);
//            timeGrid.add(hourRow, 0, hour);
//        }
//
//        dayContainer.getChildren().add(timeGrid);
//        scrollPane.setContent(dayContainer);
//        scrollPane.setVvalue(8.0 / 24.0);
//
//        calendarContainer.getChildren().setAll(scrollPane);
//    }

    private void renderDayView() {
        // Create main container
        HBox dayContainer = new HBox(0);
        dayContainer.setStyle("-fx-background-color: white;");

        // Time labels column (left side)
        VBox timeColumn = new VBox(0);
        timeColumn.setMinWidth(55);
        timeColumn.setPrefWidth(55);
        timeColumn.setStyle("-fx-background-color: #fafbfc; -fx-border-color: #f1f5f9; -fx-border-width: 0 1 0 0;");

        // Events area (right side) - uses AnchorPane for absolute positioning
        AnchorPane eventsArea = new AnchorPane();
        eventsArea.setStyle("-fx-background-color: white;");
        HBox.setHgrow(eventsArea, Priority.ALWAYS);

        int totalHeight = 24 * HOUR_HEIGHT;
        eventsArea.setMinHeight(totalHeight);
        eventsArea.setPrefHeight(totalHeight);

        // Draw hour rows and grid lines
        for (int hour = 0; hour < 24; hour++) {
            // Time label
            HBox timeRow = new HBox();
            timeRow.setMinHeight(HOUR_HEIGHT);
            timeRow.setPrefHeight(HOUR_HEIGHT);
            timeRow.setAlignment(Pos.TOP_RIGHT);
            timeRow.setPadding(new Insets(0, 8, 0, 0));

            Label timeLabel = new Label(String.format("%02d:00", hour));
            timeLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-size: 11; -fx-font-weight: 500;");
            timeRow.getChildren().add(timeLabel);
            timeColumn.getChildren().add(timeRow);

            // Hour grid line
            Line hourLine = new Line();
            hourLine.setStartX(0);
            hourLine.setEndX(3000);
            hourLine.setStartY(hour * HOUR_HEIGHT);
            hourLine.setEndY(hour * HOUR_HEIGHT);
            hourLine.setStroke(Color.web("#e2e8f0"));
            hourLine.setStrokeWidth(1);
            eventsArea.getChildren().add(hourLine);

            // Create drop zone for each hour
            Pane dropZone = createHourDropZone(hour, HOUR_HEIGHT);
            AnchorPane.setTopAnchor(dropZone, (double) hour * HOUR_HEIGHT);
            AnchorPane.setLeftAnchor(dropZone, 0.0);
            AnchorPane.setRightAnchor(dropZone, 0.0);
            eventsArea.getChildren().add(dropZone);
        }

        // Get items for current date
        List<PlannerItem> dayItems = plannerManager.getItemsForDate(currentDate).stream()
                .filter(this::matchesFilters)
                .filter(this::matchesTypeFilter)
                .filter(i -> isShowCompleted() || !checkItemCompletion(i))
                .sorted(Comparator.comparing(PlannerItem::getStartTime))
                .collect(Collectors.toList());

        // Calculate overlapping groups and position items side by side
        List<List<PlannerItem>> overlapGroups = calculateOverlapGroups(dayItems);

        for (List<PlannerItem> group : overlapGroups) {
            positionItemsInGroup(group, eventsArea);
        }

        // Double-click to add item
        eventsArea.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                int clickedHour = (int) (e.getY() / HOUR_HEIGHT);
                clickedHour = Math.max(0, Math.min(23, clickedHour));
                showAddItemMenuAtTime(eventsArea, currentDate, clickedHour, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        dayContainer.getChildren().addAll(timeColumn, eventsArea);

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(dayContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background: white; -fx-background-color: white;");

        // Scroll to 7 AM by default
        scrollPane.setVvalue(7.0 / 24.0);

        calendarContainer.getChildren().setAll(scrollPane);
    }

// ════════════════════════════════════════════════════════════════════════════
// CREATE DROP ZONE FOR EACH HOUR
// ════════════════════════════════════════════════════════════════════════════

    private Pane createHourDropZone(int hour, int hourHeight) {
        Pane dropZone = new Pane();
        dropZone.setMinHeight(hourHeight);
        dropZone.setPrefHeight(hourHeight);
        dropZone.setStyle("-fx-background-color: transparent;");

        // Drag over
        dropZone.setOnDragOver(e -> {
            if (e.getGestureSource() != dropZone && draggedItem != null) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        // Drag entered - highlight
        dropZone.setOnDragEntered(e -> {
            if (e.getGestureSource() != dropZone && draggedItem != null) {
                dropZone.setStyle("-fx-background-color: rgba(59, 130, 246, 0.1); -fx-border-color: #3b82f6; -fx-border-width: 1; -fx-border-style: dashed;");
            }
            e.consume();
        });

        // Drag exited - remove highlight
        dropZone.setOnDragExited(e -> {
            dropZone.setStyle("-fx-background-color: transparent;");
            e.consume();
        });

        // Drop
        dropZone.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;

            if (db.hasString() && draggedItem != null) {
                // Calculate duration to preserve it
                long minutes = Duration.between(
                        draggedItem.getStartTime(),
                        draggedItem.getEndTime()
                ).toMinutes();

                LocalTime newStartTime = LocalTime.of(hour, 0);
                LocalTime newEndTime = newStartTime.plusMinutes(minutes);

                // Make sure end time doesn't go past midnight
                if (newEndTime.isBefore(newStartTime)) {
                    newEndTime = LocalTime.of(23, 59);
                }

                if (draggedItem.getItemType() == PlannerItemType.TASK) {
                    TaskPlannerAdapter adapter = (TaskPlannerAdapter) draggedItem;
                    adapter.setDate(currentDate);
                    adapter.setStartTime(newStartTime);
                    adapter.setEndTime(newEndTime);
                    plannerManager.updateTask(adapter.getTask());
                } else {
                    EventPlannerAdapter adapter = (EventPlannerAdapter) draggedItem;
                    adapter.setDate(currentDate);
                    adapter.setStartTime(newStartTime);
                    adapter.setEndTime(newEndTime);
                    plannerManager.updateEvent(adapter.getEvent());
                }

                success = true;
                updateView();
            }

            e.setDropCompleted(success);
            e.consume();
        });

        return dropZone;
    }

    // ════════════════════════════════════════════════════════════════════════════
// CALCULATE OVERLAPPING GROUPS
// ════════════════════════════════════════════════════════════════════════════

    private List<List<PlannerItem>> calculateOverlapGroups(List<PlannerItem> items) {
        List<List<PlannerItem>> groups = new ArrayList<>();

        if (items.isEmpty()) return groups;

        List<PlannerItem> sortedItems = new ArrayList<>(items);
        sortedItems.sort(Comparator.comparing(PlannerItem::getStartTime));

        List<PlannerItem> currentGroup = new ArrayList<>();
        LocalTime groupEnd = LocalTime.MIN;

        for (PlannerItem item : sortedItems) {
            LocalTime itemStart = item.getStartTime();
            LocalTime itemEnd = item.getEndTime();

            if (currentGroup.isEmpty() || itemStart.isBefore(groupEnd)) {
                currentGroup.add(item);
                if (itemEnd.isAfter(groupEnd)) {
                    groupEnd = itemEnd;
                }
            } else {
                groups.add(new ArrayList<>(currentGroup));
                currentGroup.clear();
                currentGroup.add(item);
                groupEnd = itemEnd;
            }
        }

        if (!currentGroup.isEmpty()) {
            groups.add(currentGroup);
        }

        return groups;
    }

// ════════════════════════════════════════════════════════════════════════════
// POSITION ITEMS SIDE BY SIDE - COMPACT VERSION
// ════════════════════════════════════════════════════════════════════════════

    private void positionItemsInGroup(List<PlannerItem> group, AnchorPane container) {
        if (group.isEmpty()) return;

        int groupSize = group.size();

        // ══════════════════════════════════════════════════════════════════════
        // CONFIGURABLE SIZING - Adjust these values to control card size
        // ══════════════════════════════════════════════════════════════════════
        double maxItemWidth = 280;   // Maximum width for a single item
        double minItemWidth = 120;   // Minimum width
        double itemGap = 6;          // Gap between items
        double leftMargin = 12;      // Left margin from time column
        double rightMargin = 20;     // Right margin

        for (int i = 0; i < groupSize; i++) {
            PlannerItem item = group.get(i);
            final int index = i;

            // Calculate vertical position based on time
            double startMinutes = item.getStartTime().getHour() * 60 + item.getStartTime().getMinute();
            double endMinutes = item.getEndTime().getHour() * 60 + item.getEndTime().getMinute();

            double top = (startMinutes / 60.0) * HOUR_HEIGHT;
            double height = ((endMinutes - startMinutes) / 60.0) * HOUR_HEIGHT;
            height = Math.max(height, 28); // Minimum height

            // Create the item card with initial width
            VBox itemCard = createCompactDayViewCard(item, maxItemWidth, height);

            // Position
            AnchorPane.setTopAnchor(itemCard, top + 2); // Small offset from grid line

            // Dynamic width calculation based on container size
            container.widthProperty().addListener((obs, oldVal, newVal) -> {
                double containerWidth = newVal.doubleValue();
                double availableWidth = containerWidth - leftMargin - rightMargin;

                // Calculate item width
                double itemWidth;
                if (groupSize == 1) {
                    // Single item: use max width or available, whichever is smaller
                    itemWidth = Math.min(maxItemWidth, availableWidth);
                } else {
                    // Multiple items: divide space
                    itemWidth = (availableWidth - (itemGap * (groupSize - 1))) / groupSize;
                    itemWidth = Math.min(itemWidth, maxItemWidth);
                    itemWidth = Math.max(itemWidth, minItemWidth);
                }

                double left = leftMargin + (index * (itemWidth + itemGap));

                itemCard.setPrefWidth(itemWidth);
                itemCard.setMaxWidth(itemWidth);
                AnchorPane.setLeftAnchor(itemCard, left);
            });

            // Set initial position
            double initialWidth = Math.min(maxItemWidth, 300);
            double initialLeft = leftMargin + (i * (initialWidth + itemGap));
            itemCard.setPrefWidth(initialWidth);
            itemCard.setMaxWidth(initialWidth);
            AnchorPane.setLeftAnchor(itemCard, initialLeft);

            container.getChildren().add(itemCard);
        }
    }

// ════════════════════════════════════════════════════════════════════════════
// CREATE COMPACT DAY VIEW CARD
// ════════════════════════════════════════════════════════════════════════════

    private VBox createCompactDayViewCard(PlannerItem item, double width, double height) {
        VBox card = new VBox(1);
        card.setPrefWidth(width);
        card.setMaxWidth(width);
        card.setPrefHeight(height);
        card.setMinHeight(28);
        card.setPadding(new Insets(5, 8, 5, 10));
        card.setAlignment(Pos.TOP_LEFT);

        boolean isTask = item.getItemType() == PlannerItemType.TASK;
        boolean isCompleted = checkItemCompletion(item);
        String baseColor = item.getColor();

        // Compact card styling with left accent border
        String normalStyle = String.format(
                "-fx-background-color: linear-gradient(to right, %s 0%%, %sE8 100%%); " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-radius: 5; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 3, 0, 0, 1);",
                baseColor + "F0",
                baseColor
        );
        card.setStyle(normalStyle);

        // Title row with icon
        HBox titleRow = new HBox(4);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        // Small type indicator
        Label typeIndicator = new Label(isTask ? "▪" : "▪");
        typeIndicator.setStyle("-fx-text-fill: white; -fx-font-size: 8;");

        // Title
        Label titleLabel = new Label(item.getTitle());
        titleLabel.setMaxWidth(width - 30);

        if (isCompleted) {
            titleLabel.setStyle(
                    "-fx-font-size: 11; " +
                            "-fx-font-weight: 600; " +
                            "-fx-text-fill: rgba(255,255,255,0.65); " +
                            "-fx-strikethrough: true;"
            );
        } else {
            titleLabel.setStyle(
                    "-fx-font-size: 11; " +
                            "-fx-font-weight: 600; " +
                            "-fx-text-fill: white;"
            );
        }

        titleRow.getChildren().addAll(typeIndicator, titleLabel);
        card.getChildren().add(titleRow);

        // Time label (show if enough height)
        if (height >= 38) {
            String timeText = item.getStartTime().format(timeFormatter) + " - " +
                    item.getEndTime().format(timeFormatter);
            Label timeLabel = new Label(timeText);
            timeLabel.setStyle(
                    "-fx-font-size: 9; " +
                            "-fx-text-fill: rgba(255,255,255,0.8);"
            );
            card.getChildren().add(timeLabel);
        }

        // User badge for tasks (show if enough height)
        if (height >= 55 && isTask) {
            TaskPlannerAdapter adapter = (TaskPlannerAdapter) item;
            String username = adapter.getTask().getUser() != null
                    ? adapter.getTask().getUser().getUsername()
                    : "";
            if (!username.isEmpty()) {
                Label userLabel = new Label("👤 " + username);
                userLabel.setStyle("-fx-font-size: 8; -fx-text-fill: rgba(255,255,255,0.7);");
                card.getChildren().add(userLabel);
            }
        }

        // Hover effect
        String hoverStyle = String.format(
                "-fx-background-color: linear-gradient(to right, %s 0%%, %s 100%%); " +
                        "-fx-background-radius: 5; " +
                        "-fx-border-radius: 5; " +
                        "-fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);",
                baseColor,
                baseColor + "D0"
        );

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e -> card.setStyle(normalStyle));

        // Double-click to edit
        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (isTask) {
                    showTaskDialog((TaskPlannerAdapter) item);
                } else {
                    showEventDialog((EventPlannerAdapter) item);
                }
                e.consume();
            }
        });

        // Context menu
        setupItemContextMenu(card, item);

        // ══════════════════════════════════════════════════════════════════════
        // DRAG SOURCE SETUP
        // ══════════════════════════════════════════════════════════════════════
        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(item.getId());
            db.setContent(content);

            // Store reference
            draggedItem = item;
            draggedItemCard = card;

            // Visual feedback
            card.setOpacity(0.5);

            e.consume();
        });

        card.setOnDragDone(e -> {
            card.setOpacity(1.0);
            draggedItem = null;
            draggedItemCard = null;
            e.consume();
        });

        return card;
    }


    private HBox createDayViewHourRow(int hour, LocalDate date) {
        HBox row = new HBox();
        row.setMinHeight(60);
        row.setPrefHeight(60);
        row.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

        Label timeLabel = new Label(String.format("%02d:00", hour));
        timeLabel.setPrefWidth(70);
        timeLabel.setAlignment(Pos.TOP_RIGHT);
        timeLabel.setPadding(new Insets(5, 10, 0, 0));
        timeLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 12;");

        VBox itemsArea = new VBox(3);
        itemsArea.setPadding(new Insets(5));
        HBox.setHgrow(itemsArea, Priority.ALWAYS);

        setupDropTarget(itemsArea, date, hour);

        List<PlannerItem> hourItems = plannerManager.getItemsForDate(date).stream()
                .filter(i -> i.getStartTime().getHour() == hour)
                .filter(this::matchesFilters)
                .filter(this::matchesTypeFilter)
                .filter(i -> isShowCompleted() || !checkItemCompletion(i))
                .sorted(Comparator.comparing(PlannerItem::getStartTime))
                .toList();

        for (PlannerItem item : hourItems) {
            VBox itemCard = createDraggablePlannerItemCard(item);
            itemsArea.getChildren().add(itemCard);
        }

        row.getChildren().addAll(timeLabel, itemsArea);
        itemsArea.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                showAddItemMenuAtTime(itemsArea, date, hour, e.getScreenX(), e.getScreenY());
                e.consume();
            }
        });

        return row;
    }

    /**
     * ✨ ENHANCED: Create beautiful draggable card - COMPACT with GRADIENT
     */
    private VBox createDraggablePlannerItemCard(PlannerItem item) {
        VBox card = new VBox(4);  // Reduced from 6
        card.setPadding(new Insets(10, 12, 10, 12));  // Reduced from 12, 14

        String backgroundColor = item.getColor();
        String borderStyle = item.getItemType() == PlannerItemType.TASK ? "dashed" : "solid";

        // GRADIENT colors for consistency with sidebar cards
        card.setStyle(
                "-fx-background-color: linear-gradient(to bottom right, " + backgroundColor + "D0, " + backgroundColor + "A0); " +
                        "-fx-background-radius: 8; " +  // Slightly smaller
                        "-fx-cursor: hand; " +
                        "-fx-border-style: " + borderStyle + "; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-border-color: derive(" + backgroundColor + ", -15%); " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 3, 0, 0, 1);"  // Smaller shadow
        );

        // Header with badges - SMALLER
        HBox headerBox = new HBox(5);  // Reduced from 6
        headerBox.setAlignment(Pos.CENTER_LEFT);

        String typeIcon = item.getItemType() == PlannerItemType.TASK ? "📋" : "📅";
        Label typeLabel = new Label(typeIcon);
        typeLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-size: 11; " +  // Reduced from 12
                        "-fx-background-color: rgba(0,0,0,0.25); " +
                        "-fx-padding: 2 6; " +  // Reduced from 3 8
                        "-fx-background-radius: 6;"  // Reduced from 8
        );
        headerBox.getChildren().add(typeLabel);

        if (item.getItemType() == PlannerItemType.TASK) {
            Label privateLabel = new Label("🔒");
            privateLabel.setStyle(
                    "-fx-text-fill: white; " +
                            "-fx-font-size: 10; " +  // Reduced from 11
                            "-fx-background-color: rgba(0,0,0,0.3); " +
                            "-fx-padding: 2 5; " +  // Reduced from 3 7
                            "-fx-background-radius: 6;"  // Reduced from 8
            );
            headerBox.getChildren().add(privateLabel);
            TaskPlannerAdapter adapter = (TaskPlannerAdapter) item;
            String username = adapter.getTask().getUser() != null
                    ? adapter.getTask().getUser().getUsername()
                    : "?";

            Label userLabel = new Label("👤 " + username);
            userLabel.setStyle(
                    "-fx-text-fill: white; " +
                            "-fx-font-size: 9; " +
                            "-fx-background-color: rgba(255,255,255,0.25); " +
                            "-fx-padding: 2 5; " +
                            "-fx-background-radius: 6;"
            );
            headerBox.getChildren().add(userLabel);
        }

        // Title - SMALLER
        boolean isCompleted = checkItemCompletion(item);
        Label titleLabel = new Label(item.getTitle());
        titleLabel.setStyle(
                "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 13;"  // Reduced from 14
        );
        if (isCompleted) {
            titleLabel.setStyle(titleLabel.getStyle() +
                    "-fx-strikethrough: true; " +
                    "-fx-opacity: 0.75;"
            );
        }

        // Time - SMALLER
        String timeText = item.getStartTime().format(timeFormatter) + " - " +
                item.getEndTime().format(timeFormatter);
        Label timeLabel = new Label(timeText);
        timeLabel.setStyle(
                "-fx-text-fill: rgba(255,255,255,0.95); " +
                        "-fx-font-size: 11; " +  // Reduced from 12
                        "-fx-font-weight: 600;"
        );

        card.getChildren().addAll(headerBox, titleLabel, timeLabel);

        card.setUserData(item);

        setupDragSource(card, item);
        setupItemContextMenu(card, item);

        card.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (item.getItemType() == PlannerItemType.TASK) {
                    showTaskDialog((TaskPlannerAdapter) item);
                } else {
                    showEventDialog((EventPlannerAdapter) item);
                }
            }
        });

        // Hover effect with stored styles
        final String baseStyle = card.getStyle();
        final String hoverStyle = baseStyle.replace(
                "dropshadow(gaussian, rgba(0,0,0,0.15), 4, 0, 0, 2)",
                "dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 3)"
        );

        card.setOnMouseEntered(e -> {
            card.setStyle(hoverStyle);
        });

        card.setOnMouseExited(e -> {
            card.setStyle(baseStyle);
        });

        return card;
    }

    private void setupDragSource(VBox card, PlannerItem item) {
        card.setOnDragDetected(e -> {
            Dragboard db = card.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(item.getId());
            db.setContent(content);

            draggedItem = item;
            draggedItemCard = card;
            card.setOpacity(0.5);

            e.consume();
        });

        card.setOnDragDone(e -> {
            card.setOpacity(1.0);
            draggedItem = null;
            draggedItemCard = null;
            e.consume();
        });
    }

    private void setupDropTarget(Node target, LocalDate date, int hour) {
        target.setOnDragOver(e -> {
            if (e.getGestureSource() != target && draggedItem != null) {
                e.acceptTransferModes(TransferMode.MOVE);
            }
            e.consume();
        });

        target.setOnDragEntered(e -> {
            if (e.getGestureSource() != target && draggedItem != null) {
                target.setStyle(target.getStyle() + "-fx-background-color: #eff6ff;");
            }
            e.consume();
        });

        target.setOnDragExited(e -> {
            target.setStyle(target.getStyle().replace("-fx-background-color: #eff6ff;", ""));
            e.consume();
        });

        target.setOnDragDropped(e -> {
            Dragboard db = e.getDragboard();
            boolean success = false;

            if (db.hasString() && draggedItem != null) {
                long minutes = Duration.between(
                        draggedItem.getStartTime(),
                        draggedItem.getEndTime()
                ).toMinutes();

                if (draggedItem.getItemType() == PlannerItemType.TASK) {
                    TaskPlannerAdapter adapter = (TaskPlannerAdapter) draggedItem;
                    adapter.setDate(date);
                    adapter.setStartTime(LocalTime.of(hour, 0));
                    adapter.setEndTime(LocalTime.of(hour, 0).plusMinutes(minutes));
                    plannerManager.updateTask(adapter.getTask());
                } else {
                    EventPlannerAdapter adapter = (EventPlannerAdapter) draggedItem;
                    adapter.setDate(date);
                    adapter.setStartTime(LocalTime.of(hour, 0));
                    adapter.setEndTime(LocalTime.of(hour, 0).plusMinutes(minutes));
                    plannerManager.updateEvent(adapter.getEvent());
                }

                updateView();
                success = true;
            }

            e.setDropCompleted(success);
            e.consume();
        });
    }

    private void renderWeekView() {
        GridPane weekGrid = new GridPane();
        weekGrid.setHgap(1);
        weekGrid.setVgap(1);
        weekGrid.setStyle("-fx-background-color: #e5e7eb;");
        weekGrid.setPadding(new Insets(10));

        LocalDate weekStart = currentDate.minusDays(currentDate.getDayOfWeek().getValue() - 1);

        // Header row with day names and dates
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            VBox headerCell = createWeekHeaderCell(day);
            weekGrid.add(headerCell, i, 0);
        }

        // Day columns
        for (int i = 0; i < 7; i++) {
            LocalDate day = weekStart.plusDays(i);
            VBox dayColumn = createWeekDayColumn(day);
            GridPane.setVgrow(dayColumn, javafx.scene.layout.Priority.ALWAYS);
            weekGrid.add(dayColumn, i, 1);
        }

        // Column constraints
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7);
            weekGrid.getColumnConstraints().add(column);
        }

        // Row constraints
        RowConstraints headerRow = new RowConstraints();
        RowConstraints contentRow = new RowConstraints();
        contentRow.setVgrow(javafx.scene.layout.Priority.ALWAYS);
        weekGrid.getRowConstraints().addAll(headerRow, contentRow);

        calendarContainer.getChildren().setAll(weekGrid);
    }

    private VBox createWeekHeaderCell(LocalDate day) {
        VBox header = new VBox(2);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(10));
        header.setStyle("-fx-background-color: #f3f4f6;");

        Label dayName = new Label(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()));
        dayName.setFont(Font.font("System", FontWeight.BOLD, 12));
        dayName.setStyle("-fx-text-fill: #6b7280;");

        Label dayNumber = new Label(String.valueOf(day.getDayOfMonth()));
        dayNumber.setFont(Font.font("System", FontWeight.NORMAL, 16));

        if (day.equals(LocalDate.now())) {
            dayNumber.setStyle("-fx-text-fill: white; -fx-background-color: #3b82f6; " +
                    "-fx-background-radius: 20; -fx-min-width: 32; -fx-min-height: 32; " +
                    "-fx-alignment: center;");
        }

        header.getChildren().addAll(dayName, dayNumber);
        return header;
    }

    private VBox createWeekDayColumn(LocalDate date) {
        VBox column = new VBox(3);
        column.setMinHeight(400);
        column.setPadding(new Insets(5));
        column.setStyle("-fx-background-color: white;");

        setupDropTarget(column, date, 9);

        List<PlannerItem> dayItems = plannerManager.getItemsForDate(date).stream()
                .filter(this::matchesFilters)
                .filter(this::matchesTypeFilter)
                .filter(i -> isShowCompleted() || !checkItemCompletion(i))
                .sorted(Comparator.comparing(PlannerItem::getStartTime))
                .toList();

        for (PlannerItem item : dayItems) {
            VBox itemCard = createDraggablePlannerItemCard(item);
            column.getChildren().add(itemCard);
        }

        column.setOnMouseClicked(e -> {
//            if (e.getClickCount() == 2) {
//                showAddItemMenu(column, date, e.getScreenX(), e.getScreenY());
//            }
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                showAddItemMenu(column, date, e.getScreenX(), e.getScreenY());
                e.consume();  // ← CRITICAL: prevent propagation
            }
        });

        return column;
    }

    private void showAddItemMenu(Node owner, LocalDate date, double x, double y) {
        ContextMenu menu = new ContextMenu();
        menu.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 15, 0, 0, 5); " +
                        "-fx-padding: 6;"
        );

        MenuItem addTaskItem = new MenuItem("📋 Add Task");
        addTaskItem.setStyle("-fx-padding: 10 16; -fx-font-size: 13; -fx-font-weight: 500;");
        addTaskItem.setOnAction(e -> {
            Task newTask = new Task();
            newTask.setDueDate(date.atTime(9, 0));
            newTask.setUser(currentUser);  // ✨ FIXED
            newTask.setStatus(Status.TODO);  // ✨ FIXED
            newTask.setPriority(com.smarttask.model.Priority.MEDIUM);  // ✨ FIXED
            showTaskDialog(new TaskPlannerAdapter(newTask));
        });

        MenuItem addEventItem = new MenuItem("📅 Add Event");
        addEventItem.setStyle("-fx-padding: 10 16; -fx-font-size: 13; -fx-font-weight: 500;");
        addEventItem.setOnAction(e -> {
            CalendarEvent newEvent = new CalendarEvent();
            newEvent.setDate(date);
            newEvent.setStartTime(LocalTime.of(9, 0));  // ✨ FIXED
            newEvent.setEndTime(LocalTime.of(10, 0));  // ✨ FIXED
            newEvent.setPriority(CalendarEvent.Priority.STANDARD);  // ✨ FIXED
            newEvent.setEventType(CalendarEvent.EventType.ONE_TIME_EVENT);  // ✨ FIXED
            showEventDialog(new EventPlannerAdapter(newEvent));
        });

        menu.getItems().addAll(addTaskItem, addEventItem);
        menu.show(owner, x, y);
    }

    private void renderMonthView() {
        GridPane monthGrid = new GridPane();
        monthGrid.setHgap(1);
        monthGrid.setVgap(1);
        monthGrid.setStyle("-fx-background-color: #e5e7eb;");
        monthGrid.setPadding(new Insets(10));

        // Day headers
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            Label header = new Label(dayNames[i]);
            header.setFont(Font.font("System", FontWeight.BOLD, 12));
            header.setMaxWidth(Double.MAX_VALUE);
            header.setAlignment(Pos.CENTER);
            header.setPadding(new Insets(10));
            header.setStyle("-fx-background-color: #f3f4f6; -fx-text-fill: #6b7280;");
            monthGrid.add(header, i, 0);
        }

        // Get first day of month
        LocalDate firstOfMonth = currentDate.withDayOfMonth(1);
        int firstDayOfWeek = firstOfMonth.getDayOfWeek().getValue();
        int daysInMonth = currentDate.lengthOfMonth();

        // Fill calendar
        for (int week = 0; week < 6; week++) {
            for (int day = 0; day < 7; day++) {
                int cellDay = week * 7 + day + 1 - (firstDayOfWeek - 1);

                VBox dayCell;
                if (cellDay > 0 && cellDay <= daysInMonth) {
                    LocalDate date = currentDate.withDayOfMonth(cellDay);
                    dayCell = createMonthDayCell(date);
                } else {
                    dayCell = new VBox();
                    dayCell.setStyle("-fx-background-color: #fafafa;");
                }

                GridPane.setHgrow(dayCell, javafx.scene.layout.Priority.ALWAYS);
                GridPane.setVgrow(dayCell, javafx.scene.layout.Priority.ALWAYS);
                monthGrid.add(dayCell, day, week + 1);
            }
        }

        // Column constraints
        for (int i = 0; i < 7; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7);
            monthGrid.getColumnConstraints().add(column);
        }

        // Row constraints
        RowConstraints headerRow = new RowConstraints();
        monthGrid.getRowConstraints().add(headerRow);

        for (int i = 0; i < 6; i++) {
            RowConstraints row = new RowConstraints();
            row.setVgrow(javafx.scene.layout.Priority.ALWAYS);
            monthGrid.getRowConstraints().add(row);
        }

        calendarContainer.getChildren().setAll(monthGrid);
    }

    private VBox createMonthDayCell(LocalDate date) {
        VBox cell = new VBox(3);
        cell.setMinHeight(80);
        cell.setPadding(new Insets(5));
        cell.setStyle("-fx-background-color: white;");

        setupDropTarget(cell, date, 9);

        // Day number
        Label dayLabel = new Label(String.valueOf(date.getDayOfMonth()));
        dayLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        if (date.equals(LocalDate.now())) {
            dayLabel.setStyle("-fx-text-fill: white; -fx-background-color: #3b82f6; " +
                    "-fx-background-radius: 16; -fx-min-width: 28; -fx-min-height: 28; " +
                    "-fx-alignment: center;");
            cell.setStyle("-fx-background-color: #eff6ff; -fx-border-color: #3b82f6; -fx-border-width: 2;");
        }

        cell.getChildren().add(dayLabel);

        // Items
        List<PlannerItem> dayItems = plannerManager.getItemsForDate(date).stream()
                .filter(this::matchesFilters)
                .filter(this::matchesTypeFilter)
                .filter(i -> isShowCompleted() || !checkItemCompletion(i))
                .sorted(Comparator.comparing(PlannerItem::getStartTime))
                .toList();

        int maxDisplay = 3;
        for (int i = 0; i < Math.min(dayItems.size(), maxDisplay); i++) {
            cell.getChildren().add(createMonthItemLabel(dayItems.get(i)));
        }

        if (dayItems.size() > maxDisplay) {
            Label moreLabel = new Label("+" + (dayItems.size() - maxDisplay) + " more");
            moreLabel.setStyle("-fx-text-fill: #6b7280; -fx-font-size: 10; -fx-cursor: hand;");
            moreLabel.setOnMouseClicked(e -> showDayItemsDialog(date, dayItems));
            cell.getChildren().add(moreLabel);
        }

        cell.setOnMouseClicked(e -> {
//            if (e.getClickCount() == 2) {
//                showAddItemMenu(cell, date, e.getScreenX(), e.getScreenY());
//            }
            if (e.getClickCount() == 2 && e.getButton() == MouseButton.PRIMARY) {
                showAddItemMenu(cell, date, e.getScreenX(), e.getScreenY());
                e.consume();  // ← CRITICAL: prevent propagation
            }
        });

        return cell;
    }

    private Label createMonthItemLabel(PlannerItem item) {
        String typeIcon = item.getItemType() == PlannerItemType.TASK ? "📋" : "📅";
        Label label = new Label(typeIcon + " " + item.getStartTime().format(timeFormatter) + " " + item.getTitle());
        label.setMaxWidth(Double.MAX_VALUE);

        String borderStyle = item.getItemType() == PlannerItemType.TASK ? "dashed" : "solid";

        label.setStyle(
                "-fx-background-color: " + item.getColor() + "; " +
                        "-fx-text-fill: white; " +
                        "-fx-padding: 2 5; " +
                        "-fx-background-radius: 3; " +
                        "-fx-font-size: 10; " +
                        "-fx-cursor: hand; " +
                        "-fx-border-style: " + borderStyle + "; " +
                        "-fx-border-width: 1; " +
                        "-fx-border-color: derive(" + item.getColor() + ", -20%);"
        );

        if (checkItemCompletion(item)) {
            label.setStyle(label.getStyle() + "-fx-opacity: 0.6; -fx-strikethrough: true;");
        }

        setupItemContextMenu(label, item);

        label.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                if (item.getItemType() == PlannerItemType.TASK) {
                    showTaskDialog((TaskPlannerAdapter) item);
                } else {
                    showEventDialog((EventPlannerAdapter) item);
                }
            }
            e.consume();
        });

        return label;
    }

    private void renderYearView() {
        GridPane yearGrid = new GridPane();
        yearGrid.setHgap(15);
        yearGrid.setVgap(15);
        yearGrid.setPadding(new Insets(20));

        for (int month = 1; month <= 12; month++) {
            VBox monthBox = createMiniMonthView(month);
            int row = (month - 1) / 3;
            int col = (month - 1) % 3;
            yearGrid.add(monthBox, col, row);
        }

        for (int i = 0; i < 3; i++) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(33.33);
            yearGrid.getColumnConstraints().add(column);
        }

        ScrollPane scrollPane = new ScrollPane(yearGrid);
        scrollPane.setFitToWidth(true);
        calendarContainer.getChildren().setAll(scrollPane);
    }

    private VBox createMiniMonthView(int month) {
        VBox monthBox = new VBox(5);
        monthBox.setStyle("-fx-background-color: white; -fx-padding: 10; " +
                "-fx-border-color: #e5e7eb; -fx-border-radius: 8; -fx-background-radius: 8;");

        LocalDate monthDate = LocalDate.of(currentDate.getYear(), month, 1);
        Label monthLabel = new Label(monthDate.getMonth().getDisplayName(TextStyle.FULL, Locale.getDefault()));
        monthLabel.setFont(Font.font("System", FontWeight.BOLD, 14));

        GridPane miniGrid = new GridPane();
        miniGrid.setHgap(2);
        miniGrid.setVgap(2);

        // Day headers
        String[] dayNames = {"M", "T", "W", "T", "F", "S", "S"};
        for (int i = 0; i < 7; i++) {
            Label header = new Label(dayNames[i]);
            header.setFont(Font.font("System", FontWeight.BOLD, 9));
            header.setMinWidth(22);
            header.setAlignment(Pos.CENTER);
            header.setStyle("-fx-text-fill: #6b7280;");
            miniGrid.add(header, i, 0);
        }

        // Days
        int firstDayOfWeek = monthDate.getDayOfWeek().getValue();
        int daysInMonth = monthDate.lengthOfMonth();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = monthDate.withDayOfMonth(day);
            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.setMinWidth(22);
            dayLabel.setMinHeight(22);
            dayLabel.setAlignment(Pos.CENTER);
            dayLabel.setFont(Font.font(9));

            if (date.equals(LocalDate.now())) {
                dayLabel.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 11;");
            } else if (plannerManager.hasItemsOn(date)) {
                dayLabel.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold;");
            }

            int row = (day + firstDayOfWeek - 2) / 7 + 1;
            int col = (day + firstDayOfWeek - 2) % 7;
            miniGrid.add(dayLabel, col, row);
        }

        monthBox.getChildren().addAll(monthLabel, miniGrid);

        // Click to navigate to month
        monthBox.setOnMouseClicked(e -> {
            currentDate = monthDate;
            currentView = ViewType.MONTH;
            monthViewButton.setSelected(true);
            updateView();
        });
        monthBox.setStyle(monthBox.getStyle() + "-fx-cursor: hand;");

        return monthBox;
    }

    private void showDayItemsDialog(LocalDate date, List<PlannerItem> items) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Items for " + date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy")));
        dialog.setHeaderText(items.size() + " item(s)");

        ListView<PlannerItem> listView = new ListView<>();
        listView.setItems(FXCollections.observableArrayList(items));
        listView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(PlannerItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(createStreamlinedCard(item));
                }
            }
        });
        listView.setPrefHeight(400);

        dialog.getDialogPane().setContent(listView);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }


    private void showTaskDialog(TaskPlannerAdapter existingAdapter) {
        Dialog<Task> dialog = new Dialog<>();

        // Window title (not visible in dialog, but set for window manager)
        dialog.setTitle(existingAdapter == null ? "Add Task" : "Edit Task");

        // Custom header
        VBox headerBox = new VBox(8);
        headerBox.setPadding(new Insets(20, 24, 16, 24));
        headerBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

        Label headerIcon = new Label("📋");
        headerIcon.setStyle("-fx-font-size: 24;");

        Label headerTitle = new Label(existingAdapter == null ? "Create New Task" : "Edit Task");
        headerTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label headerSubtitle = new Label(existingAdapter == null ?
                "Add a private task to your personal calendar" :
                "Update task details");
        headerSubtitle.setStyle("-fx-font-size: 13; -fx-text-fill: #6b7280;");

        headerBox.getChildren().addAll(headerIcon, headerTitle, headerSubtitle);

        // Buttons
        ButtonType saveButtonType = new ButtonType("Save Task", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OTHER);

        if (existingAdapter != null) {
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, deleteButtonType, ButtonType.CANCEL);
        } else {
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        }

        // Main content
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #f9fafb;");

        // Title section
        VBox titleSection = new VBox(8);
        Label titleLabel = new Label("Task Title *");
        titleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextField titleField = new TextField();
        titleField.setPromptText("Enter task title");
        titleField.setPrefHeight(40);
        titleField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 12; " +
                        "-fx-font-size: 14;"
        );

        titleSection.getChildren().addAll(titleLabel, titleField);

        // Description section
        VBox descSection = new VBox(8);
        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextArea descField = new TextArea();
        descField.setPromptText("Add task description (optional)");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        descField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 12; " +
                        "-fx-font-size: 14;"
        );

        descSection.getChildren().addAll(descLabel, descField);

// ============= PROJECT SELECTION SECTION =============
        VBox projectSection = new VBox(8);
        Label projectLabel = new Label("Project");
        projectLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        ComboBox<Project> projectCombo = new ComboBox<>();
        projectCombo.setPrefHeight(40);
        projectCombo.setMaxWidth(Double.MAX_VALUE);
        projectCombo.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 0 12;"
        );
        projectCombo.setPromptText("Select a project (optional)");

// ✨ ADD STRING CONVERTER - This is crucial!
        projectCombo.setConverter(new StringConverter<Project>() {
            @Override
            public String toString(Project project) {
                if (project == null) {
                    return "";
                }
                return project.getName() != null ? project.getName() : "No Project";
            }

            @Override
            public Project fromString(String string) {
                return null; // Not needed for display
            }
        });

// Load projects from database
        ProjectDAO projectDAO = new ProjectDAO();
        List<Project> userProjects = projectDAO.getProjectsByUserId(currentUser.getId());

// Add "No Project" option
        Project noProject = new Project();
        noProject.setId(null);
        noProject.setName("No Project");
        noProject.setColor(null);

        projectCombo.getItems().add(noProject);
        projectCombo.getItems().addAll(userProjects);

// Set default selection
        projectCombo.setValue(noProject);

// Custom cell factory for dropdown list
        projectCombo.setCellFactory(listView -> new ListCell<Project>() {
            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(10);
                    container.setAlignment(Pos.CENTER_LEFT);

                    // Color indicator
                    if (project.getColor() != null && !project.getColor().isEmpty()) {
                        Region colorDot = new Region();
                        colorDot.setMinSize(10, 10);
                        colorDot.setMaxSize(10, 10);
                        colorDot.setStyle("-fx-background-color: " + project.getColor() + "; -fx-background-radius: 5;");
                        container.getChildren().add(colorDot);
                    }

                    String displayName = project.getName() != null ? project.getName() : "No Project";
                    Label nameLabel = new Label(displayName);
                    nameLabel.setStyle("-fx-font-size: 13;");
                    container.getChildren().add(nameLabel);

                    setGraphic(container);
                }
            }
        });

// Button cell for selected value display
        projectCombo.setButtonCell(new ListCell<Project>() {
            @Override
            protected void updateItem(Project project, boolean empty) {
                super.updateItem(project, empty);
                if (empty || project == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(8);
                    container.setAlignment(Pos.CENTER_LEFT);
                    container.setPadding(new Insets(0, 0, 0, 0));

                    // Color dot
                    if (project.getColor() != null && !project.getColor().isEmpty()) {
                        Region colorDot = new Region();
                        colorDot.setMinSize(8, 8);
                        colorDot.setMaxSize(8, 8);
                        colorDot.setStyle("-fx-background-color: " + project.getColor() + "; -fx-background-radius: 4;");
                        container.getChildren().add(colorDot);
                    }

                    // Project name
                    String displayName = project.getName() != null ? project.getName() : "No Project";
                    Label nameLabel = new Label(displayName);
                    nameLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");
                    container.getChildren().add(nameLabel);

                    setGraphic(container);
                    setText(null); // ← CRITICAL: Set text to null when using graphic
                }
            }
        });

        projectSection.getChildren().addAll(projectLabel, projectCombo);

        // Date & Time section
        HBox dateTimeBox = new HBox(12);

        // Date
        VBox dateBox = new VBox(8);
        HBox.setHgrow(dateBox, Priority.ALWAYS);
        Label dateLabel = new Label("Due Date *");
        dateLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        DatePicker datePicker = new DatePicker();
        datePicker.setPrefHeight(40);
        datePicker.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        dateBox.getChildren().addAll(dateLabel, datePicker);

        // Time
        VBox timeBox = new VBox(8);
        Label timeLabel = new Label("Time *");
        timeLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox timeInputBox = new HBox(8);
        timeInputBox.setAlignment(Pos.CENTER_LEFT);

        Spinner<Integer> hourSpinner = new Spinner<>(0, 23, 9);
        Spinner<Integer> minuteSpinner = new Spinner<>(0, 59, 0, 15);
        hourSpinner.setEditable(true);
        minuteSpinner.setEditable(true);
        hourSpinner.setPrefWidth(70);
        minuteSpinner.setPrefWidth(70);
        hourSpinner.setPrefHeight(40);
        minuteSpinner.setPrefHeight(40);

        Label colonLabel = new Label(":");
        colonLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        timeInputBox.getChildren().addAll(hourSpinner, colonLabel, minuteSpinner);
        timeBox.getChildren().addAll(timeLabel, timeInputBox);

        dateTimeBox.getChildren().addAll(dateBox, timeBox);

        // Priority section
        VBox prioritySection = new VBox(8);
        Label priorityLabel = new Label("Priority *");
        priorityLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox priorityButtonsBox = new HBox(8);
        ToggleGroup priorityGroup = new ToggleGroup();

        // Priority buttons with colors
        ToggleButton[] priorityButtons = new ToggleButton[4];
        String[] priorityNames = {"Optional", "Standard", "Important", "Urgent"};
        String[] priorityColors = {"#9E9E9E", "#2196F3", "#FF9800", "#F44336"};
        com.smarttask.model.Priority[] priorityValues = {
                com.smarttask.model.Priority.LOW,
                com.smarttask.model.Priority.MEDIUM,
                com.smarttask.model.Priority.HIGH,
                com.smarttask.model.Priority.URGENT
        };

        for (int i = 0; i < 4; i++) {
            final int index = i;
            ToggleButton btn = new ToggleButton(priorityNames[i]);
            btn.setToggleGroup(priorityGroup);
            btn.setUserData(priorityValues[i]);
            btn.setPrefHeight(40);
            btn.setPrefWidth(100);

            final String color = priorityColors[i];
            btn.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: " + color + "; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-background-radius: 8; " +
                            "-fx-text-fill: " + color + "; " +
                            "-fx-font-size: 12; " +
                            "-fx-font-weight: 600; " +
                            "-fx-cursor: hand;"
            );

            btn.selectedProperty().addListener((obs, old, isSelected) -> {
                if (isSelected) {
                    btn.setStyle(
                            "-fx-background-color: " + color + "; " +
                                    "-fx-border-color: " + color + "; " +
                                    "-fx-border-width: 2; " +
                                    "-fx-border-radius: 8; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 12; " +
                                    "-fx-font-weight: 600; " +
                                    "-fx-cursor: hand;"
                    );
                } else {
                    btn.setStyle(
                            "-fx-background-color: white; " +
                                    "-fx-border-color: " + color + "; " +
                                    "-fx-border-width: 2; " +
                                    "-fx-border-radius: 8; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-text-fill: " + color + "; " +
                                    "-fx-font-size: 12; " +
                                    "-fx-font-weight: 600; " +
                                    "-fx-cursor: hand;"
                    );
                }
            });

            priorityButtons[i] = btn;
            priorityButtonsBox.getChildren().add(btn);
        }

        prioritySection.getChildren().addAll(priorityLabel, priorityButtonsBox);

        // Status section
        VBox statusSection = new VBox(8);
        Label statusLabel = new Label("Status *");
        statusLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        ComboBox<Status> statusCombo = new ComboBox<>();
        statusCombo.getItems().addAll(Status.values());
        statusCombo.setPrefHeight(40);
        statusCombo.setPrefWidth(200);
        statusCombo.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        statusSection.getChildren().addAll(statusLabel, statusCombo);
        // ============= CREATED BY SECTION =============
        VBox createdBySection = new VBox(8);
        Label createdByLabel = new Label("Created By");
        createdByLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox userInfoBox = new HBox(10);
        userInfoBox.setAlignment(Pos.CENTER_LEFT);
        userInfoBox.setPadding(new Insets(10));
        userInfoBox.setStyle(
                "-fx-background-color: #f0f9ff; " +
                        "-fx-border-color: #bae6fd; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-border-width: 1;"
        );

// User icon
        Label userIcon = new Label("👤");
        userIcon.setStyle("-fx-font-size: 18;");

// User details
        VBox userDetails = new VBox(2);

        String displayUsername = existingAdapter != null && existingAdapter.getTask().getUser() != null
                ? existingAdapter.getTask().getUser().getUsername()
                : (currentUser != null ? currentUser.getUsername() : "Unknown");

        Label usernameLabel = new Label(displayUsername);
        usernameLabel.setStyle(
                "-fx-text-fill: #0369a1; " +
                        "-fx-font-size: 14; " +
                        "-fx-font-weight: 700;"
        );

        Label roleLabel = new Label("Task Owner");
        roleLabel.setStyle(
                "-fx-text-fill: #64748b; " +
                        "-fx-font-size: 11; " +
                        "-fx-font-weight: 500;"
        );

        userDetails.getChildren().addAll(usernameLabel, roleLabel);
        userInfoBox.getChildren().addAll(userIcon, userDetails);
        createdBySection.getChildren().addAll(createdByLabel, userInfoBox);

        // Populate if editing
        if (existingAdapter != null) {
            Task task = existingAdapter.getTask();
            titleField.setText(task.getTitle());
            descField.setText(task.getDescription());
            datePicker.setValue(existingAdapter.getDate());
            hourSpinner.getValueFactory().setValue(existingAdapter.getStartTime().getHour());
            minuteSpinner.getValueFactory().setValue(existingAdapter.getStartTime().getMinute());
            statusCombo.setValue(task.getStatus());

            // Set selected project
            if (task.getProjectId() != null && !task.getProjectId().isEmpty()) {
                projectCombo.getItems().stream()
                        .filter(p -> p.getId() != null && p.getId().equals(task.getProjectId()))
                        .findFirst()
                        .ifPresent(projectCombo::setValue);}
            // Select priority button
            com.smarttask.model.Priority taskPriority = task.getPriority();
            for (int i = 0; i < priorityValues.length; i++) {
                if (priorityValues[i] == taskPriority) {
                    priorityButtons[i].setSelected(true);
                    break;
                }
            }
        } else {
            datePicker.setValue(currentDate);
            priorityButtons[1].setSelected(true); // Standard
            statusCombo.setValue(Status.TODO);
        }

        mainContent.getChildren().addAll(
                titleSection,
                descSection,
                projectSection,
                dateTimeBox,
                prioritySection,
                statusSection,
                createdBySection
        );

        // Combine header and content
        VBox fullContent = new VBox(0);
        fullContent.getChildren().addAll(headerBox, mainContent);

        dialog.getDialogPane().setContent(fullContent);
        dialog.getDialogPane().setStyle("-fx-background-color: #f9fafb;");

        // Style buttons
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setStyle(
                "-fx-background-color: #3b82f6; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 20; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand;"
        );

        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: #374151; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 20; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand;"
        );

        if (existingAdapter != null) {
            Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
            deleteButton.setStyle(
                    "-fx-background-color: #ef4444; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 10 20; " +
                            "-fx-font-weight: 600; " +
                            "-fx-cursor: hand;"
            );

            deleteButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Delete Task");
                confirmAlert.setHeaderText("Delete \"" + existingAdapter.getTask().getTitle() + "\"?");
                confirmAlert.setContentText("This action cannot be undone.");

                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        plannerManager.removeTask(existingAdapter.getTask());
                        updateView();
                        dialog.close();
                    }
                });
                event.consume();
            });
        }

        // Validation
        Runnable validateInput = () -> {
            boolean isValid = titleField.getText() != null &&
                    !titleField.getText().trim().isEmpty() &&
                    datePicker.getValue() != null &&
                    priorityGroup.getSelectedToggle() != null &&
                    statusCombo.getValue() != null;
            saveButton.setDisable(!isValid);
        };

        titleField.textProperty().addListener((obs, old, n) -> validateInput.run());
        datePicker.valueProperty().addListener((obs, old, n) -> validateInput.run());
        priorityGroup.selectedToggleProperty().addListener((obs, old, n) -> validateInput.run());
        statusCombo.valueProperty().addListener((obs, old, n) -> validateInput.run());

        validateInput.run();

        // Result converter
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                Task task = existingAdapter != null ? existingAdapter.getTask() : new Task();

                task.setTitle(titleField.getText().trim());
                task.setDescription(descField.getText());
                task.setStatus(statusCombo.getValue());

                ToggleButton selectedPriority = (ToggleButton) priorityGroup.getSelectedToggle();
                task.setPriority((com.smarttask.model.Priority) selectedPriority.getUserData());

                LocalDate date = datePicker.getValue();
                LocalTime time = LocalTime.of(hourSpinner.getValue(), minuteSpinner.getValue());
                task.setDueDate(date.atTime(time));

                // ✨ SET PROJECT ID
                Project selectedProject = projectCombo.getValue();
                if (selectedProject != null && selectedProject.getId() != null) {
                    task.setProjectId(selectedProject.getId());
                } else {
                    task.setProjectId(null);
                }

                if (existingAdapter == null) {
                    task.setUser(currentUser);
                }

                return task;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(task -> {
            if (existingAdapter == null) {
                plannerManager.addTask(task);
            } else {
                plannerManager.updateTask(task);
            }
            updateView();
        });
    }

    /**
     * ═══════════════════════════════════════════════════════════════════════════════
     * COMPLETE FIXED showEventDialog METHOD
     * ═══════════════════════════════════════════════════════════════════════════════
     *
     * Replace your entire showEventDialog method with this one.
     * All bugs have been fixed:
     * - Platform buttons declared before use in event handlers
     * - selectedPlatform array used instead of non-existent platformGroup
     * - Complete meeting section properly structured
     */
    // ═══════════════════════════════════════════════════════════════════════════════
    // 🔧 FIXED showEventDialog() - VISIBILITY SECTION NOW DISPLAYS PROPERLY
    // ═══════════════════════════════════════════════════════════════════════════════
    //
    // THE PROBLEM: sharedUsersBox was not being added to visibilitySection!
    //
    // Replace your entire showEventDialog() method with this one.

    public void showEventDialog(EventPlannerAdapter existingAdapter) {
        CalendarEvent existingEvent = existingAdapter != null ? existingAdapter.getEvent() : null;

        Dialog<CalendarEvent> dialog = new Dialog<>();
        dialog.setTitle(existingEvent == null || existingEvent.getTitle() == null ? "Add Event" : "Edit Event");

        // ============= CUSTOM HEADER =============
        VBox headerBox = new VBox(8);
        headerBox.setPadding(new Insets(20, 24, 16, 24));
        headerBox.setStyle("-fx-background-color: #ffffff; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

        Label headerIcon = new Label("📅");
        headerIcon.setStyle("-fx-font-size: 24;");

        Label headerTitle = new Label(existingEvent == null || existingEvent.getTitle() == null ?
                "Create New Event" : "Edit Event");
        headerTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #111827;");

        // Dynamic subtitle based on visibility selection
        Label headerSubtitle = new Label("Add a public event visible to everyone");
        headerSubtitle.setStyle("-fx-font-size: 13; -fx-text-fill: #6b7280;");

        headerBox.getChildren().addAll(headerIcon, headerTitle, headerSubtitle);

        // ============= BUTTONS =============
        ButtonType saveButtonType = new ButtonType("Save Event", ButtonBar.ButtonData.OK_DONE);
        ButtonType deleteButtonType = new ButtonType("Delete", ButtonBar.ButtonData.OTHER);

        if (existingEvent != null && existingEvent.getTitle() != null) {
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, deleteButtonType, ButtonType.CANCEL);
        } else {
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        }

        // ============= SCROLLABLE MAIN CONTENT =============
        VBox mainContent = new VBox(20);
        mainContent.setPadding(new Insets(24));
        mainContent.setStyle("-fx-background-color: #f9fafb;");

        // ═══════════════════════════════════════════════════════════════════════
        // 🔧 FIX: VISIBILITY & SHARING SECTION - NOW PROPERLY DISPLAYS
        // ═══════════════════════════════════════════════════════════════════════

        VBox visibilitySection = new VBox(12);
        visibilitySection.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #e5e7eb; " +
                        "-fx-border-radius: 12; " +
                        "-fx-background-radius: 12; " +
                        "-fx-padding: 16;"
        );

        Label visibilityLabel = new Label("Who can see this event? *");
        visibilityLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox visibilityButtons = new HBox(8);
        visibilityButtons.setAlignment(Pos.CENTER_LEFT);
        ToggleGroup visibilityGroup = new ToggleGroup();

        ToggleButton publicBtn = new ToggleButton("🌍 Public");
        ToggleButton privateBtn = new ToggleButton("🔒 Private");
        ToggleButton sharedBtn = new ToggleButton("👥 Shared");

        publicBtn.setToggleGroup(visibilityGroup);
        privateBtn.setToggleGroup(visibilityGroup);
        sharedBtn.setToggleGroup(visibilityGroup);

        publicBtn.setUserData(CalendarEvent.EventVisibility.PUBLIC);
        privateBtn.setUserData(CalendarEvent.EventVisibility.PRIVATE);
        sharedBtn.setUserData(CalendarEvent.EventVisibility.SHARED);

        // Style buttons
        String visBtnStyle =
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-text-fill: #374151; " +
                        "-fx-font-size: 12; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 10 20;";

        String visBtnSelectedStyle =
                "-fx-background-color: #3b82f6; " +
                        "-fx-border-color: #3b82f6; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand; " +
                        "-fx-padding: 10 20;";

        publicBtn.setStyle(visBtnStyle);
        privateBtn.setStyle(visBtnStyle);
        sharedBtn.setStyle(visBtnStyle);

        publicBtn.selectedProperty().addListener((obs, old, sel) ->
                publicBtn.setStyle(sel ? visBtnSelectedStyle : visBtnStyle));
        privateBtn.selectedProperty().addListener((obs, old, sel) ->
                privateBtn.setStyle(sel ? visBtnSelectedStyle : visBtnStyle));
        sharedBtn.selectedProperty().addListener((obs, old, sel) ->
                sharedBtn.setStyle(sel ? visBtnSelectedStyle : visBtnStyle));

        visibilityButtons.getChildren().addAll(publicBtn, privateBtn, sharedBtn);

        // Set default selection to PUBLIC
        publicBtn.setSelected(true);

        // Description label for current selection
        Label visibilityDesc = new Label("🌍 Everyone can see this event");
        visibilityDesc.setStyle("-fx-font-size: 11; -fx-text-fill: #6b7280; -fx-font-style: italic;");

        // ═══════════════════════════════════════════════════════════════════════
        // SHARED USERS SECTION (initially hidden)
        // ═══════════════════════════════════════════════════════════════════════

        VBox sharedUsersBox = new VBox(12);
        sharedUsersBox.setVisible(false);
        sharedUsersBox.setManaged(false);
        sharedUsersBox.setStyle(
                "-fx-background-color: #f0f9ff; " +
                        "-fx-border-color: #bae6fd; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 16;"
        );

        Label sharedLabel = new Label("Select people to share with:");
        sharedLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: #0369a1;");

        // User selection (from database)
        VBox userSelectionBox = new VBox(8);
        Label userLabel = new Label("👥 Users from system:");
        userLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

        ListView<User> userListView = new ListView<>();
        userListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        userListView.setPrefHeight(120);
        userListView.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6;"
        );

        // Load users from database
        try {
            com.smarttask.server.dao.UserDAO userDAO = new com.smarttask.server.dao.UserDAO();
            List<User> allUsers = userDAO.findAll();
            allUsers.removeIf(u -> u.getId().equals(currentUser.getId()));
            userListView.setItems(FXCollections.observableArrayList(allUsers));
        } catch (Exception e) {
            System.err.println("Error loading users: " + e.getMessage());
        }

        userListView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(8);
                    box.setAlignment(Pos.CENTER_LEFT);
                    Label icon = new Label("👤");
                    Label name = new Label(user.getUsername());
                    name.setStyle("-fx-font-size: 12;");
                    Label email = new Label("(" + user.getEmail() + ")");
                    email.setStyle("-fx-font-size: 10; -fx-text-fill: #6b7280;");
                    box.getChildren().addAll(icon, name, email);
                    setGraphic(box);
                }
            }
        });

        userSelectionBox.getChildren().addAll(userLabel, userListView);

        // External email input
        VBox emailBox = new VBox(8);
        Label emailLabel = new Label("📧 External emails:");
        emailLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

        Label emailNote = new Label("These users will receive an email invitation");
        emailNote.setStyle("-fx-font-size: 10; -fx-text-fill: #059669; -fx-font-style: italic;");

        HBox emailInputBox = new HBox(8);
        emailInputBox.setAlignment(Pos.CENTER_LEFT);

        TextField newEmailField = new TextField();
        newEmailField.setPromptText("Enter email address");
        newEmailField.setPrefHeight(36);
        HBox.setHgrow(newEmailField, Priority.ALWAYS);
        newEmailField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 8;"
        );

        Button addEmailBtn = new Button("+ Add");
        addEmailBtn.setPrefHeight(36);
        addEmailBtn.setStyle(
                "-fx-background-color: #3b82f6; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 8 16; " +
                        "-fx-font-size: 11; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand;"
        );

        emailInputBox.getChildren().addAll(newEmailField, addEmailBtn);

        FlowPane emailChipsPane = new FlowPane();
        emailChipsPane.setHgap(6);
        emailChipsPane.setVgap(6);
        emailChipsPane.setPadding(new Insets(8));
        emailChipsPane.setStyle(
                "-fx-background-color: #f9fafb; " +
                        "-fx-border-color: #e5e7eb; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6;"
        );
        emailChipsPane.setPrefWrapLength(400);

        List<String> externalEmailsList = new ArrayList<>();

        java.util.function.Consumer<String> addEmailChip = email -> {
            HBox chip = new HBox(6);
            chip.setAlignment(Pos.CENTER);
            chip.setPadding(new Insets(4, 8, 4, 8));
            chip.setStyle(
                    "-fx-background-color: #dbeafe; " +
                            "-fx-background-radius: 12; " +
                            "-fx-border-color: #93c5fd; " +
                            "-fx-border-radius: 12; " +
                            "-fx-border-width: 1;"
            );

            Label chipEmailLabel = new Label(email);
            chipEmailLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #1e40af; -fx-font-weight: 600;");

            Button removeBtn = new Button("×");
            removeBtn.setStyle(
                    "-fx-background-color: transparent; " +
                            "-fx-text-fill: #1e40af; " +
                            "-fx-font-size: 14; " +
                            "-fx-font-weight: bold; " +
                            "-fx-padding: 0 4; " +
                            "-fx-cursor: hand;"
            );

            removeBtn.setOnAction(e -> {
                externalEmailsList.remove(email);
                emailChipsPane.getChildren().remove(chip);
            });

            chip.getChildren().addAll(chipEmailLabel, removeBtn);
            emailChipsPane.getChildren().add(chip);
        };

        java.util.function.Predicate<String> isValidEmail = email -> {
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
            return email != null && email.matches(emailRegex);
        };

        addEmailBtn.setOnAction(e -> {
            String email = newEmailField.getText().trim();
            if (email.isEmpty()) return;
            if (!isValidEmail.test(email)) {
                showEmailError("Invalid email format: " + email);
                return;
            }
            if (externalEmailsList.contains(email)) {
                showEmailError("Email already added: " + email);
                return;
            }
            externalEmailsList.add(email);
            addEmailChip.accept(email);
            newEmailField.clear();
            newEmailField.requestFocus();
        });

        newEmailField.setOnKeyPressed(e -> {
            if (e.getCode() == javafx.scene.input.KeyCode.ENTER) {
                addEmailBtn.fire();
            }
        });

        Label emailInstructions = new Label("💡 Press Enter or click 'Add' to add each email");
        emailInstructions.setStyle("-fx-font-size: 10; -fx-text-fill: #6b7280; -fx-font-style: italic;");

        emailBox.getChildren().addAll(emailLabel, emailNote, emailInputBox, emailChipsPane, emailInstructions);

        // 🔧 FIX: Add everything to sharedUsersBox
        sharedUsersBox.getChildren().addAll(sharedLabel, userSelectionBox, emailBox);

        // ═══════════════════════════════════════════════════════════════════════
        // 🔧 FIX: VISIBILITY CHANGE LISTENER
        // ═══════════════════════════════════════════════════════════════════════

        visibilityGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                CalendarEvent.EventVisibility vis = (CalendarEvent.EventVisibility) newToggle.getUserData();

                switch (vis) {
                    case PUBLIC:
                        visibilityDesc.setText("🌍 Everyone can see this event");
                        headerSubtitle.setText("Add a public event visible to everyone");
                        sharedUsersBox.setVisible(false);
                        sharedUsersBox.setManaged(false);
                        break;
                    case PRIVATE:
                        visibilityDesc.setText("🔒 Only you can see this event");
                        headerSubtitle.setText("Add a private event only you can see");
                        sharedUsersBox.setVisible(false);
                        sharedUsersBox.setManaged(false);
                        break;
                    case SHARED:
                        visibilityDesc.setText("👥 Share with specific people");
                        headerSubtitle.setText("Add a shared event for selected people");
                        sharedUsersBox.setVisible(true);
                        sharedUsersBox.setManaged(true);
                        break;
                }
            }
        });

        // 🔧 FIX: ADD ALL COMPONENTS TO visibilitySection (THIS WAS THE BUG!)
        visibilitySection.getChildren().addAll(
                visibilityLabel,
                visibilityButtons,
                visibilityDesc,
                sharedUsersBox  // ← THIS WAS MISSING!
        );

        // ═══════════════════════════════════════════════════════════════════════
        // MEETING LINK SECTION
        // ═══════════════════════════════════════════════════════════════════════

        VBox meetingSection = new VBox(12);
        Label meetingLabel = new Label("Meeting Link (Optional)");
        meetingLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        CheckBox hasMeetingCheck = new CheckBox("This event has an online meeting");
        hasMeetingCheck.setStyle("-fx-font-size: 12;");

        VBox meetingDetailsBox = new VBox(12);
        meetingDetailsBox.setVisible(false);
        meetingDetailsBox.setManaged(false);
        meetingDetailsBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #e5e7eb; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 16;"
        );

        // Platform selection
        HBox platformBox = new HBox(8);
        Label platformLabel = new Label("Platform:");
        platformLabel.setStyle("-fx-font-size: 11; -fx-font-weight: 600;");

        ToggleGroup platformGroup = new ToggleGroup();

        ToggleButton googleMeetBtn = new ToggleButton("📹 Google Meet");
        ToggleButton zoomBtn = new ToggleButton("🎥 Zoom");
        ToggleButton teamsBtn = new ToggleButton("💼 Teams");
        ToggleButton customBtn = new ToggleButton("🔗 Custom");

        googleMeetBtn.setToggleGroup(platformGroup);
        zoomBtn.setToggleGroup(platformGroup);
        teamsBtn.setToggleGroup(platformGroup);
        customBtn.setToggleGroup(platformGroup);

        googleMeetBtn.setUserData("google_meet");
        zoomBtn.setUserData("zoom");
        teamsBtn.setUserData("teams");
        customBtn.setUserData("custom");

        String platformBtnStyle =
                "-fx-background-color: #f3f4f6; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 6; " +
                        "-fx-background-radius: 6; " +
                        "-fx-padding: 6 12; " +
                        "-fx-font-size: 11; " +
                        "-fx-cursor: hand;";

        googleMeetBtn.setStyle(platformBtnStyle);
        zoomBtn.setStyle(platformBtnStyle);
        teamsBtn.setStyle(platformBtnStyle);
        customBtn.setStyle(platformBtnStyle);

        platformBox.getChildren().addAll(platformLabel, googleMeetBtn, zoomBtn, teamsBtn, customBtn);

        // Meeting link field
        TextField meetingLinkField = new TextField();
        meetingLinkField.setPromptText("Paste your meeting link here");
        meetingLinkField.setPrefHeight(36);

        // Instructions
        VBox linkInstructionsBox = new VBox(4);
        Label linkInstructionLabel = new Label("💡 How to get a meeting link:");
        linkInstructionLabel.setStyle("-fx-font-size: 10; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

        Label googleInstr = new Label("• Google Meet: Go to meet.google.com → Start a meeting → Copy link");
        Label zoomInstr = new Label("• Zoom: Schedule meeting in Zoom app → Copy invitation link");
        Label teamsInstr = new Label("• Teams: Create meeting in Teams → Copy join link");

        googleInstr.setStyle("-fx-font-size: 9; -fx-text-fill: #9ca3af;");
        zoomInstr.setStyle("-fx-font-size: 9; -fx-text-fill: #9ca3af;");
        teamsInstr.setStyle("-fx-font-size: 9; -fx-text-fill: #9ca3af;");

        linkInstructionsBox.getChildren().addAll(linkInstructionLabel, googleInstr, zoomInstr, teamsInstr);

        // Password
        HBox passwordBox = new HBox(8);
        Label passwordLabel = new Label("Password:");
        passwordLabel.setStyle("-fx-font-size: 11; -fx-min-width: 70;");

        TextField passwordField = new TextField();
        passwordField.setPromptText("Optional meeting password");
        passwordField.setPrefHeight(36);
        HBox.setHgrow(passwordField, Priority.ALWAYS);

        passwordBox.getChildren().addAll(passwordLabel, passwordField);

        meetingDetailsBox.getChildren().addAll(platformBox, meetingLinkField, linkInstructionsBox, passwordBox);

        hasMeetingCheck.selectedProperty().addListener((obs, old, hasCheck) -> {
            meetingDetailsBox.setVisible(hasCheck);
            meetingDetailsBox.setManaged(hasCheck);
        });

        meetingSection.getChildren().addAll(meetingLabel, hasMeetingCheck, meetingDetailsBox);

        // ═══════════════════════════════════════════════════════════════════════
        // TITLE SECTION
        // ═══════════════════════════════════════════════════════════════════════

        VBox titleSection = new VBox(8);
        Label titleLabel = new Label("Event Title *");
        titleLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextField titleField = new TextField();
        titleField.setPromptText("Enter event title");
        titleField.setPrefHeight(40);
        titleField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 12; " +
                        "-fx-font-size: 14;"
        );

        titleSection.getChildren().addAll(titleLabel, titleField);

        // ═══════════════════════════════════════════════════════════════════════
        // DESCRIPTION SECTION
        // ═══════════════════════════════════════════════════════════════════════

        VBox descSection = new VBox(8);
        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        TextArea descField = new TextArea();
        descField.setPromptText("Add event description (optional)");
        descField.setPrefRowCount(3);
        descField.setWrapText(true);
        descField.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 12; " +
                        "-fx-font-size: 14;"
        );

        descSection.getChildren().addAll(descLabel, descField);

        // ═══════════════════════════════════════════════════════════════════════
        // DATE SECTION
        // ═══════════════════════════════════════════════════════════════════════

        VBox dateSection = new VBox(8);
        Label dateLabel = new Label("Event Date *");
        dateLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        DatePicker datePicker = new DatePicker();
        datePicker.setPrefHeight(40);
        datePicker.setMaxWidth(Double.MAX_VALUE);
        datePicker.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        dateSection.getChildren().addAll(dateLabel, datePicker);

        // ═══════════════════════════════════════════════════════════════════════
        // TIME SECTION
        // ═══════════════════════════════════════════════════════════════════════

        HBox timeBox = new HBox(12);

        VBox startTimeBox = new VBox(8);
        HBox.setHgrow(startTimeBox, Priority.ALWAYS);
        Label startTimeLabel = new Label("Start Time *");
        startTimeLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox startTimeInputBox = new HBox(8);
        startTimeInputBox.setAlignment(Pos.CENTER_LEFT);

        Spinner<Integer> startHourSpinner = new Spinner<>(0, 23, 9);
        Spinner<Integer> startMinuteSpinner = new Spinner<>(0, 59, 0, 15);
        startHourSpinner.setEditable(true);
        startMinuteSpinner.setEditable(true);
        startHourSpinner.setPrefWidth(70);
        startMinuteSpinner.setPrefWidth(70);
        startHourSpinner.setPrefHeight(40);
        startMinuteSpinner.setPrefHeight(40);

        Label startColonLabel = new Label(":");
        startColonLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        startTimeInputBox.getChildren().addAll(startHourSpinner, startColonLabel, startMinuteSpinner);
        startTimeBox.getChildren().addAll(startTimeLabel, startTimeInputBox);

        VBox endTimeBox = new VBox(8);
        HBox.setHgrow(endTimeBox, Priority.ALWAYS);
        Label endTimeLabel = new Label("End Time *");
        endTimeLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox endTimeInputBox = new HBox(8);
        endTimeInputBox.setAlignment(Pos.CENTER_LEFT);

        Spinner<Integer> endHourSpinner = new Spinner<>(0, 23, 10);
        Spinner<Integer> endMinuteSpinner = new Spinner<>(0, 59, 0, 15);
        endHourSpinner.setEditable(true);
        endMinuteSpinner.setEditable(true);
        endHourSpinner.setPrefWidth(70);
        endMinuteSpinner.setPrefWidth(70);
        endHourSpinner.setPrefHeight(40);
        endMinuteSpinner.setPrefHeight(40);

        Label endColonLabel = new Label(":");
        endColonLabel.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        endTimeInputBox.getChildren().addAll(endHourSpinner, endColonLabel, endMinuteSpinner);
        endTimeBox.getChildren().addAll(endTimeLabel, endTimeInputBox);

        timeBox.getChildren().addAll(startTimeBox, endTimeBox);

        // ═══════════════════════════════════════════════════════════════════════
        // PRIORITY SECTION
        // ═══════════════════════════════════════════════════════════════════════

        VBox prioritySection = new VBox(8);
        Label priorityLabel = new Label("Priority *");
        priorityLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox priorityButtonsBox = new HBox(8);
        ToggleGroup priorityGroup = new ToggleGroup();

        ToggleButton[] priorityButtons = new ToggleButton[4];
        String[] priorityNames = {"Optional", "Standard", "Important", "Urgent"};
        String[] priorityColors = {"#9E9E9E", "#2196F3", "#FF9800", "#F44336"};
        CalendarEvent.Priority[] priorityValues = {
                CalendarEvent.Priority.OPTIONAL,
                CalendarEvent.Priority.STANDARD,
                CalendarEvent.Priority.IMPORTANT,
                CalendarEvent.Priority.URGENT
        };

        for (int i = 0; i < 4; i++) {
            ToggleButton btn = new ToggleButton(priorityNames[i]);
            btn.setToggleGroup(priorityGroup);
            btn.setUserData(priorityValues[i]);
            btn.setPrefHeight(40);
            btn.setPrefWidth(100);

            final String color = priorityColors[i];
            btn.setStyle(
                    "-fx-background-color: white; " +
                            "-fx-border-color: " + color + "; " +
                            "-fx-border-width: 2; " +
                            "-fx-border-radius: 8; " +
                            "-fx-background-radius: 8; " +
                            "-fx-text-fill: " + color + "; " +
                            "-fx-font-size: 12; " +
                            "-fx-font-weight: 600; " +
                            "-fx-cursor: hand;"
            );

            btn.selectedProperty().addListener((obs, old, isSelected) -> {
                if (isSelected) {
                    btn.setStyle(
                            "-fx-background-color: " + color + "; " +
                                    "-fx-border-color: " + color + "; " +
                                    "-fx-border-width: 2; " +
                                    "-fx-border-radius: 8; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-font-size: 12; " +
                                    "-fx-font-weight: 600; " +
                                    "-fx-cursor: hand;"
                    );
                } else {
                    btn.setStyle(
                            "-fx-background-color: white; " +
                                    "-fx-border-color: " + color + "; " +
                                    "-fx-border-width: 2; " +
                                    "-fx-border-radius: 8; " +
                                    "-fx-background-radius: 8; " +
                                    "-fx-text-fill: " + color + "; " +
                                    "-fx-font-size: 12; " +
                                    "-fx-font-weight: 600; " +
                                    "-fx-cursor: hand;"
                    );
                }
            });

            priorityButtons[i] = btn;
            priorityButtonsBox.getChildren().add(btn);
        }

        prioritySection.getChildren().addAll(priorityLabel, priorityButtonsBox);

        // ═══════════════════════════════════════════════════════════════════════
        // EVENT TYPE SECTION
        // ═══════════════════════════════════════════════════════════════════════

        VBox eventTypeSection = new VBox(8);
        Label eventTypeLabel = new Label("Event Type *");
        eventTypeLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        HBox eventTypeButtonsBox = new HBox(8);
        ToggleGroup eventTypeGroup = new ToggleGroup();

        ToggleButton oneTimeBtn = new ToggleButton("One-time Event");
        oneTimeBtn.setToggleGroup(eventTypeGroup);
        oneTimeBtn.setUserData(CalendarEvent.EventType.ONE_TIME_EVENT);
        oneTimeBtn.setPrefHeight(40);
        oneTimeBtn.setPrefWidth(150);

        ToggleButton recurringBtn = new ToggleButton("Recurring Event");
        recurringBtn.setToggleGroup(eventTypeGroup);
        recurringBtn.setUserData(CalendarEvent.EventType.RECURRING_EVENT);
        recurringBtn.setPrefHeight(40);
        recurringBtn.setPrefWidth(150);

        String toggleStyle =
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-text-fill: #374151; " +
                        "-fx-font-size: 12; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand;";

        String toggleSelectedStyle =
                "-fx-background-color: #3b82f6; " +
                        "-fx-border-color: #3b82f6; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 12; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand;";

        oneTimeBtn.setStyle(toggleStyle);
        recurringBtn.setStyle(toggleStyle);

        oneTimeBtn.selectedProperty().addListener((obs, old, isSelected) -> {
            oneTimeBtn.setStyle(isSelected ? toggleSelectedStyle : toggleStyle);
        });

        recurringBtn.selectedProperty().addListener((obs, old, isSelected) -> {
            recurringBtn.setStyle(isSelected ? toggleSelectedStyle : toggleStyle);
        });

        eventTypeButtonsBox.getChildren().addAll(oneTimeBtn, recurringBtn);
        eventTypeSection.getChildren().addAll(eventTypeLabel, eventTypeButtonsBox);

        // Recurring options (initially hidden)
        VBox recurringBox = new VBox(12);
        recurringBox.setVisible(false);
        recurringBox.setManaged(false);
        recurringBox.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 16;"
        );

        Label recurringLabel = new Label("Recurrence Settings");
        recurringLabel.setStyle("-fx-font-size: 13; -fx-font-weight: 600; -fx-text-fill: #374151;");

        ComboBox<CalendarEvent.PeriodicType> periodicTypeCombo = new ComboBox<>();
        periodicTypeCombo.getItems().addAll(
                CalendarEvent.PeriodicType.PER_DAY,
                CalendarEvent.PeriodicType.PER_WEEK,
                CalendarEvent.PeriodicType.PER_MONTH,
                CalendarEvent.PeriodicType.PER_YEAR
        );
        periodicTypeCombo.setPrefHeight(40);
        periodicTypeCombo.setMaxWidth(Double.MAX_VALUE);

        VBox weekDaysSection = new VBox(8);
        weekDaysSection.setVisible(false);
        weekDaysSection.setManaged(false);

        Label weekDaysLabel = new Label("Select Days");
        weekDaysLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

        HBox weekDaysBox = new HBox(6);
        CheckBox[] dayChecks = new CheckBox[7];
        String[] dayLabels = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int i = 0; i < 7; i++) {
            dayChecks[i] = new CheckBox(dayLabels[i]);
            dayChecks[i].setStyle("-fx-font-size: 11;");
            weekDaysBox.getChildren().add(dayChecks[i]);
        }

        weekDaysSection.getChildren().addAll(weekDaysLabel, weekDaysBox);

        VBox monthPlaceSection = new VBox(8);
        monthPlaceSection.setVisible(false);
        monthPlaceSection.setManaged(false);

        Label monthPlaceLabel = new Label("Day of Month");
        monthPlaceLabel.setStyle("-fx-font-size: 12; -fx-font-weight: 600; -fx-text-fill: #6b7280;");

        ComboBox<CalendarEvent.MonthPlace> monthPlaceCombo = new ComboBox<>();
        monthPlaceCombo.getItems().addAll(CalendarEvent.MonthPlace.values());
        monthPlaceCombo.setPrefHeight(40);
        monthPlaceCombo.setMaxWidth(Double.MAX_VALUE);

        monthPlaceSection.getChildren().addAll(monthPlaceLabel, monthPlaceCombo);

        periodicTypeCombo.valueProperty().addListener((obs, old, newVal) -> {
            weekDaysSection.setVisible(newVal == CalendarEvent.PeriodicType.PER_WEEK);
            weekDaysSection.setManaged(newVal == CalendarEvent.PeriodicType.PER_WEEK);
            monthPlaceSection.setVisible(newVal == CalendarEvent.PeriodicType.PER_MONTH);
            monthPlaceSection.setManaged(newVal == CalendarEvent.PeriodicType.PER_MONTH);
        });

        recurringBox.getChildren().addAll(recurringLabel, periodicTypeCombo, weekDaysSection, monthPlaceSection);

        eventTypeGroup.selectedToggleProperty().addListener((obs, old, newVal) -> {
            boolean isRecurring = newVal != null &&
                    newVal.getUserData() == CalendarEvent.EventType.RECURRING_EVENT;
            recurringBox.setVisible(isRecurring);
            recurringBox.setManaged(isRecurring);
        });

        // Completion checkbox
        CheckBox completedCheck = new CheckBox("Mark as completed");
        completedCheck.setStyle("-fx-font-size: 13; -fx-text-fill: #374151;");

        // ═══════════════════════════════════════════════════════════════════════
        // POPULATE EXISTING DATA
        // ═══════════════════════════════════════════════════════════════════════

        if (existingEvent != null && existingEvent.getTitle() != null) {
            titleField.setText(existingEvent.getTitle());
            descField.setText(existingEvent.getDescription());
            datePicker.setValue(existingEvent.getDate() != null ? existingEvent.getDate() : currentDate);

            if (existingEvent.getStartTime() != null) {
                startHourSpinner.getValueFactory().setValue(existingEvent.getStartTime().getHour());
                startMinuteSpinner.getValueFactory().setValue(existingEvent.getStartTime().getMinute());
            }
            if (existingEvent.getEndTime() != null) {
                endHourSpinner.getValueFactory().setValue(existingEvent.getEndTime().getHour());
                endMinuteSpinner.getValueFactory().setValue(existingEvent.getEndTime().getMinute());
            }

            completedCheck.setSelected(existingEvent.isCompleted());

            // Select priority button
            CalendarEvent.Priority eventPriority = existingEvent.getPriority();
            for (int i = 0; i < priorityValues.length; i++) {
                if (priorityValues[i] == eventPriority) {
                    priorityButtons[i].setSelected(true);
                    break;
                }
            }

            // Populate visibility
            CalendarEvent.EventVisibility vis = existingEvent.getVisibility();
            if (vis != null) {
                switch (vis) {
                    case PUBLIC:
                        publicBtn.setSelected(true);
                        break;
                    case PRIVATE:
                        privateBtn.setSelected(true);
                        break;
                    case SHARED:
                        sharedBtn.setSelected(true);
                        sharedUsersBox.setVisible(true);
                        sharedUsersBox.setManaged(true);

                        List<String> sharedIds = existingEvent.getSharedWithUserIds();
                        for (User user : userListView.getItems()) {
                            if (sharedIds.contains(user.getId())) {
                                userListView.getSelectionModel().select(user);
                            }
                        }

                        List<String> emails = existingEvent.getSharedWithEmails();
                        if (emails != null && !emails.isEmpty()) {
                            externalEmailsList.addAll(emails);
                            for (String email : emails) {
                                addEmailChip.accept(email);
                            }
                        }
                        break;
                }
            }

            // Populate meeting info
            if (existingEvent.hasMeetingLink()) {
                hasMeetingCheck.setSelected(true);
                meetingDetailsBox.setVisible(true);
                meetingDetailsBox.setManaged(true);
                meetingLinkField.setText(existingEvent.getMeetingLink());

                if (existingEvent.getMeetingPassword() != null) {
                    passwordField.setText(existingEvent.getMeetingPassword());
                }

                String platform = existingEvent.getMeetingPlatform();
                if ("google_meet".equals(platform)) googleMeetBtn.setSelected(true);
                else if ("zoom".equals(platform)) zoomBtn.setSelected(true);
                else if ("teams".equals(platform)) teamsBtn.setSelected(true);
                else if (platform != null) customBtn.setSelected(true);
            }

            // Event type
            if (existingEvent.getEventType() == CalendarEvent.EventType.RECURRING_EVENT) {
                recurringBtn.setSelected(true);
                recurringBox.setVisible(true);
                recurringBox.setManaged(true);

                if (existingEvent.getPeriodicType() != null) {
                    periodicTypeCombo.setValue(existingEvent.getPeriodicType());

                    if (existingEvent.getPeriodicType() == CalendarEvent.PeriodicType.PER_WEEK &&
                            existingEvent.getDaysInWeek() != null) {
                        String[] days = existingEvent.getDaysInWeek().split(",");
                        for (String day : days) {
                            int idx = Integer.parseInt(day.trim()) - 1;
                            if (idx >= 0 && idx < 7) {
                                dayChecks[idx].setSelected(true);
                            }
                        }
                    }

                    if (existingEvent.getPlaceInMonth() != null) {
                        monthPlaceCombo.setValue(existingEvent.getPlaceInMonth());
                    }
                }
            } else {
                oneTimeBtn.setSelected(true);
            }
        } else {
            // New event defaults
            datePicker.setValue(currentDate);
            priorityButtons[1].setSelected(true); // Standard
            oneTimeBtn.setSelected(true);
            publicBtn.setSelected(true);
        }

        // ═══════════════════════════════════════════════════════════════════════
        // ADD ALL SECTIONS TO MAIN CONTENT
        // ═══════════════════════════════════════════════════════════════════════

        mainContent.getChildren().addAll(
                visibilitySection,  // ← NOW INCLUDES sharedUsersBox
                meetingSection,
                titleSection,
                descSection,
                dateSection,
                timeBox,
                prioritySection,
                eventTypeSection,
                recurringBox,
                completedCheck
        );

        // Wrap in ScrollPane
        ScrollPane scrollPane = new ScrollPane(mainContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setStyle("-fx-background: #f9fafb; -fx-background-color: #f9fafb;");
        scrollPane.setPrefHeight(500);
        scrollPane.setMaxHeight(600);

        VBox fullContent = new VBox(0);
        fullContent.getChildren().addAll(headerBox, scrollPane);

        dialog.getDialogPane().setContent(fullContent);
        dialog.getDialogPane().setStyle("-fx-background-color: #f9fafb;");
        dialog.getDialogPane().setPrefWidth(600);

        // Style buttons
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.setStyle(
                "-fx-background-color: #3b82f6; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 20; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand;"
        );

        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        cancelButton.setStyle(
                "-fx-background-color: white; " +
                        "-fx-text-fill: #374151; " +
                        "-fx-border-color: #d1d5db; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-padding: 10 20; " +
                        "-fx-font-weight: 600; " +
                        "-fx-cursor: hand;"
        );

        if (existingEvent != null && existingEvent.getTitle() != null) {
            Node deleteButton = dialog.getDialogPane().lookupButton(deleteButtonType);
            deleteButton.setStyle(
                    "-fx-background-color: #ef4444; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 10 20; " +
                            "-fx-font-weight: 600; " +
                            "-fx-cursor: hand;"
            );

            deleteButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
                Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmAlert.setTitle("Delete Event");
                confirmAlert.setHeaderText("Delete \"" + existingEvent.getTitle() + "\"?");
                confirmAlert.setContentText("This action cannot be undone.");

                confirmAlert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        plannerManager.removeEvent(existingEvent);
                        updateView();
                        dialog.close();
                    }
                });
                event.consume();
            });
        }

        // Validation
        Runnable validateInput = () -> {
            boolean isValid = titleField.getText() != null &&
                    !titleField.getText().trim().isEmpty() &&
                    datePicker.getValue() != null &&
                    priorityGroup.getSelectedToggle() != null &&
                    eventTypeGroup.getSelectedToggle() != null &&
                    visibilityGroup.getSelectedToggle() != null;
            saveButton.setDisable(!isValid);
        };

        titleField.textProperty().addListener((obs, old, n) -> validateInput.run());
        datePicker.valueProperty().addListener((obs, old, n) -> validateInput.run());
        priorityGroup.selectedToggleProperty().addListener((obs, old, n) -> validateInput.run());
        eventTypeGroup.selectedToggleProperty().addListener((obs, old, n) -> validateInput.run());
        visibilityGroup.selectedToggleProperty().addListener((obs, old, n) -> validateInput.run());

        validateInput.run();

        // ═══════════════════════════════════════════════════════════════════════
        // RESULT CONVERTER
        // ═══════════════════════════════════════════════════════════════════════

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                CalendarEvent event = existingEvent != null ? existingEvent : new CalendarEvent();

                event.setTitle(titleField.getText().trim());
                event.setDescription(descField.getText());
                event.setDate(datePicker.getValue());
                event.setStartTime(LocalTime.of(startHourSpinner.getValue(), startMinuteSpinner.getValue()));
                event.setEndTime(LocalTime.of(endHourSpinner.getValue(), endMinuteSpinner.getValue()));
                event.setCompleted(completedCheck.isSelected());

                ToggleButton selectedPriority = (ToggleButton) priorityGroup.getSelectedToggle();
                event.setPriority((CalendarEvent.Priority) selectedPriority.getUserData());

                ToggleButton selectedType = (ToggleButton) eventTypeGroup.getSelectedToggle();
                event.setEventType((CalendarEvent.EventType) selectedType.getUserData());

                // Handle recurring
                if (event.getEventType() == CalendarEvent.EventType.RECURRING_EVENT) {
                    event.setPeriodicType(periodicTypeCombo.getValue());

                    if (periodicTypeCombo.getValue() == CalendarEvent.PeriodicType.PER_WEEK) {
                        StringBuilder daysBuilder = new StringBuilder();
                        for (int i = 0; i < 7; i++) {
                            if (dayChecks[i].isSelected()) {
                                if (daysBuilder.length() > 0) daysBuilder.append(",");
                                daysBuilder.append(i + 1);
                            }
                        }
                        event.setDaysInWeek(daysBuilder.toString());
                    }

                    if (periodicTypeCombo.getValue() == CalendarEvent.PeriodicType.PER_MONTH) {
                        event.setPlaceInMonth(monthPlaceCombo.getValue());
                    }

                    if (periodicTypeCombo.getValue() == CalendarEvent.PeriodicType.PER_YEAR) {
                        event.setYearlyDate(datePicker.getValue());
                    }
                }

                // Save visibility
                ToggleButton selectedVis = (ToggleButton) visibilityGroup.getSelectedToggle();
                CalendarEvent.EventVisibility visibility =
                        (CalendarEvent.EventVisibility) selectedVis.getUserData();
                event.setVisibility(visibility);

                System.out.println("📋 Saving event with visibility: " + visibility);

                // Save shared users only if SHARED
                if (visibility == CalendarEvent.EventVisibility.SHARED) {
                    List<String> selectedUserIds = new ArrayList<>();
                    for (User user : userListView.getSelectionModel().getSelectedItems()) {
                        selectedUserIds.add(user.getId());
                    }
                    event.setSharedWithUserIds(selectedUserIds);
                    System.out.println("   Shared with " + selectedUserIds.size() + " users");

                    event.setSharedWithEmails(new ArrayList<>(externalEmailsList));
                    System.out.println("   Shared with " + externalEmailsList.size() + " external emails");
                } else {
                    event.setSharedWithUserIds(new ArrayList<>());
                    event.setSharedWithEmails(new ArrayList<>());
                }

                // Save meeting info
                if (hasMeetingCheck.isSelected() && !meetingLinkField.getText().trim().isEmpty()) {
                    event.setMeetingLink(meetingLinkField.getText().trim());
                    event.setMeetingPassword(passwordField.getText().trim());

                    Toggle selectedPlatform = platformGroup.getSelectedToggle();
                    if (selectedPlatform != null) {
                        event.setMeetingPlatform((String) selectedPlatform.getUserData());
                    }
                } else {
                    event.setMeetingLink(null);
                    event.setHasMeetingLink(false);
                }

                // Set creator
                if (event.getCreatorUserId() == null) {
                    event.setCreatorUserId(currentUser.getId());
                }

                return event;
            }
            return null;
        });

        // Show dialog and handle result
        dialog.showAndWait().ifPresent(event -> {
            boolean isNew = existingEvent == null || existingEvent.getTitle() == null;

            if (isNew) {
                plannerManager.addEvent(event);
            } else {
                plannerManager.updateEvent(event);
            }

            // Send email notifications for shared events
            if (event.getVisibility() == CalendarEvent.EventVisibility.SHARED) {
                sendEventInvitations(event, isNew);
            }

            updateView();
        });
    }

// ═══════════════════════════════════════════════════════════════════════════════
// 🔧 NEW: EMAIL NOTIFICATION METHOD
// ═══════════════════════════════════════════════════════════════════════════════

    private void showEmailError(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Invalid Email");
        alert.setHeaderText("Email Validation Error");
        alert.setContentText(message);

        // Style the dialog
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white;");

        Button okButton = (Button) dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.setStyle(
                    "-fx-background-color: #f59e0b; " +
                            "-fx-text-fill: white; " +
                            "-fx-background-radius: 8; " +
                            "-fx-padding: 8 20; " +
                            "-fx-font-weight: 600; " +
                            "-fx-cursor: hand;"
            );
        }

        alert.showAndWait();
    }

    /**
     * Send email invitations to external users
     */
    private void sendEventInvitations(CalendarEvent event, boolean isNewEvent) {
        List<String> externalEmails = event.getSharedWithEmails();

        if (externalEmails == null || externalEmails.isEmpty()) {
            return;
        }

        // Run in background thread to not block UI
        new Thread(() -> {
            try {
                EmailNotificationService emailService = new EmailNotificationService();

                for (String email : externalEmails) {
                    boolean sent = emailService.sendEventInvitation(
                            email,
                            event,
                            currentUser,
                            isNewEvent ? "You've been invited to an event" : "Event updated"
                    );

                    if (sent) {
                        System.out.println("📧 Email sent to: " + email);
                    } else {
                        System.err.println("❌ Failed to send email to: " + email);
                    }
                }
            } catch (Exception e) {
                System.err.println("❌ Error sending invitations: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    private void setupItemContextMenu(Node node, PlannerItem item) {
        ContextMenu menu = new ContextMenu();

        MenuItem editItem = new MenuItem("Edit");
        editItem.setOnAction(e -> {
            if (item.getItemType() == PlannerItemType.TASK) {
                showTaskDialog((TaskPlannerAdapter) item);
            } else {
                showEventDialog((EventPlannerAdapter) item);
            }
        });

        MenuItem toggleCompleteItem = new MenuItem(
                checkItemCompletion(item) ? "Mark as Incomplete" : "Mark as Complete"
        );
        toggleCompleteItem.setOnAction(e -> {
            if (item.getItemType() == PlannerItemType.TASK) {
                TaskPlannerAdapter adapter = (TaskPlannerAdapter) item;
                Task task = adapter.getTask();
                task.setStatus(
                        task.getStatus() == Status.COMPLETED ?
                                Status.TODO : Status.COMPLETED
                );
                plannerManager.updateTask(task);
            } else {
                EventPlannerAdapter adapter = (EventPlannerAdapter) item;
                CalendarEvent event = adapter.getEvent();
                event.toggleCompleted();
                plannerManager.updateEvent(event);
            }
            updateView();
        });

        MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            String itemType = item.getItemType() == PlannerItemType.TASK ? "Task" : "Event";
            alert.setTitle("Delete " + itemType);
            alert.setHeaderText("Are you sure?");
            alert.setContentText("Do you want to delete \"" + item.getTitle() + "\"?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    if (item.getItemType() == PlannerItemType.TASK) {
                        plannerManager.removeTask(((TaskPlannerAdapter) item).getTask());
                    } else {
                        plannerManager.removeEvent(((EventPlannerAdapter) item).getEvent());
                    }
                    updateView();
                }
            });
        });

        menu.getItems().addAll(editItem, toggleCompleteItem, new SeparatorMenuItem(), deleteItem);
        node.setOnContextMenuRequested(e -> menu.show(node, e.getScreenX(), e.getScreenY()));
    }

    // PUBLIC API
    public PlannerManager getPlannerManager() {
        return plannerManager;
    }

    public void addTask(Task task) {
        plannerManager.addTask(task);
        updateView();
    }

    public void addEvent(CalendarEvent event) {
        plannerManager.addEvent(event);
        updateView();
    }

    public void setCurrentDate(LocalDate date) {
        this.currentDate = date;
        updateView();
    }

    private String generateMeetingCode() {
        // Google Meet format: xxx-yyyy-zzz
        Random random = new Random();
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 3; i++) {
            if (i > 0) code.append("-");
            for (int j = 0; j < (i == 1 ? 4 : 3); j++) {
                code.append((char) ('a' + random.nextInt(26)));
            }
        }

        return code.toString();
    }

    public void setViewType(ViewType viewType) {
        this.currentView = viewType;
        switch (viewType) {
            case DAY -> dayViewButton.setSelected(true);
            case WEEK -> weekViewButton.setSelected(true);
            case MONTH -> monthViewButton.setSelected(true);
            case YEAR -> yearViewButton.setSelected(true);
            case KANBAN -> kanbanViewButton.setSelected(true);
        }
    }

    public void refreshCalendar() {
        updateView();
    }
}