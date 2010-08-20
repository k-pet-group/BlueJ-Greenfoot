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
import bluej.utility.Debug;
import bluej.utility.SortedProperties;
import java.io.File;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.table.AbstractTableModel;

/**
 * Table model of midlet entries.
 * @author Cecilia Vargas
 */
public class MIDletDeploymentTableModel extends AbstractTableModel
{   
    private List<MIDletTableEntry> midlets;     //List of MIDletTableEntry's to display in table
    private File       projectDir;
    private ImageIcon  defaultIcon; //Default icon for midlets
    private boolean[ ] exclude;     //Whether to exclude a midlet in the dialog.  

    private String[ ] columnNames = {
            Config.getString( "midlet.deployment.midlets.classname") ,
            Config.getString( "midlet.deployment.midlets.name"     ) ,
            Config.getString( "midlet.deployment.midlets.icon"     ) };
      
    public MIDletDeploymentTableModel(List<String> currentList, File proj, SortedProperties props)
    {
        projectDir = proj;
        defaultIcon = getDefaultIcon();
        fillInTable(currentList, props);
    }
    
    public int     getColumnCount( )      { return 3; }

    public int     getRowCount( )         { return midlets.size( );  }

    public String  getColumnName( int c ) { return columnNames[ c ]; }
    
    public Class<?>  getColumnClass(int c ) { return getValueAt( 0, c ).getClass( ); }   
    
    public boolean isCellEditable( int r, int c ) { return c == 1; } //only name is editable

    public Object getValueAt( int r, int c )
    { 
        MIDletTableEntry entry = ( MIDletTableEntry ) midlets.get( r );
        if ( c == 0 ) 
            return entry.getClassName( );
        else if ( c == 1 )
            return entry.getName( );
        else if ( c == 2 )         
            return entry.getIcon( );
        else
            return "ERROR in method getValueAt for row,col: " + r + "," + c;
    }
    
    public void setValueAt( Object value, int r, int c )
    {
        if ( c == 1 || c == 2 ) {  
            MIDletTableEntry entry = (MIDletTableEntry) midlets.get( r );    
            
            if ( c == 1 ) { 
                entry.setName( ( String ) value );               
            } else {
                 try {
                    URL url = new URL( ( String ) value );
                    entry.setIcon( new ImageIcon( url ) );
                } 
                catch ( MalformedURLException mue ) {                     
                    Debug.reportError( "In setValueAt, could not create ImageIcon from value passed in." );
                }                 
            }          
            midlets.set( r, entry ); 
            fireTableCellUpdated( r, c );
        }     
    }
    
    boolean[] getExcluded( ) { return exclude; }
            
    /***************************************************************************
     * Fill in the table of midlets. Data comes from merging the Properties and
     * List passed as arguments. The List is the current list of midlets
     * in the project. Properties props has the data loaded from the midlet.defs
     * file. Because midlets.defs has the midlets that were shown in the dialog
     * the last time the dialog was displayed, we display all the midlet classes
     * in list current but taking the name and icon from props. If the
     * midlet class is not in props, we display it with default name and icon.
     * Midlets in props but not in the list are ignored.    
     * The method also allocates and fills in the array exclude which indicates
     * whether a midlet is to be excluded or not in the dialog. Excluded midlets
     * appear in the dialog as disabled and are excluded from the jad file.
     * 
     * <p>Note: icon file names in midlet.defs are in URL format, for example:
     * file\:/C\:/BlueJProjects/MEprojects/WTKdemo/res/icons/myicon.gif
     * 
     * @param current  List of current midlets in the project. 
     * @param props    Properties that were loaded from the midlet.defs file
     */ 
    private void fillInTable(List<String> current, SortedProperties props)
    {   
        String    cl;    // Fully qualified name of the MIDlet class.
        String    icon;  // Name of the icon file.
        String    name;  // Name of the midlet to be displayed in the emulator.
        String    excl;  // Whether to exclude the midlet from the deployed suite.
        ImageIcon image; // ImageIcon object created from the icon file.

        midlets = new ArrayList<MIDletTableEntry>();  //List to fill in.        

        exclude = new boolean[ current.size( ) ];
        for ( int i = 0; i < exclude.length ; i++ ) {
            exclude[ i ] = false;
        }
                            
        int j = 0;
        for ( int i = 1; ( cl = props.getProperty( "midlet" + i + ".class" ) ) != null; i++ )
        {
            if ( current.contains( cl ) )
            {
                current.remove( cl ); //remove cuz after loop we want only new midlets in current       
                name = props.getProperty(  "midlet" + i + ".name"    );
                icon = props.getProperty(  "midlet" + i + ".icon"    ); 
                excl = props.getProperty(  "midlet" + i + ".exclude" );
                exclude[ j ] = excl.equals( "true" );
                j++;
                image = defaultIcon;
                try {
                    image = new ImageIcon( new URL( icon ) );
                } 
                catch ( MalformedURLException mue ) {                     
                    Debug.reportError( 
                         "Could not create ImageIcon from midlet.defs info for midlet " 
                          + name + ". The icon String passed to URL constructor was " + icon );
                }                 
                midlets.add( new MIDletTableEntry( name, image, cl ) );
            }
        }        
        // Add to the table what is left in the current list. These are new
        // midlets that were not displayed before in the dialog so we assign
        // default values to their names and icons.      
        for ( int i = 0 ; i < current.size( ) ; i++ ) {
            cl = (String) current.get( i ); //qualified class name            
            name = cl;       
            //Strip off class name after the last dot if there are package names
            int pos = cl.lastIndexOf(".");
            if ( pos > 0 ) name = cl.substring( pos + 1 );
            midlets.add( new MIDletTableEntry( name, defaultIcon, cl ) ); 
        } 
    }

