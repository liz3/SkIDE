package com.skide.include

import java.util.*
import kotlin.collections.HashMap

class ColorRule(val foreground:String, val background:String, val style:String)

class ColorScheme(val name:String, val base:String, val colors: HashMap<String, Any>, val rules: HashMap<String, ColorRule>) {
    override fun toString(): String {
        if(name == "vs" || name == "vs-dark") return name
        return "$name > $base"
    }
}

