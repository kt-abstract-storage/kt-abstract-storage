package com.itswin11.ktabstractstorage.system.io

import com.itswin11.ktabstractstorage.streams.UnifiedStream
import com.itswin11.ktabstractstorage.streams.asUnifiedStream
import java.io.InputStream
import java.io.OutputStream

/**
 * Wraps this [UnifiedStream] in a [StreamFile].
 */
fun UnifiedStream.asStreamFile(
    id: String = hashCode().toString(),
    name: String = hashCode().toString(),
    shouldDispose: Boolean = false,
): StreamFile = StreamFile(this, id, name).also { it.shouldDispose = shouldDispose }

/**
 * Wraps this [InputStream] in a [StreamFile].
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
 */
fun OutputStream.asStreamFile(
    id: String = hashCode().toString(),
    name: String = hashCode().toString(),
    shouldDispose: Boolean = false,
    closeOutputStreamOnClose: Boolean = true,
): StreamFile =
    asUnifiedStream(closeOutputStreamOnClose)
        .asStreamFile(id = id, name = name, shouldDispose = shouldDispose)
