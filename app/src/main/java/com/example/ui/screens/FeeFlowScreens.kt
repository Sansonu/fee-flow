package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.Payment
import com.example.data.Student
import com.example.ui.components.GlassBadge
import com.example.ui.components.GlassButton
import com.example.ui.components.GlassCard
import com.example.ui.components.GlassColors
import com.example.ui.viewmodel.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

/**
 * A highly-customized, performant spring-based entering motion transition
 * that mimics Framer Motion's physical spring behaviors (friction, mass, tension).
 */
@Composable
fun FramerMotionEntrance(
    delayMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        isVisible = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "framer_alpha"
    )

    val offsetY by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 24.dp,
        animationSpec = spring(
            dampingRatio = 0.62f, // Elegant physical slide-and-settle response
            stiffness = Spring.StiffnessLow
        ),
        label = "framer_offsetY"
    )

    val scale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0.93f,
        animationSpec = spring(
            dampingRatio = 0.62f,
            stiffness = Spring.StiffnessLow
        ),
        label = "framer_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY.toPx()
                this.scaleX = scale
                this.scaleY = scale
            }
    ) {
        content()
    }
}

/**
 * 1. DASHBOARD COMPOSABLE
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: FeeFlowViewModel,
    isDark: Boolean,
    onRecordPaymentClick: (Student?) -> Unit
) {
    val teacherName by viewModel.teacherName.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    val totalCollected by viewModel.totalCollected.collectAsState()
    val totalPending by viewModel.totalPending.collectAsState()
    val totalStudentsCount by viewModel.totalStudentsCount.collectAsState()
    val overdueCount by viewModel.overdueCount.collectAsState()
    val dueThisWeek by viewModel.dueThisWeekList.collectAsState()

    val csvPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.processCSVFileUri(it) }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Welcoming Title Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            ) {
                Text(
                    text = "Welcome Back,",
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = teacherName,
                    color = if (isDark) Color.White else Color.Black,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
            }
        }

        // Metrics Grid (Total Students, Collected, Pending, Due count)
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                maxItemsInEachRow = 2
            ) {
                val itemWidth = Modifier.weight(1f)
                
                // Card 1: Students
                FramerMotionEntrance(
                    delayMillis = 0,
                    modifier = itemWidth
                ) {
                    MetricGlassCard(
                        title = "Total Students",
                        value = "$totalStudentsCount",
                        icon = Icons.Rounded.People,
                        color = GlassColors.CyberTeal,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Card 2: Collected
                FramerMotionEntrance(
                    delayMillis = 80,
                    modifier = itemWidth
                ) {
                    MetricGlassCard(
                        title = "Collected",
                        value = "$currency${formatAmount(totalCollected)}",
                        icon = Icons.Rounded.Payments,
                        color = GlassColors.EmeraldGreen,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Card 3: Pending
                FramerMotionEntrance(
                    delayMillis = 160,
                    modifier = itemWidth
                ) {
                    MetricGlassCard(
                        title = "Pending",
                        value = "$currency${formatAmount(totalPending)}",
                        icon = Icons.Rounded.AccountBalance,
                        color = GlassColors.AmberGlow,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Card 4: Due Count
                FramerMotionEntrance(
                    delayMillis = 240,
                    modifier = itemWidth
                ) {
                    MetricGlassCard(
                        title = "Due Count",
                        value = "$overdueCount",
                        icon = Icons.Rounded.Warning,
                        color = GlassColors.SoftRed,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Quick Actions Core Pane
        item {
            GlassCard(
                isDark = isDark,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Quick Command Center",
                    color = if (isDark) Color.White else Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        onClick = { onRecordPaymentClick(null) },
                        isDark = isDark,
                        testTag = "record_payment_quick_btn",
                        accentColor = GlassColors.EmeraldGreen,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Record Payment",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Payment",
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    GlassButton(
                        onClick = { csvPicker.launch("*/*") },
                        isDark = isDark,
                        testTag = "import_excel_quick_btn",
                        accentColor = GlassColors.CyberTeal,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CloudUpload,
                            contentDescription = "Bulk Import",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Import Data",
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Download Template
                GlassButton(
                    onClick = { viewModel.shareCSVTemplate() },
                    isDark = isDark,
                    testTag = "download_template_btn",
                    accentColor = GlassColors.RoyalPurple,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Download Template",
                        tint = if (isDark) Color.White else Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Share CSV Import Template",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // "Due This Week" Area
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Due This Week",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${dueThisWeek.size} student(s)",
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (dueThisWeek.isEmpty()) {
                    EmptyListFallback(
                        isDark = isDark,
                        text = "No payments due this week. Excellent job!",
                        icon = Icons.Rounded.CheckCircle
                    )
                } else {
                    dueThisWeek.forEach { student ->
                        Spacer(modifier = Modifier.height(8.dp))
                        DueStudentRow(
                            student = student,
                            currency = currency,
                            isDark = isDark,
                            onRecordClick = { onRecordPaymentClick(student) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MetricGlassCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    GlassCard(
        isDark = isDark,
        modifier = modifier.height(115.dp),
        cornerRadius = 28.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Text(
                text = value,
                color = if (title.lowercase() == "collected" && isDark) Color(0xFF86EFAC) else if (title.lowercase().contains("due") && isDark) Color(0xFFFCA5A5) else if (title.lowercase() == "pending" && isDark) Color(0xFFFDE68A) else (if (isDark) Color.White else Color.Black),
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DueStudentRow(
    student: Student,
    currency: String,
    isDark: Boolean,
    onRecordClick: () -> Unit
) {
    val balance = (student.totalFee - student.paidAmount).coerceAtLeast(0.0)
    
    val namesStr = student.name.split(" ").filter { it.isNotEmpty() }
    val initials = if (namesStr.size >= 2) {
        "${namesStr[0].take(1)}${namesStr[1].take(1)}".uppercase()
    } else if (namesStr.isNotEmpty()) {
        namesStr[0].take(2).uppercase()
    } else {
        "ST"
    }
    
    val avatarGradient = remember(student.name) {
        val hash = Math.abs(student.name.hashCode()) % 3
        when (hash) {
            0 -> Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF9333EA))) // Indigo to Purple
            1 -> Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF10B981))) // Teal to Emerald
            else -> Brush.linearGradient(listOf(Color(0xFFFB923C), Color(0xFFEC4899))) // Orange to Pink
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.65f))
            .border(1.dp, (if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)), RoundedCornerShape(20.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Initials avatar matching Immersive UI
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarGradient),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = student.name,
                color = if (isDark) Color.White else Color.Black,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${student.studentClass} • Due Today",
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                fontSize = 12.sp
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$currency${formatAmount(balance)}",
                    color = if (isDark) Color(0xFFFCA5A5) else GlassColors.SoftRed,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "DUE TODAY",
                    color = if (isDark) Color(0xFFFCA5A5).copy(alpha = 0.7f) else GlassColors.SoftRed.copy(alpha = 0.8f),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }

            IconButton(
                onClick = onRecordClick,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(GlassColors.EmeraldGreen.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = Icons.Rounded.Payments,
                    contentDescription = "Pay",
                    tint = GlassColors.EmeraldGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * 2. STUDENTS LIST SCREEN
 */
@OptIn(ExperimentalFoundationApi::class)
data class ClassSummary(
    val className: String,
    val totalStudents: Int,
    val totalFee: Double,
    val totalPaid: Double,
    val pendingFee: Double,
    val englishStudents: List<Student>,
    val hindiStudents: List<Student>
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentsScreen(
    viewModel: FeeFlowViewModel,
    isDark: Boolean,
    onAddStudentClick: () -> Unit,
    onStudentSelect: (Student) -> Unit,
    onRecordPayment: (Student) -> Unit
) {
    val students by viewModel.students.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("All") } // "All", "Paid", "Partial", "Due"
    var viewMode by remember { mutableStateOf("Students") } // "Students" or "Classes"

    // Expanded states for class summary medium details
    val expandedClasses = remember { mutableStateMapOf<String, Boolean>() }
    val expandedEnglishMediums = remember { mutableStateMapOf<String, Boolean>() }
    val expandedHindiMediums = remember { mutableStateMapOf<String, Boolean>() }

    // Filtered lists of students
    val filteredStudents = remember(students, searchQuery, activeFilter) {
        students.filter { student ->
            val matchesQuery = student.name.contains(searchQuery, ignoreCase = true) ||
                    student.id.contains(searchQuery, ignoreCase = true) ||
                    student.studentClass.contains(searchQuery, ignoreCase = true)
            
            val matchesFilter = when (activeFilter) {
                "Paid" -> student.status == "Paid"
                "Partial" -> student.status == "Partial"
                "Due" -> student.status == "Due"
                else -> true
            }

            matchesQuery && matchesFilter
        }
    }

    // Class wise summarization
    val classSummaries = remember(students, searchQuery) {
        val groups = students.groupBy { it.studentClass }
        groups.map { (className, classStudents) ->
            val totalFee = classStudents.sumOf { it.totalFee }
            val totalPaid = classStudents.sumOf { it.paidAmount }
            val pendingFee = classStudents.sumOf { (it.totalFee - it.paidAmount).coerceAtLeast(0.0) }
            val english = classStudents.filter { it.medium == "English" }
            val hindi = classStudents.filter { it.medium == "Hindi" }
            
            ClassSummary(
                className = className.ifBlank { "Unassigned Class" },
                totalStudents = classStudents.size,
                totalFee = totalFee,
                totalPaid = totalPaid,
                pendingFee = pendingFee,
                englishStudents = english,
                hindiStudents = hindi
            )
        }.filter {
            searchQuery.isBlank() || 
            it.className.contains(searchQuery, ignoreCase = true) ||
            it.englishStudents.any { s -> s.name.contains(searchQuery, ignoreCase = true) } ||
            it.hindiStudents.any { s -> s.name.contains(searchQuery, ignoreCase = true) }
        }.sortedBy { it.className }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // TOP TAB SEGMENT SELECTOR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                .border(1.dp, (if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)), RoundedCornerShape(16.dp))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Students" to Icons.Rounded.People, "Classes" to Icons.Rounded.Class).forEach { (mode, icon) ->
                val isSelected = viewMode == mode
                val selectedBg = if (isSelected) {
                    Brush.linearGradient(listOf(GlassColors.RoyalPurple.copy(alpha = 0.22f), GlassColors.CyberTeal.copy(alpha = 0.22f)))
                } else {
                    Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                }
                val selectedBorder = if (isSelected) GlassColors.CyberTeal.copy(alpha = 0.4f) else Color.Transparent
                
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(selectedBg)
                        .border(if (isSelected) 1.dp else 0.dp, selectedBorder, RoundedCornerShape(12.dp))
                        .clickable { viewMode = mode }
                        .testTag("students_view_tab_$mode"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = mode,
                            tint = if (isSelected) (if (isDark) Color.White else GlassColors.RoyalPurple) else (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (mode == "Students") "Students List" else "Class-Wise Details",
                            color = if (isSelected) (if (isDark) Color.White else GlassColors.RoyalPurple) else (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // CONDITIONAL RENDER BASED ON TAB
        if (viewMode == "Students") {
            // ORIGINAL STUDENTS VIEW
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("students_screen_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Search & Add Block
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GlassTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = "Search name, ID, class...",
                            isDark = isDark,
                            icon = Icons.Rounded.Search,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(
                            onClick = onAddStudentClick,
                            modifier = Modifier
                                .size(50.dp)
                                .shadow(8.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(GlassColors.VioletNeon, GlassColors.CyberTeal)
                                    )
                                )
                                .testTag("add_student_fab")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "Add Student",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }

                // Filter Tabs
                item {
                    ScrollableTabRow(
                        selectedTabIndex = when (activeFilter) {
                            "Paid" -> 1
                            "Partial" -> 2
                            "Due" -> 3
                            else -> 0
                        },
                        edgePadding = 0.dp,
                        containerColor = Color.Transparent,
                        divider = {},
                        indicator = {},
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val filters = listOf("All", "Paid", "Partial", "Due")
                        filters.forEach { filter ->
                            val selected = activeFilter == filter
                            val bgCol = if (selected) {
                                GlassColors.RoyalPurple.copy(alpha = 0.25f)
                            } else {
                                if (isDark) Color(0xFF1B1435).copy(alpha = 0.4f) else Color.White.copy(alpha = 0.4f)
                            }
                            val borderCol = if (selected) GlassColors.CyberTeal.copy(alpha = 0.7f) else Color.Transparent

                            Box(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bgCol)
                                    .border(1.dp, borderCol, RoundedCornerShape(12.dp))
                                    .clickable { activeFilter = filter }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                                    .testTag("filter_tab_$filter"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = filter,
                                    color = if (selected) {
                                        if (isDark) Color.White else GlassColors.RoyalPurple
                                    } else {
                                        (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                                    },
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Empty State Check
                if (filteredStudents.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                        EmptyListFallback(
                            isDark = isDark,
                            text = "No students found. Add students manually or import via Excel!",
                            icon = Icons.Rounded.People
                        )
                    }
                } else {
                    items(
                        items = filteredStudents,
                        key = { it.id }
                    ) { student ->
                        StudentGlassCard(
                            student = student,
                            currency = currency,
                            isDark = isDark,
                            onTap = { onStudentSelect(student) },
                            onLongPress = { viewModel.markFullyPaid(student) },
                            onRecordPayment = { onRecordPayment(student) },
                            modifier = Modifier.animateItem()
                        )
                    }
                }
            }
        } else {
            // CLASS-WISE DETAILS VIEW
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("class_wise_screen_list"),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Class Search Bar
                item {
                    GlassTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = "Search by class name, student name...",
                        isDark = isDark,
                        icon = Icons.Rounded.Search,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                if (classSummaries.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(40.dp))
                        EmptyListFallback(
                            isDark = isDark,
                            text = "No classes found. Add student profiles with classes to populate this section!",
                            icon = Icons.Rounded.Class
                        )
                    }
                } else {
                    items(
                        items = classSummaries,
                        key = { it.className }
                    ) { summary ->
                        val isClassExpanded = expandedClasses[summary.className] ?: false
                        val isEnglishExpanded = expandedEnglishMediums[summary.className] ?: false
                        val isHindiExpanded = expandedHindiMediums[summary.className] ?: false

                        // Class card container
                        val collectionProgress = if (summary.totalFee > 0.0) {
                            (summary.totalPaid / summary.totalFee).toFloat().coerceIn(0f, 1f)
                        } else 0f

                        val shadowColor = if (isDark) Color.Black.copy(alpha = 0.3f) else Color(0xFF6E56CF).copy(alpha = 0.05f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = shadowColor, spotColor = shadowColor)
                                .clip(RoundedCornerShape(24.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.65f))
                                .border(1.dp, if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                                .clickable {
                                    expandedClasses[summary.className] = !isClassExpanded
                                }
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Pinned Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(GlassColors.VioletNeon.copy(alpha = 0.2f), GlassColors.CyberTeal.copy(alpha = 0.2f))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.School,
                                                contentDescription = "Class",
                                                tint = GlassColors.VioletNeon,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = summary.className,
                                                color = if (isDark) Color.White else Color.Black,
                                                fontSize = 17.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "${summary.totalStudents} students mapped",
                                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }

                                    // Expand Collapse Chevron
                                    Icon(
                                        imageVector = if (isClassExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Expand Class Details",
                                        tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f)
                                    )
                                }

                                // Money Progress bar
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Fee Collection Progress",
                                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${(collectionProgress * 100).toInt()}%",
                                            color = GlassColors.EmeraldGreen,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    // Custom visual glass progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape)
                                            .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(collectionProgress)
                                                .clip(CircleShape)
                                                .background(
                                                    Brush.horizontalGradient(
                                                        listOf(GlassColors.VioletNeon, GlassColors.CyberTeal)
                                                    )
                                                )
                                        )
                                    }
                                }

                                // Quick Financial stats totals
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total Fees",
                                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "$currency${formatAmount(summary.totalFee)}",
                                            color = if (isDark) Color.White else Color.Black,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "Collected",
                                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "$currency${formatAmount(summary.totalPaid)}",
                                            color = GlassColors.EmeraldGreen,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Remaining Due",
                                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                            fontSize = 10.sp
                                        )
                                        Text(
                                            text = "$currency${formatAmount(summary.pendingFee)}",
                                            color = if (summary.pendingFee > 0) GlassColors.SoftRed else (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // MEDIUM DETAILS UNDER DROP-DOWN
                                AnimatedVisibility(
                                    visible = isClassExpanded,
                                    enter = expandVertically() + fadeIn(),
                                    exit = shrinkVertically() + fadeOut()
                                ) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.padding(top = 10.dp)
                                    ) {
                                        HorizontalDivider(
                                            thickness = 1.dp,
                                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f)
                                        )

                                        // 🌎 ENGLISH MEDIUM ROW DETAILED ACCORDION
                                        MediumGroupSection(
                                            title = "English Medium",
                                            icon = Icons.Rounded.Language,
                                            students = summary.englishStudents,
                                            currency = currency,
                                            isExpanded = isEnglishExpanded,
                                            isDark = isDark,
                                            onToggleExpand = {
                                                expandedEnglishMediums[summary.className] = !isEnglishExpanded
                                            },
                                            onStudentSelect = onStudentSelect,
                                            onRecordPayment = onRecordPayment
                                        )

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // 🪔 HINDI MEDIUM ROW DETAILED ACCORDION (हिंदी माध्यम)
                                        MediumGroupSection(
                                            title = "Hindi Medium (हिंदी माध्यम)",
                                            icon = Icons.Rounded.Translate,
                                            students = summary.hindiStudents,
                                            currency = currency,
                                            isExpanded = isHindiExpanded,
                                            isDark = isDark,
                                            onToggleExpand = {
                                                expandedHindiMediums[summary.className] = !isHindiExpanded
                                            },
                                            onStudentSelect = onStudentSelect,
                                            onRecordPayment = onRecordPayment
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom expandable container displaying child list for Hindi/English Mediums.
 */
@Composable
fun MediumGroupSection(
    title: String,
    icon: ImageVector,
    students: List<Student>,
    currency: String,
    isExpanded: Boolean,
    isDark: Boolean,
    onToggleExpand: () -> Unit,
    onStudentSelect: (Student) -> Unit,
    onRecordPayment: (Student) -> Unit
) {
    val totalPending = students.sumOf { (it.totalFee - it.paidAmount).coerceAtLeast(0.0) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f))
            .border(1.dp, (if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)), RoundedCornerShape(16.dp))
            .padding(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = if (title.startsWith("English")) GlassColors.CyberTeal else GlassColors.VioletNeon,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    color = if (isDark) Color.White else Color.Black,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${students.size}",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Bal: $currency${formatAmount(totalPending)}",
                    color = if (totalPending > 0) GlassColors.SoftRed else GlassColors.EmeraldGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Expand medium details",
                    tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            if (students.isEmpty()) {
                Text(
                    text = "No students mapped in this medium.",
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    students.forEach { s ->
                        val bal = (s.totalFee - s.paidAmount).coerceAtLeast(0.0)
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.02f))
                                .clickable { onStudentSelect(s) }
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = s.name,
                                        color = if (isDark) Color.White else Color.Black,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "(${s.id})",
                                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                        fontSize = 10.sp
                                    )
                                }
                                Text(
                                    text = "Collected: $currency${formatAmount(s.paidAmount)} / $currency${formatAmount(s.totalFee)}",
                                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                    fontSize = 11.sp
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                GlassBadge(text = s.status, statusType = s.status, isDark = isDark)
                                
                                if (bal > 0) {
                                    IconButton(
                                        onClick = { onRecordPayment(s) },
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(GlassColors.EmeraldGreen.copy(alpha = 0.15f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Payments,
                                            contentDescription = "Collect payment",
                                            tint = GlassColors.EmeraldGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StudentGlassCard(
    student: Student,
    currency: String,
    isDark: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onRecordPayment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val balance = (student.totalFee - student.paidAmount).coerceAtLeast(0.0)
    val initials = if (student.name.isNotBlank()) {
        student.name.split(" ").filter { it.isNotBlank() }.take(2)
            .map { it.first().uppercase() }.joinToString("")
    } else "ST"

    val avatarGradient = remember(student.name) {
        val hash = Math.abs(student.name.hashCode()) % 3
        when (hash) {
            0 -> Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF9333EA))) // Indigo to Purple
            1 -> Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF10B981))) // Teal to Emerald
            else -> Brush.linearGradient(listOf(Color(0xFFFB923C), Color(0xFFEC4899))) // Orange to Pink
        }
    }

    val shadowColor = if (isDark) Color.Black.copy(alpha = 0.30f) else Color(0xFF6E56CF).copy(alpha = 0.05f)

    // Gesture Swipe-to-Pay parameters
    val density = LocalDensity.current
    val swipeLimit = with(density) { 140.dp.toPx() }
    
    var offsetX by remember { mutableStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "swipe_to_pay_offset"
    )

    // Trigger payment if swiped past threshold
    val swipeThreshold = swipeLimit * 0.85f

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        // SWIPE BACKGROUND REVEAL ZONE
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isDark) {
                        Color(0xFF10B981).copy(alpha = 0.15f)
                    } else {
                        Color(0xFF10B981).copy(alpha = 0.08f)
                    }
                )
                .border(
                    1.dp,
                    Color(0xFF10B981).copy(alpha = if (isDark) 0.35f else 0.20f),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            val progress = (offsetX / swipeLimit).coerceIn(0f, 1f)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.graphicsLayer {
                    alpha = progress
                    scaleX = 0.85f + 0.15f * progress
                    scaleY = 0.85f + 0.15f * progress
                }
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Payments,
                        contentDescription = "Swipe to Pay",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = if (offsetX >= swipeThreshold) "RELEASE TO RECORD PAYMENT" else "SWIPE TO PAY $currency${formatAmount(balance)}",
                    color = Color(0xFF10B981),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // FOREGROUND SWIPEABLE GLASS TILE
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    translationX = animatedOffset
                }
                .pointerInput(student.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX >= swipeThreshold && balance > 0) {
                                onRecordPayment()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = {
                            offsetX = 0f
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            // Only allow pulling from left to right for pay trigger if student has balance
                            if (balance > 0) {
                                offsetX = (offsetX + dragAmount).coerceIn(0f, swipeLimit * 1.2f)
                            }
                        }
                    )
                }
                .shadow(16.dp, RoundedCornerShape(24.dp), ambientColor = shadowColor, spotColor = shadowColor)
                .clip(RoundedCornerShape(24.dp))
                .background(
                    if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.65f)
                )
                .border(
                    1.dp,
                    (if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)),
                    RoundedCornerShape(24.dp)
                )
                .combinedClickable(
                    onClick = onTap,
                    onLongClick = onLongPress
                )
                .padding(16.dp)
                .testTag("student_card_${student.id}")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Avatar initials styled with vibrant brand gradients
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(avatarGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = student.name,
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = student.id,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Class: ${student.studentClass}",
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Money Breakdowns
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "Total Fee",
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "$currency${formatAmount(student.totalFee)}",
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = "Paid",
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "$currency${formatAmount(student.paidAmount)}",
                                color = GlassColors.EmeraldGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Column {
                            Text(
                                text = "Pending",
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                            Text(
                                text = "$currency${formatAmount(balance)}",
                                color = if (balance > 0) GlassColors.SoftRed else (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassBadge(text = student.status, statusType = student.status, isDark = isDark)
                    
                    IconButton(
                        onClick = onRecordPayment,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GlassColors.EmeraldGreen.copy(alpha = 0.15f))
                            .testTag("record_pay_icon_btn_${student.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Payments,
                            contentDescription = "Pay",
                            tint = GlassColors.EmeraldGreen,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3. MANUAL ADD/EDIT STUDENT SCREEN
 */
@Composable
fun AddEditStudentScreen(
    viewModel: FeeFlowViewModel,
    isDark: Boolean,
    onCancel: () -> Unit
) {
    val editingStudent by viewModel.editingStudent.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var name by remember { mutableStateOf("") }
    var studentClass by remember { mutableStateOf("") }
    var studentMedium by remember { mutableStateOf("English") }
    var totalFeeStr by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var dueDateStr by remember { mutableStateOf("") }

    // Init values
    LaunchedEffect(editingStudent) {
        editingStudent?.let { s ->
            name = s.name
            studentClass = s.studentClass
            studentMedium = s.medium
            totalFeeStr = s.totalFee.toString()
            contact = s.contact
            dueDateStr = s.dueDate
        } ?: run {
            name = ""
            studentClass = ""
            studentMedium = "English"
            totalFeeStr = ""
            contact = ""
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            dueDateStr = sdf.format(Date())
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("add_edit_student_screen"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = if (editingStudent == null) "Add Student Profile" else "Edit Student Profile",
                color = if (isDark) Color.White else Color.Black,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Configure custom tuition package details below.",
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }

        item {
            GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Name input
                    Text(
                        text = "STUDENT NAME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    GlassTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = "e.g. Liam Henderson",
                        isDark = isDark,
                        icon = Icons.Rounded.Person,
                        modifier = Modifier.fillMaxWidth().testTag("add_student_name_input")
                    )

                    // Class input
                    Text(
                        text = "CLASS / GRADE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    GlassTextField(
                        value = studentClass,
                        onValueChange = { studentClass = it },
                        placeholder = "e.g. Class 12A",
                        isDark = isDark,
                        icon = Icons.Rounded.School,
                        modifier = Modifier.fillMaxWidth().testTag("add_student_class_input")
                    )

                    // Instruction Medium Toggle
                    Text(
                        text = "INSTRUCTION MEDIUM",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                            .border(1.dp, (if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)), RoundedCornerShape(14.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("English", "Hindi").forEach { med ->
                            val isSelected = studentMedium == med
                            val selectedBg = if (isSelected) {
                                Brush.linearGradient(listOf(GlassColors.VioletNeon.copy(alpha = 0.25f), GlassColors.CyberTeal.copy(alpha = 0.25f)))
                            } else {
                                Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            }
                            val selectedBorder = if (isSelected) GlassColors.CyberTeal.copy(alpha = 0.4f) else Color.Transparent
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(selectedBg)
                                    .border(if (isSelected) 1.dp else 0.dp, selectedBorder, RoundedCornerShape(12.dp))
                                    .clickable { studentMedium = med }
                                    .testTag("medium_select_$med"),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (med == "English") Icons.Rounded.Language else Icons.Rounded.Translate,
                                        contentDescription = med,
                                        tint = if (isSelected) (if (isDark) Color.White else GlassColors.RoyalPurple) else (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (med == "English") "English Medium" else "Hindi Medium (हिंदी)",
                                        color = if (isSelected) (if (isDark) Color.White else GlassColors.RoyalPurple) else (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Total Fee
                    Text(
                        text = "TOTAL ACTION TUITION FEE ($currency)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    GlassTextField(
                        value = totalFeeStr,
                        onValueChange = { totalFeeStr = it },
                        placeholder = "e.g. 1500",
                        isDark = isDark,
                        icon = Icons.Rounded.Payments,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_fee_input")
                    )

                    // Contact
                    Text(
                        text = "CONTACT DETAILS / PHONE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    GlassTextField(
                        value = contact,
                        onValueChange = { contact = it },
                        placeholder = "e.g. +1 555-0199",
                        isDark = isDark,
                        icon = Icons.Rounded.Phone,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth().testTag("add_student_contact_input")
                    )

                    // Due Date
                    Text(
                        text = "DUE DATE (yyyy-MM-dd)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    GlassTextField(
                        value = dueDateStr,
                        onValueChange = { dueDateStr = it },
                        placeholder = "yyyy-MM-dd",
                        isDark = isDark,
                        icon = Icons.Rounded.CalendarToday,
                        modifier = Modifier.fillMaxWidth().testTag("add_student_date_input")
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cancel
                GlassButton(
                    onClick = onCancel,
                    isDark = isDark,
                    testTag = "add_student_cancel_btn",
                    accentColor = GlassColors.SoftRed,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Cancel",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Save
                val isFormValid = name.isNotBlank() && studentClass.isNotBlank() && totalFeeStr.toDoubleOrNull() != null
                GlassButton(
                    onClick = {
                        val totalFee = totalFeeStr.toDoubleOrNull() ?: 0.0
                        val rawId = editingStudent?.id ?: ("FLW" + String.format("%03d", viewModel.students.value.size + Random.nextInt(100, 999)))
                        val paid = editingStudent?.paidAmount ?: 0.0
                        val status = when {
                            paid >= totalFee -> "Paid"
                            paid > 0 -> "Partial"
                            else -> "Due"
                        }
                        val student = Student(
                            id = rawId,
                            name = name,
                            studentClass = studentClass,
                            medium = studentMedium,
                            contact = contact,
                            totalFee = totalFee,
                            paidAmount = paid,
                            dueDate = dueDateStr,
                            status = status
                        )
                        viewModel.saveStudent(student)
                        onCancel()
                    },
                    isDark = isDark,
                    testTag = "add_student_save_btn",
                    enabled = isFormValid,
                    accentColor = GlassColors.EmeraldGreen,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Save Profile",
                        color = if (isFormValid) (if (isDark) Color.White else Color.Black) else Color.Gray,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * 4. HISTORIC TRANSACTION HISTORY FEED SCREEN
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PaymentHistoryScreen(
    viewModel: FeeFlowViewModel,
    isDark: Boolean
) {
    val payments by viewModel.payments.collectAsState()
    val students by viewModel.students.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    val studentMap = remember(students) { students.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("history_screen"),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Payment Audit Feed",
                color = if (isDark) Color.White else Color.Black,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Chronological audit log of all recorded tuition collections.",
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }

        if (payments.isEmpty()) {
            item {
                Spacer(modifier = Modifier.height(40.dp))
                EmptyListFallback(
                    isDark = isDark,
                    text = "No collection records exist yet. Record a new payment inside student listing!",
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong
                )
            }
        } else {
            items(
                items = payments,
                key = { it.id }
            ) { payment ->
                val student = studentMap[payment.studentId]
                val studentName = student?.name ?: "Unknown Student"
                val studentClass = student?.studentClass ?: "N/A"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.65f))
                        .border(1.dp, (if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)), RoundedCornerShape(20.dp))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = studentName,
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$studentClass • Paid ${formatDate(payment.date)} via ${payment.method}",
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                            fontSize = 12.sp
                        )
                        if (payment.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Note: ${payment.notes}",
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "+$currency${formatAmount(payment.amount)}",
                            color = GlassColors.EmeraldGreen,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Delete payment
                        IconButton(
                            onClick = { viewModel.deletePayment(payment) },
                            modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(GlassColors.SoftRed.copy(alpha = 0.12f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete",
                                tint = GlassColors.SoftRed,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 5. SETTINGS SCREEN
 */
@Composable
fun SettingsScreen(
    viewModel: FeeFlowViewModel,
    isDark: Boolean
) {
    val teacherName by viewModel.teacherName.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()
    val defaultDueDay by viewModel.defaultDueDay.collectAsState()

    var editingName by remember { mutableStateOf(teacherName) }
    var editingCurrency by remember { mutableStateOf(currency) }
    var editingDueDay by remember { mutableStateOf(defaultDueDay.toString()) }

    LaunchedEffect(teacherName, currency, defaultDueDay) {
        editingName = teacherName
        editingCurrency = currency
        editingDueDay = defaultDueDay.toString()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "FeeFlow Control Room",
                color = if (isDark) Color.White else Color.Black,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp
            )
            Text(
                text = "Manage system preferences and file exports.",
                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                fontSize = 14.sp
            )
        }

        item {
            GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Teacher Name
                    Text(
                        text = "TEACHER NAME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    GlassTextField(
                        value = editingName,
                        onValueChange = { 
                            editingName = it
                            viewModel.updateTeacherName(it)
                        },
                        placeholder = "Teacher name",
                        isDark = isDark,
                        icon = Icons.Rounded.Person,
                        modifier = Modifier.fillMaxWidth().testTag("teacher_name_input")
                    )

                    // Currency Symbol
                    Text(
                        text = "CURRENCY SYMBOL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    GlassTextField(
                        value = editingCurrency,
                        onValueChange = { 
                            editingCurrency = it
                            viewModel.updateCurrencySymbol(it)
                        },
                        placeholder = "e.g. $, €, ₹",
                        isDark = isDark,
                        icon = Icons.Rounded.AttachMoney,
                        modifier = Modifier.fillMaxWidth().testTag("currency_symbol_input")
                    )

                    // Default due day
                    Text(
                        text = "DEFAULT DUE DAY OF MONTH",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )
                    GlassTextField(
                        value = editingDueDay,
                        onValueChange = { 
                            editingDueDay = it
                            it.toIntOrNull()?.let { day ->
                                if (day in 1..31) {
                                    viewModel.updateDefaultDueDay(day)
                                }
                            }
                        },
                        placeholder = "15",
                        isDark = isDark,
                        icon = Icons.Rounded.CalendarToday,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("due_day_input")
                    )
                }
            }
        }

        item {
            GlassCard(isDark = isDark, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = "System Adjustments",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color.Black
                    )

                    // Scheme toggle row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isDark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode,
                                contentDescription = "Theme",
                                tint = GlassColors.CyberTeal,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Liquid Dark Mode",
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 14.sp
                            )
                        }

                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.toggleTheme() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = GlassColors.CyberTeal,
                                checkedTrackColor = GlassColors.RoyalPurple.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.testTag("dark_mode_switch")
                        )
                    }

                    Divider(color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f))

                    // Backup / Export
                    Text(
                        text = "EXPORTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )

                    GlassButton(
                        onClick = { viewModel.exportAllToCSV() },
                        isDark = isDark,
                        testTag = "export_excel_btn",
                        accentColor = GlassColors.CyberTeal,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = "Export Excel",
                            tint = if (isDark) Color.White else Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Export Tuition Database to CSV",
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 6. COMPLEX DETAIL PROFILE OVERLAY SHEET
 */
@Composable
fun StudentDetailSheet(
    student: Student,
    viewModel: FeeFlowViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onRecordPayment: () -> Unit
) {
    val payments by viewModel.payments.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    val studentPayments = remember(payments, student.id) {
        payments.filter { it.studentId == student.id }
    }
    val balance = (student.totalFee - student.paidAmount).coerceAtLeast(0.0)

    val customGradient = Brush.radialGradient(
        colors = listOf(GlassColors.RoyalPurple.copy(alpha = 0.25f), Color.Transparent),
        center = Offset(300f, 100f),
        radius = 800f
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .clickable(enabled = false) {}
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .background(if (isDark) Color(0xFF0F0824) else Color.White)
                    .drawBehind {
                        drawRect(brush = customGradient)
                    }
                    .border(
                        1.dp,
                        Brush.verticalGradient(
                            listOf(GlassColors.CyberTeal.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .padding(24.dp)
                    .testTag("student_detail_sheet")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Close handle bar
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(36.dp, 5.dp)
                            .clip(CircleShape)
                            .background(
                                (if (isDark) Color.White else Color.Black).copy(alpha = 0.15f)
                            )
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Student title and badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = student.name,
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Student ID: ${student.id} | Class: ${student.studentClass} (${student.medium} Medium)",
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                fontSize = 13.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onEdit,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GlassColors.RoyalPurple.copy(alpha = 0.15f))
                                    .testTag("edit_student_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit",
                                    tint = GlassColors.RoyalPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    viewModel.deleteStudent(student.id)
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(GlassColors.SoftRed.copy(alpha = 0.15f))
                                    .testTag("delete_student_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete",
                                    tint = GlassColors.SoftRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Contact Details Area
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f))
                            .padding(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Phone,
                            contentDescription = "Contact",
                            tint = GlassColors.CyberTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Primary Contact: ${student.contact.ifBlank { "N/A" }}",
                            color = if (isDark) Color.White else Color.Black,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Summary Block
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryBox(
                            header = "TOTAL FEE",
                            valStr = "$currency${formatAmount(student.totalFee)}",
                            subtext = "Tuition package",
                            tint = if (isDark) Color.White else Color.Black,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryBox(
                            header = "TOTAL PAID",
                            valStr = "$currency${formatAmount(student.paidAmount)}",
                            subtext = "Accumulated",
                            tint = GlassColors.EmeraldGreen,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryBox(
                            header = "BALANCE DUE",
                            valStr = "$currency${formatAmount(balance)}",
                            subtext = student.status,
                            tint = if (balance > 0) GlassColors.SoftRed else GlassColors.EmeraldGreen,
                            isDark = isDark,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Payment Log history",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color.Black
                        )

                        GlassButton(
                            onClick = onRecordPayment,
                            isDark = isDark,
                            testTag = "record_payment_sheet_btn",
                            accentColor = GlassColors.EmeraldGreen,
                            modifier = Modifier.height(38.dp)
                        ) {
                            Text(
                                text = "Record Payment",
                                color = if (isDark) Color.White else Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Render transaction feed for student
                    if (studentPayments.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.02f) else Color.Black.copy(alpha = 0.02f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No payments ledger recorded for this profile yet.",
                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(20.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            items(studentPayments) { p ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.04f))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "+$currency${formatAmount(p.amount)} via ${p.method}",
                                            color = GlassColors.EmeraldGreen,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formatDate(p.date),
                                            fontSize = 11.sp,
                                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                                        )
                                        if (p.notes.isNotBlank()) {
                                            Text(
                                                text = "Note: ${p.notes}",
                                                fontSize = 10.sp,
                                                color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { viewModel.deletePayment(p) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Delete",
                                            tint = GlassColors.SoftRed.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryBox(
    header: String,
    valStr: String,
    subtext: String,
    tint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.04f))
            .border(1.dp, (if (isDark) Color.White else Color.Black).copy(alpha = 0.06f), RoundedCornerShape(16.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = header,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = valStr,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtext,
            fontSize = 10.sp,
            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * 7. RECORD PAYMENT MODAL DIALOG
 */
@Composable
fun RecordPaymentModal(
    preFilledStudent: Student?,
    viewModel: FeeFlowViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val students by viewModel.students.collectAsState()
    val currency by viewModel.currencySymbol.collectAsState()

    var activeStudent by remember { mutableStateOf<Student?>(null) }
    var amountStr by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("Cash") } // "Cash", "Online"
    var notes by remember { mutableStateOf("") }
    var dateStr by remember { mutableStateOf("") }

    var expandedDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(preFilledStudent, students) {
        if (preFilledStudent != null) {
            activeStudent = preFilledStudent
            val bal = (preFilledStudent.totalFee - preFilledStudent.paidAmount).coerceAtLeast(0.0)
            amountStr = if (bal > 0) bal.toString() else ""
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        dateStr = sdf.format(Date())
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clickable(enabled = false) {}
                    .fillMaxWidth(0.9f)
                    .clip(RoundedCornerShape(28.dp))
                    .background(if (isDark) Color(0xFF100925) else Color.White)
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(GlassColors.EmeraldGreen.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        RoundedCornerShape(28.dp)
                    )
                    .padding(24.dp)
                    .testTag("record_payment_modal")
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Record Action Payment",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Profile Dropdown or static selection
                    if (preFilledStudent != null) {
                        // Static student view
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.05f))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Payee:",
                                    fontSize = 11.sp,
                                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                                )
                                Text(
                                    text = preFilledStudent.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDark) Color.White else Color.Black
                                )
                            }
                        }
                    } else {
                        // Custom profile selector dropdown
                        Column {
                            Text(
                                    text = "SELECT STUDENT PAYEE",
                                    fontSize = 11.sp,
                                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.05f))
                                    .border(1.dp, (if (isDark) Color.White else Color.Black).copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                    .clickable { expandedDropdown = !expandedDropdown }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("student_dropdown_trigger")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = activeStudent?.name ?: "Tap to choose student payee...",
                                        color = if (activeStudent != null) (if (isDark) Color.White else Color.Black) else (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                        fontSize = 14.sp
                                    )

                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = "Expand",
                                        tint = if (isDark) Color.White else Color.Black
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = expandedDropdown,
                                onDismissRequest = { expandedDropdown = false },
                                modifier = Modifier
                                    .fillMaxWidth(0.8f)
                                    .background(if (isDark) Color(0xFF160E33) else Color.White)
                            ) {
                                students.forEach { s ->
                                    val bal = (s.totalFee - s.paidAmount).coerceAtLeast(0.0)
                                    DropdownMenuItem(
                                        text = { Text("${s.name} (${s.studentClass}) • Bal: $currency${formatAmount(bal)}") },
                                        onClick = {
                                            activeStudent = s
                                            amountStr = if (bal > 0) bal.toString() else ""
                                            expandedDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Payment Amount Input
                    Column {
                        Text(
                            text = "PAYMENT AMOUNT ($currency)",
                            fontSize = 11.sp,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        GlassTextField(
                            value = amountStr,
                            onValueChange = { amountStr = it },
                            placeholder = "e.g. 500",
                            isDark = isDark,
                            icon = Icons.Rounded.Payments,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth().testTag("payment_amount_input")
                        )
                    }

                    // Payment Method Tab
                    Column {
                        Text(
                            text = "PAYMENT METHOD",
                            fontSize = 11.sp,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf("Cash", "Online").forEach { item ->
                                val selected = method == item
                                GlassButton(
                                    onClick = { method = item },
                                    isDark = isDark,
                                    testTag = "pay_method_btn_$item",
                                    accentColor = GlassColors.CyberTeal,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (item == "Cash") Icons.Rounded.Payments else Icons.Rounded.AccountBalance,
                                        contentDescription = item,
                                        tint = if (selected) GlassColors.CyberTeal else (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item,
                                        color = if (selected) {
                                            if (isDark) Color.White else GlassColors.RoyalPurple
                                        } else {
                                            (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                                        },
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }

                    // Action Notes (Optional)
                    Column {
                        Text(
                            text = "OPTIONAL AUDIT NOTES",
                            fontSize = 11.sp,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        GlassTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            placeholder = "e.g. Paid in cash at reception",
                            isDark = isDark,
                            icon = Icons.Rounded.Edit,
                            modifier = Modifier.fillMaxWidth().testTag("payment_notes_input")
                        )
                    }

                    // Date Input
                    Column {
                        Text(
                            text = "TRANSACTION DATE (yyyy-MM-dd)",
                            fontSize = 11.sp,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        GlassTextField(
                            value = dateStr,
                            onValueChange = { dateStr = it },
                            placeholder = "yyyy-MM-dd",
                            isDark = isDark,
                            icon = Icons.Rounded.CalendarToday,
                            modifier = Modifier.fillMaxWidth().testTag("payment_date_input")
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlassButton(
                            onClick = onDismiss,
                            isDark = isDark,
                            testTag = "record_payment_cancel_btn",
                            accentColor = GlassColors.SoftRed,
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Discard", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }

                        val canSave = activeStudent != null && amountStr.toDoubleOrNull() != null
                        GlassButton(
                            onClick = {
                                activeStudent?.let { s ->
                                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                                    viewModel.recordPayment(s.id, amt, method, notes, dateStr)
                                }
                                onDismiss()
                            },
                            isDark = isDark,
                            testTag = "record_payment_save_btn",
                            enabled = canSave,
                            accentColor = GlassColors.EmeraldGreen,
                            modifier = Modifier.weight(2f)
                        ) {
                            Text("Record Pay", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (canSave) (if (isDark) Color.White else Color.Black) else Color.Gray)
                        }
                    }
                }
            }
        }
    }
}

/**
 * 8. GLASS EXCEL PREVIEW MODAL
 */
@Composable
fun ExcelImportPreviewModal(
    previewRows: List<RawImportRow>,
    viewModel: FeeFlowViewModel,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val headers = remember(previewRows) {
        if (previewRows.isNotEmpty()) previewRows[0].values.keys.toList() else emptyList()
    }

    var selectedNameHeader by remember { mutableStateOf("") }
    var selectedClassHeader by remember { mutableStateOf("") }
    var selectedFeeHeader by remember { mutableStateOf("") }
    var selectedPaidHeader by remember { mutableStateOf("") }
    var selectedDueHeader by remember { mutableStateOf("") }
    var selectedContactHeader by remember { mutableStateOf("") }

    // Auto mapping trigger
    LaunchedEffect(headers) {
        headers.forEach { h ->
            val hl = h.lowercase()
            when {
                hl.contains("name") || hl == "student" -> selectedNameHeader = h
                hl.contains("class") || hl == "grade" || hl == "section" -> selectedClassHeader = h
                hl.contains("total") || hl.contains("fee") || hl == "amount" -> selectedFeeHeader = h
                hl.contains("paid") || hl.contains("collected") || hl.contains("paidamount") -> selectedPaidHeader = h
                hl.contains("due") || hl.contains("date") -> selectedDueHeader = h
                hl.contains("contact") || hl.contains("phone") || hl.contains("mobile") -> selectedContactHeader = h
            }
        }

        // fallbacks
        if (selectedNameHeader.isEmpty() && headers.isNotEmpty()) selectedNameHeader = headers[0]
        if (selectedClassHeader.isEmpty() && headers.size > 1) selectedClassHeader = headers[1]
        if (selectedFeeHeader.isEmpty() && headers.size > 2) selectedFeeHeader = headers[2]
        if (selectedPaidHeader.isEmpty() && headers.size > 3) selectedPaidHeader = headers[3]
        if (selectedDueHeader.isEmpty() && headers.size > 4) selectedDueHeader = headers[4]
        if (selectedContactHeader.isEmpty() && headers.size > 5) selectedContactHeader = headers[5]
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .clickable(enabled = false) {}
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(if (isDark) Color(0xFF100925) else Color.White)
                    .border(
                        1.dp,
                        Brush.linearGradient(
                            listOf(GlassColors.CyberTeal.copy(alpha = 0.4f), Color.Transparent)
                        ),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(20.dp)
                    .testTag("excel_preview_modal")
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Excel Bulk Import Preview",
                        color = if (isDark) Color.White else Color.Black,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Verify mapping of excel headers to local student profiles.",
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
                        fontSize = 12.sp
                    )

                    // Columns mapping lists
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.03f))
                            .border(1.dp, (if (isDark) Color.White else Color.Black).copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "MAPPED HEADERS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                        )

                        // Mapping selectors mapping
                        HeaderSelectorRow("Name:", selectedNameHeader, headers) { selectedNameHeader = it }
                        HeaderSelectorRow("Class:", selectedClassHeader, headers) { selectedClassHeader = it }
                        HeaderSelectorRow("Total Fee:", selectedFeeHeader, headers) { selectedFeeHeader = it }
                        HeaderSelectorRow("Paid:", selectedPaidHeader, headers) { selectedPaidHeader = it }
                        HeaderSelectorRow("Due Date:", selectedDueHeader, headers) { selectedDueHeader = it }
                        HeaderSelectorRow("Contact:", selectedContactHeader, headers) { selectedContactHeader = it }
                    }

                    // Table preview
                    Text(
                        text = "PREVIEW (TOP 3 ROWS IN FILE)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        previewRows.take(3).forEachIndexed { i, row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background((if (isDark) Color.White else Color.Black).copy(alpha = 0.04f))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Row ${i + 1}:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = GlassColors.CyberTeal,
                                    modifier = Modifier.width(50.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    row.values.forEach { (k, v) ->
                                        Text(
                                            text = "$k : $v",
                                            fontSize = 11.sp,
                                            color = if (isDark) Color.White else Color.Black
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Commit Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        GlassButton(
                            onClick = onDismiss,
                            isDark = isDark,
                            accentColor = GlassColors.SoftRed,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }

                        GlassButton(
                            onClick = {
                                val cMapping = FeeFlowViewModel.ColumnMapping(
                                    selectedNameHeader,
                                    selectedClassHeader,
                                    selectedFeeHeader,
                                    selectedPaidHeader,
                                    selectedDueHeader,
                                    selectedContactHeader
                                )
                                viewModel.executeImport(cMapping)
                            },
                            isDark = isDark,
                            accentColor = GlassColors.EmeraldGreen,
                            modifier = Modifier.weight(1.5f)
                        ) {
                            Text("Confirm Import", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = if (isDark) Color.White else Color.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderSelectorRow(
    label: String,
    selected: String,
    allHeaders: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = (if (LocalInspectionMode.current) Color.Black else Color.White).copy(alpha = 0.6f)
        )

        Box {
            Text(
                text = selected.ifBlank { "Unmapped" },
                color = GlassColors.CyberTeal,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .clickable { expanded = true }
                    .padding(4.dp)
            )

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF130A2F))
            ) {
                allHeaders.forEach { h ->
                    DropdownMenuItem(
                        text = { Text(h, color = Color.White) },
                        onClick = {
                            onSelect(h)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * 9. ENHANCED GLASS DUPLICATE DIALOG DISPATCHER
 */
@Composable
fun DuplicateCheckDialog(
    state: DuplicateCheckState,
    viewModel: FeeFlowViewModel,
    isDark: Boolean
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(if (isDark) Color(0xFF150A2E) else Color.White)
                .border(2.dp, GlassColors.AmberGlow.copy(alpha = 0.4f), RoundedCornerShape(24.dp))
                .padding(24.dp)
                .testTag("duplicate_dialog")
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Warning,
                    contentDescription = "Duplicate Check",
                    tint = GlassColors.AmberGlow,
                    modifier = Modifier.size(44.dp)
                )

                Text(
                    text = "Duplicate Profile Found",
                    color = if (isDark) Color.White else Color.Black,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Student with name \"${state.importing.name}\" and class \"${state.importing.studentClass}\" already exists inside local database. Choose resolution?",
                    color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.6f),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GlassButton(
                        onClick = { viewModel.handleDuplicateSkip() },
                        isDark = isDark,
                        testTag = "duplicate_skip_btn",
                        accentColor = GlassColors.SoftRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Skip Row", color = if (isDark) Color.White else Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    GlassButton(
                        onClick = { viewModel.handleDuplicateUpdate() },
                        isDark = isDark,
                        testTag = "duplicate_update_btn",
                        accentColor = GlassColors.CyberTeal,
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text("Update Details", color = if (isDark) Color.White else Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

/**
 * UTILS COMPOSABLE FALLBACKS
 */
@Composable
fun EmptyListFallback(
    isDark: Boolean,
    text: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Empty",
            tint = (if (isDark) Color.White else Color.Black).copy(alpha = 0.2f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = text,
            color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.5f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.8f)
        )
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isDark: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = (if (isDark) Color.White else Color.Black).copy(alpha = 0.4f)) },
        leadingIcon = { Icon(icon, contentDescription = null, tint = if (isDark) Color.White.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.4f)) },
        singleLine = true,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f),
            unfocusedContainerColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f),
            disabledContainerColor = (if (isDark) Color.White else Color.Black).copy(alpha = 0.05f),
            focusedIndicatorColor = GlassColors.CyberTeal,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = if (isDark) Color.White else Color.Black,
            unfocusedTextColor = if (isDark) Color.White else Color.Black
        ),
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = keyboardOptions,
        modifier = modifier.border(
            1.dp,
            (if (isDark) Color.White else Color.Black).copy(alpha = 0.08f),
            RoundedCornerShape(16.dp)
        )
    )
}

// Format Helpers
fun formatAmount(amt: Double): String {
    return if (amt % 1.0 == 0.0) {
        String.format(Locale.getDefault(), "%,.0f", amt)
    } else {
        String.format(Locale.getDefault(), "%,.2f", amt)
    }
}

fun formatDate(dateStr: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
