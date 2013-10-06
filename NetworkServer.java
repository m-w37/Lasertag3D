//NetworkServer.java (7/15/2010)
//handles the server-side networking aspects of Lasertag3D
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net;


import java.io.*;
import java.net.*;
import java.nio.*;

import javax.vecmath.*;

import java.util.*;

import com.mattweidner.lt3d.sprite.*;
import com.mattweidner.lt3d.Lasertag3DSettings;


public abstract class NetworkServer {

    protected Hashtable<InetSocketAddress, Byte> players;
    protected InetSocketAddress[] playersById;

    protected PlayerSprite playerSprites[];

    protected ByteBuffer inBuffer, outBuffer;

    protected SpriteManager manager;
    protected NetworkClient client;
    protected int currentPacketId;
    protected int numPlayers;

    protected Vector3f spareVec = new Vector3f();


    protected NetworkServer( InetSocketAddress players[], SpriteManager manager,
            NetworkClient client )
    {
        super();

        if( Lasertag3DSettings.localSession == false ) {
            if( players.length > 256 ) {
                throw new IllegalArgumentException("Too many players" );
            }

            this.playersById = players;
            this.players = new Hashtable<InetSocketAddress, Byte>();
            for( byte b = 0; b < players.length; b++ ) {
                if( players[ b ] != null ) {
                    this.players.put( players[ b ], new Byte( b ) );
                }
            }
        }

        playerSprites = manager.getPlayerSprites();

        inBuffer = ByteBuffer.allocate( client.getOutBufferSize() );
        outBuffer = ByteBuffer.allocate( client.getInBufferSize( players.length ) );

        this.manager = manager;
        this.client = client;
        this.numPlayers = players.length;
    }


    public abstract void open() throws IOException;

    /** Closes all streams, with no warning given to clients.
    **/
    public abstract void close() throws IOException;

    /** This is guaranteed to return true at least between the end of open()
    * and the start of close() (all forms).
    **/
    public abstract boolean isOpen();
    
    /** Ends the game, then closes
    **/
    public abstract void endGame() throws IOException;


    public void finalize()
    {
        if( isOpen() ) {
            try { close(); }
            catch( Exception exc ) {}
        }
    }


    protected InputFrame getInputFrame( ByteBuffer buffer, int playerId,
            int frameId )
    {
        short keycodes = buffer.getShort();
//DEBUG  System.out.println( "ns, keycodes=" + keycodes );
        byte rotationIndicator = buffer.get();
        float xRotation = 0, yRotation = 0;
        if( rotationIndicator == (byte) 'r' ) {
            xRotation = buffer.getFloat();
            yRotation = buffer.getFloat();
        }
        Vector3f shotDirection = null;
        if( buffer.get() == (byte) 'f' ) {
            shotDirection = new Vector3f( buffer.getFloat(), buffer.getFloat(),
                    buffer.getFloat() );
        }
        return new InputFrame( frameId, playerId, keycodes, xRotation,
                yRotation, shotDirection );
    }


    private final Vector3f forwards = new Vector3f( 0, 0, -1 ),
            up = new Vector3f( 0, 1, 0 ),
            left = new Vector3f( -1, 0, 0 );
    private final Vector3f upRotateAxis = new Vector3f( 1, 0, 0 ),
            leftRotateAxis = new Vector3f( 0, 1, 0 ),
            ccwRotateAxis = new Vector3f( 0, 0, 1 );
            
