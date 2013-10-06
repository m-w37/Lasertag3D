//NetworkServer2.java (8/5/2010)
//Lasertag3D networking server option 2 - packets, with server-side frame loop
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net;


import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;
import javax.vecmath.Matrix3f;

import java.util.concurrent.LinkedBlockingQueue;

import com.mattweidner.lt3d.sprite.SpriteManager;
import com.mattweidner.lt3d.Lasertag3DSettings;


public class NetworkServer2 extends NetworkServer {

    private final static int NUM_SEND_IMP = 5;//number of times to send important packets
    public final static int WAIT_TIME = 10000;//time, in ms, to wait for connections
    public final static int DELAY = 5000;//time, in ms, for clients to wait before open()ing
    private final static int L_ADDR_MESSAGE_INTERVAL = 1000;//time (ms) between 'l' messages


    private NetworkListener listener;

    private DatagramSocket receiveSocket;
    private DatagramSocket sendSocket;

    private int playerGamePasswords[];
    private InputFrame playerStates[];
    private int lastPacketIds[];
    private long lastReceiveTimes[];
    private int playersConnected;
    private boolean playersConnectedList[];

    private int currentFrameId;
    private long startTime;
    
    private Thread inThread, outThread;
    private boolean continueRunning;
    private boolean processGameCommands;
    private boolean closed = false;


    //note that port is the port to bind to; the group's port is contained in groupAddr
    public NetworkServer2( int playerGamePasswords[], int port,
            SpriteManager manager, NetworkClient client,
            NetworkListener listener )
            throws IOException
    {
        //the players' addresses are not figured out until they connect
        super( new InetSocketAddress[ playerGamePasswords.length ], manager,
                client );
        this.playerGamePasswords = playerGamePasswords;
        this.listener = listener;

//DEBUG  System.out.println( "ns2: port=" + port );
/*        receiveSocket = new DatagramSocket( null );
        receiveSocket.setReuseAddress( true );
        receiveSocket.bind( new InetSocketAddress( port ) );

        sendSocket = new DatagramSocket( null );
        sendSocket.setReuseAddress( true );
        sendSocket.bind( new InetSocketAddress( port ) );
*/
        sendSocket = new DatagramSocket( port );
        receiveSocket = sendSocket;

        lastPacketIds = new int[ playerSprites.length ];
        lastReceiveTimes = new long[ playerSprites.length ];

        playerStates = new InputFrame[ playerSprites.length ];

        playersConnectedList = new boolean[ playerSprites.length ];

        //start listening for connections
        processGameCommands = false;
        continueRunning = true;
        inThread = new InThread();
        inThread.start();
    }
    
    
    public void open() throws IOException
    {
        if( closed ) return;//this was closed before it was opened

        //wait for all players to connect, or WAIT_TIME ms
        long startTime = System.currentTimeMillis();//innacuracies ignorable

        while( (playersConnected < numPlayers) &&
                ((System.currentTimeMillis()-startTime) < WAIT_TIME )
        ) {
            try { Thread.sleep( 50 ); }
            catch( InterruptedException exc ) {}
        }

/*DEBUG*/System.out.println( "Timedout/all connected: playersConnected=" + playersConnected );

        //tell clients to start
        /* This start signal will give players with less delay a short amount of
            time to move before players with longer delay can; however, the difference
            will be equal to the difference between the round-trip packet delays,
            so one that gives an intolerable disadvantage at startup would already
            be giving an intolerable disadvantage in the rest of the game
            (unavoidable in this network model).
        */
        synchronized( outBuffer ) {
            outBuffer.clear();
            outBuffer.put( (byte) 's' );
            outBuffer.flip();
            //multiple times, to decrease likelihood of packet loss
            sendToGroup( outBuffer, NUM_SEND_IMP );
        }
//DEBUG  System.out.println( "ns2: 's' sent" );

        for( int i = 0; i < lastReceiveTimes.length; i++ ) {
            lastReceiveTimes[ i ] = System.nanoTime();
        }

        if( playersConnected < numPlayers ) {//some players did not connect
            for( int i = 0; i < numPlayers; i++ ) {
                if( playersConnectedList[ i ] == false ) {//player i did not connect
/*DEBUG*/           System.out.println( "ns2: player " + i + " did not connect" );
                    playerLeft( i, NetworkClient.LOST_CONNECTION );
                }
            }
        }

        outThread = new OutThread();
        outThread.start();
        processGameCommands = true;
    }
    
