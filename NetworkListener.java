//NetworkListener.java (6/28/2010)
//represents classes listening for network-related LT3D events
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net;


import javax.media.j3d.*;
import javax.vecmath.*;
import java.net.*;


public interface NetworkListener {

    /** These methods are all called by networking classes
    * to communicate between the networking side and the display side of
    * Lasertag3D.  They should return as quickly as possible so as not
    * to slow down the networking speeds.
    **/

    /** Called by GameStarterPanel when the player has selected a game to
    * join and that game has started.  server is the game's server's network
    * address, multicastAddr is the ip of the multicast group used
    * (multicast sockets should bind to Lasertag3D.MULTICAST_ADDR),
    * playerIdInGame is this player's id for the game (the one that should
    * be passed to the networking classes), thisInetAddr is this player's
    * ip address as the server sees it,
    * playerNames is an array of the players' names indexed by id,
    * isServer tells whether this player is running the server, playerAddresses
    * is an array of the players' ips indexed by id (null if isServer==false),
    * and roomFileName is the (processed) name of the room the game is in,
    * taken from the list of room names given to GameStarterPanel.
    **/
    public void startGame( InetSocketAddress server,
            byte playerIdInGame, InetAddress thisInetAddr,
            String playerNames[], boolean isServer,
            int playerGamePasswords[], int thisGamePassword,
            String roomFileName );

    /** Called by NetworkClient when it has sufficient information to
    * describe a new scene.  playerPositions contains the new  positions
    * of the players, indexed by id; and dotPositions describes the positions
    * of any dots cast by lasers, in no particular order.
    * If this is the server, this method will not be called.
    **/
    public void updateScene( Vector3f playerPositions[], Matrix3f playerRotations[],
            Vector3f dotPositions[] );

    /** Called by NetworkClient when it receives (possibly old) hit information.
    * playersHit contains the ids
    * of the players hit this frame, in no particular order (except in
    * relation to shooters[]), and shooters contains the ids of the players
    * who hit someone else this frame, with matching indexes to playersHit
    * (e.g., the player with id shooters[ 3 ] is the one who hit the player
    * with id playersHit[ 3 ]).
    * If this is the server, this method will not be called.
    **/
    public void updateHits( byte playersHit[], byte shooters[] );

    //indicates a score update.  The scores are indexed by player id.
    public void updateScores( int scores[] );

    //indicates that this player was hit by shooterId
    public void thisWasHit( int shooterId );

    //indicates that this player shot playerHitId
    public void thisDidHit( int playerHitId );

    /** Indicates that player playerId left the game. reason is one of the
    * constants defined in NetworkClient telling why the player left.
    **/
    public void playerLeft( int playerId, int reason );

    /** Indicates an exception was thrown while receiving, sending, or
    * processing messages.  Exceptions will not end NetworkClient's loops;
    * however, they should be dealt with and the application closed if
    * a real error has occurred.  Generally, SocketTimeoutExceptions should
    * be ignored while other IOExceptions should cause network closure.
    **/
    public void exceptionThrown( Exception exc );

    /** Tells that the game has ended with the scores given, indexed by id.
    * The listener should not attempt to close the client or the server,
    * as they will close themselves.
    **/
    public void gameOver( int scores[] );

    /** Indicates that it has been NetworkClient.SERVER_TIMEOUT ns since
    * a message has been received from the server and a takeover may be
    * necessary.
    **/
    public void serverTimedOut();

    /** Asks whether or not this java instance also hosts the current game's
    * server.
    **/
    public boolean isServer();

    /** Allows the server or client to communicate with the user.  These messages
    * should be displayed to the user in a friendly way and in real time.
    * A message of <code>null</code> indicates the previous message should be cleared
    * and any corresponding displays (e.g. dialog boxes) should be cleared with it.
    **/
    public void messageForUser( String message );

    /** Called whenever Lasertag3DSettings.password has been updated.
    **/
    public void resetPassword();
}
