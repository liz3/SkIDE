package com.skide.core.code

import com.skide.gui.GUIManager
import com.skide.gui.controllers.FindFrameController
import com.skide.gui.controllers.ReplaceFrameController
import com.skide.include.OpenFileHolder
import com.skide.utils.StringSearchResult
import com.skide.utils.search
import com.skide.utils.setIcon
import javafx.scene.input.KeyCode
import kotlin.collections.ArrayList


class FindHandler(val manager: CodeManager, val project: OpenFileHolder) {

    private val area = manager.area
    private var visible = false
    private val node = GUIManager.getScene("SearchFrame.fxml")
    private var entries = ArrayList<StringSearchResult>()
    var executed = false
    var currentPoint = 0

    init {
        (node.second as FindFrameController).closeBtn.setIcon("delete", false)
        (node.second as FindFrameController).closeBtn.setOnAction {
            switchGui()
        }
        registerEvents()
    }

    fun switchGui() {

        if (visible) {
            clearSearchHighlighting()
            project.borderPane.top = null
            visible = false
            manager.autoComplete.stopped = false
        } else {
            manager.autoComplete.stopped = true
            project.borderPane.top = node.first
            visible = true
            (node.second as FindFrameController).searchField.requestFocus()
            computeHighlight()

        }


    }

    private fun clearSearchHighlighting() {

        if (project.coreManager.configManager.get("highlighting") == "true") manager.highlighter.restartHighlighting()

    }

    private fun computeHighlight() {
        if (project.coreManager.configManager.get("highlighting") == "true") manager.highlighter.stopHighLighting()
    }

    private fun check() {

        val ctrl = node.second as FindFrameController

        val result = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

        if (result.size != 0) {

            ctrl.nextEntry.isDisable = false
            ctrl.prevEntry.isDisable = false


            executed = true
            entries = area.text.search(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected) as ArrayList<StringSearchResult>

            if (entries.size == 0) return

            area.selectRange(0, 0)

            area.moveTo(entries[currentPoint].start)
            area.selectRange(entries[currentPoint].start, entries[currentPoint].end)
            currentPoint++

        } else {

            ctrl.nextEntry.isDisable = true
            ctrl.prevEntry.isDisable = true
        }

    }

    private fun registerEvents() {

        val ctrl = node.second as FindFrameController

        ctrl.searchField.setOnKeyReleased {
            if (it.code == KeyCode.ESCAPE) {
                switchGui()

            }
        }
        ctrl.searchField.textProperty().addListener { _, _, _ ->
            if (ctrl.searchField.text == "") {
                return@addListener
            }
            currentPoint = 0
            executed = false
            entries.clear()
            if (project.coreManager.configManager.get("highlighting") == "true") manager.highlighter.searchHighlighting(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected)


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
                    area.moveTo(entries[0].start)
                    area.selectRange(entries[0].start, entries[0].end)
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
        ctrl.caseSensitive.setOnAction {
            if (ctrl.searchField.text == "") {
                return@setOnAction
            }
            currentPoint = 0
            executed = false
            entries.clear()
            if (project.coreManager.configManager.get("highlighting") == "true") manager.highlighter.searchHighlighting(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected)
            check()
        }
        ctrl.regexEnableCheck.setOnAction {
            if (ctrl.searchField.text == "") {
                return@setOnAction
            }
            currentPoint = 0
            executed = false
            entries.clear()
            if (project.coreManager.configManager.get("highlighting") == "true") manager.highlighter.searchHighlighting(ctrl.searchField.text, ctrl.caseSensitive.isSelected, ctrl.regexEnableCheck.isSelected)
            check()
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

                    area.moveTo(entries[0].start)
                    area.selectRange(entries[0].start, entries[0].end)
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
}

