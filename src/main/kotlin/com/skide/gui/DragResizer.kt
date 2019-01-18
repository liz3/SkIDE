package com.skide.gui

import com.skide.include.SkErrorFront
import javafx.scene.Cursor
import javafx.scene.control.TabPane
import javafx.scene.control.TreeView
import javafx.scene.layout.BorderPane


class DragResizer {

    private var windowHeight = 0.0
    private var mousePosY = 0.0
    private var bottomSecHeight = 0.0


    fun makeResizable(bottomSec: BorderPane, tabPane: TabPane) {
        windowHeight = bottomSec.scene.height

        bottomSec.minHeight = 30.0

        bottomSec.scene.heightProperty().addListener { _, _, newValue ->
            if (bottomSec.height > newValue.toDouble() - 30) {
                //bottomSec.minHeight = newValue.toDouble() - 30
            }
        }
        tabPane.setOnMouseMoved { event ->
            windowHeight = bottomSec.scene.height
            mousePosY = event.sceneY
            bottomSecHeight = bottomSec.height

            if (canDrag(windowHeight, mousePosY, bottomSecHeight) && bottomSec.cursor !== Cursor.S_RESIZE) {
                bottomSec.cursor = Cursor.S_RESIZE
            } else if (bottomSec.cursor === Cursor.S_RESIZE) {
                bottomSec.cursor = Cursor.DEFAULT
            }

        }

        tabPane.setOnMouseDragged { event ->
            windowHeight = bottomSec.scene.height
            mousePosY = event.sceneY
            bottomSecHeight = bottomSec.height
            if (bottomSec.cursor === Cursor.S_RESIZE) {
                bottomSec.prefHeight = if (mousePosY < 30) windowHeight - 30 else windowHeight - event.sceneY
            }
        }
    }

    fun makeResizable(bottomSec: BorderPane, tabPane: TreeView<SkErrorFront>) {
        windowHeight = bottomSec.scene.height

        bottomSec.minHeight = 30.0

        bottomSec.scene.heightProperty().addListener { _, _, newValue ->
            if (bottomSec.height > newValue.toDouble() - 30) {
                //bottomSec.minHeight = newValue.toDouble() - 30
            }
        }
        tabPane.setOnMouseMoved { event ->
            windowHeight = bottomSec.scene.height
            mousePosY = event.sceneY
            bottomSecHeight = bottomSec.height

            if (canDrag(windowHeight, mousePosY, bottomSecHeight) && bottomSec.cursor !== Cursor.S_RESIZE) {
                bottomSec.cursor = Cursor.S_RESIZE
            } else if (bottomSec.cursor === Cursor.S_RESIZE) {
                bottomSec.cursor = Cursor.DEFAULT
            }

        }

        tabPane.setOnMouseDragged { event ->
            windowHeight = bottomSec.scene.height
            mousePosY = event.sceneY
            bottomSecHeight = bottomSec.height
            if (bottomSec.cursor === Cursor.S_RESIZE) {
                bottomSec.prefHeight = if (mousePosY < 30) windowHeight - 30 else windowHeight - event.sceneY
            }
        }
    }

    fun makeResizable(bottomSec: BorderPane, tabPane: BorderPane) {
        windowHeight = bottomSec.scene.height

        bottomSec.minHeight = 30.0

        bottomSec.scene.heightProperty().addListener { _, _, newValue ->
            if (bottomSec.height > newValue.toDouble() - 30) {
                //bottomSec.minHeight = newValue.toDouble() - 30
            }
        }
        tabPane.setOnMouseMoved { event ->
            windowHeight = bottomSec.scene.height
            mousePosY = event.sceneY
            bottomSecHeight = bottomSec.height

            if (canDrag(windowHeight, mousePosY, bottomSecHeight) && bottomSec.cursor !== Cursor.S_RESIZE) {
                bottomSec.cursor = Cursor.S_RESIZE
            } else if (bottomSec.cursor === Cursor.S_RESIZE) {
                bottomSec.cursor = Cursor.DEFAULT
            }

        }

        tabPane.setOnMouseDragged { event ->
            windowHeight = bottomSec.scene.height
            mousePosY = event.sceneY
            bottomSecHeight = bottomSec.height
            if (bottomSec.cursor === Cursor.S_RESIZE) {
                bottomSec.prefHeight = if (mousePosY < 30) windowHeight - 30 else windowHeight - event.sceneY
            }
        }
    }


    private fun canDrag(windowHeight: Double, mousePosY: Double, bottomSecHeight: Double): Boolean {
        val bottomSecOffsetTop = windowHeight - bottomSecHeight
        val mousePosYRelativeToBottomSec = mousePosY - bottomSecOffsetTop
        return mousePosYRelativeToBottomSec <= 25
    }

}

class DragResizerLeft {

    private var windowWidth = 0.0
    private var mousePosY = 0.0
    private var secWidth = 0.0

    fun makeResizable(section: TreeView<String>, xPane: BorderPane) {
        windowWidth = xPane.scene.width


        section.setOnMouseMoved { event ->
            windowWidth = xPane.scene.width
            mousePosY = event.sceneX
            secWidth = section.width

            if (canDrag(mousePosY, secWidth) && section.cursor !== Cursor.S_RESIZE) {
                section.cursor = Cursor.H_RESIZE
            } else if (section.cursor === Cursor.H_RESIZE) {
                section.cursor = Cursor.DEFAULT
            }

        }

        section.setOnMouseDragged { event ->
            windowWidth = xPane.scene.width
            mousePosY = event.sceneX
            secWidth = section.width
            if (section.cursor === Cursor.H_RESIZE) {
                xPane.prefWidth = event.sceneX
            }
        }
    }

    private fun canDrag(mousePosY: Double, secWidth: Double): Boolean {
        val mousePosYRelativeToBottomSec = mousePosY - secWidth
        return mousePosYRelativeToBottomSec >= -2
    }
}