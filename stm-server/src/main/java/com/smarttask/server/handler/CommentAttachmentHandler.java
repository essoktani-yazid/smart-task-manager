package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.CommentAttachment;
import com.smarttask.server.dao.CommentAttachmentDAO;
import com.smarttask.server.util.GsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class CommentAttachmentHandler implements HttpHandler {
    private final CommentAttachmentDAO attachmentDAO = new CommentAttachmentDAO();
    private final Gson gson = GsonUtils.getGson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        try {
            if ("GET".equals(method)) {
                if (path.startsWith("/api/comment-attachments/comment/")) {
                    String commentId = path.substring("/api/comment-attachments/comment/".length());
                    handleGetByCommentId(exchange, commentId);
                } else if (path.startsWith("/api/comment-attachments/")) {
                    String id = path.substring("/api/comment-attachments/".length());
                    handleGetById(exchange, id);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("POST".equals(method)) {
                if (path.equals("/api/comment-attachments")) {
                    handleCreate(exchange);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else if ("DELETE".equals(method)) {
                if (path.startsWith("/api/comment-attachments/")) {
                    String id = path.substring("/api/comment-attachments/".length());
                    handleDelete(exchange, id);
                } else {
                    sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetByCommentId(HttpExchange exchange, String commentId) throws IOException {
        try {
            List<CommentAttachment> attachments = attachmentDAO.findByCommentId(commentId);
            String json = gson.toJson(attachments);
            sendResponse(exchange, 200, json);
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        try {
            var attachment = attachmentDAO.findById(id);
            if (attachment.isPresent()) {
                String json = gson.toJson(attachment.get());
                sendResponse(exchange, 200, json);
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        try {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            CommentAttachment attachment = gson.fromJson(body, CommentAttachment.class);
            
            String id = attachmentDAO.save(attachment);
            attachment.setId(id);
            
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 201, gson.toJson(attachment));
        } catch (Exception e) {
            sendResponse(exchange, 400, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void handleDelete(HttpExchange exchange, String id) throws IOException {
        try {
            if (attachmentDAO.deleteById(id)) {
                sendResponse(exchange, 204, "");
            } else {
                sendResponse(exchange, 404, "{\"error\":\"Not Found\"}");
            }
        } catch (Exception e) {
            sendResponse(exchange, 500, "{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.sendResponseHeaders(statusCode, body.getBytes(StandardCharsets.UTF_8).length);
        OutputStream os = exchange.getResponseBody();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.close();
    }
}
