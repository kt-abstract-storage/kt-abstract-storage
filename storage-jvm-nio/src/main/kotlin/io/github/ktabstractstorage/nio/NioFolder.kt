package io.github.ktabstractstorage.nio

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
import java.io.FileNotFoundException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.util.Comparator

/**
 * A java.nio implementation of a modifiable folder.
 *
 * @param path The path represented by this folder.
 * @throws IllegalArgumentException if the path does not exist or is not a directory.
 */
class NioFolder(
    internal val path: Path,
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
            require(Files.exists(path)) {
                "Folder path does not exist: $path"
            }
            require(Files.isDirectory(path)) {
                "Path is not a directory: $path"
            }
        }
    }

    private val normalizedPath = path.toAbsolutePath().normalize()

    override val id: String = normalizedPath.toString()

    override val name: String = normalizedPath.fileName?.toString() ?: normalizedPath.toString()

    override suspend fun getParentAsync(): Folder? = normalizedPath.parent?.let(::NioFolder)

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> = flow {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }

        val items: List<StorableChild> = withContext(Dispatchers.IO) {
            Files.list(normalizedPath).use { children ->
                val collected = mutableListOf<StorableChild>()
                children.forEach { childPath ->
                    val child = childForPath(childPath) ?: return@forEach
                    val matchesType = when (type) {
                        StorableType.ALL -> true
                        StorableType.FILE -> child is ChildFile
                        StorableType.FOLDER -> child is ChildFolder
                        StorableType.NONE -> error("StorableType.NONE must be rejected before enumeration.")
                    }

                    if (matchesType) {
                        collected += child
                    }
                }

                collected
            }
        }

        for (item in items) {
            emit(item)
        }
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
            ?.takeIf { it.startsWith(normalizedPath) }
            ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")

        childForPath(target)
            ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")
    }

    override suspend fun getFirstByNameAsync(name: String): StorableChild = withContext(Dispatchers.IO) {
        val target = normalizedPath.resolve(name).normalize()
        if (target.parent != normalizedPath) {
            throw FileNotFoundException("No storage item with the name '$name' could be found.")
        }

        childForPath(target)
            ?: throw FileNotFoundException("No storage item with the name '$name' could be found.")
    }

    override suspend fun getFolderWatcherAsync(): NioFolderWatcher = withContext(Dispatchers.IO) {
        NioFolderWatcher(this@NioFolder)
    }

    override suspend fun deleteAsync(item: StorableChild) = withContext(Dispatchers.IO) {
        val target = resolveChildPath(item)
        if (!Files.exists(target)) {
            throw FileNotFoundException("Item ${item.name} was not found in folder $name")
        }
        deleteRecursively(target)
    }

    override suspend fun createFolderAsync(name: String, overwrite: Boolean): ChildFolder =
        withContext(Dispatchers.IO) {
            val target = normalizedPath.resolve(name)

            when {
                Files.notExists(target) -> Files.createDirectory(target)
                Files.isDirectory(target) -> return@withContext createUnvalidated(target)
                !overwrite -> throw FileAlreadyExistsException(target.toString())
                else -> {
                    deleteRecursively(target)
                    Files.createDirectory(target)
                }
            }

            createUnvalidated(target)
        }

    override suspend fun createFileAsync(name: String, overwrite: Boolean): ChildFile =
        withContext(Dispatchers.IO) {
            val target = normalizedPath.resolve(name)

            when {
                Files.notExists(target) -> Files.createFile(target)
                Files.isRegularFile(target) -> {
                    if (overwrite) {
                        Files.newOutputStream(target, CREATE, TRUNCATE_EXISTING).use { }
                    }
                    return@withContext NioFile.createUnvalidated(target)
                }
                !overwrite -> throw FileAlreadyExistsException(target.toString())
                else -> {
                    deleteRecursively(target)
                    Files.createFile(target)
                }
            }

            NioFile.createUnvalidated(target)
        }

    override suspend fun moveFromAsync(
        fileToMove: ChildFile,
        source: ModifiableFolder,
        overwrite: Boolean,
        fallback: MoveFromDelegate,
    ): ChildFile = withContext(Dispatchers.IO) {
        if (fileToMove is NioFile) {
            return@withContext moveSystemFile(fileToMove.path, overwrite, fileToMove.name)
        }

        fallback(this@NioFolder, fileToMove, source, overwrite)
    }

    override suspend fun moveFromAsync(
        fileToMove: ChildFile,
        source: ModifiableFolder,
        overwrite: Boolean,
        newName: String,
        fallback: MoveRenamedFromDelegate,
    ): ChildFile = withContext(Dispatchers.IO) {
        if (fileToMove is NioFile) {
            return@withContext moveSystemFile(fileToMove.path, overwrite, newName)
        }

        fallback(this@NioFolder, fileToMove, source, overwrite, newName)
    }

    override suspend fun createCopyOfAsync(
        fileToCopy: File,
        overwrite: Boolean,
        fallback: CreateCopyOfDelegate,
    ): ChildFile = withContext(Dispatchers.IO) {
        if (fileToCopy is NioFile) {
            return@withContext copySystemFile(fileToCopy.path, overwrite, fileToCopy.name)
        }

        fallback(this@NioFolder, fileToCopy, overwrite)
    }

    override suspend fun createCopyOfAsync(
        fileToCopy: File,
        overwrite: Boolean,
        newName: String,
        fallback: CreateRenamedCopyOfDelegate,
    ): ChildFile = withContext(Dispatchers.IO) {
        if (fileToCopy is NioFile) {
            return@withContext copySystemFile(fileToCopy.path, overwrite, newName)
        }

        fallback(this@NioFolder, fileToCopy, overwrite, newName)
    }

    override suspend fun getRootAsync(): Folder? =
        normalizedPath.root?.let(::NioFolder)


    private fun childForPath(path: Path): StorableChild? = when {
        Files.isDirectory(path) -> createUnvalidated(path)
        Files.isRegularFile(path) -> NioFile.createUnvalidated(path)
        else -> null
    }

    private fun pathFromIdOrNull(id: String): Path? = try {
        Path.of(id).toAbsolutePath().normalize()
    } catch (_: InvalidPathException) {
        null
    }

    private fun moveSystemFile(sourcePath: Path, overwrite: Boolean, newName: String): ChildFile {
        val destinationPath = normalizedPath.resolve(newName)

        when {
            Files.notExists(destinationPath) -> Files.move(sourcePath, destinationPath)
            Files.isRegularFile(destinationPath) && overwrite -> Files.move(sourcePath, destinationPath, REPLACE_EXISTING)
            Files.isRegularFile(destinationPath) -> return NioFile.createUnvalidated(destinationPath)
            !overwrite -> throw FileAlreadyExistsException(destinationPath.toString())
            else -> {
                deleteRecursively(destinationPath)
                Files.move(sourcePath, destinationPath)
            }
        }

        return NioFile.createUnvalidated(destinationPath)
    }

    private fun copySystemFile(sourcePath: Path, overwrite: Boolean, newName: String): ChildFile {
        val destinationPath = normalizedPath.resolve(newName)

        when {
            Files.notExists(destinationPath) -> Files.copy(sourcePath, destinationPath)
            Files.isRegularFile(destinationPath) && overwrite -> Files.copy(sourcePath, destinationPath, REPLACE_EXISTING)
            Files.isRegularFile(destinationPath) -> return NioFile.createUnvalidated(destinationPath)
            !overwrite -> throw FileAlreadyExistsException(destinationPath.toString())
            else -> {
                deleteRecursively(destinationPath)
                Files.copy(sourcePath, destinationPath)
            }
        }

        return NioFile.createUnvalidated(destinationPath)
    }

    private fun resolveChildPath(item: StorableChild): Path {
        val candidate = when (item) {
            is NioFile -> item.path
            is NioFolder -> item.path
            else -> normalizedPath.resolve(item.name)
        }.toAbsolutePath().normalize()

        if (candidate.parent != normalizedPath) {
            throw FileNotFoundException("Item ${item.name} does not belong to folder $name")
        }

        return candidate
    }

    private fun deleteRecursively(path: Path) {
        if (Files.isDirectory(path)) {
            Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach(Files::deleteIfExists)
        } else {
            Files.deleteIfExists(path)
        }
    }

    internal companion object {
        fun createUnvalidated(path: Path) = NioFolder(path, skipValidation = true)
    }
}
