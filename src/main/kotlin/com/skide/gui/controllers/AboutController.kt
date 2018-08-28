package com.skide.gui.controllers

import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.image.ImageView


class AboutController {


    @FXML
    lateinit var imageView: ImageView 

    @FXML
    lateinit var versionLabel: Label 

    @FXML
    lateinit var infoTextLabel: Label 

    @FXML
    lateinit var discordBtn: Button 

    @FXML
    lateinit var gitlabBtn: Button 

    @FXML
    lateinit var donateBtn: Button

    @FXML
    lateinit var okBtn: Button
}