package com.eddndev.purpura.ui.addevent

import android.content.ContentResolver
import android.net.Uri
import android.provider.ContactsContract

// Contacto elegido en el selector del sistema (REQ-ADD-002): nombre visible + telefono. El telefono
// es opcional (ref null) cuando el contacto no tiene ninguno guardado.
data class PickedContact(val name: String, val ref: String?)

// Resuelve el URI que devuelve ActivityResultContracts.PickContact() a nombre + primer telefono.
// Se invoca solo con READ_CONTACTS concedido (el Fragment lo pide antes de abrir el selector): la
// lectura de la tabla Phone lo exige. Devuelve null si no se pudo leer la fila del contacto.
object ContactResolver {

    fun resolve(resolver: ContentResolver, contactUri: Uri): PickedContact? {
        val row = readContactRow(resolver, contactUri) ?: return null
        val phone = if (row.hasPhone) readPrimaryPhone(resolver, row.id) else null
        return PickedContact(name = row.name, ref = phone)
    }

    private data class ContactRow(val id: String, val name: String, val hasPhone: Boolean)

    private fun readContactRow(resolver: ContentResolver, uri: Uri): ContactRow? {
        val projection = arrayOf(
            ContactsContract.Contacts._ID,
            ContactsContract.Contacts.DISPLAY_NAME,
            ContactsContract.Contacts.HAS_PHONE_NUMBER,
        )
        resolver.query(uri, projection, null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)).orEmpty()
            val hasPhone = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0
            return ContactRow(id = id, name = name, hasPhone = hasPhone)
        }
        return null
    }

    private fun readPrimaryPhone(resolver: ContentResolver, contactId: String): String? {
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
            arrayOf(contactId),
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
            }
        }
        return null
    }
}
