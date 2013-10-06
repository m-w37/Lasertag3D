//SettingsPanel.java (7/15/2011)
//presents a GUI interface for changing LT3D settings
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;
import java.net.*;
import java.util.Hashtable;

import com.mattweidner.lt3d.net.NetworkClient;


public class SettingsPanel extends JPanel implements ActionListener {

    private final static String WARNING_TEXT = "<html>\n<body>\n" +
            "<p style=\"font-size:large;\" align=\"center\">\n" +
            "NOTE: In order to host multiplayer games, you will\n" +
            "need to port forward the server port that\n" +
            "you select.\n</p>\n" +
            "</body>\n</html>";

    private final static String lt3dKeys[] = {"Increase Speed",
            "Go Forwards", "Go Backwards", "Strafe Up", "Strafe Down",
            "Strafe Left", "Strafe Right", "Rotate Counter-clockwise",
            "Rotate Clockwise"
    };
    private final static int AWTKeys[] = {KeyEvent.VK_0,KeyEvent.VK_1,
        KeyEvent.VK_2,KeyEvent.VK_3,KeyEvent.VK_4,KeyEvent.VK_5,KeyEvent.VK_6,
        KeyEvent.VK_7,KeyEvent.VK_8,KeyEvent.VK_9,KeyEvent.VK_A,KeyEvent.VK_B,
        KeyEvent.VK_C,KeyEvent.VK_D,KeyEvent.VK_E,KeyEvent.VK_F,KeyEvent.VK_G,
        KeyEvent.VK_H,KeyEvent.VK_I,KeyEvent.VK_J,KeyEvent.VK_K,KeyEvent.VK_L,
        KeyEvent.VK_M,KeyEvent.VK_N,KeyEvent.VK_O,KeyEvent.VK_P,KeyEvent.VK_Q,
        KeyEvent.VK_R,KeyEvent.VK_S,KeyEvent.VK_T,KeyEvent.VK_U,KeyEvent.VK_V,
        KeyEvent.VK_W,KeyEvent.VK_X,KeyEvent.VK_Y,KeyEvent.VK_Z,
        KeyEvent.VK_UP,KeyEvent.VK_DOWN,KeyEvent.VK_LEFT,KeyEvent.VK_RIGHT,
        KeyEvent.VK_ALT,KeyEvent.VK_CONTROL,KeyEvent.VK_SHIFT,KeyEvent.VK_WINDOWS,
        KeyEvent.VK_SPACE,KeyEvent.VK_TAB,KeyEvent.VK_ENTER,
        KeyEvent.VK_INSERT,KeyEvent.VK_DELETE,
        KeyEvent.VK_HOME,KeyEvent.VK_END,
        KeyEvent.VK_PAGE_UP,KeyEvent.VK_PAGE_DOWN,
        KeyEvent.VK_NUMPAD0,
        KeyEvent.VK_NUMPAD1,KeyEvent.VK_NUMPAD2,KeyEvent.VK_NUMPAD3,
        KeyEvent.VK_NUMPAD4,KeyEvent.VK_NUMPAD5,KeyEvent.VK_NUMPAD6,
        KeyEvent.VK_NUMPAD7,KeyEvent.VK_NUMPAD8,KeyEvent.VK_NUMPAD9,
        KeyEvent.VK_BACK_QUOTE,KeyEvent.VK_BACK_SLASH,KeyEvent.VK_BACK_SPACE,
        KeyEvent.VK_OPEN_BRACKET,KeyEvent.VK_CLOSE_BRACKET,KeyEvent.VK_COMMA,
        KeyEvent.VK_EQUALS,KeyEvent.VK_MINUS,KeyEvent.VK_PERIOD,
        KeyEvent.VK_QUOTE,KeyEvent.VK_SEMICOLON,KeyEvent.VK_SLASH
    };

    private JTabbedPane tabbedPane;
    private JPanel keyPanel, generalPanel, portPanel;
    //save/cancel buttons
    private JButton saveButton, cancelButton;
    //general settings
    private JTextField name;
    private JSlider fovSlider;
    private JSlider mouseSensitivitySlider;
    //key settings
    private JComboBox keyBoxes[];
    private JLabel keyLabels[];
    //port settings
    private JEditorPane portWarning;
    private JCheckBox gameHostingEnabledCheckBox;
    private JLabel serverPortLabel;
    private JSpinner serverPortSpinner;

