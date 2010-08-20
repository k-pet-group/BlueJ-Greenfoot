/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr;

import bluej.Config;
import bluej.BlueJTheme;
import bluej.utility.Debug;
import bluej.utility.FileUtility;
import bluej.utility.EscapeDialog;
import bluej.utility.DialogManager;
import bluej.utility.MiksGridLayout;
import bluej.utility.SortedProperties;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.filechooser.FileFilter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.List;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
/**
 * Dialog for deploying a MIDlet suite. 
 * User specifies attributes needed for the jad and jar files.
 * 
 * @author Cecilia Vargas
 */
public class MIDletDeploymentDialog extends EscapeDialog implements ListSelectionListener 
{
    private static final String MIDLET_FILE = "midlet.defs";
    private static final String EXCLUDE_LABEL = Config.getString( "midlet.deployment.midlets.exclude" );
    private static final String INCLUDE_LABEL = Config.getString( "midlet.deployment.midlets.include" );    
    
    private MIDletDeploymentTableModel tableModel;  
    
    private JTable       table; 
    private JTextField   suiteName, suiteVendor, suiteVersion, suiteDescription;
    private JButton      changeButton, inOutButton, upButton, downButton;
    private JRadioButton runToolkit, createFiles;     
 
    private File verified;  //Directory with preverified files to be put in jar file.
    private File projectDir;
    private File jadFile;
    private File jarFile;
    private File midletsFile;  //The midlet.defs file.    
    private List<String> midlets; //List of midlets in project to display in this dialog.
    
    private Manifest          manifest;    
    private PkgMgrFrame       frame;
    private boolean           ok;   
    private SortedProperties  props;   //Properties of the midlets in the midlet suite.
    private boolean[ ]        exclude; //Whether to exclude a midlet from the midlet
                                       //suite being deployed. This array is initialized
                                       //by the model and manipulated in this dialog.
    /***************************************************************************
     * Constructor.
     * 
     * @param parent     The parent frame of this dialog.
     * @param verified   The directory with the output files of the preverify 
     *                   command. These files will go into the jar file.
     * @param midlets    The current list of midlets in the project. 
     *                   These midlets appear in the dialog table.
     */
    public MIDletDeploymentDialog( PkgMgrFrame parent, File verified, List<String> midlets )
    {      
        super( parent, Config.getString( "midlet.deployment.title" ), true );
        frame = parent;
        projectDir = parent.getProject( ).getProjectDir( );
        this.verified = verified;
        this.midlets = midlets;
        manifest = new Manifest( );
        midletsFile = new File( projectDir, MIDLET_FILE ); 
        loadMidletFile( );
        makeDialog( );
    }

    /***************************************************************************
     * If the midlet.defs file exists, loads it into a Properties object. 
     * This file has info for the dialog and the table model.
     */
    private void loadMidletFile( )
    {
        FileInputStream fis = null;
        props = new SortedProperties( );

        if ( midletsFile.exists( ) ) {
            try {
                fis = new FileInputStream( midletsFile );
                props.load( fis );
                fis.close();
            }
            catch( IOException e ) {
                Debug.reportError( "Problem reading midlets file in root package"); 
                e.printStackTrace();
            }
        }
    }              
         
    /***************************************************************************
     * Builds the main panel that contains the dialog.
     */    
    private void makeDialog( )
    {   
        JPanel panel = new JPanel( new BorderLayout( ) );
        panel.setBorder( BlueJTheme.dialogBorder );        
        panel.add( makeMIDletSuitePanel( ),    BorderLayout.PAGE_START );
        panel.add( makeMIDletsBox( ),          BorderLayout.CENTER     ); 
        panel.add( makeOkCancelButtonPanel( ), BorderLayout.PAGE_END   );
        
        JPanel mainPanel = new JPanel( new BorderLayout( ) );
        mainPanel.setBorder( BlueJTheme.dialogBorder );  
        mainPanel.add( makeRadioPanel( ), BorderLayout.PAGE_START );
        mainPanel.add( panel,             BorderLayout.CENTER     );
     
        getContentPane( ).add( mainPanel );
        pack( );
        DialogManager.centreDialog( this );    
    }

