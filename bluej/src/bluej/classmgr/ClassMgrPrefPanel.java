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
package bluej.classmgr;

import bluej.pkgmgr.Project;
import java.net.URL;
import java.util.Iterator;
import java.util.MissingResourceException;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import bluej.*;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.prefmgr.*;

/**
 * A PrefPanel subclass to allow the user to interactively add a new library
 * to the browser.  The new library can be specified as a file (ZIP or JAR
 * archive) with an associated description.
 *
 * @author  Andrew Patterson
 * @version $Id: ClassMgrPrefPanel.java 7051 2010-01-25 15:31:24Z nccb $
 */
public class ClassMgrPrefPanel extends JPanel
    implements PrefPanelListener
{
    private static final String userlibPrefix = "bluej.userlibrary";

    private JTable userLibrariesTable = null;
    private ClassPathTableModel userLibrariesModel = null;

    private ClassPath userLibraries;
    
    private boolean classPathModified = false;

    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     */
    public ClassMgrPrefPanel()
    {
        // Get the list of user libraries from the configuration.
        // This list os the one that is saved into the config file.
        userLibraries = new ClassPath();
        addConfigEntries(userLibraries, userlibPrefix);

        // TODO: There a re a few historical issues here, the first one is that this list is calculated
        // here but in reality now it is much more dynamic, there is no need to restart BlueJ to have
        // the new value applied, so this list should also be dynamic.
        // The second point is that it does not make much sense to say loaded or unloaded since
        // if it is a valid jar the it is in the project classloader.
        // Somthing to fix in the future.
        // It may have more meaning to show what is the project classloader, that would include all
        // libraries, and paths, including +libs 
        ArrayList<URL> userlibList = Project.getUserlibContent();
        ClassPath cp = new ClassPath(userlibList.toArray(new URL[userlibList.size()]));
        List<ClassPathEntry> userlibExtLibrariesList = cp.getEntries();

        // Construct a user editable table of user libraries and add/remove buttons

        JLabel userLibrariesTag = new JLabel(Config.getString("classmgr.userlibraries"));
        {
            userLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
        }

        // hold the scrolling table and the column of add/remove buttons in a row
        JPanel userLibPane = new JPanel(new BorderLayout());
        {
            JScrollPane scrollPane = new JScrollPane();
            {
				// table of user library classpath entries
                userLibrariesModel = new ClassPathTableModel(userLibraries);
                userLibrariesTable = new JTable(userLibrariesModel);
                {
                    userLibrariesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    userLibrariesTable.setPreferredScrollableViewportSize(new Dimension(400, 80));
                }

                TableColumn notfoundColumn = 
					userLibrariesTable.getColumn(userLibrariesTable.getColumnName(0));
                {
                    notfoundColumn.setPreferredWidth(20);
                }

                TableColumn locationColumn = 
					userLibrariesTable.getColumn(userLibrariesTable.getColumnName(1));
                {
                    locationColumn.setPreferredWidth(280);
                }

                scrollPane.setAlignmentY(TOP_ALIGNMENT);
                scrollPane.setViewportView(userLibrariesTable);
            }

            // hold the two Add and Delete buttons in a column
            JPanel buttonPane = new JPanel();
            {
                buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.Y_AXIS));
                buttonPane.setAlignmentY(TOP_ALIGNMENT);
                buttonPane.setBorder(BorderFactory.createEmptyBorder(0,5,0,0));

                JButton addButton = new JButton(Config.getString("classmgr.add"));
                {
                    addButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                addUserLibrary();
                            }
                        });
                }
                JButton deleteButton = new JButton(Config.getString("classmgr.delete"));
                {
                    deleteButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                deleteUserLibrary();
                            }
                        });
                }

		// allow the Add and Delete buttons to be resized to equal width
		addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
						addButton.getPreferredSize().height));
		deleteButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
						deleteButton.getPreferredSize().height));

                buttonPane.add(addButton);
                buttonPane.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
                buttonPane.add(deleteButton);
            }

            userLibPane.setAlignmentX(LEFT_ALIGNMENT);

            userLibPane.add(scrollPane, BorderLayout.CENTER);
            userLibPane.add(buttonPane, BorderLayout.EAST);
        }

        // Construct a list of system libraries

        JScrollPane userlibExtLibrariesScrollPane = new JScrollPane();
        {
            JList list = new JList();
            {
                list.setListData(userlibExtLibrariesList.toArray());
                list.setCellRenderer(new ClassMgrCellRenderer());
                list.setEnabled(false);
                list.setVisibleRowCount(6);
            }

            userlibExtLibrariesScrollPane.setViewportView(list);
            userlibExtLibrariesScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        }
        
        String userlibLocation = Config.getString("classmgr.userliblibraries") 
            + " (" + Config.getBlueJLibDir() + File.separator + "userlib)";
        JLabel userlibExtLibrariesTag = new JLabel(userlibLocation);
        {
            userlibExtLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
            userlibExtLibrariesTag.setLabelFor(userlibExtLibrariesScrollPane);
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.generalBorder);

        add(userLibrariesTag);
        add(userLibPane);
        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        add(userlibExtLibrariesTag);
        add(userlibExtLibrariesScrollPane);
    }

    /**
     * Returns an ArrayList of URLS holding jars that the user wish to be added to 
     * the Project classloader.
     * @return a non null but possibly empty arrayList of URL.
     */
    public ArrayList<URL> getUserConfigContent ()
    {
        return userLibraries.getURLs();
    }
    
    
    /**
     * Retrieve from the system wide Config entries corresponding to classpath
     * entries. The entries to retrieve start with prefix and have 1.location,
     * 2.location etc appended to them until an entry is not found.
     *
     * @param   prefix    the prefix of the property names to look up
     */
    private void addConfigEntries(ClassPath cp, String prefix)
    {
        int resourceID = 1;
        try {
            while (true) {
                String location = Config.getPropString(prefix + resourceID + ".location", null);

                if (location == null)
                    break;

                cp.addClassPath(location, "");

                resourceID++;
            }
        } catch (MissingResourceException mre) {
            // it is normal that this is exception is thrown, it just means we've come
            // to the end of the file
        }
    }

    public void beginEditing()
    {
    }

    public void revertEditing()
    {
        userLibrariesModel.revertEntries();
    }

    public void commitEditing()
    {
        if (classPathModified) {
            DialogManager.showMessage(null, "classmgr-changes-no-effect");
            classPathModified = false;
        }
        
        userLibrariesModel.commitEntries();
        saveUserLibraries();
    }


    /**
     * Save user classpath entries into the system wide Config properties object.
     * The entries stored start with prefix and have 1.location,
     * 2.location etc appended to them.
     */
    private void saveUserLibraries()
    {
        String r1;
        int resourceID = 1;

        while(true) {
            r1 = Config.removeProperty(userlibPrefix + resourceID + ".location");

            if(r1 == null)
                break;

            resourceID++;
        }

        Iterator<ClassPathEntry> it = userLibraries.getEntries().iterator();
        resourceID = 1;

        while (it.hasNext()) {
            ClassPathEntry nextEntry = it.next();
            Config.putPropString(userlibPrefix + resourceID + ".location",
                                    nextEntry.getPath());
            resourceID++;
        }
    }



    /**
     * Pop up a dialog to allow the user to add a library
     * to their user library classpath.
     **/
    private void addUserLibrary()
    {
    	File file = FileUtility.getFile(getParent(), Config.getString("prefmgr.misc.addLibTitle"),
    			null, new LibraryFileFilter(), false);
    	
    	if (file != null) {
    		String librarylocation = file.getAbsolutePath();

            userLibrariesModel.addEntry(new ClassPathEntry(librarylocation,"", true));
            
            classPathModified = true;
    	}
    }

    /**
     * Delete the currently selected row (if there is one)
     * of the user library table from the user library
     * classpath.
     */
    private void deleteUserLibrary()
    {
        int which = userLibrariesTable.getSelectedRow();

        if(which != -1) {
            classPathModified = true;
            userLibrariesModel.deleteEntry(which);
        }
    }
}

