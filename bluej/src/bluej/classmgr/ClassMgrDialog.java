package bluej.classmgr;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.event.*;

import java.io.IOException;
import java.io.File;
import java.util.Vector;

import bluej.Config;

/**
 * A JDialog subclass to allow the user to interactively add a new library
 * to the browser.  The new library can be specified as either a file (ZIP or JAR
 * archive) or directory, with an associated alias in either case.
 * 
 * @author Andrew Patterson
 * @version $Id: ClassMgrDialog.java 132 1999-06-16 04:44:24Z ajp $
 */
public class ClassMgrDialog extends JDialog {

	private static ClassMgrDialog dialog = new ClassMgrDialog(Config.getString("classmgr.title"));

	private JTable userLibrariesTable = null;
	private ClassPathTableModel userLibrariesModel = null;

	/**
	 * Setup the UI for the dialog and event handlers for the dialog's buttons.
	 * 
	 * @param title the title of the dialog
	 */
	private ClassMgrDialog(String title) {

		Vector bootLibrariesList = new Vector(ClassMgr.getClassMgr().bootLibraries.getEntries());
		Vector systemLibrariesList = new Vector(ClassMgr.getClassMgr().systemLibraries.getEntries());

		setTitle(title);

		JPanel dialogPane = new JPanel();
		{
			// Construct a user editable table of user libraries and add/remove buttons

			JLabel userLibrariesTag = new JLabel("User libraries");
			{
				userLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
			}

			// hold the scrolling table and the column of add/remove buttons in a row
			JPanel lefttorightPane = new JPanel();
			{
				JScrollPane scrollPane = new JScrollPane();
				{
					// table of user library classpath entries
					userLibrariesModel = new ClassPathTableModel(ClassMgr.getClassMgr().userLibraries);
					userLibrariesTable = new JTable(userLibrariesModel);						
					{
						userLibrariesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					}
	
					scrollPane.setAlignmentY(TOP_ALIGNMENT);
					scrollPane.setViewportView(userLibrariesTable);
				}

				// hold the two Add and Delete buttons in a column
				JPanel toptobottomPane = new JPanel();
				{
					toptobottomPane.setLayout(new BoxLayout(toptobottomPane, BoxLayout.Y_AXIS));
					toptobottomPane.setAlignmentY(TOP_ALIGNMENT);

					JButton addButton = new JButton("Add");
					{
						addButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								addUserLibrary();
	    					}
						});
					}
					JButton deleteButton = new JButton("Delete");
					{
						deleteButton.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
								deleteUserLibrary();
							}
						});
					}

					toptobottomPane.add(addButton);
					toptobottomPane.add(deleteButton);

					// allow the Add and Delete buttons to be resized to equal width
					addButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
									addButton.getPreferredSize().height));
					deleteButton.setMaximumSize(new Dimension(Integer.MAX_VALUE,
									deleteButton.getPreferredSize().height));
				}

				lefttorightPane.setLayout(new BoxLayout(lefttorightPane, BoxLayout.X_AXIS));
				lefttorightPane.setAlignmentX(LEFT_ALIGNMENT);

				lefttorightPane.add(scrollPane);
				lefttorightPane.add(Box.createRigidArea(new Dimension(5,0)));
				lefttorightPane.add(toptobottomPane);
			}

			// Construct a list of system libraries

			JScrollPane systemLibrariesScrollPane = new JScrollPane();
			{
				JList list = new JList();
				{
					list.setListData(systemLibrariesList);
					list.setCellRenderer(new ClassMgrCellRenderer());
					list.setEnabled(false);
				}

				systemLibrariesScrollPane.setViewportView(list);
				systemLibrariesScrollPane.setAlignmentX(LEFT_ALIGNMENT);
			}

			JLabel systemLibrariesTag = new JLabel("System libraries");
			{
				systemLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
				systemLibrariesTag.setLabelFor(systemLibrariesScrollPane);
			}

			// Construct a list of boot libraries

			JScrollPane bootLibrariesScrollPane = new JScrollPane();
			{
				JList list = new JList();
				{
					list.setListData(bootLibrariesList);
					list.setCellRenderer(new ClassMgrCellRenderer());
					list.setEnabled(false);
				}

				bootLibrariesScrollPane.setViewportView(list);
				bootLibrariesScrollPane.setAlignmentX(LEFT_ALIGNMENT);
			}

			JLabel bootLibrariesTag = new JLabel("Boot libraries");
			{
				bootLibrariesTag.setAlignmentX(LEFT_ALIGNMENT);
				bootLibrariesTag.setLabelFor(bootLibrariesScrollPane);
			}

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			{
				buttonPanel.setAlignmentX(LEFT_ALIGNMENT);
	
				JButton okButton = new JButton(Config.getString("okay"));
				{
					okButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							userLibrariesModel.commitEntries();
							ClassMgr.getClassMgr().userLibraries.putConfigFile(System.out);
							setVisible(false);
						}
					});
				}

				getRootPane().setDefaultButton(okButton);

				JButton cancelButton = new JButton(Config.getString("cancel"));
				{
					cancelButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							userLibrariesModel.revertEntries();
							setVisible(false);
						}
					});
				}

				buttonPanel.add(okButton);
				buttonPanel.add(cancelButton);

				// try to make the OK and cancel buttons have equal width
				okButton.setPreferredSize(new Dimension(cancelButton.getPreferredSize().width,
								okButton.getPreferredSize().height));
			}

			dialogPane.setLayout(new BoxLayout(dialogPane, BoxLayout.Y_AXIS));
			dialogPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

			dialogPane.add(userLibrariesTag);
			dialogPane.add(lefttorightPane);
			dialogPane.add(Box.createRigidArea(new Dimension(0,5)));
			dialogPane.add(systemLibrariesTag);
			dialogPane.add(systemLibrariesScrollPane);
			dialogPane.add(Box.createRigidArea(new Dimension(0,5)));
			dialogPane.add(bootLibrariesTag);
			dialogPane.add(bootLibrariesScrollPane);
			dialogPane.add(Box.createRigidArea(new Dimension(0,5)));
			dialogPane.add(buttonPanel);

		}	// end dialogPane

