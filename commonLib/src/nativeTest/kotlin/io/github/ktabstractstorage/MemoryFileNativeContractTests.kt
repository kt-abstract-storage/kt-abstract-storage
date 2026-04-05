package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.memory.MemoryFolder
import io.github.ktabstractstorage.testing.NativeCommonFileTests

class MemoryFileNativeContractTests : NativeCommonFileTests() {

    override suspend fun createFileFixtureAsync(initialContent: ByteArray): FileFixture {
        val root = MemoryFolder("root")
        val file = root.createFileAsync("sample.bin")

        if (initialContent.isNotEmpty()) {
            file.openStreamAsync(FileAccessMode.WRITE).use { stream ->
                stream.write(initialContent, 0, initialContent.size)
                stream.flush()
            }
        }

        return FileFixture(file)
    }
}
