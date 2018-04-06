package com.skide.core.code.autocomplete

import com.skide.core.code.CodeManager
import com.skide.utils.CurrentStateInfo
import org.fxmisc.wellbehaved.event.EventPattern
import org.fxmisc.wellbehaved.event.InputMap
import org.fxmisc.wellbehaved.event.Nodes
import java.util.*
import java.util.regex.Pattern

object SequenceReplacePattern{
    const val valuePattern = "%.+?%"
    
    const val optionalPattern = "\\[.*?\\]"
    
    const val groupPattern = "\\(([^)]+)\\)"
    
    const val optionalGroupPattern = "\\[\\(([^)]+)\\)\\]"

    val patternCompiler = Pattern.compile("(?<VALUE>$valuePattern)|(?<OPTIONAL>$optionalPattern)|(?<GROUP>$groupPattern)|(?<OPTIONALGROUP>$optionalGroupPattern)")
}

//if [the] Bedwars game [(named|with name)] %string% is (startable|able to start):
enum class ReplaceSequenceType{
    OPTIONAL,
    GROUP,
    VALUE,
    OPTIONAL_VALUE,
    OPTIONAL_GROUP
}

data class ReplaceSequenceItem(val absoluteStart: Int, val absoluteEnd: Int, val type: ReplaceSequenceType, val fields: HashMap<String, Any> = HashMap())

class ReplaceSequence(val manager: CodeManager){
    val area = manager.area

    var computing = false

    val list = Vector<ReplaceSequenceItem>()

    var atIndex = -1

    val im = InputMap.consume(EventPattern.keyTyped("\t"), { })

    var originalLength = 0

    var lineIndex = 0

    fun compute(info: CurrentStateInfo){
        if (computing){
            return
        }

        computing = true

        manager.autoComplete.stopped = true

        manager.autoComplete.hideList()

        parse(info)

        Nodes.addInputMap(area, im)
    }


    private fun parse(info: CurrentStateInfo){
        list.clear()

        lineIndex = info.currentNode.linenumber

        originalLength = info.currentNode.raw.length

        val absStart = area.caretPosition - area.caretColumn

        val matcher = SequenceReplacePattern.patternCompiler.matcher(info.currentNode.raw)

        while (matcher.find()){
            val start = matcher.start()

            val end = matcher.end()

            if (matcher.group("VALUE") != null){
                list.add(ReplaceSequenceItem(absStart + start, absStart + end, ReplaceSequenceType.VALUE))
            }else{
                list.add(ReplaceSequenceItem(absStart + start, absStart + end, ReplaceSequenceType.VALUE))
            }
        }

        fire()
    }

    fun fire(){
        if (!computing){
            return
        }

        atIndex++

        if (atIndex == list.size){
            cancel()

            return
        }

        val currentItem = list[atIndex]

        val nowLength = area.paragraphs[lineIndex - 1].text.length

        if (currentItem != null){
            area.selectRange(currentItem.absoluteStart + (nowLength - originalLength), currentItem.absoluteEnd + (nowLength - originalLength))
        }else{
            cancel()
        }
    }

    fun cancel(){
        if (!computing){
            return
        }

        Nodes.removeInputMap(area, im)

        computing = false

        manager.autoComplete.stopped = false

        list.clear()

        atIndex = -1

        originalLength = 0

        lineIndex = 0
    }
}