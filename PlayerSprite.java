//PlayerSprite.java (7/19/2010)
//represents a player in Lasertag3D and handles their interactions
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.sprite;


import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.pickfast.*;
import com.sun.j3d.utils.geometry.Text2D;

import java.awt.Font;

import com.mattweidner.lt3d.*;


public class PlayerSprite extends Shape3D {

    public final static float MOVE_LATERAL_STEP = 0.02f;
    public final static float MOVE_STEP = 0.035f;
    public final static float RUN_STEP = MOVE_STEP * 2;
    public final static Vector3d DOWN = new Vector3d( 0.0, -1.0, 0.0 );
    //angular speeds for key-controlled rotations
    public final static float ROTATE_STEP = (float) (Math.PI / 200.0);
    public final static float ROTATE_FAST_STEP = (float) (Math.PI / 100.0);

    public final static Appearance ghostAppearance;
    static {
        ghostAppearance = new Appearance();
        RenderingAttributes ra = new RenderingAttributes();
        ra.setVisible( false );
        ghostAppearance.setRenderingAttributes( ra );
    }

    public final static Appearance outlineApp;
    static {
        PolygonAttributes outlinePA = new PolygonAttributes();
        outlinePA.setPolygonMode( PolygonAttributes.POLYGON_LINE );
        outlinePA.setCullFace( PolygonAttributes.CULL_NONE );
        ColoringAttributes color = new ColoringAttributes(
                new Color3f( 1, 0, 0 ), ColoringAttributes.SHADE_FLAT
        );
        outlineApp = new Appearance();
        outlineApp.setPolygonAttributes( outlinePA );
        outlineApp.setColoringAttributes( color );
    }


    private BranchGroup branchGroup;//group containing all of this sprite
    private TransformGroup shapeTG;//group containing this sprite's shape
    private TransformGroup viewerTG;/* moves the viewer around; null if
            this sprite is not the player's sprite */
    private Matrix3d rotationM3d = new Matrix3d();
    private double viewerVerticalRotation = 0;/* This is used in gravity mode
            to tell how much to vertically rotate the viewerTG (the actual
            shape is not rotated). */

    private Shape3D outlineShape;//like this, but in outline
    private OrientedShape3D nameShape;//has the player's name in it

    private TransformGroup ghostTG;//used in collision tests
    private Bounds bounds;
    private Shape3D ghostShape;
    private PickTool pickTool;
    private BranchGroup pickScene;
    private double centerToGround, radius;
    private int playerId;//id of this PlayerSprite

    private boolean inGame;

    private Transform3D spareT3D, secondSpareT3D, storeT3D, toMove, toMove2;
    private Vector3f spareVec, secondSpareVec;
    private Point3d spareP3d;
    private Vector3d spareV3d;


