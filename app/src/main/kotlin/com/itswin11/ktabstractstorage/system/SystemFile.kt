package com.itswin11.ktabstractstorage.system

import com.itswin11.ktabstractstorage.ChildFile
import com.itswin11.ktabstractstorage.File
import com.itswin11.ktabstractstorage.Folder
import com.itswin11.ktabstractstorage.enums.FileAccessMode
import com.itswin11.ktabstractstorage.streams.FileStream
import com.itswin11.ktabstractstorage.streams.UnifiedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path

/**
 * A java.nio implementation of [File].
 *
 * @param path The path represented by this file.
 * @throws IllegalArgumentException if the path does not exist or is not a regular file.
 */
class SystemFile(
    internal val path: Path,
    private val skipValidation: Boolean = false,
) : ChildFile {
    init {
        if (!skipValidation) {
            require(Files.exists(path)) {
                "File path does not exist: $path"
            }
            require(Files.isRegularFile(path)) {
                "Path is not a regular file: $path"
            }
        }
    }

    private val normalizedPath = path.toAbsolutePath().normalize()

    override val id: String = normalizedPath.toString()

    override val name: String = normalizedPath.fileName?.toString() ?: normalizedPath.toString()

    override suspend fun getParentAsync(): Folder?
        = normalizedPath.parent?.let(::SystemFolder)

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream
        = FileStream(normalizedPath.toFile(), accessMode)

    internal companion object {
        fun createUnvalidated(path: Path) = SystemFile(path, skipValidation = true)
    }
}
