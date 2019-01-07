package com.skide.gui.controllers

import com.skide.gui.settings.TypeListEntry
import javafx.fxml.FXML
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.TextField


class SnippetRuleController {

    @FXML
    lateinit var typeList: ListView<TypeListEntry>

    @FXML
    lateinit var startsWithCheck: CheckBox

    @FXML
    lateinit var startsWithField: TextField

    @FXML
    lateinit var containsCheck: CheckBox

    @FXML
    lateinit var containsField: TextField

    @FXML
    lateinit var endsWithCheck: CheckBox

    @FXML
    lateinit var titleLabel: Label

    @FXML
    lateinit var endsWithField: TextField
}