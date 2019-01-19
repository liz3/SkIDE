package com.skide.gui.controllers

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.PasswordField
import javafx.scene.control.TextField


class LoginPromptController {

    @FXML
    lateinit var titleLabel: Label

    @FXML
    lateinit var descriptionLabel: Label

    @FXML
    lateinit var nameField: TextField

    @FXML
    lateinit var passField: PasswordField

    @FXML
    lateinit var cancelBtn: Button

    @FXML
    lateinit var loginBtn: Button
}