package io.github.ktabstractstorage

import io.github.ktabstractstorage.streams.MemoryStream
import io.github.ktabstractstorage.testing.JvmCommonModifiableFolderTests
import io.github.ktabstractstorage.ziparchive.ZipArchiveFolder

class ZipArchiveInMemoryModifiableFolderCommonContractTests : JvmCommonModifiableFolderTests() {


    override suspend fun createModifiableFolderFixtureAsync(): ModifiableFolderFixture {
        val stream = MemoryStream()
        val root = ZipArchiveFolder(stream, id = "zip:memory", name = "archive.zip")
        return ModifiableFolderFixture(root)
    }

    override suspend fun createModifiableFolderWithItemsFixtureAsync(
        fileCount: Int,
        folderCount: Int,
    ): ModifiableFolderFixture {
        val stream = MemoryStream()
        val root = ZipArchiveFolder(stream, id = "zip:memory", name = "archive.zip")

        repeat(fileCount) { index ->
            root.createFileAsync("file-$index.tmp")
        }
        repeat(folderCount) { index ->
            root.createFolderAsync("folder-$index")
        }

        return ModifiableFolderFixture(root)
    }
}

