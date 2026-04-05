package io.github.ktabstractstorage

import io.github.ktabstractstorage.nio.NioFolder
import io.github.ktabstractstorage.testing.JvmCommonModifiableFolderTests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.util.Comparator

private suspend fun createTempFolderDirectoryOnIo(prefix: String): java.nio.file.Path = withContext(Dispatchers.IO) {
    Files.createTempDirectory(prefix)
}

private suspend fun deleteFolderRecursivelyIfExistsOnIo(rootPath: java.nio.file.Path) = withContext(Dispatchers.IO) {
    if (Files.exists(rootPath)) {
        Files.walk(rootPath)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }
}

class NioModifiableFolderCommonContractTests : JvmCommonModifiableFolderTests() {


    override suspend fun createModifiableFolderFixtureAsync(): ModifiableFolderFixture {
        val rootPath = createTempFolderDirectoryOnIo("kt-abstract-storage")
        val root = NioFolder(rootPath)

        return ModifiableFolderFixture(root) {
            deleteFolderRecursivelyIfExistsOnIo(rootPath)
        }
    }

    override suspend fun createModifiableFolderWithItemsFixtureAsync(
        fileCount: Int,
        folderCount: Int,
    ): ModifiableFolderFixture {
        val rootPath = createTempFolderDirectoryOnIo("kt-abstract-storage-with-items")
        val root = NioFolder(rootPath)

        repeat(fileCount) { index ->
            root.createFileAsync("file-$index.tmp")
        }
        repeat(folderCount) { index ->
            root.createFolderAsync("folder-$index")
        }

        return ModifiableFolderFixture(root) {
            deleteFolderRecursivelyIfExistsOnIo(rootPath)
        }
    }
}

