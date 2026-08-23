package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.api.GeminiContent
import com.example.api.GeminiPart
import com.example.api.generateTutorResponse
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ZoomDzViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = ZoomDzRepository(db)

    val exercises: StateFlow<List<Exercise>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val courses: StateFlow<List<Course>> = repository.allCourses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val teachers: StateFlow<List<Teacher>> = repository.allTeachers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMsg>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI state
    private val _points = MutableStateFlow(150) // Starting points
    val points: StateFlow<Int> = _points.asStateFlow()

    private val _isTutorLoading = MutableStateFlow(false)
    val isTutorLoading: StateFlow<Boolean> = _isTutorLoading.asStateFlow()

    // Live Class state
    private val _liveClassActive = MutableStateFlow(false)
    val liveClassActive: StateFlow<Boolean> = _liveClassActive.asStateFlow()

    private val _liveWhiteboardText = MutableStateFlow("مرحباً بكم في درس الرياضيات المباشر!\nالموضوع: الدوال وتطبيقاتها")
    val liveWhiteboardText: StateFlow<String> = _liveWhiteboardText.asStateFlow()

    private val _liveClassChat = MutableStateFlow<List<Pair<String, String>>>(
        listOf(
            "أحمد" to "السلام عليكم يا أستاذ، هل البث واضح؟",
            "أستاذ فريد" to "وعليكم السلام، نعم البث مستقر، سنبدأ بعد قليل.",
            "مريم" to "مرحباً بالجميع!"
        )
    )
    val liveClassChat: StateFlow<List<Pair<String, String>>> = _liveClassChat.asStateFlow()

    init {
        // Pre-populate data if database is empty
        viewModelScope.launch(Dispatchers.IO) {
            exercises.collectLatest { list ->
                if (list.isEmpty()) {
                    populateDefaultData()
                }
            }
        }
    }

    private suspend fun populateDefaultData() {
        val defaultExercises = listOf(
            Exercise(
                id = "ex_math_01",
                title = "حل المعادلة الرياضية",
                subject = "رياضيات",
                gradeLevel = "السنة الرابعة متوسط",
                questionText = "حل المعادلة التالية: 3x + 5 = 20. ما هي قيمة x؟",
                optionA = "x = 4",
                optionB = "x = 5",
                optionC = "x = 6",
                optionD = "x = 3",
                correctAnswer = "B",
                explanation = "نقوم بنقل 5 إلى الطرف الآخر فتصبح المعادلة: 3x = 20 - 5، أي 3x = 15. نقسم على 3 فتكون قيمة x = 5."
            ),
            Exercise(
                id = "ex_phys_01",
                title = "حساب السرعة المتوسطة",
                subject = "فيزياء",
                gradeLevel = "السنة الأولى ثانوي",
                questionText = "سيارة تقطع مسافة 150 كم في زمن قدره ساعتين (2h). كم تبلغ سرعتها المتوسطة بوحدة كم/سا؟",
                optionA = "65 كم/سا",
                optionB = "80 كم/سا",
                optionC = "75 كم/سا",
                optionD = "90 كم/سا",
                correctAnswer = "C",
                explanation = "السرعة المتوسطة = المسافة / الزمن. v = d / t = 150 / 2 = 75 كم/سا."
            ),
            Exercise(
                id = "ex_arab_01",
                title = "إعراب الفاعل والمفعول",
                subject = "لغة عربية",
                gradeLevel = "السنة الخامسة ابتدائي",
                questionText = "ما هو الإعراب الصحيح لكلمة 'المجتهدُ' في جملة: 'نجح التلميذُ المجتهدُ'؟",
                optionA = "فاعل مرفوع وعلامة رفعه الضمة",
                optionB = "صفة (نعت) مرفوعة وعلامة رفعها الضمة",
                optionC = "مفعول به منصوب",
                optionD = "مضاف إليه مجرور",
                correctAnswer = "B",
                explanation = "المجتهدُ نعت للتلميذِ مرفوع مثله وعلامة رفعه الضمة الظاهرة على آخره."
            ),
            Exercise(
                id = "ex_eng_01",
                title = "Irregular Verbs (Past Simple)",
                subject = "إنجليزية",
                gradeLevel = "اللغات الأجنبية",
                questionText = "What is the Past Simple form of the verb 'GO'?",
                optionA = "Went",
                optionB = "Gone",
                optionC = "Goes",
                optionD = "Goed",
                correctAnswer = "A",
                explanation = "The past simple form of the irregular verb 'go' is 'went'. 'Gone' is the past participle."
            )
        )

        val defaultCourses = listOf(
            Course(
                id = "c_01",
                title = "التحضير لشهادة التعليم المتوسط BEM: الدوال",
                subject = "رياضيات",
                gradeLevel = "السنة الرابعة متوسط",
                teacherName = "الأستاذ مراد فريد",
                description = "دورة تفصيلية ومكثفة تشرح الدوال الخطية والتآلفية بالتفصيل مع حل مسائل شهادات التعليم المتوسط السابقة.",
                videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
            ),
            Course(
                id = "c_02",
                title = "الميكانيك والحركة الدائرية المنتظمة",
                subject = "فيزياء",
                gradeLevel = "السنة الأولى ثانوي",
                teacherName = "الأستاذة حنان بلخير",
                description = "شرح مبسط لقوانين نيوتن والحركة الدائرية مع تطبيقات وتمارين محلولة لضمان الفهم والتحضير للفرص والامتحانات.",
                videoUrl = "https://www.w3schools.com/html/movie.mp4"
            ),
            Course(
                id = "c_03",
                title = "قواعد النحو والصرف للابتدائي",
                subject = "لغة عربية",
                gradeLevel = "السنة الخامسة ابتدائي",
                teacherName = "الأستاذ مصطفى بن علي",
                description = "تأسيس شامل في قواعد الإعراب من المبتدئ إلى الاحتراف مع نماذج تفاعلية لتبسيط لغتنا الجميلة لأطفالنا.",
                videoUrl = "https://www.w3schools.com/html/mov_bbb.mp4"
            )
        )

        val defaultTeachers = listOf(
            Teacher(
                id = "t_01",
                name = "الأستاذ مراد فريد",
                subject = "رياضيات فيزياء",
                rating = 4.9f,
                isOnline = true,
                bio = "أستاذ رياضيات ذو خبرة 12 سنة في تقديم الدروس الخصوصية والتحضير لشهادة التعليم المتوسط والباكالوريا بالجزائر العاصمة.",
                price = "1500 DA/حصّة"
            ),
            Teacher(
                id = "t_02",
                name = "الأستاذة حنان بلخير",
                subject = "علوم الطبيعة والحياة",
                rating = 4.8f,
                isOnline = true,
                bio = "مصححة لشهادات البكالوريا وخبيرة في تبسيط منهجية العلوم الطبيعية لطلبة الطور الثانوي والمتوسط.",
                price = "1800 DA/حصّة"
            ),
            Teacher(
                id = "t_03",
                name = "الأستاذ مصطفى بن علي",
                subject = "لغة عربية وفلسفة",
                rating = 4.7f,
                isOnline = false,
                bio = "أستاذ وباحث مهتم بتبسيط قواعد الإعراب والأدب لطلبة البكالوريا ومختلف الأطوار التعليمية بالجزائر.",
                price = "1200 DA/حصّة"
            )
        )

        repository.insertExercises(defaultExercises)
        repository.insertCourses(defaultCourses)
        repository.insertTeachers(defaultTeachers)
    }

    fun submitAnswer(exercise: Exercise, answer: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val isCorrect = answer == exercise.correctAnswer
            val updated = exercise.copy(
                isSolved = true,
                userAnswer = answer,
                isCorrect = isCorrect
            )
            repository.updateExercise(updated)

            if (isCorrect) {
                _points.update { it + 25 } // Award 25 points for correct answer
            }
        }
    }

    fun enrollInCourse(course: Course) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = course.copy(enrolled = !course.enrolled)
            repository.updateCourse(updated)
        }
    }

    fun toggleBookmarkTeacher(teacher: Teacher) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = teacher.copy(isBookmarked = !teacher.isBookmarked)
            repository.updateTeacher(updated)
        }
    }

    // Gemini AI Chat operations
    fun sendChatMessage(text: String) {
        if (text.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            val userMsg = ChatMsg(sender = "user", text = text)
            repository.insertMessage(userMsg)

            _isTutorLoading.value = true

            // Gather conversation context for Gemini
            val historyList = chatMessages.value.map {
                val role = if (it.sender == "user") "user" else "model"
                GeminiContent(parts = listOf(GeminiPart(text = it.text)))
            }

            val response = generateTutorResponse(text, historyList)

            val tutorMsg = ChatMsg(sender = "tutor", text = response)
            repository.insertMessage(tutorMsg)

            _isTutorLoading.value = false
        }
    }

    fun clearChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearChat()
        }
    }

    // Live Classroom actions
    fun toggleLiveClass() {
        _liveClassActive.update { !it }
    }

    fun sendLiveClassChat(user: String, text: String) {
        if (text.isBlank()) return
        _liveClassChat.update { list ->
            list + (user to text)
        }
    }

    fun claimReferralReward() {
        _points.update { it + 100 } // Reward 100 points
    }
}
