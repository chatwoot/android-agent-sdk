package com.chatwoot.sdk.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ChatwootConfiguration(
    val accountId: Int,
    val apiHost: String,
    val accessToken: String,
    val pubsubToken: String,
    val websocketUrl: String,
    val customColor: Int? = null,
    val customBackButtonDrawable: Int? = null,
    val customConnectedIcon: Int? = null,
    val customDisconnectedIcon: Int? = null
) : Parcelable

data class ChatwootProfile(
    val name: String,
    val avatarUrl: String? = null
)