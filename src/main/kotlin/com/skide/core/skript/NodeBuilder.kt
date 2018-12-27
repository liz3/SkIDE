package com.skide.core.skript

import com.skide.Info
import com.skide.include.*
import com.skide.utils.EditorUtils
import java.util.*
import java.util.regex.Pattern

class NodeBuilder(val node: Node) {

    val content = node.raw.trim().replace("\r", "").replace("\t", "")
    var hasComment = false
    var commentPart = ""
    val fields = HashMap<String, Any>()
    val parent = node.parent

    init {
        var inBrace = false
        val arr = content.toCharArray()
        if (node.nodeType != NodeType.COMMENT) {

            for (x in 0 until content.length) {
                if (arr[x] == '"') {
                    inBrace = !inBrace
                }
                if (arr[x] == '#' && !inBrace) {
                    hasComment = true
                    commentPart = content.substring(x)
                    break
                }
            }
        }
    }

    fun getType(): NodeType {


        var theType = NodeType.UNDEFINED
        if (parent != null && parent.nodeType == NodeType.OPTIONS) {
            NodeType.OPTION
        } else if (content.toLowerCase().startsWith("options:")) {
            theType = NodeType.OPTIONS
        }
        val matcher = Pattern.compile("[^\\>\\[( {]+\\(.*\\)(?!.*\")").matcher(content)
        if (matcher.find()) {
            theType = NodeType.FUNCTION_CALL
            fields["name"] = matcher.group().split("(").first().trim()
        }
        if (content.toLowerCase().startsWith("function ")) {
            //parse method stuff
            parseMethodParameters()
            theType = NodeType.FUNCTION
        }
        if (content.toLowerCase().startsWith("every ")) {
            theType = NodeType.INTERVAL
        }

        if (theType == NodeType.UNDEFINED && node.tabLevel == 0 && content.isNotEmpty() && content.isNotBlank()) {
            theType = NodeType.EVENT


            var found = false
            val available = Vector<AddonItem>()
            for (event in node.parser.events) {
                if (EditorUtils.fullFillsEventRequirements(event.requirements, content)) available.add(event)
            }
            if (available.size > 0) {
                var lastHit = 0
                var highest = available.firstElement()
                if (available.size == 1) {
                    fields["event"] = highest
                    fields["name"] = highest.name
                    found = true
                } else {
                    for (addonItem in available) {
                        var hits = 0
                        content.replace(":", "").split(" ").forEach {
                            if (addonItem.pattern.contains(it)) hits++
                        }
                        if (hits > lastHit) {
                            highest = addonItem
                            lastHit = hits
                        }
                    }
                    fields["event"] = highest
                    fields["name"] = highest.name
                    found = true
                }
                available.clear()
            }
            if (!found) {
                fields["name"] = "Unknown Event"
                fields["invalid"] = true
            }
        }

        if (content.toLowerCase().startsWith("command ")) {
            fields.put("name", content.split(" ")[1].replace("/", "").replace(":", ""))
            theType = NodeType.COMMAND
        }
        if (content.toLowerCase().startsWith("#")) {
            theType = NodeType.COMMENT
        }
        if (content.toLowerCase().startsWith("if ")) {
            theType = NodeType.IF_STATEMENT
        }
        if (content.toLowerCase().startsWith("else ") || content.toLowerCase().startsWith("else if")) {
            theType = NodeType.ELSE_STATEMENT
        }
        if (content.toLowerCase().startsWith("loop ")) {
            theType = NodeType.LOOP
        }
        if (content.toLowerCase().startsWith("while ")) {
            theType = NodeType.LOOP
        }
        if (content.toLowerCase().startsWith("trigger:")) {
            theType = NodeType.TRIGGER
        }
        if (content.toLowerCase().startsWith("class ")) {
            fields.put("name", content.split(" ")[1].replace(":", ""))
            theType = NodeType.CLASS

        }
        if (content.toLowerCase().startsWith("stop ")) {
            theType = NodeType.STOP
        }
        if (content.toLowerCase().startsWith("set {")) {
            try {
                //get var name
                val pattern = Pattern.compile("\\{([^{}]|%\\{|}%)+}").matcher(content)

                if (pattern.find()) {

                    var  name = pattern.group()
                    when {
                        name.startsWith("{_") -> fields["visibility"] = "local"
                        name.startsWith("{@") -> {
                            fields["visibility"] = "global"
                            fields["from_option"] = true
                        }
                        else -> fields["visibility"] = "global"
                    }
                    if(name.contains("::")) name = name.substring(0, name.indexOf("::"))
                    if (name.startsWith("{_") || name.startsWith("{@")) {

                        fields["name"] = name.substring(2, name.length - 1)
                    } else {
                        fields["name"] = name.substring(1, name.length - 1)

                    }

                    if (content.contains("::")) {
                        val listOrMapPath = content.split(name)[1].substring(3).split("}").first().split("::")
                        fields["path"] = listOrMapPath
                        fields["hasPath"] = true
                    }
                }


            } catch (e: Exception) {
                e.printStackTrace()
            }
            theType = NodeType.SET_VAR
        }
        if (theType == NodeType.UNDEFINED && content != "") theType = NodeType.STATEMENT

        return theType
    }

    private fun parseMethodParameters() {
        val paramList = Vector<MethodParameter>()
        if (!content.contains("(") || !content.contains(")")) {
            fields["invalid"] = true
            return
        }
        val name = content.split(" ")[1].split("(")[0]
        val params = content.split("(")[1].split(")")[0].split(",")
        val returnType: String
        params.forEach {

            if (it != "" && it.contains(":")) {
                val paramName = it.trim().split(":").first()
                var paramType = it.trim().split(":")[1]
                var value = ""
                if (paramType.contains("=")) {
                    value = paramType.split("=")[1].trim().replace("\"", "")
                    paramType = paramType.split("=").first().trim()
                }
                paramList.add(MethodParameter(paramName, paramType, value))
            }
        }
        fields["name"] = name
        fields["params"] = paramList

        if (content.contains("::")) {
            returnType = content.split("::")[1].trim().replace(":", "")
            fields["return"] = returnType
        } else {

            fields["return"] = "void"
        }

        fields["ready"] = true
    }
}