package moe.shizuku.manager.home

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import moe.shizuku.manager.Helps
import moe.shizuku.manager.R
import moe.shizuku.manager.hide.HideAppsActivity
import moe.shizuku.manager.hide.HideAppsManager
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.ui.theme.ShizukuComposeTheme
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.UserHandleCompat
import rikka.core.util.ClipboardUtils

@Composable
fun HomeComposeScreen(
    status: ServiceStatus?,
    grantedCount: Int?,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onStopService: () -> Unit,
    onManageApps: () -> Unit,
    onOpenTerminal: () -> Unit,
    onStartRoot: () -> Unit,
    onRestartRoot: () -> Unit,
    onOpenWirelessGuide: () -> Unit,
    onPairWireless: () -> Unit,
    onStartWirelessAdb: () -> Unit,
    onCopyAdbCommand: () -> Unit,
    onSendAdbCommand: () -> Unit,
    onOpenAdbPermissionHelp: () -> Unit,
    onOpenLearnMore: () -> Unit,
    onOpenDevelopmentSettings: () -> Unit = {}
) {
    ShizukuComposeTheme {
        HomeScreenContent(
            status = status,
            grantedCount = grantedCount,
            onNavigateBack = onNavigateBack,
            onOpenSettings = onOpenSettings,
            onStopService = onStopService,
            onManageApps = onManageApps,
            onOpenTerminal = onOpenTerminal,
            onStartRoot = onStartRoot,
            onRestartRoot = onRestartRoot,
            onOpenWirelessGuide = onOpenWirelessGuide,
            onPairWireless = onPairWireless,
            onStartWirelessAdb = onStartWirelessAdb,
            onCopyAdbCommand = onCopyAdbCommand,
            onSendAdbCommand = onSendAdbCommand,
            onOpenAdbPermissionHelp = onOpenAdbPermissionHelp,
            onOpenLearnMore = onOpenLearnMore,
            onOpenDevelopmentSettings = onOpenDevelopmentSettings
        )
    }
}

