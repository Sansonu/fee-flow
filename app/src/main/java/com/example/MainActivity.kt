package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.Student
import com.example.ui.components.DynamicIslandNav
import com.example.ui.components.GlassBackground
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.FeeFlowViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: FeeFlowViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firestore Service
        com.example.data.FirestoreService.init(this)
        
        // Edge-to-edge support for notch safety and transparent system nav bars
        enableEdgeToEdge()

        setContent {
            val isDark by viewModel.isDarkMode.collectAsState()
            val currentScreen by viewModel.currentScreen.collectAsState()
            val selectedStudent by viewModel.selectedStudent.collectAsState()

            // Modals and dialog states
            val importPreviewRows by viewModel.importPreviewRows.collectAsState()
            val duplicateCheckRow by viewModel.duplicateCheckRow.collectAsState()

            var recordPaymentStudent by remember { mutableStateOf<Student?>(null) }
            var isRecordPaymentOpen by remember { mutableStateOf(false) }

            MyApplicationTheme(darkTheme = isDark, dynamicColor = false) {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    // Full shifting mesh gradient background
                    GlassBackground(isDark = isDark) {
                        // Core Screen content with smooth spring crossfades
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = innerPadding.calculateTopPadding()) // respect camera notch
                        ) {
                            AnimatedContent(
                                targetState = currentScreen,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(350)) togetherWith
                                            fadeOut(animationSpec = tween(300))
                                },
                                label = "screen_routing"
                            ) { screen ->
                                when (screen) {
                                    AppScreen.DASHBOARD -> {
                                        DashboardScreen(
                                            viewModel = viewModel,
                                            isDark = isDark,
                                            onRecordPaymentClick = { student ->
                                                recordPaymentStudent = student
                                                isRecordPaymentOpen = true
                                            }
                                        )
                                    }
                                    AppScreen.STUDENTS -> {
                                        StudentsScreen(
                                            viewModel = viewModel,
                                            isDark = isDark,
                                            onAddStudentClick = {
                                                viewModel.editingStudent.value = null
                                                viewModel.navigateTo(AppScreen.ADD_EDIT_STUDENT)
                                            },
                                            onStudentSelect = { student ->
                                                viewModel.selectStudent(student)
                                            },
                                            onRecordPayment = { student ->
                                                recordPaymentStudent = student
                                                isRecordPaymentOpen = true
                                            }
                                        )
                                    }
                                    AppScreen.ADD_EDIT_STUDENT -> {
                                        AddEditStudentScreen(
                                            viewModel = viewModel,
                                            isDark = isDark,
                                            onCancel = {
                                                viewModel.editingStudent.value = null
                                                viewModel.navigateTo(AppScreen.STUDENTS)
                                            }
                                        )
                                    }
                                    AppScreen.HISTORY -> {
                                        PaymentHistoryScreen(
                                            viewModel = viewModel,
                                            isDark = isDark
                                        )
                                    }
                                    AppScreen.SETTINGS -> {
                                        SettingsScreen(
                                            viewModel = viewModel,
                                            isDark = isDark
                                        )
                                    }
                                }
                            }

                            // Dynamic Island Bottom navigation pill (centered floating over content)
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .windowInsetsPadding(WindowInsets.navigationBars) // safe systems nav gesture pill padding
                                    .padding(bottom = 12.dp)
                            ) {
                                val islandState by viewModel.islandState.collectAsState()
                                val totalPending by viewModel.totalPending.collectAsState()
                                val totalStudents by viewModel.totalStudentsCount.collectAsState()

                                DynamicIslandNav(
                                    currentScreen = currentScreen,
                                    islandState = islandState,
                                    isDark = isDark,
                                    onScreenSelected = { screen ->
                                        viewModel.selectStudent(null)
                                        viewModel.navigateTo(screen)
                                    },
                                    onRecordPaymentClick = {
                                        recordPaymentStudent = null
                                        isRecordPaymentOpen = true
                                    },
                                    onOverdueClick = {
                                        // clicking overdue pill directs to students list under due entries
                                        viewModel.selectStudent(null)
                                        viewModel.navigateTo(AppScreen.STUDENTS)
                                    }
                                )
                            }
                        }

                        // Floating Overlays & Sheet drawers
                        
                        // 1. Student detail sheet drawer
                        selectedStudent?.let { student ->
                            StudentDetailSheet(
                                student = student,
                                viewModel = viewModel,
                                isDark = isDark,
                                onDismiss = { viewModel.selectStudent(null) },
                                onEdit = {
                                    viewModel.editingStudent.value = student
                                    viewModel.selectStudent(null)
                                    viewModel.navigateTo(AppScreen.ADD_EDIT_STUDENT)
                                },
                                onRecordPayment = {
                                    recordPaymentStudent = student
                                    isRecordPaymentOpen = true
                                }
                            )
                        }

                        // 2. Record action payment dialog modal
                        if (isRecordPaymentOpen) {
                            RecordPaymentModal(
                                preFilledStudent = recordPaymentStudent,
                                viewModel = viewModel,
                                isDark = isDark,
                                onDismiss = {
                                    isRecordPaymentOpen = false
                                    recordPaymentStudent = null
                                }
                            )
                        }

                        // 3. Importer Preview Modal
                        importPreviewRows?.let { rows ->
                            ExcelImportPreviewModal(
                                previewRows = rows,
                                viewModel = viewModel,
                                isDark = isDark,
                                onDismiss = { viewModel.importPreviewRows.value = null }
                            )
                        }

                        // 4. Importer Duplicate State Dialogue
                        duplicateCheckRow?.let { state ->
                            DuplicateCheckDialog(
                                state = state,
                                viewModel = viewModel,
                                isDark = isDark
                            )
                        }
                    }
                }
            }
        }
    }
}
