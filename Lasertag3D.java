//Lasertag3D.java (6/5/2011)
//Lasertag3D main program
//by Matthew Weidner (www.mattweidner.com)


/** The following application-specific lines should be contained in any scenes
* used with this game:
* 1. Gravity information (is gravityOn): 'g' t/f
* 2. Player start positions: 's' id x y z m00 m01 m02 m10 m11 m12 m20 m21 m22
*   where id is the playerId, (x,y,z) is the start point, and the m
*   values indicate the rotation matrix
**/

package com.mattweidner.lt3d;


import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;

import javax.swing.text.html.parser.ParserDelegator;//workaround

import java.io.*;
import javax.swing.filechooser.FileSystemView;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

import java.lang.reflect.*;

import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.loaders.objectfile.ObjectFile;
import com.sun.j3d.utils.pickfast.behaviors.*;
import com.sun.j3d.utils.pickfast.*;
import com.sun.j3d.utils.universe.*;

import com.mattweidner.lt3d.scene.*;
import com.mattweidner.lt3d.net.*;
import com.mattweidner.lt3d.sprite.*;


public class Lasertag3D implements NetworkListener, ActionListener {

    public final static double centerToGround = 0.4;
    public final static double radius = 0.3;
    public final static Point3f laserPosition = new Point3f( 0.0f, 0.0f, -0.11f );
    public final static Point3d lowerBound = new Point3d( -0.3, -0.2, -0.2 );
    public final static Point3d upperBound = new Point3d( 0.3, 0.2, 0.1 );
    public final static Color3f dotColor = new Color3f( 1.0f, 0.0f, 0.0f );
    public final static float DOT_SIZE = 0.05f;
    public final static double BACK_CLIP_DISTANCE = 40.0;
    public final static double FRONT_CLIP_DISTANCE = 0.02;
    public final static int AUTOREPEAT_DELAY = 1;//ms

    //main directories; set by command line
    public static String WRITABLE_DIR;//as file

    //file/URL names, relative to the main directories
    public static String SETTINGS_FILE = "settings.txt";
    public static String LOG_FILE = "log.txt";

    public static String DEFAULT_SETTINGS_URL = "/default_settings.txt";
    public static String SPRITE_OBJ_URL = "/data/sprite.obj";
    public static String BG_IMAGE_URL = "/data/background.png";
    public static String BUTTON_IMAGE_URL = "/data/button.png";
    public static String SOUND_ON_URL = "/data/sound_on.png";
    public static String SOUND_OFF_URL = "/data/sound_off.png";
    public static String ABOUT_URL = "/data/about.html";
    public static String HELP_URL = "/data/help.html";
    public static String ROOMS_INDEX_URL = "/rooms_index.txt";
    public static String ROOMS_DIR_URL = "/rooms/";
    public static String GOOD_SOUND_URL = "/data/good.wav";
    public static String BAD_SOUND_URL = "/data/bad.wav";


    //variables
    private static LogPrintStream logger;

    //gui vars
    private JFrame frame;
    private JPanel mainPanel;
    private JPanel gamePanel, roomChooser, infoPanel;
    private MenuPanel menuPanel;
    private SettingsPanel settingsPanel;
    private GameStarterPanel gameStarterPanel;
    private boolean inGameStarter;
    private JButton returnToMenu;
    private MuteButton playSoundButton;
    private ImageIcon soundOn, soundOff;
    private final static String MENU_ITEMS[] = { "Explore!",
            "Lasers (+People)!", "Instructions!", "Personalization!", "Meta!"
    };
    private CardLayout cardLayout;

    //game gui
    private JTextArea messageArea, scoreArea;
    private String currentScoreText;
    private JButton where;
    private JScrollPane messagePane, scorePane;
    private Canvas3D canvas;
    private JSplitPane hSplitPane /*outer*/, vSplitPane /*inner, on the left*/;
    //room names
    private String roomNames[];//roomFileName = lower(roomName) + ".mwg"
    private boolean roomNamesError;/** If roomNames == null, this indicates whether
            * the content has not loaded yet or has errored while loading.
            **/
    private ListSelectionListener roomChooserListener;//used in 1 player games
    //java3d vars
    private boolean inGame;
    private boolean isGameInitialized;
    private GameSceneLoader loader;
    private BranchGroup scene;
    private SimpleUniverse universe;
    private SoundHandler soundHandler;
    private SpriteManager spriteManager;
    private PlayerSprite playerSprite;
    private BasicSpriteManager basicSpriteManager;
    private BasicSprite basicSprite;
    private TransformGroup viewerTG;
    private SpriteBehavior behavior;

    //list used by roomChooser (single-player games)
    private JList list;
    private JScrollPane listPane;

    //info gui (shows Help, About, and final scores)
    private JEditorPane infoEditorPane;
    private JButton infoNextPage, infoLastPage;
    private JLabel pageNumber;
    private String aboutText, helpTexts[];
    private int helpTextIndex = 0;
    private boolean aboutShowing, helpShowing;

    //networking vars
    private byte playerId;
    private String names[];
    private boolean isServer;
    private boolean isOnServerLAN;//see the description in NetworkClient2.java
    private InetSocketAddress serverAddr;
    private int playerGamePasswords[];
    private int thisGamePassword;
    private NetworkClient client;
    private NetworkServer server;

    private File roomToLoad;

    //variables used to monitor asynchronous threads
    private boolean initSuccessful;//tells whether init() succeeded or errored


//--------INITIALIZATION------------------------------------------------------------------


