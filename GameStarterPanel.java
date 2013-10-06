//GameStarterPanel.java (8/17/2010)
//presents a gui used to choose a game to join or start
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d;


import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;
import javax.swing.border.LineBorder;

import java.util.*;

import com.mattweidner.lt3d.net.start.*;
import com.mattweidner.lt3d.net.*;
import com.mattweidner.lt3d.scene.*;


public class GameStarterPanel extends JPanel implements ListSelectionListener,
        ActionListener, Runnable {

    private Lasertag3D main;

    private GameStarterNetworker networker;//networking interface

    private NetworkListener listener;//listens for when the game starts

    private CardLayout cardLayout;//main layout manager

    private JPanel choosePanel, waitPanel, createPanel;//the different GUIs

    private boolean continueRunning;

    //choosePanel GUI and variables
    private String roomNames[];//the names of the rooms that games are grouped in
    private JTabbedPane tabbedPane;//holds the lists, each representing one room
    private JTable tables[];//the lists of games to join, drawn as tables
    private JButton roomInfo, gameInfo, ping, join, create;/* used to join games,
            make new ones, or get info on existing games/the room.
    */
    private int selectedIndex = -1;//the index of the selected game in its list
    private GameInfo selectedGame;//the currently selected game
    private GameInfo games[][];//all of the games, grouped by room

    //waitPanel GUI
    private JLabel status;//displays the status of a joined game
    private JButton requestStart, cancel;//buttons used with joined games

    //createPanel GUI
    private JTextField name, description;
    private JSpinner difficulty, numPlayers;
    private JButton createOkay, createCancel;


    public GameStarterPanel( Lasertag3D main )
    {
        this.main = main;

        //make game chooser gui
        choosePanel = new JPanel();
        choosePanel.setLayout( new BorderLayout() );

        tabbedPane = new JTabbedPane();
        choosePanel.add( tabbedPane, BorderLayout.CENTER );

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout( new GridLayout( 0, 3 ) );
        roomInfo = new JButton( "Room Info..." );
        roomInfo.addActionListener( this );
        buttonPanel.add( roomInfo );
        gameInfo = new JButton( "Game Info..." );
        gameInfo.addActionListener( this );
        buttonPanel.add( gameInfo );
        ping = new JButton( "Ping Game's Server" );
        ping.addActionListener( this );
        buttonPanel.add( ping );
        join = new JButton( "Join Game" );
        join.addActionListener( this );
        buttonPanel.add( join );
        create = new JButton( "Create Game" );
        create.addActionListener( this );
        buttonPanel.add( create );
        choosePanel.add( buttonPanel, BorderLayout.SOUTH );

        //make gui for waiting on game
        waitPanel = new JPanel();
        waitPanel.setLayout( new BorderLayout() );

        status = new JLabel( "Waiting for other players, 0 so far" );
        waitPanel.add( status, BorderLayout.NORTH );
        buttonPanel = new JPanel();
        buttonPanel.setLayout( new FlowLayout() );
        requestStart = new JButton( "Request Start" );
        requestStart.addActionListener( this );
        buttonPanel.add( requestStart );
        cancel = new JButton( "Cancel" );
        cancel.addActionListener( this );
        buttonPanel.add( cancel );
        waitPanel.add( buttonPanel, BorderLayout.SOUTH );

        //make gui for creating a game
        createPanel = new JPanel();
        createPanel.setLayout( new GridLayout( 0, 2 ) );

        name = new JTextField( "", 20 );
        createPanel.add( new JLabel( "Name" ) );
        createPanel.add( name );
        description = new JTextField( "", 20 );
        createPanel.add( new JLabel( "Description" ) );
        createPanel.add( description );
        difficulty = new JSpinner( new SpinnerNumberModel(
                1, 1, 5, 1 ) );
        createPanel.add( new JLabel( "Skill level (1 is lowest)" ) );
        createPanel.add( difficulty );
        numPlayers = new JSpinner( new SpinnerNumberModel(
                2, 2, GameStarterNetworker.MAX_PLAYERS, 1 ) );
        createPanel.add( new JLabel( "Number of players (2-10)" ) );
        createPanel.add( numPlayers );
        createOkay = new JButton( "Okay" );
        createOkay.addActionListener( this );
        createPanel.add( createOkay );
        createCancel = new JButton( "Cancel" );
        createCancel.addActionListener( this );
        createPanel.add( createCancel );

        //make main gui
        cardLayout = new CardLayout();
        this.setLayout( cardLayout );
        this.add( choosePanel, "choosePanel" );
        this.add( waitPanel, "waitPanel" );
        this.add( createPanel, "createPanel" );
    }


    //start the panel
    public void start( NetworkListener listener, String playerName, Frame frame )
            throws IOException, IllegalArgumentException
    {
        if( continueRunning ) return;//already running

        networker = new GameStarterNetworker( playerName, listener );

        this.listener = listener;

        //set vars in the GUI
        create.setEnabled( true );
        gameInfo.setEnabled( false );//user can't press it until a game is selected
        ping.setEnabled( false );//user can't press it until a game is selected
        join.setEnabled( false );//user can't press it until a game is selected
        status.setText( "Waiting for other players, 0 so far" );
        
        //make the tabbed pane
        roomNames = networker.getRoomNames();
        tables = new JTable[ roomNames.length ];
        games = new GameInfo[ roomNames.length ][];
        //make listener which joins a game that is double-clicked
        MouseListener doubleClickListener = new MouseAdapter() {
            public void mouseClicked( MouseEvent e ) {
                if( e.getClickCount() == 2 ) {
                    synchronized( GameStarterPanel.this ) {
                        join();
                    }
                }
            }
        };
        for( int i = 0; i < roomNames.length; i++ ) {
            games[ i ] = networker.getGames( roomNames[ i ] );
            TableModel model = new DefaultTableModel(
                    GameInfo.to2DArray( games[ i ] ), GameInfo.COLUMN_NAMES
            ) {
                public boolean isCellEditable( int row, int column )
                {
                    return false;
                }
            };//uneditable
            tables[ i ] = new JTable( model );
            tables[ i ].setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
            tables[ i ].setRowSelectionAllowed( true );
            tables[ i ].setColumnSelectionAllowed( false );
            //cells cannot be individually selected due to the previous 2 rows
            addBoldHeaderRenderer( tables[ i ] );
            tables[ i ].getSelectionModel().addListSelectionListener( this );
            tables[ i ].addMouseListener( doubleClickListener );
            JScrollPane scrollPane = new JScrollPane( tables[ i ] );
            tabbedPane.addTab( roomNames[ i ], scrollPane );
        }
        
        //show choosePanel first
        cardLayout.show( this, "choosePanel" );

        Thread updateThread = new Thread( this );
        continueRunning = true;
        updateThread.start();
    }

    public void stop()
    {
        continueRunning = false;

        synchronized( this ) {//wait for the loop to end
            if( networker != null ) networker.close();
            networker = null;
            listener = null;
            tabbedPane.removeAll();//remove all tabs
            tables = null;
            games = null;
            selectedIndex = -1;
            selectedGame = null;
        }
    }


    public boolean returnToMenu( boolean ask )
    {
        if( networker != null && networker.isGameStarting() ) return false;

        if( ask && (networker != null ) && networker.isGameChosen() ) {
            int result = JOptionPane.showConfirmDialog( this,
                    "Are you sure you want to return to the main menu?\n" +
                    "You will unjoin your game if you do.",
                    "Confirm Unjoin Game",
                    JOptionPane.YES_NO_OPTION
            );
            if( result != JOptionPane.YES_OPTION ) return false;
        }
        stop();
        return true;
    }


    public synchronized void valueChanged( ListSelectionEvent e )
    {
        if( e.getValueIsAdjusting() == false ) {
            int selectedTab = tabbedPane.getSelectedIndex();
            JTable selectedTable = tables[ selectedTab ];
            selectedIndex = selectedTable.getSelectionModel().getLeadSelectionIndex();
            if( selectedIndex == -1 || games[ selectedTab ].length == 0 ) {
                //nothing is selected (or an error message is selected)
                gameInfo.setEnabled( false );
                ping.setEnabled( false );
                join.setEnabled( false );
                create.setEnabled( true );
                return;
            }
            //else
            selectedGame = games[ selectedTab ][ selectedIndex ];
            gameInfo.setEnabled( true );
            ping.setEnabled( true );
            join.setEnabled( true );
            create.setEnabled( true );
        }
    }


    public void actionPerformed( ActionEvent e )
    {
        //process this first
        if( e.getSource() == cancel ) {//unjoin current game
            char returnValue = networker.unjoinGame();
            //show an appropriate message
            switch( returnValue ) {
                case 'e': JOptionPane.showMessageDialog( this,
                            "Network error; the game could not be unjoined.",
                            "Unjoin Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    break;
                case 'i': break;
                case 's': returnToGameChooser();
                    break;
            }
            return;
        }

        //else
        synchronized( this ) {
            //choosePanel buttons
            if( e.getSource() == roomInfo ) {
                String fileName = Lasertag3D.getRoomFileName(
                        roomNames[ tabbedPane.getSelectedIndex() ]
                );
                //get description
                try {
                    URL url = Lasertag3D.class.getResource(
                            Lasertag3D.ROOMS_DIR_URL + fileName
                    );
                    String info[] = GameSceneLoader.getInfo( url );
                    JOptionPane.showMessageDialog( this, info[ 0 ] + ": " + info[ 1 ],
                            "Room Info", JOptionPane.INFORMATION_MESSAGE
                    );
                }
                catch( Exception exc ) {
                    System.err.println( "Error: while fetching scene description for " +
                            fileName + ":"
                    );
                    exc.printStackTrace();
                    JOptionPane.showMessageDialog( this,
                            "There was an error while looking up this room's scene.",
                            "Room Info Error", JOptionPane.ERROR_MESSAGE
                    );
                }
            }
            
            else if( e.getSource() == gameInfo ) {
                JOptionPane.showMessageDialog( this, selectedGame, "Game Info",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }

            else if( e.getSource() == ping ) {
                try {
                    networker.pingGame( selectedGame );//asynchronous
                }
                catch( IOException exc ) {
                    System.err.println( "Error: pingGame: " );
                    exc.printStackTrace();
                    JOptionPane.showMessageDialog( this,
                            "Error: could not retrieve server address for game " +
                            selectedGame.getName() + ".", "Ping Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

            else if( e.getSource() == join ) {
                join();
            }

            else if( e.getSource() == create ) {
                if( Lasertag3DSettings.gameHostingEnabled ) {
                    cardLayout.show( this, "createPanel" );
                }
                else {
                    JOptionPane.showMessageDialog( this,
                            "You must enable game hosting to create games\n" +
                            "(Personalization -> Game Hosting)",
                            "Game Hosting Disabled",
                            JOptionPane.ERROR_MESSAGE
                    );
                }
            }

            //waitPanel buttons
            else if( e.getSource() == requestStart ) {//start requested
                networker.requestStart();
            }

            //createPanel buttons
            else if( e.getSource() == createOkay ) {//process the create data
                if( create() ) {
                    //switch to waitPanel
                    status.setText( "Waiting for other players, " +
                            (networker.getPlayersSoFar()-1) + " so far." );
                    cardLayout.show( this, "waitPanel" );
                }
                else {//create failed; act as if cancel was hit
                    cardLayout.show( this, "choosePanel" );
                    synchronized( this ) { refreshList( true ); }
                    //refreshList will enable buttons correctly
                }
            }

            else if( e.getSource() == createCancel ) {//dismiss the createPanel
                cardLayout.show( this, "choosePanel" );
                synchronized( this ) { refreshList( true ); }
                //refreshList will enable buttons correctly
            }
        }
    }

    public boolean create()
    {
        if( "".equals( name.getText() ) || "".equals( description.getText() ) ) {
            //required field not filled out, show error
            JOptionPane.showMessageDialog( this,
                    "Could not create game: Name and Description " +
                    "must have values.", "Create Error",
                    JOptionPane.ERROR_MESSAGE
            );
            return false;
        }
        else {//create game
            if( networker.createGame( name.getText(),
                    description.getText(),
                    ((Integer) difficulty.getValue()).intValue(),
                    ((Integer) numPlayers.getValue()).intValue(),
                    roomNames[ tabbedPane.getSelectedIndex() ]
            ) ) {
                selectedGame = networker.getChosenGame();
                cardLayout.show( this, "waitPanel" );
                return true;
            }
            else {//error
                JOptionPane.showMessageDialog( this, "Could not create game.",
                        "Create Error", JOptionPane.ERROR_MESSAGE
                );
                return false;
            }
        }
    }

    public void join()
    {
        if( selectedGame == null ) return;

        create.setEnabled( false );
        join.setEnabled( false );
        boolean gameInfoWasEnabled = gameInfo.isEnabled();
        gameInfo.setEnabled( false );
        ping.setEnabled( false );
        char returnValue = networker.joinGame( selectedGame );
        switch( returnValue ) {
            case 's': //switch to waitPanel
                status.setText( "Waiting for other players, " +
                        (networker.getPlayersSoFar()-1) + " so far." );
                cardLayout.show( this, "waitPanel" );
                return;//break out of this method
            case 'l': //too late
                JOptionPane.showMessageDialog( this,
                        "Could not join game " + selectedGame.getName() + ".\n" +
                        "It is starting already.",
                        "Join Results", JOptionPane.INFORMATION_MESSAGE
                );
                break;//drop through to end of switch block
            case 'e': default:
                JOptionPane.showMessageDialog( this,
                        "Network Error; could not join game " +
                        selectedGame.getName() + ".\n",
                        "Join Error", JOptionPane.ERROR_MESSAGE
                );
                break;//drop through to end of switch block
        }
        //this is executed unless join was successful
        join.setEnabled( true );
        create.setEnabled( true );
        gameInfo.setEnabled( gameInfoWasEnabled );
        ping.setEnabled( gameInfoWasEnabled );//gameInfo and ping enable together
    }

    /** This assumes that the current game has already been unjoined.
    **/
    private void returnToGameChooser()
    {
        cardLayout.show( this, "choosePanel" );
        refreshList( true );
        //refreshList (via valueChanged) will enable buttons correctly
    }


    public void run()
    {
        while( continueRunning ) {
            if( networker.isGameChosen() ) {
                synchronized( this ) {
                    char updateStatusFlag = networker.updateStatus();
                    if( updateStatusFlag != 's' ) {
                        //unable to update status; unjoin the game
                        networker.unjoinGame();//ignore error messages
                        if( updateStatusFlag == 'd' ) {
                            JOptionPane.showMessageDialog( this,
                                    "The game host has left, disbanding the game.",
                                    "Game Disbanded",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                        else {
                            if( updateStatusFlag != 'e' ) {
                                System.out.println( "gsp: unrecognized gsn." +
                                        "updateStatus() return flag: " +
                                        updateStatusFlag );
                            }
                            JOptionPane.showMessageDialog( this,
                                    "Server-side error; you have been unjoined from your game.",
                                    "Server-Side Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                        returnToGameChooser();
                    }
                    if( networker.isGameStarting() ) {//start the game
                        System.out.println( "gsp: game is starting" );
                        //first, get the room's file name
                        selectedGame = networker.getChosenGame();
                        String roomFileName = Lasertag3D.getRoomFileName(
                                selectedGame.getRoomName()
                        );
                        listener.startGame( selectedGame.getServerAddr(),
                                networker.getPlayerIdInGame(),
                                networker.getThisInetAddr(),
                                networker.getPlayerNames(), networker.isServer(),
                                networker.getPlayerGamePasswords(),
                                networker.getThisGamePassword(),
                                roomFileName
                        );
                        stop();//stop this thread and reset all variables
                    }
                    else if( networker.isStartRequestPending() ) {
                        try {
                            if( JOptionPane.showConfirmDialog( this,
                                    networker.getRequestSender() + " has requested a " +
                                    "game start.  Do you accept?",
                                    "Start Request", JOptionPane.YES_NO_OPTION ) ==
                                    JOptionPane.YES_OPTION
                            ) {//player accepted start request
                                networker.acceptStartRequest( true );
                            }
                            else networker.acceptStartRequest( false );
                        }
                        catch( IOException exc ) {
                            exc.printStackTrace();
                            JOptionPane.showMessageDialog( this,
                                    "Network error; your response could not be processed.",
                                    "Start Request Response Error",
                                    JOptionPane.ERROR_MESSAGE
                            );
                        }
                    }
                    else if( networker.isOnlyPlayer() ) {
                        JOptionPane.showMessageDialog( this,
                                "Your request was rejected because you are the " +
                                "only player.",
                                "Start Request Results",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                    else if( networker.isAlreadyRequest() ) {
                        JOptionPane.showMessageDialog( this,
                                "There is already a request being processed.",
                                "Start Request Results",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                    else if( networker.isRequestFailed() ) {
                        JOptionPane.showMessageDialog( this,
                                "Your request could not be processed.",
                                "Start Request Results",
                                JOptionPane.ERROR_MESSAGE
                        );
                    }
                    else if( networker.wasRequestRejected() ) {
                        JOptionPane.showMessageDialog( this,
                                "Your start request was rejected " +
                                "by the other players.",
                                "Start Request Results",
                                JOptionPane.INFORMATION_MESSAGE
                        );
                    }
                    if( status != null && networker != null ) {
                        status.setText( "Waiting for other players, " +
                                (networker.getPlayersSoFar()-1) + " so far."
                        );
                    }
                }
            }

            else {//still trying to join a game
                //synchronization is enforced within the method
                refreshList();
            }

            try { Thread.sleep( Lasertag3DSettings.panelUpdateDelay ); }
            catch( InterruptedException exc ) {}
        }
    }

    private void refreshList() { refreshList( false ); }

//DEBUG  private boolean hasAlerted = false;

    private void refreshList( boolean isFromEventQueue )
    {
        if( choosePanel.isVisible() == false ) return;//nothing to do

        //run this synchronously with the event thread
        Runnable refresh = new Runnable() {
            public void run()
            {
                synchronized( this ) {
                    int i = tabbedPane.getSelectedIndex();
                    JTable selectedTable = tables[ i ];
                    boolean foundOldGame = false;
                    int columnWidths[] = new int[ GameInfo.COLUMN_NAMES.length ];
                    try {
                        games[ i ] = networker.getGames( roomNames[ i ] );
//DEBUG                 if( hasAlerted == false && games[ i ].length != 0 ) {
//DEBUG                     hasAlerted = true;
//DEBUG                     JOptionPane.showMessageDialog( null, "Starting",
//DEBUG                             "Lasertag3D", JOptionPane.INFORMATION_MESSAGE
//DEBUG                     );
//DEBUG                 }
                        //save the old column widths, if they aren't error rows
                        boolean setColumnWidths = false;
                        if( columnWidths.length == tables[ i ].getColumnCount() ) {
                            setColumnWidths = true;
                            TableColumnModel columnModel = tables[ i ].getColumnModel();
                            for( int j = 0; j < columnWidths.length; j++ ) {
                                columnWidths[ j ] = columnModel.getColumn( j ).getPreferredWidth();
                            }
                        }
                        //set the list data
                        DefaultTableModel model = (DefaultTableModel) tables[ i ].getModel();
                        model.setDataVector( GameInfo.to2DArray( games[ i ] ), GameInfo.COLUMN_NAMES );
                        //find the old selectedGame
                        if( selectedGame != null ) {
                            for( int j = 0; j < games[ i ].length; j++ ) {
                                if( games[ i ][ j ].equals( selectedGame ) ) {
                                    tables[ i ].setRowSelectionInterval( j, j );
                                    tables[ i ].getSelectionModel().setLeadSelectionIndex( j );
                                    selectedIndex = j;
                                    foundOldGame = true;
                                }
                            }
                        }
                        //restore the old column widths, if they aren't error rows
                        if( setColumnWidths ) {
                            TableColumnModel columnModel = tables[ i ].getColumnModel();
                            for( int j = 0; j < columnWidths.length; j++ ) {
                                columnModel.getColumn( j ).setPreferredWidth( columnWidths[ j ] );
                            }
                        }
                        addBoldHeaderRenderer( tables[ i ] );
                    }

                    catch( IOException exc ) {
                        exc.printStackTrace();
                        Object[][] noData = new Object[ 0 ][];
                        String message[] = new String[ 1 ];
                        message[ 0 ] = "Network error; Could not retrieve games from server.";
                        DefaultTableModel model = (DefaultTableModel) tables[ i ].getModel();
                        model.setDataVector( noData, message );
                        //drop through
                    }

                    if( foundOldGame == false ) {//the selected game was removed
                        selectedGame = null;
                        gameInfo.setEnabled( false );
                        join.setEnabled( false );
                        ping.setEnabled( false );
                        tables[ i ].clearSelection();
                    }
                }

            }
        };//end Runnable

        if( isFromEventQueue ) {//run it normally
            //if invokeAndWait is used within the event queue, an error occurs
            refresh.run();
        }
        else {
            //sync it with the event queue
            try {
                SwingUtilities.invokeAndWait( refresh );
            }
            catch( Exception exc ) {
                System.err.println( "Error: while refreshing list: " );
                exc.printStackTrace();
            }
        }
    }


    //utility method used to make the headers be bold
    private void addBoldHeaderRenderer( JTable table )
    {
        TableColumnModel columnModel = table.getColumnModel();
        for( int j = 0; j < table.getColumnCount(); j++ ) {
            TableColumn column = columnModel.getColumn( j );
            column.setHeaderRenderer( new TableCellRenderer() {
                    /* thanks to http://www.velocityreviews.com/forums/
                            t128419-changing-fonts-in-a-jtable-header.html
                    */
                    public Component getTableCellRendererComponent( JTable table, Object value,
                            boolean isSelected, boolean hasFocus, int row, int column
                    ) {
                        String text = value.toString();
                        JLabel label = new JLabel( "<html><body><b>" + text +
                                "</b></body></html>"
                        );
                        label.setBorder( new LineBorder( Color.BLACK, 1 ) );
                        return label;
                    }
                }
            );
        }
    }
}
