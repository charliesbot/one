package com.charliesbot.shared.core.models

sealed class CommandStatus {
    object Idle : CommandStatus()
    object Sending : CommandStatus()
    object Success : CommandStatus()
    data class Error(val message: String) : CommandStatus()
}
