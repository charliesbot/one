package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.services.FastingEventCallbacks
import com.charliesbot.shared.core.services.FastingEventProcessor

class StopFastingUseCase(
  private val fastingRepository: FastingDataRepository,
  private val eventProcessor: FastingEventProcessor,
  private val localCallbacks: FastingEventCallbacks,
) {
  suspend operator fun invoke(): Result<Unit> = runCatching {
    val existingItem = fastingRepository.getCurrentFasting()
    if (existingItem?.isFasting != true) {
      return@runCatching
    }
    val (previousItem, currentItem) = fastingRepository.stopFasting(existingItem.fastingGoalId)
    eventProcessor.processStateChange(previousItem, currentItem, localCallbacks)
  }
}
