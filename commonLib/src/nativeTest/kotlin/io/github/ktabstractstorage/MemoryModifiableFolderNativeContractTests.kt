package io.github.ktabstractstorage

import io.github.ktabstractstorage.memory.MemoryFolder
import io.github.ktabstractstorage.testing.NativeCommonModifiableFolderTests

class MemoryModifiableFolderNativeContractTests : NativeCommonModifiableFolderTests() {

    override suspend fun createModifiableFolderFixtureAsync(): ModifiableFolderFixture =
        ModifiableFolderFixture(MemoryFolder("root"))

    override suspend fun createModifiableFolderWithItemsFixtureAsync(
        fileCount: Int,
        folderCount: Int,
    ): ModifiableFolderFixture {
        val root = MemoryFolder("root")

        repeat(fileCount) { index ->
            root.createFileAsync("file-$index.tmp")
        }
        repeat(folderCount) { index ->
            root.createFolderAsync("folder-$index")
        }

        return ModifiableFolderFixture(root)
    }
}
