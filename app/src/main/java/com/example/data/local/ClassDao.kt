package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ClassEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClassDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(classEntity: ClassEntity)

  @Update
  suspend fun update(classEntity: ClassEntity)

  @Query("DELETE FROM classes WHERE id = :id AND teacherId = :teacherId")
  suspend fun delete(id: String, teacherId: String): Int

  @Query("SELECT * FROM classes WHERE teacherId = :teacherId ORDER BY grade ASC, section ASC")
  fun getClassesFlow(teacherId: String): Flow<List<ClassEntity>>

  @Query("SELECT * FROM classes WHERE teacherId = :teacherId ORDER BY grade ASC, section ASC")
  suspend fun getClasses(teacherId: String): List<ClassEntity>

  @Query("SELECT * FROM classes WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): ClassEntity?

  @Query("SELECT * FROM classes WHERE id = :id AND teacherId = :teacherId LIMIT 1")
  suspend fun getByIdAndTeacher(id: String, teacherId: String): ClassEntity?

  @Query("SELECT COUNT(*) FROM classes WHERE teacherId = :teacherId")
  suspend fun countClasses(teacherId: String): Int

  @Query("SELECT COUNT(*) FROM classes WHERE teacherId = :teacherId")
  fun countClassesFlow(teacherId: String): Flow<Int>
}
