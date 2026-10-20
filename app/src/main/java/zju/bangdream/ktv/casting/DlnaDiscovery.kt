package zju.bangdream.ktv.casting

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Shared by the connection page and the change-device dialog. */
object DlnaDiscovery {
    const val EMPTY_MESSAGE = "未获取到 DLNA 设备，请查看运行日志中的搜索结果和错误详情"
    const val ERROR_MESSAGE = "DLNA 搜索发生异常，请查看运行日志中的错误详情"
    private const val TAG = "DLNA_DISCOVERY"

    suspend fun search(context: Context, descriptionUrl: String? = null): Array<DlnaDeviceItem> =
        withContext(Dispatchers.IO) {
            logNetworkContext(context)
            runDlnaDiscovery(
                mode = if (descriptionUrl == null) "自动搜索" else "手动连接",
                discover = {
                    if (descriptionUrl == null) searchWithLock(context)
                    else RustEngine.searchDeviceByUrl(descriptionUrl)
                },
                log = { level, message -> LogRepository.add(level, TAG, message) }
            )
        }

    private fun searchWithLock(context: Context): Array<DlnaDeviceItem> {
        val manager = context.applicationContext.getSystemService(WifiManager::class.java)
        // A local strong reference remains alive until synchronous JNI discovery returns.
        // Every search owns its lock; cancellation/exception also goes through finally.
        val lock = manager.createMulticastLock("ktv_dlna_discovery")
        lock.setReferenceCounted(false)
        return runWithDiscoveryLock(
            acquire = {
                lock.acquire()
                LogRepository.add(LogLevel.INFO, TAG, "多播锁已获取：isHeld=${lock.isHeld}")
            },
            release = {
                if (lock.isHeld) lock.release()
                LogRepository.add(LogLevel.INFO, TAG, "多播锁已释放：isHeld=${lock.isHeld}")
            },
            discover = { RustEngine.searchDevices() }
        )
    }

    private fun logNetworkContext(context: Context) {
        LogRepository.add(
            LogLevel.INFO, TAG,
            "诊断环境：App=${BuildConfig.VERSION_NAME}，Android=${Build.VERSION.RELEASE}" +
                "，SDK=${Build.VERSION.SDK_INT}，设备=${Build.MANUFACTURER} ${Build.MODEL}"
        )
        try {
            val manager = context.getSystemService(ConnectivityManager::class.java)
            val network = manager.activeNetwork
            val capabilities = manager.getNetworkCapabilities(network)
            val properties = manager.getLinkProperties(network)
            LogRepository.add(
                LogLevel.INFO, TAG,
                "系统默认网络：存在=${network != null}" +
                    "，Wi-Fi=${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)}" +
                    "，VPN=${capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN)}" +
                    "，本地地址=${properties?.linkAddresses?.joinToString { it.toString() } ?: "未知"}" +
                    "，接口=${properties?.interfaceName ?: "未知"}；此信息不代表 SSDP 实际选用的接口"
            )
        } catch (error: SecurityException) {
            // Diagnostic metadata must not prevent the existing discovery call.
            LogRepository.add(LogLevel.WARN, TAG, "无法读取默认网络信息：${error.javaClass.simpleName}")
        }
    }
}

internal fun runWithDiscoveryLock(
    acquire: () -> Unit,
    release: () -> Unit,
    discover: () -> Array<DlnaDeviceItem>
): Array<DlnaDeviceItem> {
    acquire()
    try {
        return discover()
    } finally {
        release()
    }
}

internal fun runDlnaDiscovery(
    mode: String,
    discover: () -> Array<DlnaDeviceItem>,
    log: (LogLevel, String) -> Unit
): Array<DlnaDeviceItem> {
    try {
        return discover()
    } catch (error: Exception) {
        log(LogLevel.ERROR, "$mode：原生调用异常，${error.javaClass.name}: ${error.message}")
        throw error
    }
}
