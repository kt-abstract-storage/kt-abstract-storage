package io.github.ktabstractstorage.system.io

import io.github.ktabstractstorage.streams.UnifiedStream

/**
 * Wraps this [UnifiedStream] in a [StreamFile].
 *
 * @param id Stable identifier for the resulting file.
 * @param name Display name for the resulting file.
 * @param shouldDispose Whether closing returned streams should close the wrapped stream.
 */
fun UnifiedStream.asStreamFile(
    id: String = hashCode().toString(),
    name: String = hashCode().toString(),
    shouldDispose: Boolean = false,
): StreamFile = StreamFile(this, id, name).also { it.shouldDispose = shouldDispose }