    /***************************************************************************
     * Make the radio button panel for the execution options.
     */    
    private JPanel makeRadioPanel( )            
    {     
        String toolkitText  = Config.getString( "midlet.deployment.runWTK"        );
        String genFilesText = Config.getString( "midlet.deployment.generateFiles" );
        
        runToolkit  = new JRadioButton( toolkitText ,  true );
        createFiles = new JRadioButton( genFilesText , false );       
        
        JPanel radioPanel = new JPanel( new GridLayout( 2, 1 ) ); 
        radioPanel.setBorder( BorderFactory.createEmptyBorder( 0, 10, 0, 10 ) );
        radioPanel.add( runToolkit  );
        radioPanel.add( createFiles );        

        ButtonGroup bGroup = new ButtonGroup( );
        bGroup.add( runToolkit  );
        bGroup.add( createFiles ); 
        
        return radioPanel;
    }     

    /***************************************************************************
     * Builds the panel box for the MIDlet suite fields. This box is made up of 
     * a box for labels and another box for the textfields. The textfields are
     * initialized with the properties that were loaded from the midlet.defs file,
     * of with default values if this file does not exist.
     */    
    private JPanel makeMIDletSuitePanel( )            
    {     
        JPanel panel = new JPanel( new MiksGridLayout( 4, 2, 10, 5) );
        panel.setAlignmentX( LEFT_ALIGNMENT );
        
        String title = Config.getString( "midlet.deployment.suite.title"       ); 
        String name  = Config.getString( "midlet.deployment.suite.name"        );
        String vers  = Config.getString( "midlet.deployment.suite.version"     );        
        String vend  = Config.getString( "midlet.deployment.suite.vendor"      ); 
        String desc  = Config.getString( "midlet.deployment.suite.description" );   
        
        panel.setBorder( BorderFactory.createCompoundBorder(
                             BorderFactory.createTitledBorder( title ),
                             BlueJTheme.generalBorder  ) ); 
        
        JLabel suiteNameLabel        = new JLabel( name );
        JLabel suiteVersionLabel     = new JLabel( vers );       
        JLabel suiteVendorLabel      = new JLabel( vend );       
        JLabel suiteDescriptionLabel = new JLabel( desc );          
        
        //reuse String fields to get info from the midlet.defs file
        name = props.getProperty( "midlet.suite.name", projectDir.getName() );
        vers = props.getProperty( "midlet.suite.version", "1.0" );
        vend = props.getProperty( "midlet.suite.vendor", "BlueJ ME" ); 
        desc = props.getProperty( "midlet.suite.description", "" );       

        panel.add( suiteNameLabel                            );
        panel.add( suiteName = new JTextField( name )        ); 
        panel.add( suiteVersionLabel                         );
        panel.add( suiteVersion = new JTextField( vers )     );
        panel.add( suiteVendorLabel                          );
        panel.add( suiteVendor = new JTextField( vend )      );
        panel.add( suiteDescriptionLabel                     );
        panel.add( suiteDescription = new JTextField( desc ) );    

        return panel;
    }
    
   /***************************************************************************
    * Builds panel with the table of midlets and the buttons to manipulate the
    * individual table entries.
    */
    private Box makeMIDletsBox( )
    {
        tableModel = new MIDletDeploymentTableModel( midlets, projectDir, props ); 
        exclude = tableModel.getExcluded( );
        
        table = new JTable( tableModel );
        table.setRowHeight(25);        
        table.setShowGrid( false );
        table.setOpaque( true );
        table.setDragEnabled( false );
        table.setRowSelectionAllowed( true );
        table.setColumnSelectionAllowed( false );
        table.setBackground( getBackground( ) );        
        table.setIntercellSpacing( new Dimension( ) );
        table.getTableHeader().setReorderingAllowed( false );
        table.getSelectionModel().addListSelectionListener( this );        
        table.setAutoResizeMode( JTable.AUTO_RESIZE_ALL_COLUMNS );        
        table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
        
        TableColumnModel colModel = table.getColumnModel( );
        colModel.getColumn( 2 ).setCellRenderer( new IconColumnRenderer( ) );
        table.getTableHeader( ).setDefaultRenderer( new HeaderRenderer( ) );
        table.setDefaultRenderer( String.class, new MidletTableRenderer( ) );

        colModel.getColumn( 0 ).setPreferredWidth( 90 );
        colModel.getColumn( 1 ).setPreferredWidth( 55 );
        colModel.getColumn( 2 ).setPreferredWidth( 30 );         

        Dimension prefSize       = table.getMaximumSize( );
        Dimension scrollPrefSize = table.getPreferredScrollableViewportSize( );
        JScrollPane scroller = new JScrollPane( table );
        scroller.setPreferredSize( new Dimension (scrollPrefSize.width,
                                                  prefSize.height + 50 ) ); 
        scroller.setBorder( BlueJTheme.generalBorder );
        
        String title = Config.getString( "midlet.deployment.midlets.title" );         
        Box box = new Box( BoxLayout.PAGE_AXIS );
        box.setBorder( BorderFactory.createTitledBorder( title ) );
        box.add( scroller );  
        
        box.add( makeButtonPanel( ) );        
        table.setRowSelectionInterval( 0, 0 );        
        return box;
    }

