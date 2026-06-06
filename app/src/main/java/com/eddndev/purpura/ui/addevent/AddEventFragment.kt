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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentAddEventBinding
import com.eddndev.purpura.domain.model.Event
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder
import com.eddndev.purpura.ui.common.ARG_EVENT_ID
import com.eddndev.purpura.ui.common.RESULT_EVENT_EDITED
import com.eddndev.purpura.ui.location.LocationPickerFragment
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Anadir Evento (REQ-ADD-001..009). Formulario que reune tipo/contacto/lugar/fecha/hora/estatus/
// recordatorio y delega en AddEventViewModel. Al guardar con exito regresa a Inicio, que ya observa
// el cache de Room (create() escribe en cache) y mostrara el evento si cae en la ventana proxima.
//
// Alcance v1: la ubicacion se captura como etiqueta de texto; el selector de mapa (lat/lng) llega
// cuando se resuelva el billing de Maps. Los selectores de fecha/hora se descartan al rotar (sus
// listeners no sobreviven) pero la seleccion ya elegida se conserva en onSaveInstanceState.
@AndroidEntryPoint
class AddEventFragment : Fragment() {

    private var _binding: FragmentAddEventBinding? = null
    private val binding get() = _binding!!
    private val viewModel: AddEventViewModel by viewModels()

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null

    // Coordenadas elegidas en el selector de mapa (null = el usuario no abrio el mapa).
    private var pickedLat: Double? = null
    private var pickedLng: Double? = null

    // Telefono del contacto elegido en el selector del sistema (null = nombre escrito a mano o sin
    // telefono). Se limpia si el usuario edita el nombre manualmente: el ref deja de corresponder.
    private var pickedContactRef: String? = null
    // Evita que el watcher del campo borre el ref cuando somos nosotros quienes ponemos el nombre
    // (al elegir contacto, prefill de edicion o restauracion tras rotar).
    private var settingContactFromPicker = false

    // Modo edicion (formulario reutilizado desde el Detalle): se deriva del argumento eventId. Cuando
    // esta presente, el VM carga el evento y este Fragment vuelca sus valores en los widgets.
    private var editEventId: String? = null
    private val isEditMode get() = editEventId != null

