package com.skide.include

import com.skide.core.skript.NodeBuilder
import com.skide.core.skript.SkriptParser
import java.util.*

enum class NodeType {
    STATEMENT,
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
    FUNCTION_CALL,
    INTERVAL
}

enum class ErrorSeverity(val num: Int) {
    HINT(1),
    INFO(2),
    WARNING(4),
    ERROR(8)
}

class SkErrorItem(val error: SkError, val cb: () -> Unit)
class SkErrorFront(val value: Any) {

    override fun toString(): String {

        if (value is SkErrorItem)
            return "${value.error.severity} [${value.error.startLine}]: ${value.error.message}"
        return value as String
    }

}

class SkError(val startLine: Int, val endLine: Int, val startColumn: Int, val endColumn: Int, val severity: ErrorSeverity, val message: String)

class MethodParameter(val name: String, val type: String, val value: String)

class Node(val parser: SkriptParser, val parent: Node? = null, val raw: String, var tabLevel: Int, val linenumber: Int, val childNodes: Vector<Node> = Vector()) {
    private val builder = NodeBuilder(this)
    val nodeType = builder.getType()
    override fun toString() = builder.content
    val fields = builder.fields
    lateinit var events: Vector<AddonItem>
    fun getContent() = builder.content
    fun hasComment() = builder.hasComment
}