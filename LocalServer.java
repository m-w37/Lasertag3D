//LocalServer.java (5/21/2011)
//Lasertag3D networking server for local sessions - no networking
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;

import com.mattweidner.lt3d.sprite.SpriteManager;
import com.mattweidner.lt3d.Lasertag3DSettings;


public class LocalServer extends NetworkServer {

    private boolean continueRunning;
    private int frameId;

    public LocalServer( SpriteManager manager, NetworkClient client )
    {
        super( new InetSocketAddress[ 1 ], manager, client );
    }
    
    
    public void open() throws IOException {}

    //return value is meaningless
    public boolean isOpen()
    {
        return false;
    }


    public void endGame() throws IOException { close(); }

    public void close() throws IOException
    {
        continueRunning = false;
    }

    public void printScores()
    {
        continueRunning = false;

        //print scores
        int scores[] = manager.getScores();
        int numShots[] = manager.getNumShots();
        int hitsTaken[] = manager.getHitsTaken();
        int hitsMade[] = manager.getHitsMade();
        System.out.println( "Game Stats" );
        System.out.println( "Id\tscore\tnumShots\thitsTaken\thitsMade" );
        for( int i = 0; i < scores.length; i++ ) {
            System.out.println( i + "\t" + scores[ i ] + "\t" + numShots[ i ] +
                    "\t\t" + hitsTaken[ i ] + "\t\t" + hitsMade[ i ]
            );
        }
    }

    public int[] getScores()
    {
        return manager.getScores();
    }

    public void process( boolean keysPressed[], float xRotation,
            float yRotation )
    {
        //generate keyCode short from the boolean array
        short keycodes = 0;
        for( int i = 0; i < keysPressed.length; i++ ) {
            if( keysPressed[ i ] ) keycodes += 1 << i;
        }
        try {
            //format the data for NetworkServer.getInputFrame
            inBuffer.clear();
            inBuffer.putShort( keycodes );
            if( xRotation == 0 & yRotation == 0 ) inBuffer.put( (byte) 0 );
            else {
                inBuffer.put( (byte) 'r' );
                inBuffer.putFloat( xRotation );
                inBuffer.putFloat( yRotation );
            }
            inBuffer.put( (byte) 0 );
            inBuffer.flip();
            //get an input frame from inBuffer
            InputFrame frame = getInputFrame( inBuffer, 0, frameId++ );
            processFrame( frame );
        }
        catch( Exception exc ) {
            if( continueRunning ) exc.printStackTrace();
        }
    }
}
