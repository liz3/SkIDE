package com.skide.gui.settings

import com.skide.CoreManager
import com.skide.core.code.ChangeWatcher
import com.skide.gui.GUIManager
import com.skide.gui.controllers.GeneralSettingsGUIController
import com.skide.include.ColorRule
import com.skide.include.ColorScheme
import javafx.application.Platform
import javafx.concurrent.Worker
import javafx.scene.Scene
import javafx.scene.control.ColorPicker
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Pane
import javafx.scene.layout.Priority
import javafx.scene.paint.Color
import javafx.stage.Modality
import javafx.stage.Stage
import javafx.stage.StageStyle
import netscape.javascript.JSObject
import java.awt.MouseInfo


fun colorPicker(start: String, settingsStage: Stage, callback: (String) -> Unit) {

    val stage = Stage()
    stage.title = "Color"
    stage.initModality(Modality.WINDOW_MODAL)
    stage.initStyle(StageStyle.UTILITY)
    stage.initOwner(settingsStage)

    val p = MouseInfo.getPointerInfo().location
    stage.x = p.x.toDouble()
    stage.y = p.y.toDouble()

    val pane = AnchorPane()
    if (GUIManager.settings.get("global_font_size").toString().isNotEmpty())
        pane.style = "-fx-font-size: ${GUIManager.settings.get("global_font_size")}px"
    val picker = if (start.isNotEmpty())
        ColorPicker(Color.web(start))
    else
        ColorPicker()

    pane.children.add(picker)
    stage.scene = Scene(pane, 155.0, 50.0)
    picker.setOnAction {
        callback(Integer.toHexString(picker.value.hashCode()))
    }
    if (GUIManager.settings.get("theme") == "Dark")
        stage.scene.stylesheets.add(GUIManager.settings.getCssPath("ThemeDark.css"))

    stage.show()
}

class ColorListEntry(val name: String, val action: (String) -> Unit) : HBox() {

    private val labelLeft = Label()
    val field = TextField()

    init {
        field.textProperty().addListener { _, _, newValue ->
            action(newValue)
        }
        field.setOnMouseClicked {
            if (it.clickCount == 2)
                colorPicker(field.text, field.scene.window as Stage) { color ->
                    field.text = "#$color"
                    action(field.text)
                }
        }
        val pane = Pane()
        labelLeft.text = name
        HBox.setHgrow(pane, Priority.ALWAYS)
        field.prefWidth = 70.0
        field.promptText = "Color"
        this.children.addAll(labelLeft, pane, field)

    }

}

class RuleListEntry(val name: String, val action: (String, String) -> Unit) : HBox() {

    private val labelLeft = Label()
    val foreGround = TextField()
    val style = TextField()

    init {
        foreGround.textProperty().addListener { _, _, newValue ->
            action(newValue, style.text)
        }
        style.textProperty().addListener { _, _, newValue ->
            action(foreGround.text, newValue)
        }
        foreGround.setOnMouseClicked {
            if (it.clickCount == 2)
                colorPicker(foreGround.text, foreGround.scene.window as Stage) { color ->
                    foreGround.text = "#$color"
                    action(foreGround.text, style.text)
                }
        }
        val pane = Pane()
        labelLeft.text = name
        HBox.setHgrow(pane, Priority.ALWAYS)
        this.spacing = 5.0
        foreGround.prefWidth = 70.0
        style.prefWidth = 70.0
        foreGround.promptText = "Color"
        style.promptText = "Style"
        this.children.addAll(labelLeft, pane, foreGround, style)

    }

}

class DemoReady(val cb: () -> Unit) {

    fun ready() {
        cb()
    }
}

class ThemesGuiHandler(val ctrl: GeneralSettingsGUIController, val coreManager: CoreManager) {

    private val localSchemes = HashMap<String, ColorScheme>()
    var loaded = false
    private val vs = ColorScheme("vs", "", HashMap(), HashMap())
    private val vsDark = ColorScheme("vs-dark", "", HashMap(), HashMap())
    val engine = ctrl.schemesPreviewView.engine!!
    private val updateWatcher = ChangeWatcher(650) {
        Platform.runLater {
            reloadPreview()
        }
    }

    private fun reloadPreview() {
        engine.load(this.javaClass.getResource("/www/preview.html").toString())
    }

