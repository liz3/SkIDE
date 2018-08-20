package com.skide.core.code

import com.skide.include.OpenFileHolder
import javafx.scene.control.Label
import javafx.stage.Popup
import java.time.Duration


class TooltipHandler(val codeManager: CodeManager, val project: OpenFileHolder) {

    var popup = Popup()
    var popupMsg = Label()

    val area = codeManager.area

    /*
    fun setup() {

        popupMsg.style = "-fx-background-color: black;" +
                "-fx-text-fill: white;" +
                "-fx-padding: 5;"
        popup.content.add(popupMsg)

        area.mouseOverTextDelay = Duration.ofSeconds(1)
        area.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_BEGIN) { e ->
            val chIdx = e.characterIndex
            val pos = e.screenPosition
            //popupMsg.text = "Character '" + area.getText(chIdx, chIdx + 1) + "' at " + pos

            val currLine = {
                val curr = area.caretPosition
                area.moveTo(chIdx)
                val result = area.getCaretLine() - 1
                area.moveTo(curr)

                result
            }.invoke()

            if (codeManager.marked.containsKey(currLine)) {
                popupMsg.text = project.coreManager.insightClient.getInspectionFromClass(codeManager.marked[currLine]?.inspectionClass)?.description
                popup.show(area, pos.x, pos.y + 10)

            }
        }
        area.addEventHandler(MouseOverTextEvent.MOUSE_OVER_TEXT_END) { popup.hide() }
    }
     */
}