package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TeacherProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TeacherProfileDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(profile: TeacherProfileEntity)

  @Update
  suspend fun update(profile: TeacherProfileEntity)

  @Query("SELECT * FROM teacher_profiles WHERE userId = :userId LIMIT 1")
  suspend fun getByUserId(userId: String): TeacherProfileEntity?

  @Query("SELECT * FROM teacher_profiles WHERE userId = :userId LIMIT 1")
  fun getByUserIdFlow(userId: String): Flow<TeacherProfileEntity?>
}
