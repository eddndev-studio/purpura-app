package com.eddndev.purpura.ui

import com.eddndev.purpura.R
import com.eddndev.purpura.data.backup.InvalidBackupFileException
import com.eddndev.purpura.domain.backup.ImportMode
import com.eddndev.purpura.domain.backup.ImportResult
import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.repository.CloudBackup
import com.eddndev.purpura.domain.repository.DriveNotAuthorizedException
import com.eddndev.purpura.domain.usecase.backup.ImportEventsUseCase
import com.eddndev.purpura.ui.backup.RestoreViewModel
import com.eddndev.purpura.ui.support.FakeBackupFileStore
import com.eddndev.purpura.ui.support.FakeCloudBackupRepository
import com.eddndev.purpura.ui.support.FakeEventRepository
import com.eddndev.purpura.ui.support.sampleEvent
import com.eddndev.purpura.ui.support.sampleExportDocument
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

// Maquina de estados de RestoreViewModel: lee el archivo elegido, importa en modo `partial` y
// publica el resumen; distingue archivo invalido de fallo de red; y guarda contra reentradas.
@OptIn(ExperimentalCoroutinesApi::class)
class RestoreViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeEventRepository()
    private val fileStore = FakeBackupFileStore()
    private val cloudBackup = FakeCloudBackupRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = RestoreViewModel(
        importEvents = ImportEventsUseCase(repository),
        fileStore = fileStore,
        cloudBackup = cloudBackup,
    )

    private fun emptyStream(): java.io.InputStream = ByteArrayInputStream(ByteArray(0))

    @Test
    fun `restaurar un archivo valido importa en modo partial y publica el resultado`() = runTest(dispatcher) {
        val document = sampleExportDocument(listOf(sampleEvent("a"), sampleEvent("b")))
        fileStore.readResult = document
        val result = ImportResult(imported = 2, updated = 0, skipped = 0, failed = 0, errors = emptyList())
        repository.importResult = result
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.restoreFrom(::emptyStream)

        val request = repository.importRequests.single()
        assertEquals(ImportMode.partial, request.mode)
        assertEquals(document.events, request.events)
        assertEquals(result, viewModel.uiState.value.result)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `un archivo invalido avisa y no importa`() = runTest(dispatcher) {
        fileStore.readError = InvalidBackupFileException()
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.restoreFrom(::emptyStream)

        assertEquals(R.string.restore_error_invalid_file, viewModel.uiState.value.errorRes)
        assertTrue(repository.importRequests.isEmpty())
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `un fallo de red al importar emite aviso`() = runTest(dispatcher) {
        fileStore.readResult = sampleExportDocument(listOf(sampleEvent("a")))
        repository.importError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.restoreFrom(::emptyStream)

        assertEquals(R.string.error_network, viewModel.uiState.value.errorRes)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `listar Drive publica los respaldos disponibles`() = runTest(dispatcher) {
        val backups = listOf(
            CloudBackup("id-2", "purpura-respaldo-2026-06-07.json", Instant.EPOCH),
            CloudBackup("id-1", "purpura-respaldo-2026-06-01.json", Instant.EPOCH),
        )
        cloudBackup.listResult = backups
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.loadDriveBackups()

        assertEquals(backups, viewModel.uiState.value.driveBackups)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `restaurar desde Drive descarga importa y publica el resultado`() = runTest(dispatcher) {
        val document = sampleExportDocument(listOf(sampleEvent("a"), sampleEvent("b")))
        cloudBackup.downloadResult = document
        val result = ImportResult(imported = 2, updated = 0, skipped = 0, failed = 0, errors = emptyList())
        repository.importResult = result
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.restoreFromDrive("id-2")

        assertEquals("id-2", cloudBackup.downloaded.single())
        val request = repository.importRequests.single()
        assertEquals(ImportMode.partial, request.mode)
        assertEquals(document.events, request.events)
        assertEquals(result, viewModel.uiState.value.result)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `listar Drive sin autorizacion pide iniciar sesion con Google`() = runTest(dispatcher) {
        cloudBackup.listError = DriveNotAuthorizedException()
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.loadDriveBackups()

        assertEquals(R.string.restore_drive_auth_needed, viewModel.uiState.value.errorRes)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `no reimporta mientras hay una restauracion en curso`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        fileStore.readResult = sampleExportDocument(listOf(sampleEvent("a")))
        repository.importGate = gate
        repository.importResult = ImportResult(1, 0, 0, 0, emptyList())
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.restoreFrom(::emptyStream) // queda suspendido en el gate de import
        assertTrue(viewModel.uiState.value.isWorking)
        viewModel.restoreFrom(::emptyStream) // ignorado: ya hay una restauracion en curso

        gate.complete(Unit)
        assertEquals(1, repository.importRequests.size)
    }
}
