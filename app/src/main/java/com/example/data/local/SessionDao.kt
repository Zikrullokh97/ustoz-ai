package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(session: SessionEntity)

  @Update
  suspend fun update(session: SessionEntity)

  @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
  suspend fun getActiveSession(): SessionEntity?

  @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
  fun getActiveSessionFlow(): Flow<SessionEntity?>

  @Query("UPDATE sessions SET isActive = 0")
  suspend fun deactivateAllSessions()

  @Query("SELECT * FROM sessions WHERE userId = :userId AND isPinSet = 1 ORDER BY createdAt DESC LIMIT 1")
  suspend fun getLatestConfiguredPinForUser(userId: String): SessionEntity?

  @Query("DELETE FROM sessions WHERE sessionId = :sessionId")
  suspend fun deleteSession(sessionId: String)

  @Query("DELETE FROM sessions WHERE isActive = 1")
  suspend fun deleteActiveSession()

  @Query("SELECT * FROM sessions WHERE accessToken = :token AND isActive = 1 LIMIT 1")
  suspend fun getSessionByAccessToken(token: String): SessionEntity?

  @Query("UPDATE sessions SET isActive = 0 WHERE userId = :userId")
  suspend fun deactivateSessionsForUser(userId: String)

  @Query("DELETE FROM sessions WHERE userId = :userId")
  suspend fun deleteSessionsForUser(userId: String)
}
