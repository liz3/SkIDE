@file:Suppress("unused")

package com.skide.core.code

import com.skide.CoreManager
import com.skide.gui.ListViewPopUp
import com.skide.gui.WebViewDebugger
import com.skide.include.OpenFileHolder
import com.skide.utils.*
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.event.EventHandler
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.input.Clipboard
import javafx.scene.input.ClipboardContent
import javafx.scene.input.DataFormat
import javafx.scene.input.KeyCode
import javafx.scene.layout.StackPane
import javafx.scene.web.WebEngine
import javafx.scene.web.WebEvent
import javafx.scene.web.WebView
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import netscape.javascript.JSObject
import java.io.File


class CallbackHook(private val rdy: () -> Unit) {
    fun call() = rdy()
}

class EventHandler(private val area: CodeArea) {

    fun copy() {
        Platform.runLater {
            area.copySelectionToClipboard()
        }
    }

    fun cut() {
        Platform.runLater {
            val selection = area.getSelection()
            area.copySelectionToClipboard()
            area.replaceContentInRange(selection.startLineNumber, selection.startColumn, selection.endLineNumber, selection.endColumn, "")
        }
    }

    fun eventNotify(name: String, ev: Any) {
        if (name == "onDidChangeCursorPosition") {
            val currentLine = area.getCurrentLine()
            if (area.line != currentLine) {
                if (area.openFileHolder.codeManager.sequenceReplaceHandler.computing || area.getLineContent(currentLine).isBlank())
                    area.openFileHolder.codeManager.sequenceReplaceHandler.cancel()
                area.codeManager.parseStructure(false)
            }
            area.line = currentLine
        }
        if (name == "onDidChangeModelContent") {
           area.updateWatcher.update()
        }
    }

    fun cmdCall(key: String) {
        if (area.editorCommands.containsKey(key)) {
            area.editorCommands[key]!!.cb()
        }
    }

    fun findReferences(model: Any, position: Any, context: Any): Any {

        val lineNumber = (position as JSObject).getMember("lineNumber") as Int
        val column = position.getMember("column") as Int

        val word = area.getWordAtPosition(lineNumber, column)

        return area.codeManager.referenceProvider.findReference(model, lineNumber, word, area.getArray())
    }

    fun gotoCall(model: Any, position: Any, token: Any): Any {
        val pos = position as JSObject
        val lineNumber = pos.getMember("lineNumber") as Int
        val column = pos.getMember("column") as Int

        val result = area.openFileHolder.codeManager.definitonFinder.search(lineNumber, column, area.getWordAtPosition(lineNumber, column))
        val obj = area.getObject()
        if (!result.success) return obj

        if (result.fName != "") {
            area.openFileHolder.openProject.project.fileManager.projectFiles.values.forEach { f ->
                if (f.name.endsWith(".sk") && f.name == result.fName && token as Boolean) {
                    if (area.openFileHolder.openProject.guiHandler.openFiles.containsKey(f)) {
                        val entry = area.openFileHolder.openProject.guiHandler.openFiles[f]
                        if (entry != null) {
                            entry.tabPane.selectionModel.select(entry.tab)
                            entry.area.moveLineToCenter(result.line)
                            entry.area.setSelection(result.line, 1, result.line, entry.area.getColumnLineAmount(result.line))
                        }
                    } else {
                        area.openFileHolder.openProject.eventManager.openFile(f, false) {
                            Platform.runLater {
                                it.area.moveLineToCenter(result.line)
                                it.area.setSelection(result.line, 1, result.line, it.area.getColumnLineAmount(result.line))

                            }
                        }
                    }
                }
            }
            return obj
        }
        obj.setMember("range", area.createObjectFromMap(hashMapOf(
                Pair("startLineNumber", result.line),
                Pair("endLineNumber", result.line),
                Pair("startColumn", result.column),
                Pair("endColumn", result.column))))
        obj.setMember("uri", (model as JSObject).getMember("uri"))
        return obj
    }

    fun autoCompleteRequest(doc: Any, pos: Any, token: Any, context: Any): JSObject {
        val array = area.getArray()
        if (area.coreManager.configManager.get("auto_complete") == "true") {


            if (area.getCurrentColumn() < 3 && !area.getLineContent(area.getCurrentLine()).startsWith("\t")) {
                area.openFileHolder.codeManager.autoComplete.showGlobalAutoComplete(array)
            } else {
                area.openFileHolder.codeManager.autoComplete.showLocalAutoComplete(array)
            }
        }
        return array
    }

