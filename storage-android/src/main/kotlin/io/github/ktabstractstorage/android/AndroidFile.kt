package io.github.ktabstractstorage.android

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.ktabstractstorage.ChildFile
import io.github.ktabstractstorage.Folder
import io.github.ktabstractstorage.enums.FileAccessMode
import io.github.ktabstractstorage.extensions.interfaces.GetRoot
import io.github.ktabstractstorage.streams.FileStream
import io.github.ktabstractstorage.streams.UnifiedStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.file.Path

/**
 * A [ChildFile] implementation backed by a [java.io.File], suitable for use on Android.
 *
 * A secondary constructor accepting [java.nio.file.Path] is available for API 26+ callers — it
 * converts the path to a [File] internally so all logic stays `java.io`-based and compatible
 * with API 21+.
 *
 * @param file The file represented by this instance.
 * @throws IllegalArgumentException if [file] does not exist or is not a regular file.
 */
class AndroidFile internal constructor(
    internal val file: File,
    private val skipValidation: Boolean = false,
) : GetRoot, ChildFile {

    /**
     * Constructs an [AndroidFile] from a [java.nio.file.Path].
     *
     * The path is converted to a [java.io.File] so all internal operations remain
     * compatible with API 21+. This constructor itself requires API 26 because
     * [java.nio.file] was not available on Android before that.
     *
     * @param path The NIO path representing the file.
     * @throws IllegalArgumentException if the path does not exist or is not a regular file.
     */
    @RequiresApi(Build.VERSION_CODES.O)
    constructor(path: Path) : this(path.toFile())

    init {
        if (!skipValidation) {
            require(file.exists()) { "File path does not exist: ${file.absolutePath}" }
            require(file.isFile) { "Path is not a regular file: ${file.absolutePath}" }
        }
    }

    override val id: String = file.canonicalPath

    override val name: String = file.name

    /**
     * Returns this file's path as a [java.nio.file.Path].
     *
     * Requires API 26+ because [java.nio.file] is not available on earlier Android versions.
     */
    @get:RequiresApi(Build.VERSION_CODES.O)
    val nioPath: Path
        get() = file.toPath()

    override suspend fun getParentAsync(): Folder? =
        withContext(Dispatchers.IO) { file.parentFile?.let { AndroidFolder.createUnvalidated(it) } }

    override suspend fun openStreamAsync(accessMode: FileAccessMode): UnifiedStream =
        withContext(Dispatchers.IO) { FileStream(file, accessMode) }

    override suspend fun getRootAsync(): Folder? = withContext(Dispatchers.IO) {
        var current: File = file.canonicalFile
        while (current.parentFile != null) {
            current = current.parentFile!!
        }
        AndroidFolder.createUnvalidated(current)
    }

    internal companion object {
        fun createUnvalidated(file: File) = AndroidFile(file, skipValidation = true)

        @RequiresApi(Build.VERSION_CODES.O)
        fun createUnvalidated(path: Path) = AndroidFile(path.toFile(), skipValidation = true)
    }
}

