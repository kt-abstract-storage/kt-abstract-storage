package io.github.ktabstractstorage.extensions

import io.github.ktabstractstorage.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Opens this file for reading and reads all bytes.
 *
 * @param bufferSize Size of the temporary read buffer.
 */
suspend fun File.readBytesAsync(bufferSize: Int = DEFAULT_BUFFER_SIZE): ByteArray {
    require(bufferSize > 0) { "bufferSize must be greater than zero." }

    openReadAsync().use { source ->
        val chunks = mutableListOf<ByteArray>()
        var totalBytes = 0
        val buffer = ByteArray(bufferSize)

        while (true) {
            val bytesRead = source.readAsync(buffer, 0, buffer.size)
            if (bytesRead <= 0) {
                break
            }

            chunks += buffer.copyOf(bytesRead)
            totalBytes += bytesRead
        }

        val output = ByteArray(totalBytes)
        var writeOffset = 0
        for (chunk in chunks) {
            chunk.copyInto(output, writeOffset)
            writeOffset += chunk.size
        }

        return output
    }
}

/**
 * Opens this file for reading and reads all text as UTF-8.
 *
 * @param bufferSize Size of the temporary read buffer.
 */
suspend fun File.readTextAsync(
    bufferSize: Int = DEFAULT_BUFFER_SIZE,
): String = readBytesAsync(bufferSize).decodeToString()

/**
 * Reads text lines in [lineRange] (start inclusive, end exclusive).
 *
 * @param lineRange Pair of line indexes as start-inclusive and end-exclusive.
 */
fun File.readTextAsync(lineRange: Pair<Int, Int>): Flow<String> = flow {
    val (start, endExclusive) = lineRange
    require(start >= 0 && endExclusive >= 0) { "Line range values must be non-negative." }
    require(endExclusive >= start) { "Line range end must be greater than or equal to start." }

    val lines = readTextAsync().lineSequence().toList()
    val end = minOf(endExclusive, lines.size)
    for (index in start until end) {
        emit(lines[index])
    }
}

/**
 * Reads text lines in [lineRange] with [columnRange] (both end-exclusive).
 *
 * @param lineRange Pair of line indexes as start-inclusive and end-exclusive.
 * @param columnRange Pair of column indexes as start-inclusive and end-exclusive.
 */
fun File.readTextAsync(
    lineRange: Pair<Int, Int>,
    columnRange: Pair<Int, Int>,
): Flow<String> = flow {
    val (columnStart, columnEndExclusive) = columnRange
    require(columnStart >= 0 && columnEndExclusive >= 0) { "Column range values must be non-negative." }
    require(columnEndExclusive >= columnStart) { "Column range end must be greater than or equal to start." }

    readTextAsync(lineRange).collect { line ->
        if (columnStart >= line.length) {
            emit("")
        } else {
            val length = minOf(columnEndExclusive - columnStart, line.length - columnStart)
            emit(line.substring(columnStart, columnStart + length))
        }
    }
}


