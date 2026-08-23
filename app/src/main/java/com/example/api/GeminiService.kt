package com.example.api

import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class GeminiPart(val text: String)

@JsonClass(generateAdapter = true)
data class GeminiContent(val parts: List<GeminiPart>)

@JsonClass(generateAdapter = true)
data class GeminiRequest(
    val contents: List<GeminiContent>,
    val systemInstruction: GeminiContent? = null
)

@JsonClass(generateAdapter = true)
data class GeminiCandidate(val content: GeminiContent)

@JsonClass(generateAdapter = true)
data class GeminiResponse(val candidates: List<GeminiCandidate>?)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(logging)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(GeminiApiService::class.java)
    }
}

suspend fun generateTutorResponse(prompt: String, chatHistory: List<GeminiContent>): String {
    val apiKey = BuildConfig.GEMINI_API_KEY
    if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
        return "أهلاً بك! الرجاء إدخال مفتاح API الخاص بـ Gemini في الإعدادات لتفعيل الأستاذ الذكي."
    }

    val systemPrompt = """
        You are 'الاستاذ الذكي' (Smart Teacher/Tutor) for ZoomDz educational platform in Algeria. 
        Your goal is to explain and solve homework, academic concepts, and school lessons for Algerian students in an encouraging, friendly, and structured manner.
        - Always speak in friendly Algerian Arabic (mixture of clean Arabic and common Algerian Darja phrases) to make the student feel comfortable and understood.
        - You cover all levels: Primary (الابتدائي), Middle (المتوسط), High School (الثانوي), and Languages (اللغات).
        - Break down explanations step-by-step.
        - Do not give raw solutions directly; guide the student so they learn HOW to think and solve it.
        - Use emojis to make learning fun and engaging! 🇩🇿📚✨
    """.trimIndent()

    val contentsList = mutableListOf<GeminiContent>()
    // Add history
    contentsList.addAll(chatHistory)
    // Add active prompt
    contentsList.add(GeminiContent(parts = listOf(GeminiPart(text = prompt))))

    val request = GeminiRequest(
        contents = contentsList,
        systemInstruction = GeminiContent(parts = listOf(GeminiPart(text = systemPrompt)))
    )

    return try {
        val response = RetrofitClient.service.generateContent(apiKey, request)
        val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
        text ?: "عذراً، لم أستطع فهم الإجابة. يرجى المحاولة مجدداً."
    } catch (e: Exception) {
        "عذراً، حدث خطأ أثناء الاتصال بالأستاذ الذكي: ${e.message}"
    }
}
