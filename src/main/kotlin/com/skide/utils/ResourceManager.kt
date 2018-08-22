package com.skide.utils

import com.skide.CoreManager
import com.skide.gui.Prompts
import com.skide.include.Addon
import com.skide.include.AddonItem
import com.skide.include.DocType
import javafx.scene.control.Alert
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.*
import kotlin.collections.HashMap


class ResourceManager(val coreManager: CoreManager) {


    val addons = HashMap<String, Addon>()
    val cssFiles = HashMap<String, String>()
    val skriptDocList = Vector<AddonItem>()
    val skriptVersions = Vector<String>()
    val file = File(coreManager.configManager.rootFolder, "docs.json")
    val addonsFile = File(coreManager.configManager.rootFolder, "addons.json")
    val skriptDoc = File(coreManager.configManager.rootFolder, "skript.json")
    val skriptVersionsFile = File(coreManager.configManager.rootFolder, "skript-vers.json")
    val skriptVersionsFolder = File(coreManager.configManager.rootFolder, "skript-versions")

    private fun parseCurrentSkriptVersionDocs() {


        val read = readFile(skriptDoc)

        if (read.first == FileReturnResult.SUCCESS) {

            JSONObject(read.second).getJSONArray("result").forEach {


                val addon = Addon(-1, "Skript", "Skript")
                it as JSONObject
                val id = it.getInt("id")
                val name = it.getString("name")
                val addonVersion = it.getString("version")
                val reviewed = if (it.has("reviewed")) it.getString("reviewed") else ""
                val pattern = if (it.has("pattern")) it.getString("pattern") else ""
                val plugin = if (it.has("plugin")) it.getString("plugin") else ""
                val eventValues = if (it.has("eventvalues")) it.getString("eventvalues") else ""
                val changers = if (it.has("changers")) it.getString("changers") else ""
                val tags = if (it.has("tags")) it.getString("tags") else ""
                val returnType = if (it.has("returntype")) it.getString("returntype") else ""

                val type = when (it.getString("doc")) {
                    "conditions" -> DocType.CONDITION
                    "effects" -> DocType.EFFECTS
                    "events" -> DocType.EVENT
                    "expressions" -> DocType.EXPRESSION
                    "types" -> DocType.TYPE
                    else -> DocType.TYPE
                }


                skriptDocList.add(AddonItem(id, name, type, addon, reviewed, addonVersion, pattern, plugin, eventValues, changers, tags, returnType))

            }
        }
    }

    fun parseSkriptVersions() {
        JSONArray(readFile(skriptVersionsFile).second).forEach { skriptVersions.add(it as String) }
    }

    fun loadResources(callback: (Int, Int, String) -> Unit) {

        val total = 5

        callback(total, 1, "https://liz3.net/sk/depot/")



        if (!skriptVersionsFolder.exists()) skriptVersionsFolder.mkdir()

        if (!file.exists() || coreManager.configManager.get("meta_update") == "true") {
            try {
                callback(total, 2, "Downloading: https://liz3.net/sk/?function=getAllSyntax")
                downloadFile("https://liz3.net/sk/?function=getAllSyntax", file.absolutePath)
                callback(total, 3, "Downloading: https://liz3.net/sk/?function=getAllAddons")
                downloadFile("https://liz3.net/sk/?function=getAllAddons", addonsFile.absolutePath)
                callback(total, 4, "Downloading: https://liz3.net/sk/?function=getAddonSyntax&addon=skript")
                downloadFile("https://liz3.net/sk/?function=getAddonSyntax&addon=skript", skriptDoc.absolutePath)
                callback(total, 5, "Downloading: https://skripttools.net/api.php?t=skript&action=getlist")
                downloadFile("https://skripttools.net/api.php?t=skript&action=getlist", skriptVersionsFile.absolutePath)
            } catch (e: Exception) {

            }
        }



        callback(total, 6, "Reading docs")
        parseCurrentSkriptVersionDocs()
        callback(total, 7, "Reading script versions")
        parseSkriptVersions()


        val str = readFile(file.absolutePath)
        val obj = JSONObject(str.second)

        val addonDevs = JSONObject(readFile(addonsFile).second).getJSONArray("result")

        callback(total, 8, "Parsing Addons...")

        obj.getJSONArray("result").forEach {
            it as JSONObject
            val id = it.getInt("id")
            val name = it.getString("name")
            val addonName = it.getString("addon")

            if (addonName != "Skript" && addonName != "skript") {

                var addonVersion = it.getString("version")
                val reviewed = if (it.has("reviewed")) it.getString("reviewed") else ""
                val pattern = if (it.has("pattern")) it.getString("pattern") else ""
                val plugin = if (it.has("plugin")) it.getString("plugin") else ""
                val author = {
                    var devName = ""

                    addonDevs.forEach { dev ->
                        dev as JSONObject
                        if (dev.getString("addon") == addonName.split(" ").first()) devName = dev.getString("author")
                    }

                    devName
                }.invoke()
                val eventValues = if (it.has("eventvalues")) it.getString("eventvalues") else ""
                val changers = if (it.has("changers")) it.getString("changers") else ""
                val tags = if (it.has("tags")) it.getString("tags") else ""
                val returnType = if (it.has("returntype")) it.getString("returntype") else ""


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


                addonVer!!.addElement(AddonItem(id, name, type, addon, reviewed, addonVersion, pattern, plugin, eventValues, changers, tags, returnType))
            }
        }
    }

    fun downloadSkriptVersion(name: String): File {

        val skFile = File(skriptVersionsFolder, name)

        if (!skFile.exists()) {
            skFile.createNewFile()
            Prompts.infoCheck("Information", "Sk-IDE is downloading a file", "Sk-IDE is Downloading a file!", Alert.AlertType.INFORMATION)
            downloadFile("https://skripttools.net/dl/${URLEncoder.encode(name, "UTF-8")}", skFile.absolutePath)
        }

        return skFile
    }
}

