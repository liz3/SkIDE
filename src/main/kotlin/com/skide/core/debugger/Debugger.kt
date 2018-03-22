package com.skide.core.debugger

import com.skide.gui.GuiManager
import com.skide.gui.controllers.ErrorReportGuiController
import javafx.application.Platform
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.PrintStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*


class Debugger {

    init {
        System.setOut(SystemOut())
        System.setErr(SystemErr())
    }

}

class SystemErr : PrintStream(System.err) {

    private var err = ""
    private var recording = false

    override fun println(raw: String) {
        var msg = raw
        val cal = Calendar.getInstance();
        val sdf = SimpleDateFormat("d.M.Y HH:mm:ss");
        if (!msg.startsWith("["))
            msg = " $msg"

        err += msg + "\n"
        if (!recording) {

            recording = true

            Thread {

                try {
                    Thread.sleep(750)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }


                Platform.runLater {
                    val win = GuiManager.getWindow("ErrorReport.fxml", "Error", false)
                    val ctrl = win.controller as ErrorReportGuiController

                    val cal = Calendar.getInstance()
                    val sdf = SimpleDateFormat("d.M.Y HH:mm:ss")

                    val theError = "OS: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}\nJava: ${System.getProperty("java.runtime.version")}\nTime: ${sdf.format(cal.time)}\n$err"

                    ctrl.contentArea.text = theError

                    ctrl.discordLink.setOnAction {
                        Desktop.getDesktop().browse(URI("https://discord.gg/Ud2WdVU"))
                    }
                    ctrl.githubLink.setOnAction {
                        Desktop.getDesktop().browse(URI("https://github.com/Sk-IDE/SkIDE/issues"))
                    }
                    ctrl.copyToClipBoard.setOnAction {
                        val stringSelection = StringSelection(theError)
                        val clpbrd = Toolkit.getDefaultToolkit().systemClipboard
                        clpbrd.setContents(stringSelection, null)
                    }

                    win.stage.isResizable = false
                    win.stage.show()
                    recording = false
                    err = ""
                }

            }.start()

        }






         super.println("[" + sdf.format(cal.time) + " | ERROR]" + msg)
    }

    override fun println(x: Any?) {
        println(x.toString())
    }

}

class SystemOut : PrintStream(System.out) {


    override fun println(msg: String) {
        var msg = msg
        val cal = Calendar.getInstance();
        val sdf = SimpleDateFormat("d.M.Y HH:mm:ss");
        if (!msg.startsWith("["))
            msg = " $msg"
       // area.appendText("$msg\n")
        super.println("[" + sdf.format(cal.time) + " | MSG]" + msg)
    }
    override fun println(msg: Any?) {
       println(msg.toString())
    }

}