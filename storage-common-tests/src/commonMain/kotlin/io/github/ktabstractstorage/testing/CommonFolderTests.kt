package io.github.ktabstractstorage.testing

import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.ChildFolder
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.flow.toList
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith

/**
 * Common reusable contracts for [Folder] implementations.
 */
abstract class CommonFolderTests {
    protected open class FolderFixture(
        val folder: Folder,
        val cleanup: suspend () -> Unit = {},
    )

    protected open val allowsIdEqualToName: Boolean = false

    protected abstract suspend fun createFolderFixtureAsync(): FolderFixture

    protected abstract suspend fun createFolderWithItemsFixtureAsync(fileCount: Int, folderCount: Int): FolderFixture

    protected suspend fun runAllCommonFolderContracts() {
        runConstructorCallValidParametersContract()
        runHasValidNameContract()
        runHasValidIdContract()
        runGetItemsAllCombinationsContract()
    }

    protected suspend fun runConstructorCallValidParametersContract() {
        val fixture = createFolderFixtureAsync()
        fixture.cleanup()
    }

    protected suspend fun runHasValidNameContract() {
        val fixture = createFolderFixtureAsync()
        try {
            assertFalse(fixture.folder.name.isBlank())
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runHasValidIdContract() {
        val fixture = createFolderFixtureAsync()
        try {
            val folder = fixture.folder
            assertFalse(folder.id.isBlank())
            if (!allowsIdEqualToName) {
                assertFalse(folder.name == folder.id)
            }
        } finally {
            fixture.cleanup()
        }
    }

    protected suspend fun runGetItemsAllCombinationsContract() {
        runGetItemsSingleCombinationContract(StorableType.NONE, 0, 0)
        runGetItemsSingleCombinationContract(StorableType.NONE, 2, 2)
        runGetItemsSingleCombinationContract(StorableType.FILE, 2, 0)
        runGetItemsSingleCombinationContract(StorableType.FILE, 0, 2)
        runGetItemsSingleCombinationContract(StorableType.FILE, 0, 0)
        runGetItemsSingleCombinationContract(StorableType.FOLDER, 2, 0)
        runGetItemsSingleCombinationContract(StorableType.FOLDER, 0, 2)
        runGetItemsSingleCombinationContract(StorableType.FOLDER, 0, 0)
        runGetItemsSingleCombinationContract(StorableType.ALL, 2, 0)
        runGetItemsSingleCombinationContract(StorableType.ALL, 0, 2)
        runGetItemsSingleCombinationContract(StorableType.ALL, 0, 0)
        runGetItemsSingleCombinationContract(StorableType.ALL, 2, 2)
    }

    protected suspend fun runGetItemsSingleCombinationContract(
        type: StorableType,
        fileCount: Int,
        folderCount: Int,
    ) {
        val fixture = createFolderWithItemsFixtureAsync(fileCount, folderCount)
        try {
            val folder = fixture.folder

            if (type == StorableType.NONE) {
                assertFailsWith<IllegalArgumentException> {
                    folder.getItemsAsync(type).toList()
                }
                return
            }

            val items = folder.getItemsAsync(type).toList()
            val returnedFileCount = items.count { it is ChildFile }
            val returnedFolderCount = items.count { it is ChildFolder }
            val otherReturnedItemCount = items.size - returnedFileCount - returnedFolderCount

            when (type) {
                StorableType.FILE -> {
                    assertEquals(fileCount, returnedFileCount)
                    assertEquals(0, returnedFolderCount)
                }

                StorableType.FOLDER -> {
                    assertEquals(folderCount, returnedFolderCount)
                    assertEquals(0, returnedFileCount)
                }

                StorableType.ALL -> {
                    assertEquals(fileCount, returnedFileCount)
                    assertEquals(folderCount, returnedFolderCount)
                }

                StorableType.NONE -> error("NONE is handled above")
            }

            assertEquals(0, otherReturnedItemCount)
        } finally {
            fixture.cleanup()
        }
    }
}


