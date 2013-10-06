//SpriteBehavior.java (6/10/2011)
//handles sprite behavior
//this used to be an inner class in Lasertag3D.java; it was created in 3/2011

package com.mattweidner.lt3d.sprite;


import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.loaders.objectfile.ObjectFile;
import com.sun.j3d.utils.pickfast.behaviors.*;
import com.sun.j3d.utils.pickfast.*;
import com.sun.j3d.utils.universe.*;

import com.mattweidner.lt3d.net.NetworkClient;
import com.mattweidner.lt3d.Lasertag3DSettings;


public class SpriteBehavior extends Behavior {

    private Canvas3D canvas;
    private NetworkClient client;
    private Robot robot;
    private boolean isCapturingMouse;

    private WakeupCondition condition;
    boolean upPressed, downPressed, leftPressed, rightPressed, controlDown;
    private long upWhen, downWhen, leftWhen, rightWhen;

    public SpriteBehavior( Canvas3D canvas ) throws AWTException
    {
        this.canvas = canvas;
        robot = new Robot();
        setIsCapturingMouse( true );

        WakeupCriterion criteria[] = new WakeupCriterion[ 6 ];
        criteria[ 0 ] = new WakeupOnAWTEvent( KeyEvent.KEY_PRESSED );
        criteria[ 1 ] = new WakeupOnAWTEvent( KeyEvent.KEY_RELEASED );
        criteria[ 2 ] = new WakeupOnAWTEvent( MouseEvent.MOUSE_PRESSED );
        criteria[ 3 ] = new WakeupOnAWTEvent( MouseEvent.MOUSE_RELEASED );
        criteria[ 4 ] = new WakeupOnAWTEvent( MouseEvent.MOUSE_MOVED );
        criteria[ 5 ] = new WakeupOnAWTEvent( MouseEvent.MOUSE_EXITED );
        condition = new WakeupOr( criteria );
    }

    public void setNetworkClient( NetworkClient client )
    {
        this.client = client;
    }


    public void initialize()
    {
        wakeupOn( condition );
    }


    private final static Cursor blankCursor;
    static {
        //make the blank cursor
        Toolkit toolkit = Toolkit.getDefaultToolkit();
        blankCursor = toolkit.createCustomCursor(
                toolkit.createImage( new byte[ 0 ] ),
                new Point( 0, 0 ), "blank" );
    }

    private void setIsCapturingMouse( boolean isCapturingMouse )
    {
        if( isCapturingMouse == this.isCapturingMouse ) return;
        this.isCapturingMouse = isCapturingMouse;
        if( isCapturingMouse ) {
            if( blankCursor == null ) {
                
            }
            canvas.setCursor( blankCursor );
        }
        else {
            canvas.setCursor( Cursor.getDefaultCursor() );
        }
    }


    //vars used by processStimulus, globalized for efficiency
    private WakeupCriterion event;
    private AWTEvent awtEvents[];
    private KeyEvent keyEvent;
    private MouseEvent mouseEvent;
    private int id;

