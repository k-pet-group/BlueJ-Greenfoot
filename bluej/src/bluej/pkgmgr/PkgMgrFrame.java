package bluej.pkgmgr;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.awt.*;
import java.awt.print.*;
import java.text.DateFormat;

//import com.apple.mrj.MRJApplicationUtils;  // for handling MacOS specific events
//import com.apple.mrj.MRJQuitHandler;
//import com.apple.mrj.MRJAboutHandler;

import bluej.Config;
import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.utility.*;
import bluej.graph.GraphEditor;
import bluej.debugger.*;
import bluej.views.*;
import bluej.terminal.Terminal;
import bluej.terminal.TerminalButtonModel;
import bluej.prefmgr.PrefMgrDialog;
import bluej.prefmgr.PrefMgr;
import bluej.browser.LibraryBrowser;
import bluej.utility.filefilter.JavaSourceFilter;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;
import bluej.groupwork.*;


/**
 * The main user interface frame which allows editing of packages
 *
 * @version $Id: PkgMgrFrame.java 986 2001-10-23 14:22:23Z mik $
 */
public class PkgMgrFrame extends JFrame
    implements BlueJEventListener, ActionListener, ItemListener, MouseListener,
               PackageEditorListener //, MRJQuitHandler, MRJAboutHandler
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

    static final int DEFAULT_WIDTH = 420;
    static final int DEFAULT_HEIGHT = 300;

    private static final String bluejUrl = Config.getPropString("bluej.url.bluej");
    private static final String tutorialUrl = Config.getPropString("bluej.url.tutorial");
    private static final String referenceUrl = Config.getPropString("bluej.url.reference");

    private static final String webBrowserMsg = Config.getString("pkgmgr.webBrowserMsg");
    private static final String webBrowserError = Config.getString("pkgmgr.webBrowserError");
    private static final String creatingVM = Config.getString("pkgmgr.creatingVM");
    private static final String creatingVMDone = Config.getString("pkgmgr.creatingVMDone");

    private static final String generatingDocu = Config.getString("pkgmgr.generatingDocu");
    private static final String docuGenerated = Config.getString("pkgmgr.docuGenerated");
    private static final String docuAborted = Config.getString("pkgmgr.docuAborted");

    private static final String addClassTitle = Config.getString("pkgmgr.addClass.title");
    private static final String addLabel = Config.getString("pkgmgr.addClass.buttonLabel");
    private static final String importpkgTitle = Config.getString("pkgmgr.importPkg.title");

    private static final Icon workingIcon = Config.getImageAsIcon("image.working");
    private static final Icon notWorkingIcon = Config.getImageAsIcon("image.working.disab");
    private static final Icon stoppedIcon = Config.getImageAsIcon("image.working.stopped");

    private static PageFormat pageFormat = new PageFormat();

    // instance fields:

    private JPanel buttonPanel;
    private JPanel viewPanel;
    private JPanel showPanel;
    private JButton imgExtendsButton;
    private JButton imgDependsButton;

    private JLabel statusbar = new JLabel(" ");

    private JMenuBar menubar = null;
    private JButton progressButton;

    private JCheckBoxMenuItem showUsesMenuItem;
    private JCheckBoxMenuItem showExtendsMenuItem;

    private JCheckBox showUsesCheckbox;
    private JCheckBox showExtendsCheckbox;

    /* The scroller which holds the PackageEditor we use to edit packages */
    private JScrollPane classScroller = null;

    /* The package that this frame is working on or null for the case where
       there is no package currently being edited (check with isEmptyFrame()) */
    private Package pkg = null;

    /* The graph editor which works on the package or null for the case where
       there is no package current being edited (isEmptyFrame() == true) */
    private PackageEditor editor = null;

    private ObjectBench objbench;
    private LibraryCallDialog libraryCallDialog = null;

    // ============================================================
    // static methods to create and remove frames

    private static List frames = new ArrayList();  // of PkgMgrFrames

    /**
     * Open a PkgMgrFrame with no package.
     * Packages can be installed into this
     * frame using the methods openPackage/closePackage.
     */
    public static PkgMgrFrame createFrame()
    {
        PkgMgrFrame frame = new PkgMgrFrame();

        frames.add(frame);

        BlueJEvent.addListener(frame);

        return frame;
    }

    /**
     * Open a PkgMgrFrame with a package.
     * This may create a new frame or return an existing frame
     * if this package is already being edited by a frame. If an empty
     * frame exists, that frame will be used to show the package.
     */
    public static PkgMgrFrame createFrame(Package pkg)
    {
        PkgMgrFrame pmf = findFrame(pkg);

        if (pmf == null) {
            // check whether we've got an empty frame

            if(frames.size() == 1)
                pmf = (PkgMgrFrame)frames.get(0);

            if((pmf == null) || !pmf.isEmptyFrame())
                pmf = createFrame();

            pmf.openPackage(pkg);
        }

        return pmf;
    }

    /**
     * Remove a frame from the set of currently open PkgMgrFrames.
     * The PkgMgrFrame must not be editing a package when this
     * function is called.
     */
    public static void closeFrame(PkgMgrFrame frame)
    {
        if(!frame.isEmptyFrame())
            throw new IllegalArgumentException();

        frames.remove(frame);

        BlueJEvent.removeListener(frame);

        // frame should be garbage collected but we will speed it
        // on its way
        frame.dispose();
    }

    /**
     * Find a frame which is editing a particular Package and return
     * it or return null if it is not being edited
     */
    public static PkgMgrFrame findFrame(Package pkg)
    {
        for(Iterator i = frames.iterator(); i.hasNext(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)i.next();

            if (!pmf.isEmptyFrame() && pmf.getPackage() == pkg)
                return pmf;
        }
        return null;
    }

    /**
     * @return the number of currently open top level frames
     */
    public static int frameCount()
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
     * Find all PkgMgrFrames which are currently editing a particular
     * project
     *
     * @param   proj        the project whose packages to look for
     *
     * @return  an array of open PkgMgrFrame objects which are currently
     *          editing a package from this project, or null if none exist
     */
    public static PkgMgrFrame[] getAllProjectFrames(Project proj)
    {
        return getAllProjectFrames(proj, "");
    }

    /**
     * Find all PkgMgrFrames which are currently editing a particular
     * project, and which are below a certain point in the package
     * heirarchy.
     *
     * @param   proj        the project whose packages to look for
     * @param   pkgPrefix   the package name of a package to look for
     *                      it and all its children ie if passed
     *                      java.lang we would return frames for java.lang,
     *                      and java.lang.reflect if they exist
     *
     * @return  an array of open PkgMgrFrame objects which are currently
     *          editing a package from this project and which have the
     *          package prefix specified, or null if none exist
     */
    public static PkgMgrFrame[] getAllProjectFrames(Project proj, String pkgPrefix)
    {
        List list = new ArrayList();
        String pkgPrefixWithDot = pkgPrefix + ".";

        for(Iterator i = frames.iterator(); i.hasNext(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)i.next();

            if (!pmf.isEmptyFrame() && pmf.getProject() == proj) {

                String fullName = pmf.getPackage().getQualifiedName();

                // we either match against the package prefix with a
                // dot added (this stops false matches against similarly
                // named package ie java.lang and java.language) or we
                // match the full name against the package prefix
                if (fullName.startsWith(pkgPrefixWithDot))
                    list.add(pmf);
                else if (fullName.equals(pkgPrefix) || (pkgPrefix.length()==0))
                    list.add(pmf);
            }
        }

        if (list.size() == 0)
            return null;

        return (PkgMgrFrame[])list.toArray(new PkgMgrFrame[list.size()]);
    }

    /**
     * Refresh (repaint()) all open frames.
     * Called when class diagram notation style is changed
     * in PrefMgr.
     */
    public static void refreshAllFrames()
    {
        if (!frames.isEmpty()) {
            for(Iterator i = frames.iterator(); i.hasNext(); ) {
                PkgMgrFrame frame = (PkgMgrFrame)i.next();
                frame.setButtonImages();
                frame.repaint();
            }
        }
    }

    /**
     * Display a short text message to the user. Without specifying a package,
     * this is done by showing the message in the status bars of all open
     * package windows.
     */
    public static void displayMessage(String message)
    {
        for(Iterator i = frames.iterator(); i.hasNext(); ) {
            PkgMgrFrame frame = (PkgMgrFrame)i.next();
            frame.setStatus(message);
        }
    }

    /**
     * Display a short text message in the frame of the specified package.
     */
    public static void displayMessage(Package sourcePkg, String message)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if(pmf != null)
            pmf.setStatus(message);
    }

    public static void showError(Package sourcePkg, String msgId)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if(pmf != null)
            DialogManager.showError(pmf, msgId);
    }

    public static void showMessage(Package sourcePkg, String msgId)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if(pmf != null)
            DialogManager.showMessage(pmf, msgId);
    }

    public static void showMessageWithText(Package sourcePkg, String msgId, String text)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if(pmf != null)
            DialogManager.showMessageWithText(pmf, msgId, text);
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

        makeFrame();

        updateWindowTitle();
	//MRJApplicationUtils.registerQuitHandler(this);
	//MRJApplicationUtils.registerAboutHandler(this);

        setStatus(bluej.Main.BLUEJ_VERSION_TITLE);
    }


    /**
     *  Displays the package in the frame for editing
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
        this.editor = new PackageEditor(pkg);
        editor.addMouseListener(this);        // This listener MUST be before
        editor.addMouseListener(editor);      //  the editor itself!
        this.pkg.editor = this.editor;

        classScroller.setViewportView(editor);
        editor.addPackageEditorListener(this);

        // fetch some properties from the package that interest us
        Properties p = pkg.getLastSavedProperties();

        try {
            String width_str = p.getProperty(
                                             "package.editor.width",
                                             Integer.toString(DEFAULT_WIDTH));
            String height_str = p.getProperty(
                                              "package.editor.height",
                                              Integer.toString(DEFAULT_HEIGHT));

            classScroller.setPreferredSize(new Dimension(Integer.parseInt(width_str),
                                                         Integer.parseInt(height_str)));

            String x_str = p.getProperty("package.editor.x",
                                              Integer.toString(16));
            String y_str = p.getProperty("package.editor.y",
                                              Integer.toString(16));

            int x = 16, y = 16;

            x = Integer.parseInt(x_str);
            y = Integer.parseInt(y_str);

            if (x > (Config.screenBounds.width - 16))
                x = Config.screenBounds.width - 16;

            if (y > (Config.screenBounds.height - 16))
                y = Config.screenBounds.height - 16;

            setLocation(x,y);
        }
        catch (Exception e) { }

        pack();
        editor.revalidate();

        // we have had trouble with BlueJ freezing when
        // the enable/disable GUI code was run off a menu
        // item. This code will delay the menu disable
        // until after menu processing has finished
        Runnable enableUI = new Runnable() {
                public void run() {
                    enableFunctions(true);  // changes menu items
                    updateWindowTitle();
                    show();
                }
            };

        SwingUtilities.invokeLater(enableUI);
    }

    /**
     * Closes the current package.
     */
    public void closePackage()
    {
        if(isEmptyFrame())
            return;

        classScroller.setViewportView(null);

        editor.removePackageEditorListener(this);
        editor.removeMouseListener(this);

        getObjectBench().removeAll(getProject().getUniqueId());

        getPackage().closeAllEditors();

        Project proj = getProject();

        editor = null;
        pkg = null;

        // if there are no other frames editing this project, we close
        // the project
        if (PkgMgrFrame.getAllProjectFrames(proj) == null)
            Project.closeProject(proj);

        // we have had trouble with BlueJ freezing when
        // the enable/disable GUI code was run off a menu
        // item. This code will delay the menu disable
        // until after menu processing has finished
        Runnable disableUI = new Runnable() {
                public void run() {
                    enableFunctions(false);  // changes menu items
                    updateWindowTitle();
                }
            };

        SwingUtilities.invokeLater(disableUI);
    }

    /**
     * Override standard show to add de-iconify and bring-to-front.
     */
    public void show()
    {
        super.show();
        setState(Frame.NORMAL);
        toFront();
    }

    /**
     * Return the package shown by this frame.
     *
     * This call should be bracketed by a call to
     * isEmptyFrame() before use.
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

    // =================== MouseListener interface ======================

    public void mousePressed(MouseEvent evt)
    {
        clearStatus();
    }

    public void mouseReleased(MouseEvent evt) {}
    public void mouseClicked(MouseEvent evt) {}
    public void mouseEntered(MouseEvent evt) {}
    public void mouseExited(MouseEvent evt) {}

    // =============== PackageEditorListener interface ==================

    /**
     * Deal with an event generated by a target in the package we are
     * currently editing.
     */
    public void targetEvent(PackageEditorEvent e)
    {
        int evtId = e.getID();

        switch(evtId) {
        case PackageEditorEvent.TARGET_CALLABLE:   // user has initiated method
            //  call or constructor
            callMethod(e.getCallable());
            break;

        case PackageEditorEvent.TARGET_REMOVE:     // user has initiated target
            //   "remove" option
            doRemove((Target) e.getSource());
            break;

        case PackageEditorEvent.TARGET_OPEN:       // user has initiated a
            //   package open operation
            openPackageTarget(e.getName());
            break;

        case PackageEditorEvent.TARGET_RUN:       // user has initiated a
            //   run operation
            runAppletTarget((Target) e.getSource());
            break;

        case PackageEditorEvent.OBJECT_PUTONBENCH: // "Get" object from
            //   object inspector
            putObjectOnBench(e.getDebuggerObject(), e.getFieldName(),
                             e.getInstanceName());
            break;


        }
    }

    // ============ end of PackageEditorListener interface ===============

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

        case PROJ_OPENNONBLUEJ:        // can be executed when isEmptyFrame() is true
            doOpenNonBlueJ();
            break;

        case PROJ_CLOSE:
            doClose(true);
            break;

        case PROJ_SAVE:
            getProject().saveAll();
            break;

        case PROJ_SAVEAS:
            getProject().saveAs(this);
            break;

        case PROJ_IMPORT:        // can be executed when isEmptyFrame() is true
            doImport();
            break;

        case PROJ_EXPORT:
            doExport();
            break;

        case PROJ_PAGESETUP:
            pageSetup();
            break;

        case PROJ_PRINT:
            print();
            break;

        case PROJ_QUIT:        // can be executed when isEmptyFrame() is true
	    wantToQuit();
            break;

            // Edit commands
        case EDIT_NEWCLASS:
            createNewClass();
            break;

        case EDIT_NEWPACKAGE:
            createNewPackage();
            break;

        case EDIT_ADDCLASS:
            doAddFromFile();
            break;

        case EDIT_REMOVE:
            doRemove();
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
            compileSelected();
            break;

        case TOOLS_CALLLIBRARY:
            callLibraryClass();
            break;

        case TOOLS_REBUILD:
            pkg.rebuild();
            break;

        case TOOLS_GENERATEDOC:
            String message = pkg.generateDocumentation();
            if (message!="")
                DialogManager.showText(this,message);
            break;

            //         case TOOLS_BROWSE:
            //             DialogManager.NYI(this);
            //             //            LibraryBrowser lb = new LibraryBrowser();
            //             break;

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
            ExecControls.showHide(true, true, null);
            break;

            // Group work commands
        case GRP_CREATE:
            //CVSGroupPkgManager groupPkgMgr=new CVSGroupPkgManager();
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
        case HELP_ABOUT:                    // can be executed when isEmptyFrame() is true
	    handleAbout();
            break;

        case HELP_VERSIONCHECK:             // can be executed when isEmptyFrame() is true
            VersionCheckDialog dialog = new VersionCheckDialog(this);
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

        case HELP_WEBSITE:                  // can be executed when isEmptyFrame() is true
            showWebPage(bluejUrl);
            break;

        case HELP_TUTORIAL:                 // can be executed when isEmptyFrame() is true
            showWebPage(tutorialUrl);
            break;

        case HELP_STANDARDAPI:              // can be executed when isEmptyFrame() is true
            showWebPage(Config.getPropString("bluej.url.javaStdLib"));
            break;

        default:
            Debug.reportError("unknown command ID");
            break;
        }
    }

    // --- below are implementations of particular user actions ---

    /**
     * Allow the user to select a directory into which we create
     * a project.
     */
    protected boolean doNewProject()
    {
        String newname = FileUtility.getFileName(this, newpkgTitle, createLabel, false);

        if (newname == null)
            return false;

        if (Project.createNewProject(newname)) {
            Project proj = Project.openProject(newname);

            if(isEmptyFrame()) {
                openPackage(proj.getPackage(""));
            }
            else {
                PkgMgrFrame pmf = createFrame(proj.getPackage(""));
                DialogManager.tileWindow(pmf, this);
                pmf.show();
            }
        }
        return true;
    }

    /**
     * Open a dialog that lets the user choose a project.
     * The project selected is opened in a frame.
     */
    private void doOpen()
    {
        File dirName = FileUtility.getPackageName(this);

        if (dirName != null) {
            Project openProj = Project.openProject(dirName.getAbsolutePath());

            if(openProj != null) {

                Package pkg = openProj.getPackage(openProj.getInitialPackageName());

                PkgMgrFrame pmf;

                if ((pmf = findFrame(pkg)) == null) {
                    if(isEmptyFrame()) {
                        pmf = this;
                        openPackage(pkg);
                    }
                    else {
                        pmf = createFrame(pkg);

                        DialogManager.tileWindow(pmf, this);
                    }
                }

                pmf.show();
            }
        }
    }

    /**
     * Open a dialog that lets the user choose a project.
     * The project selected is opened in a frame.
     */
    private void doOpenNonBlueJ()
    {
        File dirName = FileUtility.getNonBlueJDirectoryName(this);

        if(dirName == null)
            return;

        if (dirName != null) {
            if (Project.openProject(dirName.getAbsolutePath()) != null) {
                DialogManager.showError(this, "open-non-bluej-already-bluej");
                return;
            }

            /*            File aFile = FileUtility.findFile(dirName, ".java");

                          if (aFile != null) {
                          ClassInfo info = ClassParser.parse(aFile);

                          DialogManager.showError(this, "open-non-bluej-invalid");
                          }
            */
            // add bluej.pkg files through the directory structure
            Import.convertDirectory(dirName);

            Project openProj = Project.openProject(dirName.getAbsolutePath());

            // if after converting the directory, the project still doesn't open
            // then there was no java source in the directories
            if (openProj == null) {
                return;
            }

            Package pkg = openProj.getPackage(openProj.getInitialPackageName());

            PkgMgrFrame pmf;

            if ((pmf = findFrame(pkg)) == null) {
                if(isEmptyFrame()) {
                    pmf = this;
                    openPackage(pkg);
                }
                else {
                    pmf = createFrame(pkg);

                    DialogManager.tileWindow(pmf, this);
                }
            }

            pmf.show();
        }
    }


    /**
     * Perform a user initiated close of this frame/package.
     *
     * There are two different methods for the user to initiate
     * a close. One is through the "Close" menu item and the other
     * is with the windows close button. We want slightly different
     * behaviour for these two cases.
     */
    public void doClose(boolean keepLastFrame)
    {
        doSave();
        closePackage();

        // If only one frame and this was from the menu
        // "close", close should close existing package rather
        // than remove frame

        if(frameCount() == 1) {
            if(keepLastFrame) {        // close package, leave frame
                updateWindowTitle();
            }
            else {                      // all frames gone, lets quit
                doQuit();
            }
        }
        else {                          // remove package and frame
            PkgMgrFrame.closeFrame(this);
        }
    }

    /**
     * Quit menu was chosen - redefined from MRJQuitHandler
     */
    public void handleQuit()
    {
        Debug.message("exiting through MacOS quit handler!");
        // this is an error on MacOS: when run from the MacOS quithandler
	// no event handling is possible! showing the dialog breaks everything.
	// fix: show the dialog from another thread... (or don't show dialog)
        wantToQuit();
    }
        
    /**
     * Quit menu item was chosen.
     */
    public void wantToQuit()
    {
	int answer = 0;
	if(frameCount() > 1)
	    answer = DialogManager.askQuestion(this, "quit-all");
	if(answer == 0)
	    doQuit();
    }

    private void doQuit()
    {
        // close all open frames.
        PkgMgrFrame[] f = getAllFrames();

        // We replicate some of the behaviour of doClose() here
        // rather than call it to avoid a nasty recursion
        for(int i = f.length - 1; i >= 0; i--) {
            f[i].doSave();
            f[i].closePackage();
            PkgMgrFrame.closeFrame(f[i]);
        }

        bluej.Main.exit();
    }

    /**
     * Save this package. Don't ask questions - just do it.
     */
    protected void doSave()
    {
        if(isEmptyFrame())
            return;

        // store the current editor size in the bluej.pkg file
        Properties p = new Properties();

        Dimension d = classScroller.getSize(null);

        p.put("package.editor.width", Integer.toString(d.width));
        p.put("package.editor.height", Integer.toString(d.height));

        Point point = getLocation();

        p.put("package.editor.x", Integer.toString(point.x));
        p.put("package.editor.y", Integer.toString(point.y));

        pkg.save(p);

        setStatus(packageSaved);
    }

    /**
     * Import into a new project or import into the current project.
     */
    private void doImport()
    {
        // prompt for the directory to import from
        File importDir;
        String importName = FileUtility.getFileName(this,
                             Config.getString("pkgmgr.importPkg.title"),
                             Config.getString("pkgmgr.importPkg.buttonLabel"),
                             true);

        if (importName == null)
            return;

        importDir = new File(importName);

        if (!importDir.isDirectory())
            return;

        // if we are an empty then we shouldn't go on (we shouldn't get
        // here)
        if (isEmptyFrame())
            return;

        // recursively copy files from import directory to package directory
        Object[] fails = FileUtility.recursiveCopyFile(
                                                       new File(importName), getPackage().getPath());

        // if we have any files which failed the copy, we show them now
        if (fails != null) {
            JDialog ifd = new ImportFailedDialog(this, fails);
            ifd.show();
        }

        // add bluej.pkg files through the imported directory structure
        Import.convertDirectory(getPackage().getPath());

        // reload all the packages (which discovers classes which may have
        // been added by the import)
        getProject().reloadAll();
    }

    /**
     * Implementation of the "Add Class from File" user function
     */
    private void doAddFromFile()
    {
        String className = FileUtility.getFileName(this,
                                                   addClassTitle, addLabel, false,
                                                   FileUtility.getJavaSourceFilter());

        if(className == null)
            return;

        File classFile = new File(className);

        int result = pkg.importFile(classFile);
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

    /**
     * Implementation of the "Export" user function
     */
    private void doExport()
    {
        ExportManager exporter = new ExportManager(this);
        exporter.export();
    }

    /**
     * Creates a page setup dialog to alter page dimensions.
     *
     */
    public void pageSetup()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        pageFormat = job.validatePage(job.pageDialog(pageFormat));
    }


    /**
     * print - implementation of the "print" user function
     */
    private void print()
    {
        PackagePrinter printer = new PackagePrinter(pkg, pageFormat);
        printer.setPriority((Thread.currentThread().getPriority() - 1));
        printer.start();
    }

    /**
     * About menu was chosen - redefined from MRJAboutHandler
     */
    public void handleAbout()
    {
            AboutBlue about = new AboutBlue(this, bluej.Main.BLUEJ_VERSION);
            about.setVisible(true);
    }

    /**
     * Interactively call a method or a constructor
     */
    private void callMethod(CallableView cv)
    {
        ResultWatcher watcher = null;

        if(cv instanceof ConstructorView) {
            // if we are constructing an object, create a watcher that waits for
            // completion of the call and then places the object on the object
            // bench
            watcher = new ResultWatcher() {
                    public void putResult(DebuggerObject result, String name) {
                        if((name == null) || (name.length() == 0))
                            name = "result";
                        if(result != null) {
                            ObjectWrapper wrapper =
                                new ObjectWrapper(PkgMgrFrame.this, result.getInstanceFieldObject(0),
                                                  name);
                            getObjectBench().add(wrapper);
                        }
                        else
                            Debug.reportError("cannot get execution result");
                    }
                };
        }
        else if(cv instanceof MethodView) {
            MethodView mv = (MethodView) cv;

            // if we are calling a main method then we want to simulate a
            // new launch of an application, so first of all we unload all our
            // classes (prevents problems with static variables not being
            // reinitialised because the class hangs around from a previous call)
            if(mv.isMain()) {
                getProject().removeLocalClassLoader();
                getProject().removeRemoteClassLoaderLeavingBreakpoints();
            }

            // if we are calling a method that has a result, create a watcher
            // that waits for completion of the call and then displays the
            // result
            if(!mv.isVoid()) {
                watcher = new ResultWatcher() {
                        public void putResult(DebuggerObject result, String name) {
                            ObjectViewer viewer =
                                ObjectViewer.getViewer(false, result, name, getPackage(), true,
                                                       PkgMgrFrame.this);
                            BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, viewer.getResult());
                        }
                    };
            }
        }

        // create an Invoker to handle the actual invocation

        new Invoker(this, cv, null, watcher);
    }

    /**
     * Open a package target.
     */
    private void openPackageTarget(String newname)
    {
        PkgMgrFrame pmf;
        Package p = getPackage().getProject().getPackage(newname);

        if ((pmf = findFrame(p)) == null) {
            pmf = createFrame(p);
            DialogManager.tileWindow(pmf, this);
        }
        pmf.show();
    }

    /**
     * Run a target (currently only applets)
     */
    private void runAppletTarget(Target t)
    {
        if (t instanceof ClassTarget) {
            ClassTarget ct = (ClassTarget) t;

            ct.runApplet(this);
        }
    }

    /**
     * Create an object on the object bench.
     */
    private void putObjectOnBench(DebuggerObject object, String fieldName, String instanceName)
    {
        ObjectWrapper wrapper = new ObjectWrapper(this, object, fieldName);
        getObjectBench().add(wrapper);  // might change name

        // load the object into runtime scope
        Debugger.debugger.addObjectToScope(getPackage().getId(), instanceName,
                                           fieldName, wrapper.getName());
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
                String template = dlg.getTemplateName();
                target = new ClassTarget(pkg, name, template);

                target.generateSkeleton();

                pkg.findSpaceForVertex(target);
                pkg.addTarget(target);

                editor.revalidate();
                editor.scrollRectToVisible(target.getRectangle());
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

                if(newpkgDir.mkdir()) {
                    File newpkgFile = new File(newpkgDir, Package.pkgfileName);

                    try {
                        if(newpkgFile.createNewFile()) {
                            PackageTarget target = new PackageTarget(pkg, name);

                            pkg.findSpaceForVertex(target);
                            pkg.addTarget(target);
                            editor.revalidate();
                            editor.scrollRectToVisible(target.getRectangle());
                            editor.repaint();
                        }
                    }
                    catch(IOException ioe) { }
                }
            }
        }
    }

    public void doRemove(Target target)
    {
        if(target instanceof ClassTarget) {
            ClassTarget t = (ClassTarget) target;
            removeClass(t);
        } else if(target instanceof PackageTarget) {
            if(!(target instanceof ParentPackageTarget)) {
                PackageTarget t = (PackageTarget) target;
                removePackage(t);
            }
        }
    }

    /**
     * Removes the currently selected ClassTarget
     * from the Package in this frame.
     */
    public void doRemove()
    {
        Target target = pkg.getSelectedTarget();

        if(target == null)
            DialogManager.showError(this, "no-class-selected");
        else {
            doRemove(target);
        }
    }

    /**
     * Removes the specified ClassTarget from the Package.
     */
    public void removeClass(ClassTarget removableTarget)
    {
        // Check they realise that this will delete the files.
        int response = DialogManager.askQuestion(this, "really-remove-class");

        // if they agree
        if(response == 0)
            pkg.removeClass(removableTarget);
    }

    /**
     * Removes the specified PackageTarget from the Package.
     */
    public void removePackage(PackageTarget removableTarget)
    {
        String name = removableTarget.getQualifiedName();
        PkgMgrFrame[] f = getAllProjectFrames(getProject(), name);

        if (f != null) {
            DialogManager.showError(this, "remove-package-open");
            return;
        }

        // Check they realise that this will delete ALL the files.
        int response = DialogManager.askQuestion(this, "really-remove-package");

        // if they agree
        if(response == 0)
            pkg.removePackage(removableTarget);
    }


    /**
     * Compiles the currently selected class target.
     */
    public void compileSelected()
    {
        Target target = pkg.getSelectedTarget();

        if(target == null)
            DialogManager.showError(this, "no-class-selected");
        else {
            if(target instanceof ClassTarget) {
                ClassTarget t = (ClassTarget) target;
                pkg.compile(t);
            }
        }
    }

    /**
     * User function "Use Library Class...". Pop up the dialog that allows
     * users to invoke library classes.
     */
    public void callLibraryClass()
    {
        if(libraryCallDialog == null)
            libraryCallDialog = new LibraryCallDialog(this);
        libraryCallDialog.setVisible(true);
    }

    /**
     * Toggle the state of the "show uses arrows" switch.
     */
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
            executionHalted();
            break;
        case BlueJEvent.HALT:
            executionHalted();
            break;
        case BlueJEvent.CONTINUE:
            executionContinued();
            break;
        case BlueJEvent.GENERATING_DOCU:
            setStatus(generatingDocu);
            break;
        case BlueJEvent.DOCU_GENERATED:
            setStatus(docuGenerated);
            break;
        case BlueJEvent.DOCU_ABORTED:
            setStatus(docuAborted);
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
    private void executionFinished()
    {
        progressButton.setEnabled(false);
        if(ExecControls.execControlsShown())
            ExecControls.getExecControls().updateThreads(null);
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


    // --- general support functions for user function implementations ---

    public String toString()
    {
        String str = "PkgMgrFrame(): ";

        if(isEmptyFrame())
            str += "empty";
        else
            str += getPackage().toString() + " " + getProject().toString();

        return str;
    }

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
        setIconImage(Config.frameImage);

        setupMenus();

        JPanel mainPanel = new JPanel();

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

            imgDependsButton = new JButton();
            imgDependsButton.setToolTipText(Config.getString("tooltip.newUses"));
            imgDependsButton.addActionListener(this);
            imgDependsButton.setRequestFocusEnabled(false);   // never get keyboard focus
            buttonPanel.add(imgDependsButton);
            buttonPanel.add(Box.createVerticalStrut(3));
            actions.put(imgDependsButton, new Integer(EDIT_NEWUSES));

            imgExtendsButton = new JButton();
            imgExtendsButton.setToolTipText(Config.getString("tooltip.newExtends"));
            imgExtendsButton.addActionListener(this);
            imgExtendsButton.setRequestFocusEnabled(false);   // never get keyboard focus
            buttonPanel.add(imgExtendsButton);
            buttonPanel.add(Box.createVerticalStrut(3));
            actions.put(imgExtendsButton, new Integer(EDIT_NEWINHERITS));

            setButtonImages();

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
        JScrollPane objScroller = new JScrollPane(objbench = new ObjectBench());
        bottomPanel.add("North", objScroller);
        bottomPanel.add("South", statusbar);

        mainPanel.add("West", toolPanel);
        mainPanel.add("South", bottomPanel);

        classScroller = new JScrollPane();
        mainPanel.add(classScroller, "Center");
        classScroller.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        getContentPane().add(mainPanel);

        pack();

        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent E)
                {
                    PkgMgrFrame pmf = (PkgMgrFrame)E.getWindow();
                    pmf.doClose(false);
                }
            });

        // grey out certain functions if package not open.
        if(isEmptyFrame())
            enableFunctions(false);
    }


    /**
     * Set the images on the interface buttons according to preferences.
     */
    private void setButtonImages()
    {
        String dependsImage = "image.build.depends";
        String extendsImage = "image.build.extends";
        String umlSuffix = ".uml";
        if(PrefMgr.isUML()) {
            dependsImage += umlSuffix;
            extendsImage += umlSuffix;
        }
        ImageIcon dependsIcon = Config.getImageAsIcon(dependsImage);
        ImageIcon extendsIcon = Config.getImageAsIcon(extendsImage);
        imgDependsButton.setIcon(dependsIcon);
        imgExtendsButton.setIcon(extendsIcon);
        makeButtonNotGrow(imgExtendsButton);
        makeButtonNotGrow(imgDependsButton);
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
            if (CmdTypes[menuType] == GRP_COMMAND)
                continue;

            menu = new JMenu(menuStr);

            for(int i = 0; i < CmdStrings[menuType].length; i++) {
                int actionId = CmdTypes[menuType] + i;
                String itemId = menuId + "." + CmdStrings[menuType][i];
                String itemStr = Config.getString(itemId);
                ImageIcon itemIcon = Config.getImageAsIcon("image." + CmdTypeNames[menuType] + "." + CmdStrings[menuType][i]);
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
                    item.setModel(new ExecControlButtonModel());
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

                if (itemIcon != null)
                    item.setIcon(itemIcon);

                // Add new Item to the Menu & associate Action to it.
                if(CmdTypes[menuType] != GRP_COMMAND) {
                    menu.add(item);
                    actions.put(item, new Integer(actionId));
                }

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

    // menu bar definition

    private static int SHORTCUT_MASK = 
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
        //Event.CTRL_MASK;

    static final int PROJ_COMMAND = 1000;
    static final int PROJ_NEW = PROJ_COMMAND;
    static final int PROJ_OPEN = PROJ_NEW + 1;
    static final int PROJ_OPENNONBLUEJ = PROJ_OPEN + 1;
    static final int PROJ_CLOSE = PROJ_OPENNONBLUEJ + 1;
    static final int PROJ_SAVE = PROJ_CLOSE + 1;
    static final int PROJ_SAVEAS = PROJ_SAVE + 1;
    static final int PROJ_IMPORT = PROJ_SAVEAS + 1;
    static final int PROJ_EXPORT = PROJ_IMPORT + 1;
    static final int PROJ_PAGESETUP = PROJ_EXPORT + 1;
    static final int PROJ_PRINT = PROJ_PAGESETUP + 1;
    static final int PROJ_QUIT = PROJ_PRINT + 1;

    static final String[] ProjCmds = {
        "new", "open", "openNonBlueJ", "close", "save", "saveAs", "import", "export",
        "pageSetup", "print", "quit"
    };

    static final KeyStroke[] ProjKeys = {
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_O, SHORTCUT_MASK),
        null,   // open non bluej
        KeyStroke.getKeyStroke(KeyEvent.VK_W, SHORTCUT_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_S, SHORTCUT_MASK),
        null, // save as
        null, // import
        null, // export
        null, // page setup
        KeyStroke.getKeyStroke(KeyEvent.VK_P, SHORTCUT_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_Q, SHORTCUT_MASK)
    };

    static final int[] ProjSeparators = {
        PROJ_SAVEAS, PROJ_EXPORT, PROJ_PRINT
    };

    static final int EDIT_COMMAND = PROJ_COMMAND + 100;
    static final int EDIT_NEWCLASS = EDIT_COMMAND;
    static final int EDIT_NEWPACKAGE = EDIT_NEWCLASS + 1;
    static final int EDIT_ADDCLASS = EDIT_NEWPACKAGE + 1;
    static final int EDIT_REMOVE = EDIT_ADDCLASS + 1;
    static final int EDIT_NEWUSES = EDIT_REMOVE + 1;
    static final int EDIT_NEWINHERITS = EDIT_NEWUSES + 1;
    static final int EDIT_REMOVEARROW = EDIT_NEWINHERITS + 1;


    static final String[] EditCmds = {
        "newClass", "newPackage", "addClass", "remove", "newUses", "newInherits",
        "removeArrow"
    };

    static final KeyStroke[] EditKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_N, SHORTCUT_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_R, SHORTCUT_MASK),
        null,
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
    static final int TOOLS_CALLLIBRARY = TOOLS_COMPILESELECTED + 1;
    static final int TOOLS_REBUILD = TOOLS_CALLLIBRARY + 1;
    static final int TOOLS_GENERATEDOC = TOOLS_REBUILD + 1;
    //static final int TOOLS_BROWSE = TOOLS_GENERATEDOC + 1;
    //static final int TOOLS_PREFERENCES = TOOLS_BROWSE + 1;
    static final int TOOLS_PREFERENCES = TOOLS_GENERATEDOC + 1;

    static final String[] ToolsCmds = {
        "compile", "compileSelected", "callLibrary", "rebuild", "generateDoc",
        // "browse",
        "preferences",
    };

    static final KeyStroke[] ToolsKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_K, SHORTCUT_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_K, Event.SHIFT_MASK | SHORTCUT_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_L, SHORTCUT_MASK),
        null,
        null,
        // KeyStroke.getKeyStroke(KeyEvent.VK_B, SHORTCUT_MASK),
        null
    };

    static final int[] ToolsSeparators = {
        TOOLS_REBUILD,
        TOOLS_GENERATEDOC,
    };

    static final int VIEW_COMMAND = TOOLS_COMMAND + 100;
    static final int VIEW_SHOWUSES = VIEW_COMMAND;
    static final int VIEW_SHOWINHERITS = VIEW_SHOWUSES + 1;
    static final int VIEW_SHOWCONTROLS = VIEW_SHOWINHERITS + 1;
    static final int VIEW_SHOWTERMINAL = VIEW_SHOWCONTROLS + 1;

    static final String[] ViewCmds = {
        "showUses", "showInherits", "showExecControls", "showTerminal",
    };

    static final KeyStroke[] ViewKeys = {
        KeyStroke.getKeyStroke(KeyEvent.VK_U, SHORTCUT_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_I, SHORTCUT_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_D, SHORTCUT_MASK),
        KeyStroke.getKeyStroke(KeyEvent.VK_T, SHORTCUT_MASK),
    };

    static final int[] ViewSeparators = {
        VIEW_SHOWINHERITS,
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
    static final int HELP_VERSIONCHECK = HELP_ABOUT + 1;
    static final int HELP_COPYRIGHT = HELP_VERSIONCHECK + 1;
    static final int HELP_WEBSITE = HELP_COPYRIGHT + 1;
    static final int HELP_TUTORIAL = HELP_WEBSITE + 1;
    static final int HELP_STANDARDAPI = HELP_TUTORIAL + 1;

    static final String[] HelpCmds = {
        "about", "versionCheck", "copyright", "website", "tutorial", "standardApi",
    };

    static final KeyStroke[] HelpKeys = {
        null,
        KeyStroke.getKeyStroke(KeyEvent.VK_V, SHORTCUT_MASK),
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
        GRP_COMMAND, HELP_COMMAND
    };

    static final String[] CmdTypeNames = {
        "package", "edit", "tools", "view", "group", "help"
    };

    static final String[][] CmdStrings = {
        ProjCmds, EditCmds, ToolsCmds, ViewCmds, GrpCmds, HelpCmds
    };

    static final KeyStroke[][] CmdKeys = {
        ProjKeys, EditKeys, ToolsKeys, ViewKeys, GrpKeys, HelpKeys
    };

    static final int[][] CmdSeparators = {
        ProjSeparators, EditSeparators, ToolsSeparators, ViewSeparators,
        GrpSeparators, HelpSeparators
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

        List dontDisable = Arrays.asList(new String [] {
            Config.getString("menu.package.new"),
            Config.getString("menu.package.open"),
            Config.getString("menu.package.openNonBlueJ"),
            Config.getString("menu.package.quit"),
            Config.getString("menu.tools.browse"),
            Config.getString("menu.tools.preferences"),
            Config.getString("menu.view.showExecControls"),
            Config.getString("menu.view.showTerminal"),
            Config.getString("menu.help.about"),
            Config.getString("menu.help.versionCheck"),
            Config.getString("menu.help.copyright"),
            Config.getString("menu.help.website"),
            Config.getString("menu.help.tutorial"),
            Config.getString("menu.help.standardApi"),
        });

        for (int i = 0; i < menus.length; i++) {
            if(menus[i] != null) {
                menu = (JMenu)menus[i];

                for (int j = 0; j < menu.getItemCount(); j++) {
                    JMenuItem item = menu.getItem(j);
                    if(item != null) {  // separators are returned as null
                        String label = item.getText();
                        if(! dontDisable.contains(label))
                            item.setEnabled(enable);
                    }
                }
            }
        }
    }
}
