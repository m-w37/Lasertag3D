//SafeHyperlinkListener.java (8/15/2011)
//provides a wrapper around the java.awt.Desktop class that supports J2SE 5
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d;


import javax.swing.*;
import javax.swing.event.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.lang.reflect.*;


public class SafeHyperlinkListener implements HyperlinkListener {

    //static method/var
    private static SafeHyperlinkListener listener = new SafeHyperlinkListener();

    //adds the listener and a popup menu with copy feature, in case it doesn't work
    public static void addTo( JEditorPane pane )
    {
        pane.addHyperlinkListener( listener );
        //add the copy menu
        JPopupMenu popup = new JPopupMenu();
        CopyListener copyListener = new CopyListener( pane, popup );
        JMenuItem copy = new JMenuItem( "Copy" );
        copy.addActionListener( copyListener );
        popup.add( copy );
        pane.addMouseListener( copyListener );
    }

    private static class CopyListener extends MouseAdapter implements ActionListener {
        private JEditorPane pane;
        private JPopupMenu popup;

        public CopyListener( JEditorPane pane, JPopupMenu popup )
        {
            this.pane = pane;
            this.popup = popup;
        }

        public void actionPerformed( ActionEvent e ) { pane.copy(); }

        public void mousePressed( MouseEvent e ) { popup( e ); }
        public void mouseReleased( MouseEvent e ) { popup( e ); }
        private void popup( MouseEvent e )
        {
            if( e.isPopupTrigger() ) {
                popup.show( e.getComponent(), e.getX(), e.getY() );
            }
        }
    }


    //object methods/vars
    private Object desktop;//the Desktop class only exists in version >= 1.6
    private Class desktopClass;

    //suppress warnings about desktopClass's lack of a type
    @SuppressWarnings("unchecked")
    public SafeHyperlinkListener()
    {
        try {
            desktopClass = Class.forName( "java.awt.Desktop" );
            Method getDesktop = desktopClass.getMethod( "getDesktop",
                    (Class[]) null
            );
            desktop = getDesktop.invoke( null, (Object[]) null );
        }
        catch( ClassNotFoundException exc ) {//this is not the right version
            System.out.println( "InfoHyperlinkListener: not supported" );
        }
        catch( Exception exc ) {
            System.err.println( "Error: InfoHyperlinkListener: " + exc.getMessage() );
            exc.printStackTrace();
        }
    }

    //suppress warnings about desktopClass's lack of a type
    @SuppressWarnings("unchecked")
    public synchronized void hyperlinkUpdate( HyperlinkEvent e )
    {
        if( desktopClass != null && desktop != null ) {
            if( e.getEventType() == HyperlinkEvent.EventType.ACTIVATED ) {
                try {
                    URI uri = e.getURL().toURI();
                    Method browse = desktopClass.getMethod( "browse",
                            uri.getClass()
                    );
                    browse.invoke( desktop, uri );
                }
                catch( InvocationTargetException exc ) {//browse through an exception
                    System.err.println( "Error: while opening link: " +
                            exc.getMessage()
                    );
                    exc.printStackTrace();
                }
                catch( URISyntaxException exc ) {
                    System.err.println( "Error: bad hyperlink: " + e.getURL() );
                }
                catch( Exception exc ) {
                    System.err.println( "Error: InfoHyperlinkListener: " +
                            exc.getMessage()
                    );
                    exc.printStackTrace();
                }
            }
        }
    }
}
