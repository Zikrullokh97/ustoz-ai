package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "users",
  indices = [Index(value = ["emailOrPhone"], unique = true)]
)
data class UserEntity(
  @PrimaryKey
  val userId: String,
  val emailOrPhone: String,
  val passwordHash: String,
  val salt: String,
  val createdAt: Long = System.currentTimeMillis()
)
