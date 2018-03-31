package com.skide.core.code.highlighting

import com.skide.core.code.CodeManager
import com.skide.utils.StyleSpanMerger
import com.skide.utils.restart
import javafx.application.Platform
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.*
import java.util.function.BiFunction
import java.util.regex.Pattern

class Highlighting(val manager: CodeManager) {

    val area = manager.area
    private val x = area.richChanges().filter({ ch -> ch.inserted != ch.removed })
    var sub = x.subscribe({
        area.setStyleSpans(0, computHighlighting(area.text))


    })
    private val markedLines = Vector<String>()

    fun computeHighlighting() {
        area.replaceText(0, 0, area.text)
    }

    fun searchHighlighting(searched: String, case: Boolean, regex: Boolean) {


        val pos = area.caretPosition
        sub.unsubscribe()
        sub = x.subscribe({ area.setStyleSpans(0, computeSearchHightlighting(area.text, searched, case, regex)) })
        val text = area.text

        area.clear()
        area.appendText(text)
        area.moveTo(pos)

    }

    fun restartHighlighting() {

        val pos = area.caretPosition
        sub.unsubscribe()

        sub = x.subscribe({

            area.setStyleSpans(0, computHighlighting(area.text))
        })
        val text = area.text
        area.clear()
        area.appendText(text)
        area.moveTo(pos)
    }

    fun stopHighLighting() {

        val pos = area.caretPosition
        sub.unsubscribe()
        area.clearStyle(0, area.text.length)
        val text = area.text
        area.clear()
        area.appendText(text)
        area.moveTo(pos)
    }


    private fun computHighlighting(text: String): StyleSpans<Collection<String>> {


        var lastKwEnd = 0
        val spansBuilder = StyleSpansBuilder<Collection<String>>()


        val matcher = patternCompilerStatic.matcher(text)
        while (matcher.find()) {
            val styleClass = when {
                matcher.group("SECTION") != null -> "section"
                matcher.group("COLOR0") != null -> "color-0"
                matcher.group("COLOR1") != null -> "color-1"
                matcher.group("COLOR2") != null -> "color-2"
                matcher.group("COLOR3") != null -> "color-3"
                matcher.group("COLOR4") != null -> "color-4"
                matcher.group("COLOR5") != null -> "color-5"
                matcher.group("COLOR6") != null -> "color-6"
                matcher.group("COLOR7") != null -> "color-7"
                matcher.group("COLOR8") != null -> "color-8"
                matcher.group("COLOR9") != null -> "color-9"
                matcher.group("COLORA") != null -> "color-a"
                matcher.group("COLORB") != null -> "color-b"
                matcher.group("COLORC") != null -> "color-c"
                matcher.group("COLORD") != null -> "color-d"
                matcher.group("COLORE") != null -> "color-e"
                matcher.group("COLORF") != null -> "color-f"
                matcher.group("NUMBERS") != null -> "numbers"
                matcher.group("OPERATORS") != null -> "operators"
                matcher.group("COMMAND") != null -> "operators"
                matcher.group("PAREN") != null -> "paren"
                matcher.group("BRACKET") != null -> "bracket"
                matcher.group("STRING") != null -> "string"
                matcher.group("COMMENT") != null -> "comment"
                matcher.group("VARS") != null -> "vars"
                else -> null
            }!!


            spansBuilder.add(emptyList(), matcher.start() - lastKwEnd)
            spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
            lastKwEnd = matcher.end()
        }
        spansBuilder.add(emptyList(), text.length - lastKwEnd)

        return spansBuilder.create()
    }

    private fun computeSearchHightlighting(text: String, search: String, case: Boolean, regex: Boolean): StyleSpans<Collection<String>> {

        val matcher = searchPatternCompiler(search, case, regex).matcher(text)
        var lastKwEnd = 0
        val spansBuilder = StyleSpansBuilder<Collection<String>>()

        while (matcher.find()) {
            val styleClass = when {
                matcher.group("SEARCH") != null -> "marked"
                else -> null
            }!!
            spansBuilder.add(emptyList(), matcher.start() - lastKwEnd)
            spansBuilder.add(setOf(styleClass), matcher.end() - matcher.start())
            lastKwEnd = matcher.end()
        }
        spansBuilder.add(emptyList(), text.length - lastKwEnd)
        return spansBuilder.create()
    }

    private fun searchPatternCompiler(word: String, case: Boolean, regex: Boolean): Pattern {

        val content = if (regex) word else Pattern.quote(word)

        return if (!case) Pattern.compile("(?<SEARCH>$content)", Pattern.CASE_INSENSITIVE) else Pattern.compile("(?<SEARCH>$content)")
    }

