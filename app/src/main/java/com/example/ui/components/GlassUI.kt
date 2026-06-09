package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ripple
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

// Aesthetic iOS 26 Colors
object GlassColors {
    val RoyalPurple = Color(0xFF6E56CF)
    val VioletNeon = Color(0xFF9E00FF)
    val CyberTeal = Color(0xFF00F0FF)
    val BubblegumPink = Color(0xFFEE46BC)
    val AmberGlow = Color(0xFFFFB000)
    val EmeraldGreen = Color(0xFF30D158)
    val SoftRed = Color(0xFFFF453A)

    // Dark volcanic base
    val DarkBaseGrad1 = Color(0xFF050508)
    val DarkBaseGrad2 = Color(0xFF0A0A10)
    val DarkBaseGrad3 = Color(0xFF020204)

    // Light crystal base
    val LightBaseGrad1 = Color(0xFFF3F0FA)
    val LightBaseGrad2 = Color(0xFFE4E0F3)
    val LightBaseGrad3 = Color(0xFFFFFFFF)
}

/**
 * Animated Gradient Mesh Background that slowly shifts colors over time mimicking iOS 26 fluid screens.
 */
@Composable
fun GlassBackground(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mesh_bg")
    
    // Animate angles for fluid motion
    val angle1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = SpecularSprings.bgSlowTween(25000),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle1"
    )

    val angle2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = SpecularSprings.bgSlowTween(35000),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle2"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height

                // Draw base background color
                val baseColors = if (isDark) {
                    listOf(GlassColors.DarkBaseGrad1, GlassColors.DarkBaseGrad2, GlassColors.DarkBaseGrad3)
                } else {
                    listOf(GlassColors.LightBaseGrad1, GlassColors.LightBaseGrad2, GlassColors.LightBaseGrad3)
                }
                
                drawRect(
                    brush = Brush.linearGradient(
                        colors = baseColors,
                        start = Offset(0f, 0f),
                        end = Offset(width, height)
                    )
                )

                // Shifting Mesh Gradient Circle 1 (Purple glow - bg-purple-600/20)
                val cx1 = width * (0.5f + 0.35f * cos(angle1))
                val cy1 = height * (0.3f + 0.25f * sin(angle1))
                val r1 = width * 0.75f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF7C3AED).copy(alpha = if (isDark) 0.20f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(cx1, cy1),
                        radius = r1
                    )
                )

                // Shifting Mesh Gradient Circle 2 (Blue glow - bg-blue-600/20)
                val cx2 = width * (0.4f + 0.3f * sin(angle2))
                val cy2 = height * (0.7f + 0.25f * cos(angle2))
                val r2 = width * 0.85f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2563EB).copy(alpha = if (isDark) 0.20f else 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(cx2, cy2),
                        radius = r2
                    )
                )

                // Shifting Mesh Gradient Circle 3 (Pink glow - bg-pink-500/10)
                val cx3 = width * (0.8f + 0.2f * cos(angle2 + angle1))
                val cy3 = height * (0.4f + 0.2f * sin(angle2 - angle1))
                val r3 = width * 0.6f
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFEC4899).copy(alpha = if (isDark) 0.10f else 0.06f),
                            Color.Transparent
                        ),
                        center = Offset(cx3, cy3),
                        radius = r3
                    )
                )
            },
        content = content
    )
}

object SpecularSprings {
    fun bgSlowTween(duration: Int): TweenSpec<Float> = tween(
        durationMillis = duration,
        easing = LinearEasing
    )
}

/**
 * Semi-transparent premium Liquid Glass block with shifting dynamic border and shadow.
 */
@Composable
fun GlassCard(
    isDark: Boolean,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 24.dp,
    borderWidth: Dp = 1.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_border")
    
    // Animate color rotation of the oil-slick iridescent border
    val flowProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "flow_progress"
    )

    // Compute dynamic border colors for oil-slick visual with subtle glass shimmer
    val iridescentColors = if (isDark) {
        listOf(
            Color.White.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.06f),
            Color.White.copy(alpha = 0.15f),
            GlassColors.CyberTeal.copy(alpha = 0.08f),
            Color.White.copy(alpha = 0.12f)
        )
    } else {
        listOf(
            Color.Black.copy(alpha = 0.08f),
            Color.Black.copy(alpha = 0.04f),
            Color.Black.copy(alpha = 0.10f)
        )
    }

    val currentBorderBrush = Brush.linearGradient(
        colors = iridescentColors,
        start = Offset(flowProgress * 500f, 0f),
        end = Offset((flowProgress + 1f) * 500f, 500f),
        tileMode = TileMode.Repeated
    )

    // Card background color - extremely transparent premium backdrop glass (bg-white/5)
    val glassBgColor = if (isDark) {
        Color.White.copy(alpha = 0.05f) // Matches exact bg-white/5 from spec!
    } else {
        Color(0xFFFFFFFF).copy(alpha = 0.65f) // Crisp Crystal Glass
    }

    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color(0xFF6E56CF).copy(alpha = 0.08f)

    Column(
        modifier = modifier
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(cornerRadius),
                clip = false,
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .background(
                color = glassBgColor,
                shape = RoundedCornerShape(cornerRadius)
            )
            .border(
                width = borderWidth,
                brush = currentBorderBrush,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(20.dp),
        content = content
    )
}

/**
 * Iridescent Tactile Glass Button that responds with a clean spring bounce.
 */
@Composable
fun GlassButton(
    onClick: () -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    testTag: String = "",
    enabled: Boolean = true,
    accentColor: Color = GlassColors.VioletNeon,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Spring scaling feedback
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.6f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "btn_scale"
    )

    val borderColors = if (enabled) {
        listOf(accentColor.copy(alpha = 0.8f), GlassColors.CyberTeal.copy(alpha = 0.8f))
    } else {
        listOf(Color.Gray.copy(alpha = 0.3f), Color.LightGray.copy(alpha = 0.2f))
    }

    val btnBgColor = if (!enabled) {
        if (isDark) Color(0xFF1E1E2E).copy(alpha = 0.3f) else Color(0xFFE0E0E0).copy(alpha = 0.4f)
    } else if (isDark) {
        Color(0xFF1E1035).copy(alpha = 0.72f)
    } else {
        Color(0xFFF1EDFC).copy(alpha = 0.75f)
    }

    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(16.dp))
            .background(btnBgColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(borderColors),
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(color = accentColor),
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .testTag(testTag),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * Beautiful Glass Badge for status (Paid, Partial, Due).
 */
@Composable
fun GlassBadge(
    text: String,
    statusType: String, // "Paid", "Partial", "Due"
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, borderColor) = when (statusType) {
        "Paid" -> Triple(
            GlassColors.EmeraldGreen.copy(alpha = 0.15f),
            if (isDark) Color(0xFF66FF99) else Color(0xFF1D8F39),
            GlassColors.EmeraldGreen.copy(alpha = 0.4f)
        )
        "Partial" -> Triple(
            GlassColors.AmberGlow.copy(alpha = 0.15f),
            if (isDark) Color(0xFFFFD466) else Color(0xFFB37400),
            GlassColors.AmberGlow.copy(alpha = 0.4f)
        )
        else -> Triple(
            GlassColors.SoftRed.copy(alpha = 0.15f),
            if (isDark) Color(0xFFFF8080) else Color(0xFFD62720),
            GlassColors.SoftRed.copy(alpha = 0.4f)
        )
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
