# Google Firestore Integration Guide for AI Terror Mod

This guide will help you set up Google Cloud Firestore to store and retrieve AI learning data about players for the AI Terror Mod.

## Prerequisites

- Google Cloud account (free tier)
- Project-NULL cloned locally
- Java 17+
- Gradle already configured

## Step 1: Create a Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Sign in or create a free account
3. Create a new project:
   - Click the project dropdown at the top
   - Click "NEW PROJECT"
   - Name it: `AI-Terror-Mod` (or your preferred name)
   - Click "CREATE"

## Step 2: Enable Firestore

1. In the Cloud Console, go to **Firestore** (search for it)
2. Click **"Create Database"**
3. Choose:
   - **Location**: Select closest to you (e.g., `us-east1`)
   - **Mode**: Select **"Start in Native mode"** (recommended for real-time data)
4. Click **"Create Database"**

## Step 3: Set Up Authentication

### Option A: Service Account (Recommended for Server-Side)

1. Go to **IAM & Admin → Service Accounts**
2. Click **"Create Service Account"**
3. Name: `ai-terror-mod-server`
4. Click **"Create and Continue"**
5. Grant role: **Editor** (for development; restrict later)
6. Click **"Continue"** then **"Done"**
7. Click the service account you just created
8. Go to **"Keys"** tab
9. Click **"Add Key → Create New Key"**
10. Choose **JSON** format
11. Click **"Create"** - a JSON file will download
12. Save this file as `firebase-adminsdk.json` in your project root (add to `.gitignore`!)

### Option B: Application Default Credentials (For Local Development)

If using local development:
```bash
gcloud auth application-default login
```

## Step 4: Add Firebase Admin SDK to build.gradle

Add these dependencies to your `build.gradle` file:

```gradle
dependencies {
    // ... existing dependencies ...
    
    // Firebase Admin SDK
    implementation "com.google.firebase:firebase-admin:9.2.0"
    
    // Google Cloud Firestore
    implementation "com.google.cloud:google-cloud-firestore:3.15.0"
}
```

Then run: `gradle build`

## Step 5: Create Firestore Model Classes

Create `src/main/java/com/rjwebb134/aiterror/data/PlayerLearningData.java`:

```java
package com.rjwebb134.aiterror.data;

import java.util.HashMap;
import java.util.Map;

public class PlayerLearningData {
    private String playerId;
    private String playerName;
    private long lastUpdated;
    private Map<String, Object> behaviorPatterns;
    private Map<String, Float> fearFactors;
    private int scareAttempts;
    private float successRate;
    
    public PlayerLearningData() {
        this.behaviorPatterns = new HashMap<>();
        this.fearFactors = new HashMap<>();
        this.scareAttempts = 0;
        this.successRate = 0.0f;
    }
    
    // Constructor
    public PlayerLearningData(String playerId, String playerName) {
        this();
        this.playerId = playerId;
        this.playerName = playerName;
        this.lastUpdated = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getPlayerId() { return playerId; }
    public void setPlayerId(String playerId) { this.playerId = playerId; }
    
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public Map<String, Object> getBehaviorPatterns() { return behaviorPatterns; }
    public void setBehaviorPatterns(Map<String, Object> patterns) { 
        this.behaviorPatterns = patterns; 
    }
    
    public Map<String, Float> getFearFactors() { return fearFactors; }
    public void setFearFactors(Map<String, Float> factors) { 
        this.fearFactors = factors; 
    }
    
    public int getScareAttempts() { return scareAttempts; }
    public void setScareAttempts(int attempts) { this.scareAttempts = attempts; }
    
    public float getSuccessRate() { return successRate; }
    public void setSuccessRate(float rate) { this.successRate = rate; }
    
    // Update methods
    public void recordScareAttempt(boolean successful) {
        this.scareAttempts++;
        if (successful) {
            this.successRate = (this.successRate * (this.scareAttempts - 1) + 1) / this.scareAttempts;
        } else {
            this.successRate = (this.successRate * (this.scareAttempts - 1)) / this.scareAttempts;
        }
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void updateBehaviorPattern(String pattern, Object value) {
        this.behaviorPatterns.put(pattern, value);
        this.lastUpdated = System.currentTimeMillis();
    }
    
    public void updateFearFactor(String factor, float value) {
        this.fearFactors.put(factor, value);
        this.lastUpdated = System.currentTimeMillis();
    }
}
```

