package bluej.pkgmgr;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.io.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.print.*;
import java.text.DateFormat;

import com.apple.mrj.MRJApplicationUtils;  // for handling MacOS specific events
import com.apple.mrj.MRJQuitHandler;
import com.apple.mrj.MRJAboutHandler;

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
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.HelpDialog;
import bluej.testmgr.*;

import antlr.*;

/**
 * The main user interface frame which allows editing of packages
 *
 * @version $Id: PkgMgrFrame.java 1638 2003-03-04 04:04:27Z ajp $
 */
public class PkgMgrFrame extends JFrame
    implements BlueJEventListener, MouseListener,
               PackageEditorListener, MRJQuitHandler, MRJAboutHandler
{
    // static final Color bgColor = Config.getItemColour("colour.background");
    public Font PkgMgrFont = PrefMgr.getStandardFont();

    static final int DEFAULT_WIDTH = 420;
    static final int DEFAULT_HEIGHT = 300;

    private static final Icon workingIcon = Config.getImageAsIcon("image.working");
    private static final Icon notWorkingIcon = Config.getImageAsIcon("image.working.disab");
    private static final Icon stoppedIcon = Config.getImageAsIcon("image.working.stopped");

    // set PageFormat for default page for default printer
    private static PageFormat pageFormat = PrinterJob.getPrinterJob().defaultPage();

    private static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

    // instance fields:

    private JPanel buttonPanel;

    private JCheckBoxMenuItem showUsesMenuItem;
    private JCheckBoxMenuItem showExtendsMenuItem;

    private JButton imgExtendsButton;
    private JButton imgDependsButton;
    private JTextField executorField;
    
    private JMenu recentProjectsMenu;
    private int toolsExtensionsSeparatorIndex;

    private JLabel statusbar;
    
    JLabel testStatusMessage;
    JButton endTestButton;
    JButton cancelTestButton;
    
    private ClassTarget testTarget = null;
    private String testTargetMethod;
    

    private JMenuBar menubar = null;
    private JMenu toolsMenu;
    private List itemsToDisable;
    private JButton progressButton;


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
    private FreeFormCallDialog freeFormCallDialog = null;
    private ProjectPrintDialog projectPrintDialog = null;

    // ============================================================
    // static methods to create and remove frames

    private static List frames = new ArrayList();  // of PkgMgrFrames

    private static ExtensionsManager extMgr = ExtensionsManager.getExtMgr();

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

        extMgr.addMenuItems (pmf.getProject(), pmf, pmf.toolsMenu);
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
    
    /**
     * Gets the most recently used PkgMgrFrame
     * @return the PkgMgrFrame that currently has the focus
     */
    public static PkgMgrFrame getMostRecent()
    {
        PkgMgrFrame[] frames = getAllFrames();
        PkgMgrFrame mostRecent = frames[0];
        for (int i=0; i<frames.length; i++) {
            if (frames[i].getFocusOwner() != null) mostRecent = frames[i];
        }
        return mostRecent;
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

        MRJApplicationUtils.registerQuitHandler(this);
        MRJApplicationUtils.registerAboutHandler(this);

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
        //editor.setFocusable(true);  // since 1.4 - after 1.3 is phased out, use this and remove isFocusTraversable from GraphEditor
        editor.addMouseListener(this);        // This listener MUST be before
        editor.addMouseListener(editor);      //  the editor itself!
        editor.addKeyListener(editor);
        this.pkg.editor = this.editor;
        editor.requestFocus();

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

        String uses_str = p.getProperty("package.showUses", "true");
        String extends_str = p.getProperty("package.showExtends", "true");

        showUsesMenuItem.setSelected(uses_str.equals("true"));
        showExtendsMenuItem.setSelected(extends_str.equals("true"));

        updateShowUsesInPackage();
        updateShowExtendsInPackage();

        pack();
        editor.revalidate();

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
         case PackageEditorEvent.TARGET_CALLABLE:
            // user has initiated method call or constructor
            callMethod(e.getCallable());
            break;

         case PackageEditorEvent.TARGET_REMOVE:
            // user has initiated target "remove" option
            doRemove((Target) e.getSource());
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
            {
                // put objects on object bench into fixtures
                ClassTarget bct = (ClassTarget) e.getSource();

                if (bct.getRole() instanceof UnitTestClassRole) {
                    UnitTestClassRole utcr = (UnitTestClassRole) bct.getRole();

                    utcr.doBenchToFixture(this, bct);
                }
            }
            break;

         case PackageEditorEvent.TARGET_FIXTURETOBENCH:
            {
                // put objects on object bench into fixtures
                ClassTarget bct = (ClassTarget) e.getSource();

                if (bct.getRole() instanceof UnitTestClassRole) {
                    UnitTestClassRole utcr = (UnitTestClassRole) bct.getRole();

                    utcr.doFixtureToBench(this, bct);
                }
            }
            break;

        case PackageEditorEvent.TARGET_MAKETESTCASE:
            {
                // start recording a new test case
                ClassTarget bct = (ClassTarget) e.getSource();

                if (bct.getRole() instanceof UnitTestClassRole) {
                    UnitTestClassRole utcr = (UnitTestClassRole) bct.getRole();

                    utcr.doMakeTestCase(this, bct);
                }
            }
            break;

         case PackageEditorEvent.OBJECT_PUTONBENCH:
            // "Get" object from object inspector
            String newObjectName = DialogManager.askString((Component) e.getSource(), "getobject-new-name");

            if (newObjectName != null && JavaNames.isIdentifier(newObjectName)) {
                putObjectOnBench(e.getDebuggerObject(), newObjectName);
            }
            break;
        }
    }

    // ============ end of PackageEditorListener interface ===============

    // --- below are implementations of particular user actions ---

    /**
     * Allow the user to select a directory into which we create
     * a project.
     */
    protected boolean doNewProject()
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
    private void doOpen()
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
            extMgr.projectOpening (openProj, this, toolsMenu);
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
    private void doOpenNonBlueJ()
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
        closePackage();

        // If only one frame and this was from the menu
        // "close", close should close existing package rather
        // than remove frame

        if(frameCount() == 1) {
            if(keepLastFrame) {        // close package, leave frame
                updateWindowTitle();
                updateRecentProjects();
            }
            else {                      // all frames gone, lets quit
                doQuit();
            }
        } else {                          // remove package and frame
            PkgMgrFrame.closeFrame(this);
        }
    }

    /**
     * MacOS Quit menu was chosen - redefined from MRJQuitHandler
     */
    public void handleQuit()
    {
        // On MacOS, no event handling is possible in the quithandler. That's why
        // we don't show the dialog here (wantToQuit), but exit straight out.
        // possible fix: show the dialog from another thread.
        doQuit();
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

        p.put("package.showUses", new Boolean(isShowUses()).toString());
        p.put("package.showExtends", new Boolean(isShowExtends()).toString());

        pkg.save(p);

        setStatus(Config.getString("pkgmgr.packageSaved"));
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
    private void doAddFromFile()
    {
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
     * accessor method for PageFormat object that can be used by various 
     * printing subsystems eg. source code printing from editor
     * @return common PageFormat object representing page preferences 
     */
    public static PageFormat getPageFormat()
    {
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
     * print - implementation of the "print" user function
     */
    private void print() 
    {
        if (projectPrintDialog == null)
            projectPrintDialog = new ProjectPrintDialog(this);

        if (projectPrintDialog.display()) {
            PackagePrintManager printManager = new PackagePrintManager(this.getPackage(), pageFormat, projectPrintDialog);
            printManager.start();
        }  
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
     * Copyright menu item was chosen.
     */
    public void showCopyright()
    {
        JOptionPane.showMessageDialog(this,
              new String[] {
                  "BlueJ \u00a9 2000-2002 Michael K\u00F6lling, John Rosenberg.",
                  " ",
                  "BlueJ is available free of charge and may be",
                  "redistributed freely. It may not be sold for",
                  "profit or included in other packages which",
                  "are sold for profit without written authorisation."
              },
              "BlueJ Copyright", JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Interactively call a class (ie static) method or a class constructor
     */
    private void callMethod(CallableView cv)
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
                        DebuggerObject realResult = result.getInstanceFieldObject(0);

                        ObjectWrapper wrapper = ObjectWrapper.getWrapper(
                                                       PkgMgrFrame.this, getObjectBench(),
                                                       realResult,
                                                       name);
                        getObjectBench().add(wrapper);

                        Debugger.debugger.addObjectToScope(getPackage().getId(),
                                                wrapper.getName(), realResult);
                                                
                        getObjectBench().addInteraction(ir);
                    }
                    else
                        Debug.reportError("cannot get execution result");
                }
                public void putError(String msg) { }
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

            // create a watcher
            // that waits for completion of the call and then displays the
            // result (or does nothing if void)
            watcher = new ResultWatcher() {
                public void putResult(DebuggerObject result, String name, InvokerRecord ir)
                {
                    getObjectBench().addInteraction(ir);
                    
                    // a void result returns a name of null
                    if (name == null)
                        return;
                        
                    ObjectInspector viewer =
                        ObjectInspector.getInstance(true, result, name, getPackage(), ir,
                                               PkgMgrFrame.this);
                    BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, viewer.getResult());
                }
                public void putError(String msg) { }
            };
        }

        // create an Invoker to handle the actual invocation

        new Invoker(this, cv, null, watcher).invokeInteractive();
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
     * Create an object on the object bench.
     */
    public void putObjectOnBench(DebuggerObject object, String newInstanceName)
    {
        if (!object.isNullObject()) {
            ObjectWrapper wrapper = ObjectWrapper.getWrapper(this, getObjectBench(), object, newInstanceName);
            getObjectBench().add(wrapper);  // might change name

            // load the object into runtime scope
            Debugger.debugger.addObjectToScope(getPackage().getId(),
                                                wrapper.getName(), object);
        }
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

                target.generateSkeleton(template);

                pkg.findSpaceForVertex(target);
                pkg.addTarget(target);

                editor.revalidate();
                editor.scrollRectToVisible(target.getRectangle());
                editor.repaint();
            }
        }
    }

    /**
     * Prompts the user with a dialog asking for the name of
     * a package to create. Package name can be fully qualified
     * and all intermediate packages will also be created.
     */
    private void createNewPackage()
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

        // check whether name is already used
        if (getProject().getPackage(fullName) != null) {
            DialogManager.showError(this, "duplicate-package-name");
            return;
        }

        // construct the directory name for the new package
        StringTokenizer st = new StringTokenizer(fullName, ".");
        File newPkgDir = getProject().getProjectDir();

        while(st.hasMoreTokens()) {
            newPkgDir = new File(newPkgDir, (String)st.nextToken());
        }

        // now actually construct the directories and add the bluej
        // package marker files
        if (newPkgDir.isDirectory() || newPkgDir.mkdirs()) {
            st = new StringTokenizer(fullName, ".");
            newPkgDir = getProject().getProjectDir();
            File newPkgFile = new File(newPkgDir, Package.pkgfileName);

            try {
                newPkgFile.createNewFile();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

            while(st.hasMoreTokens()) {
                newPkgDir = new File(newPkgDir, (String)st.nextToken());
                newPkgFile = new File(newPkgDir, Package.pkgfileName);

                try {
                    newPkgFile.createNewFile();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }

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
     * The user function to add a uses arrow to the diagram was invoked.
     */
    public void doNewUses()
    {
        pkg.setState(Package.S_CHOOSE_USES_FROM);
        setStatus(Config.getString("pkgmgr.chooseUsesFrom"));
    }

    /**
     * The user function to add an inherits arrow to the dagram was invoked.
     */
    public void doNewInherits()
    {
        pkg.setState(Package.S_CHOOSE_EXT_FROM);
        setStatus(Config.getString("pkgmgr.chooseInhFrom"));
    }

    /**
     * The user function to remove an arrow from the dagram was invoked.
     */
    public void doRemoveArrow()
    {
        pkg.setState(Package.S_DELARROW);
        setStatus(Config.getString("pkgmgr.chooseArrow"));
    }

    private void doEndTest()
    {
        testStatusMessage.setEnabled(false);
        testStatusMessage.setText("");
        endTestButton.setEnabled(false);
        cancelTestButton.setEnabled(false);
        
        getProject().setTestMode(false);
        
        if (testTarget.getRole() instanceof UnitTestClassRole) {
            UnitTestClassRole utcr = (UnitTestClassRole) testTarget.getRole();

            utcr.doEndMakeTestCase(this, testTarget, testTargetMethod);
        }            
    }
    
    private void doCancelTest()
    {
        testStatusMessage.setEnabled(false);
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
     * User function "Free Form Call...". Pop up the dialog that allows
     * users to make that call.
     */
    public void callLibraryClass()
    {
        if(libraryCallDialog == null)
            libraryCallDialog = new LibraryCallDialog(this);
        libraryCallDialog.setVisible(true);
    }

    /**
     * User function "Use Library Class...". Pop up the dialog that allows
     * users to invoke library classes.
     */
    public void callFreeForm()
    {
        if(freeFormCallDialog == null) {
            freeFormCallDialog = new FreeFormCallDialog(this);
        }
        ResultWatcher watcher = new ResultWatcher() {
           public void putResult(DebuggerObject result, String name, InvokerRecord ir)
           {
                getObjectBench().addInteraction(ir);

               ObjectInspector viewer =
                    ObjectInspector.getInstance(true, result, name, getPackage(), ir,
                                           PkgMgrFrame.this);
               BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, viewer.getResult());
           }
           public void putError(String msg) { }
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
            setStatus(Config.getString("pkgmgr.creatingVM"));
            break;
        case BlueJEvent.CREATE_VM_DONE:
            setStatus(Config.getString("pkgmgr.creatingVMDone"));
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
            setStatus(Config.getString("pkgmgr.generatingDocu"));
            break;
        case BlueJEvent.DOCU_GENERATED:
            setStatus(Config.getString("pkgmgr.docuGenerated"));
            break;
        case BlueJEvent.DOCU_ABORTED:
            setStatus(Config.getString("pkgmgr.docuAborted"));
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
            setStatus(Config.getString("pkgmgr.webBrowserMsg"));
        else
            setStatus(Config.getString("pkgmgr.webBrowserError"));
    }

    // --- the following methods set up the GUI frame ---

    private void makeFrame()
    {
        setFont(PkgMgrFont);
        setIconImage(Config.frameImage);

        setupMenus();

        JPanel mainPanel = new JPanel();

        mainPanel.setLayout(new BorderLayout(5, 5));
        mainPanel.setBorder(Config.generalBorderWithStatusBar);

        // create the left hand side toolbar
        JPanel toolPanel = new JPanel();
        {
            buttonPanel = new JPanel();
            {
                buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
                buttonPanel.setBorder(BorderFactory.createEmptyBorder(5,5,0,5));
    
                ImageIcon emptyIcon = Config.getImageAsIcon("image.empty");
    
                JButton button = createButton(Config.getString("menu.edit.newClass"),
                                              emptyIcon,
                                              Config.getString("tooltip.newClass"));
                button.addActionListener(new ActionListener() {
                                            public void actionPerformed(ActionEvent e) {
                                                createNewClass(); }
                                         });
                buttonPanel.add(button);
                buttonPanel.add(Box.createVerticalStrut(3));
    
    
                imgDependsButton = createButton("",
                                          Config.getImageAsIcon("image.build.depends.uml"),
                                          Config.getString("tooltip.newUses"));
                imgDependsButton.addActionListener(new ActionListener() {
                                            public void actionPerformed(ActionEvent e) {
                                                doNewUses(); }
                                         });
                buttonPanel.add(imgDependsButton);
                buttonPanel.add(Box.createVerticalStrut(3));
    
                imgExtendsButton = createButton("",
                                          Config.getImageAsIcon("image.build.extends.uml"),
                                          Config.getString("tooltip.newExtends"));
                imgExtendsButton.addActionListener(new ActionListener() {
                                            public void actionPerformed(ActionEvent e) {
                                                doNewInherits(); }
                                         });
                buttonPanel.add(imgExtendsButton);
                buttonPanel.add(Box.createVerticalStrut(3));
    
                button = createButton(Config.getString("menu.tools.compile"),
                                      emptyIcon,
                                      Config.getString("tooltip.compile"));
                button.addActionListener(new ActionListener() {
                                            public void actionPerformed(ActionEvent e) {
                                                pkg.compile(); }
                                         });
                buttonPanel.add(button);
            }
    
            // Image Button Panel to hold the Progress Image
            //        JPanel progressPanel = new JPanel ();
    
            progressButton = new JButton(workingIcon);
            progressButton.setDisabledIcon(notWorkingIcon);
            progressButton.setMargin(new Insets(0, 0, 0, 0));
            progressButton.setToolTipText(Config.getString("tooltip.progress"));
            progressButton.addActionListener(new ActionListener() {
                                public void actionPerformed(ActionEvent e) {
                                    ExecControls.showHide(true, true, null);
                                }
                           });
            progressButton.setEnabled(false);
            //        progressPanel.add(progressButton);
    
            progressButton.setAlignmentX(0.5f);
            buttonPanel.setAlignmentX(0.5f);
        }
    
        toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
        toolPanel.add(buttonPanel);
        toolPanel.add(Box.createVerticalGlue());
        toolPanel.add(progressButton);
        toolPanel.add(Box.createVerticalStrut(3));

        // create the bottom object bench and status area
        
        JPanel bottomPanel = new JPanel();
        {
            bottomPanel.setLayout(new BorderLayout());

            JPanel testPanel = new JPanel();
            testPanel.setLayout(new FlowLayout());
            testPanel.add(testStatusMessage = new JLabel(""));
            testPanel.add(endTestButton = new JButton("end test"));
            testPanel.add(cancelTestButton = new JButton("cancel test"));

            endTestButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doEndTest(); }
             });
            cancelTestButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    doCancelTest(); }
             });

            testStatusMessage.setEnabled(false);
            endTestButton.setEnabled(false);
            cancelTestButton.setEnabled(false);

            bottomPanel.add(testPanel, BorderLayout.NORTH);
            objbench = new ObjectBench();

            JComponent bench = objbench.getComponent();
            bench.setBorder(BorderFactory.createCompoundBorder(
                                BorderFactory.createBevelBorder(BevelBorder.LOWERED),
                                BorderFactory.createEmptyBorder(5,0,5,0)));
            bottomPanel.add(bench, BorderLayout.CENTER);
            statusbar = new JLabel(" ");
            statusbar.setAlignmentX(0.0f);
            
