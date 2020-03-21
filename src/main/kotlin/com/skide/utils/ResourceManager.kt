package com.skide.utils

import com.skide.CoreManager
import com.skide.gui.Prompts
import com.skide.include.Addon
import com.skide.include.AddonItem
import com.skide.include.DocType
import javafx.application.Platform
import javafx.scene.control.Alert
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*
import kotlin.collections.HashMap
import kotlin.system.exitProcess


class ResourceManager(val coreManager: CoreManager) {

    val skript = Addon(-1, "Skript", "")
    val addons = HashMap<String, Addon>()
    val skriptVersions = Vector<String>()
    private val addonsFile = File(coreManager.configManager.rootFolder, "addons.json")
    private val addonsCopyFile = File(coreManager.configManager.rootFolder, "addons-copy.json")
    private val skriptVersionsFile = File(coreManager.configManager.rootFolder, "skript-vers.json")
    private val skriptVersionsCopyFile = File(coreManager.configManager.rootFolder, "skript-vers-copy.json")
    private val skriptVersionsFolder = File(coreManager.configManager.rootFolder, "skript-versions")

    private val skHubFile = File(coreManager.configManager.rootFolder, "skHub.json")
    private val skHubCopyFile = File(coreManager.configManager.rootFolder, "skHub-copy.json")


    private fun parseSkriptVersions(arr: JSONArray) {
        arr.forEach { skriptVersions.add(it as String) }
    }

    private fun readAddons(arr: JSONArray) {

        for (entry in arr) {
            entry as JSONObject
            val name = entry.getString("name")
            if (name == "Skript") continue
            val addon = Addon(addons.size.toLong(), name, entry.getString("author"))
            addons[name] = addon
            addon.versions["default"] = Vector()

        }
    }

    fun loadAddon(name: String) {
        if (addons[name] != null && addons[name]!!.loaded) {
            return
        }
        val obj = JSONArray(readFile(skHubFile.absolutePath).second)
        for (entry in obj) {
            entry as JSONObject
            val addonObj = entry.getJSONObject("addon")
            if (addonObj.getString("name") != name) continue
            val addon = getAddon(addonObj.getString("name"))
            val list = addon.versions["default"]!!
            val title = entry.getString("title")
            val type = when (entry.getString("syntax_type")) {
                "event" -> DocType.EVENT
                "condition" -> DocType.CONDITION
                "effect" -> DocType.EFFECTS
                "expression" -> DocType.EXPRESSION
                "type" -> DocType.TYPE
                else -> DocType.TYPE
            }
            val patterns = entry.getString("syntax_pattern").split("\n")
            val returnType =
                    if (entry.has("return_type") && !entry.isNull("return_type"))
                        entry.getString("return_type")
                    else ""
            val eventValues =
                    if (entry.has("event_values") && !entry.isNull("event_values"))
                        entry.getString("event_values")
                    else ""
            val version = entry.getString("compatible_addon_version")
            for ((counter, pattern) in patterns.withIndex()) {
                val description = entry.getString("description")
                val item = AddonItem(entry.getInt("id"), "$title - ${counter + 1}", type, addon, pattern, "Description: $description\nSince: $version", eventValues, returnType)
                if (item.type == DocType.EVENT)
                    item.requirements = EditorUtils.extractNeededPartsFromEvent(pattern)

                for (req in entry.getJSONArray("required_plugins")) {
                    req as JSONObject
                    item.plugins.add(req.getString("name"))
                }
                list.add(item)
            }
        }
        addons[name]?.loaded = true
    }

