package com.skide.gui

import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.TextInputDialog
import java.util.Optional



object Prompts {

    fun textPrompt(title: String, header: String): String {
        val input = TextInputDialog()
        input.title = title
        input.headerText = header

        return try {
            input.showAndWait().get()
        }catch (e:Exception) {
            ""
        }
    }
    fun passPrompt(): String {
        val pd = PasswordDialog()
        val result = pd.showAndWait()

        return try {
           result.get()
        }catch (e:Exception) {
            ""
        }
    }


    fun infoCheck(title: String, header: String, body: String, type: Alert.AlertType): Boolean {

        val alert = Alert(type)
        alert.title = title
        alert.headerText = header
        alert.contentText = body
        alert.isResizable = false
        alert.graphic = null

        val result = alert.showAndWait() ?: return false
        return result.get() == ButtonType.OK

    }
}