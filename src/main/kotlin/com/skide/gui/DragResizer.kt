package com.skide.gui

import javafx.scene.Cursor
import javafx.scene.layout.BorderPane

class DragResizer{
    private var windowHeight = 0.0
    private var mousePosY = 0.0
    private var bottomSecHeight = 0.0

    fun makeResizable(bottomSec: BorderPane){
        windowHeight = bottomSec.scene.height

        bottomSec.minHeight = 30.0

        bottomSec.scene.heightProperty().addListener{ _, _, newValue ->
            if (bottomSec.height > newValue.toDouble() - 30){
                //bottomSec.minHeight = newValue.toDouble() - 30
            }
        }

        bottomSec.setOnMouseMoved{ event ->
            windowHeight = bottomSec.scene.height

            mousePosY = event.sceneY

            bottomSecHeight = bottomSec.height

            if (canDrag(windowHeight, mousePosY, bottomSecHeight) && bottomSec.cursor !== Cursor.S_RESIZE){
                bottomSec.cursor = Cursor.S_RESIZE
            } else if (bottomSec.cursor === Cursor.S_RESIZE){
                bottomSec.cursor = Cursor.DEFAULT
            }
        }

        bottomSec.setOnMouseDragged{ event ->
            windowHeight = bottomSec.scene.height

            mousePosY = event.sceneY

            bottomSecHeight = bottomSec.height

            if (canDrag(windowHeight, mousePosY, bottomSecHeight)){
                bottomSec.prefHeight = if (mousePosY < 30) windowHeight - 30 else windowHeight - event.sceneY
            }
        }
    }

    private fun canDrag(windowHeight: Double, mousePosY: Double, bottomSecHeight: Double): Boolean{
        val bottomSecOffsetTop = windowHeight - bottomSecHeight

        val mousePosYRelativeToBottomSec = mousePosY - bottomSecOffsetTop

        return mousePosYRelativeToBottomSec <= 25
    }
}