package com.skide.include

import com.skide.core.management.ProjectFileManager
import org.json.JSONArray
import java.io.File
import java.util.*

class Project(val id: Long, var name: String, val folder: File, var skriptVersion: String, val files: JSONArray, val addons: JSONArray, val primaryServer: Long){
    val fileManager = ProjectFileManager(this)
}

enum class CompileOptionType{
    PER_FILE,
    CONCATENATE,
    JAR
}

class CompileOption(var name: String, var outputDir: File, var method: CompileOptionType, var remEmptyLines: Boolean, var remComments: Boolean, var obsfuscate: Boolean, var obfuscateLevel: Int, val includedFiles: Vector<File> = Vector(), val excludedFiles: Vector<File> = Vector()){
    override fun toString(): String{
        return name
    }
}