    public PlayerSprite( Vector3f startTranslation, Matrix3f startRotation,
            Geometry geometry, Appearance appearance, PickTool pickTool,
            BranchGroup pickScene, Bounds bounds, double centerToGround,
            double radius, int playerId, String name, boolean willContainViewer )
    {
    	super( geometry, appearance );
        if( willContainViewer ) {
            //make this invisible
            setAppearance( ghostAppearance );
        }
        setCapability( Node.ALLOW_PICKABLE_WRITE );

        branchGroup = new BranchGroup();
        branchGroup.setCapability( BranchGroup.ALLOW_DETACH );

        toMove = new Transform3D();
        toMove.setAutoNormalize( true );
        spareT3D = new Transform3D();
        spareT3D.setAutoNormalize( true );
        toMove.set( startRotation, startTranslation, 1.0f );

        shapeTG = new TransformGroup();
        shapeTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
        shapeTG.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
        shapeTG.getTransform( spareT3D );
        spareT3D.mul( toMove );
        shapeTG.setTransform( spareT3D );
        shapeTG.addChild( this );

        if( willContainViewer == false ) {
            //make outline shape
            Shape3D outlineShape = new Shape3D( geometry, outlineApp );
            outlineShape.setPickable( false );
            shapeTG.addChild( outlineShape );
        }

        rotationM3d.set( startRotation );

        ghostTG = new TransformGroup();
        ghostTG.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
        ghostTG.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
        ghostTG.setTransform( spareT3D );
        ghostShape = new Shape3D( geometry, ghostAppearance );
        ghostTG.addChild( ghostShape );
        ghostTG.setPickable( false );
        branchGroup.addChild( ghostTG );

        //make nameShape
        Text3D text = new Text3D( new Font3D( new Font( "SansSerif", Font.PLAIN,
                10 ), new FontExtrusion() ), name, new Point3f( 0, 15, 0 ),
                Text3D.ALIGN_CENTER, Text3D.PATH_RIGHT
        );
        Appearance textAppearance = new Appearance();
        Material material = new Material();
        material.setAmbientColor( new Color3f( 0, 0, 0 ) );
        material.setLightingEnable( false );
        textAppearance.setMaterial( material );
        nameShape = new OrientedShape3D( text, textAppearance,
                OrientedShape3D.ROTATE_ABOUT_AXIS, new Vector3f( 0, 1, 0 )
        );
        TransformGroup nameTG = new TransformGroup();
        spareT3D.setIdentity();
        spareT3D.set( 0.02 );
        nameTG.setTransform( spareT3D );
        nameTG.setPickable( false );
        nameTG.addChild( nameShape );
        shapeTG.addChild( nameTG );
        branchGroup.addChild( shapeTG );

        this.pickTool = pickTool;
        this.pickScene = pickScene;
        this.bounds = bounds;
        this.centerToGround = centerToGround;
        this.radius = radius;
        this.playerId = playerId;
        inGame = true;

        toMove2 = new Transform3D();
        secondSpareT3D = new Transform3D();
        secondSpareT3D.setAutoNormalize( true );
        storeT3D = new Transform3D();
        spareVec = new Vector3f();
        secondSpareVec = new Vector3f();
        spareP3d = new Point3d();
        spareV3d = new Vector3d();
        //these two are defined below
        rotateToMove.setAutoNormalize( true );
        rotateT3D.setAutoNormalize( true );
    }


    public void setViewerTG( TransformGroup viewerTG )
    {
        this.viewerTG = viewerTG;
        shapeTG.getTransform( spareT3D );
        spareT3D.get( spareV3d );
        secondSpareT3D.set( rotationM3d, spareV3d, 1.0 );
        viewerTG.setTransform( secondSpareT3D );
    }


    public void attachTo( Group group )
    {
        group.addChild( branchGroup );
    }

    public void removeFrom( Group group )
    {
        group.removeChild( branchGroup );
    }


    public void addToViewerVerticalRotation( double rotationToAdd )
    {
        double newViewerVerticalRotation = this.viewerVerticalRotation +
                rotationToAdd;
        if( newViewerVerticalRotation > Math.PI / 2.0 ) {
            newViewerVerticalRotation = Math.PI / 2.0;
        }
        else if( newViewerVerticalRotation < -Math.PI / 2.0 ) {
            newViewerVerticalRotation = -Math.PI / 2.0;
        }
        setViewerVerticalRotation( newViewerVerticalRotation );
    }

    public void setViewerVerticalRotation( double viewerVerticalRotation )
    {
        this.viewerVerticalRotation = viewerVerticalRotation;
        applyViewerVerticalRotation();
    }

    public double getViewerVerticalRotation()
    {
        return viewerVerticalRotation;
    }

