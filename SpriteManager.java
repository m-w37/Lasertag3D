//SpriteManager.java (7/19/2010)
//handles sprites and inter-sprite interactions (firing of laser beams)
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.sprite;


import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.pickfast.*;

import java.util.*;

import com.mattweidner.lt3d.scene.GameSceneLoader;
import com.mattweidner.lt3d.net.NetworkListener;
import com.mattweidner.lt3d.SoundHandler;


public class SpriteManager {

    public final static int TEXTURE_OPAQUE = 0;
    public final static int TEXTURE_TRANSPARENT = 1;

    public final static int FRAMES_BETWEEN_SHOTS = 50;

    public final static int MADE_HIT_BONUS = 3;
    public final static int TOOK_HIT_LOSS = -1;

    public final static double REPICK_STEP = 0.3;
    public final static float DOT_FROM_WALL = 0.02f;


    private PlayerSprite playerSprites[];
    private int playerId;
    private int lastShots[];
    private int numShots[];
    private int hitsMade[];
    private int hitsTaken[];
    private boolean isScoresChanged;/* Whether or not any scores changed since the
            last time getScores() was called
    */

/*    private Switch dotSwitch;
    private BitSet bitSet;
*/

    private ArrayList<Integer> playersHit;//ids of who was hit lately
    private ArrayList<Integer> shooters;//ids of who shot; matches with playersHit
    private ArrayList<Point3f> dotPositions;//where a laser hit lately

    private NetworkListener listener;
    private BranchGroup sceneBG;
    private SoundHandler soundHandler;
    private PickTool pickTool;
    private Point3f laserPosition;
    
    private Transform3D spareT3D, secondSpareT3D;
    private Vector3f spareVec, spareV3f2;
    private Point3f spareP3f;
    private Point3d spareP3d;
    private Vector3d spareV3d, spareV3d2;

