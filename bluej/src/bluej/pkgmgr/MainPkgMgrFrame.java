package bluej.pkgmgr;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.*;
import java.awt.PrintJob;
import java.awt.AWTEvent;

import java.io.File;

import bluej.Config;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.debugger.ExecControls;
import bluej.graph.GraphEditor;
import bluej.debugger.ObjectBench;
import bluej.pkgmgr.ClassTarget;


public class MainPkgMgrFrame extends PkgMgrFrame {

    public static String nameUsedError = Config.getString("error.newClass.duplicateName");

    public static String tutorialUrl = Config.getPropString("bluej.url.tutorial");
    public static String referenceUrl = Config.getPropString("bluej.url.reference");
    public static String libraryUrl = Config.getPropString("bluej.url.javaStdLib");
    public static String webBrowserMsg = Config.getString("pkgmgr.webBrowserMsg");
    public static String webBrowserError = Config.getString("pkgmgr.webBrowserError");

    private static final String importClassTitle = Config.getString("pkgmgr.importClass.title");
    private static final String importLabel = Config.getString("pkgmgr.importClass.buttonLabel");
    private static final String importpkgTitle = Config.getString("pkgmgr.importPkg.title");
    private static final String saveAsTitle =  Config.getString("pkgmgr.saveAs.title");
    private static final String saveLabel =  Config.getString("pkgmgr.saveAs.buttonLabel");

    private static final ImageIcon workingIcon = new ImageIcon(Config.getImageFilename("image.working"));
    private static final ImageIcon notWorkingIcon = new ImageIcon(Config.getImageFilename("image.working.disab"));
    private static final ImageIcon stoppedIcon = new ImageIcon(Config.getImageFilename("image.working.stopped"));
    private static final Image iconImage = new ImageIcon(Config.getImageFilename("image.icon")).getImage();

    private static LibraryBrowserPkgMgrFrame browser = null;
    private static ExecControls execCtrlWindow = null;

    // instance fields:

    JPanel buttonPanel;
    JPanel viewPanel;
    JPanel showPanel;

    JButton progressButton;

    JCheckBoxMenuItem showUsesMenuItem;
    JCheckBoxMenuItem showExtendsMenuItem;
    JCheckBoxMenuItem showControlsMenuItem;
    JCheckBoxMenuItem showTerminalMenuItem;
	
    JCheckBox showUsesCheckbox;
    JCheckBox showExtendsCheckbox;

    JButton imgUsesButton = null;
    JButton imgExtendButton = null;


    ObjectBench objbench;

    /**
     * Create a new MainPkgMgrFrame which may or may not show a
     * package. If the parameter is null, the frame is opened
     * without referring to a package. If the parameter refers
     * to a valid package, it is opened and displayed.
     */
    public MainPkgMgrFrame(String packagePath) {
	String pkgdir;

	if(packagePath == null)
	    pkgdir = noTitle;
	else {
	    if(checkPackage(packagePath))
		pkgdir = packagePath;
	    else
		pkgdir = noTitle;
	}
	if(pkgdir.endsWith(File.separator))
	    pkgdir = pkgdir.substring(0, pkgdir.length()-1);
		
	pkg = new Package(pkgdir, this);
	editor = new GraphEditor(pkg, this);

	createFrame();

	if(pkgdir != noTitle) {
	    pkg.load(pkgdir);
	    Main.addPackage(pkg);
	}
	setWindowTitle();

	setStatus(AppTitle);
    }
  
    /**
     * Create an empty MainPkgMgrFrame
     */
    public MainPkgMgrFrame() {
	this(null);
    }

    /**
     * Return the object bench.
     */
    public ObjectBench getObjectBench()
    {
	return objbench;
    }

    /**
     * Return the package shown by this frame.
     */
    public Package getPackage()
    {
	return pkg;
    }

