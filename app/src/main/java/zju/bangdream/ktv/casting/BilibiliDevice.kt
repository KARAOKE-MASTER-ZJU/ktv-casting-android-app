package zju.bangdream.ktv.casting

data class BilibiliDevice(val name: String, val brand: String, val model: String, val buvid: String)

fun parseBilibiliDevices(json: String): List<BilibiliDevice> {
    return try {
        val arr = org.json.JSONArray(json)
        (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            BilibiliDevice(
                o.optString("name"),
                o.optString("brand"),
                o.optString("model"),
                o.optString("buvid")
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
