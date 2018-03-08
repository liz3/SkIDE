package com.skide.include

import com.skide.core.management.ProjectFileManager
import org.json.JSONArray
import java.io.File


class Project(val id: Long, val name: String, val folder: File, val skriptVersion:String, val files:JSONArray, val primaryServer:Long) {

    val fileManager = ProjectFileManager(this)

}
