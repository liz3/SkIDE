package com.skide.gui.controllers
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.MenuBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;


class ProjectGuiController {

    @FXML
    lateinit var rootBorderPane: BorderPane

    @FXML
    lateinit var mainRightBorderPane: BorderPane

    @FXML
    lateinit var mainLeftBorderPane: BorderPane

    @FXML
    lateinit var browserTabPane: TabPane

    @FXML
    lateinit var mainUpperBorderPane: BorderPane

    @FXML
    lateinit var mainBenuBar: MenuBar

    @FXML
    lateinit var controlsHBox: HBox

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
}