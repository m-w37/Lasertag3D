//ServerTimeoutException.java (8/8/2011)
//inidicates that the server took too long to connect and most likely failed
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d.net;


public class ServerTimeoutException extends RuntimeException {
    public ServerTimeoutException() { super(); }
    public ServerTimeoutException( String err ) { super( err ); }
}
