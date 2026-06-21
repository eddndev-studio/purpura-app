package com.eddndev.purpura.ui.addevent

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.ui.common.ARG_EVENT_ID
import com.eddndev.purpura.ui.common.RESULT_EVENT_EDITED
import com.eddndev.purpura.ui.compose.purpuraComposeView
import com.eddndev.purpura.ui.location.LocationPickerFragment
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

// Anadir / Editar Evento (REQ-ADD-001..009). Migrado a Compose (AddEventScreen): la pantalla posee el
// estado del formulario y los pickers de fecha/hora; el Fragment conserva los selectores externos
// (contacto del sistema, mapa) y permisos, y entrega sus resultados por [externalPicks]. La validacion
// y el guardado viven en AddEventViewModel.
@AndroidEntryPoint
class AddEventFragment : Fragment() {

    private val viewModel: AddEventViewModel by viewModels()
    private val editEventId: String? get() = arguments?.getString(ARG_EVENT_ID)?.takeIf { it.isNotBlank() }
    private val isEditMode get() = editEventId != null

    // Canal de resultados de selectores externos hacia la pantalla Compose. Un Channel BUFFERED (no un
    // SharedFlow replay=0) garantiza entrega exactly-once aunque haya un hueco entre el emit y la
    // (re)suscripcion del colector: al volver del selector de mapa la vista de este Fragment se
    // recrea y arranca un colector nuevo; el valor espera en el canal hasta que ese colector lo drena
    // (REQ-ADD-005: el pick de ubicacion no se puede perder).
    private val externalPicks = Channel<ExternalPick>(Channel.BUFFERED)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* ignorado */ }

    private val pickContact =
        registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            uri?.let(::applyPickedContact)
        }

    private val contactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pickContact.launch(null)
            } else {
                view?.let { Snackbar.make(it, R.string.add_event_contacts_denied, Snackbar.LENGTH_LONG).show() }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = purpuraComposeView {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        AddEventScreen(
            state = state,
            externalPicks = externalPicks.receiveAsFlow(),
            onBack = { findNavController().navigateUp() },
            onPickContact = ::ensureContactsPermissionThenPick,
            onPickLocation = ::openLocationPicker,
            onSubmit = ::submit,
            onClearFieldErrors = viewModel::clearFieldErrors,
            onPrefillHandled = viewModel::prefillHandled,
            onSaved = ::onSaved,
            onRetryLoad = { editEventId?.let(viewModel::startEditing) },
            onErrorShown = viewModel::errorShown,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // El selector de mapa devuelve lat/lng (+ etiqueta) por Fragment Result.
        setFragmentResultListener(LocationPickerFragment.REQUEST_KEY) { _, bundle ->
            externalPicks.trySend(
                ExternalPick.Location(
                    lat = bundle.getDouble(LocationPickerFragment.RESULT_LAT),
                    lng = bundle.getDouble(LocationPickerFragment.RESULT_LNG),
                    label = bundle.getString(LocationPickerFragment.RESULT_LABEL),
                ),
            )
        }
        // Modo edicion: startEditing es idempotente; el VM emite el prefill que la pantalla vuelca. El
        // titulo ("Editar evento" vs "Anadir Evento") lo decide AddEventScreen segun state.editing.
        editEventId?.let { id -> viewModel.startEditing(id) }
    }

    private fun submit(input: AddEventInput) {
        // Si el evento lleva recordatorio, pide el permiso de notificaciones (no bloquea el guardado).
        if (input.reminder != Reminder.none) requestNotificationPermissionIfNeeded()
        viewModel.submit(input)
    }

    private fun onSaved() {
        if (isEditMode) setFragmentResult(RESULT_EVENT_EDITED, Bundle.EMPTY)
        val controller = findNavController()
        if (controller.currentDestination?.id == R.id.addEventFragment) controller.navigateUp()
    }

    private fun openLocationPicker(lat: Double?, lng: Double?) {
        val args = bundleOf()
        if (lat != null && lng != null) {
            args.putDouble(LocationPickerFragment.ARG_LAT, lat)
            args.putDouble(LocationPickerFragment.ARG_LNG, lng)
        }
        if (findNavController().currentDestination?.id == R.id.addEventFragment) {
            findNavController().navigate(R.id.locationPickerFragment, args)
        }
    }

    private fun ensureContactsPermissionThenPick() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) pickContact.launch(null) else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
    }

    private fun applyPickedContact(uri: Uri) {
        val picked = ContactResolver.resolve(requireContext().contentResolver, uri)
        if (picked == null) {
            view?.let { Snackbar.make(it, R.string.add_event_contact_read_failed, Snackbar.LENGTH_LONG).show() }
            return
        }
        externalPicks.trySend(ExternalPick.Contact(picked.name, picked.ref))
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