    /***************************************************************************
     * Builds the panel with the move up, move down, and exclude/include buttons.
     */        
    private JPanel makeButtonPanel( )
    {        
        changeButton = new JButton( Config.getString( "midlet.deployment.midlets.changeicon" ) ); 
        upButton     = new JButton( Config.getString( "midlet.deployment.midlets.moveup"     ) );
        downButton   = new JButton( Config.getString( "midlet.deployment.midlets.movedown"   ) );
        inOutButton  = new JButton( );

        changeButton.addActionListener( new ChangeIconListener( ) );

        inOutButton.addActionListener( new ActionListener( ) {            
		public void actionPerformed( ActionEvent evt ) { 
                     int row = table.getSelectedRow( );
                     exclude[ row ]  = ! exclude[ row ];
                     tableModel.fireTableRowsUpdated( row, row );
                     if ( exclude[ row ] )
                         inOutButton.setText( INCLUDE_LABEL );
                     else
                         inOutButton.setText( EXCLUDE_LABEL );                   
                }        		
	} );                
        upButton.addActionListener( new ActionListener( ) {            
		public void actionPerformed( ActionEvent evt ) { 
                     int row = table.getSelectedRow( );
                     if ( row > 0 ) {
                         tableModel.moveRowUp( row );
                         swapExcluded( row, row - 1 );
                         table.setRowSelectionInterval( row - 1, row - 1 );
                     }
                }        		
	} );
        downButton.addActionListener( new ActionListener( ) {            
		public void actionPerformed( ActionEvent evt ) { 
                     int row = table.getSelectedRow( );
                     if ( row <  ( tableModel.getRowCount( ) - 1 ) ) {
                         tableModel.moveRowDown( row );
                         swapExcluded( row, row + 1 );                         
                         table.setRowSelectionInterval( row + 1, row + 1 );
                     } 
                }        		
	} );                
        JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        buttonPanel.add( changeButton );        
        buttonPanel.add( inOutButton  );
        buttonPanel.add( upButton     );   
        buttonPanel.add( downButton   );  
        return buttonPanel;
    }

    /***************************************************************************
     * Swaps two entries, i and j, in the exclude array.
     */    
    private void swapExcluded( int i, int j )  
    {
        boolean temp = exclude[ j ];
        exclude[ j ] = exclude[ i ];
        exclude[ i ] = temp;
    }
    
    /***************************************************************************
     * Builds the Ok and Cancel button panel.
     */    
    private JPanel makeOkCancelButtonPanel( )
    {        
        JButton okButton     = BlueJTheme.getOkButton( );
        JButton cancelButton = BlueJTheme.getCancelButton( );     
        
	okButton.addActionListener( new ActionListener( )  {
		public void actionPerformed( ActionEvent evt ) { doOk(); }        		
	} );
        cancelButton.addActionListener( new ActionListener( )  {
		public void actionPerformed( ActionEvent evt ) { doCancel(); }        		
	} );
           
        JPanel buttonPanel = new JPanel( new FlowLayout( FlowLayout.RIGHT ) );
        buttonPanel.setBorder( BorderFactory.createEmptyBorder( 10, 0, 0, 0 ) );
        getRootPane( ).setDefaultButton( okButton );
        buttonPanel.add( okButton     );
        buttonPanel.add( cancelButton );   
        return buttonPanel;
    }

    /***************************************************************************
     * Returns true if user pressed Ok button, and there were
     * no problems in writing the midlet.defs file, and the radio button to run
     * the toolkit was selected. Returns false if user pressed Cancel button, or
     * if the create-files-only radio button was selected, or if there was a 
     * problem in writing the midlet.defs file. See doCancel( ) and doOk( ).
     */        
    boolean runEmulator( )
    {
        ok = false;
        setVisible( true );  //returns after Ok or Cancel
        return ok;
    }

