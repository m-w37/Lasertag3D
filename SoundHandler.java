//SoundHandler.java (8/20/2011)
//handles sounds for Lasertag3D
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d;


import javax.media.j3d.*;
import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.vecmath.Point3f;

import javax.sound.sampled.*;

import java.io.*;
import java.net.*;


public class SoundHandler {

    private final static float[] DISTANCES = { 1, 2,    4,     8,      16,      32 };
    private final static float[] GAINS =     { 1, 0.5f, 0.25f, 0.125f, 0.0625f, 0 };

    private int playerId;
    private AudioDevice audioDevice;
    private MediaContainer goodSoundMC, badSoundMC;
    private PointSound goodSound, badSound;
//    private PointSound goodSounds[], badSounds[];//indexed by playerId
//    private TransformGroup parents[];//indexed by playerId

    public SoundHandler( URL goodSoundURL, URL badSoundURL,
            SimpleUniverse universe )
    {
        //once JOALMixer is implemented, this should be changed
        System.setProperty( "j3d.audiodevice",
                "com.sun.j3d.audioengines.javasound.JavaSoundMixer"
        );
        audioDevice = universe.getViewer().createAudioDevice();
        if( audioDevice == null ) return;
        try {
            goodSoundMC = new MediaContainer( goodSoundURL );
            goodSoundMC.setCacheEnable( true );
        }
        catch( SoundException exc ) {
            System.err.println( "Error: while loading goodSound: " + exc.getMessage() );
            exc.printStackTrace();
        }
        try {
            badSoundMC = new MediaContainer( badSoundURL );
            badSoundMC.setCacheEnable( true );
        }
        catch( SoundException exc ) {
            System.err.println( "Error: while loading badSound: " + exc.getMessage() );
            exc.printStackTrace();
        }
    }


    public void setPlaySound( boolean playSound )
    {
        if( Lasertag3DSettings.playSound && (playSound == false) ) {
            //stop the current sounds
/*            if( goodSounds != null && badSounds != null ) {
                for( int i = 0; i < goodSounds.length; i++ ) {
                    if( goodSoundMC != null ) goodSounds[ i ].setEnable( false );
                    if( badSoundMC != null ) badSounds[ i ].setEnable( false );
                }
            }
*/
            if( goodSoundMC != null && goodSound != null ) goodSound.setEnable( false );
            if( badSoundMC != null && badSound != null ) badSound.setEnable( false );
        }

        Lasertag3DSettings.playSound = playSound;
    }

    public boolean getPlaySound() { return Lasertag3DSettings.playSound; }


    public void disable()
    {
        goodSoundMC = null;
        badSoundMC = null;
    }

    public void set( TransformGroup parents[], Bounds bounds, int playerId )
    {
        this.playerId = playerId;

        if( Lasertag3DSettings.localSession ) return;

/*        this.parents = parents;
        goodSounds = new PointSound[ parents.length ];
        badSounds = new PointSound[ parents.length ];
        for( int i = 0; i < parents.length; i++ ) {
            parents[ i ].setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
            if( goodSoundMC != null ) {
                goodSounds[ i ] = new PointSound( goodSoundMC, 1.0f, new Point3f( 0,0,0 ) );
                goodSounds[ i ].setDistanceGain( DISTANCES, GAINS );
                goodSounds[ i ].setSchedulingBounds( bounds );
                goodSounds[ i ].setCapability( PointSound.ALLOW_ENABLE_WRITE );
                goodSounds[ i ].setCapability( PointSound.ALLOW_POSITION_WRITE );
                if( i == playerId ) goodSounds[ i ].setPriority( 1.0f );
                else goodSounds[ i ].setPriority( 0.5f );
                parents[ i ].addChild( goodSounds[ i ] );
            }
            if( badSoundMC != null ) {
                badSounds[ i ] = new PointSound( badSoundMC, 1.0f, new Point3f( 0,0,0 ) );
                badSounds[ i ].setDistanceGain( DISTANCES, GAINS );
                badSounds[ i ].setSchedulingBounds( bounds );
                badSounds[ i ].setCapability( PointSound.ALLOW_ENABLE_WRITE );
                badSounds[ i ].setCapability( PointSound.ALLOW_POSITION_WRITE );
                if( i == playerId ) badSounds[ i ].setPriority( 1.0f );
                else badSounds[ i ].setPriority( 0.5f );
                parents[ i ].addChild( badSounds[ i ] );
            }
        }
*/
        if( goodSoundMC != null ) {
            goodSound = new PointSound( goodSoundMC, 1.0f, new Point3f( 0,0,0 ) );
            goodSound.setSchedulingBounds( bounds );
            goodSound.setCapability( PointSound.ALLOW_ENABLE_WRITE );
            goodSound.setPriority( 1.0f );
            parents[ playerId ].addChild( goodSound );
        }

        if( badSoundMC != null ) {
            badSound = new PointSound( badSoundMC, 1.0f, new Point3f( 0,0,0 ) );
            badSound.setSchedulingBounds( bounds );
            badSound.setCapability( PointSound.ALLOW_ENABLE_WRITE );
            badSound.setPriority( 1.0f );
            parents[ playerId ].addChild( badSound );
        }
    }

    private Point3f p3fg = new Point3f();
    private Transform3D t3dg = new Transform3D();
    public void goodSound( int playerId )
    {
        if( Lasertag3DSettings.localSession ||
            goodSoundMC == null ||
            !Lasertag3DSettings.playSound
        ) return;

/*        goodSounds[ playerId ].setEnable( false );
        p3fg.set( 0, 0, 0 );
        parents[ playerId ].getTransform( t3dg );
        t3dg.transform( p3fg );
        goodSounds[ playerId ].setPosition( p3fg );
        goodSounds[ playerId ].setEnable( true );
*/
        if( playerId == this.playerId ) {
            goodSound.setEnable( false );
            goodSound.setEnable( true );
        }
    }

    private Point3f p3fb = new Point3f();
    private Transform3D t3db = new Transform3D();
    public void badSound( int playerId )
    {
        if( Lasertag3DSettings.localSession ||
            badSoundMC == null ||
            !Lasertag3DSettings.playSound
        ) return;

/*        badSounds[ playerId ].setEnable( false );
        p3fb.set( 0, 0, 0 );
        parents[ playerId ].getTransform( t3db );
        t3db.transform( p3fb );
        badSounds[ playerId ].setPosition( p3fb );
        badSounds[ playerId ].setEnable( true );
*/
        if( playerId == this.playerId ) {
            badSound.setEnable( false );
            badSound.setEnable( true );
        }
    }
}
