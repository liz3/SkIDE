package com.skide.include

enum class RemoteHostType { SFTP, FTP }
class RemoteHost(val name:String,
                 var type: RemoteHostType,
                 var host:String,
                 var port:Int,
                 var passwordSaved:Boolean,
                 var isPrivateKey:Boolean,
                 var privateKeyPath:String,
                 var password:String,
                 var folderPath:String,
                 var username:String) {
    override fun toString(): String {
        return name
    }
}