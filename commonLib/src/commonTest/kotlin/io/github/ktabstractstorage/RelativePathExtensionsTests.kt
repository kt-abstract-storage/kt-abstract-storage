package io.github.ktabstractstorage

import io.github.ktabstractstorage.extensions.getItemByRelativePathAsync
import io.github.ktabstractstorage.extensions.getItemsAlongRelativePathAsync
import io.github.ktabstractstorage.extensions.getItemsAlongRelativePathToAsync
import io.github.ktabstractstorage.extensions.getRelativePathToAsync
import io.github.ktabstractstorage.memory.MemoryFolder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RelativePathExtensionsTests {
    private val samplePaths = listOf("/", "/a/", "/b/", "/c", "/a/a/", "/a/c", "/b/a/c")

    private suspend fun buildTree(): MemoryFolder {
        val root = MemoryFolder("root")
        val a = root.createFolderAsync("a") as ModifiableFolder
        val b = root.createFolderAsync("b") as ModifiableFolder
        root.createFileAsync("c")

        val aa = a.createFolderAsync("a") as ModifiableFolder
        val ab = a.createFolderAsync("b") as ModifiableFolder
        a.createFileAsync("c")
        ab.createFileAsync("c")

        aa.createFolderAsync("a")
        aa.createFolderAsync("b")
        aa.createFileAsync("c")

        val ba = b.createFolderAsync("a") as ModifiableFolder
        val bb = b.createFolderAsync("b") as ModifiableFolder
        b.createFileAsync("c")

        ba.createFolderAsync("a")
        ba.createFolderAsync("b")
        ba.createFileAsync("c")

        bb.createFolderAsync("a")
        bb.createFolderAsync("b")
        bb.createFileAsync("c")

        return root
    }

    @Test
    fun traverse_and_regenerate_relative_path() = runTest {
        val root = buildTree()

        for (path in samplePaths) {
            val item = root.getItemByRelativePathAsync(path)
            if (path == "/") {
                assertEquals(root, item)
            } else {
                assertTrue(path.contains(item.name), "Expected '$path' to include '${item.name}'")
            }

            if (item is StorableChild) {
                val regenerated = root.getRelativePathToAsync(item)
                assertEquals(path, regenerated)
            }
        }
    }

    @Test
    fun get_items_along_relative_path_yields_expected_sequence() = runTest {
        val root = buildTree()

        val yielded = root.getItemsAlongRelativePathAsync("/a/a/c").toList().map { it.name }
        assertEquals(listOf("a", "a", "c"), yielded)
    }

    @Test
    fun get_items_along_relative_path_to_yields_chain() = runTest {
        val root = buildTree()
        val target = root.getItemByRelativePathAsync("/b/a/c") as StorableChild

        val yielded = root.getItemsAlongRelativePathToAsync(target).toList().map { it.name }
        assertEquals(listOf("b", "a", "c"), yielded)
    }

    @Test
    fun supports_parent_traversal() = runTest {
        val root = buildTree()
        val start = root.getItemByRelativePathAsync("/a/a/")

        val yielded = start.getItemsAlongRelativePathAsync("../c").toList().map { it.name }
        assertEquals(listOf("a", "c"), yielded)
    }
}

