package com.skide.gui.controllers

import javafx.scene.control.CheckBox
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.TextArea
import javafx.scene.control.TextField


class FindFrameController {

    @FXML
    lateinit var searchField: TextField

    @FXML
    lateinit var prevEntry: Button

    @FXML
    lateinit var nextEntry: Button

    @FXML
    lateinit var regexEnableCheck: CheckBox

    @FXML
    lateinit var caseSensitive: CheckBox

}

class ReplaceFrameController {

    @FXML
    lateinit var searchField: TextField

    @FXML
    lateinit var prevEntry: Button

    @FXML
    lateinit var nextEntry: Button

    @FXML
    lateinit var regexEnableCheck: CheckBox

    @FXML
    lateinit var caseSensitive: CheckBox

    @FXML
    lateinit var replaceField: TextField

    @FXML
    lateinit var replaceBtn: Button

    @FXML
    lateinit var replaceAllBtn: Button

}

class SkunityQuestionFameController {

    @FXML
    lateinit var sendBtn: Button

    @FXML
    lateinit var cancelBtn: Button

    @FXML
    lateinit var titleBar: TextField

    @FXML
    lateinit var contentArea: TextArea

}