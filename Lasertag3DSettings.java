//Lasertag3DSettings.java (5/16/2011)
//maintains game settings for Lasertag3D, e.g., gravity on/off
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d;

import java.util.*;
import java.io.*;
import java.net.*;

import com.mattweidner.lt3d.net.NetworkClient;


public class Lasertag3DSettings {

    //constants
//    public final static String SERVER_ADDR = "http://www.mattweidner.com/Lasertag3D/server.php";
//    public final static String SERVER_ADDR = "http://localhost/Lasertag3D/server2.php";
    public final static String SERVER_ADDR = "http://www.mattweidner.com/Lasertag3D/server2.php";
    public final static int frameDelay = 10;//time between frames, in ms
    public final static int framesPerScoreUpdate = 50;/* number of frames to wait
            before sending out a score update to all clients
    */
    public final static int panelUpdateDelay = 1000;//update interval in GameStarterPanel

    //vars
    public static String playerName;
    public static int password;
    public static boolean localSession;//if true, no networking is needed
    public static boolean gravityOn;
    public static int fieldOfView;//field of view, in degrees; 0 < fov < 180
    public static float mouseRotateMaxStep;/* max angular speed for
            mouse-movement-controlled rotations
    */
    public final static float MOUSE_ROTATE_MAX_STEP_DEFAULT = 0.8f;
    public final static float MOUSE_ROTATE_SMOOTHNESS = /*0.3f*/0;/* fraction of
            the mouse's new position which comes from its old position;
            higher values (<1.0) lead to more gradually-ending mouse
            rotations
    */

    /** The player can create (and thus host) multiplayer games iff this
    * is true.
    **/
    public static boolean gameHostingEnabled;
    /** This port value should NOT be used by NetworkServer's; they should get
    * port values via their constructors.
    **/
    public static int serverPort;

    //playSound should not be modified directly; use the methods in SoundHandler instead
    public static boolean playSound;

    private static Hashtable<Integer, Integer> keyCodeMappings;
    static {
        keyCodeMappings = new Hashtable<Integer, Integer>();
    }
    public static Object keyLock = new Object();/* If a method is iteratively
            filling the key table with values, synchronize on this object
            from before you call clear() until after the last key code is
            associated.  Other threads will not be able to query the key
            code table while this object is synchronized on.
    */

    public static void clearKeyCodeMappings()
    {
        keyCodeMappings.clear();
    }


    /** associates a java.awt.event.KeyEvent keycode with a
    * com.mattweidner.lt3d.net.NetworkClient keycode
    * Note that one KeyEvent key code can NOT be associated with multiple lt3d
    * key codes.
    **/
    public static void associateKeyCode( int keyEventKeyCode, int lt3dKeyCode )
            throws IllegalArgumentException
    {
        if( lt3dKeyCode == -1 ) {
            throw new IllegalArgumentException( "keyCode is -1:" +
                    " this value is reserved as the error code."
            );
        }
        if( lt3dKeyCode >= NetworkClient.NUM_KEY_CODES ) {
            throw new IllegalArgumentException( "too many keys" );
        }
        keyCodeMappings.put( keyEventKeyCode, lt3dKeyCode );
    }

    //a return of -1 indicates the key code does not match anything
    public static int getLT3DCodeFromKeyEventCode( int keyEventKeyCode )
    {
        Integer value = null;
        synchronized( keyLock ) {
            value = keyCodeMappings.get( new Integer( keyEventKeyCode ) );
        }
        if( value == null ) return -1;
        //else
        return value.intValue();
    }

    //a return of -1 indicates the key code does not match anything
    //WARNING: this method is inefficient
    public static int getKeyEventCodeFromLT3DCode( int lt3dKeyCode )
    {
        synchronized( keyLock ) {
            //get a list of the keys
            Enumeration<Integer> hashKeyEnum = keyCodeMappings.keys();
            /* test each key to see if its associated value matches lt3dKeyCode;
                    if so, return that key.
            */
            while( hashKeyEnum.hasMoreElements() ) {
                Integer key = hashKeyEnum.nextElement();
                Integer value = keyCodeMappings.get( key );
                if( value.intValue() == lt3dKeyCode ) {
                    return key.intValue();
                }
            }
        }
        return -1;
    }


//methods for loading and saving settings

    /** Settings file format:
    * line 1: name
    * line 2: "p" + password
    * line 3: gameHostingEnabled
    * line 4: serverPort
    * line 5: playSound
    * line 6: fieldOfView
    * line 7: mouseRotateMaxStep
    * line 8+: key_code lt3d_key_code
    * where keycode identifies a KeyEvent key code and lt3d_key_code identifies
    * an lt3d key code.  Both values are in their numerical form.
    * Last line: 'e'
    * Blank lines are permissible.  Comment lines start with '#' as their first character.
    **/
    public static void load( File file ) throws IOException
    {
        BufferedReader reader = new BufferedReader(
                new FileReader( file )
        );
        try { load( reader ); }
        finally {
            try { if( reader != null ) reader.close(); }
            catch( IOException exc ) {}
        }
    }

