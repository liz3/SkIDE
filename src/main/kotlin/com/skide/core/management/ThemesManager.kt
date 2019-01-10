package com.skide.core.management

import com.skide.CoreManager
import com.skide.core.code.CodeArea
import com.skide.include.ColorRule
import com.skide.include.ColorScheme
import com.skide.utils.readFile
import com.skide.utils.writeFile
import netscape.javascript.JSObject
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import kotlin.collections.HashMap

class SchemesManager(val core: CoreManager) {

    val schemes = HashMap<String, ColorScheme>()
    val file = core.configManager.colorSchemesFile

    fun prepare() {
        Thread {
            loadSchemes()
        }.start()
    }

    fun writeSchemes() {
        val arr = JSONArray()

        for (scheme in schemes.values) {
            val obj = JSONObject()
            obj.put("name", scheme.name)
            obj.put("base", scheme.base)

            val colors = JSONObject()
            for (color in scheme.colors) {
                colors.put(color.key, color.value)
            }
            val rules = JSONObject()
            for (rule in scheme.rules) {
                val rObj = JSONObject()
                rObj.put("foreground", rule.value.foreground)
                rObj.put("style", rule.value.style)
                rules.put(rule.key, rObj)
            }
            obj.put("colors", colors)
            obj.put("rules", rules)

            arr.put(obj)
        }

        writeFile(arr.toString().toByteArray(), file, false, true)
    }

    fun registerTheme(area: CodeArea, name: String) {
        val colors = area.getObject()
        val rules = area.getArray()
        val scheme = schemes[name]!!
        for (c in scheme.colors) {
            colors.setMember(c.key, c.value)
        }
        var count = 0
        for (rule in scheme.rules) {
            val obj = area.getObject()
            obj.setMember("token", rule.key)
            if(rule.value.foreground.isNotEmpty())
                obj.setMember("foreground", rule.value.foreground)
            if(rule.value.style.isNotEmpty())
                obj.setMember("fontStyle", rule.value.style)
            rules.setSlot(count, obj)
            count++
        }
        try {
            area.getWindow().call("registerTheme", scheme.name, scheme.base, colors, rules)
        }catch (e:Exception) {
            e.printStackTrace()
        }
    }

    private fun loadSchemes() {
        val arr = JSONArray(readFile(file).second)

        for (entry in arr) {
            entry as JSONObject
            val name = entry.getString("name")
            val base = entry.getString("base")
            val scheme = ColorScheme(name, base, HashMap(), HashMap())
            val colors = entry.getJSONObject("colors")
            val rules = entry.getJSONObject("rules")

            for (key in colors.keys())
                scheme.colors[key] = colors.get(key)

            for (key in rules.keys()) {
                val e = rules.get(key) as JSONObject
                scheme.rules[key] = ColorRule(e.getString("foreground"),  e.getString("style"))
            }
            schemes[name] = scheme
        }
    }
}