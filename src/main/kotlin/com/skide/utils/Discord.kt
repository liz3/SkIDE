package com.skide.utils

import com.github.psnrigner.discordrpcjava.*
import com.skide.gui.GUIManager

class Discord (private var disabled : Boolean = false) {

    private var discordRpc = DiscordRpc()

    init {
        if (getOS() == OperatingSystemType.MAC_OS)
            disabled = true
        update()
        GUIManager.closingHooks.add { stop() }
    }

    private fun update() {
        if (disabled)
            return

        try {
            discordRpc = DiscordRpc()
            discordRpc.init("425466853943672852", object : DiscordEventHandler {
                override fun joinRequest(joinRequest: DiscordJoinRequest?) {
                }

                override fun joinGame(joinSecret: String?) {
                }

                override fun ready() {

                }

                override fun disconnected(errorCode: ErrorCode?, message: String?) {
                }

                override fun spectateGame(spectateSecret: String?) {
                }

                override fun errored(errorCode: ErrorCode?, message: String?) {
                }
            }, true)

            discordRpc.runCallbacks()
        } catch (e: RuntimeException) {
            e.printStackTrace()
            disabled = true
            println("Discord RPC disabled")
        }
    }

    fun stop() {
        if (disabled)
            return

        discordRpc.shutdown()
    }

    fun update(details: String, state: String) {
        if (disabled)
            return

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