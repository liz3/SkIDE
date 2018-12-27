package com.skide.gui

import com.skide.core.management.ConfigManager
import com.skide.utils.OperatingSystemType
import com.skide.utils.getOS
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.scene.control.TextInputDialog
import javafx.scene.layout.Region
import javafx.stage.StageStyle


object Prompts {

    var theme = ""
    lateinit var configManager: ConfigManager

    fun textPrompt(title: String, header: String, default:String = ""): String {
        val input = TextInputDialog(default)

        if (theme == "Dark") {
            val dialogPane = input.dialogPane
            dialogPane.stylesheets.add(configManager.getCssPath("ThemeDark.css"))
        }

        input.title = title
        input.headerText = header
        input.graphic = null
        if(getOS() == OperatingSystemType.LINUX)input.initStyle(StageStyle.UTILITY)

        return try {
            input.showAndWait().get()
        } catch (e: Exception) {
            ""
        }
    }

    fun passPrompt(): String {
        val pd = PasswordDialog()

        if (theme == "Dark") {
            val dialogPane = pd.dialogPane
            dialogPane.stylesheets.add(configManager.getCssPath("ThemeDark.css"))
        }
        if(getOS() == OperatingSystemType.LINUX)pd.initStyle(StageStyle.UTILITY)

        val result = pd.showAndWait()

        return try {
            result.get()
        } catch (e: Exception) {
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

        if(getOS() == OperatingSystemType.LINUX)alert.initStyle(StageStyle.UTILITY)
        if (theme == "Dark") {
            val dialogPane = alert.dialogPane
            dialogPane.stylesheets.add(configManager.getCssPath("ThemeDark.css"))
        }

        alert.dialogPane.minHeight = Region.USE_PREF_SIZE

        val result = alert.showAndWait() ?: return false
        return result.get() == ButtonType.OK

    }
}