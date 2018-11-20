package com.skide.include
import com.skide.core.skript.NodeBuilder
import java.util.*

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
    private val builder = NodeBuilder(this)
    val nodeType = builder.getType()
    override fun toString() = builder.content
    val fields = builder.fields
}