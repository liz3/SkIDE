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
    val skriptDocList = Vector<AddonItem>()
    val skriptVersions = Vector<String>()
    val addonsFile = File(coreManager.configManager.rootFolder, "addons.json")
    val skriptVersionsFile = File(coreManager.configManager.rootFolder, "skript-vers.json")
    val skriptVersionsFolder = File(coreManager.configManager.rootFolder, "skript-versions")

    val skHubFile = File(coreManager.configManager.rootFolder, "skHub.json")


    fun parseSkriptVersions() {
        JSONArray(readFile(skriptVersionsFile).second).forEach { skriptVersions.add(it as String) }
    }

    fun readAddons() {

        for (entry in JSONArray(readFile(addonsFile).second)) {
            entry as JSONObject
            val name = entry.getString("name")
            val addon = Addon(addons.size.toLong(), name, entry.getString("author"))
            addons[name] = addon
            addon.versions["0.0.0"] = Vector()

        }
    }

    fun loadResources(callback: (Int, Int, String) -> Unit) {

        val total = 5

        callback(total, 1, "https://liz3.net/sk/depot/")

        if (!skriptVersionsFolder.exists()) skriptVersionsFolder.mkdir()
        if (!skHubFile.exists() || coreManager.configManager.get("meta_update") == "true") {
            try {
                callback(total, 2, "Downloading: https://skripthub.net/api/v1/addon/")
                downloadFile("https://skripthub.net/api/v1/addon/", addonsFile.absolutePath)

                callback(total, 4, "Downloading: https://skripttools.net/api.php?t=skript&action=getlist")
                downloadFile("https://skripttools.net/api.php?t=skript&action=getlist", skriptVersionsFile.absolutePath)

                callback(total, 5, "Downloading: https://skripthub.net/api/v1/addonsyntaxlist/")
                downloadFile("https://skripthub.net/api/v1/addonsyntaxlist/", skHubFile.absolutePath)
            } catch (e: Exception) {

            }
        }
        callback(total, 6, "Reading script versions")
        parseSkriptVersions()
        readAddons()

        val obj = JSONArray(readFile(skHubFile.absolutePath).second)
        callback(total, 8, "Parsing Addons...")
        for (entry in obj) {
            entry as JSONObject
            val addonObj = entry.getJSONObject("addon")
            val addon = getAddon(addonObj.getString("name"))
            val list = addon.versions["0.0.0"]!!
            val title = entry.getString("title")
            val type = when (entry.getString("syntax_type")) {
                "event" -> DocType.EVENT
                "condition" -> DocType.CONDITION
                "effect" -> DocType.EFFECTS
                "expression" -> DocType.EXPRESSION
                "type" -> DocType.TYPE
                else -> DocType.TYPE
            }

            val description = entry.getString("description")
            val pattern = entry.getString("syntax_pattern").split("\n").first()
            val returnType =
                    if (entry.has("return_type") && !entry.isNull("return_type"))
                        entry.getString("return_type")
                    else ""
            val eventValues =
                    if (entry.has("event_values") && !entry.isNull("event_values"))
                        entry.getString("event_values")
                    else ""
            val item = AddonItem(entry.getInt("id"), title, type, addon, pattern, description, eventValues, returnType)

            for (req in entry.getJSONArray("required_plugins")) {
                req as JSONObject
                item.plugins.add(req.getString("name"))
            }

            if (addonObj.getString("name") == "Skript") skriptDocList.addElement(item)

            list.add(item)
        }

    }

    private fun getAddon(name: String): Addon {
        if (addons.containsKey(name))
            return addons[name]!!
        val addon = Addon(addons.size.toLong(), name, "unknown")
        addons[name] = addon
        addon.versions["0.0.0"] = Vector()
        return addon
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

