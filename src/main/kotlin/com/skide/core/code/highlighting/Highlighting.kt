package com.skide.core.code.highlighting

import com.skide.core.code.CodeManager
import java.util.*
import java.util.regex.Pattern


class Highlighting(val manager: CodeManager) {

    private var stopped = false
    val area = manager.area

  /*
    fun runHighlighting() {
        if (stopped) return
        val highlightedSpans = computHighlighting(area.text)
        val marks = mapMarked()

        val spans = highlightedSpans.overlay(
                marks
        ) { originalList, addedList ->
            if (addedList.isEmpty()) {
                originalList
            } else {
                val l = ArrayList<String>(originalList.size + addedList.size)
                l.addAll(originalList)
                l.addAll(addedList)
                l
            }


        }
        area.setStyleSpans(0, spans)
    }

    fun computeHighlighting() {
        area.replaceText(0, 0, area.text)
    }

    fun searchHighlighting(searched: String, case: Boolean, regex: Boolean) {
        area.setStyleSpans(0, computeSearchHightlighting(area.text, searched, case, regex))
    }

    fun restartHighlighting() {
        stopped = false
        sub.unsubscribe()
        sub = x.subscribe({
            if (manager.linesAmount <= 2000) runHighlighting()
        })
        runHighlighting()

    }

    fun stopHighLighting() {
        sub.unsubscribe()
        stopped = true
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
                matcher.group("YAML") != null -> "yaml"

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
                matcher.group("SEARCH") != null -> "searched"
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

    private fun mapMarked(): StyleSpans<Collection<String>>? {


        if (manager.marked.size == 0) return StyleSpans.singleton(emptyList(), 0)

        val sorted = manager.marked.toSortedMap()

        val builder = StyleSpansBuilder<Collection<String>>()
        var endOfPrevStyle = 0
        for (line in sorted.keys) {
            val startStyle = area.getAbsolutePosition(line, 0)
            builder.add(Collections.emptyList(), startStyle - endOfPrevStyle)
            val lineLength = area.getParagraphLength(line)
            val text = area.paragraphs[line].text
            val offset = text.takeWhile { it.isWhitespace() }.length
            val substr = text.takeLastWhile { it.isWhitespace() }.length
            if (lineLength - offset - substr <= 0) continue
            builder.add(emptyList(), offset)
            builder.add(Collections.singletonList("marked"), lineLength - offset - substr)
            builder.add(emptyList(), substr)
            endOfPrevStyle = startStyle + lineLength
        }
        return builder.create()


    }
   */

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
                    + "|(?<OPERATORS>" + HighlighterStatics.joinBoundaryPattern(HighlighterStatics.KEYWORDS) + ")"
                    + "|(?<COMMAND>" + HighlighterStatics.COMMAND_PATTERN + ")"
                    + "|(?<PAREN>" + HighlighterStatics.PAREN_PATTERN + ")"
                    + "|(?<BRACKET>" + HighlighterStatics.BRACKET_PATTERN + ")"
                    + "|(?<STRING>" + HighlighterStatics.STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + HighlighterStatics.COMMENT_PATTERN + ")"
                    + "|(?<VARS>" + HighlighterStatics.VAR_PATTERN + ")"
                    + "|(?<YAML>" + HighlighterStatics.YAML_PATTERN + ")")

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
    val KEYWORDS = arrayOf("set", "if", "stop", "loop", "return", "function", "options", "true", "false", "else", "else if", "trigger", "on", "while", "is")
    const val COMMENT_PATTERN = "#[^\n]*"
    const val VAR_PATTERN = "\\{\\S*}"
    const val PAREN_PATTERN = "\\(|\\)"
    const val BRACKET_PATTERN = "\\[|\\]"
    const val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""

    const val YAML_PATTERN = "yaml"

    fun joinBoundaryPattern(items: Array<String>) = "\\b(" + items.joinToString("|") + ")\\b"
    fun joinList(items: Array<String>) = items.joinToString("|")

}