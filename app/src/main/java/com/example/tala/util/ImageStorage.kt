package com.example.tala.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object ImageStorage {
    fun getAppImagesDir(context: Context): File =
        File(context.filesDir, "images").apply { if (!exists()) mkdirs() }

    fun copyUriToInternal(context: Context, uri: Uri): File? = try {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "jpg"
        val outFile = File(getAppImagesDir(context), "img_${System.currentTimeMillis()}.$extension")
        resolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output -> input.copyTo(output) }
        }
        outFile
    } catch (_: Exception) { null }

    fun copyFileToInternal(context: Context, sourceFile: File, suggestedExtension: String? = null): File? = try {
        val extension = suggestedExtension ?: sourceFile.extension.ifBlank { "jpg" }
        val destFile = File(getAppImagesDir(context), "img_${System.currentTimeMillis()}.$extension")
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(destFile).use { output -> input.copyTo(output) }
        }
        destFile
    } catch (_: Exception) { null }

    fun getImageDimensions(context: Context, uri: Uri): Pair<Int, Int>? = try {
        val inputStream = when (uri.scheme?.lowercase()) {
            "file" -> FileInputStream(File(uri.path ?: return null))
            else -> context.contentResolver.openInputStream(uri)
        } ?: return null
        inputStream.use { stream ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, options)
            val width = options.outWidth
            val height = options.outHeight
            if (width > 0 && height > 0) Pair(width, height) else null
        }
    } catch (_: Exception) { null }

    fun shouldCrop(context: Context, uri: Uri, maxSide: Int = 2048): Boolean {
        val dims = getImageDimensions(context, uri) ?: return false
        val max = kotlin.math.max(dims.first, dims.second)
        return max > maxSide
    }
}


