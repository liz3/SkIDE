Sk-IDE
------
Sk-IDE is an integrated development environment (IDE) for the Skript language. Skript is a plugin for Minecraft multiplayer servers that allows easy scripting of server modifications. 
This IDE provides a way to easily write Skripts, by providing auto-completion, syntax-highlighting and many more things.

# Developer Information
Sk-IDE is written in Kotlin for the most part. Although you can still contribute with Java code, it is preferred to use Kotlin.

## Dependencies
 - Java Development Kit version 11

## Building
```sh
$ gradlew shadowJar
```
This will output the jar file to `build/libs`

# Libraries, Frameworks and APIs in use
 - [Kotlin Language](http://kotlinlang.org/) - Programming language in use.
 - [JavaFX](http://www.oracle.com/technetwork/java/javase/overview/javafx-overview-2158620.html) - GUI framework in use
 - [ControlsFX](http://fxexperience.com/controlsfx/) - JavaFX addon for advanced components 
 - [Monaco](https://microsoft.github.io/monaco-editor/) - browser based code editor
 - [TerminalFX](https://github.com/javaterminal/TerminalFX) - JavaFX terminal-view library
 - [Skript Hub](http://skripthub.net/) - Provider for auto-complete data
 - [skUnity](http://skunity.com/) - Integration with forums.skunity.com
 - [SkriptTools](https://skripttools.net/) - Information to Skript binary meta data
 - [Gradle](https://gradle.org/) - Build system
 - [JSON](https://www.json.org/json-en.html) - JSON implementation for Java
 - [Jsch](http://www.jcraft.com/jsch/) - SSH implementation for Java
 - [Apache Commons Net](https://commons.apache.org/proper/commons-net/) - Used for FTP implementation

# License
This project is licensed under the GNU General Public License v2.0. You can find more information about it in the [LICENSE](LICENSE) file.
