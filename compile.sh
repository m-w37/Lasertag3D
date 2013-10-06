#!/bin/sh

echo "cleaning..."
rm -f *.class *~
rm -rf com
echo "done"

echo "compiling..."

#main code
javac -bootclasspath compatability_1_5/java-rt-jar-stubs-1.5.0.jar -source 1.5 -target 1.5 -d . MWGFileFormatException.java ServerTimeoutException.java Lasertag3DSettings.java GameSceneLoader.java GameSceneBuilder.java NetworkListener.java SoundHandler.java PlayerSprite.java SpriteManager.java BasicSprite.java BasicSpriteManager.java NetworkClient.java NetworkServer.java PacketSorter.java NetworkClient2.java NetworkServer2.java LocalServer.java LocalClient.java GameInfo.java GameStarterNetworker.java MuteButton.java MenuPanel.java SpriteBehavior.java LogPrintStream.java SafeHyperlinkListener.java GameStarterPanel.java SettingsPanel.java Lasertag3D.java

echo "done"
exit 0
