//MWGFileFormatException.java (5/28/2010)
//thrown by GameSceneLoader to indicate an error
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d.scene;

public class MWGFileFormatException extends RuntimeException {
    public MWGFileFormatException() { super(); }
    public MWGFileFormatException( String err ) { super( err ); }
}
