package com.eddndev.purpura.ui.common

import com.eddndev.purpura.R
import com.eddndev.purpura.domain.error.DomainError
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// toErrorMessageRes es el unico punto donde se decide que es "error de UI": debe PROPAGAR la
// cancelacion estructurada (una corrutina superada no pinta aviso) y mapear el resto a @StringRes.
class ErrorTextTest {

    @Test
    fun `propaga CancellationException en vez de mapearla`() {
        val thrown = runCatching { CancellationException("superada").toErrorMessageRes() }.exceptionOrNull()
        assertTrue("debio relanzar la cancelacion", thrown is CancellationException)
    }

    @Test
    fun `mapea un DomainError a su recurso`() {
        assertEquals(R.string.error_network, DomainError.Network.toErrorMessageRes())
    }

    @Test
    fun `mapea un error desconocido a error_unexpected`() {
        assertEquals(R.string.error_unexpected, RuntimeException("x").toErrorMessageRes())
    }
}
