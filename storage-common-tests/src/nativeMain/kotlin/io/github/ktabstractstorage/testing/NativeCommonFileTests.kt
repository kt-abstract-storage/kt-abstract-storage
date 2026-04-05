package io.github.ktabstractstorage.testing

import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * Native test wrappers for [CommonFileTests] contracts.
 */
abstract class NativeCommonFileTests : CommonFileTests() {
    @Test
    fun file_constructor_valid_parameters() = runTest {
        runConstructorCallValidParametersContract()
    }

    @Test
    fun file_id_not_blank() = runTest {
        runIdNotBlankContract()
    }

    @Test
    fun file_open_read() = runTest {
        runOpenReadContract()
    }

    @Test
    fun file_open_write() = runTest {
        runOpenWriteContract()
    }

    @Test
    fun file_open_read_write() = runTest {
        runOpenReadWriteContract()
    }
}
