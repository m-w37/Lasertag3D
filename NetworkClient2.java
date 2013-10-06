//NetworkClient2.java (7/23/2010)
//LT3D networking model 2 - packets, with server-side frame loop
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net;


import java.io.*;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.InetSocketAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.channels.DatagramChannel;
import java.nio.*;
import java.util.Timer;
import java.util.TimerTask;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;
import javax.media.j3d.Transform3D;

import com.mattweidner.lt3d.Lasertag3DSettings;
import com.mattweidner.lt3d.sprite.*;


public class NetworkClient2 extends NetworkClient {


    public final static int PACKET_TIMEOUT = 500;//packet timeout, in ms
    public final static long TIMEOUT = 5 * 1000000000L;
            //time to wait for server data or client 'a' messages, in ns
    private final static int NUM_SEND_DATA = 2;//times to send normal data packets
    private final static int NUM_SEND_IMP = 5;//times to send important packets
    private final static int ALIVE_MESSAGE_INTERVAL = 1000;
            //time between 'a' messages, in ms
    private final static int WAIT_TIME = NetworkServer2.WAIT_TIME +
            NetworkServer2.DELAY + 5000;
            //time (ms) to wait for server


    private DatagramSocket receiveSocket;//receives packets from the server
    private PacketSorter packetSorter;/** Sorts received packets and drops
            * ones that are not needed
            **/
    private DatagramSocket sendSocket;//sends packets to the server

    private int gamePassword;

    private boolean keysPressed[];
    private long lastReceiveTime;
    private int currentFrameId;
    private int currentPacketId;
    private long startTime;

    private Timer timeoutTimer;

    private Thread inThread;
    private Thread outThread;/** Only job is to send occasional messages to the server
            * telling it [the server] that this client still has a connection **/
    private boolean continueRunning, processGameCommands, closed;


    public NetworkClient2( InetSocketAddress serverAddr,
            String name, int gamePassword, byte id, int numPlayers,
            NetworkListener listener, boolean isServer,
            PlayerSprite playerSprite, BasicSprite basicSprite )
            throws IOException, NullPointerException
    {
        super( serverAddr, name, id, numPlayers, listener, isServer,
                playerSprite, basicSprite );

        receiveSocket = new DatagramSocket();
        packetSorter = new PacketSorter( receiveSocket,
                getInBufferSize( numPlayers ), listener, this
        );

        keysPressed = new boolean[ NetworkClient.NUM_KEY_CODES ];

        this.gamePassword = gamePassword;
        System.out.println( "gamePassword=" + gamePassword );

        if( isServer ) {
//DEBUG     System.out.println( "nc2: isServer" );
            //set the server's address to localhost if it is on this computer
            this.serverAddr = new InetSocketAddress( "127.0.0.1", serverAddr.getPort() );
        }

        continueRunning = true;
        inThread = new InThread();
        inThread.start();
    }


    public void open() throws IOException
    {
        if( closed ) return;//this was closed before it was opened

        //start the timeout timer; wait 15 seconds for the server's response
        TimerTask timeoutTask = new TimerTask() {
            public void run()
            {
                if( (processGameCommands == false) && (closed == false) ) {
                    ServerTimeoutException exc =
                            new ServerTimeoutException( "Server timed out" );
                    listener.exceptionThrown( exc );
                }
                timeoutTimer.cancel();
            }
        };
        timeoutTimer = new Timer( true );
        timeoutTimer.schedule( timeoutTask, WAIT_TIME );

        listener.messageForUser( "Waiting for server..." );

        //connect to server for sending info        
        sendSocket = receiveSocket;

        synchronized( outBuffer ) {
            outBuffer.clear();
            outBuffer.put( (byte) 's' );
            outBuffer.put( id );
            outBuffer.putInt( gamePassword );
            outBuffer.flip();
            sendToServer( outBuffer, NUM_SEND_IMP );
        }
//DEBUG  System.out.println( "nc2: 's' sent; serverAddr=" + serverAddr );

        processGameCommands = false;
    }

    public boolean isOpen()
    {
        return continueRunning;
    }


    public void rotateCore( float xRotation, float yRotation )
    {
        sendRotation( xRotation, yRotation );
    }

    public void keyPressed( int keycode )
    {
        if( Lasertag3DSettings.gravityOn ) {
            if( NetworkClient.isKeyCodeZeroGOnly( keycode ) ) return;//ignore it
        }

        if( keysPressed[ keycode ] == false ) {
            keysPressed[ keycode ] = true;
            sendKeycodes();
        }
    }

    public void keyReleased( int keycode )
    {
        if( Lasertag3DSettings.gravityOn ) {
            if( NetworkClient.isKeyCodeZeroGOnly( keycode ) ) return;//ignore it
        }

        if( keysPressed[ keycode ] ) {
            keysPressed[ keycode ] = false;
            sendKeycodes();
        }
    }

    public void fireShot()
    {
        sendShot();
    }


