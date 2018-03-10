package com.skide.utils

import com.skide.CoreManager
import com.skide.include.Addon
import com.skide.include.AddonItem
import com.skide.include.DocType
import org.json.JSONObject
import java.io.File
import java.util.*

class ResourceManager(val coreManager: CoreManager) {

    val addons = HashMap<String, Addon>()
    val file = File(coreManager.configManager.rootFolder, "docs.json")
    val addonsFile = File(coreManager.configManager.rootFolder, "addons.json")

    init {

    }

    fun loadResources() {

        if (file.exists()) {
            file.delete()
        }

        downloadFile("https://liz3.net/sk/?function=getAllSyntax", file.absolutePath)
        downloadFile("https://liz3.net/sk/?function=getAllAddons", addonsFile.absolutePath)


        val str = readFile(file.absolutePath)

        val addonDevs = JSONObject(readFile(addonsFile).second).getJSONArray("result")
        val obj = JSONObject(str.second)


        obj.getJSONArray("result").forEach {
            it as JSONObject
            val name = it.getString("name")
            val addonName = it.getString("addon")
            var addonVersion = it.getString("version")
            val reviewed = if( it.has("reviewed")) it.getString("reviewed")  else ""
            val pattern = if( it.has("pattern")) it.getString("pattern")  else ""
            val plugin = if( it.has("plugin")) it.getString("plugin")  else ""
            val author = {
                var devName = ""

                addonDevs.forEach {dev ->
                   dev as JSONObject
                    if(dev.getString("addon") == addonName) devName = dev.getString("author")
                }

                devName
            }.invoke()
            val eventValues = if( it.has("eventvalues")) it.getString("eventvalues")  else ""
            val changers = if( it.has("changers")) it.getString("changers")  else ""
            val tags = if( it.has("tags")) it.getString("tags")  else ""
            val returnType = if( it.has("returntype")) it.getString("returntype")  else ""
            val id = it.getInt("id")

            if (!addons.containsKey(addonName)) addons[addonName] = Addon((addons.size + 1).toLong(), addonName, author)
            val addon = addons[addonName]!!
            if (addonVersion == null || addonVersion == "") addonVersion = "0.0.0"
            if (!addon.versions.containsKey(addonVersion)) addon.versions[addonVersion] = Vector()
            val addonVer = addon.versions[addonVersion]
            val type = when (it.getString("doc")) {
                "conditions" -> DocType.CONDITION
                "effects" -> DocType.EFFECTS
                "events" -> DocType.EVENT
                "expressions" -> DocType.EXPRESSION
                "types" -> DocType.TYPE
                else -> DocType.TYPE
            }


            addonVer!!.addElement(AddonItem(id, name,  type, addon, reviewed, addonVersion, pattern, plugin, eventValues, changers, tags, returnType))
        }
        println("")

    }
}