    public void move( Vector3f direction )
    {
        if( inGame == false ) return;

        //temporarily unpickable so adjustHeight doesn't think this is the ground
        setPickable( false );

        toMove.set( direction );
        shapeTG.getTransform( spareT3D );
        shapeTG.getTransform( storeT3D );
        spareT3D.mul( toMove );
        if( Lasertag3DSettings.gravityOn ) {
            //adjust height
            spareP3d.set( 0.0, 0.0, 0.0 );
            spareT3D.transform( spareP3d );
            double difference = adjustHeight( spareP3d );
            spareVec.set( 0.0f, (float) difference, 0.0f );
            toMove2.set( spareVec );
            spareT3D.mul( toMove2 );
        }

        //check to make sure the transform is congruent
        makeCongruent( spareT3D );

        shapeTG.setTransform( spareT3D );
        ghostTG.setTransform( spareT3D );
        if( viewerTG != null ) {
            if( Lasertag3DSettings.gravityOn ) {
                applyViewerVerticalRotation();
            }
            else {
                viewerTG.setTransform( spareT3D );
            }
        }

        //make pickable again
        setPickable( true );
    }

    //local vars for canMove and canRotate
    double spareD;
    Point3d translationP3d = new Point3d();
    public boolean canMove( Vector3f direction )
    {
        if( inGame == false ) return false;
        ghostTG.getTransform( storeT3D );//store it

        //temporarily unpickable, so ghost doesn't collide with self
        setPickable( false );

        toMove.set( direction );
        ghostTG.getTransform( spareT3D );
        spareT3D.mul( toMove );
        if( Lasertag3DSettings.gravityOn ) {
            //adjust height
            spareP3d.set( 0.0, 0.0, 0.0 );
            spareT3D.transform( spareP3d );
            double difference = adjustHeight( spareP3d );
            spareVec.set( 0.0f, (float) difference, 0.0f );
            toMove2.set( spareVec );
            spareT3D.mul( toMove2 );
        }
        ghostTG.setTransform( spareT3D );

        //test for collision
        //make bounds
        Bounds transformedBounds = (Bounds) bounds.clone();
        transformedBounds.transform( spareT3D );
        try {
            PickInfo info = pickScene.pickClosest( PickInfo.PICK_GEOMETRY,
                    PickInfo.NODE, new PickBounds( transformedBounds ) );
            if( info != null ) {
//DEBUG         System.out.println( "node: " + info.getNode() );
                return false;
            }
            else return true;
        }
        catch( NullPointerException exc ) {
/*DEBUG*/   System.err.println( "exception: " );
/*DEBUG*/   exc.printStackTrace();
            return false;
        }
        finally {
            //put ghost back
            ghostTG.setTransform( storeT3D );
            //make pickable again
            setPickable( true );
        }
    }


    //utility method
    //angle is in degrees
    private Matrix3d spareM3d = new Matrix3d();
    private AxisAngle4d spareAA4d = new AxisAngle4d();
    private Vector3d axisD = new Vector3d();
    public void rotate( Vector3f axis, float angle )
    {
        axisD.set( axis );
        spareAA4d.set( axisD, angle );
        spareM3d.set( spareAA4d );
        rotate( spareM3d );
    }

    private Transform3D rotateT3D = new Transform3D();
    private Transform3D rotateToMove = new Transform3D();
    private Transform3D rotateToMove2 = new Transform3D();
    private Vector3f rotateV3f = new Vector3f();
    private Point3d rotateP3d = new Point3d();
    public void rotate( Matrix3d rotationMatrix )
    {
        if( inGame == false ) return;

        //temporarily unpickable so adjustHeight doesn't think this is the ground
        setPickable( false );

        rotationMatrix.normalize();
        rotateToMove.setIdentity();
        rotateToMove.set( rotationMatrix );
        shapeTG.getTransform( rotateT3D );
        shapeTG.getTransform( storeT3D );
        rotateT3D.mul( rotateToMove );
        if( Lasertag3DSettings.gravityOn ) {
            //adjust height
            rotateP3d.set( 0.0, 0.0, 0.0 );
            rotateT3D.transform( rotateP3d );
            double difference = adjustHeight( rotateP3d );
            rotateV3f.set( 0.0f, (float) difference, 0.0f );
            rotateToMove2.set( rotateV3f );
            rotateT3D.mul( rotateToMove2 );
        }

        //check to make sure the transform is congruent
        makeCongruent( rotateT3D );

        shapeTG.setTransform( rotateT3D );
        ghostTG.setTransform( rotateT3D );
        rotateT3D.getRotationScale( rotationM3d );
        if( viewerTG != null ) {
            if( Lasertag3DSettings.gravityOn ) {
                applyViewerVerticalRotation();
            }
            else {
                viewerTG.setTransform( rotateT3D );
            }
        }

        //make pickable again
        setPickable( true );
    }