    private String AWTKeyNames[];

    private Lasertag3D main;


    public SettingsPanel( Lasertag3D main )
    {
        this.main = main;

        AWTKeyNames = new String[ AWTKeys.length ];
        for( int i = 0; i < AWTKeys.length; i++ ) {
            AWTKeyNames[ i ] = KeyEvent.getKeyText( AWTKeys[ i ] );
        }
        //options tabs
        tabbedPane = new JTabbedPane();
        makeGeneralPanel();
        tabbedPane.addTab( "General", generalPanel );
        makeKeyPanel();
        tabbedPane.addTab( "Key Bindings", keyPanel );
        makePortPanel();
        tabbedPane.addTab( "Game Hosting", portPanel );
        //save/cancel buttons
        JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.TRAILING ) );
        saveButton = new JButton( "Save" );
        saveButton.addActionListener( this );
        buttonPanel.add( saveButton );
        cancelButton = new JButton( "Cancel" );
        cancelButton.addActionListener( this );
        buttonPanel.add( cancelButton );
        //options are centered, buttons are on the bottom
        setLayout( new BorderLayout() );
        add( tabbedPane );
        add( buttonPanel, BorderLayout.SOUTH );

        reset();
    }

    private void makeGeneralPanel()
    {
        generalPanel = new JPanel();
        GridLayout layout = new GridLayout( 0, 2 );
        layout.setHgap( 10 );
        layout.setVgap( 3 );
        generalPanel.setLayout( layout );
        //make name
        name = new JTextField( 20 );
        name.setMaximumSize( name.getPreferredSize() );
        generalPanel.add(
                new JLabel( "Screen Name", SwingConstants.RIGHT )
        );
        generalPanel.add( name );
        fovSlider = new JSlider( 0, 180 );
        fovSlider.setMajorTickSpacing( 45 );
        fovSlider.setMinorTickSpacing( 10 );
        fovSlider.setPaintTicks( true );
        fovSlider.setPaintLabels( true );
        generalPanel.add(
                new JLabel( "Field of view (in degrees)", SwingConstants.RIGHT )
        );
        generalPanel.add( fovSlider );
        /* Slider values are set up so that mouseRotateMaxStep =
                DEFAULT * 2^(value / 100).
        */
        mouseSensitivitySlider = new JSlider( -200, 200 );
        mouseSensitivitySlider.setMajorTickSpacing( 100 );
        mouseSensitivitySlider.setPaintTicks( true );
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put( new Integer( -200 ), new JLabel( "Low" ) );
        labelTable.put( new Integer( 0 ), new JLabel( "Default" ) );
        labelTable.put( new Integer( 200 ), new JLabel( "High" ) );
        mouseSensitivitySlider.setLabelTable( labelTable );
        mouseSensitivitySlider.setPaintLabels( true );
        generalPanel.add(
                new JLabel( "Mouse sensitivity", SwingConstants.RIGHT )
        );
        generalPanel.add( mouseSensitivitySlider );
    }

    //used by Lasertag3D when the player enters a name at the beginning
    public void resetName()
    {
        name.setText( Lasertag3DSettings.playerName );
        saveSettings( true );
    }

    //used by GameStarterNetworker when the server gives it a password
    public void resetPassword()
    {
        saveSettings( true );
    }

    private void makeKeyPanel()
    {
        JPanel centerKeyPanel = new JPanel();
        GridLayout layout = new GridLayout( 0, 2 );
        layout.setHgap( 10 );
        layout.setVgap( 3 );
        centerKeyPanel.setLayout( layout );
        keyBoxes = new JComboBox[ lt3dKeys.length ];
        keyLabels = new JLabel[ lt3dKeys.length ];
        for( int i = 0; i < lt3dKeys.length; i++ ) {
            keyLabels[ i ] = new JLabel( lt3dKeys[ i ], SwingConstants.RIGHT );
            centerKeyPanel.add( keyLabels[ i ] );
            keyBoxes[ i ] = new JComboBox( AWTKeyNames );
            keyBoxes[ i ].setEditable( false );
            centerKeyPanel.add( keyBoxes[ i ] );
        }
        JLabel mouseControlLabel = new JLabel( "The rotations are also " +
                "controlled by the middle and right mouse buttons." );
        keyPanel = new JPanel();
        keyPanel.add( centerKeyPanel );
        keyPanel.add( mouseControlLabel, BorderLayout.SOUTH );
    }

    //finds the index of awtKeyCode in AWTKeys[]
    private int findIndexOf( int awtKeyCode )
    {
        for( int i = 0; i < AWTKeys.length; i++ ) {
            if( AWTKeys[ i ] == awtKeyCode ) return i;
        }
        return -1;//not found
    }

    private void makePortPanel()
    {
        portPanel = new JPanel();
        portPanel.setLayout( new BoxLayout( portPanel, BoxLayout.PAGE_AXIS ) );

        portWarning = new JEditorPane( "text/html", WARNING_TEXT );
        portWarning.setEditable( false );
        SafeHyperlinkListener.addTo( portWarning );
        portWarning.setMaximumSize( portWarning.getPreferredSize() );
        portPanel.add( portWarning );

        JPanel panel1 = new JPanel();
        panel1.setLayout( new FlowLayout( FlowLayout.RIGHT ) );
        gameHostingEnabledCheckBox = new JCheckBox( "Enable Game Hosting" );
        gameHostingEnabledCheckBox.addActionListener( this );
        panel1.add( gameHostingEnabledCheckBox );
        portPanel.add( panel1 );

        JPanel panel2 = new JPanel();
        panel2.setLayout( new FlowLayout( FlowLayout.RIGHT ) );
        serverPortLabel = new JLabel( "Server port: ", SwingConstants.RIGHT );
        panel2.add( serverPortLabel );
        serverPortSpinner = new JSpinner( new SpinnerNumberModel(
                Lasertag3DSettings.serverPort, 0, 65535, 1 )
        );
        panel2.add( serverPortSpinner );
        panel2.setAlignmentX( Component.LEFT_ALIGNMENT );
        portPanel.add( panel2 );

        Dimension fillerSize = new Dimension( 1, Short.MAX_VALUE );
        Dimension noSize = new Dimension( 1, 1 );
        portPanel.add( new Box.Filler( noSize, fillerSize, fillerSize ) );
    }


    /** Causes this SettingsPanel to discard any unsaved user input.
    **/
    public synchronized void reset()
    {
        name.setText( Lasertag3DSettings.playerName );
        fovSlider.setValue( Lasertag3DSettings.fieldOfView );
        double sensitivityValue = 100 * Math.log(
                Lasertag3DSettings.mouseRotateMaxStep /
                Lasertag3DSettings.MOUSE_ROTATE_MAX_STEP_DEFAULT ) /
                Math.log( 2 );
        mouseSensitivitySlider.setValue( (int) Math.round( sensitivityValue ) );
        for( int i = 0; i < lt3dKeys.length; i++ ) {
            int currentValue = Lasertag3DSettings.getKeyEventCodeFromLT3DCode( i );
            keyBoxes[ i ].setSelectedIndex( findIndexOf( currentValue ) );
        }
        gameHostingEnabledCheckBox.setSelected( Lasertag3DSettings.gameHostingEnabled );
        serverPortLabel.setEnabled( Lasertag3DSettings.gameHostingEnabled );
        serverPortSpinner.setEnabled( Lasertag3DSettings.gameHostingEnabled );
        serverPortSpinner.setValue( Lasertag3DSettings.serverPort );

        tabbedPane.setSelectedIndex( 0 );
    }


