package bluej.browser;

import bluej.pkgmgr.LibraryBrowserPkgMgrFrame;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;

import java.io.File;

/**
 * A JDialog subclass to allow the user to interactively add a new library
 * to the browser.  The new library can be specified as either a file (ZIP or JAR
 * archive) or directory, with an associated alias in either case.
 * 
 * @author $Author: mik $
 * @version $Id: AddLibraryDialog.java 36 1999-04-27 04:04:54Z mik $
 */
public class AddLibraryDialog extends JDialog {
    private static AddLibraryDialog dialog = null;
    private static String alias = null;

    private JTextField libraryTF = new JTextField(30);
    private JTextField aliasTF = new JTextField(30);
    private LibraryBrowserPkgMgrFrame parent = null;
	private static final Dimension TAGSIZE = new Dimension(60, 20);
	private static final FlowLayout PANELLAYOUT = new FlowLayout(FlowLayout.LEFT);
		
	/**
	 * Setup the UI for the dialog and event handlers for the dialog's buttons.
	 * 
	 * @param owner the LibraryBrowserPkgMgrFrame spawning this dialog
	 * @param title the title of the dialog
	 */
    public AddLibraryDialog(LibraryBrowserPkgMgrFrame owner, String title) {
	super(owner, title);
	setResizable(false);
	this.parent = owner;
	getContentPane().setLayout(new GridLayout(3, 1));
		
	JPanel libraryPanel = new JPanel(PANELLAYOUT);
	JLabel libTag = new JLabel("Library:");
	libTag.setPreferredSize(TAGSIZE);
	
	libraryPanel.add(libTag);
	libraryPanel.add(libraryTF);
	JButton browseButton = new JButton("...");
	browseButton.setMargin(new Insets(0, 0, 0, 0));
	libraryPanel.add(browseButton);
		
	JPanel aliasPanel = new JPanel(PANELLAYOUT);
	JLabel aliasTag = new JLabel("Alias:");
	aliasTag.setPreferredSize(TAGSIZE);
	aliasPanel.add(aliasTag);
	aliasPanel.add(aliasTF);
		
	JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	JButton okButton = new JButton("Ok");
	buttonPanel.add(okButton);
	JButton cancelButton = new JButton("Cancel");
	buttonPanel.add(cancelButton);

	getContentPane().add(libraryPanel);
	getContentPane().add(aliasPanel);
	getContentPane().add(buttonPanel);
	pack();

	/**
	 * If we have a valid library entered, ask the parent frame to add it.
	 */
	okButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		setVisible(false);
		if (libraryTF.getText() != null) {
			File library = new File(libraryTF.getText());
			if (aliasTF.getText() == null || aliasTF.getText().equals(""))
				alias = library.getName();
			else
				alias = aliasTF.getText();
			parent.addNewLibrary(library, alias);
		}
	    }
	});

	cancelButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		setVisible(false);
	    }
	});

	browseButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
				// when adding a new library, 
				// ask the user to select the file or directory
		JFileChooser chooser = new JFileChooser(); 
		chooser.setFileFilter(new LibraryFileFilter()); 
		// files for archive libraries, directories for library trees
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setDialogTitle("Add library: Step 1 of 2");
		int returnVal = chooser.showOpenDialog(getParent()); 
		if (returnVal == JFileChooser.APPROVE_OPTION) {
		    File library = chooser.getSelectedFile();
		    libraryTF.setText(library.getAbsolutePath());
		}
	    }
	});
    }
    
    /**
     * Set up the dialog.  The first argument can be null,
     * but it really should be a component in the dialog's
     * controlling frame.
     * 
     * @param comp the parent component for the dialog.
     * @param title the title of the dialog.
     **/
    public static void init(Component comp, String title) {
	Frame frame = JOptionPane.getFrameForComponent(comp);
	dialog = new AddLibraryDialog((LibraryBrowserPkgMgrFrame)frame, title);
    }

    /**
     * Show the initialized dialog.  The first argument should
     * be null if you want the dialog to come up in the center
     * of the screen.  Otherwise, the argument should be the
     * component on top of which the dialog should appear.
     * 
     * @param comp the parent component for the dialog.
     */
    public static void showDialog(Component comp) {
	if (dialog != null) {
	    dialog.setLocationRelativeTo(comp);
	    dialog.setVisible(true);
	} else {
	    System.err.println("Call initialize before calling showDialog.");
	}
    }

}
