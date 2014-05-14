/*
 This file is part of the BlueJ program. 
 Copyright (C) 2012,2013,2014  Michael Kolling and John Rosenberg 
 
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
package bluej.prefmgr;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.filefilter.DirectoryFilter;

/**
 * "Interface" preference panel. Settings for what to show (teamwork, testing tools etc)
 * and interface language.
 * 
 * @author Davin McCall
 */
public class InterfacePanel extends JPanel
        implements PrefPanelListener, ActionListener, ItemListener
{
    private static final String toolkitDir = "bluej.javame.toolkit.dir";

    private JCheckBox showTestBox;
    private JCheckBox showTeamBox;
    private JCheckBox showJavaMEBox;
    private JLabel toolkitDirLabel;
    private JTextField toolkitDirField;
    private JButton toolkitBrowseButton;
    
    private ArrayList<String> allLangsInternal;
    private JComboBox langDropdown;
    
    private JCheckBox accessibility;
    
    public InterfacePanel()
    {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        add(box);
        
        setBorder(BlueJTheme.generalBorder);

        box.add(Box.createVerticalGlue());

        if(!Config.isGreenfoot()) {
            box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

            JPanel testPanel = new JPanel(new GridLayout(0,1,0,0));
            {
                testPanel.setBorder(BorderFactory.createCompoundBorder(
                                              BorderFactory.createTitledBorder(
                                                     Config.getString("prefmgr.misc.tools.title")),
                                              BlueJTheme.generalBorder));
                testPanel.setAlignmentX(LEFT_ALIGNMENT);

                showTestBox = new JCheckBox(Config.getString("prefmgr.misc.showTesting"));
                testPanel.add(showTestBox);

                showTeamBox = new JCheckBox(Config.getString("prefmgr.misc.showTeam"));
                testPanel.add(showTeamBox);
                
                showJavaMEBox = new JCheckBox(Config.getString("prefmgr.misc.showJavaME"));
                testPanel.add(showJavaMEBox);
                
                JPanel toolkitPanel = new JPanel( new BorderLayout( 5, 0 ) );
                {
                    toolkitDirLabel = new JLabel( Config.getString( "prefmgr.misc.wtk.dir.label" ) );
                    toolkitDirField = new JTextField(15);
                    toolkitBrowseButton = new JButton( Config.getString( "prefmgr.misc.wtk.button" ) );
                    toolkitPanel.add( toolkitDirLabel,     BorderLayout.WEST   );
                    toolkitPanel.add( toolkitDirField,     BorderLayout.CENTER );
                    toolkitPanel.add( toolkitBrowseButton, BorderLayout.EAST   );                   
                }
                testPanel.add( toolkitPanel );
            }
            box.add(testPanel);

            box.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
            
            showJavaMEBox.addItemListener( this ); 
            toolkitBrowseButton.addActionListener( this );
        }
        
        JPanel langPanel = new JPanel();
        {
            langPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            Config.getString("prefmgr.interface.language.title")),
                            BlueJTheme.generalBorder)
                    );
            langPanel.setAlignmentX(LEFT_ALIGNMENT);
            langPanel.setLayout(new BoxLayout(langPanel, BoxLayout.Y_AXIS));
            
            Box langSelBox = new Box(BoxLayout.X_AXIS);
            {
                langSelBox.add(new JLabel(Config.getString("prefmgr.interface.language") + ":"));
                langSelBox.add(Box.createHorizontalStrut(BlueJTheme.componentSpacingSmall));

                allLangsInternal = new ArrayList<String>();
                List<String> allLangsReadable = new ArrayList<String>();
                
                for (int i = 1; ; i++) {
                    String langString = Config.getPropString("bluej.language" + i, null);
                    if (langString == null) {
                        break;
                    }
                    
                    // The format of a language string is:
                    //    internal-name:display-name:iso3cc
                    // The iso3cc (ISO country code) is optional.
                    
                    int colonIndex = langString.indexOf(':');
                    if (colonIndex == -1) {
                        continue; // don't understand this one
                    }
                    
                    int secondColon = langString.indexOf(':', colonIndex + 1);
                    if (secondColon == -1) {
                        secondColon = langString.length();
                    }
                    
                    allLangsInternal.add(langString.substring(0, colonIndex));
                    allLangsReadable.add(langString.substring(colonIndex + 1, secondColon));
                }
                
                if (allLangsInternal.isEmpty()) {
                    // Guard against modified or corrupted bluej.defs file
                    allLangsInternal.add(Config.language);
                    allLangsReadable.add(Config.language);
                }
                
                String [] langs = new String[allLangsReadable.size()];
                allLangsReadable.toArray(langs);

                langDropdown = new JComboBox(langs);
                langSelBox.add(langDropdown);
            }
            langSelBox.setAlignmentX(0.0f);
            langPanel.add(langSelBox);
            
            langPanel.add(Box.createVerticalStrut(BlueJTheme.componentSpacingSmall));
            
            JLabel t = new JLabel(Config.getString("prefmgr.interface.language.restart"));
            t.setAlignmentX(0.0f);
            langPanel.add(t);
        }
        box.add(langPanel);        
        
        JPanel accessibilityPanel = new JPanel();
        {
            accessibilityPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createTitledBorder(
                            Config.getString("prefmgr.accessibility.title")),
                            BlueJTheme.generalBorder)
                    );
            accessibilityPanel.setAlignmentX(LEFT_ALIGNMENT);
            accessibilityPanel.setLayout(new BoxLayout(accessibilityPanel, BoxLayout.Y_AXIS));
            
            accessibility = new JCheckBox(Config.getString("prefmgr.accessibility.support"));
            accessibilityPanel.add(accessibility);
            
            accessibilityPanel.add(Box.createVerticalStrut(BlueJTheme.componentSpacingSmall));
        }
        box.add(accessibilityPanel);
    }
    
    @Override
    public void beginEditing()
    {
        if(!Config.isGreenfoot()) {
            showTestBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS));
            showTeamBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_TEAM_TOOLS));
            showJavaMEBox.setSelected(PrefMgr.getFlag(PrefMgr.SHOW_JAVAME_TOOLS));
                        
            if ( showJavaMEBox.isSelected( ) ) {
                toolkitDirField.setText( Config.getPropString( toolkitDir, "" ) );
                enableToolkitPanel( true ); 
            } else {
                toolkitDirField.setText( "" );
                enableToolkitPanel( false );   
            }
        }

        String currentLang = Config.getPropString("bluej.language", "english");
        int curLangIndex = allLangsInternal.indexOf(currentLang);
        if (curLangIndex == -1) {
            curLangIndex = 0;
        }
        langDropdown.setSelectedIndex(curLangIndex);
        
        accessibility.setSelected(PrefMgr.getFlag(PrefMgr.ACCESSIBILITY_SUPPORT));
    }

    /**
     * Gray out or not the components in the toolkit panel depending on the argument passed.
     */    
    private void enableToolkitPanel( boolean b ) 
    {
        toolkitDirLabel.setEnabled( b );
        toolkitDirField.setEnabled( b );
        toolkitBrowseButton.setEnabled( b );
    }

    @Override
    public void commitEditing()
    {
        if(!Config.isGreenfoot()) {
            PrefMgr.setFlag(PrefMgr.SHOW_TEST_TOOLS, showTestBox.isSelected());
            PrefMgr.setFlag(PrefMgr.SHOW_TEAM_TOOLS, showTeamBox.isSelected());
            PrefMgr.setFlag(PrefMgr.SHOW_JAVAME_TOOLS, showJavaMEBox.isSelected());            

            PkgMgrFrame.updateTestingStatus();
            PkgMgrFrame.updateTeamStatus();
            PkgMgrFrame.updateJavaMEstatus(); 
            
            String tkDir = toolkitDirField.getText(); 
            if (! tkDir.equals( "" )) {
                Config.putPropString(toolkitDir, tkDir);
            }
        }
        
        Config.putPropString("bluej.language", allLangsInternal.get(langDropdown.getSelectedIndex()));
        
        PrefMgr.setFlag(PrefMgr.ACCESSIBILITY_SUPPORT, accessibility.isSelected());
    }
    
    @Override
    public void revertEditing()
    {
    }
    
    /**
     * Called when user ticks or unticks the 'Show Java ME controls' checkbox. 
     * When ticked, we automatically try to find the location of the Toolkit, 
     * and if we can't find it we pop up a file chooser. When unticked, we
     * gray out the Wireless Toolkit panel.
     */
    @Override
    public void itemStateChanged( ItemEvent event )
    {
        if ( event.getStateChange( ) == ItemEvent.SELECTED ) 
        {       
            enableToolkitPanel( true );
            String toolkitDirectory = tryToFindToolkit( );
            if ( toolkitDirectory.equals( "" ) )
            {
                letUserChooseToolkitDir();
                toolkitDirectory = toolkitDirField.getText().trim();
            }
            else
            {   //we found a toolkit
                toolkitDirField.setText( toolkitDirectory );
            }

            /* Check that names of core library Jar files are valid.
             * Interim fix for #266. We should really provide a GUI to
             * select needed libraries by API-name on a per-project basis.
             */
            checkCoreLibraryJars(toolkitDirectory);
        } 
        else  //checkbox was deselected
        {
            enableToolkitPanel( false );
            toolkitDirField.setText( "" );  
        }
    }
    
    /**
     * Find the Wireless Toolkit. In Windows we search all the filesystem roots.
     * In other systems (Linux, that is) we search the directories in the
     * initializer list of array 'roots' listed below. Note that we search only 
     * one level down from each root. That is, we can find C:\WTK2.5.1 but not
     * C:\someDirectory\WTK2.5.1, or /usr/local/WTK2.5.1 but not
     * /usr/local/mydir/WTK2.5.1
     * 
     * @return String containing our first guess or "" if toolkit not found.
     */    
    private String tryToFindToolkit( )
    {  
        File[] roots = { new File( System.getProperty( "user.home" ) ),
                         new File( "/usr/local"                      ),
                         new File( "/usr/lib"                        ), 
                        };   
        
        if (Config.isWinOS()) {
            roots = File.listRoots( );
        }

        File[] dirs;
        for (int i = 0; i < roots.length ; i++)
        {
            dirs = roots[i].listFiles(new DirectoryFilter());
            if ( dirs != null) {
                for ( int j = 0; j < dirs.length ; j++ ) { 
                    if (isToolkitDirectory(dirs[j])) {
                        return dirs[j].toString();
                    }
                }
            }
        }
        return "";   
    }
    
    /**
     * Check whether a directory fulfills the requirements of being a Wireless
     * Toolkit. The requirements are:
     *   1. That the bin, lib, and docs directories be all present.
     *   2. That there is an emulator file under the bin directory.
     * These were taken from the Unified Emulator Interface specification, 
     * version 1.0.2, dated Apr 2006, http://java.sun.com/j2me/docs/uei_specs.pdf
     * 
     * @param  dirToCheck   directory to check
     * @return true if it fulfills requirements false otherwise
     */ 
    private boolean isToolkitDirectory(File dirToCheck)
    {  
         File file = new File(dirToCheck, "bin");
         if (file.isDirectory())
         {   
             File emulatorInLinux   = new File(file, "emulator");
             File emulatorInWindows = new File(file, "emulator.exe");
             if ((! emulatorInWindows.exists())  &&  (! emulatorInLinux.exists())) {
                 return false; 
             }
         }
         else {
             return false;
         }
             
         file = new File(dirToCheck, "lib");
         File anotherFile = new File(dirToCheck, "docs");
         return file.isDirectory() && anotherFile.isDirectory();
    }    
    
    /**
     * Check that the Jar files named in the Java ME corelibraries property
     * actually exist in the selected toolkit (the names vary with toolkit). If
     * they don't exist, use the Jar files' Manifest properties to find the Jar
     * containing the MIDP 2.0 and CLDC 1.1 libraries, which are the documented
     * default core libraries, and write the Jar file names back into the property.
     * @param tkdir The root folder name for the Wireless toolkit
     */
    private void checkCoreLibraryJars(String tkdir)
    {
        String libs = Config.getPropString("bluej.javame.corelibraries", null);

        // Check to see that all of the Jar files named in the
        // corelibraries property exist
        String libDir = tkdir + File.separator + "lib" + File.separator;
        StringTokenizer st = new StringTokenizer( libs );
        boolean allFilesExist = true; // so far
        while ( st.hasMoreTokens( ) && allFilesExist) {
            allFilesExist = new File( libDir + st.nextToken( ) ).exists();
        }

        // If they do, assume that they are correct
        if(allFilesExist) return;

        // Look at all the Jar files in the toolkit's lib folder
        // To find the ones which implement CLDC 1.1 and MIDP 2.0

        File [] list = new File(libDir).listFiles(
            new FileFilter() {
                public boolean accept(File pathname) { return pathname.getName().endsWith(".jar"); }
            }
        );

        File theCLDCJar = null;
        File theMIDPJar = null;
        for(File f : list) {
            Attributes manifest;
            try {
                manifest = new JarFile(f).getManifest().getMainAttributes();
            } catch (IOException ex) {
                continue;   // Malformed Jar?
            }
            String API = manifest.getValue("API");
            String version = manifest.getValue("API-Specification-Version");
            if(API == null || version == null) continue;

            if(API.equals("CLDC") && version.equals("1.1"))
            {
                theCLDCJar = f;
                if(theMIDPJar != null) break;
            }
            else if(API.equals("MIDP") && version.equals("2.0"))
            {
                theMIDPJar = f;
                if(theCLDCJar != null) break;
            }
        }

        if(theMIDPJar == null || theCLDCJar == null)
        {
            /* Oops. Can't find the needed Jars. Just leave things as they are
             * so that the (wrong) names in the property are the thing that needs
             * fixing-by-hand.
             */
            return;
        }

        /* Write the newly discovered names back into the property string
         * Anyone caching these values won't get the update, but that's currently
         * only the BPClassLoader, and hence only open projects. That's acceptable
         * as this action is a global setting.
         */
        String prop = theCLDCJar.getName() + " " + theMIDPJar.getName();
        Config.putPropString("bluej.javame.corelibraries", prop);
    }
    
    /**
     * Called when the Browse button is pressed.
     */
    public void actionPerformed(ActionEvent e)
    {
        letUserChooseToolkitDir();
    }
    
    /**
     * Pop up a file chooser to let user specify Toolkit location. 
     */
    private void letUserChooseToolkitDir()
    {
        String toolkitDirectory = "";  
        JFileChooser chooser = new JFileChooser();                   
        chooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY );
        chooser.setDialogTitle( Config.getString( "prefmgr.misc.filechooser.title" ) );
                
        int returnVal = chooser.showOpenDialog( getParent( ) ); 
        if (returnVal == JFileChooser.APPROVE_OPTION)
        {
            toolkitDirectory = chooser.getSelectedFile().toString();
            toolkitDirField.setText(toolkitDirectory);  
        }
        else if (returnVal == JFileChooser.CANCEL_OPTION) 
        {
            String s = toolkitDirField.getText().trim();
            if (s.equals(""))
            {
                showJavaMEBox.setSelected(false);
                enableToolkitPanel(false);
            }
        }
    }
}
