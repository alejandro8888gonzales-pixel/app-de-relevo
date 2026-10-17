package com.example.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileStorageHelper {

    /**
     * Copies a content Uri (image, video, etc) into the app's secure, persistent local files space.
     * This avoids content Uri expiration, permission loss, or file deletion/damage by external apps.
     */
    fun copyUriToLocal(context: Context, uri: Uri, folderName: String, compress: Boolean = false): String? {
        return try {
            val contentResolver = context.contentResolver
            val mimeType = contentResolver.getType(uri) ?: ""
            
            // Deduce file extension with robust fallbacks
            var extension = android.webkit.MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: ""
            if (extension.isEmpty()) {
                val lastSegment = uri.lastPathSegment ?: ""
                extension = when {
                    mimeType.startsWith("image/") -> "jpg"
                    mimeType.startsWith("video/") -> "mp4"
                    mimeType == "application/pdf" -> "pdf"
                    uri.path?.endsWith(".docx", true) == true || lastSegment.endsWith(".docx", true) -> "docx"
                    uri.path?.endsWith(".doc", true) == true || lastSegment.endsWith(".doc", true) -> "doc"
                    uri.path?.endsWith(".xlsx", true) == true || lastSegment.endsWith(".xlsx", true) -> "xlsx"
                    uri.path?.endsWith(".xls", true) == true || lastSegment.endsWith(".xls", true) -> "xls"
                    else -> {
                        if (lastSegment.contains(".")) {
                            lastSegment.substringAfterLast(".")
                        } else {
                            "bin"
                        }
                    }
                }
            }

            val storageDir = File(context.filesDir, folderName).apply {
                if (!exists()) mkdirs()
            }

            // High-efficiency lossless GZIP compression if setting is enabled and is a document
            val isDoc = mimeType.contains("pdf") || mimeType.contains("word") || mimeType.contains("document") ||
                         extension == "pdf" || extension == "docx" || extension == "doc" || extension == "txt" || extension == "xlsx"
            
            val shouldCompress = compress && isDoc
            val destFile = if (shouldCompress) {
                File(storageDir, "evidence_${System.currentTimeMillis()}.$extension.gz")
            } else {
                File(storageDir, "evidence_${System.currentTimeMillis()}.$extension")
            }
            
            contentResolver.openInputStream(uri)?.use { inputStream ->
                if (shouldCompress) {
                    java.util.zip.GZIPOutputStream(FileOutputStream(destFile)).use { gzip ->
                        inputStream.copyTo(gzip)
                    }
                } else {
                    FileOutputStream(destFile).use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }
            }
            destFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lossless decompression on-the-fly for GZIP compressed files
     */
    fun decompressIfNeeded(context: Context, filePath: String): File {
        val originalFile = File(filePath)
        if (!originalFile.name.endsWith(".gz") || !originalFile.exists()) {
            return originalFile
        }
        
        return try {
            val cacheDir = File(context.cacheDir, "decompressed_docs").apply {
                if (!exists()) mkdirs()
            }
            // Strip the .gz
            val baseName = originalFile.name.substringBeforeLast(".gz")
            val tempFile = File(cacheDir, baseName)
            
            java.util.zip.GZIPInputStream(originalFile.inputStream()).use { gzip ->
                FileOutputStream(tempFile).use { output ->
                    gzip.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            originalFile
        }
    }

    /**
     * Prepares a file for modern secure external sharing (e.g. WhatsApp, Outlook, Adobe).
     * Automatically decompresses if GZIP is detected, and copies the file to the
     * externalCacheDir if possible (or cacheDir) to avoid strict platform permission blockages.
     */
    fun getShareableFile(context: Context, filePath: String): File? {
        return try {
            val originalFile = File(filePath)
            if (!originalFile.exists()) return null

            val decFile = if (originalFile.name.endsWith(".gz")) {
                decompressIfNeeded(context, filePath)
            } else {
                originalFile
            }

            if (!decFile.exists()) return null

            // Destination directory in externalCacheDir first (perfect for open-to-share permissions), fallback to cacheDir
            val shareDir = File(context.externalCacheDir ?: context.cacheDir, "shared_documents").apply {
                if (!exists()) mkdirs()
            }

            // Construct exact clean user-friendly filename
            val targetName = decFile.name
            val finalSharedFile = File(shareDir, targetName)

            // Copy file to shareable directory with overwrite capability
            decFile.inputStream().use { input ->
                FileOutputStream(finalSharedFile).use { output ->
                    input.copyTo(output)
                }
            }
            finalSharedFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Copies a file from Sandboxed internal storage to the public Downloads folder of the device.
     */
    fun downloadFileToPublicFolder(context: Context, filePath: String, fileName: String): Boolean {
        return try {
            val originalFile = File(filePath)
            if (!originalFile.exists()) return false

            val finalFile = if (originalFile.name.endsWith(".gz")) {
                decompressIfNeeded(context, filePath)
            } else {
                originalFile
            }

            if (!finalFile.exists()) return false

            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                val ext = fileName.substringAfterLast(".", "").lowercase()
                val mime = when (ext) {
                    "pdf" -> "application/pdf"
                    "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    "doc" -> "application/msword"
                    "txt" -> "text/plain"
                    "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                    "xls" -> "application/vnd.ms-excel"
                    else -> "*/*"
                }
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mime)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
            }

            val downloadsUri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                val legacyFile = File(downloadsDir, fileName)
                finalFile.inputStream().use { input ->
                    FileOutputStream(legacyFile).use { output ->
                        input.copyTo(output)
                    }
                }
                android.media.MediaScannerConnection.scanFile(context, arrayOf(legacyFile.absolutePath), null, null)
                return true
            }

            val targetUri = resolver.insert(downloadsUri, contentValues) ?: return false
            resolver.openOutputStream(targetUri)?.use { output ->
                finalFile.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Prepares a temporary file in the app cache directory and returns a FileProvider Uri.
     * This is used as a destination for the CAMERA Capture intent.
     */
    fun getTempImageUriForCamera(context: Context): UriAndFile? {
        return try {
            val cacheDirectory = File(context.cacheDir, "camera_pics").apply {
                if (!exists()) mkdirs()
            }
            val tempFile = File.createTempFile("cam_${System.currentTimeMillis()}", ".jpg", cacheDirectory)
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, tempFile)
            UriAndFile(uri, tempFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

data class UriAndFile(
    val uri: Uri,
    val file: File
)
