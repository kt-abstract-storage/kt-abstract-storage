package com.itswin11.ktabstractstorage

import com.itswin11.ktabstractstorage.enums.FileAccessMode
import com.itswin11.ktabstractstorage.streams.UnifiedStream
import com.itswin11.ktabstractstorage.streams.truncated

/**
 * A [File] wrapper that limits the length of any stream opened from the wrapped file.
 *
 * @param file The wrapped file.
 * @param maxLength The maximum number of bytes exposed by opened streams.
 * @param parentOverride Optional parent to report instead of the wrapped file's parent.
 */
class TruncatedFile(
    val file: File,
    var maxLength: Long,
) : File {
    init {
        require(maxLength >= 0) { "maxLength must be non-negative." }
    }

    override val id: String
        get() = file.id

    override val name: String
        get() = file.name

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream =
        file.openStreamAsync(accessMode).truncated(maxLength)
}