    /**
     * Handle the invocation of a user action.
     */
    protected void handleAction(AWTEvent evt)
    {
	Object src = evt.getSource();
	Integer evtIdObj = (Integer)actions.get(src);
	int evtId = (evtIdObj != null) ? evtIdObj.intValue() : -1;
	String name;
	int tmp;

	// Debug.message("Event[" + evtId + "] Obj[" + src.toString() + "]" );

	// Always reset unless one of these buttons are pressed
	if (evtId != EDIT_NEWUSES && evtId != EDIT_NEWINHERITS) {
	    resetDependencyButtons();
	    pkg.setState(Package.S_IDLE);
	}
	clearStatus();

	switch(evtId) {
	    // Package commands
	case PKG_NEW:
	    doNewPackage();
	    break;
		
	case PKG_OPEN:
	    doOpen();
	    break;

	case PKG_CLOSE:
	    doClose();
	    break;

	case PKG_SAVE:
	    doSave();
	    setStatus(packageSaved);
	    break;

	case PKG_SAVEAS:
	    doSaveAs();
	    break;

	case PKG_IMPORTCLASS:
	    importClass();
	    break;

	case PKG_IMPORTPKG:
	    importPackage();
	    break;

	case PKG_EXPORTPKG:
	    exportPackage();
	    break;

	case PKG_PRINT:
	    print();
	    break;

	case PKG_QUIT:
	    int answer = 0;
	    if(bluej.Main.frameCount() > 1)
		answer = Utility.askQuestion(this, "Quit all open packages?", 
					     "Quit All", "Cancel", null);
	    if(answer == 0)
		bluej.Main.exit();
	    break;

		
	    // Edit commands
	case EDIT_NEWCLASS:
	    createNewClass();
	    break;
			
	case EDIT_REMOVECLASS:
	    removeClass();
	    break;
			
	case EDIT_NEWUSES:
	    pkg.setState(Package.S_CHOOSE_USES_FROM);
	    setStatus(chooseUsesFrom);
	    break;

	case EDIT_NEWINHERITS:
	    pkg.setState(Package.S_CHOOSE_EXT_FROM);
	    setStatus(chooseInhFrom);
	    break;

	case EDIT_REMOVEARROW:
	    pkg.setState(Package.S_DELARROW);
	    setStatus(chooseArrow);
	    break;

	    // Tools commands
	case TOOLS_COMPILE:
	    pkg.compile();
	    break;

	case TOOLS_COMPILESELECTED:
	    Utility.NYI(this);
	    break;

	case TOOLS_COMPILEALL:
	    pkg.compileAll();
	    break;

	case TOOLS_BROWSE:
	    Utility.NYI(this);
//  	    getBrowser().setVisible(true);
//  	    // offset browser from this window
//  	    getBrowser().setLocation(this.getLocation().x + 100, 
//  				     this.getLocation().y + 100);
//  	    getBrowser().invalidate();
//  	    getBrowser().validate();
	    break;

	    // View commands
	case VIEW_SHOWUSES:
	    toggleShowUses(src);
	    break;
			
	case VIEW_SHOWINHERITS:
	    toggleShowExtends(src);
	    break;

	case VIEW_SHOWCONTROLS:
	    toggleExecControls();
	    break;

	case VIEW_SHOWTERMINAL:
	    Utility.NYI(this);
	    break;

	    // Group work commands
	case GRP_CREATE:
	case GRP_OPEN:
	case GRP_UPDATESELECTED:
	case GRP_UPDATEALL:
	case GRP_COMMITSELECTED:
	case GRP_COMMITALL:
	case GRP_STATUSSELECTED:
	case GRP_STATUSALL:
	case GRP_USERS:
	case GRP_CONFIG:
	    Utility.NYI(this);
	    break;

	    // Help commands
	case HELP_TUTORIAL:
	    showWebPage(tutorialUrl);
	    break;

	case HELP_REFERENCE:
	    showWebPage(referenceUrl);
	    break;

	case HELP_STANDARDAPI:
	    showWebPage(libraryUrl);
	    break;

	case HELP_ABOUT:
	    AboutBlue about = new AboutBlue(this, Config.Version);
	    about.setVisible(true);
	    break;
		
	default:
	    Utility.reportError("unknown command ID");
	    break;
	}
    }

    // --- below are implementations of particular user actions ---

