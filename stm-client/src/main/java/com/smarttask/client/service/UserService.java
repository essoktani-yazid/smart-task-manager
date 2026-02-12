package com.smarttask.client.service;

import com.smarttask.client.util.GsonUtils;
import com.smarttask.model.User;
import com.smarttask.client.config.AppConfig;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Service to manage user operations.
 * Uses standard java.net.http.HttpClient for consistency.
 */
public class UserService {

    private static final String BASE_URL = AppConfig.API_URL + "/users";

    private final HttpClient client;

    public UserService() {
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Récupère tous les utilisateurs (Pour la liste d'assignation).
     * @return Une liste d'utilisateurs.
     */
    public List<User> findAll() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            User[] users = GsonUtils.getGson().fromJson(response.body(), User[].class);
            return Arrays.asList(users);
        } else {
            // En cas d'erreur serveur, on retourne une liste vide ou on log
            System.err.println("Erreur lors de la récupération des users : " + response.statusCode());
            return Collections.emptyList();
        }
    }

    /**
     * Récupère un utilisateur par son ID.
     */
    public User findById(String id) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/" + id))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            return GsonUtils.getGson().fromJson(response.body(), User.class);
        }
        return null;
    }

    /**
     * Met à jour le profil d'un utilisateur.
     */
    public boolean updateUser(User user) {
        try {
            String json = GsonUtils.getGson().toJson(user);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BASE_URL + "/" + user.getId()))
                    .header("Content-Type", "application/json")
                    .PUT(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Créer un nouvel utilisateur (Si besoin pour une interface d'admin).
     */
    public User createUser(User user) throws IOException, InterruptedException {
        String json = GsonUtils.getGson().toJson(user);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 201 || response.statusCode() == 200) {
            return GsonUtils.getGson().fromJson(response.body(), User.class);
        }
        throw new IOException("Erreur création user: " + response.body());
    }

	/**
	 * Récupère un utilisateur par son username.
	 */
	public User findByUsername(String username) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(BASE_URL + "/username/" + username))
				.GET()
				.build();

		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

		if (response.statusCode() == 200) {
			return GsonUtils.getGson().fromJson(response.body(), User.class);
		}

		return null;
	}
}
