package moe.shizuku.manager.home

import android.content.Intent
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForwardIos
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material.icons.outlined.Wifi
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import moe.shizuku.manager.R
import moe.shizuku.manager.hide.HideAppsActivity
import moe.shizuku.manager.hide.HideAppsManager
import moe.shizuku.manager.model.ServiceStatus
import moe.shizuku.manager.starter.Starter
import moe.shizuku.manager.ui.theme.ShizukuComposeTheme
import moe.shizuku.manager.utils.EnvironmentUtils
import moe.shizuku.manager.utils.UserHandleCompat

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
    onOpenLearnMore: () -> Unit
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
            onOpenLearnMore = onOpenLearnMore
        )
    }
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
    onOpenLearnMore: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    var dialog by remember { mutableStateOf<HomeDialog?>(null) }
    val resolvedStatus = status ?: ServiceStatus()
    val running = resolvedStatus.isRunning
    val isRoot = EnvironmentUtils.isRooted()
    val hiddenCount = remember(context) {
        HideAppsManager.getHiddenPackages(context).size
    }

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

            // 4. Start Shizuku Section
            item {
                SectionHeader(title = stringResource(R.string.home_section_start_methods))
            }

            // Wireless Debugging Card (Android 11+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R || EnvironmentUtils.getAdbTcpPort() > 0) {
                item {
                    StartMethodCard(
                        icon = Icons.Outlined.Wifi,
                        title = stringResource(R.string.home_wireless_adb_title),
                        tag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) "Android 11+" else null,
                        summary = stringResource(R.string.home_start_wireless_summary),
                        primaryLabel = stringResource(R.string.home_root_button_start),
                        secondaryLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) stringResource(R.string.adb_pairing) else null,
                        tertiaryLabel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) stringResource(R.string.home_wireless_adb_view_guide_button) else null,
                        onPrimary = onStartWirelessAdb,
                        onSecondary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) onPairWireless else null,
                        onTertiary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) onOpenWirelessGuide else null
                    )
                }
            }

            // Root Access Card
            if (UserHandleCompat.myUserId() == 0 && (isRoot || !running)) {
                val rootRestart = running && resolvedStatus.uid == 0
                item {
                    StartMethodCard(
                        icon = Icons.Outlined.PlayArrow,
                        title = stringResource(R.string.home_root_title),
                        tag = "Root",
                        summary = stringResource(R.string.home_start_root_summary),
                        primaryLabel = if (rootRestart) stringResource(R.string.home_root_button_restart) else stringResource(R.string.home_root_button_start),
                        onPrimary = if (rootRestart) onRestartRoot else onStartRoot
                    )
                }
            }

            // Computer ADB Card
            item {
                StartMethodCard(
                    icon = Icons.Outlined.Computer,
                    title = stringResource(R.string.home_adb_title),
                    tag = "ADB",
                    summary = stringResource(R.string.home_start_adb_summary),
                    primaryLabel = stringResource(R.string.home_adb_button_view_command),
                    onPrimary = { dialog = HomeDialog.AdbCommand }
                )
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
                            url = "https://github.com/Towartz/Shizuku-mod"
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
                confirmButton = {
                    TextButton(onClick = {
                        dialog = null
                        onStopService()
                    }) {
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
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (status.uid == 0) {
                        TextButton(onClick = onRestartRoot) {
                            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.home_root_button_restart))
                        }
                    }
                    TextButton(onClick = onStopService) {
                        Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
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
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun StartMethodCard(
    icon: ImageVector,
    title: String,
    tag: String? = null,
    summary: String,
    primaryLabel: String? = null,
    secondaryLabel: String? = null,
    tertiaryLabel: String? = null,
    onPrimary: (() -> Unit)? = null,
    onSecondary: (() -> Unit)? = null,
    onTertiary: (() -> Unit)? = null
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.size(14.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (tag != null) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (primaryLabel != null || secondaryLabel != null || tertiaryLabel != null) {
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    tertiaryLabel?.let { label ->
                        OutlinedButton(onClick = { onTertiary?.invoke() }) {
                            Text(label)
                        }
                    }
                    secondaryLabel?.let { label ->
                        FilledTonalButton(onClick = { onSecondary?.invoke() }) {
                            Text(label)
                        }
                    }
                    primaryLabel?.let { label ->
                        Button(onClick = { onPrimary?.invoke() }) {
                            Text(label)
                        }
                    }
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