    /**
     * doOpen - open a BlueJ package
     */
    private void doOpen() {
	String openPkg = openPackageDialog(true);
	if (openPkg != null) {
	    if(pkg.getDirName() == noTitle || pkg.getDirName() == null) 
		doOpenPackage(openPkg);
	    else {
				// Otherwise open it in a new window
		PkgMgrFrame frame = new MainPkgMgrFrame(openPkg);
		frame.setVisible(true);
	    }
	}
    }

    /**
     * doSaveAs - implementation if the "Save As.." user function
     */
    private void doSaveAs()
    {
	// get a file name to save under
	String newname = getFileNameDialog(saveAsTitle, saveLabel);

	if (newname != null) {

	    // check whether name is already in use
	    File dir = new File(newname);
	    if(dir.exists()) {
		Utility.showError(this, 
				  "A file or directory with this\n" +
				  "name already exists.");
		return;
	    }

	    // save package under new name

	    Main.removePackage(pkg);	// remove under old name
	    int result = pkg.saveAs(newname);
	    Main.addPackage(pkg);	// add under new name

	    if(result == Package.CREATE_ERROR)
		Utility.showError(this, 
				  "Could not write package to new\n" +
				  "location. Please check the name\n" +
				  "and access rights.");
	    else if(result == Package.COPY_ERROR)
		Utility.showError(this, 
				  "There was a problem copying some\n" +
				  "of the package files. The package\n" +
				  "may be incomplete.");

	    setWindowTitle();
	}
    }

    /**
     * importClass - implementation if the "Import Class" user function
     */
    private void importClass()
    {
	String className = getFileNameDialog(importClassTitle, importLabel);

	if(className != null) {
	    int result = pkg.importFile(className);
	    switch (result) {
	        case Package.NO_ERROR: 
		    editor.repaint();
		    break;
	        case Package.FILE_NOT_FOUND: 
		    Utility.showError(this, 
			      "The specified file\ndoes not exist.");
		    break;
	        case Package.ILLEGAL_FORMAT: 
		    Utility.showError(this, 
			      "Cannot import file. The file must\n" +
			      "be a Java source file (it's name\n" +
			      "must end in \".java\").");
		    break;
	        case Package.CLASS_EXISTS: 
		    Utility.showError(this, 
			      "A class with this name already exists\n" +
			      "in this package. You cannot have two\n" +
			      "classes with the same name.");
		    break;
	        case Package.COPY_ERROR: 
		    Utility.showError(this, 
			      "An error occured during the attempt to\n" +
			      "import the file. Check access rights and\n" +
			      "disk space.");
		    break;
	    }
	}
    }

    /**
     * importPackage - implementation if the "Import Package" user function
     */
    private void importPackage()
    {
	Utility.NYI(this);
//  	    String dirname = openPackageDialog(false);
//  	    if(dirname == null)
//  		break;
			
//  	    File pkgfile = new File(dirname, Package.pkgfileName);
//  	    if(pkgfile.exists())
//  		Utility.reportError("Package " + dirname + " already exists");
//  	    else {
//  		String pkgname = Utility.askStringDialog(this, 
//  							 "Set name of imported package", 
//  							 "Package name:", "");
//  		if((pkgname == null) || (pkgname.length() == 0))
//  		    pkgname = Package.noPackage;

//  		if(Package.importDir(dirname, pkgname))
//  		    doOpenPackage(dirname);
//  	    }
    }

    /**
     * exportPackage - implementation if the "Export Package" user function
     */
    private void exportPackage()
    {
	Utility.NYI(this);
    }

    /**
     * print - implementation if the "print" user function
     */
    private void print()
    {
	PrintJob printjob = getToolkit().getPrintJob(this, 
				"BlueJ package: " + pkg.getDirName(),
				System.getProperties());
	if(printjob != null) {
	    printGraph(printjob);
	    printjob.end();
	}
    }

    /**
     * printGraph - part of the print function. Pring out the package graph
     */
    private void printGraph(PrintJob printjob) 
    {
	Dimension pageSize = printjob.getPageDimension();
	Rectangle printArea = pkg.getPrintArea(pageSize);
	Dimension graphSize = pkg.getMinimumSize();
	int cols = (graphSize.width + printArea.width - 1) / printArea.width;
	int rows = (graphSize.height + printArea.height - 1) / printArea.height;
		
	for(int i = 0; i < rows; i++)
	    for(int j = 0; j < cols; j++) {
		Graphics g = printjob.getGraphics();
		pkg.printTitle(g, pageSize, i * cols + j + 1);
		
		g.translate(printArea.x - j * printArea.width, 
			    printArea.y - i * printArea.height);
		g.setClip(j * printArea.width, i * printArea.height, 
			  printArea.width, printArea.height);
		editor.print(g);
		g.dispose();
	    }
    }

