package com.skriptide.include

import java.util.*
import kotlin.collections.HashMap

enum class NodeType {
    EXECUTION,
    IF_STATEMENT,
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
    UNDEFINED
}

class MethodParameter(val name: String, val type: String, val value:String)

class Node(val parent: Node? = null, val content: String, var tabLevel: Int, val linenumber: Int, val childNodes: Vector<Node> = Vector()) {

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
            fields.put("name", content.split(" ")[1].replace("/","").replace(":",""))
            theType = NodeType.COMMAND
        }
        if (content.toLowerCase().startsWith("#")) {
            theType = NodeType.COMMENT
        }
        if (content.toLowerCase().startsWith("on ")) {
            fields.put("name", content.split(" ")[1].replace(":",""))
            theType = NodeType.EVENT
        }
        if (content.toLowerCase().startsWith("if ")) {
            theType = NodeType.IF_STATEMENT
        }
        if (content.toLowerCase().startsWith("loop ")) {
            theType = NodeType.LOOP
        }
        if (content.toLowerCase().startsWith("stop ")) {
            theType = NodeType.LOOP
        }
        if (content.toLowerCase().startsWith("set ")) {
            try {
                //get var name
                fields.put("name", content.split("{")[1].split("}").first())
            } catch (e: Exception) {
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
        val name = content.split(" ")[1].split("(")[0]
        val params = content.split("(")[1].split(")")[0].split(",")
        var returnType = "void"
        params.forEach {

           if(it != "") {
               val paramName = it.trim().split(":").first()
               var paramType = it.trim().split(":")[1]
               var value = ""
               if(paramType.contains("=")) {
                   value = paramType.split("=")[1].trim().replace("\"","")
                   paramType = paramType.split("=").first().trim()
               }
               paramList.add(MethodParameter(paramName, paramType, value))
           }
        }
        fields.put("name", name)
        fields.put("params", paramList)

        if (content.contains("::")) {
            returnType = content.split("::")[1].trim().replace(":", "")
            fields.put("return", returnType)
        }
        fields.put("return", "void")
    }
}