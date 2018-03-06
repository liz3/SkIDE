package com.skide.core.code.autocomplete

import com.skide.core.code.CodeManager
import com.skide.include.NodeType
import com.skide.include.OpenFileHolder
import com.skide.utils.EditorUtils
import com.skide.utils.getCaretLine
import javafx.application.Platform

class AutoCompleteCompute(val manager: CodeManager, val project: OpenFileHolder) {

    val area = manager.area

    var currentLine = 0
    var lineBefore = 0

    init {


        registerEventListener()
    }

    private fun registerEventListener() {


        area.caretPositionProperty().addListener { observable, oldValue, newValue ->

            lineBefore = currentLine
            currentLine = area.getCaretLine()

            if (lineBefore != currentLine) onLineChange()

        }
        area.textProperty().addListener { observable, oldValue, newValue ->

        }
    }

    private fun onLineChange() {

     Platform.runLater {
         val parseResult = manager.parseStructure()
         manager.parseResult = parseResult

         val old = EditorUtils.getLineNode(lineBefore, parseResult)
         val current = EditorUtils.getLineNode(currentLine, parseResult)

         if(old != null && current != null) {

             if(old.linenumber == current.linenumber - 1) {
                // println("moved one down")
                 if(current.nodeType == NodeType.UNDEFINED) {
                     val tabCount = old.tabLevel -1
                     var str = ""
                     for(x in 0..tabCount) {
                         str += "\t"
                     }

                     if(old.nodeType != NodeType.EXECUTION && old.nodeType != NodeType.UNDEFINED && old.nodeType != NodeType.COMMENT && old.nodeType != NodeType.SET_VAR) str += "\t"
                     area.replaceText(area.caretPosition, area.caretPosition, str)
                 }


                 return@runLater
             }
             if(old.linenumber == current.linenumber + 1) {
                // println("moved one up")


                 return@runLater
             }

         }
     }
    }



}