package com.itswin11.ktabstractstorage.extensions

import com.itswin11.ktabstractstorage.File
import java.nio.charset.Charset

/**
 * Opens this file for writing and writes [content].
 *
 * @param content Raw bytes to write.
 */
suspend fun File.writeBytesAsync(content: ByteArray) {
    openWriteAsync().use { stream ->
        stream.writeAsync(content, 0, content.size)
        stream.flushAsync()
    }
}

/**
 * Opens this file for writing and writes [content] as text with [charset].
 *
 * @param content Text content to write.
 * @param charset Charset used to encode [content].
 */
suspend fun File.writeTextAsync(content: String, charset: Charset = Charsets.UTF_8) {
    val encoded = charset.encode(content)
    val bytes = ByteArray(encoded.remaining())
    encoded.get(bytes)
    writeBytesAsync(bytes)
}

/**
 * Writes the requested line range from [content] to this file as UTF-8 text.
 * The line range is start-inclusive and end-inclusive.
 *
 * @param content Text content used as the source.
 * @param lineRange Pair of line indexes as start-inclusive and end-inclusive.
 */
suspend fun File.writeTextAsync(content: String, lineRange: Pair<Int, Int>) {
    val lines = content.lineSequence().toList()
    val (start, endInclusive) = lineRange

    require(start >= 0 && endInclusive >= 0) { "Line range values must be non-negative." }
    require(endInclusive >= start) { "Line range end must be greater than or equal to start." }

    val selected = lines
        .drop(start)
        .take((endInclusive - start + 1).coerceAtLeast(0))
        .joinToString(separator = "\n")

    writeTextAsync(selected)
}

/**
 * Writes the requested line and column ranges from [content] to this file as UTF-8 text.
 * [lineRange] is start-inclusive and end-inclusive, [columnRange] is start-inclusive and end-exclusive.
 *
 * @param content Text content used as the source.
 * @param lineRange Pair of line indexes as start-inclusive and end-inclusive.
 * @param columnRange Pair of column indexes as start-inclusive and end-exclusive.
 */
suspend fun File.writeTextAsync(
    content: String,
    lineRange: Pair<Int, Int>,
    columnRange: Pair<Int, Int>,
) {
    val (colStart, colEndExclusive) = columnRange
    require(colStart >= 0 && colEndExclusive >= 0) { "Column range values must be non-negative." }
    require(colEndExclusive >= colStart) { "Column range end must be greater than or equal to start." }

    val lines = content.lineSequence().toList()
    val (lineStart, lineEndInclusive) = lineRange

    require(lineStart >= 0 && lineEndInclusive >= 0) { "Line range values must be non-negative." }
    require(lineEndInclusive >= lineStart) { "Line range end must be greater than or equal to start." }

    val selected = lines
        .drop(lineStart)
        .take((lineEndInclusive - lineStart + 1).coerceAtLeast(0))
        .joinToString(separator = "\n") { line ->
            if (colStart >= line.length) {
                ""
            } else {
                val length = minOf(colEndExclusive - colStart, line.length - colStart)
                line.substring(colStart, colStart + length)
            }
        }

    writeTextAsync(selected)
}

