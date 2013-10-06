//PacketSorter.java (8/1/2011)
//used by Lt3D.net to prevent client-side packet backlogs
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d.net;


import java.net.*;
import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.concurrent.*;


public class PacketSorter implements Runnable {

    private DatagramSocket socket;
    private ByteBuffer buffer;
    private int dataLength;
    private NetworkListener listener;
    private NetworkClient2 client;
    private Thread thread;
    private boolean continueRunning = true;
    private boolean hasPacket;

    private LinkedBlockingQueue<DatagramPacket> normalPackets;
    private DatagramPacket currentDataPacket;
    private int greatestFrameId = -1;

    /** socket is stored by reference.  No synchronization is done on it;
    * the DatagramSocket class already provides necessary protection around
    * blocking I/O operations.
    **/
    public PacketSorter( DatagramSocket socket, int dataLength,
            NetworkListener listener, NetworkClient2 client )
    {
        this.socket = socket;
        this.dataLength = dataLength;
        this.listener = listener;
        this.client = client;
        buffer = ByteBuffer.allocate( dataLength );
        normalPackets = new LinkedBlockingQueue<DatagramPacket>();
        thread = new Thread( this );
        thread.start();
    }

    //This method will NOT close the socket.
    public void close()
    {
        continueRunning = false;
        thread.interrupt();
    }

    public DatagramPacket receivePacket() throws IOException
    {
        //wait for a packet
        while( hasPacket == false ) {
            try { Thread.sleep( 1 ); }
            catch( InterruptedException exc ) {}
        }

        synchronized( this ) {
            DatagramPacket packet = normalPackets.poll();
            if( packet != null ) {
                if( normalPackets.peek() == null && currentDataPacket == null ) {
                    //if there are no other packets
                    hasPacket = false;
                }
                return packet;
            }

            //else
            assert (currentDataPacket != null);
            packet = currentDataPacket;
            //forget about the old packet
            currentDataPacket = null;
            hasPacket = false;
            //now return it
            return packet;
        }
    }

    public void run()
    {
        while( continueRunning ) {
            try {
                byte data[] = new byte[ dataLength ];
                DatagramPacket packet = new DatagramPacket( data, dataLength );
                socket.receive( packet );
//DEBUG         System.out.println( "PacketSorter: received packet" );

                if( client.checkPacket( packet ) == false ) continue;

                //else
                if( packet.getData()[ 0 ] == (byte) 'p' ) {//this is scene data
                    buffer.clear();
                    buffer.put( packet.getData() );
                    buffer.flip();
                    buffer.get();//skip identifier
                    //check if it is a useful packet (data is current)
                    int frameId = buffer.getInt();
                    if( frameId > greatestFrameId ) {//this packet is useful
                        synchronized( this ) {
                            greatestFrameId = frameId;
                            currentDataPacket = packet;
                            hasPacket = true;
                        }
                    }
                }
                else {//this is other data and should be queued
                    synchronized( this ) {
                        normalPackets.put( packet );
                        hasPacket = true;
                    }
                }
            }
            catch( Exception exc ) {
                if( continueRunning ) listener.exceptionThrown( exc );
                //else ignore; exc was caused by close()
            }
        }
    }
}
