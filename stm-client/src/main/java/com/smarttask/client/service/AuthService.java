package com.smarttask.client.service;

import com.google.gson.Gson;
import com.smarttask.model.User;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.nio.charset.StandardCharsets;

/**
 * Service pour gérer l'authentification via l'API HTTP.
 * Communique avec le serveur pour les opérations de login et register.
 */
public class AuthService {
    private static final String BASE_URL = "http://localhost:8080/api/auth";
    private final Gson gson = new Gson();

    /**
     * Tente de connecter un utilisateur.
     * @param username Le nom d'utilisateur
     * @param password Le mot de passe
     * @return L'utilisateur connecté ou null si les identifiants sont incorrects
     */
    public User login(String username, String password) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL + "/login");
            request.setHeader("Content-Type", "application/json");

            LoginRequest loginRequest = new LoginRequest();
            loginRequest.username = username;
            loginRequest.password = password;

            String json = gson.toJson(loginRequest);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

                if (statusCode == 200) {
                    return gson.fromJson(responseBody, User.class);
                } else {
                    System.err.println("Erreur de connexion: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Enregistre un nouvel utilisateur.
     * @param username Le nom d'utilisateur
     * @param password Le mot de passe
     * @param email L'email
     * @return L'utilisateur créé ou null en cas d'erreur
     */
    public User register(String username, String password, String email) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(BASE_URL + "/register");
            request.setHeader("Content-Type", "application/json");

            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            user.setEmail(email);

            String json = gson.toJson(user);
            request.setEntity(new StringEntity(json, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                HttpEntity entity = response.getEntity();
                String responseBody = EntityUtils.toString(entity, StandardCharsets.UTF_8);

                if (statusCode == 201) {
                    return gson.fromJson(responseBody, User.class);
                } else {
                    System.err.println("Erreur d'inscription: " + responseBody);
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Classe interne pour la requête de login
    private static class LoginRequest {
        String username;
        String password;
    }
}