    private void sendRotation( float xRotation, float yRotation )
    {
        if( processGameCommands == false ) return;

        try {
            synchronized( outBuffer ) {

                outBuffer.clear();
                outBuffer.put( (byte) 'r' );

                outBuffer.putInt( currentPacketId );
                currentPacketId++;

                outBuffer.putFloat( xRotation );
                outBuffer.putFloat( yRotation );

                outBuffer.flip();
                sendToServer( outBuffer, NUM_SEND_DATA );

                //server timeout testing
                checkServerTimeout();
            }
        }

        catch( Exception exc ) { listener.exceptionThrown( exc ); }
    }

    private void sendKeycodes()
    {
        if( processGameCommands == false ) return;

        try {
            synchronized( outBuffer ) {

                outBuffer.clear();
                outBuffer.put( (byte) 'k' );

                outBuffer.putInt( currentPacketId );
                currentPacketId++;

                short keycodes = 0;
                for( int i = 0; i < keysPressed.length; i++ ) {
                    if( keysPressed[ i ] ) keycodes += 1 << i;
                }
                outBuffer.putShort( keycodes );

                outBuffer.flip();
                sendToServer( outBuffer, NUM_SEND_DATA );

                //server timeout testing
                checkServerTimeout();
            }
        }

        catch( Exception exc ) { listener.exceptionThrown( exc ); }
    }

    Vector3f direction = new Vector3f();
    private void sendShot()
    {
        if( processGameCommands == false ) return;

        try {
            synchronized( outBuffer ) {
                getViewerOrientation( direction );

                outBuffer.clear();
                outBuffer.put( (byte) 'f' );

                outBuffer.putInt( currentPacketId );
                currentPacketId++;

                outBuffer.putFloat( direction.getX() );
                outBuffer.putFloat( direction.getY() );
                outBuffer.putFloat( direction.getZ() );

                outBuffer.flip();
                sendToServer( outBuffer, NUM_SEND_DATA );

                //server timeout testing
                checkServerTimeout();
            }
        }

        catch( Exception exc ) { listener.exceptionThrown( exc ); }
    }


    /** Writes a message of the form <code>'b'</code> and then
    * disconnects everything.
    **/
    public void closeWithByeMessage() throws IOException
    {
        continueRunning = false;
        closed = true;

        synchronized( outBuffer ) {
            outBuffer.clear();
            outBuffer.put( (byte) 'b' );
            outBuffer.flip();
            sendToServer( outBuffer, NUM_SEND_IMP );
        }

        close();
    }

    public void close() throws IOException
    {
        continueRunning = false;
        closed = true;

        packetSorter.close();
        receiveSocket.close();
        sendSocket.close();
    }


    private void sendToServer( ByteBuffer buffer, int numTimes ) throws IOException
    {
        for( int i = 0; i < numTimes; i++ ) {
            sendToServer( buffer );
            buffer.flip();
        }
    }

    private void sendToServer( ByteBuffer buffer ) throws IOException
    {
        DatagramPacket packet = new DatagramPacket( buffer.array(),
                buffer.arrayOffset(), buffer.capacity(),
                serverAddr.getAddress(), serverAddr.getPort()
        );
        sendSocket.send( packet );
    }


    private void checkServerTimeout()
    {
        if( processGameCommands == false ) return;

        //server timeout testing
        if( (System.nanoTime() - lastReceiveTime) >= TIMEOUT ) {
            //server timed out
            listener.serverTimedOut();
        }
    }


    /** This checks that the packet is from the server, takes care of it
    * if it is not, and then returns a boolean indicating whether or not
    * the packet should be processed normally.
    **/
    public boolean checkPacket( DatagramPacket packet )
    {
        boolean isFromServer = serverAddr.equals(
            packet.getSocketAddress()
        );

        if( isFromServer == false ) {
/*DEBUG*/   System.out.println( "nc2: not server, addr=" +
/*DEBUG*/           packet.getSocketAddress() );
            return false;
        }

        return true;
    }

    private class InThread extends Thread {

