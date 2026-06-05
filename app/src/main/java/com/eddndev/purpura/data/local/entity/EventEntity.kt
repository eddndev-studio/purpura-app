package com.eddndev.purpura.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// Cache de lectura (06-app-architecture §5.2). Refleja la tabla `events` del backend
// (05-database-schema): columnas snake_case, instantes como epochMillis (Long) para
// indexar y filtrar por rango localmente, enums como codigo ASCII (String). Sin
// TypeConverters: el mapeo Instant<->Long y enum<->String vive en EventEntityMapper.
@Entity(
    tableName = "events",
    indices = [
        Index(value = ["user_id", "starts_at"]),
        Index(value = ["user_id", "event_type", "starts_at"]),
    ],
)
data class EventEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "event_type") val eventType: String,
    @ColumnInfo(name = "contact_name") val contactName: String,
    @ColumnInfo(name = "contact_ref") val contactRef: String?,
    @ColumnInfo(name = "location_lat") val locationLat: Double,
    @ColumnInfo(name = "location_lng") val locationLng: Double,
    @ColumnInfo(name = "location_label") val locationLabel: String?,
    @ColumnInfo(name = "description") val description: String,
    @ColumnInfo(name = "starts_at") val startsAtEpochMs: Long,
    @ColumnInfo(name = "event_status") val eventStatus: String,
    @ColumnInfo(name = "reminder_type") val reminderType: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMs: Long,
)
