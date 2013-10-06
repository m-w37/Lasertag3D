//GameSceneLoader.java (5/26/2010)
//builds a game scene from a .mwg file
//by Matthew Weidner (www.mattweidner.com)


package com.mattweidner.lt3d.scene;


import javax.swing.ImageIcon;

import java.awt.MediaTracker;
import java.awt.Color;

import java.util.Hashtable;
import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;

import java.io.File;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.net.URL;
import java.net.MalformedURLException;

import javax.media.j3d.Group;
import javax.media.j3d.TransformGroup;
import javax.media.j3d.SharedGroup;
import javax.media.j3d.Link;
import javax.media.j3d.Shape3D;
import javax.media.j3d.Material;
import javax.media.j3d.TextureAttributes;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.Texture;
import javax.media.j3d.Texture2D;
import javax.media.j3d.Appearance;
import javax.media.j3d.QuadArray;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Transform3D;
import javax.media.j3d.Light;
import javax.media.j3d.AmbientLight;
import javax.media.j3d.DirectionalLight;
import javax.media.j3d.PointLight;
import javax.media.j3d.SpotLight;
import javax.media.j3d.BoundingBox;
import javax.media.j3d.CapabilityNotSetException;
import javax.media.j3d.RestrictedAccessException;

import javax.vecmath.Point3i;
import javax.vecmath.Point3f;
import javax.vecmath.Point3d;
import javax.vecmath.TexCoord2f;
import javax.vecmath.Vector3f;
import javax.vecmath.Color3f;
import javax.vecmath.Matrix3f;

import com.sun.j3d.utils.image.TextureLoader;


public class GameSceneLoader {

    /** <p>Builds a scene graph from a .mwg file. The file is formatted as
    * a series of lines that are either comments, starting with '#' or '//';
    * data lines, starting with 'd'; shape lines, starting with 's'; texture
    * lines, starting with 't'; lighting lines, starting with 'l'; or a
    * property line, starting with 'p'.</p>
    *
    *
    * <p>The very first line in the file is a property line,
    * and it is formatted as: <code>p &quot;name&quot; &quot;description&quot;
    * xscale yscale zscale xmax ymax zmax</code>
    * , where name is a name used to identify this course and is in quotes,
    * description is a short description used to describe this course
    * to users and is also in quotes,
    * xscale is the number of Java3D x units in one GameScene x unit,
    * yscale is the number of Java3D y units in one GameScene y unit,
    * zscale is the number of Java3D z units in one GameScene z unit,
    * xmax is the size of the region along the x axis (width),
    * ymax is the size of the region along the y axis (height), and
    * zmax is the size of the region along the z axis (length).
    * xmax, ymax, and zmax are all integer coordinates in GameScene units.<br />
    *
    * Shape lines define a shape that is in the game, such as a ceiling or
    * floor tile.  The shape must have exactly 4 vertices, and strange errors
    * may occur if they are not coplanar.  The lines are formatted as:
    * <code>s p0x,p0y,p0z p1x,p1y,p1z p2x,p2y,p2z p3x,p3y,p3z properties
    * shapenum</code>, where each set of 3 coordinates are the x,y,z
    * coordinates (in GameScene units) of a shape, starting with the
    * bottom-left corner and going counterclockwise to maintain proper
    * texture orientation, and numbers can be floating-point; properties
    * is an application-specific integer used to identify this shape's
    * properties; and shapenum is a unique parsable integer used to
    * identify this shape in data lines.  If shapenum is not unique,
    * results are undefined.<br />
    *
    * A file may contain many texture lines, which are formatted as:
    * <code>t location properties texnum [b]</code>
    * , where location is the location of the image, either as a relative
    * file name (starting with rel:), an rgb color (col:r,g,b), or a URL
    * that points to an image file used to represent the texture;
    * properties is an application-specific integer used to identify this
    * texture's properties; texnum is a unique parsable integer used
    * to identify this texture in data lines; and b is an option that, if
    * present, causes this texture to be drawn in outline instead of filled.
    * If texnum is not unique, results are undefined.<br />
    *
    * Lighting lines define the location and type of light sources.  They
    * are formatted as: 
    * <code>l type color [location] [direction] [spread]</code>, where
    * type is the type of light source, color is the color of the light
    * (r g b, all integers from 0-255), location is a point (x y z) in GameScene
    * coordinates (may be floating point) where the light source is,
    * direction is a vector (x y z, may be floating point) of the light
    * source's beam direction, and spread is the spread angle, in degress,
    * of the beam (may be floating point).  The types are a, d, p, and s.
    *  a is an ambient light, which has no direction or location: it permeates
    * everything, coming from all directions.  It has no location, direction,
    * or spread.  d is a directional light, having a direction but infinitely
    * far away (the angle does not vary): it is like the sun.  It has a direction
    * but no location or spread.  p is for a point light which radiates in all
    * directions from a fixed point: it is like a light bulb.  It has a
    * location but no spread or direction.  s is for a spotlight which
    * is located at location, points in direction, and has a beam of
    * width spread*2 degrees: it is like a flashlight.  If spread is 180,
    * it is effectively a point light.<br />
    *
    * Most of the lines will be data lines, which describe and position
    * tiles, walls, and other objects in a GameScene.  A data line is
    * formatted as: <code>d shapenum texnum x y z [xdir ydir zdir]</code>
    * , where shapenum is the shapenum of the shape it is; texnum is the
    * texnum used to represent the texture this panel will have; x, y, and
    * z are the location of this panel's bottom-left-back corner, in GameScene
    * coordinates (may be floating point); and xdir, ydir, and zdir (optional)
    * are the  directions of the positive x, y, and z axes, respectively,
    * represented by a  + or - and an axis, e.g., +x or -z.  If all three axes
    * are not used, results are undefined.  Note that GameScene units are not
    * the same as Java3D units.  All coordinates must be positive and less
    * than their relative maximums (defined in the property line) or they
    * may be cropped or incorrectly lit.<br />
    *
    * Additionally, there is an application-specific line that contains
    * data that is only relevant to one program using this file type.
    * It is formatted as: <code>a "data"</code>, where data is a string
    * , in quotes, that can be retrieved through the getApplicationSpecifics()
    * method.</p>
    *
    *
    **/


