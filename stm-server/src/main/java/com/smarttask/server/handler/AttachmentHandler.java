package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.smarttask.model.Attachment;
import com.smarttask.server.dao.AttachmentDAO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class AttachmentHandler implements HttpHandler {
    private final AttachmentDAO dao = new AttachmentDAO();
    private final Gson gson;

    public AttachmentHandler() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new com.google.gson.JsonSerializer<LocalDateTime>() {
            @Override
            public com.google.gson.JsonElement serialize(LocalDateTime src, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonSerializationContext context) {
                return new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
        });
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new com.google.gson.JsonDeserializer<LocalDateTime>() {
            @Override
            public LocalDateTime deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type typeOfSrc, com.google.gson.JsonDeserializationContext context) {
                return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        });
        this.gson = gsonBuilder.create();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method) && path.startsWith("/api/attachments/task/")) {
                String taskId = path.substring("/api/attachments/task/".length());
                List<Attachment> list = dao.findByTask(taskId);
                sendResponse(exchange, 200, gson.toJson(list));
            } else if ("POST".equals(method) && path.equals("/api/attachments")) {
                String body = readRequestBody(exchange);
                Attachment a = gson.fromJson(body, Attachment.class);
                dao.save(a);
                sendResponse(exchange, 201, gson.toJson(a));
            } else if ("DELETE".equals(method) && path.startsWith("/api/attachments/")) {
                String id = path.substring("/api/attachments/".length());
                if (dao.delete(id)) {
                    sendResponse(exchange, 200, "{\"message\":\"Deleted\"}");
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not found\"}");
                }
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
