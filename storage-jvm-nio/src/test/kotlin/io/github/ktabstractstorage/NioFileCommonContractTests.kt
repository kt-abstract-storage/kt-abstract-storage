package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.nio.NioFolder
import io.github.ktabstractstorage.testing.JvmCommonFileTests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.util.Comparator

private suspend fun createTempDirectoryOnIo(prefix: String): java.nio.file.Path = withContext(Dispatchers.IO) {
    Files.createTempDirectory(prefix)
}

private suspend fun deleteRecursivelyIfExistsOnIo(rootPath: java.nio.file.Path) = withContext(Dispatchers.IO) {
    if (Files.exists(rootPath)) {
        Files.walk(rootPath)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.deleteIfExists(it) }
    }
}

class NioFileCommonContractTests : JvmCommonFileTests() {

    override suspend fun createFileFixtureAsync(initialContent: ByteArray): FileFixture {
        val rootPath = createTempDirectoryOnIo("kt-abstract-storage-nio-file")
        val root = NioFolder(rootPath)
        val file = root.createFileAsync("sample.bin")

        if (initialContent.isNotEmpty()) {
            file.openStreamAsync(FileAccessMode.WRITE).use { stream ->
                stream.write(initialContent, 0, initialContent.size)
                stream.flush()
            }
        }

        return FileFixture(file) {
            deleteRecursivelyIfExistsOnIo(rootPath)
        }
    }
}

