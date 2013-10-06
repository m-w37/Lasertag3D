//GameSceneBuilder.java (9/25/2010)
//used to design a .mwg file
//by Matthew Weidner (www.mattweidner.com)

package com.mattweidner.lt3d.scene;


import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;

import javax.media.j3d.*;
import com.sun.j3d.utils.universe.*;
import com.sun.j3d.utils.geometry.*;
import com.sun.j3d.utils.behaviors.vp.*;
import javax.vecmath.*;


public class GameSceneBuilder extends JFrame implements ActionListener {

    private final static int CHANGES_FOR_SYNC = 10;/** Number of changes to add
            * directly to the scene before the scene is recreated using
            * GameSceneLoader.attachTo( Group group )
            **/

    //numeric constants that are used often
    private final static Integer i0 = new Integer( 0 ), i1 = new Integer( 1 );
    private final static Double d0 = new Double( 0.0 ), d1 = new Double( 1.0 ),
            ddot1 = new Double( 0.1 );


    private String base;
    private JMenu file;
    private JMenuItem newScene, open, save, saveAs, exit;
    private JMenu sceneMenu;
    private JMenuItem editScene;
    private JPopupMenu popupMenu;
    private JMenuItem newLight, newTextureDef, newShapeDef,
            newDataDef, newAppSpecific, editImmutably, delete;
    private JTree tree;
    private DefaultTreeModel treeModel;
    private DefaultMutableTreeNode textures, shapes, datas, lights, appSpecifics;
    private int changesSinceSync;
    private JSlider transparencySlider;
    private TransparencyAttributes transparency;
    private Canvas3D canvas;
    private SimpleUniverse universe;
    private OrbitBehavior orbitControls;
    private BranchGroup scene;
    private BranchGroup loadedScene;//only BranchGroups can be added to live scene
    private BranchGroup detailsGroup;//contains axes, axis labels, etc.
    private GameSceneLoader loader;
    private boolean fileOpen, fileChosen;
    private JFileChooser openChooser, saveChooser;
    private boolean promptForSave;
    private JDialog pleaseWait;
    private JDialog newDialog, lightDialog, textureDialog, shapeDialog,
            dataDialog;
    //newDialog components
    private JTextField newName, newDescription, newBase;
    private PointPanel newMax, newScale;
    private JButton newOkay, newCancel;
    private ButtonListener newButtonListener;
    //lightDialog components
    private JRadioButton lightAmbient, lightDirectional, lightPoint, lightSpot;
    private PointPanel lightColor, lightLocation, lightDirection;
    private JSpinner lightSpread;
    private JButton lightOkay, lightCancel;
    private ButtonListener lightButtonListener;
    //textureDialog components
    private JTextField textureLocation;
    private JSpinner textureProperties, textureTexnum;
    private JCheckBox textureBorder;
    private JButton textureOkay, textureCancel;
    private ButtonListener textureButtonListener;
    //shapeDialog components
    private PointPanel shapeP0, shapeP1, shapeP2, shapeP3;
    private JSpinner shapeProperties, shapeShapenum;
    private JButton shapeOkay, shapeCancel;
    private ButtonListener shapeButtonListener;
    //dataDialog components
    private PointPanel dataLocation;
    private JTextField dataXdir, dataYdir, dataZdir;
    private JSpinner dataShapenum, dataTexnum;
    private JButton dataOkay, dataCancel;
    private ButtonListener dataButtonListener;

