package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.kiofiles.KioFolder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.io.files.Path
import java.nio.file.Files
import java.util.Comparator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KioStorageTests {
    @Test
    fun system_folder_create_list_delete_roundtrip() = runTest {
        val rootPath = Files.createTempDirectory("kt-abstract-storage-kio")
        try {
            val root = KioFolder(Path(rootPath.toString()))

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
            if (Files.exists(rootPath)) {
                Files.walk(rootPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach { Files.deleteIfExists(it) }
            }
        }
    }
}

