package com.chatwoot.sdk

import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.ViewCompat
import android.graphics.Color
import coil.load
import coil.transform.CircleCropTransformation
import com.chatwoot.sdk.models.ChatwootConfiguration
import com.chatwoot.sdk.models.ChatwootProfile
import com.chatwoot.sdk.databinding.ActivityChatwootBinding
import com.chatwoot.sdk.utils.TextDrawable

class ChatwootActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatwootBinding
    private var profile: ChatwootProfile? = null
    private lateinit var config: ChatwootConfiguration
    private var conversationId: Int = 0

    private inner class WebAppInterface {
        @JavascriptInterface
        fun closeChat() {
            Log.d("ChatwootSDK", "closeChat called from JavaScript")
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configure window to handle system bars
        setupSystemBars()
        
        binding = ActivityChatwootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get configuration and conversation ID
        config = intent.getParcelableExtra("config")
            ?: throw IllegalStateException("ChatwootConfiguration is required")
        
        conversationId = intent.getIntExtra("conversationId", 0)
        if (conversationId == 0) {
            throw IllegalStateException("Conversation ID is required")
        }

        setupHeader()
        setupWebView()
        injectConfiguration()
        
        // Set system bar spaces
        setupSystemBarSpaces()
    }

    private fun setupSystemBarSpaces() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            
            // Apply status bar height
            binding.statusBarSpace.layoutParams.height = insets.top
            binding.statusBarSpace.requestLayout()
            
            // Apply navigation bar height
            binding.navigationBarSpace.layoutParams.height = insets.bottom
            binding.navigationBarSpace.requestLayout()
            
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupSystemBars() {
        // Make content draw under system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Set status bar and navigation bar colors
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        
        // Make system bar icons dark
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
    }

    private fun setupHeader() {
        binding.apply {
            backButton.setOnClickListener { finish() }
            
            // Set default profile name
            profileName.text = "Chat User"
            
            // Update profile when available
            ChatwootSDK.getProfile { newProfile ->
                runOnUiThread {
                    updateProfile(newProfile)
                }
            }
        }
    }

    private fun updateProfile(profile: ChatwootProfile?) {
        profile?.let {
            binding.profileName.text = it.name
            
            // Load avatar if available
            it.avatarUrl?.let { url ->
                binding.avatarImage.load(url) {
                    transformations(CircleCropTransformation())
                }
            } ?: run {
                // Show initials avatar
                binding.avatarImage.setImageDrawable(
                    TextDrawable.create(getInitials(it.name))
                )
            }
        }
    }

    private fun getInitials(name: String): String {
        return name.split(" ")
            .take(2)
            .mapNotNull { it.firstOrNull()?.toString() }
            .joinToString("")
            .uppercase()
    }

    private fun setupWebView() {
        binding.webView.apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                javaScriptCanOpenWindowsAutomatically = true
            }
            
            addJavascriptInterface(WebAppInterface(), "AndroidInterface")
            
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = false

                override fun onPageFinished(view: WebView, url: String) {
                    super.onPageFinished(view, url)
                    injectConfiguration()
                }
            }

            loadUrl("file:///android_asset/index.html")
        }
    }

    private fun injectConfiguration() {
        val script = """
            window.__WOOT_ISOLATED_SHELL__ = true;
            window.__WOOT_ACCOUNT_ID__ = ${config.accountId};
            window.__WOOT_API_HOST__ = '${config.apiHost}';
            window.__WOOT_ACCESS_TOKEN__ = '${config.accessToken}';
            window.__PUBSUB_TOKEN__ = '${config.pubsubToken}';
            window.__WEBSOCKET_URL__ = '${config.websocketUrl}';
            window.__WOOT_CONVERSATION_ID__ = $conversationId;
            
            console.log('Injecting config:', {
                accountId: window.__WOOT_ACCOUNT_ID__,
                apiHost: window.__WOOT_API_HOST__,
                accessToken: window.__WOOT_ACCESS_TOKEN__,
                pubsubToken: window.__PUBSUB_TOKEN__,
                websocketUrl: window.__WEBSOCKET_URL__,
                conversationId: window.__WOOT_CONVERSATION_ID__
            });
            
            // Dispatch configuration loaded event
            document.dispatchEvent(
                new CustomEvent('chatwootConfigLoaded', { 
                    detail: {
                        accountId: ${config.accountId},
                        apiHost: '${config.apiHost}',
                        accessToken: '${config.accessToken}',
                        pubsubToken: '${config.pubsubToken}',
                        websocketUrl: '${config.websocketUrl}',
                        conversationId: $conversationId
                    }
                })
            );
        """.trimIndent()

        binding.webView.evaluateJavascript(script) { result ->
            Log.d("ChatwootSDK", "Configuration injection result: $result")
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
} 