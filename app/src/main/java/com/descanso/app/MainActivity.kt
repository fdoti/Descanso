package com.descanso.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.descanso.app.data.AppInfo
import com.descanso.app.data.BlockPrefs
import com.descanso.app.data.loadInstalledApps
import com.descanso.app.service.BlockAccessibilityService
import com.descanso.app.service.BlockTimerService
import com.descanso.app.ui.theme.DescansoTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            DescansoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DescansoApp()
                }
            }
        }
    }
}

private fun isAccessibilityEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/${BlockAccessibilityService::class.java.name}"
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false
    return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
}

@Composable
private fun OnResume(action: () -> Unit) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) action()
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
}

@Composable
fun DescansoApp() {
    val context = LocalContext.current

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    val selected: SnapshotStateMap<String, Boolean> = remember { mutableStateMapOf() }
    var minutes by remember { mutableFloatStateOf(30f) }
    var query by remember { mutableStateOf("") }
    var accessibilityOn by remember { mutableStateOf(false) }
    var endTime by remember { mutableLongStateOf(0L) }

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(Unit) {
        BlockPrefs.blockedPackages(context).forEach { selected[it] = true }
        endTime = BlockPrefs.endTime(context)
        apps = loadInstalledApps(context)
        loading = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    OnResume {
        accessibilityOn = isAccessibilityEnabled(context)
        endTime = BlockPrefs.endTime(context)
    }

    val active = endTime > System.currentTimeMillis()

    if (active) {
        ActiveScreen(
            endTime = endTime,
            blockedCount = selected.count { it.value },
            onExpired = { endTime = 0 },
            onStop = {
                BlockPrefs.clear(context)
                context.stopService(Intent(context, BlockTimerService::class.java))
                endTime = 0
            }
        )
    } else {
        ConfigScreen(
            apps = apps,
            loading = loading,
            selected = selected,
            minutes = minutes,
            onMinutesChange = { minutes = it },
            query = query,
            onQueryChange = { query = it },
            accessibilityOn = accessibilityOn,
            onOpenAccessibility = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            },
            onStart = {
                val pkgs = selected.filterValues { it }.keys.toSet()
                if (pkgs.isEmpty()) {
                    Toast.makeText(context, "Elegí al menos una app para bloquear", Toast.LENGTH_SHORT).show()
                    return@ConfigScreen
                }
                if (!isAccessibilityEnabled(context)) {
                    Toast.makeText(context, "Activá el servicio “Descanso” en Accesibilidad", Toast.LENGTH_LONG).show()
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                    return@ConfigScreen
                }
                val end = System.currentTimeMillis() + (minutes.toLong() * 60_000L)
                BlockPrefs.setBlock(context, pkgs, end)
                ContextCompat.startForegroundService(
                    context, Intent(context, BlockTimerService::class.java)
                )
                endTime = end
            }
        )
    }
}

@Composable
private fun ActiveScreen(
    endTime: Long,
    blockedCount: Int,
    onExpired: () -> Unit,
    onStop: () -> Unit,
) {
    var remaining by remember { mutableLongStateOf(endTime - System.currentTimeMillis()) }
    LaunchedEffect(endTime) {
        while (true) {
            remaining = endTime - System.currentTimeMillis()
            if (remaining <= 0) {
                onExpired()
                break
            }
            delay(1000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Descanso en curso", style = MaterialTheme.typography.titleLarge)
        Text(
            BlockTimerService.format(remaining.coerceAtLeast(0)),
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(vertical = 16.dp)
        )
        Text(
            "$blockedCount app(s) bloqueada(s)",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(40.dp))
        OutlinedButton(onClick = onStop) {
            Text("Terminar descanso")
        }
    }
}

@Composable
private fun ConfigScreen(
    apps: List<AppInfo>,
    loading: Boolean,
    selected: SnapshotStateMap<String, Boolean>,
    minutes: Float,
    onMinutesChange: (Float) -> Unit,
    query: String,
    onQueryChange: (String) -> Unit,
    accessibilityOn: Boolean,
    onOpenAccessibility: () -> Unit,
    onStart: () -> Unit,
) {
    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter { it.label.contains(query, ignoreCase = true) }
    }
    val selectedCount = selected.count { it.value }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item {
            Text(
                "Descanso",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 24.dp, bottom = 4.dp)
            )
            Text(
                "Bloqueá apps por un rato para desconectar.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (!accessibilityOn) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Falta activar el permiso de Accesibilidad",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            "Sin esto Descanso no puede bloquear las apps. Tocá el botón y activá “Descanso”.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        Button(onClick = onOpenAccessibility) {
                            Text("Abrir ajustes de Accesibilidad")
                        }
                    }
                }
            }
        }

        item {
            Text(
                "Duración: ${minutes.toInt()} min",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Slider(
                value = minutes,
                onValueChange = onMinutesChange,
                valueRange = 5f..240f,
                steps = 46,
            )
            Row(modifier = Modifier.padding(bottom = 12.dp)) {
                listOf(15, 30, 60, 120).forEach { preset ->
                    FilterChip(
                        selected = minutes.toInt() == preset,
                        onClick = { onMinutesChange(preset.toFloat()) },
                        label = { Text("$preset") },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                placeholder = { Text("Buscar app…") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            )
            Text(
                "Apps a bloquear ($selectedCount elegidas)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (loading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
        } else {
            items(filtered, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    checked = selected[app.packageName] == true,
                    onToggle = { checked ->
                        if (checked) selected[app.packageName] = true
                        else selected.remove(app.packageName)
                    }
                )
            }
        }

        item {
            Spacer(Modifier.height(88.dp))
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Button(
            onClick = onStart,
            enabled = selectedCount > 0,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .height(52.dp)
        ) {
            Text("Empezar descanso (${minutes.toInt()} min)")
        }
    }
}

@Composable
private fun AppRow(
    app: AppInfo,
    checked: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!checked) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(Color(0x11000000), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Image(
                bitmap = app.icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp)
            )
        }
        Text(
            app.label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        )
        Checkbox(checked = checked, onCheckedChange = onToggle)
    }
}