    public GameSceneBuilder()
    {
        super( "Game Scene Builder" );

        //make menu bar
        JMenuBar menuBar = new JMenuBar();
        file = new JMenu( "File" );
        file.setMnemonic( 'F' );
        newScene = new JMenuItem( "New", 'N' );
        newScene.addActionListener( this );
        file.add( newScene );
        open = new JMenuItem( "Open...", 'O' );
        open.addActionListener( this );
        file.add( open );
        file.addSeparator();
        save = new JMenuItem( "Save", 'S' );
        save.addActionListener( this );
        file.add( save );
        saveAs = new JMenuItem( "Save As...", 'A' );
        saveAs.addActionListener( this );
        file.add( saveAs );
        file.addSeparator();
        exit = new JMenuItem( "Exit", 'x' );
        exit.addActionListener( this );
        file.add( exit );
        menuBar.add( file );

        sceneMenu = new JMenu( "Scene" );
        sceneMenu.setMnemonic( 'S' );
        editScene = new JMenuItem( "Edit Scene...", 'E' );
        editScene.addActionListener( this );
        sceneMenu.add( editScene );
        menuBar.add( sceneMenu );

        setJMenuBar( menuBar );

        //make file choosers
        openChooser = new JFileChooser();
        saveChooser = new JFileChooser();

        //make popup menu
        popupMenu = new JPopupMenu();
        newLight = new JMenuItem( "New Light...", 'L' );
        newLight.addActionListener( this );
        popupMenu.add( newLight );
        newTextureDef = new JMenuItem( "New Texture Definition...", 'T' );
        newTextureDef.addActionListener( this );
        popupMenu.add( newTextureDef );
        newShapeDef = new JMenuItem( "New Shape Definition...", 'S' );
        newShapeDef.addActionListener( this );
        popupMenu.add( newShapeDef );
        newDataDef = new JMenuItem( "New Data Definition...", 'D' );
        newDataDef.addActionListener( this );
        popupMenu.add( newDataDef );
        newAppSpecific = new JMenuItem( "New Application Line...", 'A' );
        newAppSpecific.addActionListener( this );
        popupMenu.add( newAppSpecific );
        editImmutably = new JMenuItem( "Edit Immutably...", 'I' );
        editImmutably.addActionListener( this );
        popupMenu.add( editImmutably );
        delete = new JMenuItem( "Delete...", 't' );
        delete.addActionListener( this );
        delete.setEnabled( false );
        editImmutably.setEnabled( false );
        popupMenu.add( delete );
        MouseListener popupListener = new MouseAdapter() {
            public void mousePressed( MouseEvent e ) { popup( e ); }
            public void mouseReleased( MouseEvent e ) { popup( e ); }
            public void mouseClicked( MouseEvent e ) { popup( e ); }
            public void popup( MouseEvent e )
            {
                if( e.isPopupTrigger() ) {
                    popupMenu.show( e.getComponent(), e.getX(), e.getY() );
                    validate();//help with light/heavy weight mixing
                }
            }
        };
        addMouseListener( popupListener );

        //make Java3D environment
        Container c = getContentPane();
        JPanel wrapper = new JPanel();
        wrapper.setLayout( new BorderLayout() );
        wrapper.setOpaque( false );
        canvas = new Canvas3D( SimpleUniverse.getPreferredConfiguration() );
        canvas.setPreferredSize( new Dimension( 1000, 1000 ) );
        wrapper.add( canvas, BorderLayout.CENTER );
        c.add( wrapper, BorderLayout.CENTER );
        canvas.setFocusable( true );
        canvas.requestFocus();
        universe = new SimpleUniverse( canvas );
        View view = universe.getViewer().getView();
        view.setTransparencySortingPolicy( View.TRANSPARENCY_SORT_GEOMETRY );
        view.setBackClipDistance( 100.0 );//accommodate large scenes
        //make axes
        detailsGroup = new BranchGroup();
        detailsGroup.setCapability( BranchGroup.ALLOW_DETACH );
        ColoringAttributes lineColor = new ColoringAttributes(
                new Color3f( 1.0f, 1.0f, 1.0f ), ColoringAttributes.SHADE_FLAT
        );
        Appearance lineApp = new Appearance();
        lineApp.setMaterial( null );
        lineApp.setColoringAttributes( lineColor );
        Point3f linePoint = new Point3f( 0.0f, 0.0f, 0.0f );
        LineArray xAxis = new LineArray( 2, LineArray.COORDINATES );
        LineArray yAxis = new LineArray( 2, LineArray.COORDINATES );
        LineArray zAxis = new LineArray( 2, LineArray.COORDINATES );
        xAxis.setCoordinate( 0, linePoint );
        yAxis.setCoordinate( 0, linePoint );
        zAxis.setCoordinate( 0, linePoint );
        linePoint.set( 10.0f, 0.0f, 0.0f );
        xAxis.setCoordinate( 1, linePoint );
        linePoint.set( 0.0f, 10.0f, 0.0f );
        yAxis.setCoordinate( 1, linePoint );
        linePoint.set( 0.0f, 0.0f, 10.0f );
        zAxis.setCoordinate( 1, linePoint );
        detailsGroup.addChild( new Shape3D( xAxis, lineApp ) );
        detailsGroup.addChild( new Shape3D( yAxis, lineApp ) );
        detailsGroup.addChild( new Shape3D( zAxis, lineApp ) );
        //make axis labels
        PolygonAttributes axisLabelPA = new PolygonAttributes();
        axisLabelPA.setCullFace( PolygonAttributes.CULL_NONE );
        Transform3D lineT3D = new Transform3D();
        lineT3D.setScale( 5.0 );
        Vector3f labelVector = new Vector3f( 5.0f, 0.0f, 0.0f );
        lineT3D.setTranslation( labelVector );
        Text2D xLabel = new Text2D( "X AXIS", new Color3f( 1.0f, 1.0f, 1.0f ),
                "SansSerif", 36, Font.BOLD );
        xLabel.getAppearance().setPolygonAttributes( axisLabelPA );
        TransformGroup xLabelT3D = new TransformGroup( lineT3D );
        xLabelT3D.addChild( xLabel );
        detailsGroup.addChild( xLabelT3D );
        labelVector.set( 0.0f, 5.0f, 0.0f );
        lineT3D.setTranslation( labelVector );
        Text2D yLabel = new Text2D( "Y AXIS", new Color3f( 1.0f, 1.0f, 1.0f ),
                "SansSerif", 36, Font.BOLD );
        yLabel.getAppearance().setPolygonAttributes( axisLabelPA );
        TransformGroup yLabelT3D = new TransformGroup( lineT3D );
        yLabelT3D.addChild( yLabel );
        detailsGroup.addChild( yLabelT3D );
        labelVector.set( 0.0f, 0.0f, 5.0f );
        lineT3D.setTranslation( labelVector );
        Text2D zLabel = new Text2D( "Z AXIS", new Color3f( 1.0f, 1.0f, 1.0f ),
                "SansSerif", 36, Font.BOLD );
        zLabel.getAppearance().setPolygonAttributes( axisLabelPA );
        TransformGroup zLabelT3D = new TransformGroup( lineT3D );
        zLabelT3D.addChild( zLabel );
        detailsGroup.addChild( zLabelT3D );
        //make scene
        scene = new BranchGroup();
        scene.setCapability( Group.ALLOW_CHILDREN_EXTEND );
        scene.setCapability( Group.ALLOW_CHILDREN_WRITE );
        loadedScene = new BranchGroup();
        loadedScene.setCapability( Group.ALLOW_CHILDREN_EXTEND );
        loadedScene.setCapability( Group.ALLOW_CHILDREN_WRITE );
        loadedScene.setCapability( BranchGroup.ALLOW_DETACH );
        loadedScene.addChild( detailsGroup );
        universe.addBranchGraph( scene );
        //create user's viewpoint
        TransformGroup viewTG = universe.getViewingPlatform().
                getViewPlatformTransform();
        Transform3D transform = new Transform3D();
        viewTG.getTransform( transform );
        transform.lookAt( new Point3d( -1, 1, -1 ), new Point3d( 0, 0, 0 ),
                new Vector3d( 0, 1, 0 ) );
        transform.invert();
        viewTG.setTransform( transform );
        //create OrbitControls for navigation
        orbitControls = new OrbitBehavior( canvas, OrbitBehavior.REVERSE_ALL );
        orbitControls.setSchedulingBounds( new BoundingBox(
                new Point3d( -100,-100,-100 ), new Point3d( 100,100,100 ) )
        );
        universe.getViewingPlatform().setViewPlatformBehavior( orbitControls );

        //make left panel
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout( new BorderLayout() );
        //make tree
        DefaultMutableTreeNode top = new DefaultMutableTreeNode();
        textures = new DefaultMutableTreeNode( "Textures" );
        top.add( textures );
        shapes = new DefaultMutableTreeNode( "Shapes" );
        top.add( shapes );
        datas = new DefaultMutableTreeNode( "Datas" );
        top.add( datas );
        lights = new DefaultMutableTreeNode( "Lights" );
        top.add( lights );
        appSpecifics = new DefaultMutableTreeNode( "Application Specifics" );
        top.add( appSpecifics );
        tree = new JTree( top );
        tree.getSelectionModel().setSelectionMode(
                TreeSelectionModel.SINGLE_TREE_SELECTION );
        tree.addTreeSelectionListener( new TreeSelectionListener() {
                public void valueChanged( TreeSelectionEvent e )
                {
                    if( tree.getSelectionPath() == null ) {
                        delete.setEnabled( false );
                        editImmutably.setEnabled( false );
                    }
                    else {
                        delete.setEnabled( true );
                        editImmutably.setEnabled( true );
                    }
                }
            }
        );
        treeModel = (DefaultTreeModel) tree.getModel();
        tree.addMouseListener( popupListener );
        JScrollPane treePane = new JScrollPane( tree );
        leftPanel.add( treePane, BorderLayout.CENTER );
        treePane.addMouseListener( popupListener );
        //make transparency
        transparency = new TransparencyAttributes( TransparencyAttributes.NICEST, 0.0f );
        transparency.setCapability( TransparencyAttributes.ALLOW_VALUE_WRITE );
        //make transparency slider
        transparencySlider = new JSlider( JSlider.HORIZONTAL, 0, 100, 0 );
        transparencySlider.setMajorTickSpacing( 25 );
        Hashtable<Integer, JLabel> labelTable = new Hashtable<Integer, JLabel>();
        labelTable.put( new Integer( 0 ), new JLabel( "Opaque" ) );
        labelTable.put( new Integer( 100 ), new JLabel( "Transparent" ) );
        transparencySlider.setLabelTable( labelTable );
        transparencySlider.addChangeListener( new ChangeListener() {
                public void stateChanged( ChangeEvent e )
                {
                    if( e.getSource() == transparencySlider ) {
                        if( transparencySlider.getValueIsAdjusting() == false ) {
                            transparency.setTransparency(
                                    (float) (transparencySlider.getValue() / 100.0)
                            );
                        }
                    }
                }
            }
        );
        leftPanel.add( transparencySlider, BorderLayout.SOUTH );
        c.add( leftPanel, BorderLayout.WEST );
        transparencySlider.addMouseListener( popupListener );

        //make pleaseWait dialog
        pleaseWait = new JDialog( this, "Please Wait", true );
        pleaseWait.getContentPane().add( new JLabel( "Syncing, please wait." ) );

        //make dialogs
        //make newDialog
        newDialog = new JDialog( this, "New Scene", true );
        c = newDialog.getContentPane();
        c.setLayout( new GridLayout( 0, 2 ) );
        newName = new JTextField( 20 );
        c.add( new JLabel( "Name" ) );
        c.add( newName );
        newDescription = new JTextField( 20 );
        c.add( new JLabel( "Description" ) );
        c.add( newDescription );
        newScale = new PointPanel( true, d0, null );
        c.add( new JLabel( "Scale" ) );
        c.add( newScale );
        newMax = new PointPanel( false, i0, null );
        c.add( new JLabel( "Max" ) );
        c.add( newMax );
        newBase = new JTextField( 20 );
        c.add( new JLabel( "Base" ) );
        c.add( newBase );
        newCancel = new JButton( "Cancel" );
        c.add( newCancel );
        newOkay = new JButton( "Okay" );
        c.add( newOkay );
        newButtonListener = new ButtonListener( newOkay, newCancel, newDialog );
        newDialog.getRootPane().setDefaultButton( newOkay );
        newDialog.pack();

        //make lightDialog
        lightDialog = new JDialog( this, "New Light", true );
        c = lightDialog.getContentPane();
        c.setLayout( new GridLayout( 0, 2 ) );
        ActionListener lightListener = new ActionListener() {
            public void actionPerformed( ActionEvent e )
            {
                if( e.getSource() == lightAmbient ) {
                    lightLocation.setEnabled( false );
                    lightDirection.setEnabled( false );
                    lightSpread.setEnabled( false );
                }
                else if( e.getSource() == lightDirectional ) {
                    lightLocation.setEnabled( false );
                    lightDirection.setEnabled( true );
                    lightSpread.setEnabled( false );
                }
                else if( e.getSource() == lightPoint ) {
                    lightLocation.setEnabled( true );
                    lightDirection.setEnabled( false );
                    lightSpread.setEnabled( false );
                }
                else if( e.getSource() == lightSpot ) {
                    lightLocation.setEnabled( true );
                    lightDirection.setEnabled( true );
                    lightSpread.setEnabled( true );
                }
            }
        };
        ButtonGroup group = new ButtonGroup();
        lightAmbient = new JRadioButton( "Ambient" );
        group.add( lightAmbient );
        lightAmbient.addActionListener( lightListener );
        c.add( lightAmbient );
        lightDirectional = new JRadioButton( "Directional" );
        group.add( lightDirectional );
        lightDirectional.addActionListener( lightListener );
        c.add( lightDirectional );
        lightPoint = new JRadioButton( "Point" );
        group.add( lightPoint );
        lightPoint.addActionListener( lightListener );
        c.add( lightPoint );
        lightSpot = new JRadioButton( "Spot" );
        group.add( lightSpot );
        lightSpot.addActionListener( lightListener );
        c.add( lightSpot );
        lightAmbient.setSelected( true );//default choice
        lightColor = new PointPanel( false, i0, new Integer( 255 ) );
        c.add( new JLabel( "Color (r,g,b)" ) );
        c.add( lightColor );
        Double dMinus10 = new Double( -10.0 );
        lightLocation = new PointPanel( true, dMinus10, null );
        lightLocation.setEnabled( false );
        c.add( new JLabel( "Location" ) );
        c.add( lightLocation );
        lightDirection = new PointPanel( true, dMinus10, null );
        lightDirection.setEnabled( false );
        c.add( new JLabel( "Direction" ) );
        c.add( lightDirection );
        Double d0 = new Double( 0.0 );
        lightSpread = new JSpinner( new SpinnerNumberModel( 0.0, 0.0, 180.0, 0.1 ) );
        lightSpread.setEnabled( false );
        c.add( new JLabel( "Spread" ) );
        c.add( lightSpread );
        lightCancel = new JButton( "Cancel" );
        c.add( lightCancel );
        lightOkay = new JButton( "Okay" );
        c.add( lightOkay );
        lightButtonListener = new ButtonListener( lightOkay, lightCancel,
                lightDialog );
        lightDialog.getRootPane().setDefaultButton( lightOkay );
        lightDialog.pack();

        //make textureDialog
        textureDialog = new JDialog( this, "New Texture", true );
        c = textureDialog.getContentPane();
        c.setLayout( new GridLayout( 0, 2 ) );
        textureLocation = new JTextField( 20 );
        c.add( new JLabel( "Location" ) );
        c.add( textureLocation );
        textureProperties = new JSpinner( new SpinnerNumberModel( i0, i0, null,
                i1 ) );
        c.add( new JLabel( "Properties" ) );
        c.add( textureProperties );
        textureTexnum = new JSpinner( new SpinnerNumberModel( i0, i0, null, i1 ) );
        c.add( new JLabel( "Texnum" ) );
        c.add( textureTexnum );
        textureBorder = new JCheckBox( "Border" );
        c.add( textureBorder );
        c.add( new JPanel() );//filler, so cancel and okay are on next line
        textureCancel = new JButton( "Cancel" );
        c.add( textureCancel );
        textureOkay = new JButton( "Okay" );
        c.add( textureOkay );
        textureButtonListener = new ButtonListener( textureOkay, textureCancel,
                textureDialog );
        textureDialog.getRootPane().setDefaultButton( textureOkay );
        textureDialog.pack();

        //make shapeDialog
        shapeDialog = new JDialog( this, "New Shape", true );
        c = shapeDialog.getContentPane();
        c.setLayout( new GridLayout( 0, 2 ) );
        shapeP0 = new PointPanel( true, dMinus10, null );
        c.add( shapeP0 );
        shapeP1 = new PointPanel( true, dMinus10, null );
        c.add( shapeP1 );
        shapeP2 = new PointPanel( true, dMinus10, null );
        c.add( shapeP2 );
        shapeP3 = new PointPanel( true, dMinus10, null );
        c.add( shapeP3 );
        shapeProperties = new JSpinner( new SpinnerNumberModel( i0, i0, null, i1 ) );
        c.add( new JLabel( "Properties" ) );
        c.add( shapeProperties );
        shapeShapenum = new JSpinner( new SpinnerNumberModel( i0, i0, null, i1 ) );
        c.add( new JLabel( "Shapenum" ) );
        c.add( shapeShapenum );
        shapeCancel = new JButton( "Cancel" );
        c.add( shapeCancel );
        shapeOkay = new JButton( "Okay" );
        c.add( shapeOkay );
        shapeButtonListener = new ButtonListener( shapeOkay, shapeCancel,
                shapeDialog );
        shapeDialog.getRootPane().setDefaultButton( shapeOkay );
        shapeDialog.pack();

        //make dataDialog
        dataDialog = new JDialog( this, "New DataDef", true );
        c = dataDialog.getContentPane();
        c.setLayout( new GridLayout( 0, 2 ) );
        dataLocation = new PointPanel( true, dMinus10, null, d1 );
        c.add( new JLabel( "Location" ) );
        c.add( dataLocation );
        c.add( new JLabel( "Axis Dirs" ) );
        dataXdir = new JTextField( "+x", 2 );
        c.add( dataXdir );
        dataYdir = new JTextField( "+y", 2 );
        c.add( dataYdir );
        dataZdir = new JTextField( "+z", 2 );
        c.add( dataZdir );
        dataShapenum = new JSpinner( new SpinnerNumberModel( i0, i0, null, i1 ) );
        c.add( new JLabel( "Shapenum" ) );
        c.add( dataShapenum );
        dataTexnum = new JSpinner( new SpinnerNumberModel( i0, i0, null, i1 ) );
        c.add( new JLabel( "Texnum" ) );
        c.add( dataTexnum );
        dataCancel = new JButton( "Cancel" );
        c.add( dataCancel );
        dataOkay = new JButton( "Okay" );
        c.add( dataOkay );
        dataButtonListener = new ButtonListener( dataOkay, dataCancel, dataDialog );
        dataDialog.getRootPane().setDefaultButton( dataOkay );
        dataDialog.pack();

        //JOptionPane.showInputDialog used for application specifics

        //init and show frame
        setSize( 800, 600 );
        setDefaultCloseOperation( JFrame.DO_NOTHING_ON_CLOSE );
        addWindowListener( new WindowAdapter() {
                public void windowClosing( WindowEvent e ) { tryExit(); }
            }
        );
        setVisible( true );
    }


