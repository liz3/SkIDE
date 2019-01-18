package com.skide.core.code

import com.skide.include.*
import com.skide.utils.EditorUtils
import javafx.application.Platform
import netscape.javascript.JSObject
import java.util.*
import java.util.regex.Pattern
import kotlin.collections.HashMap


class ErrorProvider(val manager: CodeManager) {

    private val variablePattern = Pattern.compile("\\{([^{}]|%\\{|}%)+}")!!
    private val callPatterns = Pattern.compile("[^\\>\\[( {&]+\\(.*\\)(?!.*\")")!!
    private val errors = Vector<SkError>()
    private val area = manager.area

    private fun checkForQuote(raw: String, start: Int): Boolean {

        var c = start
        while (c >= 0) {
            if (raw[c] == '"') {
                return false

            }
            c--
        }
        return true
    }

    fun runChecks(parseResult: Vector<Node>) {
        errors.clear()
        var ignoreCase = false
        var ignoreMissingFuncs = false
        for (node in parseResult) {
            if (node.nodeType == NodeType.COMMENT && node.getContent().contains("@skide:ignore-case")) ignoreCase = true
            if (node.nodeType == NodeType.COMMENT && node.getContent().contains("@skide:ignore-missing-functions")) ignoreMissingFuncs = true
            if (node.nodeType == NodeType.COMMENT && node.getContent().contains("@skide:ignore-all")) {
                Platform.runLater {
                    pushErrors()
                }
                return
            }
        }
        val calls = Vector<() -> Unit>()
        val variables = EditorUtils.filterByNodeType(NodeType.SET_VAR, parseResult)
        val variablesFromOptions = EditorUtils.filterByNodeType(NodeType.OPTIONS, parseResult)
        val globalVars = Vector<String>()
        val funcs = Vector<Node>()
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
        val allNodes = EditorUtils.flatList(parseResult)
        for (node in allNodes) {


            if (node.nodeType == NodeType.FUNCTION) {
                if (node.fields["invalid"] == true) continue
                funcs.add(node)
                val flatList = EditorUtils.flatList(node.childNodes)
                val parameter = node.fields["params"] as Vector<*>
                //Determine not used Local variables
                run {
                    val paramMap = HashMap<String, Int>()
                    parameter.forEach { param ->
                        param as MethodParameter
                        paramMap["{_${param.name}}"] = 0
                    }
                    for (childNodes in flatList) {
                        for (key in paramMap.keys) {
                            if (childNodes.getContent().contains(key, ignoreCase)) paramMap[key] = paramMap[key]!! + 1
                        }
                    }
                    for (key in paramMap.keys) {
                        if (paramMap[key] == 0) {
                            val braceIndex = node.raw.indexOf("(")
                            val name = key.substring(2, key.length - 1)
                            calls += {
                                report(SkError(node.linenumber, node.linenumber, node.raw.indexOf(name, braceIndex) + 1,
                                        node.raw.indexOf(name, braceIndex) + name.length + 1, ErrorSeverity.INFO, "Local parameter $name never used"))
                            }
                        }
                    }
                }

                run {
                    val vars = Vector<String>()
                    for (methodParameter in parameter) {
                        methodParameter as MethodParameter
                        val name = methodParameter.name
                        val str = "{_$name}"
                        if (!vars.contains(str)) vars.add(str)
                    }
                    //Check for shadowed vars
                    for (childNode in EditorUtils.filterByNodeType(NodeType.SET_VAR, node)) {
                        if (childNode.fields["visibility"] != "local") continue
                        val name = childNode.fields["name"] as String
                        val str = (if (name.contains("::"))
                            "{_${name.split("::").first()}}"
                        else
                            "{_$name}")
                        if (!vars.contains(str)) {
                            vars.add(str)
                        } else {
                            for (methodParameter in parameter) {
                                methodParameter as MethodParameter

                                val paramName = methodParameter.name
                                if (paramName == name) {
                                    val index = childNode.raw.indexOf(str)
                                    calls += {
                                        report(SkError(childNode.linenumber, childNode.linenumber, index + 1,
                                                index + str.length + 1, ErrorSeverity.WARNING, "Shadowed variable from function parameter $name"))
                                    }
                                }
                            }

                        }
                    }

                    if (!node.raw.contains("@skide:suppress-vars")) {
                        //check for missing variables
                        for (childNode in flatList) {
                            if (childNode.nodeType == NodeType.COMMENT) continue

                            val matcher = variablePattern.matcher(childNode.raw)

                            while (matcher.find()) {
                                val start = matcher.start()
                                val end = matcher.end()
                                val group = matcher.group()
                                if (!group.startsWith("{_") || group.contains("::")) continue
                                //    if () group = "${group.split("::").first()}}"
                                var found = false
                                for (v in vars)
                                    if (v.contains(group, ignoreCase)) {
                                        found = true
                                        break
                                    }
                                if (!found) {
                                    if (checkForQuote(childNode.raw, start))
                                        calls += {
                                            report(SkError(childNode.linenumber, childNode.linenumber, start + 1, end + 1, ErrorSeverity.ERROR, "Local variable $group not found!"))
                                        }
                                }

                            }

                        }
                    }
                }
                var found = false
                for (childNode in allNodes) {
                    if (childNode == node) continue
                    if (childNode.nodeType != NodeType.COMMENT && childNode.nodeType != NodeType.COMMAND) {
                        if (childNode.getContent().replace(" ", "").contains("${node.fields["name"]}(")) {
                            found = true
                        }
                    }
                }
                if (manager.area.coreManager.configManager.get("cross_auto_complete") == "true" && !found) {

                    for ((key, value) in manager.crossNodes) {
                        if (key == area.file) continue
                        for (childNode in value) {
                            if (childNode.tabLevel == 0) continue
                            if (childNode.getContent().replace(" ", "").contains("${node.fields["name"]}(")) {
                                found = true
                            }
                        }
                    }
                }

                if (!found) {
                    calls += {
                        reportLineWarning("Function: ${node.fields["name"]} never used", node.linenumber)
                    }
                }
            } else if (node.nodeType == NodeType.EVENT) {
                //Check if event is known
                if (node.fields.containsKey("invalid") && node.fields["invalid"] == true)
                    calls += {
                        reportLineWarning("Event not found!", node.linenumber)
                    }
                val flatList = EditorUtils.flatList(node.childNodes)
                run {
                    val vars = Vector<String>()
                    for (childNode in EditorUtils.filterByNodeType(NodeType.SET_VAR, node)) {
                        if (childNode.fields["visibility"] != "local") continue
                        val name = childNode.fields["name"] as String
                        val str = if (name.contains("::"))
                            "{_${name.split("::").first()}}"
                        else
                            "{_$name}"
                        if (!vars.contains(str)) {
                            vars.add(str)
                        }
                    }
                    //check for local vars
                    for (childNode in flatList) {
                        if (childNode.nodeType == NodeType.COMMENT || childNode.nodeType == NodeType.SET_VAR) continue
                        /*
                        val matcher = variablePattern.matcher(childNode.raw)
                            while (matcher.find()) {
                            val start = matcher.start()
                            val end = matcher.end()
                            var group = matcher.group()
                            if (!group.startsWith("{_")) continue
                            if (group.contains("::")) group = group.split("::").first() + "}"
                            if (!vars.contains(group))
                                if (checkForQuote(childNode.raw, start))
                                    calls += {
                                        report(childNode.linenumber, SkError(childNode.linenumber, childNode.linenumber, start + 1,
                                                end + 1, ErrorSeverity.ERROR, "Local variable $group not found!"))
                                    }
                        }
                         */
                    }
                }


            }
        }
        for ((nIndex, node) in allNodes.withIndex()) {
            if (node.nodeType == NodeType.FUNCTION ||
                    node.nodeType == NodeType.IF_STATEMENT || node.nodeType == NodeType.EVENT || node.nodeType == NodeType.LOOP || node.nodeType == NodeType.OPTIONS || node.nodeType == NodeType.COMMAND) {

                if (if (node.hasComment()) !node.getContent().substring(0, node.getContent().lastIndexOf("#")).trim().endsWith(":") else !node.getContent().endsWith(":")) {
                    calls += {
                        reportLineError("Does not end with :", node.linenumber)
                    }
                }

            }
            if (node.nodeType != NodeType.UNDEFINED && node.nodeType != NodeType.COMMENT && !ignoreMissingFuncs && !node.raw.contains("@skide:ignore-missing-functions")) {

                val matcher = callPatterns.matcher(node.raw)
                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    val name = matcher.group().split("(").first().trim()
                    var found = false
                    for (func in funcs) {
                        if (func.fields["name"] == name) {
                            found = true
                            break
                        }
                    }
                    if (manager.area.coreManager.configManager.get("cross_auto_complete") == "true" && !found) {
                        for (value in manager.crossNodes.values) {
                            for (sNode in EditorUtils.filterByNodeType(NodeType.FUNCTION, value)) {
                                if (sNode.fields["name"] == name) {
                                    found = true
                                    break
                                }
                            }
                            if (found) break
                        }
                    }
                    if (!found) {

                        calls += {
                            report(SkError(node.linenumber, node.linenumber, start + 1, end + name.length + 1, ErrorSeverity.ERROR, "Function $name not found"))
                        }

                    }
                }
            }
            /*
                     run {
                val matcher = variablePattern.matcher(node.raw)
                while (matcher.find()) {
                    val start = matcher.start()
                    val end = matcher.end()
                    var text = matcher.group()
                    if (!text.startsWith("{_")) {
                        if (text.contains("::")) text = text.split("::").first() + "}"
                        if (!globalVars.contains(text))
                            if (checkForQuote(node.raw, start))
                                calls += {
                                    report(node.linenumber, SkError(node.linenumber, node.linenumber, start + 1, end + 1, ErrorSeverity.ERROR, "Variable $text not found!"))
                                }
                    }
                }
            }
             */
            if (node.nodeType == NodeType.IF_STATEMENT || node.nodeType == NodeType.LOOP) {
                if (node != allNodes.last()) {
                    if (allNodes[nIndex + 1].tabLevel <= node.tabLevel && allNodes[nIndex + 1].nodeType != NodeType.UNDEFINED)
                        calls += {
                            reportLineWarning("Empty Block", node.linenumber)
                        }

                } else {
                    calls += {
                        reportLineWarning("Empty Block", node.linenumber)
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

    fun report(error: SkError) {
        errors.add(error)
    }

    private fun pushErrors() {
        area.openFileHolder.openProject.errorFrontEnd.update(area.file, errors, area)
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