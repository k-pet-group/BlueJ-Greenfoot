package bluej.classmgr;

import javax.swing.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import bluej.*;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.prefmgr.*;

/**
 * A PrefPanel subclass to allow the user to interactively add a new library
 * to the browser.  The new library can be specified as a file (ZIP or JAR
 * archive) with an associated description.
 *
 * @author  Andrew Patterson
 * @version $Id: ClassMgrPrefPanel.java 2210 2003-10-11 14:50:39Z mik $
 */
public class ClassMgrPrefPanel extends JPanel
    implements PrefPanelListener
{
    private JTable userLibrariesTable = null;
    private ClassPathTableModel userLibrariesModel = null;

    /**
     * Registers the class manager preference panel with the preferences
     * dialog
     */
    public static void register()
    {
        ClassMgrPrefPanel p = new ClassMgrPrefPanel();
        PrefMgrDialog.add(p, Config.getString("classmgr.prefpaneltitle"), p);
    }

    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     *
     * @param title the title of the dialog
     */
    private ClassMgrPrefPanel()
    {
        List bootLibrariesList = new ArrayList(ClassMgr.getClassMgr().bootLibraries.getEntries());
        List systemLibrariesList = new ArrayList(ClassMgr.getClassMgr().systemLibraries.getEntries());

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
                userLibrariesModel = new ClassPathTableModel(ClassMgr.getClassMgr().userLibraries);
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

        JScrollPane systemLibrariesScrollPane = new JScrollPane();
        {
            JList list = new JList();
            {
                list.setListData(systemLibrariesList.toArray());
                list.setCellRenderer(new ClassMgrCellRenderer());
                list.setEnabled(false);
                list.setVisibleRowCount(3);
            }

            systemLibrariesScrollPane.setViewportView(list);
            systemLibrariesScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        }

        JLabel systemLibrariesTag = new JLabel(Config.getString("classmgr.systemlibraries"));
        {
            systemLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
            systemLibrariesTag.setLabelFor(systemLibrariesScrollPane);
        }

        // Construct a list of boot libraries

        JScrollPane bootLibrariesScrollPane = new JScrollPane();
        {
            JList list = new JList();
            {
                list.setListData(bootLibrariesList.toArray());
                list.setCellRenderer(new ClassMgrCellRenderer());
                list.setEnabled(false);
                list.setVisibleRowCount(3);
            }

            bootLibrariesScrollPane.setViewportView(list);
            bootLibrariesScrollPane.setAlignmentX(LEFT_ALIGNMENT);
        }

        JLabel bootLibrariesTag = new JLabel(Config.getString("classmgr.bootlibraries"));
        {
            bootLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
            bootLibrariesTag.setLabelFor(bootLibrariesScrollPane);
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.generalBorder);

        add(userLibrariesTag);
        add(userLibPane);
        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        add(systemLibrariesTag);
        add(systemLibrariesScrollPane);
        add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        add(bootLibrariesTag);
        add(bootLibrariesScrollPane);
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
        userLibrariesModel.commitEntries();

        ClassMgr.getClassMgr().saveUserLibraries();
    }

    /**
     * Pop up a dialog to allow the user to add a library
     * to their user library classpath.
     **/
    private void addUserLibrary()
    {
        // when adding a new library,
        // ask the user to select the file or directory
        JFileChooser chooser = new JFileChooser();
        {
            // LibraryFileFilter is a private class defined below
            chooser.setFileFilter(new LibraryFileFilter());
            // files for archive libraries, directories for library trees
            chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            chooser.setDialogTitle("Select directory or jar/zip file");
            int returnVal = chooser.showOpenDialog(getParent());
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                String librarylocation = chooser.getSelectedFile().getAbsolutePath();

                userLibrariesModel.addEntry(new ClassPathEntry(librarylocation,"", true));

                DialogManager.showMessage(null, "classmgr-changes-no-effect");
            }
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

        if(which != -1)
            userLibrariesModel.deleteEntry(which);
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
		return "Library files (*.jar;*.zip) or class directories";
	}
}