    fun contextMenuEmit(ev: JSObject) {
        if (((ev.getMember("event") as JSObject).getMember("leftButton") as Boolean) &&
                !((ev.getMember("event") as JSObject).getMember("rightButton") as Boolean)) return

        val selection = area.getSelection()
        if (selection.startColumn == selection.endColumn && selection.startLineNumber == selection.endLineNumber &&
                area.editorActions.containsKey("skunityReport")) {
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
        return area.getObject()
    }
}

class EditorActionBinder(val id: String, val cb: () -> Unit) {
    lateinit var instance: Any
}

class EditorCommandBinder(val id: String, val cb: () -> Unit) {
    lateinit var instance: JSObject
    fun activate() = instance.call("set", true)
    fun deactivate() = instance.call("set", false)
}

class CodeArea(val coreManager: CoreManager, val file: File, val rdy: (CodeArea) -> Unit) {

    var line = 1
    lateinit var view: WebView
    lateinit var engine: WebEngine
    lateinit var editor: JSObject
    lateinit var selection: JSObject
    lateinit var openFileHolder: OpenFileHolder
    lateinit var codeManager: CodeManager
    val editorActions = HashMap<String, EditorActionBinder>()
    val editorCommands = HashMap<String, EditorCommandBinder>()
    val eventHandler = EventHandler(this)
    lateinit var debugger: WebViewDebugger
    var findWidgetVisible = false
    val updateWatcher = ChangeWatcher(1000) {
        Platform.runLater {
            codeManager.parseResult = codeManager.parseStructure()
        }
    }

    fun getArray() = engine.executeScript("getArr();") as JSObject
    fun getObject() = engine.executeScript("getObj();") as JSObject
    fun getFunction() = engine.executeScript("getFunc();") as JSObject
    fun getWindow() = engine.executeScript("window") as JSObject
    fun getModel() = engine.executeScript("editor.getModel()") as JSObject
    private fun startEditor(options: Any) {
        editor = getWindow().call("startEditor", options) as JSObject
    }

    fun copySelectionToClipboard() {

        val sel = getSelection()
        val cb = Clipboard.getSystemClipboard()
        val content = ClipboardContent()
        val str = getContentRange(sel.startLineNumber, sel.endLineNumber, sel.startColumn, sel.endColumn)
        if (str.isEmpty()) return
        content.putString(str)
        cb.setContent(content)
    }

    fun pasteSelectionFromClipboard() {

        val selection = getSelection()
        val cb = Clipboard.getSystemClipboard()
        if (cb.hasContent(DataFormat.PLAIN_TEXT)) {
            val text = cb.string
            replaceContentInRange(selection.startLineNumber, selection.startColumn, selection.endLineNumber, selection.endColumn, text)

        }
    }

