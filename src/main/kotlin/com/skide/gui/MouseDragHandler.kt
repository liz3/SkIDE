package com.skide.gui

import com.skide.gui.project.OpenProjectGuiManager
import javafx.scene.control.TabPane
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.layout.VBox


class MouseDragHandler(val pane: TabPane, val coreManager: OpenProjectGuiManager) {


    fun registerPreviewPane(pPane: VBox) {

        pPane.setOnDragOver {
            if (it.dragboard.hasFiles()) {
                it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            }

            it.consume()
        }
        pPane.setOnDragDropped {

            if (it.dragboard.hasFiles()) {
                coreManager.openProject.eventManager.disablePreview()
                it.dragboard.files.forEach {
                    coreManager.openProject.eventManager.openFile(it, pane, true)
                }
            }

        }
    }

    fun setup() {

        pane.setOnDragDetected {
            coreManager.draggedTab = pane.selectionModel.selectedItem
            val db = pane.startDragAndDrop(*TransferMode.ANY)
            val content = ClipboardContent()
            content.putString("")
            db.setContent(content)

            it.consume()

        }
        pane.setOnDragDropped {
            if (coreManager.draggedTab != null) {
                coreManager.dragDone = true
                pane.tabs.add(coreManager.draggedTab)
            } else {
                if (it.dragboard.hasFiles()) {
                    it.dragboard.files.forEach {
                        coreManager.openProject.eventManager.openFile(it, pane, true)
                    }
                }
            }
        }
        pane.setOnDragOver {
            if (it.gestureSource != pane &&
                    it.dragboard.hasString()) {
                it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            } else if (it.dragboard.hasFiles()) {
                it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            }

            it.consume()
        }
        pane.setOnDragDone {
            if (coreManager.dragDone) {
                pane.tabs.remove(coreManager.draggedTab)

            }
            coreManager.draggedTab = null
            coreManager.dragDone = false

        }
    }
}