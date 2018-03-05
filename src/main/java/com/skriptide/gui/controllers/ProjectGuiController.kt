package com.skriptide.gui.controllers
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
    var rootBorderPane: BorderPane? = null

    @FXML
    var mainRightBorderPane: BorderPane? = null

    @FXML
    var mainLeftBorderPane: BorderPane? = null

    @FXML
    var browserTabPane: TabPane? = null

    @FXML
    var mainUpperBorderPane: BorderPane? = null

    @FXML
    var mainBenuBar: MenuBar? = null

    @FXML
    var controlsHBox: HBox? = null

    @FXML
    var mainCenterAnchorPane: AnchorPane? = null

    @FXML
    var editorMainTabPane: TabPane? = null

    @FXML
    var templateTab: Tab? = null

    @FXML
    var templateTabBorderPane: BorderPane? = null

    @FXML
    var templateEditorTextArea: TextArea? = null

    @FXML
    var herachieLabel: Label? = null
}