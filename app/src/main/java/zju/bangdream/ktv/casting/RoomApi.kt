package zju.bangdream.ktv.casting

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed interface EnsureRoomResult {
    data object Success : EnsureRoomResult
    data class Failure(val message: String) : EnsureRoomResult
}

object RoomApi {
    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    suspend fun ensureRoom(baseUrl: String, roomId: String): EnsureRoomResult =
        withContext(Dispatchers.IO) {
            try {
                ensureRoomBlocking(baseUrl, roomId)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                RustEngine.logFromKotlin(
                    "RoomApi",
                    "准备房间失败: ${e.message}",
                    LogLevel.ERROR
                )
                EnsureRoomResult.Failure("无法连接服务器，请检查服务器网址和网络连接")
            }
        }

    private fun ensureRoomBlocking(baseUrl: String, roomId: String): EnsureRoomResult {
        val serverUrl = baseUrl.trim().trimEnd('/').toHttpUrlOrNull()
            ?: return EnsureRoomResult.Failure("服务器网址格式不正确")

        val existsRequest = Request.Builder()
            .url(serverUrl.apiUrl("roomExists", roomId))
            .get()
            .build()

        httpClient.newCall(existsRequest).execute().use { response ->
            if (!response.isSuccessful) {
                return EnsureRoomResult.Failure("检查房间失败（HTTP ${response.code}）")
            }
            val json = response.body?.string()?.let(::JSONObject)
                ?: return EnsureRoomResult.Failure("服务器返回了无效的房间信息")
            if (!json.has("exists")) {
                return EnsureRoomResult.Failure("服务器不支持房间创建接口，请更新服务器")
            }
            if (json.optBoolean("exists")) return EnsureRoomResult.Success
        }

        val createRequest = Request.Builder()
            .url(serverUrl.apiUrl("createRoom", roomId))
            .post(ByteArray(0).toRequestBody())
            .build()

        httpClient.newCall(createRequest).execute().use { response ->
            if (!response.isSuccessful) {
                return EnsureRoomResult.Failure("创建房间失败（HTTP ${response.code}）")
            }
            val json = response.body?.string()?.let(::JSONObject)
                ?: return EnsureRoomResult.Failure("服务器返回了无效的创建结果")
            if (json.optBoolean("success") || json.optString("msg") == "房间已存在") {
                return EnsureRoomResult.Success
            }
            return EnsureRoomResult.Failure(json.optString("msg", "创建房间失败"))
        }
    }

    private fun HttpUrl.apiUrl(endpoint: String, roomId: String): HttpUrl =
        newBuilder()
            .addPathSegments("api/$endpoint")
            .addQueryParameter("roomId", roomId)
            .build()
}
