package io.github.ktabstractstorage.testing

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.ModifiableFolder
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.extensions.createCopyOfAsync
import io.github.ktabstractstorage.extensions.moveFromAsync
import kotlinx.coroutines.flow.toList
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Common reusable contracts for [ModifiableFolder] implementations.
 */
abstract class CommonModifiableFolderTests : CommonFolderTests() {
    protected class ModifiableFolderFixture(
        val root: ModifiableFolder,
        cleanup: suspend () -> Unit = {},
    ) : FolderFixture(root, cleanup)

    /**
     * Some implementations treat overwrite=true for folders as open-existing behavior.
     */
    protected open val overwriteExistingFolderClearsContents: Boolean = false

    protected abstract suspend fun createModifiableFolderFixtureAsync(): ModifiableFolderFixture

    protected abstract suspend fun createModifiableFolderWithItemsFixtureAsync(
        fileCount: Int,
        folderCount: Int,
    ): ModifiableFolderFixture

    override suspend fun createFolderFixtureAsync(): FolderFixture = createModifiableFolderFixtureAsync()

    override suspend fun createFolderWithItemsFixtureAsync(fileCount: Int, folderCount: Int): FolderFixture =
        createModifiableFolderWithItemsFixtureAsync(fileCount, folderCount)

    protected suspend fun runAllCommonModifiableFolderContracts() {
        runAllCommonFolderContracts()
        runCreateListDeleteRoundtripContract()
        runDeleteAsyncContract()
        runCreateNewFolderNameNotExistsContract()
        runCreateNewFolderNameExistsNoOverwriteContract()
        runCreateNewFolderNameExistsOverwriteContract()
        runCreateNewFileNameNotExistsContract()
        runCreateNewFileNameExistsNoOverwriteContract()
        runCreateNewFileNameExistsOverwriteContract()
        runCreateCopyOfContract()
        runMoveFromContract()
    }

