package com.eddndev.purpura.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import com.eddndev.purpura.databinding.FragmentPlaceholderBinding

// Base para las pantallas que en el esqueleto del 10 de junio solo muestran su
// identidad (titulo + descripcion). En #8 cada subclase se reemplaza por su UI real.
abstract class PlaceholderFragment(
    @StringRes private val titleRes: Int,
    @StringRes private val bodyRes: Int,
) : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        val binding = FragmentPlaceholderBinding.inflate(inflater, container, false)
        binding.placeholderTitle.setText(titleRes)
        binding.placeholderBody.setText(bodyRes)
        return binding.root
    }
}
