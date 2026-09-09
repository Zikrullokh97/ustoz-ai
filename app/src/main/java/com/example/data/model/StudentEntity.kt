package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "students",
  foreignKeys = [
    ForeignKey(
      entity = ClassEntity::class,
      parentColumns = ["id"],
      childColumns = ["classId"],
      onDelete = ForeignKey.CASCADE
    ),
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["teacherId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [
    Index(value = ["classId"]),
    Index(value = ["teacherId"]),
    Index(value = ["teacherId", "classId"])
  ]
)
data class StudentEntity(
  @PrimaryKey
  val id: String,
  val classId: String,
  val teacherId: String,
  val firstName: String,
  val lastName: String,
  val studentNumber: String,
  val birthDate: String,
  val gender: String,
  val phone: String = "",
  val photo: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis()
) {
  val fullName: String
    get() = "$firstName $lastName".trim()
}
