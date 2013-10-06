//LocalClient.java (5/21/2011)
//LT3D local session networking model - no networking
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.media.j3d.Transform3D;

import com.mattweidner.lt3d.Lasertag3DSettings;
import com.mattweidner.lt3d.sprite.*;


public class LocalClient extends NetworkClient {

    private boolean keysPressed[];
    private float xRotation, yRotation;
    private boolean continueRunning;
    private LocalServer server;
    private LocalThread thread;


    public LocalClient( String name, NetworkListener listener,
            PlayerSprite playerSprite, BasicSprite basicSprite )
    {
        super( null, name, (byte) 0, 1, listener, false, playerSprite,
                basicSprite );

        this.server = server;

        keysPressed = new boolean[ NetworkClient.NUM_KEY_CODES ];
    }

    public void setLocalServer( LocalServer server )
    {
        this.server = server;
    }


    public void open() throws IOException
    {
        if( server == null ) {
            throw new NullPointerException( "lc2: server has not been set" );
        }
        continueRunning = true;
        thread = new LocalThread();
        thread.start();
    }

    public boolean isOpen()
    {
        return continueRunning;
    }


    public void rotateCore( float xRotation, float yRotation )
    {
        this.xRotation = xRotation;
        this.yRotation = yRotation;
    }

    public void keyPressed( int keycode )
    {
        if( Lasertag3DSettings.gravityOn ) {
            if( isKeyCodeZeroGOnly( keycode ) ) return;//ignore it
        }

        keysPressed[ keycode ] = true;
    }

    public void keyReleased( int keycode )
    {
        if( Lasertag3DSettings.gravityOn ) {
            if( isKeyCodeZeroGOnly( keycode ) ) return;//ignore it
        }

        keysPressed[ keycode ] = false;
    }

    public void fireShot() {}


    /** Writes a message of the form <code>'b'</code> and then
    * disconnects everything.
    **/
    public void closeWithByeMessage() throws IOException
    {
        continueRunning = false;
        int scores[] = new int[ 0 ];
        listener.gameOver( scores );
    }

    public void close() throws IOException
    {
        continueRunning = false;
    }


    private class LocalThread extends Thread {
        public void run()
        {
            long startTime, diffInMs;
            while( continueRunning ) {
                startTime = System.currentTimeMillis();

                server.process( keysPressed, xRotation, yRotation );
                //reset inputs
                xRotation = 0;
                yRotation = 0;

                diffInMs = System.currentTimeMillis() - startTime;
                if( diffInMs > Lasertag3DSettings.frameDelay ) {
//DEBUG             System.out.println( "lc: in processing loop: diff=" +
//DEBUG                     diffInMs + ", too long"
//DEBUG             );
                    if( diffInMs / Lasertag3DSettings.frameDelay >= 50 ) {
                        //delay is more than 50 times what is expected
                        System.err.println( "Error: lc: in processing loop: diff=" +
                                diffInMs + ", far too long"
                        );
                    }
                }
                else {
                    try { Thread.sleep( Lasertag3DSettings.frameDelay - diffInMs ); }
                    catch( InterruptedException exc ) {}
                }
            }
        }
    }


    public int getOutBufferSize()
    {
        return 13;
    }

    public int getInBufferSize( int numPlayers )
    {
        return 0;
    }
}
