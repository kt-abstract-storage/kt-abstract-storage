package io.github.ktabstractstorage.testing

import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Native test wrappers for [CommonModifiableFolderTests] contracts.
 */
abstract class NativeCommonModifiableFolderTests : CommonModifiableFolderTests() {
    @Test
    fun folder_constructor_valid_parameters() = runTest {
        runConstructorCallValidParametersContract()
    }

    @Test
    fun folder_has_valid_name() = runTest {
        runHasValidNameContract()
    }

    @Test
    fun folder_has_valid_id() = runTest {
        runHasValidIdContract()
    }

    @Test
    fun get_items_filter_none_throws_on_empty_folder() = runTest {
        runGetItemsSingleCombinationContract(StorableType.NONE, fileCount = 0, folderCount = 0)
    }

    @Test
    fun get_items_filter_none_throws_on_mixed_items() = runTest {
        runGetItemsSingleCombinationContract(StorableType.NONE, fileCount = 2, folderCount = 2)
    }

    @Test
    fun get_items_filter_file_on_2_files_0_folders() = runTest {
        runGetItemsSingleCombinationContract(StorableType.FILE, fileCount = 2, folderCount = 0)
    }

    @Test
    fun get_items_filter_file_on_0_files_2_folders() = runTest {
        runGetItemsSingleCombinationContract(StorableType.FILE, fileCount = 0, folderCount = 2)
    }

    @Test
    fun get_items_filter_file_on_empty_folder() = runTest {
        runGetItemsSingleCombinationContract(StorableType.FILE, fileCount = 0, folderCount = 0)
    }

    @Test
    fun get_items_filter_folder_on_2_files_0_folders() = runTest {
        runGetItemsSingleCombinationContract(StorableType.FOLDER, fileCount = 2, folderCount = 0)
    }

    @Test
    fun get_items_filter_folder_on_0_files_2_folders() = runTest {
        runGetItemsSingleCombinationContract(StorableType.FOLDER, fileCount = 0, folderCount = 2)
    }

    @Test
    fun get_items_filter_folder_on_empty_folder() = runTest {
        runGetItemsSingleCombinationContract(StorableType.FOLDER, fileCount = 0, folderCount = 0)
    }

    @Test
    fun get_items_filter_all_on_2_files_0_folders() = runTest {
        runGetItemsSingleCombinationContract(StorableType.ALL, fileCount = 2, folderCount = 0)
    }

    @Test
    fun get_items_filter_all_on_0_files_2_folders() = runTest {
        runGetItemsSingleCombinationContract(StorableType.ALL, fileCount = 0, folderCount = 2)
    }

    @Test
    fun get_items_filter_all_on_empty_folder() = runTest {
        runGetItemsSingleCombinationContract(StorableType.ALL, fileCount = 0, folderCount = 0)
    }

    @Test
    fun get_items_filter_all_on_2_files_2_folders() = runTest {
        runGetItemsSingleCombinationContract(StorableType.ALL, fileCount = 2, folderCount = 2)
    }

    @Test
    fun create_list_delete_roundtrip() = runTest {
        runCreateListDeleteRoundtripContract()
    }

    @Test
    fun delete() = runTest {
        runDeleteAsyncContract()
    }

    @Test
    fun create_new_folder_name_not_exists() = runTest {
        runCreateNewFolderNameNotExistsContract()
    }

    @Test
    fun create_new_folder_name_exists_no_overwrite() = runTest {
        runCreateNewFolderNameExistsNoOverwriteContract()
    }

    @Test
    fun create_new_folder_name_exists_overwrite() = runTest {
        runCreateNewFolderNameExistsOverwriteContract()
    }

    @Test
    fun create_new_file_name_not_exists() = runTest {
        runCreateNewFileNameNotExistsContract()
    }

    @Test
    fun create_new_file_name_exists_no_overwrite() = runTest {
        runCreateNewFileNameExistsNoOverwriteContract()
    }

    @Test
    fun create_new_file_name_exists_overwrite() = runTest {
        runCreateNewFileNameExistsOverwriteContract()
    }

    @Test
    fun create_copy_of() = runTest {
        runCreateCopyOfContract()
    }

    @Test
    fun move_from() = runTest {
        runMoveFromContract()
    }
}