/**
 * A private class to render class path entries into a list box
 * in the format of
 * location (description)
 */
class ClassMgrCellRenderer implements ListCellRenderer
{
	// This is the only method defined by ListCellRenderer.  We just
	// reconfigure the Jlabel each time we're called.

	public Component getListCellRendererComponent(
		JList list,
		Object value,            // value to display
		int index,               // cell index
		boolean isSelected,      // is the cell selected
		boolean cellHasFocus)    // the list and the cell have the focus
	{
		Component sup =
			new DefaultListCellRenderer().getListCellRendererComponent(list,
							value,index,isSelected,cellHasFocus);

		ClassPathEntry cpe = (ClassPathEntry)value;

		String s = cpe.getCanonicalPathNoException() + " (" + cpe.getStatusString() + ")";

		((JLabel)sup).setText(s);

   		return sup;
	}
}

/**
 * A simple FileFilter subclass to accept on valid library files (i.e., ZIP or JAR extension)
 * Used by the addUserLibrary method to only allow selection of valid library archive files.
 */
class LibraryFileFilter extends FileFilter
{
	/**
	 * Check if it is a valid library archive file.
	 *
	 * @param	f the file to be check.
	 * @return	true if the file was accepted.
	 */
	public boolean accept(File f) {
		return (f.isDirectory() ||
			f.getName().toLowerCase().endsWith(".jar") ||
			f.getName().toLowerCase().endsWith(".zip"));
	}

	/**
	 * Return a description of the files accepted by this filter.  Used
	 * in the "file types" drop down list in file chooser dialogs.
	 *
	 * @return	a description of the files accepted by this filter.
	 */
	public String getDescription() {
		return Config.getString("prefmgr.misc.libFileFilter");
	}
}

