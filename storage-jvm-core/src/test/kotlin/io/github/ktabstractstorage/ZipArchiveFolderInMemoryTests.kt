package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.extensions.getFirstByNameAsync
import io.github.ktabstractstorage.extensions.readTextAsync
import io.github.ktabstractstorage.extensions.writeTextAsync
import io.github.ktabstractstorage.streams.MemoryStream
import io.github.ktabstractstorage.ziparchive.ZipArchiveFolder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Behavior-specific tests for in-memory ZIP archives.
 * Tests the content and nested structure behavior unique to this storage backend.
 * For comprehensive contract tests, see [ZipArchiveInMemoryModifiableFolderCommonContractTests].
 */
class ZipArchiveFolderInMemoryTests {
    private suspend fun createFileWithText(folder: ModifiableFolder, name: String, content: String): File {
        val file = folder.createFileAsync(name)
        file.writeTextAsync(content)
        return file
    }

    @Test
    fun in_memory_archive_supports_nested_items_and_file_content() = runTest {
        val stream = MemoryStream()
        val root = ZipArchiveFolder(stream, id = "zip:test", name = "test.zip")

        val subA = root.createFolderAsync("subA") as ModifiableFolder
        createFileWithText(subA, "fileA.txt", "hello zip")

        val subB = root.createFolderAsync("subB")
        assertIs<ChildFolder>(subB)

        val rootItems = root.getItemsAsync(StorableType.ALL).toList().map { it.name }
        assertEquals(listOf("subA", "subB"), rootItems)

        val fileFromFolder = (root.getFirstByNameAsync("subA") as Folder)
            .getFirstByNameAsync("fileA.txt") as File

        assertEquals("hello zip", fileFromFolder.readTextAsync())
    }
}
