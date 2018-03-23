package com.skide.core.management

import com.skide.CoreManager
import com.skide.include.CompileOption
import com.skide.include.CompileOptionType
import com.skide.include.Project
import com.skide.utils.FileReturnResult
import com.skide.utils.readFile
import com.skide.utils.writeFile
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.nio.file.Files
import java.util.*
import kotlin.collections.HashMap

enum class ProjectConfigurationLoadResult {
    SUCCESS,
    NOT_FOUND,
    ERROR
}

class ProjectManager(val coreManager: CoreManager) {

    val openProjects = Vector<OpenProject>()


    fun createNewProject(name: String, path: String, skriptVersion: String, open: Boolean) {

        val id = System.currentTimeMillis() / 1000L
        val pointHolder = PointerHolder(id, name, path)
        coreManager.configManager.addProject(pointHolder)

        if (!createProjectFiles(name, path, skriptVersion, id)) {


            return
        }
        if (open) openProject(pointHolder)


    }

    fun importProject(name: String, path: String, skriptVersion: String, open: Boolean) {


        val id = System.currentTimeMillis() / 1000L
        val pointHolder = PointerHolder(id, name, path)
        coreManager.configManager.addProject(pointHolder)

        if (!createProjectFilesFromImport(name, path, skriptVersion, id)) {
            return
        }
        if (open) openProject(pointHolder)


    }

    fun openProject(holder: PointerHolder) {

        val projectConfig = loadProjectConfiguration(holder)

        if (projectConfig.first == ProjectConfigurationLoadResult.SUCCESS && projectConfig.second != null) {

            openProjects.addElement(OpenProject(projectConfig.second!!, coreManager))
        }
    }

    private fun loadProjectConfiguration(holder: PointerHolder): Pair<ProjectConfigurationLoadResult, Project?> {

        val configFile = File(holder.path, ".project.Sk-IDE")
        val readResult = readFile(configFile)
        if (readResult.first == FileReturnResult.NOT_FOUND) return Pair(ProjectConfigurationLoadResult.NOT_FOUND, null)

        if (readResult.first == FileReturnResult.SUCCESS) {

            val obj = JSONObject(readResult.second)

            val project = Project(obj.getLong("project_id"), obj.getString("name"), File(holder.path), obj.getString("skript_version"), obj.getJSONArray("files"), obj.getJSONArray("addons"), obj.getLong("primary_server_id"))
            return Pair(ProjectConfigurationLoadResult.SUCCESS, project)

        }

        return Pair(ProjectConfigurationLoadResult.ERROR, null)
    }

    private fun createProjectFiles(name: String, path: String, skriptVersion: String, id: Long): Boolean {
        val projectFolder = File(path)

        if (!projectFolder.mkdir()) return false

        val obj = JSONObject()
        obj.put("name", name)
        obj.put("skript_version", skriptVersion)
        obj.put("project_id", id)
        obj.put("files", JSONArray())
        obj.put("addons", JSONArray())
        obj.put("primary_server_id", -1)

        val configFile = File(projectFolder, ".project.Sk-IDE")
        writeFile(obj.toString().toByteArray(), configFile, false, true)
        return true
    }

    private fun createProjectFilesFromImport(name: String, path: String, skriptVersion: String, id: Long): Boolean {

        val projectFolder = File(path)

        if (!projectFolder.exists()) return false

        val possOldConfigFile = File(projectFolder, ".project.Sk-IDE")
        if (possOldConfigFile.exists()) {
            //TODO inform user
            possOldConfigFile.delete()
        }
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("skript_version", skriptVersion)
        obj.put("project_id", id)
        obj.put("addons", JSONArray())
        obj.put("primary_server_id", -1)
        val filesArr = JSONArray()
        projectFolder.listFiles().forEach {
            filesArr.put(it.absolutePath)
        }
        obj.put("files", filesArr)
        val configFile = File(projectFolder, ".project.Sk-IDE")
        writeFile(obj.toString().toByteArray(), configFile, false, true)
        return true
    }

}

class ProjectFileManager(val project: Project) {

    val configFile = File(project.folder, ".project.Sk-IDE")
    private val compileOptsFile = File(project.folder, ".compileInfo.Sk-IDE")
    val projectFiles = HashMap<String, File>()
    val addons = HashMap<String, String>()
    val openFilesForSave = Vector<String>()
    val compileOptions = HashMap<String, CompileOption>()


    init {
        project.files.forEach {
            val f = File(it as String)
            if (f.exists()) {
                projectFiles[f.name] = f
            }
        }
        project.addons.forEach {
            it as JSONObject
            addons[it.getString("name")] = it.getString("version")
        }
        rewriteConfig()
        if (compileOptsFile.exists()) {
            loadCompileOptions()
        } else {
            compileOptsFile.createNewFile()
            val default = CompileOption(project.name, project.folder, CompileOptionType.CONCATENATE, true, true, false, 0)

            default.includedFiles += projectFiles.values
            compileOptions["Default"] = default
            writeCompileOptions()

        }

    }

