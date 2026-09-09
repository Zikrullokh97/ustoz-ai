package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.StudentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(student: StudentEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(students: List<StudentEntity>)

  @Update
  suspend fun update(student: StudentEntity)

  @Query("DELETE FROM students WHERE id = :id AND teacherId = :teacherId")
  suspend fun delete(id: String, teacherId: String): Int

  @Query("SELECT * FROM students WHERE teacherId = :teacherId ORDER BY lastName ASC, firstName ASC")
  fun getStudentsFlow(teacherId: String): Flow<List<StudentEntity>>

  @Query("SELECT * FROM students WHERE teacherId = :teacherId ORDER BY lastName ASC, firstName ASC")
  suspend fun getStudents(teacherId: String): List<StudentEntity>

  @Query("SELECT * FROM students WHERE classId = :classId AND teacherId = :teacherId ORDER BY lastName ASC, firstName ASC")
  fun getStudentsByClassFlow(classId: String, teacherId: String): Flow<List<StudentEntity>>

  @Query("SELECT * FROM students WHERE classId = :classId AND teacherId = :teacherId ORDER BY lastName ASC, firstName ASC")
  suspend fun getStudentsByClass(classId: String, teacherId: String): List<StudentEntity>

  @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
  suspend fun getById(id: String): StudentEntity?

  @Query("SELECT * FROM students WHERE id = :id AND teacherId = :teacherId LIMIT 1")
  suspend fun getByIdAndTeacher(id: String, teacherId: String): StudentEntity?

  @Query("SELECT COUNT(*) FROM students WHERE classId = :classId AND teacherId = :teacherId")
  suspend fun countStudentsInClass(classId: String, teacherId: String): Int

  @Query("SELECT COUNT(*) FROM students WHERE teacherId = :teacherId")
  suspend fun countAllStudents(teacherId: String): Int

  @Query("SELECT COUNT(*) FROM students WHERE teacherId = :teacherId")
  fun countAllStudentsFlow(teacherId: String): Flow<Int>

  @Query(
    "SELECT * FROM students WHERE teacherId = :teacherId AND " +
        "(firstName LIKE '%' || :query || '%' OR lastName LIKE '%' || :query || '%' OR studentNumber LIKE '%' || :query || '%') " +
        "ORDER BY lastName ASC, firstName ASC"
  )
  suspend fun searchStudents(teacherId: String, query: String): List<StudentEntity>
}
