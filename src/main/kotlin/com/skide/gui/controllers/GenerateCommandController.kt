package com.skide.gui.controllers

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.TextField

class GenerateCommandController {


    @FXML
    lateinit var commandNameField: TextField

    @FXML
    lateinit var aliasesField: TextField

    @FXML
    lateinit var executableField: TextField

    @FXML
    lateinit var descriptionField: TextField

    @FXML
    lateinit var permissionField: TextField

    @FXML
    lateinit var permissionMessageField: TextField

    @FXML
    lateinit var cooldownField: TextField

    @FXML
    lateinit var cooldownMessageField: TextField

    @FXML
    lateinit var cooldownBypassField: TextField

    @FXML
    lateinit var cooldownStorageField: TextField

    @FXML
    lateinit var usageField: TextField

    @FXML
    lateinit var cancelButton: Button

    @FXML
    lateinit var createButton: Button


    fun addline(prefix: String, field: TextField): String {
        return if (field.text.isNotEmpty())
            "\t$prefix: ${field.text}\n"
        else
            ""
    }

    fun generateString(): String {
        var out = "command /${commandNameField.text}:\n"
        out  += addline("aliases", aliasesField)
        out  += addline("executable by", executableField)
        out  += addline("usage", usageField)
        out  += addline("description", descriptionField)
        out  += addline("permission", permissionField)
        out  += addline("permission message", permissionMessageField)
        out  += addline("cooldown", cooldownField)
        out  += addline("cooldown message", cooldownMessageField)
        out  += addline("cooldown bypass", cooldownBypassField)
        out  += addline("cooldown storage", cooldownStorageField)
        out += "\ttrigger:\n\t\t# Code here"
        return out
    }
}