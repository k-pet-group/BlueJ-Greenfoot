package bluej.pkgmgr;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.*;
import java.awt.*;
import java.awt.print.*;
import java.awt.AWTEvent;

import java.util.*;
import java.io.*;
import java.text.DateFormat;


import bluej.Config;
import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.DialogManager;
import bluej.graph.GraphEditor;
import bluej.debugger.*;
import bluej.views.*;
import bluej.terminal.Terminal;
import bluej.terminal.TerminalButtonModel;
import bluej.prefmgr.PrefMgrDialog;
import bluej.prefmgr.PrefMgr;

/**
 * The main user interface frame which allows editing of packages
 *
 * @version $Id: PkgMgrFrame.java 514 2000-05-25 07:57:41Z ajp $
 */
public class PkgMgrFrame extends JFrame
    implements BlueJEventListener, ActionListener, ItemListener, PackageEditorListener, Printable
{
    // static final Color bgColor = Config.getItemColour("colour.background");
    public Font PkgMgrFont = PrefMgr.getStandardFont();
    // Internal strings
    static final String chooseUsesFrom = Config.getString("pkgmgr.chooseUsesFrom");
    static final String chooseInhFrom = Config.getString("pkgmgr.chooseInhFrom");
    static final String chooseArrow = Config.getString("pkgmgr.chooseArrow");
    static final String packageSaved = Config.getString("pkgmgr.packageSaved");
    static final String errCloseAlert =  Config.getString("pkgmgr.error.close.title");
    static final String errCloseText =  Config.getString("pkgmgr.error.close.text");
    static final String newpkgTitle =  Config.getString("pkgmgr.newPkg.title");
    static final String createLabel =  Config.getString("pkgmgr.newPkg.buttonLabel");

    static int DEFAULT_WIDTH = 512;
    static int DEFAULT_HEIGHT = 450;

    private static String tutorialUrl = Config.getPropString("bluej.url.tutorial");
    private static String referenceUrl = Config.getPropString("bluej.url.reference");

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

    private static ExecControls execCtrlWindow = null;

    private PageFormat pageFormat;
    // private static final String notationStyle = Config.getDefaultPropString("bluej.notation.style", Graph.UML);

    // instance fields:

    JPanel buttonPanel;
    JPanel viewPanel;
    JPanel showPanel;

    static PackageChooser pkgChooser = null;	// chooser for packages only
    static JFileChooser fileChooser = null;	// chooser for all files/dirs
    JScrollPane classScroller = null;
    JScrollPane objScroller = null;

    /* The package that this frame is working on or null for the case where
       there is no package currently being edited (check with isEmptyFrame()) */
    private Package pkg = null;

    /* The graph editor which works on the package or null for the case where
        there is no package current being edited (isEmptyFrame() == true) */
    private PackageEditor editor = null;

    private PkgMgrFrame outer;

    private JPanel mainPanel;
    private JLabel statusbar = new JLabel();

    private JMenuBar menubar = null;
    private JButton progressButton;

    JCheckBoxMenuItem showUsesMenuItem;
    JCheckBoxMenuItem showExtendsMenuItem;

    JCheckBox showUsesCheckbox;
    JCheckBox showExtendsCheckbox;

    ObjectBench objbench;

    // ============================================================
    // static methods to create and remove frames

    private static Vector frames = new Vector();  // of PkgMgrFrames

    /**
     * Open a PkgMgrFrame with no package. This is the only way
     * PkgMgrFrames can be created. Packages can be installed into
     * frames using the methods openPackage/closePackage.
     */
    public static PkgMgrFrame createFrame()
    {
        PkgMgrFrame frame = new PkgMgrFrame();
        frames.addElement(frame);
        return frame;
    }

    /**
     * Remove a frame from the set of currently open PkgMgrFrames.
     */
    public static void closeFrame(PkgMgrFrame frame)
    {
        // If only one frame, close should close existing package rather
        // than remove frame

        if(frames.size() == 1) {	// close package, leave frame
            frame.doSave();
//            frame.removePackage();
            frame.updateWindowTitle();
        }
        else {                      // remove package and frame
            frame.doClose();
            frames.removeElement(frame);
        }

        BlueJEvent.removeListener(frame);
    }

    /**
     * return the number of currently open top level frames
     */
    private static int frameCount()
    {
        return frames.size();
    }

    /**
     * @return an array of all PkgMgrFrame objects, or null if none exist
     */
    public static PkgMgrFrame[] getAllFrames()
    {
        if (frames.size() == 0)
            return null;

        PkgMgrFrame[] openFrames = new PkgMgrFrame[frames.size()];
        frames.toArray(openFrames);

        return openFrames;
    }

    /**
     * @return  an array of open PkgMgrFrame objects which are currently
     *          editing a package from this project, or null if none exist
     */
    public static PkgMgrFrame[] getAllProjectFrames(Project proj)
    {
        int count=0, i=0;

        for(Enumeration e = frames.elements(); e.hasMoreElements(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)e.nextElement();

            if (!pmf.isEmptyFrame() && pmf.getProject() == proj)
                count++;
        }

        if (count == 0)
            return null;

        PkgMgrFrame[] projectFrames = new PkgMgrFrame[count];

        for(Enumeration e = frames.elements(); e.hasMoreElements(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)e.nextElement();

            if (!pmf.isEmptyFrame() && pmf.getProject() == proj)
                projectFrames[i++] = pmf;
        }

        return projectFrames;
    }

    /**
     * Close all open frames.
     */
    public static void handleExit()
    {
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

    public static void showError(Package sourcePkg, String msgId)
    {
        for(Enumeration e = frames.elements(); e.hasMoreElements(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)e.nextElement();

            if(!pmf.isEmptyFrame() && pmf.getPackage() == sourcePkg)
                DialogManager.showError(pmf, msgId);
        }
    }

    public static void showMessage(Package sourcePkg, String msgId)
    {
        for(Enumeration e = frames.elements(); e.hasMoreElements(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)e.nextElement();

            if(!pmf.isEmptyFrame() && pmf.getPackage() == sourcePkg)
                DialogManager.showMessage(pmf, msgId);
        }
    }

    public static void showMessageWithText(Package sourcePkg, String msgId, String text)
    {
        for(Enumeration e = frames.elements(); e.hasMoreElements(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)e.nextElement();

            if(!pmf.isEmptyFrame() && pmf.getPackage() == sourcePkg)
                DialogManager.showMessageWithText(pmf, msgId, text);
        }
    }


    // ================ (end of static part) ==========================

    /**
     * Create a new PkgMgrFrame which does not show a package.
     *
     * This constructor can only be called via createFrame().
     */
    private PkgMgrFrame()
    {
        this.pkg = null;
        this.editor = null;
        this.outer = this;

        makeFrame();

        updateWindowTitle();

        setStatus(bluej.Main.BLUEJ_VERSION_TITLE);

        BlueJEvent.addListener(this);
    }

    /**
     *  Displays the package in the frame ready for editing
     */
    public void openPackage(Package pkg)
    {
        if(pkg == null)
            throw new NullPointerException();

        // if we are already editing a package, close it and
        // open the new one
        if(this.pkg != null) {
            closePackage();
        }

        this.pkg = pkg;
        this.editor = new PackageEditor(pkg, this);

        classScroller = new JScrollPane(editor);
        mainPanel.add(classScroller, "Center");

        editor.addPackageEditorListener(this);

        invalidate();
        validate();

        enableFunctions(true);
        updateWindowTitle();

        //mik:Main.addPackage(pkg);
    }

    /**
     * Closes the current package.
     */
    public void closePackage()
    {
        if(isEmptyFrame())
            return;

        if(classScroller != null)
            mainPanel.remove(classScroller);

        editor.removePackageEditorListener(this);

        getProject().removeLocalClassLoader();
        getProject().removeRemoteClassLoader();
        pkg.closeAllEditors();

        // remove package from list of open packages
        //mik:Main.removePackage(pkg);

        editor = null;
        pkg = null;

//XXX this is causing lock ups??
//        enableFunctions(false);
    }

    /**
     * Return the package shown by this frame.
     */
    public Package getPackage()
    {
        return pkg;
    }

    /**
     * Return the project of the package shown by this frame.
     */
    public Project getProject()
    {
        return pkg.getProject();
    }

    /**
     * Return true if this frame is currently editing a
     * package. A call to this should bracket all uses of
     * getPackage() and editor.
     */
    public boolean isEmptyFrame()
    {
        return pkg == null;
    }

    /**
     *  Get (via a dialogue from the user) and return a new package name.
     *  If cancelled or an invalid name was specified, return null.
     */
    protected String getFileNameDialog(String title, String buttonLabel)
    {
        JFileChooser newChooser = getFileChooser(false);
        newChooser.setDialogTitle(title);

        int result = newChooser.showDialog(this, buttonLabel);

        if (result == JFileChooser.APPROVE_OPTION)
            return newChooser.getSelectedFile().getPath();
        else if (result == JFileChooser.CANCEL_OPTION)
            return null;
        else {
            DialogManager.showError(this, "error-no-name");
            return null;
        }
    }

    /**
     * return a file chooser for choosing any directory (default behaviour)
     */
    private JFileChooser getFileChooser(boolean directoryOnly)
    {
        if(fileChooser == null) {
            fileChooser = new JFileChooser(
                           Config.getPropString("bluej.defaultProjectPath",
                                                "."));
            fileChooser.setFileView(new PackageFileView());
        }
        if (directoryOnly)
            fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        else
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        return fileChooser;
    }

    /**
     * Return a BlueJ package chooser, i.e. a file chooser which
     * recognises BlueJ packages and treats them differently.
     */
    private PackageChooser getPackageChooser()
    {
        if(pkgChooser == null)
            pkgChooser = new PackageChooser(
                           Config.getPropString("bluej.defaultProjectPath",
                                                "."));
        return pkgChooser;
    }

    /**
     * Open a dialog that asks for a package to open. If the dialog is
     * cancelled by the user, null is returned. Otherwise the result is
     * the name of an existing directory (either plain or a BlueJ package).
     */
    private String openPackageDialog()
    {
        PackageChooser chooser = getPackageChooser();

        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            String dirname = chooser.getSelectedFile().getPath();
            if(dirname != null) {
                // remove any trailing slashes
                int len = dirname.length();
                while((len > 0) && (dirname.charAt(len - 1) == File.separatorChar))
                    --len;
                dirname = dirname.substring(0, len);
            }
            return dirname;
        }

        return null;
    }

    /**
     * Set the window title to show the current package name.
     */
    protected String updateWindowTitle()
    {
        if (isEmptyFrame()) {
            setTitle("BlueJ");
            return "BlueJ";
        }
        else {
            String title = "BlueJ:  " + getProject().
                                            getProjectName();

            if (!getPackage().isUnnamedPackage())
                title = title + "  [" + getPackage().getQualifiedName() + "]";

            setTitle(title);
            return title;
        }
    }

    /**
     * Display a message in the status bar of the frame
     */
    public void setStatus(String status)
    {
        if (statusbar != null)
            statusbar.setText(status);
    }

    public void clearStatus()
    {
        if (statusbar != null)
            statusbar.setText(" ");
    }

    /**
     * Set the frames cursor to a WAIT_CURSOR while system is busy
     */
    public void setWaitCursor(boolean wait)
    {
        if(wait)
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        else
            setCursor(Cursor.getDefaultCursor());
    }

    /**
     * Return the object bench.
     */
    public ObjectBench getObjectBench()
    {
        return objbench;
    }

    public void targetEvent(PackageEditorEvent e)
    {
        int evtId = e.getID();

        switch(evtId) {
         case PackageEditorEvent.TARGET_CALLABLE:   // user has initiated method call or constructor

            ResultWatcher watcher = null;
            CallableView cv = e.getCallable();


            if(cv instanceof ConstructorView)
            {
                // if we are constructing an object, create a watcher that waits for
                // completion of the call and then places the object on the object
                // bench
                watcher = new ResultWatcher() {
                    public void putResult(DebuggerObject result, String name) {
                        if((name == null) || (name.length() == 0))
                            name = "result";
                        if(result != null) {
                            ObjectWrapper wrapper =
                                new ObjectWrapper(outer, result.getInstanceFieldObject(0),
                                                   name);
                            getObjectBench().add(wrapper);
                        }
                        else
                            Debug.reportError("cannot get execution result");
                    }
                };
            }
            else if(cv instanceof MethodView)
            {
                // if we are calling a method that has a result, create a watcher
                // that waits for completion of the call and then displays the
                // result
                if(!((MethodView)cv).isVoid()) {
                    watcher = new ResultWatcher() {
                        public void putResult(DebuggerObject result, String name) {
                            ObjectViewer viewer =
                                ObjectViewer.getViewer(false, result, name, getPackage(), true,
                                                       outer);
                        }
                    };
                }
            }

            // create an Invoker to handle the actual invocation

            new Invoker(this, cv, null, watcher);
            break;

         case PackageEditorEvent.TARGET_REMOVE:     // user has initiated target "remove" option
            ClassTarget t = (ClassTarget) e.getSource();

            removeClass(t);
            break;

         case PackageEditorEvent.TARGET_OPEN:       // user has initiated a package open operation
			String newname = e.getName();

			Package p = getPackage().getProject().getPackage(newname);

			openPackage(p);
            break;
        }
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
            if(!isEmptyFrame())
                pkg.setState(Package.S_IDLE);
        }
        clearStatus();

        switch(evtId) {
        // Project commands
         case PROJ_NEW:                 // can be executed when isEmptyFrame() is true
            doNewProject();
            break;

         case PROJ_OPEN:                // can be executed when isEmptyFrame() is true
            doOpen();
            break;

         case PROJ_CLOSE:
            closeFrame(this);
            break;

         case PROJ_SAVE:
            doSave();
            setStatus(packageSaved);
            break;

         case PROJ_SAVEAS:
            doSaveAs();
            break;

         case PROJ_IMPORTCLASS:
            importClass();
            break;

         case PROJ_PRINT:
            print();
            break;

         case PROJ_QUIT:                // can be executed when isEmptyFrame() is true
            int answer = 0;
            if(frameCount() > 1)
                answer = DialogManager.askQuestion(this, "quit-all");
            if(answer == 0) {
                bluej.Main.exit();
            }
            break;

            // Edit commands
         case EDIT_NEWCLASS:
            createNewClass();
            break;

         case EDIT_NEWPACKAGE:
            createNewPackage();
            break;

         case EDIT_REMOVE:
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
            DialogManager.NYI(this);
            break;

        case TOOLS_REBUILD:
            pkg.rebuild();
            break;

        case TOOLS_BROWSE:
        /*
            DialogManager.showText(this,
        	"The library browser is not implemented in this version.\n" +
        	"To browse the Java standard libraries, select \"Java\n" +
        	"Class Libraries...\" from the Help menu.");
        */

            // offset browser from this window
        /*  	    getBrowser().setLocation(this.getLocation().x + 100,
        			     this.getLocation().y + 100);
            getBrowser().invalidate();
            getBrowser().validate();
            getBrowser().setVisible(true); */
            break;

        case TOOLS_PREFERENCES:         // can be executed when isEmptyFrame() is true
            PrefMgrDialog.showDialog(this);
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
            DialogManager.NYI(this);
            break;

            // Help commands
        case HELP_TUTORIAL:                 // can be executed when isEmptyFrame() is true
            showWebPage(tutorialUrl);
            break;

        case HELP_REFERENCE:                // can be executed when isEmptyFrame() is true
            showWebPage(referenceUrl);
            break;

        case HELP_STANDARDAPI:              // can be executed when isEmptyFrame() is true
            showWebPage(Config.getPropString("bluej.url.javaStdLib"));
            break;

        case HELP_ABOUT:                    // can be executed when isEmptyFrame() is true
            AboutBlue about = new AboutBlue(this, bluej.Main.BLUEJ_VERSION);
            about.setVisible(true);
            break;

        case HELP_COPYRIGHT:
        	JOptionPane.showMessageDialog(this,
        	new String[] {
        	    "BlueJ \u00a9 2000 Michael K\u00F6lling, John Rosenberg.",
        	    " ",
        	    "BlueJ is available free of charge and may be",
        	    "redistributed freely. It may not be sold for",
        	    "profit or included in other packages which",
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
     *
     */
    protected void doNewProject()
    {
        String newname = getFileNameDialog(newpkgTitle, createLabel);

        if (Project.createNewProject(newname))
        {
            if(isEmptyFrame()) {
                Project.openProject(newname, this);
            }
            else {
                Project.openProject(newname);
            }
        }
    }

    /**
     * This comment is no longer true
     * open a dialog that lets the user choose a project
     * (either a BlueJ project or a plain directory to import). If the
     * chosen directory is not a BlueJ package then "Package.importPackage"
     * is called to try to turn the chosen directory into a BlueJ project.
     * If this was successful or the project was a BlueJ project in the
     * first place, then the project is opened in a frame.
     */
    private void doOpen()
    {
        String pkgPath = openPackageDialog();

        if (pkgPath != null) {

            if(isEmptyFrame()) {
                Project.openProject(pkgPath, this);
            }
            else {
                Project.openProject(pkgPath);
            }

//            File packageDir = new File(pkgPath);

            // if path is not a valid BlueJ package - try to import it
//            if (!Package.isBlueJPackage(packageDir)) {
//                if (!Package.importPackage(packageDir,this)) {
//                    DialogManager.showMessage(this,"no-java-sources-found");
 //                   return;
//                }
//            }
//            else
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
        if(isEmptyFrame())
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
                DialogManager.showError(this, "directory-exists");
                return;
            }

	    // save package under new name

	    //mik:Main.removePackage(pkg);	// remove under old name
	    int result = pkg.saveAs(newname);
	    //mik:Main.addPackage(pkg);	// add under new name

	    if(result == Package.CREATE_ERROR)
		DialogManager.showError(this, "cannot-write-package");
	    else if(result == Package.COPY_ERROR)
		DialogManager.showError(this, "cannot-copy-package");

            updateWindowTitle();
        }
    }

    /**
     * importClass - implementation of the "Import Class" user function
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
                DialogManager.showError(this, "file-does-not-exist");
                break;
	        case Package.ILLEGAL_FORMAT:
                DialogManager.showError(this, "cannot-import");
                break;
	        case Package.CLASS_EXISTS:
                DialogManager.showError(this, "duplicate-name");
                break;
	        case Package.COPY_ERROR:
                DialogManager.showError(this, "error-in-import");
                break;
            }
        }
    }

    /**
     * exportPackage - implementation if the "Export Package" user function
     */
    private void exportPackage()
    {
        DialogManager.NYI(this);
    }


    /**
     * print - implementation of the "print" user function
     */
    private void print()
    {
        PrinterJob printerJob = PrinterJob.getPrinterJob();
        if(pageFormat == null)
            pageFormat = new PageFormat();

        //Paper paper = pageFormat.getPaper();

        // make it A4 roughly
	// this gives a one size fit all approach adopted at present due to
	// inconsistent page size handling in the 2D printing framework.
	// This should be unified with printing also done in the editor (moe)
        //paper.setSize(a4Width, a4Height);

        // manipulate borders for a reasonable size print area
        //double leftSideMargin = 36;
        //double rightSideMargin = 72;
        //double topMargin = 36;
        //double bottomMargin = 72;

        //paper.setImageableArea(leftSideMargin,
        //                       topMargin,
        //                       paper.getWidth() - (leftSideMargin + rightSideMargin),
        //                       paper.getHeight() - (topMargin + bottomMargin));

        //pageFormat.setPaper(paper);

        Dimension graphSize = pkg.getMinimumSize();
	if(graphSize.width > graphSize.height)
	    pageFormat.setOrientation(PageFormat.LANDSCAPE);
	else
	    pageFormat.setOrientation(PageFormat.PORTRAIT);

        pageFormat = printerJob.validatePage(pageFormat);
        printerJob.setPrintable(this, pageFormat);

        if (printerJob.printDialog()) {
            try {
                // call the Printable interface to do the actual printing
                printerJob.print();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private int pageColumns = 0;
    private int pageRows = 0;
    private int currentColumn = 0;
    private int currentRow = 0;

    final static int a4Width = 595;
    final static int a4Height = 840;

    /**
     * Method that implements Printable interface and does that actual printing of
     * class diagram.
     */
    public int print(Graphics g, PageFormat pageFormat, int pageIndex)
    {
        // temporary solution that only prints one page
        if(pageIndex >= 1)
            return Printable.NO_SUCH_PAGE;

        Dimension pageSize = new Dimension((int)pageFormat.getImageableWidth(),
                                           (int)pageFormat.getImageableHeight());
        Dimension graphSize = pkg.getMinimumSize();
        Rectangle printArea = getPrintArea(pageFormat);
        pageColumns = (graphSize.width + printArea.width - 1) / printArea.width;
        pageRows = (graphSize.height + printArea.height - 1) / printArea.height;

	// loop does not do much at present, only first page printed
        for(int i = 0; i < pageRows; i++) {
            for(int j = 0; j < 1; j++) {
                printTitle(g, pageFormat, i * pageColumns + j + 1);
                g.translate(printArea.x - j * printArea.width,
                            printArea.y - i * printArea.height);
                g.setClip(j * printArea.width, i * printArea.height,
                          printArea.width, printArea.height);
                editor.paint(g);
            }
        }
        return Printable.PAGE_EXISTS;
    }

    public void resetDependencyButtons()
    {
        // pop them up
//        if (imgUsesButton != null) {
            // imgUsesButton.resetState();
            // imgExtendButton.resetState();
//            }
    }

    // Add a title to printouts
    static final int PRINT_HMARGIN = 6;
    static final int PRINT_VMARGIN = 24;
    static final Font printTitleFont = new Font("SansSerif", Font.PLAIN,
                                                12); //Config.printTitleFontsize);
    static final Font printInfoFont = new Font("SansSerif", Font.ITALIC,
                                               10); //Config.printInfoFontsize);

    /**
     * Return the rectangle on the page in which to draw the class diagram.
     * The rectangle is the page minus margins minus space for header and
     * footer text.
     */
    public Rectangle getPrintArea(PageFormat pageFormat)
    {
        FontMetrics tfm = getFontMetrics(printTitleFont);
        FontMetrics ifm = getFontMetrics(printInfoFont);
        return new Rectangle((int)pageFormat.getImageableX() + PRINT_HMARGIN,
                             (int)pageFormat.getImageableY() + 2 * PRINT_VMARGIN,
                             (int)pageFormat.getImageableWidth() - (2 * PRINT_HMARGIN),
                             (int)pageFormat.getImageableHeight() - (2 * PRINT_VMARGIN));
    }

    /**
     * Print the page title and other page decorations (frame, footer).
     */
    public void printTitle(Graphics g, PageFormat pageFormat, int pageNum)
    {
        FontMetrics tfm = getFontMetrics(printTitleFont);
        FontMetrics ifm = getFontMetrics(printInfoFont);
        Rectangle printArea = new Rectangle((int)pageFormat.getImageableX(),
                                            (int)pageFormat.getImageableY(),
                                            (int)pageFormat.getImageableWidth(),
                                            (int)pageFormat.getImageableHeight());

        // frame header area
//XXX        g.setColor(lightGrey);
        g.fillRect(printArea.x, printArea.y, printArea.width, PRINT_VMARGIN);

//XXX        g.setColor(titleCol);
        g.drawRect(printArea.x, printArea.y, printArea.width, PRINT_VMARGIN);

        // frame print area
        g.drawRect(printArea.x, printArea.y, printArea.width,
                   printArea.height - PRINT_VMARGIN);

        // write header
/*XXX        String title = (packageName == noPackage) ? dirname : packageName;
        g.setFont(printTitleFont);
        Utility.drawCentredText(g, "BlueJ package - " + title,
                                printArea.x, printArea.y,
                                printArea.width, tfm.getHeight());
        // write footer
        g.setFont(printInfoFont);
        DateFormat dateFormat = DateFormat.getDateTimeInstance();
        Utility.drawRightText(g, dateFormat.format(new Date()) + ", page " + pageNum,
                              printArea.x, printArea.y + printArea.height - PRINT_VMARGIN,
                              printArea.width, ifm.getHeight()); */
    }

    /**
     * Implementation of the "New Class" user function.
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
                DialogManager.showError(this, "duplicate-name");
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
     * Implementation of the "New Package" user function
     */
    private void createNewPackage()
    {
        NewPackageDialog dlg = new NewPackageDialog(this);
        boolean okay = dlg.display();

        if(okay) {
            String name = dlg.getPackageName();
            if (name.length() > 0) {
                // check whether name is already used
                if(pkg.getTarget(name) != null) {
                    DialogManager.showError(this, "duplicate-name");
                    return;
                }

                File newpkgDir = new File(getPackage().getPath(), name);

                if(newpkgDir.mkdir())
                {
                    File newpkgFile = new File(newpkgDir, Package.pkgfileName);

                    try {
                        if(newpkgFile.createNewFile())
                        {
                            PackageTarget target = new PackageTarget(pkg, name);

                            pkg.addTarget(target);
                            editor.repaint();
                        }
                    }
                    catch(IOException ioe)
                    {

                    }
                }
            }
        }
    }

    /**
     * Removes the currently selected ClassTarget
     * from the Package in this frame.
     */
    public void removeClass()
    {
	Target target = pkg.getSelectedTarget();

	if(target == null)
	    DialogManager.showError(this, "no-class-selected");
	else {
		if(target instanceof ClassTarget)
		{
			ClassTarget t = (ClassTarget) target;
	    		removeClass(t);
		}
	}
    }

    /**
     * Removes the specified ClassTarget from the Package.
     */
    public void removeClass(ClassTarget removableTarget)
    {
        // Check they realise that this will delete the files.
        int response = DialogManager.askQuestion(this, "really-remove");

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
            DialogManager.centreWindow(execCtrlWindow, this);
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
                showSourcePosition(thread, false);
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
        pkg.removeStepMarks();
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
        pkg.removeStepMarks();
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
        showSourcePosition(thread, true);
    }

    /**
     * showSourcePosition - The debugger display needs updating.
     */
    private void showSourcePosition(DebuggerThread thread,
                                    boolean updateDebugger)
    {
        int frame = thread.getSelectedFrame();
        if(pkg.showSource(thread.getClassSourceName(frame),
                          thread.getLineNumber(frame),
                          thread.getName(), false))
            execCtrlWindow.setVisible(true);

        if(updateDebugger)
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

    public void itemStateChanged(ItemEvent evt)
    {
        handleAction(evt);
    }
    public void actionPerformed(ActionEvent evt)
    {
        handleAction(evt);
    }

    // --- the following methods set up the GUI frame ---

    private void makeButtonNotGrow(JButton button)
    {
        Dimension pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
        button.setMargin(new Insets(2,0,2,0));
    }

    private void makeFrame()
    {
        setFont(PkgMgrFont);
        setIconImage(iconImage);

        setupMenus();

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout(5, 5));
        mainPanel.setBorder(Config.generalBorderWithStatusBar);

        // create toolbar

        JPanel toolPanel = new JPanel();

        buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
            buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
            String newClassString = Config.getString("menu.edit." +
                                        EditCmds[EDIT_NEWCLASS - EDIT_COMMAND]);

            JButton button = new JButton(newClassString);
            button.setFont(PkgMgrFont);
            button.setToolTipText(Config.getString("tooltip.newClass"));
            button.addActionListener(this);
            button.setRequestFocusEnabled(false);   // never get keyboard focus
            makeButtonNotGrow(button);
            buttonPanel.add(button);
            buttonPanel.add(Box.createVerticalStrut(3));
            actions.put(button, new Integer(EDIT_NEWCLASS));

            ImageIcon usesIcon = new ImageIcon(Config.getImageFilename("image.build.depends"));
            button = new JButton(usesIcon);
            button.setToolTipText(Config.getString("tooltip.newUses"));
            button.addActionListener(this);
            button.setRequestFocusEnabled(false);   // never get keyboard focus
            makeButtonNotGrow(button);
            buttonPanel.add(button);
            buttonPanel.add(Box.createVerticalStrut(3));
            actions.put(button, new Integer(EDIT_NEWUSES));

            ImageIcon extendsIcon = new ImageIcon(Config.getImageFilename("image.build.extends"));
            button = new JButton(extendsIcon);
            button.setToolTipText(Config.getString("tooltip.newExtends"));
            button.addActionListener(this);
            button.setRequestFocusEnabled(false);   // never get keyboard focus
            makeButtonNotGrow(button);
            buttonPanel.add(button);
            buttonPanel.add(Box.createVerticalStrut(3));
            actions.put(button, new Integer(EDIT_NEWINHERITS));

            String compileString = Config.getString("menu.tools." +
                                        ToolsCmds[TOOLS_COMPILE - TOOLS_COMMAND]);
            button = new JButton(compileString);
            button.setFont(PkgMgrFont);
            button.setToolTipText(Config.getString("tooltip.compile"));
            button.addActionListener(this);
            button.setRequestFocusEnabled(false);   // never get keyboard focus
            makeButtonNotGrow(button);
            buttonPanel.add(button);
            actions.put(button, new Integer(TOOLS_COMPILE));
        }


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
        showUsesCheckbox.setToolTipText(Config.getString("tooltip.showUses"));
        // showUsesCheckbox.setMargin(new Insets(0,0,0,0));
        actions.put(showUsesCheckbox, new Integer(VIEW_SHOWUSES));

        showPanel.add(showUsesCheckbox);
        // Add the Checkbox to ShowExtends Arrows (this must also control
        // the Menu's Checkbox Items)

        showExtendsCheckbox = new JCheckBox(Config.getString("pkgmgr.view.inheritLabel"),
                                            null, true);
        showExtendsCheckbox.addItemListener(this);
        showExtendsCheckbox.setFont(PkgMgrFont);
        showExtendsCheckbox.setToolTipText(Config.getString(
                                                   "tooltip.showExtends"));
        // showExtendsCheckbox.setMargin(new Insets(0,0,0,0));
        actions.put(showExtendsCheckbox, new Integer(VIEW_SHOWINHERITS));
        showPanel.add(showExtendsCheckbox);
        viewPanel.add("Center",showPanel);

        // Image Button Panel to hold the Progress Image
//        JPanel progressPanel = new JPanel ();

        progressButton = new JButton(workingIcon);
        progressButton.setDisabledIcon(notWorkingIcon);
        progressButton.setMargin(new Insets(0, 0, 0, 0));
        progressButton.setToolTipText(Config.getString("tooltip.progress"));
        progressButton.addActionListener(this);
        actions.put(progressButton, new Integer(VIEW_SHOWCONTROLS));
        progressButton.setEnabled(false);
//        progressPanel.add(progressButton);
//        viewPanel.add("South",progressPanel);

        progressButton.setAlignmentX(0.5f);
        buttonPanel.setAlignmentX(0.5f);
        viewPanel.setAlignmentX(0.5f);

        viewPanel.setMaximumSize(viewPanel.getMinimumSize());

        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
        toolPanel.add(buttonPanel);
        toolPanel.add(Box.createVerticalGlue());
        toolPanel.add(viewPanel);
        toolPanel.add(Box.createVerticalStrut(4));
        toolPanel.add(progressButton);
        toolPanel.add(Box.createVerticalStrut(3));

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


        // grey out certain functions if package not open.
        if(isEmptyFrame())
            enableFunctions(false);
    }

    /**
     * setupMenus - Create the menu bar
     */
    private void setupMenus()
    {
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
     * Commands - for lookup from events
     */
    Hashtable actions = new Hashtable();	// mapping from event source -> action

    static final int PROJ_COMMAND = 1000;
    static final int PROJ_NEW = PROJ_COMMAND;
    static final int PROJ_OPEN = PROJ_NEW + 1;
    static final int PROJ_CLOSE = PROJ_OPEN + 1;
    static final int PROJ_SAVE = PROJ_CLOSE + 1;
    static final int PROJ_SAVEAS = PROJ_SAVE + 1;
    static final int PROJ_IMPORTCLASS = PROJ_SAVEAS + 1;
    static final int PROJ_PRINT = PROJ_IMPORTCLASS + 1;
    static final int PROJ_QUIT = PROJ_PRINT + 1;

    static final String[] ProjCmds = {
        "new", "open", "close", "save", "saveAs", "importClass",
        "print", "quit"
    };

    static final KeyStroke[] ProjKeys = {
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_O, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_W, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_S, Event.CTRL_MASK),
        null,
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_P, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, Event.CTRL_MASK)
    };

    static final int[] ProjSeparators = {
        PROJ_SAVEAS, PROJ_IMPORTCLASS, PROJ_PRINT
    };

    static final int EDIT_COMMAND = PROJ_COMMAND + 100;
    static final int EDIT_NEWCLASS = EDIT_COMMAND;
    static final int EDIT_NEWPACKAGE = EDIT_NEWCLASS + 1;
    static final int EDIT_REMOVE = EDIT_NEWPACKAGE + 1;
    static final int EDIT_NEWUSES = EDIT_REMOVE + 1;
    static final int EDIT_NEWINHERITS = EDIT_NEWUSES + 1;
    static final int EDIT_REMOVEARROW = EDIT_NEWINHERITS + 1;


    static final String[] EditCmds = {
        "newClass", "newPackage", "remove", "newUses", "newInherits",
        "removeArrow"
    };

    static final KeyStroke[] EditKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_N, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_R, Event.CTRL_MASK),
        null,
        null,
        null,
        null,
        null
    };

    static final int[] EditSeparators = {
        EDIT_REMOVE  //, EDIT_REMOVEARROW
    };

    static final int TOOLS_COMMAND = EDIT_COMMAND + 100;
    static final int TOOLS_COMPILE = TOOLS_COMMAND;
    static final int TOOLS_COMPILESELECTED = TOOLS_COMPILE + 1;
    static final int TOOLS_REBUILD = TOOLS_COMPILESELECTED + 1;
    static final int TOOLS_BROWSE = TOOLS_REBUILD + 1;
    static final int TOOLS_PREFERENCES = TOOLS_BROWSE + 1;
//    static final int TOOLS_PREFERENCES = TOOLS_REBUILD + 1;

    static final String[] ToolsCmds = {
	"compile", "compileSelected", "rebuild", "browse", "preferences",
    };

    static final KeyStroke[] ToolsKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_C, Event.SHIFT_MASK | Event.CTRL_MASK),
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_B, Event.CTRL_MASK),
        null
    };

    static final int[] ToolsSeparators = {
	TOOLS_REBUILD,
	TOOLS_BROWSE
    };

    static final int VIEW_COMMAND = TOOLS_COMMAND + 100;
    static final int VIEW_SHOWUSES = VIEW_COMMAND;
    static final int VIEW_SHOWINHERITS = VIEW_SHOWUSES + 1;
    static final int VIEW_SHOWCONTROLS = VIEW_SHOWINHERITS + 1;
    static final int VIEW_SHOWTERMINAL = VIEW_SHOWCONTROLS + 1;
    static final int VIEW_CLEARTERMINAL = VIEW_SHOWTERMINAL + 1;

    static final String[] ViewCmds = {
        "showUses", "showInherits", "showExecControls", "showTerminal",
        "clearTerminal",
    };

    static final KeyStroke[] ViewKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_U, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_I, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_D, Event.CTRL_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_T, Event.CTRL_MASK),
        null,
    };

    static final int[] ViewSeparators = {
        VIEW_SHOWINHERITS, VIEW_SHOWTERMINAL,
    };

    static final int GRP_COMMAND = VIEW_COMMAND + 100;
    static final int GRP_CREATE = GRP_COMMAND;
    static final int GRP_OPEN = GRP_CREATE + 1;
    static final int GRP_UPDATESELECTED = GRP_OPEN + 1;
    static final int GRP_UPDATEALL = GRP_UPDATESELECTED + 1;
    static final int GRP_COMMITSELECTED = GRP_UPDATEALL + 1;
    static final int GRP_COMMITALL = GRP_COMMITSELECTED + 1;
    static final int GRP_STATUSSELECTED = GRP_COMMITALL + 1;
    static final int GRP_STATUSALL = GRP_STATUSSELECTED + 1;
    static final int GRP_USERS = GRP_STATUSALL + 1;
    static final int GRP_CONFIG = GRP_USERS + 1;

    static final String[] GrpCmds = {
        "make", "open",
        "updateSelected", "updateAll",
        "commitSelected", "commitAll",
        "statusSelection", "statusAll",
        "users",
        "configuration"
    };

    static final KeyStroke[] GrpKeys = {
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
    };

    static final int[] GrpSeparators = {
        GRP_OPEN, GRP_COMMITALL, GRP_USERS
    };

    static final int HELP_COMMAND = GRP_COMMAND + 100;
    static final int HELP_ABOUT = HELP_COMMAND;
    static final int HELP_COPYRIGHT = HELP_ABOUT + 1;
    static final int HELP_TUTORIAL = HELP_COPYRIGHT + 1;
    static final int HELP_REFERENCE = HELP_TUTORIAL + 1;
    static final int HELP_STANDARDAPI = HELP_REFERENCE + 1;

    static final String[] HelpCmds = {
        "about", "copyright", "tutorial", "reference", "standardApi",
    };

    static final KeyStroke[] HelpKeys = {
        null,
        null,
        null,
        null,
        null,
    };

    static final int[] HelpSeparators = {
        HELP_COPYRIGHT
    };

    static final int[] CmdTypes = {
        PROJ_COMMAND, EDIT_COMMAND, TOOLS_COMMAND, VIEW_COMMAND,
        /* GRP_COMMAND, */ HELP_COMMAND
    };

    static final String[] CmdTypeNames = {
        "package", "edit", "tools", "view", /* "group", */ "help"
    };

    static final String[][] CmdStrings = {
        ProjCmds, EditCmds, ToolsCmds, ViewCmds, /* GrpCmds, */ HelpCmds
    };

    static final KeyStroke[][] CmdKeys = {
        ProjKeys, EditKeys, ToolsKeys, ViewKeys, /* GrpKeys, */ HelpKeys
    };

    static final int[][] CmdSeparators = {
        ProjSeparators, EditSeparators, ToolsSeparators, ViewSeparators,
        /* GrpSeparators, */ HelpSeparators
    };

    /**
     * Enable/disable functionality
     */
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
}