        public void run()
        {
            DatagramPacket packet;

            while( continueRunning ) {
                try {
                    synchronized( inBuffer ) {
//DEBUG                 System.out.println( "nc2: in sync(inBuffer)" );

                        packet = packetSorter.receivePacket();
//DEBUG                 System.out.println( "nc2: received" );

                        if( checkPacket( packet ) == false ) continue;
                        //else process normal input
                            
                        inBuffer.clear();
                        inBuffer.put( packet.getData() );
                        inBuffer.flip();

                        lastReceiveTime = System.nanoTime();

                        byte identifier = inBuffer.get();

                        if( identifier == (byte) 's' ) {//start message
//DEBUG                     System.out.println( "nc2: 's' received" );
                            if( processGameCommands == false ) {
                                listener.messageForUser( null );
                                processGameCommands = true;
                                outThread = new OutThread();
                                outThread.start();
                            }
                            //else ignore; server sends several of these
                        }

                        else if( identifier == (byte) 'b' ) {//bye message
//DEBUG                     System.out.println( "nc2: 'b' message received" );
                            int playerId = inBuffer.get();
                            if( playersInGame[ playerId ] ) {
                                playersInGame[ playerId ] = false;
                                listener.playerLeft( playerId, inBuffer.get() );
                            }
                        }//end else if 'b'

                        else if( processGameCommands == false ) {
                            continue;//prevent processing
                        }

                        else if( identifier == (byte) 'p' ) {//data about scene
                            if( isServer ) continue;//server does this for us
                            //else

                            int frameId = inBuffer.getInt();
                            boolean skip = false;
                            if( frameId <= currentFrameId ) {//earlier frame
                                if( currentFrameId - frameId >=
                                        Lasertag3DSettings.framesPerScoreUpdate
                                ) {
                                    continue;//very old
                                }
                                skip = true;
                            }
                            else currentFrameId = frameId;

                            //process data

                            /* Get players hit and who shot them;
                                    process this even if it's an earlier frame.
                                    The effect of duplicate packets is minimal
                                    and so is ignored here.
                            */ 
                            byte playersHit[] = new byte[ inBuffer.get() ];
                            byte shooters[] = new byte[ playersHit.length ];
                            for( int i = 0; i < playersHit.length; i++ ) {
                                playersHit[ i ] = inBuffer.get();
                                shooters[ i ] = inBuffer.get();
                            }
                            if( playersHit.length != 0 ) {
                                listener.updateHits( playersHit, shooters );
                            }

                            // Also process the scores; they should still be current.
                            int scores[] = null;
                            if( inBuffer.get() == (byte) 's' ) {
                                scores = new int[ numPlayers ];
                                for( int i = 0; i < scores.length; i++ ) {
                                    scores[ i ] = inBuffer.getInt();
                                }
                                listener.updateScores( scores );
                            }
                            
                            if( skip ) continue;//skip the rest

                            //get player positions
                            Vector3f playerPositions[] = new Vector3f[ numPlayers ];
                            Matrix3f playerRotations[] = new Matrix3f[ numPlayers ];
                            for( int i = 0; i < numPlayers; i++ ) {
                                playerPositions[ i ] = new Vector3f(
                                        inBuffer.getFloat(), inBuffer.getFloat(),
                                        inBuffer.getFloat()
                                );
                                playerRotations[ i ] = new Matrix3f( inBuffer.getFloat(),
                                        inBuffer.getFloat(), inBuffer.getFloat(),
                                        inBuffer.getFloat(), inBuffer.getFloat(),
                                        inBuffer.getFloat(), inBuffer.getFloat(),
                                        inBuffer.getFloat(), inBuffer.getFloat()
                                );
                            }

                        /* no dot positions... */
                            Vector3f dotPositions[] = null;
                        /* ... end no dot positions */
/*CHANGE THE LINES ABOVE THIS//get dot positions
ALSO UPDATE IN BUFFER SIZE  Vector3f dotPositions[] = new Vector3f[ inBuffer.get() ];
                            for( int i = 0; i < dotPositions.length; i++ ) {
                                dotPositions[ i ] = new Vector3f(
                                        inBuffer.getFloat(), inBuffer.getFloat(),
                                        inBuffer.getFloat() );
                            }
*/
                            //tell listener
                            listener.updateScene( playerPositions, playerRotations,
                                    dotPositions
                            );
                        }//end else if( 'p' )

                        else if( identifier == (byte) 'e' ) {//game over
//DEBUG                     System.out.println( "nc2: e received" );
                            int scores[] = new int[ numPlayers ];
                            for( int i = 0; i < numPlayers; i++ ) {
                                scores[ i ] = inBuffer.getInt();
                            }
                            processGameCommands = true;
                            close();
                            listener.gameOver( scores );
                        }//end else if( 'e' )

                        lastReceiveTime = System.nanoTime();

                    }//end synchronized( inBuffer )

                }//end try
                catch( Exception exc ) {
                    if( continueRunning ) listener.exceptionThrown( exc );
                    //else ignore; exc was caused by close()
                }

            }//end while( continueRunning )
        }//end run

    }//end class InThread


    private class OutThread extends Thread {
        public void run()
        {
            long startTime, diffInMs;
            while( continueRunning ) {
                startTime = System.currentTimeMillis();
                try {
                    synchronized( outBuffer ) {
                        outBuffer.clear();
                        outBuffer.put( (byte) 'a' );
                        outBuffer.flip();
                        sendToServer( outBuffer, NUM_SEND_IMP );
//DEBUG                 System.out.println( "nc2: sent 'a' message to server" );
                    }
                }
                catch( Exception exc ) {
                    if( continueRunning ) listener.exceptionThrown( exc );
                    //else ignore; exc was caused by close()
                }

                diffInMs = System.currentTimeMillis() - startTime;
                if( diffInMs > ALIVE_MESSAGE_INTERVAL ) {
                    System.err.println( "Error: nc2: in 'a' message thread: diff=" +
                            diffInMs + ", too long" );
                }
                else {
                    try { Thread.sleep( ALIVE_MESSAGE_INTERVAL - diffInMs ); }
                    catch( InterruptedException exc ) {}
                }
            }
        }
    }


    public int getOutBufferSize()
    {
        return 17;
    }

    public int getInBufferSize( int numPlayers )
    {
        return 54 * numPlayers + 6;
    }
}
