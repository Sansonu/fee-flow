package com.example.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class FeeFlowRepository(private val database: AppDatabase) {
    private val studentDao = database.studentDao()
    private val paymentDao = database.paymentDao()

    val allStudents: Flow<List<Student>> = studentDao.getAllStudentsFlow()
    val allPayments: Flow<List<Payment>> = paymentDao.getAllPaymentsFlow()

    fun getPaymentsForStudent(studentId: String): Flow<List<Payment>> {
        return paymentDao.getPaymentsForStudentFlow(studentId)
    }

    suspend fun getStudentById(id: String): Student? {
        return studentDao.getStudentById(id)
    }

    suspend fun insertStudent(student: Student) {
        studentDao.insertStudent(student)
        FirestoreService.addStudent(student.toFirestore())
    }

    suspend fun insertStudents(students: List<Student>) {
        studentDao.insertStudents(students)
        students.forEach { FirestoreService.addStudent(it.toFirestore()) }
    }

    suspend fun updateStudent(student: Student) {
        studentDao.updateStudent(student)
        FirestoreService.addStudent(student.toFirestore())
    }

    suspend fun deleteStudent(studentId: String) {
        database.withTransaction {
            paymentDao.deletePaymentsForStudent(studentId)
            studentDao.deleteStudentById(studentId)
        }
    }

    suspend fun recordPayment(
        studentId: String,
        amount: Double,
        date: String,
        method: String,
        notes: String
    ): Payment {
        return database.withTransaction {
            val payment = Payment(
                studentId = studentId,
                amount = amount,
                date = date,
                method = method,
                notes = notes
            )
            // Save payment
            paymentDao.insertPayment(payment)
            
            // Recalculate Student status
            recalculateStudentDetails(studentId)
            
            // Sync recorded payment to Firebase Firestore
            FirestoreService.recordPayment(payment.toFirestore())
            
            payment
        }
    }

    suspend fun deletePayment(payment: Payment) {
        database.withTransaction {
            paymentDao.deletePayment(payment)
            recalculateStudentDetails(payment.studentId)
        }
    }

    suspend fun recalculateStudentDetails(studentId: String) {
        val student = studentDao.getStudentById(studentId) ?: return
        val studentPayments = paymentDao.getPaymentsForStudent(studentId)
        val newPaidAmount = studentPayments.sumOf { it.amount }
        val newStatus = when {
            newPaidAmount >= student.totalFee -> "Paid"
            newPaidAmount > 0 -> "Partial"
            else -> "Due"
        }
        val updatedStudent = student.copy(
            paidAmount = newPaidAmount,
            status = newStatus
        )
        studentDao.insertStudent(updatedStudent)
        
        // Sync calculated details back to cloud firestore
        FirestoreService.addStudent(updatedStudent.toFirestore())
    }
}

// Convert local domain models cleanly to custom Firestore schema definitions
fun Student.toFirestore(): FirestoreStudent {
    return FirestoreStudent(
        id = id,
        name = name,
        studentClass = studentClass,
        medium = medium,
        contact = contact,
        totalFee = totalFee,
        paidAmount = paidAmount,
        dueDate = dueDate,
        status = status,
        createdAt = createdAt
    )
}

fun Payment.toFirestore(docId: String = ""): FirestorePayment {
    return FirestorePayment(
        id = docId,
        studentId = studentId,
        amount = amount,
        date = date,
        method = method,
        notes = notes,
        createdAt = createdAt
    )
}

