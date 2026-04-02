package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import io.github.ktabstractstorage.memory.MemoryFolder
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals

class RecursiveFolderTraversalTests {
    private suspend fun assertTraversalOrder(folder: Folder, expectedNames: List<String>) {
        val names = folder.getItemsAsync(StorableType.ALL).toList().map { it.name }
        assertContentEquals(expectedNames, names)
    }

    private suspend fun buildBreadthSampleTree(): MemoryFolder {
        val root = MemoryFolder("root")

        root.createFileAsync("root-file")
        val a = root.createFolderAsync("A") as ModifiableFolder
        val b = root.createFolderAsync("B") as ModifiableFolder

        a.createFileAsync("A1")
        val aa = a.createFolderAsync("AA") as ModifiableFolder
        a.createFolderAsync("AB")

        aa.createFileAsync("AA1")
        b.createFileAsync("B1")

        return root
    }

    private suspend fun buildDepthSampleTree(): MemoryFolder {
        val root = MemoryFolder("root")

        root.createFileAsync("root-file")
        val a = root.createFolderAsync("A") as ModifiableFolder

        a.createFileAsync("A1")
        val aa = a.createFolderAsync("AA") as ModifiableFolder
        aa.createFileAsync("AA1")
        a.createFolderAsync("AB")

        val b = root.createFolderAsync("B") as ModifiableFolder
        b.createFileAsync("B1")

        return root
    }

    @Test
    fun breadthFirst_enumerates_all_in_level_order() = runTest {
        val root = buildBreadthSampleTree()
        val bfs = BreadthFirstRecursiveFolder(root)

        assertTraversalOrder(
            bfs,
            listOf("root-file", "A", "B", "A1", "AA", "AB", "B1", "AA1"),
        )
    }

    @Test
    fun breadthFirst_respects_max_depth() = runTest {
        val root = buildBreadthSampleTree()
        val bfs = BreadthFirstRecursiveFolder(root, maxDepth = 1)

        assertTraversalOrder(bfs, listOf("root-file", "A", "B"))
    }

    @Test
    fun depthFirst_enumerates_all_in_depth_order() = runTest {
        val root = buildDepthSampleTree()
        val dfs = DepthFirstRecursiveFolder(root)

        assertTraversalOrder(
            dfs,
            listOf("root-file", "A", "A1", "AA", "AA1", "AB", "B", "B1"),
        )
    }

    @Test
    fun depthFirst_respects_max_depth() = runTest {
        val root = buildDepthSampleTree()
        val dfs = DepthFirstRecursiveFolder(root, maxDepth = 2)

        assertTraversalOrder(
            dfs,
            listOf("root-file", "A", "A1", "AA", "AB", "B", "B1"),
        )
    }
}


