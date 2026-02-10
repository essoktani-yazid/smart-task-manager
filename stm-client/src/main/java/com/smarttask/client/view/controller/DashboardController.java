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
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
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
    @FXML private Label timeTrackedLabel;
    @FXML private Label tasksDoneLabel;

    // --- FXML UI : CHARTS ---
    @FXML private BarChart<String, Number> performanceChart;
    @FXML private PieChart timeDistributionChart;

    // --- SERVICES & DATA ---
    private User currentUser;
    private WebSocket webSocket;
    private final Gson gson = GsonUtils.getGson();

    // Injection des Services
    private final TaskService taskService = new TaskService();
    private final TimeTrackingService timeTrackingService = new TimeTrackingService();
    private final ProjectService projectService = new ProjectService();

    // Stockage des stats pour l'IA
    private JsonObject calculatedStatsForAI;
    private boolean analysisSent = false;

    @FXML
    public void initialize() {
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        resetView();

        if (this.currentUser != null) {
            System.out.println("✅ Dashboard init pour : " + currentUser.getUsername());
            refreshDashboardData();
        } else {
            System.out.println("⚠️ Aucun utilisateur connecté (Dashboard)");
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

    // =================================================================================
    // 1. CHARGEMENT DES DONNÉES
    // =================================================================================

    private void refreshDashboardData() {
        new Thread(() -> {
            try {
                if (currentUser == null) return;

                List<Task> allTasks = taskService.getTasksByUser(currentUser.getId());
                List<TimeTracking> timeLogs = timeTrackingService.getTimeLogsByUser(currentUser.getId());
                List<Project> projects = projectService.getProjectsByUser(currentUser.getId());

                Platform.runLater(() -> {
                    calculateAndDisplayMetrics(allTasks, timeLogs);
                    updatePerformanceChart(allTasks);
                    updateDistributionChart(allTasks, projects);
                });

                prepareAIStats(allTasks, timeLogs);
                Platform.runLater(this::trySendAnalysis);

            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> insightTitleLabel.setText("Erreur chargement données"));
            }
        }).start();
    }

    // =================================================================================
    // 2. CALCULS ET AFFICHAGE (METRICS)
    // =================================================================================

    private void calculateAndDisplayMetrics(List<Task> tasks, List<TimeTracking> timeLogs) {
        int totalTasks = tasks.size();
        long tasksDone = tasks.stream().filter(t -> t.getStatus() == Status.COMPLETED).count();
        int productivityScore = totalTasks > 0 ? (int)((tasksDone * 100) / totalTasks) : 0;

        // --- CORRECTION ICI : getDurationMs() ---
        // On convertit ms en heures (/ 3600000.0) et on gère le null
        double totalHours = timeLogs.stream()
                .mapToDouble(log -> (log.getDurationMs() != null ? log.getDurationMs() : 0L) / 3600000.0) 
                .sum();

        int h = (int) totalHours;
        int m = (int) ((totalHours - h) * 60);

        TrendResult trend = calculateWeeklyTrend(tasks);

        productivityScoreLabel.setText(productivityScore + "%");
        tasksDoneLabel.setText(tasksDone + " / " + totalTasks);
        timeTrackedLabel.setText(String.format("%dh %02dm", h, m));

        if (productivityTrendLabel != null) {
            productivityTrendLabel.setText(trend.message);
            productivityTrendLabel.getStyleClass().removeAll("trend-up", "trend-down", "trend-neutral");
            if (trend.difference > 0) productivityTrendLabel.getStyleClass().add("trend-up");
            else if (trend.difference < 0) productivityTrendLabel.getStyleClass().add("trend-down");
            else productivityTrendLabel.getStyleClass().add("trend-neutral");
        }
    }

    private void prepareAIStats(List<Task> tasks, List<TimeTracking> timeLogs) {
        long overdue = tasks.stream()
                .filter(t -> t.getStatus() != Status.COMPLETED 
                        && t.getDueDate() != null 
                        && t.getDueDate().toLocalDate().isBefore(LocalDate.now()))
                .count();

        // --- CORRECTION ICI AUSSI : getDurationMs() ---
        double hours = timeLogs.stream()
                .mapToDouble(l -> (l.getDurationMs() != null ? l.getDurationMs() : 0L) / 3600000.0)
                .sum();

        calculatedStatsForAI = new JsonObject();
        calculatedStatsForAI.addProperty("username", currentUser.getUsername());
        calculatedStatsForAI.addProperty("tasks_total", tasks.size());
        calculatedStatsForAI.addProperty("tasks_done", tasks.stream().filter(t -> t.getStatus() == Status.COMPLETED).count());
        calculatedStatsForAI.addProperty("overdue", overdue);
        calculatedStatsForAI.addProperty("hours_worked", hours);
    }

    // =================================================================================
    // 3. GRAPHIQUES (CHARTS)
    // =================================================================================

    private void updatePerformanceChart(List<Task> tasks) {
        performanceChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Tâches Terminées");

        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEE");

        for (int i = 4; i >= 0; i--) {
            LocalDate dateToCheck = today.minusDays(i);
            long count = tasks.stream()
                    .filter(t -> t.getStatus() == Status.COMPLETED)
                    .filter(t -> t.getDueDate() != null && t.getDueDate().toLocalDate().isEqual(dateToCheck))
                    .count();
            series.getData().add(new XYChart.Data<>(dateToCheck.format(formatter), count));
        }
        performanceChart.getData().add(series);
    }

    private void updateDistributionChart(List<Task> tasks, List<Project> projects) {
        timeDistributionChart.getData().clear();
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();

        Map<String, String> projectNames = projects.stream()
                .collect(Collectors.toMap(p -> p.getId().toString(), Project::getName));

        Map<String, Long> distribution = new HashMap<>();

        for (Task t : tasks) {
            String label = "Sans Projet";
            if (t.getProjectId() != null) {
                label = projectNames.getOrDefault(t.getProjectId().toString(), "Inconnu");
            }
            distribution.put(label, distribution.getOrDefault(label, 0L) + 1);
        }

        distribution.forEach((name, count) -> pieData.add(new PieChart.Data(name, count)));
        timeDistributionChart.setData(pieData);
    }

    // =================================================================================
    // 4. LOGIQUE TENDANCE (WEEKLY TREND)
    // =================================================================================

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

    // =================================================================================
    // 5. WEBSOCKET & IA INTELLIGENCE
    // =================================================================================

    private void connectWebSocket() {
        System.out.println("⏳ Connexion WebSocket vers " + AppConfig.AI_WS_URL);
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create(AppConfig.AI_WS_URL), new WebSocketListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    System.out.println("✅ WebSocket Connecté");
                    trySendAnalysis();
                })
                .exceptionally(ex -> {
                    System.err.println("❌ Erreur WebSocket : " + ex.getMessage());
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

        System.out.println("📤 Envoi Stats Réelles à l'IA : " + calculatedStatsForAI);
        webSocket.sendText(gson.toJson(payload), true);
        analysisSent = true;
    }

    private void handleAIResponse(String jsonString) {
        Platform.runLater(() -> {
            try {
                JsonObject json = gson.fromJson(jsonString, JsonObject.class);
                String mood = json.has("mood") ? json.get("mood").getAsString() : "🤖";
                String title = json.has("title") ? json.get("title").getAsString() : "Analyse";
                String message = json.has("message") ? json.get("message").getAsString() : "";
                String colorHex = json.has("theme_color") ? json.get("theme_color").getAsString() : "#6366F1";
                String actionLabel = (json.has("action_label") && !json.get("action_label").isJsonNull()) 
                                     ? json.get("action_label").getAsString() : null;

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
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // =================================================================================
    // 6. ANIMATIONS & DESIGN
    // =================================================================================

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
        String rgbaBg = hexToRgba(colorHex, 0.1);
        
        smartHeaderCard.setStyle(
            "-fx-background-color: white; " +
            "-fx-background-radius: 20; " +
            "-fx-border-color: " + colorHex + "; " +
            "-fx-border-radius: 20; " +
            "-fx-border-width: 1.5; " +
            "-fx-effect: dropshadow(three-pass-box, " + rgbaShadow + ", 20, 0, 0, 5);"
        );
        moodContainer.setStyle("-fx-background-color: " + rgbaBg + "; -fx-background-radius: 50%;");
        smartHeaderCard.setOpacity(1.0);
    }

    private void animateCardEntrance() {
        FadeTransition ft = new FadeTransition(Duration.millis(600), smartHeaderCard);
        ft.setFromValue(0.0); ft.setToValue(1.0);
        TranslateTransition tt = new TranslateTransition(Duration.millis(600), smartHeaderCard);
        tt.setFromY(20); tt.setToY(0);
        ft.play(); tt.play();
    }

    private void resetView() {
        moodEmojiLabel.setText("⏳");
        insightTitleLabel.setText("Connexion...");
        insightMessageLabel.setText("Récupération des données...");
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
        @Override
        public void onOpen(WebSocket webSocket) { WebSocket.Listener.super.onOpen(webSocket); }
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            handleAIResponse(data.toString());
            return WebSocket.Listener.super.onText(webSocket, data, last);
        }
    }
}