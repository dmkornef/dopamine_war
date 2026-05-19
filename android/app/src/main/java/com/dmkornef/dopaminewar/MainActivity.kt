package com.dmkornef.dopaminewar

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.dmkornef.dopaminewar.ui.theme.DopamineWarTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            DopamineWarTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF101010)
                ) {
                    MainScreen()
                }
            }
        }
    }
}

@Composable
private fun MainScreen() {
    val blockedApps = remember {
        defaultBlockedApps()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = "DopamineWar",
            color = Color.White,
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Selected apps are locked until access is approved.",
            color = Color(0xFF9E9E9E),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Blocked apps",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            blockedApps.forEach { app ->
                BlockedAppRow(app = app)
            }
        }
    }
}

@Composable
private fun BlockedAppRow(app: BlockedApp) {
    val context = LocalContext.current
    val appInfo = remember(app.packageName) {
        loadInstalledAppInfo(
            context = context,
            packageName = app.packageName,
            fallbackName = app.fallbackName
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF1B1B1B))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (appInfo.iconBitmap != null) {
            Image(
                bitmap = appInfo.iconBitmap.asImageBitmap(),
                contentDescription = appInfo.label,
                modifier = Modifier.size(48.dp)
            )
        } else {
            AppIconPlaceholder(label = appInfo.label)
        }

        Text(
            text = appInfo.label,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )

        Text(
            text = "LOCKED",
            color = Color(0xFFFF4D4D),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun AppIconPlaceholder(label: String) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2A2A2A)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
    }
}

private fun defaultBlockedApps(): List<BlockedApp> {
    return listOf(
        BlockedApp(
            packageName = "com.google.android.youtube",
            fallbackName = "YouTube"
        ),
        BlockedApp(
            packageName = "com.instagram.android",
            fallbackName = "Instagram"
        )
    )
}

private fun loadInstalledAppInfo(
    context: Context,
    packageName: String,
    fallbackName: String
): InstalledAppInfo {
    val packageManager = context.packageManager

    return try {
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val label = packageManager.getApplicationLabel(applicationInfo).toString()
        val icon = packageManager.getApplicationIcon(applicationInfo)

        InstalledAppInfo(
            label = label,
            iconBitmap = icon.toSafeBitmap()
        )
    } catch (_: PackageManager.NameNotFoundException) {
        InstalledAppInfo(
            label = fallbackName,
            iconBitmap = null
        )
    }
}

private fun Drawable.toSafeBitmap(): Bitmap? {
    return try {
        toBitmap(width = 96, height = 96)
    } catch (_: Exception) {
        null
    }
}

@Immutable
private data class BlockedApp(
    val packageName: String,
    val fallbackName: String
)

@Immutable
private data class InstalledAppInfo(
    val label: String,
    val iconBitmap: Bitmap?
)