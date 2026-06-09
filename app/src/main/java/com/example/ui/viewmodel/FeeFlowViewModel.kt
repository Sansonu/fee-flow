package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.FeeFlowRepository
import com.example.data.Payment
import com.example.data.Student
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

sealed class IslandState {
    object Idle : IslandState()
    data class OverdueAlert(val count: Int) : IslandState()
    data class ExpandedStats(val totalDue: Double, val totalStudents: Int) : IslandState()
    data class Success(val message: String) : IslandState()
    object Celebration : IslandState()
}

class FeeFlowViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    private val repository: FeeFlowRepository
    private val prefs = context.getSharedPreferences("feeflow_prefs", Context.MODE_PRIVATE)

    // Screen State
    var currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    
    // Form States or currently selected student
    val selectedStudent = MutableStateFlow<Student?>(null)
    val editingStudent = MutableStateFlow<Student?>(null) // null means adding new

    // Settings States (backed by SharedPrefs)
    val teacherName = MutableStateFlow(prefs.getString("teacher_name", "Teacher Sarah") ?: "Teacher Sarah")
    val currencySymbol = MutableStateFlow(prefs.getString("currency_symbol", "$") ?: "$")
    val defaultDueDay = MutableStateFlow(prefs.getInt("default_due_day", 15))
    val isDarkMode = MutableStateFlow(prefs.getBoolean("is_dark_mode", true))

    // DB flows
    val students: StateFlow<List<Student>>
    val payments: StateFlow<List<Payment>>

    // Combined metric calculations
    val totalCollected: StateFlow<Double>
    val totalPending: StateFlow<Double>
    val totalStudentsCount: StateFlow<Int>
    val overdueCount: StateFlow<Int>
    val dueThisWeekList: StateFlow<List<Student>>

    // Dynamic Island State
    val islandState = MutableStateFlow<IslandState>(IslandState.Idle)
    
    // Import States
    val importPreviewRows = MutableStateFlow<List<RawImportRow>?>(null)
    val duplicateCheckRow = MutableStateFlow<DuplicateCheckState?>(null)
    private var pendingImportList = mutableListOf<Student>()
    private var currentImportIndex = 0

    init {
        val database = AppDatabase.getDatabase(context)
        repository = FeeFlowRepository(database)

        students = repository.allStudents.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        payments = repository.allPayments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Metrics
        totalCollected = payments.map { list -> list.sumOf { it.amount } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        totalStudentsCount = students.map { it.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        totalPending = students.map { list -> list.sumOf { (it.totalFee - it.paidAmount).coerceAtLeast(0.0) } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

        // Overdue standard checking: status != "Paid" AND due date < today
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        overdueCount = students.map { list ->
            val todayStr = sdf.format(Date())
            list.count { it.status != "Paid" && it.dueDate < todayStr }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        // Due This Week
        dueThisWeekList = students.map { list ->
            val today = Calendar.getInstance()
            val endOfWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 7) }
            val todayStr = sdf.format(today.time)
            val nextWeekStr = sdf.format(endOfWeek.time)
            list.filter { it.status != "Paid" && it.dueDate >= todayStr && it.dueDate <= nextWeekStr }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        // Monitor overdue to show alert in Dynamic Island
        viewModelScope.launch {
            overdueCount.collect { count ->
                if (count > 0 && islandState.value is IslandState.Idle) {
                    islandState.value = IslandState.OverdueAlert(count)
                } else if (count == 0 && islandState.value is IslandState.OverdueAlert) {
                    islandState.value = IslandState.Idle
                }
            }
        }
    }

    // Actions
    fun updateTeacherName(name: String) {
        teacherName.value = name
        prefs.edit().putString("teacher_name", name).apply()
    }

    fun updateCurrencySymbol(sym: String) {
        currencySymbol.value = sym
        prefs.edit().putString("currency_symbol", sym).apply()
    }

    fun updateDefaultDueDay(day: Int) {
        defaultDueDay.value = day
        prefs.edit().putInt("default_due_day", day).apply()
    }

    fun toggleTheme() {
        val nextVal = !isDarkMode.value
        isDarkMode.value = nextVal
        prefs.edit().putBoolean("is_dark_mode", nextVal).apply()
    }

    // Navigation and screen management
    fun navigateTo(screen: AppScreen) {
        currentScreen.value = screen
    }

    fun selectStudent(student: Student?) {
        selectedStudent.value = student
    }

    fun triggerIslandCelebration() {
        viewModelScope.launch {
            islandState.value = IslandState.Celebration
            delay(3500)
            resetIslandState()
        }
    }

    fun triggerIslandSuccess(message: String) {
        viewModelScope.launch {
            islandState.value = IslandState.Success(message)
            delay(3500)
            resetIslandState()
        }
    }

    private fun resetIslandState() {
        val count = overdueCount.value
        if (count > 0) {
            islandState.value = IslandState.OverdueAlert(count)
        } else {
            islandState.value = IslandState.Idle
        }
    }

    // Database Actions
    fun saveStudent(student: Student) {
        viewModelScope.launch {
            repository.insertStudent(student)
            triggerIslandSuccess("Student Saved!")
        }
    }

    fun deleteStudent(id: String) {
        viewModelScope.launch {
            repository.deleteStudent(id)
            if (selectedStudent.value?.id == id) {
                selectedStudent.value = null
            }
            triggerIslandSuccess("Student Deleted")
        }
    }

    fun recordPayment(studentId: String, amount: Double, method: String, notes: String, date: String) {
        viewModelScope.launch {
            repository.recordPayment(studentId, amount, date, method, notes)
            // Trigger celebration
            triggerIslandCelebration()
            // reload selected student to update visual panel
            val updated = repository.getStudentById(studentId)
            if (selectedStudent.value?.id == studentId) {
                selectedStudent.value = updated
            }
        }
    }

    fun deletePayment(payment: Payment) {
        viewModelScope.launch {
            repository.deletePayment(payment)
            triggerIslandSuccess("Payment Deleted")
            // refresh active selected
            val pId = payment.studentId
            val updated = repository.getStudentById(pId)
            if (selectedStudent.value?.id == pId) {
                selectedStudent.value = updated
            }
        }
    }

    fun markFullyPaid(student: Student) {
        viewModelScope.launch {
            val remain = (student.totalFee - student.paidAmount).coerceAtLeast(0.0)
            if (remain > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                repository.recordPayment(student.id, remain, sdf.format(Date()), "Cash", "Quick fully paid action")
                triggerIslandCelebration()
                Log.d("FeeFlow", "Student ${student.name} fully paid")
            }
        }
    }

    // Bulk Importer
    fun processCSVFileUri(uri: Uri) {
        viewModelScope.launch {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                val csvContent = inputStream?.bufferedReader()?.use { it.readText() }
                if (!csvContent.isNullOrBlank()) {
                    parseCSV(csvContent)
                } else {
                    triggerIslandSuccess("Invalid CSV File")
                }
            } catch (e: Exception) {
                Log.e("FeeFlow", "CSV load failed", e)
                triggerIslandSuccess("Failed to read CSV")
            }
        }
    }

    fun parseCSV(csvText: String) {
        // Simple robust CSV parser
        val lines = csvText.lines().filter { it.isNotBlank() }
        if (lines.isEmpty()) {
            triggerIslandSuccess("Empty Content")
            return
        }

        val rawHeaders = parseCSVLine(lines[0])
        val matchedMapping = autoDetectHeaders(rawHeaders)

        val previewRows = mutableListOf<RawImportRow>()
        for (i in 1 until lines.size) {
            val columns = parseCSVLine(lines[i])
            if (columns.isNotEmpty()) {
                val dataMap = mutableMapOf<String, String>()
                rawHeaders.forEachIndexed { index, header ->
                    if (index < columns.size) {
                        dataMap[header] = columns[index]
                    }
                }
                previewRows.add(RawImportRow(dataMap))
            }
        }

        importPreviewRows.value = previewRows
    }

    private fun parseCSVLine(line: String): List<String> {
        val result = mutableListOf<String>()
        var currentToken = StringBuilder()
        var insideQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            if (c == '"') {
                insideQuotes = !insideQuotes
            } else if (c == ',' && !insideQuotes) {
                result.add(currentToken.toString().trim())
                currentToken = StringBuilder()
            } else {
                currentToken.append(c)
            }
            i++
        }
        result.add(currentToken.toString().trim())
        return result
    }

    private fun autoDetectHeaders(headers: List<String>): ColumnMapping {
        var nameCol = ""
        var classCol = ""
        var feeCol = ""
        var paidCol = ""
        var dueCol = ""
        var contactCol = ""

        headers.forEach { header ->
            val h = header.lowercase()
            when {
                h.contains("name") || h == "student" -> nameCol = header
                h.contains("class") || h == "grade" || h == "section" -> classCol = header
                h.contains("total") || h.contains("fee") || h == "amount" -> feeCol = header
                h.contains("paid") || h.contains("collected") || h.contains("paidamount") -> paidCol = header
                h.contains("due") || h.contains("date") -> dueCol = header
                h.contains("contact") || h.contains("phone") || h.contains("mobile") -> contactCol = header
            }
        }

        // fallbacks if empty
        if (nameCol.isEmpty() && headers.isNotEmpty()) nameCol = headers[0]
        if (classCol.isEmpty() && headers.size > 1) classCol = headers[1]
        if (feeCol.isEmpty() && headers.size > 2) feeCol = headers[2]
        if (paidCol.isEmpty() && headers.size > 3) paidCol = headers[3]
        if (dueCol.isEmpty() && headers.size > 4) dueCol = headers[4]
        if (contactCol.isEmpty() && headers.size > 5) contactCol = headers[5]

        return ColumnMapping(nameCol, classCol, feeCol, paidCol, dueCol, contactCol)
    }

    data class ColumnMapping(
        val nameCol: String,
        val classCol: String,
        val feeCol: String,
        val paidCol: String,
        val dueCol: String,
        val contactCol: String
    )

    fun executeImport(mapping: ColumnMapping) {
        val rows = importPreviewRows.value ?: return
        val listToImport = mutableListOf<Student>()
        val startIdIndex = students.value.size + 1
        
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val defaultDateStr = sdf.format(Date())

        rows.forEachIndexed { idx, row ->
            val name = row.values[mapping.nameCol] ?: "Student ${startIdIndex + idx}"
            val studentClass = row.values[mapping.classCol] ?: "Class A"
            val totalFee = row.values[mapping.feeCol]?.toDoubleOrNull() ?: 1000.0
            val paidAmount = row.values[mapping.paidCol]?.toDoubleOrNull() ?: 0.0
            val dueDateStr = row.values[mapping.dueCol]?.ifBlank { null } ?: defaultDateStr
            val contact = row.values[mapping.contactCol] ?: "None"

            val studentId = "FLW" + String.format("%03d", startIdIndex + idx)
            val calculatedStatus = when {
                paidAmount >= totalFee -> "Paid"
                paidAmount > 0 -> "Partial"
                else -> "Due"
            }

            var detectedMedium = "English"
            if (studentClass.contains("Hindi", ignoreCase = true) || studentClass.contains("हिंदी", ignoreCase = true)) {
                detectedMedium = "Hindi"
            }

            val s = Student(
                id = studentId,
                name = name,
                studentClass = studentClass,
                medium = detectedMedium,
                contact = contact,
                totalFee = totalFee,
                paidAmount = paidAmount,
                dueDate = dueDateStr,
                status = calculatedStatus
            )
            listToImport.add(s)
        }

        pendingImportList = listToImport
        currentImportIndex = 0
        importPreviewRows.value = null // dismiss modal
        processNextImportRow()
    }

    private fun processNextImportRow() {
        if (currentImportIndex >= pendingImportList.size) {
            triggerIslandSuccess("✓ Imported ${pendingImportList.size} students")
            pendingImportList.clear()
            return
        }

        val importingStudent = pendingImportList[currentImportIndex]
        // Check for duplicates
        viewModelScope.launch {
            val isDuplicate = students.value.find { 
                it.name.equals(importingStudent.name, ignoreCase = true) && 
                it.studentClass.equals(importingStudent.studentClass, ignoreCase = true) 
            }

            if (isDuplicate != null) {
                duplicateCheckRow.value = DuplicateCheckState(importingStudent, isDuplicate)
            } else {
                repository.insertStudent(importingStudent)
                // Add default transaction if there is paid amount
                if (importingStudent.paidAmount > 0) {
                    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    repository.recordPayment(importingStudent.id, importingStudent.paidAmount, sdf.format(Date()), "Cash", "Import initial balance")
                }
                currentImportIndex++
                processNextImportRow()
            }
        }
    }

    fun handleDuplicateSkip() {
        duplicateCheckRow.value = null
        currentImportIndex++
        processNextImportRow()
    }

    fun handleDuplicateUpdate() {
        val state = duplicateCheckRow.value ?: return
        viewModelScope.launch {
            val studentToUpdate = state.existing.copy(
                totalFee = state.importing.totalFee, // update fee
                contact = state.importing.contact,
                dueDate = state.importing.dueDate
            )
            repository.updateStudent(studentToUpdate)
            
            // if paidAmount has increased in the file, record a payment
            val diff = state.importing.paidAmount - state.existing.paidAmount
            if (diff > 0) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                repository.recordPayment(studentToUpdate.id, diff, sdf.format(Date()), "Cash", "Import updated balance")
            }
            
            duplicateCheckRow.value = null
            currentImportIndex++
            processNextImportRow()
        }
    }

    // Direct Import Template Downloader
    fun shareCSVTemplate() {
        viewModelScope.launch {
            try {
                val templateText = "Name,Class,Total Fee,Amount Paid,Due Date,Contact\n" +
                        "John Doe,Class 10A,1500,500,2026-06-15,9876543210\n" +
                        "Jane Smith,Class 12B,2000,2000,2026-06-20,8765432109\n" +
                        "Robert Johnson,Class 9C,1200,0,2026-06-12,7654321098\n"

                val file = File(context.cacheDir, "FeeFlow_Template.csv")
                file.writeText(templateText)

                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "FeeFlow Import Template")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Share Template")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                triggerIslandSuccess("Template Shared!")
            } catch (e: Exception) {
                Log.e("FeeFlow", "Template share failed", e)
                triggerIslandSuccess("Could not share template")
            }
        }
    }

    // Export completely to CSV
    fun exportAllToCSV() {
        viewModelScope.launch {
            try {
                val studentList = students.value
                val paymentsList = payments.value

                val csvText = StringBuilder()
                csvText.append("ID,Name,Class,Total Fee,Amount Paid,Balance,Due Date,Status,Contact\n")
                studentList.forEach { s ->
                    val bal = (s.totalFee - s.paidAmount).coerceAtLeast(0.0)
                    csvText.append("${s.id},\"${s.name.replace("\"", "\"\"")}\",\"${s.studentClass.replace("\"", "\"\"")}\",${s.totalFee},${s.paidAmount},${bal},${s.dueDate},${s.status},\"${s.contact.replace("\"", "\"\"")}\"\n")
                }

                val file = File(context.cacheDir, "FeeFlow_Export.csv")
                file.writeText(csvText.toString())

                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "FeeFlow Students Export")
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                val chooser = Intent.createChooser(intent, "Export Data")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
                triggerIslandSuccess("Export Shared!")
            } catch (e: Exception) {
                Log.e("FeeFlow", "Export failed", e)
                triggerIslandSuccess("Export failing")
            }
        }
    }
}

enum class AppScreen {
    DASHBOARD,
    STUDENTS,
    ADD_EDIT_STUDENT,
    HISTORY,
    SETTINGS
}

data class RawImportRow(
    val values: Map<String, String>
)

data class DuplicateCheckState(
    val importing: Student,
    val existing: Student
)
