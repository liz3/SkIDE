package com.skide.gui

import com.skide.gui.controllers.ListViewPopupController
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.input.MouseButton
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle


class ListViewPopUpItem(val name:String, val cb: () -> Unit) {
    override fun toString(): String {
        return name
    }
}
class ListViewPopUp(val info:String, val items:HashMap<String, () -> Unit>, done: () -> Unit) {




    init {

        Platform.runLater {
            val stage = Stage()
            stage.initStyle(StageStyle.UTILITY)
            stage.initModality(Modality.WINDOW_MODAL)
            stage.isResizable = false
            stage.centerOnScreen()

            val scene = GUIManager.getScene("ListView.fxml")

            stage.scene = Scene(scene.first)
            stage.sizeToScene()
            val controller = scene.second as ListViewPopupController

            controller.cancelBtn.setOnAction {
                stage.close()
            }

            for((key, cb) in items) {
                controller.list.items.add(ListViewPopUpItem(key,cb))
            }

            controller.infoLabel.text = info
            controller.okBtn.setOnAction {
                if(controller.list.selectionModel.selectedItem != null)
                    controller.list.selectionModel.selectedItem.cb()
                stage.close()
            }
            controller.list.setOnMouseClicked { mouseEvent ->
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mouseEvent.clickCount == 2) {
                        if(controller.list.selectionModel.selectedItem != null)
                            controller.list.selectionModel.selectedItem.cb()
                        stage.close()

                    }
                }
            }
            if (GUIManager.settings.get("theme") == "Dark") {
                stage.scene.stylesheets.add(GUIManager.settings.getCssPath("ThemeDark.css"))
            }
            stage.show()
        }
    }
}