package com.skide.gui.controllers

import com.skide.include.Server
import com.skide.include.ServerAddon
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ComboBox
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.control.CheckBox


class GeneralSettingsGUIController {

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
    lateinit var serverStartAgsTextField: TextField

    @FXML
    lateinit var settingsTheneComboBox: ComboBox<String>

    @FXML
    lateinit var settingsFontTextField: TextField

    @FXML
    lateinit var settingsFontSizeTextField: TextField

    @FXML
    lateinit var settingsAutoCompleteCheck: CheckBox

    @FXML
    lateinit var settingsHighlightingCheck: CheckBox

    @FXML
    lateinit var settingsCssFileBtn: Button

    @FXML
    lateinit var keyParenField: TextField

    @FXML
    lateinit var keyBracketField: TextField

    @FXML
    lateinit var keyCurlyBracket: TextField

    @FXML
    lateinit var keyQuoteField: TextField

    @FXML
    lateinit var crossFileAutoComplete: CheckBox

    @FXML
    lateinit var autoCompleteCutField: TextField

    @FXML
    lateinit var fixesCutField: TextField

    @FXML
    lateinit var settingsInsightsCheck:CheckBox

    @FXML
    lateinit var metaDataGenerateCheck:CheckBox

    @FXML
    lateinit var settingsInsightsPortField:TextField
}