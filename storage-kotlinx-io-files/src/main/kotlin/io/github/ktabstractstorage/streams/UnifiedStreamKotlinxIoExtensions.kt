package io.github.ktabstractstorage.streams

import io.github.ktabstractstorage.streams.extensions.SinkUnifiedStreamAdapter
import io.github.ktabstractstorage.streams.extensions.SourceSinkUnifiedStream
import io.github.ktabstractstorage.streams.extensions.SourceUnifiedStreamAdapter
import io.github.ktabstractstorage.streams.extensions.UnifiedStreamRawSinkAdapter
import io.github.ktabstractstorage.streams.extensions.UnifiedStreamRawSourceAdapter
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.buffered

/**
 * Exposes this [UnifiedStream] as a buffered kotlinx-io [Source].
 *
 * @param closeUnifiedStreamOnClose When `true`, closing the returned [Source]
 * also closes this [UnifiedStream].
 */
fun UnifiedStream.asSource(closeUnifiedStreamOnClose: Boolean = true): Source =
    UnifiedStreamRawSourceAdapter(this, closeUnifiedStreamOnClose).buffered()

/**
 * Exposes this [UnifiedStream] as a buffered kotlinx-io [Sink].
 *
 * @param closeUnifiedStreamOnClose When `true`, closing the returned [Sink]
 * also closes this [UnifiedStream].
 */
fun UnifiedStream.asSink(closeUnifiedStreamOnClose: Boolean = true): Sink =
    UnifiedStreamRawSinkAdapter(this, closeUnifiedStreamOnClose).buffered()

/**
 * Exposes this kotlinx-io [Source] as a [UnifiedStream].
 *
 * The resulting stream is read-only and non-seekable.
 *
 * @param closeSourceOnClose When `true`, closing the returned [UnifiedStream]
 * also closes this [Source].
 */
fun Source.asUnifiedStream(closeSourceOnClose: Boolean = true): UnifiedStream =
    SourceUnifiedStreamAdapter(this, closeSourceOnClose)

/**
 * Exposes this kotlinx-io [Sink] as a [UnifiedStream].
 *
 * The resulting stream is write-only and non-seekable.
 *
 * @param closeSinkOnClose When `true`, closing the returned [UnifiedStream]
 * also closes this [Sink].
 */
fun Sink.asUnifiedStream(closeSinkOnClose: Boolean = true): UnifiedStream =
    SinkUnifiedStreamAdapter(this, closeSinkOnClose)

/**
 * Combines a [source] and [sink] into one non-seekable [UnifiedStream].
 */
fun UnifiedStream.Companion.combineIoStreams(
    source: Source,
    sink: Sink,
    closeSourceOnClose: Boolean = true,
    closeSinkOnClose: Boolean = true,
): UnifiedStream = SourceSinkUnifiedStream(
    source = source,
    sink = sink,
    closeSourceOnClose = closeSourceOnClose,
    closeSinkOnClose = closeSinkOnClose,
)
