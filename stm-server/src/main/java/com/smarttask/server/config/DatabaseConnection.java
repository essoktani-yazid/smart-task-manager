// package com.smarttask.server.config;

// import java.io.InputStream;
// import java.sql.Connection;
// import java.sql.DriverManager;
// import java.sql.SQLException;
// import java.util.Properties;

// /**
//  * Utility class for managing JDBC database connections.
//  * Uses a connection pool pattern for better performance.
//  */
// public class DatabaseConnection {
//     private static String url;
//     private static String username;
//     private static String password;
//     private static String driver;
//     private static DatabaseConnection instance;

//     static {
//         loadDatabaseProperties();
//         try {
//             Class.forName(driver);
//         } catch (ClassNotFoundException e) {
//             throw new RuntimeException("MySQL JDBC Driver not found", e);
//         }
//     }

//     /**
//      * Loads database configuration from database.properties file.
//      */
//     private static void loadDatabaseProperties() {
//         try (InputStream input = DatabaseConnection.class.getClassLoader()
//                 .getResourceAsStream("database.properties")) {
//             if (input == null) {
//                 throw new RuntimeException("Unable to find database.properties");
//             }

//             Properties properties = new Properties();
//             properties.load(input);

//             url = properties.getProperty("db.url");
//             username = properties.getProperty("db.username");
//             password = properties.getProperty("db.password");
//             driver = properties.getProperty("db.driver");
//         } catch (Exception e) {
//             throw new RuntimeException("Error loading database properties", e);
//         }
//     }

//     /**
//      * Gets a new database connection.
//      * 
//      * @return Connection object
//      * @throws SQLException if a database access error occurs
//      */
//     public static Connection getConnection() throws SQLException {
//         return DriverManager.getConnection(url, username, password);
//     }

//     public static DatabaseConnection getInstance() {
//         if (instance == null) {
//             synchronized (DatabaseConnection.class) {
//                 if (instance == null) {
//                     instance = new DatabaseConnection();
//                 }
//             }
//         }
//         return instance;
//     }

//     /**
//      * Closes a database connection.
//      * 
//      * @param connection the connection to close
//      */
//     public static void closeConnection(Connection connection) {
//         if (connection != null) {
//             try {
//                 connection.close();
//             } catch (SQLException e) {
//                 System.err.println("Error closing connection: " + e.getMessage());
//             }
//         }
//     }
// }


package com.smarttask.server.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static String url;
    private static String username;
    private static String password;
    private static String driver = "com.mysql.cj.jdbc.Driver"; 
    private static DatabaseConnection instance;

    static {
        // --- CONFIGURATION RAILWAY ---
        // On récupère les composants séparés pour construire une URL JDBC propre
        String host = System.getenv("MYSQLHOST");
        String port = System.getenv("MYSQLPORT");
        String dbName = System.getenv("MYSQLDATABASE");
        
        String envUser = System.getenv("MYSQLUSER");
        String envPass = System.getenv("MYSQLPASSWORD");

        if (host != null && !host.isEmpty()) {
            System.out.println(">>> Mode Railway détecté.");
            
            // Construction manuelle de l'URL pour éviter les erreurs de parsing et ajouter les options SSL
            // allowPublicKeyRetrieval=true est OBLIGATOIRE sur Railway avec le nouveau driver
            url = String.format("jdbc:mysql://%s:%s/%s?allowPublicKeyRetrieval=true&useSSL=false&characterEncoding=UTF-8", 
                                host, port, dbName);
            
            username = envUser;
            password = envPass;
            
            System.out.println(">>> URL JDBC générée : " + url); // Log pour débugger (sans le mot de passe)
        } else {
            // --- CONFIGURATION LOCALE ---
            System.out.println(">>> Mode Local détecté. Chargement de database.properties...");
            loadDatabaseProperties();
        }

        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("MySQL JDBC Driver not found", e);
        }
    }

    private static void loadDatabaseProperties() {
        try (InputStream input = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            
            if (input == null) throw new RuntimeException("database.properties introuvable !");

            Properties prop = new Properties();
            prop.load(input);

            url = prop.getProperty("db.url");
            username = prop.getProperty("db.username");
            password = prop.getProperty("db.password");
            
        } catch (Exception e) {
            throw new RuntimeException("Erreur chargement properties", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (url == null) throw new SQLException("URL Base de données non configurée");
        return DriverManager.getConnection(url, username, password);
    }
    
    // ... (Gardez les méthodes getInstance et closeConnection comme avant) ...
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) instance = new DatabaseConnection();
            }
        }
        return instance;
    }

    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) { /* Ignorer */ }
        }
    }
}