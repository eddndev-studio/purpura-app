package com.eddndev.purpura.ui

import com.eddndev.purpura.R
import com.eddndev.purpura.domain.error.DomainError
import com.eddndev.purpura.domain.usecase.backup.ExportEventsUseCase
import com.eddndev.purpura.domain.repository.DriveNotAuthorizedException
import com.eddndev.purpura.ui.backup.BackupViewModel
import com.eddndev.purpura.ui.support.FakeBackupFileStore
import com.eddndev.purpura.ui.support.FakeCloudBackupRepository
import com.eddndev.purpura.ui.support.FakeEventRepository
import com.eddndev.purpura.ui.support.sampleEvent
import com.eddndev.purpura.ui.support.sampleExportDocument
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException

// Maquina de estados de BackupViewModel: el flujo de dos pasos del Storage Access Framework
// (exportar -> abrir selector -> escribir), el atajo de respaldo vacio y las rutas de error.
@OptIn(ExperimentalCoroutinesApi::class)
class BackupViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private val repository = FakeEventRepository()
    private val fileStore = FakeBackupFileStore()
    private val cloudBackup = FakeCloudBackupRepository()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun buildViewModel() = BackupViewModel(
        exportEvents = ExportEventsUseCase(repository),
        fileStore = fileStore,
        cloudBackup = cloudBackup,
    )

    @Test
    fun `con eventos pide abrir el selector y al guardar reporta el conteo`() = runTest(dispatcher) {
        repository.exportResult = sampleExportDocument(listOf(sampleEvent("a"), sampleEvent("b")))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.prepareBackup()

        val fileName = viewModel.uiState.value.pendingFileName
        assertTrue(viewModel.uiState.value.isWorking)
        assertNull(viewModel.uiState.value.errorRes)
        assertTrue(fileName != null && fileName.startsWith("purpura-respaldo-") && fileName.endsWith(".json"))

        viewModel.launchHandled()
        assertNull(viewModel.uiState.value.pendingFileName)

        viewModel.saveBackup { ByteArrayOutputStream() }

        assertEquals(2, fileStore.written.single().count)
        assertEquals(2, viewModel.uiState.value.savedCount)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `un respaldo sin eventos avisa y no abre el selector`() = runTest(dispatcher) {
        repository.exportResult = sampleExportDocument(emptyList())
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.prepareBackup()

        assertEquals(R.string.backup_empty, viewModel.uiState.value.infoRes)
        assertNull(viewModel.uiState.value.pendingFileName)
        assertFalse(viewModel.uiState.value.isWorking)
        assertTrue(fileStore.written.isEmpty())
    }

    @Test
    fun `un fallo de red al exportar emite aviso y no abre el selector`() = runTest(dispatcher) {
        repository.exportError = DomainError.Network
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.prepareBackup()

        assertEquals(R.string.error_network, viewModel.uiState.value.errorRes)
        assertNull(viewModel.uiState.value.pendingFileName)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `un fallo al escribir el archivo emite aviso de guardado`() = runTest(dispatcher) {
        repository.exportResult = sampleExportDocument(listOf(sampleEvent("a")))
        fileStore.writeError = IOException("destino no disponible")
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.prepareBackup()
        viewModel.launchHandled()
        viewModel.saveBackup { ByteArrayOutputStream() }

        assertEquals(R.string.backup_error_save, viewModel.uiState.value.errorRes)
        assertFalse(viewModel.uiState.value.isWorking)
        assertTrue(fileStore.written.isEmpty())
    }

    @Test
    fun `cancelar el selector libera el estado y no escribe nada`() = runTest(dispatcher) {
        repository.exportResult = sampleExportDocument(listOf(sampleEvent("a")))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.prepareBackup()
        viewModel.launchHandled()
        viewModel.backupCancelled()

        assertFalse(viewModel.uiState.value.isWorking)

        viewModel.saveBackup { ByteArrayOutputStream() } // ya no hay documento pendiente
        assertTrue(fileStore.written.isEmpty())
    }

    @Test
    fun `respaldar en Drive exporta sube y reporta el conteo`() = runTest(dispatcher) {
        repository.exportResult = sampleExportDocument(listOf(sampleEvent("a"), sampleEvent("b")))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.backupToDrive()

        val (fileName, document) = cloudBackup.uploaded.single()
        assertTrue(fileName.startsWith("purpura-respaldo-") && fileName.endsWith(".json"))
        assertEquals(2, document.count)
        assertEquals(2, viewModel.uiState.value.savedCount)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `respaldar en Drive sin eventos avisa y no sube nada`() = runTest(dispatcher) {
        repository.exportResult = sampleExportDocument(emptyList())
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.backupToDrive()

        assertEquals(R.string.backup_empty, viewModel.uiState.value.infoRes)
        assertFalse(viewModel.uiState.value.isWorking)
        assertTrue(cloudBackup.uploaded.isEmpty())
    }

    @Test
    fun `respaldar en Drive sin autorizacion pide iniciar sesion con Google`() = runTest(dispatcher) {
        repository.exportResult = sampleExportDocument(listOf(sampleEvent("a")))
        cloudBackup.uploadError = DriveNotAuthorizedException()
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.backupToDrive()

        assertEquals(R.string.backup_drive_auth_needed, viewModel.uiState.value.errorRes)
        assertFalse(viewModel.uiState.value.isWorking)
    }

    @Test
    fun `no reexporta mientras hay un respaldo en curso`() = runTest(dispatcher) {
        val gate = CompletableDeferred<Unit>()
        repository.exportGate = gate
        repository.exportResult = sampleExportDocument(listOf(sampleEvent("a")))
        val viewModel = buildViewModel()
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.prepareBackup() // queda suspendido en el gate
        assertTrue(viewModel.uiState.value.isWorking)
        viewModel.prepareBackup() // ignorado: ya hay un respaldo en curso

        gate.complete(Unit)
        assertEquals(1, repository.exportQueries.size)
    }
}
