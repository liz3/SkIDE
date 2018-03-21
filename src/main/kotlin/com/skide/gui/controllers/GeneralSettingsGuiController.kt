package com.skide.gui.controllers
import com.skide.include.Server
import com.skide.include.ServerAddon
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import javafx.scene.control.TextField


class GeneralSettingsGuiController {

    @FXML
    lateinit var serverServerList: ListView<Server>

    @FXML
    lateinit var serverSkriptVersionComboBox: ComboBox<String>

    @FXML
    lateinit var serverServerPathTextField: TextField

    @FXML
    lateinit var serverServerFolderPathChooseBtn: Button

    @FXML
    lateinit var serverNewServerNameTextField: TextField

    @FXML
    lateinit var serverNewServerCreateBtn: Button

    @FXML
    lateinit var serverServerFolderPathTextField: TextField

    @FXML
    lateinit var serverServertPathChooseBtn: Button

    @FXML
    lateinit var serverServerNameTextField: TextField

    @FXML
    lateinit var serverAddonList: ListView<ServerAddon>

    @FXML
    lateinit var serverAddAddonFromFileChooseBtn: Button

    @FXML
    lateinit var serverAddAddonFromPresetComboBox: ComboBox<*>

    @FXML
    lateinit var serverAddAddonFromPresetBtn: Button

    @FXML
    lateinit var serverAddAddonFromFileBtn: Button

    @FXML
    lateinit var serverServerDeleteBtn: Button

    @FXML
    lateinit var serverAddonDeleteBtn: Button

    @FXML
    lateinit var serverAddAddonFromFileTextField: TextField

    @FXML
    lateinit var okBtn: Button

    @FXML
    lateinit var cancelBtn: Button

    @FXML
    lateinit var applyBtn: Button

    @FXML
    lateinit var serverStartAgsTextField:TextField

}