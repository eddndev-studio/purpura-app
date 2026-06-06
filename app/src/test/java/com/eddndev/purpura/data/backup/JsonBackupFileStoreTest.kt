package com.eddndev.purpura.data.backup

import com.eddndev.purpura.data.remote.mapper.EventRemoteMapper
import com.eddndev.purpura.domain.model.Contact
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Location
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.ui.support.sampleExportDocument
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Instant

// El codec REAL del archivo de respaldo (no el fake). Es el corazon de Respaldo/Restaurar y el
// unico componente cuya correctitud no se puede confirmar en dispositivo, asi que el ida-y-vuelta
// write->read se verifica aqui, incluido el camino de los opcionales nulos (contact.ref / label).
@OptIn(ExperimentalCoroutinesApi::class)
class JsonBackupFileStoreTest {

    private val store = JsonBackupFileStore(
        moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build(),
        mapper = EventRemoteMapper(),
        io = UnconfinedTestDispatcher(),
    )

    private fun event(
        id: String,
        contactRef: String?,
        locationLabel: String?,
    ): Event = Event(
        id = id,
        userId = "u1",
        type = EventType.cita,
        contact = Contact("Ana", contactRef),
        location = Location(19.4, -99.1, locationLabel),
        description = "Reunion de avance",
        startsAt = Instant.parse("2026-06-10T15:30:00Z"),
        status = EventStatus.pendiente,
        reminder = Reminder.ten_minutes_before,
        createdAt = Instant.parse("2026-06-01T10:00:00Z"),
        updatedAt = Instant.parse("2026-06-02T11:00:00Z"),
    )

    @Test
    fun `el ida y vuelta preserva el documento, incluidos los opcionales nulos`() = runTest {
        val populated = event("full", contactRef = "ana@mail.com", locationLabel = "Campus Sur")
        val sparse = event("sparse", contactRef = null, locationLabel = null)
        val document = sampleExportDocument(listOf(populated, sparse))

        val bytes = ByteArrayOutputStream()
        store.write(document) { bytes }
        val restored = store.read { ByteArrayInputStream(bytes.toByteArray()) }

        assertEquals(document, restored)
        // El camino opcional-nulo realmente se ejercita (no se volvio "" al serializar).
        val restoredSparse = restored.events.single { it.id == "sparse" }
        assertNull(restoredSparse.contact.ref)
        assertNull(restoredSparse.location.label)
    }

    @Test(expected = InvalidBackupFileException::class)
    fun `un contenido que no es json se reporta como archivo invalido`() = runTest {
        store.read { "no soy json".byteInputStream() }
    }

    @Test(expected = InvalidBackupFileException::class)
    fun `un archivo vacio se reporta como archivo invalido`() = runTest {
        store.read { "".byteInputStream() }
    }

    @Test(expected = InvalidBackupFileException::class)
    fun `un json ajeno al esquema de respaldo se reporta como archivo invalido`() = runTest {
        store.read { """{"otra":"cosa"}""".byteInputStream() }
    }
}
