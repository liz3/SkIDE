package com.skide.include

import java.util.*
import kotlin.collections.HashMap

enum class NodeType {
    EXECUTION,
    IF_STATEMENT,
    ELSE_STATEMENT,
    COMMAND,
    EVENT,
    OPTIONS,
    TRIGGER,
    SET_VAR,
    COMMENT,
    LOOP,
    STOP,
    ROOT,
    OPTION,
    FUNCTION,
    UNDEFINED,
    INVALID,
    CLASS
}

class MethodParameter(val name: String, val type: String, val value: String)

class Node(val parent: Node? = null, val raw: String, var tabLevel: Int, val linenumber: Int, val childNodes: Vector<Node> = Vector()) {

    val content = raw.trim().replace("\r", "").replace("\t", "")
    var hasComment = false
    var commentPart = ""
    val fields = HashMap<String, Any>()

    val nodeType: NodeType = {

        var theType = NodeType.UNDEFINED
        if (parent != null && parent.nodeType == NodeType.OPTIONS) {
            NodeType.OPTION
        } else if (content.toLowerCase().startsWith("options:")) {
            theType = NodeType.OPTIONS
        }
        if (content.toLowerCase().startsWith("function ")) {
            //parse method stuff
            parseMethodParameters()
            theType = NodeType.FUNCTION
        }
        if (content.toLowerCase().startsWith("command ")) {
            fields.put("name", content.split(" ")[1].replace("/", "").replace(":", ""))
            theType = NodeType.COMMAND
        }
        if (content.toLowerCase().startsWith("#")) {
            theType = NodeType.COMMENT
        }
        if (content.toLowerCase().startsWith("on ")) {
            fields.put("name", content.replace(":", "").replace("on ", ""))
            theType = NodeType.EVENT
        }
        if (content.toLowerCase().startsWith("if ")) {
            theType = NodeType.IF_STATEMENT
        }
        if (content.toLowerCase().startsWith("else ")) {
            theType = NodeType.ELSE_STATEMENT
        }
        if (content.toLowerCase().startsWith("loop ")) {
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


                if (content.substring(3).trim().startsWith("{{")) {
                    val name = content.split("{")[2].split("}").first()
                    println(name)
                    when {
                        name.startsWith("_") -> fields.put("visibility", "local")
                        name.startsWith("@") -> {
                            fields["visibility"] = "global"
                            fields["from_option"] = true
                        }
                        else -> fields["visibility"] = "global"
                    }
                    if (name.startsWith("_") || name.startsWith("@")) {

                        fields["name"] = name.substring(1)
                    } else {
                        fields["name"] = name

                    }
                    fields["set_value"] = content.split("to")[1]


                    if (content.contains("::")) {
                        val listOrMapPath = content.split(name)[1].substring(3).split("}").first().split("::")
                        println(listOrMapPath)
                        fields["path"] = listOrMapPath
                    }

                } else {
                    val name = content.split("{")[1].split("}").first()

                    when {
                        name.startsWith("_") -> fields.put("visibility", "local")
                        name.startsWith("@") -> {
                            fields.put("visibility", "global")
                            fields.put("from_option", true)
                        }
                        else -> fields.put("visibility", "global")
                    }
                    if (name.startsWith("_") || name.startsWith("@")) {

                        fields.put("name", name.substring(1))
                    } else {
                        fields.put("name", name)

                    }
                    fields.put("set_value", content.split("to")[1])
                    if (content.contains("::")) {
                        val listOrMapPath = content.split(name)[1].substring(3).split("}").first().split("::")
                        println(listOrMapPath)
                        fields.put("path", listOrMapPath)
                    }
                }
            } catch (e: Exception) {
                fields.put("visibility", "global")
                fields.put("name", "")
                fields.put("invalid", true)
            }
            theType = NodeType.SET_VAR
        }
        if (theType == NodeType.UNDEFINED && content != "") theType = NodeType.EXECUTION
        theType
    }.invoke()

    init {
        var inBrace = false
        val arr = content.toCharArray()
        if (nodeType != NodeType.COMMENT) {

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

    private fun parseMethodParameters() {
        val paramList = Vector<MethodParameter>()
        if (!content.contains("(") || !content.contains(")")) return
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