package io.github.ktabstractstorage

/**
 * A minimum implementation of [Storable].
 *
 * **Remarks:**
 * - Useful to identify a resource which might not be accessible, such as when it is removed.
 *
 * @param id A unique and consistent identifier for this file or folder.
 * This dedicated resource identifier is used to identify the exact file being referenced.
 * @param name The name of the file or folder, with the extension (if any).
 */
data class SimpleStorableItem(
    override val id: String,
    override val name: String
) : Storable

