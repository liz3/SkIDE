package com.skide.gui.controllers

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.TextField

class GenerateCommandController {

    @FXML
    lateinit var descriptionField: TextField
    @FXML
    lateinit var commandNameField: TextField
    @FXML
    lateinit var permissionField: TextField
    @FXML
    lateinit var createButton: Button
    @FXML
    lateinit var cancelButton: Button
}