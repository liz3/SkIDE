package com.skide.utils

import com.skide.CoreManager
import com.skide.include.Addon
import com.skide.include.AddonItem
import com.skide.include.DocType
import org.json.JSONObject
import java.io.File
import java.util.*

class ResourceManager(val coreManager: CoreManager) {

    val addons = HashMap<String, Addon>()
    val skriptDocList = Vector<AddonItem>()
    val file = File(coreManager.configManager.rootFolder, "docs.json")
    val addonsFile = File(coreManager.configManager.rootFolder, "addons.json")
    val skriptDoc = File(coreManager.configManager.rootFolder, "skript.json")

    init {

    }

    private fun parseSkriptVersion() {

        val read = readFile(skriptDoc)

        if(read.first == FileReturnResult.SUCCESS) {

            JSONObject(read.second).getJSONArray("result").forEach {


                val addon = Addon(-1, "Skript", "Skript")
                it as JSONObject
                val id = it.getInt("id")
                val name = it.getString("name")
                val addonName = it.getString("addon")



                    val addonVersion = it.getString("version")
                    val reviewed = if( it.has("reviewed")) it.getString("reviewed")  else ""
                    val pattern = if( it.has("pattern")) it.getString("pattern")  else ""
                    val plugin = if( it.has("plugin")) it.getString("plugin")  else ""
                    val eventValues = if( it.has("eventvalues")) it.getString("eventvalues")  else ""
                    val changers = if( it.has("changers")) it.getString("changers")  else ""
                    val tags = if( it.has("tags")) it.getString("tags")  else ""
                    val returnType = if( it.has("returntype")) it.getString("returntype")  else ""

                    val type = when (it.getString("doc")) {
                        "conditions" -> DocType.CONDITION
                        "effects" -> DocType.EFFECTS
                        "events" -> DocType.EVENT
                        "expressions" -> DocType.EXPRESSION
                        "types" -> DocType.TYPE
                        else -> DocType.TYPE
                    }


                    skriptDocList.add(AddonItem(id, name,  type, addon, reviewed, addonVersion, pattern, plugin, eventValues, changers, tags, returnType))

            }
        }
    }

    fun loadResources() {

        if (file.exists()) {
            file.delete()
        }

        downloadFile("https://liz3.net/sk/?function=getAllSyntax", file.absolutePath)
        downloadFile("https://liz3.net/sk/?function=getAllAddons", addonsFile.absolutePath)
        downloadFile("https://liz3.net/sk/?function=getAddonSyntax&addon=skript", skriptDoc.absolutePath)


        parseSkriptVersion()

        val str = readFile(file.absolutePath)

        val addonDevs = JSONObject(readFile(addonsFile).second).getJSONArray("result")
        val obj = JSONObject(str.second)


        obj.getJSONArray("result").forEach {
            it as JSONObject
            val id = it.getInt("id")
            val name = it.getString("name")
            val addonName = it.getString("addon")

          if(addonName != "Skript" && addonName != "skript") {

              var addonVersion = it.getString("version")
              val reviewed = if( it.has("reviewed")) it.getString("reviewed")  else ""
              val pattern = if( it.has("pattern")) it.getString("pattern")  else ""
              val plugin = if( it.has("plugin")) it.getString("plugin")  else ""
              val author = {
                  var devName = ""

                  addonDevs.forEach {dev ->
                      dev as JSONObject
                      if(dev.getString("addon") == addonName.split(" ").first()) devName = dev.getString("author")
                  }

                  devName
              }.invoke()
              if(author == "") println("dev null for $id $addonName in allSyntaxes")
              val eventValues = if( it.has("eventvalues")) it.getString("eventvalues")  else ""
              val changers = if( it.has("changers")) it.getString("changers")  else ""
              val tags = if( it.has("tags")) it.getString("tags")  else ""
              val returnType = if( it.has("returntype")) it.getString("returntype")  else ""


              if (!addons.containsKey(addonName)) addons[addonName] = Addon((addons.size + 1).toLong(), addonName, author)
              val addon = addons[addonName]!!
              if (addonVersion == null || addonVersion == "") addonVersion = "0.0.0"
              if (!addon.versions.containsKey(addonVersion)) addon.versions[addonVersion] = Vector()
              val addonVer = addon.versions[addonVersion]
              val type = when (it.getString("doc")) {
                  "conditions" -> DocType.CONDITION
                  "effects" -> DocType.EFFECTS
                  "events" -> DocType.EVENT
                  "expressions" -> DocType.EXPRESSION
                  "types" -> DocType.TYPE
                  else -> DocType.TYPE
              }


              addonVer!!.addElement(AddonItem(id, name,  type, addon, reviewed, addonVersion, pattern, plugin, eventValues, changers, tags, returnType))
          }
        }
    }
}