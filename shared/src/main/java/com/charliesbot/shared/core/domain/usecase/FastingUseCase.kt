package com.charliesbot.shared.core.domain.usecase

import android.util.Log
import com.charliesbot.shared.core.constants.AppConstants.LOG_TAG
import com.charliesbot.shared.core.data.repositories.fastingDataRepository.FastingDataRepository
import com.charliesbot.shared.core.models.FastingDataItem
import com.charliesbot.shared.core.services.FastingEventCallbacks
import com.charliesbot.shared.core.services.FastingEventManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class FastingUseCase(
    private val fastingRepository: FastingDataRepository,
    private val eventManager: FastingEventManager,
    private val localCallbacks: FastingEventCallbacks
) {

    fun getCurrentFastingFlow(): Flow<FastingDataItem?> {
        return fastingRepository.fastingDataItem
    }

    /**
     * Start a new fasting session locally.
     *
     * @param goalId The ID of the fasting goal/duration selected by the user
     * @throws IllegalStateException if there's already an active fasting session
     * @throws Exception if any part of the process fails
     */
    suspend fun startFasting(goalId: String) {
        Log.d(LOG_TAG, "FastingUseCase: Starting fasting locally with goal: $goalId")
        try {
            val existingItem = fastingRepository.getCurrentFasting()
            if (existingItem?.isFasting == true) {
                throw IllegalStateException("Cannot start fasting: there's already an active session")
            }

            val startTime = System.currentTimeMillis()
            val (previousItem, currentItem) = fastingRepository.startFasting(startTime, goalId)

            eventManager.processStateChange(previousItem, currentItem, localCallbacks)
            Log.d(LOG_TAG, "FastingUseCase: Processed local start event via EventManager")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "FastingUseCase: Failed to start fasting locally", e)
            // Re-throw to let the calling ViewModel handle user-facing error messaging
            throw e
        }
    }

    /**
     * Complete the current fasting session locally.
     *
     * @throws IllegalStateException if no active fasting session exists
     * @throws Exception if any part of the process fails
     */
    suspend fun stopFasting() {
        Log.d(LOG_TAG, "FastingUseCase: Stopping fasting locally")
        try {
            val existingItem = fastingRepository.getCurrentFasting()
            if (existingItem?.isFasting != true) {
                throw IllegalStateException("No active fasting session to stop")
            }
            val (previousItem, currentItem) = fastingRepository.stopFasting(existingItem.fastingGoalId)
            eventManager.processStateChange(previousItem, currentItem, localCallbacks)
            Log.d(LOG_TAG, "FastingUseCase: Processed local stop event via EventManager")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "FastingUseCase: Failed to stop fasting locally", e)
            throw e
        }
    }

    suspend fun updateFastingConfig(startTimeMillis: Long? = null, goalId: String? = null) {
        Log.d(LOG_TAG, "FastingUseCase: Updating Fasting config locally")
        try {

            if (startTimeMillis == null && goalId == null) {
                throw IllegalStateException("No valid update config provided")
            }

            val (previousItem, currentItem) = fastingRepository.updateFastingConfig(
                startTimeMillis,
                goalId
            )

            eventManager.processStateChange(previousItem, currentItem, localCallbacks)
            Log.d(LOG_TAG, "FastingUseCase: Processed local update event via EventManager")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "FastingUseCase: Failed to update fasting locally", e)
            throw e
        }
    }


    /**
     * Manually sync the current fasting state with the other device.
     * Useful for recovering from sync failures or ensuring consistency.
     */
    suspend fun syncCurrentState() {
        Log.d(LOG_TAG, "FastingUseCase: Manually syncing current state")

        try {
            val currentFasting = fastingRepository.getCurrentFasting()
            // Use updateFastingGoalId to trigger a sync without changing the goal
            fastingRepository.updateFastingConfig(
                currentFasting?.startTimeInMillis,
                currentFasting?.fastingGoalId
            )
            Log.d(LOG_TAG, "FastingUseCase: Successfully synced current state")
        } catch (e: Exception) {
            Log.e(LOG_TAG, "FastingUseCase: Failed to sync current state", e)
            throw e
        }
    }

//    suspend fun getFastingHistory(limit: Int = 10): List<FastingDataItem> {
//        return try {
//            fastingRepository.getFastingHistory(limit)
//        } catch (e: Exception) {
//            Log.e(LOG_TAG, "FastingUseCase: Failed to get fasting history", e)
//            throw e
//        }
//    }
}