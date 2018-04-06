package com.skide.core.management

import com.skide.include.OpenFileHolder
import com.skide.utils.readFile
import javafx.application.Platform

class ExternalHandler(val openFileHolder: OpenFileHolder) {

    init {
        Platform.runLater {
            openFileHolder.area.appendText(readFile(openFileHolder.f).second)

            openFileHolder.area.focusedProperty().addListener { _, _, newValue ->

                if (!newValue) {
                    openFileHolder.saveCode()
                }
            }
        }

    }
}