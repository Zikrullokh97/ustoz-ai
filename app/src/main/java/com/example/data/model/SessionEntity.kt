package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "sessions",
  foreignKeys = [
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["userId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["userId"]), Index(value = ["isActive"])]
)
data class SessionEntity(
  @PrimaryKey
  val sessionId: String,
  val userId: String,
  val accessToken: String,
  val refreshToken: String,
  val pinHash: String? = null,
  val pinSalt: String? = null,
  val isPinSet: Boolean = false,
  val isUnlocked: Boolean = false,
  val isActive: Boolean = true,
  val createdAt: Long = System.currentTimeMillis(),
  val expiresAt: Long = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000) // 30 days
)
