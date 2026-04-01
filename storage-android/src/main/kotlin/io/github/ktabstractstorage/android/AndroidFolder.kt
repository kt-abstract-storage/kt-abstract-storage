package io.github.ktabstractstorage.android

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ChildFolder
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.ModifiableFolder
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.errors.StorageFileAlreadyExistsException
import io.github.ktabstractstorage.errors.StorageFileNotFoundException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path

/**
 * A [ModifiableFolder] implementation backed by a [java.io.File] directory, suitable for use on
 * Android (API 21+).
 *
 * Unlike the NIO-based `SystemFolder`, this implementation relies only on `java.io.File` APIs,
 * which are available across all supported Android API levels.
 *
 * A secondary constructor accepting [java.nio.file.Path] is available for API 26+ callers — it
 * converts the path to a [File] internally so all logic stays `java.io`-based.
 *
 * @param file The directory represented by this folder.
 * @throws IllegalArgumentException if [file] does not exist or is not a directory.
 */
class AndroidFolder internal constructor(
    internal val file: File,
    private val skipValidation: Boolean = false,
) : ModifiableFolder, ChildFolder {

    /**
     * Constructs an [AndroidFolder] from a [java.nio.file.Path].
     *
     * The path is converted to a [java.io.File] so all internal operations remain
     * compatible with API 21+. This constructor itself requires API 26 because
     * [java.nio.file] was not available on Android before that.
     *
     * @param path The NIO path representing the directory.
     * @throws IllegalArgumentException if the path does not exist or is not a directory.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    constructor(path: Path) : this(path.toFile())

    init {
        if (!skipValidation) {
            require(file.exists()) { "Folder path does not exist: ${file.absolutePath}" }
            require(file.isDirectory) { "Path is not a directory: ${file.absolutePath}" }
        }
    }

    override val id: String = file.canonicalPath

    override val name: String = file.name.ifEmpty { file.canonicalPath }

    /**
     * Returns this folder's path as a [java.nio.file.Path].
     *
     * Requires API 26+ because [java.nio.file] is not available on earlier Android versions.
     */
    @get:RequiresApi(Build.VERSION_CODES.O)
    val nioPath: Path
        get() = file.toPath()

    override suspend fun getParentAsync(): Folder? =
        withContext(Dispatchers.IO) { file.parentFile?.let { createUnvalidated(it) } }

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> = flow {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }

        val items: List<StorableChild> = withContext(Dispatchers.IO) {
            (file.listFiles() ?: emptyArray()).mapNotNull { child ->
                val storable = childForFile(child) ?: return@mapNotNull null
                val matchesType = when (type) {
                    StorableType.ALL -> true
                    StorableType.FILE -> storable is ChildFile
                    StorableType.FOLDER -> storable is ChildFolder
                    StorableType.NONE -> error("StorableType.NONE must be rejected before enumeration.")
                }
                storable.takeIf { matchesType }
            }
        }

        for (item in items) emit(item)
    }

    override suspend fun getFolderWatcherAsync(): AndroidFolderWatcher =
        withContext(Dispatchers.IO) { AndroidFolderWatcher(this@AndroidFolder) }

    override suspend fun deleteAsync(item: StorableChild) = withContext(Dispatchers.IO) {
        val target = resolveChild(item)
        if (!target.exists()) {
            throw StorageFileNotFoundException("Item '${item.name}' was not found in folder '$name'.")
        }
        deleteRecursively(target)
    }

    override suspend fun createFolderAsync(name: String, overwrite: Boolean): ChildFolder =
        withContext(Dispatchers.IO) {
            val target = File(file, name)
            when {
                !target.exists() -> {
                    target.mkdir() || error("Could not create directory: ${target.absolutePath}")
                }
                target.isDirectory -> return@withContext createUnvalidated(target)
                !overwrite -> throw StorageFileAlreadyExistsException("'$name' already exists as a file.")
                else -> {
                    deleteRecursively(target)
                    target.mkdir() || error("Could not create directory: ${target.absolutePath}")
                }
            }
            createUnvalidated(target)
        }

    override suspend fun createFileAsync(name: String, overwrite: Boolean): ChildFile =
        withContext(Dispatchers.IO) {
            val target = File(file, name)
            when {
                !target.exists() -> {
                    target.createNewFile() || error("Could not create file: ${target.absolutePath}")
                }
                target.isFile -> {
                    if (overwrite) target.writeBytes(ByteArray(0))
                    return@withContext AndroidFile.createUnvalidated(target)
                }
                !overwrite -> throw StorageFileAlreadyExistsException("'$name' already exists as a folder.")
                else -> {
                    deleteRecursively(target)
                    target.createNewFile() || error("Could not create file: ${target.absolutePath}")
                }
            }
            AndroidFile.createUnvalidated(target)
        }

    private fun childForFile(child: File): StorableChild? = when {
        child.isDirectory -> createUnvalidated(child)
        child.isFile -> AndroidFile.createUnvalidated(child)
        else -> null
    }

    private fun resolveChild(item: StorableChild): File {
        val candidate = when (item) {
            is AndroidFile -> item.file
            is AndroidFolder -> item.file
            else -> File(file, item.name)
        }.canonicalFile

        require(candidate.parentFile?.canonicalFile == file.canonicalFile) {
            "Item '${item.name}' does not belong to folder '$name'."
        }

        return candidate
    }

    private fun deleteRecursively(target: File) {
        if (target.isDirectory) {
            target.walkBottomUp().forEach { it.delete() }
        } else {
            target.delete()
        }
    }

    internal companion object {
        fun createUnvalidated(file: File) = AndroidFolder(file, skipValidation = true)

        @RequiresApi(Build.VERSION_CODES.O)
        fun createUnvalidated(path: Path) = AndroidFolder(path.toFile(), skipValidation = true)
    }
}

