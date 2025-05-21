package com.chatwoot.sdk

import android.app.Activity
import android.content.Intent
import android.util.Log
import com.chatwoot.sdk.models.ChatwootConfiguration
import com.chatwoot.sdk.models.ChatwootProfile
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

object ChatwootSDK {
    private var configuration: ChatwootConfiguration? = null
    private var profile: ChatwootProfile? = null
    private val client = OkHttpClient()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val profileListeners = mutableListOf<(ChatwootProfile?) -> Unit>()

    fun setup(config: ChatwootConfiguration) {
        configuration = config
        fetchProfileData(config)
    }

    fun getCurrentConfig(): ChatwootConfiguration? = configuration

    private fun fetchProfileData(config: ChatwootConfiguration) {
        val request = Request.Builder()
            .url("${config.apiHost}/api/v1/profile")
            .addHeader("Accept", "application/json")
            .addHeader("api_access_token", config.accessToken)
            .build()

        scope.launch {
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        Log.e("ChatwootSDK", "Profile fetch failed: ${response.code}")
                        updateProfile(null)
                        return@use
                    }

                    response.body?.string()?.let { body ->
                        val json = JSONObject(body)
                        var displayName = "Chat User"

                        // Extract name using priority order
                        json.optString("name").takeIf { it.isNotEmpty() }?.let { displayName = it }
                        json.optString("available_name").takeIf { it.isNotEmpty() }?.let { displayName = it }
                        json.optString("display_name").takeIf { it.isNotEmpty() }?.let { displayName = it }

                        val avatarUrl = json.optString("avatar_url").takeIf { it.isNotEmpty() }
                        val newProfile = ChatwootProfile(displayName, avatarUrl)
                        
                        Log.d("ChatwootSDK", "Profile fetched: $newProfile")
                        updateProfile(newProfile)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatwootSDK", "Error fetching profile", e)
                updateProfile(null)
            }
        }
    }

    private fun updateProfile(newProfile: ChatwootProfile?) {
        profile = newProfile
        scope.launch(Dispatchers.Main) {
            profileListeners.forEach { it(newProfile) }
        }
    }

    fun getProfile(listener: (ChatwootProfile?) -> Unit) {
        // If we already have the profile, return it immediately
        profile?.let {
            listener(it)
            return
        }

        // Add listener to be notified when profile is fetched
        profileListeners.add(listener)
        
        // If we have configuration but no profile, try fetching again
        configuration?.let { config ->
            if (profile == null) {
                fetchProfileData(config)
            }
        }
    }

    fun loadChatUI(activity: Activity, conversationId: Int) {
        val config = configuration ?: run {
            Log.e("ChatwootSDK", "ChatwootSDK must be setup before use")
            return
        }

        val intent = Intent(activity, ChatwootActivity::class.java).apply {
            putExtra("config", config)
            putExtra("conversationId", conversationId)
        }
        activity.startActivity(intent)
    }
} 