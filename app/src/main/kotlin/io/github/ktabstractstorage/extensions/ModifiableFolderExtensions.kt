package io.github.ktabstractstorage.extensions

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.File
import io.github.ktabstractstorage.ModifiableFolder
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.extensions.interfaces.CreateCopyOf
import io.github.ktabstractstorage.extensions.interfaces.CreateRenamedCopyOf
import io.github.ktabstractstorage.extensions.interfaces.MoveFrom
import io.github.ktabstractstorage.extensions.interfaces.MoveRenamedFrom
import java.io.FileNotFoundException
import java.nio.file.FileAlreadyExistsException

/**
 * Creates a copy of [fileToCopy] within this folder.
 *
 * @param fileToCopy Source file to copy.
 * @param overwrite Whether to overwrite an existing destination file.
 */
suspend fun ModifiableFolder.createCopyOfAsync(
    fileToCopy: File,
    overwrite: Boolean,
): ChildFile {
    if (this is CreateCopyOf) {
        return createCopyOfAsync(fileToCopy, overwrite) { destination, file, ov ->
            createCopyOfFallbackAsync(destination, file, ov, file.name)
        }
    }

    return createCopyOfFallbackAsync(this, fileToCopy, overwrite, fileToCopy.name)
}

/**
 * Creates a copy of [fileToCopy] within this folder using [newName].
 *
 * @param fileToCopy Source file to copy.
 * @param overwrite Whether to overwrite an existing destination file.
 * @param newName Name to assign to the created copy.
 */
suspend fun ModifiableFolder.createCopyOfAsync(
    fileToCopy: File,
    overwrite: Boolean,
    newName: String,
): ChildFile {
    if (this is CreateRenamedCopyOf) {
        return createCopyOfAsync(
            fileToCopy,
            overwrite,
            newName,
            ::createCopyOfFallbackAsync,
        )
    }

    return createCopyOfFallbackAsync(this, fileToCopy, overwrite, newName)
}

/**
 * Copies this file's content into [destinationFile].
 *
 * @param destinationFile Target file that receives copied content.
 */
suspend fun File.copyToAsync(destinationFile: File) {
    openStreamAsync(FileAccessMode.READ).use { sourceStream ->
        destinationFile.openStreamAsync(FileAccessMode.WRITE).use { destinationStream ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val bytesRead = sourceStream.readAsync(buffer, 0, buffer.size)
                if (bytesRead <= 0) {
                    break
                }

                destinationStream.writeAsync(buffer, 0, bytesRead)
            }

            destinationStream.flushAsync()
        }
    }
}

/**
 * Moves [fileToMove] from [source] into this folder.
 *
 * @param fileToMove Source file to move.
 * @param source Folder currently containing [fileToMove].
 * @param overwrite Whether to overwrite an existing destination file.
 */
suspend fun ModifiableFolder.moveFromAsync(
    fileToMove: ChildFile,
    source: ModifiableFolder,
    overwrite: Boolean,
): ChildFile {
    if (this is MoveFrom) {
        return moveFromAsync(fileToMove, source, overwrite) { destination, file, src, ov ->
            moveFromFallbackAsync(destination, file, src, ov, file.name)
        }
    }

    return moveFromFallbackAsync(this, fileToMove, source, overwrite, fileToMove.name)
}

/**
 * Moves [fileToMove] from [source] into this folder using [newName].
 *
 * @param fileToMove Source file to move.
 * @param source Folder currently containing [fileToMove].
 * @param overwrite Whether to overwrite an existing destination file.
 * @param newName Name to assign to the moved file.
 */
suspend fun ModifiableFolder.moveFromAsync(
    fileToMove: ChildFile,
    source: ModifiableFolder,
    overwrite: Boolean,
    newName: String,
): ChildFile {
    if (this is MoveRenamedFrom) {
        return moveFromAsync(fileToMove, source, overwrite, newName, ::moveFromFallbackAsync)
    }

    return moveFromFallbackAsync(this, fileToMove, source, overwrite, newName)
}

private suspend fun createCopyOfFallbackAsync(
    destinationFolder: ModifiableFolder,
    fileToCopy: File,
    overwrite: Boolean,
    newName: String,
): ChildFile {
    if (!overwrite) {
        destinationFolder.ensureNoExistingItem(newName)
    }

    val destinationFile = destinationFolder.createFileAsync(newName, overwrite = true)
    fileToCopy.copyToAsync(destinationFile)
    return destinationFile
}

private suspend fun moveFromFallbackAsync(
    destinationFolder: ModifiableFolder,
    fileToMove: ChildFile,
    source: ModifiableFolder,
    overwrite: Boolean,
    newName: String,
): ChildFile {
    if (!overwrite) {
        destinationFolder.ensureNoExistingItem(newName)
    }

    val movedFile = createCopyOfFallbackAsync(destinationFolder, fileToMove, overwrite = true, newName = newName)
    source.deleteAsync(fileToMove)
    return movedFile
}

private suspend fun ModifiableFolder.ensureNoExistingItem(name: String) {
    try {
        getFirstByNameAsync(name)
        throw FileAlreadyExistsException(name)
    } catch (_: FileNotFoundException) {
        // Name is free.
    }
}


