package com.skide.gui

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.input.KeyCode
import javafx.scene.input.MouseButton
import javafx.scene.layout.*
import javafx.scene.paint.Color
import javafx.stage.Stage
import javafx.stage.StageStyle
import java.util.*


class SearchPopUpItem(val primaryString: String, val secondaryString: String, val action: () -> Unit) : HBox() {

    private val labelLeft = Label()
    private val labelRight = Label()

    init {
        val pane = Pane()
        labelLeft.text = primaryString
        HBox.setHgrow(pane, Priority.ALWAYS);
        labelRight.text = secondaryString
        this.children.addAll(labelLeft, pane, labelRight)

    }
}

class SearchPopUp(val update: (List<SearchPopUpItem>, String) -> List<SearchPopUpItem>) {

    private var i = -1
    private val root = BorderPane()
    private val listView = ListView<SearchPopUpItem>()
    private val inputField = TextField()
    private val stage = Stage()
    private val bottomCheck = SearchPopUpItem("No Results...", "") {}

    private fun getSelected(): SearchPopUpItem? {
        return listView.selectionModel.selectedItem
    }

    init {
        bottomCheck.isDisable = true
        inputField.setOnKeyPressed {
            if (it.code == KeyCode.DOWN && listView.items.size != 0 && !listView.items.contains(bottomCheck)) {
                it.consume()
                listView.requestFocus()
            }
        }
        inputField.setOnKeyReleased {
            if (it.code == KeyCode.DOWN) return@setOnKeyReleased
            val text = inputField.text
            if (text.isEmpty()) {
                listView.items.clear()
            } else {
                val result = update(listView.items, text)
                val toRemove = Vector<SearchPopUpItem>()
                for (item in listView.items)
                    if (!result.contains(item)) toRemove.add(item)
                for (searchPopUpItem in result)
                    if (!listView.items.contains(searchPopUpItem)) listView.items.add(searchPopUpItem)

                for (searchPopUpItem in toRemove)
                    listView.items.remove(searchPopUpItem)
            }
            if (listView.items.size == 0)
                listView.items.add(bottomCheck)
            else
                if (listView.items.contains(bottomCheck))
                    listView.items.remove(bottomCheck)

            listView.prefHeight = ((listView.items.size * 24 + 2).toDouble())
            stage.sizeToScene()
        }
        root.top = inputField
        root.center = listView
        listView.setOnMouseClicked {
            if (it.button == MouseButton.PRIMARY && it.clickCount == 2) {
                val sel = getSelected()
                if (sel != null && sel != bottomCheck) {
                    sel.action()
                    stage.close()
                }
            }
        }
        listView.setOnKeyPressed {
            if (it.code == KeyCode.UP) {
                if (i == 0 && listView.selectionModel.selectedIndex == 0) {
                    it.consume()
                    inputField.requestFocus()
                    inputField.selectEnd()
                }
            } else if (it.code == KeyCode.ENTER) {
                val selected = getSelected()
                if (selected != null && selected != bottomCheck) {
                    selected.action()
                    stage.close()
                }
            } else if (it.code != KeyCode.DOWN) {
                val text = inputField.text + it.text
                it.consume()
                Platform.runLater {
                    inputField.requestFocus()
                    inputField.text = text

                    inputField.deselect()
                    inputField.positionCaret(text.length)
                }
            }
        }
        listView.selectionModel.selectedIndexProperty().addListener { _, _, newValue ->
            Platform.runLater {
                i = newValue.toInt()
            }
        }
        stage.focusedProperty().addListener { _, _, newValue ->
            if (!newValue) {
                stage.close()
            }
        }
        inputField.prefWidth = 400.0
        val scene = Scene(root)
        root.background = Background.EMPTY
        scene.fill = Color.TRANSPARENT
        stage.initStyle(StageStyle.UNDECORATED)
        stage.scene = scene
        listView.items.add(bottomCheck)
        listView.prefHeight = ((listView.items.size * 24 + 2).toDouble())
        if (GUIManager.settings.get("theme") == "Dark")
            root.stylesheets.add(GUIManager.settings.getCssPath("ThemeDark.css"))
        if (GUIManager.settings.get("global_font_size").toString().isNotEmpty())
            root.style = "-fx-font-size: ${GUIManager.settings.get("global_font_size")}px"
        stage.sizeToScene()
        stage.show()
    }
}