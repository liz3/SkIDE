package com.skide.core.code.highlighting

import com.skide.core.code.CodeManager
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern

class Highlighting(val manager: CodeManager) {

    val area = manager.area

    fun computeHighlighting() {


            area.richChanges()
                    .filter({ ch -> ch.inserted != ch.removed }).subscribe({ area.setStyleSpans(0, computHightlighting(area.text)) })

            area.replaceText(0, 0, area.text)

    }

// This is a comment
    private fun computHightlighting(text: String): StyleSpans<Collection<String>> {

        val matcher = patternCompiler.matcher(text)
        var lastKwEnd = 0
        val spansBuilder = StyleSpansBuilder<Collection<String>>()

        while (matcher.find()) {
            val styleClass = when {
                matcher.group("PAREN") != null -> "paren"
             //   matcher.group("BRACE") != null -> "brace"
                matcher.group("BRACKET") != null -> "bracket"
                matcher.group("STRING") != null -> "string"
                matcher.group("COMMENT") != null -> "comment"
                matcher.group("KEYWORDS") != null -> "keywords"
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

    private val patternCompiler = Pattern.compile(
            "(?<PAREN>" + HightlighterStatics.PAREN_PATTERN + ")"
                //    + "|(?<BRACE>" + HightlighterStatics.BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + HightlighterStatics.BRACKET_PATTERN + ")"
                    + "|(?<STRING>" + HightlighterStatics.STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + HightlighterStatics.COMMENT_PATTERN + ")"
                    + "|(?<VARS>" + HightlighterStatics.VAR_PATTERN + ")"
                    + "|(?<KEYWORDS>" + HightlighterStatics.joinBoundaryPattern(HightlighterStatics.KEYWORDS) + ")")
}

object HightlighterStatics {

    val KEYWORDS = arrayOf("set", "if", "stop", "loop", "trigger", "permission","permission-message","description", "return", "function", "options", "true", "false", "cancel", "else", "else if")
    const val COMMENT_PATTERN = "#[^\n]*"
    const val VAR_PATTERN = "\\{\\S*}"
    const val PAREN_PATTERN = "\\(|\\)"
    const val BRACE_PATTERN = "\\{|\\}"
    const val BRACKET_PATTERN = "\\[|\\]"
    const val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
    fun joinBoundaryPattern(items: Array<String>) = "\\b(" + items.joinToString("|") + ")\\b"


}