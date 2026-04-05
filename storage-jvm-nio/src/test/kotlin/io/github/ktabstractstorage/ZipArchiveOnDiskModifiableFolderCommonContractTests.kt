package io.github.ktabstractstorage

import io.github.ktabstractstorage.nio.NioFile
import io.github.ktabstractstorage.testing.JvmCommonModifiableFolderTests
import io.github.ktabstractstorage.ziparchive.ZipArchiveFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import kotlin.io.path.deleteIfExists

private suspend fun createTempZipFile(prefix: String): java.nio.file.Path = withContext(Dispatchers.IO) {
    Files.createTempFile(prefix, ".zip")
}

private suspend fun deleteZipFileIfExists(path: java.nio.file.Path) = withContext(Dispatchers.IO) {
    path.deleteIfExists()
}

class ZipArchiveOnDiskModifiableFolderCommonContractTests : JvmCommonModifiableFolderTests() {


    override suspend fun createModifiableFolderFixtureAsync(): ModifiableFolderFixture {
        val tempZip = createTempZipFile("kt-abstract-storage-zip")
        val file = NioFile(tempZip)
        val root = ZipArchiveFolder(file, id = "zip:disk", name = file.name)

        return ModifiableFolderFixture(root) {
            deleteZipFileIfExists(tempZip)
        }
    }

    override suspend fun createModifiableFolderWithItemsFixtureAsync(
        fileCount: Int,
        folderCount: Int,
    ): ModifiableFolderFixture {
        val tempZip = createTempZipFile("kt-abstract-storage-zip-items")
        val file = NioFile(tempZip)
        val root = ZipArchiveFolder(file, id = "zip:disk", name = file.name)

        repeat(fileCount) { index ->
            root.createFileAsync("file-$index.tmp")
        }
        repeat(folderCount) { index ->
            root.createFolderAsync("folder-$index")
        }

        return ModifiableFolderFixture(root) {
            deleteZipFileIfExists(tempZip)
        }
    }
}

