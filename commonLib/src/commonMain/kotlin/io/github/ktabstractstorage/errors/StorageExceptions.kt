package io.github.ktabstractstorage.errors

/**
 * HRESULT-style error codes used by storage exceptions.
 */
object StorageHResults {
	/** COR_E_IO */
	const val IO_EXCEPTION: Int = 0x80131620.toInt()

	/** HRESULT_FROM_WIN32(ERROR_FILE_NOT_FOUND) */
	const val FILE_NOT_FOUND: Int = 0x80070002.toInt()

	/** HRESULT_FROM_WIN32(ERROR_FILE_EXISTS) */
	const val FILE_ALREADY_EXISTS: Int = 0x80070050.toInt()
}

/**
 * Base exception for storage failures with a stable HRESULT-like error code.
 */
open class StorageException(
	message: String,
	val hResult: Int,
	cause: Throwable? = null,
) : Exception(message, cause)

/**
 * Thrown when an operation refers to a storage item that does not exist.
 */
class StorageFileNotFoundException(
	message: String,
	cause: Throwable? = null,
) : StorageException(message, StorageHResults.FILE_NOT_FOUND, cause)

/**
 * Thrown when creating an item conflicts with an existing item.
 */
class StorageFileAlreadyExistsException(
	message: String,
	cause: Throwable? = null,
) : StorageException(message, StorageHResults.FILE_ALREADY_EXISTS, cause)

/**
 * Thrown when a stream operation fails due to invalid state or I/O constraints.
 */
class StorageIOException(
	message: String,
	cause: Throwable? = null,
) : StorageException(message, StorageHResults.IO_EXCEPTION, cause)


