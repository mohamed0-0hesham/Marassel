package com.hesham0_0.marassel.worker

import androidx.lifecycle.asFlow
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.hesham0_0.marassel.domain.model.MessageStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkInfoMessageBridge @Inject constructor(
    private val workManager: WorkManager,
) {

    fun observeMessageStatus(
        localId: String,
        workRequestId: UUID,
    ): Flow<MessageStatusUpdate> =
        workManager
            .getWorkInfoByIdLiveData(workRequestId)
            .asFlow()
            .filterNotNull()
            .map { workInfo -> workInfo.toStatusUpdate(localId) }
            .filterNotNull()
            .distinctUntilChanged()

    fun observeMessageStatusByTag(localId: String): Flow<MessageStatusUpdate> =
        workManager
            .getWorkInfosByTagLiveData(localId)
            .asFlow()
            .filterNotNull()
            .map { workInfoList ->
                // For a unique work chain, there should be at most one active request.
                // Pick the most relevant one (running > enqueued > succeeded > failed)
                workInfoList
                    .sortedByDescending { it.state.ordinal }
                    .firstOrNull()
                    ?.toStatusUpdate(localId)
            }
            .filterNotNull()
            .distinctUntilChanged()

    fun observeUploadProgress(workRequestId: UUID): Flow<Int?> =
        workManager
            .getWorkInfoByIdLiveData(workRequestId)
            .asFlow()
            .filterNotNull()
            .map { workInfo ->
                if (workInfo.state == WorkInfo.State.RUNNING) {
                    workInfo.progress.getInt(WorkerKeys.KEY_UPLOAD_PROGRESS, -1)
                        .takeIf { it >= 0 }
                } else null
            }
            .distinctUntilChanged()

    private fun WorkInfo.toStatusUpdate(localId: String): MessageStatusUpdate? {
        val status = when (state) {
            WorkInfo.State.ENQUEUED,
            WorkInfo.State.BLOCKED,
            WorkInfo.State.RUNNING  -> MessageStatus.PENDING

            WorkInfo.State.SUCCEEDED -> MessageStatus.SENT

            WorkInfo.State.FAILED,
            WorkInfo.State.CANCELLED -> MessageStatus.FAILED
        }

        // Extract the Firebase key from the output data on success,
        // so the ViewModel can persist it alongside the SENT status.
        val firebaseKey = if (state == WorkInfo.State.SUCCEEDED) {
            outputData.getString(WorkerKeys.KEY_LOCAL_ID)
        } else null

        return MessageStatusUpdate(
            localId     = localId,
            status      = status,
            firebaseKey = firebaseKey,
            workState   = state,
        )
    }
}

data class MessageStatusUpdate(
    val localId: String,
    val status: MessageStatus,
    val firebaseKey: String?,
    val workState: WorkInfo.State,
) {
    val isTerminal: Boolean
        get() = status == MessageStatus.SENT || status == MessageStatus.FAILED
}