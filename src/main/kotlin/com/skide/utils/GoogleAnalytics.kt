package com.skide.utils

import com.brsanthu.googleanalytics.GoogleAnalytics
import com.skide.CoreManager
import com.skide.gui.GUIManager

class GoogleAnalytics(val coreManager:CoreManager) {

    var enabled = false
    lateinit var ga: GoogleAnalytics

    init {

    }


    fun start() {
        enabled = true
        ga = GoogleAnalytics.builder().withTrackingId("UA-130243026-3").build()
        val startR = ga.screenView()
                .sessionControl("start")
                .send().requestParams.get("cid")!!
        ga.pageView()
                .documentTitle("Sk-IDE main menu")
                .documentPath("/start")
                .send()
        GUIManager.closingHooks.add {
            println("Sending session close")
            ga.pageView()
                    .documentTitle("stopped")
                    .documentPath("/end")
                    .send()
        }
    }

}