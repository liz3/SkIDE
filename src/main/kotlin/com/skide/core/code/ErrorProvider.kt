package com.skide.core.code

import com.skide.include.*
import com.skide.utils.EditorUtils
import javafx.application.Platform
import netscape.javascript.JSObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.collections.HashMap


class ErrorProvider(val manager: CodeManager) {

    private val variablePattern = Pattern.compile("\\{([^{}]|%\\{|}%)+}")!!
    private val errors = Vector<SkError>()
    private val area = manager.area

    fun runChecks(parseResult: Vector<Node>) {
        errors.clear()
        val calls = Vector<() -> Unit>()
        val variables = EditorUtils.filterByNodeType(NodeType.SET_VAR, parseResult)
        val variablesFromOptions = EditorUtils.filterByNodeType(NodeType.OPTIONS, parseResult)
        val globalVars = Vector<String>()
        for (variable in variables) {
            if (variable.fields["visibility"] == "global") {
                val str =
                        if (variable.fields["hasPath"] == true)
                            "{${(variable.fields["name"] as String).split("::").first()}}"
                        else
                            "{${variable.fields["name"]}}"

                if (!globalVars.contains(str)) globalVars.add(str)
            }
        }
        for (variable in variablesFromOptions) {
            for (child in variable.childNodes) {
                if (child.getContent().isNotEmpty() && child.getContent().isNotBlank()) {
                    val t = child.getContent().split(":").first().trim()
                    globalVars.add("{@$t}")
                }
            }
        }
        for (node in EditorUtils.flatList(parseResult)) {
            if (node.nodeType == NodeType.FUNCTION ||
                    node.nodeType == NodeType.SET_VAR || node.nodeType == NodeType.EVENT || node.nodeType == NodeType.OPTIONS) continue
            val matcher = variablePattern.matcher(node.raw)

            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()
                var text = matcher.group()
                if (!text.startsWith("{_")) {
                    if (text.contains("::")) text = text.split("::").first() + "}"
                    if (!globalVars.contains(text)) {
                        var cont = true
                        var c = start
                        while (c >= 0) {
                            if(node.raw[c] == '"') {
                                cont = false
                                break
                            }
                            c--
                        }
                       if(cont) {
                           calls += {
                               report(node.linenumber, SkError(node.linenumber, node.linenumber, start + 1, end + 1, ErrorSeverity.ERROR, "Variable $text not found!"))
                           }
                       }
                    }
                }
            }
        }
        EditorUtils.filterByNodeType(NodeType.FUNCTION, parseResult).forEach {
            val flatList = EditorUtils.flatList(it.childNodes)
            val parameter = it.fields["params"] as Vector<MethodParameter>
            run {
                val paramMap = HashMap<String, Int>()
                parameter.forEach { param ->
                    paramMap["{_${param.name}}"] = 0
                }
                for (node in flatList) {
                    for (key in paramMap.keys) {
                        if (node.getContent().contains(key)) paramMap[key] = paramMap[key]!! + 1
                    }
                }
                for (key in paramMap.keys) {
                    if (paramMap[key] == 0) {
                        val braceIndex = it.raw.indexOf("(")
                        val name = key.substring(2, key.length - 1)
                        calls += {
                            report(it.linenumber, SkError(it.linenumber, it.linenumber, it.raw.indexOf(name, braceIndex) + 1,
                                    it.raw.indexOf(name, braceIndex) + name.length + 1, ErrorSeverity.INFO, "Local parameter $name never used"))
                        }
                    }
                }
            }
            run {
                val vars = Vector<String>()
                for (methodParameter in parameter) {
                    val name = methodParameter.name
                    val str = "{_$name}"
                    if (!vars.contains(str)) vars.add(str)
                }
                for (node in EditorUtils.filterByNodeType(NodeType.SET_VAR, it)) {
                    if (node.fields["visibility"] != "local") continue
                    val name = node.fields["name"] as String
                    val str = if (name.contains("::"))
                        "{_${name.split("::").first()}}"
                    else
                        "{_$name}"
                    if (!vars.contains(str)) {
                        vars.add(str)
                    } else {
                        for (methodParameter in parameter) {
                            val paramName = methodParameter.name
                            if(paramName == name) {
                                val index = node.raw.indexOf(str)
                                calls += {
                                    report(node.linenumber, SkError(node.linenumber, node.linenumber, index + 1,
                                            index + str.length + 1, ErrorSeverity.WARNING, "Shadowed variable from function parameter $name"))

                                }
                            }
                        }

                    }
                }
                for (node in flatList) {
                    if (node.nodeType == NodeType.COMMENT) continue
                    val matcher = variablePattern.matcher(node.raw)

                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        var group = matcher.group()
                        if(group.contains("::")) group = group.split("::").first() + "}"
                        if (!group.startsWith("{_")) continue
                        if (!vars.contains(group)) {
                            var cont = true
                            var c = start
                            while (c >= 0) {
                                if(node.raw[c] == '"') {
                                    cont = false
                                    break
                                }
                                c--
                            }
                            if(cont) {
                                calls += {
                                    report(node.linenumber, SkError(node.linenumber, node.linenumber, start + 1,
                                            end + 1, ErrorSeverity.ERROR, "Local variable $group not found!"))
                                }
                            }
                        }
                    }
                }
            }
        }
        EditorUtils.filterByNodeType(NodeType.EVENT, parseResult).forEach {
            if (it.fields.containsKey("invalid") && it.fields["invalid"] == true) {
                calls += {
                    reportLineWarning("Event not found!", it.linenumber)
                }
            }
            val flatList = EditorUtils.flatList(it.childNodes)
            run {
                val vars = Vector<String>()
                for (node in EditorUtils.filterByNodeType(NodeType.SET_VAR, it)) {
                    if (node.fields["visibility"] != "local") continue
                    val name = node.fields["name"] as String
                    val str = if (name.contains("::"))
                        "{_${name.split("::").first()}}"
                    else
                        "{_$name}"
                    if (!vars.contains(str)) {
                        vars.add(str)
                    }
                }
                for (node in flatList) {
                    if (node.nodeType == NodeType.COMMENT || node.nodeType == NodeType.SET_VAR) continue
                    val matcher = variablePattern.matcher(node.raw)
                    while (matcher.find()) {
                        val start = matcher.start()
                        val end = matcher.end()
                        var group = matcher.group()
                        if(group.contains("::")) group = group.split("::").first() + "}"
                        if (!group.startsWith("{_")) continue
                        if (!vars.contains(group)) {
                            var cont = true
                            var c = start
                            while (c >= 0) {
                                if(node.raw[c] == '"') {
                                    cont = false
                                    break
                                }
                                c--
                            }
                            if(cont) {
                                calls += {
                                    report(node.linenumber, SkError(node.linenumber, node.linenumber, start + 1,
                                            end + 1, ErrorSeverity.ERROR, "Local variable $group not found!"))
                                }
                            }
                        }
                    }
                }
            }
        }
        Platform.runLater {
            calls.forEach { it() }
            pushErrors()
        }
    }

    fun reportLineError(message: String, line: Int) {
        errors.add(SkError(line, line, 1, area.getColumnLineAmount(line), ErrorSeverity.ERROR, message))
    }

    fun reportLineWarning(message: String, line: Int) {
        errors.add(SkError(line, line, 1, area.getColumnLineAmount(line), ErrorSeverity.WARNING, message))
    }

    fun reportLineInfo(message: String, line: Int) {
        errors.add(SkError(line, line, 1, area.getColumnLineAmount(line), ErrorSeverity.INFO, message))
    }

    fun report(line: Int, error: SkError) {
        errors.add(error)
    }

    private fun pushErrors() {
        val array = area.getArray()
        var counter = 0
        errors.forEach {
            array.setSlot(counter, area.createObjectFromMap(hashMapOf(
                    Pair("startLineNumber", it.startLine),
                    Pair("endLineNumber", it.endLine),
                    Pair("startColumn", it.startColumn),
                    Pair("endColumn", it.endColumn),
                    Pair("message", it.message),
                    Pair("severity", it.severity.num))))
            counter++
        }
        val obj = area.engine.executeScript("monaco.editor") as JSObject
        obj.call("setModelMarkers", area.getModel(), area.getModel().call("getModeId"), array)

    }
}