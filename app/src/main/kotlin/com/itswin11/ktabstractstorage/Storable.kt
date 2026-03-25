package com.itswin11.ktabstractstorage

/**
 * Represents an item that can be stored or retrieved from a storage source.
 */
interface Storable {
    /**
     * A unique identifier for this item that is consistent across reruns.
     *
     * **Remarks:**
     * - Custom and (especially cloud) file systems often use a flat or near-flat database and
     *   a predictable or custom ID as the primary-key, which can be used as an ID.
     * - Paths that are unique to the local file system can be used as an ID.
     * - Uri-based resource paths that change (e.g. when re-authenticating) should not be used
     *   as an ID.
     * - Names aren't guaranteed to be non-empty or unique within or across folders,
     *   and should not be used as an ID.
     * - The implementation can use any string data available to produce this ID, so long as
     *   it identifies this specific resource across runs.
     */
    val id: String

    /**
     * The name of the item, with the extension (if any).
     */
    val name: String
}

/**
 * Abstract base class for [Storable] implementations that provides a default `toString()` implementation.
 */
abstract class StorableBase : Storable {
    /**
     * Returns a string representation in the format `name (ID)`.
     */
    override fun toString(): String = "$name ($id)"
}

/**
 * Returns a string representation in the format `name (ID)`.
 */
fun Storable.toDisplayString(): String = "$name ($id)"