//  experiment with expression eval entry field layout
//             bottom.setBorder(BorderFactory.createEmptyBorder(2,2,4,2));
//             bottom.add(statusbar, BorderLayout.CENTER);
//             bottom.add(new JTextField(30), BorderLayout.EAST);
//             bottomPanel.add(bottom, BorderLayout.SOUTH);
            bottomPanel.add(statusbar, BorderLayout.SOUTH);
        }

        mainPanel.add(toolPanel, BorderLayout.WEST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        classScroller = new JScrollPane();
        mainPanel.add(classScroller, BorderLayout.CENTER);
        classScroller.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));

        getContentPane().add(mainPanel);

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


    /**
     * Set the images on the interface buttons according to preferences.
     */
    private JButton createButton(String text, ImageIcon icon, String toolTip)
    {
        JButton button = new JButton() {
            public boolean isFocusTraversable() { return false; }           
        };
        button.setText(text);
        button.setFont(PkgMgrFont);
        if (icon != null)
            button.setIcon(icon);

        // mac os property to change button shape
        button.putClientProperty("JButton.buttonType", "toolbar");  // "icon"

        button.setToolTipText(toolTip);
//       button.setRequestFocusEnabled(false);   // never get keyboard focus

        // make the button infinitately extendable width-wise, but never
        // grow in height
        Dimension pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
//        button.setMargin(new Insets(2, 0, 2, 0));

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
        menubar.add(menu);
        {
            createMenuItem("menu.package.new", menu, 0, 0, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doNewProject(); }
                           });
            createMenuItem("menu.package.open", menu, KeyEvent.VK_O, SHORTCUT_MASK, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doOpen(); }
                           });
            recentProjectsMenu = new JMenu(Config.getString("menu.package.openRecent"));
            menu.add(recentProjectsMenu);
            createMenuItem("menu.package.openNonBlueJ", menu, 0, 0, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doOpenNonBlueJ(); }
                           });
            createMenuItem("menu.package.close", menu, KeyEvent.VK_W, SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doClose(true); }
                           });
            createMenuItem("menu.package.save", menu, KeyEvent.VK_S, SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); getProject().saveAll(); }
                           });
            createMenuItem("menu.package.saveAs", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); getProject().saveAs(frame); }
                           });
            menu.addSeparator();

            createMenuItem("menu.package.import", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doImport(); }
                           });
            createMenuItem("menu.package.export", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doExport(); }
                           });
            menu.addSeparator();

            createMenuItem("menu.package.pageSetup", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); pageSetup(); }
                           });
            createMenuItem("menu.package.print", menu, KeyEvent.VK_P, SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); print(); }
                           });
            menu.addSeparator();

