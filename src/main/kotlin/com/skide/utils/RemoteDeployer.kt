package com.skide.utils

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.skide.core.management.OpenProject
import com.skide.gui.Prompts
import com.skide.include.CompileOption
import com.skide.include.RemoteHost
import com.skide.include.RemoteHostType
import javafx.application.Platform
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.apache.commons.net.ftp.FTPReply
import org.controlsfx.control.Notifications
import java.io.ByteArrayInputStream
import java.io.PrintWriter


class RemoteDeployer(val project: OpenProject) {


    fun deploy(content: String, fileName: String, host: RemoteHost) {
        if (host.type == RemoteHostType.SFTP)
            if (host.isPrivateKey) sftpDeployWithKey(host, fileName, content) else sftpDeploy(host, fileName, content)
        else
            ftpDeploy(host, fileName, content)
    }

    fun depploy(compileOption: CompileOption, host: RemoteHost) {
        project.compiler.compile(project, compileOption, project.guiHandler.lowerTabPaneEventManager.setupBuildLogTabForInput(), { result ->
            if (host.type == RemoteHostType.SFTP)
                if (host.isPrivateKey) sftpDeployWithKey(host, compileOption.name + ".sk", result) else sftpDeploy(host, compileOption.name, result)
            else
                ftpDeploy(host, compileOption.name + ".sk", result)
        })

    }

    private fun ftpDeploy(host: RemoteHost, fileName: String, content: String) {
        val c = FTPClient()
        Platform.runLater {
            try {
                c.addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
                val exitCode: Int = c.replyCode
                c.connect(host.host, host.port)
                if (!FTPReply.isPositiveCompletion(exitCode)) c.disconnect()
                c.login(host.username, if (host.passwordSaved) host.password else Prompts.passPrompt())
                c.setFileType(FTP.BINARY_FILE_TYPE)
                c.storeFile(host.folderPath + fileName, ByteArrayInputStream(content.toByteArray()))
                Notifications.create()
                        .title("File Deployed")
                        .text("File $fileName has been deployed successfully to ${host.host}").darkStyle()
                        .showInformation()
            } catch (e: Exception) {
                Notifications.create()
                        .title("File Deploy error")
                        .text("Error while deploying $fileName ${e.message}").darkStyle()
                        .showError()
            }
        }
    }

    private fun sftpDeploy(host: RemoteHost, fileName: String, content: String) {
        val jsch = JSch()
        Platform.runLater {
            try {
                val session = jsch.getSession(host.username, host.host, host.port)
                val config = java.util.Properties()
                config["StrictHostKeyChecking"] = "no"
                session.setConfig(config)
                session.setPassword(if (host.passwordSaved) host.password else Prompts.passPrompt())
                session.connect()
                val channel = session.openChannel("sftp")
                channel.connect()
                val c = channel as ChannelSftp
                c.cd(host.folderPath)
                c.put(ByteArrayInputStream(content.toByteArray()), fileName)
                c.exit()
                session.disconnect()
                Notifications.create()
                        .title("File Deployed")
                        .text("File $fileName has been deployed successfully to ${host.host}").darkStyle()
                        .showInformation()
            } catch (e: Exception) {
                Notifications.create()
                        .title("File Deploy error")
                        .text("Error while deploying $fileName ${e.message}").darkStyle()
                        .showError()
            }
        }
    }

    private fun sftpDeployWithKey(host: RemoteHost, fileName: String, content: String) {
        val jsch = JSch()
        Platform.runLater {
            try {
                if (host.passwordSaved) jsch.addIdentity(host.privateKeyPath, host.password) else jsch.addIdentity(host.privateKeyPath, Prompts.passPrompt())
                val session = jsch.getSession(host.username, host.host, host.port)
                val config = java.util.Properties()
                config["StrictHostKeyChecking"] = "no"
                session.setConfig(config)
                session.connect()
                val channel = session.openChannel("sftp")
                channel.connect()
                val c = channel as ChannelSftp
                c.cd(host.folderPath)
                c.put(ByteArrayInputStream(content.toByteArray()), fileName)
                c.exit()
                session.disconnect()
                Notifications.create()
                        .title("File Deployed")
                        .text("File $fileName has been deployed successfully to ${host.host}").darkStyle()
                        .showInformation()
            } catch (e: Exception) {
                Notifications.create()
                        .title("File Deploy error")
                        .text("Error while deploying $fileName ${e.message}").darkStyle()
                        .showError()
            }
        }
    }
}