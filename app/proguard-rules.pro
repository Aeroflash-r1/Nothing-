# Keep Room entities and database
-keep class com.nothing.assistant.data.ChatMessage { *; }
-keep class com.nothing.assistant.data.ChatDatabase { *; }
-keep class com.nothing.assistant.data.ChatDao { *; }

# Keep Gemini Client models
-keep class com.nothing.assistant.data.GeminiClient { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Keep serializable/parcelable models
-keepclassmembers class * implements java.io.Serializable { *; }