    private String name, description;
    private String base;/** URL used in place of rel: in texture locations OR
            * a relative file location (starting with a .) **/
    private String baseURL;/** base with relative file locations changed into
            * an absolute file URL **/
    private float xscale, yscale, zscale;/** The length in Java3D units of
            * GameScene unit **/
    private Point3f maxPoint;//the upper corner of the bounding box, in GameScene units
    private BoundingBox bounds;//the bounding box
    private Hashtable<Integer, TextureDefinition> textureDefs;//define textures
    private Hashtable<Integer, ShapeDefinition> shapeDefs;//define shapes
    private ArrayList<DataDefinition> dataDefs;//encompass info in data lines
    private ArrayList<LightDefinition> lightDefs;//define lighting nodes
    private ArrayList<String> applicationSpecifics;
    private Hashtable<Long, SharedGroup> sharedGroups;


    /** Utility method that retrieves the name and description from the given
    * resource.
    * The return value is a string array with length 2.  Entry 0 is the name
    * and entry 1 is the description.
    **/
    public static String[] getInfo( File file ) throws IOException,
            MWGFileFormatException
    {
        return getInfo( new FileInputStream( file ) );
    }

    /** Utility method that retrieves the name and description from the given
    * resource.
    * The return value is a string array with length 2.  Entry 0 is the name
    * and entry 1 is the description.
    **/
    public static String[] getInfo( URL url ) throws IOException,
            MWGFileFormatException
    {
        return getInfo( url.openStream() );
    }

    /** Utility method that retrieves the name and description from the given
    * resource.
    * The return value is a string array with length 2.  Entry 0 is the name
    * and entry 1 is the description.
    **/
    public static String[] getInfo( InputStream stream )
            throws IOException, MWGFileFormatException
    {
        return getInfo( new InputStreamReader( stream, "UTF-8" ) );
    }

    /** Utility method that retrieves the name and description from the given
    * resource.
    * The return value is a string array with length 2.  Entry 0 is the name
    * and entry 1 is the description.
    **/
    public static String[] getInfo( Reader stream )
            throws IOException, MWGFileFormatException
    {
        //get readable stream
        BufferedReader reader = new BufferedReader( stream );

        //read part of property line (first one): p "name" "description" ...
        String line = reader.readLine().trim();
        reader.close();
        try{
            int nameStart = line.indexOf( '\"' ) + 1;
            int nameEnd = line.indexOf( '\"', nameStart );
            int descStart = line.indexOf( '\"', nameEnd + 1 ) + 1;
            int descEnd = line.indexOf( '\"', descStart );
            String data[] = new String[ 2 ];
            data[ 0 ] = line.substring( nameStart, nameEnd );
            data[ 1 ] = line.substring( descStart, descEnd );
            return data;
        }
        catch( ArrayIndexOutOfBoundsException exc ) {
            throw new MWGFileFormatException( "Error on line 1" +
                    ": not enough arguments for p line.  Usage:" +
                    " p \"name\" \"description\"" +
                    " xscale yscale zscale xmax ymax zmax." );
        }
        catch( IndexOutOfBoundsException exc ) {
            throw new MWGFileFormatException( "Error on line 1" +
                    ": not enough quotes.  Usage: p \"name\" \"description\"" +
                    " xscale yscale zscale xmax ymax zmax." );
        }
        catch( NumberFormatException exc ) {
            throw new MWGFileFormatException( "Error on line 1" +
                    ": a number is not parsable.  Usage:" +
                    " p \"name\" \"description\"" +
                    " xscale yscale zscale xmax ymax zmax." );
        }
    }
    

    /** Loads the specified resource into memory, along with any referenced
    * images, and parses the file.  Note that the actual Shape3D's are not
    * built, so some memory isn't used until attachTo( Group group ) is called.
    * The stream is closed at the end.  base is set to the directory this file
    * is in and is always absolute.
    * @exception IOException  If there is an error reading from the stream or
    * loading a referenced image.
    * @exception MWGFileFormatException  If there is a syntax error in
    * the file.
    **/
    public GameSceneLoader( File file ) throws IOException,
            MWGFileFormatException
    {
        this( file, file.getParentFile().toURI().toURL().toString() );
    }

    public GameSceneLoader( File file, String base ) throws IOException,
            MWGFileFormatException
    {
        this( new FileInputStream( file ), base );
    }

    /** Loads the specified resource into memory, along with any referenced
    * images, and parses the file.  Note that the actual Shape3D's are not
    * built, so some memory isn't used until attachTo( Group group ) is called.
    * The stream is closed at the end.
    * @exception IOException  If there is an error reading from the stream or
    * loading a referenced image.
    * @exception MWGFileFormatException  If there is a syntax error in
    * the file.
    **/
    public GameSceneLoader( URL url ) throws IOException,
            MWGFileFormatException, IllegalArgumentException
    {
        this( url.openStream(), getBase( url ) );
    }

    public GameSceneLoader( InputStream stream, String base )
            throws IOException, MWGFileFormatException, IllegalArgumentException
    {
        this( new InputStreamReader( stream, "UTF-8" ), base );
    }

