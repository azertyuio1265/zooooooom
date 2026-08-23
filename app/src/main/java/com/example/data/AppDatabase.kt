package com.example.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): Flow<List<Exercise>>

    @Query("SELECT * FROM exercises WHERE id = :id")
    suspend fun getExerciseById(id: String): Exercise?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<Exercise>)

    @Update
    suspend fun updateExercise(exercise: Exercise)
}

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses")
    fun getAllCourses(): Flow<List<Course>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Update
    suspend fun updateCourse(course: Course)
}

@Dao
interface TeacherDao {
    @Query("SELECT * FROM teachers")
    fun getAllTeachers(): Flow<List<Teacher>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTeachers(teachers: List<Teacher>)

    @Update
    suspend fun updateTeacher(teacher: Teacher)
}

@Dao
interface ChatMsgDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMsg>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(msg: ChatMsg)

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()
}

@Database(
    entities = [Exercise::class, Course::class, Teacher::class, ChatMsg::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun courseDao(): CourseDao
    abstract fun teacherDao(): TeacherDao
    abstract fun chatMsgDao(): ChatMsgDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zoomdz_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class ZoomDzRepository(private val db: AppDatabase) {
    val allExercises: Flow<List<Exercise>> = db.exerciseDao().getAllExercises()
    val allCourses: Flow<List<Course>> = db.courseDao().getAllCourses()
    val allTeachers: Flow<List<Teacher>> = db.teacherDao().getAllTeachers()
    val allMessages: Flow<List<ChatMsg>> = db.chatMsgDao().getAllMessages()

    suspend fun insertExercises(exercises: List<Exercise>) = db.exerciseDao().insertExercises(exercises)
    suspend fun updateExercise(exercise: Exercise) = db.exerciseDao().updateExercise(exercise)

    suspend fun insertCourses(courses: List<Course>) = db.courseDao().insertCourses(courses)
    suspend fun updateCourse(course: Course) = db.courseDao().updateCourse(course)

    suspend fun insertTeachers(teachers: List<Teacher>) = db.teacherDao().insertTeachers(teachers)
    suspend fun updateTeacher(teacher: Teacher) = db.teacherDao().updateTeacher(teacher)

    suspend fun insertMessage(msg: ChatMsg) = db.chatMsgDao().insertMessage(msg)
    suspend fun clearChat() = db.chatMsgDao().clearChat()
}
