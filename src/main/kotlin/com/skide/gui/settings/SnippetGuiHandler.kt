package com.skide.gui.settings

import com.skide.CoreManager
import com.skide.gui.GUIManager
import com.skide.gui.controllers.GeneralSettingsGUIController
import com.skide.gui.controllers.SnippetRuleController
import com.skide.include.NodeType
import com.skide.include.Snippet
import com.skide.include.SnippetRule
import com.skide.include.toVector
import com.skide.utils.deleteDirectoryRecursion
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import java.util.*


class TypeListEntry(val theType: NodeType,val action: (Boolean) -> Unit) : HBox() {

    private val labelLeft = Label()
    val checkBox = CheckBox()

    init {
        checkBox.selectedProperty().addListener { _, _, newValue ->
            action(newValue)
        }
        val pane = Pane()
        labelLeft.text = theType.toString()
        HBox.setHgrow(pane, Priority.ALWAYS)
        checkBox.text = ""
        this.children.addAll(labelLeft, pane, checkBox)

    }

}

class SnippetGuiHandler(val ctrl: GeneralSettingsGUIController, val coreManager: CoreManager) {

    private val localSnippets = Vector<Snippet>()

    private val rootPane = {
        val pane = GUIManager.getScene("fxml/SnippetRuleGui.fxml")
        val pCtrl = pane.second as SnippetRuleController
        pCtrl.titleLabel.text = "Root line of Tree"
        pCtrl.startsWithCheck.setOnAction { _ ->
            runAsserted {
                val currentVal = it.rootRule.startsWith.second
                it.rootRule.startsWith = Pair(pCtrl.startsWithCheck.isSelected, currentVal)
                pCtrl.startsWithField.isDisable = !pCtrl.startsWithCheck.isSelected

            }
        }
        pCtrl.containsCheck.setOnAction { _ ->
            runAsserted {
                val currentVal = it.rootRule.contains.second
                it.rootRule.contains = Pair(pCtrl.containsCheck.isSelected, currentVal)
                pCtrl.containsField.isDisable = !pCtrl.containsCheck.isSelected

            }
        }
        pCtrl.endsWithCheck.setOnAction { ev ->
            runAsserted {
                val currentVal = it.rootRule.endsWith.second
                it.rootRule.endsWith = Pair(pCtrl.endsWithCheck.isSelected, currentVal)
                pCtrl.endsWithField.isDisable = !pCtrl.endsWithCheck.isSelected
            }
        }
        pCtrl.startsWithField.textProperty().addListener { _, _, newValue ->
            runAsserted {
                val currentVal = it.rootRule.startsWith.first
                it.rootRule.startsWith = Pair(currentVal, newValue)
            }
        }
        pCtrl.containsField.textProperty().addListener { _, _, newValue ->
            runAsserted {
                val currentVal = it.rootRule.contains.first
                it.rootRule.contains = Pair(currentVal, newValue)
            }
        }
        pCtrl.endsWithField.textProperty().addListener { _, _, newValue ->
            runAsserted {
                val currentVal = it.rootRule.endsWith.first
                it.rootRule.endsWith = Pair(currentVal, newValue)
            }
        }
        for(value in NodeType.values()) {
            pCtrl.typeList.items.add(TypeListEntry(value) {v ->
                runAsserted {
                    if(!it.rootRule.allowedTypes.contains(value) && v) {
                        it.rootRule.allowedTypes.add(value)
                    } else if(!v && it.rootRule.allowedTypes.contains(value)) {
                        it.rootRule.allowedTypes.remove(value)
                    }
                }
            })

        }
        ctrl.snippetRulesContainer.children.add(pane.first)
        pCtrl
    }.invoke()
    private val parentPane = {
        val pane = GUIManager.getScene("fxml/SnippetRuleGui.fxml")
        val pCtrl = pane.second as SnippetRuleController
        pCtrl.titleLabel.text = "Parent line"
        pCtrl.startsWithCheck.setOnAction { ev ->
            runAsserted {
                val currentVal = it.parentRule.startsWith.second
                it.parentRule.startsWith = Pair(pCtrl.startsWithCheck.isSelected, currentVal)
                pCtrl.startsWithField.isDisable = !pCtrl.startsWithCheck.isSelected
            }
        }
        pCtrl.containsCheck.setOnAction { ev ->
            runAsserted {
                val currentVal = it.parentRule.contains.second
                it.parentRule.contains = Pair(pCtrl.containsCheck.isSelected, currentVal)
                pCtrl.containsField.isDisable = !pCtrl.containsCheck.isSelected
            }
        }
        pCtrl.endsWithCheck.setOnAction { ev ->
            runAsserted {
                val currentVal = it.parentRule.endsWith.second
                it.parentRule.endsWith = Pair(pCtrl.endsWithCheck.isSelected, currentVal)
                pCtrl.endsWithField.isDisable = !pCtrl.endsWithCheck.isSelected
            }
        }
        pCtrl.startsWithField.textProperty().addListener { _, _, newValue ->
            runAsserted {
                val currentVal = it.parentRule.startsWith.first
                it.parentRule.startsWith = Pair(currentVal, newValue)
            }
        }
        pCtrl.containsField.textProperty().addListener { _, _, newValue ->
            runAsserted {
                val currentVal = it.parentRule.contains.first
                it.parentRule.contains = Pair(currentVal, newValue)
            }
        }
        pCtrl.endsWithField.textProperty().addListener { _, _, newValue ->
            runAsserted {
                val currentVal = it.parentRule.endsWith.first
                it.parentRule.endsWith = Pair(currentVal, newValue)
            }
        }
        for(value in NodeType.values()) {
            pCtrl.typeList.items.add(TypeListEntry(value) {v ->
                runAsserted {
                    if(!it.parentRule.allowedTypes.contains(value) && v) {
                        it.parentRule.allowedTypes.add(value)
                    } else if(!v && it.parentRule.allowedTypes.contains(value)) {
                        it.parentRule.allowedTypes.remove(value)
                    }
                }
            })

        }
        ctrl.snippetRulesContainer.children.add(pane.first)
        pCtrl
    }.invoke()

