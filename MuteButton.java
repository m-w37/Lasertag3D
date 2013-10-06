//MuteButton.java (8/25/2011)
//creates a mute button
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class MuteButton extends JPanel {

    private SoundHandler handler;
    private Image soundOn, soundOff;

    public MuteButton( SoundHandler soundHandler, Image soundOn, Image soundOff,
            int size )
    {
        this.handler = soundHandler;
        this.soundOn = soundOn;
        this.soundOff = soundOff;

        Dimension dim = new Dimension( size, size );
        setPreferredSize( dim );
        setMinimumSize( dim );
        setMaximumSize( dim );

        setFocusable( true );
        addMouseListener( new MouseListener() {
                public void mousePressed( MouseEvent e )
                {
                    handler.setPlaySound( !handler.getPlaySound() );
                    repaint();
                }
                public void mouseReleased( MouseEvent e ) {}
                public void mouseEntered( MouseEvent e ) {}
                public void mouseExited( MouseEvent e ) {}
                public void mouseClicked( MouseEvent e ) {}
            }
        );
    }

    public void paintComponent( Graphics g )
    {
        super.paintComponent( g );

        if( handler.getPlaySound() ) {
            g.drawImage( soundOn, 0, 0, getWidth(), getHeight(), this );
        }
        else g.drawImage( soundOff, 0, 0, getWidth(), getHeight(), this );
    }
}
