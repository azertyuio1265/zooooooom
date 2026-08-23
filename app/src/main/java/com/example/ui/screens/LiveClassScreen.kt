package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.example.ui.ZoomDzViewModel
import com.example.ui.theme.AmberGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryEmerald

@Composable
fun LiveClassScreen(viewModel: ZoomDzViewModel) {
    val isActive by viewModel.liveClassActive.collectAsState()
    val whiteboardText by viewModel.liveWhiteboardText.collectAsState()
    val chatMessages by viewModel.liveClassChat.collectAsState()

    var userMessage by remember { mutableStateOf("") }
    var micMuted by remember { mutableStateOf(false) }
    var camOff by remember { mutableStateOf(false) }
    var handRaised by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("live_class_screen")
    ) {
        if (!isActive) {
            // Live class list / landing page
            Text(
                text = "بديل زووم التعليمي المباشر 📹",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "ادخل إلى غرف الفيديو التفاعلية والبث المباشر المجاني تماماً!",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Active Class Item Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مباشر الآن",
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "35 طالب متواجد",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "مراجعة شاملة لاختبار الرياضيات - الفصل الثالث",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الأستاذ: فريد مراد (ثانوية ديدوش مراد)",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "سنتناول في هذه الحصة مراجعة شاملة للدوال الخطية والتآلفية، طرق البرهان، وحل مسائل شاملة مشابهة لأسئلة الـ BEM.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { viewModel.toggleLiveClass() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SecondaryEmerald)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.VideoCall, contentDescription = "دخول")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "دخول الغرفة التفاعلية مجاناً", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            // Active classroom simulator view!
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.toggleLiveClass() }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "خروج", tint = Color.Red)
                }
                Text(
                    text = "غرفة الرياضيات التفاعلية المباشرة 🔴",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Red)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = "BEM 2026", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Central content: Row of Video & Whiteboard
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Whiteboard Screen (المصطبة الدراسية)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.2f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1E293B))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Gesture, contentDescription = "سبورة", tint = AmberGold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "السبورة التعليمية للأستاذ 📋",
                                color = AmberGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Divider(color = Color.White.copy(alpha = 0.1f), modifier = Modifier.padding(bottom = 8.dp))
                        Text(
                            text = whiteboardText,
                            color = Color.White,
                            fontSize = 14.sp,
                            lineHeight = 22.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // 2. Control Row and Feedbacks
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Controls
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(
                            onClick = { micMuted = !micMuted },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (micMuted) Color.Red.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (micMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "الميكروفون",
                                tint = if (micMuted) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { camOff = !camOff },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (camOff) Color.Red.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (camOff) Icons.Default.VideocamOff else Icons.Default.Videocam,
                                contentDescription = "الكاميرا",
                                tint = if (camOff) Color.Red else MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { handRaised = !handRaised },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = if (handRaised) AmberGold.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PanTool,
                                contentDescription = "رفع اليد",
                                tint = if (handRaised) AmberGold else MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (handRaised) {
                        Text(
                            text = "لقد رفعت يدك للمشاركة! ✋",
                            color = AmberGold,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // 3. Live Group Chat
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.5f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "الدردشة الحية للغرفة 💬",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        Divider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), modifier = Modifier.padding(bottom = 8.dp))

                        // Messages lists
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chatMessages) { (sender, text) ->
                                val isTeacher = sender.contains("أستاذ")
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = sender,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = if (isTeacher) SecondaryEmerald else MaterialTheme.colorScheme.primary
                                        )
                                        if (isTeacher) {
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SecondaryEmerald.copy(alpha = 0.15f))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Text(text = "مؤطر", color = SecondaryEmerald, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    Text(
                                        text = text,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }

                        // Input send message
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            TextField(
                                value = userMessage,
                                onValueChange = { userMessage = it },
                                placeholder = { Text("اكتب رسالة للمجموعة...", fontSize = 12.sp) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                maxLines = 1
                            )

                            IconButton(
                                onClick = {
                                    if (userMessage.isNotBlank()) {
                                        viewModel.sendLiveClassChat("أنا", userMessage)
                                        userMessage = ""
                                    }
                                },
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Send,
                                    contentDescription = "إرسال",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
