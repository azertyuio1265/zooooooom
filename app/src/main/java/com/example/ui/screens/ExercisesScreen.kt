package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Exercise
import com.example.ui.ZoomDzViewModel

@Composable
fun ExercisesScreen(viewModel: ZoomDzViewModel, onNavigateToTutor: (String) -> Unit) {
    val exercises by viewModel.exercises.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("exercises_screen")
    ) {
        // Top Header Info
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "التمارين والحلول التفاعلية 📝",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "حل التمارين واكسب نقاط وهدايا قيمة!",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (exercises.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(exercises) { exercise ->
                    ExerciseItemCard(
                        exercise = exercise,
                        onSubmit = { ans -> viewModel.submitAnswer(exercise, ans) },
                        onAskTutor = {
                            val prompt = "أهلاً يا أستاذ! هل يمكن أن تشرح لي بالتفصيل الممل كيف نحل هذا التمرين:\n\n" +
                                    "السؤال: ${exercise.questionText}\n" +
                                    "الخيارات:\n" +
                                    "أ) ${exercise.optionA}\n" +
                                    "ب) ${exercise.optionB}\n" +
                                    "ج) ${exercise.optionC}\n" +
                                    "د) ${exercise.optionD}\n\n" +
                                    "والجواب الصحيح هو الخيار (${exercise.correctAnswer}). يرجى شرح المفهوم step-by-step بالدرجة الجزائري وبشكل ممتع."
                            onNavigateToTutor(prompt)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ExerciseItemCard(
    exercise: Exercise,
    onSubmit: (String) -> Unit,
    onAskTutor: () -> Unit
) {
    var selectedOption by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Subject and Grade Tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = exercise.subject,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = exercise.gradeLevel,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Question Text
            Text(
                text = exercise.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = exercise.questionText,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Options list
            val options = listOf(
                "A" to exercise.optionA,
                "B" to exercise.optionB,
                "C" to exercise.optionC,
                "D" to exercise.optionD
            )

            options.forEach { (key, valText) ->
                val isSelected = selectedOption == key
                val isAnswered = exercise.isSolved
                val optionBg = when {
                    isAnswered && exercise.correctAnswer == key -> Color(0xFFD1FAE5) // Green for correct
                    isAnswered && exercise.userAnswer == key && !exercise.isCorrect -> Color(0xFFFEE2E2) // Red for wrong selected
                    isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.background
                }
                val optionBorder = when {
                    isAnswered && exercise.correctAnswer == key -> Color(0xFF10B981)
                    isAnswered && exercise.userAnswer == key && !exercise.isCorrect -> Color(0xFFEF4444)
                    isSelected -> MaterialTheme.colorScheme.primary
                    else -> Color.Transparent
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clickable(enabled = !isAnswered) {
                            selectedOption = key
                        },
                    colors = CardDefaults.cardColors(containerColor = optionBg),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, optionBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected || (isAnswered && exercise.correctAnswer == key)) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when(key) {
                                    "A" -> "أ"
                                    "B" -> "ب"
                                    "C" -> "ج"
                                    else -> "د"
                                },
                                color = if (isSelected || (isAnswered && exercise.correctAnswer == key)) Color.White else MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = valText,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Result or CTA
            if (!exercise.isSolved) {
                Button(
                    onClick = {
                        if (selectedOption.isNotEmpty()) {
                            onSubmit(selectedOption)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = selectedOption.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(text = "تأكيد الإجابة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (exercise.isCorrect) Color(0xFFD1FAE5).copy(alpha = 0.3f)
                            else Color(0xFFFEE2E2).copy(alpha = 0.3f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = if (exercise.isCorrect) Icons.Default.CheckCircle else Icons.Default.Error,
                            contentDescription = "النتيجة",
                            tint = if (exercise.isCorrect) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (exercise.isCorrect) "إجابة صحيحة! (+25 نقطة) 🎉" else "إجابة خاطئة! حظاً أوفر المرة القادمة.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (exercise.isCorrect) Color(0xFF065F46) else Color(0xFF991B1B)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "الشرح المبسط: ${exercise.explanation}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = onAskTutor,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Psychology,
                                contentDescription = "الأستاذ الذكي"
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "شرح مفصل مع الأستاذ الذكي 🤖", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