    /***************************************************************************
     * Invoked when Cancel button is pressed.
     */    
    private void doCancel( ) 
    {
        ok = false;
        setVisible( false );        
    }

    /***************************************************************************
     * Invoked when Ok button is pressed.
     */    
    private void doOk( )
    {
        createJar( );
        createJad( );        
        ok = false;
        if ( saveMIDletSuite( ) && runToolkit.isSelected( ) )
            ok = true;
        setVisible( false );
        frame.setStatus( Config.getString( "pkgmgr.midlet.deploying" ) );           
    }

    /***************************************************************************
     * Saves into a brand new midlet.defs file the midlets info in the table. Also
     * save the midlet suite info, like name and version, contained in the JTextFields.
     * Returns true if there were no IO exceptions, otherwise it returns false.
     */    
    private boolean saveMIDletSuite( )
    {
        props = new SortedProperties( );
        
        props.setProperty( "midlet.suite.name",        suiteName.getText().trim() );
        props.setProperty( "midlet.suite.version",     suiteVersion.getText().trim() );        
        props.setProperty( "midlet.suite.vendor",      suiteVendor.getText().trim() );
        props.setProperty( "midlet.suite.description", suiteDescription.getText().trim() );      

        tableModel.saveTableToProps( props );
  
        midletsFile.delete();                     
        try {
            midletsFile.createNewFile( );   
            FileOutputStream output = new FileOutputStream( midletsFile );
            props.store(output, "MIDlet suite file");
            output.close();
        }
        catch (IOException e) {
            Debug.reportError("Error saving MIDlet suite file " + midletsFile + ": " + e);
            return false;
        }
        return true;
    }
    
    /***************************************************************************
     * Jars the preverified files, and the resources directoy if there is one.
     */
    private void createJar( )
    {   
        OutputStream    oStream = null;
        JarOutputStream jStream = null; 
        verifySuiteTextFields( );    
        jarFile = new File( projectDir, projectDir.getName( ) + ".jar" );                 
        try { 
            // Create the manifest 
            Attributes attr = manifest.getMainAttributes();   
            attr.put( Attributes.Name.MANIFEST_VERSION, "1.0" );
            attr.putValue( "MIDlet-Name",    suiteName.getText( )    );   
            attr.putValue( "MIDlet-Version", suiteVersion.getText( ) );
            attr.putValue( "MIDlet-Vendor",  suiteVendor.getText( )  ); 
            // Create the jar file with the manifest.
            oStream = new FileOutputStream( jarFile );
            jStream = new JarOutputStream( oStream, manifest );

            writeDirToJar( verified,  "", jStream, jarFile.getCanonicalFile( ) );
            File resources = new File( projectDir, "res" );
            if ( resources.exists( ) ) 
                writeDirToJar( resources, "", jStream, jarFile.getCanonicalFile( ) );

        }
        catch( IOException e ) {
            DialogManager.showError( frame, "error-writing-jar" );
            Debug.reportError( "Problem writing jar file: " + e );            
        } finally {
            try {
                if( jStream != null )
                    jStream.close();
            } catch ( IOException e ) {}
        }
    }

    /***************************************************************************
     * Write the contents of a directory to a jar stream. 
     * Recursively called for subdirectories.
     */
    private void writeDirToJar(File sourceDir,          String pathPrefix,
                               JarOutputStream jStream, File outputFile   )
                               throws IOException {
        File[] dir = sourceDir.listFiles();        
        for( int i = 0; i < dir.length; i++ ) {            
            if( dir[i].isDirectory( ) ) 
                
                writeDirToJar( dir[i],  pathPrefix + dir[i].getName( ) + "/",
                               jStream, outputFile                             ); 
            else
                writeJarEntry( dir[i], jStream, pathPrefix + dir[i].getName( ) );          
        }
    }

    /***************************************************************************
     * Writes a jar file entry to the jar output stream. The entryName should always
     * be a path with / separators, not the platform-dependent File.separator.
     */
    private void writeJarEntry( File file, JarOutputStream jStream, String entryName )
                                throws IOException {
        InputStream in = null;        
        try {            
            jStream.putNextEntry( new ZipEntry( entryName ) );
            in = new FileInputStream( file );            
            FileUtility.copyStream( in, jStream );
        }
        catch( ZipException ze ) {
            Debug.message( "Problem writing zipentry to jarfile " + ze );
        }
        finally {
            if( in != null )
                in.close( );
        }
    }    
    
