package zju.bangdream.ktv.casting.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import zju.bangdream.ktv.casting.BilibiliPart
import zju.bangdream.ktv.casting.CurrentBilibiliParts
import java.util.Locale

@Composable
fun BilibiliPartsDialog(
    parts: CurrentBilibiliParts?,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    onSelect: (BilibiliPart) -> Unit,
) {
    AlertDialog(
        modifier = Modifier.fillMaxWidth(),
        onDismissRequest = onDismiss,
        title = { Text("选择分 P") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "切换后将从头播放",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                when {
                    isLoading -> CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )
                    parts == null -> Text(
                        "无法获取分 P，请稍后重试",
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(parts.parts, key = { it.pageIndex }) { part ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSelect(part) }
                                    .padding(vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "P${part.page} · ${part.part}",
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        String.format(
                                            Locale.ROOT,
                                            "%02d:%02d",
                                            part.durationSecs / 60,
                                            part.durationSecs % 60,
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (part.pageIndex == parts.currentPageIndex) {
                                    Text("当前", color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onRefresh, enabled = !isLoading) { Text("刷新") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun BilibiliPartsDialogPreview() {
    zju.bangdream.ktv.casting.ui.theme.KtvCastingTheme {
        BilibiliPartsDialog(
            parts = CurrentBilibiliParts(
                songId = "preview-song",
                bvid = "BV1LS4MzKE8y",
                currentPageIndex = 0,
                parts = listOf(
                    BilibiliPart(0, 1, 1L, "第一段", 185L),
                    BilibiliPart(1, 2, 2L, "第二段", 240L),
                ),
            ),
            isLoading = false,
            onRefresh = {},
            onDismiss = {},
            onSelect = {},
        )
    }
}
