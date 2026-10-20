package zju.bangdream.ktv.casting

import org.json.JSONObject

data class BilibiliPart(
    val pageIndex: Int,
    val page: Int,
    val cid: Long,
    val part: String,
    val durationSecs: Long,
)

data class CurrentBilibiliParts(
    val songId: String,
    val bvid: String,
    val currentPageIndex: Int,
    val parts: List<BilibiliPart>,
)

fun parseBilibiliParts(json: String): CurrentBilibiliParts? {
    return try {
        val obj = JSONObject(json)
        val items = obj.getJSONArray("parts")
        CurrentBilibiliParts(
            songId = obj.getString("songId"),
            bvid = obj.getString("bvid"),
            currentPageIndex = obj.getInt("currentPageIndex"),
            parts = (0 until items.length()).map { index ->
                val item = items.getJSONObject(index)
                BilibiliPart(
                    pageIndex = item.getInt("pageIndex"),
                    page = item.getInt("page"),
                    cid = item.getLong("cid"),
                    part = item.getString("part"),
                    durationSecs = item.getLong("durationSecs"),
                )
            },
        )
    } catch (_: Exception) {
        null
    }
}

internal fun bilibiliPartSelectionMessage(result: Int): String? = when (result) {
    1 -> "已提交分 P 切换"
    0 -> null
    else -> "未能确认切换结果，请手动刷新查看"
}
