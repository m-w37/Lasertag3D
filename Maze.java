//Maze.java (2/5/2010)
//a class used with Maze3DPanel that represents a maze
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d.scene;

public class Maze {

    public final static int FRONT_OPEN = 1;
    public final static int TOP_OPEN = 2;
    public final static int BOTTOM_OPEN = 4;
    public final static int RIGHT_OPEN = 8;
    public final static int LEFT_OPEN = 16;
    public final static int BACK_OPEN = 32;

    public final static int VIEW_FACING_FRONT = 0;
    public final static int VIEW_FACING_RIGHT = 1;
    public final static int VIEW_FACING_BACK = 2;
    public final static int VIEW_FACING_LEFT = 3;


    private byte data[][][];//a representation of the maze, never null
    private String name;//this maze's name, never null, only ""
    private String description;//a description of this maze, never null, only ""

    /** Constructs a new Maze object with default, non-null values.
     * The variables can then be set with calls to the set methods below.
     */
    public Maze()
    {
        data = new byte[0][0][0];
        data[0][0][0] = 0;
        name = "Unknown";
        description = "Unknown";
    }

    /** Constructs a new Maze object based on the String specified.
     * The string should come from or be formatted in a .maze file
     * format (see inside method <init>(String, boolean)).
     *
     * @param str - a String in the .maze file format used to construct
     *     the maze.
     * @throws IllegalArgumentException - if there is a formatting problem,
     *     mainly with the <data> tag.
     */
    public Maze( String str )
            throws IllegalArgumentException
    {
        this( str, false );
    }

    /** Constructs a new maze object based on the String specified.
     * The string should come from or be formatted in a .maze file
     * format (see inside constructor).  If ignoreExceptions is true,
     * any IllegalArgumentExceptions thrown internally will be caught
     * and handled.  This is useful if you have a partial .maze file
     * and wish to the use the set methods for the rest of the data.
     *
     * @param str - a String in the .maze file format used to construct
     *     the maze.
     * @param ignoreExceptions - whether or not exceptions should be
     *     passed up a level and prevent this object's instantiation.
     * @throws IllegalArgumentException - if ignoreExceptions is false
     *     and there is an error in the format of str.
     */
    public Maze( String str, boolean ignoreExceptions )
            throws IllegalArgumentException
    {

        /* The string is represented in an xml type format with
         * 3 tags: name, desc, and data.  The data in <name> is the
         * name var exactly and desc is description.  The <size> tag
         * contains the size of the maze, height \\s depth \\s width.
         * The data tag is more complex: It contains an inner tag,
         * <floor>, that seperates the first set of indices in data.
         * Inside layer is another tag, <row>, that defines a horizontal
         * row.  Inside row are numbers seperated by white spaces that
         * define the openings in that cube (see above).  All tags
         * are case sensitive and extremely strict. */
        int nameStart = str.indexOf( "<name>" ) + 6;
        int nameEnd = str.indexOf( "</name>" );
        int descStart = str.indexOf( "<desc>" ) + 6;
        int descEnd = str.indexOf( "</desc>" );
        int sizeStart = str.indexOf( "<size>" ) + 6;
        int sizeEnd = str.indexOf( "</size>" );
        int dataStart = str.indexOf( "<data>" ) + 6;
        int dataEnd = str.indexOf( "</data>" );

        //extract vars
        if( nameStart < 0 || nameEnd < 0 ) name = "Unknown";//tag missing
        else name = str.substring( nameStart, nameEnd );

        if( descStart < 0 || descEnd < 0 ) description = "Unknown";
        else description = str.substring( descStart, descEnd );

        if( (dataStart < 0 || dataEnd < 0) && (ignoreExceptions == false ) )
            throw new IllegalArgumentException( "missing data tag" );
        if( (sizeStart < 0 || sizeEnd < 0) && (ignoreExceptions == false ) )
            throw new IllegalArgumentException( "missing size tag" );

        try{
            extractData( str.substring( dataStart, dataEnd ), str.substring(
                    sizeStart, sizeEnd ) );
        }
        catch( IllegalArgumentException exc ) {
            if( ignoreExceptions == false ) throw exc;
            else {
                data = new byte[0][0][0];
                data[0][0][0] = 0;
            }
        }
    }

