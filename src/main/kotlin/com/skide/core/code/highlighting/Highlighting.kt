package com.skide.core.code.highlighting

import com.skide.core.code.CodeManager
import org.fxmisc.richtext.model.StyleSpans
import org.fxmisc.richtext.model.StyleSpansBuilder
import java.util.regex.Pattern

class Highlighting(val manager: CodeManager) {

    val area = manager.area
    private val x = area.richChanges().filter({ ch -> ch.inserted != ch.removed })
    var sub = x.subscribe({ area.setStyleSpans(0, computHighlighting(area.text)) })

    fun computeHighlighting() {


        area.replaceText(0, 0, area.text)

    }

    fun searchHighlighting(searched: String, case:Boolean, regex:Boolean) {


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

        sub = x.subscribe({ area.setStyleSpans(0, computHighlighting(area.text)) })
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

    // This is a comment
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

        val content = if(regex) word else Pattern.quote(word)

        return if(!case) Pattern.compile("(?<SEARCH>$content)", Pattern.CASE_INSENSITIVE) else Pattern.compile("(?<SEARCH>$content)")
    }

    private val patternCompiler = Pattern.compile(
            "(?<PAREN>" + HighlighterStatics.PAREN_PATTERN + ")"
                    //    + "|(?<BRACE>" + HighlighterStatics.BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + HighlighterStatics.BRACKET_PATTERN + ")"
                    + "|(?<STRING>" + HighlighterStatics.STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + HighlighterStatics.COMMENT_PATTERN + ")"
                    + "|(?<VARS>" + HighlighterStatics.VAR_PATTERN + ")"
                    + "|(?<KEYWORDS>" + HighlighterStatics.joinBoundaryPattern(HighlighterStatics.KEYWORDS) + ")")
}

object HighlighterStatics {

    val KEYWORDS = arrayOf("set" +
            "", "if", "stop", "loop", "trigger", "permission", "permission-message", "description", "return", "function", "options", "true", "false", "cancel", "else", "else if")
    const val COMMENT_PATTERN = "#[^\n]*"
    const val VAR_PATTERN = "\\{\\S*}"
    const val PAREN_PATTERN = "\\(|\\)"
    const val BRACE_PATTERN = "\\{|\\}"
    const val BRACKET_PATTERN = "\\[|\\]"
    const val STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\""
    fun joinBoundaryPattern(items: Array<String>) = "\\b(" + items.joinToString("|") + ")\\b"


}