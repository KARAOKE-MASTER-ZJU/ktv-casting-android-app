package zju.bangdream.ktv.casting.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import zju.bangdream.ktv.casting.LogFileExporter
import zju.bangdream.ktv.casting.LogItem
import zju.bangdream.ktv.casting.LogLevel
import zju.bangdream.ktv.casting.LogRepository
import zju.bangdream.ktv.casting.R
import zju.bangdream.ktv.casting.ui.theme.KtvCastingTheme

@Composable
fun LogScreen(onBack: () -> Unit) {
    BackHandler { onBack() }

    val logs by LogRepository.logs.collectAsState()
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingSaveText by rememberSaveable { mutableStateOf<String?>(null) }
    var exporting by remember { mutableStateOf(false) }
    val canExport = logs.isNotEmpty() && !exporting && pendingSaveText == null

    val saveLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(LogFileExporter.MIME_TYPE)
    ) { uri ->
        val text = pendingSaveText
        pendingSaveText = null
        if (uri != null && text != null) {
            exporting = true
            scope.launch {
                val message = try {
                    LogFileExporter.save(context, uri, text)
                    "日志已保存"
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    "保存失败，请重试"
                } finally {
                    exporting = false
                }
                snackbarHostState.showSnackbar(message)
            }
        }
    }

    LogScreenContent(
        logs = logs,
        canExport = canExport,
        onBack = onBack,
        onClear = { LogRepository.clear() },
        onCopy = {
            val text = LogFileExporter.format(logs)
            clipboard.setText(AnnotatedString(text))
            scope.launch { snackbarHostState.showSnackbar("日志已复制") }
        },
        onShare = {
            val text = LogFileExporter.format(logs)
            exporting = true
            scope.launch {
                val failed = try {
                    val intent = LogFileExporter.createShareIntent(context, text)
                    context.startActivity(Intent.createChooser(intent, "分享日志文件"))
                    false
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    true
                } finally {
                    exporting = false
                }
                if (failed) {
                    snackbarHostState.showSnackbar("分享失败，请重试")
                }
            }
        },
        onSave = {
            pendingSaveText = LogFileExporter.format(logs)
            try {
                saveLauncher.launch(LogFileExporter.fileName())
            } catch (error: Exception) {
                pendingSaveText = null
                scope.launch { snackbarHostState.showSnackbar("无法打开保存窗口，请重试") }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LogScreenContent(
    logs: List<LogItem>,
    canExport: Boolean,
    onBack: () -> Unit,
    onClear: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
    onSave: () -> Unit,
    snackbarHost: @Composable () -> Unit = {}
) {
    val listState = rememberLazyListState()

    LaunchedEffect(logs.size) {
        if (logs.isNotEmpty()) {
            listState.animateScrollToItem(logs.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("运行日志", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onClear, enabled = logs.isNotEmpty()) {
                        Icon(Icons.Default.Delete, contentDescription = "清空")
                    }
                    IconButton(onClick = onCopy, enabled = canExport) {
                        Icon(painterResource(R.drawable.ic_content_copy), contentDescription = "复制日志")
                    }
                    IconButton(onClick = onShare, enabled = canExport) {
                        Icon(Icons.Default.Share, contentDescription = "分享日志文件")
                    }
                    IconButton(onClick = onSave, enabled = canExport) {
                        Icon(painterResource(R.drawable.ic_save), contentDescription = "保存日志到本地")
                    }
                }
            )
        },
        snackbarHost = snackbarHost
    ) { padding ->
        if (logs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("暂无日志")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surface),
                state = listState,
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(logs) { item ->
                    LogRow(item)
                }
            }
        }
    }
}

@Composable
private fun LogRow(item: LogItem) {
    val levelColor = when (item.level) {
        LogLevel.ERROR -> MaterialTheme.colorScheme.error
        LogLevel.WARN -> Color(0xFFFF9800)
        LogLevel.INFO -> MaterialTheme.colorScheme.primary
        LogLevel.DEBUG -> MaterialTheme.colorScheme.secondary
        LogLevel.VERBOSE -> MaterialTheme.colorScheme.outline
        LogLevel.UNKNOWN -> MaterialTheme.colorScheme.outlineVariant
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${item.time}  [${item.level.label}]  ${item.tag}",
                style = MaterialTheme.typography.labelSmall,
                color = levelColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "运行日志")
@Composable
private fun LogScreenPreview() {
    KtvCastingTheme {
        LogScreenContent(
            logs = listOf(
                LogItem("12:30:00.123", LogLevel.INFO, "CastingService", "投屏服务已启动"),
                LogItem("12:30:01.456", LogLevel.DEBUG, "DLNA", "发现设备：客厅电视"),
                LogItem("12:30:02.789", LogLevel.WARN, "Network", "连接超时，正在重试"),
                LogItem("12:30:03.012", LogLevel.ERROR, "Player", "播放失败\n请检查设备连接后重试")
            ),
            canExport = true,
            onBack = {},
            onClear = {},
            onCopy = {},
            onShare = {},
            onSave = {}
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640, name = "暂无日志")
@Composable
private fun LogScreenEmptyPreview() {
    KtvCastingTheme {
        LogScreenContent(
            logs = emptyList(),
            canExport = false,
            onBack = {},
            onClear = {},
            onCopy = {},
            onShare = {},
            onSave = {}
        )
    }
}
