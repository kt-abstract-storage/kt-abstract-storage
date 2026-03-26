package com.itswin11.ktabstractstorage.ziparchive

import com.itswin11.ktabstractstorage.streams.asInputStream
import com.itswin11.ktabstractstorage.streams.asOutputStream
import com.itswin11.ktabstractstorage.streams.asUnifiedStream
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.ByteArrayInputStream
import java.util.LinkedHashMap
import java.util.LinkedHashSet
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal data class ZipChildDescriptor(
    val name: String,
    val path: String,
    val isFolder: Boolean,
)

internal class ZipArchiveStore(
    private val io: ZipArchiveIo,
) {
    private val mutex = Mutex()
    private var loaded = false
    private val directories = LinkedHashSet<String>()
    private val files = LinkedHashSet<String>()
    private val updatedFiles = LinkedHashMap<String, ByteArray>()

    suspend fun listChildren(parentPath: String): List<ZipChildDescriptor> = mutex.withLock {
        ensureLoadedLocked()

        val prefix = if (parentPath.isEmpty()) "" else "$parentPath/"
        val byName = linkedMapOf<String, ZipChildDescriptor>()

        for (dirPath in directories) {
            val child = directChildOrNull(dirPath, prefix, true)
            if (child != null) byName.putIfAbsent(child.name, child)
        }

        for (filePath in files) {
            val child = directChildOrNull(filePath, prefix, false)
            if (child != null) byName.putIfAbsent(child.name, child)
        }

        byName.values.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }, { it.name }))
    }

    suspend fun fileExists(path: String): Boolean = mutex.withLock {
        ensureLoadedLocked()
        files.contains(path)
    }

    suspend fun directoryExists(path: String): Boolean = mutex.withLock {
        ensureLoadedLocked()
        directories.contains(path)
    }

    suspend fun openEntryReadStream(path: String) = mutex.withLock {
        ensureLoadedLocked()

        if (!files.contains(path)) {
            throw FileNotFoundException("No storage item with path '$path' could be found.")
        }

        updatedFiles[path]?.let { bytes ->
            return@withLock ByteArrayInputStream(bytes).asUnifiedStream(closeInputStreamOnClose = true)
        }

        val source = io.openRead()
        val zipIn = ZipInputStream(source.asInputStream(closeUnifiedStreamOnClose = true))

        try {
            while (true) {
                val entry = zipIn.nextEntry ?: break
                val normalized = normalize(entry.name)
                if (entry.isDirectory || normalized != path) {
                    continue
                }

                return@withLock zipIn.asUnifiedStream(closeInputStreamOnClose = true)
            }
        } catch (ex: Exception) {
            zipIn.close()
            throw ex
        }

        zipIn.close()
        throw FileNotFoundException("No storage item with path '$path' could be found.")
    }

    suspend fun createFolder(path: String, overwrite: Boolean) = mutex.withLock {
        ensureLoadedLocked()
        requireWritable()

        when {
            directories.contains(path) -> return@withLock
            files.contains(path) && !overwrite -> throw java.nio.file.FileAlreadyExistsException(path)
            files.contains(path) && overwrite -> {
                files.remove(path)
                updatedFiles.remove(path)
            }
        }

        directories.add(path)
        addParentDirectoriesLocked(path)
        persistLocked()
    }

    suspend fun createFile(path: String, overwrite: Boolean) = mutex.withLock {
        ensureLoadedLocked()
        requireWritable()

        when {
            files.contains(path) && !overwrite -> return@withLock
            files.contains(path) && overwrite -> {
                updatedFiles[path] = ByteArray(0)
                persistLocked()
                return@withLock
            }
            directories.contains(path) && !overwrite -> throw java.nio.file.FileAlreadyExistsException(path)
            directories.contains(path) && overwrite -> deletePathLocked(path)
        }

        files.add(path)
        updatedFiles[path] = ByteArray(0)
        addParentDirectoriesLocked(path)
        persistLocked()
    }

    suspend fun upsertFile(path: String, content: ByteArray) = mutex.withLock {
        ensureLoadedLocked()
        requireWritable()

        if (directories.contains(path)) {
            deletePathLocked(path)
        }

        files.add(path)
        updatedFiles[path] = content.copyOf()
        addParentDirectoriesLocked(path)
        persistLocked()
    }

    suspend fun deletePath(path: String) = mutex.withLock {
        ensureLoadedLocked()
        requireWritable()

        if (!files.contains(path) && !directories.contains(path)) {
            throw FileNotFoundException("No storage item with path '$path' could be found.")
        }

        deletePathLocked(path)
        persistLocked()
    }

    private fun requireWritable() {
        if (io.isReadOnly) throw UnsupportedOperationException("ZIP archive is read-only.")
    }

    private fun deletePathLocked(path: String) {
        files.remove(path)
        updatedFiles.remove(path)
        directories.remove(path)

        val prefix = "$path/"
        files.removeIf { it.startsWith(prefix) }
        updatedFiles.keys.removeIf { it.startsWith(prefix) }
        directories.removeIf { it.startsWith(prefix) }
    }

    private fun directChildOrNull(path: String, parentPrefix: String, isFolder: Boolean): ZipChildDescriptor? {
        if (!path.startsWith(parentPrefix) || path.length <= parentPrefix.length) return null
        val remainder = path.substring(parentPrefix.length)
        if ('/' in remainder) return null
        return ZipChildDescriptor(remainder, path, isFolder)
    }

    private suspend fun ensureLoadedLocked() {
        if (loaded) return
        loaded = true

        try {
            io.openRead().use { stream ->
                ZipInputStream(stream.asInputStream(closeUnifiedStreamOnClose = false)).use { zipIn ->
                    while (true) {
                        val entry = zipIn.nextEntry ?: break
                        val normalized = normalize(entry.name)
                        if (normalized.isEmpty()) continue

                        if (entry.isDirectory) {
                            directories.add(normalized)
                            addParentDirectoriesLocked(normalized)
                        } else {
                            files.add(normalized)
                            addParentDirectoriesLocked(normalized)
                        }
                    }
                }
            }
        } catch (_: ZipException) {
            // Not a valid ZIP yet; treat as empty archive.
        }
    }

    private suspend fun persistLocked() {
        val source = io.openRead()
        val sourceInput = source.asInputStream(closeUnifiedStreamOnClose = true)

        io.openWrite().use { out ->
            ZipOutputStream(out.asOutputStream(closeUnifiedStreamOnClose = false)).use { zipOut ->
                directories.sorted().forEach { dirPath ->
                    zipOut.putNextEntry(ZipEntry("$dirPath/"))
                    zipOut.closeEntry()
                }

                copyUnchangedEntries(sourceInput, zipOut)

                updatedFiles.entries
                    .sortedBy { it.key }
                    .forEach { (path, data) ->
                        if (!files.contains(path)) {
                            return@forEach
                        }

                        zipOut.putNextEntry(ZipEntry(path))
                        zipOut.write(data)
                        zipOut.closeEntry()
                    }
            }

            out.flushAsync()
        }
    }

    private fun copyUnchangedEntries(sourceInput: InputStream, zipOut: ZipOutputStream) {
        try {
            ZipInputStream(sourceInput).use { zipIn ->
                val copyBuffer = ByteArray(DEFAULT_BUFFER_SIZE)
                while (true) {
                    val entry = zipIn.nextEntry ?: break
                    val normalized = normalize(entry.name)
                    if (entry.isDirectory || normalized.isEmpty()) {
                        continue
                    }

                    if (!files.contains(normalized) || updatedFiles.containsKey(normalized)) {
                        continue
                    }

                    zipOut.putNextEntry(ZipEntry(normalized))
                    while (true) {
                        val read = zipIn.read(copyBuffer)
                        if (read <= 0) {
                            break
                        }
                        zipOut.write(copyBuffer, 0, read)
                    }
                    zipOut.closeEntry()
                }
            }
        } catch (_: ZipException) {
            // Treat non-zip source as empty source.
        }
    }

    private fun addParentDirectoriesLocked(path: String) {
        val parts = path.split('/')
        if (parts.size <= 1) return

        var current = ""
        for (i in 0 until parts.lastIndex) {
            current = if (current.isEmpty()) parts[i] else "$current/${parts[i]}"
            directories.add(current)
        }
    }

    private fun normalize(path: String): String =
        path.replace('\\', '/').trim('/').split('/')
            .filter { it.isNotBlank() && it != "." && it != ".." }
            .joinToString("/")
}

