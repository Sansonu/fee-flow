package com.example.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore

/**
 * Data representation conforming to the 'students' collection schema
 */
data class FirestoreStudent(
    val id: String = "",
    val name: String = "",
    val studentClass: String = "",
    val medium: String = "English", // "English" or "Hindi"
    val contact: String = "",
    val totalFee: Double = 0.0,
    val paidAmount: Double = 0.0,
    val dueDate: String = "", // e.g. "2026-06-15"
    val status: String = "Due", // "Paid", "Partial", "Due"
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "name" to name,
            "studentClass" to studentClass,
            "medium" to medium,
            "contact" to contact,
            "totalFee" to totalFee,
            "paidAmount" to paidAmount,
            "dueDate" to dueDate,
            "status" to status,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): FirestoreStudent {
            return FirestoreStudent(
                id = map["id"] as? String ?: "",
                name = map["name"] as? String ?: "",
                studentClass = map["studentClass"] as? String ?: "",
                medium = map["medium"] as? String ?: "English",
                contact = map["contact"] as? String ?: "",
                totalFee = (map["totalFee"] as? Number)?.toDouble() ?: 0.0,
                paidAmount = (map["paidAmount"] as? Number)?.toDouble() ?: 0.0,
                dueDate = map["dueDate"] as? String ?: "",
                status = map["status"] as? String ?: "Due",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Data representation conforming to the 'payments' collection schema
 */
data class FirestorePayment(
    val id: String = "",
    val studentId: String = "",
    val amount: Double = 0.0,
    val date: String = "", // e.g. "2026-06-09"
    val method: String = "", // "Cash", "Online"
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> {
        return mapOf(
            "id" to id,
            "studentId" to studentId,
            "amount" to amount,
            "date" to date,
            "method" to method,
            "notes" to notes,
            "createdAt" to createdAt
        )
    }

    companion object {
        fun fromMap(map: Map<String, Any>): FirestorePayment {
            return FirestorePayment(
                id = map["id"] as? String ?: "",
                studentId = map["studentId"] as? String ?: "",
                amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
                date = map["date"] as? String ?: "",
                method = map["method"] as? String ?: "",
                notes = map["notes"] as? String ?: "",
                createdAt = (map["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
            )
        }
    }
}

/**
 * Firebase Firestore integration helper for Tuition Intelligence.
 * Manages collections of 'students' and 'payments' along with automatic calculation triggers.
 */
object FirestoreService {
    private const val TAG = "FirestoreService"
    private var firestoreInstance: FirebaseFirestore? = null

    /**
     * Initializes Firebase App programmatically if it hasn't been initialized via google-services plugin,
     * ensuring a robust runtime experience.
     */
    fun init(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setProjectId("feeflow-tuition-intelligence")
                    .setApplicationId("com.aistudio.feeflow.tuition.qptf")
                    .setApiKey("mock-key-for-compilation-and-runtime-graceful-fallback")
                    .build()
                FirebaseApp.initializeApp(context, options)
                Log.d(TAG, "Firebase initialized programmatically.")
            } else {
                Log.d(TAG, "Firebase already initialized.")
            }
            firestoreInstance = FirebaseFirestore.getInstance()
            Log.d(TAG, "Firestore Instance obtained successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase/Firestore: ${e.message}", e)
        }
    }

    val db: FirebaseFirestore?
        get() = firestoreInstance

    /**
     * Helper to calculate student payment status ("Paid", "Partial", "Due")
     */
    fun calculatePaymentStatus(totalFee: Double, paidAmount: Double): String {
        return when {
            paidAmount >= totalFee -> "Paid"
            paidAmount > 0.0 -> "Partial"
            else -> "Due"
        }
    }

    /**
     * Adds a new student to the 'students' collection in Firestore
     */
    fun addStudent(student: FirestoreStudent, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val database = db ?: run {
            onFailure(Exception("Firestore is not initialized"))
            return
        }
        val documentRef = if (student.id.isNotEmpty()) {
            database.collection("students").document(student.id)
        } else {
            database.collection("students").document()
        }
        
        val recordToSave = student.copy(id = documentRef.id)
        documentRef.set(recordToSave.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "Student added with ID: ${documentRef.id}")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add student: ${e.message}", e)
                onFailure(e)
            }
    }

    /**
     * Records a payment to the 'payments' collection. Automatically triggers 
     * the recalculation of the parent student's payment status, total paid, and balance update.
     */
    fun recordPayment(payment: FirestorePayment, onSuccess: (String) -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val database = db ?: run {
            onFailure(Exception("Firestore is not initialized"))
            return
        }
        val docRef = database.collection("payments").document()
        val finalPayment = payment.copy(id = docRef.id)

        docRef.set(finalPayment.toMap())
            .addOnSuccessListener {
                Log.d(TAG, "Payment added with ID: ${docRef.id} for student: ${payment.studentId}")
                // Auto-recalculate and update student status and paid amounts
                recalculateStudentStatus(payment.studentId, 
                    onSuccess = {
                        onSuccess(docRef.id)
                    },
                    onFailure = { err ->
                        Log.w(TAG, "Payment registered, but student recalculation failed: ${err.message}")
                        onSuccess(docRef.id) // Still report success for payment recording
                    }
                )
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to record payment: ${e.message}", e)
                onFailure(e)
            }
    }

    /**
     * Automatically queries all payments for a student ID, aggregates the sum, 
     * updates the student model in 'students' collection, and calculates status.
     */
    fun recalculateStudentStatus(studentId: String, onSuccess: () -> Unit = {}, onFailure: (Exception) -> Unit = {}) {
        val database = db ?: run {
            onFailure(Exception("Firestore is not initialized"))
            return
        }

        // 1. Fetch the student first
        database.collection("students").document(studentId).get()
            .addOnSuccessListener { studentDoc ->
                if (!studentDoc.exists()) {
                    onFailure(Exception("Student with ID $studentId not found"))
                    return@addOnSuccessListener
                }
                
                val currentStudentMap = studentDoc.data ?: run {
                    onFailure(Exception("Student data is null"))
                    return@addOnSuccessListener
                }
                val student = FirestoreStudent.fromMap(currentStudentMap)

                // 2. Fetch all payments for this student
                database.collection("payments")
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .addOnSuccessListener { paymentsSnapshot ->
                        var totalPaidSum = 0.0
                        for (doc in paymentsSnapshot.documents) {
                            val amt = doc.getDouble("amount") ?: 0.0
                            totalPaidSum += amt
                        }

                        // 3. Compute target status automatically
                        val computedStatus = calculatePaymentStatus(student.totalFee, totalPaidSum)

                        // 4. Save updated student back to Firestore
                        val updatedStudent = student.copy(
                            paidAmount = totalPaidSum,
                            status = computedStatus
                        )

                        database.collection("students").document(studentId).set(updatedStudent.toMap())
                            .addOnSuccessListener {
                                Log.i(TAG, "Successfully recalculated. Updated Student ID $studentId: PaidAmount=$totalPaidSum, Status=$computedStatus")
                                onSuccess()
                            }
                            .addOnFailureListener { e ->
                                onFailure(e)
                            }
                    }
                    .addOnFailureListener { err ->
                        onFailure(err)
                    }
            }
            .addOnFailureListener { err ->
                onFailure(err)
            }
    }

    /**
     * Helper to read all students in real-time or via simple Query get
     */
    fun fetchStudents(onResult: (List<FirestoreStudent>) -> Unit, onError: (Exception) -> Unit) {
        val database = db ?: run {
            onError(Exception("Firestore is not initialized"))
            return
        }
        database.collection("students").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { FirestoreStudent.fromMap(it) }
                }
                onResult(list)
            }
            .addOnFailureListener { err ->
                onError(err)
            }
    }

    /**
     * Helper to read all payments in real-time or via query
     */
    fun fetchPayments(onResult: (List<FirestorePayment>) -> Unit, onError: (Exception) -> Unit) {
        val database = db ?: run {
            onError(Exception("Firestore is not initialized"))
            return
        }
        database.collection("payments").get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.data?.let { FirestorePayment.fromMap(it) }
                }
                onResult(list)
            }
            .addOnFailureListener { err ->
                onError(err)
            }
    }
}
