package io.github.ktabstractstorage

import io.github.ktabstractstorage.extensions.getFirstByNameAsync
import io.github.ktabstractstorage.extensions.readTextAsync
import io.github.ktabstractstorage.extensions.writeTextAsync
import io.github.ktabstractstorage.nio.NioFile
import io.github.ktabstractstorage.ziparchive.ZipArchiveFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Behavior-specific tests for on-disk ZIP archives.
 * Tests the persistence behavior unique to this storage backend.
 * For comprehensive contract tests, see [ZipArchiveOnDiskModifiableFolderCommonContractTests].
 */
class ZipArchiveFolderTests {
    private suspend fun <T> withTempZipFile(block: suspend (NioFile) -> T): T {
        val tempZip = withContext(Dispatchers.IO) {
            Files.createTempFile("kt-abstract-storage", ".zip")
        }
        return try {
            block(NioFile(tempZip))
        } finally {
            withContext(Dispatchers.IO) {
                tempZip.deleteIfExists()
            }
        }
    }

    @Test
    fun on_disk_archive_persists_updates() = runTest {
        withTempZipFile { file ->
            val archive = ZipArchiveFolder(file, id = "zip:disk", name = file.name)
            val entry = archive.createFileAsync("persisted.txt")
            entry.writeTextAsync("persist me")

            val reopened = ZipArchiveFolder(file, id = "zip:disk", name = file.name)
            val reopenedEntry = reopened.getFirstByNameAsync("persisted.txt") as File

            assertEquals("persist me", reopenedEntry.readTextAsync())
        }
    }
}

