package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.events.FastingEventCallbacks
import com.charliesbot.shared.core.domain.events.FastingEventProcessor
import com.charliesbot.shared.core.domain.repository.FastingDataRepository

class StartFastingUseCase(
  private val fastingRepository: FastingDataRepository,
  private val eventProcessor: FastingEventProcessor,
  private val localCallbacks: FastingEventCallbacks,
) {
  suspend operator fun invoke(goalId: String): Result<Unit> = runCatching {
    val existingItem = fastingRepository.getCurrentFasting()
    if (existingItem?.isFasting == true) {
      throw IllegalStateException("Cannot start fasting: there's already an active session")
    }

    val startTime = System.currentTimeMillis()
    val (previousItem, currentItem) = fastingRepository.startFasting(startTime, goalId)

    eventProcessor.processStateChange(previousItem, currentItem, localCallbacks)
  }
}
