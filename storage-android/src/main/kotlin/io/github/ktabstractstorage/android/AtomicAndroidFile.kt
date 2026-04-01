package io.github.ktabstractstorage.android

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.streams.UnifiedStream
import java.io.File
import java.nio.file.Path

/**
 * Android file variant that uses [AtomicStream] (AndroidX AtomicFile-backed)
 * for writable modes.
 */
class AtomicAndroidFile internal constructor(
    file: File,
    skipValidation: Boolean = false,
    cachedNioPath: Any? = null,
) : AndroidFile(file, skipValidation, cachedNioPath) {

    @RequiresApi(Build.VERSION_CODES.O)
    constructor(path: Path) : this(path.toFile(), cachedNioPath = path)

    override fun createStream(accessMode: FileAccessMode): UnifiedStream =
        when (accessMode) {
            FileAccessMode.READ -> super.createStream(accessMode)
            FileAccessMode.WRITE,
            FileAccessMode.READ_AND_WRITE -> AtomicStream(file, accessMode)
        }

    internal companion object {
        fun createUnvalidated(file: File) = AtomicAndroidFile(file, skipValidation = true)

        @RequiresApi(Build.VERSION_CODES.O)
        fun createUnvalidated(path: Path) = AtomicAndroidFile(path.toFile(), skipValidation = true, cachedNioPath = path)
    }
}

