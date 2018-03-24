Sk-IDE
------
[![Join the Discord](https://discordapp.com/api/guilds/324602899839844352/widget.png?style=shield)](https://discord.io/sk-ide)

Sk-IDE is an integrated development environment (IDE) for the Skript language. Skript is a plugin for Minecraft multiplayer servers that allows easy scripting of server modifications. 
This IDE provides a way to easily write Skripts, by providing auto-completion, syntax-highlighting and many more things.

# Developer Information
Sk-IDE is written in Kotlin for the most part. Although you can still contribute with Java code, it is preferred to use Kotlin.

## Dependencies
 - Java Development Kit version 8
 - Gradle version 3.0.0 (or newer)

## Building
```sh
$ gradle build
```
This will output the jar file to `build/libs`

# Libraries, Frameworks and APIs in use
 - [Kotlin Language](http://kotlinlang.org/) - Programming language in use.
 - [JavaFX](http://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html) - GUI framework in use
 - [RichTextFX](https://github.com/FXMisc/RichTextFX) - JavaFX advanced text-view library
 - [TerminalFX](https://github.com/javaterminal/TerminalFX) - JavaFX terminal-view library
 - [skUnity](http://skunity.com/) - Integration with skunity.com
 - [SkriptTools](https://skripttools.net/) - Advanced Skript tools
 - [Gradle](https://gradle.org/) - Build system
 - [JavaFX Gradle Plugin](https://github.com/FibreFoX/javafx-gradle-plugin) - JavaFX plugin for build system
 - [JSON](https://www.json.org/json-en.html) - JSON implementation for Java
 - [Discord RPC](https://github.com/PSNRigner/discord-rpc-java) - Discord integration for Java

# License
This project is licensed under the GNU General Public License v2.0. You can find more information about it in the [LICENSE](LICENSE) file.
