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

import java.util.Vector;
import java.util.Enumeration;
import java.io.File;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.graph.GraphEditor;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerThread;
import bluej.debugger.ObjectBench;
import bluej.debugger.ExecControls;
import bluej.debugger.ExecControlButtonModel;
import bluej.terminal.Terminal;
import bluej.terminal.TerminalButtonModel;
import bluej.classmgr.ClassMgrDialog;

public class PkgMgrFrame extends PkgFrame 

    implements BlueJEventListener
{

    public static String nameUsedError = Config.getString("error.newClass.duplicateName");

    private static String tutorialUrl = Config.getPropString("bluej.url.tutorial");
    private static String referenceUrl = Config.getPropString("bluej.url.reference");
    private static String libraryUrl = Config.getPropString("bluej.url.javaStdLib");
    private static String webBrowserMsg = Config.getString("pkgmgr.webBrowserMsg");
    private static String webBrowserError = Config.getString("pkgmgr.webBrowserError");
    private static final String creatingVM = Config.getString("pkgmgr.creatingVM");
    private static final String creatingVMDone = Config.getString("pkgmgr.creatingVMDone");

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
	
    JCheckBox showUsesCheckbox;
    JCheckBox showExtendsCheckbox;

    JButton imgUsesButton = null;
    JButton imgExtendButton = null;

    ObjectBench objbench;

    // ============================================================
    // static methods to create and remove frames

    private static Vector frames = new Vector();  // of PkgMgrFrames

    /**
     * Open a PkgMgrFrame for a new package. This is the only way
     * PkgMgrFrames can be created.
     */
    public static PkgMgrFrame createFrame(String packagePath) {
	PkgMgrFrame frame = new PkgMgrFrame(packagePath);
	frames.addElement(frame);
	return frame;
    }
	
    /**
     * Remove a frame from the set of currently open PkgMgrFrames.
     */
    public static void closeFrame(PkgMgrFrame frame) {

	// If only one frame, close should close existing package rather
	// than remove frame 

	if(frames.size() == 1) {	// close package, leave frame
	    frame.doSave();
	    frame.removePackage();
	}
	else {				// remove package and frame
	    frame.doClose();
	    frames.removeElement(frame);
	}
    }

    /**
     * frameCount - return the number of currently open top level frames
     */
    private static int frameCount()
    {
	return frames.size();
    }

    /**
     * About to exit - close all open frames.
     */
    public static void handleExit() {
	for(int i = frames.size() - 1; i >= 0; i--) {
	    PkgMgrFrame frame = (PkgMgrFrame)frames.elementAt(i);
	    frame.doClose();
	}
    }

    /**
     * displayMessage - display a short text message to the user. In the
     *  current implementation, this is done by showing the message in
     *  the status bars of all open package windows.
     */
    public static void displayMessage(String message)
    {
	for(Enumeration e = frames.elements(); e.hasMoreElements(); ) {
	    PkgMgrFrame frame = (PkgMgrFrame)e.nextElement();
	    frame.setStatus(message);
	}
    }

    // ================ (end of static part) ==========================


    /**
     * Create a new PkgMgrFrame which may or may not show a
     * package. If the parameter is null, the frame is opened
     * without referring to a package. If the parameter refers
     * to a valid package, it is opened and displayed.
     *
     * This constructor can only be called via createFrame().
     */
    private PkgMgrFrame(String packagePath) {
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

	makeFrame();

	if(pkgdir != noTitle) {
	    pkg.load(pkgdir);
	    Main.addPackage(pkg);
	}
	setWindowTitle();
	setStatus(AppTitle);

	BlueJEvent.addListener(this);
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
	    closeFrame(this);
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
	    if(frameCount() > 1)
		answer = Utility.askQuestion(this, "Quit all open packages?", 
					     "Quit All", "Cancel", null);
	    if(answer == 0) {
		bluej.Main.exit();
	    }
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

	case EDIT_PREFERENCES:
		ClassMgrDialog.showDialog(null);
		break;

	    // Tools commands
	case TOOLS_COMPILE:
	    pkg.compile();
	    break;

	case TOOLS_COMPILESELECTED:
	    Utility.NYI(this);
	    break;

	case TOOLS_REBUILD:
	    pkg.rebuild();
	    break;

	case TOOLS_BROWSE:
//	    Utility.NYI(this);
  	    getBrowser().setVisible(true);
//  	    // offset browser from this window
//  	    getBrowser().setLocation(this.getLocation().x + 100, 
// 				     this.getLocation().y + 100);
//  	    getBrowser().invalidate();
// 	    getBrowser().validate();
	    break;

	    // View commands
	case VIEW_SHOWUSES:
	    toggleShowUses(src);
	    break;
			
	case VIEW_SHOWINHERITS:
	    toggleShowExtends(src);
	    break;

	case VIEW_SHOWCONTROLS:
	    showHideExecControls(true, true);
	    break;

	case VIEW_CLEARTERMINAL:
	    clearTerminal();
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
	    AboutBlue about = new AboutBlue(this, bluej.Main.BLUEJ_VERSION);
	    about.setVisible(true);
	    break;

	case HELP_COPYRIGHT:    
		JOptionPane.showMessageDialog(this,
		new String[] { 
		    "BlueJ \u00a9 1999 Michael K\u00F6lling, John Rosenberg.",
		    " ",
		    "BlueJ is available free of charge and may be",
		    "redistributed freely. It may not be sold for",
		    "for profit or included in other packages which",
		    "are sold for profit without written authorisation."
		    },
		"BlueJ Copyright", JOptionPane.INFORMATION_MESSAGE);
	    break;

	default:
	    Debug.reportError("unknown command ID");
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
		PkgMgrFrame frame = createFrame(openPkg);
		frame.setVisible(true);
	    }
	}
    }

    /**
     * Close this package. This should be called ONLY through the
     * static functions in this class since it needs to be taken
     * out of the frame list when closed.
     */
    private void doClose()
    {
	doSave();
	closePackage();
	setVisible(false);
	BlueJEvent.removeListener(this);
    }

    /**
     * Save this package. Don't ask questions - just do it.
     */
    protected void doSave()
    {
	if(pkg.getDirName() == noTitle)
	    return;
	pkg.save();
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
//  		Debug.reportError("Package " + dirname + " already exists");
//  	    else {
//  		String pkgname = Utility.askString(this, 
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
	NewClassDialog dlg = new NewClassDialog(this);
	boolean okay = dlg.display();

	if(okay) {
	    String name = dlg.getClassName();
	    if (name.length() > 0) {

		// check whether name is already used
		if(pkg.getTarget(name) != null) {
		    Utility.showError(this, nameUsedError);
		    return;
		}

		ClassTarget target =  null;
		int classType = dlg.getClassType();
		target = new ClassTarget(pkg, name, classType == NewClassDialog.NC_APPLET);
		
		target.setAbstract(classType == NewClassDialog.NC_ABSTRACT);
		target.setInterface(classType == NewClassDialog.NC_INTERFACE);
		target.generateSkeleton();
		
		pkg.addTarget(target);
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
     * getExecControls - return the Execution Control window.
     */
    public ExecControls getExecControls()
    {
	return execCtrlWindow;
    }

    /**
     * Show or hide the exec control window. 
     */
    public void showHideExecControls(boolean show, boolean update)
    {
	if(execCtrlWindow == null) {
	    execCtrlWindow = new ExecControls();
	    Utility.centreWindow(execCtrlWindow, this);
//  	    Utility.showMessage(this, 
//  				"ATTENTION: The debugger is not yet\n" +
//  				"fully functional and it known to cause\n" +
//  				"problems. It should not be used.");
	}
	execCtrlWindow.setVisible(show);
	if(show && update)
	    execCtrlWindow.updateThreads();
    }

    /**
     * Clear the terminal window. 
     */
    private void clearTerminal()
    {
	Terminal.getTerminal().clear();
    }

    // ---- BlueJEventListener interface ----

    /**
     *  A BlueJEvent was raised. Check whether it is one that we're interested
     *  in.
     */
    public void blueJEvent(int eventId, Object arg)
    {
	DebuggerThread thread;

	switch(eventId) {
	    case BlueJEvent.CREATE_VM:
		setStatus(creatingVM);
		break;
	    case BlueJEvent.CREATE_VM_DONE:
		setStatus(creatingVMDone);
		break;
	    case BlueJEvent.EXECUTION_STARTED:
		executionStarted();
		break;
	    case BlueJEvent.EXECUTION_FINISHED:
		executionFinished();
		break;
	    case BlueJEvent.BREAKPOINT:
		thread = (DebuggerThread)arg;
		if(thread.getParam() == pkg)
		    hitBreakpoint(thread);
		break;
	    case BlueJEvent.HALT:
		thread = (DebuggerThread)arg;
		if(thread.getParam() == pkg)
		    hitHalt(thread);
		break;
	    case BlueJEvent.CONTINUE:
		thread = (DebuggerThread)arg;
		if(thread.getParam() == pkg)
		    executionContinued();
		break;
	    case BlueJEvent.SHOW_SOURCE:
		thread = (DebuggerThread)arg;
		if(thread.getParam() == pkg)
		    showSource(thread);
		break;
	}
    }

    // ---- end of BlueJEventListener interface ----

    /**
     * executionStarted - indicate in the interface that the machine has 
     *  started executing.
     */
    public void executionStarted()
    {
	progressButton.setEnabled(true);
	Terminal.getTerminal().activate(true);
    }

    /**
     * executionFinished - indicate in the interface that the machine has
     *  finished an execution.
     */
    public void executionFinished()
    {
	progressButton.setEnabled(false);
	if(execCtrlWindow != null && execCtrlWindow.isVisible())
	    execCtrlWindow.updateThreads();
	Terminal.getTerminal().activate(false);
    }

    /**
     * executionHalted - indicate in the interface that the machine
     *  temporarily stopped executing.
     */
    private void executionHalted()
    {
	progressButton.setIcon(stoppedIcon);
    }

    /**
     * executionContinued - indicate in the interface that the machine
     *  is executing again.
     */
    private void executionContinued()
    {
	progressButton.setIcon(workingIcon);
    }


    /**
     * hitBreakpoint - A breakpoint in this package was hit.
     */
    private void hitBreakpoint(DebuggerThread thread)
    {
	pkg.showSource(thread.getClassSourceName(0), 
		       thread.getLineNumber(0), 
		       thread.getName(), true);
	showHideExecControls(true, true);
	executionHalted();
    }


    /**
     * hitHalt - execution stopped interactively or after a step.
     */
    private void hitHalt(DebuggerThread thread)
    {
	executionHalted();
	updateDebugDisplay(thread, false);
    }


    /**
     * showSource - source display was requested
     */
    private void showSource(DebuggerThread thread)
    {
	updateDebugDisplay(thread, true);
    }


    /**
     * updateDebugDisplay - The debugger display needs updating.
     */
    private void updateDebugDisplay(DebuggerThread thread, boolean sourceOnly)
    {
	int frame = thread.getSelectedFrame();
	pkg.showSource(thread.getClassSourceName(frame), 
		       thread.getLineNumber(frame), 
		       thread.getName(), false);
	if(!sourceOnly)
	    execCtrlWindow.updateThreads();
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

    private void makeFrame() {

	setFont(PkgMgrFont);
	// setBackground(bgColor);
	setIconImage(iconImage);

	setupMenus();

	mainPanel = new JPanel();
	mainPanel.setLayout(new BorderLayout(5, 5));
	mainPanel.setBorder(Config.generalBorderWithStatusBar);

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

//	getContentPane().setLayout(new GridBagLayout());
//	GridBagConstraints constraints = new GridBagConstraints();
//	constraints.insets = new Insets(10, 10, 10, 10);
//	constraints.fill = GridBagConstraints.BOTH;
//	constraints.weightx = 1;
//	constraints.weighty = 1;
	getContentPane().add(mainPanel);

	setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);

	addWindowListener(new WindowAdapter() {
	    public void windowClosing(WindowEvent E)
		{
		    if(frameCount() == 1)
			bluej.Main.exit();
		    else
			PkgMgrFrame.closeFrame((PkgMgrFrame)E.getWindow());

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

	    //Debug.message("MenuType[" + menuType + "]" + menuId + "[" + menuStr + "]" );
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
		    item = new JCheckBoxMenuItem(itemStr,false);
		    item.setModel(new ExecControlButtonModel(this));
		    if (accelerator != null)
			item.setAccelerator(accelerator);
		    break;

		case VIEW_SHOWTERMINAL:
		    item = new JCheckBoxMenuItem(itemStr,false);
		    item.setModel(new TerminalButtonModel());
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
	    // Hack while "setHelpMenu" does not work...
	    if(CmdTypes[menuType] == HELP_COMMAND)
		menubar.add(Box.createHorizontalGlue());

	    // Add the menu to the MenuBar
	    menubar.add(menu);
	}

	if(menu != null) {
	    // Always put help menu last
	    //menubar.setHelpMenu(menu);  // not implemented in Swing 1.1
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
	    if(menus[i] != null) {
		menu = (JMenu)menus[i];

		if(menu.getText().equals(Config.getString("menu.package")) ||
		   menu.getText().equals(Config.getString("menu.tools"))) {
		    for (int j = 0; j < menu.getItemCount(); j++) {
			JMenuItem item = menu.getItem(j);
			if(item != null) {  // separators are returned as null
			    String label = item.getText();
			    if(!label.equals(Config.getString("menu.package.new"))
			       && !label.equals(Config.getString("menu.package.open"))
			       && !label.equals(Config.getString("menu.package.importPackage"))
			       && !label.equals(Config.getString("menu.package.quit"))
			       && !label.equals(Config.getString("menu.package"))
			       && !label.equals(Config.getString("menu.tools"))
			       && !label.equals(Config.getString("menu.tools.browse"))
			       && !label.equals(Config.getString("menu.tools.preferences")))
				item.setEnabled(enable);
			}
		    }
		}
		else if(!menu.getText().equals(Config.getString("menu.help")))
		    menu.setEnabled(enable);
	    }
	}
    }

    public LibraryBrowserPkgMgrFrame getBrowser() {
	if (browser == null)
	    browser = new LibraryBrowserPkgMgrFrame(false);
	
	return browser;
    }
}
