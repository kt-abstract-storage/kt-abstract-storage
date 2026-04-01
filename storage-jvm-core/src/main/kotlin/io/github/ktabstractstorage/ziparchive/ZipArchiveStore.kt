package io.github.ktabstractstorage.ziparchive

import io.github.ktabstractstorage.streams.asInputStream
import io.github.ktabstractstorage.streams.asOutputStream
import io.github.ktabstractstorage.streams.asUnifiedStream
import io.github.ktabstractstorage.streams.UnifiedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
    private val persistCoalescingWindowMs: Long = 0,
) {
    private val mutex = Mutex()
    private val persistMutex = Mutex()
    private var loaded = false
    private val directories = LinkedHashSet<String>()
    private val files = LinkedHashSet<String>()
    private val updatedFiles = LinkedHashMap<String, ByteArray>()

    init {
        require(persistCoalescingWindowMs >= 0) { "persistCoalescingWindowMs must be non-negative." }
    }

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

    suspend fun openEntryReadStream(path: String): UnifiedStream {
        val inMemory = mutex.withLock {
        ensureLoadedLocked()

        if (!files.contains(path)) {
            throw FileNotFoundException("No storage item with path '$path' could be found.")
        }

        updatedFiles[path]?.let { bytes ->
            return@withLock ByteArrayInputStream(bytes).asUnifiedStream(closeInputStreamOnClose = true)
        }

        null
    }

        if (inMemory != null) {
            return inMemory
        }

        val source = io.openRead()
        return ioBound {
            val zipIn = ZipInputStream(source.asInputStream(closeUnifiedStreamOnClose = true))

            try {
                while (true) {
                    val entry = zipIn.nextEntry ?: break
                    val normalized = normalize(entry.name)
                    if (entry.isDirectory || normalized != path) {
                        continue
                    }

                    return@ioBound zipIn.asUnifiedStream(closeInputStreamOnClose = true)
                }
            } catch (ex: Exception) {
                zipIn.close()
                throw ex
            }

            zipIn.close()
            throw FileNotFoundException("No storage item with path '$path' could be found.")
        }
    }

    suspend fun createFolder(path: String, overwrite: Boolean) {
        val changed = mutex.withLock {
            ensureLoadedLocked()
            requireWritable()

            when {
                directories.contains(path) -> return@withLock false
                files.contains(path) && !overwrite -> throw java.nio.file.FileAlreadyExistsException(path)
                files.contains(path) && overwrite -> {
                    files.remove(path)
                    updatedFiles.remove(path)
                }
            }

            directories.add(path)
            addParentDirectoriesLocked(path)
            true
        }

        if (changed) {
            persistUntilCaughtUp()
        }
    }

    suspend fun createFile(path: String, overwrite: Boolean) {
        val changed = mutex.withLock {
            ensureLoadedLocked()
            requireWritable()

            when {
                files.contains(path) && !overwrite -> return@withLock false
                files.contains(path) && overwrite -> {
                    updatedFiles[path] = ByteArray(0)
                    return@withLock true
                }
                directories.contains(path) && !overwrite -> throw java.nio.file.FileAlreadyExistsException(path)
                directories.contains(path) && overwrite -> deletePathLocked(path)
            }

            files.add(path)
            updatedFiles[path] = ByteArray(0)
            addParentDirectoriesLocked(path)
            true
        }

        if (changed) {
            persistUntilCaughtUp()
        }
    }

    suspend fun upsertFile(path: String, content: ByteArray) {
        mutex.withLock {
            ensureLoadedLocked()
            requireWritable()

            if (directories.contains(path)) {
                deletePathLocked(path)
            }

            files.add(path)
            updatedFiles[path] = content.copyOf()
            addParentDirectoriesLocked(path)
        }

        persistUntilCaughtUp()
    }

    suspend fun deletePath(path: String) {
        val changed = mutex.withLock {
            ensureLoadedLocked()
            requireWritable()

            if (!files.contains(path) && !directories.contains(path)) {
                throw FileNotFoundException("No storage item with path '$path' could be found.")
            }

            deletePathLocked(path)
            true
        }

        if (changed) {
            persistUntilCaughtUp()
        }
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

        val source = io.openRead()

        ioBound {
            try {
                source.use { stream ->
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
    }

    private suspend fun persistUntilCaughtUp() {
        persistMutex.withLock {
            while (true) {
                // Give nearby mutations a short window to join this persist cycle.
                if (persistCoalescingWindowMs > 0) {
                    delay(persistCoalescingWindowMs)
                }

                val snapshot = mutex.withLock {
                    ensureLoadedLocked()
                    PersistSnapshot(
                        directories = directories.toList(),
                        files = files.toSet(),
                        updatedFiles = updatedFiles.toMap(),
                    )
                }

                val source = io.openRead()

                val unchangedEntries = ioBound {
                    source.asInputStream(closeUnifiedStreamOnClose = true).use { sourceInput ->
                        readUnchangedEntries(sourceInput, snapshot.files, snapshot.updatedFiles)
                    }
                }

                val destination = io.openWrite()

                ioBound {
                    destination.use { out ->
                        ZipOutputStream(out.asOutputStream(closeUnifiedStreamOnClose = false)).use { zipOut ->
                            snapshot.directories.sorted().forEach { dirPath ->
                                zipOut.putNextEntry(ZipEntry("$dirPath/"))
                                zipOut.closeEntry()
                            }

                            unchangedEntries
                                .entries
                                .sortedBy { it.key }
                                .forEach { (path, data) ->
                                    zipOut.putNextEntry(ZipEntry(path))
                                    zipOut.write(data)
                                    zipOut.closeEntry()
                                }

                            snapshot.updatedFiles.entries
                                .sortedBy { it.key }
                                .forEach { (path, data) ->
                                    if (!snapshot.files.contains(path)) {
                                        return@forEach
                                    }

                                    zipOut.putNextEntry(ZipEntry(path))
                                    zipOut.write(data)
                                    zipOut.closeEntry()
                                }
                        }

                        out.flush()
                    }
                }

                val caughtUp = mutex.withLock {
                    // Clear only entries still equal to what was just persisted.
                    snapshot.updatedFiles.forEach { (path, data) ->
                        val current = updatedFiles[path] ?: return@forEach
                        if (current.contentEquals(data)) {
                            updatedFiles.remove(path)
                        }
                    }
                    updatedFiles.isEmpty()
                }

                if (caughtUp) {
                    return
                }
            }
        }
    }

    private fun readUnchangedEntries(
        sourceInput: InputStream,
        filesSnapshot: Set<String>,
        updatedFilesSnapshot: Map<String, ByteArray>,
    ): Map<String, ByteArray> {
        val unchanged = LinkedHashMap<String, ByteArray>()
        try {
            ZipInputStream(sourceInput).use { zipIn ->
                val copyBuffer = ByteArray(64 * 1024)
                while (true) {
                    val entry = zipIn.nextEntry ?: break
                    val normalized = normalize(entry.name)
                    if (entry.isDirectory || normalized.isEmpty()) {
                        continue
                    }

                    if (!filesSnapshot.contains(normalized) || updatedFilesSnapshot.containsKey(normalized)) {
                        continue
                    }

                    val entryOutput = java.io.ByteArrayOutputStream()
                    while (true) {
                        val read = zipIn.read(copyBuffer)
                        if (read <= 0) {
                            break
                        }
                        entryOutput.write(copyBuffer, 0, read)
                    }
                    unchanged[normalized] = entryOutput.toByteArray()
                }
            }
        } catch (_: ZipException) {
            // Treat non-zip source as empty source.
        }

        return unchanged
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

    private suspend fun <T> ioBound(block: () -> T): T = withContext(Dispatchers.IO) { block() }

    private data class PersistSnapshot(
        val directories: List<String>,
        val files: Set<String>,
        val updatedFiles: Map<String, ByteArray>,
    )
}

