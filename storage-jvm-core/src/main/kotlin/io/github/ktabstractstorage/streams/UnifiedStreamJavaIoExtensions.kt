package io.github.ktabstractstorage.streams

import io.github.ktabstractstorage.streams.extensions.InputOutputStreamUnifiedAdapter
import io.github.ktabstractstorage.streams.extensions.InputStreamUnifiedAdapter
import io.github.ktabstractstorage.streams.extensions.OutputStreamUnifiedAdapter
import io.github.ktabstractstorage.streams.extensions.UnifiedInputStreamAdapter
import io.github.ktabstractstorage.streams.extensions.UnifiedOutputStreamAdapter
import java.io.InputStream
import java.io.OutputStream

/**
 * Exposes this [UnifiedStream] as a blocking [InputStream].
 *
 * @param closeUnifiedStreamOnClose When `true`, closing the returned [InputStream]
 * also closes this [UnifiedStream].
 */
fun UnifiedStream.asInputStream(closeUnifiedStreamOnClose: Boolean = true): InputStream =
    UnifiedInputStreamAdapter(this, closeUnifiedStreamOnClose)

/**
 * Exposes this [UnifiedStream] as a blocking [OutputStream].
 *
 * @param closeUnifiedStreamOnClose When `true`, closing the returned [OutputStream]
 * also closes this [UnifiedStream].
 */
fun UnifiedStream.asOutputStream(closeUnifiedStreamOnClose: Boolean = true): OutputStream =
    UnifiedOutputStreamAdapter(this, closeUnifiedStreamOnClose)

/**
 * Exposes this [InputStream] as a [UnifiedStream].
 *
 * The resulting stream is read-only and non-seekable.
 *
 * @param closeInputStreamOnClose When `true`, closing the returned [UnifiedStream]
 * also closes this [InputStream].
 */
fun InputStream.asUnifiedStream(closeInputStreamOnClose: Boolean = true): UnifiedStream =
    InputStreamUnifiedAdapter(this, closeInputStreamOnClose)

/**
 * Exposes this [OutputStream] as a [UnifiedStream].
 *
 * The resulting stream is write-only and non-seekable.
 *
 * @param closeOutputStreamOnClose When `true`, closing the returned [UnifiedStream]
 * also closes this [OutputStream].
 */
fun OutputStream.asUnifiedStream(closeOutputStreamOnClose: Boolean = true): UnifiedStream =
    OutputStreamUnifiedAdapter(this, closeOutputStreamOnClose)

/**
 * Exposes this [InputStream] together with [output] as a single [UnifiedStream].
 *
 * The resulting stream is non-seekable and supports both read and write.
 */
fun InputStream.asUnifiedStream(
    output: OutputStream,
    closeInputStreamOnClose: Boolean = true,
    closeOutputStreamOnClose: Boolean = true,
): UnifiedStream = InputOutputStreamUnifiedAdapter(
    input = this,
    output = output,
    closeInputOnClose = closeInputStreamOnClose,
    closeOutputOnClose = closeOutputStreamOnClose,
)

/**
 * Exposes this [OutputStream] together with [input] as a single [UnifiedStream].
 *
 * The resulting stream is non-seekable and supports both read and write.
 */
fun OutputStream.asUnifiedStream(
    input: InputStream,
    closeInputStreamOnClose: Boolean = true,
    closeOutputStreamOnClose: Boolean = true,
): UnifiedStream = InputOutputStreamUnifiedAdapter(
    input = input,
    output = this,
    closeInputOnClose = closeInputStreamOnClose,
    closeOutputOnClose = closeOutputStreamOnClose,
)
