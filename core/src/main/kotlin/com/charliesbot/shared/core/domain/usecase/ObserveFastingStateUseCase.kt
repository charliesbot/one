package com.charliesbot.shared.core.domain.usecase

import com.charliesbot.shared.core.domain.repository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import kotlinx.coroutines.flow.Flow

class ObserveFastingStateUseCase(private val fastingRepository: FastingDataRepository) {
  operator fun invoke(): Flow<FastingDataItem?> = fastingRepository.fastingDataItem
}