    public synchronized void actionPerformed( ActionEvent e )
    {
        Object source = e.getSource();
        //file menu options
        if( source == newScene ) newScene();
        else if( source == open ) open();
        else if( source == save ) save();
        else if( source == saveAs ) saveAs();
        else if( source == exit ) tryExit();
        //scene menu options
        else if( source == editScene ) editScene();
        else {
            //popup menu options
            if( source == newLight ) newLight();
            else if( source == newTextureDef ) newTextureDef();
            else if( source == newShapeDef ) newShapeDef();
            else if( source == newDataDef ) newDataDef();
            else if( source == newAppSpecific ) newAppSpecific();
            else if( source == editImmutably ) editImmutably();
            else if( source == delete ) delete();
            promptForSave = true;//editing done
            validate();
        }
    }


    //file menu options
    private void newScene()
    {
        if( promptForSave() ) return;
        //else
        //show dialog
        newDialog.setLocationRelativeTo( this );
        newDialog.setVisible( true );
        //waiting for user to press a button...
        //this thread's execution continues when the dialog is dismissed
        boolean okay = newButtonListener.okayPressed;
        newButtonListener.reset();
        newDialog.setVisible( false );
        if( !okay ) return;
        //else
        this.base = newBase.getText();
        if( base == null || "".equals( base ) ) {
            base = ".";
        }
        Point3d max = newMax.getPoint3d();
        Point3d scale = newScale.getPoint3d();
        String propertiesString = "p \"" + newName.getText() + "\" \"" +
                newDescription.getText() + "\" " + scale.x + " " +
                scale.y + " " + scale.z + " " + max.x + " " + max.y + " " +
                max.z + "\n";
        try {
            loader = new GameSceneLoader( new StringReader( propertiesString ),
                base );
            removeScene();
            loader.attachTo( loadedScene );
            scene.addChild( loadedScene );
//            orbitControls.setSchedulingBounds( loader.getBounds() );
            fileOpen = true;
            fileChosen = false;
            promptForSave = false;
            changesSinceSync = 0;
            System.gc();//collect unclaimed memory
            repaint();
        }
        catch( IOException exc ) {}
        catch( MWGFileFormatException exc ) {
            JOptionPane.showMessageDialog( this, "Bad Input: " + exc.getMessage(),
                    "Bad Input", JOptionPane.ERROR_MESSAGE );
        }
    }