    public void resetDependencyButtons()
    {
	// pop them up
	if (imgUsesButton != null) {
	    // imgUsesButton.resetState();
	    // imgExtendButton.resetState();
	}
    }

    /**
     * createNewClass - implementation if the "New Class" user function
     */
    private void createNewClass()
    {
	NewClassDialog d = new NewClassDialog(this);

	if(d.doShow()) {
	    if (d.newClassName.length() > 0) {

		// check whether name is already used
		if(pkg.getTarget(d.newClassName) != null) {
		    Utility.showError(this, nameUsedError);
		    return;
		}
		ClassTarget newCT = new ClassTarget(pkg, d.newClassName);
		
		newCT.setAbstract(d.classType == d.NC_ABSTRACT);
		newCT.setInterface(d.classType == d.NC_INTERFACE);
		newCT.generateSkeleton();
		
		pkg.addTarget(newCT);
		editor.repaint();
	    }
	}
    }

    /**
     * removeClass - removes the currently selected ClassTarget
     * from the Package in this frame.
     */
    public void removeClass()
    {
	ClassTarget target = (ClassTarget) pkg.getSelectedTarget();
	if(target == null)
	    Utility.showError(this, "No Class selected for removal.\n" 
			      + "Select Class by clicking on it before\n"
			      + "trying to remove..");
	else
	    removeClass(target);

    }

    /**
    /**
     * removeClass - removes the specified ClassTarget from the Package. 
     */
    public void removeClass(ClassTarget removableTarget)
    {
	// Check they realise that this will delete the files.
	int response = Utility.askQuestion(this, "Removing this class will permanently\ndelete the source file.\nDo you want to continue?", "Ok", "Cancel", null);

	// if they agree
	if(response == 0)
	    pkg.removeClass(removableTarget);
    }

    public void toggleShowUses(Object src)
    {
	if(showUsesCheckbox.isSelected() != showUsesMenuItem.isSelected()) {
	    if(src == showUsesMenuItem)
		showUsesCheckbox.setSelected(showUsesMenuItem.isSelected());
	    else
		showUsesMenuItem.setSelected(showUsesCheckbox.isSelected());
		
	    pkg.toggleShowUses();
	    editor.repaint();
	}
    }
	
    public void toggleShowExtends(Object src)
    {
	if(showExtendsCheckbox.isSelected() != showExtendsMenuItem.isSelected()) {
	    if(src == showExtendsMenuItem)
		showExtendsCheckbox.setSelected(showExtendsMenuItem.isSelected());
	    else
		showExtendsMenuItem.setSelected(showExtendsCheckbox.isSelected());

	    boolean result = pkg.toggleShowExtends();
	    editor.repaint();
	}
    }

    /**
     * Toggle visibility of the exec control window. 
     */
    public void toggleExecControls()
    {
	boolean isVisible = (execCtrlWindow != null) && execCtrlWindow.isVisible();
	showHideExecControls(!isVisible, true);
    }

    /**
     * Show or hide the exec control window. 
     */
    public void showHideExecControls(boolean show, boolean update)
    {
	if(execCtrlWindow == null) {
	    execCtrlWindow = new ExecControls();
	    Utility.centreWindow(execCtrlWindow, this);
	    Utility.showMessage(this, 
				"ATTENTION: The debugger is not yet\n" +
				"fully functional and it known to cause\n" +
				"problems. It should not be used.");
	}
	execCtrlWindow.setVisible(show);
	if(show && update)
	    execCtrlWindow.updateThreads();
	showControlsMenuItem.setSelected(show);
    }

    /**
     * startExecution - indicate in the interface that the machine has 
     *  started executing.
     */
    public void startExecution()
    {
	progressButton.setEnabled(true);
    }

