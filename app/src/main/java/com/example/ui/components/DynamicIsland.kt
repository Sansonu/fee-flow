package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.IslandState
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * iOS 26 Dynamic Island Interactive bottom glass navigation bar with spring state-switching.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIslandNav(
    currentScreen: AppScreen,
    islandState: IslandState,
    isDark: Boolean,
    onScreenSelected: (AppScreen) -> Unit,
    onRecordPaymentClick: () -> Unit,
    onOverdueClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "island_effects")
    
    // 1. Subtle internal breathing glow pulse
    val breathingGlowFactor by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(22000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathing_glow"
    )

    // 2. Oil-slick border progress animation
    val borderRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "border_rotation"
    )

    // Mode-aware size calculation with bouncy springs
    // Standard springs with 0.58f damping for slight interactive wiggle
    val islandSpring = spring<Dp>(
        dampingRatio = 0.58f,
        stiffness = Spring.StiffnessLow
    )

    val (islandWidth, islandHeight) = when (islandState) {
        is IslandState.Idle -> Pair(330.dp, 64.dp)
        is IslandState.OverdueAlert -> Pair(345.dp, 70.dp)
        is IslandState.ExpandedStats -> Pair(340.dp, 130.dp)
        is IslandState.Success -> Pair(340.dp, 66.dp)
        is IslandState.Celebration -> Pair(160.dp, 66.dp)
    }

    val animatedWidth by animateDpAsState(targetValue = islandWidth, animationSpec = islandSpring, label = "width_island")
    val animatedHeight by animateDpAsState(targetValue = islandHeight, animationSpec = islandSpring, label = "height_island")

    val glassBg = if (isDark) {
        Color.Black.copy(alpha = 0.60f) // Matches bg-black/60 from design spec!
    } else {
        Color(0xFFFCFCFF).copy(alpha = 0.85f)
    }

    // Iridescent shifting border brush - subtle frosted shimmers
    val slickColors = if (isDark) {
        listOf(
            Color.White.copy(alpha = 0.15f),
            Color.White.copy(alpha = 0.08f),
            GlassColors.CyberTeal.copy(alpha = 0.12f),
            Color.White.copy(alpha = 0.15f)
        )
    } else {
        listOf(
            Color.Black.copy(alpha = 0.12f),
            Color.Black.copy(alpha = 0.05f),
            Color.Black.copy(alpha = 0.12f)
        )
    }
    val iridescentBorderBrush = Brush.linearGradient(
        colors = slickColors,
        start = Offset(borderRotation * 600f, 0f),
        end = Offset((borderRotation + 1f) * 600f, 600f),
    )

    Box(
        modifier = modifier
            .width(animatedWidth)
            .height(animatedHeight)
            .clip(RoundedCornerShape(32.dp))
            .background(glassBg)
            .drawBehind {
                // Breathing radial glow internally
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GlassColors.VioletNeon.copy(alpha = breathingGlowFactor),
                            Color.Transparent
                        ),
                        center = center,
                        radius = size.width * 0.5f
                    )
                )
            }
            .border(
                width = 1.5.dp,
                brush = iridescentBorderBrush,
                shape = RoundedCornerShape(32.dp)
            )
            .testTag("dynamic_island_pill"),
        contentAlignment = Alignment.Center
    ) {
        // Render content based on island states
        AnimatedContent(
            targetState = islandState,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.95f)) togetherWith
                        fadeOut(animationSpec = tween(200))
            },
            label = "island_content"
        ) { state ->
            when (state) {
                is IslandState.Idle -> {
                    // Standard bottom navigation views
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NavTabItem(
                            icon = Icons.Rounded.Home,
                            label = "Home",
                            isSelected = currentScreen == AppScreen.DASHBOARD,
                            onClick = { onScreenSelected(AppScreen.DASHBOARD) },
                            isDark = isDark
                        )

                        NavTabItem(
                            icon = Icons.Rounded.People,
                            label = "Students",
                            isSelected = currentScreen == AppScreen.STUDENTS,
                            onClick = { onScreenSelected(AppScreen.STUDENTS) },
                            isDark = isDark
                        )

                        // Plus floating record payment button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(GlassColors.VioletNeon, GlassColors.CyberTeal)
                                    )
                                )
                                .clickable { onRecordPaymentClick() }
                                .testTag("island_plus_action"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Payment",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        NavTabItem(
                            icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                            label = "Ledger",
                            isSelected = currentScreen == AppScreen.HISTORY,
                            onClick = { onScreenSelected(AppScreen.HISTORY) },
                            isDark = isDark
                        )

                        NavTabItem(
                            icon = Icons.Rounded.Settings,
                            label = "Settings",
                            isSelected = currentScreen == AppScreen.SETTINGS,
                            onClick = { onScreenSelected(AppScreen.SETTINGS) },
                            isDark = isDark
                        )
                    }
                }

                is IslandState.OverdueAlert -> {
                    // Overdue layout horizontally expanded
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .combinedClickable(
                                onClick = onOverdueClick,
                                onLongClick = { /* No-op, or expand details */ }
                            )
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(GlassColors.SoftRed)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Notice: ${state.count} Tuition Overdue",
                                color = GlassColors.SoftRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Glass view button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GlassColors.SoftRed.copy(alpha = 0.15f))
                                .border(1.dp, GlassColors.SoftRed.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Collect",
                                color = if (isDark) Color.White else GlassColors.SoftRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                is IslandState.ExpandedStats -> {
                    // Long-pressed metrics box
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total Pending Due",
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = "FeeFlow Active",
                                color = GlassColors.CyberTeal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$${state.totalDue}",
                                color = GlassColors.SoftRed,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(GlassColors.EmeraldGreen.copy(alpha = 0.15f))
                                    .clickable { onRecordPaymentClick() }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Collect Payment",
                                    color = GlassColors.EmeraldGreen,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                is IslandState.Success -> {
                    // Success notification morph
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(GlassColors.EmeraldGreen.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = "Done",
                                tint = GlassColors.EmeraldGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = state.message,
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                is IslandState.Celebration -> {
                    // Celebration mode confetti overlay
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🎉 Paid!",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.graphicsLayer {
                                shadowElevation = 10f
                            }
                        )
                    }
                    
                    // CONFETTI FALLING PARTICLES ON TOP
                    ConfettiGlassParticles()
                }
            }
        }
    }
}

