//BasicSprite.java (2/7/2011)
//client side sprite for lt3d
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d.sprite;


import javax.media.j3d.*;
import javax.vecmath.*;
import com.sun.j3d.utils.geometry.Text2D;

import java.awt.Font;

import com.mattweidner.lt3d.Lasertag3DSettings;


public class BasicSprite extends Shape3D {

    private BranchGroup branchGroup;//group containing all of this sprite
    private TransformGroup shapeTGTrans, shapeTGRot;
    private TransformGroup viewerTG;/* moves the viewer around; null if
            this sprite is not the player's sprite */

    private Vector3f translation = new Vector3f();
    private Matrix3f rotationM3f = new Matrix3f();
    private double viewerVerticalRotation;

    private Shape3D outlineShape;//like this, but in outline
    private OrientedShape3D nameShape;//has the player's name in it

    private int playerId;
    private boolean inGame;

    private Transform3D toMove = new Transform3D(),
            spareT3D = new Transform3D(),
            secondSpareT3D = new Transform3D();
    private Vector3f spareV3f = new Vector3f();
    private Point3f spareP3f = new Point3f();


    public BasicSprite( Vector3f startTranslation, Matrix3f startRotation,
            Geometry geometry, Appearance appearance, int playerId, String name,
            boolean willContainViewer )
    {
    	super( geometry, appearance );
        if( willContainViewer ) {
            //make this invisible
            setAppearance( PlayerSprite.ghostAppearance );
        }

        branchGroup = new BranchGroup();
        branchGroup.setCapability( BranchGroup.ALLOW_DETACH );

        this.playerId = playerId;
        inGame = true;

        if( willContainViewer == false ) {
            //make outline shape
            outlineShape = new Shape3D( geometry,
                    PlayerSprite.outlineApp );
            outlineShape.setPickable( false );
        }

        shapeTGTrans = new TransformGroup();
        shapeTGTrans.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
        shapeTGTrans.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
        spareT3D.set( startTranslation );
        shapeTGTrans.setTransform( spareT3D );

        shapeTGRot = new TransformGroup();
        shapeTGRot.setCapability( TransformGroup.ALLOW_TRANSFORM_WRITE );
        shapeTGRot.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
        spareT3D.set( startRotation );
        shapeTGRot.setTransform( spareT3D );

        shapeTGRot.addChild( this );
        if( willContainViewer == false ) {
            shapeTGRot.addChild( outlineShape );
        }
        shapeTGTrans.addChild( shapeTGRot );

        //make nameShape
        Text3D text = new Text3D( new Font3D( new Font( "SansSerif", Font.PLAIN,
                10 ), new FontExtrusion() ), name, new Point3f( 0, 20f, 0 ),
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
        shapeTGTrans.addChild( nameTG );//rotation irrelevant
        branchGroup.addChild( shapeTGTrans );

        rotationM3f.set( startRotation );
        translation.set( startTranslation );
    }


    public void setViewerTG( TransformGroup viewerTG )
    {
//DEBUG  System.out.println( "BasicSprite: setViewerTG: tg=" + viewerTG );
        this.viewerTG = viewerTG;
        spareT3D.setRotation( rotationM3f );
        spareT3D.setTranslation( translation );
        viewerTG.setTransform( spareT3D );
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

    public void set( Vector3f newTranslation, Matrix3f newRotation )
    {
        if( inGame == false ) return;

        spareT3D.set( newTranslation );
        shapeTGTrans.setTransform( spareT3D );
        this.translation = newTranslation;

        spareT3D.set( newRotation );
        shapeTGRot.setTransform( spareT3D );
        this.rotationM3f.set( newRotation );

        if( viewerTG != null ) {
            if( Lasertag3DSettings.gravityOn ) {
                applyViewerVerticalRotation();
            }
            else {
                spareT3D.setRotation( rotationM3f );
                spareT3D.setTranslation( translation );
                viewerTG.setTransform( spareT3D );
            }
        }
    }

    private Transform3D shapeT3D = new Transform3D();
    private Transform3D verticalRotationT3D = new Transform3D();
    private void applyViewerVerticalRotation()
    {
        if( viewerTG != null ) {
            if( Lasertag3DSettings.gravityOn ) {
                shapeT3D.setRotation( rotationM3f );
                shapeT3D.setTranslation( translation );
                verticalRotationT3D.rotX( viewerVerticalRotation );
                shapeT3D.mul( verticalRotationT3D );
                PlayerSprite.makeCongruent( shapeT3D );
                viewerTG.setTransform( shapeT3D );
            }
        }
    }


    public void leave() { inGame = false; }

    public boolean isInGame() { return inGame; }


    private Transform3D getT3D = new Transform3D();
    /* gets the location of this point relative to the player and
        puts it back in point
    */
    public void transform( Point3f point )
    {
        shapeTGRot.getTransform( getT3D );
        getT3D.transform( point );
        shapeTGTrans.getTransform( getT3D );
        getT3D.transform( point );
    }

    /* gets the location of this point relative to the player and
        puts it back in point
    */
    public void transform( Point3d point )
    {
        shapeTGRot.getTransform( getT3D );
        getT3D.transform( point );
        shapeTGTrans.getTransform( getT3D );
        getT3D.transform( point );
    }

    // gets the location of this player's (0,0,0)
    public void getTranslation( Vector3f vector )
    {
        vector.set( translation );
    }

    public void getRotation( Matrix3f matrix )
    {
        matrix.set( rotationM3f );
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
        return shapeTGTrans;//rotation irrelevant for sounds
    }
}
