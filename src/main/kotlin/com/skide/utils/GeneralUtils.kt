package com.skide.utils

import com.skide.gui.GUIManager
import sun.font.LayoutPathImpl.getPath
import java.awt.im.InputContext
import java.util.ArrayList
import java.io.File



fun adjustVersion(value: String): String {

    var str = value.replace("-dev", ".")
    var fails = 0
    while (true) {
        try {
            Integer.parseInt(str.replace(".", ""))
            break
        } catch (e: Exception) {
            str = str.substring(0, str.length - 1)
            fails++
        }
    }
    if (str.length == 3) str += ".0"
    return if (fails == 0) str else "$str$fails"
}

fun restart() {
    val javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java"
    val currentJar = File(GUIManager::class.java.protectionDomain.codeSource.location.toURI())

    if (!currentJar.name.endsWith(".jar"))
        return
    val command = ArrayList<String>()
    command.add(javaBin)
    command.add("-jar")
    command.add(currentJar.path)

    val builder = ProcessBuilder(command)
  Thread{
      builder.start()
  }.start()
    System.exit(0)
}
fun getLocale(): String {

    val context = InputContext.getInstance()
   return context.locale.toString()
}
