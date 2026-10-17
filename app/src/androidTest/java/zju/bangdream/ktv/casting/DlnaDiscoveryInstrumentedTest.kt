package zju.bangdream.ktv.casting

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File
import zju.bangdream.ktv.casting.ui.screens.DeviceSelectorScreen
import zju.bangdream.ktv.casting.ui.theme.KtvCastingTheme

/** Run on an isolated emulator with no DLNA renderer. No real room/account is used. */
class DlnaDiscoveryInstrumentedTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun emptySearchLogsUdpComparisonReleasesLockAndOpensLogs() {
        RustEngine.initLogging(2)
        LogRepository.clear()
        var openedLogs = false
        compose.setContent {
            KtvCastingTheme {
                DeviceSelectorScreen(
                    activeRoom = ActiveRoom("http://127.0.0.1:9", "diagnostic-fixture"),
                    onActiveRoomChange = {},
                    onDeviceSelect = { _, _, _ -> },
                    onBilibiliMode = { _, _ -> },
                    onOpenLogs = { openedLogs = true }
                )
            }
        }
        compose.onNodeWithText("搜索 DLNA 设备").performScrollTo().performClick()
        compose.waitUntil(timeoutMillis = 20_000) {
            LogRepository.logs.value.any { it.message.contains("多播锁已释放：isHeld=false") }
        }
        val logs = LogRepository.logs.value.joinToString("\n") { "${it.tag}: ${it.message}" }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.cacheDir, "dlna-search.txt").writeText(logs)
        assertTrue(logs.contains("多播锁已获取：isHeld=true"))
        assertTrue(logs.contains("UDP发送成功"))
        for (target in listOf("AVTransport:1", "MediaRenderer:1", "ssdp:all")) {
            assertTrue(logs.lines().any { it.contains("目标对照结果") && it.contains(target) })
        }
        compose.onNodeWithText(DlnaDiscovery.EMPTY_MESSAGE).performScrollTo().assertIsDisplayed()
        File(context.cacheDir, "dlna-search.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.onNodeWithText("查看运行日志").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(openedLogs) }
    }
}