    public boolean isOpen()
    {
        return continueRunning;
    }


    public void endGame() throws IOException
    {
        continueRunning = false;
        closed = true;

        synchronized( outBuffer ) {

            outBuffer.clear();
            outBuffer.put( (byte) 'e' );
            //put scores
            int scores[] = manager.getScores();
            for( int i = 0; i < scores.length; i++ ) {
                outBuffer.putInt( scores[ i ] );
            }

            outBuffer.flip();
            sendToGroup( outBuffer, NUM_SEND_IMP );
//DEBUG     System.out.println( "ns2: end sent" );
        }

        close();
    }


    public void close() throws IOException
    {
        continueRunning = false;
        closed = true;

        //print scores
        int scores[] = manager.getScores();
        int numShots[] = manager.getNumShots();
        int hitsTaken[] = manager.getHitsTaken();
        int hitsMade[] = manager.getHitsMade();

        sendSocket.close();
        receiveSocket.close();
    }


    private void sendToGroup( ByteBuffer buffer, int numTimes ) throws IOException
    {
        for( int i = 0; i < numTimes; i++ ) {
            sendToGroup( buffer );
            buffer.flip();
        }
    }

    private void sendToGroup( ByteBuffer buffer ) throws IOException
    {
        for( int i = 0; i < playersById.length; i++ ) {
            //once the game starts, don't send to players who did not join the game
            if( processGameCommands && (playersConnectedList[ i ] == false) ) continue;
            //avoid NullPointerExceptions
            if( playersById[ i ] == null ) continue;

            DatagramPacket packet = new DatagramPacket( buffer.array(),
                    buffer.arrayOffset(), buffer.capacity(),
                    playersById[ i ].getAddress(), playersById[ i ].getPort()
            );
            sendSocket.send( packet );
        }
    }


    private void playerLeft( int playerId, int reason ) throws IOException
    {
//DEBUG  System.out.println( "ns2: playerLeft, id=" + playerId + ", reason=" + reason );
        playersConnectedList[ playerId ] = false;
        manager.playerLeft( playerId );
        playerStates[ playerId ] = null;
        synchronized( outBuffer ) {
            outBuffer.clear();
            outBuffer.put( (byte) 'b' );
            outBuffer.put( (byte) playerId );
            outBuffer.put( (byte) reason );
            outBuffer.flip();
            sendToGroup( outBuffer );
        }
    }


    private class InThread extends Thread {

