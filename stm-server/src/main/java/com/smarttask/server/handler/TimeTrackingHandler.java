package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.TimeTracking;
import com.smarttask.server.dao.TimeTrackingDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class TimeTrackingHandler implements HttpHandler {
    private final TimeTrackingDAO dao = new TimeTrackingDAO();
    private final Gson gson = new Gson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method) && path.startsWith("/api/timetracking/task/")) {
                String taskId = path.substring("/api/timetracking/task/".length());
                List<TimeTracking> list = dao.findByTask(taskId);
                sendResponse(exchange, 200, gson.toJson(list));
            } else if ("POST".equals(method) && path.equals("/api/timetracking")) {
                String body = readRequestBody(exchange);
                TimeTracking tt = gson.fromJson(body, TimeTracking.class);
                dao.save(tt);
                sendResponse(exchange, 201, gson.toJson(tt));
            } else if ("PUT".equals(method) && path.startsWith("/api/timetracking/")) {
                String body = readRequestBody(exchange);
                TimeTracking tt = gson.fromJson(body, TimeTracking.class);
                dao.update(tt);
                sendResponse(exchange, 200, gson.toJson(tt));
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, response.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}
