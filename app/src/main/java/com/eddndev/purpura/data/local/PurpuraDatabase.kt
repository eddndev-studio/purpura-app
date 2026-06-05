package com.eddndev.purpura.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.eddndev.purpura.data.local.dao.EventDao
import com.eddndev.purpura.data.local.entity.EventEntity

// Base de datos local de Room (cache de lectura, 06-app-architecture §5.2). No es store
// autoritativo: la API Go es la fuente de verdad (REQ-NFR-005). Sin TypeConverters: las
// columnas ya son tipos primitivos (Long/String/Double).
@Database(
    entities = [EventEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class PurpuraDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        const val NAME = "purpura.db"
    }
}
