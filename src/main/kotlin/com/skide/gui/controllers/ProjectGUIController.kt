package com.skide.gui.controllers

import javafx.fxml.FXML;
import javafx.scene.control.*
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;


class ProjectGUIController {

    @FXML
    lateinit var rootBorderPane: BorderPane

    @FXML
    lateinit var mainLowerBorderPane: BorderPane

    @FXML
    lateinit var consoleTabArea: TabPane

    @FXML
    lateinit var consoleAddBtn: Button

    @FXML
    lateinit var consoleRemBtn: Button

    @FXML
    lateinit var mainLeftBorderPane: BorderPane

    @FXML
    lateinit var browserTabPane: TabPane

    @FXML
    lateinit var mainUpperBorderPane: BorderPane

    @FXML
    lateinit var mainBenuBar: MenuBar

    @FXML
    lateinit var mainCenterAnchorPane: AnchorPane

    @FXML
    lateinit var editorMainTabPane: TabPane

    @FXML
    lateinit var templateTab: Tab

    @FXML
    lateinit var templateTabBorderPane: BorderPane

    @FXML
    lateinit var templateEditorTextArea: TextArea

    @FXML
    lateinit var herachieLabel: Label

    @FXML
    lateinit var lowerTabPane: TabPane

    @FXML
    lateinit var lowerTabPaneToggleBtn: Button

    @FXML
    lateinit var runsTabPane: TabPane

    @FXML
    lateinit var toggleTreeViewButton: Button

    @FXML
    lateinit var browserUpperHBox: HBox

    @FXML
    lateinit var activeSideLabel:Label
}