package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.KTPDocumentEntity
import com.example.data.model.KTPEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface KTPDao {

  // KTP Documents
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertDocument(document: KTPDocumentEntity)

  @Update
  suspend fun updateDocument(document: KTPDocumentEntity)

  @Query("DELETE FROM ktp_documents WHERE id = :id AND teacherId = :teacherId")
  suspend fun deleteDocument(id: String, teacherId: String): Int

  @Query("SELECT * FROM ktp_documents WHERE teacherId = :teacherId ORDER BY createdAt DESC")
  suspend fun getDocumentsByTeacher(teacherId: String): List<KTPDocumentEntity>

  @Query("SELECT * FROM ktp_documents WHERE teacherId = :teacherId ORDER BY createdAt DESC")
  fun getDocumentsByTeacherFlow(teacherId: String): Flow<List<KTPDocumentEntity>>

  @Query("SELECT * FROM ktp_documents WHERE id = :id AND teacherId = :teacherId LIMIT 1")
  suspend fun getDocumentById(id: String, teacherId: String): KTPDocumentEntity?

  // KTP Entries
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntries(entries: List<KTPEntryEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertEntry(entry: KTPEntryEntity)

  @Update
  suspend fun updateEntry(entry: KTPEntryEntity)

  @Query("DELETE FROM ktp_entries WHERE id = :id AND teacherId = :teacherId")
  suspend fun deleteEntry(id: String, teacherId: String): Int

  @Query("DELETE FROM ktp_entries WHERE ktpDocumentId = :documentId AND teacherId = :teacherId")
  suspend fun deleteEntriesByDocument(documentId: String, teacherId: String): Int

  @Query("SELECT * FROM ktp_entries WHERE ktpDocumentId = :documentId AND teacherId = :teacherId ORDER BY date ASC, lessonNumber ASC")
  suspend fun getEntriesByDocument(documentId: String, teacherId: String): List<KTPEntryEntity>

  @Query("SELECT * FROM ktp_entries WHERE id = :id AND teacherId = :teacherId LIMIT 1")
  suspend fun getEntryById(id: String, teacherId: String): KTPEntryEntity?

  @Query("SELECT * FROM ktp_entries WHERE classId = :classId AND teacherId = :teacherId ORDER BY date ASC, lessonNumber ASC")
  suspend fun getEntriesByClass(classId: String, teacherId: String): List<KTPEntryEntity>
}
