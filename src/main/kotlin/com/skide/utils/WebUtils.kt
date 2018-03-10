package com.skide.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.nio.channels.ReadableByteChannel


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
