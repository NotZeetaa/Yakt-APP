package com.notzeetaa.yakt

import kotlinx.serialization.Serializable

@Serializable
data class RemoteVersionResponse(
    val version: String,
    val updated: String,
    val changelog: List<String>,
    val scriptsList: List<ScriptEntry>
)

@Serializable
data class ScriptEntry(
    val name: String,
    val sha256: String
)
