package com.songsit.fuellogpro.data.local

/**
 * Item C (multi-photo support): rather than stacking a second Room migration on top of the
 * migration8To9 `photoUri` TEXT column just added to FuelEntryEntity/ExpenseEntity, multiple
 * attachments (up to a few) are stored in that same single TEXT column as a comma-joined list
 * of local file paths. Each path is a UUID-named file under filesDir/photos (see
 * MainActivity.pickPhoto), so it never contains a comma itself. This helper is the only place
 * that knows about the joined encoding.
 */
object PhotoUris {
    private const val SEPARATOR = ","

    fun split(value: String?): List<String> =
        value?.split(SEPARATOR)?.map(String::trim)?.filter(String::isNotBlank) ?: emptyList()

    fun join(values: List<String>): String? =
        values.filter(String::isNotBlank).takeIf { it.isNotEmpty() }?.joinToString(SEPARATOR)
}

// Attachments are mostly photos, but expenses can also attach a PDF (e.g. an emailed e-receipt) —
// callers use this to tell the two apart since a PDF can't be decoded/previewed as an image.
fun isPdfPath(path: String): Boolean = path.substringAfterLast('.', "").equals("pdf", ignoreCase = true)
