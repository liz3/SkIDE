package com.skide.core.management

import com.skide.CoreManager
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

        if(projectConfig.first == ProjectConfigurationLoadResult.SUCCESS && projectConfig.second != null) {

            openProjects.addElement(OpenProject(projectConfig.second!!, coreManager))
        }
    }

    private fun loadProjectConfiguration(holder: PointerHolder): Pair<ProjectConfigurationLoadResult, Project?> {

        val configFile = File(holder.path, ".project.skide")
        val readResult = readFile(configFile)
        if (readResult.first == FileReturnResult.NOT_FOUND) return Pair(ProjectConfigurationLoadResult.NOT_FOUND, null)

        if (readResult.first == FileReturnResult.SUCCESS) {

            val obj = JSONObject(readResult.second)

            val project = Project(obj.getLong("project_id"), obj.getString("name"), File(holder.path), obj.getString("skript_version"), obj.getJSONArray("files"), obj.getLong("primary_server_id"))
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
        obj.put("primary_server_id", -1)

        val configFile = File(projectFolder, ".project.skide")
        writeFile(obj.toString().toByteArray(), configFile, false, true)
        return true
    }
    private fun createProjectFilesFromImport(name: String, path: String, skriptVersion: String, id: Long): Boolean {

        println(path)
        val projectFolder = File(path)

        val possOldConfigFile = File(projectFolder, ".project.skide")
        if(possOldConfigFile.exists()) {
            //TODO inform user
            possOldConfigFile.delete()
        }
        val obj = JSONObject()
        obj.put("name", name)
        obj.put("skript_version", skriptVersion)
        obj.put("project_id", id)
        obj.put("primary_server_id", -1)
        val filesArr = JSONArray()
        projectFolder.listFiles().forEach {
            filesArr.put(it.absolutePath)
        }
        obj.put("files", filesArr)
        val configFile = File(projectFolder, ".project.skide")
        writeFile(obj.toString().toByteArray(), configFile, false, true)
        return true
    }

}

class ProjectFileManager(val project: Project) {

    val configFile = File(project.folder, ".project.skide")
    val projectFiles = HashMap<String, File>()

    init {
        project.files.forEach {
            val f = File(it as String)
            if(f.exists()) {
                projectFiles.put(f.name, f)
            }
        }
        rewriteConfig()
    }

    private fun rewriteConfig() {

        val obj = JSONObject()
        obj.put("name", project.name)
        obj.put("skript_version", project.skriptVersion)
        obj.put("project_id", project.id)
        obj.put("primary_server_id", project.primaryServer)
        val filesArr = JSONArray()
        for(file in projectFiles.values) {
            filesArr.put(file.absolutePath)
        }
        obj.put("files", filesArr)

        writeFile(obj.toString().toByteArray(), configFile)
    }

    fun addFile(name:String): Boolean {
        val rName = if(name.contains(".")) name else name + ".sk"
        if(projectFiles.containsKey(rName)) return false
        val file = File(project.folder, rName)
        file.createNewFile()
        projectFiles.put(rName, file)
        rewriteConfig()
        return true
    }
    fun deleteFile(rName:String): Boolean {
        if(!projectFiles.containsKey(rName)) return false
        val file = projectFiles[rName]
        if(file?.exists()!!) file.delete() else return false
        projectFiles.remove(rName)
        rewriteConfig()
        return true
    }
    fun reNameFile(rName:String, newNameRaw:String): Boolean {
        val newName = if(newNameRaw.contains(".")) newNameRaw else newNameRaw + ".sk"
        if(!projectFiles.containsKey(rName)) return false
        val file = projectFiles[rName]
        if(!file?.exists()!!) return false
        projectFiles.remove(rName)
        Files.move(file.toPath(), file.toPath().resolveSibling(newName))
        val nFile = File(project.folder, newName)
        projectFiles.put(newName, nFile)
        rewriteConfig()
        return true
    }

}