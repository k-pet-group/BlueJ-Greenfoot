package bluej.utility;

import bluej.Config;
import bluej.pkgmgr.Package;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.*;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import bluej.utility.DialogManager;
import bluej.utility.filefilter.*;

/**
 * A file chooser for opening packages.
 *
 * Extends the behaviour of JFileChooser in the following ways: <BR><BR>
 * Only directories (either BlueJ packages or plain ones) are displayed. <BR>
 * BlueJ packages are displayed with a different icon. <BR>
 *
 * @author Michael Kolling
 * @author Axel Schmolitzky
 * @author Markus Ostman
 * @version $Id: PackageChooser.java 1418 2002-10-18 09:38:56Z mik $
 */
class PackageChooser extends JFileChooser
{
    static final Icon classIcon = Config.getImageAsIcon("image.classIcon");
    static final Icon packageIcon = Config.getImageAsIcon("image.packageIcon");

    static final String previewLine1 = Config.getString("utility.packageChooser.previewPane1");
    static final String previewLine2 = Config.getString("utility.packageChooser.previewPane2");

    PackageDisplay displayPanel;

    public PackageChooser(File startDirectory)
    {
        this(startDirectory, true);
    }

    /**
     * Create a new PackageChooser.
     * @param startDirectory the directory to start the package selection in.
     */
    public PackageChooser(File startDirectory, boolean preview)
    {
        super(startDirectory);

        setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
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

                    if (e.getPropertyName().equals(JFileChooser.DIRECTORY_CHANGED_PROPERTY)) {
                    }
                    if (e.getPropertyName().equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    }
                }
            });
        }
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