    // Permiso de notificaciones (API 33+). Se solicita al guardar con un recordatorio activo; si el
    // usuario lo niega, la alarma se programa igual pero no producira aviso visible (decision suya).
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* resultado ignorado */ }

    // Selector de contactos del sistema (REQ-ADD-002). Devuelve el URI del contacto elegido (o null si
    // el usuario cancela); de ahi resolvemos nombre + telefono. Solo se lanza con READ_CONTACTS dado.
    private val pickContact =
        registerForActivityResult(ActivityResultContracts.PickContact()) { uri ->
            uri?.let(::applyPickedContact)
        }

    // Permiso de contactos (runtime). Se pide al tocar "Elegir contacto"; si se concede, abre el
    // selector enseguida; si se niega, avisa y el usuario escribe el nombre a mano (sin vincular).
    private val contactsPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                pickContact.launch(null)
            } else {
                Snackbar.make(binding.root, R.string.add_event_contacts_denied, Snackbar.LENGTH_LONG).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAddEventBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        savedInstanceState?.let(::restoreDateTime)
        savedInstanceState?.let(::restoreLocation)

        binding.dateButton.setOnClickListener { openDatePicker() }
        binding.timeButton.setOnClickListener { openTimePicker() }
        binding.pickLocationButton.setOnClickListener { openLocationPicker() }
        binding.pickContactButton.setOnClickListener { ensureContactsPermissionThenPick() }
        binding.saveButton.setOnClickListener { onSave() }

        listenForPickedLocation()

        // Al editar un campo se limpia su error para no dejarlo "pegado" mientras el usuario corrige.
        binding.descriptionInput.doAfterTextChanged { viewModel.clearFieldErrors() }
        binding.contactInput.doAfterTextChanged {
            viewModel.clearFieldErrors()
            // Edicion manual del nombre: el ref deja de corresponder al contacto vinculado, salvo que
            // seamos nosotros poniendo el nombre (elegir contacto / prefill / restauracion).
            if (!settingContactFromPicker) clearContactLink()
        }

        // Modo edicion: el id llega como argumento (sobrevive a la recreacion). startEditing es
        // idempotente, asi que llamarlo en cada onViewCreated no recarga ni re-rellena.
        editEventId = arguments?.getString(ARG_EVENT_ID)?.takeIf { it.isNotBlank() }
        editEventId?.let { id ->
            applyEditModeChrome()
            viewModel.startEditing(id)
        }

        observeState()
    }

    // Ajustes visuales del modo edicion: titulo, texto del boton y ocultar el estatus (en edicion el
    // Detalle es el dueno del estatus via su propio endpoint). Se aplican en cada recreacion.
    private fun applyEditModeChrome() {
        binding.saveButton.setText(R.string.add_event_update_action)
        binding.statusSection.isVisible = false
        (requireActivity() as? AppCompatActivity)?.supportActionBar?.title =
            getString(R.string.add_event_edit_title)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Los selectores son DialogFragment: sobreviven a la recreacion pero pierden sus listeners,
        // asi que descartamos cualquiera huerfano y el usuario lo reabre (la seleccion previa ya se
        // restauro arriba). Mismo criterio que QueryFragment.
        (parentFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? DialogFragment)?.dismiss()
        (parentFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? DialogFragment)?.dismiss()
        // El sistema ya restauro el texto del campo arriba (super), lo que disparo el watcher y limpio
        // el ref. Re-vinculamos despues para que el contacto sobreviva a la rotacion.
        savedInstanceState?.let(::restoreContactLink)
    }

    private fun restoreContactLink(state: Bundle) {
        val ref = state.getString(KEY_CONTACT_REF) ?: return
        pickedContactRef = ref
        binding.contactLinked.isVisible = true
    }

    private fun restoreDateTime(state: Bundle) {
        if (state.containsKey(KEY_DATE)) {
            selectedDate = LocalDate.ofEpochDay(state.getLong(KEY_DATE))
            binding.dateButton.text = dateFormat.format(selectedDate)
        }
        if (state.containsKey(KEY_TIME)) {
            selectedTime = LocalTime.ofSecondOfDay(state.getInt(KEY_TIME).toLong())
            binding.timeButton.text = timeFormat.format(selectedTime)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        selectedDate?.let { outState.putLong(KEY_DATE, it.toEpochDay()) }
        selectedTime?.let { outState.putInt(KEY_TIME, it.toSecondOfDay()) }
        val lat = pickedLat
        val lng = pickedLng
        if (lat != null && lng != null) {
            outState.putDouble(KEY_LAT, lat)
            outState.putDouble(KEY_LNG, lng)
        }
        pickedContactRef?.let { outState.putString(KEY_CONTACT_REF, it) }
    }

    private fun restoreLocation(state: Bundle) {
        if (state.containsKey(KEY_LAT) && state.containsKey(KEY_LNG)) {
            pickedLat = state.getDouble(KEY_LAT)
            pickedLng = state.getDouble(KEY_LNG)
            binding.locationSelected.isVisible = true
        }
    }

    private fun openLocationPicker() {
        val args = bundleOf()
        // Si ya hay una ubicacion elegida, el selector arranca centrado ahi.
        val lat = pickedLat
        val lng = pickedLng
        if (lat != null && lng != null) {
            args.putDouble(LocationPickerFragment.ARG_LAT, lat)
            args.putDouble(LocationPickerFragment.ARG_LNG, lng)
        }
        if (findNavController().currentDestination?.id == R.id.addEventFragment) {
            findNavController().navigate(R.id.locationPickerFragment, args)
        }
    }

    private fun listenForPickedLocation() {
        setFragmentResultListener(LocationPickerFragment.REQUEST_KEY) { _, bundle ->
            pickedLat = bundle.getDouble(LocationPickerFragment.RESULT_LAT)
            pickedLng = bundle.getDouble(LocationPickerFragment.RESULT_LNG)
            binding.locationSelected.isVisible = true
            // La etiqueta geocodificada solo rellena el campo Lugar si el usuario no escribio una.
            val label = bundle.getString(LocationPickerFragment.RESULT_LABEL)
            if (!label.isNullOrBlank() && binding.placeInput.text.isNullOrBlank()) {
                binding.placeInput.setText(label)
            }
        }
    }

    // Pide READ_CONTACTS si falta y abre el selector. Con el permiso ya dado, abre directo.
    private fun ensureContactsPermissionThenPick() {
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) pickContact.launch(null) else contactsPermission.launch(Manifest.permission.READ_CONTACTS)
    }

    // Vuelca el contacto elegido en el campo: pone el nombre (con la guarda para no perder el ref) y
    // guarda su telefono como ref. Si no se pudo leer la fila, avisa y no toca el formulario.
    private fun applyPickedContact(uri: Uri) {
        val picked = ContactResolver.resolve(requireContext().contentResolver, uri)
        if (picked == null) {
            Snackbar.make(binding.root, R.string.add_event_contact_read_failed, Snackbar.LENGTH_LONG).show()
            return
        }
        settingContactFromPicker = true
        binding.contactInput.setText(picked.name)
        settingContactFromPicker = false
        pickedContactRef = picked.ref
        // El indicador de "vinculado" solo aplica si quedo un telefono ligado al nombre.
        binding.contactLinked.isVisible = picked.ref != null
        viewModel.clearFieldErrors()
    }

    // Desvincula el contacto: el nombre se conserva como texto libre, pero ya no apunta a un telefono.
    private fun clearContactLink() {
        pickedContactRef = null
        binding.contactLinked.isVisible = false
    }

    private fun openDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(R.string.add_event_date_picker_title)
            .setSelection(selectedDate?.toUtcMillis() ?: MaterialDatePicker.todayInUtcMilliseconds())
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            binding.dateButton.text = dateFormat.format(selectedDate)
            viewModel.clearFieldErrors()
        }
        picker.show(parentFragmentManager, DATE_PICKER_TAG)
    }

    private fun openTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(selectedTime?.hour ?: DEFAULT_HOUR)
            .setMinute(selectedTime?.minute ?: 0)
            .setTitleText(R.string.add_event_time_picker_title)
            .build()
        picker.addOnPositiveButtonClickListener {
            selectedTime = LocalTime.of(picker.hour, picker.minute)
            binding.timeButton.text = timeFormat.format(selectedTime)
            viewModel.clearFieldErrors()
        }
        picker.show(parentFragmentManager, TIME_PICKER_TAG)
    }

    private fun onSave() {
        val reminder = selectedReminder()
        // Si el evento lleva recordatorio, pide el permiso de notificaciones (no bloquea el guardado).
        if (reminder != Reminder.none) requestNotificationPermissionIfNeeded()
        viewModel.submit(
            AddEventInput(
                description = binding.descriptionInput.text?.toString().orEmpty(),
                contactName = binding.contactInput.text?.toString().orEmpty(),
                placeLabel = binding.placeInput.text?.toString().orEmpty(),
                type = selectedType(),
                status = selectedStatus(),
                reminder = reminder,
                date = selectedDate,
                time = selectedTime,
                lat = pickedLat,
                lng = pickedLng,
                contactRef = pickedContactRef,
            ),
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun selectedType(): EventType = when (binding.typeChipGroup.checkedChipId) {
        R.id.chipAddTypeJunta -> EventType.junta
        R.id.chipAddTypeEntrega -> EventType.entrega_proyecto
        R.id.chipAddTypeExamen -> EventType.examen
        R.id.chipAddTypeOtros -> EventType.otros
        else -> EventType.cita
    }

    private fun selectedStatus(): EventStatus = when (binding.statusChipGroup.checkedChipId) {
        R.id.chipAddStatusRealizado -> EventStatus.realizado
        R.id.chipAddStatusAplazado -> EventStatus.aplazado
        else -> EventStatus.pendiente
    }

    private fun selectedReminder(): Reminder = when (binding.reminderChipGroup.checkedChipId) {
        R.id.chipAddReminderAtTime -> Reminder.at_time
        R.id.chipAddReminderTen -> Reminder.ten_minutes_before
        R.id.chipAddReminderDay -> Reminder.one_day_before
        else -> Reminder.none
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: AddEventUiState) {
        // Prefill de edicion: vuelca el evento cargado en los widgets una sola vez.
        state.prefill?.let { event ->
            prefillForm(event)
            viewModel.prefillHandled()
        }

        binding.descriptionLayout.error = state.descriptionError?.let(::getString)
        binding.contactLayout.error = state.contactError?.let(::getString)
        binding.dateTimeError.isVisible = state.dateTimeError != null
        state.dateTimeError?.let(binding.dateTimeError::setText)
        // La carga del evento a editar tambien muestra progreso y bloquea Guardar (sin formulario
        // valido aun); un fallo de carga lo mantiene bloqueado.
        binding.savingProgress.isVisible = state.isSubmitting || state.isLoadingEvent
        binding.saveButton.isEnabled = !state.isSubmitting && !state.isLoadingEvent && !state.loadFailed

        if (state.saved) {
            // En edicion avisamos al Detalle para que recargue antes de volver.
            if (isEditMode) setFragmentResult(RESULT_EVENT_EDITED, Bundle.EMPTY)
            navigateBack()
            return
        }
        state.errorRes?.let { messageRes ->
            val snackbar = Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG)
            // Si fallo la carga del evento a editar, ofrecemos reintentar (aun no hay formulario).
            if (state.loadFailed) {
                editEventId?.let { id -> snackbar.setAction(R.string.detail_retry) { viewModel.startEditing(id) } }
            }
            snackbar.show()
            viewModel.errorShown()
        }
    }

    // Vuelca el evento en edicion sobre los widgets. La fecha/hora se interpretan en la zona del
    // dispositivo (igual que al guardar). Las coordenadas solo se marcan si son reales (lat/lng != 0):
    // los eventos viejos solo-etiqueta conservan asi su comportamiento sin mapa.
    private fun prefillForm(event: Event) {
        binding.descriptionInput.setText(event.description)
        // El nombre se vuelca con la guarda para no perder el ref que sembramos enseguida; asi al editar
        // solo otros campos el contacto vinculado del evento se reenvia intacto en el patch.
        settingContactFromPicker = true
        binding.contactInput.setText(event.contact.name)
        settingContactFromPicker = false
        pickedContactRef = event.contact.ref
        binding.contactLinked.isVisible = event.contact.ref != null
        binding.placeInput.setText(event.location.label.orEmpty())
        binding.checkType(event.type)
        binding.checkStatus(event.status)
        binding.checkReminder(event.reminder)

        val zoned = event.startsAt.atZone(ZoneId.systemDefault())
        selectedDate = zoned.toLocalDate()
        selectedTime = zoned.toLocalTime()
        binding.dateButton.text = dateFormat.format(selectedDate)
        binding.timeButton.text = timeFormat.format(selectedTime)

        if (event.location.lat != 0.0 || event.location.lng != 0.0) {
            pickedLat = event.location.lat
            pickedLng = event.location.lng
            binding.locationSelected.isVisible = true
        }
        viewModel.clearFieldErrors()
    }

    // Regresa a Inicio una sola vez (guardado contra doble pop si render se reemite).
    private fun navigateBack() {
        val controller = findNavController()
        if (controller.currentDestination?.id == R.id.addEventFragment) {
            controller.navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun LocalDate.toUtcMillis(): Long =
        atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    private companion object {
        const val DATE_PICKER_TAG = "add_event_date_picker"
        const val TIME_PICKER_TAG = "add_event_time_picker"
        const val KEY_DATE = "add_event_selected_date"
        const val KEY_TIME = "add_event_selected_time"
        const val KEY_LAT = "add_event_picked_lat"
        const val KEY_LNG = "add_event_picked_lng"
        const val KEY_CONTACT_REF = "add_event_picked_contact_ref"
        const val DEFAULT_HOUR = 12
        val LOCALE: Locale = Locale("es", "MX")
        val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE)
        val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE)
    }
}