    fun applyChanges() {

        Thread {
            coreManager.snippetManager.snippets.clear()
            coreManager.snippetManager.snippets += localSnippets
            coreManager.snippetManager.saveSnippets()

        }.start()
    }

    private fun getSnippet(name: String): Snippet {

        return Snippet(System.currentTimeMillis(), name, "", "", SnippetRule(NodeType.values().toVector(), Pair(false, ""), Pair(false, ""), Pair(false, "")), SnippetRule(NodeType.values().toVector(), Pair(false, ""), Pair(false, ""), Pair(false, "")), true)
    }
    private fun resetValues() {

        ctrl.snippetLabelField.text = ""
        ctrl.snippetContentArea.text = ""

        //root
        for (x in rootPane.typeList.items)
            x.checkBox.isSelected = false
        rootPane.startsWithCheck.isSelected = false
        rootPane.containsCheck.isSelected = false
        rootPane.endsWithCheck.isSelected = false
        rootPane.startsWithField.text = ""
        rootPane.containsField.text = ""
        rootPane.endsWithField.text = ""


        //parent
        for (x in parentPane.typeList.items)
            x.checkBox.isSelected = false
        parentPane.startsWithCheck.isSelected = false
        parentPane.containsCheck.isSelected = false
        parentPane.endsWithCheck.isSelected = false
        parentPane.startsWithField.text = ""
        parentPane.containsField.text = ""
        parentPane.endsWithField.text = ""

    }
    private fun setupItem() {

        ctrl.snippetLabelField.text = current().label
        ctrl.snippetContentArea.text = current().insertText
        ctrl.snippetTriggerReplaceSequence.isSelected = current().triggerReplaceSequence

        //root
        for (x in rootPane.typeList.items)
            x.checkBox.isSelected = current().rootRule.allowedTypes.contains(x.theType)
        rootPane.startsWithCheck.isSelected = current().rootRule.startsWith.first
        rootPane.containsCheck.isSelected = current().rootRule.contains.first
        rootPane.endsWithCheck.isSelected = current().rootRule.endsWith.first

        rootPane.startsWithField.isDisable = !current().rootRule.startsWith.first
        rootPane.containsField.isDisable = !current().rootRule.contains.first
        rootPane.endsWithField.isDisable = !current().rootRule.endsWith.first

        rootPane.startsWithField.text = current().rootRule.startsWith.second
        rootPane.containsField.text = current().rootRule.contains.second
        rootPane.endsWithField.text = current().rootRule.endsWith.second


        //parent
        for (x in parentPane.typeList.items)
            x.checkBox.isSelected = current().parentRule.allowedTypes.contains(x.theType)
        parentPane.startsWithCheck.isSelected = current().parentRule.startsWith.first
        parentPane.containsCheck.isSelected = current().parentRule.contains.first
        parentPane.endsWithCheck.isSelected = current().parentRule.endsWith.first

        parentPane.startsWithField.isDisable = !current().parentRule.startsWith.first
        parentPane.containsField.isDisable = !current().parentRule.contains.first
        parentPane.endsWithField.isDisable = !current().parentRule.endsWith.first

        parentPane.startsWithField.text = current().parentRule.startsWith.second
        parentPane.containsField.text = current().parentRule.contains.second
        parentPane.endsWithField.text = current().parentRule.endsWith.second

    }
    fun init() {

        localSnippets.clear()
        localSnippets += coreManager.snippetManager.snippets

        ctrl.snippetListView.items.addAll(localSnippets)

        ctrl.snippetListView.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
            if(newValue != null)
                setupItem()
            else
                resetValues()
        }
        ctrl.snippetDeleteBtn.setOnAction {
            if(current() != null) {
                val item = current()
                ctrl.snippetListView.items.remove(item)
                localSnippets.remove(item)
            }
        }
        ctrl.snippetLabelField.textProperty().addListener { _, _, newValue ->
            runAsserted {
                it.label = newValue
            }
        }
        ctrl.snippetTriggerReplaceSequence.selectedProperty().addListener { _, _, newValue ->
            runAsserted {
                it.triggerReplaceSequence = newValue
            }
        }
        ctrl.snippetContentArea.textProperty().addListener { _, _, newValue ->
            runAsserted {
                it.insertText = newValue
            }
        }
        ctrl.snippetNewBtn.setOnAction {
            val name = ctrl.snippetNewField.text
            if(name.isEmpty() ||name.isBlank()) return@setOnAction
            for (x in localSnippets)
                if(x.name == name) return@setOnAction
            val item = getSnippet(name)
            localSnippets.add(item)
            ctrl.snippetListView.items.add(item)
            ctrl.snippetListView.selectionModel.select(item)
        }
    }

    private fun runAsserted(block: (Snippet) -> Unit) {
        if (current() != null)
            block(current())
    }

    private fun current() = ctrl.snippetListView.selectionModel.selectedItem
}