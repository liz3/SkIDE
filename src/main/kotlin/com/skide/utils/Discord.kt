package com.skide.utils

import com.github.psnrigner.discordrpcjava.*

class Discord {

    private var discordRpc = DiscordRpc()


    init {

        update()
    }

    private fun update() {
        if (getOs() == OperatingSystemType.MAC_OS) return

        discordRpc = DiscordRpc()
        discordRpc.init("425466853943672852", object : DiscordEventHandler {
            override fun joinRequest(joinRequest: DiscordJoinRequest?) {
            }

            override fun joinGame(joinSecret: String?) {
            }

            override fun ready() {

            }override fun disconnected(errorCode: ErrorCode?, message: String?) {
            }
            override fun spectateGame(spectateSecret: String?) {
            }
            override fun errored(errorCode: ErrorCode?, message: String?) {
            }
        }, true)


        discordRpc.runCallbacks()
    }

    fun stop() {
        if (getOs() == OperatingSystemType.MAC_OS) return
        discordRpc.shutdown()

    }
    fun update(details: String, state: String) {

        if (getOs() == OperatingSystemType.MAC_OS) return
        Thread {
            stop()
            update()
            val drp = DiscordRichPresence()
            drp.details = details
            drp.state = state
            drp.largeImageKey = "default_liz3"
            drp.largeImageText = ""

            try {
                discordRpc.updatePresence(drp)
                discordRpc.runCallbacks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()


    }
}