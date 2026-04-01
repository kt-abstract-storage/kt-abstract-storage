package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.extensions.getFirstByNameAsync
import io.github.ktabstractstorage.extensions.readTextAsync
import io.github.ktabstractstorage.extensions.writeTextAsync
import io.github.ktabstractstorage.streams.MemoryStream
import io.github.ktabstractstorage.system.SystemFile
import io.github.ktabstractstorage.ziparchive.ZipArchiveFolder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ZipArchiveFolderTests {
    @Test
    fun in_memory_archive_supports_nested_items_and_file_content() = runTest {
        val stream = MemoryStream()
        val root = ZipArchiveFolder(stream, id = "zip:test", name = "test.zip")

        val subA = root.createFolderAsync("subA") as ModifiableFolder
        val fileA = subA.createFileAsync("fileA.txt")
        fileA.writeTextAsync("hello zip")

        val subB = root.createFolderAsync("subB")
        assertIs<ChildFolder>(subB)

        val rootItems = root.getItemsAsync(StorableType.ALL).toList().map { it.name }
        assertEquals(listOf("subA", "subB"), rootItems)

        val fileFromFolder = (root.getFirstByNameAsync("subA") as Folder)
            .getFirstByNameAsync("fileA.txt") as File

        assertEquals("hello zip", fileFromFolder.readTextAsync())
    }

    @Test
    fun on_disk_archive_persists_updates() = runTest {
        val tempZip = Files.createTempFile("kt-abstract-storage", ".zip")
        try {
            val file = SystemFile(tempZip)
            val archive = ZipArchiveFolder(file, id = "zip:disk", name = tempZip.fileName.toString())
            val entry = archive.createFileAsync("persisted.txt")
            entry.writeTextAsync("persist me")

            val reopened = ZipArchiveFolder(SystemFile(tempZip), id = "zip:disk", name = tempZip.fileName.toString())
            val reopenedEntry = reopened.getFirstByNameAsync("persisted.txt") as File

            assertEquals("persist me", reopenedEntry.readTextAsync())
        } finally {
            tempZip.deleteIfExists()
        }
    }
}

