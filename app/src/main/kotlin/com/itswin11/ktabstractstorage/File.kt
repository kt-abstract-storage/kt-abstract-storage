package com.itswin11.ktabstractstorage

import com.itswin11.ktabstractstorage.enums.FileAccessMode
import com.itswin11.ktabstractstorage.streams.UnifiedStream

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