    /***************************************************************************
     * Return the default icon, which is in <project dir>/res/icons/default.png.
     * However, if this file does not exist, we have to fail gracefully. This 
     * file is created by BlueJ when the ME project is created, so if the file
     * does not exist it is probably because the user deleted it to not have a
     * default icon.
     */     
    private ImageIcon getDefaultIcon( )
    {
        String defaultIconFilename = MIDletDeployer.ICONS_DIR + File.separator + 
                                     MIDletDeployer.DEFAULT_MIDLET_ICON;
        File file = new File( projectDir, defaultIconFilename  );
        if ( file.exists( ) )
            try {
                return new ImageIcon( file.toURI( ).toURL( ) );
            } catch ( java.net.MalformedURLException mue ) {
                Debug.reportError( "For some strange reason BlueJ could not create a default icon." +
                                   " This should never happen. Something is goofy.");
            }
        return new ImageIcon( );
    }  
    
    /***************************************************************************
     * Save the table of midlets into a Properties object.
     * @param props   Properties objects into which to save the table.
     */    
    void saveTableToProps( SortedProperties props )
    {
        int i = 1;
        for (Iterator<MIDletTableEntry> iterator = midlets.iterator(); iterator.hasNext();) {
            MIDletTableEntry entry = ( MIDletTableEntry ) iterator.next();    
            props.setProperty( "midlet" + i + ".name",  entry.getName() );
            props.setProperty( "midlet" + i + ".class", entry.getClassName() );
            String s = entry.getIcon().getDescription( );
            if ( s == null ) s = " ";
            props.setProperty( "midlet" + i + ".icon", s  );
            props.setProperty( "midlet" + i + ".exclude", exclude[ i - 1 ] + "" );
            i++;
        }
    }   

    /***************************************************************************
     * Write out to a PrintWriter the table of midlets.
     * @param pw      PrintWriter to which to write the table.
     */    
    void writeTableToPrintWriter( PrintWriter pw )
    {
        int i = 1; 
        int j = 0;
        for (Iterator<MIDletTableEntry> iterator = midlets.iterator(); iterator.hasNext();)
        {
            MIDletTableEntry entry = ( MIDletTableEntry ) iterator.next(); 
            if ( ! exclude[ j ] ) {                
                String s = entry.getIcon().getDescription( );
                if ( s == null ) 
                    s = " ";  
                else {
                    int index = s.lastIndexOf( "/icons" );
                    s = s.substring( index );
                }
                pw.println( "MIDlet-" + i + ": " + entry.getName() + ", " + s + ", " 
                                                 + entry.getClassName() ); 
                i++;
            }
            j++;
        }
    }        

    /***************************************************************************
     * Move up a row in the table.
     * @param row   Row to move up.
     */        
    void moveRowUp( int row )
    {
        if ( row > 0 ) {
            MIDletTableEntry temp = (MIDletTableEntry) midlets.get( row - 1 );
            midlets.set( row - 1, midlets.get( row ) ); 
            midlets.set( row, temp );             
            fireTableRowsUpdated( row - 1, row );
        }
    }

    /***************************************************************************
     * Move down a row in the table.
     * @param row   Row to move down.
     */        
    void moveRowDown( int row )
    {
        if ( row <  ( midlets.size() - 1 ) )  {
            MIDletTableEntry temp = (MIDletTableEntry) midlets.get( row + 1 ); 
            midlets.set( row + 1, midlets.get( row ) ); 
            midlets.set( row, temp );                     
            fireTableRowsUpdated( row, row + 1 );
        }
    }    

    /***************************************************************************
     * The entry of the table of midlets.
     */        
    private class MIDletTableEntry 
    {
        private ImageIcon icon;        
        private String    name;
        private String    className; //the qualified class name
        
        public MIDletTableEntry( String name, ImageIcon icon, String className)
        {
            this.name      = name;
            this.icon      = icon;
            this.className = className;
        }        
        public String    getName( )      { return name;      }
        public String    getClassName( ) { return className; }
        public ImageIcon getIcon( )      { return icon;      }    
        
        public void setName     ( String name )    { this.name = name; }
        public void setIcon     ( ImageIcon icon ) { this.icon = icon; }   
        
        public String toString( ) {
            return name + " ," + className + " ," + icon.getDescription( );
        }
    }
}