//Event processing

    public synchronized void actionPerformed( ActionEvent e )
    {
        if( e.getSource() == saveButton ) {
            saveSettings();
            reset();
            main.returnToMenu();
        }
        else if( e.getSource() == cancelButton ) {
            reset();
            main.returnToMenu();
        }
        else if( e.getSource() == gameHostingEnabledCheckBox ) {
            serverPortLabel.setEnabled( gameHostingEnabledCheckBox.isSelected() );
            serverPortSpinner.setEnabled( gameHostingEnabledCheckBox.isSelected() );
        }
        else {
            System.err.println( "In SettingsPanel: event from unknown " +
                    "source: " + e.getSource()
            );
        }
    }

    private void saveSettings()
    {
        saveSettings( false );
    }

    //if quiet is true, then the user will not see any error messages
    private void saveSettings( boolean quiet )
    {
        setGeneral();
        setKeys();
        setPort();
        writeSettings( quiet );
    }

    private void setGeneral()
    {
        setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );

        String playerName = name.getText();
        if( "".equals( playerName ) ) playerName = "unset";
        playerName = playerName.replaceAll( "\\s", "_" );
        if( playerName.length() > 20 ) {//trim to 20 characters
            playerName = playerName.substring( 0, 20 );
        }
//easter egg
        if( "ignore".equalsIgnoreCase( playerName ) ) {
            JOptionPane.showMessageDialog( this, "Please ignore this notice.",
                    "Please Ignore This Notice",
                    JOptionPane.PLAIN_MESSAGE
            );
        }
