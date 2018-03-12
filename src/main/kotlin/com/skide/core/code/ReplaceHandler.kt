package com.skide.core.code

import com.skide.gui.GuiManager
import com.skide.gui.controllers.ReplaceFrameController
import com.skide.include.OpenFileHolder
import com.skide.utils.StringSearchResult
import com.skide.utils.search
import javafx.scene.input.KeyCode

class ReplaceHandler(val manager: CodeManager, val project: OpenFileHolder) {

    private val area = manager.area
    private var visible = false
    private val node = GuiManager.getScene("ReplaceFrame.fxml")
    private var entries = ArrayList<StringSearchResult>()
    var executed = false
    var currentPoint = 0

    init {
        registerEvents()
    }

    fun switchGui() {

        if (visible) {
            manager.autoComplete.stopped = false
            project.borderPane.top = null
            clearSearchHighlighting()
            visible = false
        } else {
            manager.autoComplete.stopped = true
            project.borderPane.top = node.first
            visible = true
            (node.second as ReplaceFrameController).searchField.requestFocus()
            computeHighlight()

        }


    }

    private fun clearSearchHighlighting() {

        manager.highlighter.restartHighlighting()

    }

    private fun computeHighlight() {
        manager.highlighter.stopHighLighting()
    }

    private fun check() {

        val ctrl = node.second as ReplaceFrameController

        val result = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

        if (result.size != 0) {

            ctrl.nextEntry.isDisable = false
            ctrl.prevEntry.isDisable = false
            ctrl.replaceAllBtn.isDisable = false
            ctrl.replaceBtn.isDisable = false

            executed = true
            entries = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

            if (entries.size == 0) return

            area.selectRange(0, 0)
            area.moveTo(entries[currentPoint].start)
            area.selectRange(entries[currentPoint].start, entries[currentPoint].end)

        } else {

            ctrl.nextEntry.isDisable = true
            ctrl.prevEntry.isDisable = true
            ctrl.replaceAllBtn.isDisable = true
            ctrl.replaceBtn.isDisable = true
        }


    }
    private fun registerEvents() {

        val ctrl = node.second as ReplaceFrameController

        ctrl.searchField.setOnKeyReleased {
            if (it.code == KeyCode.ESCAPE) {
                project.borderPane.top = null
                clearSearchHighlighting()
                visible = false
                area.requestFocus()

            }
            if (it.code == KeyCode.ENTER) {
                ctrl.replaceField.requestFocus()
            }
        }

        ctrl.searchField.textProperty().addListener { _, _, _ ->
            if (ctrl.searchField.text == "") {
                return@addListener
            }
            currentPoint = 0
            executed
            entries.clear()
            manager.highlighter.searchHighlighting(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected)

            check()
        }

        ctrl.nextEntry.setOnAction {

            if (!executed) {
                executed = true
                entries = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

                if (entries.size == 0) return@setOnAction

                area.selectRange(0, 0)
                if (entries.size == 1) {
                    area.moveTo(entries.first().start)
                    area.selectRange(entries.first().start, entries.first().end)
                    return@setOnAction
                }
                area.moveTo(entries[currentPoint + 1].start)
                area.selectRange(entries[currentPoint + 1].start, entries[currentPoint + 1].end)
                currentPoint++


            } else {
                if (entries.size == 0) return@setOnAction

                area.selectRange(0, 0)

                if (entries.size == 1) {

                    return@setOnAction
                }
                if (currentPoint == entries.size - 1) {
                    currentPoint = 0
                    area.moveTo(entries.first().start)
                    area.selectRange(entries.first().start, entries.first().end)
                    return@setOnAction
                }
                area.moveTo(entries[currentPoint + 1].start)
                area.selectRange(entries[currentPoint + 1].start, entries[currentPoint + 1].end)
                currentPoint++
            }
        }
        ctrl.replaceField.setOnKeyReleased {
            if(it.code == KeyCode.ENTER) {
               replace()
            }
        }
        ctrl.caseSensitive.setOnAction {
            if (ctrl.searchField.text == "") {
                return@setOnAction
            }
            currentPoint = 0
            executed
            entries.clear()
            manager.highlighter.searchHighlighting(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected)
        }
        ctrl.regexEnableCheck.setOnAction {
            if (ctrl.searchField.text == "") {
                return@setOnAction
            }
            currentPoint = 0
            executed
            entries.clear()
            manager.highlighter.searchHighlighting(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected)
        }
        ctrl.replaceBtn.setOnAction {
            replace()
        }

        ctrl.replaceAllBtn.setOnAction {
            if (!executed) {
                executed = true
                entries = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

            }
            if (entries.size == 0) return@setOnAction

            val toReplace = ctrl.replaceField.text
            while (entries.size != 0) {
                area.replaceText(entries.first().start, entries.first().end, toReplace)
                entries = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>
            }
            executed = false
            entries.clear()
        }
        ctrl.prevEntry.setOnAction {

            if (!executed) {
                executed = true
                entries = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

                if (entries.size == 0) return@setOnAction

                area.selectRange(0, 0)
                if (entries.size == 1) {
                    area.moveTo(entries.first().start)
                    area.selectRange(entries.first().start, entries.first().end)
                    return@setOnAction
                }
                area.moveTo(entries[currentPoint + 1].start)
                area.selectRange(entries[currentPoint + 1].start, entries[currentPoint + 1].end)
                currentPoint++


            } else {
                if (entries.size == 0) return@setOnAction

                area.selectRange(0, 0)

                if (entries.size == 1) {

                    return@setOnAction
                }
                if (currentPoint == 0) {
                    currentPoint = entries.size - 1
                    area.moveTo(entries.last().start)
                    area.selectRange(entries.last().start, entries.last().end)
                    return@setOnAction
                }

                area.moveTo(entries[currentPoint - 1].start)
                area.selectRange(entries[currentPoint - 1].start, entries[currentPoint - 1].end)
                currentPoint--
            }
        }
    }

    private fun replace() {

        val ctrl = node.second as ReplaceFrameController

        if (!executed) {
            executed = true
            entries = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

        }
        if (entries.size == 0) return


        val entry = entries[currentPoint]
        val toReplace = ctrl.replaceField.text

        area.replaceText(entry.start, entry.end, toReplace)
        var index = entries.indexOf(entry)
        entries = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

        if (index < 0) index = 0
        if (entries.size != 0) {
            area.moveTo(entries[index].start)
            area.selectRange(entries[index].start, entries[index].end)
            if (currentPoint < 0) currentPoint = 0
        } else if (entries.size == 0) executed = false

    }
}