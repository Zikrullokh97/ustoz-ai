package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.UserEntity

@Dao
interface UserDao {
  @Insert(onConflict = OnConflictStrategy.ABORT)
  suspend fun insert(user: UserEntity)

  @Query("SELECT * FROM users WHERE LOWER(emailOrPhone) = LOWER(:emailOrPhone) LIMIT 1")
  suspend fun findByEmailOrPhone(emailOrPhone: String): UserEntity?

  @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
  suspend fun findById(userId: String): UserEntity?
}
