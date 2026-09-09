package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "teacher_profiles",
  foreignKeys = [
    ForeignKey(
      entity = UserEntity::class,
      parentColumns = ["userId"],
      childColumns = ["userId"],
      onDelete = ForeignKey.CASCADE
    )
  ],
  indices = [Index(value = ["userId"], unique = true)]
)
data class TeacherProfileEntity(
  @PrimaryKey
  val profileId: String,
  val userId: String,
  val firstName: String,
  val lastName: String,
  val emailOrPhone: String,
  val language: String,
  val subject: String,
  val school: String = "",
  val schoolName: String = school,
  val experience: String = "",
  val experienceYears: Int = 0,
  val photo: String? = null,
  val phone: String = "",
  val email: String = "",
  val createdAt: Long = System.currentTimeMillis()
) {
  val fullName: String
    get() = "$firstName $lastName".trim()

  val effectiveSchoolName: String
    get() = schoolName.ifBlank { school }

  val effectivePhone: String
    get() = if (phone.isNotBlank()) phone else if (!emailOrPhone.contains("@")) emailOrPhone else ""

  val effectiveEmail: String
    get() = if (email.isNotBlank()) email else if (emailOrPhone.contains("@")) emailOrPhone else ""
}
