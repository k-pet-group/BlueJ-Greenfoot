package bluej.pkgmgr;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.tree.TreePath;

import java.awt.event.*; // WindowAdapter, ActionListener, etc.
import java.awt.*; // Dimension, etc.
import javax.swing.border.TitledBorder;

import bluej.graph.GraphEditor;
import bluej.Config;
import bluej.browser.CodeViewer;
import bluej.browser.AttributeChooser;
import bluej.browser.LibraryChooser;
import bluej.browser.ChooseUseDestinationDIalog;
import bluej.browser.FindLibraryDialog;
import bluej.utility.ToggleMessageBox;
import bluej.utility.ToggleMessageBoxOwner;
import bluej.utility.Utility;
import bluej.utility.Debug;
import bluej.editor.Editor;

import java.util.*;
import java.io.File;
import java.net.URL;
import java.net.MalformedURLException;

/**
 * Implements a  browser for BlueJ class libraries.
 * Runs as either a standalone application, or spawned from
 * the BlueJ environment
 *
 * @author $Author: ajp $
 * @date $Date: 1999-06-03 03:15:30 +0100 (Thu, 03 Jun 1999) $
 * $Header$
 **/
public class LibraryBrowserPkgMgrFrame extends PkgFrame implements ActionListener, ToggleMessageBoxOwner {
	// top level UI containers
    private JPanel chooserPanel = new JPanel();
    private JPanel classPanel = new JPanel(new BorderLayout());
    private AttributeChooser attributeChooser = null;
    private CodeViewer codeViewer = null;
    private LibraryChooser libraryChooser = null;
    
    private JMenuItem closeMI = null;
    private JMenuItem refreshMI = null;
    private JMenuItem addLibMI = null;
    private JMenuItem findMI = null;
    private JMenuItem aboutMI = null;
    private JMenuItem topicsMI = null;
    private JMenuItem contentsMI = null;
    private JMenuItem indexMI = null;
    private JMenuItem propMI = null;
    private JMenuItem editPackageMI = null;
    private JMenuBar menuBar = null;
  
    private JScrollPane scroller = null;

    private static final Dimension CHOOSERPANELSIZE = new Dimension(200, 250);

    /* 
    Package caching and history attributes
    Package cache and history are maintained by 2 separate objects:
    Package cache: Hashtable mapping package dir -> Package object
    Package history: Hashtable mapping package name -> package dir
    */
    private JComboBox packageHistory = null;
    private PackageCacheMgr packageCache = new PackageCacheMgr();
    // list of items in history 
    // keys = package name, objects = path to package
    private Hashtable packageHistoryModel = new Hashtable();
    private JPanel classPanelControls = new JPanel(new BorderLayout());
    private static ImageIcon BACKBUTTONIMG = null;
    private static ImageIcon FORWARDBUTTONIMG = null;
    private JButton previousPackage = null;
    private JButton nextPackage = null;

    private String currentPackageName = null;
    private String currentPackageDir = null;
    private Package currentPackage = null;
    private static LibraryBrowserPkgMgrFrame frame = null;
    public static boolean isStandalone = false;
    public static String TEMPDIR = null;

	private static final char INNERCLASSINDICATOR = '$';
	private static final String JAVASOURCEEXTENSION = ".java";
	
	// index into showDialog array
	private static final int INNERCLASSDIALOG = 0;
	private static final int NOSOURCEFORCLASSDIALOG = 1;
	private boolean[] showDialog = {true, true};

    public static void main(String args[]) {
	// startup logic to determine home variable snatched
	// from BlueJ.Main
	String home = null;
	
	if ((args.length >= 2) && "-home".equals(args[0])) {
	    home = args[1];
	    String[] newArgs = new String[args.length - 2];
	    System.arraycopy(args, 2, newArgs, 0, args.length - 2);
	    args = newArgs;
	}	else {
	    String install = System.getProperty("install.root");
	    home = System.getProperty("bluej.home", install); 
	}
	
	if (home == null) {
	    System.err.println("The LibraryBrowser should be run from a script that sets the \"bluej.home\" property");
	    System.exit(-1);
	}
	
	Config.initialise(home);
	
	frame = new LibraryBrowserPkgMgrFrame(true);
	frame.setVisible(true);
	
	frame.isStandalone = true;
    }
    