    private void open()
    {
        promptForSave();
        if( openChooser.showOpenDialog( this ) != JFileChooser.APPROVE_OPTION ) {
            return;
        }
        //else
        File file = openChooser.getSelectedFile();
        BufferedReader stream = null;
        try {
            stream = new BufferedReader( new FileReader( file ) );
            stream.readLine();
            String baseString = stream.readLine();
            boolean hasBase = false;
            if( baseString != null ) {
                if( baseString.startsWith( "//base:" ) ) {
                    this.base = baseString.substring( 7 );
                    hasBase = true;
                    stream.close();
                }
            }
            if( hasBase ) loader = new GameSceneLoader( file, base );
            else {
                loader = new GameSceneLoader( file );
                base = loader.getBase();
            }
//            orbitControls.setSchedulingBounds( loader.getBounds() );
            //allow objects to be transparent
            GameSceneLoader.TextureDefinition textures[] = loader.getTextureDefinitions().
                    values().toArray( new GameSceneLoader.TextureDefinition[ 0 ] );
            for( int i = 0; i < textures.length; i++ ) {
                textures[ i ].getAppearance().setTransparencyAttributes( transparency );
            }
            removeScene();
            sync( false );
            fileOpen = true;
            fileChosen = true;
            promptForSave = false;
            changesSinceSync = 0;
            saveChooser.setSelectedFile( file );
            System.gc();//collect unclaimed memory
            repaint();
        }
        catch( IOException exc ) {
            JOptionPane.showMessageDialog( this, "The file " + file +
                    " could not be opened: " + exc.getMessage(), "Open Error",
                    JOptionPane.ERROR_MESSAGE );
        }
        catch( MWGFileFormatException exc ) {
            JOptionPane.showMessageDialog( this, exc.getMessage(), "Open Error",
                    JOptionPane.ERROR_MESSAGE );
        }
    }

