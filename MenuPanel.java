//MenuPanel.java (6/6/2011)
//displays a basic menu
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d;


import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


public class MenuPanel extends JPanel {

    private ActionListener listener;
    private String menuItems[];
    private ImageIcon backgroundIcon, buttonIcon;
    private Image background, button;
    private Font font;
    private Paint paint;
    private int spacing;


    public MenuPanel( ActionListener listener, String menuItems[],
            ImageIcon background, ImageIcon button, Font font, Paint paint )
    {
        this.listener = listener;
        this.menuItems = menuItems;
        this.backgroundIcon = background;
        this.background = backgroundIcon.getImage();
        this.buttonIcon = button;
        this.button = buttonIcon.getImage();
        this.font = font;
        this.paint = paint;

//DEBUG  System.out.println( getPreferredSize() );
        MenuPanelListener mouseListener = new MenuPanelListener();
        addMouseListener( mouseListener );
        addMouseMotionListener( mouseListener );
    }


    public void paintComponent( Graphics g )
    {
        super.paintComponent( g );
        Graphics2D g2d = (Graphics2D) g;
        g2d.setPaint( paint );
        g2d.setFont( font );

        g2d.drawImage( background, 0, 0, getWidth(), getHeight(), this );

        int spacing = getSpacing();
        int buttonHeight = buttonIcon.getIconHeight();
        int buttonWidth = buttonIcon.getIconWidth();
        int x = (getWidth() - buttonWidth) / 2;

        for( int i = 0; i < menuItems.length; i++ ) {
            int y = spacing + i*(buttonHeight + spacing);
            g2d.drawImage( button, x, y, buttonWidth, buttonHeight, this );
            g2d.drawString( menuItems[ i ], x, y + buttonHeight );
        }
    }

    //utility methods
    private int getSpacing()
    {
        int heightOfButtons = buttonIcon.getIconHeight() * menuItems.length;
        int diff = getHeight() - heightOfButtons;
        int spacing = diff / menuItems.length;//this is rounded down by default
        return (spacing < 1)? 1: spacing;//don't let the buttons collide
    }

    //-1 indicates no button is under this point
    private int getButtonIndex( int x, int y )
    {
        //check width
        int width = getWidth();
        int buttonWidth = buttonIcon.getIconWidth();
        int edge = (width - buttonWidth) / 2;
        if( x < edge || x > (width - edge)) {
            return -1;//outside button area
        }

        //check height
        int buttonHeight = buttonIcon.getIconHeight();
        int spacing = getSpacing();
        int buttonIndex = y / (buttonHeight + spacing);
        int heightOnButton = y % (buttonHeight + spacing);
        if( heightOnButton >= spacing ) {
            return buttonIndex;
        }
        else {
            return -1;
        }
    }


    private class MenuPanelListener extends MouseAdapter
            implements MouseMotionListener
    {
        public void mousePressed( MouseEvent e )
        {
            int buttonIndex = getButtonIndex( e.getX(), e.getY() );
            if( buttonIndex != -1 ) {
                ActionEvent event = new ActionEvent( MenuPanel.this,
                        ActionEvent.ACTION_PERFORMED,
                        menuItems[ buttonIndex ]
                );
                try {
                    listener.actionPerformed( event );
                }
                catch( Exception exc ) { exc.printStackTrace(); }
            }
        }

        public void mouseReleased( MouseEvent e ) {
            updateCursor( e );
        }

        public void mouseDragged( MouseEvent e ) {}

        public void mouseMoved( MouseEvent e )
        {
            updateCursor( e );
        }

        private void updateCursor( MouseEvent e )
        {
            int buttonIndex = getButtonIndex( e.getX(), e.getY() );
            int oldCursorType = getCursor().getType();
            //care is taken to not disrupt cursors set by outside programs
            //also, the cursor is only changed if it is not already showing
            if(     buttonIndex == -1 &&
                    oldCursorType == Cursor.HAND_CURSOR
                ) {
                setCursor( Cursor.getDefaultCursor() );
            }
            else if( buttonIndex != -1 &&
                     oldCursorType == Cursor.DEFAULT_CURSOR
                ) {
                setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
            }
            //else ignore; other program has set cursor or it is already set
        }
    }
}
