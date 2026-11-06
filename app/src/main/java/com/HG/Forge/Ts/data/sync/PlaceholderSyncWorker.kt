package com.HG.Forge.Ts.data.sync
import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
/** Phase 0 boundary; real synchronization follows the approved remote schema. */
class PlaceholderSyncWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params) {
 override suspend fun doWork():Result=Result.success()
}
