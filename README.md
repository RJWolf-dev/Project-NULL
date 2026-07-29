# AI Terror Mod (Project-NULL)

An experimental AI has broken free from the lab and has made its way into YOUR Minecraft world. This AI will analyze everything it can with a single goal in mind: to scare you until you quit. This AI has the ability to interact with YOU and YOUR world. Remember! This is an experimental AI, and it is still in BETA. Please report any crashes, errors, breaks, anomalies, and anything that you feel is necessary to report. My team and I are committed to making sure that this mod for Minecraft Java edition is working correctly. More updates in the future!

## Firestore setup

Firestore support is installed through the Gradle dependencies in `build.gradle` and initialized when the mod starts.

Do **not** commit Firebase service account JSON files to this repository. Instead, provide credentials at runtime using one of these options:

1. JVM property:

   ```bash
   -Daiterror.firebase.credentials=/absolute/path/to/firebase-adminsdk.json
   ```

2. Environment variable:

   ```bash
   FIREBASE_SERVICE_ACCOUNT_PATH=/absolute/path/to/firebase-adminsdk.json
   ```

3. Google Application Default Credentials:

   ```bash
   GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/to/firebase-adminsdk.json
   ```

If none of those values is provided, the mod falls back to Google Application Default Credentials from the host environment.
