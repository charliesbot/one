package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.services.FastingEventCallbacks
import com.charliesbot.shared.core.services.FastingEventManager

class UpdateFastingConfigUseCase(
  private val fastingRepository: FastingDataRepository,
  private val eventManager: FastingEventManager,
  private val localCallbacks: FastingEventCallbacks,
) {
  suspend operator fun invoke(startTimeMillis: Long? = null, goalId: String? = null): Result<Unit> =
    runCatching {
      if (startTimeMillis == null && goalId == null) {
        throw IllegalStateException("No valid update config provided")
      }

      val (previousItem, currentItem) =
        fastingRepository.updateFastingConfig(startTimeMillis, goalId)

      eventManager.processStateChange(previousItem, currentItem, localCallbacks)
    }
}