//		JTabbedPane tabbedPane = new JTabbedPane();

		// arbitrary dimensions here.. what is the best way of getting these??
		dialogPane.setPreferredSize(new Dimension(610,410));
//		tabbedPane.addTab("Classes", null, dialogPane);

		getContentPane().add(dialogPane);
		pack();
    }

	/**
	 * Pop up a dialog to allow the user to add a library
	 * to their user library classpath.
	 **/
	private void addUserLibrary() {
		// when adding a new library, 
		// ask the user to select the file or directory
		JFileChooser chooser = new JFileChooser();
		{
			// LibraryFileFilter is a private class defined below
			chooser.setFileFilter(new LibraryFileFilter()); 
			// files for archive libraries, directories for library trees
			chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			chooser.setDialogTitle("Select directory or jar file");
			int returnVal = chooser.showOpenDialog(getParent()); 
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				String librarylocation = chooser.getSelectedFile().getAbsolutePath();

				userLibrariesModel.addEntry(new ClassPathEntry(librarylocation,""));
			}
		}
	}

	/**
	 * Delete the currently selected row (if there is one)
	 * of the user library table from the user library
	 * classpath.
	 */
	private void deleteUserLibrary() {
		int which = userLibrariesTable.getSelectedRow();

		if(which != -1)
			userLibrariesModel.deleteEntry(which);
	}

	/**
	 * Show the initialized dialog.  The first argument should
	 * be null if you want the dialog to come up in the center
	 * of the screen.  Otherwise, the argument should be the
	 * component on top of which the dialog should appear.
	 * 
	 * @param comp the parent component for the dialog.
	 */
	public static boolean showDialog(Component comp) {
		if (comp != null) {
			dialog.setLocationRelativeTo(comp);
		}
		dialog.setVisible(true);

		return true;
	}
}

/**
 * A private class to render class path entries into a list box
 * in the format of 
 * location (description)
 */
class ClassMgrCellRenderer implements ListCellRenderer {
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
	
		String s = cpe.getCanonicalPathNoException() + " (" + cpe.getDescription() + ")";;

		((JLabel)sup).setText(s);
        
   		return sup;
	}
}

/**
 * A simple FileFilter subclass to accept on valid library files (i.e., ZIP or JAR extension)
 * Used by the addUserLibrary method to only allow selection of valid library archive files.
 */
class LibraryFileFilter extends FileFilter {
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
		return "Library files (*.jar;*.zip)";
	}
}
