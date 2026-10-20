package zju.bangdream.ktv.casting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private val bilibiliPartsJson = """
    {
        "songId": "song-a",
        "bvid": "BV1LS4MzKE8y",
        "currentPageIndex": 1,
        "parts": [
            {"pageIndex": 0, "page": 1, "cid": 3000000001, "part": "第一段", "durationSecs": 65},
            {"pageIndex": 1, "page": 2, "cid": 3000000002, "part": "第二段", "durationSecs": 120}
        ]
    }
""".trimIndent()

class BilibiliPartsTest {
    @Test
    fun parse_preservesSongIdPageIndicesAnd64BitCid() {
        val result = requireNotNull(parseBilibiliParts(bilibiliPartsJson))

        assertEquals("song-a", result.songId)
        assertEquals("BV1LS4MzKE8y", result.bvid)
        assertEquals(1, result.currentPageIndex)
        assertEquals(listOf(0, 1), result.parts.map { it.pageIndex })
        assertEquals(listOf(1, 2), result.parts.map { it.page })
        assertEquals(3000000001L, result.parts.first().cid)
        assertEquals("第一段", result.parts.first().part)
        assertEquals(65L, result.parts.first().durationSecs)
    }

    @Test
    fun parse_emptyMalformedOrIncompleteResponseReturnsNull() {
        listOf("", "not json", "{}", bilibiliPartsJson.replace("\"cid\"", "\"missing\""))
            .forEach { assertNull(parseBilibiliParts(it)) }
    }
}
