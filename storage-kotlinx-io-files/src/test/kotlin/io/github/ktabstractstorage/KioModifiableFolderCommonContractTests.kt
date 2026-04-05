package io.github.ktabstractstorage

import io.github.ktabstractstorage.kiofiles.KioFolder
import io.github.ktabstractstorage.testing.JvmCommonModifiableFolderTests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import java.nio.file.Files
import java.util.Comparator

private suspend fun createTempFolderTestDirectory(prefix: String): java.nio.file.Path =
    withContext(Dispatchers.IO) { Files.createTempDirectory(prefix) }

private suspend fun deleteFolderTestDirectoryRecursively(rootPath: java.nio.file.Path) =
    withContext(Dispatchers.IO) {
        if (Files.exists(rootPath)) {
            Files.walk(rootPath)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

class KioModifiableFolderCommonContractTests : JvmCommonModifiableFolderTests() {

    override suspend fun createModifiableFolderFixtureAsync(): ModifiableFolderFixture {
        val rootPath = createTempFolderTestDirectory("kt-abstract-storage-kio")
        val root = KioFolder(Path(rootPath.toString()))

        return ModifiableFolderFixture(root) {
            deleteFolderTestDirectoryRecursively(rootPath)
        }
    }

    override suspend fun createModifiableFolderWithItemsFixtureAsync(
        fileCount: Int,
        folderCount: Int,
    ): ModifiableFolderFixture {
        val rootPath = createTempFolderTestDirectory("kt-abstract-storage-kio-with-items")
        val root = KioFolder(Path(rootPath.toString()))

        repeat(fileCount) { index -> root.createFileAsync("file-$index.tmp") }
        repeat(folderCount) { index -> root.createFolderAsync("folder-$index") }

        return ModifiableFolderFixture(root) {
            deleteFolderTestDirectoryRecursively(rootPath)
        }
    }

}

