package com.skide.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.net.ssl.HttpsURLConnection


fun downloadFile(target: String, path: String) {
    val url = URL(target);
    val bis = BufferedInputStream(url.openStream())
    val fis = FileOutputStream(File(path))
    val buffer = ByteArray(1024)
    var count: Int
    while (true) {
        count = bis.read(buffer, 0, 1024)
        if (count == -1) break
        fis.write(buffer, 0, count)
    }
    fis.close()
    bis.close()
}

fun encodeHTTPParams(params: Map<String, String>): String {
    return params.map {
        URLEncoder.encode(it.key, "UTF-8") + "=" + URLEncoder.encode(it.value, "UTF-8")
    }.joinToString("&")
}

fun request(path: String, method: String = "GET", headers: Map<String, String> = HashMap(), body: String = ""): Triple<Int, MutableMap<String, MutableList<String>>, InputStream> {
    val connection = {
        val url = URL(path)
        if (path.startsWith("https://")) {
            url.openConnection() as HttpsURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }
    }.invoke()

    connection.requestMethod = method
    connection.instanceFollowRedirects = true
    headers.forEach {
        connection.addRequestProperty(it.key, it.value)
    }
    connection.doInput = true
    connection.doOutput = true
    if (method == "POST") {
        connection.outputStream.write(body.toByteArray())
        connection.outputStream.flush()
    }
    connection.connect()
    return Triple(connection.responseCode, connection.headerFields, connection.inputStream)
}