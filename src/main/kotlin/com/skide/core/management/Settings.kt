package com.skide.core.management

import com.skide.CoreManager
import com.skide.utils.FileReturnResult
import com.skide.utils.readFile
import com.skide.utils.writeFile
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
    var skUnityKey = ""
        set(value) {
            field = value
            if (configLoaded) writeFile(getConfigObject().toString().toByteArray(), configFile)
        }
    var skUnityUsername = ""
        set(value) {
            field = value
            if (configLoaded) writeFile(getConfigObject().toString().toByteArray(), configFile)
        }

    val rootFolder = File(System.getProperty("user.home"), ".Sk-IDE")
    var defaultProjectPath = File(System.getProperty("user.home"), "Sk-IDE-Projects")
        set(value) {
            field = value
            if (configLoaded) writeFile(getConfigObject().toString().toByteArray(), configFile)
        }
    var defaultServerPath = File(System.getProperty("user.home"), "Sk-IDE-Server")
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


    fun load(): ConfigLoadResult {
        loaded = if (loaded) return ConfigLoadResult.SUCCESS else true
        var firstRun = false

        if (!rootFolder.exists()) firstRun = true
        if (!configFile.exists()) firstRun = true

        if (firstRun) return if (createFiles()) ConfigLoadResult.FIRST_RUN else ConfigLoadResult.ERROR


        //read the main Config
        readConfig()

        val projectsFileResult = readFile(projectsFile)
        val serversFileResult = readFile(serversFile)

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

            val serversArray = serverObj.getJSONArray("servers")

            serversArray.forEach {
                it as JSONObject
                servers.put(it.getLong("id"), PointerHolder(it.getLong("id"), it.getString("name"), it.getString("path")))
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
        for ((id, server) in servers) {
            val serverObj = JSONObject()
            serverObj.put("id", id)
            serverObj.put("name", server.name)
            serverObj.put("path", server.path)
            serverArr.put(serverObj)
        }
        obj.put("servers", serverArr)


        return writeFile(obj.toString().toByteArray(), serversFile, false, false).first == FileReturnResult.SUCCESS
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
        projects[id] = holder

        return writeMapToFile(projectsFile, projects)
    }

    //SERVER
    fun addServer(holder: PointerHolder): Boolean {
        if (servers.containsKey(holder.id)) return false
        servers[holder.id] = holder

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

    private fun readConfig(): Boolean {

        val readResult = readFile(configFile)

        if (readResult.first == FileReturnResult.SUCCESS) {
            val obj = JSONObject(readResult.second)
            lastOpened = obj.getInt("last_open")
            defaultProjectPath = File(obj.getString("default_project"))
            defaultServerPath = File(obj.getString("default_server"))
            if (obj.has("skunity_key")) skUnityKey = obj.getString("skunity_key")
            if (obj.has("skunity_name")) skUnityUsername = obj.getString("skunity_name")



            val settingsObj = obj.getJSONObject("settings")

            if(settingsObj.length() == 0) {
                writeDefaultSettings()
                return readConfig()
            }
            settingsObj.keySet().forEach {
                settings[it] = settingsObj.get(it)
            }

        }
        configLoaded = true
        return true
    }

    private fun createFiles(): Boolean {

        val brackets = "[]".toByteArray()

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
        writeDefaultSettings()
        configLoaded = true

        return true
    }

    private fun writeDefaultSettings() {
        set("auto_complete", "true")
        set("theme", "dark")
        set("highlighting", "true")
        set("font", "Source Code Pro")
        set("font_size", "15")
    }
    fun get(key: String): Any? {
        return settings[key]
    }

    fun set(key: String, value: Any) {
        settings.put(key, value)
        writeFile(getConfigObject().toString().toByteArray(), configFile, false, false)
    }

    private fun getConfigObject(): JSONObject {

        val obj = JSONObject()
        obj.put("last_open", lastOpened)
        obj.put("default_project", defaultProjectPath.absolutePath)
        obj.put("default_server", defaultServerPath.absolutePath)
        if (skUnityKey != "") obj.put("skunity_key", skUnityKey)
        if (skUnityUsername != "") obj.put("skunity_name", skUnityUsername)
        val settArr = JSONObject()
        for ((key, value) in settings) {
            settArr.put(key, value)
        }
        obj.put("settings", settArr)

        return obj
    }
}