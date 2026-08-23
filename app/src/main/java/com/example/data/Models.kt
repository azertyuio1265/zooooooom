package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "exercises")
data class Exercise(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val gradeLevel: String, // e.g. "Primary", "Middle", "High", "Languages"
    val questionText: String,
    val optionA: String,
    val optionB: String,
    val optionC: String,
    val optionD: String,
    val correctAnswer: String, // "A", "B", "C", "D"
    val explanation: String,
    val isSolved: Boolean = false,
    val userAnswer: String = "",
    val isCorrect: Boolean = false
)

@Entity(tableName = "courses")
data class Course(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val gradeLevel: String,
    val teacherName: String,
    val description: String,
    val videoUrl: String,
    val enrolled: Boolean = false
)

@Entity(tableName = "teachers")
data class Teacher(
    @PrimaryKey val id: String,
    val name: String,
    val subject: String,
    val rating: Float,
    val isOnline: Boolean,
    val bio: String,
    val price: String, // e.g. "2000 DA/حصّة"
    val isBookmarked: Boolean = false
)

@Entity(tableName = "chat_messages")
data class ChatMsg(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "tutor"
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)