    public static void load( URL url ) throws IOException
    {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader( url.openStream() )
        );
        try { load( reader ); }
        finally {
            try { if( reader != null ) reader.close(); }
            catch( IOException exc ) {}
        }
    }

    public static void load( BufferedReader reader ) throws IOException
    {
        String line = null;

        //mark everything as unset
        boolean isPasswordSet = false;
        boolean isGameHostingEnabledSet = false;
        boolean isPlaySoundSet = false;
        boolean isInKeys = false;
        boolean tryLineAgain = false;
        playerName = null;
        fieldOfView = -1;
        mouseRotateMaxStep = -1;
        serverPort = -1;

        while( true ) {
            if( tryLineAgain ) {
                tryLineAgain = false;
                //don't get the next line yet
            }
            else {
                line = reader.readLine();
            }

            if( line == null || "e".equalsIgnoreCase( line.trim() ) ) break;
            line = line.trim();
            if( "".equals( line ) || line.startsWith( "#" ) ) continue;
            //else
            if( playerName == null ) {
                playerName = line;
            }
            else if( isPasswordSet == false ) {
                if( line.startsWith( "p" ) ) {
                    password = Integer.parseInt( line.substring( 1 ) );
                }
                else {//password hasn't been added to the file yet
                    password = 0;
                    tryLineAgain = true;
                }
                isPasswordSet = true;
            }
            else if( isGameHostingEnabledSet == false ) {
                if( "true".equalsIgnoreCase( line ) ||
                        "false".equalsIgnoreCase( line ) ) {
                    gameHostingEnabled = Boolean.parseBoolean( line );
                }
                else {
                    //gameHostingEnabled has not been added to the file yet
                    gameHostingEnabled = false;
                    tryLineAgain = true;
                }
                isGameHostingEnabledSet = true;
            }
            else if( serverPort == -1 ) {
                serverPort = Integer.parseInt( line );
                if( serverPort < 0 || serverPort > 65535 ) {
                    throw new IllegalArgumentException( "bad serverPort value" );
                }
            }
            else if( isPlaySoundSet == false ) {
                if( "true".equalsIgnoreCase( line ) ||
                        "false".equalsIgnoreCase( line ) ) {
                    playSound = Boolean.parseBoolean( line );
                    isPlaySoundSet = true;
                }
                else {
                    /* This line is the serverPort value and the last line was
                            the port value, which is left over from a previous
                            version.
                    */
                    serverPort = Integer.parseInt( line );
                    if( serverPort < 0 || serverPort > 65535 ) {
                        throw new IllegalArgumentException( "bad serverPort value" );
                    }
                    //now the next line will be processed as the playSound value
                }
            }
            else if( fieldOfView == -1 ) {
                fieldOfView = Integer.parseInt( line );
                if( fieldOfView <= 0 ) fieldOfView = 1;
                if( fieldOfView >= 180 ) fieldOfView = 179;
            }
            else if( mouseRotateMaxStep == -1 ) {
                try {
                    mouseRotateMaxStep = Float.parseFloat( line );
                }
                catch( NumberFormatException exc ) {
                    //set to the default value
                    mouseRotateMaxStep = MOUSE_ROTATE_MAX_STEP_DEFAULT;
                    tryLineAgain = true;
                }
            }
            //add new lines here
            //remember to add the new lines if they don't exist yet (don't error)
            //remember to make changes in default_settings.txt
            else if( isInKeys == false ) {
                if( "keys".equalsIgnoreCase( line ) == false ) {
                    throw new IllegalArgumentException( "too many lines before \"keys\"" );
                }
                //else
                isInKeys = true;
            }
            else {
                String tokens[] = line.split( "\\s+" );
                associateKeyCode( Integer.parseInt( tokens[ 0 ] ),
                        Integer.parseInt( tokens[ 1 ] )
                );
            }
        }
        reader.close();
        if( mouseRotateMaxStep == -1 ) {//some vars not set
            throw new IllegalArgumentException( "some variables were not set" );
        }
    }

    public static void save( File file, int numKeyCodes )
            throws IOException, IllegalArgumentException
    {
        BufferedWriter writer = new BufferedWriter( new FileWriter( file ) );
        try {
            //write name
            writer.write( "#name" );
            writer.newLine();
            writer.write( playerName );
            writer.newLine();
            //write password
            writer.write( "#password" );
            writer.newLine();
            writer.write( "p" + password );
            writer.newLine();
            //write gameHostingEnabled
            writer.write( "#game hosting enabled" );
            writer.newLine();
            writer.write( "" + gameHostingEnabled );
            writer.newLine();
            //write server port
            writer.write( "#server port" );
            writer.newLine();
            writer.write( "" + serverPort );
            writer.newLine();
            //write play sound
            writer.write( "#play sound" );
            writer.newLine();
            writer.write( "" + playSound );
            writer.newLine();
            //write fieldOfView
            writer.write( "#field of view, in degrees" );
            writer.newLine();
            writer.write( "" + fieldOfView );
            writer.newLine();
            //write mouseRotateMaxStep
            writer.write( "#mouseRotateMaxStep" );
            writer.newLine();
            writer.write( "" + mouseRotateMaxStep );
            writer.newLine();
            //write key bindings
            writer.write( "#key bindings" );
            writer.newLine();
            writer.write( "keys" );
            writer.newLine();
            for( int i = 0; i < numKeyCodes; i++ ) {
                writer.write( getKeyEventCodeFromLT3DCode( i ) +
                        " " + i
                );
                writer.newLine();
            }
            //write 'e'
            writer.newLine();
            writer.write( "e" );
        }
        finally {
            try { if( writer != null ) writer.close(); }
            catch( IOException exc ) {}
        }
    }
}
