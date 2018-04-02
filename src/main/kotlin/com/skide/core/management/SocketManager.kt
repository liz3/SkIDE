package com.skide.core.management

import com.skide.CoreManager
import javafx.application.Platform
import org.json.JSONObject
import java.io.File
import java.net.ServerSocket
import java.net.Socket
import kotlin.math.ln

class SocketManager(val core: CoreManager) {

    lateinit var socket: ServerSocket
    private var running = false
    fun start() {

        var start = 45664

        while (true) {
            if (start == 45674) break
            try {
                socket = ServerSocket(start)

                break
            } catch (e: Exception) {
                start++
            }
        }
        running = true

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
                if(action == "focus") {
                    Platform.runLater {
                        if (core.projectManager.openProjects.size != 0)core.projectManager.openProjects.first().guiHandler.window.stage.requestFocus()
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


         var start = 45664

        while (true) {
            if (start == 45674) break
            try {

                var buff = ByteArray(6)
                val socket = Socket("127.0.0.1", start)

                socket.getInputStream().read(buff)
                if(String(buff) == "SK-IDE") {
                    val f = File(path)

                    val obj = JSONObject()
                    if(f.exists() && path != "") {
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
                start++
            }


    }
}