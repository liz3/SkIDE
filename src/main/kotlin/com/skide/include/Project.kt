package com.skide.include

import com.skide.core.management.ProjectFileManager
import org.json.JSONArray
import org.json.JSONObject
import java.io.File


class Project(val id: Long, var name: String, val folder: File, var skriptVersion:String, val files:JSONArray, val addons:JSONArray, val primaryServer:Long) {

    val fileManager = ProjectFileManager(this)

}
