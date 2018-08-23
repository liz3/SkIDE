package com.skide.core.code

import com.skide.CoreManager
import com.skide.gui.ListViewPopUp
import com.skide.gui.Menus
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
import javafx.scene.control.MenuItem
import javafx.scene.web.*;


class CallbackHook(val rdy: () -> Unit) {
    fun call() {
        rdy()
    }
}

class EventHandler(val area: CodeArea) {

    fun eventNotify(name: String, ev: Any) {}

    fun gotoCall(model: Any, position: Any, token: Any): Any {
        return area.createObjectFromMap(hashMapOf(
                Pair("startLineNumber", 75),
                Pair("endLineNumber", 75),
                Pair("startColumn", 5),
                Pair("endColumn", 38)))
    }

    fun contextMenuEmit(ev: JSObject) {
        if (((ev.getMember("event") as JSObject).getMember("leftButton") as Boolean) &&
                !((ev.getMember("event") as JSObject).getMember("rightButton") as Boolean)) return

        println("Executing event")
        val selection = area.getSelection()

        if (selection.startColumn == selection.endColumn && selection.startLineNumber == selection.endLineNumber && area.editorActions.containsKey("skunityReport")) {
            println("true")
            area.removeAction("skunityReport");
        } else if (area.coreManager.skUnity.loggedIn && !area.editorActions.containsKey("skunityReport")) {
            area.addSkUnityReportAction()
        }
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


    fun addSkUnityReportAction() {
        addAction("skunityReport", "Ask on skUnity") {
            val selection = getSelection()
            val content = getContentRange(selection.startLineNumber, selection.endLineNumber, selection.startColumn, selection.endColumn)
            coreManager.skUnity.initer(content)
        }
    }

    private fun prepareEditorActions() {
        if (coreManager.skUnity.loggedIn) {
            addSkUnityReportAction()
        } else {
            coreManager.skUnity.addListener {
                addSkUnityReportAction()
            }
        }
        addAction("compile", "Export/Compile") {
            val openProject = openFileHolder.openProject
            val map = HashMap<String, () -> Unit>()
            for ((name, opt) in openProject.project.fileManager.compileOptions) {

                map[name] = {
                    openProject.guiHandler.openFiles.forEach { it.value.saveCode() }
                    openProject.compiler.compile(openProject.project, opt,
                            openProject.guiHandler.lowerTabPaneEventManager.setupBuildLogTabForInput())
                }

            }

            ListViewPopUp("Compile/Export", map) {}
        }
        addAction("run", "Run this File") {
            val map = HashMap<String, () -> Unit>()
            coreManager.serverManager.servers.forEach {

                map[it.value.configuration.name] = {

                    openFileHolder.openProject.run(it.value, openFileHolder)

                }
            }

            ListViewPopUp("Run this file", map) {}
        }
        addAction("upload", "Upload this file") {
            val map = HashMap<String, () -> Unit>()

            openFileHolder.openProject.project.fileManager.hosts.forEach {
                map[it.name] = {
                    openFileHolder.openProject.deployer.deploy(text, openFileHolder.f.name, it)

                }
            }
            ListViewPopUp("Upload this file", map) {}
        }
        addAction("runc", "Run Configuration") {
            val map = HashMap<String, () -> Unit>()

            for ((name, opt) in openFileHolder.openProject.project.fileManager.compileOptions) {
                map[name] = {
                    val map2 = HashMap<String, () -> Unit>()
                    coreManager.serverManager.servers.forEach {

                        map2[it.value.configuration.name] = {
                            openFileHolder.openProject.guiHandler.openFiles.forEach { code -> code.value.saveCode() }
                            openFileHolder.openProject.run(it.value, opt)
                        }


                    }
                    ListViewPopUp(name, map2) {}
                }
            }
            ListViewPopUp("Run Configuration", map) {}
        }
        addAction("test", "ReplaceTest") {
            replaceContentInRange(getCurrentLine(), 1, getCurrentLine(), getColumnLineAmount(getCurrentLine()), "teeeeesssst")
        }
    }

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
                    val win = getWindow()
                    val cbHook = CallbackHook {
                        startEditor(engine.executeScript("getDefaultOptions();") as JSObject)
                        selection = engine.executeScript("selection") as JSObject
                        prepareEditorActions()
                        rdy(this)
                    }
                    win.setMember("skide", eventHandler)
                    win.setMember("cbh", cbHook)
                    engine.executeScript("cbhReady();")
                }
            }
            val url = this.javaClass.getResource("/www/index.html")
            engine.load(url.toString())
        }
    }

    private fun getArray() = engine.executeScript("getArr();") as JSObject
    private fun getObject() = engine.executeScript("getObj();") as JSObject
    private fun getFunction() = engine.executeScript("getFunc();") as JSObject
    private fun getWindow() = engine.executeScript("window") as JSObject
    private fun getModel() = engine.executeScript("editor.getModel()") as JSObject
    private fun startEditor(options: Any) {
        editor = getWindow().call("startEditor", options) as JSObject
    }

    fun createObjectFromMap(fields: Map<String, Any>): JSObject {
        val obj = getObject()
        for ((key, value) in fields) obj.setMember(key, value)
        return obj
    }

    fun addAction(id: String, label: String, cb: () -> Unit) {
        if (editorActions.containsKey(id)) return
        val action = EditorActionBinder(id, cb)
        action.instance = getWindow().call("addAction", id, label)
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

    data class Selection(val endColumn: Int, val endLineNumber: Int, val positionColumn: Int, val positionLineNumber: Int, val selectionStartColumn: Int, val selectionStartLineNumber: Int, val startColumn: Int, val startLineNumber: Int)

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

    fun getLineCount() = getModel().call("getLineCount") as Int
    fun getColumnLineAmount(line: Int) = getModel().call("getLineMaxColumn", line) as Int
    fun getWordAtPosition(line: Int = getCurrentLine(), column: Int = getCurentColumn()): String {
        return (getModel().call("getWordAtPosition", createObjectFromMap(hashMapOf(Pair("lineNumber", line), Pair("column", column)))) as JSObject).getMember("word") as String
    }

    fun setPosition(line: Int, column: Int) = editor.call("setPosition", createObjectFromMap(hashMapOf(Pair("column", column), Pair("lineNumber", line))))

    fun updateOptions(fields: Map<String, Any>) = editor.call("updateOptions", createObjectFromMap(fields))

    fun getCurrentLine() = engine.executeScript("editor.getPosition().lineNumber") as Int

    fun getCurentColumn() = engine.executeScript("editor.getPosition().column") as Int

    fun setCursorPosition(line: Int, column: Int) = editor.call("setPosition", createObjectFromMap(hashMapOf(Pair("lineNumber", line), Pair("column", column))))

    fun getContentRange(startLine: Int, endLine: Int, startColumn: Int, endColumn: Int): String {
        return getModel().call("getValueInRange", createObjectFromMap(hashMapOf(Pair("endColumn", endColumn), Pair("endLineNumber", endLine),
                Pair("startColumn", startColumn), Pair("startLineNumber", startLine)))) as String
    }

    fun replaceContentInRange(startLine: Int, startCol: Int, endLine: Int, endColumn: Int, replace: String) {
        val selections = editor.call("getSelections")
        val replaceObject = createObjectFromMap(hashMapOf(Pair("range",
                createObjectFromMap(hashMapOf(Pair("startLineNumber", startLine), Pair("startColumn", startCol), Pair("endLineNumber", endLine), Pair("endColumn", endColumn)))),
                Pair("text", replace)))
        val arr = getArray()
        arr.setSlot(0, replaceObject)
        getModel().call("pushEditOperations", selections, arr, getFunction())
    }

    fun replaceLine(number: Int, text: String) {
        replaceContentInRange(number, 1, number, getColumnLineAmount(number), text)
    }

    fun getLineContent(line: Int) = getModel().call("getLineContent", line) as String

    var text: String
        set(value) {
            editor.call("setValue", value)
        }
        get() {
            return editor.call("getValue") as String
        }
}