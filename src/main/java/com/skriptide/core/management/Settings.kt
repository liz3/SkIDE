package com.skriptide.core.management

import com.skriptide.CoreManager
import com.skriptide.include.Addon
import com.skriptide.utils.FileReturnResult
import com.skriptide.utils.readFile
import com.skriptide.utils.writeFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class PointerHolder(val id: Long, val name: String, val path: String)
enum class ConfigLoadResult {
    SUCCESS,
    FIRST_RUN,
    ERROR
}

class ConfigManager(val coreManager: CoreManager) {
    var loaded = false
        private set
    private var configLoaded = false

    private val settings = HashMap<String, Any>()

    val rootFolder = File(System.getProperty("user.home"), ".skide")
    var defaultProjectPath = File(System.getProperty("user.home"), "SkIde-Projects")
        set(value) {
            field = value
            if (configLoaded) writeFile(getConfigObject().toString().toByteArray(), configFile)
        }
    var defaultServerPath = File(System.getProperty("user.home"), "SkIde-Server")
        set(value) {
            field = value
            if (configLoaded) writeFile(getConfigObject().toString().toByteArray(), configFile)
        }
    var lastOpened = -1
        set(value) {
            field = value
            if (configLoaded) writeFile(getConfigObject().toString().toByteArray(), configFile)
        }


    val configFile = File(rootFolder, "settings.skide")
    val projectsFile = File(rootFolder, "projects.skide")
    val serversFile = File(rootFolder, "servers.skide")
    val addonFile = File(rootFolder, "addons.skide")
    val hostsFile = File(rootFolder, "hosts.skide")

    val projects = HashMap<Long, PointerHolder>()
    val servers = HashMap<Long, PointerHolder>()
    val apis = HashMap<Long, PointerHolder>()
    val addons = HashMap<Long, Addon>()


    fun load(): ConfigLoadResult {
        loaded = if (loaded) return ConfigLoadResult.SUCCESS else true
        var firstRun = false

        if (!rootFolder.exists()) firstRun = true
        if (!configFile.exists()) firstRun = true

        if (firstRun) return if (createFiles()) ConfigLoadResult.FIRST_RUN else ConfigLoadResult.ERROR


        //read the main Config
        val configResult = readConfig()

        val projectsFileResult = readFile(projectsFile)
        val serversFileResult = readFile(serversFile)
        val addonFileResult = readFile(addonFile)

        //For Projects
        if (projectsFileResult.first == FileReturnResult.SUCCESS) {
            val projectsArray = JSONArray(projectsFileResult.second)
            projectsArray.forEach {
                it as JSONObject
                projects.put(it.getLong("id"), PointerHolder(it.getLong("id"), it.getString("name"), it.getString("path")))
            }
        }
        if (serversFileResult.first == FileReturnResult.SUCCESS) {
            val serverObj = JSONObject(serversFileResult.second)
            val apisArray = serverObj.getJSONArray("apis")
            val serversArray = serverObj.getJSONArray("servers")
            apisArray.forEach {
                it as JSONObject
                apis.put(it.getLong("id"), PointerHolder(it.getLong("id"), it.getString("name"), it.getString("path")))
            }
            serversArray.forEach {
                it as JSONObject
                servers.put(it.getLong("id"), PointerHolder(it.getLong("id"), it.getString("name"), it.getString("path")))
            }
        }
        if (addonFileResult.first == FileReturnResult.SUCCESS) {
            val addonsArray = JSONArray(addonFileResult.second)

            addonsArray.forEach {
                it as JSONObject
                addons.put(it.getLong("id"), Addon(it.getLong("id"), it.getString("name"), it.getString("version"), File(it.getString("path"))))
            }
        }

        return ConfigLoadResult.SUCCESS
    }

    private fun writeMapToFile(file: File, map: HashMap<Long, PointerHolder>): Boolean {

        val arr = JSONArray()

        for ((id, value) in map) {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("name", value.name)
            obj.put("path", value.path)
            arr.put(obj)
        }
        return writeFile(arr.toString().toByteArray(), file, false, false).first == FileReturnResult.SUCCESS
    }

    private fun writeServersFile(): Boolean {
        val obj = JSONObject()
        val serverArr = JSONArray()
        val apisObArr = JSONArray()
        for ((id, server) in servers) {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("name", server.name)
            obj.put("path", server.path)
            serverArr.put(obj)
        }
        for ((id, api) in apis) {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("name", api.name)
            obj.put("path", api.path)
            apisObArr.put(obj)
        }
        obj.put("servers", serverArr)
        obj.put("apis", apis)

        return writeFile(obj.toString().toByteArray(), serversFile, false, false).first == FileReturnResult.SUCCESS
    }

