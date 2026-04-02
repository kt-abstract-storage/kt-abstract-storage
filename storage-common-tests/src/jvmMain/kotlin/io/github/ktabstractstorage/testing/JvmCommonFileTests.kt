package io.github.ktabstractstorage.testing

import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

/**
 * JVM test wrappers for [CommonFileTests] contracts.
 */
abstract class JvmCommonFileTests : CommonFileTests() {
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