    //utility method
    //angle is in degrees
    public boolean canRotate( Vector3f axis, float angle )
    {
        axisD.set( axis );
        spareAA4d.set( axisD, angle );
        spareM3d.set( spareAA4d );
        return canRotate( spareM3d );
    }

    public boolean canRotate( Matrix3d rotationMatrix )
    {
        if( inGame == false ) return false;
        ghostTG.getTransform( storeT3D );//store it

        //temporarily unpickable, so ghost doesn't collide with self
        setPickable( false );

        rotationMatrix.normalize();
        toMove.setIdentity();
        toMove.set( rotationMatrix );
        ghostTG.getTransform( spareT3D );
        spareT3D.mul( toMove );
        if( Lasertag3DSettings.gravityOn ) {
            //adjust height
            spareP3d.set( 0.0, 0.0, 0.0 );
            spareT3D.transform( spareP3d );
            double difference = adjustHeight( spareP3d );
            spareVec.set( 0.0f, (float) difference, 0.0f );
            toMove2.set( spareVec );
            spareT3D.mul( toMove2 );
        }
        ghostTG.setTransform( spareT3D );

        //test for collision
        //make bounds

        Bounds transformedBounds = (Bounds) bounds.clone();
        transformedBounds.transform( spareT3D );
        try {
            PickInfo info = pickScene.pickClosest( PickInfo.PICK_GEOMETRY,
                    PickInfo.NODE, new PickBounds( transformedBounds ) );
            boolean result;
            if( info != null ) {
//DEBUG         System.out.println( "node: " + info.getNode() );
                return false;
            }
            else return true;
        }
        catch( NullPointerException exc ) {
/*DEBUG*/   System.err.println( "PlayerSprite: eact scoresxception: " );
/*DEBUG*/   exc.printStackTrace();
            return false;
        }
        finally {
            //put ghost back
            ghostTG.setTransform( storeT3D );
            //make pickable again
            setPickable( true );
        }
    }

    private Point3d spareAHP3d = new Point3d();
    private double distance;
    private double adjustHeight( Point3d heightP3d )
    {
        if( inGame == false ) return 0.0;
        if( Lasertag3DSettings.gravityOn == false ) return 0.0;
        //else
    	spareAHP3d.set( heightP3d );
        pickTool.setMode( PickInfo.PICK_GEOMETRY );
        pickTool.setFlags( PickInfo.CLOSEST_DISTANCE );
    	pickTool.setShape( new PickCylinderRay( spareAHP3d, DOWN, radius ),
                spareAHP3d
        );
    	PickInfo ground = pickTool.pickClosest();
    	if( ground != null ) distance = ground.getClosestDistance();
        else distance = centerToGround;
        double downwardChange = distance - centerToGround;
        if( downwardChange > 0 ) return - downwardChange * 0.5;
        else return - downwardChange;
    }

    private Transform3D shapeT3D = new Transform3D();
    private Transform3D verticalRotationT3D = new Transform3D();
    private void applyViewerVerticalRotation()
    {
        if( viewerTG != null ) {
            if( Lasertag3DSettings.gravityOn ) {
                shapeTG.getTransform( shapeT3D );
                verticalRotationT3D.rotX( viewerVerticalRotation );
                shapeT3D.mul( verticalRotationT3D );
                makeCongruent( shapeT3D );
                viewerTG.setTransform( shapeT3D );
            }
        }
    }