    fun writeAddonFile(): Boolean {

        val array = JSONArray()

        for ((id, addon) in addons) {
            val obj = JSONObject()
            obj.put("id", id)
            obj.put("name", addon.name)
            obj.put("version", addon.version)
            obj.put("path", addon.file.absolutePath)
            array.put(obj)
        }

        return writeFile(array.toString().toByteArray(), addonFile, false, false).first == FileReturnResult.SUCCESS
    }

    //PROJECTS
    fun addProject(holder: PointerHolder): Boolean {
        if (projects.containsKey(holder.id)) return false
        projects.put(holder.id, holder)
        return writeMapToFile(projectsFile, projects)
    }

    fun deleteProject(id: Long): Boolean {
        if (!projects.containsKey(id)) return false
        projects.remove(id)
        return writeMapToFile(projectsFile, projects)
    }

    fun alterProject(id: Long, holder: PointerHolder): Boolean {
        if (!projects.containsKey(id)) return false
        projects.put(id, holder)

        return writeMapToFile(projectsFile, projects)
    }

    //SERVER
    fun addServer(holder: PointerHolder): Boolean {
        if (servers.containsKey(holder.id)) return false
        servers.put(holder.id, holder)

        return writeServersFile()
    }

    fun deleteServer(id: Long): Boolean {
        if (!servers.containsKey(id)) return false
        servers.remove(id)
        return writeServersFile()
    }

    fun alterServer(id: Long, holder: PointerHolder): Boolean {
        if (!servers.containsKey(id)) return false
        servers.put(id, holder)

        return writeServersFile()
    }

    //APIS
    fun addApi(holder: PointerHolder): Boolean {
        if (apis.containsKey(holder.id)) return false
        apis.put(holder.id, holder)

        return writeServersFile()
    }

    fun deleteApi(id: Long): Boolean {
        if (!apis.containsKey(id)) return false
        apis.remove(id)
        return writeServersFile()
    }

    fun alterApi(id: Long, holder: PointerHolder): Boolean {
        if (!apis.containsKey(id)) return false
        apis.put(id, holder)

        return writeServersFile()
    }

    //ADDON
    fun addAddon(addon: Addon): Boolean {
        if (addons.containsKey(addon.id)) return false
        addons.put(addon.id, addon)

        return writeAddonFile()
    }

    fun deleteAddon(id: Long): Boolean {
        if (!addons.containsKey(id)) return false
        addons.remove(id)
        return writeAddonFile()
    }

    fun alterAddon(id: Long, addon: Addon): Boolean {
        if (!addons.containsKey(id)) return false
        addons.put(id, addon)

        return writeServersFile()
    }

    private fun readConfig(): Boolean {

        val readResult = readFile(configFile)

        if (readResult.first == FileReturnResult.SUCCESS) {
            val obj = JSONObject(readResult.second)
            lastOpened = obj.getInt("last_open")
            defaultProjectPath = File(obj.getString("default_project"))
            defaultServerPath = File(obj.getString("default_server"))

            val settingsObj = obj.getJSONObject("settings")

            settingsObj.keySet().forEach {
                settings.put(it, settingsObj.get(it))
            }

        }
        configLoaded = true
        return true
    }

    private fun createFiles(): Boolean {

        val brackets = "[]".toByteArray()
        val braces = "{}".toByteArray()

        val objForServer = JSONObject()
        objForServer.put("servers", JSONArray())
        objForServer.put("apis", JSONArray())

        if (!rootFolder.exists()) if (!rootFolder.mkdir()) return false


        if (!defaultProjectPath.exists()) defaultProjectPath.mkdir()
        if (!defaultServerPath.exists()) defaultServerPath.mkdir()

        if (writeFile(getConfigObject().toString().toByteArray(), configFile, false, true).first != FileReturnResult.SUCCESS) return false
        if (writeFile(brackets, projectsFile, false, true).first != FileReturnResult.SUCCESS) return false
        if (writeFile(objForServer.toString().toByteArray(), serversFile, false, true).first != FileReturnResult.SUCCESS) return false
        if (writeFile(brackets, hostsFile, false, true).first != FileReturnResult.SUCCESS) return false
        if (writeFile(brackets, addonFile, false, true).first != FileReturnResult.SUCCESS) return false

        configLoaded = true
        return true
    }

    fun get(key:String): Any? {
        return settings[key]
    }
    fun set(key:String, value:Any) {
        settings.put(key, value)
        writeFile(getConfigObject().toString().toByteArray(), configFile, false, false)
    }
    private fun getConfigObject(): JSONObject {

        val obj = JSONObject()
        obj.put("last_open", lastOpened)
        obj.put("default_project", defaultProjectPath.absolutePath)
        obj.put("default_server", defaultServerPath.absolutePath)
        val settArr = JSONObject()
        for ((key, value) in settings) {
            settArr.put(key, value)
        }
        obj.put("settings", settArr)

        return obj
    }
}