private enum class StartMethodTab {
    WIRELESS,
    ROOT,
    COMPUTER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    status: ServiceStatus?,
    grantedCount: Int?,
    onNavigateBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onStopService: () -> Unit,
    onManageApps: () -> Unit,
    onOpenTerminal: () -> Unit,
    onStartRoot: () -> Unit,
    onRestartRoot: () -> Unit,
    onOpenWirelessGuide: () -> Unit,
    onPairWireless: () -> Unit,
    onStartWirelessAdb: () -> Unit,
    onCopyAdbCommand: () -> Unit,
    onSendAdbCommand: () -> Unit,
    onOpenAdbPermissionHelp: () -> Unit,
    onOpenLearnMore: () -> Unit,
    onOpenDevelopmentSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }
    val resolvedStatus = status ?: ServiceStatus()
    val running = resolvedStatus.isRunning
    val isRoot = EnvironmentUtils.isRooted()
    val hiddenPackages by HideAppsManager.hiddenPackagesFlow.collectAsState()
    val hiddenCount = hiddenPackages.size

    val defaultTab = remember {
        when {
            isRoot -> StartMethodTab.ROOT
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || EnvironmentUtils.getAdbTcpPort() > 0 -> StartMethodTab.WIRELESS
            else -> StartMethodTab.COMPUTER
        }
    }
    var selectedTab by remember { mutableStateOf(defaultTab) }

    val versionName = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
            .getOrNull()
            .orEmpty()
    }

    BackHandler(onBack = onNavigateBack)

    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_options))
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hide_apps_title)) },
                            onClick = {
                                menuExpanded = false
                                context.startActivity(Intent(context, HideAppsActivity::class.java))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_about)) },
                            onClick = {
                                menuExpanded = false
                                dialog = HomeDialog.About
                            }
                        )
                        if (running) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_stop)) },
                                onClick = {
                                    menuExpanded = false
                                    dialog = HomeDialog.Stop
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                end = 16.dp,
                bottom = innerPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Dynamic Hero Status Banner
            item {
                HeroStatusBanner(
                    status = resolvedStatus,
                    running = running,
                    onStopService = { dialog = HomeDialog.Stop },
                    onRestartRoot = onRestartRoot
                )
            }

            // 2. Limited ADB Warning (if applicable)
            if (running && !resolvedStatus.permission) {
                item {
                    WarningBanner(
                        title = stringResource(R.string.home_adb_is_limited_title),
                        summary = stringResource(R.string.home_adb_is_limited_description),
                        actionLabel = stringResource(R.string.home_adb_button_view_help),
                        onAction = onOpenAdbPermissionHelp
                    )
                }
            }

            // 3. Quick Action Grid (2x2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Security,
                            title = stringResource(R.string.home_metric_authorized),
                            subtitle = if (running) {
                                context.resources.getQuantityString(
                                    R.plurals.home_app_management_authorized_apps_count,
                                    grantedCount ?: 0,
                                    grantedCount ?: 0
                                )
                            } else {
                                stringResource(R.string.home_status_inactive)
                            },
                            enabled = running,
                            onClick = onManageApps
                        )
                        QuickActionGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.VisibilityOff,
                            title = stringResource(R.string.hide_apps_title),
                            subtitle = stringResource(R.string.hide_apps_count, hiddenCount),
                            enabled = true,
                            onClick = { context.startActivity(Intent(context, HideAppsActivity::class.java)) }
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        QuickActionGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.Terminal,
                            title = stringResource(R.string.home_metric_terminal),
                            subtitle = stringResource(R.string.home_metric_terminal_desc),
                            enabled = running,
                            onClick = onOpenTerminal
                        )
                        QuickActionGridTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Outlined.HelpOutline,
                            title = stringResource(R.string.home_grid_docs),
                            subtitle = stringResource(R.string.home_grid_docs_summary),
                            enabled = true,
                            onClick = onOpenLearnMore
                        )
                    }
                }
            }

            // 4. Start Shizuku Section (Segmented Selector)
            item {
                SectionHeader(
                    title = stringResource(R.string.home_section_start_methods),
                    badge = if (isRoot) {
                        {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Text(
                                    text = stringResource(R.string.home_status_root_available),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    } else null
                )
            }

            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val tabs = StartMethodTab.values()
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = selectedTab == tab

                        SegmentedButton(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                            icon = {
                                Icon(
                                    imageVector = when (tab) {
                                        StartMethodTab.WIRELESS -> Icons.Outlined.Wifi
                                        StartMethodTab.ROOT -> Icons.Outlined.PlayArrow
                                        StartMethodTab.COMPUTER -> Icons.Outlined.Computer
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        ) {
                            Text(
                                text = when (tab) {
                                    StartMethodTab.WIRELESS -> stringResource(R.string.home_tab_wireless)
                                    StartMethodTab.ROOT -> stringResource(R.string.home_tab_root)
                                    StartMethodTab.COMPUTER -> stringResource(R.string.home_tab_computer)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            item {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "StartMethodCard"
                ) { tab ->
                    when (tab) {
                        StartMethodTab.WIRELESS -> {
                            WirelessAdbStartCard(
                                onStart = onStartWirelessAdb,
                                onPair = onPairWireless,
                                onOpenDevSettings = onOpenDevelopmentSettings,
                                onGuide = onOpenWirelessGuide
                            )
                        }
                        StartMethodTab.ROOT -> {
                            val rootRestart = running && resolvedStatus.uid == 0
                            RootStartCard(
                                isRestart = rootRestart,
                                onStart = if (rootRestart) onRestartRoot else onStartRoot
                            )
                        }
                        StartMethodTab.COMPUTER -> {
                            ComputerAdbStartCard(
                                onCopyCommand = {
                                    ClipboardUtils.put(context, Starter.adbCommand)
                                    Toast.makeText(context, context.getString(R.string.home_command_copied), Toast.LENGTH_SHORT).show()
                                },
                                onViewCommandDialog = { dialog = HomeDialog.AdbCommand },
                                onGuide = { dialog = HomeDialog.AdbCommand }
                            )
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    when (dialog) {
        HomeDialog.About -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                confirmButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                title = { Text(stringResource(R.string.action_about)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                        Text(text = versionName)
                        LinkRow(
                            label = context.getString(R.string.about_view_source_code, "GitHub"),
                            url = "https://github.com/Towartz/Shizuku-Next"
                        )
                        LinkRow(
                            label = context.getString(R.string.about_follow_channel, "t.me/np_nbcn"),
                            url = "https://t.me/np_nbcn"
                        )
                    }
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        HomeDialog.Stop -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                icon = {
                    Icon(
                        imageVector = Icons.Outlined.PowerSettingsNew,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            dialog = null
                            onStopService()
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { dialog = null }) {
                        Text(stringResource(android.R.string.cancel))
                    }
                },
                title = { Text(stringResource(R.string.action_stop)) },
                text = { Text(stringResource(R.string.dialog_stop_message)) },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        HomeDialog.AdbCommand -> {
            AlertDialog(
                onDismissRequest = { dialog = null },
                confirmButton = {
                    TextButton(onClick = {
                        dialog = null
                        onCopyAdbCommand()
                    }) {
                        Text(stringResource(R.string.home_adb_dialog_view_command_copy_button))
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        dialog = null
                        onSendAdbCommand()
                    }) {
                        Text(stringResource(R.string.home_adb_dialog_view_command_button_send))
                    }
                },
                title = { Text(stringResource(R.string.home_adb_button_view_command)) },
                text = {
                    Text(
                        text = Starter.adbCommand,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.extraLarge
            )
        }

        null -> Unit
    }
}

@Composable
private fun HeroStatusBanner(
    status: ServiceStatus,
    running: Boolean,
    onStopService: () -> Unit,
    onRestartRoot: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (running) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
            }
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (running) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.size(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (running) {
                            stringResource(R.string.home_status_service_is_running, stringResource(R.string.app_name))
                        } else {
                            stringResource(R.string.home_status_service_not_running, stringResource(R.string.app_name))
                        },
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (running) {
                            stringResource(
                                R.string.home_status_service_version,
                                if (status.uid == 0) "Root" else "ADB",
                                status.versionName
                            )
                        } else {
                            stringResource(R.string.home_status_inactive)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = if (running) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                ) {
                    Text(
                        text = if (running) stringResource(R.string.home_status_active) else stringResource(R.string.home_status_inactive),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (running) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onError,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            if (running) {
                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (status.uid == 0) {
                        FilledTonalButton(
                            onClick = onRestartRoot,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.home_root_button_restart))
                        }
                    }
                    FilledTonalButton(
                        onClick = onStopService,
                        colors = androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Outlined.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.action_stop))
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionGridTile(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(enabled = enabled, onClick = onClick),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(26.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    badge: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onSurface
        )
        badge?.invoke()
    }
}

@Composable
private fun WirelessAdbStartCard(
    onStart: () -> Unit,
    onPair: () -> Unit,
    onOpenDevSettings: () -> Unit,
    onGuide: () -> Unit
) {
    val currentPort = remember { EnvironmentUtils.getAdbTcpPort() }
    val isPortActive = currentPort in 1..65535

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Wifi,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_tab_wireless),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isPortActive) {
                                stringResource(R.string.home_wireless_port_active, currentPort)
                            } else {
                                stringResource(R.string.home_start_wireless_summary)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isPortActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (isPortActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = if (isPortActive) "Port: $currentPort" else "Android 11+",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isPortActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Step 1: Developer Options
            WirelessStepItem(
                icon = Icons.Outlined.Settings,
                title = stringResource(R.string.home_wireless_step_dev_options_title),
                description = stringResource(R.string.home_wireless_step_dev_options_desc),
                actionButton = {
                    OutlinedButton(
                        onClick = onOpenDevSettings,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.home_wireless_open_dev_options), style = MaterialTheme.typography.labelMedium)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(10.dp))

            // Step 2: Pairing
            WirelessStepItem(
                icon = Icons.Outlined.Wifi,
                title = stringResource(R.string.home_wireless_step_pairing_title),
                description = stringResource(R.string.home_wireless_step_pairing_desc),
                actionButton = {
                    FilledTonalButton(
                        onClick = onPair,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Wifi,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.home_wireless_btn_pair), style = MaterialTheme.typography.labelMedium)
                    }
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
            Spacer(modifier = Modifier.height(10.dp))

            // Step 3: Start Service
            WirelessStepItem(
                icon = Icons.Outlined.PlayArrow,
                title = stringResource(R.string.home_wireless_step_start_title),
                description = stringResource(R.string.home_wireless_step_start_desc),
                actionButton = {
                    Button(
                        onClick = onStart,
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.home_wireless_btn_start), style = MaterialTheme.typography.labelMedium)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
            TextButton(
                onClick = onGuide,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Outlined.HelpOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.home_wireless_adb_view_guide_button))
            }
        }
    }
}

@Composable
private fun WirelessStepItem(
    icon: ImageVector,
    title: String,
    description: String,
    actionButton: @Composable () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            modifier = Modifier.size(32.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        actionButton()
    }
}

@Composable
private fun RootStartCard(
    isRestart: Boolean,
    onStart: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_tab_root),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.home_root_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Root",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.home_start_root_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isRestart) Icons.Outlined.Refresh else Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isRestart) stringResource(R.string.home_root_button_restart) else stringResource(R.string.home_root_button_start)
                )
            }
        }
    }
}

@Composable
private fun ComputerAdbStartCard(
    onCopyCommand: () -> Unit,
    onViewCommandDialog: () -> Unit,
    onGuide: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Computer,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.home_tab_computer),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.home_adb_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = "ADB",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = stringResource(R.string.home_start_adb_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Starter.adbCommand,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onCopyCommand,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.home_command_copy))
                }
                OutlinedButton(
                    onClick = onViewCommandDialog,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.home_adb_button_view_command))
                }
            }
        }
    }
}

@Composable
private fun WarningBanner(
    title: String,
    summary: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        ),
        shape = MaterialTheme.shapes.extraLarge,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun LinkRow(label: String, url: String) {
    val context = LocalContext.current
    Text(
        text = label,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium
    )
    Text(
        text = url,
        modifier = Modifier.clickable {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
        },
        color = MaterialTheme.colorScheme.secondary,
        style = MaterialTheme.typography.bodySmall
    )
}

private enum class HomeDialog {
    About,
    Stop,
    AdbCommand
}