        public void run()
        {
            byte array[] = new byte[ client.getOutBufferSize() ];
            DatagramPacket inPacket = new DatagramPacket( array, array.length );

            while( continueRunning ) {
                try {
                    synchronized( inBuffer ) {

//DEBUG                 System.out.println( "ns2: in sync" );

                        receiveSocket.receive( inPacket );
                        inBuffer.clear();
                        inBuffer.put( array );
                        inBuffer.flip();
//DEBUG                 System.out.println( "ns2: received" );
                
                        InetSocketAddress packetAddress =
                                (InetSocketAddress) inPacket.getSocketAddress();
                        Byte playerIdObj = players.get( packetAddress );
                        if( playerIdObj == null ) {
                            //should be a player joining
                            if( inBuffer.get() == (byte) 's' ) {
                                byte playerId = inBuffer.get();
                                int password = inBuffer.getInt();
                                System.out.println( "id=" + playerId +
                                        ", received password=" + password +
                                        ", real password=" +
                                        playerGamePasswords[ playerId ] );
                                if( playerId >= 0 && playerId <
                                        playerGamePasswords.length ) {
                                    if( playerGamePasswords[ playerId ] == password ) {
                                        //password is correct
/*DEBUG*/                               System.out.println( "ns2: 's' received, playerId=" +
/*DEBUG*/                                       playerId + ", address=" + packetAddress );
                                        lastReceiveTimes[ playerId ] = System.nanoTime();
                                        assert (playersConnectedList[ playerId ] == false);
                                        //connect the player
                                        players.put( packetAddress, playerId );
                                        playersById[ playerId ] = packetAddress;
                                        playersConnected++;
                                        playersConnectedList[ playerId ] = true;
                                        playerStates[ playerId ] = new InputFrame( playerId );
                                        continue;
                                    }
                                }
                            }
                            else System.out.println( "not 's'" );

                            if( playerIdObj == null ) {//still
/*DEBUG*/                       System.out.println( "ns2: unknown address=" +
/*DEBUG*/                               inPacket.getSocketAddress() );
                                continue;//unknown sender, ignore it
                            }
                        }

                        //else
                        byte playerId = playerIdObj.byteValue();
                        lastReceiveTimes[ playerId ] = System.nanoTime();

                        byte identifier = inBuffer.get();

                        if( playersConnectedList[ playerId ] == false ) {
                            continue;//player left, ignore stray packets
                        }

                        else if( identifier == (byte) 'a' ) {//alive message
                            //only function is to get lastReceiveTime, done above
                        }

                        else if( identifier == (byte) 'b' ) {//bye message, player left
                            playerLeft( playerId, NetworkClient.LEFT_VOLUNTARILY );
                        }

                        else if( processGameCommands == false ) {
                            continue;//prevent processing
                        }

                        else if( identifier == (byte) 'r' ) {//rotation message
                            int packetId = inBuffer.getInt();
                            if( packetId <= lastPacketIds[ playerId ] ) continue;
                                    //earlier message, ignore it
                            //else
                            lastPacketIds[ playerId ] = packetId;
                            
                            playerStates[ playerId ].xRotation = inBuffer.getFloat();
                            playerStates[ playerId ].yRotation = inBuffer.getFloat();
                        }

                        else if( identifier == (byte) 'k' ) {//keycode message
                            int packetId = inBuffer.getInt();
                            if( packetId <= lastPacketIds[ playerId ] ) continue;
                                    //earlier message, ignore it
                            //else
                            lastPacketIds[ playerId ] = packetId;
                            
                            short keycodes = inBuffer.getShort();
                            playerStates[ playerId ].keycodes = keycodes;
                        }

                        else if( identifier == (byte) 'f' ) {//fire message
                            int packetId = inBuffer.getInt();

                            playerStates[ playerId ].shotDirection = new Vector3f(
                                    inBuffer.getFloat(), inBuffer.getFloat(),
                                    inBuffer.getFloat() );
                        }

                    }//end synchronized( inBuffer )
                }//end try

                catch( Exception exc ) {
                    if( continueRunning ) exc.printStackTrace();
                    //else ignore
                }

            }//end while loop
        }//end run()
    }//end class InThread


    private class OutThread extends Thread {

