package io.github.ktabstractstorage.nio

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.extensions.interfaces.GetRoot
import io.github.ktabstractstorage.streams.FileStream
import io.github.ktabstractstorage.streams.UnifiedStream
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
class NioFile(
    internal val path: Path,
    private val skipValidation: Boolean = false,
) : GetRoot, ChildFile {
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

    override suspend fun getParentAsync(): Folder? = normalizedPath.parent?.let(::NioFolder)

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream =
        withContext(Dispatchers.IO) { FileStream(path.toFile(), accessMode) }

    override suspend fun getRootAsync(): Folder? =
        normalizedPath.root?.let(::NioFolder)


    internal companion object {
        fun createUnvalidated(path: Path) = NioFile(path, skipValidation = true)
    }
}
