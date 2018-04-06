package com.skide.utils

import com.skide.CoreManager
import com.skide.gui.GUIManager
import com.skide.gui.Prompts
import javafx.application.Platform
import javafx.scene.control.Alert
import java.io.File
import kotlin.math.ln

class InsightsManager(val coreManager: CoreManager) {

    var loaded = false



    val folder = {
        val f = File(coreManager.configManager.rootFolder, "insights")

        if (!f.exists()) f.mkdir()

        f
    }.invoke()

    private val versionFile = File(folder, "version.txt")
    private val zipPath = File(folder,"bin.zip")


    val binFolder = {
        val f = File(folder, "bin")

        if (!f.exists()) f.mkdir()

        f
    }.invoke()

    private fun start() {

        val path = File(binFolder, "SkriptInsightHoster").absolutePath
        if(getOS() != OperatingSystemType.WINDOWS) Runtime.getRuntime().exec("chmod +x $path")
        val pb = ProcessBuilder()
        pb.command(path)

        Thread {
            val p = pb.start()
            GUIManager.closingHooks.add {
                p.destroy()
            }
            Thread.sleep(1000)
            coreManager.insightClient.initEngine()
        loaded = true
        }.start()

    }

    fun setup(callback: () -> Unit) {
        if (coreManager.configManager.get("disable_insights") == "true") return
        val latest = checkVersion()


        if (!versionFile.exists()) {
            update(true) {
                callback()
            }
        } else {
            val currentVersion = readFile(versionFile).second

            if (currentVersion != latest) update {

                callback()
            } else {
                callback()
                start()
            }

        }
    }

    private fun update(inform:Boolean = false, callback: () -> Unit) {

       Platform.runLater {
           if(inform) {
               if(!Prompts.infoCheck("Attention", "Additional Download required", "In order to use Inspections, SK-IDE needs to download ~20MB. Download them now?", Alert.AlertType.CONFIRMATION)) {

                   callback()
                   return@runLater
               }
           }


           Thread {
               val lnk = "https://liz3.net/sk/insights/${
               when (getOS()) {
                   OperatingSystemType.MAC_OS -> "darwin"
                   OperatingSystemType.WINDOWS -> "win"
                   OperatingSystemType.LINUX -> "unix"
                   else -> ""

               }}/bin.zip"
               downloadFile(lnk, zipPath.absolutePath)


               unzip(zipPath.absolutePath, binFolder.absolutePath)

               writeFile(checkVersion().toByteArray(), versionFile, false, true)

               callback()
               start()
           }.start()
       }
    }

    private fun checkVersion(): String {

        var str = ""
        val response = request("https://liz3.net/sk/insights/" + {
            when (getOS()) {
                OperatingSystemType.MAC_OS -> "darwin"
                OperatingSystemType.WINDOWS -> "win"
                OperatingSystemType.LINUX -> "unix"
                else -> ""

            }
        }.invoke()).third

        while (true) {
            val r = response.read()
            if (r == -1) break
            str += r.toChar()
        }
        return str
    }

}