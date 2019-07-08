package com.skide.core.management

import com.skide.CoreManager
import com.skide.gui.GUIManager
import com.skide.utils.readFile
import com.skide.utils.writeFile
import javafx.application.Platform
import org.json.JSONObject
import java.io.File
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket

class SocketManager(val core: CoreManager) {

    lateinit var socket: ServerSocket
    private var running = false
    fun start() {

        val lockfile = File(System.getProperty("user.home"), ".skide_lockfile")
        var start = 45664

        while (true) {
            if (start == 45974) break
            try {
                socket = ServerSocket(start, 50, InetAddress.getByName("127.0.0.1"))

                break
            } catch (e: Exception) {
                start++
            }
        }

        running = true
        writeFile("$start".toByteArray(), lockfile, false, true)
        lockfile.deleteOnExit()
        val th = Thread {
            while (running) {
                val sk = socket.accept()
                handle(sk)
            }
        }
        th.name = "SK-IDE Socket thread"
        th.start()

    }

    private fun handle(c: Socket) {
        c.getOutputStream().write("SK-IDE".toByteArray())
        c.getOutputStream().flush()
        Thread {
            Thread.sleep(250)
            try {
                val message = JSONObject(String(c.getInputStream().readBytes()))
                val action = message.getString("action")

                if (action == "open_file") {

                    val lnk = message.getString("lnk")
                    Platform.runLater {
                        if (core.projectManager.openProjects.size != 0) {
                            core.projectManager.openProjects.first().eventManager.openFile(File(lnk), true)
                        }
                    }
                    c.close()
                }
                if (action == "focus") {
                    Platform.runLater {
                        if (core.projectManager.openProjects.size != 0)
                            core.projectManager.openProjects.first().guiHandler.window.stage.requestFocus()
                        else
                            GUIManager.activeGuis[1]?.stage!!.requestFocus()
                    }
                }
            } catch (e: Exception) {
                c.close()
            }


        }.start()
    }

    fun stop() {

        running = false
        socket.close()
    }
}

fun handle(path: String) {
    val lockfile = File(File(CoreManager::class.java.protectionDomain.codeSource.location.toURI()).parent, "lockfile")
    if (lockfile.exists()) {

        val result = readFile(lockfile).second

        val port = result.toInt()

        try {
            val buff = ByteArray(6)
            val socket = Socket("127.0.0.1", port)

            socket.getInputStream().read(buff)
            if (String(buff) == "SK-IDE") {
                val f = File(path)

                val obj = JSONObject()
                if (path != "" && f.exists()) {
                    obj.put("action", "open_file")
                    obj.put("lnk", f.absolutePath)
                } else {
                    obj.put("action", "focus")
                }
                socket.getOutputStream().write(obj.toString().toByteArray())
                socket.getOutputStream().flush()
                socket.close()
                System.exit(0)
            }
        } catch (e: Exception) {
            lockfile.delete()
        }
    }


}
