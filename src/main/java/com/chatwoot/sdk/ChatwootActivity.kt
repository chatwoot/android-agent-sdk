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
    private lateinit var config: ChatwootConfiguration
    private var conversationId: Int = 0
    private lateinit var connectivityManager: ConnectivityManager
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var isWebViewReady = false
    private var lastNetworkState: Boolean? = null
    private var networkStateRunnable: Runnable? = null

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

        // Set status bar color
        val customColor = config.customColor
        if (customColor != null) {
            window.statusBarColor = customColor

            // Adjust status bar icon color based on custom color brightness
            val isLightColor = isColorLight(customColor)
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = isLightColor
            }
        } else {
            window.statusBarColor = Color.TRANSPARENT

            // Make status bar icons dark for transparent bar
            WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = true
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

                // Adjust text color based on background brightness
                val isLightColor = isColorLight(color)
                val textColor = if (isLightColor) Color.BLACK else Color.WHITE
                
                inboxName.setTextColor(textColor)

                // Adjust back button tint
                backButton.drawable?.setTint(textColor)
                
                // Adjust network status icon tint
                networkStatusIcon.drawable?.setTint(textColor)
            }

            // Set up network status icons - will be updated by network monitoring
            setupNetworkStatusIcons()

            // Set inbox name
            inboxName.text = config.inboxName
        }
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
                    isWebViewReady = true
                    injectConfiguration()
                }
            }

            loadUrl("file:///android_asset/index.html")
        }
    }

    private fun injectConfiguration() {
        val isOnline = isNetworkAvailable()
        val shouldDisableEditor = config.disableEditor || !isOnline
        val script = """
            console.log('Chatwoot SDK configuration loaded');

            // Set all possible variable combinations
            window.__WOOT_ISOLATED_SHELL__ = true;
            window.__WOOT_ACCOUNT_ID__ = ${config.accountId};
            window.__WOOT_API_HOST__ = '${config.apiHost}';
            window.__WOOT_ACCESS_TOKEN__ = '${config.accessToken}';
            window.__PUBSUB_TOKEN__ = '${config.pubsubToken}';
            window.__WEBSOCKET_URL__ = '${config.websocketUrl}';
            window.__WOOT_CONVERSATION_ID__ = $conversationId;
            window.__WOOT_DISABLE_EDITOR__ = $shouldDisableEditor;
            window.__WOOT_EDITOR_DISABLE_UPLOAD__ = ${config.editorDisableUpload};
            window.__WOOT_IS_ONLINE__ = $isOnline;
            window.__DISABLE_EDITOR__ = $shouldDisableEditor;
            window.__EDITOR_DISABLE_UPLOAD__ = ${config.editorDisableUpload};
            window.disableEditor = $shouldDisableEditor;
            window.editorDisableUpload = ${config.editorDisableUpload};

            // Dispatch configuration loaded event
            document.dispatchEvent(
                new CustomEvent('chatwootConfigLoaded', {
                    detail: {
                        accountId: ${config.accountId},
                        apiHost: '${config.apiHost}',
                        accessToken: '${config.accessToken}',
                        pubsubToken: '${config.pubsubToken}',
                        websocketUrl: '${config.websocketUrl}',
                        conversationId: $conversationId,
                        disableEditor: $shouldDisableEditor,
                        editorDisableUpload: ${config.editorDisableUpload},
                        isOnline: $isOnline
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
        
        // Update the network status icon with the actual connectivity state
        updateNetworkStatusIcon(isNetworkAvailable())
        
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                val hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
                
                // Consider online if we have internet capability and connected via WiFi or cellular
                val isOnline = hasInternet && (hasWifi || hasCellular)
                
                runOnUiThread {
                    updateNetworkStatusIcon(isOnline)
                    if (isWebViewReady) {
                        updateNetworkStatus(isOnline)
                    }
                }
            }

            override fun onLost(network: Network) {
                runOnUiThread {
                    updateNetworkStatusIcon(false)
                    if (isWebViewReady) {
                        updateNetworkStatus(false)
                    }
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
        
        val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
        val hasCellular = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
        
        return hasInternet && (hasWifi || hasCellular)
    }

    private fun updateNetworkStatus(isOnline: Boolean) {
        // Only update if state actually changed
        if (lastNetworkState == isOnline) {
            return
        }
        
        Log.d("ChatwootSDK", "Network status changed: isOnline=$isOnline")
        
        // Cancel any pending network state updates
        networkStateRunnable?.let {
            binding.webView.removeCallbacks(it)
        }
        
        // Debounce rapid network changes - wait 1 second before applying
        networkStateRunnable = Runnable {
            lastNetworkState = isOnline
            updateEditorStateOnly(isOnline)
        }
        
        binding.webView.postDelayed(networkStateRunnable!!, 1000)
    }

    private fun updateEditorStateOnly(isOnline: Boolean) {
        val shouldDisableEditor = config.disableEditor || !isOnline
        val script = """
            console.log('Network status update:', $isOnline);

            // Re-dispatch the chatwootConfigLoaded event with updated editor state
            document.dispatchEvent(
                new CustomEvent('chatwootConfigLoaded', {
                    detail: {
                        accountId: ${config.accountId},
                        apiHost: '${config.apiHost}',
                        accessToken: '${config.accessToken}',
                        pubsubToken: '${config.pubsubToken}',
                        websocketUrl: '${config.websocketUrl}',
                        conversationId: $conversationId,
                        disableEditor: $shouldDisableEditor,
                        editorDisableUpload: ${config.editorDisableUpload},
                        isOnline: $isOnline
                    }
                })
            );

            // Update editor functionality and button styling when network state changes
            setTimeout(() => {
                // Handle input elements
                const inputs = document.querySelectorAll('input, textarea, [contenteditable]');
                inputs.forEach(input => {
                    if ($shouldDisableEditor) {
                        if (!input.getAttribute('data-offline-disabled')) {
                            if (input.contentEditable !== undefined) {
                                input.contentEditable = 'false';
                            } else {
                                input.disabled = true;
                            }
                            input.style.pointerEvents = 'none';
                            input.setAttribute('data-offline-disabled', 'true');
                        }
                    } else {
                        if (input.getAttribute('data-offline-disabled')) {
                            if (input.contentEditable !== undefined) {
                                input.contentEditable = 'true';
                            } else {
                                input.disabled = false;
                            }
                            input.style.pointerEvents = '';
                            input.removeAttribute('data-offline-disabled');
                        }
                    }
                });
                
                // Handle buttons
                const buttons = document.querySelectorAll('button');
                buttons.forEach(button => {
                    if ($shouldDisableEditor) {
                        if (!button.getAttribute('data-offline-disabled')) {
                            button.setAttribute('data-original-filter', button.style.filter || '');
                            button.setAttribute('data-original-opacity', button.style.opacity || '');
                            button.setAttribute('data-original-disabled', button.disabled.toString());
                            
                            button.disabled = true;
                            button.style.filter = 'grayscale(1)';
                            button.style.opacity = '0.6';
                            button.style.pointerEvents = 'none';
                            button.setAttribute('data-offline-disabled', 'true');
                        }
                    } else {
                        if (button.getAttribute('data-offline-disabled')) {
                            const originalFilter = button.getAttribute('data-original-filter');
                            const originalOpacity = button.getAttribute('data-original-opacity');
                            const originalDisabled = button.getAttribute('data-original-disabled') === 'true';
                            
                            button.style.filter = originalFilter || '';
                            button.style.opacity = originalOpacity || '';
                            button.style.pointerEvents = '';
                            button.disabled = originalDisabled;
                            
                            button.removeAttribute('data-original-filter');
                            button.removeAttribute('data-original-opacity');
                            button.removeAttribute('data-original-disabled');
                            button.removeAttribute('data-offline-disabled');
                        }
                    }
                });
            }, 100);
        """.trimIndent()

        binding.webView.evaluateJavascript(script) { result ->
            Log.d("ChatwootSDK", "Network status update result: $result")
        }
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