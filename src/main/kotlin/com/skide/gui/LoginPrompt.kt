package com.skide.gui

import com.skide.gui.controllers.LoginPromptController
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.input.KeyCode
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.stage.StageStyle


class LoginPrompt(val title:String, val description:String, val cb:(Boolean, String, String) -> Unit) {

    init {
     try {
         val loader = FXMLLoader()
         val root = loader.load<VBox>(this.javaClass.getResourceAsStream("/fxml/LoginForm.fxml"))
         val controller = loader.getController<LoginPromptController>()
         val stage = Stage()
         val scene = Scene(root)
         stage.title = title
         stage.scene = scene
         stage.isResizable = false
         stage.sizeToScene()
         controller.descriptionLabel.text = description
         controller.titleLabel.text = title
         controller.cancelBtn.setOnAction {
             stage.close()
             cb(false, controller.nameField.text, controller.passField.text)
         }
         controller.loginBtn.setOnAction {
             stage.close()
             cb(true, controller.nameField.text, controller.passField.text)
         }
         controller.passField.setOnKeyPressed {
             if(it.code == KeyCode.ENTER) {
                 stage.close()
                 cb(true, controller.nameField.text, controller.passField.text)
             }
         }
         stage.show()
     }catch (e:Exception) {
         e.printStackTrace()
     }
    }

}