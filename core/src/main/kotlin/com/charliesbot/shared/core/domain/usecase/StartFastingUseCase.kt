package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.services.FastingEventCallbacks
import com.charliesbot.shared.core.services.FastingEventManager

class StartFastingUseCase(
    private val fastingRepository: FastingDataRepository,
    private val eventManager: FastingEventManager,
    private val localCallbacks: FastingEventCallbacks,
) {
    suspend operator fun invoke(goalId: String): Result<Unit> = runCatching {
        val existingItem = fastingRepository.getCurrentFasting()
        if (existingItem?.isFasting == true) {
            throw IllegalStateException("Cannot start fasting: there's already an active session")
        }

        val startTime = System.currentTimeMillis()
        val (previousItem, currentItem) = fastingRepository.startFasting(startTime, goalId)

        eventManager.processStateChange(previousItem, currentItem, localCallbacks)
    }
}
