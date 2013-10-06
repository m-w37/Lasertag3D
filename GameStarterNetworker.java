//GameStarterNetworker.java (9/10/2010)
//manages the networking involved in starting a Lasertag3D game
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net.start;


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import com.mattweidner.lt3d.Lasertag3DSettings;
import com.mattweidner.lt3d.net.NetworkListener;


public class GameStarterNetworker implements Runnable {

    public final static int MAX_PLAYERS = 10;
    public final static int TIMEOUT = 1000;//in ms
    public final static int NUM_SEND_PING = 5;


    private String playerName;
    private long playerId;

    private DatagramSocket socket;//used for pinging game servers
    private DatagramPacket packet;
    private Thread pingThread;
    private LinkedBlockingQueue<GameInfo> queue;
    private boolean continueRunning;

    private DatagramSocket serverSocket;//responds to pings
    private DatagramPacket serverPacket;
    private Thread serverThread;
    private boolean serverRunning;

    private boolean gameChosen;
    private GameInfo chosenGame;

    private int ping;//ping to this game's server, in ms

    private boolean isGameStarting;
    private int playersSoFar;
    private boolean isServer;
    private boolean isStartRequestPending;
    private String requestSender;//null or "" if there is no request
    private boolean isOnlyPlayer;
    private boolean isAlreadyRequest;
    private boolean isRequestFailed;
    private boolean wasRequestRejected;
    private byte playerIdInGame;
    private String playerNames[];
    private int playerGamePasswords[];
    private int thisGamePassword;
    private InetAddress thisInetAddr;


    public GameStarterNetworker( String playerName, NetworkListener listener )
            throws IOException, IllegalArgumentException
    {
        if( playerName == null ) {
            throw new IllegalArgumentException( "you must enter a name" );
        }
        playerName = playerName.replace( "\\s", "_" );
        if( playerName.length() > 20 ) playerName = playerName.substring( 0, 20 );

//DEBUG  System.out.println( "gsn: playerName=" + playerName );
        playerName = URLEncoder.encode( playerName, "UTF-8" );
//DEBUG  System.out.println( "gsn: encoded playerName=" + playerName );

        URL url = null;
        try {
            url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=connect&playername=" +
                    playerName + "&password=" + Lasertag3DSettings.password +
                    "&serverport=" + Lasertag3DSettings.serverPort
            );
        }
        catch( MalformedURLException exc ) {
            throw new Error( "Bad SERVER_ADDR in GameStarterNetworker" );
        }

        URLConnection connection = url.openConnection();
        connection.setDefaultUseCaches( false );
        connection.setUseCaches( false );
        connection.connect();

        BufferedReader in = new BufferedReader( new InputStreamReader(
                connection.getInputStream() )
        );
        String line = in.readLine();
        if( "n".equals( line ) ) {//name already taken
            in.close();
            throw new IllegalArgumentException( "your name is already taken" );
        }
        else if( "i".equals( line ) ) {//illegal name
            in.close();
            throw new IllegalArgumentException( "your name is formatted improperly" );
        }
        else if( "s".equals( line ) == false ) {//connection failed for unknown reason
            in.close();
            throw new IOException( "Could not connect to main server" );
        }

        //else
        this.playerId = Long.parseLong( in.readLine() );
        Lasertag3DSettings.password = Integer.parseInt( in.readLine() );
        listener.resetPassword();
        
        this.playerName = playerName;
        in.close();

