package com.smarttask.server.handler;

import com.google.gson.Gson;
import com.smarttask.model.User;
import com.smarttask.server.dao.UserDAO;
import com.smarttask.server.util.GsonUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public class UserHandler implements HttpHandler {

    private final UserDAO dao = new UserDAO();
    private final Gson gson = GsonUtils.getGson();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String path = exchange.getRequestURI().getPath();

        // 1. GESTION CORS
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(method)) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        try {
            // 2. ROUTAGE

			if (path.startsWith("/api/users/username/")) {
				String username = path.substring("/api/users/username/".length());

				if (username.isEmpty()) {
					sendResponse(exchange, 400, "{\"error\":\"Missing username\"}");
					return;
				}

				if ("GET".equals(method)) {
					handleGetByUsername(exchange, username);
				} else {
					sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
				}
				return;
			}

            // CAS A : /api/users (Collection)
            if (path.equals("/api/users")) {
                if ("GET".equals(method)) {
                    handleGetAll(exchange);
                } else if ("POST".equals(method)) {
                    handleCreate(exchange);
                } else {
                    sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
                return;
            }

            // CAS B : /api/users/{id} (Individuel)
            if (path.startsWith("/api/users/")) {
                String userId = path.substring("/api/users/".length());

                if (userId.isEmpty()) {
                    sendResponse(exchange, 400, "{\"error\":\"Missing ID\"}");
                    return;
                }

                switch (method) {
                    case "GET":
                        handleGetById(exchange, userId);
                        break;
                    case "PUT":
                        handleUpdateUser(exchange, userId);
                        break;
                    case "DELETE":
                        handleDelete(exchange, userId);
                        break;
                    default:
                        sendResponse(exchange, 405, "{\"error\":\"Method Not Allowed\"}");
                }
                return;
            }

            sendResponse(exchange, 404, "{\"error\":\"Endpoint Not Found\"}");

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Internal Server Error: " + e.getMessage() + "\"}");
        }
    }

    // --- MÉTHODES DE TRAITEMENT CORRIGÉES ---

    private void handleGetAll(HttpExchange exchange) throws IOException {
        // Cette méthode fonctionne car on a ajouté findAll() dans le DAO
        List<User> users = dao.findAll();
        String json = gson.toJson(users);
        sendResponse(exchange, 200, json);
    }

    private void handleGetById(HttpExchange exchange, String id) throws IOException {
        // CORRECTION : Gestion de l'Optional
        Optional<User> userOpt = dao.findById(id);

        if (userOpt.isPresent()) {
            sendResponse(exchange, 200, gson.toJson(userOpt.get()));
        } else {
            sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
        }
    }

    private void handleCreate(HttpExchange exchange) throws IOException {
        String body = readRequestBody(exchange);
        User newUser = gson.fromJson(body, User.class);

        try {
            // CORRECTION : DAO.save retourne l'ID (String), pas void
            String newId = dao.save(newUser);
            newUser.setId(newId); // On met à jour l'objet avec son nouvel ID

            sendResponse(exchange, 201, gson.toJson(newUser));
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Could not create user\"}");
        }
    }

    private void handleUpdateUser(HttpExchange exchange, String userId) throws IOException {
        String body = readRequestBody(exchange);
        User userToUpdate = gson.fromJson(body, User.class);

        userToUpdate.setId(userId);

        try {
            // CORRECTION : DAO.update est void. On vérifie l'existence avant.
            if (dao.findById(userId).isEmpty()) {
                sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
                return;
            }

            dao.update(userToUpdate);
            // Si pas d'exception levée, c'est un succès
            sendResponse(exchange, 200, gson.toJson(userToUpdate));

        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Update failed: " + e.getMessage() + "\"}");
        }
    }

    private void handleDelete(HttpExchange exchange, String userId) throws IOException {
        try {
            // CORRECTION : On vérifie l'existence avant de supprimer
            if (dao.findById(userId).isEmpty()) {
                sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
                return;
            }

            // CORRECTION : La méthode s'appelle deleteById, pas delete
            dao.deleteById(userId);

            sendResponse(exchange, 200, "{\"message\":\"User deleted\"}");
        } catch (Exception e) {
            e.printStackTrace();
            sendResponse(exchange, 500, "{\"error\":\"Delete failed\"}");
        }
    }

	private void handleGetByUsername(HttpExchange exchange, String username) throws IOException {
		Optional<User> userOpt = dao.findByUsername(username);

		if (userOpt.isPresent()) {
			sendResponse(exchange, 200, gson.toJson(userOpt.get()));
		} else {
			sendResponse(exchange, 404, "{\"error\":\"User not found\"}");
		}
	}

    // --- UTILITAIRES ---

    private String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
