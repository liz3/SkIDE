package com.skide.gui

import com.skide.CoreManager
import com.skide.gui.project.OpenProjectGuiManager
import com.skide.gui.project.ProjectGuiEventListeners
import javafx.scene.control.TabPane
import jdk.nashorn.internal.objects.NativeRegExp.source
import jnr.ffi.util.BufferUtil.putString
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode
import javafx.scene.input.Dragboard


class MouseDragHandler(val pane: TabPane, val coreManager: OpenProjectGuiManager) {


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