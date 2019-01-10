package com.skide.gui.controllers

import com.skide.gui.settings.ColorListEntry
import com.skide.gui.settings.RuleListEntry
import com.skide.include.ColorScheme
import com.skide.include.Server
import com.skide.include.ServerAddon
import com.skide.include.Snippet
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.web.WebView
import javafx.scene.control.ComboBox






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
    lateinit var jvmStartAgsTextField: TextField

    @FXML
    lateinit var settingsTheneComboBox: ComboBox<String>

    @FXML
    lateinit var settingsFontTextField: TextField


    @FXML
    lateinit var globalFontSize: TextField

    @FXML
    lateinit var settingsFontSizeTextField: TextField

    @FXML
    lateinit var settingsAutoCompleteCheck: CheckBox

    @FXML
    lateinit var settingsAutoCompleteAddonCheck: CheckBox

    @FXML
    lateinit var settingsAutoCompleteSkriptCheck: CheckBox

    @FXML
    lateinit var webViewDebuggerCheck: CheckBox

    @FXML
    lateinit var settingsHighlightingCheck: CheckBox

    @FXML
    lateinit var crossFileAutoComplete: CheckBox
    @FXML
    lateinit var metaDataGenerateCheck: CheckBox
    @FXML
    lateinit var settingsUpdateDataCheck: CheckBox
    @FXML
    lateinit var analyiticsCheck: CheckBox

    @FXML
    lateinit var updateCheck: CheckBox
    @FXML
    lateinit var betaUpdateCheck: CheckBox

    @FXML
    lateinit var snippetTriggerReplaceSequence: CheckBox


    @FXML
    lateinit var snippetNewField: TextField

    @FXML
    lateinit var snippetNewBtn: Button

    @FXML
    lateinit var snippetListView: ListView<Snippet>

    @FXML
    lateinit var snippetDeleteBtn: Button

    @FXML
    lateinit var snippetLabelField: TextField

    @FXML
    lateinit var snippetContentArea: TextArea

    @FXML
    lateinit var snippetRulesContainer: HBox

    @FXML
    lateinit var schemesSelectComboBox: ComboBox<ColorScheme>

    @FXML
    lateinit var schemesNewTextField: TextField

    @FXML
    lateinit var schemesNewBtn: Button

    @FXML
    lateinit var schemesDeleteBtn: Button

    @FXML
    lateinit var schemesColorsList: ListView<ColorListEntry>

    @FXML
    lateinit var schemesRulesList: ListView<RuleListEntry>

    @FXML
    lateinit var schemesPreviewView: WebView

}