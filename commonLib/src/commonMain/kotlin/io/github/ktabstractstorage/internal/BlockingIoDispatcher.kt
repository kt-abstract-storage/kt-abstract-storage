package io.github.ktabstractstorage.internal

import kotlinx.coroutines.CoroutineDispatcher

/**
 * A dispatcher suitable for offloading blocking I/O work.
 *
 * On platforms that provide a dedicated I/O thread pool (JVM/Android), this
 * maps to that pool. On other platforms it falls back to the default dispatcher.
 */
internal expect val blockingIoDispatcher: CoroutineDispatcher
