package com.skide.utils

import com.github.psnrigner.discordrpcjava.*

class Discord {

    private var discordRpc = DiscordRpc()


    init {

        update()
    }

    private fun update() {
        discordRpc = DiscordRpc()
        discordRpc.init("425466853943672852", object : DiscordEventHandler {
            override fun joinRequest(joinRequest: DiscordJoinRequest?) {
                // will never be called
            }

            override fun joinGame(joinSecret: String?) {
                // will never be called
            }

            override fun ready() {

            }

            override fun disconnected(errorCode: ErrorCode?, message: String?) {

            }

            override fun spectateGame(spectateSecret: String?) {
                // will never be called
            }

            override fun errored(errorCode: ErrorCode?, message: String?) {
                // i guess could be handled somehow
            }
        }, true)


        discordRpc.runCallbacks()
    }

    fun stop() = discordRpc.shutdown()

    fun update(details: String, imageText: String) {

        Thread {
            stop()
            update()
            val drp = DiscordRichPresence()
            drp.details = details
            drp.state = "SkIde"
            drp.largeImageKey = "default_liz3"
            drp.largeImageText = imageText

            try {
                discordRpc.updatePresence(drp)
                discordRpc.runCallbacks()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()

    }
}