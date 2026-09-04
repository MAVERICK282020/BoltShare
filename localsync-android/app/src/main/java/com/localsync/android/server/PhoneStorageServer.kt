package com.localsync.android.server

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.util.Log
import android.webkit.MimeTypeMap
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.net.URLDecoder
import java.util.concurrent.Executors

/**
 * Embedded lightweight HTTP server running on Android (Port 8085).
 * Allows paired PC to directly browse phone storage, stream/preview media, and upload files over LAN.
 */
class PhoneStorageServer(private val context: Context, private val port: Int = 8085) {

    private val tag = "PhoneStorageServer"
    private var serverSocket: ServerSocket? = null
    private val threadPool = Executors.newCachedThreadPool()
    @Volatile
    private var isRunning = false
    private var wifiLock: android.net.wifi.WifiManager.WifiLock? = null
    private var wakeLock: android.os.PowerManager.WakeLock? = null

    fun start() {
        if (isRunning) return
        isRunning = true

        // Acquire WifiLock & WakeLock so phone doesn't kill network in sleep mode
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            wifiLock = wifiManager?.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "LocalSync:WifiLock")?.apply {
                setReferenceCounted(false)
                acquire()
            }
            val powerManager = context.applicationContext.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            wakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "LocalSync:WakeLock")?.apply {
                setReferenceCounted(false)
                acquire(8 * 60 * 60 * 1000L) // 8 hours max
            }
            Log.i(tag, "⚡ WifiLock and WakeLock acquired for background LAN stability")
        } catch (e: Exception) {
            Log.w(tag, "Could not acquire locks", e)
        }

        Thread {
            try {
                serverSocket = ServerSocket(port)
                Log.i(tag, "⚡ PhoneStorageServer started on port $port")
                while (isRunning) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        threadPool.execute { handleClient(client) }
                    } catch (e: Exception) {
                        if (!isRunning) break
                        Log.e(tag, "Error accepting client connection", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to start PhoneStorageServer on port $port", e)
            } finally {
                stop()
            }
        }.apply {
            isDaemon = true
            name = "PhoneStorageServerThread"
            start()
        }
    }

    fun stop() {
        isRunning = false
        try {
            wifiLock?.let { if (it.isHeld) it.release() }
            wakeLock?.let { if (it.isHeld) it.release() }
        } catch (ignored: Exception) {}
        try {
            serverSocket?.close()
        } catch (ignored: Exception) {}
        serverSocket = null
        Log.i(tag, "PhoneStorageServer stopped")
    }

    private fun handleClient(socket: Socket) {
        try {
            socket.soTimeout = 30000
            val input = socket.getInputStream()
            val output = socket.getOutputStream()

            // 1. Read HTTP header bytes up to CRLF CRLF without buffering the body
            val headerStream = ByteArrayOutputStream()
            var prev1 = -1
            var prev2 = -1
            var prev3 = -1
            var cur: Int
            while (input.read().also { cur = it } != -1) {
                headerStream.write(cur)
                if (prev3 == 13 && prev2 == 10 && prev1 == 13 && cur == 10) {
                    break // CRLF CRLF reached!
                }
                prev3 = prev2
                prev2 = prev1
                prev1 = cur
            }

            val headerString = headerStream.toString("UTF-8")
            val lines = headerString.split("\r\n")
            if (lines.isEmpty() || lines[0].isBlank()) return

            val requestLine = lines[0]
            val parts = requestLine.split(" ")
            if (parts.size < 2) return

            val method = parts[0].uppercase()
            val rawUri = parts[1]

            // Parse headers into map
            val headers = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                val colonIdx = line.indexOf(':')
                if (colonIdx > 0) {
                    val key = line.substring(0, colonIdx).trim().lowercase()
                    val value = line.substring(colonIdx + 1).trim()
                    headers[key] = value
                }
            }

            // Parse URL and query params
            val questionIdx = rawUri.indexOf('?')
            val path = if (questionIdx >= 0) rawUri.substring(0, questionIdx) else rawUri
            val query = if (questionIdx >= 0) rawUri.substring(questionIdx + 1) else ""
            val queryParams = parseQueryParams(query)

            // Handle CORS preflight
            if (method == "OPTIONS") {
                sendResponse(output, 204, "No Content", "text/plain", ByteArray(0))
                return
            }

            when (path) {
                "/ping", "/status" -> {
                    val status = JSONObject().apply {
                        put("status", "ok")
                        put("device", android.os.Build.MODEL)
                        put("port", port)
                        put("time", System.currentTimeMillis())
                    }
                    sendJsonResponse(output, 200, status.toString())
                }
                "/storage-info" -> {
                    try {
                        val stat = android.os.StatFs(Environment.getExternalStorageDirectory().path)
                        val total = stat.totalBytes
                        val free = stat.availableBytes
                        val used = total - free
                        val res = JSONObject().apply {
                            put("totalBytes", total)
                            put("usedBytes", used)
                            put("freeBytes", free)
                            put("device", android.os.Build.MODEL)
                        }
                        sendJsonResponse(output, 200, res.toString())
                    } catch (e: Exception) {
                        sendError(output, 500, e.message ?: "Failed to get storage info")
                    }
                }
                "/roots" -> {
                    val roots = getStorageRootsJson()
                    sendJsonResponse(output, 200, roots.toString())
                }
                "/files" -> {
                    val requestedPath = queryParams["path"]
                    if (requestedPath.isNullOrBlank() || requestedPath == "/") {
                        val roots = getStorageRootsJson()
                        sendJsonResponse(output, 200, roots.toString())
                    } else {
                        val files = listDirectoryJson(requestedPath)
                        sendJsonResponse(output, 200, files.toString())
                    }
                }
                "/download" -> {
                    val filePath = queryParams["path"]
                    if (filePath.isNullOrBlank()) {
                        sendError(output, 400, "Missing path parameter")
                        return
                    }
                    val inline = queryParams["inline"] == "true"
                    streamFile(output, filePath, inline)
                }
                "/upload" -> {
                    val targetPath = queryParams["path"] ?: ""
                    val fileName = queryParams["name"] ?: "uploaded_file"
                    val contentLength = headers["content-length"]?.toLongOrNull() ?: -1L
                    if (contentLength < 0) {
                        sendError(output, 400, "Missing or invalid Content-Length")
                        return
                    }
                    saveUploadedFile(input, output, targetPath, fileName, contentLength)
                }
                "/delete" -> {
                    val targetPath = queryParams["path"]
                    if (targetPath.isNullOrBlank()) {
                        sendError(output, 400, "Missing path parameter")
                        return
                    }
                    val target = File(targetPath)
                    val deleted = target.exists() && target.delete()
                    if (deleted) {
                        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), null, null)
                    }
                    val res = JSONObject().apply { put("deleted", deleted) }
                    sendJsonResponse(output, if (deleted) 200 else 400, res.toString())
                }
                else -> {
                    sendError(output, 404, "Not Found")
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Error handling client request", e)
        } finally {
            try { socket.close() } catch (ignored: Exception) {}
        }
    }

    private fun parseQueryParams(query: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        if (query.isBlank()) return result
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf('=')
            if (idx > 0) {
                val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8")
                val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                result[key] = value
            }
        }
        return result
    }

    private fun getStorageRootsJson(): JSONArray {
        val array = JSONArray()
        val baseDir = Environment.getExternalStorageDirectory()

        val rootCandidates = listOf(
            RootCandidate("📱 Internal Storage", baseDir.absolutePath),
            RootCandidate("📸 Photos & Camera", File(baseDir, "DCIM").absolutePath),
            RootCandidate("📥 Downloads", File(baseDir, "Download").absolutePath),
            RootCandidate("🖼️ Pictures", File(baseDir, "Pictures").absolutePath),
            RootCandidate("📄 Documents", File(baseDir, "Documents").absolutePath),
            RootCandidate("🎵 Music", File(baseDir, "Music").absolutePath),
            RootCandidate("🎬 Videos", File(baseDir, "Movies").absolutePath),
            RootCandidate("💬 WhatsApp Media", File(baseDir, "Android/media/com.whatsapp/WhatsApp/Media").absolutePath)
        )

        for (candidate in rootCandidates) {
            val file = File(candidate.path)
            if (file.exists() || candidate.title.startsWith("📱")) {
                val obj = JSONObject().apply {
                    put("name", candidate.title)
                    put("path", candidate.path)
                    put("isDirectory", true)
                    put("size", 0L)
                    put("lastModified", if (file.exists()) file.lastModified() else System.currentTimeMillis())
                    put("extension", "")
                    put("mimeType", "inode/directory")
                }
                array.put(obj)
            }
        }
        return array
    }

    private data class RootCandidate(val title: String, val path: String)

    private fun listDirectoryJson(dirPath: String): JSONArray {
        val array = JSONArray()
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) {
            return array
        }

        val files = dir.listFiles() ?: emptyArray()

        // Sort: Folders first, then alphabetically
        val sortedFiles = files.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

        for (f in sortedFiles) {
            try {
                val isDir = f.isDirectory
                val ext = if (!isDir && f.name.contains(".")) f.name.substringAfterLast(".").lowercase() else ""
                val mime = if (isDir) "inode/directory" else getMimeType(ext)

                val obj = JSONObject().apply {
                    put("name", f.name)
                    put("path", f.absolutePath)
                    put("isDirectory", isDir)
                    put("size", if (isDir) 0L else f.length())
                    put("lastModified", f.lastModified())
                    put("extension", ext)
                    put("mimeType", mime)
                }
                array.put(obj)
            } catch (ignored: Exception) {}
        }
        return array
    }

    private fun streamFile(output: OutputStream, filePath: String, inline: Boolean = false) {
        val file = File(filePath)
        if (!file.exists() || file.isDirectory) {
            sendError(output, 404, "File not found: $filePath")
            return
        }

        val ext = file.extension.lowercase()
        val mime = getMimeType(ext)
        val fileLength = file.length()
        val fileName = file.name.replace("\"", "")
        val disposition = if (inline) "inline" else "attachment"

        val headerBuilder = StringBuilder()
        headerBuilder.append("HTTP/1.1 200 OK\r\n")
        headerBuilder.append("Content-Type: $mime\r\n")
        headerBuilder.append("Content-Length: $fileLength\r\n")
        headerBuilder.append("Content-Disposition: $disposition; filename=\"$fileName\"\r\n")
        headerBuilder.append("Accept-Ranges: bytes\r\n")
        headerBuilder.append("Access-Control-Allow-Origin: *\r\n")
        headerBuilder.append("Connection: close\r\n\r\n")

        val headerBytes = headerBuilder.toString().toByteArray(Charsets.UTF_8)
        output.write(headerBytes)

        FileInputStream(file).use { fis ->
            val buffer = ByteArray(64 * 1024)
            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
        }
        output.flush()
    }

    private fun saveUploadedFile(
        input: InputStream,
        output: OutputStream,
        targetPath: String,
        fileName: String,
        contentLength: Long
    ) {
        var targetDir = File(targetPath)
        val baseDir = Environment.getExternalStorageDirectory()

        // Resolve friendly paths or roots to real external storage directories
        if (targetPath.isBlank() || targetPath == "/" || targetPath.equals("Download", ignoreCase = true) || targetPath.equals("Downloads", ignoreCase = true)) {
            targetDir = File(baseDir, "Download")
        } else if (targetPath.contains("DCIM") || targetPath.contains("Photos")) {
            targetDir = File(baseDir, "DCIM")
        } else if (targetPath.contains("Pictures")) {
            targetDir = File(baseDir, "Pictures")
        } else if (targetPath.contains("Documents")) {
            targetDir = File(baseDir, "Documents")
        }

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        val destFile = File(targetDir, fileName)
        FileOutputStream(destFile).use { fos ->
            val buffer = ByteArray(64 * 1024)
            var remaining = contentLength
            while (remaining > 0) {
                val toRead = Math.min(buffer.size.toLong(), remaining).toInt()
                val read = input.read(buffer, 0, toRead)
                if (read == -1) break
                fos.write(buffer, 0, read)
                remaining -= read
            }
        }

        // Notify MediaScanner so photos and files appear immediately in Gallery
        try {
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
        } catch (ignored: Exception) {}

        val res = JSONObject().apply {
            put("status", "success")
            put("name", destFile.name)
            put("path", destFile.absolutePath)
            put("size", destFile.length())
        }
        sendJsonResponse(output, 200, res.toString())
    }

    private fun getMimeType(extension: String): String {
        if (extension.isBlank()) return "application/octet-stream"
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "application/octet-stream"
    }

    private fun sendJsonResponse(output: OutputStream, statusCode: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        sendResponse(output, statusCode, if (statusCode == 200) "OK" else "Error", "application/json; charset=utf-8", bytes)
    }

    private fun sendError(output: OutputStream, statusCode: Int, message: String) {
        val json = JSONObject().apply {
            put("error", message)
            put("statusCode", statusCode)
        }.toString()
        sendJsonResponse(output, statusCode, json)
    }

    private fun sendResponse(output: OutputStream, statusCode: Int, statusText: String, contentType: String, data: ByteArray) {
        val writer = PrintWriter(OutputStreamWriter(output, Charsets.UTF_8))
        writer.print("HTTP/1.1 $statusCode $statusText\r\n")
        writer.print("Content-Type: $contentType\r\n")
        writer.print("Content-Length: ${data.size}\r\n")
        writer.print("Access-Control-Allow-Origin: *\r\n")
        writer.print("Access-Control-Allow-Methods: GET, POST, DELETE, OPTIONS\r\n")
        writer.print("Access-Control-Allow-Headers: *\r\n")
        writer.print("Connection: close\r\n\r\n")
        writer.flush()
        if (data.isNotEmpty()) {
            output.write(data)
            output.flush()
        }
    }
}
