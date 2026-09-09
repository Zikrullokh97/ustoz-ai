package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AttendanceAuditEntity
import com.example.data.model.AttendanceEntity
import com.example.data.model.CameraEntity
import com.example.data.model.ClassEntity
import com.example.data.model.KTPDocumentEntity
import com.example.data.model.KTPEntryEntity
import com.example.data.model.LessonEntity
import com.example.data.model.ScheduleEntity
import com.example.data.model.SessionEntity
import com.example.data.model.StudentCardEntity
import com.example.data.model.StudentEntity
import com.example.data.model.StudentFaceEnrollmentEntity
import com.example.data.model.TeacherProfileEntity
import com.example.data.model.UserEntity

@Database(
  entities = [
    UserEntity::class,
    TeacherProfileEntity::class,
    SessionEntity::class,
    ClassEntity::class,
    StudentEntity::class,
    ScheduleEntity::class,
    LessonEntity::class,
    AttendanceEntity::class,
    KTPDocumentEntity::class,
    KTPEntryEntity::class,
    CameraEntity::class,
    StudentCardEntity::class,
    StudentFaceEnrollmentEntity::class,
    AttendanceAuditEntity::class
  ],
  version = 5,
  exportSchema = false
)
abstract class UstozDatabase : RoomDatabase() {
  abstract fun userDao(): UserDao
  abstract fun teacherProfileDao(): TeacherProfileDao
  abstract fun sessionDao(): SessionDao
  abstract fun classDao(): ClassDao
  abstract fun studentDao(): StudentDao
  abstract fun scheduleDao(): ScheduleDao
  abstract fun lessonDao(): LessonDao
  abstract fun attendanceDao(): AttendanceDao
  abstract fun ktpDao(): KTPDao
  abstract fun automatedAttendanceDao(): AutomatedAttendanceDao

  companion object {
    @Volatile
    private var INSTANCE: UstozDatabase? = null

    fun getDatabase(context: Context): UstozDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          UstozDatabase::class.java,
          "ustoz_ai.db"
        )
          .fallbackToDestructiveMigration()
          .build()
        INSTANCE = instance
        instance
      }
    }
  }
}
