package com.itswin11.ktabstractstorage.system

import com.itswin11.ktabstractstorage.ChildFile
import com.itswin11.ktabstractstorage.ChildFolder
import com.itswin11.ktabstractstorage.Folder
import com.itswin11.ktabstractstorage.ModifiableFolder
import com.itswin11.ktabstractstorage.StorableChild
import com.itswin11.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.TRUNCATE_EXISTING
import java.util.Comparator

/**
 * A java.nio implementation of a modifiable folder.
 *
 * @param path The path represented by this folder.
 * @throws IllegalArgumentException if the path does not exist or is not a directory.
 */
class SystemFolder(
    internal val path: Path,
    private val skipValidation: Boolean = false,
) : ModifiableFolder, ChildFolder {
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

    override suspend fun getParentAsync(): Folder? = normalizedPath.parent?.let(::SystemFolder)

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> = flow {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }

        val items: List<StorableChild> = withContext(Dispatchers.IO) {
            Files.createDirectories(normalizedPath)
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

    override suspend fun getFolderWatcherAsync(): SystemFolderWatcher = withContext(Dispatchers.IO) {
        Files.createDirectories(normalizedPath)
        SystemFolderWatcher(this@SystemFolder)
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
            Files.createDirectories(normalizedPath)
            val target = normalizedPath.resolve(name)

            when {
                Files.notExists(target) -> Files.createDirectory(target)
                Files.isDirectory(target) -> return@withContext SystemFolder(target)
                !overwrite -> throw FileAlreadyExistsException(target.toString())
                else -> {
                    deleteRecursively(target)
                    Files.createDirectory(target)
                }
            }

            SystemFolder(target)
        }

    override suspend fun createFileAsync(name: String, overwrite: Boolean): ChildFile =
        withContext(Dispatchers.IO) {
            Files.createDirectories(normalizedPath)
            val target = normalizedPath.resolve(name)

            when {
                Files.notExists(target) -> Files.createFile(target)
                Files.isRegularFile(target) -> {
                    if (overwrite) {
                        Files.newOutputStream(target, CREATE, TRUNCATE_EXISTING).use { }
                    }
                    return@withContext SystemFile(target)
                }
                !overwrite -> throw FileAlreadyExistsException(target.toString())
                else -> {
                    deleteRecursively(target)
                    Files.createFile(target)
                }
            }

            SystemFile(target)
        }

    private fun childForPath(path: Path): StorableChild? = when {
        Files.isDirectory(path) -> SystemFolder.createUnvalidated(path)
        Files.isRegularFile(path) -> SystemFile.createUnvalidated(path)
        else -> null
    }

    private fun resolveChildPath(item: StorableChild): Path {
        val candidate = when (item) {
            is SystemFile -> item.path
            is SystemFolder -> item.path
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
        fun createUnvalidated(path: Path) = SystemFolder(path, skipValidation = true)
    }
}