    fun mapMarked() {

        //manager.marked is a set of line-numbers
        for (line in manager.marked) {

            //is invoked from another thread so Platform.runLater is required
            Platform.runLater {
                //get the lines text
                val text = area.paragraphs[line].text
                //Currently 0 would be changed to absolute line start later
                val start = 0
                val end = text.length
                //get the spans
                val spans = area.getStyleSpans(start, end)
                //The merge method we talked about before
                val result = StyleSpanMerger.merge(spans, end)
                //reset the style
                area.setStyleSpans(start, end, result)
            }

        }

    }

    private val patternCompilerStatic = Pattern.compile(
            "(?<SECTION>" + HighlighterStatics.SECTION_PATTERN + ")"
                    + "|(?<COLOR0>" + HighlighterStatics.COLOR_0_PATTERN + ")"
                    + "|(?<COLOR1>" + HighlighterStatics.COLOR_1_PATTERN + ")"
                    + "|(?<COLOR2>" + HighlighterStatics.COLOR_2_PATTERN + ")"
                    + "|(?<COLOR3>" + HighlighterStatics.COLOR_3_PATTERN + ")"
                    + "|(?<COLOR4>" + HighlighterStatics.COLOR_4_PATTERN + ")"
                    + "|(?<COLOR5>" + HighlighterStatics.COLOR_5_PATTERN + ")"
                    + "|(?<COLOR6>" + HighlighterStatics.COLOR_6_PATTERN + ")"
                    + "|(?<COLOR7>" + HighlighterStatics.COLOR_7_PATTERN + ")"
                    + "|(?<COLOR8>" + HighlighterStatics.COLOR_8_PATTERN + ")"
                    + "|(?<COLOR9>" + HighlighterStatics.COLOR_9_PATTERN + ")"
                    + "|(?<COLORA>" + HighlighterStatics.COLOR_A_PATTERN + ")"
                    + "|(?<COLORB>" + HighlighterStatics.COLOR_B_PATTERN + ")"
                    + "|(?<COLORC>" + HighlighterStatics.COLOR_C_PATTERN + ")"
                    + "|(?<COLORD>" + HighlighterStatics.COLOR_D_PATTERN + ")"
                    + "|(?<COLORE>" + HighlighterStatics.COLOR_E_PATTERN + ")"
                    + "|(?<COLORF>" + HighlighterStatics.COLOR_F_PATTERN + ")"

                    + "|(?<NUMBERS>" + HighlighterStatics.NUMBERS_PATTERN + ")"

                    + "|(?<OPERATORS>" + HighlighterStatics.OPERATORS_PATTERN + ")"
                    + "|(?<COMMAND>" + HighlighterStatics.COMMAND_PATTERN + ")"

                    + "|(?<PAREN>" + HighlighterStatics.PAREN_PATTERN + ")"
                    + "|(?<BRACKET>" + HighlighterStatics.BRACKET_PATTERN + ")"
                    + "|(?<STRING>" + HighlighterStatics.STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + HighlighterStatics.COMMENT_PATTERN + ")"
                    + "|(?<VARS>" + HighlighterStatics.VAR_PATTERN + ")")

}

object HighlighterStatics {

    const val SECTION_PATTERN = "(?<=\\n)\\s*usage:|executable by:|aliases:|permission:|permission message:|description:|cooldown:|cooldown message:|cooldown bypass:|cooldown storage:"

    const val COLOR_0_PATTERN = "§0|&0"
    const val COLOR_1_PATTERN = "§1|&1"
    const val COLOR_2_PATTERN = "§2|&2"
    const val COLOR_3_PATTERN = "§3|&3"
    const val COLOR_4_PATTERN = "§4|&4"
    const val COLOR_5_PATTERN = "§5|&5"
    const val COLOR_6_PATTERN = "§6|&6"
    const val COLOR_7_PATTERN = "§7|&7"
    const val COLOR_8_PATTERN = "§8|&8"
    const val COLOR_9_PATTERN = "§9|&9"
    const val COLOR_A_PATTERN = "§a|&a"
    const val COLOR_B_PATTERN = "§b|&b"
    const val COLOR_C_PATTERN = "§c|&c"
    const val COLOR_D_PATTERN = "§d|&d"
    const val COLOR_E_PATTERN = "§e|&e"
    const val COLOR_F_PATTERN = "§f|&f"

    const val NUMBERS_PATTERN = "[0-9]"

    const val COMMAND_PATTERN = "(?<=\\G|\\n)command(?=\\s)"
    const val OPERATORS_PATTERN = "trigger:|if |else:|else if |while |loop | is | contains |function |set |on "
    //val KEYWORDS = arrayOf("set", "if", "stop", "loop", "return", "function", "options", "true", "false", "cancel", "else", "else if")
    const val COMMENT_PATTERN = "#[^\n]*"
    const val VAR_PATTERN = "\\{\\S*}"
    const val PAREN_PATTERN = "\\(|\\)"
    const val BRACKET_PATTERN = "\\[|\\]"
    const val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
    fun joinBoundaryPattern(items: Array<String>) = "\\b(" + items.joinToString("|") + ")\\b"
    fun joinList(items: Array<String>) = items.joinToString("|")

}