//          if(Config.osname.startsWith("Mac")) {...}   // no "Quit" here for Mac
            createMenuItem("menu.package.quit", menu, KeyEvent.VK_Q, SHORTCUT_MASK, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); wantToQuit(); }
                           });
        }


        menu = new JMenu(Config.getString("menu.edit"));
        menubar.add(menu);
        {
            createMenuItem("menu.edit.newClass", menu, KeyEvent.VK_N, SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); createNewClass(); }
                           });
            createMenuItem("menu.edit.newPackage", menu, KeyEvent.VK_R, SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); createNewPackage(); }
                           });
            createMenuItem("menu.edit.addClass", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doAddFromFile(); }
                           });
            createMenuItem("menu.edit.remove", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doRemove(); }
                           });
            menu.addSeparator();

            createMenuItem("menu.edit.newUses", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { clearStatus(); doNewUses(); }
                           });
            createMenuItem("menu.edit.newInherits", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { clearStatus(); doNewInherits(); }
                           });
            createMenuItem("menu.edit.removeArrow", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); doRemoveArrow(); }
                           });
        }


        menu = new JMenu(Config.getString("menu.tools"));
        toolsMenu = menu;
        menubar.add(menu);
        {
            createMenuItem("menu.tools.compile", menu, KeyEvent.VK_K, SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); pkg.compile(); }
                           });
            createMenuItem("menu.tools.compileSelected", menu, KeyEvent.VK_K, Event.SHIFT_MASK | SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); compileSelected(); }
                           });
            createMenuItem("menu.tools.rebuild", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); pkg.rebuild(); }
                           });
            menu.addSeparator();

            createMenuItem("menu.tools.callLibrary", menu, KeyEvent.VK_L, SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); callLibraryClass(); }
                           });
            createMenuItem("menu.tools.callFreeForm", menu, KeyEvent.VK_E, SHORTCUT_MASK, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); callFreeForm(); }
                           });
            menu.addSeparator();

            createMenuItem("menu.tools.generateDoc", menu, 0, 0, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); generateProjectDocumentation(); }
                           });
            menu.addSeparator();

            createMenuItem("menu.tools.preferences", menu, 0, 0, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); PrefMgrDialog.showDialog(frame); }
                           });




            toolsExtensionsCheckSeparator();
            extMgr.addMenuItems (null, this, toolsMenu);
        }


        menu = new JMenu(Config.getString("menu.view"));
        menubar.add(menu);
        {
            showUsesMenuItem = createCheckboxMenuItem("menu.view.showUses", menu,
                           KeyEvent.VK_U, SHORTCUT_MASK, true, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); updateShowUsesInPackage(); }
                           });
            showExtendsMenuItem = createCheckboxMenuItem("menu.view.showInherits", menu,
                           KeyEvent.VK_I, SHORTCUT_MASK, true, true,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); updateShowExtendsInPackage(); }
                           });
            menu.addSeparator();

            JCheckBoxMenuItem item = createCheckboxMenuItem("menu.view.showExecControls", menu,
                           KeyEvent.VK_D, SHORTCUT_MASK, false, false, null);
            item.setModel(new ExecControlButtonModel());

            item = createCheckboxMenuItem("menu.view.showTerminal", menu,
                           KeyEvent.VK_T, SHORTCUT_MASK, false, false, null);
            item.setModel(new TerminalButtonModel());

            item = createCheckboxMenuItem("menu.view.showTestDisplay", menu,
                           0, 0, false, false, null);
            item.setModel(new TestDisplayButtonModel());
        }


    /**
      GrpCmds = {
        "make", "open",
        "updateSelected", "updateAll",
        "commitSelected", "commitAll",
        "statusSelection", "statusAll",
        "users",
        "configuration"
      }
    **/

        menu = new JMenu(Config.getString("menu.help"));
        menubar.add(Box.createHorizontalGlue());  // Hack while "setHelpMenu" does not work...
        menubar.add(menu);
        {
            createMenuItem("menu.help.about", menu, 0, 0, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); handleAbout(); }
                           });
            createMenuItem("menu.help.versionCheck", menu, KeyEvent.VK_V, SHORTCUT_MASK, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); VersionCheckDialog dialog = new VersionCheckDialog(frame); }
                           });
            createMenuItem("menu.help.extensions", menu, 0, 0, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); new HelpDialog(frame); }
                           });
            createMenuItem("menu.help.copyright", menu, 0, 0, false, 
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); showCopyright(); }
                           });
            menu.addSeparator();

            createMenuItem("menu.help.website", menu, 0, 0, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); showWebPage(Config.getPropString("bluej.url.bluej")); }
                           });
            createMenuItem("menu.help.tutorial", menu, 0, 0, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); showWebPage(Config.getPropString("bluej.url.tutorial")); }
                           });
            createMenuItem("menu.help.standardApi", menu, 0, 0, false,
                           new ActionListener() {
                               public void actionPerformed(ActionEvent e) { menuCall(); showWebPage(Config.getPropString("bluej.url.javaStdLib")); }
                           });
        }
        addUserHelpItems(menu);
        updateRecentProjects();

        setJMenuBar(menubar);
    }


    /**
    * just returns the menu tool bar
    */
    public JMenu getToolsMenu()
      {
          return toolsMenu;
      }


    /**
     * I need this call so the separator is added when a menu is added.
     */
    public void toolsExtensionsCheckSeparator()
      {
          if ( extMgr.haveMenuItems(null, this, toolsMenu) ) {
              // I am wishing to add a separator but there is already one.
              if ( toolsExtensionsSeparatorIndex > 0 ) return;
              // Yes, this is the right place where to count ....
              toolsExtensionsSeparatorIndex=toolsMenu.getItemCount();
              toolsMenu.addSeparator();
          } else {
              // I want to remove the separator but there is none...
              if ( toolsExtensionsSeparatorIndex <= 0 ) return;
              toolsMenu.remove(toolsExtensionsSeparatorIndex);
              toolsExtensionsSeparatorIndex=0;
          }
      }



    /**
     * Add a new menu item to a menu.
     */
    private void createMenuItem(String itemStr, JMenu menu, int key, int modifiers,
                                boolean disable, ActionListener listener)
    {
        JMenuItem item = new JMenuItem(Config.getString(itemStr));

        if (key != 0)
            item.setAccelerator(KeyStroke.getKeyStroke(key, modifiers));
        item.addActionListener(listener);
        menu.add(item);
        if(disable)
            itemsToDisable.add(item);
    }

    /**
     * Register that a menu item that should be disabled when the frame is empty
     * @param item the menu item to disable whenever the frame is empty
     */
    public void disableOnEmpty (JMenuItem item)
    {
        itemsToDisable.add (item);
        item.setEnabled (!isEmptyFrame());
    }
     

    /**
     * Add a new menu item to a menu.
     */
    private JCheckBoxMenuItem createCheckboxMenuItem(String itemStr, JMenu menu,
                                                    int key, int modifiers, boolean selected,
                                                    boolean disable, ActionListener listener)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(Config.getString(itemStr), selected);

        if (key != 0)
            item.setAccelerator(KeyStroke.getKeyStroke(key, modifiers));
        item.addActionListener(listener);
        menu.add(item);
        if(disable)
            itemsToDisable.add(item);

        return item;
    }


    /**
     * Called on (almost) every menu invocation to clean up.
     */
    private void menuCall()
    {
        if(!isEmptyFrame())
            pkg.setState(Package.S_IDLE);
        clearStatus();
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
     * Enable/disable functionality
     */
    protected void enableFunctions(boolean enable)
    {
        // set Button enable status
        Component[] panelComponents = buttonPanel.getComponents();
        for(int i = 0; i < panelComponents.length; i++)
            panelComponents[i].setEnabled(enable);

        for (Iterator it = itemsToDisable.iterator(); it.hasNext(); ) {
            JMenuItem menu = (JMenuItem)it.next();
            menu.setEnabled(enable);
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
