# Quick Fixes Guide - AI Terror Mod

## CRITICAL FIXES REQUIRED

### 1. Fix Blocking Firestore Operations (Game Freeze Issue)

**File:** `src/main/java/com/rjwebb134/aiterror/data/FirestoreManager.java`

**Problem:** `.get()` call blocks the game thread
```java
future.get(); // ← FREEZE
```

**Fix:** Use async callbacks
```java
future.addListener(
    () -> System.out.println("[AI Terror Mod] ✓ Saved data"),
    Executors.newFixedThreadPool(2)
);
```

---

### 2. Fix Memory Leak in AudioMemory (OutOfMemory Risk)

**File:** `src/main/java/com/rjwebb134/aiterror/AIManager.java` (Lines 241-265)

**Problem:** Unbounded HashMap growth
```java
private final Map<String, Integer> keywordScores = new HashMap<>(); // Never cleared
```

**Fix:** Implement LinkedHashMap with LRU eviction
```java
private final Map<String, Integer> keywordScores = new LinkedHashMap<String, Integer>() {
    private static final int MAX_KEYWORDS = 1000;
    @Override
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > MAX_KEYWORDS;
    }
};
```

---

### 3. Fix Unbounded Player Data Maps (OutOfMemory Risk)

**File:** `src/main/java/com/rjwebb134/aiterror/AIManager.java` (Lines 22-24)

**Problem:** Player maps never cleared when players disconnect

**Fix:** Add cleanup on player disconnect
```java
public void onPlayerDisconnect(UUID playerId) {
    playerPlans.remove(playerId);
    lastAnalyzedStates.remove(playerId);
    behaviorStreaks.remove(playerId);
    audioMemories.remove(playerId);
}
```

---

### 4. Secure Firestore Credentials (Security Risk)

**File:** `src/main/java/com/rjwebb134/aiterror/data/FirestoreManager.java`

**Problem:** Credential paths and errors printed to System.out

**Fix:** Remove sensitive logging
```java
// Bad - Exposes paths and errors
System.out.println("[AI Terror Mod] ✓ Firestore initialized successfully!");

// Good - Silent success
LOGGER.info("[AI Terror Mod] Firestore initialized");
```

---

### 5. Cache Regex Pattern (Performance)

**File:** `src/main/java/com/rjwebb134/aiterror/AIManager.java` (Line 253)

**Problem:** New regex compiled on every transcript

**Fix:** Use static Pattern
```java
private static final Pattern TOKEN_PATTERN = Pattern.compile("[^a-z0-9]+");

for (String token : TOKEN_PATTERN.split(normalized)) {
    // Use token
}
```

---

## Testing Checklist

- [ ] Run `gradle build` with Java 17
- [ ] Verify all tests pass
- [ ] Test player disconnect - verify no memory leak
- [ ] Test Firestore save - verify no game freeze
- [ ] Monitor memory usage over 1 hour with 10+ players
- [ ] Check logs - no credential paths exposed

---

**Priority Order:**
1. **Fix blocking Firestore calls** (CRITICAL - causes freeze)
2. **Fix memory leaks** (CRITICAL - OOM crash)
3. **Secure credentials** (CRITICAL - data breach)
4. **Cache regex pattern** (HIGH - CPU)
