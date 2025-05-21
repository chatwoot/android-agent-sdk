# 💬 Chatwoot Android SDK

Android SDK for Chatwoot

## 📦 Installation

### Option 1: JitPack (Recommended)

Add the JitPack repository to your project's `settings.gradle` or `build.gradle`:

```kotlin
dependencyResolutionManagement {
    repositories {
        // ... other repositories
        maven(url = "https://jitpack.io")
    }
}
```

Add the dependency in your app's `build.gradle`:

```kotlin
dependencies {
    implementation("com.github.chatwoot:chatwoot-sdk:1.0.0")
}
```

### Option 2: Manual via `build.gradle`

If you're managing dependencies directly in your `build.gradle`:

```kotlin
allprojects {
    repositories {
        // ... other repositories
        maven(url = "https://jitpack.io")
    }
}

dependencies {
    implementation("com.github.chatwoot:chatwoot-sdk:1.0.0")
}
```

## 📸 Camera and Photo Library Permissions (Required)

To enable photo capture or image upload features in the chat interface, **you must add the following permissions to your app's `AndroidManifest.xml`:**

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

For Android 10 (API level 29) and above, you should also add:

```xml
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

## ⚙️ Configuration Parameters

| Parameter        | Type      | Required | Description                                 |
|-----------------|-----------|----------|---------------------------------------------|
| `accountId`     | `Int`     | ✅        | Unique ID for the Chatwoot account          |
| `apiHost`       | `String`  | ✅        | Chatwoot API host URL                       |
| `accessToken`   | `String`  | ✅        | Access token for authentication             |
| `pubsubToken`   | `String`  | ✅        | Token for real-time updates                 |
| `websocketUrl`  | `String`  | ✅        | WebSocket URL for real-time communication   |

## 🛠️ Example Usage

### Step 1: Set up the SDK

```kotlin
val config = ChatwootConfiguration(
    accountId = 1,
    apiHost = "https://your-chatwoot.com",
    accessToken = "YOUR_ACCESS_TOKEN",
    pubsubToken = "YOUR_PUBSUB_TOKEN",
    websocketUrl = "wss://your-chatwoot.com"
)

ChatwootSDK.setup(config)

```

### Step 2: Show the Chat Interface

```kotlin
// In your Activity or Fragment
val conversationId = 123 // Required: conversation ID to load
ChatwootSDK.loadChatUI(this, conversationId)
```

The `conversationId` is required to load the chat UI. Make sure you have a valid conversation ID before calling `loadChatUI`.

## 🔧 Development Setup

Before building or publishing, please run:

```bash
./gradlew wrapper --gradle-version 8.5
```

This will generate the required Gradle wrapper JAR file in the gradle/wrapper directory.