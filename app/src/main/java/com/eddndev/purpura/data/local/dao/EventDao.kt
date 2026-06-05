package com.eddndev.purpura.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.eddndev.purpura.data.local.entity.EventEntity
import kotlinx.coroutines.flow.Flow

// DAO del cache. Lecturas reactivas (Flow) para inicio y calendario; upsert/borrado que
// el repositorio usa para sincronizar el cache tras cada llamada exitosa a la API
// (06-app-architecture §5.2). La verdad la escribe siempre el backend.
@Dao
interface EventDao {

    @Query("SELECT * FROM events WHERE starts_at BETWEEN :from AND :to ORDER BY starts_at ASC")
    fun observeBetween(from: Long, to: Long): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun findById(id: String): EventEntity?

    @Upsert
    suspend fun upsertAll(events: List<EventEntity>)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM events")
    suspend fun clear()
}