    /***************************************************************************
     * Creates the application descriptor file. Unlike the jar's manifest
     * atttributes, jad attributes cannot have continuation lines. In the jad, 
     * each attribute name and its value have to be in one line. (Cecilia could 
     * not find a line length limit in the specs. In testing, she tried very 
     * long lines which did not break anything.)
     */         
    private void createJad( )
    {
        jadFile = new File( projectDir, projectDir.getName( ) + ".jad" );   
        PrintWriter pw = null;       
        try {     
            pw = new PrintWriter( new FileOutputStream( jadFile ) );
            pw.println( "MIDlet-Name: "    + suiteName.getText( )    );
            pw.println( "MIDlet-Version: " + suiteVersion.getText( ) );
            pw.println( "MIDlet-Vendor: "  + suiteVendor.getText( )  );             
            String description = suiteDescription.getText().trim();
            if ( ! description.equals( "" ) )
                pw.println( "MIDlet-Description: " + description ); 
            pw.println( "MicroEdition-Profile: MIDP-2.0" );
            pw.println( "MicroEdition-Configuration: CLDC-1.1" );
            pw.println( "MIDlet-Jar-URL: "  + jarFile.getName() );
            pw.println( "MIDlet-Jar-Size: " + jarFile.length()  );
            tableModel.writeTableToPrintWriter( pw );
        }
        catch ( IOException ioe ) {
            Debug.message( "Problem writing jad file" );
            ioe.printStackTrace();
        }
        finally {
            if( pw != null ) {
                pw.flush( );
                pw.close( );
            }   
        }
    }
    
    /***************************************************************************
     * Returns the application descriptor file. 
     */          
    File getJadFile( ) { return jadFile; }    
    
    /***************************************************************************
     * Sets the midletsuite name, vendor and version to default values if they
     * are blank. These textfields are used in the jar's manifest and in the
     * jad, and they cannot be blank, as per the specification.
     */          
    private void verifySuiteTextFields( )
    {
        String vendor  = suiteVendor.getText().trim();
        String version = suiteVersion.getText().trim();
        String name    = suiteName.getText().trim();  
       
        if ( vendor.equals( "" )  ) vendor  = "BlueJ ME";
        if ( version.equals( "" ) ) version = "1.0";
        if ( name.equals( "" )    ) name    = projectDir.getName( );
        
        suiteVendor.setText(  vendor  );
        suiteVersion.setText( version );
        suiteName.setText(    name    ); 
    }    
       
    
    /***************************************************************************
     * Called when selected row changes.
     */    
    public void valueChanged( ListSelectionEvent lse )
    {        
        if ( lse.getValueIsAdjusting( ) )  // ignore mouse down, dragging, etc.
            return;            
        
        if ( table.isRowSelected( 0 ) ) 
            upButton.setEnabled( false ); // The first row can't be moved up.
        else
            upButton.setEnabled( true );  
        
        if ( table.isRowSelected( tableModel.getRowCount( ) - 1  ) ) 
            downButton.setEnabled( false ); // The last row can't be moved down.
        else
            downButton.setEnabled( true ); 

        if ( exclude[ table.getSelectedRow( ) ] )
            inOutButton.setText( INCLUDE_LABEL );
        else
            inOutButton.setText( EXCLUDE_LABEL );
     }    

    /***************************************************************************
     * Renderer for the table header. This class is implemented to disable the
     * default highlighting of column headers when these are clicked.
     */       
    private class HeaderRenderer extends DefaultTableCellRenderer  
    {
        public Component getTableCellRendererComponent( JTable t, Object value, 
                                                        boolean isSelected,
                                                        boolean f, int r, int c ) { 
            setText( (String) value ); 
            setHorizontalAlignment( JLabel.CENTER );            
            if ( isSelected ) 
                setBackground( table.getBackground( ) );
            return this;
        }        
    }
    
