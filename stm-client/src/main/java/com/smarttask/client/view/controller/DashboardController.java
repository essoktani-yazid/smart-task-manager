package com.smarttask.client.view.controller;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.smarttask.client.config.AppConfig;
import com.smarttask.client.service.ProjectService;
import com.smarttask.client.service.TaskService;
import com.smarttask.client.service.TimeTrackingService;
import com.smarttask.client.util.GsonUtils;
import com.smarttask.client.util.SessionManager;
import com.smarttask.model.Project;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.TimeTracking;
import com.smarttask.model.User;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;

public class DashboardController {

    // --- FXML UI : SMART HEADER ---
    @FXML private HBox smartHeaderCard;
    @FXML private StackPane moodContainer;
    @FXML private Label moodEmojiLabel;
    @FXML private Label insightTitleLabel;
    @FXML private Label insightMessageLabel;
    @FXML private Button smartActionButton;

    // --- FXML UI : METRICS ---
    @FXML private Label productivityScoreLabel;
    @FXML private Label productivityTrendLabel;
    @FXML private Label overdueTasksLabel;
    @FXML private Label tasksDoneLabel;

    // --- FXML UI : CHARTS & LISTS ---
    @FXML private BarChart<String, Number> performanceChart;
    @FXML private PieChart timeDistributionChart;
    @FXML private VBox upcomingTasksContainer;

    // --- SERVICES & DATA ---
    private User currentUser;
    private WebSocket webSocket;
    private final Gson gson = GsonUtils.getGson();

    private final TaskService taskService = new TaskService();
    private final TimeTrackingService timeTrackingService = new TimeTrackingService();
    private final ProjectService projectService = new ProjectService();

    private JsonObject calculatedStatsForAI;
    private boolean analysisSent = false;

