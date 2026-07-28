package com.rjwebb134.aiterror.data;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import net.minecraft.entity.player.PlayerEntity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * Manages all Firestore operations for player learning data
 * Handles initialization, CRUD operations, and data persistence
 */
public class FirestoreManager {
    private static Firestore db;
    private static final String COLLECTION_NAME = "player_learning_data";
    private static final String SERVICE_ACCOUNT_PROPERTY = "aiterror.firebase.credentials";
    private static final String SERVICE_ACCOUNT_ENV = "FIREBASE_SERVICE_ACCOUNT_PATH";
    private static final String GOOGLE_APPLICATION_CREDENTIALS_ENV = "GOOGLE_APPLICATION_CREDENTIALS";
    private static boolean initialized = false;
    
    /**
     * Initialize Firebase Admin SDK and Firestore connection.
     *
     * <p>Credentials are resolved in this order:</p>
     * <ol>
     *     <li>JVM property {@code -Daiterror.firebase.credentials=/path/to/service-account.json}</li>
     *     <li>Environment variable {@code FIREBASE_SERVICE_ACCOUNT_PATH}</li>
     *     <li>Environment variable {@code GOOGLE_APPLICATION_CREDENTIALS}</li>
     *     <li>Google Application Default Credentials</li>
     * </ol>
     */
    public static void initialize() {
        try {
            if (initialized) {
                System.out.println("[AI Terror Mod] Firestore already initialized");
                return;
            }
            
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(buildFirebaseOptions());
            }
            
            db = FirestoreClient.getFirestore();
            initialized = true;
            System.out.println("[AI Terror Mod] ✓ Firestore initialized successfully!");
            System.out.println("[AI Terror Mod] Collection: " + COLLECTION_NAME);
            
        } catch (Exception e) {
            System.err.println("[AI Terror Mod] ✗ Failed to initialize Firestore: " + e.getMessage());
            System.err.println("[AI Terror Mod] Configure credentials with -D" + SERVICE_ACCOUNT_PROPERTY
                    + "=/path/to/service-account.json, " + SERVICE_ACCOUNT_ENV + ", or "
                    + GOOGLE_APPLICATION_CREDENTIALS_ENV + ".");
            e.printStackTrace();
        }
    }

    private static FirebaseOptions buildFirebaseOptions() throws IOException {
        String credentialsPath = firstNonBlank(
                System.getProperty(SERVICE_ACCOUNT_PROPERTY),
                System.getenv(SERVICE_ACCOUNT_ENV),
                System.getenv(GOOGLE_APPLICATION_CREDENTIALS_ENV)
        );

        GoogleCredentials credentials;
        if (credentialsPath != null) {
            Path path = Path.of(credentialsPath);
            if (!Files.isRegularFile(path)) {
                throw new IOException("Firebase service account file does not exist: " + path.toAbsolutePath());
            }

            try (InputStream serviceAccount = new FileInputStream(path.toFile())) {
                credentials = GoogleCredentials.fromStream(serviceAccount);
            }
        } else {
            credentials = GoogleCredentials.getApplicationDefault();
        }

        return FirebaseOptions.builder()
                .setCredentials(credentials)
                .build();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
    
    /**
     * Check if Firestore is initialized
     */
    public static boolean isInitialized() {
        return initialized && db != null;
    }
    
    /**
     * Save or update player learning data to Firestore
     */
    public static void savePlayerData(PlayerLearningData data) {
        if (!isInitialized()) {
            System.err.println("[AI Terror Mod] ✗ Firestore not initialized! Cannot save player data");
            return;
        }
        
        try {
            ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME)
                    .document(data.getPlayerId())
                    .set(data);
            
            future.get(); // Wait for write to complete
            System.out.println("[AI Terror Mod] ✓ Saved data for player: " + data.getPlayerName());
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[AI Terror Mod] ✗ Error saving player data: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Retrieve player learning data from Firestore
     * Returns null if player data doesn't exist
     */
    public static PlayerLearningData getPlayerData(String playerId) {
        if (!isInitialized()) {
            System.err.println("[AI Terror Mod] ✗ Firestore not initialized! Cannot retrieve player data");
            return null;
        }
        
        try {
            ApiFuture<DocumentSnapshot> future = db.collection(COLLECTION_NAME)
                    .document(playerId)
                    .get();
            
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                return document.toObject(PlayerLearningData.class);
            } else {
                return null;
            }
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[AI Terror Mod] ✗ Error retrieving player data: " + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    /**
     * Get existing player data or create new profile if doesn't exist
     * This is the primary method for getting player data
     */
    public static PlayerLearningData getOrCreatePlayerData(PlayerEntity player) {
        if (!isInitialized()) {
            System.err.println("[AI Terror Mod] ✗ Firestore not initialized! Cannot get/create player data");
            return null;
        }
        
        String playerId = player.getUuidAsString();
        PlayerLearningData data = getPlayerData(playerId);
        
        if (data == null) {
            // Create new learning profile for this player
            data = new PlayerLearningData(playerId, player.getName().getString());
            savePlayerData(data);
            System.out.println("[AI Terror Mod] ✓ Created new learning profile for: " + player.getName().getString());
        }
        
        return data;
    }
    
    /**
     * Delete player data from Firestore (GDPR compliance, etc.)
     */
    public static void deletePlayerData(String playerId) {
        if (!isInitialized()) {
            System.err.println("[AI Terror Mod] ✗ Firestore not initialized! Cannot delete player data");
            return;
        }
        
        try {
            ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME)
                    .document(playerId)
                    .delete();
            
            future.get();
            System.out.println("[AI Terror Mod] ✓ Deleted data for player: " + playerId);
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[AI Terror Mod] ✗ Error deleting player data: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Get count of all players in the database
     */
    public static int getPlayerCount() {
        if (!isInitialized()) {
            return 0;
        }
        
        try {
            ApiFuture<AggregateQuerySnapshot> future = db.collection(COLLECTION_NAME)
                    .count()
                    .get();
            
            AggregateQuerySnapshot snapshot = future.get();
            return (int) snapshot.getCount();
            
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[AI Terror Mod] ✗ Error getting player count: " + e.getMessage());
            Thread.currentThread().interrupt();
            return -1;
        }
    }
    
    /**
     * Close Firestore connection (call during server shutdown)
     * This ensures all pending writes are flushed
     */
    public static void shutdown() {
        if (db != null) {
            try {
                db.close();
                initialized = false;
                System.out.println("[AI Terror Mod] ✓ Firestore connection closed cleanly");
            } catch (Exception e) {
                System.err.println("[AI Terror Mod] ✗ Error closing Firestore: " + e.getMessage());
            }
        }
    }
    
    /**
     * Get Firestore instance (advanced usage)
     */
    public static Firestore getInstance() {
        return db;
    }
}