    /** Cumulative rounding errors sometimes make the t3d's be non-congruent
    * (which causes an exception when they are used above a View);
    * this method makes the t3d congruent again by forcing it to have a
    * uniform scale.
    * The core of this method comes from: http://java.net/jira/browse/JAVA3D-64
    **/
    private static Matrix3d m3d = new Matrix3d();
    private static double columns[] = new double[ 3 ];
    public static void makeCongruent( Transform3D t3d )
    {
        if( (t3d.getType() & Transform3D.CONGRUENT) == 0 ) {
            synchronized( m3d ) {
/*DEBUG*/       System.out.println( "makeCongruent() called" );
                t3d.get( m3d );
                columns[ 0 ] = m3d.m00*m3d.m00 + m3d.m10*m3d.m10 + m3d.m20*m3d.m20;
                columns[ 1 ] = m3d.m01*m3d.m01 + m3d.m11*m3d.m11 + m3d.m21*m3d.m21;
                columns[ 2 ] = m3d.m02*m3d.m02 + m3d.m12*m3d.m12 + m3d.m22*m3d.m22;

                if( columns[ 0 ] == columns[ 1 ] && columns[ 0 ] == columns[ 2 ] ) return;

                columns[ 0 ] = Math.sqrt( columns[ 0 ] );
                columns[ 1 ] = Math.sqrt( columns[ 1 ] );
                columns[ 2 ] = Math.sqrt( columns[ 2 ] );

/*                int closestToOne = 0;
                if( Math.abs(columns[ 1 ] - 1.0) < Math.abs(columns[ 0 ] - 1.0) ) {
                    closestToOne = 1;
                }
                if( Math.abs(columns[ 2 ] - 1.0) < Math.abs(columns[ closestToOne ] - 1.0) ) {
                    closestToOne = 2;
                }*/

                double scale;
                for( int i = 0; i < 3; i++ ) {
//                    if( i == closestToOne ) continue;
                    //else
                    scale = /*columns[ closestToOne ]*/ 1.0 / columns[ i ];
                    m3d.setElement( 0, i, m3d.getElement( 0, i ) * scale );
                    m3d.setElement( 1, i, m3d.getElement( 1, i ) * scale );
                    m3d.setElement( 2, i, m3d.getElement( 2, i ) * scale );
                }

                m3d.normalize();
                t3d.setRotationScale( m3d );
                t3d.normalizeCP();
            }
        }
    }




    public void leave()
    {
        inGame = false;
    }

    public boolean isInGame() { return inGame; }

    private Transform3D getT3D = new Transform3D();
    /* gets the location of this point relative to the player and
        puts it back in point
    */
    public void transform( Point3f point )
    {
        shapeTG.getTransform( getT3D );
        getT3D.transform( point );
    }

    /* gets the location of this point relative to the player and
        puts it back in point
    */
    public void transform( Point3d point )
    {
        shapeTG.getTransform( getT3D );
        getT3D.transform( point );
    }

    private Point3f getTransP3f = new Point3f();
    // gets the location of this player's (0,0,0)
    public void getTranslation( Vector3f vector )
    {
        getTransP3f.set( 0.0f, 0.0f, 0.0f );
        transform( getTransP3f );
        vector.set( getTransP3f );
    }

    public void getRotation( Matrix3f m3f )
    {
        m3f.set( rotationM3d );
    }

    private Transform3D viewerT3D = new Transform3D();
    private Matrix3f viewerRotation = new Matrix3f();
    /** Gets the orientation of viewerTG and puts it into vector. **/
    public void getViewerOrientation( Vector3f vector )
    {
        viewerTG.getTransform( viewerT3D );
        viewerT3D.get( viewerRotation );
        vector.set( 0, 0, 1 );
        viewerRotation.transform( vector );
        vector.negate();
    }
    
    public int getPlayerId() { return playerId; }

    //returns the group to be placed above the sound
    public TransformGroup getSoundParent()
    {
        return shapeTG;
    }

    public void where()
    {
        Vector3f loc = new Vector3f();
        getTranslation( loc );
        Matrix3f rot = new Matrix3f();
        getRotation( rot );
        System.out.println( "loc=" + loc );
        System.out.println( "rot=" + rot );
    }
}
