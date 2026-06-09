package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "students")
data class Student(
    @PrimaryKey val id: String, // e.g. "FLW001"
    val name: String,
    val studentClass: String, // 'class' is a Kotlin reserved keyword
    val medium: String = "English", // "English" or "Hindi"
    val contact: String,
    val totalFee: Double,
    val paidAmount: Double,
    val dueDate: String, // String representation e.g. "2026-06-15"
    val status: String, // "Paid", "Partial", "Due"
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

@Entity(tableName = "payments")
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val amount: Double,
    val date: String, // e.g. "2026-06-09"
    val method: String, // "Cash", "Online"
    val notes: String,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable
