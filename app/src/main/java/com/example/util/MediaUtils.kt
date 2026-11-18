package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Base64
import androidx.core.content.FileProvider
import com.google.firebase.Firebase
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.storage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.io.ByteArrayOutputStream
import java.io.File

object MediaUtils {
    private const val MAX_INLINE_BYTES = 420_000

    fun createTempCameraUri(context: Context): Uri {
        val file = File(context.cacheDir, "rx_photo_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }

    fun compressBitmapToBase64(bitmap: Bitmap, maxDimension: Int = 1024): String {
        val ratio = minOf(maxDimension.toFloat() / bitmap.width, maxDimension.toFloat() / bitmap.height, 1f)
        val w = (bitmap.width * ratio).toInt().coerceAtLeast(1)
        val h = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        val scaled = if (w != bitmap.width || h != bitmap.height) {
            Bitmap.createScaledBitmap(bitmap, w, h, true)
        } else bitmap

        var quality = 78
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 12
        } while (bytes.size > MAX_INLINE_BYTES && quality >= 24)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    fun uriToBitmap(context: Context, uri: Uri): Bitmap? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun base64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (_: Exception) {
            null
        }
    }

    fun readSmallDocumentAsBase64(context: Context, uri: Uri): Triple<String, String, Long>? {
        return try {
            val name = queryFileName(context, uri) ?: "document_${System.currentTimeMillis()}"
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
            val size = bytes.size.toLong()
            val encoded = if (bytes.size <= MAX_INLINE_BYTES) {
                Base64.encodeToString(bytes, Base64.NO_WRAP)
            } else {
                ""
            }
            Triple(name, encoded, size)
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadChatMediaToStorage(
        context: Context,
        uri: Uri,
        conversationId: String,
        prefix: String,
        mimeType: String
    ): Result<Pair<String, String>> {
        return try {
            val rawName = queryFileName(context, uri) ?: "${prefix}_${System.currentTimeMillis()}"
            val safeName = rawName.replace(Regex("[^a-zA-Z0-9._-]"), "_").take(80)
            val ref = Firebase.storage.reference
                .child("chat_media/$conversationId/${prefix}_${System.currentTimeMillis()}_$safeName")
            val metadata = StorageMetadata.Builder()
                .setContentType(mimeType.ifBlank { "application/octet-stream" })
                .build()
            ref.putFile(uri, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Result.success(rawName to downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun openAttachment(
        context: Context,
        base64: String,
        mediaUrl: String,
        fileName: String,
        mimeType: String
    ): Boolean {
        return try {
            if (base64.isNotBlank()) {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                val safeName = fileName.ifBlank { "attachment_${System.currentTimeMillis()}" }
                    .replace(Regex("[^a-zA-Z0-9._-]"), "_")
                val target = File(context.cacheDir, "${System.currentTimeMillis()}_$safeName")
                target.writeBytes(bytes)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", target)
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mimeType.ifBlank { "*/*" })
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Open $safeName"))
                true
            } else if (mediaUrl.isNotBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(mediaUrl))
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    fun queryFileName(context: Context, uri: Uri): String? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIdx >= 0) {
                return cursor.getString(nameIdx)
            }
        }
        return uri.lastPathSegment
    }

    fun createVoiceRecorder(context: Context): Pair<MediaRecorder, File> {
        val dir = File(context.filesDir, "audio").apply { mkdirs() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.m4a")
        val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        recorder.setAudioEncodingBitRate(32000)
        recorder.setAudioSamplingRate(16000)
        recorder.setOutputFile(file.absolutePath)
        recorder.prepare()
        recorder.start()
        return recorder to file
    }

    fun encodeAudioFileBase64(file: File): String {
        return try {
            val bytes = file.readBytes()
            if (bytes.size <= MAX_INLINE_BYTES) Base64.encodeToString(bytes, Base64.NO_WRAP) else ""
        } catch (_: Exception) {
            ""
        }
    }

    fun playBase64Audio(context: Context, base64: String, onDone: () -> Unit): MediaPlayer? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val tmp = File(context.cacheDir, "play_${System.currentTimeMillis()}.m4a")
            tmp.writeBytes(bytes)
            MediaPlayer().apply {
                setDataSource(tmp.absolutePath)
                setOnCompletionListener {
                    it.release()
                    tmp.delete()
                    onDone()
                }
                prepare()
                start()
            }
        } catch (_: Exception) {
            onDone()
            null
        }
    }
}