    /** Loads the specified resource into memory, along with any referenced
    * images, and parses the file.  Note that the actual Shape3D's are not
    * built, so some memory isn't used until attachTo( Group group ) is called.
    * The stream is closed at the end.
    * @exception IOException  If there is an error reading from the stream or
    * loading a referenced image.
    * @exception MWGFileFormatException  If there is a syntax error in
    * the file.
    **/
    public GameSceneLoader( Reader stream, String base )
            throws IOException, MWGFileFormatException, IllegalArgumentException
    {
        //get readable stream
        if( stream == null ) throw new IllegalArgumentException( "stream is null" );
        BufferedReader reader = null;
        this.base = base;
        if( base.startsWith( "." ) ) {//change to absolute URL from relative file
            File file = new File( base );
            baseURL = file.toURI().toURL().toString();
        }
        else baseURL = base;
        try{
            reader = new BufferedReader( stream );
            //initialize global array-like objects
            textureDefs = new Hashtable<Integer, TextureDefinition>();
            shapeDefs = new Hashtable<Integer, ShapeDefinition>();
            dataDefs = new ArrayList<DataDefinition>();
            applicationSpecifics = new ArrayList<String>();
            lightDefs = new ArrayList<LightDefinition>();
            sharedGroups = new Hashtable<Long, SharedGroup>();


            //initialize locals
            String line = null;
            StringTokenizer tokenizer = null;
            String start = null;
            boolean propertiesInitialized = false;
            String location = null;


            /*read property line (first one): p "name" "description"
            * xscale yscale zscale xmax ymax zmax*/
            line = reader.readLine().trim();
            try{
                int nameStart = line.indexOf( '\"' ) + 1;
                int nameEnd = line.indexOf( '\"', nameStart );
                int descStart = line.indexOf( '\"', nameEnd + 1 ) + 1;
                int descEnd = line.indexOf( '\"', descStart );
                this.name = line.substring( nameStart, nameEnd );
                this.description = line.substring( descStart, descEnd );
                String theRest[] = line.substring( descEnd + 1 ).trim().
                        split( "\\s+" );
                this.xscale = Float.parseFloat( theRest[ 0 ] );
                this.yscale = Float.parseFloat( theRest[ 1 ] );
                this.zscale = Float.parseFloat( theRest[ 2 ] );
                this.maxPoint = new Point3f( Float.parseFloat( theRest[ 3 ] ),
                        Float.parseFloat( theRest[ 4 ] ),
                        Float.parseFloat( theRest[ 5 ] )
                );
                this.bounds = new BoundingBox( new Point3d( -1.0, -1.0, -1.0 ),
                        new Point3d( maxPoint.x * xscale, maxPoint.y * yscale,
                                maxPoint.z * zscale )
                );
            }
            catch( ArrayIndexOutOfBoundsException exc ) {
                throw new MWGFileFormatException( "Error on line 1" +
                        ": not enough arguments for p line.  Usage:" +
                        " p \"name\" \"description\"" +
                        " xscale yscale zscale xmax ymax zmax." );
            }
            catch( IndexOutOfBoundsException exc ) {
                throw new MWGFileFormatException( "Error on line 1" +
                        ": not enough quotes.  Usage: p \"name\" \"description\"" +
                        " xscale yscale zscale xmax ymax zmax." );
            }
            catch( NumberFormatException exc ) {
                throw new MWGFileFormatException( "Error on line 1" +
                        ": a number is not parsable.  Usage:" +
                        " p \"name\" \"description\"" +
                        " xscale yscale zscale xmax ymax zmax." );
            }//end p line processing


            //loop through other lines, processing one at a time
            for( int i = 2; (line=reader.readLine()) != null; i++ ) {
                line = line.trim();

                //case 1: comment line
                if( line.startsWith( "#" ) || line.startsWith( "//" ) ) {
                    continue;//comment line, ignore
                }
                //else split it into words
                tokenizer = new StringTokenizer( line );

                //case 2: empty/all whitespace line
                if( tokenizer.hasMoreElements() == false ) continue;//empty line
                //else get first word (identifier letter)
                start = tokenizer.nextToken();

                //case 3: texture definition: t location properties texnum [b]
                if( start.equalsIgnoreCase( "t" ) ) {
                    try{
                        location = tokenizer.nextToken();
                        int properties = Integer.parseInt( tokenizer.nextToken() );
                        int texnum = Integer.parseInt( tokenizer.nextToken() );
                        boolean border = false;
                        if( tokenizer.hasMoreElements() ) {//might be 'b'
                            if( "b".equalsIgnoreCase( tokenizer.nextToken() ) ) {
                                border = true;
                            }
                            else {//too many words on line
                                throw new MWGFileFormatException( "Error on line " +
                                        i + ": too many elements.  Usage: t location" +
                                        " properties texnum [b]." );
                            }
                        }
                        textureDefs.put( new Integer( texnum ),
                                new TextureDefinition( properties, texnum, location,
                                        border
                                )
                        );
                        if( tokenizer.hasMoreElements() ) {//too many words on line
                            throw new MWGFileFormatException( "Error on line " +
                                    i + ": too many elements.  Usage: t location" +
                                " properties texnum [b]." );
                        }
                    }
                    catch( MalformedURLException exc ) {//invalid url
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": invalid texture location.  Usage: t location" +
                                " properties texnum [b]." );
                    }
                    catch( IOException exc ) {//thrown if image fails to load
                        throw new IOException( "Error on line " + i +
                                ": failed to load image at " + location + "." );
                    }
                    catch( NoSuchElementException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": not enough elements.  Usage: t location" +
                                " properties texnum [b]." );
                    }
                    catch( NumberFormatException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": a number is not parsable.  Usage: t" +
                                " location properties texnum [b]." );
                    }
                    catch( IllegalArgumentException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": " + exc.getMessage() + "  Usage: t" +
                                "location properties texnum [b]." );
                    }
                }//end if( t line )


                //case 4: shape line: s p0x,p0y,p0z p1x,p1y,p1z p2x,p2y,p2z
                        //p3x,p3y,p3z  properties shapenum
                else if( start.equalsIgnoreCase( "s" ) ) {
                    try{
                        Point3f points[] = new Point3f[ 4 ];
                        for( int j = 0; j < 4; j++ ) {
                            String pointString = tokenizer.nextToken();
                            String numberStrings[] = pointString.split( "," );
                            float x = Float.parseFloat( numberStrings[ 0 ] );
                            float y = Float.parseFloat( numberStrings[ 1 ] );
                            float z = Float.parseFloat( numberStrings[ 2 ] );
                            points[ j ] = new Point3f( x, y, z );
                        }
                        int properties = Integer.parseInt( tokenizer.nextToken() );
                        int shapenum = Integer.parseInt( tokenizer.nextToken() );
                        shapeDefs.put( new Integer( shapenum ),
                                new ShapeDefinition( points, shapenum, properties ) );
                        if( tokenizer.hasMoreElements() ) {//too many words on line
                            throw new MWGFileFormatException( "Error on line " +
                                    i + ": too many elements.  Usage: s" +
                                " p0x,p0y,p0z p1x,p1y,p1z p2x,p2y,p2z p3x,p3y,p3z" +
                                " properties shapenum." );
                        }
                    }
                    catch( NoSuchElementException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": not enough elements.  Usage: s p0x,p0y,p0z" +
                                " p1x,p1y,p1z p2x,p2y,p2z p3x,p3y,p3z properties" +
                                " shapenum." );
                    }
                    catch( NumberFormatException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": a number is not parsable.  Usage: s p0x,p0y,p0z" +
                                " p1x,p1y,p1z p2x,p2y,p2z p3x,p3y,p3z" +
                                " properties shapenum." );
                    }
                }//end if ( s line )

                //case 5: light line: l type color [location] [direction] [spread]
                else if( start.equalsIgnoreCase( "l" ) ) {
                    try {
                        char type = tokenizer.nextToken().charAt( 0 );
                        Color color = new Color( Integer.parseInt(
                                tokenizer.nextToken() ), Integer.parseInt(
                                tokenizer.nextToken() ), Integer.parseInt(
                                tokenizer.nextToken() )
                        );
                        float x = 0.0f, y = 0.0f, z = 0.0f;
                        if( type == 'p' || type == 's' ) {
                            x = Float.parseFloat( tokenizer.nextToken() );
                            y = Float.parseFloat( tokenizer.nextToken() );
                            z = Float.parseFloat( tokenizer.nextToken() );
                        }
                        Vector3f direction = null;
                        if( type == 'd' || type == 's' ) {
                            direction = new Vector3f( Float.parseFloat(
                                    tokenizer.nextToken() ), Float.parseFloat(
                                    tokenizer.nextToken() ), Float.parseFloat(
                                    tokenizer.nextToken() )
                            );
                        }
                        float spread = 0.0f;
                        if( type == 's' ) {
                            spread = Float.parseFloat( tokenizer.nextToken() );
                        }
                        if( tokenizer.hasMoreElements() ) {//too many words on line
                            throw new MWGFileFormatException( "Error on line " +
                                    i + ": too many elements.  Usage: l" +
                                " color [location] [direction] [spread]." );
                        }
                        lightDefs.add( new LightDefinition( type, color, x, y, z,
                                direction, spread ) );
                    }
                    catch( NoSuchElementException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": not enough arguments for l line.  Usage:" +
                                " l color [location] [direction] [spread]." );
                    }
                    catch( NumberFormatException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": a number is not parsable.  Usage: l" +
                                " color [location] [direction] [spread]." );
                    }
                }

                //case 6: data line: d shapenum texnum x y z [xdir ydir zdir]
                else if( start.equalsIgnoreCase( "d" ) ) {
                    try{
                        int shapenum = Integer.parseInt( tokenizer.nextToken() );
                        ShapeDefinition shapeDef = (ShapeDefinition)
                                shapeDefs.get( new Integer( shapenum ) );
                        if( shapeDef == null ) {//shapenum not a defined shape
                            throw new MWGFileFormatException( "Error on line " +
                                    i + ": shapenum does not match any defined" +
                                    " shapes.  Make sure the shape definition" +
                                    " occurs before this line.  Usage: d" +
                                    " shapenum texnum x y z [xdir ydir zdir]." );
                        }
                        int texnum = Integer.parseInt( tokenizer.nextToken() );
                        TextureDefinition textureDef = (TextureDefinition)
                                textureDefs.get( new Integer( texnum ) );
                        if( textureDef == null ) {//texnum not a defined texture
                            throw new MWGFileFormatException( "Error on line " +
                                    i + ": texnum does not match any defined" +
                                    " textures.  Make sure the texture definition" +
                                    " occurs before this line.  Usage: d" +
                                    " shapenum texnum x y z [xdir ydir zdir]." );
                        }
                        float x = Float.parseFloat( tokenizer.nextToken() );
                        float y = Float.parseFloat( tokenizer.nextToken() );
                        float z = Float.parseFloat( tokenizer.nextToken() );
                        if( tokenizer.hasMoreElements() ) {
                            dataDefs.add( new DataDefinition( shapeDef, textureDef,
                                    x, y, z, tokenizer.nextToken(),
                                    tokenizer.nextToken(), tokenizer.nextToken(),
                                    shapenum, texnum )
                            );
                        }
                        else {
                            dataDefs.add( new DataDefinition( shapeDef, textureDef,
                                    x, y, z, shapenum, texnum ) );
                        }
                        if( tokenizer.hasMoreElements() ) {//too many words on line
                            throw new MWGFileFormatException( "Error on line " +
                                    i + ": too many elements.  Usage: d" +
                                    " shapenum texnum x y z [xdir ydir zdir]." );
                        }
                    }
                    catch( NoSuchElementException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": not enough arguments for d line.  Usage:" +
                                " d shapenum texnum x y z [xdir ydir zdir]." );
                    }
                    catch( NumberFormatException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": a number is not parsable.  Usage: d" +
                                " shapenum texnum x y z [xdir ydir zdir]." );
                    }
                    catch( IllegalArgumentException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                ": a direction is not parsable.  Usage: d" +
                                " shapenum texnum x y z [xdir ydir zdir]." );
                    }
                }//end if ( d line )

                //case 7: application-specific line: a "data"
                else if( start.equalsIgnoreCase( "a" ) ) {
                    try{
                        int dataStart = line.indexOf( '\"' ) + 1;
                        int dataEnd = line.lastIndexOf( '\"' );
                        String data = line.substring( dataStart, dataEnd );
                        applicationSpecifics.add( data );
                    }
                    catch( IndexOutOfBoundsException exc ) {
                        throw new MWGFileFormatException( "Error on line " + i +
                                " not enough quotes.  Usage: a \"data\"." );
                    }
                }

                else {//first word is not a defined type
                    throw new MWGFileFormatException( "Error on line " + i +
                            ": unrecognized line identifyer: " + start + "." );
                }
            }//end for loop

            //close streams
            reader.close();//let IOException be passed up
        }//end main try block

        catch( MWGFileFormatException exc ) {//close streams and rethrow exc
            try{ reader.close(); stream.close(); }
            catch( IOException ioexc ) {}//not much to do at this point
            throw exc;
        }
        finally {
            try { reader.close(); }
            catch( Exception exc ) {}
        }
    }//end GameSceneLoader( InputStream stream, String base )


    /** Builds the scene and attaches it to the Group node given.
    * @exception NullPointerException  if group is null.
    * @exception CapabilityNotSetException  if the appropriate capability is
    * not set in group and group is part of a live or compiled scene graph.
    * @exception RestrictedAccessException  if group is part of live or
    * compiled scene graph.
    * Warning - not synchronized
    **/
    public void attachTo( Group group )
            throws NullPointerException, CapabilityNotSetException,
            RestrictedAccessException
    {
        //add lighting
        for( int i = lightDefs.size() - 1; i >= 0; i-- ) {
            Light light = lightDefs.get( i ).getLight();
            group.addChild( light );
        }
        //add shapes
        for( int i = 0; i < dataDefs.size(); i++ ) {
            Group child = dataDefs.get( i ).getShape3DGroup();
            group.addChild( child );
        }
    }//end attachTo( BranchGroup bg )


    //methods to retrieve scene data
    public String getName() { return name; }

    public String getDescription() { return description; }

    /** Returns the base used to replace rel: in texture locations.
    * The return value may start with a dot, indicating a relative file;
    * it is the exact value given in the constructor.
    **/
    public String getBase() { return base; }

    /** Returns the baseURL used to replace rel: in texture locations.
    * This will never be a relative file and should be a proper URL.
    * It may be different than the value given in the constructor.
    **/
    public String getBaseURL() { return baseURL; }

    public String[] getApplicationSpecifics()
    {
        return applicationSpecifics.toArray( new String[ 0 ] );
    }

    public float getXScale() { return xscale; }
    public float getYScale() { return yscale; }
    public float getZScale() { return zscale; }

    public float getMaxX() { return maxPoint.x; }
    public float getMaxY() { return maxPoint.y; }
    public float getMaxZ() { return maxPoint.z; }

    public BoundingBox getBounds() { return bounds; }

    @SuppressWarnings( "unchecked" )//prevent unchecked cast warnings
    public Hashtable<Integer, TextureDefinition> getTextureDefinitions()
    {
        //make shallow copy
        return (Hashtable<Integer, TextureDefinition>) textureDefs.clone();
    }

    @SuppressWarnings( "unchecked" )
    public Hashtable<Integer, ShapeDefinition> getShapeDefinitions()
    {
        //make shallow copy
        return (Hashtable<Integer, ShapeDefinition>) shapeDefs.clone();
    }

    public DataDefinition[] getDataDefinitions()
    {
        return dataDefs.toArray( new DataDefinition[ 0 ] );
    }

    public LightDefinition[] getLightDefinitions()
    {
        return lightDefs.toArray( new LightDefinition[ 0 ] );
    }


    //methods to set scene data
    public void setBounds( float maxX, float maxY, float maxZ )
    {
        maxPoint.set( maxX, maxY, maxZ );
        bounds.setUpper( maxX * xscale, maxY * yscale, maxZ * zscale );
    }

    public void setName( String name ) throws IllegalArgumentException
    {
        if( name == null ) throw new IllegalArgumentException( "Name is null" );
        //else
        this.name = name;
    }

    public void setDescription( String description ) throws IllegalArgumentException
    {
        if( description == null ) {
            throw new IllegalArgumentException( "Description is null" );
        }
        //else
        this.description = description;
    }

    public void addApplicationSpecific( String data )
    {
        if( data == null ) data = "";
        applicationSpecifics.add( data );
    }

    //returns: whether or not data was actually removed from the list
    public boolean removeApplicationSpecific( String data )
    {
        return applicationSpecifics.remove( data );
    }

    //throws IllegalArgumentException: if texture.texnum is already taken
    public void addTextureDefinition( TextureDefinition texture )
            throws IllegalArgumentException
    {
        Integer texnum = new Integer( texture.texnum );
        if( textureDefs.get( texnum ) != null ) {
            throw new IllegalArgumentException( texture.texnum +
                    " is already a defined texnum, cannot be changed"
            );
        }
        //else
        textureDefs.put( texnum, texture );
    }

    public int getDependencyCount( TextureDefinition texture )
    {
        int count = 0;
        DataDefinition datas[] = getDataDefinitions();
        for( int i = 0; i < datas.length; i++ ) {
            if( datas[ i ].textureDef == texture ) count++;
        }
        return count;
    }

    public boolean removeTextureDefinition( TextureDefinition texture )
    {
        if( textureDefs.containsValue( texture ) == false ) return false;
        DataDefinition datas[] = getDataDefinitions();
        for( int i = 0; i < datas.length; i++ ) {
            if( datas[ i ].textureDef == texture ) removeDataDefinition( datas[ i ] );
        }
        return (textureDefs.remove( new Integer( texture.texnum ) ) == null);
    }

    //throws IllegalArgumentException: if shape.shapenum is already taken
    public void addShapeDefinition( ShapeDefinition shape )
            throws IllegalArgumentException
    {
        Integer shapenum = new Integer( shape.shapenum );
        if( shapeDefs.get( shapenum ) != null ) {
            throw new IllegalArgumentException( shape.shapenum +
                    " is already a defined shapenum, cannot be changed" );
        }
        //else
        shapeDefs.put( shapenum, shape );
    }

    public int getDependencyCount( ShapeDefinition shape )
    {
        int count = 0;
        DataDefinition datas[] = getDataDefinitions();
        for( int i = 0; i < datas.length; i++ ) {
            if( datas[ i ].shapeDef == shape ) count++;
        }
        return count;
    }

    public boolean removeShapeDefinition( ShapeDefinition shape )
    {
        if( shapeDefs.containsValue( shape ) == false ) return false;
        DataDefinition datas[] = getDataDefinitions();
        for( int i = 0; i < datas.length; i++ ) {
            if( datas[ i ].shapeDef == shape ) removeDataDefinition( datas[ i ] );
        }
        return (shapeDefs.remove( new Integer( shape.shapenum ) ) == null);
    }

    public void addDataDefinition( DataDefinition data )
            throws IllegalArgumentException
    {
        if( data == null ) throw new IllegalArgumentException( "Data is null" );
        //else
        dataDefs.add( data );
    }

    //returns: whether or not data was actually removed from the list
    public boolean removeDataDefinition( DataDefinition data )
    {
        return dataDefs.remove( data );
    }

    public void addLightDefinition( LightDefinition data )
            throws IllegalArgumentException
    {
        if( data == null ) throw new IllegalArgumentException( "Data is null" );
        //else
        lightDefs.add( data );
    }

    //returns: whether or not data was actually removed from the list
    public boolean removeLightDefinition( LightDefinition data )
    {
        return lightDefs.remove( data );
    }


    //utility method
    private static String getBase( URL url )//returns the directory of a URL
    {
        String urlString = url.toString();
        int endIndex = urlString.lastIndexOf( '/' );
        return urlString.substring( 0, endIndex + 1 );
    }


