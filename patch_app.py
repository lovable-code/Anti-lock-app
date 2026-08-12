with open('app/src/main/java/com/example/SentinelApplication.kt', 'r') as f:
    text = f.read()

old_firebase_init = """        try {
            FirebaseApp.initializeApp(this)
            Log.d("SentinelApplication", "FirebaseApp initialized successfully.")
        } catch (e: Exception) {
            Log.e("SentinelApplication", "Failed to initialize FirebaseApp", e)
        }"""

new_firebase_init = """        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                val options = com.google.firebase.FirebaseOptions.Builder()
                    .setApplicationId("1:123456789012:android:1234567890123456")
                    .setApiKey("AIzaSyDummyKeyForSentinelXMDMTesting")
                    .setProjectId("sentinelx-mdm")
                    .build()
                FirebaseApp.initializeApp(this, options)
                Log.d("SentinelApplication", "FirebaseApp initialized with fallback options.")
            } else {
                Log.d("SentinelApplication", "FirebaseApp already initialized.")
            }
        } catch (e: Exception) {
            Log.e("SentinelApplication", "Failed to initialize FirebaseApp", e)
        }"""

text = text.replace(old_firebase_init, new_firebase_init)

with open('app/src/main/java/com/example/SentinelApplication.kt', 'w') as f:
    f.write(text)
