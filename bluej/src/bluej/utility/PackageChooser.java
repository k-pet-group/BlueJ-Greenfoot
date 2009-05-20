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
package bluej.utility;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.beans.*;
import java.io.File;
import java.util.*;
import java.util.List;

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.utility.filefilter.*;

/**
 * A file chooser for opening packages.
 *
 * Extends the behaviour of JFileChooser in the following ways: <BR><BR>
 * Only directories (either BlueJ packages or plain ones) are displayed. <BR>
 * BlueJ packages are displayed with a different icon. <BR>
 *
 * @author  Michael Kolling
 * @author  Axel Schmolitzky
 * @author  Markus Ostman
 * @version $Id: PackageChooser.java 6347 2009-05-20 15:22:43Z polle $
 */
class PackageChooser extends JFileChooser
{
    static final Icon classIcon = Config.getImageAsIcon("image.filechooser.classIcon");
    static final Icon packageIcon = Config.getImageAsIcon("image.filechooser.packageIcon");

    static final String previewLine1 = Config.getString("utility.packageChooser.previewPane1");
    static final String previewLine2 = Config.getString("utility.packageChooser.previewPane2");

    PackageDisplay displayPanel;

    /**
     * Create a new PackageChooser.
     * 
     * @param startDirectory 	the directory to start the package selection in.
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
    
    public boolean accept(File f)
    {
        if (f.isDirectory())
            return true;
        
        String fname = f.getName();
        return fname.endsWith(".jar") || fname.endsWith(".JAR") ||
                fname.endsWith(".zip") || fname.endsWith(".ZIP");
    }

    /**
     *  A directory was double-clicked. If this is a BlueJ package, consider
     *  this a package selection and accept it as the "Open" action, otherwise
     *  just traverse into the directory.
     */
    public void setCurrentDirectory(File dir)   // redefined
    {
        if (Package.isPackage(dir)) {
            setSelectedFile(dir);
            super.approveSelection();
        }
        else{
            super.setCurrentDirectory(dir);
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

        protected void processMouseEvent(MouseEvent e) { }
        protected void processMouseMotionEvent(MouseEvent e) { }

        void setDisplayDirectory(File displayDir)
        {
            if (displayDir == null)
                return;

            int maxDisplay = 3;
            File subDirs[] = displayDir.listFiles(new DirectoryFilter());
            File srcFiles[] = displayDir.listFiles(new JavaSourceFilter());
            List listVec = new ArrayList();

            // headerLines is 3
            listVec.add(previewLine1);
            listVec.add(previewLine2);
            listVec.add(" ");

            if(subDirs != null) {
                for(lastClass=0; lastClass<srcFiles.length && lastClass<maxDisplay; lastClass++) {
                    String javaFileName =
                       JavaNames.stripSuffix(srcFiles[lastClass].getName(), ".java");

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
            public Component getListCellRendererComponent(JList list, Object value, int index,
                                                    boolean isSelected, boolean cellHasFocus)
            {
                Component s = super.getListCellRendererComponent(list, value, index,
                                                                    isSelected, cellHasFocus);

                if (index < headerLines)
                    ;
                else if ((index-headerLines) < lastClass)
                    ((JLabel)s).setIcon(classIcon);
                else
                    ((JLabel)s).setIcon(packageIcon);

                return s;
            }
        }
    }
}
