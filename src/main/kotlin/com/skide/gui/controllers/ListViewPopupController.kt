package com.skide.gui.controllers

import com.skide.gui.ListViewPopUpItem
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.ListView


class ListViewPopupController {

    @FXML
    lateinit var infoLabel: Label

    @FXML
    lateinit var list: ListView<ListViewPopUpItem>

    @FXML
    lateinit var okBtn: Button

    @FXML
    lateinit var cancelBtn: Button
}