    /**
     * stopExecution - indicate in the interface that the machine has
     *  finished an execution.
     */
    public void stopExecution()
    {
	progressButton.setEnabled(false);
	if(execCtrlWindow != null && execCtrlWindow.isVisible())
	    execCtrlWindow.updateThreads();
    }

    /**
     * haltExecution - indicate in the interface that the machine
     *  temporarily stopped executing.
     */
    public void haltExecution()
    {
	progressButton.setIcon(stoppedIcon);
    }

    /**
     * continueExecution - indicate in the interface that the machine
     *  is executing again.
     */
    public void continueExecution()
    {
	progressButton.setIcon(workingIcon);
    }


    // --- general support functions for user function implementations ---

    /**
     * showWebPage - show a page in a web browser and display a message
     *  in the status bar.
     */
    private void showWebPage(String url)
    {
	if (Utility.openWebBrowser(url))
	    setStatus(webBrowserMsg);
	else
	    setStatus(webBrowserError);
    }

    /**
     * checkPackage - check whether 'path' points to a valid
     *  BlueJ package directory. If not, show an appropriate
     *  error message.
     */
    private boolean checkPackage(String path)
    {
	File packageFile = new File(path);

	// check whether path exists

	if (!packageFile.exists()) {
	    Utility.showError (this, 
	       "Error in opening package.\nThe package \"" +
	       path + "\"\ndoes not exist.");
	    return false;
	}

	// check whether path is a valid BlueJ package

	if (!Utility.isJBPackage(packageFile, Package.pkgfileName)) {
	    Utility.showError (this, 
	       "Error in opening package.\nThe file/directory \"" +
	       path + "\"\nis not a BlueJ package.");
	    return false;
	}

	return true;
    }

    // --- the following methods set up the GUI frame ---