## Step 6: Create Firestore Manager Class

Create `src/main/java/com/rjwebb134/aiterror/data/FirestoreManager.java`:

```java
package com.rjwebb134.aiterror.data;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.cloud.FirestoreClient;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.concurrent.ExecutionException;

public class FirestoreManager {
    private static Firestore db;
    private static final String COLLECTION_NAME = "player_learning_data";
    
    /**
     * Initialize Firebase Admin SDK
     * Make sure firebase-adminsdk.json is in your project root
     */
    public static void initialize() {
        try {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp();
            }
            db = FirestoreClient.getFirestore();
            System.out.println("[AI Terror Mod] Firestore initialized successfully!");
        } catch (Exception e) {
            System.err.println("[AI Terror Mod] Failed to initialize Firestore: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Save or update player learning data
     */
    public static void savePlayerData(PlayerLearningData data) {
        if (db == null) {
            System.err.println("[AI Terror Mod] Firestore not initialized!");
            return;
        }
        
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME)
                .document(data.getPlayerId())
                .set(data);
        
        try {
            future.get(); // Wait for write to complete
            System.out.println("[AI Terror Mod] Saved data for player: " + data.getPlayerName());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[AI Terror Mod] Error saving player data: " + e.getMessage());
        }
    }
    
    /**
     * Retrieve player learning data
     */
    public static PlayerLearningData getPlayerData(String playerId) {
        if (db == null) {
            System.err.println("[AI Terror Mod] Firestore not initialized!");
            return null;
        }
        
        ApiFuture<DocumentSnapshot> future = db.collection(COLLECTION_NAME)
                .document(playerId)
                .get();
        
        try {
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                return document.toObject(PlayerLearningData.class);
            } else {
                System.out.println("[AI Terror Mod] No data found for player: " + playerId);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[AI Terror Mod] Error retrieving player data: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get or create player data
     */
    public static PlayerLearningData getOrCreatePlayerData(ServerPlayerEntity player) {
        String playerId = player.getUuidAsString();
        PlayerLearningData data = getPlayerData(playerId);
        
        if (data == null) {
            data = new PlayerLearningData(playerId, player.getName().getString());
            savePlayerData(data);
            System.out.println("[AI Terror Mod] Created new learning profile for: " + player.getName().getString());
        }
        
        return data;
    }
    
    /**
     * Delete player data (GDPR compliance, etc.)
     */
    public static void deletePlayerData(String playerId) {
        if (db == null) {
            System.err.println("[AI Terror Mod] Firestore not initialized!");
            return;
        }
        
        ApiFuture<WriteResult> future = db.collection(COLLECTION_NAME)
                .document(playerId)
                .delete();
        
        try {
            future.get();
            System.out.println("[AI Terror Mod] Deleted data for player: " + playerId);
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("[AI Terror Mod] Error deleting player data: " + e.getMessage());
        }
    }
    
    /**
     * Close Firestore connection (call on server shutdown)
     */
    public static void shutdown() {
        if (db != null) {
            try {
                db.close();
                System.out.println("[AI Terror Mod] Firestore connection closed");
            } catch (Exception e) {
                System.err.println("[AI Terror Mod] Error closing Firestore: " + e.getMessage());
            }
        }
    }
}
```

## Step 7: Initialize Firestore in Your Main Mod Class

In your main mod initializer (likely `ModInit.java` or similar):

