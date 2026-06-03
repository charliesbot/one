package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.events.FastingEventCallbacks
import com.charliesbot.shared.core.domain.events.FastingEventProcessor
import com.charliesbot.shared.core.domain.repository.FastingDataRepository

class UpdateFastingConfigUseCase(
  private val fastingRepository: FastingDataRepository,
  private val eventProcessor: FastingEventProcessor,
  private val localCallbacks: FastingEventCallbacks,
) {
  suspend operator fun invoke(startTimeMillis: Long? = null, goalId: String? = null): Result<Unit> =
    runCatching {
      if (startTimeMillis == null && goalId == null) {
        throw IllegalStateException("No valid update config provided")
      }

      val (previousItem, currentItem) =
        fastingRepository.updateFastingConfig(startTimeMillis, goalId)

      eventProcessor.processStateChange(previousItem, currentItem, localCallbacks)
    }
}
