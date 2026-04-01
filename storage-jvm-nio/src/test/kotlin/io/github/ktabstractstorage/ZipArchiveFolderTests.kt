package io.github.ktabstractstorage

import io.github.ktabstractstorage.extensions.getFirstByNameAsync
import io.github.ktabstractstorage.extensions.readTextAsync
import io.github.ktabstractstorage.extensions.writeTextAsync
import io.github.ktabstractstorage.system.SystemFile
import io.github.ktabstractstorage.ziparchive.ZipArchiveFolder
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.io.path.deleteIfExists
import kotlin.test.Test
import kotlin.test.assertEquals

class ZipArchiveFolderTests {

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

