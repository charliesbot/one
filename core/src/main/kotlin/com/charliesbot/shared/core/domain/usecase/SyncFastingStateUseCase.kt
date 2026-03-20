package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository

class SyncFastingStateUseCase(private val fastingRepository: FastingDataRepository) {
  suspend operator fun invoke(): Result<Unit> = runCatching {
    val currentFasting = fastingRepository.getCurrentFasting()
    fastingRepository.updateFastingConfig(
      currentFasting?.startTimeInMillis,
      currentFasting?.fastingGoalId,
    )
  }
}
