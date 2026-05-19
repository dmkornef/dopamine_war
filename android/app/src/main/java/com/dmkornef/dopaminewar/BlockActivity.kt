package com.dmkornef.dopaminewar

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import kotlin.math.PI
import kotlin.math.sin
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
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
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.dmkornef.dopaminewar.ui.theme.DopamineWarTheme

class BlockActivity : ComponentActivity() {

    private var blockedPackageName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        blockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    leaveBlockedApp()
                }
            }
        )

        setContent {
            DopamineWarTheme {
                BlockScreen(
                    blockedPackageName = blockedPackageName,
                    onRequestAccess = {
                        // Telegram request will be connected here later.
                        leaveBlockedApp()
                    },
                    onLeaveApp = {
                        leaveBlockedApp()
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        val newBlockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE).orEmpty()

        if (newBlockedPackageName.isNotBlank() && newBlockedPackageName != blockedPackageName) {
            blockedPackageName = newBlockedPackageName

            setContent {
                DopamineWarTheme {
                    BlockScreen(
                        blockedPackageName = blockedPackageName,
                        onRequestAccess = {
                            leaveBlockedApp()
                        },
                        onLeaveApp = {
                            leaveBlockedApp()
                        }
                    )
                }
            }
        }
    }

    private fun leaveBlockedApp() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"

        fun createIntent(context: Context, blockedPackageName: String): Intent {
            return Intent(context, BlockActivity::class.java).apply {
                putExtra(EXTRA_BLOCKED_PACKAGE, blockedPackageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        }
    }
}

@Composable
private fun BlockScreen(
    blockedPackageName: String,
    onRequestAccess: () -> Unit,
    onLeaveApp: () -> Unit
) {
    val context = LocalContext.current
    val appInfo = remember(blockedPackageName) {
        loadBlockedAppUiInfo(
            context = context,
            packageName = blockedPackageName
        )
    }

    val phrase = remember(blockedPackageName) {
        randomBlockPhrase(appInfo.label)
    }

    var selectedMinutes by remember {
        mutableIntStateOf(5)
    }

    var reason by remember {
        mutableStateOf("")
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0C0C0C)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            FakeBlurredAppBackground(appInfo = appInfo)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BlockCard(
                    appInfo = appInfo,
                    phrase = phrase,
                    selectedMinutes = selectedMinutes,
                    onSelectedMinutesChange = { selectedMinutes = it },
                    reason = reason,
                    onReasonChange = { reason = it },
                    onRequestAccess = onRequestAccess,
                    onLeaveApp = onLeaveApp
                )
            }
        }
    }
}

@Composable
private fun FakeBlurredAppBackground(appInfo: BlockedAppUiInfo) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appInfo.backgroundColor)
    ) {
        if (appInfo.backgroundImageResId != null) {
            Image(
                painter = painterResource(id = appInfo.backgroundImageResId),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(1f),
                contentScale = ContentScale.Crop
            )
        } else {
            DefaultGlassWavesBackground()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x33000000))
        )
    }
}

@Composable
private fun DefaultGlassWavesBackground() {
    val transition = rememberInfiniteTransition(label = "glass_waves_transition")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 4200,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "glass_waves_phase"
    )

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF06111F),
                        Color(0xFF071A2E),
                        Color(0xFF0B2540),
                        Color(0xFF050A12)
                    )
                )
            )
    ) {
        drawCircle(
            color = Color(0xFF225CFF).copy(alpha = 0.18f),
            radius = size.minDimension * 0.42f,
            center = center.copy(
                x = size.width * 0.18f,
                y = size.height * 0.22f
            )
        )

        drawCircle(
            color = Color(0xFF25D6FF).copy(alpha = 0.10f),
            radius = size.minDimension * 0.34f,
            center = center.copy(
                x = size.width * 0.88f,
                y = size.height * 0.64f
            )
        )

        repeat(6) { waveIndex ->
            val path = Path()
            val baseY = size.height * (0.18f + waveIndex * 0.135f)
            val amplitude = 18f + waveIndex * 3f
            val frequency = 1.4f + waveIndex * 0.18f
            val phaseOffset = phase * 2f * PI.toFloat() + waveIndex * 0.7f

            val steps = 90

            for (step in 0..steps) {
                val x = size.width * step / steps
                val y = baseY + sin(
                    x / size.width * 2f * PI.toFloat() * frequency + phaseOffset
                ) * amplitude

                if (step == 0) {
                    path.moveTo(x, y)
                } else {
                    path.lineTo(x, y)
                }
            }

            drawPath(
                path = path,
                color = Color.White.copy(alpha = 0.055f),
                style = Stroke(
                    width = 18f,
                    cap = StrokeCap.Round
                )
            )

            drawPath(
                path = path,
                color = Color(0xFF69D8FF).copy(alpha = 0.045f),
                style = Stroke(
                    width = 7f,
                    cap = StrokeCap.Round
                )
            )
        }
    }
}

