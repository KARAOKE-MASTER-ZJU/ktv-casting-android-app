package zju.bangdream.ktv.casting.ui.screens

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import zju.bangdream.ktv.casting.CurrentBilibiliParts
import zju.bangdream.ktv.casting.parseBilibiliParts
import zju.bangdream.ktv.casting.ui.components.BilibiliPartsDialog

/** 调用方按歌曲 ID 设置 key，查询结果在当前歌曲下复用。 */
@Composable
internal fun BilibiliPartsControl(
    enabled: Boolean,
    loadParts: suspend () -> String,
    selectPart: suspend (String, Int) -> Int,
    onSelectionResult: (Int) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }
    var isLoading by remember { mutableStateOf(enabled) }
    var hasLoaded by remember { mutableStateOf(false) }
    var parts by remember { mutableStateOf<CurrentBilibiliParts?>(null) }

    LaunchedEffect(enabled, refreshKey) {
        if (!enabled || hasLoaded) return@LaunchedEffect
        isLoading = true
        try {
            val json = loadParts()
            currentCoroutineContext().ensureActive()
            parts = parseBilibiliParts(json)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            parts = null
        } finally {
            isLoading = false
        }
        hasLoaded = true
    }

    TextButton(
        onClick = {
            isLoading = !hasLoaded
            showDialog = true
        },
        enabled = enabled,
    ) {
        val currentPart = parts?.let { data ->
            data.parts.find { it.pageIndex == data.currentPageIndex }
        }
        Text(
            text = currentPart?.let { "${it.page}P - ${it.part}" }
                ?: if (isLoading) "正在加载分 P..." else "选择分 P",
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
    if (showDialog) {
        BilibiliPartsDialog(
            parts = parts,
            isLoading = isLoading,
            onRefresh = {
                hasLoaded = false
                isLoading = true
                refreshKey++
            },
            onDismiss = { showDialog = false },
            onSelect = { part ->
                val selectedParts = parts
                showDialog = false
                if (selectedParts != null) {
                    scope.launch {
                        val result = try {
                            selectPart(selectedParts.songId, part.pageIndex)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (_: Exception) {
                            -1
                        }
                        if (result == 1 || result == 0) {
                            parts?.takeIf {
                                it.songId == selectedParts.songId && it.bvid == selectedParts.bvid
                            }?.let {
                                parts = it.copy(currentPageIndex = part.pageIndex)
                            }
                        }
                        onSelectionResult(result)
                    }
                }
            },
        )
    }
}