```java
@Mod.EventBusSubscriber(modid = "aiterror", bus = Mod.EventBusSubscriber.Bus.MOD)
public static class ModEvents {
    
    @SubscribeEvent
    public static void onServerStart(ServerStartingEvent event) {
        // Initialize Firestore when server starts
        FirestoreManager.initialize();
        System.out.println("[AI Terror Mod] Server starting - Firestore ready!");
    }
    
    @SubscribeEvent
    public static void onServerStop(ServerStoppingEvent event) {
        // Clean up Firestore connection
        FirestoreManager.shutdown();
        System.out.println("[AI Terror Mod] Server stopping - Firestore cleaned up!");
    }
}
```

## Step 8: Use Firestore in Your AI Logic

Example in your AI behavior class:

```java
public class AITerrorBehavior {
    
    public void analyzePlayerBehavior(ServerPlayerEntity player) {
        // Get player's learning data
        PlayerLearningData learningData = FirestoreManager.getOrCreatePlayerData(player);
        
        // Update behavior patterns based on current observation
        learningData.updateBehaviorPattern("lastPosition", player.getPos());
        learningData.updateBehaviorPattern("health", player.getHealth());
        learningData.updateBehaviorPattern("timeOfDay", player.getWorld().getTimeOfDay());
        
        // Save back to Firestore
        FirestoreManager.savePlayerData(learningData);
    }
    
    public void recordScareAttempt(ServerPlayerEntity player, boolean successful) {
        PlayerLearningData data = FirestoreManager.getOrCreatePlayerData(player);
        data.recordScareAttempt(successful);
        
        // Update fear factors based on success
        if (successful) {
            float currentFear = data.getFearFactors().getOrDefault("jumpScare", 0.5f);
            data.updateFearFactor("jumpScare", currentFear + 0.1f);
        }
        
        FirestoreManager.savePlayerData(data);
    }
}
```

## Firestore Free Tier Limits

**Always Free Each Month:**
- ✅ 1 GiB storage
- ✅ 50,000 read operations
- ✅ 20,000 write operations
- ✅ 20,000 delete operations

**For a typical small player base:**
- 10 players with daily updates = ~300 writes/month (well under limit)
- Querying player data = ~1,000 reads/month (well under limit)

## Firestore Database Structure

Your data will be organized like this:

```
Firestore
└── Collections: player_learning_data
    ├── Document: player_uuid_1
    │   ├── playerId: "12345678-1234-1234-1234-123456789012"
    │   ├── playerName: "PlayerName"
    │   ├── lastUpdated: 1721800000000
    │   ├── scareAttempts: 23
    │   ├── successRate: 0.65
    │   ├── behaviorPatterns: {
    │   │   └── lastPosition: {x, y, z}
    │   ├── fearFactors: {
    │   │   └── jumpScare: 0.75
    │   │   └── soundScares: 0.55
    │   │   └── environmentalFears: 0.82
    │   └── ...
    └── Document: player_uuid_2
        └── ...
```

## Security Rules (Optional but Recommended)

Go to **Firestore → Rules** and replace with:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if false;  // Only allow via authenticated server
    }
  }
}
```

Since you're using a service account, it has full access via your credentials file.

## Troubleshooting

| Issue | Solution |
|-------|----------|
| `firebase-adminsdk.json not found` | Ensure JSON file is in project root and added to `.gitignore` |
| `FirebaseApp already initialized` | Check if you're calling `initialize()` multiple times |
| `Firestore not initialized` | Ensure `FirestoreManager.initialize()` is called during server startup |
| `Cloud Firestore API not enabled` | Go to APIs & Services in Cloud Console and enable "Cloud Firestore API" |

## Next Steps

1. Integrate with your TensorFlow model to train on stored behavior patterns
2. Add analytics queries to find common player behaviors
3. Use Firebase Cloud Functions (optional) for backend processing
4. Expand to store scare effectiveness metrics

Good luck with your AI Terror Mod! 🎮👻