    private boolean save()
    {
        if( fileChosen == false ) return saveAs();
        //else
        File file = saveChooser.getSelectedFile();
        BufferedWriter writer = null;
        try {
            sync( true );
            writer = new BufferedWriter( new FileWriter( file ) );
            //write property line
            writer.write( "p \"" + loader.getName() + "\" \"" +
                    loader.getDescription() + "\" " + loader.getXScale() + " " +
                    loader.getYScale() + " " + loader.getZScale() + "  " +
                    loader.getMaxX() + " " + loader.getMaxY() + " " +
                    loader.getMaxZ() );
            writer.newLine();
            //write baseURL line
            if( base == null || "".equals( base ) ) {
                writer.write( "//base:." );
            }
            else writer.write( "//base:" + base );
            writer.newLine();
            //write generation statement
            writer.write( "//Generated by Matthew Weidner's Game Scene Builder" );
            writer.newLine();
            writer.newLine();
            //write application specific lines
            writer.write( "//application specific lines" );
            writer.newLine();
            String appSpecifics[] = loader.getApplicationSpecifics();
            for( int i = 0; i < appSpecifics.length; i++ ) {
                writer.write( "a \"" + appSpecifics[ i ] + "\"" );
                writer.newLine();
            }
            writer.newLine();
            //write lighting lines
            writer.write( "//light definitions" );
            writer.newLine();
            GameSceneLoader.LightDefinition lightDefs[] =
                    loader.getLightDefinitions();
            for( int i = 0; i < lightDefs.length; i++ ) {
                writer.write( lightDefs[ i ].toString() );
                writer.newLine();
            }
            writer.newLine();
            //write shape lines
            writer.write( "//shape definitions" );
            writer.newLine();
            GameSceneLoader.ShapeDefinition shapeDefs[] = loader.
                    getShapeDefinitions().values().toArray(
                    new GameSceneLoader.ShapeDefinition[ 0 ] );
            for( int i = 0; i < shapeDefs.length; i++ ) {
                writer.write( shapeDefs[ i ].toString() );
                writer.newLine();
            }
            writer.newLine();
            //write texture lines
            writer.write( "//texture definitions" );
            writer.newLine();
            GameSceneLoader.TextureDefinition textureDefs[] = loader.
                    getTextureDefinitions().values().toArray(
                    new GameSceneLoader.TextureDefinition[ 0 ] );
            for( int i = 0; i < textureDefs.length; i++ ) {
                writer.write( textureDefs[ i ].toString() );
                writer.newLine();
            }
            writer.newLine();
            //write data lines
            writer.write( "//data definitions" );
            writer.newLine();
            GameSceneLoader.DataDefinition dataDefs[] =
                    loader.getDataDefinitions();
            for( int i = 0; i < dataDefs.length; i++ ) {
                writer.write( dataDefs[ i ].toString() );
                writer.newLine();
            }
            promptForSave = false;
            return false;//successful
        }
        catch( IOException exc ) {
            JOptionPane.showMessageDialog( this, "Save Error: " + exc.getMessage(),
                    "Save Error", JOptionPane.ERROR_MESSAGE );
            return true;//tell about error
        }
        finally {
            try { writer.close(); }
            catch( Exception exc ) {}
        }
    }

    private boolean saveAs()
    {
        if( saveChooser.showSaveDialog( this ) != JFileChooser.APPROVE_OPTION ) {
            return true;//save not successful
        }
        //else
        fileChosen = true;
        return save();
    }

    private void tryExit()
    {
        if( promptForSave() ) return;
        //else
        dispose();//make sure screen resources are reclaimed
        System.exit( 0 );
    }


