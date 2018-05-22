package com.skide.utils

import com.skide.core.management.OpenProject
import com.skide.gui.PasswordDialog
import com.skide.gui.Prompts
import com.skide.include.CompileOption
import com.skide.include.RemoteHost
import com.skide.utils.skcompiler.SkCompiler
import javafx.application.Platform
import java.io.IOException
import java.io.FileInputStream
import org.apache.commons.net.ftp.FTP
import sun.security.jgss.GSSUtil.login
import org.apache.commons.net.ftp.FTPReply
import org.apache.commons.net.imap.IMAPReply.getReplyCode
import java.io.PrintWriter
import org.apache.commons.net.PrintCommandListener
import org.apache.commons.net.ftp.FTPClient
import java.io.ByteArrayInputStream
import java.io.FileNotFoundException
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.JSchException
import java.io.File
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import com.jcraft.jsch.JSch
import com.skide.include.RemoteHostType


class RemoteDeployer(val project: OpenProject) {


    fun deploy(content: String, fileName: String, host: RemoteHost) {

        if(host.type == RemoteHostType.SFTP)
            if(host.isPrivateKey) sftpDeployWithKey(host, fileName, content) else sftpDeploy(host, fileName, content)
        else
            ftpDeploy(host, fileName, content)

    }

    fun depploy(compileOption: CompileOption, host: RemoteHost) {

        project.compiler.compile(project.project, compileOption, project.guiHandler.lowerTabPaneEventManager.setupBuildLogTabForInput(), { result ->


        })

    }

    private fun ftpDeploy(host: RemoteHost, fileName: String, content: String) {

        val c = FTPClient()
        c.addProtocolCommandListener(PrintCommandListener(PrintWriter(System.out)))
        var exitCode = 0
        try {
            c.connect(host.host, host.port)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        exitCode = c.replyCode
        if (!FTPReply.isPositiveCompletion(exitCode)) {
            c.disconnect()
        }
        Platform.runLater {
            c.login(host.username, if (host.passwordSaved) host.password else Prompts.passPrompt())
            c.setFileType(FTP.BINARY_FILE_TYPE)
            c.storeFile(host.folderPath + fileName, ByteArrayInputStream(content.toByteArray()))

        }
    }

    private fun sftpDeploy(host: RemoteHost, fileName: String, content: String) {

        val jsch = JSch()
        val session = jsch.getSession(host.username, host.host, host.port)
        val config = java.util.Properties()
        config["StrictHostKeyChecking"] = "no"
        session.setConfig(config)
        Platform.runLater {
            session.setPassword(if (host.passwordSaved) host.password else Prompts.passPrompt())
            session.connect()
            val channel = session.openChannel("sftp")
            channel.connect()
            val c = channel as ChannelSftp
            c.cd(host.folderPath)
            c.put(ByteArrayInputStream(content.toByteArray()), fileName)
            c.exit()
            session.disconnect()
        }
    }
    private fun sftpDeployWithKey(host: RemoteHost, fileName: String, content: String) {

        val jsch = JSch()
        Platform.runLater {
        if(host.passwordSaved) jsch.addIdentity(host.privateKeyPath, host.password) else jsch.addIdentity(host.privateKeyPath,Prompts.passPrompt())
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
        }
    }
}