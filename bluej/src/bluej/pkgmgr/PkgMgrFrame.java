package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.PageFormat;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.*;
import java.util.List;

import javax.swing.*;

import bluej.*;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.DebuggerThread;
import bluej.debugmgr.*;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.debugmgr.objectbench.*;
import bluej.debugmgr.texteval.*;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.MenuManager;
import bluej.graph.GraphElementController;
import bluej.parser.ClassParser;
import bluej.parser.symtab.ClassInfo;
import bluej.pkgmgr.actions.*;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.graphPainter.GraphPainterStdImpl;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.PackageTarget;
import bluej.pkgmgr.target.Target;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.testmgr.TestDisplayFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.testmgr.record.MethodInvokerRecord;
import bluej.utility.*;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationEvent;

/**
 * The main user interface frame which allows editing of packages
 *
 * @version $Id: PkgMgrFrame.java 2714 2004-07-01 15:55:03Z mik $
 */
public class PkgMgrFrame extends JFrame
    implements BlueJEventListener, MouseListener, PackageEditorListener
{
    public Font PkgMgrFont = PrefMgr.getStandardFont();

    public static final KeyStroke restartKey = KeyStroke.getKeyStroke(KeyEvent.VK_R, 
                                                InputEvent.SHIFT_MASK | InputEvent.CTRL_MASK);

    static final int DEFAULT_WIDTH = 420;
    static final int DEFAULT_HEIGHT = 300;

    private static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    private static Application macApplication = prepareMacOSApp();
    private static boolean testToolsShown = wantToSeeTestingTools();

    // instance fields:

    private JPanel buttonPanel;
    private JPanel testPanel;

    private JCheckBoxMenuItem showUsesMenuItem;
    private JCheckBoxMenuItem showExtendsMenuItem;

    private AbstractButton imgExtendsButton;
    private AbstractButton imgDependsButton;
    private AbstractButton runButton;
    
    private JLabel statusbar;
    
    private JLabel testStatusMessage;
    private JLabel recordingLabel;
    private AbstractButton endTestButton;
    private AbstractButton cancelTestButton;
    
    private ClassTarget testTarget = null;
    private String testTargetMethod;
    
    private JMenuBar menubar = null;
    private JMenu recentProjectsMenu;
    private JMenu toolsMenu;
    private JMenu testingMenu;
    private MenuManager menuManager;
    private JMenu viewMenu;

    private JMenuItem showTestResultsItem;
    private List itemsToDisable;
    private List actionsToDisable;
    private List testItems;
    private MachineIcon machineIcon;

    /* The scroller which holds the PackageEditor we use to edit packages */
    private JScrollPane classScroller = null;

    /* The package that this frame is working on or null for the case where
       there is no package currently being edited (check with isEmptyFrame()) */
    private Package pkg = null;

    /* The graph editor which works on the package or null for the case where
       there is no package current being edited (isEmptyFrame() == true) */
    private PackageEditor editor = null;

    private ObjectBench objbench;
    private TextEvalArea textEvaluator;
    private JComponent objectBenchPanel;
    private JSplitPane splitPane;
    private JSplitPane objectBenchSplitPane;
    private boolean showingTextEvaluator = false;
    
	// lazy initialised dialogs
    private LibraryCallDialog libraryCallDialog = null;
    // TODO: can be removed when 'Evaluate expression' function is removed.
    private FreeFormCallDialog freeFormCallDialog = null;
    private ProjectPrintDialog projectPrintDialog = null;

	// set PageFormat for default page for default printer
	// this variable is lazy initialised
	private static PageFormat pageFormat = null;


    // ============================================================
    // static methods to create and remove frames

    private static List frames = new ArrayList();  // of PkgMgrFrames

    private static ExtensionsManager extMgr = ExtensionsManager.get();

    /**
     * Prepare MacOS specific behaviour (About menu, Preferences menu, Quit menu)
     */
    private static Application prepareMacOSApp()
    {
        Application macApp = new Application();
        macApp.setEnabledPreferencesMenu(true);
        macApp.addApplicationListener(new com.apple.eawt.ApplicationAdapter() {
            public void handleAbout(ApplicationEvent e) {
                getMostRecent().aboutBlueJ();
                e.setHandled(true);
            }
            public void handlePreferences(ApplicationEvent e) {
                getMostRecent().showPreferences();
                e.setHandled(true);
            }
            public void handleQuit(ApplicationEvent e) {
                getMostRecent().wantToQuit();
            }
        });
        
        return macApp;
    }


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

        if(pmf == null) {
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
        PrefMgr.setFlag(PrefMgr.SHOW_TEXT_EVAL, frame.showingTextEvaluator);

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
     * Returns an array of all PkgMgrFrame objects.
     * It can be an empty array if none is found.
     */
    public static PkgMgrFrame[] getAllFrames()
    {
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
     * Gets the most recently used PkgMgrFrame
     * @return the PkgMgrFrame that currently has the focus
     */
    public static PkgMgrFrame getMostRecent()
    {
        PkgMgrFrame[] frames = getAllFrames();

        // If there are no frames open, yet...
        if ( frames.length < 1 ) return null;

        // Assume that the most recent is the first one. Not really the best thing to do...
        PkgMgrFrame mostRecent = frames[0];

        for (int i=0; i<frames.length; i++) 
            if (frames[i].getFocusOwner() != null) mostRecent = frames[i];

        return mostRecent;
    }


    /**
     * Check whether the status of the 'Show unit test tools' preference
     * has changed, and if it has, show or hide them as requested.
     */
    public static void updateTestingStatus()
    {
        if(testToolsShown != wantToSeeTestingTools()) {
            for(Iterator i = frames.iterator(); i.hasNext(); ) {
                ((PkgMgrFrame)i.next()).showTestingTools(!testToolsShown);
            }
            testToolsShown = !testToolsShown;
        }
    }

    
    /**
     * Tell whether unit testing tools should be shown.
     */
    private static boolean wantToSeeTestingTools()
    {
        return PrefMgr.getFlag(PrefMgr.SHOW_TEST_TOOLS);
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

	/**
	 * Display a short text message in the frames of the specified project.
	 */
	public static void displayMessage(Project sourceProj, String message)
	{
		PkgMgrFrame pmf[] = getAllProjectFrames(sourceProj);

		if (pmf != null) {
			for(int i=0; i<pmf.length; i++) {
				if(pmf[i] != null)
					pmf[i].setStatus(message);
			}
		}
	}

	/**
	 * Display an error message in a dialogue attached to the specified package frame.
	 */
    public static void showError(Package sourcePkg, String msgId)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if(pmf != null)
            DialogManager.showError(pmf, msgId);
    }

	/**
	 * Display a message in a dialogue attached to the specified package frame.
	 */
    public static void showMessage(Package sourcePkg, String msgId)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if(pmf != null)
            DialogManager.showMessage(pmf, msgId);
    }

	/**
	 * Display a parameterised message in a dialogue attached to the specified 
     * package frame.
	 */
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

        setupActionDisableSet();
        makeFrame();
        updateWindowTitle();
        setStatus(bluej.Boot.BLUEJ_VERSION_TITLE);
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
        editor.setGraphPainter(GraphPainterStdImpl.getInstance());
        editor.setGraphElementController(new GraphElementController(editor, pkg));
        editor.setFocusable(true);
        editor.addMouseListener(this);        // This listener MUST be before
        editor.addMouseListener(editor);      //  the editor itself!
        editor.addKeyListener(editor);
        this.pkg.editor = this.editor;

        classScroller.setViewportView(editor);
        editor.addPackageEditorListener(this);

        // fetch some properties from the package that interest us
        Properties p = pkg.getLastSavedProperties();

        try {
            String width_str = p.getProperty("package.editor.width",
                                             Integer.toString(DEFAULT_WIDTH));
            String height_str = p.getProperty("package.editor.height",
                                              Integer.toString(DEFAULT_HEIGHT));

            classScroller.setPreferredSize(new Dimension(Integer.parseInt(width_str),
                                                         Integer.parseInt(height_str)));

            String x_str = p.getProperty("package.editor.x", "30");
            String y_str = p.getProperty("package.editor.y", "30");

            int x = Integer.parseInt(x_str);
            int y = Integer.parseInt(y_str);

            if (x > (Config.screenBounds.width - 80))
                x = Config.screenBounds.width - 80;

            if (y > (Config.screenBounds.height - 80))
                y = Config.screenBounds.height - 80;

            setLocation(x,y);
        }
        catch (NumberFormatException e) { 
            Debug.reportError("Could not read preferred project screen position");
        }

        String uses_str = p.getProperty("package.showUses", "true");
        String extends_str = p.getProperty("package.showExtends", "true");

        showUsesMenuItem.setSelected(uses_str.equals("true"));
        showExtendsMenuItem.setSelected(extends_str.equals("true"));

        updateShowUsesInPackage();
        updateShowExtendsInPackage();

        pack();
        editor.revalidate();
        editor.requestFocus();

        // we have had trouble with BlueJ freezing when
        // the enable/disable GUI code was run off a menu
        // item. This code will delay the menu disable
        // until after menu processing has finished
        Runnable enableUI = new Runnable()
        {
            public void run()
            {
                    enableFunctions(true);  // changes menu items
                    updateWindowTitle();
                    show();
                }
            };

        SwingUtilities.invokeLater(enableUI);

        this.menuManager.setAttachedObject(pkg);
        this.menuManager.addExtensionMenu(pkg.getProject());
        
        extMgr.packageOpened (pkg);
    }

    /**
     * Closes the current package.
     */
    public void closePackage()
    {
        if(isEmptyFrame())
            return;

        extMgr.packageClosing (pkg);

        classScroller.setViewportView(null);

        editor.removePackageEditorListener(this);
        editor.removeMouseListener(this);
        editor.removeKeyListener(editor);

        getObjectBench().removeAllObjects(getProject().getUniqueId());

        getPackage().closeAllEditors();

        Project proj = getProject();

        editor = null;
        pkg = null;

        this.menuManager.setAttachedObject(pkg);
        
        // if there are no other frames editing this project, we close
        // the project
        if (PkgMgrFrame.getAllProjectFrames(proj) == null)
            Project.closeProject(proj);
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
        return pkg==null ? null : pkg.getProject();
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
        } else {
            String title = Config.getString("pkgmgr.title") + getProject().getProjectName();

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

    /**
     * Clear status bar of the frame
     */
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
         case PackageEditorEvent.TARGET_CALLABLE:
            // user has initiated method call or constructor
            callMethod(e.getCallable());
            break;

         case PackageEditorEvent.TARGET_REMOVE:
            // user has initiated target "remove" option
            ((Target) e.getSource()).remove();
            break;

         case PackageEditorEvent.TARGET_OPEN:
            // user has initiated a package open operation
            openPackageTarget(e.getName());
            break;

         case PackageEditorEvent.TARGET_RUN:
            // user has initiated a run operation
            ClassTarget ct = (ClassTarget) e.getSource();
            ct.getRole().run(this, ct, e.getName());
            break;

         case PackageEditorEvent.TARGET_BENCHTOFIXTURE:
            // put objects on object bench into fixtures
            objectBenchToTestFixture((ClassTarget) e.getSource());
            break;

         case PackageEditorEvent.TARGET_FIXTURETOBENCH:
            // put objects on object bench into fixtures
            testFixtureToObjectBench((ClassTarget) e.getSource());
            break;

        case PackageEditorEvent.TARGET_MAKETESTCASE:
            // start recording a new test case
            makeTestCase((ClassTarget) e.getSource());
            break;

         case PackageEditorEvent.OBJECT_PUTONBENCH:
             // "Get" object from object inspector
             DebuggerObject gotObj = e.getDebuggerObject();
             
             boolean tryAgain = true;
             do {
                String newObjectName = DialogManager.askString((Component) e.getSource(),
                        "getobject-new-name",
                        getProject().getDebugger().guessNewName(gotObj.getClassRef().getName()));

                if(newObjectName == null) {
                    tryAgain = false; // cancelled
                }
                else if(JavaNames.isIdentifier(newObjectName)) {
                    putObjectOnBench(newObjectName, e.getDebuggerObject(), e.getInvokerRecord());
                    tryAgain = false;
                }
                else {
                    DialogManager.showError((Component) e.getSource(), "must-be-identifier");
                }
            } while(tryAgain);
            break;
        }
    }

    // ============ end of PackageEditorListener interface ===============

    // --- below are implementations of particular user actions ---

    /**
     * Allow the user to select a directory into which we create
     * a project.
     */
    public boolean doNewProject()
    {
        String newname = FileUtility.getFileName(this,
                                                 Config.getString("pkgmgr.newPkg.title"), 
                                                 Config.getString("pkgmgr.newPkg.buttonLabel"),
                                                 false, null, true);

        if (newname == null)
            return false;

        if (Project.createNewProject(newname)) {
            Project proj = Project.openProject(newname);

            if(isEmptyFrame()) {
                openPackage(proj.getPackage(""));
            } else {
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
    public void doOpen()
    {
        File dirName = FileUtility.getPackageName(this);
        
        if (dirName != null) {
            openProject(dirName.getAbsolutePath());
        }
    }

    /**
     * Open the project specified by 'projectPath'.
     * Return false if not successful.
     */
    private boolean openProject(String projectPath)
    {
        Project openProj = Project.openProject(projectPath);

        if(openProj == null)
            return false;
        else {
            if (openProj.isReadOnly()) {
                DialogManager.showMessage(this, "project-is-readonly");
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

            return true;
        }
    }


    /**
     * Open a dialog that lets a user convert existing Java
     * source into a BlueJ project.
     *
     * The project selected is opened in a frame.
     */
    public void doOpenNonBlueJ()
    {
        File dirName = FileUtility.getNonBlueJDirectoryName(this);

        if(dirName == null)
            return;

        if (dirName != null) {
            File absDirName = dirName.getAbsoluteFile();

            if (Project.isProject(absDirName.getPath())) {
                DialogManager.showError(this, "open-non-bluej-already-bluej");
                return;
            }

            // find all sub directories with Java files in them
            // then find all the Java files in those directories
            List interestingDirs = Import.findInterestingDirectories(absDirName);

            if (interestingDirs.size() == 0) {
                DialogManager.showError(this, "open-non-bluej-no-java");
                return;
            }

            List javaFiles = Import.findJavaFiles(interestingDirs);

            // for each Java file, lets check its package line against the
            // package line we think that it should have
            // for each mismatch we collect the file, the package line it had,
            // and what we want to convert it to
            List mismatchFiles = new ArrayList();
            List mismatchPackagesOriginal = new ArrayList();
            List mismatchPackagesChanged = new ArrayList();

            Iterator it = javaFiles.iterator();

            while(it.hasNext()) {
                File f = (File) it.next();

                try {
                    ClassInfo info = ClassParser.parse(f);

                    String qf = JavaNames.convertFileToQualifiedName(absDirName, f);

                    if(!JavaNames.getPrefix(qf).equals(info.getPackage())) {
                        mismatchFiles.add(f);
                        mismatchPackagesOriginal.add(info.getPackage());
                        mismatchPackagesChanged.add(qf);
                    }
                }
                catch (Exception e) { }
            }

            // now ask if they want to continue if we have detected mismatches
            if (mismatchFiles.size() > 0) {
                ImportMismatchDialog imd = new ImportMismatchDialog(this,
                                                         mismatchFiles,
                                                         mismatchPackagesOriginal,
                                                         mismatchPackagesChanged);
                imd.show();

                if (!imd.getResult())
                    return;
            }

            // now add bluej.pkg files through the directory structure
            Import.convertDirectory(interestingDirs);

            // then construct it as a project
            Project openProj = Project.openProject(absDirName.getPath());

            // if after converting the directory, the project still doesn't open
            // then who knows what has gone wrong
            if (openProj == null) {
                return;
            }

            // now lets display the new project in a frame
            Package pkg = openProj.getPackage(openProj.getInitialPackageName());

            PkgMgrFrame pmf;

            if ((pmf = findFrame(pkg)) == null) {
                if(isEmptyFrame()) {
                    pmf = this;
                    openPackage(pkg);
                } else {
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

        // If only one frame and this was from the menu
        // "close", close should close existing package rather
        // than remove frame

        if(frameCount() == 1) {
            if(keepLastFrame) {        // close package, leave frame
                closePackage();
                updateWindowTitle();
                updateRecentProjects();

                // we have had trouble with BlueJ freezing when
                // the enable/disable GUI code was run off a menu
                // item. This code will delay the menu disable
                // until after menu processing has finished
                Runnable disableUI = new Runnable()
                {
                    public void run()
                    {
                        enableFunctions(false);  // changes menu items
                        updateWindowTitle();
                    }
                };
                SwingUtilities.invokeLater(disableUI);
            }
            else {                      // all frames gone, lets quit
                doQuit();
            }
        } else {
            closePackage();                          // remove package and frame
            PkgMgrFrame.closeFrame(this);
        }
    }


    /**
     * Quit menu item was chosen.
     */
    public void wantToQuit()
    {
        int answer = 0;
        if(Project.getOpenProjectCount() > 1)
            answer = DialogManager.askQuestion(this, "quit-all");
        if(answer == 0)
            doQuit();
    }


    /**
     * perform the closing down and quitting of BlueJ.
     * Note that the order of the events is relevant 
     * - Extensions should be unloaded after package close
     */
    private void doQuit()
    {
        PkgMgrFrame[] pkgFrames = getAllFrames();

        // handle open packages so they are re-opened on startup
        handleOrphanPackages(pkgFrames);
        
        // We replicate some of the behaviour of doClose() here
        // rather than call it to avoid a nasty recursion
        for(int i = pkgFrames.length - 1; i >= 0; i--) {
            PkgMgrFrame aFrame = pkgFrames[i];
            aFrame.doSave();
            aFrame.closePackage();
            PkgMgrFrame.closeFrame(aFrame);
        }

        extMgr.unloadExtensions();
        bluej.Main.exit();
    }

    
    /**
     * When bluej is exited with open packages we want it to
     * open these the next time that is started (this is default 
     * action, can be changed by setting
     * @param openFrames
     */
    private void handleOrphanPackages(PkgMgrFrame[] openFrames)
    {
        // if there was a previous list, delete it
        if(hadOrphanPackages())
             removeOrphanPackageList();
        // add an entry for each open package     
        for(int i = 0; i < openFrames.length ; i++) {
            PkgMgrFrame aFrame = openFrames[i];
            if(!aFrame.isEmptyFrame()) {
                Config.putPropString(Config.BLUEJ_OPENPACKAGE + (i + 1), 
                    aFrame.getPackage().getPath().toString()); 
            }       
        }
    }
    

    /**
     * Checks if there were oprphan packages on last exit by looking for
     * existence of the value saved for the first orphaned package.
     * @return whether a value could be found indicating orphan package(s)
     */
    public static boolean hadOrphanPackages()
    {
        // check if bluej.openPackage1 exists
        return (Config.getPropString(Config.BLUEJ_OPENPACKAGE + 1, null) != null);
    }
    

    /**
     * removes previously listed orphan packages from bluej properties
     */
    private void removeOrphanPackageList()
    {
        String exists = "";
        for(int i = 1; exists != null; i++) {
            exists = Config.removeProperty(Config.BLUEJ_OPENPACKAGE + i);
        }
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

        p.put("package.showUses", new Boolean(isShowUses()).toString());
        p.put("package.showExtends", new Boolean(isShowExtends()).toString());

        pkg.save(p);

        setStatus(Config.getString("pkgmgr.packageSaved"));
    }

    /**
     * Import into a new project or import into the current project.
     */
    public void doImport()
    {
        // prompt for the directory to import from
        File importDir;
        String importName = FileUtility.getFileName(this,
                             Config.getString("pkgmgr.importPkg.title"),
                             Config.getString("pkgmgr.importPkg.buttonLabel"),
                             true, null, false);

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
        List dirsToConvert = Import.findInterestingDirectories(getPackage().getPath());
        Import.convertDirectory(dirsToConvert);

        // reload all the packages (which discovers classes which may have
        // been added by the import)
        getProject().reloadAll();
    }

    /**
     * Implementation of the "Add Class from File" user function
     */
    public void doAddFromFile()
    {
        // file dialog that shows .java and .class files
        String className = FileUtility.getFileName(this,
                                                   Config.getString("pkgmgr.addClass.title"),
                                                   Config.getString("pkgmgr.addClass.buttonLabel"),
                                                   false,
                                                   FileUtility.getJavaSourceFilter(),
                                                   false);

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
    public void doExport()
    {
        ExportManager exporter = new ExportManager(this);
        exporter.export();
    }

    /**
     * Creates a page setup dialog to alter page dimensions.
     *
     */
    public void doPageSetup()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        setPageFormat(job.validatePage(job.pageDialog(getPageFormat())));
    }
    
    /**
     * accessor method for PageFormat object that can be used by various 
     * printing subsystems eg. source code printing from editor
     * @return common PageFormat object representing page preferences 
     */
    public static PageFormat getPageFormat()
    {
		if (pageFormat == null)
			pageFormat = PrinterJob.getPrinterJob().defaultPage();

		return pageFormat;   
    }
    
    /**
     * set method for printing PageFormat.  Called by other elements 
     * that may manipulate pageformat, at this stage the source editor
     * is the only component that does.  The assumption is that the 
     * PageFormat should be uniform between all components that may 
     * want to send output to a printer.  
     * @param page the new PageFormat
     */
    public static void setPageFormat(PageFormat page)
    {
        pageFormat = page;   
    }

    /**
     * Implementation of the "print" user function
     */
    public void doPrint() 
    {
        if (projectPrintDialog == null)
            projectPrintDialog = new ProjectPrintDialog(this);

        if (projectPrintDialog.display()) {
            PackagePrintManager printManager = new PackagePrintManager(this.getPackage(), getPageFormat(), projectPrintDialog);
            printManager.start();
        }  
    }

    /**
     * Preferences menu was chosen.
     */
    public void showPreferences()
    {
        PrefMgrDialog.showDialog();
    }

    /**
     * About menu was chosen.
     */
    public void aboutBlueJ()
    {
        AboutBlue about = new AboutBlue(this, bluej.Boot.BLUEJ_VERSION);
        about.setVisible(true);
    }

    /**
     * Copyright menu item was chosen.
     */
    public void showCopyright()
    {
        JOptionPane.showMessageDialog(this,
              new String[] {
                  "BlueJ \u00a9 2000-2003 Michael K\u00F6lling, John Rosenberg.",
                  " ",
                  Config.getString("menu.help.copyright.line1"),
                  Config.getString("menu.help.copyright.line2"),
                  Config.getString("menu.help.copyright.line3"),
                  Config.getString("menu.help.copyright.line4"),
              },
              Config.getString("menu.help.copyright.title"),
              JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Interactively call a class (ie static) method or a class constructor
     */
    private void callMethod(final CallableView cv)
    {
        ResultWatcher watcher = null;

        if(cv instanceof ConstructorView) {
            // if we are constructing an object, create a watcher that waits for
            // completion of the call and then places the object on the object
            // bench
            watcher = new ResultWatcher() {
                public void putResult(DebuggerObject result, String name, InvokerRecord ir)
                {
                    // this shouldn't ever happen!! (ajp 5/12/02)
                    if((name == null) || (name.length() == 0))
                        name = "result";
                        
                    if(result != null) {
                        //BeanShell does not need to use the realresult, but can use result directly
                        //DebuggerObject realResult = result.getInstanceFieldObject(0);

                        ObjectWrapper wrapper = ObjectWrapper.getWrapper(
                                                       PkgMgrFrame.this, getObjectBench(),
                                                       result,
                                                       name);
                        getObjectBench().addObject(wrapper);

                        getPackage().getDebugger().addObject(wrapper.getName(), result);
                                                
                        getObjectBench().addInteraction(ir);
                    }
                    else {
                    	// we can get here if the machine is terminated mid way through
                    	// a construction. If so, lets do nothing
                    }
                }
                public void putError(String msg) { }
                /**
                 * We have no use of this information when using the constructor
                 */
                public ExpressionInformation getExpressionInformation() {                    
                    return null;
                } 
            };
        }
        else if(cv instanceof MethodView) {
            final MethodView mv = (MethodView) cv;

            // if we are calling a main method then we want to simulate a
            // new launch of an application, so first of all we unload all our
            // classes (prevents problems with static variables not being
            // reinitialised because the class hangs around from a previous call)
            if(mv.isMain()) {
                getProject().removeLocalClassLoader();
                getProject().newRemoteClassLoaderLeavingBreakpoints();
            }

            // create a watcher
            // that waits for completion of the call and then displays the
            // result (or does nothing if void)
            watcher = new ResultWatcher() {
                private ExpressionInformation expressionInformation = new ExpressionInformation(mv,getName());
                
                public void putResult(DebuggerObject result, String name, InvokerRecord ir)
                {
                    getObjectBench().addInteraction(ir);
                    
                    // a void result returns a name of null
                    if (name == null)
                        return;
                    
                    //The result can be null when terminating the program while
                    // at a breakpoint in a method that has a return value.
                    if (result == null)
                        return;
                        
                    ResultInspector viewer =
                        ResultInspector.getInstance(result, name, getPackage(), ir, expressionInformation, PkgMgrFrame.this);
                    BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, viewer.getResult());
                }
                public void putError(String msg) { }
                public ExpressionInformation getExpressionInformation() {
                    return expressionInformation;
                } 
            };
        }

        // create an Invoker to handle the actual invocation

        new Invoker(this, cv, (String)null, watcher).invokeInteractive();
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
     * Create the text fixture method in the indicated target on from
     * the current objects on the object bench.
     */
    private void objectBenchToTestFixture(ClassTarget target)
    {
        if (target.getRole() instanceof UnitTestClassRole) {
            UnitTestClassRole utcr = (UnitTestClassRole) target.getRole();

            utcr.doBenchToFixture(this, target);
        }
    }

    /**
     * Build the text fixture specified in the indicated target on the object
     * bench.
     */
    private void testFixtureToObjectBench(ClassTarget target)
    {
        if (target.getRole() instanceof UnitTestClassRole) {
            UnitTestClassRole utcr = (UnitTestClassRole) target.getRole();
            utcr.doFixtureToBench(this, target);
        }
    }
    
    /**
     * Create a test method for the indicated target.
     */
    private void makeTestCase(ClassTarget target)
    {
        if (target.getRole() instanceof UnitTestClassRole) {
            UnitTestClassRole utcr = (UnitTestClassRole) target.getRole();
            if(! testToolsShown)
                showTestingTools(true);
            utcr.doMakeTestCase(this, target);
        }
    }
    
    public void putObjectOnBench(String newInstanceName, DebuggerObject object, InvokerRecord ir)
    {
        if (!object.isNullObject()) {
            ObjectWrapper wrapper = ObjectWrapper.getWrapper(this, getObjectBench(), object, newInstanceName);
            getObjectBench().addObject(wrapper);  // might change name

            // load the object into runtime scope
            getPackage().getDebugger().addObject(wrapper.getName(), object);
                                                
            if (ir instanceof MethodInvokerRecord) {
            	MethodInvokerRecord mir = (MethodInvokerRecord) ir;
            	
            	mir.setBenchName(newInstanceName, wrapper.getObject().getClassName());
            }
        }
    }

    /**
     * Implementation of the "New Class" user function.
     */
    public void doCreateNewClass()
    {
        NewClassDialog dlg = new NewClassDialog(this);
        boolean okay = dlg.display();

        if(okay) {
            String name = dlg.getClassName();
            String template = dlg.getTemplateName();

            createNewClass(name, template);
        }        
    }

    /**
     * Creates a new class using the given name and template
     * @param name is not a fully qualified class name
     * @param template can be null in this case no template will be generated
     */
    public void createNewClass(String name, String template)
    {
        if (name.length() > 0) {
            // check whether name is already used
            if(pkg.getTarget(name) != null) {
                DialogManager.showError(this, "duplicate-name");
                return;
            }

            ClassTarget target =  null;
            target = new ClassTarget(pkg, name, template);

            target.generateSkeleton(template);

            pkg.findSpaceForVertex(target);
            pkg.addTarget(target);

            editor.revalidate();
            editor.scrollRectToVisible(target.getRectangle());
            editor.repaint();
            
            if (target.getRole() instanceof UnitTestClassRole)
            	pkg.compileQuiet(target);
        }
    }

    /**
     * Prompts the user with a dialog asking for the name of
     * a package to create. Package name can be fully qualified
     * and all intermediate packages will also be created.
     */
    public void createNewPackage()
    {
        NewPackageDialog dlg = new NewPackageDialog(this);
        boolean okay = dlg.display();

        if(!okay)
            return;

        String name = dlg.getPackageName();

        if (name.length() == 0)
            return;

        String fullName;

        // if the name is fully qualified then we leave it as is but
        // if it is not we assume they want to create a package in the
        // current package
        if (name.indexOf('.') > -1)
            fullName = name;
        else
            fullName = getPackage().getQualifiedName(name);

        // check whether name is already used as an existing package
        if (getProject().getPackage(fullName) != null) {
            DialogManager.showError(this, "duplicate-name");
            return;
        }

		// check whether name is already used for a class in the
		// parent package
		String prefix = JavaNames.getPrefix(fullName);
		String base = JavaNames.getBase(fullName);
		
		Package basePkg = getProject().getPackage(prefix);
		if (basePkg != null) {
			if (basePkg.getTarget(base) != null) {
				DialogManager.showError(this, "duplicate-name");
				return;
			}
		}

		getProject().createPackageDirectory (fullName);
    
        // check that everything has gone well and instruct all affected
        // packages to reload (to make them notice the new sub packages)
        Package newPackage = getProject().getPackage(fullName);

        if (newPackage == null) {
            Debug.reportError("creation of new package failed unexpectedly");
        }

        while(newPackage != null) {
            newPackage.reload();
            newPackage = newPackage.getParent();
        }
    }

    /**
     * Remove the selected targets. Ask before deletion. If nothing is selected
     * display an errormessage.
     */
    public void doRemove(){
        if (! (doRemoveTargets() || doRemoveDependency() )) {
            DialogManager.showError(this, "no-class-selected");
        }
    }
    
    private boolean doRemoveTargets() {
        Target[] targets = pkg.getSelectedTargets();
        if (targets.length <= 0){
            return false;
        }
        if( askRemoveClass() ){
            for(int i=0; i<targets.length; i++){
                targets[i].remove();
            }
        }
        return true;
    }
    
    private boolean doRemoveDependency() {
        Dependency dependency = pkg.getSelectedDependency();
        if (dependency == null){
            return false;
        }
        dependency.remove();
        return true;   
    }


    /**
     * The user function to add a uses arrow to the diagram was invoked.
     */
    public void doNewUses()
    {
        pkg.setState(Package.S_CHOOSE_USES_FROM);
        setStatus(Config.getString("pkgmgr.chooseUsesFrom"));
        pkg.getEditor().clearSelection();
    }

    /**
     * The user function to add an inherits arrow to the dagram was invoked.
     */
    public void doNewInherits()
    {
        pkg.setState(Package.S_CHOOSE_EXT_FROM);
        setStatus(Config.getString("pkgmgr.chooseInhFrom"));
        pkg.getEditor().clearSelection();
    }

    /**
     * The user function to remove an arrow from the dagram was invoked.
     *
    public void doRemoveArrow()
    {
        pkg.setState(Package.S_DELARROW);
        setStatus(Config.getString("pkgmgr.chooseArrow"));
    }
    */

	/**
	 * The user function to test all classes in a package
	 */
	public void doTest()
	{
		runButton.setEnabled(false);
		
		List l = pkg.getTestTargets();

		final Iterator it = l.iterator();
				
		Thread thr = new Thread() {
			public void run() {
				TestDisplayFrame.getTestDisplay().startMultipleTests();

				while(it.hasNext()) {
					ClassTarget ct = (ClassTarget) it.next();
					if (ct.isCompiled() && ct.isUnitTest()) {
						UnitTestClassRole utcr = (UnitTestClassRole) ct.getRole();
	
						utcr.doRunTest(PkgMgrFrame.this, ct, null);
					}
				}
	
				TestDisplayFrame.getTestDisplay().endMultipleTests();

				EventQueue.invokeLater(new Runnable ()  {
					public void run ()
					{
						runButton.setEnabled(true);
					}
				});
			}
		};		

		thr.start();
	}

    /**
     * The 'end test recording' button was clicked - end the recording.
     */
    public void doEndTest()
    {
        testRecordingEnded();
        
        if (testTarget.getRole() instanceof UnitTestClassRole) {
            UnitTestClassRole utcr = (UnitTestClassRole) testTarget.getRole();

            utcr.doEndMakeTestCase(this, testTarget, testTargetMethod);
        }            

		// remove objects from object bench
		getProject().removeLocalClassLoader();
		getProject().newRemoteClassLoader();

		// try to compile the test class we have just changed
		getPackage().compileQuiet(testTarget);	

		testTarget = null;
    }
    
    /**
     * The 'cancel test recording' button was clicked - cancel the recording.
     */
    public void doCancelTest()
    {
        testRecordingEnded();
        
        // remove objects from object bench (may have been put there
        // when testing was started)
		getProject().removeLocalClassLoader();
		getProject().newRemoteClassLoader();
		
		testTarget = null;
    }

    /**
     * Recording of a test case started - set the interface appropriately.
     */
    public void testRecordingStarted(String message)
    {
        recordingLabel.setEnabled(true);
        testStatusMessage.setText(message);
        endTestButton.setEnabled(true);
        cancelTestButton.setEnabled(true);

        getProject().setTestMode(true);
    }

    /**
     * Recording of a test case ended - set the interface appropriately.
     */
    private void testRecordingEnded()
    {
        recordingLabel.setEnabled(false);
        testStatusMessage.setText("");
        endTestButton.setEnabled(false);
        cancelTestButton.setEnabled(false);

        getProject().setTestMode(false);
    }

    public void setTestInfo(String testName, ClassTarget testClass)
    {
        this.testTargetMethod = testName;
        this.testTarget = testClass;
    }
    
    /**
     * Ask the user to confirm removal of package.
     * @return zero if the user confirms removal.
     */
    public boolean askRemoveClass()
    {
        int response = DialogManager.askQuestion(this, "really-remove-class");
        return response == 0;
    }


    /**
     * Ask the system and the user to confirm removal of package. The system
     * prevent the package from being removed if frames holding classes in the
     * package is open. 
     * @param removableTarget
     * @return true if the package should be removed.
     */
    public boolean askRemovePackage(PackageTarget removableTarget)
    {
        String name = removableTarget.getQualifiedName();
        PkgMgrFrame[] f = getAllProjectFrames(getProject(), name);

        if (f != null) {
            DialogManager.showError(this, "remove-package-open");
            return false;
        }

        // Check they realise that this will delete ALL the files.
        int response = DialogManager.askQuestion(this, "really-remove-package");

        // if they agree
        return response == 0;
    }


    /**
     * Compile the currently selected class targets.
     */
    public void compileSelected()
    {
        Target[] targets = pkg.getSelectedTargets();
        if (targets.length > 0){   
            for(int i=0; i<targets.length; i++){
                if(targets[i] instanceof ClassTarget) {
                    ClassTarget t = (ClassTarget) targets[i];

                    if (t.hasSourceCode())
                        pkg.compile(t);
                }
            }
        }
        else {
            DialogManager.showError(this, "no-class-selected-compile");
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
     * User function "Free Form Call...". Pop up the dialog that allows users to
     * make that call.
     */
    public void callFreeForm() {
        if (freeFormCallDialog == null) {
            freeFormCallDialog = new FreeFormCallDialog(this);
        }
        ResultWatcher watcher = new ResultWatcher() {
            FreeFormCallDialog freeFormCallDialog = PkgMgrFrame.this.freeFormCallDialog;

            public void putResult(DebuggerObject result, String name, InvokerRecord ir) {
                getObjectBench().addInteraction(ir);
                if (result != null) {
                    ResultInspector viewer = ResultInspector.getInstance(result, name,
                            getPackage(), ir, getExpressionInformation(), PkgMgrFrame.this);
                    BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, viewer.getResult());
                } else {
                    BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, null);
                }
            }

            public void putError(String msg) {
            }

            public ExpressionInformation getExpressionInformation() {
                String expression = freeFormCallDialog.getExpression();
                return new ExpressionInformation(expression);
            }
        };
        new Invoker(this, freeFormCallDialog, watcher);
    }

    /**
     * User function "Generate Documentation...".
     */
    public void generateProjectDocumentation()
    {
        String message = pkg.generateDocumentation();
        if (message!="")
            DialogManager.showText(this,message);
    }

    /**
     * Show the debugger controls for the VM associated with this project.
     */
	public void showDebugger()
	{
		if (!isEmptyFrame())
            getProject().getExecControls().showHide(true);
	}
	
    /**
     * Restart the debugger VM associated with this project.
     */
	public void restartDebugger()
	{
		if (!isEmptyFrame())
			getProject().restartVM();
	}
	
    /**
     * Toggle the state of the "show uses arrows" switch.
     */
    public void updateShowUsesInPackage()
    {
        pkg.setShowUses(isShowUses());
        editor.repaint();
    }

    public void updateShowExtendsInPackage()
    {
        pkg.setShowExtends(isShowExtends());
        editor.repaint();
    }

    public boolean isShowUses()
    {
        return showUsesMenuItem.isSelected();
    }

    public boolean isShowExtends()
    {
        return showExtendsMenuItem.isSelected();       
    }
    
    /**
     * Show or hide the testing tools.
     */
    public void showTestingTools(boolean show)
    {
        for (Iterator it = testItems.iterator(); it.hasNext(); ) {
            JComponent component = (JComponent)it.next();
            component.setVisible(show);
        }
    }
    
    /** 
     * Tell whether we are currently showing the text evaluation pane.
     * @return  true if the text eval pane is visible.
     */
    public boolean isTextEvalVisible()
    {
        return showingTextEvaluator;
    }
    
    /**
     * Show or hide the text evaluation component.
     */
    public void showHideTextEval(boolean show)
    {
        if(showingTextEvaluator == show)    // already showing the right thing?
            return;

        if(show) {
            addTextEvaluatorPane();
            textEvaluator.requestFocus();
        }
        else {
            removeTextEvaluatorPane();
        }
        pack();
        showingTextEvaluator = show;
    }

    /**
     *  A BlueJEvent was raised. Check whether it is one that we're interested
     *  in.
     */
    public void blueJEvent(int eventId, Object arg)
    {
        DebuggerThread thread;

        switch(eventId) {
        case BlueJEvent.CREATE_VM:
            setStatus(Config.getString("pkgmgr.creatingVM"));
            break;
        case BlueJEvent.CREATE_VM_DONE:
            // TODO: This seems never to be generated (a DebuggerEvent is not used instead) fix! 
            setStatus(Config.getString("pkgmgr.creatingVMDone"));
            break;
        case BlueJEvent.GENERATING_DOCU:
            setStatus(Config.getString("pkgmgr.generatingDocu"));
            break;
        case BlueJEvent.DOCU_GENERATED:
            setStatus(Config.getString("pkgmgr.docuGenerated"));
            break;
        case BlueJEvent.DOCU_ABORTED:
            setStatus(Config.getString("pkgmgr.docuAborted"));
            break;
        case BlueJEvent.CREATE_VM_FAILED:
            DialogManager.showError(this,"error-create-vm");
            doQuit();
            break;
        }
    }

    // ---- end of BlueJEventListener interface ----

    /**
     * The debugger state has changed. Indicate the state in our interface
     * and change the system state accordingly (e.g. enable/disable terminal).
     *
     * NOTE: The current implementation assumes that user VMs DO NOT run concurrently!
     */
    public void setDebuggerState(int state)
    {
		switch (state) {
		 case Debugger.NOTREADY:
		 	break;

		 case Debugger.IDLE:
		 	machineIcon.setIdle();
            getProject().getTerminal().activate(false);
        	break;
		 
		 case Debugger.RUNNING:
            machineIcon.setRunning();
            getProject().getTerminal().activate(true);
		 	break;
	 
		 case Debugger.SUSPENDED:
		 	machineIcon.setStopped();
            break;
		}
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
    public void showWebPage(String url)
    {
        if (Utility.openWebBrowser(url))
            setStatus(Config.getString("pkgmgr.webBrowserMsg"));
        else
            setStatus(Config.getString("pkgmgr.webBrowserError"));
    }

    // --- the following methods set up the GUI frame ---

    private void makeFrame()
    {
        setFont(PkgMgrFont);
        setIconImage(BlueJTheme.getIconImage());
        testItems = new ArrayList();
        
        setupMenus();

        Container contentPane = getContentPane();
        ((JPanel)contentPane).setBorder(BlueJTheme.generalBorderWithStatusBar);
        
        // create the main panel holding the diagram and toolbar on the left
        
        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

		// Install keystroke to restart the VM
        Action action = RestartVMAction.getInstance(); 
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                            (KeyStroke)action.getValue(Action.ACCELERATOR_KEY), "restartVM");
		mainPanel.getActionMap().put("restartVM", action);
		

        // create the left hand side toolbar
        JPanel toolPanel = new JPanel();
        {
            buttonPanel = new JPanel();
            {
                buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
                buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
    
                ImageIcon emptyIcon = Config.getImageAsIcon("image.empty");
    
                action = NewClassAction.getInstance(); 
                AbstractButton button = createButton(action, false, false, 4, 4);
                buttonPanel.add(button);
                buttonPanel.add(Box.createVerticalStrut(3));
        
                action = NewUsesAction.getInstance(); 
                imgDependsButton = createButton(action, true, false, 4, 4);
                buttonPanel.add(imgDependsButton);
                buttonPanel.add(Box.createVerticalStrut(3));
    
                action = NewInheritsAction.getInstance(); 
                imgExtendsButton = createButton(action, true, false, 4, 4);
                buttonPanel.add(imgExtendsButton);
                buttonPanel.add(Box.createVerticalStrut(3));
    
                action = CompileAction.getInstance(); 
                button = createButton(action, false, false, 4, 4);
                buttonPanel.add(button);
				buttonPanel.add(Box.createVerticalStrut(3));

                buttonPanel.setAlignmentX(0.5f);
            }

            testPanel = new JPanel();
            {
                testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.Y_AXIS));

                testPanel.setBorder(BorderFactory.createEmptyBorder(5,5,14,5));

				action = RunTestsAction.getInstance(); 
                runButton = createButton(action, false, false, 2, 4);
                runButton.setText(Config.getString("pkgmgr.test.run"));
                runButton.setAlignmentX(0.15f);
                testPanel.add(runButton);
                testPanel.add(Box.createVerticalStrut(8));

                recordingLabel = new JLabel(Config.getString("pkgmgr.test.record"),
                                                Config.getImageAsIcon("image.test.recording"),
                                                SwingConstants.LEADING);
                recordingLabel.setFont(PkgMgrFont);
                recordingLabel.setEnabled(false);
                recordingLabel.setAlignmentX(0.15f);
                testPanel.add(recordingLabel);
                testPanel.add(Box.createVerticalStrut(3));

                action = EndTestRecordAction.getInstance(); 
                endTestButton = createButton(action, false, false, 2, 4);
                //make the button use a different label than the one from action
                endTestButton.setText(Config.getString("pkgmgr.test.end"));
                endTestButton.setEnabled(false);

                testPanel.add(endTestButton);
                testPanel.add(Box.createVerticalStrut(3));

                action = CancelTestRecordAction.getInstance();
                cancelTestButton = createButton(action, false, false, 2, 4);
                //make the button use a different label than the one from action
                cancelTestButton.setText(Config.getString("cancel"));
                cancelTestButton.setEnabled(false);

                testPanel.add(cancelTestButton);
                
                testPanel.setAlignmentX(0.5f);
            }
            testItems.add(testPanel);

            machineIcon = new MachineIcon(this);
            machineIcon.setAlignmentX(0.5f);
            itemsToDisable.add(machineIcon);

            toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
            toolPanel.add(buttonPanel);
            toolPanel.add(Box.createVerticalGlue());
            toolPanel.add(testPanel);
            toolPanel.add(machineIcon);
        }
        mainPanel.add(toolPanel, BorderLayout.WEST);
        classScroller = new JScrollPane();
        mainPanel.add(classScroller, BorderLayout.CENTER);
        classScroller.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));


        // create the object bench
        
        objbench = new ObjectBench();
        objectBenchPanel = objbench;

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                                   mainPanel, objectBenchPanel);
        splitPane.setBorder(null);
        contentPane.add(splitPane, BorderLayout.CENTER);
        
        
        // create the bottom status area
        
        JPanel statusArea = new JPanel(new BorderLayout());
        {
            statusArea.setBorder(BorderFactory.createEmptyBorder(2,0,4,6));

            statusbar = new JLabel(" ");
            statusArea.add(statusbar, BorderLayout.CENTER);

            testStatusMessage = new JLabel(" ");
            statusArea.add(testStatusMessage, BorderLayout.WEST);
        }
        contentPane.add(statusArea, BorderLayout.SOUTH);
        
        // hide testing tools if not wanted
        if(! testToolsShown)
            showTestingTools(false);

        // show the text evaluation pane if needed
        if(PrefMgr.getFlag(PrefMgr.SHOW_TEXT_EVAL)) {
            addTextEvaluatorPane();
        }

        pack();

        addWindowListener(new WindowAdapter()
        {
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

    private void addTextEvaluatorPane()
    {
        if(textEvaluator == null) {
            textEvaluator = new TextEvalArea(this, PkgMgrFont);
            objectBenchSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                                                  objectBenchPanel, textEvaluator);
        }
        else {
            objectBenchSplitPane.setLeftComponent(objectBenchPanel);
        }
        splitPane.setBottomComponent(objectBenchSplitPane);
        showingTextEvaluator = true;
    }

    private void removeTextEvaluatorPane()
    {
        splitPane.setBottomComponent(objectBenchPanel);
        showingTextEvaluator = false;
    }
    
    /**
     * Create a button for the interface.
     * @param action  the Action abstraction dictating text, icon, tooltip, action.
     * @param notext  set true if the action text should not appear (icon only).
     * @param toggle  true if this is a toggle button, false otherwise
     * @param hSpacing  horizontal margin (left, right) 
     * @param vSpacing  vertical margin (top, bottom)
     * @return  the new button
     */
    private AbstractButton createButton(Action action, boolean notext, boolean toggle, 
                                        int hSpacing, int vSpacing)
    {
        AbstractButton button;
        if(toggle) {
            button = new JToggleButton(action);
        }
        else {
            button = new JButton(action);
        }
        button.setFont(PkgMgrFont);
        button.putClientProperty("JButton.buttonType", "toolbar");  // "icon"
        //button.setRequestFocusEnabled(false);// hint the laf that mouse click shouldn't request focus
        button.setFocusable(false); //bottons shouldn't get focus
        
        if (notext)
            button.setText(null);

        Dimension pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
        button.setMargin(new Insets(vSpacing, hSpacing, vSpacing, hSpacing));

        return button;
    }
    

    /**
     * setupMenus - Create the menu bar
     */
    private void setupMenus()
    {
        menubar = new JMenuBar();
        JMenu menu;
        final PkgMgrFrame frame = this;
        itemsToDisable = new ArrayList();
        
        menu = new JMenu(Config.getString("menu.package"));
        int mnemonic = Config.getMnemonicKey("menu.package");
        menu.setMnemonic(mnemonic);
        menubar.add(menu);
        {
            createMenuItem(NewProjectAction.getInstance(),menu);
            createMenuItem(OpenProjectAction.getInstance(),menu);
            recentProjectsMenu = new JMenu(Config.getString("menu.package.openRecent"));
            menu.add(recentProjectsMenu);
            createMenuItem(OpenNonBlueJAction.getInstance(),menu);
            createMenuItem(CloseProjectAction.getInstance(),menu);
            createMenuItem(SaveProjectAction.getInstance(),menu);
            createMenuItem(SaveProjectAsAction.getInstance(),menu);
            menu.addSeparator();

            createMenuItem(ImportProjectAction.getInstance(),menu);
            createMenuItem(ExportProjectAction.getInstance(),menu);
            menu.addSeparator();

            createMenuItem(PageSetupAction.getInstance(),menu);
            createMenuItem(PrintAction.getInstance(), menu);
            
            if(!Config.usingMacScreenMenubar()) {   // no "Quit" here for Mac
                menu.addSeparator();
                createMenuItem(QuitAction.getInstance(), menu);
            }
        }


        menu = new JMenu(Config.getString("menu.edit"));
        menu.setMnemonic(Config.getMnemonicKey("menu.edit"));
        menubar.add(menu);
        {
            createMenuItem(NewClassAction.getInstance(), menu);
            createMenuItem(NewPackageAction.getInstance(), menu);
            createMenuItem(AddClassAction.getInstance(), menu);
            createMenuItem(RemoveAction.getInstance(), menu);
            menu.addSeparator();

            createMenuItem(NewUsesAction.getInstance(), menu);
            createMenuItem(NewInheritsAction.getInstance(), menu);
        }


        menu = new JMenu(Config.getString("menu.tools"));
        toolsMenu = menu;
        menu.setMnemonic(Config.getMnemonicKey("menu.tools"));
        menubar.add(menu);
        {
            createMenuItem(CompileAction.getInstance(), menu);
            createMenuItem(CompileSelectedAction.getInstance(), menu);
            createMenuItem(RebuildAction.getInstance(), menu);
            menu.addSeparator();

            createMenuItem(UseLibraryAction.getInstance(), menu);
            createMenuItem(EvalExpressionAction.getInstance(), menu);
            menu.addSeparator();

            createMenuItem(GenerateDocsAction.getInstance(), menu);
            testingMenu = new JMenu(Config.getString("menu.tools.testing"));
            testingMenu.setMnemonic(Config.getMnemonicKey("menu.tools"));
            {
            	createMenuItem(RunTestsAction.getInstance(), testingMenu);
            	createMenuItem(EndTestRecordAction.getInstance(), testingMenu);
            	createMenuItem(CancelTestRecordAction.getInstance(), testingMenu);
            }
            testItems.add(testingMenu);
            menu.add(testingMenu);
            menu.addSeparator();

            createMenuItem(PreferencesAction.getInstance(), menu);

            // This will attache the general handling of extension menu
            menuManager = new MenuManager(menu.getPopupMenu());

            // If this is the first frame I have to attach the extension menu, no project openend here.
            if ( frames.size() <= 1 )
                menuManager.addExtensionMenu(null);
        }


        menu = new JMenu(Config.getString("menu.view"));
        menu.setMnemonic(Config.getMnemonicKey("menu.view"));
        menubar.add(menu);
        {
            showUsesMenuItem = createCheckboxMenuItem(ShowUsesAction.getInstance(),
                                                        menu, true);
            showExtendsMenuItem = createCheckboxMenuItem(ShowInheritsAction.getInstance(),
                                                        menu, true);
            menu.addSeparator();

            JCheckBoxMenuItem item;
            createCheckboxMenuItem(ShowDebuggerAction.getInstance(), menu, false);
            createCheckboxMenuItem(ShowTerminalAction.getInstance(), menu, false);
            createCheckboxMenuItem(ShowTextEvalAction.getInstance(), menu, false);
            JSeparator testSeparator = new JSeparator();
            testItems.add(testSeparator);
            menu.add(testSeparator);

            showTestResultsItem = createCheckboxMenuItem(ShowTestResultsAction.getInstance(), menu, false);
            testItems.add(showTestResultsItem);
        }
        viewMenu = menu;



        menu = new JMenu(Config.getString("menu.help"));
        menu.setMnemonic(Config.getMnemonicKey("menu.help"));
        menubar.add(Box.createHorizontalGlue());  //TODO Hack while "setHelpMenu" does not work...
        menubar.add(menu);
        {
            createMenuItem(HelpAboutAction.getInstance(), menu);
            createMenuItem(CheckVersionAction.getInstance(), menu);
            createMenuItem(CheckExtensionsAction.getInstance(), menu);
            createMenuItem(ShowCopyrightAction.getInstance(), menu);
            menu.addSeparator();

            createMenuItem(WebsiteAction.getInstance(), menu);
            createMenuItem(TutorialAction.getInstance(), menu);
            createMenuItem(StandardAPIHelpAction.getInstance(), menu);
        }
        addUserHelpItems(menu);
        updateRecentProjects();

        setJMenuBar(menubar);
    }

    
    /**
     * Add a new menu item to a menu. 
     */
    private void createMenuItem(Action action, JMenu menu)
    {
        JMenuItem item = menu.add(action);
        item.setIcon(null);
    }
    
    /**
     * Add a new menu item to a menu. 
     */
    private JCheckBoxMenuItem createCheckboxMenuItem(PkgMgrAction action,
                                            JMenu menu, boolean selected)
    {
        ButtonModel bmodel = action.getToggleModel(this);
        
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
        if(bmodel != null)
            item.setModel(action.getToggleModel(this));
        else
            item.setState(selected);
        menu.add(item);
        return item;
    }
    
    /**
     *  Return the menu tool bar. 
     *
    public JMenu getToolsMenu()
    {
        return toolsMenu;
    }    

    /**
     * Add or remove a separator in the tools menu for extensions as needed.
     * To be deleted, Damiano
    public void toolsExtensionsCheckSeparator()
    {
        if(extMgr.haveMenuItems( )) {   // do we need one?
            if (toolsExtensionsSeparatorIndex > 0)          // have one already
                return;

            toolsExtensionsSeparatorIndex = toolsMenu.getItemCount();
            toolsMenu.addSeparator();
        } 
        else {                                            // don't need one
            if (toolsExtensionsSeparatorIndex <= 0)         // don't have one
                return;

            toolsMenu.remove(toolsExtensionsSeparatorIndex);
            toolsExtensionsSeparatorIndex = 0;
        }
    }
     */
    

    /**
     * Called on (almost) every menu invocation to clean up.
     */
    public void menuCall()
    {
        if(!isEmptyFrame())
            pkg.setState(Package.S_IDLE);
        clearStatus();
    }

    /**
     * Define which actions are to be disabled when no project is open
     */
    private void setupActionDisableSet()
    {
        actionsToDisable = new ArrayList();
        actionsToDisable.add(CloseProjectAction.getInstance());
        actionsToDisable.add(SaveProjectAction.getInstance());
        actionsToDisable.add(SaveProjectAsAction.getInstance());
        actionsToDisable.add(ImportProjectAction.getInstance());
        actionsToDisable.add(ExportProjectAction.getInstance());
        actionsToDisable.add(PageSetupAction.getInstance());
        actionsToDisable.add(PrintAction.getInstance());
        actionsToDisable.add(NewClassAction.getInstance());
        actionsToDisable.add(NewPackageAction.getInstance());
        actionsToDisable.add(AddClassAction.getInstance());
        actionsToDisable.add(RemoveAction.getInstance());
        actionsToDisable.add(NewUsesAction.getInstance());
        actionsToDisable.add(NewInheritsAction.getInstance());
        actionsToDisable.add(CompileAction.getInstance());
        actionsToDisable.add(CompileSelectedAction.getInstance());
        actionsToDisable.add(RebuildAction.getInstance());
        actionsToDisable.add(UseLibraryAction.getInstance());
        actionsToDisable.add(EvalExpressionAction.getInstance());
        actionsToDisable.add(GenerateDocsAction.getInstance());
        actionsToDisable.add(ShowUsesAction.getInstance());
        actionsToDisable.add(ShowInheritsAction.getInstance());
        actionsToDisable.add(ShowDebuggerAction.getInstance());
        actionsToDisable.add(ShowTerminalAction.getInstance());
        actionsToDisable.add(ShowTextEvalAction.getInstance());
        actionsToDisable.add(RunTestsAction.getInstance());
    }    

    /**
     * Add user defined help menus. Users can add help menus via the
     * bluej.help.items property. See comment in bluej.defs.
     */
    private void addUserHelpItems(JMenu menu)
    {
        String helpItems = Config.getPropString("bluej.help.items", "");

        if(helpItems != null && helpItems.length() > 0) {
            menu.addSeparator();
            URLDisplayer urlDisplayer = new URLDisplayer();

            StringTokenizer t = new StringTokenizer(helpItems);

            while (t.hasMoreTokens()) {
                String itemID = (String)t.nextToken();
                String itemName = Config.getPropString("bluej.help." + itemID + ".label");
                String itemURL = Config.getPropString("bluej.help." + itemID + ".url");
                JMenuItem item = new JMenuItem(itemName);
                item.setActionCommand(itemURL);
                item.addActionListener(urlDisplayer);
                menu.add(item);
            }
        }
    }


    /**
     * Update the 'Open Recent' menu
     */
    private void updateRecentProjects()
    {
        ProjectOpener opener = new ProjectOpener();
        recentProjectsMenu.removeAll();

        List projects = PrefMgr.getRecentProjects();
        for(Iterator it = projects.iterator(); it.hasNext(); ) {
            JMenuItem item = recentProjectsMenu.add((String)it.next());
            item.addActionListener(opener);
        }
    }


    /**
     * Enable/disable functionality. Enable or disable all the interface elements
     * that should change when a project is or is not open.
     */
    protected void enableFunctions(boolean enable)
    {
        for (Iterator it = itemsToDisable.iterator(); it.hasNext(); ) {
            JComponent component = (JComponent)it.next();
            component.setEnabled(enable);
        }
        for (Iterator it = actionsToDisable.iterator(); it.hasNext(); ) {
            Action action = (Action)it.next();
            action.setEnabled(enable);
        }
    }


    class URLDisplayer implements ActionListener
    {
        public URLDisplayer()
        {
        }

        public void actionPerformed(ActionEvent evt)
        {
            String url = evt.getActionCommand();
            showWebPage(url);
        }
    }

    class ProjectOpener implements ActionListener {
        public ProjectOpener() {}

        public void actionPerformed(ActionEvent evt)
        {
            String project = evt.getActionCommand();
            if (!openProject(project))
                setStatus(Config.getString("pkgmgr.error.open"));
        }
    }
}
