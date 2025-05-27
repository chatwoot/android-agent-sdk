# Chatwoot SDK Example App

This is an example app that demonstrates how to use the Chatwoot Android Agent SDK.

## Features

- Configure the Chatwoot SDK with your account details
- Open the chat UI for a specific conversation

## Usage

1. Enter your Chatwoot account details:
   - Account ID
   - API Host (e.g., https://app.chatwoot.com)
   - Access Token
   - Pubsub Token
   - Websocket URL
   - Conversation ID

2. Press the "Open Chat" button to launch the chat UI

## Requirements

- Android API level 26 or higher

## How It Works

The app uses the `ChatwootSDK` to set up a configuration and load the chat UI. The main components are:

1. `ChatwootConfiguration`: Holds your account details
2. `ChatwootSDK.setup()`: Initializes the SDK with your configuration
3. `ChatwootSDK.loadChatUI()`: Opens the chat interface for a specific conversation

For more details, check the [Chatwoot SDK documentation](https://github.com/chatwoot/android-agent-sdk).
