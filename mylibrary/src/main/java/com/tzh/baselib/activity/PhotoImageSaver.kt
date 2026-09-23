package com.tzh.baselib.activity

import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/** Writes original bytes without pretending that every image is JPEG. */
internal object PhotoImageSaver {
    fun save(context: Context, source: File) {
        checkActive()
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, options)
        val mime = options.outMimeType ?: error("Unrecognized image")
        require(mime.startsWith("image/") && options.outWidth > 0 && options.outHeight > 0)
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)
            ?: error("Unsupported image type")
        val name = "${UUID.randomUUID()}.$extension"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, mime)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/biubiu")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Cannot create media entry")
            try {
                source.inputStream().use { input ->
                    (resolver.openOutputStream(uri) ?: error("Cannot open media entry")).use { output ->
                        copy(input, output)
                    }
                }
                checkActive()
                val published = resolver.update(uri, ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }, null, null)
                check(published > 0) { "Cannot publish media entry" }
            } catch (error: Exception) {
                try { resolver.delete(uri, null, null) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
                throw error
            }
        } else {
            @Suppress("DEPRECATION")
            val folder = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "biubiu")
            check(folder.isDirectory || folder.mkdirs()) { "Cannot create picture directory" }
            val destination = File(folder, name)
            try {
                source.inputStream().use { input -> destination.outputStream().use { copy(input, it) } }
                checkActive()
                MediaScannerConnection.scanFile(context, arrayOf(destination.absolutePath), arrayOf(mime), null)
            } catch (error: Exception) {
                destination.delete()
                throw error
            }
        }
    }

    private fun copy(input: InputStream, output: OutputStream) {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            checkActive()
            val size = input.read(buffer)
            if (size < 0) break
            output.write(buffer, 0, size)
        }
    }

    private fun checkActive() {
        if (Thread.currentThread().isInterrupted) throw InterruptedException("Image save cancelled")
    }
}
