package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmberGold
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryEmerald

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var currentPage by remember { mutableStateOf(0) }

    val titles = listOf(
        "مرحباً بك في ZoomDz! ✨",
        "تمارين وحلول تفاعلية 📚",
        "الأستاذ الذكي رفيقك 🤖"
    )

    val descriptions = listOf(
        "منصتك الرائدة للتدريس والتواصل المرئي المباشر بين الطلبة والأساتذة بالجزائر (بديل زووم التعليمي الأول)، غرف فيديو تفاعلية، ومجموعات دراسية.",
        "استكشف التمارين المتوافقة تماماً مع المنهاج الدراسي الجزائري لمختلف الأطوار واللغات مع حلول نموذجية وتصحيح تفاعلي فوري.",
        "الأستاذ الذكي المدعوم بالذكاء الاصطناعي (Gemini) مستعد دائماً لشرح الدروس وتبسيط المسائل الصعبة وترجمة المصطلحات بالدرجة الجزائري!"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("onboarding_screen")
    ) {
        // Decorative background waves
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = PrimaryTeal.copy(alpha = 0.08f),
                radius = 350.dp.toPx(),
                center = Offset(size.width, 0f)
            )
            drawCircle(
                color = SecondaryEmerald.copy(alpha = 0.05f),
                radius = 280.dp.toPx(),
                center = Offset(0f, size.height * 0.8f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .windowInsetsPadding(WindowInsets.safeDrawing),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header (Logo Branding)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryTeal, SecondaryEmerald)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Z",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "ZoomDz",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Central Animated Card
            AnimatedContent(
                targetState = currentPage,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() togetherWith
                            slideOutHorizontally { width -> -width } + fadeOut()
                },
                label = "page_transition",
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .wrapContentHeight(Alignment.CenterVertically)
            ) { page ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    // Educational Vector Simulation
                    Box(
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(120.dp)) {
                            when (page) {
                                0 -> {
                                    // Laptop / Video Screen
                                    drawRect(
                                        color = PrimaryTeal,
                                        topLeft = Offset(10.dp.toPx(), 20.dp.toPx()),
                                        size = androidx.compose.ui.geometry.Size(100.dp.toPx(), 70.dp.toPx()),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                                    )
                                    drawLine(
                                        color = SecondaryEmerald,
                                        start = Offset(0f, 90.dp.toPx()),
                                        end = Offset(120.dp.toPx(), 90.dp.toPx()),
                                        strokeWidth = 6.dp.toPx()
                                    )
                                    drawCircle(
                                        color = AmberGold,
                                        radius = 8.dp.toPx(),
                                        center = Offset(60.dp.toPx(), 55.dp.toPx())
                                    )
                                }
                                1 -> {
                                    // Stack of Books
                                    drawRect(
                                        color = SecondaryEmerald,
                                        topLeft = Offset(20.dp.toPx(), 40.dp.toPx()),
                                        size = androidx.compose.ui.geometry.Size(80.dp.toPx(), 25.dp.toPx())
                                    )
                                    drawRect(
                                        color = PrimaryTeal,
                                        topLeft = Offset(15.dp.toPx(), 70.dp.toPx()),
                                        size = androidx.compose.ui.geometry.Size(90.dp.toPx(), 25.dp.toPx())
                                    )
                                    drawRect(
                                        color = AmberGold,
                                        topLeft = Offset(30.dp.toPx(), 15.dp.toPx()),
                                        size = androidx.compose.ui.geometry.Size(60.dp.toPx(), 20.dp.toPx())
                                    )
                                }
                                2 -> {
                                    // Robot Brain / AI
                                    drawCircle(
                                        color = PrimaryTeal,
                                        radius = 35.dp.toPx(),
                                        center = Offset(60.dp.toPx(), 50.dp.toPx()),
                                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                                    )
                                    drawCircle(
                                        color = SecondaryEmerald,
                                        radius = 8.dp.toPx(),
                                        center = Offset(45.dp.toPx(), 45.dp.toPx())
                                    )
                                    drawCircle(
                                        color = SecondaryEmerald,
                                        radius = 8.dp.toPx(),
                                        center = Offset(75.dp.toPx(), 45.dp.toPx())
                                    )
                                    drawLine(
                                        color = AmberGold,
                                        start = Offset(45.dp.toPx(), 70.dp.toPx()),
                                        end = Offset(75.dp.toPx(), 70.dp.toPx()),
                                        strokeWidth = 4.dp.toPx()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = titles[page],
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = descriptions[page],
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            // Bottom Navigation indicators & Actions
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    repeat(3) { index ->
                        val active = index == currentPage
                        val width by animateDpAsState(if (active) 24.dp else 8.dp, label = "dot")
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(RoundedCornerShape(4.dp))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                        )
                    }
                }

                // CTA Button
                Button(
                    onClick = {
                        if (currentPage < 2) {
                            currentPage++
                        } else {
                            onFinish()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("onboarding_cta_button"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (currentPage == 2) "ابدأ الآن" else "التالي",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "التالي",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
