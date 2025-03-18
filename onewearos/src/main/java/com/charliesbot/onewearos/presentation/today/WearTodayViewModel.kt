package com.charliesbot.onewearos.presentation.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charliesbot.shared.core.data.repositories.WearableMessageRepository
import com.charliesbot.shared.core.datalayer.FastingDataClient
import com.charliesbot.shared.core.models.CommandStatus
import com.charliesbot.shared.core.models.DeviceType
import com.charliesbot.shared.core.models.FastingCommand
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class WearTodayViewModel(
    private val fastingDataClient: FastingDataClient,
    private val messageRepository: WearableMessageRepository
) : ViewModel() {
    val isFasting = fastingDataClient.isFasting
    val startTimeInMillis = fastingDataClient.startTimeInMillis

    private val _commandStatus =
        MutableStateFlow<CommandStatus>(CommandStatus.Idle)
    val commandStatus: StateFlow<CommandStatus> = _commandStatus

    override fun onCleared() {
        super.onCleared()
        fastingDataClient.cleanup()
    }

    private fun updateCommandStatus(newCommandStatus: CommandStatus) {
        viewModelScope.launch {
            _commandStatus.value = newCommandStatus
            delay(3000)
            _commandStatus.value = CommandStatus.Idle
        }
    }

    fun onStartFasting() {
        viewModelScope.launch {
            _commandStatus.value = CommandStatus.Sending
            fastingDataClient.startFasting(System.currentTimeMillis())
            updateCommandStatus(
                messageRepository.sendCommandToMobile(
                    FastingCommand.START_FASTING
                )
            )
        }
    }

    fun onStopFasting() {
        viewModelScope.launch {
            _commandStatus.value = CommandStatus.Sending
            fastingDataClient.stopFasting()
            updateCommandStatus(
                messageRepository.sendCommandToMobile(
                    FastingCommand.STOP_FASTING
                )
            )
        }
    }
}