    private fun preparePreview() {
        engine.loadWorker.stateProperty().addListener { _, _, newValue ->
            runAsserted { scheme ->
                if (newValue == Worker.State.SUCCEEDED) {
                    val window = engine.executeScript("window") as JSObject
                    window.setMember("demo", DemoReady {
                        val theme = getTheme(scheme)

                        window.call("start", scheme.base, scheme.name, theme.first, theme.second, "on world saving:\n" +
                                "\tset {variable} to true\n" +
                                "\t\n" +
                                "command /testCommand testtest:\n" +
                                "\tusage: TestCommand\n" +
                                "\ttrigger:\n" +
                                "\t\tif {variable} is true:\n" +
                                "\t\t\tset {_testtest} to \"Test String\"\n" +
                                "\t\t\tset {_number} to 25\n" +
                                "\t\tif {_testtest} is \"Cool Test String\":\n" +
                                "\t\t\tif player has permission \"skide.owned\":\n" +
                                "\t\t\t\tsend \"Message\" to player\t\t\n" +
                                "function test():\n" +
                                "\t# this is a comment")
                    })
                    Thread {

                        Platform.runLater {
                            engine.executeScript("boot();")
                        }
                    }.start()
                }
            }
        }

    }

    private fun getTheme(scheme: ColorScheme): Pair<JSObject, JSObject> {
        val colors = engine.executeScript("getObj();") as JSObject
        val rules = engine.executeScript("getArr()") as JSObject
        for (c in scheme.colors) {
            colors.setMember(c.key, c.value)
        }
        var count = 0
        for (rule in scheme.rules) {
            val obj = engine.executeScript("getObj()") as JSObject
            obj.setMember("token", rule.key)
            if (rule.value.foreground.isNotEmpty())
                obj.setMember("foreground", rule.value.foreground)
            if (rule.value.style.isNotEmpty())
                obj.setMember("fontStyle", rule.value.style)
            rules.setSlot(count, obj)
            count++
        }
        return Pair(colors, rules)
    }

    fun setup() {
        if (loaded) return
        loaded = true

        preparePreview()
        localSchemes += coreManager.schemesManager.schemes
        localSchemes["vs"] = vs
        localSchemes["vs-dark"] = vsDark

        ctrl.schemesSelectComboBox.items.addAll(localSchemes.values)



        for (c in getColorSettings()) {
            ctrl.schemesColorsList.items.add(ColorListEntry(c.first) { str ->
                runAsserted {
                    if (str.isNotEmpty()) {
                        it.colors[c.first] = str
                    } else {
                        it.colors.remove(c.first)
                    }
                    updateWatcher.update()
                }
            })
        }
        for (c in getRulesSettings()) {
            ctrl.schemesRulesList.items.add(RuleListEntry(c) { color, style ->
                runAsserted {
                    if (color.isNotEmpty() || style.isNotEmpty()) {
                        it.rules[c] = ColorRule(color, style)
                    } else {
                        if (it.rules.containsKey(c)) {
                            it.rules.remove(c)
                        }
                    }
                    updateWatcher.update()
                }
            })
        }
        ctrl.schemesDeleteBtn.setOnAction { ev ->
            runAsserted {
                localSchemes.remove(it.name)
                ctrl.schemesSelectComboBox.items.remove(it)
                ctrl.schemesSelectComboBox.selectionModel.select(vsDark)
                reloadPreview()
            }
        }
        ctrl.schemesNewBtn.setOnAction {
            val name = ctrl.schemesNewTextField.text
            if (localSchemes.containsKey(name)) return@setOnAction
            val parent = current().name
            val scheme = ColorScheme(name, parent, HashMap(), HashMap())
            localSchemes[name] = scheme
            ctrl.schemesSelectComboBox.items.add(scheme)
            ctrl.schemesSelectComboBox.selectionModel.select(scheme)
        }
        ctrl.schemesSelectComboBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->

            if ((newValue == vs || newValue == vsDark)) {
                ctrl.schemesNewBtn.isDisable = false
                ctrl.schemesDeleteBtn.isDisable = true
                ctrl.schemesColorsList.isDisable = true
                ctrl.schemesRulesList.isDisable = true
                reloadPreview()
            } else {
                ctrl.schemesNewBtn.isDisable = true
                ctrl.schemesColorsList.isDisable = false
                ctrl.schemesRulesList.isDisable = false
                ctrl.schemesDeleteBtn.isDisable = false

                runAsserted { scheme ->

                    for (item in ctrl.schemesColorsList.items) {
                        if (scheme.colors.containsKey(item.name)) {
                            item.field.text = scheme.colors[item.name].toString()
                        }
                    }
                    for (item in ctrl.schemesRulesList.items) {
                        if (scheme.rules.containsKey(item.name)) {
                            val e = scheme.rules[item.name]!!
                            item.foreGround.text = e.foreground
                            item.style.text = e.style
                        }
                    }
                }
            }
        }
        val current = coreManager.configManager.get("color_scheme") as String
        if (current == "" || !localSchemes.containsKey(current)) {
            val theme = coreManager.configManager.get("theme")
            if (theme == "Dark") ctrl.schemesSelectComboBox.selectionModel.select(vsDark)
            if (theme == "Light") ctrl.schemesSelectComboBox.selectionModel.select(vs)
        } else {
            ctrl.schemesSelectComboBox.selectionModel.select(localSchemes[current])
        }
        if (ctrl.schemesSelectComboBox.selectionModel.selectedItem == vs || ctrl.schemesSelectComboBox.selectionModel.selectedItem == vsDark) {
            ctrl.schemesNewBtn.isDisable = false
            ctrl.schemesDeleteBtn.isDisable = true
            ctrl.schemesColorsList.isDisable = true
            ctrl.schemesRulesList.isDisable = true
        }