    init {

        view = WebView()
        engine = view.engine
        view.setOnKeyPressed { ev ->

            if (ev.code == KeyCode.ESCAPE) {
                if (openFileHolder.codeManager.isSetup && openFileHolder.codeManager.sequenceReplaceHandler.computing)
                    openFileHolder.codeManager.sequenceReplaceHandler.cancel()
            }

            if (getOS() == OperatingSystemType.MAC_OS) {

                if (getLocale() == "de_DE") {
                    if (verifyKeyCombo(ev) && ev.code == KeyCode.Z)
                        triggerAction("undo")
                    if (verifyKeyCombo(ev) && ev.code == KeyCode.Y)
                        triggerAction("redo")
                } else {
                    if (verifyKeyCombo(ev) && ev.code == KeyCode.Y)
                        triggerAction("undo")
                    if (verifyKeyCombo(ev) && ev.code == KeyCode.Z)
                        triggerAction("redo")
                }

            }
            if (ev.code == KeyCode.C && verifyKeyCombo(ev)) {
                Platform.runLater {
                    copySelectionToClipboard()
                }
            }
            if (ev.code == KeyCode.X && verifyKeyCombo(ev)) {
                Platform.runLater {
                    val selection = getSelection()
                    copySelectionToClipboard()
                    replaceContentInRange(selection.startLineNumber, selection.startColumn, selection.endLineNumber, selection.endColumn, "")
                }
            }

        }

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
                    settings.setMember("language", extensionToLang(file.name.split(".").last()))
                    if (coreManager.configManager.get("theme") == "Dark")
                        settings.setMember("theme", "skript-dark")
                    else
                        settings.setMember("theme", "skript-light")
                    startEditor(settings)
                    selection = engine.executeScript("selection") as JSObject
                    rdy(this)
                    prepareEditorActions()
                    debugger = WebViewDebugger(this)
                    if (coreManager.configManager.get("webview_debug") == "true") debugger.start()
                }
                win.setMember("skide", eventHandler)
                win.setMember("cbh", cbHook)
                Thread {
                    Thread.sleep(260)
                    Platform.runLater {
                        engine.executeScript("cbhReady();")
                    }
                }.start()
            }
        }
        engine.load(this.javaClass.getResource("/www/index.html").toString())

    }

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
        addAction("compile", "Export/Minify") {
            val openProject = openFileHolder.openProject
            val map = HashMap<String, () -> Unit>()
            for ((name, opt) in openProject.project.fileManager.compileOptions) {
                map[name] = {
                    openProject.guiHandler.openFiles.forEach { it.value.manager.saveCode() }
                    openProject.compiler.compile(openProject, opt,
                            openProject.guiHandler.lowerTabPaneEventManager.setupBuildLogTabForInput())
                }
            }
            ListViewPopUp("Compile/Minify", map)
        }
        addAction("run", "Run this File") {
            val map = HashMap<String, () -> Unit>()
            coreManager.serverManager.servers.forEach {
                map[it.value.configuration.name] = {
                    openFileHolder.openProject.run(it.value, openFileHolder)
                }
            }
            ListViewPopUp("Run this file", map)
        }
        addAction("upload", "Upload this file") {
            val map = HashMap<String, () -> Unit>()
            openFileHolder.openProject.project.fileManager.hosts.forEach {
                map[it.name] = {
                    openFileHolder.openProject.deployer.deploy(text, openFileHolder.f.name, it)
                }
            }
            ListViewPopUp("Upload this file", map)
        }
        /*
          addActionCopyPaste("editor.action.clipboardCutAction", "Copy") {
              println("Copy action called")
          }
         */


        if (!openFileHolder.isExternal) {
            addAction("runc", "Run Configuration") {
                val map = HashMap<String, () -> Unit>()

                for ((name, opt) in openFileHolder.openProject.project.fileManager.compileOptions) {
                    map[name] = {
                        val map2 = HashMap<String, () -> Unit>()
                        coreManager.serverManager.servers.forEach {
                            map2[it.value.configuration.name] = {
                                openFileHolder.openProject.guiHandler.openFiles.forEach { code -> code.value.manager.saveCode() }
                                openFileHolder.openProject.run(it.value, opt)
                            }
                        }
                        ListViewPopUp(name, map2)
                    }
                }
                ListViewPopUp("Run Configuration", map)
            }
        }


        addAction("general_auto_complete_finish") {
            openFileHolder.codeManager.sequenceReplaceHandler.compute(getCurrentLine(), getLineContent(getCurrentLine()))
        }
        addAction("create_command") {
            openFileHolder.codeManager.autoComplete.createCommand()
        }

        addCommand("sequence_replacer", 2) {
            openFileHolder.codeManager.sequenceReplaceHandler.fire()
        }

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

    fun triggerAction(id: String) {
        editor.call("trigger", "_", id)
    }

    fun addAction(id: String, label: String, cb: () -> Unit) {
        if (editorActions.containsKey(id)) return
        val action = EditorActionBinder(id, cb)
        action.instance = getWindow().call("addAction", id, label)
        editorActions[id] = action
    }

    fun addActionCopyPaste(id: String, label: String, cb: () -> Unit) {
        if (editorActions.containsKey(id)) return
        val action = EditorActionBinder(id, cb)
        action.instance = getWindow().call("addActionCopyPaste", id, label)
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

    fun getWordAtPosition(line: Int = getCurrentLine(), column: Int = getCurrentColumn()): String {
        return (getModel().call("getWordAtPosition", createObjectFromMap(hashMapOf(Pair("lineNumber", line),
                Pair("column", column)))) as JSObject).getMember("word") as String
    }


    data class WordAtPos(val word: String, val endColumn: Int, val startColumn: Int)

    fun getWordUntilPosition(line: Int = getCurrentLine(), pos: Int = getCurrentColumn()): WordAtPos {
        val result = getModel().call("getWordUntilPosition",
                createObjectFromMap(hashMapOf(Pair("column", pos), Pair("lineNumber", line)))) as JSObject

        return WordAtPos(result.getMember("word") as String, result.getMember("endColumn") as Int, result.getMember("startColumn") as Int)
    }


    fun moveLineToCenter(line: Int) = editor.call("revealLineInCenter", line)
    fun setPosition(line: Int, column: Int) = editor.call("setPosition",
            createObjectFromMap(hashMapOf(Pair("column", column), Pair("lineNumber", line))))

    fun updateOptions(fields: Map<String, Any>) = editor.call("updateOptions", createObjectFromMap(fields))

    fun getCurrentLine() = engine.executeScript("editor.getPosition().lineNumber") as Int
    fun getCurrentColumn() = engine.executeScript("editor.getPosition().column") as Int
    fun setCursorPosition(line: Int, column: Int) = setPosition(line, column)

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
}