        private Matrix3f spareM3f = new Matrix3f();
        public void run()
        {
            startTime = System.nanoTime();
            long waitInMs;
            long lastScoresSend = -Lasertag3DSettings.framesPerScoreUpdate;

            while( continueRunning ) {

                try {

                    //set currentFrameId
                    currentFrameId = (int) (Math.round(
                            (System.nanoTime() - startTime) /
                            (Lasertag3DSettings.frameDelay * 1000000)
                    ));

                    //process new keycodes and reset playerStates
                    for( int i = 0; i < playerStates.length; i++ ) {
                        if( playersConnectedList[ i ] ) {
                            processFrame( playerStates[ i ] );
                            playerStates[ i ].frameId = currentFrameId + 1;
                            playerStates[ i ].xRotation = 0;
                            playerStates[ i ].yRotation = 0;
                            playerStates[ i ].shotDirection = null;
                        }
                    }

                    //then send the state message
                    synchronized( outBuffer ) {
                        outBuffer.clear();
                        outBuffer.put( (byte) 'p' );
                        outBuffer.putInt( currentFrameId + 1 );

                        //write out players hit and who shot them
                        int playersHit[] = manager.getPlayersHit();
                        int shooters[] = manager.getShooters();
                        outBuffer.put( (byte) playersHit.length );
                        for( int i = 0; i < playersHit.length; i++ ) {
                            outBuffer.put( (byte) playersHit[ i ] );
                            outBuffer.put( (byte) shooters[ i ] );
                        }

                        //occasionally send out score data
                        if( ((currentFrameId+1) - lastScoresSend >=
                                Lasertag3DSettings.framesPerScoreUpdate)
                                && manager.isScoresChanged()
                        ) {
                            lastScoresSend = currentFrameId + 1;
                            //send out the scores
                            outBuffer.put( (byte) 's' );
                            int scores[] = manager.getScores();
                            for( int i = 0; i < playerSprites.length; i++ ) {
                                outBuffer.putInt( scores[ i ] );
                            }
                            listener.updateScores( scores );
                        }
                        else outBuffer.put( (byte) 0 );

                        //write out player positions
                        for( int i = 0; i < playerSprites.length; i++ )
                        {
                            playerSprites[ i ].getTranslation( spareVec );
                            outBuffer.putFloat( spareVec.x );
                            outBuffer.putFloat( spareVec.y );
                            outBuffer.putFloat( spareVec.z );
                            //output the rotation matrix
                            //left->right, then top->bottom
                            playerSprites[ i ].getRotation( spareM3f );
                            for( int r = 0; r < 3; r++ ) {
                                for( int c = 0; c < 3; c++ ) {
                                    outBuffer.putFloat( spareM3f.getElement( r, c ) );
                                }
                            }
                        }
/*                        //write out dot positions
                        Point3f dotPositions[] = manager.getDotPositions();
                        outBuffer.put( (byte) dotPositions.length );
                        for( int i = 0; i < dotPositions.length; i++ ) {
                            outBuffer.putFloat( dotPositions[ i ].x );
                            outBuffer.putFloat( dotPositions[ i ].y );
                            outBuffer.putFloat( dotPositions[ i ].z );
                        }
*/
                        //send the packet
                        outBuffer.flip();
                        //only send once; high traffic worse than packet losses
                        sendToGroup( outBuffer );

                        //check for timed-out clients
                        for( int i = 0; i < playerStates.length; i++ ) {
                            if( playersConnectedList[ i ] ) {//if player is still in the game
                                if( (System.nanoTime() - lastReceiveTimes[ i ])
                                        >= NetworkClient2.TIMEOUT ) {
/*DEBUG*/                           System.out.println( "ns2: player timedout" );
                                    playerLeft( i, NetworkClient.LOST_CONNECTION );
                                }
                            }
                        }//end timeout checks

                    }//end synchronized( outBuffer )

                }//end try

                catch( Exception exc ) {
                    if( continueRunning ) exc.printStackTrace();
                    //else ignore; this was caused by close()
                }

                //sleep
                waitInMs = Lasertag3DSettings.frameDelay - (long) (Math.round(
                        ((System.nanoTime() - startTime) % (Lasertag3DSettings.frameDelay *
                        1000000)) / 1000000.0
                ));
                try { Thread.sleep( waitInMs ); }
                catch( InterruptedException exc ) {}

            }//end while loop
        }//end run
    }//end class OutThread
}
