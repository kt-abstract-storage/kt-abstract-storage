package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.yield
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Enables building a folder from an arbitrary list of files.
 */
class ReadOnlyCompositeFolder(
    override val id: String,
    override val name: String,
    val sources: Collection<File> = mutableListOf(),
) : Folder {
    override fun getItemsAsync(type: StorableType): Flow<StorableChild> = flow {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }

        yield()

        if (type == StorableType.FOLDER) {
            return@flow
        }

        for (file in sources.toList()) {
            emit(
                ParentOverrideChildFile(
                    inner = file,
                    parent = this@ReadOnlyCompositeFolder,
                ),
            )
        }
    }
}