@Composable
private fun BlockCard(
    appInfo: BlockedAppUiInfo,
    phrase: String,
    selectedMinutes: Int,
    onSelectedMinutesChange: (Int) -> Unit,
    reason: String,
    onReasonChange: (String) -> Unit,
    onRequestAccess: () -> Unit,
    onLeaveApp: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(Color(0xEE171717))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (appInfo.iconBitmap != null) {
            Image(
                bitmap = appInfo.iconBitmap.asImageBitmap(),
                contentDescription = appInfo.label,
                modifier = Modifier.size(72.dp)
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = "${appInfo.label} is locked",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = phrase,
            color = Color(0xFFB5B5B5),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            DurationChip(
                minutes = 5,
                selectedMinutes = selectedMinutes,
                onSelectedMinutesChange = onSelectedMinutesChange
            )

            DurationChip(
                minutes = 10,
                selectedMinutes = selectedMinutes,
                onSelectedMinutesChange = onSelectedMinutesChange
            )

            DurationChip(
                minutes = 15,
                selectedMinutes = selectedMinutes,
                onSelectedMinutesChange = onSelectedMinutesChange
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = reason,
            onValueChange = onReasonChange,
            modifier = Modifier.fillMaxWidth(),
            label = {
                Text(text = "Reason, optional")
            },
            singleLine = false,
            maxLines = 3
        )

        Spacer(modifier = Modifier.height(22.dp))

        Button(
            onClick = onRequestAccess,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = "Request access")
        }

        Spacer(modifier = Modifier.height(10.dp))

        Button(
            onClick = onLeaveApp,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text(text = "Leave app")
        }
    }
}

@Composable
private fun DurationChip(
    minutes: Int,
    selectedMinutes: Int,
    onSelectedMinutesChange: (Int) -> Unit
) {
    FilterChip(
        selected = selectedMinutes == minutes,
        onClick = {
            onSelectedMinutesChange(minutes)
        },
        label = {
            Text(text = "$minutes min")
        }
    )
}

private fun loadBlockedAppUiInfo(
    context: Context,
    packageName: String
): BlockedAppUiInfo {
    val fallbackName = fallbackNameForPackage(packageName)

    return try {
        val packageManager = context.packageManager
        val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
        val label = packageManager.getApplicationLabel(applicationInfo).toString()
        val icon = packageManager.getApplicationIcon(applicationInfo)

        BlockedAppUiInfo(
            packageName = packageName,
            label = label,
            iconBitmap = icon.toSafeBitmap(),
            backgroundColor = backgroundColorForPackage(packageName),
            backgroundImageResId = backgroundImageForPackage(packageName)
        )
    } catch (_: PackageManager.NameNotFoundException) {
        BlockedAppUiInfo(
            packageName = packageName,
            label = fallbackName,
            iconBitmap = null,
            backgroundColor = backgroundColorForPackage(packageName),
            backgroundImageResId = backgroundImageForPackage(packageName)
        )
    }
}

private fun randomBlockPhrase(appLabel: String): String {
    val phrases = listOf(
        "Nice try.",
        "Dopamine trap detected.",
        "Your brain asked. We said no.",
        "$appLabel can wait.",
        "Access requires approval.",
        "Caught before the scroll hole."
    )

    return phrases.random()
}

private fun fallbackNameForPackage(packageName: String): String {
    return when (packageName) {
        "com.google.android.youtube" -> "YouTube"
        "com.instagram.android" -> "Instagram"
        else -> "This app"
    }
}

private fun backgroundColorForPackage(packageName: String): Color {
    return when (packageName) {
        "com.google.android.youtube" -> Color(0xFF4A0505)
        "com.instagram.android" -> Color(0xFF39104A)
        else -> Color(0xFF111111)
    }
}

private fun backgroundImageForPackage(packageName: String): Int? {
    return when (packageName) {
        "com.google.android.youtube" -> R.drawable.bg_youtube
        "com.instagram.android" -> R.drawable.bg_instagram
        else -> null
    }
}

private fun Drawable.toSafeBitmap(): Bitmap? {
    return try {
        toBitmap(width = 160, height = 160)
    } catch (_: Exception) {
        null
    }
}

@Immutable
private data class BlockedAppUiInfo(
    val packageName: String,
    val label: String,
    val iconBitmap: Bitmap?,
    val backgroundColor: Color,
    val backgroundImageResId: Int?
)