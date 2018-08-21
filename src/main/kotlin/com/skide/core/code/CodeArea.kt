package com.skide.core.code

import com.skide.CoreManager
import com.skide.include.OpenFileHolder
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import netscape.javascript.JSObject
import javafx.scene.Scene
import javafx.scene.layout.StackPane
import javafx.stage.Modality
import javafx.stage.StageStyle
import javafx.stage.Stage
import javafx.event.EventHandler;
import javafx.scene.control.Label
import javafx.scene.web.*;


class CallbackHook(val rdy: () -> Unit) {


    fun call() {

        rdy()
    }

}

class EventHandler(val area: CodeArea) {

    fun eventNotify(name: String, ev: Any) {


    }

    fun gotoCall(model: Any, position: Any, token: Any): Any {
        return area.createObjectFromMap(hashMapOf(
                Pair("startLineNumber", 75),
                Pair("endLineNumber", 75),
                Pair("startColumn", 5),
                Pair("endColumn", 38)))
    }

    fun actionFire(id: String, ev: Any) {

        if (area.editorActions.containsKey(id)) area.editorActions[id]!!.cb()


    }

}

class EditorActionBinder(val id: String, val cb: () -> Unit) {

    lateinit var instance: Any
}

class CodeArea(val coreManager: CoreManager, val rdy: (CodeArea) -> Unit) {

    lateinit var view: WebView
    lateinit var engine: WebEngine
    lateinit var editor: JSObject
    lateinit var selection: JSObject
    lateinit var openFileHolder: OpenFileHolder
    val editorActions = HashMap<String, EditorActionBinder>()

    val eventHandler = EventHandler(this)


    init {
        Platform.runLater {


            view = WebView()
            engine = view.engine


            engine.onAlert = EventHandler<WebEvent<String>> { event ->
                val popup = Stage()

                popup.initStyle(StageStyle.UTILITY)
                popup.initModality(Modality.WINDOW_MODAL)

                val content = StackPane()
                content.children.setAll(
                        Label(event.data)
                )
                content.setPrefSize(200.0, 100.0)

                popup.scene = Scene(content)
                popup.showAndWait()
            }


            view.engine.loadWorker.stateProperty().addListener { _, _, newValue ->


                if (newValue === Worker.State.FAILED) {
                    println("Failed to load webpage")
                }
                if (newValue === Worker.State.SUCCEEDED) {
                    try {
                        val win = view.engine.executeScript("window") as JSObject
                        val cbHook = CallbackHook {


                            startEditor(engine.executeScript("getDefaultOptions();") as JSObject)
                            selection = engine.executeScript("selection") as JSObject
                            addAction("testAction", "Test Action") {
                                println("called")
                            }
                            rdy(this)
                        }
                        win.setMember("skide", eventHandler)
                        win.setMember("cbh", cbHook)
                        engine.executeScript("cbhReady();")
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }
            }

            val url = this.javaClass.getResource("/www/index.html")
            engine.load(url.toString())


        }
    }

    fun addAction(id: String, label: String, cb: () -> Unit) {
        if (editorActions.containsKey(id)) return
        val action = EditorActionBinder(id, cb)
        action.instance = (engine.executeScript("window") as JSObject).call("addAction", id, label)
        editorActions[id] = action

    }

    fun removeAction(id: String) {
        if (!editorActions.containsKey(id)) return
        val action = editorActions[id]

        if (action != null) {
            (action.instance as JSObject).call("dispose")
            editorActions.remove(id)

        }
    }

    fun startEditor(options: Any) {
        editor = (engine.executeScript("window") as JSObject).call("startEditor", options) as JSObject

    }

    fun createObjectFromMap(fields: Map<String, Any>): JSObject {
        val obj = engine.executeScript("getObj();") as JSObject

        for ((key, value) in fields) {
            obj.setMember(key, value)
        }

        return obj
    }

    class Selection(val endColumn: Int, val endLineNumber: Int, val positionColumn: Int, val positionLineNumber: Int, val selectionStartColumn: Int, val selectionStartLineNumber: Int, val startColumn: Int, val startLineNumber: Int)

    fun getSelection(): Selection {
        val result = editor.call("getSelection") as JSObject


        return Selection(
                result.getMember("endColumn") as Int,
                result.getMember("endLineNumber") as Int,
                result.getMember("positionColumn") as Int,
                result.getMember("positionLineNumber") as Int,
                result.getMember("selectionStartColumn") as Int,
                result.getMember("selectionStartLineNumber") as Int,
                result.getMember("startColumn") as Int,
                result.getMember("startLineNumber") as Int)
    }

    fun getLineCount(): Int {
        val model = engine.executeScript("editor.getModel()") as JSObject
        return model.call("getLineCount") as Int
    }

    fun getLastColumnFromLine(line: Int): Int {
        val model = engine.executeScript("editor.getModel()") as JSObject
        return model.call("getLineMaxColumn", line) as Int
    }

    fun setPosition(line: Int, column: Int) = editor.call("setPosition", createObjectFromMap(hashMapOf(Pair("column", column), Pair("lineNumber", line))))

    fun updateOptions(fields: Map<String, Any>) = editor.call("updateOptions", createObjectFromMap(fields))

    fun getCurrentLine() = engine.executeScript("editor.getPosition().lineNumber") as Int

    fun getCurentColumn() = engine.executeScript("editor.getPosition().column") as Int

    fun setCursorPosition(line: Int, column: Int) = editor.call("setPosition", createObjectFromMap(hashMapOf(Pair("lineNumber", line), Pair("column", column))))

    fun getLineContent(line: Int): String {
        val model = engine.executeScript("editor.getModel()") as JSObject
        return model.call("getLineContent", line) as String
    }

    var text: String
        set(value) {
            editor.call("setValue", value)
        }
        get() {
            return editor.call("getValue") as String
        }
}