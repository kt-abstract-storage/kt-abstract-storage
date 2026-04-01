package io.github.ktabstractstorage

import io.github.ktabstractstorage.enums.StorableType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch

/**
 * Enables building a folder from an arbitrary list of source folders.
 *
 * Items from all source folders are enumerated concurrently.
 */
class AggregateFolder(
    override val id: String,
    override val name: String,
    val sources: Collection<Folder> = emptyList(),
) : Folder {
    override fun getItemsAsync(type: StorableType): Flow<StorableChild> {
        require(type != StorableType.NONE) {
            "StorableType.NONE is invalid when enumerating folder contents."
        }

        val snapshot = sources.toList()
        if (snapshot.isEmpty()) {
            return emptyFlow()
        }

        return channelFlow {
            for (folder in snapshot) {
                launch {
                    folder.getItemsAsync(type).collect { send(it) }
                }
            }
        }
    }
}


