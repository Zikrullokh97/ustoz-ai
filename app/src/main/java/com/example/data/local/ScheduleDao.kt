package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScheduleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduleDao {

  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(schedule: ScheduleEntity)

  @Update
  suspend fun update(schedule: ScheduleEntity)

  @Query("DELETE FROM schedules WHERE id = :id AND teacherId = :teacherId")
  suspend fun delete(id: String, teacherId: String): Int

  @Query("SELECT * FROM schedules WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): ScheduleEntity?

  @Query("SELECT * FROM schedules WHERE teacherId = :teacherId AND active = 1 ORDER BY dayOfWeek ASC, startTime ASC")
  suspend fun getSchedulesByTeacher(teacherId: String): List<ScheduleEntity>

  @Query("SELECT * FROM schedules WHERE teacherId = :teacherId AND active = 1 ORDER BY dayOfWeek ASC, startTime ASC")
  fun getSchedulesByTeacherFlow(teacherId: String): Flow<List<ScheduleEntity>>

  @Query("SELECT * FROM schedules WHERE classId = :classId AND teacherId = :teacherId AND active = 1 ORDER BY dayOfWeek ASC, startTime ASC")
  suspend fun getSchedulesByClass(classId: String, teacherId: String): List<ScheduleEntity>

  @Query("SELECT * FROM schedules WHERE teacherId = :teacherId AND dayOfWeek = :dayOfWeek AND active = 1 ORDER BY startTime ASC")
  suspend fun getSchedulesByDay(teacherId: String, dayOfWeek: Int): List<ScheduleEntity>

  /**
   * Checks for schedule conflicts for a teacher on a specific day of week.
   * Two periods [s1, e1) and [s2, e2) overlap if s1 < e2 and s2 < e1,
   * or if lessonNumber matches.
   */
  @Query("""
    SELECT * FROM schedules 
    WHERE teacherId = :teacherId 
      AND dayOfWeek = :dayOfWeek 
      AND active = 1 
      AND (:excludeId IS NULL OR id != :excludeId)
      AND (
        (startTime < :endTime AND :startTime < endTime)
        OR (lessonNumber = :lessonNumber)
      )
  """)
  suspend fun findConflicts(
    teacherId: String,
    dayOfWeek: Int,
    startTime: String,
    endTime: String,
    lessonNumber: Int,
    excludeId: String? = null
  ): List<ScheduleEntity>
}
