package com.skide.core.debugger

import com.skide.CoreManager
import com.skide.Info
import com.skide.gui.GUIManager
import com.skide.gui.controllers.ErrorReportGUIController
import com.skide.utils.readFile
import javafx.application.Platform
import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.PrintStream
import java.net.URI
import java.text.SimpleDateFormat
import java.util.*

class Debugger{
    val sysout = SystemOut()
    
    val syserr = SystemErr()
    
    init{
        System.setOut(sysout)

        System.setErr(syserr)
    }
}

class SystemErr : PrintStream(System.err){
    private var err = ""

    lateinit var core:CoreManager

    private var recording = false

    override fun println(raw: String){
        var msg = raw

        val cal = Calendar.getInstance()

        val sdf = SimpleDateFormat("d.M.Y HH:mm:ss")

        if (!msg.startsWith("[")) {
            msg = " $msg"
        }

        err += msg + "\n"

        if (!recording){
            recording = true

            Thread{
                try{
                    Thread.sleep(750)
                }catch (e: InterruptedException){
                    e.printStackTrace()
                }

                val trace = err

                Platform.runLater{
                    val win = GUIManager.getWindow("ErrorReport.fxml", "Error", false)

                    val ctrl = win.controller as ErrorReportGUIController

                    var theError = "OS: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}\nJava: ${System.getProperty("java.runtime.version")}\nSK-IDE version: ${Info.version}\nTime: ${sdf.format(cal.time)}\n$trace"

                    ctrl.detailsCheck.selectedProperty().addListener{ _, _, x ->
                        val selected = x

                        if(selected && this::core.isInitialized){
                            theError = "OS: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}\nJava: ${System.getProperty("java.runtime.version")}\nSK-IDE version: ${Info.version}\nTime: ${sdf.format(cal.time)}\n$trace\n\n"

                            theError += "Active Windows:\n"

                            core.guiManager.activeGuis.forEach{
                                theError += "   Name: ${it.value.stage.title}\n    Controller: ${it.value.controller}\n"
                            }

                            theError += "\n\nProjects open:\n"

                            core.projectManager.openProjects.forEach{
                                theError += "   Name: ${it.project.name}\n"

                                theError += "   Skript Version: ${it.project.skriptVersion}\n"

                                theError += "   Folder: ${it.project.folder}\n"

                                theError += "   Addons:\n"

                                it.addons.forEach{
                                    theError += "       Name: ${it.key}\n"
                                }

                                theError += try{
                                    "   Config: ${readFile(File(it.project.folder, ".project.skide")).second}\n"
                                }catch (e:Exception){
                                    "   Config: Failed to read: ${e.message}\n"
                                }

                                theError += "   Metadata of open files:\n"

                                it.guiHandler.openFiles.forEach{
                                    theError += "       Name: ${it.key}\n"
                                    theError += "       Caret-pos: ${it.value.area.caretPosition}\n"
                                }
                            }

                            theError += "\n\nServers:\n"

                            core.serverManager.servers.forEach{
                                theError += "   Name: ${it.value.configuration.name}\n"

                                theError += "   Skript Version: ${it.value.configuration.skriptVersion}\n"

                                theError += "   Folder: ${it.value.configuration.folder}\n"

                                theError += try{
                                    "   Config: ${readFile(it.value.confFile).second}\n"
                                }catch (e:Exception){
                                    "   Config: Failed to read: ${e.message}\n"
                                }
                            }

                            theError += "\n\nHolders projects:\n"

                            core.configManager.projects.forEach{
                                theError += "   Name: ${it.value.name}\n"

                                theError += "   Path: ${it.value.path}\n"
                            }

                            theError += "\n\nHolders servers:\n"

                            core.configManager.servers.forEach{
                                theError += "   Name: ${it.value.name}\n"

                                theError += "   Path: ${it.value.path}\n"
                            }
                        }else{
                            theError = "OS: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}\nJava: ${System.getProperty("java.runtime.version")}\nSK-IDE version: ${Info.version}\nTime: ${sdf.format(cal.time)}\n$trace"
                        }

                        ctrl.contentArea.text = theError
                    }

                    ctrl.contentArea.text = theError

                    ctrl.discordLink.setOnAction{
                        Desktop.getDesktop().browse(URI("https://discord.gg/Ud2WdVU"))
                    }

                    ctrl.githubLink.setOnAction{
                        Desktop.getDesktop().browse(URI("https://github.com/Sk-IDE/SkIDE/issues"))
                    }

                    ctrl.copyToClipBoard.setOnAction{
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

    override fun println(x: Any?){
        println(x.toString())
    }
}

class SystemOut : PrintStream(System.out){
    override fun println(orig: String){
        var msg = orig

        val cal = Calendar.getInstance()

        val sdf = SimpleDateFormat("d.M.Y HH:mm:ss")

        if (!msg.startsWith("[")) {
            msg = " $msg"
        }

        //area.appendText("$msg\n")

        super.println("[" + sdf.format(cal.time) + " | MSG]" + msg)
    }

    override fun println(msg: Any?){
        println(msg.toString())
    }
}