        //make ping socket
        socket = new DatagramSocket( null );
        socket.setReuseAddress( true );
        socket.setSoTimeout( TIMEOUT );
        socket.bind( null );
        byte buffer[] = new byte[ 1 ];
        packet = new DatagramPacket( buffer, buffer.length );
        queue = new LinkedBlockingQueue<GameInfo>();
        pingThread = new Thread( this );
        continueRunning = true;
        pingThread.start();
    }


    public void run()
    {
        GameInfo game = null;
        byte out[] = new byte[ 1 ];
        out[ 0 ] = (byte) 'p';
        int i;
        boolean success;
        long startTime;
        while( continueRunning ) {
            game = queue.poll();
            if( game != null ) {
                try {
                    //ping the game's server
                    packet.setSocketAddress( game.getServerAddr() );
                    packet.setData( out );
                    startTime = System.nanoTime();
                    for( i = 0; i < NUM_SEND_PING; i++ ) socket.send( packet );
                    //wait for a reply
                    success = false;
                    while( (System.nanoTime() - startTime)/1000000 <= TIMEOUT ) {
                        //the timeout check is to prevent stalling by stray packets
                        socket.receive( packet );
                        if( packet.getSocketAddress().equals( game.getServerAddr() ) &&
                                packet.getData()[ 0 ] == (byte) 'a' ) {
                            success = true;
                            break;
                        }
                        //else continue
                    }
                    if( success ) {
                        game.setPing( (int) ((System.nanoTime() - startTime)/1000000) );
                    }
                    else game.setPing( GameInfo.PING_ERROR );//server did not respond
                }
                catch( SocketTimeoutException exc ) {
                    game.setPing( GameInfo.PING_ERROR );//server did not respond
                }
                catch( IOException exc ) {
                    if( continueRunning ) exc.printStackTrace();
                    //else ignore
                }
            }
            else {//wait a bit, then check again
                try { Thread.sleep( 10 ); }
                catch( InterruptedException exc ) {}
            }
        }
    }


    //gets list of room names from the server
    public String[] getRoomNames() throws IOException
    {
        URL url = null;
        try {
            url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=roomnames" );
        }
        catch( MalformedURLException exc ) {
            throw new Error( "Bad SERVER_ADDR in GameStarterNetworker" );
        }

        BufferedReader in = new BufferedReader( new InputStreamReader(
                url.openStream() ) );
        ArrayList<String> rooms = new ArrayList<String>();
        String line;
        line = in.readLine();
        if( "s".equals( line ) == false ) {//server failure
            throw new IOException( "Server side error" );
        }
        while( (line=in.readLine()) != null ) {
            rooms.add( URLDecoder.decode( line, "UTF-8" ) );
        }
        in.close();
        return rooms.toArray( new String[ 0 ] );
    }

    //gets list of joinable games in room specified
    public GameInfo[] getGames( String roomName ) throws IOException
    {
        String roomNameEn = URLEncoder.encode( roomName, "UTF-8" );
        URL url = null;
        try {
            url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=gamenames&room=" + roomNameEn );
        }
        catch( MalformedURLException exc ) {
            throw new Error( "Bad SERVER_ADDR in GameStarterNetworker" );
        }

        BufferedReader in = new BufferedReader( new InputStreamReader(
                url.openStream() )
        );
        ArrayList<GameInfo> games = new ArrayList<GameInfo>();
        String line;

        line = in.readLine();

        if( "s".equals( line ) == false ) {//server failure
            throw new IOException( "Server-side error" );
        }
        while( (line=in.readLine()) != null ) {
            games.add( new GameInfo( URLDecoder.decode( line, "UTF-8" ), roomName ) );
        }
        in.close();
        return games.toArray( new GameInfo[ 0 ] );
    }


    public void pingGame( GameInfo game ) throws IOException
    {
        if( gameChosen || isGameStarting ) return;

        //first, update the server address
        URL url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=serveraddr&playerid=" + playerId +
                "&gameid=" + game.getId()
        );
        BufferedReader in = new BufferedReader( new InputStreamReader( url.openStream() ) );
        String firstLine = in.readLine();
        if( "s".equals( firstLine ) == false ) {
            in.close();
            throw new IOException( "Server-side error: \"" + firstLine + "\"" );
        }
        //else
        game.setServerAddr( new InetSocketAddress( in.readLine(),
                Integer.parseInt( in.readLine() ) )
        );
        boolean isThisLAN = "l".equals( in.readLine() );
        in.close();

        if( isThisLAN ) {
            game.setPing( GameInfo.PING_LAN );
        }
        else {
            //now queue the game so it can be pinged asynchronously
            synchronized( queue ) {
                try { queue.put( game ); }
                catch( InterruptedException exc ) { exc.printStackTrace(); }
            }
        }
    }


    //attempts to have this player join the game specified
    //'e' = error, 's' = success, 'l' = too late
    public char joinGame( GameInfo game )
    {
        if( gameChosen ) {
            System.out.println( "gsn error: joinGame called when a game is " +
                    "already chosen." );
            return 'e';//already joined a game
        }

        InputStream in = null;
        try {
            URL url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=join&gameid=" + game.getId() +
                    "&playerid=" + playerId );
            in = url.openStream();
            int value = in.read();
            in.close();

            if( 's' == value ) {
                chosenGame = game;
                gameChosen = true;
                updateStatus();
                return 's';
            }
            else return (char) value;
        }
        catch( IOException exc ) {
            exc.printStackTrace();
            try { in.close(); }
            catch( Exception exc2 ) {}
            return 'e';
        }
    }

    //attempts to create a new game with the properties specified
    public boolean createGame( String name, String description, int difficulty,
            int numPlayers, String roomName )
    {
        if( gameChosen ) return false;

        BufferedReader in = null;
        try {
            name = URLEncoder.encode( name, "UTF-8" );
            description = URLEncoder.encode( description, "UTF-8" );
            String roomNameEn = URLEncoder.encode( roomName, "UTF-8" );

            URL url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=create&playerid=" + playerId +
                    "&gamename=" + name + "&description=" + description +
                    "&difficulty=" + difficulty + "&players=" + numPlayers +
                    "&roomname=" + roomNameEn );
            in = new BufferedReader( new InputStreamReader( url.openStream() ) );

            String s = in.readLine();
            if( "s".equals( s ) == false ) {
                in.close();
                return false;
            }

            chosenGame = new GameInfo( in.readLine(), roomName );
            playersSoFar = 1;
            isGameStarting = false;
            isServer = true;
            isStartRequestPending = false;
            requestSender = null;
            isAlreadyRequest = false;
            isRequestFailed = false;
            wasRequestRejected = false;

            try { in.close(); }//this won't drop through or return false
            catch( Exception exc ) { exc.printStackTrace(); }

            //create the ping listener
            serverThread = new ServerThread();
            serverRunning = true;
            serverThread.start();

            gameChosen = true;
            return true;
        }
        catch( IOException exc ) {
            exc.printStackTrace();
            serverRunning = false;//thread checks every TIMEOUT ms
            try { in.close(); }
            catch( Exception exc2 ) {}
            return false;
        }
    }

    //disconnects this player from the currently joined game
    //'e' = network error; 'i' = ignore; 's' = success
    public char unjoinGame()
    {
        if( gameChosen == false ) return 'e';
        if( isGameStarting ) return 'i';

        try {
            URL url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=unjoin&playerid=" + playerId );
            InputStream in = url.openStream();
            int read = in.read();
            in.close();
            if( 'i' == read ) return 'i';

            //else
            gameChosen = false;
            chosenGame = null;
            //close ping server, if any
            serverRunning = false;
            if( serverSocket != null ) {
                serverSocket.close();
            }
            return (('s' == read)? 's': 'e');
        }
        catch( IOException exc ) {
            exc.printStackTrace();
            return 'e';
        }
    }


    //sends a request from this player to start the joined game
    public void requestStart()
    {
        //since there is no immediate return value, this can return immediately
        //thus it is asynchronous
        Thread thread = new Thread( new Runnable() {
                public void run()
                {
                    InputStream in = null;
                    try {
                        URL url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=requeststart" +
                                "&playerid=" + playerId
                        );
                        in = url.openStream();
                        int read = in.read();
                        if( read == 'w' ) return;//the game is starting or there is an error, ignore
                        else if( read == 'o' ) isOnlyPlayer = true;
                        else if( read == 'a' ) isAlreadyRequest = true;
                        else if( read != 's' ) throw new IOException( "Server side error" );
                        else isAlreadyRequest = false;
                    }
                    catch( IOException exc ) {
                        exc.printStackTrace();
                        isRequestFailed = true;
                    }
                }
            }
        );
        thread.start();
    }


    private int numSequentialExceptions = 0;
    /** Retrieves information about the currently selected game.
    * Return values: 's' indicates success; 'd' indicates that the server
    * left, disbanding the game; 'e' indicates an unignorable error.
    **/
    public synchronized char updateStatus()
    {
        if( gameChosen == false ) return 'e';

        BufferedReader in = null;
        try {
            URL url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=info&playerid=" + playerId );
            in = new BufferedReader( new InputStreamReader( url.openStream() ) );
            String status = in.readLine();
            //else
            if( "d".equals( status ) ) {
                //the server left, disbanding the game
                return 'd';
            }
            if( "s".equals( status ) == false ) {
                throw new IOException( "Server side error" );
            }
            //read data
            this.isGameStarting = "t".equals( in.readLine() );
            isServer = "t".equals( in.readLine() );
            isStartRequestPending = "t".equals( in.readLine() );
            wasRequestRejected = "t".equals( in.readLine() );
            playersSoFar = Integer.parseInt( in.readLine() );
            requestSender = in.readLine();
            if( "".equals( requestSender ) ) requestSender = null;
            if( requestSender != null ) {
                requestSender = URLDecoder.decode( requestSender, "UTF-8" );
            }
            if( this.isGameStarting ) {//get the server's ip and port
                System.out.println( "gsn: game is starting" );
                chosenGame.setServerAddr( new InetSocketAddress(
                        in.readLine(), Integer.parseInt( in.readLine() ) )
                );
                thisInetAddr = InetAddress.getByName( in.readLine() );
                //read the players' names
                playerNames = new String[ playersSoFar ];
                for( int i = 0; i < playerNames.length; i++ ) {
                    playerNames[ i ] = URLDecoder.decode( in.readLine(), "UTF-8" );
                }
                //read the playerId for this game
                playerIdInGame = Byte.parseByte( in.readLine() );

                if( isServer ) {//read the players' game passwords
                    playerGamePasswords = new int[ playersSoFar ];
                    for( int i = 0; i < playerGamePasswords.length; i++ ) {
                        playerGamePasswords[ i ] = Integer.parseInt( in.readLine() );
                    }
                }
                //read this player's game password
                thisGamePassword = Integer.parseInt( in.readLine() );

                //now close this class; the ping networking is no longer needed
                /* close() calls unjoinGame(), but the call will be ignored here
                        since isGameStarting is true
                */
                close();
            }
            //clean up
            in.close();
            numSequentialExceptions = 0;
            return 's';
        }
        catch( IOException exc ) {
            numSequentialExceptions++;
            try { in.close(); }
            catch( Exception exc2 ) {}
            if( numSequentialExceptions >= 3 ) {
                //tell the caller to give up
                return 'e';
            }
            else {
                //ignore for now
                exc.printStackTrace();
                return 's';
            }
        }
    }


    //returns the currently chosen and joined game
    public GameInfo getChosenGame() { return chosenGame; }

    //returns whether or not a game has been chosen and joined
    public boolean isGameChosen() { return gameChosen; }

    //returns whether or not a start request has failed because this is the only player
    public synchronized boolean isOnlyPlayer()
    {
        boolean old = isOnlyPlayer;
        isOnlyPlayer = false;
        return old;
    }

    //returns whether or not this player's start request had an error
    public synchronized boolean isRequestFailed()
    {
        boolean old = isRequestFailed;
        isRequestFailed = false;
        return old;
    }

    //returns whether or not this player's start request was rejected
    //because another start request is already being processed
    public synchronized boolean isAlreadyRequest()
    {
        boolean old = isAlreadyRequest;
        isAlreadyRequest = false;
        return old;
    }

    //returns whether or not this player must answer a start request
    public synchronized boolean isStartRequestPending() { return isStartRequestPending; }

    //returns the name of whoever sent the start request, if anyone
    public synchronized String getRequestSender() { return requestSender; }

    //returns whether or not this player's request was rejected by the group
    public synchronized boolean wasRequestRejected()
    {
        boolean old = wasRequestRejected;
        wasRequestRejected = false;
        return old;
    }

    //sends an accept or deny message to the server for a pending start request
    public synchronized void acceptStartRequest( boolean accepted ) throws IOException
    {
        if( isStartRequestPending == false ) return;
        //else
        isStartRequestPending = false;
        URL url = new URL( Lasertag3DSettings.SERVER_ADDR + "?action=acceptrequest&value=" +
                accepted + "&playerid=" + playerId );
        InputStream in = url.openStream();
        int read = in.read();
        if( read == 'n' ) {
            isStartRequestPending = false;
            return;//game is starting or request already cleared
        }
        else if( read != 's' ) throw new IOException( "Server side error" );
        in.close();
    }

    //returns whether or not the joined game is starting
    //un-synchronized to allow threads to check without being blocked
    public boolean isGameStarting() { return isGameStarting; }

    //returns the id that should be passed to NetworkClient
    public synchronized byte getPlayerIdInGame() { return playerIdInGame; }

    //returns null if the game is not starting
    public synchronized String[] getPlayerNames() { return playerNames; }

    //returns whether or not this player will be the server for the starting game
    public synchronized boolean isServer() { return isServer; }

    //returns null if the game is not starting or this is not the server
    public synchronized int[] getPlayerGamePasswords() { return playerGamePasswords; }

    public synchronized int getThisGamePassword() { return thisGamePassword; }

    public synchronized InetAddress getThisInetAddr() { return thisInetAddr; }

    //returns the number of players who are currently joined to the same
    //game this player is joined to
    public synchronized int getPlayersSoFar() { return playersSoFar; }

    public void close()
    {
        //close ping client
        continueRunning = false;
        socket.close();

        //close ping server, if any
        serverRunning = false;
        if( serverSocket != null ) {
            serverSocket.close();
        }

        unjoinGame();
    }

    public void finalize() { close(); }


    private class ServerThread extends Thread {
        public void run()
        {
            byte buffer[] = new byte[ 1 ];
            byte out[] = new byte[ 1 ];
            out[ 0 ] = (byte) 'a';
            try {
                serverPacket = new DatagramPacket( buffer, buffer.length );
                serverSocket = new DatagramSocket( null );
                serverSocket.setReuseAddress( true );
                serverSocket.bind(
                        new InetSocketAddress( Lasertag3DSettings.serverPort )
                );
                serverSocket.setSoTimeout( TIMEOUT );
            }
            catch( IOException exc ) {
                exc.printStackTrace();
            }
            while( serverRunning ) {
                try {
                    serverSocket.receive( serverPacket );
                    //set the data to 'a' and send it back
                    serverPacket.setData( out );
                    serverSocket.send( serverPacket );
                }
                catch( SocketTimeoutException exc ) {}//ignore
                catch( IOException exc ) {
                    if( serverRunning ) exc.printStackTrace();
                    //else ignore
                }
            }
        }
    }
}