    //scene menu options
    private void editScene()
    {
        //this is basically the same as newScene

        if( fileOpen == false ) {
            JOptionPane.showMessageDialog( this, "There is no file open.\n" +
                    "Use File->New Scene... instead.", "Can Not Edit Scene",
                    JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        //else

        //make slight modifications to newScene dialog
        newScale.setEnabled( false );
        newBase.setEnabled( false );

        //show dialog
        newDialog.setLocationRelativeTo( this );
        newDialog.setVisible( true );
        //waiting for user to press a button...
        //this thread's execution continues when the dialog is dismissed
        boolean okay = newButtonListener.okayPressed;
        newButtonListener.reset();
        newDialog.setVisible( false );
        if( !okay ) return;
        //else
        Point3f max = newMax.getPoint3f();
        loader.setBounds( max.x, max.y, max.z );
        loader.setName( newName.getText() );
        loader.setDescription( newDescription.getText() );

        //restore newScene dialog to its normal state
        newScale.setEnabled( true );
        newBase.setEnabled( true );
    }


    //popup menu options
    private void newLight()
    {
        //show dialog
        lightDialog.setLocationRelativeTo( this );
        lightDialog.setVisible( true );
        //waiting for user to press a button...
        //this thread's execution continues when the dialog is dismissed
        boolean okay = lightButtonListener.okayPressed;
        lightButtonListener.reset();
        lightDialog.setVisible( false );
        if( !okay ) return;
        //else
        char type = 'a';
        if( lightAmbient.isSelected() ) type = 'a';
        else if( lightDirectional.isSelected() ) type = 'd';
        else if( lightPoint.isSelected() ) type = 'p';
        else if( lightSpot.isSelected() ) type = 's';
        Color color = lightColor.getColor();
        Point3f location = lightLocation.getPoint3f();//will be ignored for a, d
        Vector3f direction = lightDirection.getVector3f();//ignored for a, p
        float spreadAngle = ((Number) (lightSpread.getValue())).floatValue();
        GameSceneLoader.LightDefinition lightDef = loader.new LightDefinition(
                type, color, location.x, location.y, location.z, direction,
                spreadAngle
        );
        lights.add( new DefaultMutableTreeNode( lightDef ) );
        scene.removeChild( loadedScene );
        loadedScene.addChild( lightDef.getLight() );
        scene.addChild( loadedScene );
        loader.addLightDefinition( lightDef );

        //sync, maybe
        changesSinceSync++;
        if( changesSinceSync == CHANGES_FOR_SYNC ) sync( true );
        else syncTree( true );

        repaint();
    }

    private void newTextureDef()
    {
        //show dialog
        textureDialog.setLocationRelativeTo( this );
        textureDialog.setVisible( true );
        //waiting for user to press a button...
        //this thread's execution continues when the dialog is dismissed
        boolean okay = textureButtonListener.okayPressed;
        textureButtonListener.reset();
        textureDialog.setVisible( false );
        if( !okay ) return;
        //else
        int properties = ((Integer) (textureProperties.getValue())).intValue();
        int texnum = ((Integer) (textureTexnum.getValue())).intValue();
        GameSceneLoader.TextureDefinition textureDef = null;
        try {
            textureDef = loader.new TextureDefinition( properties, texnum,
                    textureLocation.getText(), textureBorder.isSelected() );
            textureDef.getAppearance().setTransparencyAttributes( transparency );
            textures.add( new DefaultMutableTreeNode( textureDef ) );
            loader.addTextureDefinition( textureDef );

            //sync, maybe
            changesSinceSync++;
            if( changesSinceSync == CHANGES_FOR_SYNC ) sync( true );
            else syncTree( true );

            repaint();
        }
        catch( MalformedURLException exc ) {
            System.out.println( exc );
            JOptionPane.showMessageDialog( this, "The image location is invalid.",
                    "New Texture Error", JOptionPane.ERROR_MESSAGE );
        }
        catch( IOException exc ) {
            JOptionPane.showMessageDialog( this, "Error: " + exc.getMessage(),
                    "New Texture Error", JOptionPane.ERROR_MESSAGE );
        }
        catch( IllegalArgumentException exc ) {
            JOptionPane.showMessageDialog( this, "Error: " + exc.getMessage(),
                    "New Texture Error", JOptionPane.ERROR_MESSAGE );
        }
    }

    private void newShapeDef()
    {
        //show dialog
        shapeDialog.setLocationRelativeTo( this );
        shapeDialog.setVisible( true );
        //waiting for user to press a button...
        //this thread's execution continues when the dialog is dismissed
        boolean okay = shapeButtonListener.okayPressed;
        shapeButtonListener.reset();
        shapeDialog.setVisible( false );
        if( !okay ) return;
        //else
        int properties = ((Integer) (shapeProperties.getValue())).intValue();
        int shapenum = ((Integer) (shapeShapenum.getValue())).intValue();
        Point3f vertices[] = new Point3f[ 4 ];
        vertices[ 0 ] = shapeP0.getPoint3f();
        vertices[ 1 ] = shapeP1.getPoint3f();
        vertices[ 2 ] = shapeP2.getPoint3f();
        vertices[ 3 ] = shapeP3.getPoint3f();
        GameSceneLoader.ShapeDefinition shapeDef = loader.new ShapeDefinition(
                vertices, shapenum, properties
        );
        shapes.add( new DefaultMutableTreeNode( shapeDef ) );
        try {
            loader.addShapeDefinition( shapeDef );
        }
        catch( IllegalArgumentException exc ) {
            JOptionPane.showMessageDialog( this, "Error: " + exc.getMessage(),
                    "New Shape Error", JOptionPane.ERROR_MESSAGE );
        }

        //sync, maybe
        changesSinceSync++;
        if( changesSinceSync == CHANGES_FOR_SYNC ) sync( true );
        else syncTree( true );

        repaint();
    }

    private void newDataDef()
    {
        //show dialog
        dataDialog.setLocationRelativeTo( this );
        dataDialog.setVisible( true );
        //waiting for user to press a button...
        //this thread's execution continues when the dialog is dismissed
        boolean okay = dataButtonListener.okayPressed;
        dataButtonListener.reset();
        dataDialog.setVisible( false );
        if( !okay ) return;
        //else
        Point3f location = dataLocation.getPoint3f();
        int shapenum = ((Integer) (dataShapenum.getValue())).intValue();
        int texnum = ((Integer) (dataTexnum.getValue())).intValue();
        GameSceneLoader.DataDefinition dataDef = loader.new DataDefinition(
                location.x, location.y, location.z, dataXdir.getText(),
                dataYdir.getText(), dataZdir.getText(), shapenum, texnum
        );
        datas.add( new DefaultMutableTreeNode( dataDef ) );
        loader.addDataDefinition( dataDef );
        scene.removeChild( loadedScene );
        loadedScene.addChild( dataDef.getShape3DGroup() );
        scene.addChild( loadedScene );

        //sync, maybe
        changesSinceSync++;
        if( changesSinceSync == CHANGES_FOR_SYNC ) sync( true );
        else syncTree( true );

        repaint();
    }

    private void newAppSpecific()
    {
        String appSpecific = JOptionPane.showInputDialog( this, "Content",
                "New Application Specific", JOptionPane.PLAIN_MESSAGE );
        appSpecifics.add( new DefaultMutableTreeNode( appSpecific ) );
        loader.addApplicationSpecific( appSpecific );

        //sync, maybe
        changesSinceSync++;
        if( changesSinceSync == CHANGES_FOR_SYNC ) sync( true );
        else syncTree( true );

        repaint();
    }

    private void editImmutably()
    {
        Object object = tree.getSelectionPath().getLastPathComponent();
        if( object instanceof DefaultMutableTreeNode ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            object = node.getUserObject();
            if( object == null ) return;
            //else
            if( object instanceof GameSceneLoader.LightDefinition ) {
                GameSceneLoader.LightDefinition lightDef =
                        (GameSceneLoader.LightDefinition) object;
                char type = lightDef.getType();
                if( type == 'a' ) lightAmbient.setSelected( true );
                else if( type == 'd' ) lightDirectional.setSelected( true );
                else if( type == 'p' ) lightPoint.setSelected( true );
                else if( type == 's' ) lightSpot.setSelected( true );
                lightColor.setTuple3f( lightDef.getColor() );
                lightLocation.setTuple3f( lightDef.getLocation() );
                lightDirection.setTuple3f( lightDef.getDirection() );
                lightSpread.setValue( lightDef.getSpreadAngle() );
                newLight();
            }
            else if( object instanceof GameSceneLoader.TextureDefinition ) {
                GameSceneLoader.TextureDefinition textureDef =
                        (GameSceneLoader.TextureDefinition) object;
                textureProperties.setValue( textureDef.getProperties() );
                textureTexnum.setValue( textureDef.getTexnum() );
                textureLocation.setText( textureDef.getLocationRaw() );
                textureBorder.setSelected( textureDef.isBorder() );
                newTextureDef();
            }
            else if( object instanceof GameSceneLoader.ShapeDefinition ) {
                GameSceneLoader.ShapeDefinition shapeDef =
                        (GameSceneLoader.ShapeDefinition) object;
                shapeProperties.setValue( shapeDef.getProperties() );
                shapeShapenum.setValue( shapeDef.getShapenum() );
                Point3f[] vertices = shapeDef.getGivenVertices();
                shapeP0.setTuple3f( vertices[ 0 ] );
                shapeP1.setTuple3f( vertices[ 1 ] );
                shapeP2.setTuple3f( vertices[ 2 ] );
                shapeP3.setTuple3f( vertices[ 3 ] );
                newShapeDef();
            }
            else if( object instanceof GameSceneLoader.DataDefinition ) {
                GameSceneLoader.DataDefinition dataDef =
                        (GameSceneLoader.DataDefinition) object;
                dataLocation.setTuple3f( dataDef.getGameSceneCoords() );
                dataShapenum.setValue( dataDef.getShapenum() );
                dataTexnum.setValue( dataDef.getTexnum() );
                dataXdir.setText( dataDef.getXdir() );
                dataYdir.setText( dataDef.getYdir() );
                dataZdir.setText( dataDef.getZdir() );
                newDataDef();
            }
            else if( object instanceof String ) {//application specific
                //editing not supported
                newAppSpecific();
            }
        }
    }

    private void delete()
    {
        Object object = tree.getSelectionPath().getLastPathComponent();
        if( object instanceof DefaultMutableTreeNode ) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) object;
            object = node.getUserObject();
            if( object == null ) return;
            //else
            if( object instanceof GameSceneLoader.LightDefinition ) {
                GameSceneLoader.LightDefinition lightDef =
                        (GameSceneLoader.LightDefinition) object;
                loader.removeLightDefinition( lightDef );
                scene.removeChild( loadedScene );
                loadedScene.removeChild( lightDef.getLight() );
                scene.addChild( loadedScene );
                syncTree( true );
            }
            else if( object instanceof GameSceneLoader.TextureDefinition ) {
                GameSceneLoader.TextureDefinition textureDef =
                        (GameSceneLoader.TextureDefinition) object;
                int dependencies = loader.getDependencyCount( textureDef );
                if( dependencies != 0 ) {
                    if( JOptionPane.showConfirmDialog( this, "The selected texture" +
                                " has " + dependencies + " dependencies. Continue?",
                                "Confirm Delete Texture", JOptionPane.YES_NO_OPTION )
                            == JOptionPane.YES_OPTION ) {
                        loader.removeTextureDefinition( textureDef );
                        sync( true );//full sync to show removed dataDefs
                    }
                    else return;
                }
                else {
                    loader.removeTextureDefinition( textureDef );
                    sync( true );
                }
            }
            else if( object instanceof GameSceneLoader.ShapeDefinition ) {
                GameSceneLoader.ShapeDefinition shapeDef =
                        (GameSceneLoader.ShapeDefinition) object;
                int dependencies = loader.getDependencyCount( shapeDef );
                if( dependencies != 0 ) {
                    if( JOptionPane.showConfirmDialog( this, "The selected shape" +
                                " has " + dependencies + " dependencies. Continue?",
                                "Confirm Delete Shape", JOptionPane.YES_NO_OPTION )
                            == JOptionPane.YES_OPTION ) {
                        loader.removeShapeDefinition( shapeDef );
                        sync( true );//full sync to show removed dataDefs
                    }
                    else return;
                }
                else {
                    loader.removeShapeDefinition( shapeDef );
                    sync( true );
                }
            }
            else if( object instanceof GameSceneLoader.DataDefinition ) {
                GameSceneLoader.DataDefinition dataDef =
                        (GameSceneLoader.DataDefinition) object;
                loader.removeDataDefinition( dataDef );
                scene.removeChild( loadedScene );
                loadedScene.removeChild( dataDef.getShape3DGroup() );
                scene.addChild( loadedScene );
                syncTree( true );
            }
            else if( object instanceof String ) {//application specific
                String appSpecific = (String) object;
                loader.removeApplicationSpecific( appSpecific );
                syncTree( true );
            }
        }

        repaint();
    }


