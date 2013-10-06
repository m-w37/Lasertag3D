//GameInfo.java (8/17/2010)
//utility class with GameStarterNetworker to hold game info
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.net.start;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Hashtable;


public class GameInfo {

    public final static int PING_UNDETERMINED = -1;
    public final static int PING_ERROR = -2;
    public final static int PING_LAN = -3;//server is on this LAN

    public static Hashtable<Long, Integer> pingTable =
            new Hashtable<Long, Integer>();//stores pings, indexed by id

    String name, description;
    long gameId;
    InetSocketAddress serverAddr;
    int difficulty;
    int ping = PING_UNDETERMINED;//in ms
    int requestedPlayers, totalPlayers;
    String playerNames[];
    String roomName, roomFileName;

    GameInfo( String serverData, String roomName )
            throws IOException
    {
        try {
            String splitData[] = serverData.split( "\\t+" );
            name = splitData[ 0 ];
            description = splitData[ 1 ];
            gameId = Long.parseLong( splitData[ 2 ] );
            serverAddr = new InetSocketAddress( splitData[ 3 ],
                    Integer.parseInt( splitData[ 4 ] )
            );
            difficulty = Integer.parseInt( splitData[ 5 ] );
            requestedPlayers = Integer.parseInt( splitData[ 6 ] );
            totalPlayers = Integer.parseInt( splitData[ 7 ] );
            playerNames = new String[ splitData.length - 8 ];
            for( int i = 8; i < splitData.length; i++ ) {
                playerNames[ i - 8 ] = splitData[ i ];
            }
            this.roomName = roomName;
            this.roomFileName = roomName + ".mwg";
            //get the ping from the last iteration
            Integer pingObj = pingTable.get( new Long( gameId ) );
            if( pingObj != null ) {
                this.ping = pingObj.intValue();
            }
        }
        catch( Exception exc ) {
            exc.printStackTrace();
            throw new IOException( "Bad server data" );
        }
    }

    //this will NOT be reset when the game list is refreshed
    public void setPing( int ping )
    {
        this.ping = ping;
        pingTable.put( new Long( gameId ), new Integer( ping ) );
    }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public long getId() { return gameId; }

    // This may change by the time the game starts.
    public InetSocketAddress getServerAddr()
    {
        return serverAddr;
    }

    //NOTE: this will be reset when the game list is refreshed
    public void setServerAddr( InetSocketAddress serverAddr )
    {
        this.serverAddr = serverAddr;
    }

    public int getDifficulty() { return difficulty; }

    public int getPing() { return ping; }

    public int getRequestedPlayers() { return requestedPlayers; }

    public int getTotalPlayers() { return totalPlayers; }

    public String[] getPlayerNames() { return playerNames; }

    public String getRoomName() { return roomName; }

    public String getRoomFileName() { return roomFileName; }    

    public String toString()
    {
        String pingString;
        switch( ping ) {
            case PING_UNDETERMINED: pingString = "unknown"; break;
            case PING_ERROR: pingString = "error"; break;
            case PING_LAN: pingString = "very small"; break;
            default: pingString = Integer.toString( ping );
        }
        String playerNamesString = "";
        for( int i = 0; i < playerNames.length; i++ ) {
            playerNamesString += playerNames[ i ];
            if( i != (playerNames.length - 1) ) playerNamesString += ", ";
        }
        return "Difficulty: " + difficulty + "; Ping: " + pingString +
                "; Players: " + totalPlayers + " out of " +
                requestedPlayers + "; Game name/description: " +
                name + "/" + description + ";  Game members: " +
                playerNamesString + "; Room: " + roomName;
    }

    //the indices match with the indices in COLUMN_NAMES
    public Object[] toObjectArray()
    {
        Object array[] = new Object[ COLUMN_NAMES.length ];
        array[ 0 ] = new Integer( difficulty );
        //get ping string
        String pingString;
        switch( ping ) {
            case PING_UNDETERMINED: pingString = "unknown"; break;
            case PING_ERROR: pingString = "error"; break;
            case PING_LAN: pingString = "very small"; break;
            default: pingString = Integer.toString( ping );
        }
        array[ 1 ] = pingString;
        array[ 2 ] = totalPlayers + " of " + requestedPlayers;
        array[ 3 ] = name;
        array[ 4 ] = description;
        //player names string
        String playerNamesString = "";
        for( int i = 0; i < playerNames.length; i++ ) {
            playerNamesString += playerNames[ i ];
            if( i != (playerNames.length - 1) ) playerNamesString += ", ";
        }
        array[ 5 ] = playerNamesString;
        return array;
    }

    public boolean equals( Object obj )
    {
        if( obj instanceof GameInfo ) {
            GameInfo gi = (GameInfo) obj;
            return (gi.getId() == gameId);
        }
        return false;
    }


//static var and method used to format games for use in a JTable

    public final static String[] COLUMN_NAMES = { "Difficulty",
            "Ping", "Player Count", "Name", "Description", "Members"
    };//room is not included because it is not needed here

    /** Converts an array of games into a 2d array of strings where
    * each row is a game and each cell is one piece of data about that game.
    **/
    public static Object[][] to2DArray( GameInfo[] games )
    {
        Object objects[][] = new Object[ games.length ][];
        for( int i = 0; i < games.length; i++ ) {
            objects[ i ] = games[ i ].toObjectArray();
        }
        return objects;
    }
}