    protected void processFrame( InputFrame frame )
    {
        if( frame == null ) return;//deal with this for subclasses

        //else
        //get which keys are pressed
        boolean keys[] = new boolean[ NetworkClient.NUM_KEY_CODES ];
        for( int i = 0; i < NetworkClient.NUM_KEY_CODES; i++ ) {
            keys[ i ] = ( (frame.keycodes & (1 << i)) != 0 );
        }

        //rotate based on the mouse's x movement
        if( frame.xRotation != 0 ) {
            rotate( leftRotateAxis, -frame.xRotation, frame.playerId );
        }

        //perform actions based on which keys are pressed
        //if keys with opposite commands are both pressed, do nothing

        //these are only used if there is no gravity
        if( Lasertag3DSettings.gravityOn == false ) {
            if( frame.yRotation != 0 ) {
                rotate( upRotateAxis, -frame.yRotation, frame.playerId );
            }
            if( keys[ NetworkClient.ROTATE_CCW ] ^ keys[ NetworkClient.ROTATE_CW ] ) {
                rotate( ccwRotateAxis, keys[ NetworkClient.ROTATE_CCW ],
                        keys[ NetworkClient.FAST ], frame.playerId
                );
            }
            if( keys[ NetworkClient.STRAFE_UP ] ^ keys[ NetworkClient.STRAFE_DOWN ] ) {
                move( up, keys[ NetworkClient.STRAFE_UP ],
                        keys[ NetworkClient.FAST ], frame.playerId
                );
            }
        }

        //these 'if' blocks are used regardless of gravity
        if( keys[ NetworkClient.GO_FORWARDS ] ^ keys[ NetworkClient.GO_BACKWARDS ] ) {
            move( forwards, keys[ NetworkClient.GO_FORWARDS ],
                    keys[ NetworkClient.FAST ], frame.playerId
            );
        }
        if( keys[ NetworkClient.STRAFE_LEFT ] ^ keys[ NetworkClient.STRAFE_RIGHT ] ) {
            move( left, keys[ NetworkClient.STRAFE_LEFT ],
                    keys[ NetworkClient.FAST ], frame.playerId
            );
        }

        //process fire message, if any
        if( frame.shotDirection != null ) {//fire message
            fireBeam( frame.playerId, frame.frameId, frame.shotDirection );
        }
    }


    protected void move( Vector3f axis, boolean directionBool, boolean fast, int playerId )
    {
        float speed = (fast)? PlayerSprite.RUN_STEP: PlayerSprite.MOVE_STEP;
        int direction = (directionBool)? 1: -1;
        spareVec.scale( direction * speed, axis );
        if( playerSprites[ playerId ].canMove( spareVec ) ) {
            playerSprites[ playerId ].move( spareVec );
        }
    }

    protected void rotate( Vector3f axis, float velocity, int playerId )
    {
        float angle = velocity * Lasertag3DSettings.mouseRotateMaxStep;
        if( playerSprites[ playerId ].canRotate( axis, angle ) ) {
            playerSprites[ playerId ].rotate( axis, angle );
        }
    }

    protected void rotate( Vector3f axis, boolean directionBool, boolean fast, int playerId )
    {
        float angle = (fast)? PlayerSprite.ROTATE_FAST_STEP: PlayerSprite.ROTATE_STEP;
        angle *= (directionBool)? 1: -1;
        if( playerSprites[ playerId ].canRotate( axis, angle ) ) {
            playerSprites[ playerId ].rotate( axis, angle );
        }
    }

    protected void fireBeam( int playerId, int frameId, Vector3f direction )
    {
        manager.fireBeam( playerId, frameId, direction );
    }


    protected class InputFrame {

        protected int frameId;
        protected int playerId;
        protected short keycodes;
        protected float xRotation, yRotation;
        protected Vector3f shotDirection;

        protected InputFrame( int playerId )
        {
            this.playerId = playerId;
            //use defaults: 0,0,null
        }
    
        protected InputFrame( int frameId, int playerId, short keycodes,
                float xRotation, float yRotation, Vector3f shotDirection )

        {
            set( frameId, playerId, keycodes, xRotation, yRotation,
                    shotDirection );
        }

        protected void set( int frameId, int playerId, short keycodes,
                float xRotation, float yRotation, Vector3f shotDirection )
        {
            this.frameId = frameId;
            this.playerId = playerId;
            this.keycodes = keycodes;
            this.xRotation = xRotation;
            this.yRotation = yRotation;
            this.shotDirection = shotDirection;
        }
    }
}
