package zju.bangdream.ktv.casting

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object LogFileExporter {
    const val MIME_TYPE = "text/plain"

    fun format(logs: List<LogItem>): String = logs.joinToString("\n") { item ->
        "${item.time} [${item.level.label}] ${item.tag}: ${item.message}"
    }

    fun fileName(): String {
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        return "ktv-casting-logs-$timestamp.txt"
    }

    suspend fun createShareIntent(context: Context, text: String): Intent =
        withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, "shared_logs")
            if (!directory.isDirectory && !directory.mkdirs()) {
                throw IOException("Cannot create log export directory")
            }
            // Each share keeps its own snapshot while the receiving app reads it.
            val file = File.createTempFile(fileName().removeSuffix(".txt") + "-", ".txt", directory)
            try {
                file.writeText(text, Charsets.UTF_8)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.logfiles", file)
                Intent(Intent.ACTION_SEND).apply {
                    type = MIME_TYPE
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newRawUri("运行日志", uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (error: Exception) {
                file.delete()
                throw error
            }
        }

    suspend fun save(context: Context, uri: Uri, text: String) = withContext(Dispatchers.IO) {
        val output = context.contentResolver.openOutputStream(uri, "wt")
            ?: throw IOException("Cannot open log export destination")
        output.bufferedWriter(Charsets.UTF_8).use { it.write(text) }
    }
}
