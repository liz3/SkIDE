package com.skide.core.code.autocomplete

import com.skide.core.code.CodeManager
import com.skide.utils.CurrentStateInfo
import com.skide.utils.EditorUtils
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import java.util.*
import kotlin.math.abs

enum class ReplaceSequenceType {
    OPTIONAL,
    GROUP,
    VALUE,
    OPTIONAL_VALUE,
    OPTIONAL_GROUP
}

data class ReplaceSeuenceItem(val absoluteStart: Int, val absoluteEnd: Int, val type: ReplaceSequenceType, val fields: HashMap<String, Any> = HashMap())

class ReplaceSequence(val manager: CodeManager) {

    val area = manager.area
    var computing = false
    val list = Vector<ReplaceSeuenceItem>()
    var atIndex = -1
    val im = InputMap.consume(
            EventPattern.keyTyped("\t"),
            {  }
    )
    var originalLength = 0
    var lineIndex = 0

    fun compute(info: CurrentStateInfo, length: Int) {
        if (computing) return
        computing = true
        manager.autoComplete.stopped = true
        parse(info, length)
        Nodes.addInputMap(area, im)
    }


    private fun parse(info: CurrentStateInfo, length: Int) {

        list.clear()
        var inQuote = false
        var absBegin = area.caretPosition - length - 1
        var currPointer = -1
        println("${area.caretPosition}:$length")
        var str = info.actualCurrentString

        while (str.startsWith("\t")) {
            str = str.substring(1)
            currPointer++
        }
        originalLength = str.length
        lineIndex = info.currentNode.linenumber
        while (currPointer != str.length - 1) {
            currPointer++
            var currentChar = str[currPointer]
            if (currentChar == '"') {
                inQuote = !inQuote
                continue
            }
            if (currentChar == '%') {
                val start = absBegin + currPointer + 2 - info.currentNode.tabLevel
                var expression = ""
                currPointer++
                currentChar = str[currPointer]
                expression += currentChar
                while (currentChar != '%') {
                    currPointer++
                    currentChar = str[currPointer]
                    expression += currentChar
                }
                val end = absBegin + currPointer + 1 - info.currentNode.tabLevel
                val value = ReplaceSeuenceItem(start, end, ReplaceSequenceType.VALUE)
                value.fields["name"] = expression.substring(0, expression.length - 1)
                list.add(value)
            }
            if (currentChar == '[') {
                val hasMultiple = str[currPointer + 1] == '('
                val start = absBegin + currPointer + 1 - info.currentNode.tabLevel
                var expression = ""
                while (currentChar != ']') {
                    currPointer++
                    currentChar = str[currPointer]
                    expression += currentChar
                }
                val end = absBegin + currPointer + 1 + 1 - info.currentNode.tabLevel
                val entry = ReplaceSeuenceItem(start, end, if (hasMultiple) ReplaceSequenceType.OPTIONAL_GROUP else ReplaceSequenceType.OPTIONAL)

                if (hasMultiple) {
                    val opts = Vector<String>()
                    expression.replace("(", "").replace(")", "").split("|").forEach { opts.add(it) }
                    entry.fields["values"] = opts
                } else {
                    entry.fields["value"] = expression
                }
                list.addElement(entry)
            }
            if (currentChar == '(') {
                val start = absBegin + currPointer + 1 - info.currentNode.tabLevel
                var expression = ""
                currPointer++
                currentChar = str[currPointer]
                expression += currentChar
                while (currentChar != ')') {
                    currPointer++
                    currentChar = str[currPointer]
                    expression += currentChar
                }
                val end = absBegin + currPointer + 1 + 1 - info.currentNode.tabLevel
                val value = ReplaceSeuenceItem(start, end, ReplaceSequenceType.GROUP)

                if (expression.contains("|")) {
                    val opts = Vector<String>()
                    expression.split("|").forEach { opts.add(it) }
                    value.fields["values"] = opts
                } else {
                    value.fields["value"] = expression

                }

                list.addElement(value)
            }

        }


        fire()
    }

    // on lua script disable:
    fun fire() {
        if (!computing) return
        atIndex++

        if(atIndex == list.size) {
            cancel()
            return
        }
        val currentItem = list[atIndex]
        val nowLength = area.paragraphs[lineIndex - 1].text.length

        if(currentItem != null) {
            area.selectRange(currentItem.absoluteStart + (nowLength - originalLength), currentItem.absoluteEnd + (nowLength - originalLength))
        } else{
            cancel()
        }
    }

    fun cancel() {
        if (!computing) return
        Nodes.removeInputMap(area, im)

        computing = false
        manager.autoComplete.stopped = false
        list.clear()
        atIndex = -1
        originalLength = 0
        lineIndex = 0

    }
}