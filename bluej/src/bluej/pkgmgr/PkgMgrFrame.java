/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2010,2011,2012,2013,2014  Michael Kolling and John Rosenberg 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package bluej.pkgmgr;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.PrinterJob;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;

import bluej.BlueJEvent;
import bluej.BlueJEventListener;
import bluej.BlueJTheme;
import bluej.Config;
import bluej.collect.DataCollector;
import bluej.debugger.Debugger;
import bluej.debugger.DebuggerObject;
import bluej.debugger.ExceptionDescription;
import bluej.debugger.gentype.GenTypeClass;
import bluej.debugmgr.ExecutionEvent;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.Invoker;
import bluej.debugmgr.LibraryCallDialog;
import bluej.debugmgr.ResultWatcher;
import bluej.debugmgr.objectbench.ObjectBench;
import bluej.debugmgr.objectbench.ObjectWrapper;
import bluej.debugmgr.texteval.TextEvalArea;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.MenuManager;
import bluej.extmgr.ToolsExtensionMenu;
import bluej.extmgr.ViewExtensionMenu;
import bluej.groupwork.actions.CheckoutAction;
import bluej.groupwork.actions.TeamActionGroup;
import bluej.groupwork.ui.ActivityIndicator;
import bluej.pkgmgr.actions.AddClassAction;
import bluej.pkgmgr.actions.CancelTestRecordAction;
import bluej.pkgmgr.actions.CheckExtensionsAction;
import bluej.pkgmgr.actions.CheckVersionAction;
import bluej.pkgmgr.actions.CloseProjectAction;
import bluej.pkgmgr.actions.CompileAction;
import bluej.pkgmgr.actions.CompileSelectedAction;
import bluej.pkgmgr.actions.DeployMIDletAction;
import bluej.pkgmgr.actions.EndTestRecordAction;
import bluej.pkgmgr.actions.ExportProjectAction;
import bluej.pkgmgr.actions.GenerateDocsAction;
import bluej.pkgmgr.actions.HelpAboutAction;
import bluej.pkgmgr.actions.ImportProjectAction;
import bluej.pkgmgr.actions.NewClassAction;
import bluej.pkgmgr.actions.NewInheritsAction;
import bluej.pkgmgr.actions.NewMEprojectAction;
import bluej.pkgmgr.actions.NewPackageAction;
import bluej.pkgmgr.actions.NewProjectAction;
import bluej.pkgmgr.actions.NewUsesAction;
import bluej.pkgmgr.actions.OpenNonBlueJAction;
import bluej.pkgmgr.actions.OpenProjectAction;
import bluej.pkgmgr.actions.PageSetupAction;
import bluej.pkgmgr.actions.PkgMgrAction;
import bluej.pkgmgr.actions.PreferencesAction;
import bluej.pkgmgr.actions.PrintAction;
import bluej.pkgmgr.actions.QuitAction;
import bluej.pkgmgr.actions.RebuildAction;
import bluej.pkgmgr.actions.RemoveAction;
import bluej.pkgmgr.actions.RestartVMAction;
import bluej.pkgmgr.actions.RunTestsAction;
import bluej.pkgmgr.actions.SaveProjectAction;
import bluej.pkgmgr.actions.SaveProjectAsAction;
import bluej.pkgmgr.actions.ShowCopyrightAction;
import bluej.pkgmgr.actions.ShowDebuggerAction;
import bluej.pkgmgr.actions.ShowInheritsAction;
import bluej.pkgmgr.actions.ShowTerminalAction;
import bluej.pkgmgr.actions.ShowTestResultsAction;
import bluej.pkgmgr.actions.ShowTextEvalAction;
import bluej.pkgmgr.actions.ShowUsesAction;
import bluej.pkgmgr.actions.StandardAPIHelpAction;
import bluej.pkgmgr.actions.TutorialAction;
import bluej.pkgmgr.actions.UseLibraryAction;
import bluej.pkgmgr.actions.WebsiteAction;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.PackageTarget;
import bluej.pkgmgr.target.Target;
import bluej.pkgmgr.target.role.UnitTestClassRole;
import bluej.prefmgr.PrefMgr;
import bluej.prefmgr.PrefMgrDialog;
import bluej.testmgr.TestDisplayFrame;
import bluej.testmgr.record.InvokerRecord;
import bluej.utility.Debug;
import bluej.utility.DialogManager;
import bluej.utility.FileUtility;
import bluej.utility.GradientFillPanel;
import bluej.utility.JavaNames;
import bluej.utility.Utility;
import bluej.views.CallableView;
import bluej.views.ConstructorView;
import bluej.views.MethodView;

/**
 * The main user interface frame which allows editing of packages
 */
