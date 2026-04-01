package io.github.ktabstractstorage.kiofiles

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.extensions.interfaces.GetRoot
import io.github.ktabstractstorage.streams.FileStream
import io.github.ktabstractstorage.streams.UnifiedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/**
 * A kotlinx-io files implementation of a storage file.
 */
class KioFile(
    internal val path: Path,
    internal val fileSystem: FileSystem = SystemFileSystem,
    private val skipValidation: Boolean = false,
) : GetRoot, ChildFile {

    init {
        if (!skipValidation) {
            val metadata = fileSystem.metadataOrNull(path)
            require(metadata != null) { "File path does not exist: $path" }
            require(metadata.isRegularFile) { "Path is not a regular file: $path" }
        }
    }

    private val normalizedPath: Path = fileSystem.resolve(path)

    override val id: String = normalizedPath.toString()

    override val name: String = normalizedPath.name

    override suspend fun getParentAsync(): Folder? =
        normalizedPath.parent?.let { KioFolder.createUnvalidated(it, fileSystem) }

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream =
        withContext(Dispatchers.IO) { FileStream(java.io.File(normalizedPath.toString()), accessMode) }

    override suspend fun getRootAsync(): Folder? {
        val root = java.io.File(normalizedPath.toString()).toPath().toAbsolutePath().normalize().root
            ?: return null
        return KioFolder.createUnvalidated(kotlinx.io.files.Path(root.toString()), fileSystem)
    }

    internal companion object {
        fun createUnvalidated(path: Path, fs: FileSystem = SystemFileSystem) =
            KioFile(path, fs, skipValidation = true)
    }
}