    /**
     * Initialize and display a new Library Browser.  Currently singleton.
     */
    public LibraryBrowserPkgMgrFrame(boolean isStandalone) {
	    this.isStandalone = isStandalone;
	    
	TEMPDIR = Config.getString("browser.tempdir");
	
	// hack way of making class singleton
	if (frame != null) {
	    Utility.showMessage(this,
				"Sorry - you can only start one copy\nof this version of the Library Browser");
	    return;
	}
	
	BACKBUTTONIMG = new ImageIcon(Config.getImageFilename("browser.classchooser.backbutton.image"));
	previousPackage = new JButton(BACKBUTTONIMG);
	previousPackage.setToolTipText("go to previous package in history list");
	FORWARDBUTTONIMG = new ImageIcon(Config.getImageFilename("browser.classchooser.forwardbutton.image"));
	nextPackage = new JButton(FORWARDBUTTONIMG);
	nextPackage.setToolTipText("go to next package in history list");
	
	// don't create these during declaration or they'll be 
	// instantiated before we even know if we want to/can
	// create another Library Browser
	attributeChooser = new AttributeChooser(this);
	codeViewer = new CodeViewer();
	libraryChooser = new LibraryChooser(this);
	
	frame = this;
	setTitle(Config.getString("browser.title") + " " + Config.getString("browser.version"));
	
	setupUI();
	
	pack();
	setVisible(false);
	
	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent e) {
		close();
	    }
	});
    }
    
    /**
     * Depending on whether we're running standalone or not, hide
     * the browser window or kill the application
     */
    public void close() {
	if (libraryChooser != null)
		libraryChooser.close();
	
	if (isStandalone)
		System.exit(0);
	else {
		System.out.println("|" + frame + "|");
		if (frame != null) {
			setVisible(false);
			frame = null; // so that next browser start wont short circuit and quit
		}
		dispose();
	}
    }
    
    /**
     * Choose the layout manager, add the components to the frame
     * and layout the frame.
     **/
    private void setupUI() {
	setupClassPanel();
	setupChooserPanel();
	setupMenus();

	JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
	splitPane.setDividerSize(5);
	splitPane.setLeftComponent(chooserPanel);
	splitPane.setRightComponent(classPanel);
	splitPane.setOneTouchExpandable(false);
			         
	//Set the initial location and size of the divider
	splitPane.setDividerLocation(250);
	
	getContentPane().setLayout(new BorderLayout());
	getContentPane().add(statusbar, BorderLayout.SOUTH);
	getContentPane().add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Create menu bar, menus and menuitems.  Setup listeners for menu items.
     **/
    protected void setupMenus() {
	menuBar = new JMenuBar();
	JMenu libraryM = new JMenu(Config.getString("browser.menu.library"));

	refreshMI = new JMenuItem(Config.getString("browser.menu.library.refresh"));
	refreshMI.addActionListener(this);
	libraryM.add(refreshMI);
	refreshMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.getKeyText(KeyEvent.VK_F5)));
	
	libraryM.addSeparator();
	closeMI = new JMenuItem(Config.getString("browser.menu.library.close"));
	closeMI.addActionListener(this);
	libraryM.add(closeMI);
	closeMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK));
	
	libraryM.setEnabled(false);
	menuBar.add(libraryM);
	
	JMenu editM = new JMenu(Config.getString("browser.menu.edit"));
	findMI = new JMenuItem(Config.getString("browser.menu.edit.find"));
	findMI.addActionListener(this);
	editM.add(findMI);
	findMI.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, Event.CTRL_MASK));

	editM.setEnabled(false);
	menuBar.add(editM);
			
	JMenu packageM = new JMenu(Config.getString("browser.menu.package"));
	
	if (!isStandalone) {
		editPackageMI = new JMenuItem(Config.getString("browser.menu.package.edit"));
		editPackageMI.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (editor == null) 
					return;
				
				Utility.NYI(LibraryBrowserPkgMgrFrame.getFrame());
				// open a new PkgMgrFrame with this package in it
				//bluej.pkgmgr.Main.openPackage(currentPackageDir);
			}});

		packageM.add(editPackageMI);
	}
	
	packageM.setEnabled(false);
	menuBar.add(packageM);
	
	JMenu helpM = new JMenu(Config.getString("browser.menu.help"));
	aboutMI = new JMenuItem(Config.getString("browser.menu.help.about"));
	helpM.add(aboutMI);
	aboutMI.addActionListener(this);
	contentsMI = new JMenuItem(Config.getString("browser.menu.help.contents"));
	helpM.add(contentsMI);
	contentsMI.addActionListener(this);
	topicsMI = new JMenuItem(Config.getString("browser.menu.help.topics"));
	helpM.add(topicsMI);
	topicsMI.addActionListener(this);
	indexMI = new JMenuItem(Config.getString("browser.menu.help.index"));
	helpM.add(indexMI);
	indexMI.addActionListener(this);
		
	menuBar.add(Box.createHorizontalGlue());
	menuBar.add(helpM);

	setJMenuBar(menuBar);
    }
    
    /**
     * Choose the layout manager, add the components to the panel
     * and layout the panel for the class panel.  It's two main
     * components are the attributeChooser and codeViewer.
     **/
    private void setupClassPanel() {
	    JPanel buttonPanel = new JPanel(new BorderLayout());
	    buttonPanel.add(previousPackage, BorderLayout.WEST);
	    buttonPanel.add(nextPackage, BorderLayout.EAST);
	classPanelControls.add(buttonPanel, BorderLayout.WEST);
	classPanel.setPreferredSize(new Dimension(400, 500));
	previousPackage.addActionListener(this);
	previousPackage.setEnabled(false);
	nextPackage.addActionListener(this);
	nextPackage.setEnabled(false);
	JPanel packageHistoryPanel = new JPanel(new BorderLayout());
	packageHistory = new JComboBox();
	packageHistory.setEditable(false);
	packageHistoryPanel.add(new JLabel("History:", JLabel.RIGHT), BorderLayout.WEST);
	packageHistoryPanel.add(packageHistory, BorderLayout.CENTER);
	classPanelControls.add(packageHistoryPanel, BorderLayout.CENTER);
	packageHistory.addActionListener(this);
		
	classPanel.add(classPanelControls, BorderLayout.NORTH);
    }

    /**
     * Choose the layout manager, add the components to the panel
     * and layout the panel for the class panel.  It's two main
     * components are the libraryChooser and classChooser.
     **/
    private void setupChooserPanel() {
	JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	splitPane.setTopComponent(libraryChooser);
	splitPane.setDividerSize(5);
	libraryChooser.setPreferredSize(CHOOSERPANELSIZE);
	splitPane.setBottomComponent(attributeChooser);
	attributeChooser.setPreferredSize(CHOOSERPANELSIZE);
	splitPane.setOneTouchExpandable(false);
			         
	//Set the initial location and size of the divider
	splitPane.setDividerLocation(300);
	chooserPanel.setLayout(new BorderLayout());
	chooserPanel.add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Handle events originated from menus, 
     **/
    public void actionPerformed(ActionEvent ae) {
	Object source = ae.getSource();
	if (source == closeMI) {
	    // we're about to exit - make sure everything is saved
	    libraryChooser.saveConfig();
				
	    close();
	} else if (source == aboutMI) {
	    Utility.showMessage(this,
				Config.getString("browser.menu.help.about.text"));
	} else if (source == topicsMI) {
		showDocument(Config.getPropString("browser.url.help.topics"));
	} else if (source == contentsMI) {
		showDocument(Config.getPropString("browser.url.help.contents"));
	} else if (source == indexMI) {
		showDocument(Config.getPropString("browser.url.help.index"));
	} else if (source == findMI) {
	    new FindLibraryDialog(this).display();
	} else if (source == refreshMI) {
	    libraryChooser.loadLibraries();
	} else if (source == this.packageHistory) {
	    // direct access of history entry for GraphEditor window
	    String selectedPackage = packageHistory.getSelectedItem().toString();
	    
	    if (!selectedPackage.equals(currentPackageName)) {
		// we have a package name - let's get the path    
		String packageToOpen = packageHistoryModel.get(selectedPackage).toString();
		if (packageToOpen != null) {
			openPackage(packageToOpen);
		}
	    }
	} else if (source == nextPackage) {
	    // back button for GraphEditor window
	    // make sure there's somewhere to go back to
	    if (packageHistory.getItemCount() > 1) {
		int newSelectedIndex = packageHistory.getSelectedIndex() + 1;
		if (newSelectedIndex < packageHistory.getItemCount()) {
			String packageToOpen = packageHistoryModel.get(packageHistory.getItemAt(newSelectedIndex).toString()).toString();
			if (packageToOpen != null)
				openPackage(packageToOpen);
		}
		// disable the back button if we're on the first option
		if (newSelectedIndex == packageHistory.getItemCount() - 1)
		    nextPackage.setEnabled(false);
		else 
		    nextPackage.setEnabled(true);
	    }
	} else if (source == previousPackage) {
	    // back button for GraphEditor window
	    // make sure there's somewhere to go back to
	    if (packageHistory.getItemCount() > 1) {
		int newSelectedIndex = packageHistory.getSelectedIndex() - 1;
		if (newSelectedIndex >= 0) {
			String packageToOpen = packageHistoryModel.get(packageHistory.getItemAt(newSelectedIndex).toString()).toString();
			if (packageToOpen != null)
				openPackage(packageToOpen);
		}
		// disable the back button if we're on the first option
		if (newSelectedIndex == 0)
		    previousPackage.setEnabled(false);
		else 
		    previousPackage.setEnabled(true);
	    }
	}
    }

    private boolean showDocument(String document) {
	    if (Utility.openWebBrowser(document)) {
		return true;
	    } else {
		return false;
	    }
    }
    
    /**
     * A class or package has been marked for use in the BlueJ environment.
     * Give the user a list of all current packages (destinations) so they can
     * choose which one to import the package or class into.
     * 
     * @param thePackage the package to be used 
     */
    public void usePackage(Target thePackage) {
	usePackage(thePackage.getName(), thePackage instanceof ClassTarget);
    }

    /**
     * A package has been chosen to be used, now we need to identify where
     * to use the package.  If only one package is open, use that, otherwise
     * show a dialog listing all open packages and ask the user to choose. 
     * Either way, ask for confirmation before proceeding.
     * 
     * @param thePackage the name of the package to use in Java format (i.e., java.awt.Frame)
     * @param isClass true if the package is a single class, false otherwise
     */
    public void usePackage(String thePackage, boolean isClass) {
	if (this.isStandalone)
	    return;
    
	Package[] possibleTargets = bluej.pkgmgr.Main.getAllOpenPackages();
	if (possibleTargets == null || possibleTargets.length == 0) {
	    Utility.showError(LibraryBrowserPkgMgrFrame.getFrame(),
			      Config.getString("browser.usepackage.notargetdialog.text"));
																		
	    return;
	}

	if (possibleTargets.length > 1) {
	    ChooseUseDestinationDIalog chooser = new ChooseUseDestinationDIalog(this, thePackage, isClass);
	    chooser.setDestinations(possibleTargets);
	    chooser.display();
	} else if (possibleTargets.length == 1) {
	    // no point giving them a choice of one
	    usePackage(possibleTargets[0], thePackage, isClass);
	}
		
    }

    /**
     * Call the appropraite method in the package to use the selected package.
     * 
     * @param thePackage the Package in which to open the class or package
     */
    public void usePackage(Package thePackage, String lib, boolean isClass) {
	    if (isClass) {
		// separate class name from package name
		thePackage.insertLibClass(lib.substring(0, lib.lastIndexOf(".")), 
					  lib.substring(lib.lastIndexOf(".") + 1, lib.length()));
	    } else
		thePackage.insertLibPackage(lib);
    }

    /**
     * This is the main method to create a graphical display of a package
     * using a GraphEditor.  
     * 
     * Note: this method only adds the package to the cache if it is loaded
     * in this method (i.e., it is null upon entering this method).  If you
     * are calling this method with a valid package object, you must ensure
     * it is added to the cache yourself.
     * 
     * @param packageName the directory containing the package
     * @param pkg the package to open, or null if the package needs to be loaded
     */
	public void openPackage(String packageName, Package pkg) {
	boolean packageLoaded = true;
	if (pkg == null) {
		packageLoaded = false;
		pkg = new Package(packageName, this);
		packageCache.addPackageToCache(packageName, pkg);
	}
	
	// create a GraphEditor for this package
	// and enable the package menu if this is the first package to open
	if (editor == null)
		menuBar.getMenu(2).setEnabled(true);
	
	editor = new GraphEditor(pkg, this);
	editor.setReadOnly(true);
	// create a JScrollPane to hold the editor
	scroller = new JScrollPane(editor);
	scroller.setBorder(new TitledBorder("Current Package"));
	// remove previous components so new GraphEditor will show
	classPanel.removeAll();
		
	// add newly created components back to panel
	classPanel.add(scroller, BorderLayout.CENTER);
	classPanel.add(classPanelControls, BorderLayout.NORTH);

	// load the package details - must do after setting up UI or NPE will occur
	if (packageLoaded == false) {
	    pkg.load(packageName, null, true, true);
	    editor.setGraph(pkg);
	} else {
	    invalidate();
	    validate();
	}

	// store attributes of currently displayed package
	currentPackageName = packageName;
	currentPackage = pkg;
	currentPackageDir = pkg.getDirName();
	
	// make sure the scrollbar resizes to the current package dimensions
	scroller.invalidate();
	scroller.validate();
		
	// don't allow duplicates in history
	String historyItem = pkg.getName();
	if (this.packageHistoryModel.get(historyItem) == null) {
		this.packageHistoryModel.put(historyItem, packageName);
		// temporarily remove action listener so that open is not called 
		// again from actionPerformed()
		packageHistory.removeActionListener(this);
		packageHistory.addItem(historyItem);
		packageHistory.addActionListener(this);
		if (packageHistoryModel.size() > 1)
			previousPackage.setEnabled(true);
	}
		
	// temporarily remove action listener so that open is not called 
	// again from actionPerformed()
	packageHistory.removeActionListener(this);
	this.packageHistory.setSelectedItem(historyItem);
	packageHistory.addActionListener(this);
	    
	// we're jumping around in the history here, 
	// so we may need to toggle the enabled state of
	// the back button at times
	if (packageHistory.getSelectedIndex() > 0)
	        previousPackage.setEnabled(true);
	else
	        previousPackage.setEnabled(false);
	if (packageHistory.getSelectedIndex() < packageHistory.getItemCount() - 1)
	        nextPackage.setEnabled(true);
	else
	        nextPackage.setEnabled(false);
	
    }

	/**
     * @param packageName the absolute path to the directory containing the package
     */
    public void openPackage(String packageName) {
	// check cache for package before creating a new one
	Package cachePackage = (Package)packageCache.getPackageFromCache(packageName);
	openPackage(packageName, cachePackage);
	libraryChooser.selectPackageInTree(packageName);
    }
	
    /**
     * Ask the library chooser to open the specified package.  The library
     * chooser will take care of opening the appropriate graph editor.
     * Called by the FindLibraryDialog class when a found library is to be opened.
     * 
     * @param thePackage the path to the package to open in the library chooser
     */
    public void openPackage(TreePath thePackage) {
	libraryChooser.openPackage(thePackage);
    }

    /**
     * Update the class chooser and library chooser to reflect a new package.  
     * Make sure we're not trying to open a ZIP/JAR package here as it won't
     * exist if we try and access it using the normal approach.
     * Invoked when a package name is double clicked in the graph editor.
     * 
     * @param thePackage the PackageTarget object associated with the new package.
     */
    public void openPackage(PackageTarget thePackage) {
	// need to use getParent() because packge base dir will end with start of the package name
	String packageToOpen = new File(thePackage.getPackage().getBaseDir()).getParent() + File.separator + thePackage.packageName.replace('.', Config.slash);
	// make sure package is selected in tree before call to libraryChooser.openPackage#
	libraryChooser.selectPackageInTree(packageToOpen);
	libraryChooser.openSelectedPackageInClassChooser();
    }

    /**
     * Update both the code viewer and attribute chooser to reflect
     * a new class.  Invoked as the result of a class icon being
     * double clicked in the graph editor.
     * 
     * @param theClass the ClassTarget object associated with the new class
     */
    public void openClass(ClassTarget theClass) {
	this.openClassInAttributeChooser(theClass);
	openClassInCodeViewer(theClass);
    }
	
    /**
     * Attempt to open the class in the code viewer if it exists.  If the class is
     * an inner class (embedded $ in name), open the parent class instead.
     * 
     * @param className the class name specified as a filesystem file (with .java extension)
     * @param isCompiled true if the file to open has been compiled
     */
    private void openClassInCodeViewer(ClassTarget theClass) {
	String className = theClass.sourceFile();
	if (!new File(className).exists()) {
		codeViewer.openClassWithNoSource(theClass, theClass.fullname, theClass.isCompiled());
		if (showDialog[NOSOURCEFORCLASSDIALOG])
			new ToggleMessageBox(this, 
					     Utility.mergeStrings(Config.getString("browser.openclassineditor.nosourceforclassdialog.text"), theClass.sourceFile()),
					     Config.getString("browser.openclassineditor.nosourceforclassdialog.title"),
					     JOptionPane.INFORMATION_MESSAGE,
					     NOSOURCEFORCLASSDIALOG).display();
	} else {
		codeViewer.openClass(theClass, theClass.fullname, theClass.isCompiled());
		int innerClassIndicatorPos = className.indexOf(INNERCLASSINDICATOR);
		if (innerClassIndicatorPos != -1) {
			// it's an inner class - let's turn foo$bar.java into foo.java for MOE
			className = className.substring(0, innerClassIndicatorPos) + JAVASOURCEEXTENSION;
			if (showDialog[INNERCLASSDIALOG])
				new ToggleMessageBox(this, 
						     Config.getString("browser.openclassineditor.innerclassdialog.text"),
						     Config.getString("browser.openclassineditor.innerclassdialog.title"),
						     JOptionPane.INFORMATION_MESSAGE,
						     INNERCLASSDIALOG).display();
						     
		}
	}
    }
		
    /**
     * @param className the class name specified in package notation (i.e., a.b.c)
     */
    public void openClassInAttributeChooser(ClassTarget theClass) {
	attributeChooser.openClass(theClass.fullname);
    }
	
    public TreePath[] findAllLibrariesMatching(String pattern, boolean caseSensitive, boolean substringSearch) {
	return this.libraryChooser.findAllLibrariesMatching(pattern, 
							    caseSensitive, 
							    substringSearch);
		
    }

    /**
     * Return the JFrame for this object.  Useful when contructing dialogs
     * where a frame parent is needed.  Might need to rethink this if
     * multiple browsers are allowed to exist simultaneously.
     */
    public static JFrame getFrame() {
	return frame;
    }

    /**
     * Disable any controls we don't want people to access before
     * we've loaded the libraries (i.e., File and Edit menus)
     */
    public void disableControls() {
	if (menuBar != null) {
		if (menuBar.getMenu(0) != null)
			menuBar.getMenu(0).setEnabled(false);
		if (menuBar.getMenu(1) != null)
			menuBar.getMenu(1).setEnabled(false);
	}
    }
		
    /**
     * Enable controls needed for use after we've loaded the
     * libraries
     */
    public void enableControls() {
	if (menuBar != null) {
	    menuBar.getMenu(0).setEnabled(true);
	    menuBar.getMenu(1).setEnabled(true);
	}
    }

    /**
     * Ask the library chooser to add the library as specified.
     * Called by the AddNewLibrary dialog when a new library
     * has been located and named.
     * 
     * @param library the file representing the new library
     * @param alias the name for the new library
     */
    public void addNewLibrary(File library, String alias) {
	libraryChooser.addNewLibrary(library, alias);
    }

    /**
     * Invoked when a ToggleMessageBox has been closed.  Use this
     * method to flag dialogs for repeated viewing based on the 
     * <code>showAgain</code> parameter.
     * 
     * @param dialogID the id passed to the ToggleMessageBox constructor
     * @param showAgain true if the user wishes to see this dialog again
     */
    public void showDialogAgain(int dialogID, boolean showAgain) {
	    if (dialogID < showDialog.length)
		showDialog[dialogID] = showAgain;
	    else
		Debug.reportError("Invalid showAgain index in LibraryBrowserPkgMgrFrame");
    }
    
    public boolean isPackageInCache(String packageName) {
	    return packageCache.isPackageInCache(packageName);
    }
    
    public Package getPackageFromCache(String packageName) {
	    return packageCache.getPackageFromCache(packageName);
    }
    
    /**
     * Add the specified package object to the cache, using the package
     * name as the key to retrieve.
     * 
     * @param packageName the directory of the package 
     * @param pkg the package to add
     * @return true if packageName did not already exist in cache
     */
    public boolean addPackageToCache(String packageName, Package pkg) {
	    return packageCache.addPackageToCache(packageName, pkg);
    }

    protected void handleAction(AWTEvent evt) {
    }

    /**
     * Called by GraphEditor methods to determine the current package
     */
    public Package getPackage()
    {
	return packageCache.getPackageFromCache(currentPackageName);
    }
    
    /**
     * @param the package name in java notation (e.g., java.awt.event)
     * @return the directory containing the package, or null if none found
     */
    public String getDirectoryForPackage(String packageName) {
	    return libraryChooser.getDirectoryForPackage(packageName);
    }
}

