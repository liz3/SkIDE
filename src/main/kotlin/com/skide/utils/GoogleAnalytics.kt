package com.skide.utils

import com.brsanthu.googleanalytics.GoogleAnalytics
import com.skide.CoreManager
import com.skide.gui.GUIManager

class GoogleAnalytics(val coreManager: CoreManager) {

    var enabled = false
    lateinit var ga: GoogleAnalytics

    init {

    }


    fun start() {
        enabled = true
        ga = GoogleAnalytics.builder().withTrackingId("UA-130243026-3").build()
        try {
            ga.screenView()
                    .sessionControl("start")
                    .send().requestParams.get("cid")!!
        } catch (e: Exception) {
        }
        try {
            ga.pageView()
                    .documentTitle("Sk-IDE main menu")
                    .documentPath("/start")
                    .send()
        } catch (e: Exception) {
        }

        GUIManager.closingHooks.add {
            println("Sending session close")
            try {
                ga.pageView()
                        .documentTitle("stopped")
                        .documentPath("/end")
                        .send()
            } catch (e: Exception) {
            }
        }
    }

}