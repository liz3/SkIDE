package com.skide.core.code.autocomplete

import com.skide.core.code.CodeManager
import com.skide.core.code.CurrentStateInfo
import java.util.*
import java.util.regex.Pattern

object SequenceReplacePattern {

    const val valuePattern = "%.+?%"
    const val optionalPattern = "\\[.*?\\]"
    const val groupPattern = "\\(([^)]+)\\)"
    const val optionalGroupPattern = "\\[\\(([^)]+)\\)\\]"

    val patternCompiler = Pattern.compile(
            "(?<VALUE>" + valuePattern + ")"
                    + "|(?<OPTIONAL>" + optionalPattern + ")"
                    + "|(?<GROUP>" + groupPattern + ")"
                    + "|(?<OPTIONALGROUP>" + optionalGroupPattern + ")")
}

//	if [the] Bedwars game [(named|with name)] %string% is (startable|able to start):
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

    var originalLength = 0
    var lineIndex = 0

    fun compute(info: CurrentStateInfo) {
        if (computing) return
        computing = true
        area.activateCommand("sequence_replacer")
        parse(info)
    }


    private fun parse(info: CurrentStateInfo) {

        list.clear()
        lineIndex = info.currentNode.linenumber
        originalLength = info.currentNode.raw.length

        val matcher = SequenceReplacePattern.patternCompiler.matcher(info.currentNode.raw)

        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()


            if (matcher.group("VALUE") != null) {
                list.add(ReplaceSeuenceItem(start, end, ReplaceSequenceType.VALUE))
            } else {
                list.add(ReplaceSeuenceItem(start, end, ReplaceSequenceType.VALUE))
            }
        }

        fire()
    }


    fun fire() {
        if (!computing) return
        atIndex++

        if (atIndex == list.size) {
            cancel()
            return
        }
        val currentItem = list[atIndex]
        val nowLength = area.getLineContent(lineIndex).length

        if (currentItem != null) {
            area.setSelection(lineIndex, (nowLength - originalLength) + 1, lineIndex,  currentItem.absoluteEnd + (nowLength - originalLength))
        } else {
            cancel()
        }
    }

    fun cancel() {
        if (!computing) return

        computing = false
        list.clear()
        atIndex = -1
        originalLength = 0
        lineIndex = 0
        area.deactivateCommand("sequence_replacer")

    }

}