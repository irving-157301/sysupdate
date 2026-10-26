package com.s.sync

import android.content.Context
import java.net.HttpURLConnection
import java.net.URL

object Uploader {

    fun send(c: Context, id: Long, name: String, mime: String, bytes: ByteArray): Boolean {
        if (post(c, "sendPhoto", "photo", id, name, mime, bytes) == 200) return true
        return post(c, "sendDocument", "document", id, name, mime, bytes) == 200
    }

    private fun post(c: Context, m: String, field: String, id: Long,
                     name: String, mime: String, b: ByteArray): Int {
        val bd = "----v" + System.nanoTime()
        val k = URL("https://api.telegram.org/bot${Prefs.BOT}/$m")
            .openConnection() as HttpURLConnection
        k.requestMethod = "POST"
        k.doOutput = true
        k.connectTimeout = 25000
        k.readTimeout = 90000
        k.setRequestProperty("Content-Type", "multipart/form-data; boundary=$bd")
        k.setRequestProperty("Connection", "close")

        k.outputStream.use { o ->
            o.write(("--$bd\r\nContent-Disposition: form-data; name=\"chat_id\"\r\n\r\n${Prefs.CHAT}\r\n").toByteArray())
            o.write(("--$bd\r\nContent-Disposition: form-data; name=\"caption\"\r\n\r\n#$id\r\n").toByteArray())
            o.write(("--$bd\r\nContent-Disposition: form-data; name=\"$field\"; filename=\"$name\"\r\nContent-Type: $mime\r\n\r\n").toByteArray())
            o.write(b)
            o.write("\r\n--$bd--\r\n".toByteArray())
        }

        val code = k.responseCode
        val body = runCatching {
            (if (code in 200..299) k.inputStream else k.errorStream)?.use { it.readBytes().decodeToString() }
        }.getOrNull() ?: ""

        if (code == 429) {
            val wait = Regex("\"retry_after\":(\\d+)").find(body)?.groupValues?.get(1)?.toIntOrNull() ?: 5
            Thread.sleep(wait * 1000L + 500)
        }
        runCatching { k.disconnect() }
        return code
    }
}