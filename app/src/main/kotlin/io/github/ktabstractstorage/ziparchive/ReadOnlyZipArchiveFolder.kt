package io.github.ktabstractstorage.ziparchive

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ChildFolder
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.StorableChild
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.extensions.interfaces.GetFirstByName
import io.github.ktabstractstorage.extensions.interfaces.GetItem
import io.github.ktabstractstorage.extensions.interfaces.GetItemRecursive
import io.github.ktabstractstorage.streams.UnifiedStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.FileNotFoundException
import java.io.IOException

open class ReadOnlyZipArchiveFolder internal constructor(
    internal val sharedState: ZipArchiveSharedState,
    protected val rootId: String,
    protected val rootName: String,
    internal val entryPath: String,
    private val parentFolder: Folder?,
) : ChildFolder, GetItem, GetItemRecursive, GetFirstByName {
    constructor(
        archiveFile: File,
        id: String = "zip:${archiveFile.id}",
        name: String = archiveFile.name,
    ) : this(
        sharedState = ZipArchiveSharedState(
            ZipArchiveStore(
                StorageFileZipArchiveIo(archiveFile, idHint = id, isReadOnly = true),
            ),
        ),
        rootId = id,
        rootName = name,
        entryPath = "",
        parentFolder = null,
    )

    constructor(
        zipStream: UnifiedStream,
        id: String = "zip:${System.identityHashCode(zipStream)}",
        name: String = "archive.zip",
    ) : this(
        sharedState = ZipArchiveSharedState(
            ZipArchiveStore(
                StreamZipArchiveIo(zipStream, idHint = id, isReadOnly = true),
            ),
        ),
        rootId = id,
        rootName = name,
        entryPath = "",
        parentFolder = null,
    )

    override val id: String
        get() = if (entryPath.isEmpty()) rootId else createEntryId(entryPath, isFolder = true)

    override val name: String
        get() = if (entryPath.isEmpty()) rootName else entryPath.substringAfterLast('/')

    override suspend fun getParentAsync(): Folder? = parentFolder

    override fun getItemsAsync(type: StorableType): Flow<StorableChild> = flow {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }
        listChildren(type).forEach { emit(it) }
    }

    override suspend fun getItemAsync(id: String): StorableChild {
        val (path, preferFolder) = parseEntryIdOrNull(id)
            ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")
        if (parentPath(path) != entryPath) {
            throw FileNotFoundException("No storage item with the id '$id' could be found.")
        }
        return createNode(path, preferFolder)
    }

    override suspend fun getItemRecursiveAsync(id: String): StorableChild {
        val (path, preferFolder) = parseEntryIdOrNull(id)
            ?: throw FileNotFoundException("No storage item with the id '$id' could be found.")
        if (path.isEmpty() || (entryPath.isNotEmpty() && !path.startsWith("$entryPath/"))) {
            throw FileNotFoundException("No storage item with the id '$id' could be found.")
        }
        return createNode(path, preferFolder)
    }

    override suspend fun getFirstByNameAsync(name: String): StorableChild {
        return listChildren(StorableType.ALL)
            .firstOrNull { it.name == name }
            ?: throw FileNotFoundException("No storage item with the name '$name' could be found.")
    }

    protected open suspend fun openEntryStream(path: String, accessMode: FileAccessMode): UnifiedStream {
        if (accessMode != FileAccessMode.READ) {
            throw IOException("ZIP archive is read-only.")
        }

        val base = sharedState.store.openEntryReadStream(path)
        return ZipFileStream(base, FileAccessMode.READ) { }
    }

    internal open fun createFolderNode(path: String): ReadOnlyZipArchiveFolder =
        ReadOnlyZipArchiveFolder(sharedState, rootId, rootName, path, this)

    internal open fun createFileNode(path: String): ZipArchiveEntryFile =
        ZipArchiveEntryFile(this, path, this, isReadOnly = true)

    internal suspend fun listChildren(type: StorableType): List<StorableChild> {
        return sharedState.store.listChildren(entryPath).mapNotNull { child ->
            val node: StorableChild = if (child.isFolder) createFolderNode(child.path) else createFileNode(child.path)
            when (type) {
                StorableType.ALL -> node
                StorableType.FILE -> node.takeIf { it is ChildFile }
                StorableType.FOLDER -> node.takeIf { it is ChildFolder }
                StorableType.NONE -> null
            }
        }
    }

    internal suspend fun createNode(path: String, preferFolder: Boolean): StorableChild {
        if (preferFolder && sharedState.store.directoryExists(path)) return createFolderNode(path)
        if (sharedState.store.fileExists(path)) return createFileNode(path)
        if (sharedState.store.directoryExists(path)) return createFolderNode(path)
        throw FileNotFoundException("No storage item with path '$path' could be found.")
    }

    internal fun createEntryId(path: String, isFolder: Boolean): String {
        val suffix = if (isFolder) "/" else ""
        return "$rootId!/$path$suffix"
    }

    internal fun parseEntryIdOrNull(value: String): Pair<String, Boolean>? {
        val prefix = "$rootId!/"
        if (!value.startsWith(prefix)) return null

        val rawPath = value.removePrefix(prefix)
        val preferFolder = rawPath.endsWith('/')
        val normalized = normalizeEntryPath(rawPath)
        if (normalized.isEmpty()) return null

        return normalized to preferFolder
    }

    internal fun normalizeEntryPath(path: String): String {
        val normalized = path.replace('\\', '/').trim('/')
        if (normalized.isEmpty()) return ""

        val segments = normalized.split('/')
        require(segments.all { it.isNotBlank() }) { "Entry path contains an empty segment." }
        require(segments.none { it == "." || it == ".." }) { "Entry path cannot include '.' or '..'." }
        return segments.joinToString("/")
    }

    internal fun combinePath(parentPath: String, childName: String): String {
        val clean = normalizeChildName(childName)
        return if (parentPath.isEmpty()) clean else "$parentPath/$clean"
    }

    internal fun normalizeChildName(name: String): String {
        require(name.isNotBlank()) { "Name cannot be blank." }
        require(!name.contains('/')) { "Name cannot contain '/'." }
        require(!name.contains('\\')) { "Name cannot contain '\\'." }
        require(name != "." && name != "..") { "Name cannot be '.' or '..'." }
        return name.trim()
    }

    internal fun parentPath(path: String): String {
        val idx = path.lastIndexOf('/')
        return if (idx < 0) "" else path.substring(0, idx)
    }

    internal suspend fun openEntryStreamForNode(path: String, accessMode: FileAccessMode): UnifiedStream =
        openEntryStream(path, accessMode)
}