    public Lasertag3D( File roomToLoad )
    {
        this.roomToLoad = roomToLoad;

        //load settings to see if playerName is set or not
        loadSettings();

        if( "unset".equals( Lasertag3DSettings.playerName ) ) {
            //prompt for a name while initializing in the background
            Thread initThread = new Thread( new Runnable() {
                    public void run() { init(); }
                }
            );
            initThread.start();

            //now ask for a name while the program inits
            String playerName = JOptionPane.showInputDialog( null,
                    "Please enter the name you would like to use.\n" +
                    "Limit 20 characters, no spaces.", "Enter a Name",
                    JOptionPane.PLAIN_MESSAGE
            );
            if( playerName == null ) {
                System.out.println("cancelled");
                exit( 1 );/* User hit the cancel button; 1 indicates to
                                    not save settings. */
            }

            //if this is changed, also update setGeneral in SettingsPanel
            if( "".equals( playerName ) ) playerName = "unset";
            playerName = playerName.replaceAll( "\\s", "_" );
            if( playerName.length() > 20 ) {//trim to 20 characters
                playerName = playerName.substring( 0, 20 );
            }
            Lasertag3DSettings.playerName = playerName;

            while( initThread.isAlive() ) {
                try { Thread.sleep( 50 ); }
                catch( InterruptedException exc ) {}
            }

            //set the name in SettingsPanel and the settings file
            settingsPanel.resetName();
        }
        else init();//initialize normally

        //check that room names loaded
        while( roomNames == null ) {
            try { Thread.sleep( 50 ); }
            catch( InterruptedException exc ) {}
        }

        if( initSuccessful == false ) {
            JOptionPane.showMessageDialog( null,
                    "There was an error while launching the program.\n" +
                    "Program will exit.", "Lasertag3D Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit( 1 );
        }
        //else

        //show window
        frame.setVisible( true );

        if( this.roomToLoad != null ) {
            SwingUtilities.invokeLater( new Runnable() {
                    public void run()
                    {
                        singlePlayerGame( Lasertag3D.this.roomToLoad.
                                getAbsolutePath(), true );
                    }
            } );
        }
    }

    private void init()
    {
        //load data asynchronously
        Thread loadDataThread = new Thread( new Runnable() {
                public void run() { loadData(); }
            }
        );
        loadDataThread.start();

        //create GUI synchronously, then tell <init>
        createGUI();
        initSuccessful = true;
    }


    private void loadSettings()
    {
        try {
            Lasertag3DSettings.load( new File( SETTINGS_FILE ) );
        }
        catch( Exception exc ) {
            System.out.println( "Could not open settings file: " +
                    exc.getClass().getName() + "." + exc.getMessage() +
                    ", using defaults"
            );
            try {
                URL url = getClass().getResource( DEFAULT_SETTINGS_URL );
                Lasertag3DSettings.load( url );
            }
            catch( Exception exc2 ) {
                System.err.println( "Error: while loading default settings: " +
                        exc
                );
                JOptionPane.showMessageDialog( null,
                        "Could not load settings file nor default settings file.\n" +
                        "Program will exit.", "Lasertag3D Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.exit( 1 );
            }
        }
    }

    private void saveSettings()
    {
        try {
            Lasertag3DSettings.save( new File( SETTINGS_FILE ), NetworkClient.NUM_KEY_CODES );
        }
        catch( Exception exc ) {
            System.err.println( "Error: while saving settings: " + exc );
            exc.printStackTrace();
        }
    }


    private void loadData()
    {
        /** Load room names
        * These are only used for single player games; multiplayer games get
        * their room names from the server.
        * ROOMS_INDEX is formatted as a list of rooms, separated by newlines.
        **/
        BufferedReader reader = null;
        try {
            URL url = getClass().getResource( ROOMS_INDEX_URL );
            reader = new BufferedReader( new InputStreamReader( url.openStream() ) );
            ArrayList<String> roomNamesList = new ArrayList<String>();
            String line;
            while( (line = reader.readLine()) != null ) {
                line = line.trim();
                if( "".equals( line ) ) continue;//skip blank lines
                roomNamesList.add( line );
            }
            roomNames = roomNamesList.toArray( new String[ 0 ] );
        }
        catch( Exception exc ) {//usually IOException or ArrayIndexOutOfBoundsException
            synchronized( this ) {
                roomNamesError = true;
                System.err.println( "Error: while loading room names: " +
                        exc.getMessage()
                );
                exc.printStackTrace();
                if( reader != null ) {
                    try { reader.close(); }
                    catch( IOException exc2 ) {}
                }

                JOptionPane.showMessageDialog( null,
                        "Error while loading room names file.\nLasertag3D will exit.",
                        "Fatal Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
            System.exit( 1 );
        }

        //about text is formatted as a full html page
        aboutText = "<html><body>Loading content...</body></html>";
        DataInputStream in = null;
        try {
            URL url = getClass().getResource( ABOUT_URL );
            URLConnection connection = url.openConnection();
            connection.connect();
            int length = connection.getContentLength();
            byte data[] = new byte[ length ];
            in = new DataInputStream( connection.getInputStream() );
            in.readFully( data );
            in.close();
            aboutText = new String( data, "UTF-8" );
            //set image location
            aboutText = aboutText.replace( "IMAGE_PLACEHOLDER",
                    getClass().getResource( BG_IMAGE_URL ).toExternalForm()
            );
        }
        catch( Exception exc ) {//usually IOException or ArrayIndexOutOfBoundsException
            System.err.println( "Error: while loading about content: " +
                    exc.getMessage()
            );
            exc.printStackTrace();
            aboutText = "<html><body>Error while loading content.  " +
                    "Please check your network connection.</body></html>";
            if( in != null ) {
                try { in.close(); }
                catch( IOException exc2 ) {}
            }
        }
        if( aboutShowing ) {
            synchronized( this ) {
                infoEditorPane.setText( aboutText );
                infoEditorPane.setCaretPosition( 0 );
            }
        }

        //help text is formatted as several full html pages, separated by "--------"
        in = null;
        helpTexts = new String[ 1 ];
        helpTexts[ 0 ] = "<html><body>Loading content...</body></html>";
        try {
            URL url = getClass().getResource( HELP_URL );
            URLConnection connection = url.openConnection();
            connection.connect();
            int length = connection.getContentLength();
            byte data[] = new byte[ length ];
            in = new DataInputStream( connection.getInputStream() );
            in.readFully( data );
            in.close();
            String fullHelpText = new String( data, "UTF-8" );
            //set image locations
            fullHelpText = fullHelpText.replace( "IMAGE_PLACEHOLDER",
                    getClass().getResource( BG_IMAGE_URL ).toExternalForm()
            );
            helpTexts = fullHelpText.split( "--------" );//eight dashes
        }
        catch( Exception exc ) {//usually IOException or ArrayIndexOutOfBoundsException
            System.err.println( "Error: while loading help content: " +
                    exc.getMessage()
            );
            exc.printStackTrace();
            helpTexts = new String[ 1 ];
            helpTexts[ 0 ] = "<html><body>Error while loading content.  " +
                    "Please check your network connection.</body></html>";
            if( in != null ) {
                try { in.close(); }
                catch( IOException exc2 ) {}
            }
        }
        if( helpShowing ) {
            synchronized( this ) {
                infoEditorPane.setText( helpTexts[ 0 ] );
                infoEditorPane.setCaretPosition( 0 );
                if( helpTexts.length > 1 ) infoNextPage.setEnabled( true );
            }
        }
    }


    private void createGUI()
    {
        mainPanel = new JPanel();
        cardLayout = new CardLayout();
        mainPanel.setLayout( cardLayout );

        //create menu gui
        URL bgImageURL = getClass().getResource( BG_IMAGE_URL );
        if( bgImageURL == null ) {
            System.err.println( "Error: createGUI: the menuBG image could not be found." );
            JOptionPane.showMessageDialog( null, "Could not load background image.\n" +
                    "Program will exit.", "Lasertag3D Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit( 1 );
        }
        //else
        ImageIcon background = new ImageIcon( bgImageURL );

        URL buttonImageURL = getClass().getResource( BUTTON_IMAGE_URL );
        if( buttonImageURL == null ) {
            System.err.println( "Error: createGUI: the button image could not be found." );
            JOptionPane.showMessageDialog( null, "Could not load menu button image.\n" +
                    "Program will exit.", "Lasertag3D Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit( 1 );
        }
        //else
        ImageIcon button = new ImageIcon( buttonImageURL );

        menuPanel = new MenuPanel( this, MENU_ITEMS, background, button,
                new Font( null, Font.PLAIN, 30 ), Color.BLUE
        );
        mainPanel.add( menuPanel, "menuPanel" );

        //create game gui and init java3d
        gamePanel = new JPanel();
        gamePanel.setLayout( new BorderLayout() );
        JComponent canvasWrapper = null;
        try {
            canvasWrapper = initJava3D();//this creates the canvas3d
        }
        catch( Exception exc ) {
            System.err.println( "Error: createGUI: " + exc.getMessage() );
            exc.printStackTrace();
            JOptionPane.showMessageDialog( null, "Error while loading 3D library.\n" +
                    "Program will exit.", "Lasertag3D Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit( 1 );
        }
        //create message area
        messageArea = new JTextArea( "Hello, " + Lasertag3DSettings.playerName + ", you are player "
                + (playerId+1) + ".\nThe game will start soon.\n"
        );
        messageArea.setLineWrap( true );
        messageArea.setWrapStyleWord( true );
        messageArea.setEditable( false );
        messageArea.setBackground( Color.BLACK );
        messageArea.setForeground( Color.WHITE );
        messagePane = new JScrollPane( messageArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        messagePane.setPreferredSize( new Dimension( 150, 500 ) );
        messagePane.setMinimumSize( new Dimension( 0, 0 ) );
        //create score area
        scoreArea = new JTextArea();
        scoreArea.setLineWrap( true );
        scoreArea.setWrapStyleWord( true );
        scoreArea.setEditable( false );
        scoreArea.setBackground( Color.BLACK );
        scoreArea.setForeground( new Color( 127,127,255 ) );
        Font old = scoreArea.getFont();
        scoreArea.setFont( new Font( old.getFontName(), Font.BOLD, old.getSize() ) );
        scorePane = new JScrollPane( scoreArea,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
        );
        scorePane.setPreferredSize( new Dimension( 350, 100 ) );
        scorePane.setMinimumSize( new Dimension( 0, 0 ) );
        //add the components, using JSplitPane's for organization
        vSplitPane = new JSplitPane( JSplitPane.VERTICAL_SPLIT, canvasWrapper,
                scorePane
        );
        vSplitPane.setDividerLocation( -100 );
        vSplitPane.setResizeWeight( 1.0 );
        where = new JButton( "Where?" );
        where.addActionListener( new ActionListener() {
                public void actionPerformed( ActionEvent e )
                {
                    spriteManager.getPlayerSprites()[ 0 ].where();
                }
            }
        );
        hSplitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, vSplitPane,
                messagePane /*replace with where*/
        );
        hSplitPane.setDividerLocation( -150 );
        hSplitPane.setResizeWeight( 1.0 );
        gamePanel.add( hSplitPane );
        mainPanel.add( gamePanel, "gamePanel" );

        //create gameStarterPanel
        gameStarterPanel = new GameStarterPanel( this );
        mainPanel.add( gameStarterPanel, "gameStarterPanel" );

        //create general list for GUIs (roomChooser,)
        list = new JList();
        listPane = new JScrollPane( list );
        mainPanel.add( listPane, "list" );

        //create settingsPanel
        settingsPanel = new SettingsPanel( this );
        mainPanel.add( settingsPanel, "settingsPanel" );

        //create infoPanel
        infoPanel = new JPanel();
        infoPanel.setLayout( new BorderLayout() );
        ParserDelegator workaround = new ParserDelegator();
        infoEditorPane = new JEditorPane( "text/html", "" );
        infoEditorPane.setEditable( false );
        SafeHyperlinkListener.addTo( infoEditorPane );
        JScrollPane scrollPane = new JScrollPane( infoEditorPane );
        infoPanel.add( scrollPane, BorderLayout.CENTER );
        JPanel infoButtons = new JPanel();
        infoButtons.setLayout( new FlowLayout() );
        infoLastPage = new JButton( "Previous" );
        infoLastPage.addActionListener( this );
        infoButtons.add( infoLastPage );
        pageNumber = new JLabel( "Page 1" );
        Font oldFont = pageNumber.getFont();
        pageNumber.setFont( new Font( oldFont.getFontName(), Font.BOLD, 16 ) );
        infoButtons.add( pageNumber );
        infoNextPage = new JButton( "Next" );
        infoNextPage.addActionListener( this );
        infoButtons.add( infoNextPage );
        infoPanel.add( infoButtons, BorderLayout.SOUTH );
        mainPanel.add( infoPanel, "infoPanel" );

        //create "Return to Menu" button
        returnToMenu = new JButton( "Return to Menu" );
        returnToMenu.setFocusable( false );/** This prevents the button from
                * capturing key commands directed at other components (Canvas3D).
        **/
        returnToMenu.addActionListener( this );
        returnToMenu.setEnabled( false );//menu is already showing

        //create mute button
        URL soundOnURL = getClass().getResource( SOUND_ON_URL );
        URL soundOffURL = getClass().getResource( SOUND_OFF_URL );
        if( soundOnURL == null || soundOffURL == null ) {
            System.err.println( "Error: createGUI: a sound button image could not be found." );
            JOptionPane.showMessageDialog( null, "Could not load mute button images.\n" +
                    "Program will exit.", "Lasertag3D Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit( 1 );
        }
        //else
        soundOn = new ImageIcon( soundOnURL );
        soundOff = new ImageIcon( soundOffURL );
        playSoundButton = new MuteButton( soundHandler, soundOn.getImage(), soundOff.getImage(),
                (int) (returnToMenu.getPreferredSize().height * 1.5)
        );

        JPanel southPanel = new JPanel( new BorderLayout() );
        southPanel.add( returnToMenu );
        southPanel.add( playSoundButton, BorderLayout.EAST );

        //load icon
        ImageIcon icon = null;
        try {
            URL iconURL = getClass().getResource( "/icon.gif" );
            if( iconURL != null ) icon = new ImageIcon( iconURL );
        }
        catch( Exception exc ) {
            System.err.println( "Error: could not load icon: " + exc );
            exc.printStackTrace();
        }

        //create window
        frame = new JFrame( "Lasertag3D" );
        Container c = frame.getContentPane();
        c.setLayout( new BorderLayout() );
        c.add( mainPanel, BorderLayout.CENTER );
        c.add( southPanel, BorderLayout.SOUTH );
        frame.setSize( 650, 500 );
        frame.setLocationRelativeTo( null );//center on screen
        if( icon != null ) frame.setIconImage( icon.getImage() );
        frame.setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        frame.addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e )
                {
                    exit( 0 );
                }
            }
        );
    }


    private JComponent initJava3D() throws Exception
    {
        JPanel wrapper = new JPanel();
        wrapper.setLayout( new BorderLayout() );
        wrapper.setOpaque( false );
        /*drawing-over-canvas code taken from http://stackoverflow.com/questions/
                2559220/java3d-painting-2d-hud-over-a-canvas3d
        */
        canvas = new Canvas3D( SimpleUniverse.getPreferredConfiguration() ) {
            private static final long serialVersionUID = 7144426579917281131L;
            public void postRender()
            {
                float centerX = (float) (getWidth() / 2.0);
                float centerY = (float) (getHeight() / 2.0);
                J3DGraphics2D g2D = getGraphics2D();
                g2D.setColor( new Color( 175, 175, 175 ) );
                g2D.setStroke( new BasicStroke( 3, BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND ) );
                g2D.draw( new Line2D.Float( centerX - 7, centerY,
                        centerX + 7, centerY ) );
                g2D.draw( new Line2D.Float( centerX, centerY - 7,
                        centerX, centerY + 7 ) );
                g2D.flush( false );
            }
        };
        canvas.setPreferredSize( new Dimension( 200, 200 ) );
        canvas.setMinimumSize( new Dimension( 0, 0 ) );
        wrapper.add( canvas, BorderLayout.CENTER );
        canvas.setFocusable( true );
        universe = new SimpleUniverse( canvas );
        View view = universe.getViewer().getView();
        view.setTransparencySortingPolicy( View.TRANSPARENCY_SORT_GEOMETRY );
        view.setBackClipDistance( BACK_CLIP_DISTANCE );//accommodate large scenes
        view.setFrontClipDistance( FRONT_CLIP_DISTANCE );
        view.setFieldOfView( Math.toRadians( Lasertag3DSettings.fieldOfView ) );
        viewerTG = universe.getViewingPlatform().getViewPlatformTransform();
        //if there is an error while loading the sounds, they will be disabled
        soundHandler = new SoundHandler( getClass().getResource( GOOD_SOUND_URL ),
                getClass().getResource( BAD_SOUND_URL ), universe
        );
        if( logger != null ) logger.setSoundHandler( soundHandler );
        return wrapper;
    }

    //this is called by SettingsPanel when the fov is changed
    public void setFieldOfView( int fov )
    {
        if( fov <= 0 ) fov = 1;
        if( fov >= 180 ) fov = 179;
        Lasertag3DSettings.fieldOfView = fov;
        universe.getViewer().getView().setFieldOfView( Math.toRadians( fov ) );
    }


//--------GUI PROCESSING------------------------------------------------------------------


    public synchronized void actionPerformed( ActionEvent e )
    {
        if( e.getSource() == returnToMenu ) {
            returnToMenu();
            return;
        }

        else if( e.getSource() == infoNextPage ) {
            if( helpTextIndex < (helpTexts.length-1) ) {
                helpTextIndex++;
                if( helpTextIndex == (helpTexts.length-1) ) {
                    infoNextPage.setEnabled( false );
                }
                infoLastPage.setEnabled( true );
                pageNumber.setText( "Page " + (helpTextIndex+1) );
                infoEditorPane.setText( helpTexts[ helpTextIndex ] );
                infoEditorPane.setCaretPosition( 0 );
            }
        }
        else if( e.getSource() == infoLastPage ) {
            if( helpTextIndex > 0 ) {
                helpTextIndex--;
                if( helpTextIndex == 0 ) {
                    infoLastPage.setEnabled( false );
                }
                infoNextPage.setEnabled( true );
                pageNumber.setText( "Page " + (helpTextIndex+1) );
                infoEditorPane.setText( helpTexts[ helpTextIndex ] );
                infoEditorPane.setCaretPosition( 0 );
            }
        }

        //don't process menu commands unless this is actually the menu
        if( returnToMenu.isEnabled() ) return;//indicates menu is not showing

        //else
        if( e.getSource() == menuPanel ) {
            //can't use switch() {...} here because of the string type
            if( MENU_ITEMS[ 0 ].equals( e.getActionCommand() ) ) {
                singlePlayerGame();
            }
            else if( MENU_ITEMS[ 1 ].equals( e.getActionCommand() ) ) {
                multiPlayerGame();
            }
            else if( MENU_ITEMS[ 2 ].equals( e.getActionCommand() ) ) {
                showHelp();
            }
            else if( MENU_ITEMS[ 3 ].equals( e.getActionCommand() ) ) {
                showSettings();
            }
            else if( MENU_ITEMS[ 4 ].equals( e.getActionCommand() ) ) {
                showAbout();
            }
            else {
                System.err.println( "Error: in actionPerformed: " +
                        "unknown command from menu: \"" +
                        e.getActionCommand() + "\""
                );
            }
        }
    }


    /** Shows the main menu.  Other panels may call this method when they
    * are finished, so that control is transferred back to the menu.
    **/
    public synchronized void returnToMenu()
    {
        if( inGame ) {
            int quitting = JOptionPane.showConfirmDialog( frame,
                    "Are you sure you want to quit the game and return " +
                    "to the main menu?", "Confirm Return to Menu",
                    JOptionPane.YES_NO_OPTION
            );
            if( quitting == JOptionPane.NO_OPTION ) return;
            SwingUtilities.invokeLater( new Runnable() { public void run() {
                    endGame( 0, !Lasertag3DSettings.localSession );
            } } );
        }
        if( inGameStarter ) {
            if( gameStarterPanel.returnToMenu( true ) == false ) return;
            //else
            inGameStarter = false;
        }
        if( !inGame || Lasertag3DSettings.localSession ) {
            //reset vars
            inGame = false;
            isGameInitialized = false;
            helpShowing = false;
            aboutShowing = false;
            returnToMenu.setEnabled( false );
            cardLayout.show( mainPanel, "menuPanel" );
        }
    }


    //starts a local session
    private void singlePlayerGame()
    {
        if( roomNames == null ) {
            JOptionPane.showMessageDialog( frame,
                    "The room names have not been loaded yet.\n" +
                    "Try again in several seconds.", "Single-Player Game Error",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }

        //else
        returnToMenu.setEnabled( true );

        list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        list.setLayoutOrientation( JList.HORIZONTAL_WRAP );
        list.setListData( roomNames );
        roomChooserListener = new RoomChooserListener();
        list.addListSelectionListener( roomChooserListener );
        cardLayout.show( mainPanel, "list" );
        //when a room is selected, RoomChooserListener.valueChanged will be called
    }

    private class RoomChooserListener implements ListSelectionListener {
        public void valueChanged( ListSelectionEvent e )
        {
            if( e.getValueIsAdjusting() ) return;//wait for final value
            int roomIndex = list.getSelectedIndex();
            if( roomIndex == -1 ) return;//nothing is selected
            //get the room file name from the room name
            //this is not in the same format as room names from the server
            String theRoom = roomNames[ roomIndex ].toLowerCase().replace( ' ', '_' );

            //remove listener
            list.removeListSelectionListener( roomChooserListener );

            list.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
            singlePlayerGame( theRoom + ".mwg", false );
            list.setCursor( Cursor.getDefaultCursor() );
        }
    }

    /* Starts a single player game with the room in roomFileName.  If localFile
       is true, the room is loaded from the hard drive using roomFileName
       as an absolute path.
    */
    private void singlePlayerGame( String roomFileName, boolean localFile )
    {
        synchronized( this ) {

            if( isGameInitialized || inGame ) return;//in case of multiple clicks

            try {
                Lasertag3DSettings.localSession = true;
                playerId = (byte) 0;
                names = new String[ 1 ];
                names[ 0 ] = Lasertag3DSettings.playerName;
                isServer = false;
                serverAddr = null;
                vSplitPane.setDividerLocation( 1.0 );

                initGame( roomFileName, localFile );
                startGame();
                returnToMenu.setText( "End Session" );
            }
            catch( Throwable exc ) {
                System.err.println( "Error: when starting local session: " +
                        exc.getMessage()
                );
                exc.printStackTrace();
                endGame( 1 );
            }
        }
    }

    //starts the game starter panel
    private void multiPlayerGame()
    {
        String playerName = Lasertag3DSettings.playerName;
        while( true ) {//in case name is already taken
            try {
                menuPanel.setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
                gameStarterPanel.start( this, playerName, frame );
                cardLayout.show( mainPanel, "gameStarterPanel" );
                inGameStarter = true;
                //the panel will call startGame (asynchronously) when ready
                returnToMenu.setEnabled( true );
                break;//break from loop
            }
            catch( IllegalArgumentException exc ) {//error with playerName
                playerName = JOptionPane.showInputDialog( frame,
                        "Error: " + exc.getMessage() + ".\n" +
                        "Please select a different name.\n" +
                        "Limit 20 characters, no spaces.", "Enter Another Name",
                        JOptionPane.WARNING_MESSAGE
                );
                if( playerName == null || "".equals( playerName ) ) {
                    break;//player cancelled
                }
                //else
                Lasertag3DSettings.playerName = playerName;
                settingsPanel.resetName();
                //go into next iteration and try again
            }
            catch( IOException exc ) {
                System.err.println( "Error: when making GSPanel: " + exc.getMessage() );
                exc.printStackTrace();
                JOptionPane.showMessageDialog( frame, "Could not connect to server.\n" +
                        "Please check your network connection.",
                        "Multi-Player Game Error", JOptionPane.ERROR_MESSAGE
                );
                break;//break from loop
            }
        }

        menuPanel.setCursor( Cursor.getDefaultCursor() );
    }

    //shows help information in the infoPanel
    private void showHelp()
    {
        returnToMenu.setEnabled( true );

        infoEditorPane.setText( helpTexts[ 0 ] );
        infoEditorPane.setCaretPosition( 0 );
        helpTextIndex = 0;
        infoNextPage.setVisible( true );
        infoLastPage.setVisible( true );
        pageNumber.setText( "Page 1" );
        pageNumber.setVisible( true );
        infoNextPage.setEnabled( false );
        infoLastPage.setEnabled( false );
        if( helpTexts.length > 1 ) infoNextPage.setEnabled( true );
        helpShowing = true;
        cardLayout.show( mainPanel, "infoPanel" );
    }

    //shows the settingsPanel
    private void showSettings()
    {
        //force player to use Save/Cancel buttons instead
        returnToMenu.setEnabled( false );

        cardLayout.show( mainPanel, "settingsPanel" );
    }

    //shows about information in the infoPanel
    private void showAbout()
    {
        returnToMenu.setEnabled( true );

        infoEditorPane.setText( aboutText );
        infoEditorPane.setCaretPosition( 0 );
        infoNextPage.setVisible( false );
        infoLastPage.setVisible( false );
        pageNumber.setVisible( false );
        aboutShowing = true;
        cardLayout.show( mainPanel, "infoPanel" );
    }


    private void exit( int status )
    {
//DEBUG  System.out.println( "Exit called; status=" + status );
        if( status == 0 ) {//only ask in good conditions
            if( inGame ) {
                int confirm = JOptionPane.showConfirmDialog( frame,
                        "Are you sure you want to quit the game?",
                        "Confirm Exit", JOptionPane.YES_NO_OPTION
                );
                if( confirm == JOptionPane.NO_OPTION ) return;
                //else fall through and exit
            }
        }
        //ask this regardless of status, but only prompt player if status is 0
        if( inGameStarter && gameStarterPanel != null ) {//ask if can exit
            boolean canExit = gameStarterPanel.returnToMenu( (status==0) );
            if( canExit == false ) return;
            //else fall through and exit
        }

        //regardless of status, cleanup    
        try {
            if( inGame || inGameStarter ) endGame( status );
            if( frame != null ) {
                frame.dispose();//System.exit may not clear java3d's screen resources
            }
        }
        catch( Throwable exc ) {//in case of exception, exit anyway
            System.err.println( "Error: exit: " + exc.getMessage() );
            exc.printStackTrace();
        }

        //if status == 0, save the settings
        if( status == 0 ) saveSettings();//errors will be ignored

        System.exit( status );
    }


//--------GAME INIT-----------------------------------------------------------------------


    private void initGame( String roomFileName, boolean absolutePath )
            throws Exception
    {
        URL roomURL;
        if( absolutePath ) {
            roomURL = new URL( "file://" + roomFileName );
        }
        else {
            roomURL = getClass().getResource( ROOMS_DIR_URL + roomFileName );
        }

        isGameInitialized = true;//after this line, variables are changed

        createScene( roomURL, getClass().getResource( SPRITE_OBJ_URL ) );
        if( initNetworking() == false ) throw new Exception( "initNetworking() error" );
        messageArea.setText( "Hello, " + Lasertag3DSettings.playerName +
                ", you are player " + (playerId+1) + ".\n"
        );
    }

    private void createScene( URL sceneMWG, URL spriteOBJ ) throws Exception
    {
        scene = new BranchGroup();
        scene.setCapability( BranchGroup.ALLOW_DETACH );
        loader = new GameSceneLoader( sceneMWG );
        setGravityOn();
        setTransparency();
        loader.attachTo( scene );
        createSprites( spriteOBJ );
        //make behavior
        behavior = new SpriteBehavior( canvas );
        behavior.setSchedulingBounds( loader.getBounds() );
        scene.addChild( behavior );
        scene.compile();
        System.gc();
    }

    private void setGravityOn()
    {
        boolean set = false;
        //look for application specific line: 'g' t/f
        String lines[] = loader.getApplicationSpecifics();
        String tokens[];
        for( int i = 0; i < lines.length; i++ ) {
            tokens = lines[ i ].trim().split( "\\s" );
            if( "g".equalsIgnoreCase( tokens[ 0 ] ) ) {
                Lasertag3DSettings.gravityOn = ("t".equalsIgnoreCase( tokens[ 1 ] ));
                set = true;
            }
        }
        if( set == false ) {
            throw new MWGFileFormatException( "Application-specific error:" +
                    " no gravity information."
            );
        }
    }

    private void setTransparency()
    {
        TransparencyAttributes transparency = new TransparencyAttributes(
                TransparencyAttributes.NICEST, 0.0f
        );
        GameSceneLoader.TextureDefinition textures[] = loader.getTextureDefinitions().
                values().toArray( new GameSceneLoader.TextureDefinition[ 0 ] );
        for( int i = 0; i < textures.length; i++ ) {
            if( textures[ i ].getProperties() == SpriteManager.TEXTURE_TRANSPARENT ) {
                textures[ i ].getAppearance().setTransparencyAttributes( transparency );
            }
        }
    }

    private void createSprites( URL spriteOBJ ) throws Exception
    {
        //load sprite geometry
        ObjectFile modelLoader = new ObjectFile();
        BranchGroup spriteBG = modelLoader.load( spriteOBJ ).getSceneGroup();
        Geometry spriteGeometry = searchGroup( spriteBG );
        if( spriteGeometry == null ) {
            JOptionPane.showMessageDialog( null, "Could not find data file " +
                    spriteOBJ + ".\nLasertag3D will exit.", "Fatal Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.err.println( "Error: geometry not found" );
            System.exit( 1 );
        }
        //else

        Vector3f startPositions[] = new Vector3f[ names.length ];
        Matrix3f startRotations[] = new Matrix3f[ names.length ];
        String startDetails[] = loader.getApplicationSpecifics();
        for( int i = 0; i < startDetails.length; i++ ) {
            //extract the playerId, start position, and start rotation
            //format is: 's' id x y z m00 m01 m02 m10 m11 m12 m20 m21 m22
            String tokens[] = startDetails[ i ].trim().split( "\\s+" );
            if( "s".equalsIgnoreCase( tokens[ 0 ] ) ) {
                int id = Integer.parseInt( tokens[ 1 ] );
                if( id >= names.length ) continue;//ignore
                //else
                startPositions[ id ] = new Vector3f(
                        loader.getXScale() * Float.parseFloat( tokens[ 2 ] ),
                        loader.getYScale() * Float.parseFloat( tokens[ 3 ] ),
                        loader.getZScale() * Float.parseFloat( tokens[ 4 ] )
                );
                int offset = 5;//what index the rotation matrix starts at
                startRotations[ id ] = new Matrix3f( Float.parseFloat( tokens[ offset ] ),
                        Float.parseFloat( tokens[ offset + 1 ] ),
                        Float.parseFloat( tokens[ offset + 2 ] ),
                        Float.parseFloat( tokens[ offset + 3 ] ),
                        Float.parseFloat( tokens[ offset + 4 ] ),
                        Float.parseFloat( tokens[ offset + 5 ] ),
                        Float.parseFloat( tokens[ offset + 6 ] ),
                        Float.parseFloat( tokens[ offset + 7 ] ),
                        Float.parseFloat( tokens[ offset + 8 ] )
                );
            }
        }
        //make sure all the players have start positions and rotations
        for( int i = 0; i < startPositions.length; i++ ) {
            if( startPositions[ i ] == null || startRotations[ i ] == null ) {
                throw new MWGFileFormatException( "Application-specific error:" +
                        " player " + i + " does not have a starting position" +
                        " or rotation."
                );
            }
        }
//DEBUG  System.out.println( "LT3D: startPositions.length=" + startPositions.length );

        //create dot appearance and geometry
        Appearance dotAppearance = new Appearance();
        Material dotMaterial = new Material();
        dotMaterial.setAmbientColor( dotColor );
        dotMaterial.setLightingEnable( false );
        dotAppearance.setMaterial( dotMaterial );
        QuadArray dotGeometry = new QuadArray( 4, QuadArray.COORDINATES );
        Point3f dotVertice = new Point3f( 0, 0, 0 );
        dotGeometry.setCoordinate( 0, dotVertice );
        dotVertice.set( DOT_SIZE, 0, 0 );
        dotGeometry.setCoordinate( 1, dotVertice );
        dotVertice.set( DOT_SIZE, DOT_SIZE, 0 );
        dotGeometry.setCoordinate( 2, dotVertice );
        dotVertice.set( 0, DOT_SIZE, 0 );
        dotGeometry.setCoordinate( 3, dotVertice );

        Appearance appearance = new Appearance();
        Material material = new Material();
        material.setLightingEnable( false );
        appearance.setMaterial( material );
        if( isServer || Lasertag3DSettings.localSession ) {
            //make a SpriteManager (more complex)
            spriteManager = new SpriteManager( scene, spriteGeometry, appearance,
                    laserPosition, new BoundingPolytope( new BoundingBox( lowerBound,
                            upperBound ) ), centerToGround, radius, startPositions,
                    startRotations, dotGeometry, dotAppearance, viewerTG,
                    this, playerId, names, soundHandler, loader.getBounds()
            );
            playerSprite = spriteManager.getPlayerSprites()[ playerId ];
        }
        else {
            basicSpriteManager = new BasicSpriteManager( scene, spriteGeometry,
                    appearance, startPositions, startRotations, dotGeometry,
                    dotAppearance, viewerTG, this, playerId, names, soundHandler,
                    loader.getBounds()
            );
            basicSprite = basicSpriteManager.getBasicSprites()[ playerId ];
        }
    }

    private Geometry searchGroup( Group group ) throws Exception
    {
        Enumeration children = group.getAllChildren();
        while( children.hasMoreElements() ) {
            Node child = (Node) children.nextElement();
            if( child instanceof Shape3D ) {
                return ((Shape3D) child).getGeometry();
            }
            else if( child instanceof Group ) {
                Geometry geometry = searchGroup( group );
                if( geometry != null ) return geometry;
                //else keep searching
            }
            else {
                System.out.println( "Searching for sprite geometry: unknown node," +
                        " class=" + child.getClass().getName() );
            }
        }
        return null;
    }


    //return value indicates success/failure
    private boolean initNetworking()
    {
        if( Lasertag3DSettings.localSession ) {
            LocalClient localClient = new LocalClient( Lasertag3DSettings.playerName,
                    this, playerSprite, basicSprite );
            LocalServer localServer = new LocalServer( spriteManager, localClient );
            localClient.setLocalServer( localServer );
            client = localClient;
            server = localServer;
        }

        else {
            try {
                client = new NetworkClient2( serverAddr,
                        Lasertag3DSettings.playerName, thisGamePassword,
                        playerId, names.length, this, isServer, playerSprite,
                        basicSprite
                );
            }
            catch( BindException exc ) {
                JOptionPane.showMessageDialog( frame, "Error while starting game: " +
                        "the port you chose is in use by another program.\n" +
                        "If this error occurs frequently, " +
                        "go into Settings and change 'port' value.",
                        "Game Start Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.err.println( "Lasertag3D: initNetworking(): " +
                        exc.getMessage()
                );
                exc.printStackTrace();
                return false;
            }
            catch( IOException exc ) {
                JOptionPane.showMessageDialog( frame, "Error while starting game.\n" +
                        "Please check your network connection.",
                        "Game Start Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.err.println( "Lasertag3D: initNetworking(): " +
                        exc.getMessage()
                );
                exc.printStackTrace();
                return false;
            }

            if( isServer ) {
                try {
                    server = new NetworkServer2( playerGamePasswords,
                            serverAddr.getPort(), spriteManager, client, this
                    );
                }
                catch( BindException exc ) {
                    JOptionPane.showMessageDialog( frame, "Error while starting game: " +
                            "the port you chose is in use by another program.\n" +
                            "If this error occurs frequently, " +
                            "go into Settings and change the 'server port' value.",
                            "Game Start Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    System.err.println( "Lasertag3D: initNetworking: " +
                            exc.getMessage()
                    );
                    exc.printStackTrace();
                    return false;
                }
                catch( IOException exc ) {
                    JOptionPane.showMessageDialog( frame, "Error while starting game.\n" +
                            "Please check your network connection.",
                            "Game Start Error",
                            JOptionPane.ERROR_MESSAGE
                    );
                    System.err.println( "Lasertag3D: initNetworking: " +
                            exc.getMessage()
                    );
                    exc.printStackTrace();
                    return false;
                }
            }
        }

        behavior.setNetworkClient( client );
        return true;
    }


//--------RUNNING THE GAME----------------------------------------------------------------


    private void startGame()
    {
        universe.addBranchGraph( scene );

        //show this before network init so the user knows that something is happening
        cardLayout.show( mainPanel, "gamePanel" );
        
        SwingUtilities.invokeLater( new Runnable() {
                public void run() { canvas.requestFocusInWindow(); }
        } );

        //start network
        try {
            //wait 5 seconds for server to start
            try { Thread.sleep( NetworkServer2.DELAY ); }
            catch( InterruptedException exc ) {}
            client.open();
        }
        catch( BindException exc ) {
            JOptionPane.showMessageDialog( frame, "Error while starting game: " +
                    "the port you chose is in use by another program.\n" +
                    "If this error occurs frequently, " +
                    "go into Settings and change the 'port' value.",
                    "Game Start Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.err.println( "Lasertag3D: startGame(no args): " + exc.getMessage() );
            exc.printStackTrace();
            endGame( 0 );
            return;
        }
        catch( IOException exc ) {
            JOptionPane.showMessageDialog( frame, "Error while starting game.\n" +
                    "Please check your network connection.",
                    "Game Start Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.err.println( "Lasertag3D: startGame(no args): " + exc.getMessage() );
            exc.printStackTrace();
            endGame( 0 );
            return;
        }
        catch( IllegalStateException exc ) {//client was closed
            return;
        }

        if( isServer ) {
            try {
//DEBUG         System.out.println( "startGame: opening server..." );
                server.open();
            }
            catch( BindException exc ) {
                JOptionPane.showMessageDialog( frame, "Error while starting game: " +
                        "the port you chose is in use by another program.\n" +
                        "If this error occurs frequently, " +
                        "go into Settings and change the 'server port' value.",
                        "Game Start Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.err.println( "Lasertag3D: startGame(no args): " +
                        exc.getMessage()
                );
                exc.printStackTrace();
                endGame( 0 );
                return;
            }
            catch( IOException exc ) {
                JOptionPane.showMessageDialog( frame, "Error while starting game.\n" +
                        "Please check your network connection.",
                        "Game Start Error",
                        JOptionPane.ERROR_MESSAGE
                );
                System.err.println( "Lasertag3D: startGame(no args): " +
                        exc.getMessage()
                );
                exc.printStackTrace();
                endGame( 0 );
                return;
            }
        }

        inGameStarter = false;
        inGame = true;
    }

    private void endGame( int status )
    {
        endGame( status, false );
    }

    private synchronized void endGame( int status, boolean showScores )
    {
        //remove scene; do this first to prevent the user from messing around
        if( scene != null ) universe.getLocale().removeBranchGraph( scene );

        //close network
        if( (server != null) && isServer ) {
            try {
                if( server.isOpen() ) server.endGame();
                else server.close();
            }
            catch( Exception exc ) {
                try { server.close(); }
                catch( Exception exc2 ) {
                    exc2.printStackTrace();
                }
            }
        }
        if( (client != null) && (isServer == false || status != 0) ) {
            try {
                if( client.isOpen() ) client.closeWithByeMessage();
                else client.close();//this prevents server timeout messages
            }
            catch( Exception exc ) {
                try { client.close(); }
                catch( Exception exc2 ) {
                    exc2.printStackTrace();
                }
            }
        }

        if( status != 0 ) {
            System.err.println( "Error: endGame called with status != 0; inGame=" + inGame );
            if( inGame ) {
                JOptionPane.showMessageDialog( frame,
                        "A network error has disconnected you.",
                        "Network Error", JOptionPane.ERROR_MESSAGE
                );
            }
            else {
                JOptionPane.showMessageDialog( frame,
                        "There was an error while initializing the game.",
                        "Game Start Error", JOptionPane.ERROR_MESSAGE
                );
            }
        }

        //remove gameStarterPanel
//DEBUG  System.out.println( "stopping gsPanel..." );
        if( inGameStarter ) {
            gameStarterPanel.returnToMenu( false );
            inGameStarter = false;
        }
//DEBUG  System.out.println( "gsPanel stopped" );

        //clear the old game from memory
        inGame = false;
        isGameInitialized = false;
        client = null;
        server = null;
        scene = null;
        loader = null;
        spriteManager = null;
        playerSprite = null;
        basicSpriteManager = null;
        basicSprite = null;
        behavior = null;
        messageArea.setText( "" );
        if( Lasertag3DSettings.localSession ) {
            vSplitPane.setDividerLocation( -100 );
            Lasertag3DSettings.localSession = false;
        }
        System.gc();

        returnToMenu.setText( "Return to Menu" );

        if( showScores && currentScoreText != null ) {
            infoEditorPane.setText( "<html>\n<body>\n" +
                    currentScoreText.
                    replace( "Scores", "<b>Scores</b>" ).
                    replace( "(Final)", "(Left Early)" ).
                    replace( " ", "<br />" ).
                    replace( "\n", "<br />" ).
                    replace( ":", ":  " ) +
                    "\n</body>\n</html>"
            );
            scoreArea.setText( "" );
            infoEditorPane.setCaretPosition( 0 );
            infoNextPage.setVisible( false );
            infoLastPage.setVisible( false );
            pageNumber.setVisible( false );
            cardLayout.show( mainPanel, "infoPanel" );
            returnToMenu.setEnabled( true );//keep enabled
        }
        else {
            //show the menu
            cardLayout.show( mainPanel, "menuPanel" );
            returnToMenu.setEnabled( false );
        }

        //now reset the vars used to show scores
        currentScoreText = null;
    }


//--------NETWORK_LISTENER METHODS--------------------------------------------------------


    public void startGame( InetSocketAddress server, byte playerIdInGame,
            InetAddress thisInetAddr, String playerNames[], boolean isServer,
            int playerGamePasswords[], int thisGamePassword,
            String roomFileName )
    {
/*DEBUG*/System.out.println( "Lt3d: isServer=" + isServer + ", id=" + playerIdInGame );
/*DEBUG*/System.out.println( "Lt3d: server=" + server.getAddress() + ":" + server.getPort() );

        if( isServer ) {
            returnToMenu.setText( "End Game" );
        }

        //set variables
        this.playerId = playerIdInGame;
        this.names = playerNames;
        this.isServer = isServer;
        this.serverAddr = server;
        this.playerGamePasswords = playerGamePasswords;
        this.thisGamePassword = thisGamePassword;

        try {
            initGame( roomFileName, false );
            System.out.println( "Game initialized successfully" );
        }
        catch( Throwable exc ) {
            System.err.println( "Error: startGame(with args): " + exc.getMessage() );
            exc.printStackTrace();
            endGame( 1 );
            return;
        }

        int scores[] = new int[ names.length ];
        Arrays.fill( scores, 0, scores.length, 0 );
        updateScores( scores );

        startGame();
        System.out.println( "Game started successfully" );
    }

    public void updateScene( Vector3f playerPositions[], Matrix3f playerRotations[],
            Vector3f dotPositions[] )
    {
        //if this method is called, this is not the server; use basic sprite package
        basicSpriteManager.set( playerPositions, playerRotations, dotPositions );
    }

    public void updateHits( byte playersHit[], byte shooters[] )
    {
        //if this method is called, this is not the server; use basic sprite package
        basicSpriteManager.setHits( playersHit, shooters );
    }

    public void updateScores( int scores[] )
    {
        scoreArea.getText();
        try {
            String scoreString = "\tScores\n";
            for( int i = 0; i < names.length; i++ ) {
                //get the highest score left, then remove it
                int index = maxIndex( scores );
                int score = scores[ index ];
                scores[ index ] = Integer.MIN_VALUE;
                //add tabs to separate scores
                if( i != 0 ) scoreString += "  \t";
                //actual score string
                scoreString += names[ index ] + ":" + score;
                //determine if the player is in the game still
                boolean inGame;
                if( isServer ) inGame = spriteManager.isPlayerInGame( index );
                else inGame = basicSpriteManager.isPlayerInGame( index );
                if( inGame == false ) scoreString += "(Final)";
            }
            scoreArea.setText( scoreString );
            if( scoreString != null && !("".equals( scoreString.trim() )) ) {
                this.currentScoreText = scoreString;
            }
        }
        catch( Exception exc ) {}
    }

    public void thisWasHit( int shooterId )
    {
        messageForUser( "Player " + (shooterId+1) + " hit you!" );
    }

    public void thisDidHit( int playerHitId )
    {
        messageForUser( "You hit player " + (playerHitId+1) + "." );
    }

    public void playerLeft( int playerId, int reason )
    {
//DEBUG  System.out.println( "lt3d: playerLeft, id=" + playerId + ", reason=" + reason );
        if( isServer ) spriteManager.playerLeft( playerId );
        else basicSpriteManager.playerLeft( playerId );
        //tell user
        if( reason == NetworkClient.LEFT_VOLUNTARILY ) {
            messageForUser( "Player " + (playerId+1) + " has left the game." );
        }
        else if( reason == NetworkClient.LOST_CONNECTION ) {
            messageForUser( "Lost connection: player " + (playerId+1) +
                    " has lost their connection and is no longer in the game."
            );
            System.err.println( "Lost connection with player " + (playerId+1) );
        }
        else {
            System.err.println( "Lasertag3D: playerLeft: reason=" + reason +
                    ", invalid value."
            );
        }
    }

    public synchronized void exceptionThrown( Exception exc )
    {
        if( exc instanceof SocketTimeoutException ) return;//ignore it
        //else
        System.err.println( "Lasertag3D: exceptionThrown: " + exc.getMessage() );
        exc.printStackTrace();
        messageForUser( "A fatal network error has occurred.  Ending game..." );
        if( isServer ) {
            try {
                server.endGame();
                System.err.println( "Server endGame() was successful" );
            }
            catch( IOException exc2 ) {
                System.err.println( "Server endGame() was not successful" );
                try {
                    server.close();
                    System.err.println( "Server close() was successful" );
                }
                catch( IOException exc3 ) {
                    System.err.println( "Server close() was not successful" );
                }
            }
            catch( NullPointerException exc4 ) {}
        }
        //whether this is server or not, client should close, too
        try {
            client.closeWithByeMessage();
            System.err.println( "Client closeWithByeMessage() was successful" );
        }
        catch( IOException exc2 ) {
            System.err.println( "Client closeWithByeMessage() was not successful" );
            try {
                client.close();
                System.err.println( "Client close() was successful" );
            }
            catch( IOException exc3 ) {
                System.err.println( "Client close() was not successful" );
            }
        }
        catch( NullPointerException exc4 ) {}

        if( exc instanceof ServerTimeoutException ) {//use specific error dialog
            JOptionPane.showMessageDialog( frame,
                    "The server took too long to connect to.\n" +
                    "The game will be cancelled.",
                    "Network Error", JOptionPane.ERROR_MESSAGE
            );
            endGame( 0 );
        }
        else endGame( 1, !Lasertag3DSettings.localSession );//show scores
    }

    public void gameOver( int scores[] )
    {
        if( Lasertag3DSettings.localSession == false ) {
            messageForUser( "Game over!" );
            updateScores( scores );
            if( inGame ) {
                endGame( 0, true );
            }
            //else endGame has already been called
        }
    }

    public void serverTimedOut()
    {
        endGame( 1, true );
    }

    public boolean isServer() { return isServer; }

    public void messageForUser( String message )
    {
        if( message == null ) messageArea.append( "Done.\n" );
        else messageArea.append( message + "\n" );
        //scroll down to the message automatically
        JViewport viewPort = messagePane.getViewport();
        Rectangle viewRect = viewPort.getViewRect();
        Point newPoint = new Point( viewRect.x,
                messageArea.getHeight() - viewRect.height
        );
        if( newPoint.y > 0 ) {//needs to be scrolled
            viewPort.setViewPosition( newPoint );
        }
    }

    public void resetPassword()
    {
        settingsPanel.resetPassword();
    }


//--------STATIC METHODS------------------------------------------------------------------


//utility methods that dissect room names

    //room name format: "[short name]" + " " + "[index, a number 1+]"
    //room file name = lower(replace(' ','_',[short name])) + ".mwg"
    public static String getRoomFileName( String roomName )
    {
        int spaceIndex = roomName.lastIndexOf( ' ' );
        String fileName = roomName.substring( 0, spaceIndex ) + ".mwg";
        return fileName.toLowerCase().replace( ' ', '_' );
    }

    public static int getRoomIndex( String roomName )
    {
        int spaceIndex = roomName.lastIndexOf( ' ' );
        return Integer.parseInt( roomName.substring( spaceIndex + 1 ) );
    }


//--------MAIN----------------------------------------------------------------------------


    public static void main( String args[] ) throws Exception
    {
        System.out.println( "Usage: java Lasertag3D [-nolog | -log <file>] [room]" );
        System.out.println( "If [room] is specified, its scene file will be " +
                "opened in single-player mode." );

        boolean noLog = false;
        String specialLogFile = null;
        File roomToLoad = null;
        boolean done = false;
        if( args.length >= 1 && "-nolog".equalsIgnoreCase( args[ 0 ] ) ) {
            noLog = true;
            if( args.length == 1 ) done = true;
        }
        else if( args.length >= 2 && "-log".equalsIgnoreCase( args[ 0 ] ) ) {
            specialLogFile = args[ 1 ];
            if( args.length == 2 ) done = true;
        }
        else if( args.length == 0 ) {
            done = true;
        }
        if( done == false ) {
            roomToLoad = new File( args[ args.length - 1 ] );
        }

        //set writableDir
        WRITABLE_DIR = getWritableDir().getAbsolutePath() + "/";

        //add the directories to the resource URL's
        SETTINGS_FILE = WRITABLE_DIR + SETTINGS_FILE;
        if( specialLogFile != null ) {
            LOG_FILE = specialLogFile;
        }
        else LOG_FILE = WRITABLE_DIR + LOG_FILE;

        //set the log file
        if( noLog == false ) {
            try {
                File logFile = new File( LOG_FILE );
                logger = new LogPrintStream( logFile );//this will overwrite the old log file
                System.setOut( logger );
                System.setErr( logger );
            }
            catch( Exception exc ) {//FileNotFoundException or SecurityException
                System.err.println( "Error: logging disabled due to: " + exc );
                exc.printStackTrace();
            }
        }
        else {
            System.out.println( "-nolog flag set" );
        }

        //start the program
        new Lasertag3D( roomToLoad );
    }


//utility methods


    //operating system codes
    public final static int WINDOWS = 0;
    public final static int WINDOWS_64 = 1;
    public final static int MAC = 2;
    public final static int LINUX = 3;
    public final static int LINUX_64 = 4;

    public static File getWritableDir() throws IOException
    {
        int os = getOs();
        File home = FileSystemView.getFileSystemView().getHomeDirectory();
        File dir = null;
        switch( os ) {
            case WINDOWS: case WINDOWS_64:
                String userProfile = System.getenv( "USERPROFILE" );
                dir = new File( userProfile, "Application Data/Lasertag3D/" );
                break;
            case LINUX:
                dir = new File( home, ".Lasertag3D/" );
                break;
            case LINUX_64:
                dir = new File( home, ".Lasertag3D/" );
                break;
            case MAC:
                dir = new File( home, "Library/Preferences/Lasertag3D/" );
                break;
            default: return null;//this will never happen
        }
        //create the directory, if it doesn't already exist
        if( dir.exists() == false ) dir.mkdirs();
        return dir;
    }

    public static int getOs()
    {
        String arch = System.getProperty( "os.arch" );
        String name = System.getProperty( "os.name" );
        boolean is64 = (arch.indexOf( "64" ) != -1);
        int os = -1;
        if( name.toLowerCase().indexOf( "windows" ) != -1 ) {
            os = (is64)? WINDOWS_64: WINDOWS;
        }
        else if( name.toLowerCase().indexOf( "mac" ) != -1 ) {
            os = MAC;
        }
        else if( name.toLowerCase().indexOf( "nix" ) != -1 ||
                name.toLowerCase().indexOf( "nux" ) != -1
        ) {
            os = (is64)? LINUX_64: LINUX;
        }
        else {
            JOptionPane.showMessageDialog( null,
                    "Your operating system is not supported.",
                    "Lasertag3D Install Error",
                    JOptionPane.ERROR_MESSAGE
            );
            System.exit( 0 );
        }
        return os;
    }

    /** Utility function used by updateScores when sorting the scores.
    * Returns the index of the max value (not the value itself).
    * If all the values equal Integer.MIN_VALUE, 0 is returned.
    **/
    public int maxIndex( int array[] )
    {
        int max = Integer.MIN_VALUE;
        int maxIndex = 0;
        for( int i = 0; i < array.length; i++ ) {
            if( array[ i ] > max ) {
                max = array[ i ];
                maxIndex = i;
            }
        }
        return maxIndex;
    }
}
