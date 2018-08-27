package com.skide.core.code

import com.skide.CoreManager
import com.skide.core.code.autocomplete.AutoCompleteItem
import com.skide.core.code.autocomplete.CompletionType
import com.skide.gui.GUIManager
import com.skide.gui.ListViewPopUp
import com.skide.gui.controllers.GenerateCommandController
import com.skide.include.DocType
import com.skide.include.Node
import com.skide.include.OpenFileHolder
import com.skide.utils.EditorUtils
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

    fun eventNotify(name: String, ev: Any) {}

    fun cmdCall(key: String) {
        if (area.editorCommands.containsKey(key)) {
            area.editorCommands[key]!!.cb()
        }
    }

    fun gotoCall(model: Any, position: Any, token: Any): Any {
        return area.createObjectFromMap(hashMapOf(
                Pair("startLineNumber", 75),
                Pair("endLineNumber", 75),
                Pair("startColumn", 5),
                Pair("endColumn", 38)))
    }

    fun autoCompleteRequest(doc: Any, pos: Any, token: Any, context: Any): JSObject {
        val array = area.getArray()

        var count = 0
        if (area.getCurentColumn() == 1) {

            array.setSlot(count, AutoCompleteItem(area, "function", CompletionType.FUNCTION, "function () :: :", "Generates a function", "This will create a function").createObject(area.getObject()))
            count++
            array.setSlot(count, AutoCompleteItem(area, "Command", CompletionType.CONSTRUCTOR, "", "Generates a Command", "This will open a Window to create a ", commandId = "create_command").createObject(area.getObject()))
            count++

            area.openFileHolder.openProject.addons.values.forEach {
                it.forEach { ev ->
                    if (ev.type == DocType.EVENT) {

                        array.setSlot(count, AutoCompleteItem(area, "EVENT: ${ev.name} (${ev.addon.name})", CompletionType.METHOD, {
                            var text = ev.pattern
                            if (text.isEmpty()) text = ev.name
                            text = text.replace("[on]", "on").replace("\n", "") + ":"
                            if (!text.startsWith("on ")) text = "on $text"
                            text
                        }.invoke(), commandId = "general_auto_complete_finish").createObject(area.getObject()))

                        count++
                    }
                }
            }
        }

        return array
    }

    fun contextMenuEmit(ev: JSObject) {
        if (((ev.getMember("event") as JSObject).getMember("leftButton") as Boolean) &&
                !((ev.getMember("event") as JSObject).getMember("rightButton") as Boolean)) return

        println("Executing event")
        val selection = area.getSelection()

        if (selection.startColumn == selection.endColumn && selection.startLineNumber == selection.endLineNumber &&
                area.editorActions.containsKey("skunityReport")) {
            println("true")
            area.removeAction("skunityReport");
        } else if (area.coreManager.skUnity.loggedIn && !area.editorActions.containsKey("skunityReport")) {
            area.addSkUnityReportAction()
        }
    }

    fun actionFire(id: String, ev: Any) {
        if (area.editorActions.containsKey(id)) area.editorActions[id]!!.cb()
    }

    fun commandFire(id: String): JSObject {
        if (area.editorActions.containsKey(id)) area.editorActions[id]!!.cb()
        return area.getObject();
    }

}

class EditorActionBinder(val id: String, val cb: () -> Unit) {
    lateinit var instance: Any
}

class EditorCommandBinder(val id: String, val cb: () -> Unit) {
    lateinit var instance: JSObject

    fun activate() {
        instance.call("set", true)
    }

    fun deactivate() {
        instance.call("set", false)
    }

}

class CodeArea(val coreManager: CoreManager, val rdy: (CodeArea) -> Unit) {

    lateinit var view: WebView
    lateinit var engine: WebEngine
    lateinit var editor: JSObject
    lateinit var selection: JSObject
    lateinit var openFileHolder: OpenFileHolder
    val editorActions = HashMap<String, EditorActionBinder>()
    val editorCommands = HashMap<String, EditorCommandBinder>()

