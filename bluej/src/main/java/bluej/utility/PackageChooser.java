/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014,2019  Michael Kolling and John Rosenberg
 
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
package bluej.utility;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.plaf.FileChooserUI;
import javax.swing.plaf.basic.BasicFileChooserUI;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.Config;
import bluej.extensions2.SourceType;
import bluej.pkgmgr.Package;
import bluej.utility.filefilter.DirectoryFilter;
import bluej.utility.filefilter.JavaSourceFilter;

/**
 * A file chooser for opening packages.
 *
 * Extends the behaviour of JFileChooser in the following ways:
 * 
 * <ul>
 * <li>Only directories (either BlueJ packages or plain ones) are displayed.
 * <li>BlueJ packages are displayed with a different icon.
 * </ul>
 *
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Markus Ostman
 */
@OnThread(Tag.Swing)
class PackageChooser extends JFileChooser
{
    static private final Icon classIcon = Config.getFixedImageAsIcon("class-icon.png");
    static private final Icon packageIcon = Config.getFixedImageAsIcon("package-icon.png");

    static private final String previewLine1 = Config.getString("utility.packageChooser.previewPane1");
    static private final String previewLine2 = Config.getString("utility.packageChooser.previewPane2");

    private PackageDisplay displayPanel;
    private boolean allowNewFiles = true;

    /**
     * Create a new PackageChooser.
     * 
     * @param startDirectory    the directory to start the package selection in.
     * @param preview           whether to show the package structure preview pane
     * @param showArchives      whether to allow choosing jar and zip files
     */
    public PackageChooser(File startDirectory, boolean preview, boolean showArchives)
    {
        super(startDirectory);

        if (showArchives) {
            setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        }
        else {
            setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        }
        setFileView(new PackageFileView());
        
        if (preview) {
            displayPanel = new PackageDisplay(startDirectory);

            setAccessory(displayPanel);

            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent e) {
                    if (!(e.getNewValue() instanceof File)) {
                        return;
                    }
                    File dir = (File)e.getNewValue();
                    if (dir == null) {
                        return;
                    }
                    if (dir.getName().equals("")) {
                        return;
                    }

                    displayPanel.setDisplayDirectory(dir.getAbsoluteFile());

                    // if (e.getPropertyName().equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) { }
                    // if (e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) { }
                }
            });
        }
    }
    
    /**
     * Set whether this chooser should allow new (non-existing) files to
     * be selected. This does not actually prevent the user from specifying
     * a non-existing file, it just tweaks UI behaviour a bit.
     */
    public void setAllowNewFiles(boolean allowNewFiles)
    {
        this.allowNewFiles = allowNewFiles;
    }
    
    /* (non-Javadoc)
     * @see javax.swing.JFileChooser#accept(java.io.File)
     */
    @Override
    public boolean accept(File f)
    {
        if (f.isDirectory())
            return true;
        
        String fname = f.getName();
        return fname.endsWith(".jar") || fname.endsWith(".JAR") ||
                fname.endsWith(".zip") || fname.endsWith(".ZIP")
                || (Config.isGreenfoot() && (fname.endsWith(".gfar") || fname.endsWith(".GFAR") || 
                        fname.endsWith(".sb")));
    }

    /**
     *  A directory was double-clicked. If this is a BlueJ package, consider
     *  this a package selection and accept it as the "Open" action, otherwise
     *  just traverse into the directory.
     */
    @Override
    public void setCurrentDirectory(File dir)   // redefined
    {
        if (Package.isPackage(dir)) {
            setSelectedFile(dir);
            super.approveSelection();
        }
        else{
            super.setCurrentDirectory(dir);
            if (allowNewFiles) {
                // Hack to make file chooser behave slightly nicer. The default behaviour is to put
                // the directory name, without a trailing slash, in the filename field.
                // See ticket #127
                FileChooserUI ui = getUI();
                if (ui instanceof BasicFileChooserUI) {
                    BasicFileChooserUI mui = (BasicFileChooserUI) ui;
                    mui.setFileName("");
                }
            }
        }
    }
    
    /**
     * Approve the selection. We have this mainly so that derived classes
     * can call it...
     */
    protected void approved()
    {
        super.approveSelection();
    }
    
    class PackageDisplay extends JList
    {
        // number of lines at the top to display a header
        // explaining what the PackageDisplay is
        final int headerLines = 3;

        // index of the last class displayed (after this all list items are packages
        // and hence will have a different icon)
        int lastClass = 0;

        PackageDisplay(File displayDir)
        {
            this.setPreferredSize(new Dimension(150,200));
            this.setCellRenderer(new MyListRenderer());

            setDisplayDirectory(displayDir);
        }

        @Override
        protected void processMouseEvent(MouseEvent e) { }
        @Override
        protected void processMouseMotionEvent(MouseEvent e) { }

        void setDisplayDirectory(File displayDir)
        {
            if (displayDir == null)
                return;

            int maxDisplay = 3;
            File subDirs[] = displayDir.listFiles(new DirectoryFilter());
            File srcFiles[] = displayDir.listFiles(new JavaSourceFilter());
            List<String> listVec = new ArrayList<String>();

            // headerLines is 3
            listVec.add(previewLine1);
            listVec.add(previewLine2);
            listVec.add(" ");

            if(subDirs != null) {
                for(lastClass=0; lastClass<srcFiles.length && lastClass<maxDisplay; lastClass++) {
                    String javaFileName =
                       JavaNames.stripSuffix(srcFiles[lastClass].getName(), "." + SourceType.Java.toString().toLowerCase());

                    // check if the name would be a valid java name
                    if (!JavaNames.isIdentifier(javaFileName))
                        continue;

                    // files with a $ in them signify inner classes (which we want to ignore)
                    if (javaFileName.indexOf('$') == -1)
                        listVec.add(javaFileName);
                }
            }

            if(srcFiles != null) {
                for(int i=0; i<subDirs.length && i<maxDisplay; i++) {
                    // first check if the directory name would be a valid package name
                    if (!JavaNames.isIdentifier(subDirs[i].getName()))
                        continue;

                    listVec.add(subDirs[i].getName());

                    // now display sub sub dirs
                    File subSubDirs[] = subDirs[i].listFiles(new DirectoryFilter());

                    if (subSubDirs != null) {
                        for(int j=0; j<subSubDirs.length; j++) {
                            // first check if the directory name would be a valid package name
                            if (!JavaNames.isIdentifier(subSubDirs[j].getName()))
                                continue;

                            listVec.add(subDirs[i].getName() + "." + subSubDirs[j].getName());
                        }
                    }
                }
            }

            setListData(listVec.toArray());
        }

        class MyListRenderer extends DefaultListCellRenderer
        {
            @Override
            public Component getListCellRendererComponent(JList list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus)
            {
                Component s = super.getListCellRendererComponent(list, value, index,
                                                                    isSelected, cellHasFocus);

                if (index >= headerLines) {
                    if ((index-headerLines) < lastClass) {
                        ((JLabel)s).setIcon(classIcon);
                    }
                    else {
                        ((JLabel)s).setIcon(packageIcon);
                    }
                }
                return s;
            }
        }
    }
}
