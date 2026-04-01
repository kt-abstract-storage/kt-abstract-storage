package io.github.ktabstractstorage.kiofiles

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ChildFolder
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.ModifiableFolder
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.extensions.interfaces.CreateCopyOfDelegate
import io.github.ktabstractstorage.extensions.interfaces.CreateRenamedCopyOf
import io.github.ktabstractstorage.extensions.interfaces.CreateRenamedCopyOfDelegate
import io.github.ktabstractstorage.extensions.interfaces.GetFirstByName
import io.github.ktabstractstorage.extensions.interfaces.GetItem
import io.github.ktabstractstorage.extensions.interfaces.GetItemRecursive
import io.github.ktabstractstorage.extensions.interfaces.GetRoot
import io.github.ktabstractstorage.extensions.interfaces.MoveFromDelegate
import io.github.ktabstractstorage.extensions.interfaces.MoveRenamedFrom
import io.github.ktabstractstorage.extensions.interfaces.MoveRenamedFromDelegate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.io.files.FileMetadata
import kotlinx.io.files.FileSystem
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import java.io.FileNotFoundException
import java.nio.file.FileAlreadyExistsException

/**
 * A kotlinx-io files implementation of a modifiable folder.
 */
class KioFolder(
    internal val path: Path,
    internal val fileSystem: FileSystem = SystemFileSystem,
    private val skipValidation: Boolean = false,
) : CreateRenamedCopyOf,
    MoveRenamedFrom,
    GetRoot,
    GetItem,
    GetItemRecursive,
    GetFirstByName,
    ModifiableFolder,
    ChildFolder {

    init {
        if (!skipValidation) {
            val metadata = fileSystem.metadataOrNull(path)
            require(metadata != null) { "Folder path does not exist: $path" }
            require(metadata.isDirectory) { "Path is not a directory: $path" }
        }
    }

    private val normalizedPath: Path = fileSystem.resolve(path)

    override val id: String = normalizedPath.toString()

    override val name: String = normalizedPath.name.ifEmpty { normalizedPath.toString() }

    override suspend fun getParentAsync(): Folder? =
        normalizedPath.parent?.let { createUnvalidated(it, fileSystem) }

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> = flow {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }

        val items = withContext(Dispatchers.IO) {
            fileSystem.list(normalizedPath).mapNotNull { childPath ->
                val child = childForPath(childPath) ?: return@mapNotNull null
                val matchesType = when (type) {
                    StorableType.ALL -> true
                    StorableType.FILE -> child is ChildFile
                    StorableType.FOLDER -> child is ChildFolder
                    StorableType.NONE -> error("StorableType.NONE must be rejected before enumeration.")
                }
                child.takeIf { matchesType }
            }
        }

        for (item in items) emit(item)
    }

    override suspend fun getItemAsync(id: String): StorableChild = withContext(Dispatchers.IO) {
        val target = pathFromIdOrNull(id)
            ?.takeIf { it.parent == normalizedPath }
            ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")

        childForPath(target)
            ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")
    }

    override suspend fun getItemRecursiveAsync(id: String): StorableChild = withContext(Dispatchers.IO) {
        val target = pathFromIdOrNull(id)
            ?.takeIf { it.toString().startsWith(normalizedPath.toString()) }
            ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")

        childForPath(target)
            ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")
    }

    override suspend fun getFirstByNameAsync(name: String): StorableChild = withContext(Dispatchers.IO) {
        val target = fileSystem.resolve(kotlinx.io.files.Path(normalizedPath, name))
        if (target.parent != normalizedPath) {
            throw FileNotFoundException("No storage item with the name '$name' could be found.")
        }

        childForPath(target)
            ?: throw FileNotFoundException("No storage item with the name '$name' could be found.")
    }

    override suspend fun getFolderWatcherAsync(): KioFolderWatcher = withContext(Dispatchers.IO) {
        KioFolderWatcher(this@KioFolder)
    }

    override suspend fun deleteAsync(item: StorableChild) = withContext(Dispatchers.IO) {
        val target = resolveChildPath(item)
        if (fileSystem.metadataOrNull(target) == null) {
            throw FileNotFoundException("Item ${item.name} was not found in folder $name")
        }
        deleteRecursively(target)
    }

    override suspend fun createFolderAsync(name: String, overwrite: Boolean): ChildFolder = withContext(Dispatchers.IO) {
        val target = kotlinx.io.files.Path(normalizedPath, name)
        val metadata = fileSystem.metadataOrNull(target)

        when {
            metadata == null -> fileSystem.createDirectories(target, false)
            metadata.isDirectory -> return@withContext createUnvalidated(target, fileSystem)
            !overwrite -> throw FileAlreadyExistsException(target.toString())
            else -> {
                deleteRecursively(target)
                fileSystem.createDirectories(target, false)
            }
        }

        createUnvalidated(target, fileSystem)
    }

    override suspend fun createFileAsync(name: String, overwrite: Boolean): ChildFile = withContext(Dispatchers.IO) {
        val target = kotlinx.io.files.Path(normalizedPath, name)
        val metadata = fileSystem.metadataOrNull(target)

        when {
            metadata == null -> fileSystem.sink(target, false).close()
            metadata.isRegularFile -> {
                if (overwrite) fileSystem.sink(target, false).close()
                return@withContext KioFile.createUnvalidated(target, fileSystem)
            }
            !overwrite -> throw FileAlreadyExistsException(target.toString())
            else -> {
                deleteRecursively(target)
                fileSystem.sink(target, false).close()
            }
        }

        KioFile.createUnvalidated(target, fileSystem)
    }

    override suspend fun moveFromAsync(
        fileToMove: ChildFile,
        source: ModifiableFolder,
        overwrite: Boolean,
        fallback: MoveFromDelegate,
    ): ChildFile = withContext(Dispatchers.IO) {
        if (fileToMove is KioFile) {
            return@withContext moveSystemFile(fileToMove.path, overwrite, fileToMove.name)
        }
        fallback(this@KioFolder, fileToMove, source, overwrite)
    }

    override suspend fun moveFromAsync(
        fileToMove: ChildFile,
        source: ModifiableFolder,
        overwrite: Boolean,
        newName: String,
        fallback: MoveRenamedFromDelegate,
    ): ChildFile = withContext(Dispatchers.IO) {
        if (fileToMove is KioFile) {
            return@withContext moveSystemFile(fileToMove.path, overwrite, newName)
        }
        fallback(this@KioFolder, fileToMove, source, overwrite, newName)
    }

    override suspend fun createCopyOfAsync(
        fileToCopy: File,
        overwrite: Boolean,
        fallback: CreateCopyOfDelegate,
    ): ChildFile = withContext(Dispatchers.IO) {
        if (fileToCopy is KioFile) {
            return@withContext copySystemFile(fileToCopy.path, overwrite, fileToCopy.name)
        }
        fallback(this@KioFolder, fileToCopy, overwrite)
    }

    override suspend fun createCopyOfAsync(
        fileToCopy: File,
        overwrite: Boolean,
        newName: String,
        fallback: CreateRenamedCopyOfDelegate,
    ): ChildFile = withContext(Dispatchers.IO) {
        if (fileToCopy is KioFile) {
            return@withContext copySystemFile(fileToCopy.path, overwrite, newName)
        }
        fallback(this@KioFolder, fileToCopy, overwrite, newName)
    }

    override suspend fun getRootAsync(): Folder? {
        val root = java.io.File(normalizedPath.toString()).toPath().toAbsolutePath().normalize().root
            ?: return null
        return createUnvalidated(kotlinx.io.files.Path(root.toString()), fileSystem)
    }

    private fun childForPath(path: Path): StorableChild? {
        val metadata = fileSystem.metadataOrNull(path) ?: return null
        return when {
            metadata.isDirectory -> createUnvalidated(path, fileSystem)
            metadata.isRegularFile -> KioFile.createUnvalidated(path, fileSystem)
            else -> null
        }
    }

    private fun pathFromIdOrNull(id: String): Path? = runCatching {
        fileSystem.resolve(kotlinx.io.files.Path(id))
    }.getOrNull()

    private fun moveSystemFile(sourcePath: Path, overwrite: Boolean, newName: String): ChildFile {
        val destinationPath = kotlinx.io.files.Path(normalizedPath, newName)
        val destinationMetadata = fileSystem.metadataOrNull(destinationPath)

        when {
            destinationMetadata?.isRegularFile == true && !overwrite -> return KioFile.createUnvalidated(destinationPath, fileSystem)
            destinationMetadata != null && !overwrite -> throw FileAlreadyExistsException(destinationPath.toString())
            destinationMetadata != null -> deleteRecursively(destinationPath)
        }

        fileSystem.atomicMove(sourcePath, destinationPath)
        return KioFile.createUnvalidated(destinationPath, fileSystem)
    }

    private fun copySystemFile(sourcePath: Path, overwrite: Boolean, newName: String): ChildFile {
        val destinationPath = kotlinx.io.files.Path(normalizedPath, newName)
        val destinationMetadata = fileSystem.metadataOrNull(destinationPath)

        when {
            destinationMetadata?.isRegularFile == true && !overwrite -> return KioFile.createUnvalidated(destinationPath, fileSystem)
            destinationMetadata != null && !overwrite -> throw FileAlreadyExistsException(destinationPath.toString())
            destinationMetadata != null -> deleteRecursively(destinationPath)
        }

        java.io.File(sourcePath.toString()).copyTo(java.io.File(destinationPath.toString()), overwrite = true)
        return KioFile.createUnvalidated(destinationPath, fileSystem)
    }

    private fun resolveChildPath(item: StorableChild): Path {
        val candidate = when (item) {
            is KioFile -> fileSystem.resolve(item.path)
            is KioFolder -> fileSystem.resolve(item.path)
            else -> fileSystem.resolve(kotlinx.io.files.Path(normalizedPath, item.name))
        }

        if (candidate.parent != normalizedPath) {
            throw FileNotFoundException("Item ${item.name} does not belong to folder $name")
        }

        return candidate
    }

    private fun deleteRecursively(path: Path) {
        val metadata: FileMetadata = fileSystem.metadataOrNull(path) ?: return
        if (metadata.isDirectory) {
            fileSystem.list(path).forEach { child -> deleteRecursively(child) }
        }
        fileSystem.delete(path, false)
    }

    internal companion object {
        fun createUnvalidated(path: Path, fs: FileSystem = SystemFileSystem) =
            KioFolder(path, fs, skipValidation = true)
    }
}

