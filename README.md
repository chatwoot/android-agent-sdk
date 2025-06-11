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
    implementation("com.github.chatwoot:android-agent-sdk:1.0.0")
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
    implementation("com.github.chatwoot:android-agent-sdk:1.0.0")
}
```

## 📱 Required Permissions

Add the following permissions to your app's `AndroidManifest.xml`:

### Network Access (Required)
```xml
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.INTERNET" />
```

### Camera and Photo Library (Required for media features)
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

### Required Parameters

| Parameter        | Type      | Required | Description                                 |
|-----------------|-----------|----------|---------------------------------------------|
| `accountId`     | `Int`     | ✅        | Unique ID for the Chatwoot account          |
| `apiHost`       | `String`  | ✅        | Chatwoot API host URL                       |
| `accessToken`   | `String`  | ✅        | Access token for authentication             |
| `pubsubToken`   | `String`  | ✅        | Token for real-time updates                 |
| `websocketUrl`  | `String`  | ✅        | WebSocket URL for real-time communication   |

### Optional Customization Parameters

| Parameter                    | Type      | Description                                                 |
|-----------------------------|-----------|-------------------------------------------------------------|
| `customColor`               | `Int`     | Custom theme color for header and status bars (Color.rgb or Color.parseColor format) |
| `customBackButtonDrawable`  | `Int`     | Custom drawable resource ID for the back button            |
| `customConnectedIcon`       | `Int`     | Custom drawable resource ID for connected network status    |
| `customDisconnectedIcon`    | `Int`     | Custom drawable resource ID for disconnected network status |
| `inboxName`                 | `String`  | Custom inbox name displayed in the app bar                 |
| `inboxNameFontSize`         | `Float`   | Font size for the inbox name in SP (default: 16f)         |
| `disableEditor`             | `Boolean` | Disable the message editor (chat becomes read-only)        |
| `editorDisableUpload`       | `Boolean` | Disable file upload functionality in the editor            |

## 🛠️ Example Usage

### Basic Setup

```kotlin
val config = ChatwootConfiguration(
    accountId = 1,
    apiHost = "https://your-chatwoot.com",
    accessToken = "YOUR_ACCESS_TOKEN",
    pubsubToken = "YOUR_PUBSUB_TOKEN",
    websocketUrl = "wss://your-chatwoot.com"
)

ChatwootSDK.setup(config)

// Load chat interface
val conversationId = 123 // Required: conversation ID to load
ChatwootSDK.loadChatUI(this, conversationId)
```

### Advanced Setup with Customizations

```kotlin
val config = ChatwootConfiguration(
    // Required parameters
    accountId = 1,
    apiHost = "https://your-chatwoot.com",
    accessToken = "YOUR_ACCESS_TOKEN",
    pubsubToken = "YOUR_PUBSUB_TOKEN",
    websocketUrl = "wss://your-chatwoot.com",
    
    // Optional customizations
    customColor = Color.parseColor("#6366F1"), // Indigo theme
    customBackButtonDrawable = R.drawable.ic_custom_back,
    customConnectedIcon = R.drawable.ic_custom_online,
    customDisconnectedIcon = R.drawable.ic_custom_offline,
    inboxName = "Customer Support",
    inboxNameFontSize = 18f, // Larger font size
    disableEditor = false, // Allow messaging
    editorDisableUpload = false // Allow file uploads
)

ChatwootSDK.setup(config)
ChatwootSDK.loadChatUI(this, conversationId)
```

### Read-Only Chat Setup

```kotlin
val config = ChatwootConfiguration(
    accountId = 1,
    apiHost = "https://your-chatwoot.com",
    accessToken = "YOUR_ACCESS_TOKEN",
    pubsubToken = "YOUR_PUBSUB_TOKEN",
    websocketUrl = "wss://your-chatwoot.com",
    disableEditor = true // Makes chat read-only
)

ChatwootSDK.setup(config)
ChatwootSDK.loadChatUI(this, conversationId)
```

## 🎨 Customization Features

### Theme Colors
The SDK automatically adjusts text colors and system bar appearance based on your custom color's brightness. Light colors will use dark text, while dark colors will use light text.

### Custom Icons
You can provide custom drawable resources for:
- Back button in the header
- Network connection status indicators

### Flexible Editor Control
- **Disable Editor**: Make the chat read-only for viewing conversations
- **Disable Uploads**: Allow messaging but prevent file attachments

### Inbox Branding
Display a custom inbox name in the app bar instead of the default "Chat User" label.

## 📋 Requirements

The `conversationId` is required to load the chat UI. Make sure you have a valid conversation ID before calling `loadChatUI`.

## 🔧 Development Setup

Before building or publishing, please run:

```bash
./gradlew wrapper --gradle-version 8.5
```

This will generate the required Gradle wrapper JAR file in the gradle/wrapper directory.