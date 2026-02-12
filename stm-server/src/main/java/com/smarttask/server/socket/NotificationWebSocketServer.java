package com.smarttask.server.socket;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import com.google.gson.JsonObject;
import com.smarttask.server.util.GsonUtils;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NotificationWebSocketServer extends WebSocketServer {

    private static final Map<String, WebSocket> userSessions = new ConcurrentHashMap<>();

    public NotificationWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("New WebSocket connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        if (message.startsWith("AUTH:")) {
            String userId = message.split(":")[1];
            userSessions.put(userId, conn);
            System.out.println("User " + userId + " authenticated on WebSocket.");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        userSessions.values().remove(conn);
    }

    public static void sendToUser(String userId, Object notificationPayload) {
        WebSocket conn = userSessions.get(userId);
        if (conn != null && conn.isOpen()) {
            JsonObject response = new JsonObject();
            response.addProperty("type", "NOTIFICATION");
            response.add("payload", GsonUtils.getGson().toJsonTree(notificationPayload));
            
            conn.send(response.toString());
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) { ex.printStackTrace(); }
    @Override
    public void onStart() { System.out.println("WebSocket Server started on port: " + getPort()); }
}