        updateWatcher.start()
    }

    fun applySettings() {

        localSchemes.remove("vs")
        localSchemes.remove("vs-dark")
        coreManager.schemesManager.schemes.clear()
        coreManager.schemesManager.schemes += localSchemes
        coreManager.schemesManager.writeSchemes()

    }

    private fun runAsserted(block: (ColorScheme) -> Unit) {
        val c = current() ?: return
        block(c)
    }

    private fun current() = ctrl.schemesSelectComboBox.selectionModel.selectedItem
    private fun getRulesSettings() = arrayOf("variable", "string", "comment", "keyword", "identifier", "number", "function")
    private fun getColorSettings(): Array<Pair<String, String>> {
        return arrayOf(Pair("foreground", "Overall foreground color. This color is only used if not overridden by a component."),
                Pair("errorForeground", "Overall foreground color for error messages. This color is only used if not overridden by a component."),
                Pair("descriptionForeground", "Foreground color for description text providing additional information, for example for a label."),
                Pair("focusBorder", "Overall border color for focused elements. This color is only used if not overridden by a component."),
                Pair("contrastBorder", "An extra border around elements to separate them from others for greater contrast."),
                Pair("contrastActiveBorder", "An extra border around active elements to separate them from others for greater contrast."),
                Pair("selection.background", "The background color of text selections in the workbench (e.g. for input fields or text areas). Note that this does not apply to selections within the editor."),
                Pair("textSeparator.foreground", "Color for text separators."),
                Pair("textLink.foreground", "Foreground color for links in text."),
                Pair("textLink.activeForeground", "Foreground color for active links in text."),
                Pair("textPreformat.foreground", "Foreground color for preformatted text segments."),
                Pair("textBlockQuote.background", "Background color for block quotes in text."),
                Pair("textBlockQuote.border", "Border color for block quotes in text."),
                Pair("textCodeBlock.background", "Background color for code blocks in text."),
                Pair("widget.shadow", "Shadow color of widgets such as find/replace inside the editor."),
                Pair("input.background", "Input box background."),
                Pair("input.foreground", "Input box foreground."),
                Pair("input.border", "Input box border."),
                Pair("inputOption.activeBorder", "Border color of activated options in input fields."),
                Pair("input.placeholderForeground", "Input box foreground color for placeholder text."),
                Pair("inputValidation.infoBackground", "Input validation background color for information severity."),
                Pair("inputValidation.infoBorder", "Input validation border color for information severity."),
                Pair("inputValidation.warningBackground", "Input validation background color for information warning."),
                Pair("inputValidation.warningBorder", "Input validation border color for warning severity."),
                Pair("inputValidation.errorBackground", "Input validation background color for error severity."),
                Pair("inputValidation.errorBorder", "Input validation border color for error severity."),
                Pair("dropdown.background", "Dropdown background."),
                Pair("dropdown.foreground", "Dropdown foreground."),
                Pair("dropdown.border", "Dropdown border."),
                Pair("list.focusBackground", "List/Tree background color for the focused item when the list/tree is active. An active list/tree has keyboard focus, an inactive does not."),
                Pair("list.focusForeground", "List/Tree foreground color for the focused item when the list/tree is active. An active list/tree has keyboard focus, an inactive does not."),
                Pair("list.activeSelectionBackground", "List/Tree background color for the selected item when the list/tree is active. An active list/tree has keyboard focus, an inactive does not."),
                Pair("list.activeSelectionForeground", "List/Tree foreground color for the selected item when the list/tree is active. An active list/tree has keyboard focus, an inactive does not."),
                Pair("list.inactiveSelectionBackground", "List/Tree background color for the selected item when the list/tree is inactive. An active list/tree has keyboard focus, an inactive does not."),
                Pair("list.inactiveSelectionForeground", "List/Tree foreground color for the selected item when the list/tree is inactive. An active list/tree has keyboard focus, an inactive does not."),
                Pair("list.hoverBackground", "List/Tree background when hovering over items using the mouse."),
                Pair("list.hoverForeground", "List/Tree foreground when hovering over items using the mouse."),
                Pair("list.dropBackground", "List/Tree drag and drop background when moving items around using the mouse."),
                Pair("list.highlightForeground", "List/Tree foreground color of the match highlights when searching inside the list/tree."),
                Pair("pickerGroup.foreground", "Quick picker color for grouping labels."),
                Pair("pickerGroup.border", "Quick picker color for grouping borders."),
                Pair("button.foreground", "Button foreground color."),
                Pair("button.background", "Button background color."),
                Pair("button.hoverBackground", "Button background color when hovering."),
                Pair("badge.background", "Badge background color. Badges are small information labels, e.g. for search results count."),
                Pair("badge.foreground", "Badge foreground color. Badges are small information labels, e.g. for search results count."),
                Pair("scrollbar.shadow", "Scrollbar shadow to indicate that the view is scrolled."),
                Pair("scrollbarSlider.background", "Slider background color."),
                Pair("scrollbarSlider.hoverBackground", "Slider background color when hovering."),
                Pair("scrollbarSlider.activeBackground", "Slider background color when active."),
                Pair("progressBar.background", "Background color of the progress bar that can show for long running operations."),
                Pair("editor.background", "Editor background color."),
                Pair("editor.foreground", "Editor default foreground color."),
                Pair("editorWidget.background", "Background color of editor widgets, such as find/replace."),
                Pair("editorWidget.border", "Border color of editor widgets. The color is only used if the widget chooses to have a border and if the color is not overridden by a widget."),
                Pair("editor.selectionBackground", "Color of the editor selection."),
                Pair("editor.selectionForeground", "Color of the selected text for high contrast."),
                Pair("editor.inactiveSelectionBackground", "Color of the selection in an inactive editor."),
                Pair("editor.selectionHighlightBackground", "Color for regions with the same content as the selection."),
                Pair("editor.findMatchBackground", "Color of the current search match."),
                Pair("editor.findMatchHighlightBackground", "Color of the other search matches."),
                Pair("editor.findRangeHighlightBackground", "Color the range limiting the search."),
                Pair("editor.hoverHighlightBackground", "Highlight below the word for which a hover is shown."),
                Pair("editorHoverWidget.background", "Background color of the editor hover."),
                Pair("editorHoverWidget.border", "Border color of the editor hover."),
                Pair("editorLink.activeForeground", "Color of active links."),
                Pair("diffEditor.insertedTextBackground", "Background color for text that got inserted."),
                Pair("diffEditor.removedTextBackground", "Background color for text that got removed."),
                Pair("diffEditor.insertedTextBorder", "Outline color for the text that got inserted."),
                Pair("diffEditor.removedTextBorder", "Outline color for text that got removed."),
                Pair("merge.currentHeaderBackground", "Current header background in inline merge-conflicts."),
                Pair("merge.currentContentBackground", "Current content background in inline merge-conflicts."),
                Pair("merge.incomingHeaderBackground", "Incoming header background in inline merge-conflicts."),
                Pair("merge.incomingContentBackground", "Incoming content background in inline merge-conflicts."),
                Pair("merge.commonHeaderBackground", "Common ancestor header background in inline merge-conflicts."),
                Pair("merge.commonContentBackground", "Common ancester content background in inline merge-conflicts."),
                Pair("merge.border", "Border color on headers and the splitter in inline merge-conflicts."),
                Pair("editorOverviewRuler.currentContentForeground", "Current overview ruler foreground for inline merge-conflicts."),
                Pair("editorOverviewRuler.incomingContentForeground", "Incoming overview ruler foreground for inline merge-conflicts."),
                Pair("editorOverviewRuler.commonContentForeground", "Common ancestor overview ruler foreground for inline merge-conflicts."),
                Pair("editor.lineHighlightBackground", "Background color for the highlight of line at the cursor position."),
                Pair("editor.lineHighlightBorder", "Background color for the border around the line at the cursor position."),
                Pair("editor.rangeHighlightBackground", "Background color of highlighted ranges, like by quick open and find features."),
                Pair("editorCursor.foreground", "Color of the editor cursor."),
                Pair("editorWhitespace.foreground", "Color of whitespace characters in the editor."),
                Pair("editorIndentGuide.background", "Color of the editor indentation guides."),
                Pair("editorLineNumber.foreground", "Color of editor line numbers."),
                Pair("editorRuler.foreground", "Color of the editor rulers."),
                Pair("editorCodeLens.foreground", "Foreground color of editor code lenses"),
                Pair("editorBracketMatch.background", "Background color behind matching brackets"),
                Pair("editorBracketMatch.border", "Color for matching brackets boxes"),
                Pair("editorOverviewRuler.border", "Color of the overview ruler border."),
                Pair("editorGutter.background", "Background color of the editor gutter. The gutter contains the glyph margins and the line numbers."),
                Pair("editorError.foreground", "Foreground color of error squigglies in the editor."),
                Pair("editorError.border", "Border color of error squigglies in the editor."),
                Pair("editorWarning.foreground", "Foreground color of warning squigglies in the editor."),
                Pair("editorWarning.border", "Border color of warning squigglies in the editor."),
                Pair("editorMarkerNavigationError.background", "Editor marker navigation widget error color."),
                Pair("editorMarkerNavigationWarning.background", "Editor marker navigation widget warning color."),
                Pair("editorMarkerNavigation.background", "Editor marker navigation widget background."),
                Pair("editorSuggestWidget.background", "Background color of the suggest widget."),
                Pair("editorSuggestWidget.border", "Border color of the suggest widget."),
                Pair("editorSuggestWidget.foreground", "Foreground color of the suggest widget."),
                Pair("editorSuggestWidget.selectedBackground", "Background color of the selected entry in the suggest widget."),
                Pair("editorSuggestWidget.highlightForeground", "Color of the match highlights in the suggest widget."),
                Pair("editor.wordHighlightBackground", "Background color of a symbol during read-access, like reading a variable."),
                Pair("editor.wordHighlightStrongBackground", "Background color of a symbol during write-access, like writing to a variable."),
                Pair("peekViewTitle.background", "Background color of the peek view title area."),
                Pair("peekViewTitleLabel.foreground", "Color of the peek view title."),
                Pair("peekViewTitleDescription.foreground", "Color of the peek view title info."),
                Pair("peekView.border", "Color of the peek view borders and arrow."),
                Pair("peekViewResult.background", "Background color of the peek view result list."),
                Pair("peekViewResult.lineForeground", "Foreground color for line nodes in the peek view result list."),
                Pair("peekViewResult.fileForeground", "Foreground color for file nodes in the peek view result list."),
                Pair("peekViewResult.selectionBackground", "Background color of the selected entry in the peek view result list."),
                Pair("peekViewResult.selectionForeground", "Foreground color of the selected entry in the peek view result list."),
                Pair("peekViewEditor.background", "Background color of the peek view editor."),
                Pair("peekViewEditorGutter.background", "Background color of the gutter in the peek view editor."),
                Pair("peekViewResult.matchHighlightBackground", "Match highlight color in the peek view result list."),
                Pair("peekViewEditor.matchHighlightBackground", "Match highlight color in the peek view editor."))

    }
}