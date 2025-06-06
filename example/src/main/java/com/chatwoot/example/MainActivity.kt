package com.chatwoot.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.chatwoot.example.ui.theme.ChatwootExampleTheme
import com.chatwoot.sdk.ChatwootSDK
import com.chatwoot.sdk.models.ChatwootConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatwootExampleTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatwootConfigScreen(
                        onOpenChat = { config, conversationId ->
                            try {
                                // Setup the SDK with the configuration
                                ChatwootSDK.setup(config)
                                
                                // Load the chat UI with the given conversation ID
                                ChatwootSDK.loadChatUI(this, conversationId)
                            } catch (e: Exception) {
                                showToast("Error: ${e.message}")
                            }
                        }
                    )
                }
            }
        }
    }
    
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatwootConfigScreen(onOpenChat: (ChatwootConfiguration, Int) -> Unit) {
    var accountId by remember { mutableStateOf("1") }
    var host by remember { mutableStateOf("https://app.chatwoot.com") }
    var accessToken by remember { mutableStateOf("accessToken") }
    var pubsubToken by remember { mutableStateOf("pubsubToken") }
    var websocketUrl by remember { mutableStateOf("wss://app.chatwoot.com") }
    var conversationId by remember { mutableStateOf("1") }
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var selectedBackButton by remember { mutableStateOf<Int?>(null) }
    var selectedConnectedIcon by remember { mutableStateOf<Int?>(null) }
    var selectedDisconnectedIcon by remember { mutableStateOf<Int?>(null) }

    var accountIdError by remember { mutableStateOf(false) }
    var conversationIdError by remember { mutableStateOf(false) }

    val colorOptions = listOf(
        Color(0xFF2196F3), // Blue
        Color(0xFF4CAF50), // Green
        Color(0xFFFF9800), // Orange
        Color(0xFF9C27B0), // Purple
        Color(0xFFF44336), // Red
        Color(0xFF607D8B)  // Blue Grey
    )

    data class BackButtonOption(val name: String, val drawable: Int?)
    val backButtonOptions = listOf(
        BackButtonOption("Android Default", null),
        BackButtonOption("Custom Design", R.drawable.ic_back_custom)
    )
    
    data class NetworkIconOption(val name: String, val connectedIcon: Int?, val disconnectedIcon: Int?)
    val networkIconOptions = listOf(
        NetworkIconOption("SDK Default", null, null),
        NetworkIconOption("Custom Design", R.drawable.ic_cloud_connected, R.drawable.ic_cloud_disconnected)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "Chatwoot android SDK",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        
        OutlinedTextField(
            value = accountId,
            onValueChange = { 
                accountId = it
                accountIdError = false
            },
            label = { Text("Account ID") },
            isError = accountIdError,
            supportingText = { if (accountIdError) Text("Please enter a valid Account ID") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("API Host") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedTextField(
            value = conversationId,
            onValueChange = { 
                conversationId = it
                conversationIdError = false 
            },
            label = { Text("Conversation ID") },
            isError = conversationIdError,
            supportingText = { if (conversationIdError) Text("Please enter a valid Conversation ID") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Custom Color (Optional)",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Default option (no color)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .border(
                            width = 2.dp,
                            color = if (selectedColor == null) MaterialTheme.colorScheme.primary 
                                   else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .background(Color.Transparent, CircleShape)
                        .clickable { selectedColor = null },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "×",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Color options
                colorOptions.forEach { color ->
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .border(
                                width = if (selectedColor == color) 3.dp else 1.dp,
                                color = if (selectedColor == color) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .background(color, CircleShape)
                            .clickable { selectedColor = color }
                    )
                }
            }

            if (selectedColor != null) {
                Text(
                    text = "Status and navigation bars will use this color",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Back Button Style (Optional)",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                backButtonOptions.forEach { option ->
                    Card(
                        modifier = Modifier
                            .clickable { selectedBackButton = option.drawable }
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(
                                        if (selectedBackButton == option.drawable) 
                                            MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surface,
                                        CircleShape
                                    )
                                    .border(
                                        width = if (selectedBackButton == option.drawable) 2.dp else 1.dp,
                                        color = if (selectedBackButton == option.drawable) 
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.outline,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (option.drawable != null) {
                                    Icon(
                                        painter = painterResource(id = option.drawable),
                                        contentDescription = option.name,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (selectedBackButton == option.drawable) 
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                } else {
                                    Text(
                                        text = "←",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = if (selectedBackButton == option.drawable) 
                                            MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedBackButton == option.drawable) 
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Network Status Icons (Optional)",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                networkIconOptions.forEach { option ->
                    Card(
                        modifier = Modifier
                            .clickable { 
                                selectedConnectedIcon = option.connectedIcon
                                selectedDisconnectedIcon = option.disconnectedIcon
                            }
                            .weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Connected icon preview
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (selectedConnectedIcon == option.connectedIcon) 
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface,
                                            CircleShape
                                        )
                                        .border(
                                            width = if (selectedConnectedIcon == option.connectedIcon) 2.dp else 1.dp,
                                            color = if (selectedConnectedIcon == option.connectedIcon) 
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (option.connectedIcon != null) {
                                        Icon(
                                            painter = painterResource(id = option.connectedIcon),
                                            contentDescription = "Connected",
                                            modifier = Modifier.size(12.dp),
                                            tint = if (selectedConnectedIcon == option.connectedIcon) 
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = "📶",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selectedConnectedIcon == option.connectedIcon) 
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                                
                                // Disconnected icon preview
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(
                                            if (selectedDisconnectedIcon == option.disconnectedIcon) 
                                                MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surface,
                                            CircleShape
                                        )
                                        .border(
                                            width = if (selectedDisconnectedIcon == option.disconnectedIcon) 2.dp else 1.dp,
                                            color = if (selectedDisconnectedIcon == option.disconnectedIcon) 
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outline,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (option.disconnectedIcon != null) {
                                        Icon(
                                            painter = painterResource(id = option.disconnectedIcon),
                                            contentDescription = "Disconnected",
                                            modifier = Modifier.size(12.dp),
                                            tint = if (selectedDisconnectedIcon == option.disconnectedIcon) 
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    } else {
                                        Text(
                                            text = "📵",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (selectedDisconnectedIcon == option.disconnectedIcon) 
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = option.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (selectedConnectedIcon == option.connectedIcon) 
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                try {
                    val accountIdInt = accountId.toInt()
                    val conversationIdInt = conversationId.toInt()
                    
                    val config = ChatwootConfiguration(
                        accountId = accountIdInt,
                        apiHost = host,
                        accessToken = accessToken,
                        pubsubToken = pubsubToken,
                        websocketUrl = websocketUrl,
                        customColor = selectedColor?.toArgb(),
                        customBackButtonDrawable = selectedBackButton,
                        customConnectedIcon = selectedConnectedIcon,
                        customDisconnectedIcon = selectedDisconnectedIcon
                    )
                    
                    onOpenChat(config, conversationIdInt)
                } catch (e: NumberFormatException) {
                    if (accountId.isEmpty() || !accountId.all { it.isDigit() }) {
                        accountIdError = true
                    }
                    if (conversationId.isEmpty() || !conversationId.all { it.isDigit() }) {
                        conversationIdError = true
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Open Chat")
        }
    }
}