    private fun processWithBackUp(f1: File, f2: File, array: Boolean, cb: (Any) -> Unit): Boolean {
        try {
            val json = if (array) JSONArray(readFile(f1).second) else JSONObject(readFile(f1).second)
            Files.copy(f1.toPath(), f2.toPath(), StandardCopyOption.REPLACE_EXISTING)
            cb(json)
            return true
        } catch (e: Exception) {
            f1.delete()
            if (f2.exists()) {
                println("falling back to copy : ${f2.name}")
                try {
                    val json = if (array) JSONArray(readFile(f2).second) else JSONObject(readFile(f2).second)
                    cb(json)
                    return true
                } catch (e: Exception) {

                }
            }
            Platform.runLater {
                Prompts.infoCheck("Error", "SkIDE  error", "SkIDE failed to read ${f1.name} and cant continue, please retry or report an error on discord", Alert.AlertType.ERROR)
                exitProcess(0)
            }
        }

        return false
    }

    fun loadResources(callback: (Int, Int, String) -> Unit): Boolean {
        val total = 5
        callback(total, 1, "https://liz3.net/sk/depot/")
        if (!skriptVersionsFolder.exists()) skriptVersionsFolder.mkdir()
        if (!skHubFile.exists() || !addonsFile.exists() || !skriptVersionsFile.exists() || coreManager.configManager.get("meta_update") == "true") {
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
        if (!processWithBackUp(skriptVersionsFile, skriptVersionsCopyFile, true) {
                    parseSkriptVersions(it as JSONArray)
                }) return false
        if (!processWithBackUp(addonsFile, addonsCopyFile, true) {
                    readAddons(it as JSONArray)

                }) return false
        if (!processWithBackUp(skHubFile, skHubCopyFile, true) {
                    readSkript(it as JSONArray)
            }) return false
        callback(total, 8, "Parsing Addons...")
        return true
    }

    private fun readSkript(obj: JSONArray) {
        val l = Vector<AddonItem>()

        for (entry in obj) {
            entry as JSONObject
            val addonObj = entry.getJSONObject("addon")
            if (addonObj.getString("name") != "Skript") continue
            val syntaxTitle = entry.getString("title")
            val type = when (entry.getString("syntax_type")) {
                "event" -> DocType.EVENT
                "condition" -> DocType.CONDITION
                "effect" -> DocType.EFFECTS
                "expression" -> DocType.EXPRESSION
                "type" -> DocType.TYPE
                else -> DocType.TYPE
            }
            val patterns = entry.getString("syntax_pattern").split("\n")
            val returnType =
                    if (entry.has("return_type") && !entry.isNull("return_type"))
                        entry.getString("return_type")
                    else ""
            val eventValues =
                    if (entry.has("event_values") && !entry.isNull("event_values"))
                        entry.getString("event_values")
                    else ""
            val version = entry.getString("compatible_addon_version")
            for (pattern in patterns) {
                val description = entry.getString("description")
                val item = AddonItem(entry.getInt("id"), syntaxTitle, type, skript, pattern, "Description: $description\nSince: $version", eventValues, returnType)
                if (item.type == DocType.EVENT)
                    item.requirements = EditorUtils.extractNeededPartsFromEvent(pattern)

                for (req in entry.getJSONArray("required_plugins")) {
                    req as JSONObject
                    item.plugins.add(req.getString("name"))
                }
                l.add(item)
                break
            }
        }
        skript.versions["default"] = l
    }

    private fun getAddon(name: String): Addon {
        if (addons.containsKey(name))
            return addons[name]!!
        val addon = Addon(addons.size.toLong(), name, "unknown")
        addons[name] = addon
        addon.versions["default"] = Vector()
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

    private fun skriptVersionPatcher(raw: String): Version {
        if (raw.isEmpty()) {
            return Version("1.0")
        }
        if (raw == "unknown (2.2)") {
            return Version("2.2")
        }
        if (raw == "unknown (before 2.1)") {
            return Version("2.1")
        }
        if (raw == "unknown" || raw == " add" || raw == " unknown (mine)" || raw == " remove" || raw == " delete)"
                || raw == " unknown (player list name)") {
            return Version("1.0")
        }
        val static = raw.trim()
        val version = static.replace("-dev", ".").split(" ").first()
        return try {
            Version(version)
        } catch (e: Exception) {

            Version("1.0")

        }
    }
}

