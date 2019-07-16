package com.skide.gui

import com.skide.gui.project.OpenProjectGuiManager
import javafx.application.Platform
import javafx.scene.control.TabPane
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.layout.VBox


class MouseDragHandler(val pane: TabPane, val coreManager: OpenProjectGuiManager) {


    fun registerPreviewPane(pPane: VBox) {

        pPane.setOnDragOver {
            if (it.gestureSource != pane &&
                    it.dragboard.hasString()) {
                it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            } else if (it.dragboard.hasFiles()) {
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
            } else if (coreManager.draggedTab != null) {
                coreManager.openProject.eventManager.disablePreview()
                val tab = coreManager.draggedTab
                tab?.tabPane?.tabs?.remove(tab)
                pane.tabs.add(tab)
                Platform.runLater {
                    pane.selectionModel.select(tab)
                    coreManager.draggedTab = null
                    coreManager.dragDone = false
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
                if(pane.tabs.contains(coreManager.draggedTab)) return@setOnDragDropped;
                coreManager.dragDone = true
                val tab = coreManager.draggedTab
                tab?.tabPane?.tabs?.remove(tab)
                pane.tabs.add(tab)
                Platform.runLater {
                    pane.selectionModel.select(tab)
                }
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

            coreManager.draggedTab = null
            coreManager.dragDone = false

        }
    }
}