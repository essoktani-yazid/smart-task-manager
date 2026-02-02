package com.smarttask.client.util;

import com.smarttask.model.User;

/**
 * Session Manager - Singleton
 * Stores the currently logged-in user across the application
 */
public class SessionManager {
    
    private static SessionManager instance;
    private User currentUser;
    
    private SessionManager() {
        // Private constructor for singleton
    }
    
    /**
     * Get singleton instance
     */
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Set current logged-in user
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        System.out.println("✅ Session set for user: " + user.getUsername() + " (ID: " + user.getId() + ")");
    }
    
    /**
     * Get current logged-in user
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return currentUser != null;
    }
    
    /**
     * Clear session (logout)
     */
    public void clearSession() {
        System.out.println("🚪 Session cleared for user: " + (currentUser != null ? currentUser.getUsername() : "none"));
        this.currentUser = null;
    }
    
    /**
     * Get current user ID
     */
    public String getCurrentUserId() {
        return currentUser != null ? currentUser.getId() : null;
    }
    
    /**
     * Get current username
     */
    public String getCurrentUsername() {
        return currentUser != null ? currentUser.getUsername() : null;
    }
}