    private void createFrame() {

	setFont(PkgMgrFont);
	// setBackground(bgColor);
	setIconImage(iconImage);

	setupMenus();

	mainPanel = new JPanel();
	mainPanel.setLayout(new BorderLayout(5, 5));

	if(pkg.getDirName() != noTitle) {
	    classScroller = new JScrollPane(editor);
	    mainPanel.add(classScroller, BorderLayout.CENTER);
	}

	// create toolbar

	JPanel toolPanel = new JPanel();

	buttonPanel = new JPanel();

	GridLayout buttonGridLayout = new GridLayout(0, 1, 0, 3);
	buttonGridLayout.setVgap(5);
	buttonPanel.setLayout(buttonGridLayout);
	buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));	
	String newClassString = Config.getString("menu.edit." + 
					 EditCmds[EDIT_NEWCLASS - EDIT_COMMAND]);
	JButton button = new JButton(newClassString);
	button.setFont(PkgMgrFont);
	button.setMargin(new Insets(2,2,2,2));
	button.addActionListener(this);
	button.setRequestFocusEnabled(false);   // never get keyboard focus
	buttonPanel.add(button);
	actions.put(button, new Integer(EDIT_NEWCLASS));

	ImageIcon usesIcon = new ImageIcon(Config.getImageFilename("image.build.depends"));
	button = imgUsesButton = new JButton(usesIcon);
	button.setMargin(new Insets(2,2,2,2));
	button.addActionListener(this);
	button.setRequestFocusEnabled(false);   // never get keyboard focus
	buttonPanel.add(button);
	actions.put(button, new Integer(EDIT_NEWUSES));

	ImageIcon extendsIcon = new ImageIcon(Config.getImageFilename("image.build.extends"));
	button = imgExtendButton = new JButton(extendsIcon);
	button.setMargin(new Insets(2,2,2,2));
	button.addActionListener(this);
	button.setRequestFocusEnabled(false);   // never get keyboard focus
	buttonPanel.add(button);
	actions.put(button, new Integer(EDIT_NEWINHERITS));

	String compileString = Config.getString("menu.tools." + ToolsCmds[TOOLS_COMPILE - TOOLS_COMMAND]);
	button = new JButton(compileString);
	button.setFont(PkgMgrFont);
	button.setMargin(new Insets(2,2,2,2));
	button.addActionListener(this);
	button.setRequestFocusEnabled(false);   // never get keyboard focus
	buttonPanel.add(button);
	actions.put(button, new Integer(TOOLS_COMPILE));

	viewPanel = new JPanel();
	viewPanel.setLayout(new BorderLayout());

	TitledBorder border = 
	    BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.darkGray),
					     Config.getString("pkgmgr.view.label"),
					     TitledBorder.CENTER,
					     TitledBorder.BELOW_TOP, 
					     PkgMgrFont);

	showPanel = new JPanel();
	showPanel.setLayout(new GridLayout(0, 1));
	showPanel.setBorder(border);

	// Add the Checkbox to ShowUses Arrows (this must also control
	// the Menu's Checkbox Items)

	showUsesCheckbox = new JCheckBox(Config.getString("pkgmgr.view.usesLabel"), 
					 null, true);
	showUsesCheckbox.addItemListener(this);
	showUsesCheckbox.setFont(PkgMgrFont);
	// showUsesCheckbox.setMargin(new Insets(0,0,0,0));
	actions.put(showUsesCheckbox, new Integer(VIEW_SHOWUSES));

	showPanel.add(showUsesCheckbox);
	// Add the Checkbox to ShowExtends Arrows (this must also control 
	// the Menu's Checkbox Items)

	showExtendsCheckbox = new JCheckBox(Config.getString("pkgmgr.view.inheritLabel"), 
					    null, true);
	showExtendsCheckbox.addItemListener(this);
	showExtendsCheckbox.setFont(PkgMgrFont);
	// showExtendsCheckbox.setMargin(new Insets(0,0,0,0));
	actions.put(showExtendsCheckbox, new Integer(VIEW_SHOWINHERITS));
	showPanel.add(showExtendsCheckbox);
	viewPanel.add("North",showPanel);
			
	// Image Button Panel to hold the Progress Image
	JPanel progressPanel = new JPanel ();
			
	progressButton = new JButton(workingIcon);
	progressButton.setDisabledIcon(notWorkingIcon);
	progressButton.setMargin(new Insets(0, 0, 0, 0));
	progressButton.addActionListener(this);
	actions.put(progressButton, new Integer(VIEW_SHOWCONTROLS));
	progressButton.setEnabled(false);
	progressPanel.add(progressButton);
	viewPanel.add("South",progressPanel);

	toolPanel.setLayout(new BorderLayout());
	toolPanel.add("North", buttonPanel);
	toolPanel.add("South", viewPanel);
			
	JPanel bottomPanel = new JPanel();
	bottomPanel.setLayout(new BorderLayout());
	objScroller = new JScrollPane(objbench = new ObjectBench());
	bottomPanel.add("North", objScroller);
	bottomPanel.add("South", statusbar);

	mainPanel.add("West", toolPanel);
	mainPanel.add("South", bottomPanel);

	getContentPane().setLayout(new GridBagLayout());
	GridBagConstraints constraints = new GridBagConstraints();
	constraints.insets = new Insets(5, 5, 0, 5);
	constraints.fill = GridBagConstraints.BOTH;
	constraints.weightx = 1;
	constraints.weighty = 1;
	getContentPane().add(mainPanel, constraints);

	setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E)
		{
		    if(bluej.Main.frameCount() == 1)
			bluej.Main.exit();
		}
	});

	// workaround for AWT cursor bug to show WAIT_CURSOR with Swing components
	Component glass = getGlassPane();
	glass.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	glass.addMouseListener(new MouseAdapter() {
	    public void mousePressed(MouseEvent e) {}
	});

	// grey out certain functions if package not open.
	if(pkg.getDirName() == noTitle)
	    enableFunctions(false);
    }

    /**
     * setupMenus - Create the menu bar
     */
    private void setupMenus() {
	menubar = new JMenuBar();
	JMenu menu = null;

	for(int menuType = 0; menuType < CmdTypes.length; menuType++) {
	    int sep = 0;

	    String menuId = "menu." + CmdTypeNames[menuType];
	    String menuStr = Config.getString(menuId);

	    // Debug.message("MenuType[" + menuType + "]" + menuId + "[" + menuStr + "]" );
	    menu = new JMenu(menuStr);

	    for(int i = 0; i < CmdStrings[menuType].length; i++) {
		int actionId = CmdTypes[menuType] + i;
		String itemId = menuId + "." + CmdStrings[menuType][i];
		String itemStr = Config.getString(itemId);
		KeyStroke accelerator = CmdKeys[menuType][i];
		JMenuItem item;
						
		switch (actionId) {
		case VIEW_SHOWUSES:		// Add these as CheckBoxMenuItems
		    item = showUsesMenuItem = new JCheckBoxMenuItem(itemStr,true);
		    item.addActionListener(this); 
		    if (accelerator != null)
			item.setAccelerator(accelerator);
		    break;

		case VIEW_SHOWINHERITS:
		    item = showExtendsMenuItem = new JCheckBoxMenuItem(itemStr,true);
		    item.addActionListener(this);
		    if (accelerator != null)
			item.setAccelerator(accelerator);
		    break;
							
		case VIEW_SHOWCONTROLS:
		    item = showControlsMenuItem = new JCheckBoxMenuItem(itemStr,false);
		    item.addActionListener(this); 
		    if (accelerator != null)
			item.setAccelerator(accelerator);
		    break;

		case VIEW_SHOWTERMINAL:
		    item = showTerminalMenuItem = new JCheckBoxMenuItem(itemStr,false);
		    item.addActionListener(this);
		    if (accelerator != null)
			item.setAccelerator(accelerator);
		    break;
							
		default:						// Otherwise
		    item = new JMenuItem(itemStr);
		    item.addActionListener(this);
		    if (accelerator != null)
			item.setAccelerator(accelerator);
		    break;
		}

		if(CmdTypes[menuType] == GRP_COMMAND)
		    item.setEnabled(false);

				// Add new Item to the Menu & associate Action to it.
		menu.add(item);
		actions.put(item, new Integer(actionId));

		if(sep < CmdSeparators[menuType].length
		   && CmdSeparators[menuType][sep] == actionId)
		    {
			menu.addSeparator();
			++sep;
		    }
	    }
	    // Add the menu to the MenuBar
	    menubar.add(menu);
	}

	if(menu != null) {
	    // Always put help menu last
	    //menubar.setHelpMenu(menu);
	}

	setJMenuBar(menubar);
    }

    /**
     ** Enable/disable functionality
     **/
    protected void enableFunctions(boolean enable)
    {
	// set Button enable status
	Component[] panelComponents = buttonPanel.getComponents();
	for(int i = 0; i < panelComponents.length; i++)
	    panelComponents[i].setEnabled(enable);

	panelComponents = viewPanel.getComponents();
	for(int i = 0; i < panelComponents.length; i++)
	    panelComponents[i].setEnabled(enable);

	panelComponents = showPanel.getComponents();
	for(int i = 0; i < panelComponents.length; i++)
	    panelComponents[i].setEnabled(enable);

	MenuElement[] menus = menubar.getSubElements();
	JMenu menu = null;

	for (int i = 0; i < menus.length; i++) {
	    menu = (JMenu)menus[i];

	    if(menu.getText().equals(Config.getString("menu.package")) ||
	       menu.getText().equals(Config.getString("menu.tools"))) {
		for (int j = 0; j < menu.getItemCount(); j++) {
		    if(!menu.getItem(j).getText().equals(Config.getString("menu.package.new"))
		       && !menu.getItem(j).getText().equals(Config.getString("menu.package.open"))
		       && !menu.getItem(j).getText().equals(Config.getString("menu.package.importPackage"))
		       && !menu.getItem(j).getText().equals(Config.getString("menu.package.quit"))
		       && !menu.getItem(j).getText().equals(Config.getString("menu.package"))
		       && !menu.getItem(j).getText().equals(Config.getString("menu.tools"))
		       && !menu.getItem(j).getText().equals(Config.getString("menu.tools.browse")))
			menu.getItem(j).setEnabled(enable);
		}
	    }
	    else if(!menu.getText().equals(Config.getString("menu.help")))
		menu.setEnabled(enable);
	}
    }

    public LibraryBrowserPkgMgrFrame getBrowser() {
	if (browser == null)
	    browser = new LibraryBrowserPkgMgrFrame(false);
	
	return browser;
    }
}