    fun delCompileOption(name: String) {
        if (!compileOptions.containsKey(name)) return
        compileOptions.remove(name)
        writeCompileOptions()

    }

    fun addCompileOption(opt: CompileOption) {

        if (compileOptions.containsKey(opt.name)) return

        compileOptions[opt.name] = opt

        writeCompileOptions()
    }

    private fun loadCompileOptions() {
        JSONArray(readFile(compileOptsFile).second).forEach { current ->
            if (current is JSONObject) {
                val compileOption = CompileOption(current.getString("name"),
                        File(current.getString("output")),
                        CompileOptionType.valueOf(current.getString("method")),
                        current.getBoolean("rem_empty_lines"),
                        current.getBoolean("rem_comments"),
                        current.getBoolean("obfuscate"),
                        current.getInt("obfuscate_lvl"))

                val includedFiles = current.getJSONArray("included_file")
                val excludedFiles = current.getJSONArray("excluded_file")

                projectFiles.values.forEach {
                    when {
                        includedFiles.contains(it.absolutePath) -> compileOption.includedFiles.add(it)
                        excludedFiles.contains(it.absolutePath) -> compileOption.excludedFiles.add(it)
                        else -> compileOption.includedFiles.add(it)
                    }
                }
                compileOptions[compileOption.name] = compileOption
            }
        }

    }

    fun writeCompileOptions() {
        val arr = JSONArray()

        compileOptions.values.forEach { c ->
            val obj = JSONObject()
            val included = JSONArray()
            val excluded = JSONArray()

            obj.put("name", c.name)
            obj.put("output", c.outputDir.absolutePath)
            obj.put("method", c.method.toString())
            obj.put("rem_empty_lines", c.remEmptyLines)
            obj.put("rem_comments", c.remComments)
            obj.put("obfuscate", c.obsfuscate)
            obj.put("obfuscate_lvl", c.obfuscateLevel)
            c.excludedFiles.forEach { excluded.put(it.absolutePath) }
            c.includedFiles.forEach { included.put(it.absolutePath) }
            obj.put("included_file", included)
            obj.put("excluded_file", excluded)

            arr.put(obj)
        }
        writeFile(arr.toString().toByteArray(), compileOptsFile)
    }

    fun rewriteConfig() {

        val obj = JSONObject()
        obj.put("name", project.name)
        obj.put("skript_version", project.skriptVersion)
        obj.put("project_id", project.id)
        obj.put("primary_server_id", project.primaryServer)
        val filesArr = JSONArray()
        for (file in projectFiles.values) {
            filesArr.put(file.absolutePath)
        }
        val addonsArray = JSONArray()
        for (addon in addons) {
            val addonObj = JSONObject()
            addonObj.put("name", addon.key)
            addonObj.put("version", addon.value)
            addonsArray.put(addonObj)
        }
        obj.put("addons", addonsArray)
        obj.put("files", filesArr)

        writeFile(obj.toString().toByteArray(), configFile)
    }

    fun addAddon(name: String, version: String) {
        addons[name] = version
        rewriteConfig()
    }

    fun changeAddonVersion(name: String, version: String) {
        addons[name] = version
        rewriteConfig()
    }

    fun removeAddon(name: String) {
        addons.remove(name)
        rewriteConfig()
    }

    fun addFile(name: String): Boolean {
        val rName = if (name.contains(".")) name else "$name.sk"
        if (projectFiles.containsKey(rName)) return false
        val file = File(project.folder, rName)
        file.createNewFile()
        projectFiles.put(rName, file)
        compileOptions.values.forEach {
            it.includedFiles.add(file)
        }
        rewriteConfig()
        writeCompileOptions()
        return true
    }

    fun deleteFile(rName: String): Boolean {
        if (!projectFiles.containsKey(rName)) return false
        val file = projectFiles[rName]
        if (file?.exists()!!) file.delete() else return false
        projectFiles.remove(rName)
        compileOptions.values.forEach {
            if (it.includedFiles.contains(file)) it.includedFiles.remove(file)
            if (it.excludedFiles.contains(file)) it.excludedFiles.remove(file)
        }
        rewriteConfig()
        writeCompileOptions()
        return true
    }

    fun reNameFile(rName: String, newNameRaw: String): Boolean {
        val newName = if (newNameRaw.contains(".")) newNameRaw else newNameRaw + ".sk"
        if (!projectFiles.containsKey(rName)) return false
        val file = projectFiles[rName]
        if (!file?.exists()!!) return false
        projectFiles.remove(rName)
        Files.move(file.toPath(), file.toPath().resolveSibling(newName))
        val nFile = File(project.folder, newName)
        projectFiles.put(newName, nFile)
        rewriteConfig()
        return true
    }

}