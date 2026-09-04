package com.localsync.android.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class FileItem(
    val name: String = "",
    val path: String = "",
    @Json(name = "isDirectory")
    val isDirectory: Boolean = false,
    val size: Long = 0L,
    val lastModified: Long = 0L,
    val mimeType: String? = null,
    val extension: String? = null
) {
    val isMedia: Boolean
        get() = mimeType?.startsWith("video/") == true ||
                mimeType?.startsWith("audio/") == true ||
                mimeType?.startsWith("image/") == true ||
                extension?.lowercase() in listOf("mp4", "mkv", "avi", "mov", "mp3", "flac", "jpg", "png", "jpeg")

    val formattedSize: String
        get() {
            if (isDirectory) return "Folder"
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            val value = size / Math.pow(1024.0, digitGroups.toDouble())
            return "%.1f %s".format(value, units[digitGroups])
        }
}
