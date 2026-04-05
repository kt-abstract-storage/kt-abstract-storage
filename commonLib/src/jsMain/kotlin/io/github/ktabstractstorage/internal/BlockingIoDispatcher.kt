package io.github.ktabstractstorage.internal

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

internal actual val blockingIoDispatcher: CoroutineDispatcher
    get() = Dispatchers.Default
