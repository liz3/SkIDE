package com.skide.utils

import com.skide.CoreManager
import java.io.File

class InsightsManager(val coreManager: CoreManager) {

    var loaded = false

    val folder = {
        val f = File(coreManager.configManager.rootFolder, "insights")

        if (!f.exists()) f.mkdir()

        f
    }.invoke()

    private val versionFile = File(folder, "version.txt")


    val binFolder = {
        val f = File(folder, "bin")

        if (!f.exists()) f.mkdir()

        f
    }.invoke()

    private fun start() {
        coreManager.insightClient.initEngine()
        loaded = true
    }

    fun setup() {
        if (coreManager.configManager.get("disable_insights") == "true") return
        val latest = checkVersion()


        if (!versionFile.exists()) {
            update()
        } else {
            val currentVersion = readFile(versionFile).second

            if (currentVersion != latest) update() else start()

        }
    }

    private fun update() {

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