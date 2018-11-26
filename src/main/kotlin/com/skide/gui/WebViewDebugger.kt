package com.skide.gui

import com.skide.core.code.CodeArea
import com.skide.utils.readFile
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.input.KeyCode
import javafx.scene.layout.BorderPane
import javafx.scene.layout.VBox
import javafx.stage.FileChooser
import javafx.stage.Stage
import netscape.javascript.JSObject
import java.io.File
import java.lang.Exception
import java.util.*

class ConsoleProxy(val debugger: WebViewDebugger) {

    fun log(param: Any) {
        debugger.serializeAndPrint(param)
    }

    fun warn(param: Any) {
        debugger.serializeAndPrint(param)
    }

    fun error(param: Any) {
        debugger.serializeAndPrint(param)

    }

    fun prePrint(x: Int) {
        if (x == 0) debugger.prePrint(DebugLevel.LOG)
        if (x == 1) debugger.prePrint(DebugLevel.WARN)
        if (x == 2) debugger.prePrint(DebugLevel.ERROR)
    }
}

enum class DebugLevel {
    LOG,
    WARN,
    ERROR,
    EXEC
}

class WebViewDebugger(val area: CodeArea) {

    val engine = area.engine
    var initialized = false
    lateinit var stage: Stage
    lateinit var inputBox: TextArea
    lateinit var outputArea: TextArea
    lateinit var consoleProxy: ConsoleProxy

    private fun isArray(obj: JSObject): Boolean {
        val window = area.engine.executeScript("window.Array") as JSObject
        return window.call("isArray", obj) as Boolean
    }

    private fun getKeys(obj: JSObject): Vector<String> {
        val list = Vector<String>()
        val window = area.engine.executeScript("window.Object") as JSObject
        val result = window.call("keys", obj)

        if (result is JSObject) {
            val length = result.getMember("length") as Int
            for (x in 0 until length) {
                list.add(result.getSlot(x) as String)

            }
        }
        return list
    }

    fun serializeAndPrint(obj: Any?) {
        if (obj == null) outputArea.appendText("undefined\n")
        if (obj is JSObject) {
            var outString = ""
            if (isArray(obj)) {
                outString += "Array: \n"
                val len = obj.getMember("length") as Int
                for (x in 0 until len) {
                    val member = obj.getSlot(x)
                    outString += if (member is JSObject) {
                        "\t$x: ${if (isArray(member)) "[]" else "{}"}\n"
                    } else {
                        "\t$x: $member\n"
                    }
                }
            } else {
                outString += "Object: \n"
                val keys = getKeys(obj)
                if (keys.size == 0) {
                    outString += "\tFunction: $obj\n"
                } else {
                    keys.forEach {
                        val member = obj.getMember(it)
                        outString += if (member is JSObject) {
                            "\t$it: ${if (isArray(member)) "[]" else "{}"}\n"
                        } else {
                            "\t$it: $member\n"
                        }
                    }
                }

            }
            outputArea.appendText("$outString\n")
        } else {
            outputArea.appendText("$obj\n")
        }
    }

    private fun getFile(name: String): File? {

        val fileChooserWindow = Stage()
        val dirChooser = FileChooser()
        dirChooser.title = name
        return dirChooser.showOpenDialog(fileChooserWindow)
    }

    fun getBox(): VBox {

        val b = VBox()

        val loadFile = Button("Load File")
        val clearBtn = Button("Clear output")
        clearBtn.setOnAction {
            outputArea.clear()
        }
        loadFile.setOnAction {
            val f = getFile("Choose Skript to load")

            if (f != null) {
                val text = readFile(f).second
                Platform.runLater {
                    try {
                        val result = engine.executeScript(text)
                        prePrint(DebugLevel.EXEC)
                        serializeAndPrint(result)
                    } catch (e: Exception) {
                        prePrint(DebugLevel.EXEC)
                        serializeAndPrint("ERROR WHILE EXECUTING: $text\n${e.message}")
                    }
                }
            }
        }
        b.children.addAll(clearBtn, loadFile)
        return b
    }

    fun prePrint(level: DebugLevel) {
        outputArea.appendText("[$level] ")
    }

    private fun setupStage() {

        inputBox = TextArea()
        outputArea = TextArea()

        outputArea.isEditable = false
        inputBox.prefHeight = 40.0

        val pane = BorderPane()
        pane.top = getBox()
        pane.center = outputArea
        pane.bottom = inputBox


        val stage = Stage()
        stage.title = "Debugger"
        stage.scene = Scene(pane, 800.0, 600.0)

        inputBox.setOnKeyPressed {
            if (it.code == KeyCode.ENTER) {
                if (!it.isShiftDown) {
                    val text = inputBox.text
                    Platform.runLater {
                        try {
                            val result = engine.executeScript(text)
                            prePrint(DebugLevel.EXEC)
                            serializeAndPrint(result)
                        } catch (e: Exception) {
                            prePrint(DebugLevel.EXEC)
                            serializeAndPrint("ERROR WHILE EXECUTING: $text\n${e.message}")
                        }
                    }
                    inputBox.clear()
                }
            }
        }
        this.stage = stage
    }

    fun start() {
        if (initialized) {
            stage.show()
            return
        }
        initialized = true
        Thread {

            Platform.runLater {
                setupStage()
                consoleProxy = ConsoleProxy(this)
                val window = engine.executeScript("window") as JSObject
                window.setMember("xlogger", consoleProxy)
                stage.show()
            }

        }.start()
    }
}