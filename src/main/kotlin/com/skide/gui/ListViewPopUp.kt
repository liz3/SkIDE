package com.skide.gui

import com.skide.gui.controllers.ListViewPopupController
import javafx.application.Platform
import javafx.scene.Scene
import javafx.scene.input.MouseButton
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle


class ListViewPopUpItem(val name: String, val cb: () -> Unit) {
    override fun toString() = name
}

class ListViewPopUp(val info: String, val items: HashMap<String, () -> Unit>, done: () -> Unit = {}) {

    init {
        Platform.runLater {
            val stage = Stage()
            val scene = GUIManager.getScene("fxml/ListView.fxml")
            val controller = scene.second as ListViewPopupController


            stage.initStyle(StageStyle.UTILITY)
            stage.initModality(Modality.WINDOW_MODAL)
            stage.isResizable = false
            stage.centerOnScreen()
            stage.scene = Scene(scene.first)
            stage.sizeToScene()

            controller.okBtn.isDisable = true
            controller.list.selectionModel.selectedItemProperty().addListener { _, _, newValue ->

                controller.okBtn.isDisable = newValue == null

            }
            controller.infoLabel.text = info

            for ((key, cb) in items)
                controller.list.items.add(ListViewPopUpItem(key, cb))


            controller.cancelBtn.setOnAction {
                stage.close()
                done()
            }
            controller.okBtn.setOnAction {
                if (controller.list.selectionModel.selectedItem != null)
                    controller.list.selectionModel.selectedItem.cb()
                stage.close()
                done()

            }
            controller.list.setOnMouseClicked { mouseEvent ->
                if (mouseEvent.button == MouseButton.PRIMARY) {
                    if (mouseEvent.clickCount == 2) {
                        if (controller.list.selectionModel.selectedItem != null)
                            controller.list.selectionModel.selectedItem.cb()
                        stage.close()
                        done()

                    }
                }
            }
            stage.show()
        }
    }
}