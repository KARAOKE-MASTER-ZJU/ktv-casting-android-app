package zju.bangdream.ktv.casting.update

import android.content.Context
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.ExistingWorkPolicy
import androidx.work.workDataOf

class ApkDownloader(private val context: Context) {

    fun downloadAndInstall(releaseInfo: ReleaseInfo) {
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(
                workDataOf(
                    "apk_url" to releaseInfo.apkUrl,
                    "tag_name" to releaseInfo.tagName,
                    "html_url" to releaseInfo.htmlUrl
                )
            )
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            "apk_download",
            ExistingWorkPolicy.KEEP,
            downloadRequest
        )
    }

    fun getPendingApkPath(): String? {
        val prefs = context.getSharedPreferences("update_check", Context.MODE_PRIVATE)
        return prefs.getString("pending_apk_path", null)
    }

    fun clearPendingApk() {
        val prefs = context.getSharedPreferences("update_check", Context.MODE_PRIVATE)
        prefs.edit().remove("pending_apk_path").apply()
    }
}