    public void processStimulus( Enumeration criteria )
    {
        while( criteria.hasMoreElements() ) {
            event = (WakeupCriterion) criteria.nextElement();
            if( event instanceof WakeupOnAWTEvent ) {
                if( client == null ) {
                    System.err.println( "Error: SpriteBehavior: client not set" );
                    continue;
                }

                awtEvents = ((WakeupOnAWTEvent) event).getAWTEvent();
                for( int i = 0; i < awtEvents.length; i++ ) {
                    id = awtEvents[ i ].getID();

                    if( id == KeyEvent.KEY_PRESSED ) {
                        keyEvent = (KeyEvent) awtEvents[ i ];
                        int keyCode = keyEvent.getKeyCode();
                        if( keyCode == KeyEvent.VK_ESCAPE ) {
                            setIsCapturingMouse( !isCapturingMouse );
                        }
                        int lt3dKeyCode = Lasertag3DSettings.
                                getLT3DCodeFromKeyEventCode( keyCode );
                        if( lt3dKeyCode != -1 ) {
                            client.keyPressed( lt3dKeyCode );
                        }
                    }

                    else if( id == KeyEvent.KEY_RELEASED ) {
                        keyEvent = (KeyEvent) awtEvents[ i ];
                        int keyCode = keyEvent.getKeyCode();
                        int lt3dKeyCode = Lasertag3DSettings.
                                getLT3DCodeFromKeyEventCode( keyCode );
                        if( lt3dKeyCode != -1 ) {
                            client.keyReleased( lt3dKeyCode );
                        }
                    }

                    else if( id == MouseEvent.MOUSE_PRESSED ) {
                        mouseEvent = (MouseEvent) awtEvents[ i ];
                        if( mouseEvent.getButton() == MouseEvent.BUTTON1 ) {
                            setIsCapturingMouse( true );
                            client.fireShot();
                        }
                        /* ROTATE_CCW and ROTATE_CW have key mappings, but
                                they can also be controlled by the mouse
                                middle and mouse right buttons, respectively.
                        */
                        else if( mouseEvent.getButton() == MouseEvent.BUTTON2 ) {
                            client.keyPressed( NetworkClient.ROTATE_CCW );
                        }
                        else if( mouseEvent.getButton() == MouseEvent.BUTTON3 ) {
                            client.keyPressed( NetworkClient.ROTATE_CW );
                        }
                    }

                    else if( id == MouseEvent.MOUSE_RELEASED ) {
                        mouseEvent = (MouseEvent) awtEvents[ i ];
                        if( mouseEvent.getButton() == MouseEvent.BUTTON2 ) {
                            client.keyReleased( NetworkClient.ROTATE_CCW );
                        }
                        else if( mouseEvent.getButton() == MouseEvent.BUTTON3 ) {
                            client.keyReleased( NetworkClient.ROTATE_CW );
                        }
                    }

                    else if( id == MouseEvent.MOUSE_MOVED ) {
                        if( isCapturingMouse == false ) continue;

                        mouseEvent = (MouseEvent) awtEvents[ i ];

                        int w = canvas.getWidth();
                        int h = canvas.getHeight();
                        float xScaled = (mouseEvent.getX() - w/2) / 300.0f;
                        float yScaled = (mouseEvent.getY() - h/2) / 300.0f;
                        if( xScaled != 0 && yScaled != 0 ) {
                            client.rotate( xScaled, yScaled );
                            captureMouse( mouseEvent.getX(), mouseEvent.getY() );
                        }
                    }

                    else if( id == MouseEvent.MOUSE_EXITED ) {
                        mouseEvent = (MouseEvent) awtEvents[ i ];
                        captureMouse( mouseEvent.getX(), mouseEvent.getY() );
                    }
                }
            }
        }
        wakeupOn( condition );
    }

    private void captureMouse( int mouseX, int mouseY )
    {
        if( isCapturingMouse ) {
            Point corner = canvas.getLocationOnScreen();
            int centerX = corner.x + canvas.getWidth() / 2;
            int centerY = corner.y + canvas.getHeight() / 2;
            mouseX += corner.x;
            mouseY += corner.y;
            //average the current and target positions, for smoothness
            int newX = (int) Math.round(
                    Lasertag3DSettings.MOUSE_ROTATE_SMOOTHNESS * mouseX +
                    (1.0 - Lasertag3DSettings.MOUSE_ROTATE_SMOOTHNESS) * centerX );
            int newY = (int) Math.round(
                    Lasertag3DSettings.MOUSE_ROTATE_SMOOTHNESS * mouseY +
                    (1.0 - Lasertag3DSettings.MOUSE_ROTATE_SMOOTHNESS) * centerY );
            //prevent perpetual motion
            if( newX == mouseX ) {
                newX = centerX;
            }
            if( newY == mouseY ) {
                newY = centerY;
            }
            //keep the mouse within the canvas
            if( newX <= corner.x ) {
                newX = corner.x + 1;
            }
            if( newX >= corner.x + canvas.getWidth() ) {
                newX = corner.x + canvas.getWidth() - 1;
            }
            if( newY <= corner.y ) {
                newY = corner.y + 1;
            }
            if( newY >= corner.y + canvas.getHeight() ) {
                newY = corner.y + canvas.getHeight() - 1;
            }
            //System.out.println( "old: " + mouseX + "," + mouseY + "; center: " +
            //        centerX + "," + centerY + "; new: " + newX + "," + newY );
            robot.mouseMove( newX, newY );
            //robot.mouseMove( centerX, centerY );
        }
    }
}
