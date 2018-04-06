package com.skide.gui

import com.skide.gui.project.OpenProjectGuiManager
import javafx.scene.control.TabPane
import javafx.scene.input.ClipboardContent
import javafx.scene.input.TransferMode

class MouseDragHandler(val pane: TabPane, val coreManager: OpenProjectGuiManager){
    fun setup(){
        pane.setOnDragDetected{
            println("${pane}Drag started")

            coreManager.draggedTab = pane.selectionModel.selectedItem

            /* drag was detected, start a drag-and-drop gesture*/
            /* allow any transfer mode */

            val db = pane.startDragAndDrop(*TransferMode.ANY)

            val content = ClipboardContent()

            content.putString("")

            db.setContent(content)

            it.consume()
        }

        pane.setOnDragDropped{
            println("${pane}Drag dropped")
            
            if(coreManager.draggedTab != null){
                coreManager.dragDone = true

                pane.tabs.add(coreManager.draggedTab)
            }else{
                if(it.dragboard.hasFiles()){
                    it.dragboard.files.forEach{
                        coreManager.openProject.eventManager.openFile(it, pane, true)
                    }
                }
            }

        }

        pane.setOnDragExited{
            println("${pane}Drag exited")
        }

        pane.setOnDragOver{
            if (it.gestureSource != pane && it.dragboard.hasString()){
                it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            }else if(it.dragboard.hasFiles()){
                println("Contains files")

                it.acceptTransferModes(*TransferMode.COPY_OR_MOVE)
            }

            it.consume()
        }

        pane.setOnDragDone{
            println("${pane}Drag done")

            if(coreManager.dragDone){
                pane.tabs.remove(coreManager.draggedTab)
            }

            coreManager.draggedTab = null

            coreManager.dragDone = false
        }
    }
}