    val eventHandler = EventHandler(this)


    fun addSkUnityReportAction() {
        addAction("skunityReport", "Ask on skUnity") {
            val selection = getSelection()
            val content = getContentRange(selection.startLineNumber, selection.endLineNumber,
                    selection.startColumn, selection.endColumn)
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
            replaceContentInRange(getCurrentLine(), 1, getCurrentLine(), getColumnLineAmount(getCurrentLine()),
                    "teeeeesssst")
        }

        addAction("general_auto_complete_finish") {
            openFileHolder.codeManager.sequenceReplaceHandler.compute(getCurrentLine(), getLineContent(getCurrentLine()))
        }
        addAction("create_command") {

            val window = GUIManager.getWindow("fxml/GenerateCommand.fxml", "Generate command", true)
            val generate = window.controller as GenerateCommandController

            generate.cancelButton.setOnAction {
                GUIManager.closeGui(window.id)
            }
            generate.createButton.setOnAction {
                val line = getCurrentLine()
                replaceContentInRange(line, 1, line, getColumnLineAmount(line), "command /" + generate.commandNameField.text + ":\n\tdescription: " + generate.descriptionField.text + "\n" + "\tpermission: " + generate.permissionField.text + "\n\ttrigger:\n\t\tsend \"hi\" to player")
                GUIManager.closeGui(window.id)
            }

        }

        addCommand("sequence_replacer", 2) {
            openFileHolder.codeManager.sequenceReplaceHandler.fire()
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
                if (newValue === Worker.State.FAILED)
                    println("Failed to load webpage")
                if (newValue === Worker.State.SUCCEEDED) {
                    val win = getWindow()
                    val cbHook = CallbackHook {
                        val settings = engine.executeScript("getDefaultOptions();") as JSObject
                        settings.setMember("fontSize", coreManager.configManager.get("font_size"))
                        settings.setMember("theme", "vs-dark")
                        startEditor(settings)
                        selection = engine.executeScript("selection") as JSObject
                        prepareEditorActions()
                        rdy(this)
                    }
                    win.setMember("skide", eventHandler)
                    win.setMember("cbh", cbHook)
                    engine.executeScript("cbhReady();")
                }
            }

            engine.load(this.javaClass.getResource("/www/index.html").toString())
        }
    }

    fun getArray() = engine.executeScript("getArr();") as JSObject
    fun getObject() = engine.executeScript("getObj();") as JSObject
    fun getFunction() = engine.executeScript("getFunc();") as JSObject
    private fun getWindow() = engine.executeScript("window") as JSObject
    private fun getModel() = engine.executeScript("editor.getModel()") as JSObject
    private fun startEditor(options: Any) {
        editor = getWindow().call("startEditor", options) as JSObject
    }

    fun activateCommand(key: String) {
        if (!editorCommands.containsKey(key)) return

        editorCommands[key]!!.activate()
    }

    fun deactivateCommand(key: String) {
        if (!editorCommands.containsKey(key)) return
        editorCommands[key]!!.deactivate()
    }

    fun addCommand(key: String, keyId: Int, cb: () -> Unit) {
        if (editorCommands.containsKey(key)) return
        val cont = getWindow().call("addCondition", key, keyId)

        val command = EditorCommandBinder(key, cb)
        command.instance = cont as JSObject
        editorCommands[key] = command
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

    fun addAction(id: String, cb: () -> Unit) {
        if (editorActions.containsKey(id)) return
        val action = EditorActionBinder(id, cb)
        action.instance = getWindow().call("addCommand", id)
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

    data class Selection(val endColumn: Int, val endLineNumber: Int, val positionColumn: Int,
                         val positionLineNumber: Int, val selectionStartColumn: Int, val selectionStartLineNumber: Int,
                         val startColumn: Int, val startLineNumber: Int)

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
        return (getModel().call("getWordAtPosition", createObjectFromMap(hashMapOf(Pair("lineNumber", line),
                Pair("column", column)))) as JSObject).getMember("word") as String
    }


    fun setPosition(line: Int, column: Int) = editor.call("setPosition",
            createObjectFromMap(hashMapOf(Pair("column", column), Pair("lineNumber", line))))

    fun updateOptions(fields: Map<String, Any>) = editor.call("updateOptions",
            createObjectFromMap(fields))

    fun getCurrentLine() = engine.executeScript("editor.getPosition().lineNumber") as Int

    fun getCurentColumn() = engine.executeScript("editor.getPosition().column") as Int

    fun setCursorPosition(line: Int, column: Int) = editor.call("setPosition",
            createObjectFromMap(hashMapOf(Pair("lineNumber", line), Pair("column", column))))

    fun getContentRange(startLine: Int, endLine: Int, startColumn: Int, endColumn: Int): String {
        return getModel().call("getValueInRange",
                createObjectFromMap(hashMapOf(Pair("endColumn", endColumn), Pair("endLineNumber", endLine),
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

    fun setSelection(startLine: Int, startCol: Int, endLine: Int, endColumn: Int) {
        editor.call("setSelection", createObjectFromMap(hashMapOf(Pair("endColumn", endColumn), Pair("endLineNumber", endLine), Pair("startColumn", startCol), Pair("startLineNumber", startLine))))
    }

    fun getLineContent(line: Int) = getModel().call("getLineContent", line) as String
    var text: String
        set(value) {
            editor.call("setValue", value)
        }
        get() {
            return editor.call("getValue") as String
        }


    fun getInfo(manager: CodeManager): CurrentStateInfo {
        val currentLine = getCurrentLine()


        var currentNode = EditorUtils.getLineNode(currentLine, manager.parseResult)

        if (currentNode == null) {
            currentNode = EditorUtils.getLineNode(currentLine - 1, manager.parseResult)
        }
        val actualCurrentString = getLineContent(currentLine)
        val column = getCurentColumn()
        var currentWord = ""
        var beforeStr = ""
        var inString = false
        var inBrace = false
        var afterStr = ""
        val charBeforeCaret = {
            if (column == 0) {
                ""
            } else {
                actualCurrentString[column - 1].toString()
            }
        }.invoke()
        val charAfterCaret = {
            if (column == actualCurrentString.length) {
                ""
            } else {
                actualCurrentString[column].toString()
            }
        }.invoke()

        for (x in 0 until actualCurrentString.length) {
            if (x == column) break
            val c = actualCurrentString[x]
            if (c == '"') inString = !inString
        }
        for (x in 0 until actualCurrentString.length) {
            if (x == column) break
            val c = actualCurrentString[x]
            if (c == '(' || c == ')') inBrace = !inBrace
        }
        if (charBeforeCaret != "") {
            var count = column
            while (count > 0 && actualCurrentString[count - 1].toString() != " " && actualCurrentString[count - 1].toString() != "\n") {

                count--
                beforeStr = actualCurrentString[count].toString() + beforeStr
            }
            count = column - 1
            while (count < actualCurrentString.length - 1 && actualCurrentString[count].toString() != " " && actualCurrentString[count].toString() != "\n") {
                count++
                afterStr += actualCurrentString[count].toString()
            }

            beforeStr = beforeStr.replace("\t", "").replace(" ", "")
            afterStr = afterStr.replace("\t", "").replace(" ", "")
            currentWord = beforeStr + afterStr


        }

        return CurrentStateInfo(currentNode!!, actualCurrentString, column, currentWord, beforeStr, afterStr, charBeforeCaret, charAfterCaret, inString, inBrace)
    }

}

class CurrentStateInfo(val currentNode: Node, val actualCurrentString: String, val column: Int, val currentWord: String,
                       val beforeString: String, val afterString: String, val charBeforeCaret: String, val charAfterCaret: String, val inString: Boolean, val inBrace: Boolean)

