package com.skide.core.management

import com.skide.CoreManager
import com.skide.gui.Prompts
import com.skide.include.Server
import com.skide.include.ServerAddon
import com.skide.include.ServerConfiguration
import com.skide.utils.readFile
import com.skide.utils.writeFile
import javafx.application.Platform
import javafx.scene.control.Alert
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.*

class ServerManager(val coreManager: CoreManager) {

    val servers = HashMap<String, Server>()
    val running = HashMap<Server, RunningServerManager>()

    fun init() {
        loadServers()
    }

    fun getServerForRun(server: Server, readyCallback: (RunningServerManager) -> Unit = { }): RunningServerManager {
        if (running.containsKey(server)) {
            readyCallback(running[server]!!)
            return running[server]!!
        }

        val runner = RunningServerManager(server, coreManager)

        runner.start(readyCallback)
        running[server] = runner
        return runner
    }

    private fun loadServers() {
        coreManager.configManager.servers.values.forEach {

            try {
                val confFile = File(it.path, ".server.skide")
                val obj = JSONObject(readFile(confFile).second)
                val server = Server(ServerConfiguration(obj.getString("name"), obj.getString("skript_version"), File(obj.getString("api")), File(it.path), obj.getString("start_args"), if(obj.has("jvm_args")) obj.getString("jvm_args") else ""), confFile, false, obj.getLong("id"))
                obj.getJSONArray("addons").forEach {
                    if (it is JSONObject) {

                        server.configuration.addons.add(ServerAddon(it.getString("name"), File(it.getString("file")), it.getBoolean("preset")))
                    }
                }
                servers[obj.getString("name")] = server
            } catch (e: Exception) {
                e.printStackTrace()
                deleteServerByName(it.id)
                Platform.runLater {
                    Prompts.infoCheck("Error", "Error while loading serer", "An error occurred while loading server ${it.name} at ${it.path}", Alert.AlertType.ERROR)
                }
            }

        }
    }

    fun createServer(server: Server): Boolean {
        server.confFile = File(server.configuration.folder, ".server.skide")

        if (!server.configuration.folder.isDirectory) return false
        if (!server.configuration.folder.exists()) {
            if (!server.configuration.folder.mkdir()) return false
        }
        val id = System.currentTimeMillis()

        val confObject = JSONObject()
        confObject.put("name", server.configuration.name)
        confObject.put("conf", server.confFile.absolutePath)
        confObject.put("skript_version", server.configuration.skriptVersion)
        confObject.put("api", server.configuration.apiPath.absolutePath)
        confObject.put("id", id)
        confObject.put("start_args", server.configuration.startArgs)
        confObject.put("jvm_args", server.configuration.jvmArgs)
        val addonArray = JSONArray()
        server.configuration.addons.forEach {
            val itsObj = JSONObject()
            itsObj.put("name", it.name)
            itsObj.put("preset", it.fromPresets)
            itsObj.put("file", it.file.absolutePath)
            addonArray.put(itsObj)
        }
        confObject.put("addons", addonArray)
        writeFile(confObject.toString().toByteArray(), server.confFile, false, true)
        servers[server.configuration.name] = server

        updateServerFiles(server)
        coreManager.configManager.addServer(PointerHolder(id, server.configuration.name, server.configuration.folder.absolutePath))
        return true
    }

    fun saveServerConfigution(server: Server) {
        if (server.running) return

        if (!servers.containsKey(server.configuration.name)) {
            createServer(server)
            return
        }
        val confObject = JSONObject()
        confObject.put("name", server.configuration.name)
        confObject.put("conf", server.confFile.absolutePath)
        confObject.put("skript_version", server.configuration.skriptVersion)
        confObject.put("api", server.configuration.apiPath.absolutePath)
        confObject.put("id", server.id)
        confObject.put("start_args", server.configuration.startArgs)
        confObject.put("jvm_args", server.configuration.jvmArgs)

        val addonArray = JSONArray()
        server.configuration.addons.forEach {
            val itsObj = JSONObject()
            itsObj.put("name", it.name)
            itsObj.put("preset", it.fromPresets)
            itsObj.put("file", it.file.absolutePath)
            addonArray.put(itsObj)
        }
        confObject.put("addons", addonArray)
        writeFile(confObject.toString().toByteArray(), server.confFile, false, true)
        updateServerFiles(server)
        coreManager.configManager.alterServer(server.id, PointerHolder(server.id, server.configuration.name, server.configuration.folder.absolutePath))

    }

    fun deleteServer(server: Server) {
        if (server.running) return
        val name = server.configuration.name
        servers.remove(name)
        coreManager.configManager.deleteServer(server.id)

    }

    fun deleteServerByName(id: Long) {

        coreManager.configManager.deleteServer(id)

    }

    fun updateServerFiles(server: Server) {

        if (server.running) return
        try {
            Files.copy(server.configuration.apiPath.toPath(), File(server.configuration.folder, "Server.jar").toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            Prompts.infoCheck("Error", "Error while updating server files", "The file ${server.configuration.apiPath.absolutePath} could not be copied to ${File(server.configuration.folder, "Server.jar").absolutePath} because ${e.message}", Alert.AlertType.ERROR)
            return
        }

        val pluginDir = File(server.configuration.folder, "plugins")

        if (!pluginDir.exists() && !pluginDir.mkdir()) return
        if (!pluginDir.isDirectory) {
            pluginDir.delete()
            pluginDir.mkdir()
        }
        pluginDir.listFiles().forEach {
            if (it.name.endsWith(".jar")) {
                it.delete()
            }
        }
        writeFile(("#By changing the setting below to TRUE you are indicating your agreement to our EULA (https://account.mojang.com/documents/minecraft_eula).\n" +
                "eula=true\n").toByteArray(), File(server.configuration.folder, "eula.txt"), false, true)


        val downloaded = coreManager.resourceManager.downloadSkriptVersion(server.configuration.skriptVersion)
        try {
            Files.copy(downloaded.toPath(), File(pluginDir, "Skript.jar").toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: Exception) {
            Prompts.infoCheck("Error", "Error while updating server files", "The file ${downloaded.absolutePath} cold not be copied to ${File(pluginDir, "Skript.jar").absolutePath} because ${e.message}", Alert.AlertType.ERROR)
            return
        }
        server.configuration.addons.forEach {
            if (it.file.exists()) try {
                Files.copy(it.file.toPath(), File(pluginDir, it.name).toPath(), StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                Prompts.infoCheck("Error", "Error while updating server files", "The file ${it.file.absolutePath} cold not be copied to ${File(pluginDir, it.name).absolutePath} because ${e.message}", Alert.AlertType.ERROR)
                return
            }


        }
    }


}