    protected suspend fun runCreateListDeleteRoundtripContract() {
        val fixture = createModifiableFolderFixtureAsync()
        try {
            val root = fixture.root

            val file = root.createFileAsync("a.txt")
            val folder = root.createFolderAsync("sub")

            val names = root.getItemsAsync(StorableType.ALL).toList().map { it.name }.sorted()
            assertEquals(listOf("a.txt", "sub"), names)

            root.deleteAsync(file)
            val remaining = root.getItemsAsync(StorableType.ALL).toList().map { it.name }
            assertEquals(listOf("sub"), remaining)

            root.deleteAsync(folder)
            assertTrue(root.getItemsAsync(StorableType.ALL).toList().isEmpty())
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runDeleteAsyncContract() {
        val fixture = createModifiableFolderWithItemsFixtureAsync(fileCount = 1, folderCount = 1)
        try {
            val root = fixture.root
            val firstItem = root.getItemsAsync(StorableType.ALL).toList().first()

            root.deleteAsync(firstItem)

            val remaining = root.getItemsAsync(StorableType.ALL).toList()
            assertTrue(remaining.none { it.id == firstItem.id })
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runCreateNewFolderNameNotExistsContract() {
        val fixture = createModifiableFolderWithItemsFixtureAsync(fileCount = 1, folderCount = 1)
        try {
            val root = fixture.root
            root.createFolderAsync("name")

            val folderCount = root.getItemsAsync(StorableType.FOLDER).toList().size
            assertEquals(2, folderCount)
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runCreateNewFolderNameExistsNoOverwriteContract() {
        val fixture = createModifiableFolderFixtureAsync()
        try {
            val root = fixture.root
            val created = root.createFolderAsync("name") as ModifiableFolder
            val marker = created.createFolderAsync("marker")

            val opened = root.createFolderAsync("name", overwrite = false) as Folder
            val openedFolders = opened.getItemsAsync(StorableType.FOLDER).toList()

            assertTrue(openedFolders.any { it.id == marker.id })
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runCreateNewFolderNameExistsOverwriteContract() {
        val fixture = createModifiableFolderFixtureAsync()
        try {
            val root = fixture.root
            val created = root.createFolderAsync("name") as ModifiableFolder
            val marker = created.createFolderAsync("marker")

            val recreated = root.createFolderAsync("name", overwrite = true) as Folder
            val recreatedFolders = recreated.getItemsAsync(StorableType.FOLDER).toList()

            if (overwriteExistingFolderClearsContents) {
                assertTrue(recreatedFolders.none { it.id == marker.id })
            } else {
                assertTrue(recreatedFolders.any { it.id == marker.id })
            }
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runCreateNewFileNameNotExistsContract() {
        val fixture = createModifiableFolderWithItemsFixtureAsync(fileCount = 1, folderCount = 1)
        try {
            val root = fixture.root
            root.createFileAsync("name")

            val fileCount = root.getItemsAsync(StorableType.FILE).toList().size
            assertEquals(2, fileCount)
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runCreateNewFileNameExistsNoOverwriteContract() {
        val fixture = createModifiableFolderFixtureAsync()
        try {
            val root = fixture.root
            val original = root.createFileAsync("name")
            val expected = deterministicPayload(32)
            writeAllBytes(original, expected)

            val reopened = root.createFileAsync("name", overwrite = false)
            val actual = readAllBytes(reopened)

            assertContentEquals(expected, actual)
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runCreateNewFileNameExistsOverwriteContract() {
        val fixture = createModifiableFolderFixtureAsync()
        try {
            val root = fixture.root
            val original = root.createFileAsync("name")
            val oldContent = deterministicPayload(32)
            writeAllBytes(original, oldContent)

            val recreated = root.createFileAsync("name", overwrite = true)
            val actual = readAllBytes(recreated)

            assertNotEquals(oldContent.toList(), actual.toList())
            assertEquals(0, actual.size)
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runCreateCopyOfContract() {
        val sourceFixture = createModifiableFolderFixtureAsync()
        val destinationFixture = createModifiableFolderFixtureAsync()

        try {
            val sourceFile = sourceFixture.root.createFileAsync("source.bin")
            val expected = deterministicPayload(48)
            writeAllBytes(sourceFile, expected)

            val copy = destinationFixture.root.createCopyOfAsync(sourceFile, overwrite = true)
            val actual = readAllBytes(copy)

            assertContentEquals(expected, actual)
        } finally {
            sourceFixture.cleanup()
            destinationFixture.cleanup()
        }
    }

    protected suspend fun runMoveFromContract() {
        val sourceFixture = createModifiableFolderFixtureAsync()
        val destinationFixture = createModifiableFolderFixtureAsync()

        try {
            val sourceFile = sourceFixture.root.createFileAsync("source.bin")
            val expected = deterministicPayload(64)
            writeAllBytes(sourceFile, expected)

            val moved = destinationFixture.root.moveFromAsync(sourceFile, sourceFixture.root, overwrite = true)
            val movedBytes = readAllBytes(moved)

            assertContentEquals(expected, movedBytes)

            val sourceItems = sourceFixture.root.getItemsAsync(StorableType.ALL).toList()
            assertTrue(sourceItems.none { it.id == sourceFile.id })
        } finally {
            sourceFixture.cleanup()
            destinationFixture.cleanup()
        }
    }

    private fun deterministicPayload(size: Int): ByteArray = ByteArray(size) { index ->
        ((index * 31) and 0xFF).toByte()
    }

    private suspend fun writeAllBytes(file: ChildFile, bytes: ByteArray) {
        file.openStreamAsync(FileAccessMode.WRITE).use { stream ->
            stream.write(bytes, 0, bytes.size)
            stream.flush()
        }
    }

    private suspend fun readAllBytes(file: ChildFile): ByteArray {
        file.openStreamAsync(FileAccessMode.READ).use { stream ->
            return readAllFromStream(stream)
        }
    }

    private fun readAllFromStream(stream: io.github.ktabstractstorage.streams.UnifiedStream): ByteArray {
        val chunk = ByteArray(1024)
        val chunks = mutableListOf<ByteArray>()
        var totalSize = 0

        while (true) {
            val read = stream.read(chunk, 0, chunk.size)
            if (read <= 0) {
                break
            }

            val part = ByteArray(read)
            chunk.copyInto(part, 0, 0, read)
            chunks += part
            totalSize += read
        }

        val all = ByteArray(totalSize)
        var offset = 0
        for (part in chunks) {
            part.copyInto(all, offset, 0, part.size)
            offset += part.size
        }

        return all
    }
}



