package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.LessonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LessonDao {

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(lesson: LessonEntity)

  @Update
  suspend fun update(lesson: LessonEntity)

  @Query("DELETE FROM lessons WHERE id = :id AND teacherId = :teacherId")
  suspend fun delete(id: String, teacherId: String): Int

  @Query("SELECT * FROM lessons WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): LessonEntity?

  @Query("SELECT * FROM lessons WHERE teacherId = :teacherId AND date = :date ORDER BY startTime ASC")
  suspend fun getLessonsByDate(teacherId: String, date: String): List<LessonEntity>

  @Query("SELECT * FROM lessons WHERE teacherId = :teacherId AND date = :date ORDER BY startTime ASC")
  fun getLessonsByDateFlow(teacherId: String, date: String): Flow<List<LessonEntity>>

  @Query("SELECT * FROM lessons WHERE classId = :classId AND teacherId = :teacherId ORDER BY date DESC, startTime ASC")
  suspend fun getLessonsByClass(classId: String, teacherId: String): List<LessonEntity>

  @Query("SELECT * FROM lessons WHERE classId = :classId AND teacherId = :teacherId AND date = :date ORDER BY startTime ASC")
  suspend fun getLessonsByDateAndClass(classId: String, teacherId: String, date: String): List<LessonEntity>

  @Query("""
    SELECT * FROM lessons 
    WHERE teacherId = :teacherId 
      AND classId = :classId 
      AND subjectId = :subjectId 
      AND date = :date 
      AND startTime = :startTime 
    LIMIT 1
  """)
  suspend fun findDuplicate(
    teacherId: String,
    classId: String,
    subjectId: String,
    date: String,
    startTime: String
  ): LessonEntity?

  @Query("""
    SELECT * FROM lessons 
    WHERE teacherId = :teacherId 
      AND date = :date 
      AND (:excludeId IS NULL OR id != :excludeId)
      AND startTime < :endTime 
      AND :startTime < endTime
  """)
  suspend fun findTimeConflicts(
    teacherId: String,
    date: String,
    startTime: String,
    endTime: String,
    excludeId: String? = null
  ): List<LessonEntity>

  @Query("SELECT COUNT(*) FROM lessons WHERE teacherId = :teacherId AND date = :date")
  suspend fun countTodayLessons(teacherId: String, date: String): Int
}
