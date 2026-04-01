package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.streams.UnifiedStream

/**
 * The simplest possible representation of a file.
 */
interface File : Storable {
    /**
     * Asynchronously opens a stream for this file with the requested [accessMode].
     *
     * @param accessMode Determines whether the returned stream supports reading,
     * writing, or both.
     */
    suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream
}