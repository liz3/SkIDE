package com.skide.core.code

import com.skide.gui.LinkOpener
import com.skide.gui.Prompts
import com.skide.include.OpenFileHolder
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.control.Alert
import javafx.scene.web.WebEngine
import javafx.scene.web.WebView
import netscape.javascript.JSObject

class CallbackHook(val rdy: () -> Unit) {


    public fun call() {

        rdy();
    }

}

class EventHandler(val area: CodeArea) {

    public fun eventNotify(name: String, ev:Any) {

       if(ev is JSObject) {


       }

    }

}

class CodeArea(rdy: (CodeArea) -> Unit) {

    lateinit var view: WebView
    lateinit var engine: WebEngine
    lateinit var editor: JSObject
    lateinit var selection: JSObject
    lateinit var openFileHolder: OpenFileHolder

    val eventHandler = EventHandler(this)


    init {
        Platform.runLater {

            view = WebView()
            engine = view.engine

            val url = this.javaClass.getResource("/www/index.html")

            view.engine.loadWorker.stateProperty().addListener { _, _, newValue ->

                if (newValue === Worker.State.SUCCEEDED) {
                    val win = view.engine.executeScript("window") as JSObject
                    val cbHook = CallbackHook {
                        editor = engine.executeScript("editor") as JSObject

                        selection = engine.executeScript("selection") as JSObject

                        rdy(this)
                    }
                    win.setMember("skide", eventHandler)
                    win.setMember("cbh", cbHook)

                }
            }
            engine.load(url.toString())


        }
    }

    fun createObjectFromMap(fields: Map<String, Any>): JSObject {
        val obj = engine.executeScript("getObj();") as JSObject

        for((key, value) in fields) {
            obj.setMember(key, value)
        }

        return obj
    }

    fun updateOptions(fields:Map<String, Any>): Any? {
        return editor.call("updateOptions", createObjectFromMap(fields));
    }
    fun getCurrentLine(): Int {
        return engine.executeScript("editor.getPosition().lineNumber") as Int
    }

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