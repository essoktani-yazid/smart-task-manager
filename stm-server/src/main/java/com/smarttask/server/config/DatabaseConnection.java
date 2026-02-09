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

/**
 * Utility class for managing JDBC database connections.
 * Compatible with Railway (Environment Variables) and Local (database.properties).
 */
public class DatabaseConnection {
    private static String url;
    private static String username;
    private static String password;
    private static String driver = "com.mysql.cj.jdbc.Driver"; // Driver par défaut
    private static DatabaseConnection instance;

    static {
        // Étape 1 : On tente de charger la configuration depuis Railway (Variables d'environnement)
        String envUrl = System.getenv("DATABASE_URL");
        String envUser = System.getenv("MYSQLUSER");
        String envPass = System.getenv("MYSQLPASSWORD");

        if (envUrl != null && !envUrl.isEmpty()) {
            System.out.println(">>> Configuration Railway détectée. Utilisation des variables d'environnement.");
            
            // Correction automatique de l'URL pour JDBC si nécessaire
            if (envUrl.startsWith("mysql://")) {
                url = envUrl.replace("mysql://", "jdbc:mysql://");
            } else {
                url = envUrl;
            }
            
            username = envUser;
            password = envPass;
        } else {
            // Étape 2 : Si pas de variables, on charge le fichier local (database.properties)
            System.out.println(">>> Aucune variable Railway trouvée. Chargement du fichier database.properties...");
            loadDatabaseProperties();
        }

        // Étape 3 : Chargement du Driver
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            // Fallback pour compatibilité
            try {
                Class.forName("com.mysql.jdbc.Driver");
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException("MySQL JDBC Driver not found", ex);
            }
        }
    }

    /**
     * Loads database configuration from database.properties file (Local only).
     */
    private static void loadDatabaseProperties() {
        try (InputStream input = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("database.properties")) {
            
            if (input == null) {
                // Si on est ici, c'est qu'on a ni variable d'environnement, ni fichier.
                throw new RuntimeException("CRITIQUE : Impossible de trouver database.properties et aucune variable ENV détectée !");
            }

            Properties properties = new Properties();
            properties.load(input);

            url = properties.getProperty("db.url");
            username = properties.getProperty("db.username");
            password = properties.getProperty("db.password");
            
            // On charge le driver depuis le fichier s'il est spécifié, sinon on garde celui par défaut
            String propDriver = properties.getProperty("db.driver");
            if (propDriver != null && !propDriver.isEmpty()) {
                driver = propDriver;
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error loading database properties", e);
        }
    }

    /**
     * Gets a new database connection.
     */
    public static Connection getConnection() throws SQLException {
        if (url == null) {
            throw new SQLException("Database URL is null. Configuration failed.");
        }
        return DriverManager.getConnection(url, username, password);
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Closes a database connection.
     */
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                System.err.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}