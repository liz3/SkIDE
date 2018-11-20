package com.skide.include

import java.io.File
import java.util.*

class Server(val configuration: ServerConfiguration, var confFile: File, var running: Boolean, val id: Long) {
    override fun toString() = configuration.name
}
class ServerConfiguration(var name: String, var skriptVersion: String, var apiPath: File, var folder: File,
                          var startAgrs: String, var addons: Vector<ServerAddon> = Vector())
class ServerAddon(val name: String, val file: File, val fromPresets: Boolean) { override fun toString() = name }