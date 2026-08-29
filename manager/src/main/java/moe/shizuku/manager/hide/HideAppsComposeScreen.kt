package moe.shizuku.manager.hide

import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.shizuku.manager.R
import moe.shizuku.manager.ui.theme.ShizukuComposeTheme
import moe.shizuku.manager.utils.AppIconCache

private enum class AppFilter {
    ALL,
    HIDDEN_ONLY
}

@Composable
fun HideAppsComposeScreen(
    onNavigateUp: () -> Unit
) {
    ShizukuComposeTheme {
        HideAppsContent(onNavigateUp = onNavigateUp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HideAppsContent(
    onNavigateUp: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(AppFilter.ALL) }
    var showMenu by remember { mutableStateOf(false) }
    var apps by remember { mutableStateOf<List<PackageInfo>?>(null) }
    var isStealthMode by remember { mutableStateOf(StealthModeManager.isStealthModeEnabled(context)) }
    val hiddenStates = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            HideAppsManager.syncAllToService(context)
            val installed = HideAppsManager.getInstalledApps(context)
            val hiddenSet = HideAppsManager.getHiddenPackages(context)
            withContext(Dispatchers.Main) {
                apps = installed
                hiddenSet.forEach { hiddenStates[it] = true }
            }
        }
    }

    val currentApps = apps ?: emptyList()
    val totalHiddenCount = remember(hiddenStates.values.toList()) {
        hiddenStates.count { it.value }
    }

    val filteredApps = remember(currentApps, searchQuery, selectedFilter, hiddenStates.toMap()) {
        currentApps.filter { packageInfo ->
            val isHidden = hiddenStates[packageInfo.packageName] == true

            val matchesFilter = when (selectedFilter) {
                AppFilter.ALL -> true
                AppFilter.HIDDEN_ONLY -> isHidden
            }
            if (!matchesFilter) return@filter false

            if (searchQuery.isBlank()) {
                true
            } else {
                val query = searchQuery.trim().lowercase()
                val pm = context.packageManager
                val label = packageInfo.applicationInfo?.loadLabel(pm)?.toString()?.lowercase() ?: ""
                label.contains(query) || packageInfo.packageName.lowercase().contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text(stringResource(R.string.hide_apps_search_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = null)
                                    }
                                }
                            }
                        )
                    } else {
                        Text(stringResource(R.string.hide_apps_title))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isSearchActive) {
                            isSearchActive = false
                            searchQuery = ""
                        } else {
                            onNavigateUp()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    if (!isSearchActive) {
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(Icons.Outlined.Search, contentDescription = null)
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.hide_apps_action_hide_all)) },
                                onClick = {
                                    showMenu = false
                                    currentApps.forEach { pi ->
                                        hiddenStates[pi.packageName] = true
                                        HideAppsManager.setPackageHidden(
                                            context = context,
                                            packageName = pi.packageName,
                                            hidden = true,
                                            explicitUid = pi.applicationInfo?.uid
                                        )
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.hide_apps_action_unhide_all)) },
                                onClick = {
                                    showMenu = false
                                    currentApps.forEach { pi ->
                                        if (hiddenStates[pi.packageName] == true) {
                                            hiddenStates[pi.packageName] = false
                                            HideAppsManager.setPackageHidden(
                                                context = context,
                                                packageName = pi.packageName,
                                                hidden = false,
                                                explicitUid = pi.applicationInfo?.uid
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        if (apps == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Unified Hero Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
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
                                    imageVector = Icons.Outlined.VisibilityOff,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.size(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.stealth_mode_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.stealth_mode_summary),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(modifier = Modifier.size(12.dp))
                                Switch(
                                    checked = isStealthMode,
                                    onCheckedChange = { checked ->
                                        isStealthMode = checked
                                        StealthModeManager.setStealthModeEnabled(context, checked)
                                    }
                                )
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.hide_apps_stats, totalHiddenCount, currentApps.size),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Filter Chips Row
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == AppFilter.ALL,
                            onClick = { selectedFilter = AppFilter.ALL },
                            label = { Text("${stringResource(R.string.hide_apps_filter_all)} (${currentApps.size})") },
                            leadingIcon = if (selectedFilter == AppFilter.ALL) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = selectedFilter == AppFilter.HIDDEN_ONLY,
                            onClick = { selectedFilter = AppFilter.HIDDEN_ONLY },
                            label = { Text("${stringResource(R.string.hide_apps_filter_hidden)} ($totalHiddenCount)") },
                            leadingIcon = if (selectedFilter == AppFilter.HIDDEN_ONLY) {
                                { Icon(Icons.Outlined.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }

                // Empty State or List
                if (filteredApps.isEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                            ),
                            shape = MaterialTheme.shapes.extraLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.hide_apps_none),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (searchQuery.isNotEmpty() || selectedFilter != AppFilter.ALL) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    TextButton(onClick = {
                                        searchQuery = ""
                                        selectedFilter = AppFilter.ALL
                                    }) {
                                        Text("Clear Filters")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    items(filteredApps, key = { it.packageName }) { packageInfo ->
                        val isHidden = hiddenStates[packageInfo.packageName] == true
                        HideAppCard(
                            packageInfo = packageInfo,
                            isHidden = isHidden,
                            onToggle = { checked ->
                                hiddenStates[packageInfo.packageName] = checked
                                HideAppsManager.setPackageHidden(
                                    context = context,
                                    packageName = packageInfo.packageName,
                                    hidden = checked,
                                    explicitUid = packageInfo.applicationInfo?.uid
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HideAppCard(
    packageInfo: PackageInfo,
    isHidden: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val pm = context.packageManager
    val appInfo = packageInfo.applicationInfo
    val label = remember(packageInfo) {
        appInfo?.loadLabel(pm)?.toString() ?: packageInfo.packageName
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isHidden) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        ),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = { onToggle(!isHidden) },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            appInfo?.let {
                HideAppIcon(applicationInfo = it, size = 44.dp)
            }
            Spacer(modifier = Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = packageInfo.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isHidden) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = stringResource(R.string.hide_apps_badge_hidden),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.size(8.dp))
            Switch(
                checked = isHidden,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun HideAppIcon(applicationInfo: ApplicationInfo, size: Dp) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val iconSizePx = with(density) { size.roundToPx() }

    val iconBitmap by produceState<Bitmap?>(initialValue = null, key1 = applicationInfo, key2 = iconSizePx) {
        value = withContext(Dispatchers.IO) {
            AppIconCache.getOrLoadBitmap(context, applicationInfo, 0, iconSizePx)
        }
    }

    if (iconBitmap != null) {
        Image(
            bitmap = iconBitmap!!.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(size)
                .clip(MaterialTheme.shapes.medium)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(MaterialTheme.shapes.medium)
        )
    }
}