    private void extractData( String dataStr, String size )
            throws IllegalArgumentException
    {
        int height, depth, width;
        String dims[] = size.split( "\\s" );//split by white space
        if( dims.length != 3 ) throw new IllegalArgumentException(
                "wrong number of dimensions" );
        height = Integer.parseInt( dims[ 0 ] );
        depth = Integer.parseInt( dims[ 1 ] );
        width = Integer.parseInt( dims[ 2 ] );

        data = new byte[ height ][ depth ][ width ];//make data array

        int floorPos = 0;
        for( int h = 0; h < data.length; h++ ) {//loop through floors
            int floorStart = dataStr.indexOf( "<floor>", floorPos ) + 7;
            int floorEnd = dataStr.indexOf( "</floor>", floorPos );
            if( floorStart == -1 || floorEnd == -1 ) throw new
                    IllegalArgumentException( "not enough floor tags" );
            floorPos = floorEnd + 8;//go to next tag next loop
            String thisFloor = dataStr.substring( floorStart, floorEnd );

            int rowPos = 0;
            for( int d = 0; d < data[h].length; d++ ) {//loop through row
                int rowStart = thisFloor.indexOf( "<row>", rowPos ) + 5;
                int rowEnd = thisFloor.indexOf( "</row>", rowPos );
                if( rowStart == -1 || rowEnd == -1 ) throw new
                        IllegalArgumentException( "missing row tag in floor " +
                            h );
                rowPos = rowEnd + 6;
                String thisRow = thisFloor.substring( rowStart, rowEnd );

                //extract values
                String values[] = thisRow.split( "\\s+" );//split between numbers
                int dataPos = 0;
                for( int w = 0; w < values.length; w++ ) {//loop in nums
//                    System.out.print( values[w] + " " );
                    if( values[ w ].matches( "\\d+" ) ) {//don't add whitespaces
                        try{
                            data[h][d][dataPos] = Byte.parseByte( values[ w ] );
                        }
                        catch( NumberFormatException exc ) {//not a number
                            throw new IllegalArgumentException( name + ": \'" + values[ w ] +
                                    "\' in row " + d + " of floor " + h +
                                    " is not a number" );
                        }
                        catch( ArrayIndexOutOfBoundsException exc ) {//w too big
                            throw new IllegalArgumentException( name + 
                                    ": missing number in row " + d + " of floor " +
                                    h );
                        }
                        dataPos++;
                    }
                }//end of width loop
//                System.out.println();
                if( --dataPos >= data[h][d].length ) throw new
                        IllegalArgumentException( name + ": too many numbers in row " +
                        d + " of floor " + h );
            }//end of depth loop

            if( thisFloor.indexOf( "<row>", rowPos ) != -1 ) throw new
                    IllegalArgumentException( "too many rows in floor " + h );
        }//end of floor loop

        if( dataStr.indexOf( "<floor>", floorPos ) != -1 ) throw new
                IllegalArgumentException( "too many floors" );
    }//end of extract data


    public void setData( byte data[][][] )
    {
        if( data == null ) {
            this.data = new byte[0][0][0];
            this.data[0][0][0] = 0;
        }
        else this.data = data;
    }

    public void setName( String name )
    {
        if( name == null ) name = "";
        this.name = name;
    }

    public void setDescription( String desc)
    {
        if( desc == null ) desc = "";
        this.description = desc;
    }


    public byte[][][] getData() { return data; }

    public String getName() { return name; }

    public String getDescription() { return description; }

    public int getHeight() { return data.length; }

    public int getDepth() { return data[0].length; }

    public int getWidth() { return data[0][0].length; }