/*--------------------------------------------------------------------------*/


    private static Material material;
    private static PolygonAttributes polygonAtts;
    private static PolygonAttributes polygonAttsBorder;
    private static TextureAttributes textureAtts;
    static {
        material = new Material();
        material.setLightingEnable( true );
        polygonAtts = new PolygonAttributes();
        polygonAtts.setCullFace( PolygonAttributes.CULL_NONE );
        polygonAtts.setBackFaceNormalFlip( true );
        polygonAttsBorder = new PolygonAttributes();
        polygonAttsBorder.setCullFace( PolygonAttributes.CULL_NONE );
        polygonAttsBorder.setBackFaceNormalFlip( true );
        polygonAttsBorder.setPolygonMode( PolygonAttributes.POLYGON_LINE );
        textureAtts = new TextureAttributes();
        textureAtts.setTextureMode( TextureAttributes.MODULATE );
    }

    public class TextureDefinition {


        int texnum;
        int properties;
        boolean border;
        Appearance appearance;
        String location;
        String locationRaw;

        public TextureDefinition( int properties, int texnum, String location,
                boolean border )
                throws IllegalArgumentException, IOException, MalformedURLException
        {
            this( properties, texnum, location, border, baseURL );//use global baseURL
        }

        public TextureDefinition( int properties, int texnum, String location,
                boolean border, String baseURL )
                throws IllegalArgumentException, IOException, MalformedURLException
        {
            this.locationRaw = location;
            this.location = (location == null)? "": location;//avoid null errors
            this.properties = properties;
            this.texnum = texnum;
            this.border = border;
            if( location.startsWith( "col:" ) ) {//color
                String rgbStrings[] = location.substring( 4 ).split( "," );
                if( rgbStrings.length != 3 ) {
                    throw new IllegalArgumentException(
                            "wrong number of values in location color, must be " +
                            "col:r,g,b"
                    );
                }
                //else
                float rgb[] = new float[ 3 ];
                for( int i = 0; i < 3; i++ ) {
                    //change from string (0-255) to float (0.0-1.0)
                    rgb[ i ] = Integer.parseInt( rgbStrings[ i ] ) / 255.0f;
                }
                Color3f ambient = new Color3f( rgb[ 0 ] * 0.2f, rgb[ 1 ] * 0.2f,
                        rgb[ 2 ] * 0.2f );
                Color3f emissive = new Color3f( 0.0f, 0.0f, 0.0f );
                Color3f diffuse = new Color3f( rgb );
                Color3f specular = new Color3f( 1.0f, 1.0f, 1.0f );
                Material coloredMaterial = new Material( ambient, emissive,
                        diffuse, specular, 64 );
                appearance = new Appearance();
                appearance.setMaterial( coloredMaterial );
                if( border ) appearance.setPolygonAttributes( polygonAttsBorder );
                else appearance.setPolygonAttributes( polygonAtts );
            }
            else {
                if( location.startsWith( "rel:" ) ) {//relative image file
                    if( baseURL == null ) throw new IllegalArgumentException(
                            "baseURL is null" );
                    location = baseURL + location.substring( 4 );//don't alter global
                }
                //location is now a URL, get its Image
                ImageIcon image = new ImageIcon( new URL( location ) );
                init( image );
            }
        }

        public TextureDefinition( int properties, ImageIcon texImage,
                int texnum, String location ) throws IOException
        {
            this.locationRaw = location;
            this.properties = properties;
            this.texnum = texnum;
            this.location = location;
            init( texImage );
        }

        private void init( ImageIcon texImage ) throws IOException
        {
            int loadStatus;
            while( (loadStatus = texImage.getImageLoadStatus()) !=
                    MediaTracker.COMPLETE ) {//wait for image to load
                if( loadStatus == MediaTracker.ABORTED ||
                        loadStatus == MediaTracker.ERRORED ) {//load error
                    throw new IOException( "Failed to load image." );
                }
                Thread.yield();//wait and try again
            }
            //image loaded, make Texture2D
            TextureLoader texLoader = new TextureLoader( texImage.getImage(),
                    TextureLoader.GENERATE_MIPMAP, null );
            Texture2D texture = (Texture2D) texLoader.getTexture();
            texture.setMinFilter( Texture.MULTI_LEVEL_LINEAR );
            texture.setEnable( true );

            //make Appearance
            appearance = new Appearance();
            appearance.setTexture( texture );
            appearance.setMaterial( material );
            if( border ) appearance.setPolygonAttributes( polygonAttsBorder );
            else appearance.setPolygonAttributes( polygonAtts );
            appearance.setTextureAttributes( textureAtts );
        }
        
        
        public int getTexnum() { return texnum; }
        
        public int getProperties() { return properties; }

        public void setProperties( int prop ) { properties = prop; }

        public boolean isBorder() { return border; }

        public String getLocationRaw() { return locationRaw; }
        
        public Appearance getAppearance() { return appearance; }

        public String toString()
        {
            String borderString = (border)? " b": "";
            return "t " + location + " " + properties + " " + texnum + borderString;
        }
    }

    private final static float texcoords[] = {0,0, 1,0, 1,1, 0,1};
    private static Point3f point = new Point3f();

    public class ShapeDefinition {

        int shapenum;
        int properties;
        QuadArray geometry;
        Point3f givenVertices[], adjustedVertices[];

        public ShapeDefinition( Point3f vertices[], int shapenum,
                int properties )
        {
            givenVertices = vertices;
            this.shapenum = shapenum;
            this.properties = properties;
            //vertices: GameScene coords -> Java3D coords
            adjustedVertices = new Point3f[ 4 ];
            for( int i = 0; i < 4; i++ ) {
                adjustedVertices[ i ] = new Point3f( vertices[ i ].x * xscale,
                        vertices[ i ].y * yscale, vertices[ i ].z * zscale );
            }
            //make Geometry
            geometry = new QuadArray( adjustedVertices.length,
                    GeometryArray.COORDINATES |
                    GeometryArray.TEXTURE_COORDINATE_2 |
                    GeometryArray.NORMALS );
            geometry.setCoordinates( 0, adjustedVertices );
            geometry.setTextureCoordinates( 0, 0, texcoords );
            //calculate normal
            Vector3f normal = new Vector3f();
            adjustedVertices[ 0 ].sub( adjustedVertices[ 1 ] );
            Vector3f v1 = new Vector3f( adjustedVertices[ 0 ] );
            adjustedVertices[ 2 ].sub( adjustedVertices[ 1 ] );
            Vector3f v2 = new Vector3f( adjustedVertices[ 2 ] );
            normal.cross( v2, v1 );
            normal.normalize();
            for( int i = 0; i < 4; i++ ) {
                geometry.setNormal( i, normal );
            }
        }
        
        
        public int getShapenum() { return shapenum; }
        
        public int getProperties() { return properties; }

        public Point3f[] getGivenVertices() { return givenVertices; }


        public void setProperties( int prop ) { properties = prop; }
        
        public QuadArray getGeometry() { return geometry; }

        public String toString()
        {
            String string = "s ";
            for( int i = 0; i < 4; i++ ) {
                string += givenVertices[ i ].x + "," + givenVertices[ i ].y +
                        "," + givenVertices[ i ].z + " ";
            }
            string += properties + " " + shapenum;
            return string;
        }
    }


    //locals moved here for efficiency
    private static Vector3f vector = new Vector3f();
    private static Transform3D transform = new Transform3D();
    private static Matrix3f matrix = new Matrix3f();
    //constants
    public final static int X_AXIS = 1;
    public final static int Y_AXIS = 2;
    public final static int Z_AXIS = 3;

    public class DataDefinition {

        ShapeDefinition shapeDef;
        TextureDefinition textureDef;
        float x, y, z;
        String xdir, ydir, zdir;
        int shapenum, texnum;
        TransformGroup translationTG, rotationTG;

        public DataDefinition( float x, float y, float z, int shapenum, int texnum )
        {
            this( x, y, z, "+x", "+y", "+z", shapenum, texnum );
        }

        public DataDefinition( float x, float y, float z, String xdir, String ydir,
                String zdir, int shapenum, int texnum )
        {
            this( shapeDefs.get( new Integer( shapenum ) ),
                    textureDefs.get( new Integer( texnum ) ), x, y, z, xdir, ydir,
                    zdir, shapenum, texnum
            );
        }

        public DataDefinition( ShapeDefinition shapeDef, TextureDefinition
                textureDef, float x, float y, float z, int shapenum, int texnum )
        {
            this( shapeDef, textureDef, x, y, z, "+x", "+y", "+z", shapenum, texnum );
        }

        public DataDefinition( ShapeDefinition shapeDef, TextureDefinition
                textureDef, float x, float y, float z, String xdir, String ydir,
                String zdir, int shapenum, int texnum )
                throws IllegalArgumentException
        {
            this.shapeDef = shapeDef;
            this.textureDef = textureDef;
            this.x = x;
            this.y = y;
            this.z = z;
            this.xdir = xdir.trim().toLowerCase();
            this.ydir = ydir.trim().toLowerCase();
            this.zdir = zdir.trim().toLowerCase();
            this.shapenum = shapenum;
            this.texnum = texnum;
            translationTG = new TransformGroup();
            rotationTG = new TransformGroup();
            synchronized( vector ){
                //set translationTG
                transform.setIdentity();
                vector.set( x * xscale, y * yscale, z * zscale );
                transform.set( vector );
                translationTG.setTransform( transform );
                //set rotationTG
                transform.setIdentity();
                setRow( matrix, 0, this.xdir );
                setRow( matrix, 1, this.ydir );
                setRow( matrix, 2, this.zdir );
                transform.setRotationScale( matrix );
                rotationTG.setTransform( transform );
                translationTG.addChild( rotationTG );
            }

/* with common shapes... */
            Long key = new Long( shapenum * 0x100000000L + texnum );
            SharedGroup sharedGroup = sharedGroups.get( key );
            if( sharedGroup == null ) {//make new one
                sharedGroup = new SharedGroup();
                Shape3D shape = new GameSceneShape3D( shapeDef,
                        textureDef, x, y, z, this.xdir, this.ydir, this.zdir
                );
                sharedGroup.addChild( shape );
                sharedGroups.put( key, sharedGroup );
            }
            rotationTG.addChild( new Link( sharedGroup ) );
/*end*/

/* without common shapes...
            Shape3D shape = new GameSceneShape3D( shapeDef,
                        textureDef, x, y, z, this.xdir, this.ydir, this.zdir
            );
            rotationTG.addChild( shape );
/*end */
            rotationTG.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
            translationTG.setCapability( TransformGroup.ALLOW_TRANSFORM_READ );
        }

        private void setRow( Matrix3f rotation, int row, String dir )
                throws IllegalArgumentException
        {
            int sign;
            if( dir.charAt( 0 ) == '+' ) sign = 1;
            else if( dir.charAt( 0 ) == '-' ) sign = -1;
            else throw new IllegalArgumentException( "" );//tell next level
            switch( dir.charAt( 1 ) ) {
                case 'x': rotation.setRow( row, sign, 0, 0 ); break;
                case 'y': rotation.setRow( row, 0, sign, 0 ); break;
                case 'z': rotation.setRow( row, 0, 0, sign ); break;
                default: throw new IllegalArgumentException( "" );
            }
        }

        public ShapeDefinition getShapeDefinition() { return shapeDef; }
        public TextureDefinition getTextureDefinition() { return textureDef; }
        public Point3f getGameSceneCoords() { return new Point3f( x, y, z ); }
        public String getXdir() { return xdir; }
        public String getYdir() { return ydir; }
        public String getZdir() { return zdir; }
        public int getShapenum() { return shapenum; }
        public int getTexnum() { return texnum; }

        public TransformGroup getShape3DGroup()
        {
            return translationTG;
//            return rotationTG;
        }

        public String toString()
        {
            return "d " + shapenum + " " + texnum + "  " + x + " " + y + " " + z +
                    "  " + xdir + " " + ydir + " " + zdir;
        }
    }



    public class GameSceneShape3D extends Shape3D {

        private int texnum;

        private int shapenum;
        private int textureProperties;
        private int shapeProperties;
        private float x, y, z;
        private String xdir, ydir, zdir;

        public GameSceneShape3D( ShapeDefinition shapeDef,
                TextureDefinition textureDef, float x, float y, float z,
                String xdir, String ydir, String zdir )
        {
            super( shapeDef.geometry, textureDef.appearance );
            super.setCapability( Shape3D.ALLOW_COLLISION_BOUNDS_READ );

            this.shapenum = shapeDef.shapenum;
            this.texnum = textureDef.texnum;
            this.textureProperties = textureDef.properties;
            this.shapeProperties = shapeDef.properties;
            this.x = x;
            this.y = y;
            this.z = z;
            this.xdir = xdir;
            this.ydir = ydir;
            this.zdir = zdir;
        }

        public int getTexnum() { return texnum; }
        public int getShapenum() { return shapenum; }
        public int getTextureProperties() { return textureProperties; }
        public int getShapeProperties() { return shapeProperties; }
        public Point3f getGameSceneCoords() { return new Point3f( x, y, z ); }
        public String getXdir() { return xdir; }
        public String getYdir() { return ydir; }
        public String getZdir() { return zdir; }
    }


    //constants
    public final static char AMBIENT_LIGHT = 'a';
    public final static char DIRECTIONAL_LIGHT = 'd';
    public final static char POINT_LIGHT = 'p';
    public final static char SPOT_LIGHT = 's';

    private final static Point3f attenuation = new Point3f( 1, 0, 0 );

    //Point3f moved here for efficiency
    private static Point3f point2 = new Point3f();

    public class LightDefinition {

        char type;
        Color3f color;
        float x, y, z;
        Vector3f direction;
        float spreadDegrees;
        float spreadRadians;
        Light light;

        public LightDefinition( char type, Color color, float x, float y,
                float z, Vector3f direction, float spreadAngle )
        {
            this( type, new Color3f( color ), x, y, z, direction, spreadAngle );
        }

        public LightDefinition( char type, Color3f color, float x, float y,
                float z, Vector3f direction, float spreadAngle )
                throws MWGFileFormatException
        {
            this.type = Character.toLowerCase( type );
            this.color = color;
            this.x = x;
            this.y = y;
            this.z = z;
            this.direction = direction;
            this.spreadDegrees = spreadAngle;
            this.spreadRadians = (float) Math.toRadians( spreadAngle );
            if( spreadAngle < 0 ) throw new MWGFileFormatException(
                        "spread must be positive." );
            //make light
            synchronized( point2 ) {
                point2.set( x, y, z );
                switch( type ) {
                    case AMBIENT_LIGHT: light = new AmbientLight( color );
                        break;
                    case DIRECTIONAL_LIGHT: light = new DirectionalLight( color,
                                direction );
                        break;
                    case POINT_LIGHT: light = new PointLight( color, point2,
                                attenuation );
                        break;
                    case SPOT_LIGHT: light = new SpotLight( color, point2,
                                attenuation, direction, spreadRadians, 0.0f );
                        break;
                }
                light.setInfluencingBounds( bounds );
            }
        }

        public char getType() { return type; }

        public Color3f getColor() { return color; }

        public Point3f getLocation() { return new Point3f( x, y, z ); }

        public Vector3f getDirection() { return direction; }

        public float getSpreadAngle() { return spreadDegrees; }

        public Light getLight()
        {
            return light;
        }

        public String toString()
        {
            Color color = this.color.get();
            String string = "l " + type + "  " + color.getRed() + " " +
                    color.getGreen() + " " + color.getBlue();
            if( type == POINT_LIGHT || type == SPOT_LIGHT ) {//add location
                string += "  " + x + " " + y + " " + z;
            }
            if( type == DIRECTIONAL_LIGHT || type == SPOT_LIGHT ) {//add direction
                string += "  " + direction.x + " " + direction.y + " " +
                        direction.z;
            }
            if( type == SPOT_LIGHT ) {//add spread
                string += "  " + spreadDegrees;
            }
            return string;
        }
    }
}
