package io.github.ktabstractstorage.android

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// ──────────────────────────────────────────────────────────────────────────────
// Internal storage (always available, no permissions required)
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Returns an [AndroidFolder] for the app's private internal files directory
 * (`/data/data/<package>/files`).
 *
 * This directory is always available and does not require any permissions. Its
 * contents are removed when the app is uninstalled.
 *
 * @see Context.getFilesDir
 */
suspend fun Context.getFilesDirFolderAsync(): AndroidFolder =
    withContext(Dispatchers.IO) { AndroidFolder.createUnvalidated(filesDir) }

/**
 * Returns an [AndroidFolder] for the app's private internal cache directory
 * (`/data/data/<package>/cache`).
 *
 * The system may delete files in this directory when storage is low. Its
 * contents are removed when the app is uninstalled.
 *
 * @see Context.getCacheDir
 */
suspend fun Context.getCacheDirFolderAsync(): AndroidFolder =
    withContext(Dispatchers.IO) { AndroidFolder.createUnvalidated(cacheDir) }

/**
 * Returns an [AndroidFolder] for the app's private code-cache directory
 * (`/data/data/<package>/code_cache`).
 *
 * Intended for storing compiled or cached code artifacts. The system may delete
 * this directory on an OS update.
 *
 * @see Context.getCodeCacheDir
 */
suspend fun Context.getCodeCacheDirFolderAsync(): AndroidFolder =
    withContext(Dispatchers.IO) { AndroidFolder.createUnvalidated(codeCacheDir) }

/**
 * Returns an [AndroidFolder] for the app's no-backup files directory
 * (`/data/data/<package>/no_backup`).
 *
 * Files placed here are excluded from Android's auto-backup mechanism. Useful
 * for caches or session tokens that should not be restored to a different device.
 *
 * @see Context.getNoBackupFilesDir
 */
suspend fun Context.getNoBackupFilesDirFolderAsync(): AndroidFolder =
    withContext(Dispatchers.IO) { AndroidFolder.createUnvalidated(noBackupFilesDir) }

/**
 * Returns an [AndroidFolder] for the app's data directory (`/data/data/<package>`).
 *
 * This is the parent of `filesDir`, `cacheDir`, and related directories.
 * Requires API level 24 (Android 7.0 Nougat) or higher.
 *
 * @see Context.getDataDir
 */
@RequiresApi(Build.VERSION_CODES.N)
suspend fun Context.getDataDirFolderAsync(): AndroidFolder =
    withContext(Dispatchers.IO) { AndroidFolder.createUnvalidated(dataDir) }

// ──────────────────────────────────────────────────────────────────────────────
// External app-specific storage
// No READ_EXTERNAL_STORAGE / WRITE_EXTERNAL_STORAGE permissions are required
// for these directories (API 19+). All return null when external storage is
// not currently mounted or otherwise unavailable.
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Returns an [AndroidFolder] for the root of the app's external private storage,
 * or `null` if external storage is not available.
 *
 * Equivalent to `getExternalFilesDir(null)`.
 *
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalFilesDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(null)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Music directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_MUSIC
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalMusicDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Pictures directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_PICTURES
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalPicturesDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_PICTURES)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Movies directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_MOVIES
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalMoviesDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Downloads directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_DOWNLOADS
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalDownloadsDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Documents directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_DOCUMENTS
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalDocumentsDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Ringtones directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_RINGTONES
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalRingtonesDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_RINGTONES)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Alarms directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_ALARMS
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalAlarmsDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_ALARMS)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Notifications directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_NOTIFICATIONS
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalNotificationsDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_NOTIFICATIONS)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Podcasts directory,
 * or `null` if external storage is not available.
 *
 * @see Environment.DIRECTORY_PODCASTS
 * @see Context.getExternalFilesDir
 */
suspend fun Context.getExternalPodcastsDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_PODCASTS)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Audiobooks directory,
 * or `null` if external storage is not available.
 *
 * Requires API level 29 (Android 10 Q) or higher.
 *
 * @see Environment.DIRECTORY_AUDIOBOOKS
 * @see Context.getExternalFilesDir
 */
@RequiresApi(Build.VERSION_CODES.Q)
suspend fun Context.getExternalAudiobooksDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_AUDIOBOOKS)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external Screenshots directory,
 * or `null` if external storage is not available.
 *
 * Requires API level 29 (Android 10 Q) or higher.
 *
 * @see Environment.DIRECTORY_SCREENSHOTS
 * @see Context.getExternalFilesDir
 */
@RequiresApi(Build.VERSION_CODES.Q)
suspend fun Context.getExternalScreenshotsDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        getExternalFilesDir(Environment.DIRECTORY_SCREENSHOTS)?.let { AndroidFolder.createUnvalidated(it) }
    }

/**
 * Returns an [AndroidFolder] for the app's external cache directory,
 * or `null` if external storage is not available.
 *
 * The system may delete this directory when storage is critically low.
 *
 * @see Context.getExternalCacheDir
 */
suspend fun Context.getExternalCacheDirFolderAsync(): AndroidFolder? =
    withContext(Dispatchers.IO) {
        externalCacheDir?.let { AndroidFolder.createUnvalidated(it) }
    }