    /***************************************************************************
     * Renderer to make midlet table look like a list (similar to BlueJ Inspector).
     * This renderer is used for the name and class columns. 
     */     
    private class MidletTableRenderer extends DefaultTableCellRenderer        
    {
        public Component getTableCellRendererComponent( JTable table,       Object value, 
                                                        boolean isSelected, boolean hasFocus, 
                                                        int row,            int column ) {
            if ( isSelected )
                setBackground( table.getSelectionBackground( ) );
            else 
                setBackground( table.getBackground( )          );       
            Border b = BorderFactory.createLineBorder( getBackground( ), 3 );
            setBorder( b );
            setText( (String) value );   
            setHorizontalAlignment( JLabel.LEADING );
            if ( column == 1 ) {
                setBackground( Color.white );
                b = BorderFactory.createLineBorder( Color.gray );
                b = BorderFactory.createCompoundBorder( getBorder( ), b );
                setBorder( b );
            }
            if ( exclude[ row ] )
                setEnabled( false );
            else 
                setEnabled( true );
            return this;
        }
    }
    
    /***************************************************************************
     * Renderer for the ImageIcon column. It overrides the default renderer to
     * disable the table cell if the midlet is excluded.
     */         
    private class IconColumnRenderer extends DefaultTableCellRenderer        
    {
        public Component getTableCellRendererComponent( JTable t, Object value,
                                                        boolean isSelected, 
                                                        boolean f, int r, int c ) {
            setIcon( ( ImageIcon ) value );
            setText( "" );
            setHorizontalAlignment( JLabel.CENTER );             

            if ( isSelected )
                setBackground( table.getSelectionBackground( ) );
            else 
                setBackground( table.getBackground( )          );     
            
            if ( exclude[ r ] )
                setEnabled( false );
            else 
                setEnabled( true );            
            return this; 
        }
    } 

    /***************************************************************************
     * Handles the change of the midlet icon file through a file chooser. 
     * The icon files for a project are in directory res/icons, so if the
     * chosen file is not in this directory, the file is copied into it.
     */    
    private class ChangeIconListener implements ActionListener 
    {                
        public void actionPerformed( ActionEvent event )
        {
            String iconsDir = projectDir + File.separator + MIDletDeployer.ICONS_DIR;
            JFileChooser chooser = new JFileChooser( iconsDir );                   
            chooser.setFileFilter( new IconFileFilter( ) );
            chooser.setFileSelectionMode( JFileChooser.FILES_AND_DIRECTORIES );
            chooser.setDialogTitle( Config.getString( "midlet.deployment.filechooser.title" ) );
                
            int returnVal = chooser.showOpenDialog( getParent( ) );                  
            if ( returnVal == JFileChooser.APPROVE_OPTION )
            {
                File chosenFile = chooser.getSelectedFile( );           
                File fileInProj = new File( iconsDir, chosenFile.getName( ) );
                
                if ( ! chosenFile.getParent( ).equals( iconsDir ) ) {//chosen file is not in project
                    if ( fileInProj.exists( ) ) { //file with same name already in project
                        if ( DialogManager.askQuestion( frame, "error-midleticon-exists" ) != 0 )
                            return;    //user does not want to overwrite file                  
                    }
                    try {
                        FileUtility.copyFile( chosenFile, fileInProj );
                    }
                    catch (IOException ioe) {
                        Debug.reportError( "Could not copy chosen icon file into <project dir>/res/icons.");
                    }
                } 
                try {  //update the table with the new icon file
                    URL url = fileInProj.toURI().toURL();
                    String selectedFile = url.toString(); 
                    int row = table.getSelectedRow( );
                    tableModel.setValueAt( selectedFile, row , 2 );
                 }
                catch ( java.net.MalformedURLException mue ) {
                    Debug.reportError( "Could not create URL from file selected by chooser." );
                    mue.printStackTrace();
                }                 
            }
         }
    }
    
    /***************************************************************************
     * A FileFilter to accept only valid midlet icon filetypes.
     */
    private class IconFileFilter extends FileFilter
    {
	public boolean accept( File f ) {
		return ( f.getName( ).toLowerCase( ).endsWith( ".png" ) ||
			 f.getName( ).toLowerCase( ).endsWith( ".gif" ) ||
                         f.getName( ).toLowerCase( ).endsWith( ".jpg" ) ||
			 f.getName( ).toLowerCase( ).endsWith( ".jpeg") );                
	}	 
	public String getDescription( ) {
		return Config.getString( "midlet.deployment.filechooser.description" );
	}
     }
}