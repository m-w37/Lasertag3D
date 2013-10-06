Lasertag3D by Matthew Weidner
Copyright 2010-2013

ABOUT

Lasertag3D is a game of single- and multi-player 3D lasertag.  Some scenes have gravity while others do not.  Movement controls are similar to those of Minecraft; left click to fire.

INSTALLATION SETUP

You will need to download Java3D (version 1.5.2 or higher) and add its files to your classpath and Java library path environment variables.  See <https://java3d.java.net/binary-builds.html>.
You will also need to run (in a terminal in the directory which you extracted the zip file into):
>chmod +x *.sh

USAGE

There are 3 standalone applications contained within this distribution: Lasertag3D, GameSceneBuilder, and MazeTranslator.

Lasertag3D:
This is the core Lasertag3D application.  To run, do (in a terminal in the directory which you extracted the zip file into):
>./compile.sh
>java com.mattweidner.lt3d.Lasertag3D [-nolog | -log <file>]
OR
>./build2.sh
>java -jar Lasertag3D.jar

GameSceneBuilder:
This is a utility which allows one to view .mwg scenes while constructing them.  The interface is not great, but it is better than editing text files by hand. To run, do (in a terminal in the directory which you extracted the zip file into):
>./compile.sh
>javac -d . GameSceneBuilder.java
>java com.mattweidner.lt3d.scene.GameSceneBuilder

MazeTranslator:
This is a utility that turns .maze files from my Maze3D game into .mwg files.  To run, do (in a terminal in the directory which you extracted the zip file into):
>javac -d . Maze.java MazeTranslator.java
>java com.mattweidner.lt3d.scene.MazeTranslator <infile> <outfile> scale

FILES AND DIRECTORIES

compatability_1_5/ - contains resources needed to export code that is backwards-compatible with Java 1.5, downloaded from <http://www.java2s.com/Code/Jar/j/Downloadjavartjarstubs150jar.htm>.

data/ - contains data files common to the whole application, such as images and help files.

rooms/ - contains data files for the scenes/rooms.

sound-data/ - contains sound files.

BasicSprite.java - moves and sets up the sprite used to represent a player when the user is a client in a multiplayer game.

BasicSpriteManager.java - initilizes the sprites and reports the scores when the user is a client in a multiplayer game.

build2.sh - compiles and packages the program, creating Lasertag3D.jar.

compile.sh - compiles the program.

default_settings.txt - the settings file loaded when the user's settings file is invalid or nonexistent.

GameInfo.java - data structure-like class which holds information about a multiplayer game currently being formed.

GameSceneBuilder.java - standalone application used to help make scene files (.mwg files).

GameSceneLoader.java - reads in a scene file (.mwg file), creating the Java3D scene specified.

GameStarterNetworker.java - connects to the mattweidner.com server, reading information about the games being formed and indicating the user's intent.

GameStarterPanel.java - GUI interface that shows games beings formed, allows the user to interact with those games, and manages the GameStarterNetworker.

icon.gif - the mattweidner.com icon.

Lasertag3D.java - entry-point of the application which handles the main GUI and starts/ends games.

Lasertag3DSettings.java - reads, writes, and makes available the application's settings, which are stored in %appdata%/.Lasertag3D/settings.txt.

LocalClient.java - instance of NetworkClient used in single-player mode.

LocalServer.java - instance of NetworkServer used in single-player mode.

LogPrintStream.java - PrintStream used to redirect stdout and stderr into %appdata%/.Lasertag3D/log.txt.

manifest.txt - manifest for Lasertag3D.jar.

make_source.sh - makes Lasertag3D_source.zip.

Maze.java - helper class used by MazeTranslator.java to parse maze files.

MazeTranslator.java - standalone application used to translate maze files for Maze3D into scene files (.mwg files).

MenuPanel.java - shows the main menu.

MuteButton.java - shows the mute button.

MWGFileFormatException.java - exception thrown by GameSceneLoader when a formatting error is encountered in a .mwg file.

NetworkClient.java - abstract superclass for NetworkClients, which manage the client-side networking.

NetworkClient2.java - implementation of NetworkClient used in multiplayer games.

NetworkListener.java - interface implemented by Lasertag3D and used by GameStarterNetworker, NetworkClient implementors, and NetworkServer implementors to communicate between the network managers and the core application.

NetworkServer.java - abstract superclass for NetworkServers, which manage the server-side networking.

NetworkServer2.java - implementation of NetworkServer used in multiplayer games.

package2.sh - packages the compiled game, creating Lasertag3D.jar.

PacketSorter.java - places packets received by NetworkClient2 into proper order, preventing old data from overwriting new data.

PlayerSprite.java - moves and sets up the sprite used to represent a player when the user is a server in a multiplayer game or is in a single-player game.

rooms_index.txt - lists the names of the rooms/scenes.

SafeHyperlinkListener.java - enables the user to open links in their browser in a backwards-compatible way.

server2_no_password.php - server2.php minus sensitive database information, where server2.php is the php file which allows the game to connect to the multiplayer database over the world wide web.

ServerTimeoutException.java - exception thrown by NetworkClient2 when the server in a multiplayer game cannot be reached in a reasonable amount of time.

SettingsPanel.java - manages the GUI for setting settings.

SoundHandler.java - loads and runs the sounds.

SpriteBehavior.java - handles user input.

SpriteManager.java - initilizes the sprites and reports the scores when the user is a server in a multiplayer game or is in a single-player game.