    /** Returns true if the coordinates given can be used to 
     * access a cube in this maze.  If any of the indexes are
     * less than 0 or are greater than or equal to the length of
     * their corresponding axis, this returns false; otherwise
     * it returns true.
     */
    public boolean isCubeInMaze( int hIndex, int dIndex, int wIndex )
    {
        if( hIndex < 0 || dIndex < 0 || wIndex < 0 ) return false;
        if( hIndex >= data.length || dIndex >= data[ 0 ].length ||
            wIndex >= data[ 0 ][ 0 ].length ) return false;
        return true;
    }


    /** Returns true if the space specified in the maze has an opening
     * in the top, relative to if the cube at (0,0,0) is in the top-left-
     * back position.  If the space does not exist in this maze, this
     * method returns false.
     *
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     */
    public boolean isFrontOpen( int hIndex, int dIndex, int wIndex )
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        return ( (data[hIndex][dIndex][wIndex] & FRONT_OPEN) != 0 );
    }

    /** Returns true if the space specified in the maze has an opening
     * in the top, relative to if the cube at (0,0,0) is in the top-left-
     * back position.  If the space does not exist in this maze, this
     * method returns false.
     *
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     */
    public boolean isTopOpen( int hIndex, int dIndex, int wIndex )
            throws ArrayIndexOutOfBoundsException
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        return ( (data[hIndex][dIndex][wIndex] & TOP_OPEN) != 0 );
    }

    /** Returns true if the space specified in the maze has an opening
     * in the bottom, relative to if the cube at (0,0,0) is in the top-left-
     * back position.  If the space does not exist in this maze, this
     * method returns false.
     *
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     */
    public boolean isBottomOpen( int hIndex, int dIndex, int wIndex )
            throws ArrayIndexOutOfBoundsException
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        return ( (data[hIndex][dIndex][wIndex] & BOTTOM_OPEN) != 0 );
    }

    /** Returns true if the space specified in the maze has an opening
     * in the right, relative to if the cube at (0,0,0) is in the top-left-
     * back position.  If the space does not exist in this maze, this
     * method returns false.
     *
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     */
    public boolean isRightOpen( int hIndex, int dIndex, int wIndex )
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        return ( (data[hIndex][dIndex][wIndex] & RIGHT_OPEN) != 0 );
    }

    /** Returns true if the space specified in the maze has an opening
     * in the left, relative to if the cube at (0,0,0) is in the top-left-
     * back position.  If the space does not exist in this maze, this
     * method returns false.
     *
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     */
    public boolean isLeftOpen( int hIndex, int dIndex, int wIndex )
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        return ( (data[hIndex][dIndex][wIndex] & LEFT_OPEN) != 0 );
    }

    /** Returns true if the space specified in the maze has an opening
     * in the back, relative to if the cube at (0,0,0) is in the top-left-
     * back position.  If the space does not exist in this maze, this
     * method returns false.
     *
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     */
    public boolean isBackOpen( int hIndex, int dIndex, int wIndex )
            throws ArrayIndexOutOfBoundsException
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        return ( (data[hIndex][dIndex][wIndex] & BACK_OPEN) != 0 );
    }


    /** Returns true if the space provided in the maze has an opening
     * in the front, if the front side is relative to the viewPoint
     * given.  So, if you were facing the absolute left, the relative
     * front would be the absolute left.  If the space does not exist
     * in this maze, this method returns false.
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     * @param viewPoint - the panel that is being faced towards
     * @throws IllegalArgumentException - if viewPoint is not one of
     *     the predefined values (VIEW_FACING_FRONT, VIEW_FACING_RIGHT,
     *     VIEW_FACING_BACK, or VIEW_FACING_LEFT)
     */
    public boolean isFrontOpen( int hIndex, int dIndex, int wIndex,
            int viewPoint )
            throws IllegalArgumentException
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        switch( viewPoint ) {
            case VIEW_FACING_FRONT:
                return isFrontOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_RIGHT:
                return isRightOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_BACK:
                return isBackOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_LEFT:
                return isLeftOpen( hIndex, dIndex, wIndex );
            default:
                throw new IllegalArgumentException( "unknown viewpoint" );
        }
    }

    /** Returns true if the space provided in the maze has an opening
     * in the right, if the front side is relative to the viewPoint
     * given.  So, if you were facing the absolute left, the relative
     * right would be the absolute front.  If the space does not exist
     * in this maze, this method returns false.
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     * @param viewPoint - the panel that is being faced towards
     * @throws IllegalArgumentException - if viewPoint is not one of
     *     the predefined values (VIEW_FACING_FRONT, VIEW_FACING_RIGHT,
     *     VIEW_FACING_BACK, or VIEW_FACING_LEFT)
     */
    public boolean isRightOpen( int hIndex, int dIndex, int wIndex,
            int viewPoint ) throws IllegalArgumentException
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        switch( viewPoint ) {
            case VIEW_FACING_FRONT:
                return isRightOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_RIGHT:
                return isBackOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_BACK:
                return isLeftOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_LEFT:
                return isFrontOpen( hIndex, dIndex, wIndex );
            default:
                throw new IllegalArgumentException( "unknown viewpoint" );
        }
    }

    /** Returns true if the space provided in the maze has an opening
     * in the back, if the front side is relative to the viewPoint
     * given.  So, if you were facing the absolute left, the relative
     * back would be the absolute right.  If the space does not exist
     * in this maze, this method returns false.
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     * @param viewPoint - the panel that is being faced towards
     * @throws IllegalArgumentException - if viewPoint is not one of
     *     the predefined values (VIEW_FACING_FRONT, VIEW_FACING_RIGHT,
     *     VIEW_FACING_BACK, or VIEW_FACING_LEFT).
     */
    public boolean isBackOpen( int hIndex, int dIndex, int wIndex,
            int viewPoint ) throws IllegalArgumentException
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        switch( viewPoint ) {
            case VIEW_FACING_FRONT:
                return isBackOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_RIGHT:
                return isLeftOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_BACK:
                return isFrontOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_LEFT:
                return isRightOpen( hIndex, dIndex, wIndex );
            default:
                throw new IllegalArgumentException( "unknown viewpoint" );
        }
    }

    /** Returns true if the space provided in the maze has an opening
     * in the left, if the front side is relative to the viewPoint
     * given.  So, if you were facing the absolute left, the relative
     * left would be the absolute back.  If the space does not exist
     * in this maze, this method returns false.
     * @param hIndex - the height of the cube
     * @param dIndex - the depth of the cube
     * @param wIndex - the width of the cube
     * @param viewPoint - the panel that is being faced towards
     * @throws IllegalArgumentException - if viewPoint is not one of
     *     the predefined values (VIEW_FACING_FRONT, VIEW_FACING_RIGHT,
     *     VIEW_FACING_BACK, or VIEW_FACING_LEFT)
     */
    public boolean isLeftOpen( int hIndex, int dIndex, int wIndex,
            int viewPoint ) throws IllegalArgumentException
    {
        if( isCubeInMaze( hIndex, dIndex, wIndex ) == false ) return false;
        switch( viewPoint ) {
            case VIEW_FACING_FRONT:
                return isLeftOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_RIGHT:
                return isFrontOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_BACK:
                return isRightOpen( hIndex, dIndex, wIndex );
            case VIEW_FACING_LEFT:
                return isBackOpen( hIndex, dIndex, wIndex );
            default:
                throw new IllegalArgumentException( "unknown viewpoint" );
        }
    }


    public String toString()
    {
        StringBuffer str = new StringBuffer();
        str.append( "<maze>\n" );
        str.append( "<name>" + name + "</name>\n" );
        str.append( "<desc>" + description + "</desc>\n" );
        str.append( "<size>" + data.length + " " + data[0].length + " " +
                data[0][0].length + "</size>\n" );
        str.append( "<data>\n" );
        for( int h = 0; h < data.length; h++ ) {//loop through floors
            str.append( "   <floor>\n" );
            for( int d = 0; d < data[h].length; d++ ) {//loop through rows
                str.append( "      <row> " );
                for( int w = 0; w < data[h][d].length; w++ ) {//loop nums
                    str.append( data[h][d][w] + " " );
                }
                str.append( "</row>\n" );
            }
            str.append( "   </floor>\n" );
        }
        str.append( "</maze>" );
        return str.toString();
    }
}
