package com.skide.gui.controllers

import com.skide.include.Addon
import javafx.fxml.FXML
import javafx.scene.control.*


class ProjectSettingsGuiController {

    @FXML
    lateinit var prNameTextField: TextField

    @FXML
    lateinit var skriptVersionComboBox: ComboBox<*>

    @FXML
    lateinit var plListView: ListView<Addon>

    @FXML
    lateinit var plUpdateBtn: Button

    @FXML
    lateinit var plNameLabel: Label

    @FXML
    lateinit var plDescriptionLabel: Label

    @FXML
    lateinit var plAuthorLabel: Label

    @FXML
    lateinit var plVersionsComboBox: ComboBox<String>

    @FXML
    lateinit var enableSupportCheckBox: CheckBox

    @FXML
    lateinit var applyBtn: Button

    @FXML
    lateinit var okBtn: Button

    @FXML
    lateinit var cancelBtn: Button

}