    /* If playerId is not an actual player id, no error will be generated;
        the user will just not be attached to any sprite.
    */
    public SpriteManager( BranchGroup sceneBG, Geometry playerSpriteGeometry,
            Appearance playerSpriteAppearance, Point3f laserPosition,
            Bounds bounds, double centerToGround, double radius,
            Vector3f startPositions[], Matrix3f startRotations[],
            Geometry dotGeometry, Appearance dotAppearance,
            TransformGroup viewerTG, NetworkListener listener, int playerId,
            String names[], SoundHandler soundHandler, Bounds sceneBounds )
    {
        //init sceneBG
        sceneBG.setCapability( Group.ALLOW_CHILDREN_WRITE );//let them be removed
        this.sceneBG = sceneBG;

        //init pickTool
        pickTool = new PickTool( this.sceneBG );
        pickTool.setMode( PickInfo.PICK_GEOMETRY );
        pickTool.setFlags( PickInfo.CLOSEST_DISTANCE );
        this.laserPosition = new Point3f( laserPosition );

        this.listener = listener;
        this.playerId = playerId;

        //init PlayerSprites
        playerSprites = new PlayerSprite[ startPositions.length ];
        TransformGroup soundParents[] = new TransformGroup[ playerSprites.length ];
        spareT3D = new Transform3D();
        for( int i = 0; i < startPositions.length; i++ ) {
            playerSprites[ i ] = new PlayerSprite( startPositions[ i ],
                    startRotations[ i ], playerSpriteGeometry,
                    playerSpriteAppearance, pickTool, sceneBG, bounds,
                    centerToGround, radius, i, names[ i ], (playerId == i)
            );
            if( i == playerId ) playerSprites[ i ].setViewerTG( viewerTG );
            playerSprites[ i ].attachTo( sceneBG );
            soundParents[ i ] = playerSprites[ i ].getSoundParent();
        }

        //init soundHandler
        soundHandler.set( soundParents, sceneBounds, playerId );
        this.soundHandler = soundHandler;

        lastShots = new int[ playerSprites.length ];
        numShots = new int[ playerSprites.length ];
        hitsMade = new int[ playerSprites.length ];
        hitsTaken = new int[ playerSprites.length ];

/*        //init dots
        bitSet = new BitSet( playerSprites.length );
        dotSwitch = new Switch( Switch.CHILD_MASK, bitSet );
        dotSwitch.setPickable( false );
        dotSwitch.setCapability( Switch.ALLOW_SWITCH_WRITE );
        Vector3f axis = new Vector3f( 0, 1, 0 );
        for( int i = 0; i < playerSprites.length; i++ ) {
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

        playersHit = new ArrayList<Integer>( playerSprites.length );
        shooters = new ArrayList<Integer>( playerSprites.length );
        dotPositions = new ArrayList<Point3f>( playerSprites.length );
        
        secondSpareT3D = new Transform3D();
        spareVec = new Vector3f();
        spareV3f2 = new Vector3f();
        spareP3f = new Point3f();
        spareP3d = new Point3d();
        spareV3d = new Vector3d();
        spareV3d2 = new Vector3d();
    }


    public PlayerSprite[] getPlayerSprites() { return playerSprites; }


    public boolean isPlayerInGame( int id )
    {
        return playerSprites[ id ].isInGame();
    }


    public void playerLeft( int playerId )
    {
        playerSprites[ playerId ].leave();
        playerSprites[ playerId ].removeFrom( sceneBG );
    }


    private Transform3D dotT3D = new Transform3D();
//NOTE: the intersection point printed out may be in an arbitrary local coord system
//use the local_to_vworld transform to get the intersection in absolute virtual coords
    public void fireBeam( int shooterPlayerId, int frameId, Vector3f direction )
    {
        //make sure it's been at least FRAMES_BETWEEN_SHOTS frames since last shot
        if( frameId - lastShots[ shooterPlayerId ] < FRAMES_BETWEEN_SHOTS ) {
            if( frameId < lastShots[ shooterPlayerId ] ) {
                //some network models do not start at frame 0
                lastShots[ shooterPlayerId ] = frameId;
            }
            else return;
        }

        lastShots[ shooterPlayerId ] = frameId;
    	numShots[ shooterPlayerId ]++;
    	
    	//get the absolute laser position
        spareP3d.set( laserPosition );
        playerSprites[ shooterPlayerId ].transform( spareP3d );

        spareV3d.set( direction );
        spareV3d.normalize();
//DEBUG  System.out.println( "shooting, start=" + spareP3d + ", dir=" + spareV3d );
        
        //now do the picking
        pickTool.setMode( PickInfo.PICK_GEOMETRY );
		pickTool.setFlags( PickInfo.NODE | PickInfo.CLOSEST_INTERSECTION_POINT |
                PickInfo.LOCAL_TO_VWORLD );

        while( true ) {//if an intersected shape is transparent, do multiple picks
        	pickTool.setShape( new PickRay( spareP3d, spareV3d ), spareP3d );
        	PickInfo hitInfo = pickTool.pickClosest();
        	Node hit = pickTool.getNode( hitInfo, PickTool.TYPE_SHAPE3D );
        	if( hit instanceof PlayerSprite ) {
        		Point3d intersection = hitInfo.getClosestIntersectionPoint();
        		PlayerSprite hitSprite = (PlayerSprite) hit;
        		int hitPlayerId = hitSprite.getPlayerId();
                if( hitPlayerId == shooterPlayerId ) return;//this happens sometimes
                //else
        		hitsMade[ shooterPlayerId ]++;
        		hitsTaken[ hitPlayerId ]++;
                soundHandler.goodSound( shooterPlayerId );
                soundHandler.badSound( hitPlayerId );
                isScoresChanged = true;
                playersHit.add( new Integer( hitPlayerId ) );
                shooters.add( new Integer( shooterPlayerId ) );
                if( hitPlayerId == this.playerId ) {
                    listener.thisWasHit( shooterPlayerId );
                }
                if( shooterPlayerId == this.playerId ) {
                    listener.thisDidHit( hitPlayerId );
                }
                spareP3f.set( intersection );
                dotPositions.add( (Point3f) spareP3f.clone() );
/*                spareVec.set( spareP3f );
                TransformGroup dotTG = (TransformGroup)
                        dotSwitch.getChild( shooterPlayerId );
                dotT3D.set( spareVec );
                dotTG.setTransform( dotT3D );
                bitSet.set( shooterPlayerId );
                dotSwitch.setChildMask( bitSet );
*/
        		break;//break from loop
        	}
        	else if( hit instanceof GameSceneLoader.GameSceneShape3D ) {
        		Point3d intersection = hitInfo.getClosestIntersectionPoint();
        		if( ((GameSceneLoader.GameSceneShape3D) hit).getTextureProperties()
                        == SpriteManager.TEXTURE_TRANSPARENT ) {
                    //pick again, using the intersection point as the new start point
                    spareP3d.set( intersection );
                    hitInfo.getLocalToVWorld().transform( spareP3d );
                    /* move the start point along direction slightly, so that the same
                        shape isn't picked again */
                    spareV3d2.normalize( spareV3d );
                    spareV3d2.scale( REPICK_STEP );
                    spareP3d.add( spareV3d2 );
                    continue;//try again
        		}
        		else {
                    spareP3f.set( intersection );
                    dotPositions.add( (Point3f) spareP3f.clone() );
/*                    TransformGroup dotTG = (TransformGroup)
                            dotSwitch.getChild( shooterPlayerId );
                    //set dot position slightly toward shooter
                    spareVec.set( spareP3f );
                    spareV3d2.normalize( spareV3d );
                    spareV3d2.scale( DOT_FROM_WALL );
                    spareV3d2.negate();
                    spareV3f2.set( spareV3d2 );
                    spareVec.add( spareV3f2 );
                    dotT3D.set( spareVec );
                    dotTG.setTransform( dotT3D );
                    bitSet.set( shooterPlayerId );
                    dotSwitch.setChildMask( bitSet );
*/
                    break;//break from loop
                }
        	}
            else if( hit == null ) break;
        }
    }


    //the caller must decide what to do about players who've left
    public int[] getScores()
    {
        int scores[] = new int[ playerSprites.length ];
        for( int i = 0; i < scores.length; i++ ) {
            scores[ i ] += hitsMade[ i ] * MADE_HIT_BONUS;
            scores[ i ] += hitsTaken[ i ] * TOOK_HIT_LOSS;
        }
        isScoresChanged = false;
        return scores;
    }

    public boolean isScoresChanged() { return isScoresChanged; }

    //warning: arrays are returned by reference; do not modify them.
    public int[] getNumShots() { return numShots; }

    //warning: arrays are returned by reference; do not modify them.
    public int[] getHitsMade() { return hitsMade; }

    //warning: arrays are returned by reference; do not modify them.
    public int[] getHitsTaken() { return hitsTaken; }


    public int[] getPlayersHit()
    {
        Integer playersHitObjects[] = playersHit.toArray(
                new Integer[ 0 ] );
        int playersHitArray[] = new int[ playersHitObjects.length ];
        for( int i = 0; i < playersHitObjects.length; i++ ) {
            playersHitArray[ i ] = playersHitObjects[ i ].intValue();
        }
        playersHit.clear();
        return playersHitArray;
    }

    public int[] getShooters()
    {
        Integer shooterObjects[] = shooters.toArray(
                new Integer[ 0 ] );
        int shooterArray[] = new int[ shooterObjects.length ];
        for( int i = 0; i < shooterObjects.length; i++ ) {
            shooterArray[ i ] = shooterObjects[ i ].intValue();
        }
        shooters.clear();
        return shooterArray;
    }

    private int framesSinceClear;
    public Point3f[] getDotPositions()
    {
        Point3f dotPositionsArray[] = dotPositions.toArray( new Point3f[ 0 ] );

/*        //clear dots every reload interval
        framesSinceClear++;
        if( framesSinceClear >= (FRAMES_BETWEEN_SHOTS - 2) ) {
            dotPositions.clear();
            bitSet.clear();
            framesSinceClear = 0;
        }
*/        dotPositions.clear();


        return dotPositionsArray;
    }
}
