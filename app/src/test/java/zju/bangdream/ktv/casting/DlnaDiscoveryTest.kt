package zju.bangdream.ktv.casting

import org.junit.Assert.*
import org.junit.Test

class DlnaDiscoveryTest {
    @Test
    fun nativeFailureIsLoggedAndRethrownInsteadOfBecomingAnEmptyResult() {
        val logs = mutableListOf<Pair<LogLevel, String>>()
        val failure = IllegalStateException("socket unavailable")
        try {
            runDlnaDiscovery("自动搜索", { throw failure }) { level, text -> logs += level to text }
            fail("native failure must reach the UI error path")
        } catch (actual: IllegalStateException) {
            assertSame(failure, actual)
        }
        assertTrue(logs.any { it.first == LogLevel.ERROR && it.second.contains("socket unavailable") })
        assertFalse(logs.any { it.second.contains("返回空列表") })
    }

    @Test
    fun emptyNativeResultDoesNotClaimThatTheDeviceIsOffline() {
        val logs = mutableListOf<Pair<LogLevel, String>>()
        val result = runDlnaDiscovery("自动搜索", { emptyArray() }) { level, text -> logs += level to text }
        assertTrue(result.isEmpty())
        assertTrue(logs.any { it.first == LogLevel.INFO && it.second.contains("设备数=0") })
        assertTrue(logs.any { it.first == LogLevel.WARN && it.second.contains("发现错误") })
        assertFalse(logs.any { it.first == LogLevel.ERROR })
    }

    @Test
    fun successfulResultPreservesDeviceAndDoesNotLogDescriptionCredentials() {
        val logs = mutableListOf<Pair<LogLevel, String>>()
        val devices = arrayOf(DlnaDeviceItem("Renderer", "http://host/description.xml?token=secret"))
        val result = runDlnaDiscovery("手动连接", { devices }) { level, text -> logs += level to text }
        assertSame(devices, result)
        assertTrue(logs.any { it.second.contains("设备数=1") })
        assertFalse(logs.any { it.second.contains("secret") })
        assertFalse(logs.any { it.first == LogLevel.WARN || it.first == LogLevel.ERROR })
    }
}
