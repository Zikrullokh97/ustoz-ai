package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AttendanceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AttendanceDao {

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertOrUpdate(attendance: AttendanceEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(attendances: List<AttendanceEntity>)

  @Update
  suspend fun update(attendance: AttendanceEntity)

  @Query("SELECT * FROM attendances WHERE lessonId = :lessonId")
  suspend fun getByLesson(lessonId: String): List<AttendanceEntity>

  @Query("SELECT * FROM attendances WHERE lessonId = :lessonId")
  fun getByLessonFlow(lessonId: String): Flow<List<AttendanceEntity>>

  @Query("SELECT * FROM attendances WHERE lessonId = :lessonId AND studentId = :studentId LIMIT 1")
  suspend fun getByLessonAndStudent(lessonId: String, studentId: String): AttendanceEntity?

  @Query("SELECT * FROM attendances WHERE studentId = :studentId ORDER BY createdAt DESC")
  suspend fun getByStudent(studentId: String): List<AttendanceEntity>

  @Query("SELECT COUNT(*) FROM attendances WHERE lessonId = :lessonId")
  suspend fun countAttendanceForLesson(lessonId: String): Int

  @Query("DELETE FROM attendances WHERE lessonId = :lessonId")
  suspend fun deleteByLesson(lessonId: String): Int
}
