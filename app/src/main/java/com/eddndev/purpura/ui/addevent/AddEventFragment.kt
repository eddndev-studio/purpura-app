package com.eddndev.purpura.ui.addevent

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.eddndev.purpura.R
import com.eddndev.purpura.databinding.FragmentAddEventBinding
import com.eddndev.purpura.domain.model.EventStatus
import com.eddndev.purpura.domain.model.EventType
import com.eddndev.purpura.domain.model.Reminder
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
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

    // Permiso de notificaciones (API 33+). Se solicita al guardar con un recordatorio activo; si el
    // usuario lo niega, la alarma se programa igual pero no producira aviso visible (decision suya).
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* resultado ignorado */ }

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

        binding.dateButton.setOnClickListener { openDatePicker() }
        binding.timeButton.setOnClickListener { openTimePicker() }
        binding.saveButton.setOnClickListener { onSave() }

        // Al editar un campo se limpia su error para no dejarlo "pegado" mientras el usuario corrige.
        binding.descriptionInput.doAfterTextChanged { viewModel.clearFieldErrors() }
        binding.contactInput.doAfterTextChanged { viewModel.clearFieldErrors() }

        observeState()
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // Los selectores son DialogFragment: sobreviven a la recreacion pero pierden sus listeners,
        // asi que descartamos cualquiera huerfano y el usuario lo reabre (la seleccion previa ya se
        // restauro arriba). Mismo criterio que QueryFragment.
        (parentFragmentManager.findFragmentByTag(DATE_PICKER_TAG) as? DialogFragment)?.dismiss()
        (parentFragmentManager.findFragmentByTag(TIME_PICKER_TAG) as? DialogFragment)?.dismiss()
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
        binding.descriptionLayout.error = state.descriptionError?.let(::getString)
        binding.contactLayout.error = state.contactError?.let(::getString)
        binding.dateTimeError.isVisible = state.dateTimeError != null
        state.dateTimeError?.let(binding.dateTimeError::setText)
        binding.savingProgress.isVisible = state.isSubmitting
        binding.saveButton.isEnabled = !state.isSubmitting

        if (state.saved) {
            navigateBack()
            return
        }
        state.errorRes?.let { messageRes ->
            Snackbar.make(binding.root, messageRes, Snackbar.LENGTH_LONG).show()
            viewModel.errorShown()
        }
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
        const val DEFAULT_HOUR = 12
        val LOCALE: Locale = Locale("es", "MX")
        val dateFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", LOCALE)
        val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", LOCALE)
    }
}
