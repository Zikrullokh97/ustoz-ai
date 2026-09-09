package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "classes",
  foreignKeys = [
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["teacherId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["teacherId"]),
    Index(value = ["teacherId", "name"])
  ]
)
data class ClassEntity(
  @PrimaryKey
  val id: String,
  val teacherId: String,
  val name: String,
  val grade: Int,
  val section: String,
  val academicYear: String,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
)