    //utilities
    private boolean promptForSave()
    {
        //returns: true if the caller should stop, false for continue
        if( fileOpen == false || promptForSave == false ) return false;//no need
        //else
        switch( JOptionPane.showConfirmDialog( this, "Do you want to save first?",
                "Unsaved Changes", JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE ) ) {
            case JOptionPane.NO_OPTION: return false;
            case JOptionPane.CANCEL_OPTION: return true;
            case JOptionPane.YES_OPTION: return save();
            default: //never happens
                return true;
        }
    }

    private void removeScene()
    {
        scene.removeAllChildren();
        loadedScene.removeAllChildren();
        loadedScene.addChild( detailsGroup );
        textures.removeAllChildren();
        shapes.removeAllChildren();
        datas.removeAllChildren();
        lights.removeAllChildren();
        appSpecifics.removeAllChildren();
        transparencySlider.setValue( new Integer( 0 ) );
    }

    private void sync( boolean showMessage )
    {
        showMessage = false;
        if( loader == null ) return;//scene not there
        //remove and reattach the scene using loader.attachTo( Group group )
        scene.removeAllChildren();
        loadedScene.removeAllChildren();
        loader.attachTo( loadedScene );
        loadedScene.addChild( detailsGroup );
        scene.addChild( loadedScene );
        syncTree( false );//message already visible or not needed, don't show
        changesSinceSync = 0;
    }

