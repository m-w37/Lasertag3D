//NetworkClient.java (6/23/2010)
//manages the client-side networking aspects of Lasertag3D
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net;


import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.vecmath.Vector3f;

import com.mattweidner.lt3d.Lasertag3DSettings;
import com.mattweidner.lt3d.sprite.*;


public abstract class NetworkClient {

    //key codes
    /* When key codes are added/removed, update NUM_KEY_CODES, 
            isZeroGOnlyKeyCode(), and SettingsPanel.
    */
    public final static int FAST = 0;
    public final static int GO_FORWARDS = 1;
    public final static int GO_BACKWARDS = 2;
    public final static int STRAFE_UP = 3;
    public final static int STRAFE_DOWN = 4;
    public final static int STRAFE_LEFT = 5;
    public final static int STRAFE_RIGHT = 6;
    public final static int ROTATE_CCW = 7;//counterclockwise
    public final static int ROTATE_CW = 8;//clockwise
    //number of key codes (remember to adjust this!)
    public final static int NUM_KEY_CODES = 9;

    //reasons for player to leave
    public final static int LEFT_VOLUNTARILY = 0;
    public final static int LOST_CONNECTION = 1;


    protected ByteBuffer inBuffer;//contains messages to give to packets
    protected ByteBuffer outBuffer;//contains messages read in from packets

    protected InetSocketAddress serverAddr;

    protected NetworkListener listener;
    protected String name;//name of this player
    protected byte id;//id of this player
    protected int numPlayers;//number of players in the game
    protected boolean playersInGame[];
    protected boolean isServer;//does this player host the server?
    protected long lastShotFrameId;//last frame when the player shot

    protected PlayerSprite playerSprite;
    protected BasicSprite basicSprite;


    protected NetworkClient( InetSocketAddress serverAddr,
            String name, byte id, int numPlayers, NetworkListener listener,
            boolean isServer, PlayerSprite playerSprite,
            BasicSprite basicSprite )
    {
        outBuffer = ByteBuffer.allocate( getOutBufferSize() );
        inBuffer = ByteBuffer.allocate( getInBufferSize( numPlayers ) );

        this.serverAddr = serverAddr;
        this.name = name;
        this.id = id;
        this.numPlayers = numPlayers;
        playersInGame = new boolean[ numPlayers ];
        Arrays.fill( playersInGame, 0, numPlayers, true );
        this.listener = listener;
        this.isServer = isServer;
        lastShotFrameId = - SpriteManager.FRAMES_BETWEEN_SHOTS;
        this.playerSprite = playerSprite;
        this.basicSprite = basicSprite;
        if( playerSprite == null && basicSprite == null ) {
            throw new IllegalArgumentException( "Both sprites are null" );
        }
    }

    public boolean isPlayerInGame( int playerId )
    {
        return playersInGame[ playerId ];
    }

    public double getViewerVerticalRotation()
    {
        if( playerSprite != null ) {
            return playerSprite.getViewerVerticalRotation();
        }
        else {
            return basicSprite.getViewerVerticalRotation();
        }
    }

    /** Gets the orientation of the player's sprite's viewerTG and puts it into
    * vector.
    **/
    public void getViewerOrientation( Vector3f direction )
    {
        if( playerSprite != null ) {
            playerSprite.getViewerOrientation( direction );
        }
        else {
            basicSprite.getViewerOrientation( direction );
        }
    }


    public abstract void open() throws IOException;

    public abstract void close() throws IOException;

    /** Tell the server that this player is leaving, then exit.
    **/
    public abstract void closeWithByeMessage() throws IOException;

    /** This is guaranteed to return true at least between the end of open()
    * and the start of close() (or closeWithByeMessage()).
    **/
    public abstract boolean isOpen();


    public void rotate( float x, float y )
    {
        if( Lasertag3DSettings.gravityOn ) {
            //vertically rotate the viewer (but not the actual sprite) client-side
            if( playerSprite != null ) {
                playerSprite.addToViewerVerticalRotation(
                        -y * Lasertag3DSettings.mouseRotateMaxStep
                );
            }
            if( basicSprite != null ) {
                basicSprite.addToViewerVerticalRotation(
                        -y * Lasertag3DSettings.mouseRotateMaxStep
                );
            }
        }
        rotateCore( x, y );
    }

    /** x and y are both on a scale of -1 to 1.
    **/
    public abstract void rotateCore( float x, float y );

    public abstract void keyPressed( int keycode );

    public abstract void keyReleased( int keycode );

    public abstract void fireShot();


    public void finalize()
    {
        if( isOpen() ) {
            try { close(); }
            catch( Exception exc ) {}
        }
    }

    public static boolean isKeyCodeZeroGOnly( int keycode )
    {
        return ( !(keycode==NetworkClient.FAST ||
                keycode==NetworkClient.GO_FORWARDS ||
                keycode==NetworkClient.GO_BACKWARDS ||
                keycode==NetworkClient.STRAFE_LEFT ||
                keycode==NetworkClient.STRAFE_RIGHT)
        );
    }


    public abstract int getOutBufferSize();

    public abstract int getInBufferSize( int numPlayers );
}
