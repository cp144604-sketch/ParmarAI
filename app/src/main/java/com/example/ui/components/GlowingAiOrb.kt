package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicNone
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CobaltGlow
import com.example.ui.theme.CrispIce
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.ObsidianBlack
import kotlin.math.cos
import kotlin.math.sin

enum class OrbVisualState {
    STANDBY,
    LISTENING,
    THINKING,
    SPEAKING,
    OFFLINE_IDLE
}

@Composable
fun GlowingAiOrb(
    state: OrbVisualState,
    audioLevel: Float,
    size: Dp = 220.dp,
    testTag: String = "ai_voice_orb",
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "orb_transition")

    val rotationOuter by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbVisualState.THINKING -> 2600
                    OrbVisualState.LISTENING, OrbVisualState.SPEAKING -> 5000
                    else -> 12000
                },
                easing = LinearEasing
            )
        ),
        label = "outer_rot"
    )

    val rotationInner by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (state) {
                    OrbVisualState.THINKING -> 1900
                    OrbVisualState.SPEAKING -> 3800
                    else -> 9000
                },
                easing = LinearEasing
            )
        ),
        label = "inner_rot"
    )

    val breathe by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathe"
    )

    val reactiveBoost by animateFloatAsState(
        targetValue = audioLevel.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = 380f),
        label = "audio_boost"
    )

    val primaryGlowColor = when (state) {
        OrbVisualState.OFFLINE_IDLE -> Color(0xFF4BA3FF)
        OrbVisualState.SPEAKING -> NeonCyan
        OrbVisualState.LISTENING -> ElectricBlue
        OrbVisualState.THINKING -> CobaltGlow
        OrbVisualState.STANDBY -> ElectricBlue
    }

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(size)
            .testTag(testTag)
            .semantics { contentDescription = "PARMAR AI Voice Orb" }
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(bounded = false, color = ElectricBlue),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = this.size.minDimension
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val baseRadius = minDim * 0.34f * breathe + (minDim * 0.08f * reactiveBoost)

            // Outer ambient radial halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlowColor.copy(alpha = 0.38f + 0.25f * reactiveBoost),
                        CobaltGlow.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = minDim * 0.50f
                ),
                center = center,
                radius = minDim * 0.50f
            )

            // Audio-reactive ripple ring
            if (state == OrbVisualState.LISTENING || state == OrbVisualState.SPEAKING) {
                drawCircle(
                    color = NeonCyan.copy(alpha = 0.28f + 0.3f * reactiveBoost),
                    radius = baseRadius * (1.24f + 0.16f * reactiveBoost),
                    center = center,
                    style = Stroke(width = 2.5.dp.toPx())
                )
            }

            // Rotating segmented futuristic arcs
            rotate(degrees = rotationOuter, pivot = center) {
                val arcRadius = minDim * 0.42f
                val arcSize = Size(arcRadius * 2f, arcRadius * 2f)
                val topLeft = Offset(center.x - arcRadius, center.y - arcRadius)

                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(
                            Color.Transparent,
                            primaryGlowColor.copy(alpha = 0.85f),
                            NeonCyan,
                            Color.Transparent
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = CobaltGlow.copy(alpha = 0.65f),
                    startAngle = 195f,
                    sweepAngle = 105f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Counter-rotating neural orbital nodes
            rotate(degrees = rotationInner, pivot = center) {
                val innerArcRadius = minDim * 0.36f
                val innerSize = Size(innerArcRadius * 2f, innerArcRadius * 2f)
                val innerTopLeft = Offset(center.x - innerArcRadius, center.y - innerArcRadius)

                drawArc(
                    color = NeonCyan.copy(alpha = 0.45f),
                    startAngle = 30f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = ElectricBlue.copy(alpha = 0.45f),
                    startAngle = 210f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = innerTopLeft,
                    size = innerSize,
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )

                for (angleDeg in listOf(30f, 150f, 270f)) {
                    val rad = Math.toRadians(angleDeg.toDouble())
                    val nodeCenter = Offset(
                        center.x + (innerArcRadius * cos(rad)).toFloat(),
                        center.y + (innerArcRadius * sin(rad)).toFloat()
                    )
                    drawCircle(
                        color = NeonCyan,
                        radius = 3.dp.toPx(),
                        center = nodeCenter
                    )
                }
            }

            // Core glass sphere
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        CobaltGlow.copy(alpha = 0.9f),
                        Color(0xFF0A1E46),
                        ObsidianBlack
                    ),
                    center = Offset(center.x - baseRadius * 0.2f, center.y - baseRadius * 0.25f),
                    radius = baseRadius * 1.15f
                ),
                center = center,
                radius = baseRadius * 0.85f
            )

            // Inner diamond frame hint
            val dRadius = baseRadius * 0.56f
            val diamondPath = Path().apply {
                moveTo(center.x, center.y - dRadius)
                lineTo(center.x + dRadius * 0.85f, center.y)
                lineTo(center.x, center.y + dRadius)
                lineTo(center.x - dRadius * 0.85f, center.y)
                close()
            }
            drawPath(
                path = diamondPath,
                color = primaryGlowColor.copy(alpha = 0.26f),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }

        Icon(
            imageVector = when (state) {
                OrbVisualState.LISTENING -> Icons.Rounded.Mic
                OrbVisualState.SPEAKING, OrbVisualState.THINKING -> Icons.Rounded.GraphicEq
                else -> Icons.Rounded.MicNone
            },
            contentDescription = "Microphone Orb Trigger",
            tint = CrispIce,
            modifier = Modifier.size(size * 0.22f)
        )
    }
}