//end easter egg
        Lasertag3DSettings.playerName = playerName;
        name.setText( Lasertag3DSettings.playerName );

        main.setFieldOfView( fovSlider.getValue() );
        /* Slider values are set up so that mouseRotateMaxStep =
                DEFAULT * 2^(value / 100).
        */
        Lasertag3DSettings.mouseRotateMaxStep = (float)
                (Lasertag3DSettings.MOUSE_ROTATE_MAX_STEP_DEFAULT *
                Math.pow( 2, mouseSensitivitySlider.getValue() / 100.0 ));
        setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
    }

    private void setKeys()
    {
        setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
        synchronized( Lasertag3DSettings.keyLock ) {
            //store old key codes
            int oldKeyCodes[] = new int[ lt3dKeys.length ];
            for( int i = 0; i < lt3dKeys.length; i++ ) {
                oldKeyCodes[ i ] = Lasertag3DSettings.getKeyEventCodeFromLT3DCode( i );
            }
            Lasertag3DSettings.clearKeyCodeMappings();
            for( int i = 0; i < lt3dKeys.length; i++ ) {
                int value = keyBoxes[ i ].getSelectedIndex();
                int keyCode;
                if( value == -1 ) keyCode = oldKeyCodes[ i ];
                else keyCode = AWTKeys[ value ];
                Lasertag3DSettings.associateKeyCode( keyCode, i );
            }
        }
        setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
    }

    private void setPort()
    {
        setCursor( Cursor.getPredefinedCursor( Cursor.WAIT_CURSOR ) );
        Lasertag3DSettings.gameHostingEnabled =
                gameHostingEnabledCheckBox.isSelected();
        int newServerPort = ((Integer) serverPortSpinner.getValue()).intValue();
        Lasertag3DSettings.serverPort = newServerPort;
        setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) );
    }


    //if quiet is true, then the user will not see any error messages
    private void writeSettings( boolean quiet )
    {
        try {
            File file = new File( Lasertag3D.SETTINGS_FILE );
            Lasertag3DSettings.save( file, NetworkClient.NUM_KEY_CODES );

            if( quiet == false ) {
                //success
                JOptionPane.showMessageDialog( this,
                        "Your settings have been saved successfully.",
                        "Settings Saved Successfully",
                        JOptionPane.INFORMATION_MESSAGE
                );
            }
        }

        catch( IllegalArgumentException exc ) {
            if( quiet == false ) {
                JOptionPane.showMessageDialog( this,
                        "The settings file does not reside on this computer " +
                        " and so could not be modified.  Your settings have not been saved.",
                        "Settings Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        catch( IOException exc ) {
            System.err.println( "Error: could not save to settings file: " +
                    exc.getMessage()
            );
            exc.printStackTrace();
            if( quiet == false ) {
                JOptionPane.showMessageDialog( this,
                        "There was an error while saving your settings.",
                        "Settings Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
        catch( Exception exc ) {
            System.err.println( "Error: could not save to settings file: " +
                    exc.getMessage()
            );
            exc.printStackTrace();
            if( quiet == false ) {
                JOptionPane.showMessageDialog( this,
                        "There was an error while saving your settings.",
                        "Settings Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }
}
