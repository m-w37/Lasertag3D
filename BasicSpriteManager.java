//BasicSpriteManager.java (2/7/2011)
//client side sprite manager for lt3d
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d.sprite;


import javax.media.j3d.*;
import javax.vecmath.*;
import java.util.*;

import com.mattweidner.lt3d.net.NetworkListener;
import com.mattweidner.lt3d.SoundHandler;


public class BasicSpriteManager {

    private BasicSprite sprites[];
    private int hitsMade[];
    private int hitsTaken[];
    private int playerId;//id of this player

/*    private Switch dotSwitch;
    private BitSet bitSet;
*/

    private BranchGroup sceneBG;
    private SoundHandler soundHandler;
    private NetworkListener listener;

    /* The indexes of playersHit and shooters match up (e.g., shooters[ 0 ] hit
        playersHit[ 0 ]).
    */
    private byte playersHit[];//ids of who was hit lately
    private byte shooters[];//ids of who hit someone lately
    private Vector3f dotPositions[];//points where a laser beam hit lately


    public BasicSpriteManager( BranchGroup sceneBG, Geometry spriteGeometry,
            Appearance spriteAppearance, Vector3f startPositions[],
            Matrix3f startRotations[], Geometry dotGeometry,
            Appearance dotAppearance, TransformGroup viewerTG,
            NetworkListener listener, int playerId, String names[],
            SoundHandler soundHandler, Bounds sceneBounds )
    {
        //init sceneBG
        sceneBG.setCapability( Group.ALLOW_CHILDREN_WRITE );//let them be removed
        this.sceneBG = sceneBG;

        //init PlayerSprites
        sprites = new BasicSprite[ startPositions.length ];
        TransformGroup soundParents[] = new TransformGroup[ sprites.length ];
        for( int i = 0; i < startPositions.length; i++ ) {
            sprites[ i ] = new BasicSprite( startPositions[ i ],
                    startRotations[ i ], spriteGeometry, spriteAppearance, i,
                    names[ i ], (playerId == i)
            );
            if( i == playerId ) sprites[ i ].setViewerTG( viewerTG );
            sprites[ i ].attachTo( sceneBG );
            soundParents[ i ] = sprites[ i ].getSoundParent();
        }

        //init soundHandler
        soundHandler.set( soundParents, sceneBounds, playerId );
        this.soundHandler = soundHandler;

        hitsMade = new int[ sprites.length ];
        hitsTaken = new int[ sprites.length ];

        playersHit = new byte[ 0 ];
        shooters = new byte[ 0 ];
        dotPositions = new Vector3f[ 0 ];

/*        //init dots
        bitSet = new BitSet( sprites.length );
        dotSwitch = new Switch( Switch.CHILD_MASK, bitSet );
        dotSwitch.setPickable( false );
        dotSwitch.setCapability( Switch.ALLOW_SWITCH_WRITE );
        Vector3f axis = new Vector3f( 0, 1, 0 );
        for( int i = 0; i < sprites.length; i++ ) {
            OrientedShape3D dot = new OrientedShape3D( dotGeometry,
                    dotAppearance, OrientedShape3D.ROTATE_ABOUT_AXIS, axis
            );
            TransformGroup dotTG = new TransformGroup();
            dotTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
            dotTG.addChild( dot );
            dotSwitch.insertChild( dotTG, i );
        }
        sceneBG.addChild( dotSwitch );
*/

        this.playerId = playerId;
        this.listener = listener;
    }

    private Transform3D spareT3D;
    public void set( Vector3f playerPositions[], Matrix3f playerRotations[],
            Vector3f dotPositions[] )
    {
/*        //clear old dots
        bitSet.clear();
        dotSwitch.setChildMask( bitSet );
*/
        //move sprites
        for( int i = 0; i < sprites.length; i++ ) {
            sprites[ i ].set( playerPositions[ i ], playerRotations[ i ] );
        }
/*        //show dots
        this.dotPositions = (Vector3f[]) dotPositions.clone();
        for( int i = 0; i < dotPositions.length; i++ ) {
            TransformGroup dotTG = (TransformGroup) dotSwitch.getChild( i );
            spareT3D.set( dotPositions[ i ] );
            dotTG.setTransform( spareT3D );
            bitSet.set( i );
        }
        dotSwitch.setChildMask( bitSet );
*/
    }

    public void setHits( byte playersHit[], byte shooters[] )
    {
        //report hits
        this.playersHit = (byte[]) playersHit.clone();
        this.shooters = (byte[]) shooters.clone();
        for( int i = 0; i < playersHit.length; i++ ) {
            hitsTaken[ playersHit[ i ] ]++;
            hitsMade[ shooters[ i ] ]++;
            soundHandler.goodSound( shooters[ i ] );
            soundHandler.badSound( playersHit[ i ] );
            if( playersHit[ i ] == playerId ) {
                listener.thisWasHit( shooters[ i ] );
            }
            if( shooters[ i ] == playerId ) {
                listener.thisDidHit( playersHit[ i ] );
            }
        }
    }


    public BasicSprite[] getBasicSprites() { return sprites; }


    public boolean isPlayerInGame( int id )
    {
        return sprites[ id ].isInGame();
    }


    public void playerLeft( int playerId )
    {
        sprites[ playerId ].leave();
        sprites[ playerId ].removeFrom( sceneBG );
    }


    public int[] getScores()
    {
        int scores[] = new int[ sprites.length ];
        for( int i = 0; i < scores.length; i++ ) {
            scores[ i ] += hitsMade[ i ] * SpriteManager.MADE_HIT_BONUS;
            scores[ i ] += hitsTaken[ i ] * SpriteManager.TOOK_HIT_LOSS;
            if( sprites[ i ].isInGame() == false ) scores[ i ] = 0;//player left
        }
        return scores;
    }


    public byte[] getPlayersHit()
    {
        return (byte[]) playersHit.clone();
    }

    public byte[] getShooters()
    {
        return (byte[]) shooters.clone();
    }


    public Vector3f[] getDotPositions()
    {
        return (Vector3f[]) dotPositions.clone();
    }
}