public class PkgMgrFrame extends JFrame
    implements BlueJEventListener, MouseListener, PackageEditorListener
{
    private static Font pkgMgrFont = PrefMgr.getStandardFont();

    static final int DEFAULT_WIDTH = 560;
    static final int DEFAULT_HEIGHT = 400;

    private static boolean testToolsShown = wantToSeeTestingTools();
    private static boolean teamToolsShown = wantToSeeTeamTools();
    private static boolean javaMEtoolsShown = wantToSeeJavaMEtools();
    
    /** Frame most recently having focus */
    private static PkgMgrFrame recentFrame = null;

    // instance fields:

    private JPanel buttonPanel;
    private JPanel testPanel;
    private JPanel javaMEPanel;
    private JPanel teamPanel;

    private JCheckBoxMenuItem showUsesMenuItem;
    private JCheckBoxMenuItem showExtendsMenuItem;

    private AbstractButton imgExtendsButton;
    private AbstractButton imgDependsButton;
    private AbstractButton runButton;

    private JLabel statusbar;
    private ActivityIndicator progressbar;

    private JLabel testStatusMessage;
    private JLabel recordingLabel;
    private AbstractButton endTestButton;
    private AbstractButton cancelTestButton;
    private JMenuItem endTestMenuItem;
    private JMenuItem cancelTestMenuItem;

    private ClassTarget testTarget = null;
    private String testTargetMethod;
    private static AtomicInteger nextTestIdentifier = new AtomicInteger(0); 
    private int testIdentifier = 0;

    private JMenuBar menubar = null;
    private JMenu recentProjectsMenu;
    private JMenu testingMenu;
    private MenuManager toolsMenuManager;
    private MenuManager viewMenuManager;
    
    private JMenu teamMenu;
    private JMenuItem shareProjectMenuItem;
    private JMenuItem teamSettingsMenuItem;
    private JMenuItem showLogMenuItem;
    private JMenuItem updateMenuItem;
    private JMenuItem commitMenuItem;
    private JMenuItem statusMenuItem;
    private AbstractButton updateButton;
    private AbstractButton commitButton;
    private AbstractButton teamStatusButton;
    private List<JComponent> teamItems;
    private JMenuItem javaMEnewProjMenuItem;
    private JMenuItem javaMEdeployMenuItem;
  
    private TeamActionGroup teamActions;
    
    private JMenuItem showTestResultsItem;
    private List<JComponent> itemsToDisable;
    private List<Action> actionsToDisable;
    private List<JComponent> testItems;
    private MachineIcon machineIcon;
    
    /* UI actions */
    private Action closeProjectAction = new CloseProjectAction();
    private Action saveProjectAction = new SaveProjectAction();
    private Action saveProjectAsAction = new SaveProjectAsAction();
    private Action importProjectAction = new ImportProjectAction();
    private Action exportProjectAction = new ExportProjectAction();
    private Action pageSetupAction = new PageSetupAction();
    private Action printAction = new PrintAction();
    private Action newClassAction = new NewClassAction();
    private Action newPackageAction = new NewPackageAction();
    private Action addClassAction = new AddClassAction();
    private Action removeAction = new RemoveAction();
    private Action newUsesAction = new NewUsesAction();
    private Action newInheritsAction = new NewInheritsAction();
    private Action compileAction = new CompileAction();
    private Action compileSelectedAction = new CompileSelectedAction();
    private Action rebuildAction = new RebuildAction();
    private Action restartVMAction = RestartVMAction.getInstance();
    private Action useLibraryAction = new UseLibraryAction();
    private Action generateDocsAction = new GenerateDocsAction();
    private PkgMgrAction showUsesAction = new ShowUsesAction();
    private PkgMgrAction showInheritsAction = new ShowInheritsAction();
    private PkgMgrAction showDebuggerAction = new ShowDebuggerAction();
    private PkgMgrAction showTerminalAction = new ShowTerminalAction();
    private PkgMgrAction showTextEvalAction = new ShowTextEvalAction();
    private Action runTestsAction = new RunTestsAction();
    private Action deployMIDletAction = new DeployMIDletAction();

    /* The scroller which holds the PackageEditor we use to edit packages */
    private JScrollPane classScroller = null;

    /*
     * The package that this frame is working on or null for the case where
     * there is no package currently being edited (check with isEmptyFrame())
     */
    private Package pkg = null;
    
    /*
     * The graph editor which works on the package or null for the case where
     * there is no package current being edited (isEmptyFrame() == true)
     */
    private PackageEditor editor = null;

    private ObjectBench objbench;
    private TextEvalArea textEvaluator;
    private JSplitPane splitPane;
    private JSplitPane objectBenchSplitPane;
    private boolean showingTextEvaluator = false;

    // lazy initialised dialogs
    private LibraryCallDialog libraryCallDialog = null;
    private ProjectPrintDialog projectPrintDialog = null;

    // set PageFormat for default page for default printer
    // this variable is lazy initialised
    private static PageFormat pageFormat = null;

    // static methods to create and remove frames

    private static List<PkgMgrFrame> frames = new ArrayList<PkgMgrFrame>(); // of PkgMgrFrames

    private static ExtensionsManager extMgr = ExtensionsManager.getInstance();

    private ExportManager exporter;

    private NoProjectMessagePanel noProjectMessagePanel = new NoProjectMessagePanel();

    /**
     * Open a PkgMgrFrame with no package. Packages can be installed into this
     * frame using the methods openPackage/closePackage.
     */
    public static PkgMgrFrame createFrame()
    {
        PkgMgrFrame frame = new PkgMgrFrame();
        frames.add(frame);
        BlueJEvent.addListener(frame);
        
        frame.addWindowFocusListener(new WindowFocusListener() {
            
            @Override
            public void windowLostFocus(WindowEvent e)
            {
                // Nothing to do...
            }
            
            @Override
            public void windowGainedFocus(WindowEvent e)
            {
                Window w = e.getWindow();
                if (w instanceof PkgMgrFrame) {
                    // This *should* always be the case
                    recentFrame = (PkgMgrFrame) w;
                }
            }
        });
        
        return frame;
    }

    /**
     * Open a PkgMgrFrame with a package. This may create a new frame or return
     * an existing frame if this package is already being edited by a frame. If
     * an empty frame exists, that frame will be used to show the package.
     */
    public static PkgMgrFrame createFrame(Package pkg)
    {
        PkgMgrFrame pmf = findFrame(pkg);

        if (pmf == null) {
            // check whether we've got an empty frame

            if (frames.size() == 1)
                pmf = frames.get(0);

            if ((pmf == null) || !pmf.isEmptyFrame())
                pmf = createFrame();

            pmf.openPackage(pkg);
        }

        return pmf;
    }

    /**
     * Remove a frame from the set of currently open PkgMgrFrames. The
     * PkgMgrFrame must not be editing a package when this function is called.
     */
    public static void closeFrame(PkgMgrFrame frame)
    {
        if (!frame.isEmptyFrame())
            throw new IllegalArgumentException();

        frames.remove(frame);

        BlueJEvent.removeListener(frame);
        PrefMgr.setFlag(PrefMgr.SHOW_TEXT_EVAL, frame.showingTextEvaluator);

        // frame should be garbage collected but we will speed it
        // on its way
        frame.dispose();
    }

    /**
     * Find a frame which is editing a particular Package and return it or
     * return null if it is not being edited
     */
    public static PkgMgrFrame findFrame(Package pkg)
    {
        for (Iterator<PkgMgrFrame> i = frames.iterator(); i.hasNext();) {
            PkgMgrFrame pmf = i.next();

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
     * Returns an array of all PkgMgrFrame objects. It can be an empty array if
     * none is found.
     */
    public static PkgMgrFrame[] getAllFrames()
    {
        PkgMgrFrame[] openFrames = new PkgMgrFrame[frames.size()];
        frames.toArray(openFrames);

        return openFrames;
    }

    /**
     * Find all PkgMgrFrames which are currently editing a particular project
     * 
     * @param proj
     *            the project whose packages to look for
     * 
     * @return an array of open PkgMgrFrame objects which are currently editing
     *         a package from this project, or null if none exist
     */
    public static PkgMgrFrame[] getAllProjectFrames(Project proj)
    {
        return getAllProjectFrames(proj, "");
    }

    /**
     * Find all PkgMgrFrames which are currently editing a particular project,
     * and which are below a certain point in the package heirarchy.
     * 
     * @param proj
     *            the project whose packages to look for
     * @param pkgPrefix
     *            the package name of a package to look for it and all its
     *            children ie if passed java.lang we would return frames for
     *            java.lang, and java.lang.reflect if they exist
     * 
     * @return an array of open PkgMgrFrame objects which are currently editing
     *         a package from this project and which have the package prefix
     *         specified, or null if none exist
     */
    public static PkgMgrFrame[] getAllProjectFrames(Project proj, String pkgPrefix)
    {
        List<PkgMgrFrame> list = new ArrayList<PkgMgrFrame>();
        String pkgPrefixWithDot = pkgPrefix + ".";

        for (Iterator<PkgMgrFrame> i = frames.iterator(); i.hasNext();) {
            PkgMgrFrame pmf = i.next();

            if (!pmf.isEmptyFrame() && pmf.getProject() == proj) {

                String fullName = pmf.getPackage().getQualifiedName();

                // we either match against the package prefix with a
                // dot added (this stops false matches against similarly
                // named package ie java.lang and java.language) or we
                // match the full name against the package prefix
                if (fullName.startsWith(pkgPrefixWithDot))
                    list.add(pmf);
                else if (fullName.equals(pkgPrefix) || (pkgPrefix.length() == 0))
                    list.add(pmf);
            }
        }

        if (list.isEmpty())
            return null;

        return list.toArray(new PkgMgrFrame[list.size()]);
    }

    /**
     * Gets the most recently used PkgMgrFrame
     * 
     * @return the PkgMgrFrame that currently has the focus
     */
    public static PkgMgrFrame getMostRecent()
    {
        if (recentFrame != null) {
            return recentFrame;
        }
        
        PkgMgrFrame[] allFrames = getAllFrames();

        // If there are no frames open, yet...
        if (allFrames.length < 1) {
            return null;
        }

        // Assume that the most recent is the first one. Not really the best
        // thing to do...
        PkgMgrFrame mostRecent = allFrames[0];

        for (int i = 0; i < allFrames.length; i++) {
            if (allFrames[i].getFocusOwner() != null) {
                mostRecent = allFrames[i];
            }
        }

        return mostRecent;
    }

    /**
     * Handle a "display about dialog" request generated by the OS
     */
    public static void handleAbout()
    {
        HelpAboutAction.getInstance().actionPerformed(getMostRecent());
    }
    
    /**
     * Handle a "show preferences" request generated by the OS
     */
    public static void handlePreferences()
    {
        PreferencesAction.getInstance().actionPerformed(getMostRecent());
    }
    
    /**
     * Handle a quite request generated by the OS
     */
    public static void handleQuit()
    {
        QuitAction.getInstance().actionPerformed(getMostRecent());
    }
    
    /**
     * Check whether the status of the 'Show unit test tools' preference has
     * changed, and if it has, show or hide them as requested.
     */
    public static void updateTestingStatus()
    {
        if (testToolsShown != wantToSeeTestingTools()) {
            for (Iterator<PkgMgrFrame> i = frames.iterator(); i.hasNext();) {
              
                PkgMgrFrame pmf = i.next();
                
                //Testing tools are always hidden in Java ME packages.  
                if ( pmf.isJavaMEpackage( ) ) {
                    pmf.showTestingTools( false );
                }
                else {
                    pmf.showTestingTools(!testToolsShown);               
                }
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
     * Check whether the status of the 'Show teamwork tools' preference has
     * changed, and if it has, show or hide them as requested.
     */
    public static void updateTeamStatus()
    {
        if (teamToolsShown != wantToSeeTeamTools()) {
            for (Iterator<PkgMgrFrame> i = frames.iterator(); i.hasNext();) {
                i.next().showTeamTools(!teamToolsShown);
            }
            teamToolsShown = !teamToolsShown;
        }
    }

    /**
     * Tell whether teamwork tools should be shown.
     */
    private static boolean wantToSeeTeamTools()
    {
        return PrefMgr.getFlag(PrefMgr.SHOW_TEAM_TOOLS);
    }
  
     /**
     * Check whether the status of the 'Show Java ME tools' preference has
     * changed, and if it has, show or hide them as requested.
     */
    public static void updateJavaMEstatus()
    {
        if ( javaMEtoolsShown != wantToSeeJavaMEtools() )  {
            for (Iterator<PkgMgrFrame> i = frames.iterator(); i.hasNext();) {
                i.next().showJavaMEtools( !javaMEtoolsShown );
            }
            javaMEtoolsShown = !javaMEtoolsShown;
        }
    }
    /**
     * Tell whether Java ME tools should be shown.
     */
    private static boolean wantToSeeJavaMEtools()
    {
        return PrefMgr.getFlag( PrefMgr.SHOW_JAVAME_TOOLS );
    }
    
    /**
     * Display a short text message to the user. Without specifying a package,
     * this is done by showing the message in the status bars of all open
     * package windows.
     */
    public static void displayMessage(String message)
    {
        for (Iterator<PkgMgrFrame> i = frames.iterator(); i.hasNext();) {
            PkgMgrFrame frame = i.next();
            frame.setStatus(message);
        }
    }

    /**
     * Display a short text message in the frame of the specified package.
     */
    public static void displayMessage(Package sourcePkg, String message)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if (pmf != null)
            pmf.setStatus(message);
    }

    /**
     * Display a short text message in the frames of the specified project.
     */
    public static void displayMessage(Project sourceProj, String message)
    {
        PkgMgrFrame pmf[] = getAllProjectFrames(sourceProj);

        if (pmf != null) {
            for (int i = 0; i < pmf.length; i++) {
                if (pmf[i] != null)
                    pmf[i].setStatus(message);
            }
        }
    }

    /**
     * Display an error message in a dialogue attached to the specified package
     * frame.
     */
    public static void showError(Package sourcePkg, String msgId)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if (pmf != null)
            DialogManager.showError(pmf, msgId);
    }

    /**
     * Display a message in a dialogue attached to the specified package frame.
     */
    public static void showMessage(Package sourcePkg, String msgId)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if (pmf != null)
            DialogManager.showMessage(pmf, msgId);
    }

    /**
     * Display a parameterised message in a dialogue attached to the specified
     * package frame.
     */
    public static void showMessageWithText(Package sourcePkg, String msgId, String text)
    {
        PkgMgrFrame pmf = findFrame(sourcePkg);

        if (pmf != null)
            DialogManager.showMessageWithText(pmf, msgId, text);
    }


    /**
     * Create a new PkgMgrFrame which does not show a package.
     * 
     * This constructor can only be called via createFrame().
     */
    private PkgMgrFrame()
    {
        this.pkg = null;
        this.editor = null;
        objbench = new ObjectBench(this);
        addCtrlTabShortcut(objbench);
        if(!Config.isGreenfoot()) {
            teamActions = new TeamActionGroup(false);
            teamActions.setAllDisabled();

            setupActionDisableSet();
            makeFrame();
            updateWindow();
            setStatus(bluej.Boot.BLUEJ_VERSION_TITLE);
        }
    }

    /**
     * Displays the package in the frame for editing
     */
    public void openPackage(Package pkg)
    {
        if (pkg == null) {
            throw new NullPointerException();
        }

        // if we are already editing a package, close it and
        // open the new one
        if (this.pkg != null) {
            closePackage();
        }

        this.pkg = pkg;

        if(! Config.isGreenfoot()) {
            this.editor = new PackageEditor(pkg, this);
            editor.getAccessibleContext().setAccessibleName(Config.getString("pkgmgr.graphEditor.title"));
            editor.setFocusable(true);
            editor.setTransferHandler(new FileTransferHandler(this));
            editor.addMouseListener(this);  // This mouse listener MUST be before
            editor.startMouseListening();   //  the editor's listener itself!
            pkg.setEditor(this.editor);
            addCtrlTabShortcut(editor);
            
            classScroller.setViewportView(editor);
            
            // fetch some properties from the package that interest us
            Properties p = pkg.getLastSavedProperties();
            
            try {
                String width_str = p.getProperty("package.editor.width", Integer.toString(DEFAULT_WIDTH));
                String height_str = p.getProperty("package.editor.height", Integer.toString(DEFAULT_HEIGHT));
                
                classScroller.setPreferredSize(new Dimension(Integer.parseInt(width_str), Integer.parseInt(height_str)));
                
                String objectBench_height_str = p.getProperty("objectbench.height");
                String objectBench_width_str = p.getProperty("objectbench.width");
                if (objectBench_height_str != null && objectBench_width_str != null) {
                    objbench.setPreferredSize(new Dimension(Integer.parseInt(objectBench_width_str),
                            Integer.parseInt(objectBench_height_str)));
                }
                
                String x_str = p.getProperty("package.editor.x", "30");
                String y_str = p.getProperty("package.editor.y", "30");
                
                int x = Integer.parseInt(x_str);
                int y = Integer.parseInt(y_str);
                
                if (x > (Config.screenBounds.width - 80))
                    x = Config.screenBounds.width - 80;
                
                if (y > (Config.screenBounds.height - 80))
                    y = Config.screenBounds.height - 80;
                
                setLocation(x, y);
            } catch (NumberFormatException e) {
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
            
            enableFunctions(true); // changes menu items
            updateWindow();
            setVisible(true);
            
            updateTextEvalBackground(isEmptyFrame());
                    
            this.toolsMenuManager.setMenuGenerator(new ToolsExtensionMenu(pkg));
            this.toolsMenuManager.addExtensionMenu(pkg.getProject());

            this.viewMenuManager.setMenuGenerator(new ViewExtensionMenu(pkg));
            this.viewMenuManager.addExtensionMenu(pkg.getProject());
        
            teamActions = pkg.getProject().getTeamActions();
            resetTeamActions();             
           
            // In Java-ME packages, we display Java-ME controls in the
            // test panel. We are just using the real estate of the test panel.
            // The rest of the testing tools (menus, etc) are always hidden.
            if (getProject().isJavaMEProject()) {
                showJavaMEcontrols(true);
                showTestingTools(false);
            }
            else {
                showTestingTools(wantToSeeTestingTools());
            }                
        }
        
        DataCollector.packageOpened(pkg);

        extMgr.packageOpened(pkg);
    }
    
    /**
     * Show or hide the Java ME controls.
     */
    private void showJavaMEcontrols(boolean show )
    {           
        javaMEdeployMenuItem.setVisible(show);
        javaMEPanel.setVisible(show);              
    }
    
    /**
     * Deploy the MIDlet suite contained in this project.
     */
    public void doDeployMIDlet()
    { 
        MIDletDeployer deployer = new MIDletDeployer( this );
        deployer.deploy( );
    } 
    
    /**
     * Set the team controls to use the team actions for the project.
     */
    private void resetTeamActions()
    {
        // The reason this is necessary is because team actions are tied to
        // a project, not to a PkgMgrFrame. However, a PkgMgrFrame may be
        // empty and not associated with a project - in that case it has its
        // own TeamActionGroup. When a project is opened, the actions from
        // the project then need to be associated with the appropriate controls.
        
        teamStatusButton.setAction(teamActions.getStatusAction());
        updateButton.setAction(teamActions.getUpdateAction());
        teamSettingsMenuItem.setAction(teamActions.getTeamSettingsAction());
        commitButton.setAction(teamActions.getCommitCommentAction());
        shareProjectMenuItem.setAction(teamActions.getImportAction());
        statusMenuItem.setAction(teamActions.getStatusAction());
        commitMenuItem.setAction(teamActions.getCommitCommentAction());
        commitMenuItem.setText(Config.getString("team.menu.commit"));
        updateMenuItem.setAction(teamActions.getUpdateAction());
        updateMenuItem.setText(Config.getString("team.menu.update"));
        showLogMenuItem.setAction(teamActions.getShowLogAction());
    }

    /**
     * Closes the current package.
     */
    public void closePackage()
    {
        if (isEmptyFrame()) {
            return;
        }
        
        extMgr.packageClosing(pkg);

        if(! Config.isGreenfoot()) {
            classScroller.setViewportView(null);
            classScroller.setBorder(Config.normalBorder);
            editor.removeMouseListener(this);
            this.toolsMenuManager.setMenuGenerator(new ToolsExtensionMenu(pkg));
            this.viewMenuManager.setMenuGenerator(new ViewExtensionMenu(pkg));
            
            getObjectBench().removeAllObjects(getProject().getUniqueId());
            clearTextEval();
            updateTextEvalBackground(true);
            showJavaMEcontrols(false);
            
            editor.graphClosed();
        }

        getPackage().closeAllEditors();
        
        DataCollector.packageClosed(pkg);

        Project proj = getProject();

        editor = null;
        pkg = null;

        // if there are no other frames editing this project, we close
        // the project
        if (PkgMgrFrame.getAllProjectFrames(proj) == null) {
            Project.cleanUp(proj);
        }
    }

    /**
     * Override standard show to add de-iconify and bring-to-front.
     */
    @Override
    public void setVisible(boolean visible)
    {
        if(!visible) {
            super.setVisible(false);
        }
        else if (!Config.isGreenfoot()) {
            super.setVisible(true);
            setState(Frame.NORMAL);
        }
    }

    /**
     * Return the package shown by this frame.
     * 
     * This call should be bracketed by a call to isEmptyFrame() before use.
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
        return pkg == null ? null : pkg.getProject();
    }

    /**
     * Return true if this frame is currently editing a package. A call to this
     * should bracket all uses of getPackage() and editor.
     */
    public boolean isEmptyFrame()
    {
        return pkg == null;
    }

    /**
     * Set the window title to show the current package name.
     */
    private void updateWindowTitle()
    {
        
        if (isEmptyFrame()) {
            setTitle("BlueJ");
        }
        else {
            String title = Config.getString("pkgmgr.title") + getProject().getProjectName();

            if (!getPackage().isUnnamedPackage())
                title = title + "  [" + getPackage().getQualifiedName() + "]";
            
            if(getProject().isTeamProject())
                title = title + " (" + Config.getString("team.project.marker") + ")";

            setTitle(title);
        }
    }
    
    /**
     * Update the window title and show needed messages
     */
    private void updateWindow()
    {
        
        if (isEmptyFrame()) {
            classScroller.setViewportView(noProjectMessagePanel);
            repaint();
        }
        updateWindowTitle();
    }

    /**
     * Display a message in the status bar of the frame
     */
    public final void setStatus(final String status)
    {
         EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (statusbar != null)
                    statusbar.setText(status);
            }
        });
        
    }
       
    /**
     * Start the activity indicator. Call from any thread.
     */
    public void startProgress()
    {
        progressbar.setRunning(true);
    }

    /**
     * Stop the activity indicator. Call from any thread.
     */
    public void stopProgress()
    {
        progressbar.setRunning(false);
    }

    /**
     * Clear status bar of the frame
     */
    public void clearStatus()
    {
       EventQueue.invokeLater(new Runnable() {
            public void run() {
                if (statusbar != null)
                    statusbar.setText(" ");
            }
        });
    }

    /**
     * Set the frames cursor to a WAIT_CURSOR while system is busy
     */
    public void setWaitCursor(boolean wait)
    {
        if (wait)
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

    /**
     * Return the Code Pad component.
     */
    public TextEvalArea getCodePad()
    {
        return textEvaluator;
    }

    public void mousePressed(MouseEvent evt)
    {
        clearStatus();
    }

    public void mouseReleased(MouseEvent evt)
    {}

    public void mouseClicked(MouseEvent evt)
    {}

    public void mouseEntered(MouseEvent evt)
    {}

    public void mouseExited(MouseEvent evt)
    {}


    @Override
    public void pkgEditorGotFocus()
    {
        classScroller.setBorder(Config.focusBorder);
    }
    
    
    @Override
    public void pkgEditorLostFocus()
    {
        classScroller.setBorder(Config.normalBorder);
    }

    /**
     * Deal with an event generated by a target in the package we are currently
     * editing.
     */
    public void targetEvent(PackageEditorEvent e)
    {
        int evtId = e.getID();

        switch(evtId) {
            case PackageEditorEvent.TARGET_CALLABLE :
                // user has initiated method call or constructor
                callMethod(e.getCallable());
                break;

            case PackageEditorEvent.TARGET_REMOVE :
                // user has initiated target "remove" option
                ((Target) e.getSource()).remove();
                break;

            case PackageEditorEvent.TARGET_OPEN :
                // user has initiated a package open operation
                openPackageTarget(e.getName());
                break;

            case PackageEditorEvent.TARGET_RUN :
                // user has initiated a run operation
                ClassTarget ct = (ClassTarget) e.getSource();
                ct.getRole().run(this, ct, e.getName());
                break;

            case PackageEditorEvent.TARGET_BENCHTOFIXTURE :
                // put objects on object bench into fixtures
                objectBenchToTestFixture((ClassTarget) e.getSource());
                break;

            case PackageEditorEvent.TARGET_FIXTURETOBENCH :
                // put objects on object bench into fixtures
                testFixtureToObjectBench((ClassTarget) e.getSource());
                break;

            case PackageEditorEvent.TARGET_MAKETESTCASE :
                // start recording a new test case
                makeTestCase((ClassTarget) e.getSource());
                break;

            case PackageEditorEvent.OBJECT_PUTONBENCH :
                // "Get" object from object inspector
                DebuggerObject gotObj = e.getDebuggerObject();

                boolean tryAgain = true;
                do {
                    String newObjectName = DialogManager.askString((Component) e.getSource(), "getobject-new-name",
                            getProject().getDebugger().guessNewName(gotObj));

                    if (newObjectName == null) {
                        tryAgain = false; // cancelled
                    }
                    else if (JavaNames.isIdentifier(newObjectName)) {
                        DataCollector.benchGet(getPackage(), newObjectName, e.getDebuggerObject().getClassName(), getTestIdentifier());
                        putObjectOnBench(newObjectName, e.getDebuggerObject(), e.getIType(), e.getInvokerRecord());
                        tryAgain = false;
                    }
                    else {
                        DialogManager.showError((Component) e.getSource(), "must-be-identifier");
                    }
                } while (tryAgain);
                break;
        }
    }
    
    /* (non-Javadoc)
     * @see bluej.pkgmgr.PackageEditorListener#recordInteraction(bluej.testmgr.record.InvokerRecord)
     */
    public void recordInteraction(InvokerRecord ir)
    {
        getObjectBench().addInteraction(ir);
    }
    
    /**
     * Gets the current test identifier (used to identify tests during the data recording)
     */
    public int getTestIdentifier()
    {
        return testIdentifier;
    }


    // --- below are implementations of particular user actions ---
    // These are broken into "interactive" methods (which can display dialogs
    // etc) and "non-interactive". In general interactive methods delegate to
    // the non-interactive variants.

    // --- non-interactive methods ---
    
    /**
     * Create a new project and display it in a frame.
     * @param dirName           The directory to create the project in
     * @param isJavaMEproject   Whether to create a Java Micro Edition project
     * @return     true if successful, false otherwise
     */
    public boolean newProject(String dirName, boolean isJavaMEproject )
    {
        if (Project.createNewProject(dirName, isJavaMEproject)) {
            Project proj = Project.openProject(dirName, this);
            
            Package unNamedPkg = proj.getPackage("");
            
            if (isEmptyFrame()) {
                openPackage( unNamedPkg );
            }
            else {
                PkgMgrFrame pmf = createFrame( unNamedPkg );
                DialogManager.tileWindow(pmf, this);
                pmf.setVisible(true);
            }    
            return true;
        }
        return false;
    }
    
    /**
     * Import a project from a directory into the current package. 
     * @param dir               The directory to import
     * @param showFailureDialog True to show a dialog with files which failed
     *                          to copy
     * @return An array of Files which failed to copy
     */
    public File[] importProjectDir(File dir, boolean showFailureDialog)
    {
        // recursively copy files from import directory to package directory
        File[] fails = FileUtility.recursiveCopyFile(dir, getPackage().getPath());

        // if we have any files which failed the copy, we show them now
        if (fails != null && showFailureDialog) {
            JDialog importFailedDlg = new ImportFailedDialog(this, fails);
            importFailedDlg.setVisible(true);
        }

        // add bluej.pkg files through the imported directory structure
        List<File> dirsToConvert = Import.findInterestingDirectories(getPackage().getPath());
        Import.convertDirectory(dirsToConvert);

        // reload all the packages (which discovers classes which may have
        // been added by the import)
        getProject().reloadAll();
        
        return fails;
    }
    
    /**
     * Creates a new class using the given name and template
     * 
     * @param name
     *            is not a fully qualified class name
     * @param template
     *            can be null in this case no template will be generated
     * @param showErr
     *            true if a "duplicate name" dialog should be shown if
     *            the named class already exists
     * @return  true if successful, false is the named class already exists
     */
    public boolean createNewClass(String name, String template, boolean showErr)
    {
        // check whether name is already used
        if (pkg.getTarget(name) != null) {
            DialogManager.showError(this, "duplicate-name");
            return false;
        }

        //check if there already exists a class in a library with that name 
        String[] conflict=new String[1];
        Class<?> c = pkg.loadClass(pkg.getQualifiedName(name));
        if (c != null){
            if (! Package.checkClassMatchesFile(c, new File(getPackage().getPath(), name + ".class"))) {
                conflict[0]=Package.getResourcePath(c);
                if (DialogManager.askQuestion(this, "class-library-conflict", conflict) == 0) {
                    return false;
                }
            }
        }

        ClassTarget target = null;
        target = new ClassTarget(pkg, name, template);

        if ( template != null ) { 
            boolean success = target.generateSkeleton(template);
            if (! success)
                return false;
        }

        pkg.findSpaceForVertex(target);
        pkg.addTarget(target);

        if (editor != null) {
            editor.revalidate();
            editor.scrollRectToVisible(target.getBounds());
            editor.repaint();
        }

        if (target.getRole() instanceof UnitTestClassRole) {
            pkg.compileQuiet(target);
        }
        
        DataCollector.addClass(pkg, target.getSourceFile());
        
        return true;
    }

    // --- interactive methods ---
    
    /**
     * Allow the user to select a directory into which we create a project.
     * @param isJavaMEproject   Whether this is a Java Micro Edition project or not.
     * @return true if the project was successfully created. False otherwise.
     */
    public boolean doNewProject( boolean isJavaMEproject )
    {
        String title = Config.getString( "pkgmgr.newPkg.title" );
        if ( isJavaMEproject )
            title = Config.getString( "pkgmgr.newMEpkg.title" );
                    
        File newnameFile = FileUtility.getDirName( this, title,
                 Config.getString( "pkgmgr.newPkg.buttonLabel" ), false, true );

        if (newnameFile == null)
            return false;

        if (newnameFile.exists()) {
            if (! newnameFile.isDirectory()) {
                DialogManager.showError(null, "directory-exists-file");
                return false;
            }
            else if (newnameFile.list().length > 0) {
                Debug.message("Attempt to create project with existing non-empty directory: " + newnameFile.getAbsolutePath());
                DialogManager.showError(null, "directory-exists-non-empty");
                return false;
            }
            // directory exists but is empty - fall through:
        }
        
        if (! newProject(newnameFile.getAbsolutePath(), isJavaMEproject)) {
            DialogManager.showErrorWithText(null, "cannot-create-directory", newnameFile.getPath());
            return false;
        }

        return true;
    }
   
    /**
     * Opens either a project from a directory or an archive.
     * 
     * @param pmf Optional parameter. Used for displaying dialogs and reuse
     *            if it is the empty frame.
     */
    public static boolean doOpen(File projectPath, PkgMgrFrame pmf)
    {     
        boolean createdNewFrame = false;
        if(pmf == null && PkgMgrFrame.frames.size() > 0) {
            pmf = PkgMgrFrame.frames.get(0);
        }
        else if(pmf == null) {
            pmf = PkgMgrFrame.createFrame();
            createdNewFrame = true;
        }

        boolean openedProject = false;
        if (projectPath != null) {
            if (projectPath.isDirectory() || Project.isProject(projectPath.toString())) {
                if(pmf.openProject(projectPath.getAbsolutePath())) {
                    openedProject = true;
                }
            }
            else {
                if(pmf.openArchive(projectPath)) {
                    openedProject = true;
                }
            }
        }
        if(createdNewFrame && !openedProject) {
            // Close newly created frame if it was never used.
            PkgMgrFrame.closeFrame(pmf);
        }
        return openedProject;
    }
    
    /**
     * Open a dialog that lets the user choose a project. The project selected
     * is opened in a frame.
     */
    public void doOpen()
    {
        File dirName = FileUtility.getPackageName(this);
        PkgMgrFrame.doOpen(dirName, this);
    }

    /**
     * Open the project specified by 'projectPath'. Return false if not
     * successful. Displays a warning dialog if the opened project resides in
     * a read-only directory.
     */
    private boolean openProject(String projectPath)
    {
        Project openProj = Project.openProject(projectPath, this);
        if (openProj == null)
            return false;
        else {
            Package initialPkg = openProj.getPackage(openProj.getInitialPackageName());

            PkgMgrFrame pmf = findFrame(initialPkg);

            if (pmf == null) {
                if (isEmptyFrame()) {
                    pmf = this;
                    openPackage(initialPkg);
                }
                else {
                    pmf = createFrame(initialPkg);

                    DialogManager.tileWindow(pmf, this);
                }
            }

            pmf.setVisible(true);

            return true;
        }
    }

    /**
     * Open a dialog that lets a user convert existing Java source into a BlueJ
     * project.
     * 
     * The project selected is opened in a frame.
     */
    public void doOpenNonBlueJ()
    {
        File dirName = FileUtility.getNonBlueJDirectoryName(this);

        if (dirName == null)
            return;

        File absDirName = dirName.getAbsoluteFile();
        
        // First confirm the chosen file exists
        if (! absDirName.exists()) {
            // file doesn't exist
            DialogManager.showError(this, "file-does-not-exist");
            return;
        }
        
        if (absDirName.isDirectory()) {
            // Check to make sure it's not already a project
            if (Project.isProject(absDirName.getPath())) {
                DialogManager.showError(this, "open-non-bluej-already-bluej");
                return;
            }
            
            // Try and convert it to a project
            if (! Import.convertNonBlueJ(this, absDirName))
                return;
            
            // then construct it as a project
            openProject(absDirName.getPath());
        }
        else {
            // Presumably it's an archive file
            openArchive(absDirName);
        }
    }

    /**
     * Open an archive file (jar or same contents with other extensions) as a
     * BlueJ project. The file contents are extracted, the containing directory
     * is then converted into a BlueJ project if necessary, and opened.
     */
    private boolean openArchive(File archive)
    {
        // Determine the output path.
        File oPath = Utility.maybeExtractArchive(archive, this);
        
        if (oPath == null)
            return false;
        
        if (Project.isProject(oPath.getPath())) {
            return openProject(oPath.getPath());
        }
        else {
            // Convert to a BlueJ project
            if (Import.convertNonBlueJ(this, oPath)) {
                return openProject(oPath.getPath());
            }
            else {
                return false;
            }
        }        
    }

    /**
     * Close all frames which show packages from the specified project. This
     * causes the project itself to close.
     */
    public static void closeProject(Project project) 
    {
        PkgMgrFrame[] allFrames = getAllProjectFrames(project);

        if (allFrames != null) {
            for (int i = 0; i < allFrames.length; i++) {
                allFrames[i].doClose(true, true);
            }
        }
    }
    
    /**
     * Perform a user initiated close of this frame/package.
     * 
     * There are two different methods for the user to initiate a close. One is
     * through the "Close" menu item and the other is with the windows close
     * button. We want slightly different behaviour for these two cases.
     */
    public void doClose(boolean keepLastFrame, boolean doSave)
    {
        if (doSave) {
            doSave();
        }

        // If only one frame and this was from the menu
        // "close", close should close existing package rather
        // than remove frame

        if (frameCount() == 1) {
            if (keepLastFrame && !Config.isGreenfoot()) { // close package, leave frame, but not for greenfoot
                testRecordingEnded(); // disable test controls
                closePackage();
                
                updateRecentProjects();
                enableFunctions(false); // changes menu items
                updateWindow();
                toolsMenuManager.addExtensionMenu(null);
                viewMenuManager.addExtensionMenu(null);
            }
            else { // all frames gone, lets quit
                bluej.Main.doQuit();
            }
        }
        else {
            closePackage(); // remove package and frame
            PkgMgrFrame.closeFrame(this);
        }
    }

    /**
     * Save this package. Don't ask questions - just do it.
     */
    public void doSave()
    {
        if (isEmptyFrame()) {
            return;
        }
        
        // store the current editor size in the bluej.pkg file
        Properties p;
        if (pkg.isUnnamedPackage()) {
            // The unnamed package also contains project properties
            p = getProject().getProjectProperties();
        }
        else {
            p = new Properties();
        }
        
        if(!Config.isGreenfoot()) {
            Dimension d = classScroller.getSize();
    
            p.put("package.editor.width", Integer.toString(d.width));
            p.put("package.editor.height", Integer.toString(d.height));
            
            Point point = getLocation();
    
            p.put("package.editor.x", Integer.toString(point.x));
            p.put("package.editor.y", Integer.toString(point.y));
            
            d = objbench.getSize();
            p.put("objectbench.width", Integer.toString(d.width));
            p.put("objectbench.height", Integer.toString(d.height));
    
            p.put("package.showUses", Boolean.toString(isShowUses()));
            p.put("package.showExtends", Boolean.toString(isShowExtends()));
        }
        pkg.save(p);
    }

    /**
     * Import into a new project or import into the current project.
     */
    public void doImport()
    {
        // prompt for the directory to import from
        File importDir = FileUtility.getDirName(this, Config.getString("pkgmgr.importPkg.title"), Config
                .getString("pkgmgr.importPkg.buttonLabel"), true, false);

        if (importDir == null)
            return;

        if (!importDir.isDirectory())
            return;

        // if we are an empty then we shouldn't go on (we shouldn't get
        // here)
        if (isEmptyFrame())
            return;

        // recursively copy files from import directory to package directory
        importProjectDir(importDir, true);
    }
    
    /**
     * Implementation of the "Add Class from File" user function
     */
    public void doAddFromFile()
    {
        // multi selection file dialog that shows .java and .class files
        File[] classes = FileUtility.getMultipleFiles(this, Config.getString("pkgmgr.addClass.title"), Config
                .getString("pkgmgr.addClass.buttonLabel"), FileUtility.getJavaSourceFilter());

        if (classes == null)
            return;
        importFromFile(classes);
    }
        
    
    /**
     * Add a given set of Java source files as classes to this package.
     */
    public void addFiles(List<File> classes)
    {
        importFromFile(classes.toArray(new File[classes.size()]));
    }
    
    /**
     * Add the given set of Java source files as classes to this package.
     */
    private void importFromFile(File[] classes)
    {
        // if there are errors this will potentially bring up multiple error
        // dialogs
        // these could be aggregated however the error messages may be different
        // for each error
        for (int i = 0; i < classes.length; i++) {
            int result = pkg.importFile(classes[i]);

            switch(result) {
                case Package.NO_ERROR :
                    // Have commented out repaint as it does not seem to be
                    // needed
                    //editor.repaint();
                    break;
                case Package.FILE_NOT_FOUND :
                    DialogManager.showErrorWithText(this, "file-does-not-exist", classes[i].getName());
                    break;
                case Package.ILLEGAL_FORMAT :
                    DialogManager.showErrorWithText(this, "cannot-import", classes[i].getName());
                    break;
                case Package.CLASS_EXISTS :
                    DialogManager.showErrorWithText(this, "duplicate-name", classes[i].getName());
                    break;
                case Package.COPY_ERROR :
                    DialogManager.showErrorWithText(this, "error-in-import", classes[i].getName());
                    break;
            }

        }
    }

    /**
     * Implementation of the "Export" user function
     */
    public void doExport()
    {
        if (exporter == null) {
            exporter = new ExportManager(this);
        }
        exporter.export();
    }

    /**
     * Creates a page setup dialog to alter page dimensions.
     *  
     */
    public void doPageSetup()
    {
        PrinterJob job = PrinterJob.getPrinterJob();
        PageFormat pfmt = job.pageDialog(getPageFormat());
        setPageFormat(pfmt);
    }

    /**
     * accessor method for PageFormat object that can be used by various
     * printing subsystems eg. source code printing from editor
     * 
     * @return common PageFormat object representing page preferences
     */
    public static PageFormat getPageFormat()
    {
        if (pageFormat == null) {
            pageFormat = PrinterJob.getPrinterJob().defaultPage();

        }
        //Important that this is set before the margins:
        int orientation = Config.getPropInteger("bluej.printer.paper.orientation", pageFormat.getOrientation());
        pageFormat.setOrientation(orientation);
        
        Paper paper = pageFormat.getPaper();
        int x = Config.getPropInteger("bluej.printer.paper.x", 72);
        int y = Config.getPropInteger("bluej.printer.paper.y", 72);
        int width = Config.getPropInteger("bluej.printer.paper.width", (int)paper.getWidth() - 72 - x);
        int height = Config.getPropInteger("bluej.printer.paper.height", (int)paper.getHeight() - 72 - y);
        paper.setImageableArea(x, y, width, height);
        //paper is a copy of pageFormat's paper, so we must use set again to make the changes:
        pageFormat.setPaper(paper);
        return pageFormat;
    }

    /**
     * set method for printing PageFormat. Called by other elements that may
     * manipulate pageformat, at this stage the source editor is the only
     * component that does. The assumption is that the PageFormat should be
     * uniform between all components that may want to send output to a printer.
     * 
     * @param page
     *            the new PageFormat
     */
    public static void setPageFormat(PageFormat page)
    {
        pageFormat = page;
        // We must get the measurements from the paper (which ignores orientation)
        // rather than page format (which takes it into account) because ultimately
        // we will use paper.setImageableArea to load the dimensions again
        Paper paper = pageFormat.getPaper();
        double x = paper.getImageableX();
        double y = paper.getImageableY();
        double width = paper.getImageableWidth();
        double height = paper.getImageableHeight();
        //The sizes are in points, so saving them as an integer should be precise enough:
        Config.putPropInteger("bluej.printer.paper.x", (int)x);
        Config.putPropInteger("bluej.printer.paper.y", (int)y);
        Config.putPropInteger("bluej.printer.paper.width", (int)width);
        Config.putPropInteger("bluej.printer.paper.height", (int)height);
        int orientation = pageFormat.getOrientation();
        Config.putPropInteger("bluej.printer.paper.orientation", orientation);

    }

    /**
     * Implementation of the "print" user function
     */
    public void doPrint()
    {
        if (projectPrintDialog == null)
            projectPrintDialog = new ProjectPrintDialog(this);

        if (projectPrintDialog.display()) {
            PackagePrintManager printManager = new PackagePrintManager(this.getPackage(), getPageFormat(),
                    projectPrintDialog);
            printManager.start();
        }
    }

    /**
     * Preferences menu was chosen.
     */
    public void showPreferences( )
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
        JOptionPane.showMessageDialog(this, new String[]{
                Config.getString("menu.help.copyright.line0"), " ",
                Config.getString("menu.help.copyright.line1"), Config.getString("menu.help.copyright.line2"),
                Config.getString("menu.help.copyright.line3"), Config.getString("menu.help.copyright.line4"),
                },
                Config.getString("menu.help.copyright.title"), JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Interactively call a class (ie static) method or a class constructor
     */
    private void callMethod(final CallableView cv)
    {
        ResultWatcher watcher = null;

        if (cv instanceof ConstructorView) {
            // if we are constructing an object, create a watcher that waits for
            // completion of the call and then places the object on the object
            // bench
            watcher = new ResultWatcher() {
                public void beginCompile()
                {
                    setWaitCursor(true);
                    setStatus(Config.getString("pkgmgr.creating"));
                }
                
                public void beginExecution(InvokerRecord ir)
                {
                    BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir);
                    setWaitCursor(false);
                }
                
                public void putResult(DebuggerObject result, String name, InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
                    executionEvent.setResultObject(result);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                    
                    getPackage().getProject().updateInspectors();
                    setStatus(Config.getString("pkgmgr.createDone"));
                    
                    // this shouldn't ever happen!! (ajp 5/12/02)
                    if ((name == null) || (name.length() == 0))
                        name = "result";

                    if (result != null) {
                        ObjectWrapper wrapper = ObjectWrapper.getWrapper(PkgMgrFrame.this, getObjectBench(), result,
                                result.getGenType(), name);
                        getObjectBench().addObject(wrapper);

                        getPackage().getDebugger().addObject(pkg.getId(), wrapper.getName(), result);

                        getObjectBench().addInteraction(ir);
                    }
                    else {
                        // This shouldn't happen, but let's play it safe.
                    }
                }

                public void putError(String msg, InvokerRecord ir)
                {
                    setStatus("");
                    setWaitCursor(false);
                }
                
                public void putException(ExceptionDescription exception, InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
                    executionEvent.setException(exception);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                    
                    setStatus("");
                    getPackage().exceptionMessage(exception);
                    getPackage().getProject().updateInspectors();
                }
                
                public void putVMTerminated(InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.TERMINATED_EXIT);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                    
                    setStatus("");
                }
            };
        }
        else if (cv instanceof MethodView) {
            final MethodView mv = (MethodView) cv;

            // create a watcher
            // that waits for completion of the call and then displays the
            // result (or does nothing if void)
            watcher = new ResultWatcher() {
                private ExpressionInformation expressionInformation = new ExpressionInformation(mv, getName());

                public void beginCompile()
                {
                    setWaitCursor(true);
                    if (mv.isMain()) {
                        getProject().removeClassLoader();
                        getProject().newRemoteClassLoaderLeavingBreakpoints();
                    }
                }
                
                public void beginExecution(InvokerRecord ir)
                {
                    BlueJEvent.raiseEvent(BlueJEvent.METHOD_CALL, ir);
                    setWaitCursor(false);
                }
                
                public void putResult(DebuggerObject result, String name, InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setMethodName(mv.getName());
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.NORMAL_EXIT);
                    executionEvent.setResultObject(result);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                    
                    getPackage().getProject().updateInspectors();
                    expressionInformation.setArgumentValues(ir.getArgumentValues());
                    getObjectBench().addInteraction(ir);

                    // a void result returns a name of null
                    if (name == null)
                        return;

                    //The result can be null when terminating the program while
                    // at a breakpoint in a method that has a return value.
                    if (result == null)
                        return;

                    getProject().getResultInspectorInstance(result, name, getPackage(), ir,
                            expressionInformation, PkgMgrFrame.this);
                }

                public void putError(String msg, InvokerRecord ir)
                {
                    setWaitCursor(false);
                }
                
                public void putException(ExceptionDescription exception, InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.EXCEPTION_EXIT);
                    executionEvent.setException(exception);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                    
                    getPackage().getProject().updateInspectors();
                    getPackage().exceptionMessage(exception);
                }
                
                public void putVMTerminated(InvokerRecord ir)
                {
                    ExecutionEvent executionEvent = new ExecutionEvent(pkg, cv.getClassName(), null);
                    executionEvent.setParameters(cv.getParamTypes(false), ir.getArgumentValues());
                    executionEvent.setResult(ExecutionEvent.TERMINATED_EXIT);
                    BlueJEvent.raiseEvent(BlueJEvent.EXECUTION_RESULT, executionEvent);
                }
            };
        }

        // create an Invoker to handle the actual invocation
        if (checkDebuggerState()) {
            new Invoker(this, cv, watcher).invokeInteractive();
        }
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
        pmf.setVisible(true);
    }

    /**
     * Create the text fixture method in the indicated target on from the
     * current objects on the object bench.
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
            if (!testToolsShown)
                showTestingTools(true);
            utcr.doMakeTestCase(this, target);
        }
    }


    /**
     * Place a given object onto the object bench. This is done by creating an object wrapper
     * for the internal object, which can then be added to the bench.
     * 
     * @param newInstanceName  Name for the instance on the bench.
     * @param object    The internal object to be placed.
     * @param iType    The "interface type" of the object. This is the type of the object
     *               for purposes of method calls etc if the actual type is inaccessible
     *               (private to another package or class).
     * @param ir    The invoker record (for recording interaction). May be null.
     * @return The actual instance name (which might be different from parameter, if there was a name clash)
     */
    public String putObjectOnBench(String newInstanceName, DebuggerObject object, GenTypeClass iType, InvokerRecord ir)
    {
        if (!object.isNullObject()) {
            ObjectWrapper wrapper = ObjectWrapper.getWrapper(this, getObjectBench(), object, iType, newInstanceName);
            getObjectBench().addObject(wrapper); // might change name
            newInstanceName = wrapper.getName();

            // load the object into runtime scope
            getPackage().getDebugger().addObject(pkg.getId(), newInstanceName, object);

            if (ir != null) {
                ir.setBenchName(newInstanceName, wrapper.getTypeName());
            }
            return newInstanceName;
        }
        else
        {
            return null;
        }
    }

    /**
     * Implementation of the "New Class" user function.
     */
    public void doCreateNewClass()
    {
        NewClassDialog dlg = new NewClassDialog(this, isJavaMEpackage());
        boolean okay = dlg.display();

        if (okay) {
            String name = dlg.getClassName();
            String template = dlg.getTemplateName();

            createNewClass(name, template, true);
        }
    }

    /**
     * Prompts the user with a dialog asking for the name of a package to
     * create. Package name can be fully qualified in which case all
     * intermediate packages will also be created as necessary.
     */
    public void doCreateNewPackage()
    {
        NewPackageDialog dlg = new NewPackageDialog(this);
        boolean okay = dlg.display();
        
        if (!okay)
            return;
        
        String name = dlg.getPackageName();

        if (name.length() == 0)
            return;

        createNewPackage(name, true);
    }
    
    /**
     * Create a package. Package name can be fully qualified in which case all
     * intermediate packages will also be created as necessary.
     * 
     * @param name    The name of the package to create
     * @param showErrDialog   If true, and a duplicate name exists, a dialog
     *                    will be displayed informing the user of the error.
     * @return true if successful
     */
    public boolean createNewPackage(String name, boolean showErrDialog)
    {
        String fullName;

        // if the name is fully qualified then we leave it as is but
        // if it is not we assume they want to create a package in the
        // current package
        if (name.indexOf('.') > -1) {
            fullName = name;
        }
        else {
            fullName = getPackage().getQualifiedName(name);
        }

        // check whether name is already used for a class or package
        // in the parent package
        String prefix = JavaNames.getPrefix(fullName);
        String base = JavaNames.getBase(fullName);

        Package basePkg = getProject().getPackage(prefix);
        if (basePkg != null) {
            if (basePkg.getTarget(base) != null) {
                if (showErrDialog)
                    DialogManager.showError(this, "duplicate-name");
                return false;
            }
        }

        getProject().createPackageDirectory(fullName);

        // check that everything has gone well and instruct all affected
        // packages to reload (to make them notice the new sub packages)
        Package newPackage = getProject().getPackage(fullName);

        if (newPackage == null) {
            Debug.reportError("creation of new package failed unexpectedly");
            // TODO propagate a more informative exception
            return false;
        }
        
        newPackage = newPackage.getParent();
        while (newPackage != null) {
            newPackage.reload();
            newPackage = newPackage.getParent();
        }
        
        return true;
    }

    /**
     * Remove the selected targets. Ask before deletion. If nothing is selected
     * display an errormessage.
     */
    public void doRemove()
    {
        Component permanentFocusOwner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getPermanentFocusOwner();
        if (permanentFocusOwner == editor || Arrays.asList(editor.getComponents()).contains(permanentFocusOwner)) { // focus in diagram
            if (!(doRemoveTargets() || doRemoveDependency())) {
                DialogManager.showError(this, "no-class-selected");
            }
        }
        else if (permanentFocusOwner == objbench || objbench.getObjects().contains(permanentFocusOwner)) { // focus in object bench
            objbench.removeSelectedObject(pkg.getId());
        }
        else {
            // ignore the command - focus is probably in text eval area
        }
    }

    private boolean doRemoveTargets()
    {
        Target[] targets = pkg.getSelectedTargets();
        if (targets.length <= 0) {
            return false;
        }
        if (askRemoveClass()) {
            for (int i = 0; i < targets.length; i++) {
                targets[i].remove();
            }
        }
        return true;
    }

    private boolean doRemoveDependency()
    {
        Dependency dependency = pkg.getSelectedDependency();
        if (dependency == null) {
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
        editor.clearSelection();
    }

    /**
     * The user function to remove an arrow from the dagram was invoked.
     * 
     * public void doRemoveArrow() { pkg.setState(Package.S_DELARROW);
     * setStatus(Config.getString("pkgmgr.chooseArrow")); }
     */

    /**
     * The user function to test all classes in a package
     */
    public void doTest()
    {
        runButton.setEnabled(false);

        List<ClassTarget> l = pkg.getTestTargets();

        // Find the number of tests
        int numTests = 0;
        ListIterator<ClassTarget> i = l.listIterator();
        while (i.hasNext()) {
            ClassTarget ct = i.next();
            if (ct.isCompiled() && ! ct.isAbstract()) {
                UnitTestClassRole utcr = (UnitTestClassRole) ct.getRole();
                numTests += utcr.getTestCount(ct);
            }
            else {
                i.remove();
            }
        }
        
        Iterator<ClassTarget> it = l.iterator();
        TestDisplayFrame.getTestDisplay().startMultipleTests(numTests);

        TestRunnerThread trt = new TestRunnerThread(this, it);
        trt.start();
    }
    
    /**
     * Called by the test runner thread when the test run has finished.
     * Re-enables the "run all tests" button.
     */
    public void endTestRun()
    {
        TestDisplayFrame.getTestDisplay().endMultipleTests();
        runButton.setEnabled(true);
    }

    /**
     * The 'end test recording' button was clicked - end the recording.
     */
    public void doEndTest()
    {
        if (testTarget != null) {
            testRecordingEnded();
            
            DataCollector.endTestMethod(getPackage(), testIdentifier);
            
            if (testTarget.getRole() instanceof UnitTestClassRole) {
                UnitTestClassRole utcr = (UnitTestClassRole) testTarget.getRole();
                
                utcr.doEndMakeTestCase(this, testTarget, testTargetMethod);
            }
            
            // try to compile the test class we have just changed. Do this before
            // installing the new class loader, because that causes a short machine
            // execution during which compilation fails with an error message
            getPackage().compileQuiet(testTarget);

            // remove objects from object bench
            getProject().removeClassLoader();
            getProject().newRemoteClassLoaderLeavingBreakpoints();
            
            testTarget = null;
        }
    }

    /**
     * The 'cancel test recording' button was clicked - cancel the recording.
     */
    public void doCancelTest()
    {
        testRecordingEnded();
        
        DataCollector.cancelTestMethod(getPackage(), testIdentifier);

        // remove objects from object bench (may have been put there
        // when testing was started)
        getProject().removeClassLoader();
        getProject().newRemoteClassLoaderLeavingBreakpoints();

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
        endTestMenuItem.setEnabled(true);
        cancelTestButton.setEnabled(true);
        cancelTestMenuItem.setEnabled(true);

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
        endTestMenuItem.setEnabled(false);
        cancelTestButton.setEnabled(false);
        cancelTestMenuItem.setEnabled(false);

        Project proj = getProject();
        if (proj != null) {
            proj.setTestMode(false);
        }
    }

    /**
     * Store information about the currently recorded test method.
     */
    public void setTestInfo(String testName, ClassTarget testClass)
    {
        this.testTargetMethod = testName;
        this.testTarget = testClass;
        this.testIdentifier = nextTestIdentifier.incrementAndGet(); // Allocate next test identifier
        DataCollector.startTestMethod(getPackage(), testIdentifier, testClass.getSourceFile(), testName);
    }

    /**
     * Ask the user to confirm removal of package.
     * 
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
     * 
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
        if (targets.length > 0) {
            for (int i = 0; i < targets.length; i++) {
                if (targets[i] instanceof ClassTarget) {
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
     * User function "Use Library Class...". Pop up the dialog that allows users
     * to invoke library classes.
     */
    public void callLibraryClass()
    {
        if (libraryCallDialog == null)
            libraryCallDialog = new LibraryCallDialog(this);
        libraryCallDialog.setVisible(true);
    }

    /**
     * User function "Generate Documentation...".
     */
    public void generateProjectDocumentation()
    {
        String message = pkg.generateDocumentation();
        if (message.length() != 0) {
            DialogManager.showText(this, message);
        }
    }

    /**
     * Check the debugger state is suitable for execution: that is, it is not already
     * executing anything or stuck at a breakpoint.
     * 
     * <P>Returns true if the debugger is currently idle, or false if it is already
     * executing, in which case an error dialog is also displayed and the debugger
     * controls window is made visible.
     */
    public boolean checkDebuggerState()
    {
        Debugger debugger = getProject().getDebugger();
        if (debugger.getStatus() == Debugger.SUSPENDED) {
            setVisible(true);
            DialogManager.showError(this, "stuck-at-breakpoint");
            return false;
        }
        else if (debugger.getStatus() == Debugger.RUNNING) {
            setVisible(true);
            DialogManager.showError(this, "already-executing");
            return false;
        }
        
        return true;
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
        {
            getProject().restartVM();
            DataCollector.restartVM(getProject());
        }
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
        for (Iterator<JComponent> it = testItems.iterator(); it.hasNext();) {
            JComponent component = it.next();
            component.setVisible(show);
        }
    }
    
    /**
     * Show or hide the teamwork tools.
     */
    public void showTeamTools(boolean show)
    {
        for (Iterator<JComponent> it = teamItems.iterator(); it.hasNext();) {
            JComponent component = it.next();
            component.setVisible(show);
        }
    }
    
    /**
     * Show or hide the Java ME tools, which for now is just the
     * 'New ME Project...' menu item in the Project menu.
     * Java ME tools show or not in all packages--not only in
     * Java ME packages--depending on whether the checkbox in 
     * the Preferences panel is ticked or not.
     */
    public void showJavaMEtools( boolean show )
    {
        javaMEnewProjMenuItem.setVisible( show );
    }

    /**
     * Notify the frame that the "shared" status of the project has changed,
     * i.e. the project has become shared or unshared.
     * 
     * @param shared  The new shared status of the project
     */
    public void updateSharedStatus(boolean shared)
    {
        updateWindow();
    }
    
    /**
     * Tell whether we are currently showing the text evaluation pane.
     * 
     * @return true if the text eval pane is visible.
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
        if (showingTextEvaluator == show) // already showing the right thing?
            return;

        if (show) {
            addTextEvaluatorPane();
            textEvaluator.requestFocus();
        }
        else {
            removeTextEvaluatorPane();
            editor.requestFocus();
        }
        pack();
        showingTextEvaluator = show;
    }

    /**
     * Clear the text evaluation component (if it exists).
     */
    public void clearTextEval()
    {
        if (textEvaluator != null) {
            textEvaluator.clear();
        }
    }
    
    /**
     * Updates the background of the text evaluation component (if it exists),
     * when a project is opened/closed
     */
    public void updateTextEvalBackground(boolean emptyFrame)
    {
        if (textEvaluator != null) {
            textEvaluator.updateBackground(emptyFrame);
        }
    }

    // ---- BlueJEventListener interface ----
    
    /**
     * A BlueJEvent was raised. Check whether it is one that we're interested
     * in.
     */
    public void blueJEvent(int eventId, Object arg)
    {
        switch(eventId) {
            case BlueJEvent.CREATE_VM :
                setStatus(Config.getString("pkgmgr.creatingVM"));
                break;
            case BlueJEvent.CREATE_VM_DONE :
                setStatus(Config.getString("pkgmgr.creatingVMDone"));
                break;
            case BlueJEvent.GENERATING_DOCU :
                setStatus(Config.getString("pkgmgr.generatingDocu"));
                break;
            case BlueJEvent.DOCU_GENERATED :
                setStatus(Config.getString("pkgmgr.docuGenerated"));
                break;
            case BlueJEvent.DOCU_ABORTED :
                setStatus(Config.getString("pkgmgr.docuAborted"));
                break;
            case BlueJEvent.CREATE_VM_FAILED :
                DialogManager.showError(this, "error-create-vm");
                break;
        }
    }

    // ---- end of BlueJEventListener interface ----

    /**
     * The debugger state has changed. Indicate the state in our interface and
     * change the system state accordingly (e.g. enable/disable terminal).
     * 
     * NOTE: The current implementation assumes that user VMs DO NOT run
     * concurrently!
     */
    public void setDebuggerState(int state)
    {
        switch(state) {
            case Debugger.NOTREADY :
            case Debugger.LAUNCH_FAILED:
                break;

            case Debugger.IDLE :
                if(machineIcon != null) {
                    machineIcon.setIdle();
                }
                getProject().getTerminal().activate(false);
                break;

            case Debugger.RUNNING :
                if(machineIcon != null) {
                    machineIcon.setRunning();
                }
                getProject().getTerminal().activate(true);
                break;

            case Debugger.SUSPENDED :
                if(machineIcon != null) {
                    machineIcon.setStopped();
                }
                break;
        }
    }

    // --- general support functions for user function implementations ---

    /**
     * String representation for debugging only.
     */
    @Override
    public String toString()
    {
        String str = "PkgMgrFrame(): ";

        if (isEmptyFrame())
            str += "empty";
        else
            str += getPackage().toString() + " " + getProject().toString();

        return str;
    }

    /**
     * showWebPage - show a page in a web browser and display a message in the
     * status bar.
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
        setFont(pkgMgrFont);
        Image icon = BlueJTheme.getIconImage();
        if (icon != null) {
            setIconImage(icon);
        }
        testItems = new ArrayList<JComponent>();
        teamItems = new ArrayList<JComponent>();

        setupMenus();
        
        // To get a gradient fill for the frame, we need to override the content pane's
        // paintComponent method to use a gradient fill (no other way to do it)
        // Hence this code, that sets the content pane to be a standard JPanel with
        // the same layout as before, but with paintComponent performing a gradient fill:
        if (!Config.isRaspberryPi()){
            setContentPane(new GradientFillPanel(getContentPane().getLayout()));
        }else{
            setContentPane(new JPanel(getContentPane().getLayout()));
        }
        // To let that gradient fill show through, all the other panes that sit
        // on top of the frame must have setOpaque(false) called, hence all the calls
        // of that type throughout the code below

        Container contentPane = getContentPane();
        ((JPanel) contentPane).setBorder(BlueJTheme.generalBorderWithStatusBar);

        // create the main panel holding the diagram and toolbar on the left

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));
        if (!Config.isRaspberryPi()) mainPanel.setOpaque(false);

        // Install keystroke to restart the VM
        Action action = RestartVMAction.getInstance();
        mainPanel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(
                (KeyStroke) action.getValue(Action.ACCELERATOR_KEY), "restartVM");
        mainPanel.getActionMap().put("restartVM", action);

        // create the left hand side toolbar
        JPanel toolPanel = new JPanel();
        if (!Config.isRaspberryPi()) toolPanel.setOpaque(false);
        {
            buttonPanel = new JPanel();
            if (!Config.isRaspberryPi()) buttonPanel.setOpaque(false);
            {
                buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
                buttonPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));

                AbstractButton button = createButton(newClassAction, false, false, 4, 4);
                buttonPanel.add(button);
                if(!Config.isMacOSLeopard()) buttonPanel.add(Box.createVerticalStrut(3));

                imgDependsButton = createButton(newUsesAction, true, false, 4, 4);
                buttonPanel.add(imgDependsButton);
                if(!Config.isMacOSLeopard()) buttonPanel.add(Box.createVerticalStrut(3));

                imgExtendsButton = createButton(newInheritsAction, true, false, 4, 4);
                buttonPanel.add(imgExtendsButton);
                if(!Config.isMacOSLeopard()) buttonPanel.add(Box.createVerticalStrut(3));

                button = createButton(compileAction, false, false, 4, 4);
                buttonPanel.add(button);
                if(!Config.isMacOSLeopard()) buttonPanel.add(Box.createVerticalStrut(3));

                buttonPanel.setAlignmentX(0.5f);
            }

            testPanel = new JPanel();
            if (!Config.isRaspberryPi()) testPanel.setOpaque(false);
            {
                testPanel.setLayout(new BoxLayout(testPanel, BoxLayout.Y_AXIS));

                testPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 14, 5));

                runButton = createButton(runTestsAction, false, false, 2, 4);
                runButton.setText(Config.getString("pkgmgr.test.run"));
                runButton.setAlignmentX(0.15f);
                testPanel.add(runButton);
                testPanel.add(Box.createVerticalStrut(8));

                recordingLabel = new JLabel(Config.getString("pkgmgr.test.record"), Config
                        .getFixedImageAsIcon("record.gif"), SwingConstants.LEADING);
                recordingLabel.setFont(pkgMgrFont);
                recordingLabel.setEnabled(false);
                recordingLabel.setAlignmentX(0.15f);
                testPanel.add(recordingLabel);
                testPanel.add(Box.createVerticalStrut(3));

                action = EndTestRecordAction.getInstance();
                endTestButton = createButton(action, false, false, 2, 4);
                //make the button use a different label than the one from
                // action
                endTestButton.setText(Config.getString("pkgmgr.test.end"));
                endTestButton.setEnabled(false);

                testPanel.add(endTestButton);
                if(!Config.isMacOSLeopard()) testPanel.add(Box.createVerticalStrut(3));

                action = CancelTestRecordAction.getInstance();
                cancelTestButton = createButton(action, false, false, 2, 4);
                //make the button use a different label than the one from
                // action
                cancelTestButton.setText(Config.getString("cancel"));
                cancelTestButton.setEnabled(false);

                testPanel.add(cancelTestButton);

                testPanel.setAlignmentX(0.5f);
            }
            testItems.add(testPanel);
            
            teamPanel = new JPanel();
            if (!Config.isRaspberryPi()) teamPanel.setOpaque(false);
            {
                teamPanel.setLayout(new BoxLayout(teamPanel, BoxLayout.Y_AXIS));

                teamPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 14, 5));
                updateButton = createButton(teamActions.getUpdateAction(), false, false, 2, 4);                
                updateButton.setAlignmentX(0.15f);
                teamPanel.add(updateButton);
                if(!Config.isMacOSLeopard()) teamPanel.add(Box.createVerticalStrut(3));
                
                commitButton = createButton(teamActions.getCommitCommentAction(), false, false, 2, 4);
                commitButton.setAlignmentX(0.15f);
                //make the button use a different label than the one from
                // action
                teamPanel.add(commitButton);
                if(!Config.isMacOSLeopard()) teamPanel.add(Box.createVerticalStrut(3));

                teamStatusButton = createButton(teamActions.getStatusAction(), false, false, 2, 4);
                teamStatusButton.setAlignmentX(0.15f);
                teamPanel.add(teamStatusButton);
                if(!Config.isMacOSLeopard()) teamPanel.add(Box.createVerticalStrut(3));
                teamPanel.setAlignmentX(0.5f);
            }
            teamItems.add(teamPanel);

            javaMEPanel = new JPanel();
            if (!Config.isRaspberryPi()) javaMEPanel.setOpaque(false);
            {
                javaMEPanel.setLayout(new BoxLayout(javaMEPanel, BoxLayout.Y_AXIS));

                javaMEPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 14, 5));

                JLabel label = new JLabel( "Java ME" );
                label.setFont( new Font ("SansSerif", Font.BOLD, 12 ) );
                label.setHorizontalAlignment( JLabel.CENTER );
                label.setForeground( label.getBackground( ).darker( ).darker( ) ); 
                Dimension pref = label.getMinimumSize();
                pref.width = Integer.MAX_VALUE;
                label.setMaximumSize(pref);
                label.setAlignmentX(0.5f);
                javaMEPanel.add( label );
                javaMEPanel.add( Box.createVerticalStrut( 4 ) );   
                
                AbstractButton button = createButton( deployMIDletAction, false, false, 4, 4 );
                button.setAlignmentX(0.5f);
                javaMEPanel.add( button );
                javaMEPanel.add( Box.createVerticalStrut( 4 ) );   
                if(!Config.isMacOSLeopard()) javaMEPanel.add(Box.createVerticalStrut(3));
                teamPanel.setAlignmentX(0.5f);
            }
            
            machineIcon = new MachineIcon();
            machineIcon.setAlignmentX(0.5f);
            itemsToDisable.add(machineIcon);

            toolPanel.setLayout(new BoxLayout(toolPanel, BoxLayout.Y_AXIS));
            toolPanel.add(buttonPanel);
            toolPanel.add(Box.createVerticalGlue());
            toolPanel.add(teamPanel);
            toolPanel.add(javaMEPanel);
            toolPanel.add(testPanel);
            toolPanel.add(machineIcon);
        }
        mainPanel.add(toolPanel, BorderLayout.WEST);

        classScroller = new JScrollPane();
        classScroller.setBorder(Config.normalBorder);
        classScroller.setPreferredSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        classScroller.setSize(new Dimension(DEFAULT_WIDTH, DEFAULT_HEIGHT));
        classScroller.setFocusable(false);
        classScroller.getVerticalScrollBar().setUnitIncrement(10);
        classScroller.getHorizontalScrollBar().setUnitIncrement(20);
        if (!Config.isRaspberryPi()) classScroller.setOpaque(false);
        mainPanel.add(classScroller, BorderLayout.CENTER);

        itemsToDisable.add(objbench);

        splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mainPanel, objbench);
        splitPane.setBorder(null);
        splitPane.setResizeWeight(1.0);
        splitPane.setDividerSize(5);
        if (!Config.isRaspberryPi()) splitPane.setOpaque(false);
        contentPane.add(splitPane, BorderLayout.CENTER);

        // create the bottom status area

        JPanel statusArea = new JPanel(new BorderLayout());
        if (!Config.isRaspberryPi()) statusArea.setOpaque(false);
        {
            statusArea.setBorder(BorderFactory.createEmptyBorder(2, 0, 4, 6));

            statusbar = new JLabel(" ");
            statusbar.setFont(pkgMgrFont);
            statusArea.add(statusbar, BorderLayout.CENTER);

            testStatusMessage = new JLabel(" ");
            testStatusMessage.setFont(pkgMgrFont);
            statusArea.add(testStatusMessage, BorderLayout.WEST);
            
            progressbar = new ActivityIndicator();
            progressbar.setRunning(false);
            statusArea.add(progressbar, BorderLayout.EAST);
        }
        contentPane.add(statusArea, BorderLayout.SOUTH);

        // hide testing tools if not wanted
        if (!testToolsShown) {
            showTestingTools(false);
        }

        // hide team tools if not wanted
        if (! teamToolsShown) {
            showTeamTools(false);
        }

        // hide Java ME tools if not wanted
        if (! javaMEtoolsShown) {
            showJavaMEtools(false);
        }
        
        javaMEPanel.setVisible(false);
        
        // show the text evaluation pane if needed
        if (PrefMgr.getFlag(PrefMgr.SHOW_TEXT_EVAL)) {
            addTextEvaluatorPane();
        }

        pack();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent E)
            {
                PkgMgrFrame pmf = (PkgMgrFrame) E.getWindow();
                pmf.doClose(false, true);
            }
        });

        // grey out certain functions if package not open.
        if (isEmptyFrame()) {
            enableFunctions(false);
        }
    }

    /**
     * Add the text evaluation pane in the lower area of the frame.
     */
    private void addTextEvaluatorPane()
    {
        classScroller.setPreferredSize(classScroller.getSize()); // memorize
                                                                 // current size
        if (textEvaluator == null) {
            textEvaluator = new TextEvalArea(this, pkgMgrFont);
            objectBenchSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, objbench, textEvaluator);
            objectBenchSplitPane.setBorder(null);
            objectBenchSplitPane.setResizeWeight(1.0);
            objectBenchSplitPane.setDividerSize(5);
            if (!Config.isRaspberryPi()) objectBenchSplitPane.setOpaque(false);
            itemsToDisable.add(textEvaluator);
            addCtrlTabShortcut(textEvaluator.getFocusableComponent());
        }
        else {
            objectBenchSplitPane.setLeftComponent(objbench);
        }
        splitPane.setBottomComponent(objectBenchSplitPane);
        showingTextEvaluator = true;
    }

    /**
     * Remove the text evaluation pane from the frame.
     */
    private void removeTextEvaluatorPane()
    {
        textEvaluator.setPreferredSize(textEvaluator.getSize()); // memorize
                                                                 // current
                                                                 // sizes
        classScroller.setPreferredSize(classScroller.getSize());
        splitPane.setBottomComponent(objbench);
        showingTextEvaluator = false;
    }

    /**
     * Create a button for the interface.
     * 
     * @param action
     *            the Action abstraction dictating text, icon, tooltip, action.
     * @param notext
     *            set true if the action text should not appear (icon only).
     * @param toggle
     *            true if this is a toggle button, false otherwise
     * @param hSpacing
     *            horizontal margin (left, right)
     * @param vSpacing
     *            vertical margin (top, bottom)
     * @return the new button
     */
    private AbstractButton createButton(Action action, boolean notext, boolean toggle, int hSpacing, int vSpacing)
    {
        AbstractButton button;
        if (toggle) {
            button = new JToggleButton(action);
        }
        else {
            button = new JButton(action);
        }
        button.setFont(pkgMgrFont);
        Utility.changeToMacButton(button);
        button.setFocusable(false); // buttons shouldn't get focus

        if (notext)
            button.setText(null);

        Dimension pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
        if(!Config.isMacOSLeopard()) {
            button.setMargin(new Insets(vSpacing, hSpacing, vSpacing, hSpacing));
        }

        return button;
    }

    /**
     * setupMenus - Create the menu bar
     */
    private void setupMenus()
    {
        menubar = new JMenuBar();
        itemsToDisable = new ArrayList<JComponent>();

        JMenu menu = new JMenu(Config.getString("menu.package"));
        int mnemonic = Config.getMnemonicKey("menu.package");
        menu.setMnemonic(mnemonic);
        menubar.add(menu);
        {
            createMenuItem(NewProjectAction.getInstance(), menu);
            javaMEnewProjMenuItem = createMenuItem( NewMEprojectAction.getInstance(), menu );            
            createMenuItem(OpenProjectAction.getInstance(), menu);
            recentProjectsMenu = new JMenu(Config.getString("menu.package.openRecent"));
            menu.add(recentProjectsMenu);
            createMenuItem(OpenNonBlueJAction.getInstance(), menu);
            createMenuItem(closeProjectAction, menu);
            createMenuItem(saveProjectAction, menu);
            createMenuItem(saveProjectAsAction, menu);
            menu.addSeparator();

            createMenuItem(importProjectAction, menu);
            createMenuItem(exportProjectAction, menu);
            javaMEdeployMenuItem = createMenuItem( deployMIDletAction, menu ); 
            javaMEdeployMenuItem.setVisible( false ); //visible only in Java ME packages
            menu.addSeparator();

            createMenuItem(pageSetupAction, menu);
            createMenuItem(printAction, menu);

            if (!Config.usingMacScreenMenubar()) { // no "Quit" here for Mac
                menu.addSeparator();
                createMenuItem(QuitAction.getInstance(), menu);
            }
        }

        menu = new JMenu(Config.getString("menu.edit"));
        menu.setMnemonic(Config.getMnemonicKey("menu.edit"));
        menubar.add(menu);
        {
            createMenuItem(newClassAction, menu);
            createMenuItem(newPackageAction, menu);
            createMenuItem(addClassAction, menu);
            createMenuItem(removeAction, menu);
            menu.addSeparator();

            createMenuItem(newUsesAction, menu);
            createMenuItem(newInheritsAction, menu);
        }

        menu = new JMenu(Config.getString("menu.tools"));
        menu.setMnemonic(Config.getMnemonicKey("menu.tools"));
        menubar.add(menu);
        {
            createMenuItem(compileAction, menu);
            createMenuItem(compileSelectedAction, menu);
            createMenuItem(rebuildAction, menu);
            createMenuItem(restartVMAction, menu);
            menu.addSeparator();

            createMenuItem(useLibraryAction, menu);
            createMenuItem(generateDocsAction, menu);

            testingMenu = new JMenu(Config.getString("menu.tools.testing"));
            testingMenu.setMnemonic(Config.getMnemonicKey("menu.tools"));
            {
                createMenuItem(runTestsAction, testingMenu);
                endTestMenuItem = createMenuItem(EndTestRecordAction.getInstance(), testingMenu);
                cancelTestMenuItem = createMenuItem(CancelTestRecordAction.getInstance(), testingMenu);
                endTestMenuItem.setEnabled(false);
                cancelTestMenuItem.setEnabled(false);
            }
            testItems.add(testingMenu);
            menu.add(testingMenu);
            
            //team menu setup
            teamMenu = new JMenu(Config.getString("menu.tools.teamwork"));
            teamMenu.setMnemonic(Config.getMnemonicKey("menu.tools"));
            {
                Action checkoutAction = CheckoutAction.getInstance();
                createMenuItem(checkoutAction , teamMenu);
                shareProjectMenuItem = createMenuItem(teamActions.getImportAction(), teamMenu);               
                
                teamMenu.addSeparator();
                
                updateMenuItem = createMenuItem(teamActions.getUpdateAction(), teamMenu);
                updateMenuItem.setText(Config.getString("team.menu.update"));
                commitMenuItem = createMenuItem(teamActions.getCommitCommentAction(), teamMenu);
                commitMenuItem.setText(Config.getString("team.menu.commit"));
                statusMenuItem = createMenuItem(teamActions.getStatusAction(), teamMenu);
                showLogMenuItem = createMenuItem(teamActions.getShowLogAction(), teamMenu);
                
                teamMenu.addSeparator();
                
                teamSettingsMenuItem = createMenuItem(teamActions.getTeamSettingsAction(), teamMenu);
            }
            teamItems.add(teamMenu);
            menu.add(teamMenu);

            if (!Config.usingMacScreenMenubar()) { // no "Preferences" here for
                                                   // Mac
                menu.addSeparator();
                createMenuItem(PreferencesAction.getInstance(), menu);
            }

            // Create the menu manager that looks after extension tools menus
            toolsMenuManager = new MenuManager(menu.getPopupMenu());

            // If this is the first frame create the extension tools menu now.
            // (Otherwise, it will be created during project open.)
            if (frames.size() <= 1) {
                toolsMenuManager.setMenuGenerator(new ToolsExtensionMenu(null));
                toolsMenuManager.addExtensionMenu(null);
            }
        }

        menu = new JMenu(Config.getString("menu.view"));
        menu.setMnemonic(Config.getMnemonicKey("menu.view"));
        menubar.add(menu);
        {
            showUsesMenuItem = createCheckboxMenuItem(showUsesAction, menu, true);
            showExtendsMenuItem = createCheckboxMenuItem(showInheritsAction, menu, true);
            menu.addSeparator();

            createCheckboxMenuItem(showDebuggerAction, menu, false);
            createCheckboxMenuItem(showTerminalAction, menu, false);
            createCheckboxMenuItem(showTextEvalAction, menu, false);
            JSeparator testSeparator = new JSeparator();
            testItems.add(testSeparator);
            menu.add(testSeparator);

            showTestResultsItem = createCheckboxMenuItem(ShowTestResultsAction.getInstance(), menu, false);
            testItems.add(showTestResultsItem);

            // Create the menu manager that looks after extension view menus
            viewMenuManager = new MenuManager(menu.getPopupMenu());

            // If this is the first frame create the extension view menu now.
            // (Otherwise, it will be created during project open.)
            if (frames.size() <= 1) {
                viewMenuManager.addExtensionMenu(null);
            }
        }

        menu = new JMenu(Config.getString("menu.help"));
        menu.setMnemonic(Config.getMnemonicKey("menu.help"));
        menubar.add(menu);
        {
            if (!Config.usingMacScreenMenubar()) { // no "About" here for Mac
                createMenuItem(HelpAboutAction.getInstance(), menu);
            }
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
    private JMenuItem createMenuItem(Action action, JMenu menu)
    {
        JMenuItem item = menu.add(action);
        item.setIcon(null);
        return item;
    }

    /**
     * Add a new menu item to a menu.
     */
    private JCheckBoxMenuItem createCheckboxMenuItem(PkgMgrAction action, JMenu menu, boolean selected)
    {
        ButtonModel bmodel = action.getToggleModel(this);

        JCheckBoxMenuItem item = new JCheckBoxMenuItem(action);
        if (bmodel != null)
            item.setModel(action.getToggleModel(this));
        else
            item.setState(selected);
        menu.add(item);
        return item;
    }

    /**
     * Return the menu tool bar.
     * 
     * public JMenu getToolsMenu() { return toolsMenu; }
     * 
     * /** Add or remove a separator in the tools menu for extensions as needed.
     * To be deleted, Damiano public void toolsExtensionsCheckSeparator() {
     * if(extMgr.haveMenuItems( )) { // do we need one? if
     * (toolsExtensionsSeparatorIndex > 0) // have one already return;
     * 
     * toolsExtensionsSeparatorIndex = toolsMenu.getItemCount();
     * toolsMenu.addSeparator(); } else { // don't need one if
     * (toolsExtensionsSeparatorIndex <= 0) // don't have one return;
     * 
     * toolsMenu.remove(toolsExtensionsSeparatorIndex);
     * toolsExtensionsSeparatorIndex = 0; } }
     */

    /**
     * Called on (almost) every menu invocation to clean up.
     */
    public void menuCall()
    {
        if (!isEmptyFrame())
            pkg.setState(Package.S_IDLE);
        clearStatus();
    }

    /**
     * Define which actions are to be disabled when no project is open
     */
    private void setupActionDisableSet()
    {
        actionsToDisable = new ArrayList<Action>();
        actionsToDisable.add(closeProjectAction);
        actionsToDisable.add(saveProjectAction);
        actionsToDisable.add(saveProjectAsAction);
        actionsToDisable.add(importProjectAction);
        actionsToDisable.add(exportProjectAction);
        actionsToDisable.add(pageSetupAction);
        actionsToDisable.add(printAction);
        actionsToDisable.add(newClassAction);
        actionsToDisable.add(newPackageAction);
        actionsToDisable.add(addClassAction);
        actionsToDisable.add(removeAction);
        actionsToDisable.add(newUsesAction);
        actionsToDisable.add(newInheritsAction);
        actionsToDisable.add(compileAction);
        actionsToDisable.add(compileSelectedAction);
        actionsToDisable.add(rebuildAction);
        actionsToDisable.add(restartVMAction);
        actionsToDisable.add(useLibraryAction);
        actionsToDisable.add(generateDocsAction);
        actionsToDisable.add(showUsesAction);
        actionsToDisable.add(showInheritsAction);
        actionsToDisable.add(showDebuggerAction);
        actionsToDisable.add(showTerminalAction);
        actionsToDisable.add(showTextEvalAction);
        actionsToDisable.add(runTestsAction);
    }

    /**
     * Add user defined help menus. Users can add help menus via the
     * bluej.help.items property. See comment in bluej.defs.
     */
    private void addUserHelpItems(JMenu menu)
    {
        String helpItems = Config.getPropString("bluej.help.items", "");

        if (helpItems != null && helpItems.length() > 0) {
            menu.addSeparator();
            URLDisplayer urlDisplayer = new URLDisplayer();

            StringTokenizer t = new StringTokenizer(helpItems);

            while (t.hasMoreTokens()) {
                String itemID = t.nextToken();
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

        List<String> projects = PrefMgr.getRecentProjects();
        for (Iterator<String> it = projects.iterator(); it.hasNext();) {
            JMenuItem item = recentProjectsMenu.add(it.next());
            item.addActionListener(opener);
        }
    }

    /**
     * Enable/disable functionality. Enable or disable all the interface
     * elements that should change when a project is or is not open.
     */
    protected void enableFunctions(boolean enable)
    {
        if (! enable) {
            teamActions.setAllDisabled();
        }
        
        for (Iterator<JComponent> it = itemsToDisable.iterator(); it.hasNext();) {
            JComponent component = it.next();
            component.setEnabled(enable);
        }
        for (Iterator<Action> it = actionsToDisable.iterator(); it.hasNext();) {
            Action action = it.next();
            action.setEnabled(enable);
        }
    }

    /**
     * Return true if this frame is editing a Java Micro Edition package.
     */
    public boolean isJavaMEpackage( )
    {
        if (pkg == null) return false;
        return pkg.getProject().isJavaMEProject();
    }        
    
    /**
     * Adds shortcuts for Ctrl-TAB and Ctrl-Shift-TAB to the given pane, which move to the
     * next/previous pane of the main three (package editor, object bench, code pad) that are visible
     */
    private void addCtrlTabShortcut(final JComponent toPane)
    {
        toPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK), "nextPMFPane");
        toPane.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
                KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "prevPMFPane");
        toPane.getActionMap().put("nextPMFPane", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                movePaneFocus(toPane, +1);
            }
        });
        toPane.getActionMap().put("prevPMFPane", new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e)
            {
                movePaneFocus(toPane, -1);
            }
        });
    }
    
    /**
     * Moves focus from given pane to prev (-1)/next (+1) pane.
     */
    private void movePaneFocus(final JComponent fromPane, int direction)
    {
        List<JComponent> visiblePanes = new ArrayList<JComponent>();
        if (editor != null)
        {
            // editor is null if no package is open
            visiblePanes.add(editor);
        }
        // Object bench is always present, even if no package open:
        visiblePanes.add(objbench);
        if (showingTextEvaluator)
        {
            visiblePanes.add(textEvaluator.getFocusableComponent());
        }
        
        for (int i = 0; i < visiblePanes.size(); i++)
        {
            if (visiblePanes.get(i) == fromPane)
            {
                int destination = i + direction;
                // Wrap around:
                if (destination >= visiblePanes.size()) destination = 0;
                if (destination < 0) destination = visiblePanes.size() - 1;
                
                visiblePanes.get(destination).requestFocusInWindow();
            }
        }
    }

    class URLDisplayer
        implements ActionListener
    {
        public URLDisplayer()
        {}

        @Override
        public void actionPerformed(ActionEvent evt)
        {
            String url = evt.getActionCommand();
            showWebPage(url);
        }
    }

    class ProjectOpener
        implements ActionListener
    {
        public ProjectOpener()
        {}

        @Override
        public void actionPerformed(ActionEvent evt)
        {
            String project = evt.getActionCommand();
            if (!openProject(project))
                setStatus(Config.getString("pkgmgr.error.open"));
        }
    }
    
}
