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
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class ChatwootActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChatwootBinding
    private var profile: ChatwootProfile? = null
    private lateinit var config: ChatwootConfiguration
    private var conversationId: Int = 0
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

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

        setupNetworkMonitoring()
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
                val subtitleColor = if (isLightColor) Color.parseColor("#666666") else Color.parseColor("#CCCCCC")
                
                profileName.setTextColor(textColor)
                inboxName.setTextColor(subtitleColor)

                // Adjust back button tint
                backButton.drawable?.setTint(textColor)
                
                // Adjust network status icon tint
                networkStatusIcon.drawable?.setTint(textColor)
            }

            // Set up network status icons
            setupNetworkStatusIcons()

            // Set default profile name and inbox name
            profileName.text = "Chat User"
            inboxName.text = config.inboxName

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

    private fun setupNetworkStatusIcons() {
        // Update icon based on current connectivity if connectivityManager is initialized
        if (::connectivityManager.isInitialized) {
            updateNetworkStatusIcon(isNetworkAvailable())
        } else {
            // Default to connected state if we can't check yet
            updateNetworkStatusIcon(true)
        }
    }

    private fun setupNetworkMonitoring() {
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        // Update the network status icon with the actual connectivity state now that we can check
        updateNetworkStatusIcon(isNetworkAvailable())
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                runOnUiThread {
                    updateNetworkStatusIcon(true)
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    updateNetworkStatusIcon(false)
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        networkCallback?.let {
            connectivityManager.registerNetworkCallback(networkRequest, it)
        }
    }

    private fun updateNetworkStatusIcon(isConnected: Boolean) {
        val icon = if (isConnected) {
            config.customConnectedIcon?.let {
                ContextCompat.getDrawable(this, it)
            } ?: ContextCompat.getDrawable(this, R.drawable.ic_network_connected)
        } else {
            config.customDisconnectedIcon?.let {
                ContextCompat.getDrawable(this, it)
            } ?: ContextCompat.getDrawable(this, R.drawable.ic_network_disconnected)
        }
        
        binding.networkStatusIcon.setImageDrawable(icon)
        
        // Apply color tint if custom color is set
        config.customColor?.let { color ->
            val isLightColor = isColorLight(color)
            val textColor = if (isLightColor) Color.BLACK else Color.WHITE
            binding.networkStatusIcon.drawable?.setTint(textColor)
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroy() {
        super.onDestroy()
        networkCallback?.let {
            connectivityManager.unregisterNetworkCallback(it)
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