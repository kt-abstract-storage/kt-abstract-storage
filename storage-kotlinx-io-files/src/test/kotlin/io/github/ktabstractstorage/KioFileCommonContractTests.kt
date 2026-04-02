package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.kiofiles.KioFolder
import io.github.ktabstractstorage.testing.JvmCommonFileTests
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.files.Path
import java.nio.file.Files
import java.util.Comparator

private suspend fun createTempFileTestDirectory(prefix: String): java.nio.file.Path =
    withContext(Dispatchers.IO) { Files.createTempDirectory(prefix) }

private suspend fun deleteFileTestDirectoryRecursively(rootPath: java.nio.file.Path) =
    withContext(Dispatchers.IO) {
        if (Files.exists(rootPath)) {
            Files.walk(rootPath)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
        }
    }

class KioFileCommonContractTests : JvmCommonFileTests() {

    override suspend fun createFileFixtureAsync(initialContent: ByteArray): FileFixture {
        val rootPath = createTempFileTestDirectory("kt-abstract-storage-kio-file")
        val root = KioFolder(Path(rootPath.toString()))
        val file = root.createFileAsync("sample.bin")

        if (initialContent.isNotEmpty()) {
            file.openStreamAsync(FileAccessMode.WRITE).use { stream ->
                stream.write(initialContent, 0, initialContent.size)
                stream.flush()
            }
        }

        return FileFixture(file) {
            deleteFileTestDirectoryRecursively(rootPath)
        }
    }

}

