package com.skide.include

import java.util.*
import kotlin.collections.HashMap

enum class DocType{
    EVENT,
    CONDITION,
    EFFECTS,
    EXPRESSION,
    TYPE
}

class Addon(val id: Long, val name: String, val author: String, val versions: HashMap<String, Vector<AddonItem>> = HashMap()){
    override fun toString() = name
}

data class AddonItem(val id: Int, val name: String, val type: DocType, val addon: Addon, val reviewed: String, val version: String = "", val pattern: String = "", val plugin: String = "", val eventValues: String = "", val changers: String = "", val tags: String = "", val returnType: String = "")