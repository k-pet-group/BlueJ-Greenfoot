package bluej.pkgmgr;

import javax.swing.*;
import javax.swing.filechooser.*;
import javax.swing.tree.TreePath;

import java.awt.event.*;
import java.awt.*;
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
 * @author	Andy Marks
 *		Andrew Patterson
 * @version	$Id $
 **/
public class LibraryBrowserPkgMgrFrame extends PkgFrame implements ActionListener, ToggleMessageBoxOwner {

	// panel which holds display of the list of classes
	private JPanel classPanel = new JPanel(new BorderLayout());
	// panel which holds display of methods/fields of current class
	private AttributeChooser attributeChooser = null;
	// panel which holds tree listing class heirarchy
	private LibraryChooser libraryChooser = null;



    private CodeViewer codeViewer = null;
    
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
  

    private static final Dimension CHOOSERPANELSIZE = new Dimension(200, 250);
	private static final Dimension classPanelSize = new Dimension(700, 450);
    /* 
	Package cache: Hashtable mapping package dir -> Package object
    */
    private PackageCacheMgr packageCache = new PackageCacheMgr();

    private String currentPackageName = null;
    private String currentPackageDir = null;
    private Package currentPackage = null;
    private static LibraryBrowserPkgMgrFrame frame = null;
    public static boolean isStandalone = false;

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
	    
	// hack way of making class singleton
	if (frame != null) {
	    Utility.showMessage(this,
				"Sorry - you can only start one copy\nof this version of the Library Browser");
	    return;
	}

	setSize(new Dimension(780,580));
	
	// don't create these during declaration or they'll be 
	// instantiated before we even know if we want to/can
	// create another Library Browser
	libraryChooser = new LibraryChooser(this);
	attributeChooser = new AttributeChooser(this);
	codeViewer = new CodeViewer();
	
	frame = this;
	setTitle(Config.getString("browser.title"));
	
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
		setupMenus();

		libraryChooser.setPreferredSize(new Dimension(250, 200));
//		attributeChooser.setPreferredSize(CHOOSERPANELSIZE);

		JPanel fullPane = new JPanel();
		{
			JSplitPane splitPaneOne = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			{
				JPanel chooserPanel = new JPanel();
				{
					/* Choose the layout manager, add the components to the panel
					   and layout the panel for the class panel.  It's two main
					   components are the libraryChooser and classChooser. */

					JSplitPane splitPaneTwo = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
					{
						splitPaneTwo.setLeftComponent(libraryChooser);
						splitPaneTwo.setDividerSize(Config.splitPaneDividerWidth);
						splitPaneTwo.setRightComponent(attributeChooser);
						splitPaneTwo.setOneTouchExpandable(false);
			         
						// set the initial location and size of the divider
//						splitPaneTwo.setDividerLocation(250);
					}

					chooserPanel.setLayout(new BorderLayout());
					chooserPanel.add(splitPaneTwo, BorderLayout.CENTER);
				}

				// classPanel is a global variable
				{
					classPanel.setPreferredSize(new Dimension(700, 400));
				}

				splitPaneOne.setDividerSize(Config.splitPaneDividerWidth);
				splitPaneOne.setTopComponent(chooserPanel);
				splitPaneOne.setBottomComponent(classPanel);
				splitPaneOne.setOneTouchExpandable(false);
			        
				// set the initial location and size of the divider
//				splitPaneOne.setDividerLocation(200);
			}

			fullPane.setBorder(Config.generalBorderWithStatusBar);
			fullPane.setLayout(new BorderLayout());
			fullPane.add(splitPaneOne, BorderLayout.CENTER);
			fullPane.add(statusbar, BorderLayout.SOUTH);
		}

		getContentPane().add(fullPane);
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
	
		setJMenuBar(menuBar);
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
	} else if (source == findMI) {
	    new FindLibraryDialog(this).display();
	} else if (source == refreshMI) {
	    libraryChooser.loadLibraries();
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

		System.out.println("libraybrowserpkgmgrframe opening pkg " + packageName);

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
		JScrollPane scroller = new JScrollPane(editor);
		// remove previous components so new GraphEditor will show
		classPanel.removeAll();
		
		// add newly created components back to panel
		classPanel.add(scroller, BorderLayout.CENTER);

		// load the package details - must do after setting up UI or NPE will occur

		Dimension d = this.getSize();

		if (packageLoaded == false) {
		    pkg.load(packageName, null, true, true);
		    editor.setGraph(pkg);
		} else {
		    invalidate();
		    validate();
		}

		this.setSize(d);

		// store attributes of currently displayed package
		currentPackageName = packageName;
		currentPackage = pkg;
		currentPackageDir = pkg.getDirName();
	
		// make sure the scrollbar resizes to the current package dimensions
		scroller.invalidate();
		scroller.validate();
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

	System.out.println("opening in code viewer " + theClass.fullname);

	codeViewer.openClassWithNoSource(theClass, theClass.fullname, theClass.isCompiled());

/*	if (!new File(className).exists()) {
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
	} */
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
//    public String getDirectoryForPackage(String packageName) {
//	    return libraryChooser.getDirectoryForPackage(packageName);
//  }
}