    private void syncTree( boolean showMessage )
    {
        showMessage = false;
        lights.removeAllChildren();
        textures.removeAllChildren();
        shapes.removeAllChildren();
        datas.removeAllChildren();
        appSpecifics.removeAllChildren();
        GameSceneLoader.LightDefinition lightDefs[] =
                loader.getLightDefinitions();
        //add everything in reverse order, so recent additions are near the top
        for( int i = lightDefs.length - 1; i >= 0; i-- ) {
            lights.add( new DefaultMutableTreeNode( lightDefs[ i ] ) );
        }
        GameSceneLoader.TextureDefinition textureDefs[] =
                loader.getTextureDefinitions().values().toArray(
                new GameSceneLoader.TextureDefinition[ 0 ]
        );
        //textureDefs and shapeDefs are already in the correct (reverse) order
        for( int i = 0; i < textureDefs.length; i++ ) {
            textures.add( new DefaultMutableTreeNode( textureDefs[ i ] ) );
        }
        GameSceneLoader.ShapeDefinition shapeDefs[] =
                loader.getShapeDefinitions().values().toArray(
                new GameSceneLoader.ShapeDefinition[ 0 ]
        );
        for( int i = 0; i < shapeDefs.length; i++ ) {
            shapes.add( new DefaultMutableTreeNode( shapeDefs[ i ] ) );
        }
        GameSceneLoader.DataDefinition dataDefs[] = loader.getDataDefinitions();
        for( int i = dataDefs.length - 1; i >= 0 ; i-- ) {
            datas.add( new DefaultMutableTreeNode( dataDefs[ i ] ) );
        }
        String appSpecificStrings[] = loader.getApplicationSpecifics();
        for( int i = appSpecificStrings.length - 1; i >= 0 ; i-- ) {
            appSpecifics.add(
                    new DefaultMutableTreeNode( appSpecificStrings[ i ] )
            );
        }
        treeModel.reload();
    }


    //utility classes
    private class PointPanel extends JPanel {
        boolean floatingPoint;
        private JSpinner x, y, z;
        public PointPanel( boolean floatingPoint, Number min, Number max )
        {
            this( floatingPoint, min, max, null );
        }

        public PointPanel( boolean floatingPoint, Number min, Number max,
                Number increment )
        {
            this.floatingPoint = floatingPoint;
            setLayout( new GridLayout( 1, 3 ) );
            if( floatingPoint ) {
                if( increment == null ) increment = ddot1;
                x = new JSpinner( new SpinnerNumberModel( d0, (Double) (min),
                        (Double) (max), increment ) );
                y = new JSpinner( new SpinnerNumberModel( d0, (Double) (min),
                        (Double) (max), increment ) );
                z = new JSpinner( new SpinnerNumberModel( d0, (Double) (min),
                        (Double) (max), increment ) );
            }
            else {
                if( increment == null ) increment = i1;
                x = new JSpinner( new SpinnerNumberModel( i0, (Integer) (min),
                        (Integer) (max), increment ) );
                y = new JSpinner( new SpinnerNumberModel( i0, (Integer) (min),
                        (Integer) (max), increment ) );
                z = new JSpinner( new SpinnerNumberModel( i0, (Integer) (min),
                        (Integer) (max), increment ) );
            }
            add( x );
            add( y );
            add( z );
        }

        public Point3d getPoint3d() { return new Point3d( ((Number) x.getValue()).
                doubleValue(), ((Number) y.getValue()).doubleValue(),
                ((Number) z.getValue()).doubleValue() );
        }

        public Point3f getPoint3f() { return new Point3f( ((Number) x.getValue()).
                floatValue(), ((Number) y.getValue()).floatValue(),
                ((Number) z.getValue()).floatValue() );
        }

        public Point3i getPoint3i() { return new Point3i( ((Number) x.getValue()).
                intValue(), ((Number) y.getValue()).intValue(),
                ((Number) z.getValue()).intValue() );
        }

        public Vector3f getVector3f() { return new Vector3f(
                ((Number) x.getValue()).floatValue(),
                ((Number) y.getValue()).floatValue(),
                ((Number) z.getValue()).floatValue() );
        }

        public Color getColor() { return new Color( ((Number) x.getValue()).
                intValue(), ((Number) y.getValue()).intValue(),
                ((Number) z.getValue()).intValue() );
        }

        public void setColor( Color color )
        {
            x.setValue( color.getRed() );
            y.setValue( color.getGreen() );
            z.setValue( color.getBlue() );
        }

        public void setTuple3i( Tuple3i tuple )
        {
            if( tuple == null ) {
                x.setValue( 0 );
                y.setValue( 0 );
                z.setValue( 0 );
            }
            else {
                x.setValue( tuple.x );
                y.setValue( tuple.y );
                z.setValue( tuple.z );
            }
        }

        public void setTuple3f( Tuple3f tuple )
        {
            if( tuple == null ) {
                x.setValue( 0f );
                y.setValue( 0f );
                z.setValue( 0f );
            }
            else {
                x.setValue( tuple.x );
                y.setValue( tuple.y );
                z.setValue( tuple.z );
            }
        }

        public void setTuple3d( Tuple3d tuple )
        {
            if( tuple == null ) {
                x.setValue( 0.0 );
                y.setValue( 0.0 );
                z.setValue( 0.0 );
            }
            else {
                x.setValue( tuple.x );
                y.setValue( tuple.y );
                z.setValue( tuple.z );
            }
        }

        public void setEnabled( boolean enabled )
        {
            super.setEnabled( enabled );
            x.setEnabled( enabled );
            y.setEnabled( enabled );
            z.setEnabled( enabled );
        }
    }

    private class ButtonListener extends WindowAdapter implements ActionListener {
        JButton okay, cancel;
        JDialog dialog;
        boolean okayPressed;
        ButtonListener( JButton okay, JButton cancel, JDialog dialog )
        {
            this.okay = okay;
            this.cancel = cancel;
            this.dialog = dialog;
            okay.addActionListener( this );
            cancel.addActionListener( this );
            dialog.addWindowListener( this );
        }

        public void actionPerformed( ActionEvent e )
        {
            if( e.getSource() == okay ) okayPressed = true;
            //else okayPressed = false;
            dialog.setVisible( false );
        }

        void reset()
        {
            okayPressed = false;
        }
    }


    public static void main( String args[] )
    {
        new GameSceneBuilder();
    }
}
