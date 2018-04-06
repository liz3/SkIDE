package com.skide.utils

import com.skide.CoreManager
import com.skide.gui.Prompts
import javafx.scene.control.Alert
import org.json.JSONObject

class SkUnity(val coreManager: CoreManager){
    var loggedIn = false

    private set
    var key = coreManager.configManager.skUnityKey

    var username = ""

    fun load(){
        if (coreManager.configManager.skUnityKey != ""){
            loggedIn = true

            key = coreManager.configManager.skUnityKey

            username = coreManager.configManager.skUnityUsername
        }
    }

    fun login(): Boolean{
        if (loggedIn){
            return false
        }

        val name = Prompts.textPrompt("Username", "Please enter your SkUnity Username")

        val password = Prompts.passPrompt()

        if (name.isEmpty() || password.isEmpty()){
            return false
        }

        val headers = HashMap<String, String>()

        headers["Content-type"] = "application/x-www-form-urlencoded"

        val map = HashMap<String, String>()

        map["action"] = "token"

        map["username"] = name

        map["password"] = password

        val result = request("https://liz3.net/sk/xf/", "POST", headers, getURLEncoded(map))

        val buff = ByteArray(result.third.available())

        result.third.read(buff)

        val content = JSONObject(String(buff))

        val data = content.getJSONObject("data")

        if (data.has("error")){
            Prompts.infoCheck("Error", "Error while logging in!", data.getString("message"), Alert.AlertType.ERROR)

            return false
        }

        coreManager.configManager.skUnityKey = data.getString("hash")

        key = data.getString("hash")

        loggedIn = true

        Prompts.infoCheck("Success", "You are logged into SkUnity!", "You may now use SkUnity Features within Sk-IDE", Alert.AlertType.INFORMATION)

        username = name

        coreManager.configManager.skUnityUsername = username

        return true
    }

    fun report(title: String, msg: String){
        val headers = HashMap<String, String>()

        headers["Content-type"] = "application/x-www-form-urlencoded"

        val map = HashMap<String, String>()

        map["action"] = "post"

        map["title"] = title

        map["msg"] = "$msg\n\nReported with Sk-IDE"

        map["token"] = key

        map["username"] = username

        val result = request("https://liz3.net/sk/xf/", "POST", headers, getURLEncoded(map))

        val buff = ByteArray(result.third.available())

        result.third.read(buff)

        val content = JSONObject(String(buff))

        val data = content.getJSONObject("data")

        if (data.has("error")){
            Prompts.infoCheck("Error", "Error while reporting the error!", data.getString("message"), Alert.AlertType.ERROR)

            return
        }else{
            Prompts.infoCheck("Success", "Post was created!", "The post was created successfully", Alert.AlertType.INFORMATION)
        }
    }
}