    @FXML
    public void initialize() {
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        resetView();

        if (this.currentUser != null) {
            System.out.println("✅ Dashboard init for: " + currentUser.getUsername());
            refreshDashboardData();
        } else {
            System.out.println("⚠️ No user logged in (Dashboard)");
        }

        connectWebSocket();
    }

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (this.currentUser != null) {
            refreshDashboardData();
            trySendAnalysis();
        }
    }

    private void refreshDashboardData() {
        // ⚡ OPTIMISÉ: Utilisation d'ExecutorService au lieu de new Thread()
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                if (currentUser == null) return;

                long startTime = System.currentTimeMillis();
                
                // ⚡ Chargement parallèle des données
                java.util.concurrent.CompletableFuture<List<Task>> tasksFuture = 
                    java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> taskService.getTasksByUser(currentUser.getId())
                    );
                
                java.util.concurrent.CompletableFuture<List<TimeTracking>> timeLogsFuture = 
                    java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> timeTrackingService.getTimeLogsByUser(currentUser.getId())
                    );
                
                java.util.concurrent.CompletableFuture<List<Project>> projectsFuture = 
                    java.util.concurrent.CompletableFuture.supplyAsync(
                        () -> projectService.getProjectsByUser(currentUser.getId())
                    );
                
                // Attendre tous les résultats en parallèle
                List<Task> allTasks = tasksFuture.join();
                List<TimeTracking> timeLogs = timeLogsFuture.join();
                List<Project> projects = projectsFuture.join();
                
                long loadTime = System.currentTimeMillis() - startTime;
                System.out.println("⚡ Data loaded in " + loadTime + "ms (parallel)");

                // ⚡ Mise à jour UI en un seul batch
                Platform.runLater(() -> {
                    long uiStartTime = System.currentTimeMillis();
                    calculateAndDisplayMetrics(allTasks);
                    updatePerformanceChart(allTasks);
                    updateDistributionChart(allTasks, projects);
                    loadUpcomingTasks(allTasks);
                    long uiTime = System.currentTimeMillis() - uiStartTime;
                    System.out.println("⚡ UI updated in " + uiTime + "ms");
                });

                prepareAIStats(allTasks, timeLogs);
                Platform.runLater(this::trySendAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> insightTitleLabel.setText("Error loading data"));
            } finally {
                executor.shutdown();
            }
        });
    }

    // --- METRICS ---
    private void calculateAndDisplayMetrics(List<Task> tasks) {
        int totalTasks = tasks.size();
        
        long tasksDone = tasks.stream().filter(t -> t.getStatus() == Status.COMPLETED).count();
        long tasksOverdue = tasks.stream()
                .filter(t -> t.getStatus() != Status.COMPLETED && t.getDueDate() != null 
                        && t.getDueDate().toLocalDate().isBefore(LocalDate.now()))
                .count();

        int productivityScore = totalTasks > 0 ? (int)((tasksDone * 100) / totalTasks) : 0;
        TrendResult trend = calculateWeeklyTrend(tasks);

        productivityScoreLabel.setText(productivityScore + "%");
        tasksDoneLabel.setText(tasksDone + " / " + totalTasks);
        if (overdueTasksLabel != null) {
            overdueTasksLabel.setText(String.valueOf(tasksOverdue));
        }

        if (productivityTrendLabel != null) {
            productivityTrendLabel.setText(trend.message);
            productivityTrendLabel.getStyleClass().removeAll("trend-up", "trend-down", "trend-neutral");
            if (trend.difference > 0) productivityTrendLabel.getStyleClass().add("trend-up");
            else if (trend.difference < 0) productivityTrendLabel.getStyleClass().add("trend-down");
            else productivityTrendLabel.getStyleClass().add("trend-neutral");
        }
    }

    // --- UPCOMING TASKS (URGENT LIST) ---
    private void loadUpcomingTasks(List<Task> tasks) {
        if (upcomingTasksContainer == null) return;
        upcomingTasksContainer.getChildren().clear();

        List<Task> urgentTasks = tasks.stream()
                .filter(t -> t.getStatus() != Status.COMPLETED)
                .filter(t -> t.getDueDate() != null)
                .sorted(Comparator.comparing(Task::getDueDate))
                .limit(4)
                .collect(Collectors.toList());

        if (urgentTasks.isEmpty()) {
            Label emptyLabel = new Label("No urgent tasks! 🎉");
            emptyLabel.setStyle("-fx-text-fill: #9CA3AF; -fx-font-style: italic; -fx-font-size: 12px;");
            upcomingTasksContainer.getChildren().add(emptyLabel);
            return;
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM dd");

        for (Task t : urgentTasks) {
            HBox row = new HBox(10);
            row.getStyleClass().add("task-row");
            row.setAlignment(Pos.CENTER_LEFT);

            Circle dot = new Circle(4);
            String color = switch (t.getPriority()) {
                case HIGH, URGENT -> "#EF4444";
                case MEDIUM -> "#F59E0B";
                default -> "#10B981";
            };
            dot.setStyle("-fx-fill: " + color + ";");

            VBox texts = new VBox(2);
            Label title = new Label(t.getTitle());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #374151; -fx-font-size: 12px;");
            
            Label date = new Label("Due: " + t.getDueDate().format(fmt));
            
            if (t.getDueDate().toLocalDate().isBefore(LocalDate.now())) {
                date.setStyle("-fx-font-size: 10px; -fx-text-fill: #EF4444; -fx-font-weight: bold;");
                date.setText("Overdue! (" + t.getDueDate().format(fmt) + ")");
            } else {
                date.setStyle("-fx-font-size: 10px; -fx-text-fill: #6B7280;");
            }

            texts.getChildren().addAll(title, date);
            row.getChildren().addAll(dot, texts);
            upcomingTasksContainer.getChildren().add(row);
        }
    }

    // --- CHARTS ---
    private void updatePerformanceChart(List<Task> tasks) {
        performanceChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Completed Tasks");

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE");

        for (int i = 4; i >= 0; i--) {
            LocalDate dateToCheck = today.minusDays(i);
            long count = tasks.stream()
                    .filter(t -> t.getStatus() == Status.COMPLETED)
                    .filter(t -> t.getDueDate() != null && t.getDueDate().toLocalDate().isEqual(dateToCheck))
                    .count();
            
            XYChart.Data<String, Number> data = new XYChart.Data<>(dateToCheck.format(formatter), count);
            series.getData().add(data);
        }
        performanceChart.getData().add(series);

        for (XYChart.Data<String, Number> data : series.getData()) {
            Tooltip t = new Tooltip(data.getYValue() + " tasks");
            t.setStyle("-fx-background-color: #4F46E5; -fx-text-fill: white;");
            if (data.getNode() != null) {
                Tooltip.install(data.getNode(), t);
                data.getNode().setOnMouseEntered(e -> data.getNode().setStyle("-fx-bar-fill: #4338CA;"));
                data.getNode().setOnMouseExited(e -> data.getNode().setStyle(""));
            }
        }
    }

    private void updateDistributionChart(List<Task> tasks, List<Project> projects) {
        timeDistributionChart.getData().clear();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        Map<String, String> projectNames = projects.stream()
                .collect(Collectors.toMap(p -> p.getId().toString(), Project::getName));

        Map<String, Long> distribution = new HashMap<>();
        for (Task t : tasks) {
            String label = "No Project";
            if (t.getProjectId() != null) label = projectNames.getOrDefault(t.getProjectId().toString(), "Unknown");
            distribution.put(label, distribution.getOrDefault(label, 0L) + 1);
        }

        distribution.forEach((name, count) -> pieData.add(new PieChart.Data(name, count)));
        timeDistributionChart.setData(pieData);

        pieData.forEach(data -> {
            String tooltipText = data.getName() + ": " + (int) data.getPieValue() + " tasks";
            Tooltip t = new Tooltip(tooltipText);
            t.setStyle("-fx-background-color: white; -fx-text-fill: #374151; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.2), 5, 0, 0, 2);");
            Tooltip.install(data.getNode(), t);
        });
    }

    // --- AI & LOGIC ---
    
    private void prepareAIStats(List<Task> tasks, List<TimeTracking> timeLogs) {
        long overdue = tasks.stream()
                .filter(t -> t.getStatus() != Status.COMPLETED && t.getDueDate() != null && t.getDueDate().toLocalDate().isBefore(LocalDate.now()))
                .count();
        double hours = timeLogs.stream().mapToDouble(l -> (l.getDurationMs() != null ? l.getDurationMs() : 0L) / 3600000.0).sum();

        calculatedStatsForAI = new JsonObject();
        calculatedStatsForAI.addProperty("username", currentUser.getUsername());
        calculatedStatsForAI.addProperty("tasks_total", tasks.size());
        calculatedStatsForAI.addProperty("tasks_done", tasks.stream().filter(t -> t.getStatus() == Status.COMPLETED).count());
        calculatedStatsForAI.addProperty("overdue", overdue);
        calculatedStatsForAI.addProperty("hours_worked", hours);
    }

    private TrendResult calculateWeeklyTrend(List<Task> allTasks) {
        LocalDate today = LocalDate.now();
        LocalDate startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
        LocalDate endOfLastWeek = startOfThisWeek.minusDays(1);

        double scoreThisWeek = calculatePeriodScore(allTasks, startOfThisWeek, today);
        double scoreLastWeek = calculatePeriodScore(allTasks, startOfLastWeek, endOfLastWeek);
        double diff = scoreThisWeek - scoreLastWeek;

        String sign = diff > 0 ? "+" : "";
        String msg = String.format("%s%.0f%% vs last week", sign, diff);
        return new TrendResult(diff, msg);
    }

    private double calculatePeriodScore(List<Task> tasks, LocalDate start, LocalDate end) {
        List<Task> periodTasks = tasks.stream()
                .filter(t -> t.getDueDate() != null)
                .filter(t -> {
                    LocalDate d = t.getDueDate().toLocalDate();
                    return !d.isBefore(start) && !d.isAfter(end);
                })
                .collect(Collectors.toList());

        if (periodTasks.isEmpty()) return 0.0;
        long done = periodTasks.stream().filter(t -> t.getStatus() == Status.COMPLETED).count();
        return ((double) done / periodTasks.size()) * 100.0;
    }

    private static class TrendResult {
        double difference;
        String message;
        TrendResult(double d, String m) { difference = d; message = m; }
    }

    private void connectWebSocket() {
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(AppConfig.AI_WS_URL), new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    System.out.println("✅ WebSocket Connected");
                    trySendAnalysis();
                })
                .exceptionally(ex -> {
                    System.err.println("❌ WebSocket Error: " + ex.getMessage());
                    return null;
                });
    }

    private void trySendAnalysis() {
        if (webSocket == null || currentUser == null || calculatedStatsForAI == null) return;
        requestAIAnalysis();
    }

    private void requestAIAnalysis() {
        if (analysisSent) return;
        JsonObject payload = new JsonObject();
        payload.addProperty("action", "ANALYZE_DASHBOARD");
        payload.add("stats", calculatedStatsForAI);
        webSocket.sendText(gson.toJson(payload), true);
        analysisSent = true;
    }

    private void handleAIResponse(String jsonString) {
        Platform.runLater(() -> {
            try {
                JsonObject json = gson.fromJson(jsonString, JsonObject.class);
                String mood = json.has("mood") ? json.get("mood").getAsString() : "🤖";
                String title = json.has("title") ? json.get("title").getAsString() : "Analysis";
                String message = json.has("message") ? json.get("message").getAsString() : "";
                String colorHex = json.has("theme_color") ? json.get("theme_color").getAsString() : "#6366F1";
                String actionLabel = (json.has("action_label") && !json.get("action_label").isJsonNull()) ? json.get("action_label").getAsString() : null;

                updateDesign(mood, title, colorHex);
                animateCardEntrance();
                streamText(message);

                if (actionLabel != null) {
                    smartActionButton.setVisible(true);
                    smartActionButton.setText(actionLabel);
                    smartActionButton.setStyle("-fx-background-color: " + colorHex + "; -fx-text-fill: white; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand;");
                } else {
                    smartActionButton.setVisible(false);
                }
            } catch (Exception e) { e.printStackTrace(); }
        });
    }

    private void streamText(String fullText) {
        insightMessageLabel.setText("");
        final Integer[] charIndex = {0};
        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(30), event -> {
            if (charIndex[0] < fullText.length()) {
                insightMessageLabel.setText(insightMessageLabel.getText() + fullText.charAt(charIndex[0]));
                charIndex[0]++;
            }
        });
        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(fullText.length());
        timeline.play();
    }

    private void updateDesign(String mood, String title, String colorHex) {
        moodEmojiLabel.setText(mood);
        insightTitleLabel.setText(title);
        insightTitleLabel.setStyle("-fx-text-fill: " + colorHex + "; -fx-font-weight: bold; -fx-font-size: 18px;");
        String rgbaShadow = hexToRgba(colorHex, 0.25);
        smartHeaderCard.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-border-color: " + colorHex + "; -fx-border-radius: 20; -fx-border-width: 1.5; -fx-effect: dropshadow(three-pass-box, " + rgbaShadow + ", 20, 0, 0, 5);");
        moodContainer.setStyle("-fx-background-color: " + hexToRgba(colorHex, 0.1) + "; -fx-background-radius: 50%;");
        smartHeaderCard.setOpacity(1.0);
    }

    private void animateCardEntrance() {
        // ⚡ OPTIMISÉ: Animation plus rapide (600ms -> 300ms)
        FadeTransition ft = new FadeTransition(Duration.millis(300), smartHeaderCard);
        ft.setFromValue(0.0); ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(300), smartHeaderCard);
        tt.setFromY(20); tt.setToY(0);
        ft.play(); tt.play();
    }

    private void resetView() {
        moodEmojiLabel.setText("⏳");
        insightTitleLabel.setText("Connecting...");
        insightMessageLabel.setText("Fetching your stats...");
        smartActionButton.setVisible(false);
        smartHeaderCard.setOpacity(0.6);
    }

    private String hexToRgba(String hex, double alpha) {
        if (hex.startsWith("#")) hex = hex.substring(1);
        try {
            int r = Integer.valueOf(hex.substring(0, 2), 16);
            int g = Integer.valueOf(hex.substring(2, 4), 16);
            int b = Integer.valueOf(hex.substring(4, 6), 16);
            return String.format("rgba(%d, %d, %d, %.2f)", r, g, b, alpha);
        } catch (Exception e) { return "rgba(0,0,0,0.1)"; }
    }

    private class WebSocketListener implements WebSocket.Listener {
        @Override public void onOpen(WebSocket webSocket) { WebSocket.Listener.super.onOpen(webSocket); }
        @Override public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            handleAIResponse(data.toString());
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
    }
}