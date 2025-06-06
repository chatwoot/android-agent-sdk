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
import android.graphics.Rect
import android.view.ViewTreeObserver
import androidx.core.content.ContextCompat

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

        // Get configuration and conversation ID first
        config = intent.getParcelableExtra("config")
            ?: throw IllegalStateException("ChatwootConfiguration is required")

        conversationId = intent.getIntExtra("conversationId", 0)
        if (conversationId == 0) {
            throw IllegalStateException("Conversation ID is required")
        }

        // Configure window to handle system bars (now that config is available)
        setupSystemBars()

        binding = ActivityChatwootBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Add global layout listener to scroll WebView when keyboard is shown
        binding.root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            binding.root.getWindowVisibleDisplayFrame(rect)
            val screenHeight = binding.root.rootView.height
            val keypadHeight = screenHeight - rect.bottom
            // If keyboard is open
            if (keypadHeight > screenHeight * 0.15) {
                // Scroll WebView to bottom
                binding.webView.post {
                    binding.webView.evaluateJavascript(
                        "(function() { window.scrollTo(0, document.body.scrollHeight); })();",
                        null
                    )
                }
            }
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
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Set status bar and navigation bar colors
        val customColor = config.customColor
        if (customColor != null) {
            window.statusBarColor = customColor
            window.navigationBarColor = customColor

            // Adjust system bar icon colors based on custom color brightness
            val isLightColor = isColorLight(customColor)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = isLightColor
                isAppearanceLightNavigationBars = isLightColor
            }
        } else {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT

            // Make system bar icons dark for transparent bars
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
                isAppearanceLightNavigationBars = true
            }
        }
    }

    private fun setupHeader() {
        binding.apply {
            backButton.setOnClickListener { finish() }

            // Set custom back button drawable if provided, otherwise keep Android default
            config.customBackButtonDrawable?.let { customDrawable ->
                val backButtonDrawable = ContextCompat.getDrawable(this@ChatwootActivity, customDrawable)
                backButton.setImageDrawable(backButtonDrawable)
            }

            // Apply custom color to header if provided
            config.customColor?.let { color ->
                statusBarSpace.setBackgroundColor(color)
                toolbar.setBackgroundColor(color)
                navigationBarSpace.setBackgroundColor(color)

                // Adjust text color based on background brightness
                val isLightColor = isColorLight(color)
                val textColor = if (isLightColor) Color.BLACK else Color.WHITE
                profileName.setTextColor(textColor)

                // Adjust back button tint
                backButton.drawable?.setTint(textColor)
            }

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

    private fun isColorLight(color: Int): Boolean {
        val red = Color.red(color)
        val green = Color.green(color)
        val blue = Color.blue(color)

        // Calculate luminance using the standard formula
        val luminance = (0.299 * red + 0.587 * green + 0.114 * blue) / 255
        return luminance > 0.5
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