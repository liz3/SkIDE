package com.skide.core.management

import com.skide.CoreManager
import com.skide.include.NodeType
import com.skide.include.Snippet
import com.skide.include.SnippetRule
import com.skide.utils.readFile
import com.skide.utils.writeFile
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class SnippetManager(val coreManager: CoreManager) {

    val snippets = Vector<Snippet>()
    private var loaded = false
    val config = coreManager.configManager

    fun prepare() {
        if (loaded) return
        loaded = true

        loadSnippets()
    }

    private fun getRule(obj: JSONObject): SnippetRule {

        val types = Vector<NodeType>()
        for (any in obj.getJSONArray("types")) {
            any as String
            types.add(NodeType.valueOf(any))
        }
        val startsWith = obj.getJSONObject("start").getBoolean("set")
        val contains = obj.getJSONObject("contains").getBoolean("set")
        val endsWith = obj.getJSONObject("end").getBoolean("set")
        val startPair = Pair(startsWith,
                if (startsWith) obj.getJSONObject("start").getString("value") else "")
        val containsPair = Pair(contains,
                if (contains) obj.getJSONObject("contains").getString("value") else "")
        val endPair = Pair(endsWith,
                if (endsWith) obj.getJSONObject("end").getString("value") else "")
        return SnippetRule(types, startPair, containsPair, endPair)
    }

    private fun setRule(rule: SnippetRule): JSONObject {
        val obj = JSONObject()
        val typesArr = JSONArray()
        for (allowedType in rule.allowedTypes) {
            typesArr.put(allowedType)
        }
        obj.put("types", typesArr)
        val startObj = JSONObject()
        startObj.put("set", rule.startsWith.first)
        startObj.put("value", rule.startsWith.second)
        obj.put("start", startObj)

        val containsObj = JSONObject()
        containsObj.put("set", rule.contains.first)
        containsObj.put("value", rule.contains.second)
        obj.put("contains", containsObj)

        val endObj = JSONObject()
        endObj.put("set", rule.endsWith.first)
        endObj.put("value", rule.endsWith.second)
        obj.put("end", endObj)

        return obj
    }

    fun saveSnippets() {
        val arr = JSONArray()
        for (s in snippets) {
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("name", s.name)
            obj.put("label", s.label)
            obj.put("replace_sequence", s.triggerReplaceSequence)
            obj.put("content", s.insertText)
            obj.put("root_rule", setRule(s.rootRule))
            obj.put("parent_rule", setRule(s.parentRule))
            arr.put(obj)
        }
        writeFile(arr.toString().toByteArray(), config.snippetsFile, false)
    }

    private fun loadSnippets() {

        val arr = JSONArray(readFile(config.snippetsFile).second)

        for (entry in arr) {
            entry as JSONObject

            val id = entry.getLong("id")
            val name = entry.getString("name")
            val label = entry.getString("label")
            val content = entry.getString("content")
            val triggerReplaceSequence = entry.getBoolean("replace_sequence")

            val rootRules = getRule(entry.getJSONObject("root_rule"))
            val parentRules = getRule(entry.getJSONObject("parent_rule"))


            snippets.add(Snippet(id, name, label, content, rootRules, parentRules, triggerReplaceSequence))

        }
    }
}