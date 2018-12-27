package com.skide.gui.project

import com.skide.core.code.CodeArea
import com.skide.core.management.OpenProject
import com.skide.include.SkError
import com.skide.include.SkErrorFront
import com.skide.include.SkErrorItem
import javafx.application.Platform
import javafx.scene.control.Tab
import javafx.scene.control.TreeItem
import javafx.scene.control.TreeView
import javafx.scene.input.MouseButton
import javafx.scene.layout.BorderPane
import java.io.File
import java.util.*


class ErrorFrontendHandler(val openProject: OpenProject) {

    private var busy = false
    private var rendered = false
    private val tab = Tab("Inspections")
    private val entries = HashMap<File, Vector<TreeItem<SkErrorFront>>>()
    private val treeView = TreeView<SkErrorFront>()
    private val root = TreeItem<SkErrorFront>()


    fun update(f: File, errors: Vector<SkError>, area: CodeArea) {
        if(busy) return
        Thread{
            busy = true
            val list = if (entries.containsKey(f)) {
                entries[f]!!
            } else {
                val list = Vector<TreeItem<SkErrorFront>>()
                entries[f] = list
                entries[f]!!
            }
            list.clear()
            for (error in errors) {
                list.add(TreeItem(SkErrorFront(SkErrorItem(error) {
                    area.moveLineToCenter(error.startLine)
                    area.setSelection(error.startLine, error.startColumn, error.endLine, error.endColumn)
                })))
            }
            Platform.runLater {
                entries[f] = list
                root.children.clear()
                for (entry in entries) {
                    val fileItem = TreeItem(SkErrorFront(entry.key.name + " - ${entry.value.size}"))
                    for (err in entry.value) {
                        fileItem.children.add(err)
                    }
                    root.children.add(fileItem)
                }
                busy = false
            }

        }.start()
    }

    fun render(): Tab {
        if(rendered) return tab
        rendered = true
        val pane = BorderPane()
        treeView.root = root
        treeView.isShowRoot = false
        treeView.setOnMouseClicked {
            if(it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                if(treeView.selectionModel.selectedItem != null) {
                    val item = treeView.selectionModel.selectedItem.value
                    if(item != null && item.value is SkErrorItem) item.value.cb()
                }
            }
        }
        pane.center = treeView


        tab.content = pane

        return tab
    }

}