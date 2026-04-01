package io.github.ktabstractstorage.system.io

import io.github.ktabstractstorage.streams.UnifiedStream
import io.github.ktabstractstorage.streams.asUnifiedStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Wraps this [InputStream] in a [StreamFile].
 *
 * @param id Stable identifier for the resulting file.
 * @param name Display name for the resulting file.
 * @param shouldDispose Whether closing returned streams should close the wrapped stream.
 * @param closeInputStreamOnClose Whether the adapter closes this input stream on close.
 */
fun InputStream.asStreamFile(
    id: String = hashCode().toString(),
    name: String = hashCode().toString(),
    shouldDispose: Boolean = false,
    closeInputStreamOnClose: Boolean = true,
): StreamFile =
    asUnifiedStream(closeInputStreamOnClose)
        .asStreamFile(id = id, name = name, shouldDispose = shouldDispose)

/**
 * Wraps this [OutputStream] in a [StreamFile].
 *
 * @param id Stable identifier for the resulting file.
 * @param name Display name for the resulting file.
 * @param shouldDispose Whether closing returned streams should close the wrapped stream.
 * @param closeOutputStreamOnClose Whether the adapter closes this output stream on close.
 */
fun OutputStream.asStreamFile(
    id: String = hashCode().toString(),
    name: String = hashCode().toString(),
    shouldDispose: Boolean = false,
    closeOutputStreamOnClose: Boolean = true,
): StreamFile =
    asUnifiedStream(closeOutputStreamOnClose)
        .asStreamFile(id = id, name = name, shouldDispose = shouldDispose)
