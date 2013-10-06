//LogPrintStream.java (8/23/2011)
//filters data, then passes it to a print stream
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d;

import java.io.*;
import javax.sound.sampled.LineUnavailableException;

public class LogPrintStream extends PrintStream {

    /** Hack: if a sound resource cannot be allocated,
    * Java3D throws an exception saying so several times per second
    * in an area where the exception cannot be caught; this will
    * try to filter out those messages and not write them to the print stream.
    **/
    private boolean ignoreThisException;
    private SoundHandler handler;
    private boolean canDisable;

    public LogPrintStream( File file )
            throws FileNotFoundException
    {
        super( file );
    }

    public LogPrintStream( OutputStream out )
    {
        super( out );
    }

    public LogPrintStream( String str )
            throws FileNotFoundException
    {
        super( str );
    }


    public void setSoundHandler( SoundHandler handler )
    {
        this.handler = handler;
        canDisable = true;
    }


    public void println( Object obj )
    {
        if( obj instanceof LineUnavailableException ) {
            if( canDisable ) {
                handler.disable();
                println( "Sound disabled" );
                canDisable = false;
            }
            ignoreThisException = true;
        }
        else println( "" + obj );
    }

    boolean isDumpingStack = false;

    public void println( String str )
    {
        if( ignoreThisException ) {
            if( str.trim().startsWith( "at" ) ) return;//don't print this
            else ignoreThisException = false;
            //fall through
        }

        //print normally
        super.println( System.currentTimeMillis() + ": " + str );
    }

    /** Use println instead, in order to add a timestamp.
    **/
    @Deprecated
    public void print( String str )
    {
        super.print( str );
    }

    /** Use println instead, in order to add a timestamp.
    **/
    @Deprecated
    public void print( Object obj )
    {
        super.print( obj );
    }
}
