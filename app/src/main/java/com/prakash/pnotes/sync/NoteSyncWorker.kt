package com.prakash.pnotes.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.prakash.pnotes.data.cloud.FirestoreSyncRepository
import com.prakash.pnotes.di.AppModule
import java.util.concurrent.TimeUnit

class NoteSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Not signed in (or Firebase not configured): nothing to sync, no retry.
            val userId = try {
                FirebaseAuth.getInstance().currentUser?.uid
            } catch (e: Exception) {
                null
            } ?: return Result.success()
            val repository = AppModule.getRepository(applicationContext)
            val result = FirestoreSyncRepository(userId, repository).sync()
            if (result.success) Result.success() else Result.retry()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "note_sync_work"
    }
}

object SyncInitializer {
    fun enqueue(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<NoteSyncWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            NoteSyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(NoteSyncWorker.WORK_NAME)
    }
}
