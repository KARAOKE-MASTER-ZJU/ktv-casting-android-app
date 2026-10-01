package zju.bangdream.ktv.casting

import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class LogFileExporterTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun share_hasReadableUtf8AttachmentAndReadPermission() = runBlocking {
        val text = "12:34:56.789 [E] 播放: 第一行\n第二行 🎵"
        val intent = LogFileExporter.createShareIntent(context, text)
        val uri = requireNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        try {
            assertEquals(Intent.ACTION_SEND, intent.action)
            assertEquals("text/plain", intent.type)
            assertEquals("content", uri.scheme)
            assertEquals("${context.packageName}.logfiles", uri.authority)
            assertEquals(uri, intent.clipData?.getItemAt(0)?.uri)
            assertTrue(intent.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(0, intent.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            assertEquals(text, readText(uri))
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }

    @Test
    fun consecutiveShares_keepIndependentSnapshots() = runBlocking {
        val first = LogFileExporter.createShareIntent(context, "第一次分享")
        val firstUri = requireNotNull(first.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        try {
            val second = LogFileExporter.createShareIntent(context, "第二次分享")
            val secondUri = requireNotNull(second.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
            try {
                assertNotEquals(firstUri, secondUri)
                assertEquals("第一次分享", readText(firstUri))
                assertEquals("第二次分享", readText(secondUri))
            } finally {
                context.contentResolver.delete(secondUri, null, null)
            }
        } finally {
            context.contentResolver.delete(firstUri, null, null)
        }
    }

    @Test
    fun save_replacesDestinationWithUtf8Text() = runBlocking {
        val intent = LogFileExporter.createShareIntent(context, "原有的较长日志内容")
        val uri = requireNotNull(intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))
        try {
            LogFileExporter.save(context, uri, "新日志 🎵")
            assertEquals("新日志 🎵", readText(uri))
        } finally {
            context.contentResolver.delete(uri, null, null)
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun shareProvider_rejectsFilesOutsideLogDirectory() {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.logfiles",
            File(context.cacheDir, "private.txt")
        )
    }

    private fun readText(uri: Uri): String =
        requireNotNull(context.contentResolver.openInputStream(uri))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
}