@Composable
fun NavTabItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isDark: Boolean
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.15f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMediumLow),
        label = "nav_scale"
    )

    Column(
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 8.dp)
            .testTag("nav_tab_${label.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) {
                GlassColors.CyberTeal
            } else {
                (if (isDark) Color.White else Color.Black).copy(alpha = 0.45f)
            },
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
        )
        
        Spacer(modifier = Modifier.height(2.dp))
        
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) {
                GlassColors.CyberTeal
            } else {
                (if (isDark) Color.White else Color.Black).copy(alpha = 0.45f)
            }
        )
    }
}

/**
 * Custom Falling Confetti particle canvas overlay.
 */
@Composable
fun ConfettiGlassParticles() {
    val particles = remember {
        List(25) {
            ConfettiParticle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * -100f,
                speedY = Random.nextFloat() * 4f + 3f,
                speedX = Random.nextFloat() * 2f - 1f,
                color = when (Random.nextInt(4)) {
                    0 -> GlassColors.CyberTeal
                    1 -> GlassColors.VioletNeon
                    2 -> GlassColors.BubblegumPink
                    else -> GlassColors.AmberGlow
                },
                size = Random.nextFloat() * 6f + 4f
            )
        }
    }

    var tick by remember { mutableStateOf(0) }

    LaunchedEffect(key1 = true) {
        repeat(70) {
            delay(16)
            particles.forEach { p ->
                p.y += p.speedY
                p.x += p.speedX * 0.05f
            }
            tick++
        }
    }

    // Canvas drawing on screen bounds
    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                particles.forEach { p ->
                    val screenX = p.x * size.width
                    val screenY = p.y
                    if (screenY in 0f..size.height) {
                        drawCircle(
                            color = p.color,
                            radius = p.size,
                            center = Offset(screenX, screenY)
                        )
                    }
                }
            }
    )
}

data class ConfettiParticle(
    var x: Float, // percentage of width 0..1
    var y: Float, // local offset Y
    val speedY: Float,
    val speedX: Float,
    val color: Color,
    val size: Float
)
