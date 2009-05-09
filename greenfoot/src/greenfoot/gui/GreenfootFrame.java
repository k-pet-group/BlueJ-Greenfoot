/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui;

import greenfoot.Actor;
import greenfoot.World;
import greenfoot.actions.AboutGreenfootAction;
import greenfoot.actions.CloseProjectAction;
import greenfoot.actions.CompileAllAction;
import greenfoot.actions.ExportProjectAction;
import greenfoot.actions.NewClassAction;
import greenfoot.actions.NewProjectAction;
import greenfoot.actions.OpenProjectAction;
import greenfoot.actions.OpenRecentProjectAction;
import greenfoot.actions.PauseSimulationAction;
import greenfoot.actions.PreferencesAction;
import greenfoot.actions.QuitAction;
import greenfoot.actions.RemoveSelectedClassAction;
import greenfoot.actions.ResetWorldAction;
import greenfoot.actions.RunOnceSimulationAction;
import greenfoot.actions.RunSimulationAction;
import greenfoot.actions.SaveCopyAction;
import greenfoot.actions.SaveProjectAction;
import greenfoot.actions.ShowApiDocAction;
import greenfoot.actions.ShowCopyrightAction;
import greenfoot.actions.ShowReadMeAction;
import greenfoot.actions.ShowWebsiteAction;
import greenfoot.core.GClass;
import greenfoot.core.GCoreClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.ProjectProperties;
import greenfoot.core.Simulation;
import greenfoot.core.WorldHandler;
import greenfoot.event.CompileListener;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;
import greenfoot.event.WorldEvent;
import greenfoot.event.WorldListener;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;
import greenfoot.gui.classbrowser.Selectable;
import greenfoot.gui.classbrowser.SelectionListener;
import greenfoot.gui.input.mouse.LocationTracker;
import greenfoot.gui.inspector.GreenfootResultInspector;
import greenfoot.gui.inspector.GreenfootClassInspector;
import greenfoot.gui.inspector.GreenfootObjectInspector;
import greenfoot.platforms.ide.SimulationDelegateIDE;
import greenfoot.platforms.ide.WorldHandlerDelegateIDE;
import greenfoot.sound.SoundFactory;
import greenfoot.util.GreenfootUtil;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.Config;
import bluej.debugger.DebuggerClass;
import bluej.debugger.DebuggerObject;
import bluej.debugmgr.ExpressionInformation;
import bluej.debugmgr.inspector.ClassInspector;
import bluej.debugmgr.inspector.Inspector;
import bluej.debugmgr.inspector.InspectorManager;
import bluej.debugmgr.inspector.ObjectInspector;
import bluej.debugmgr.inspector.ResultInspector;
import bluej.pkgmgr.Package;
import bluej.prefmgr.PrefMgr;
import bluej.testmgr.record.ClassInspectInvokerRecord;
import bluej.testmgr.record.InvokerRecord;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * The main frame for a Greenfoot project (one per project)
 * 
 * @author Poul Henriksen
 * @author mik
 *
 * @version $Id: GreenfootFrame.java 6322 2009-05-09 17:50:58Z polle $
 */
public class GreenfootFrame extends JFrame
    implements WindowListener, CompileListener, WorldListener, SelectionListener,
        InspectorManager
{
    private static final String readMeIconFile = "readme.png";
    private static final String compileIconFile = "compile.png";
    private static final int WORLD_MARGIN = 40;

    private static final int accelModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private static final int shiftAccelModifier = accelModifier | KeyEvent.SHIFT_MASK;

    private GProject project;
    
    /** This holds all object inspectors for a world. */
    private Map<DebuggerObject, Inspector> objectInspectors = new HashMap<DebuggerObject, Inspector> ();
    /** This holds all class inspectors for a world. */
    private Map<String, Inspector> classInspectors = new HashMap<String, Inspector> ();
    
    private WorldCanvas worldCanvas;
    private WorldHandler worldHandler;
    private WorldHandlerDelegateIDE worldHandlerDelegate;
    private Dimension worldDimensions;
    private ClassBrowser classBrowser;
    private ControlPanel controlPanel;
    private JScrollPane classScrollPane;
    
    private NewClassAction newClassAction;
    private SaveProjectAction saveProjectAction;
    private SaveCopyAction saveCopyAction;
    private ShowReadMeAction showReadMeAction;
    private ExportProjectAction exportProjectAction;
    private CloseProjectAction closeProjectAction;
    private RemoveSelectedClassAction removeSelectedClassAction;
    private CompileAllAction compileAllAction;
    
    private JMenu recentProjectsMenu;
    
    /**
     * Specifies whether the project has been closed and only the empty frame is showing.
     * (Behind the scenes, the project is actually still open).
     */
    private boolean isClosedProject = true;
    
    /**
     * Indicate whether we want to resize. 
     * 
     * @see #setResizeWhenPossible(boolean)
     * @see #needsResize()
     */
    private boolean resizeWhenPossible = false;
    
    private static GreenfootFrame instance;
    
    public static GreenfootFrame getGreenfootFrame(final RBlueJ blueJ)
    {
    	instance = new GreenfootFrame(blueJ);                        
        return instance;
    }
    
    /**
     * Creates a new top level frame with all the GUI components.
     */
    private GreenfootFrame(RBlueJ blueJ)
        throws HeadlessException
    {
        super("Greenfoot");
        try {
            if (Config.isWinOS()) {
                // UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
        }
        catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }
        
        LocationTracker.instance(); //force initialisation
        ImageIcon icon = new ImageIcon(GreenfootUtil.getGreenfootLogoPath());
        setIconImage(icon.getImage());

        makeFrame();
        addWindowListener(this);
        
        restoreFrameState();

        prepareMacOSApp();
        
        setVisible(true);
        worldCanvas.requestFocusInWindow();
        worldCanvas.requestFocus();
    }
    
    /**
     * Restore the current main window size from the project properties.
     */
    private void restoreFrameState()
    {
        if (project == null) {
        	// We don't have a project yet: just use default size
            setBounds(40, 40, 700, 500);
            setResizeWhenPossible(true);
            return;
        }
    	
    	ProjectProperties projectProperties = project.getProjectProperties();

        try {            
            int x = projectProperties.getInt("mainWindow.x");
            int y = projectProperties.getInt("mainWindow.y");

            int width = projectProperties.getInt("mainWindow.width");
            int height = projectProperties.getInt("mainWindow.height");

            setBounds(x, y, width, height);
            setResizeWhenPossible(false);
        } 
        catch (NumberFormatException ecx) {
            // doesn't matter - just use some default size
            setBounds(40, 40, 700, 500);
            setResizeWhenPossible(true);
        }
        
    }

    

    /**
     * Prepare MacOS specific behaviour (About menu, Preferences menu, Quit
     * menu)
     */
    private Application prepareMacOSApp()
    {
        Application macApp = new Application();
        macApp.setEnabledPreferencesMenu(true);
        macApp.addApplicationListener(new ApplicationAdapter() {
            public void handleAbout(ApplicationEvent e)
            {
                AboutGreenfootAction.getInstance(GreenfootFrame.this).actionPerformed(null);
                e.setHandled(true);
            }

            public void handlePreferences(ApplicationEvent e)
            {
               PreferencesAction.getInstance().actionPerformed(null);
               e.setHandled(true);
            }

            public void handleQuit(ApplicationEvent e)
            {
                exit();
                e.setHandled(true);
            }
        });

        return macApp;
    }
    
    
    /**
     * Open a given project into this frame.
     */
    public void openProject(final GProject project)
    {
        if (isClosedProject) {
            this.project = project;
            worldHandlerDelegate.attachProject(project);
            project.addCompileListener(this);
            setTitle("Greenfoot: " + project.getName());
            enableProjectActions();

            worldCanvas.setVisible(true);

            // Class browser
            buildClassBrowser();
            populateClassBrowser(classBrowser, project);
            classBrowser.setVisible(true);
            classScrollPane.setViewportView(classBrowser);

            restoreFrameState();

            try {
                ProjectProperties props = project.getProjectProperties();
                int initialSpeed = props.getInt("simulation.speed");
                Simulation.getInstance().setSpeed(initialSpeed);
            } catch (NumberFormatException nfe) {
                // If there is no speed info in the properties we don't care...
            }
            
            WorldHandler.getInstance().instantiateNewWorld();
            worldHandlerDelegate.getWorldTitle().setVisible(true);
            if (needsResize()) {
                pack();
            }
            isClosedProject = false;
        }
    }
    
    /**
     * Calling this will make the current frame an empty frame.
     */
    public void closeProject()
    {
        setTitle("Greenfoot: ");
        project.removeCompileListener(this);
        worldCanvas.setVisible(false);
        classBrowser.setVisible(false);
        worldHandlerDelegate.getWorldTitle().setVisible(false);
        project = null;
        enableProjectActions();
        repaint();
        isClosedProject = true;
    }

    /**
     * Get the class browser currently embedded in this frame.
     */
    public ClassBrowser getClassBrowser()
    {
    	return classBrowser;
    }
    
    /**
     * Get the project showing in this frame. If this frame is empty,
     * will return null.
     */
    public GProject getProject()
    {
    	return project;
    }
    
    /**
     * Create the GUI components for this project in the top level frame.
     * This includes opening the project and displaying the project classes.
     */
    private void makeFrame()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Some first-time initializations
        worldCanvas = new WorldCanvas(null);
        
        
        worldHandlerDelegate = new WorldHandlerDelegateIDE(this);
        WorldHandler.initialise(worldCanvas, worldHandlerDelegate);
        worldHandler = WorldHandler.getInstance();
        worldHandler.addWorldListener(this);
        Simulation.initialize(worldHandler, new SimulationDelegateIDE());
        Simulation sim = Simulation.getInstance();
       
        // Build the class browser before building the menu, because
        // some menu actions work on the class browser.
        buildClassBrowser();
        setupActions();
        setJMenuBar(buildMenu());
        setGlassPane(DragGlassPane.getInstance());

        // build the centre panel. this includes the world and the controls
        
        JPanel centrePanel = new JPanel(new BorderLayout(4, 4));

        // the world panel. this includes the world title and world
        
        JPanel worldPanel = new JPanel(new BorderLayout());
        worldPanel.setBorder(BorderFactory.createEtchedBorder());        
        
        sim.addSimulationListener(new SimulationListener() {
            public void simulationChanged(SimulationEvent e)
            {
                // If the simulation starts, try to transfer keyboard
                // focus to the world canvas to allow control of Actors
                // via the keyboard
                if (e.getType() == SimulationEvent.STARTED) {
                    worldCanvas.requestFocusInWindow();
                }
            }
        });

        sim.addSimulationListener(SoundFactory.getInstance().getSoundCollection());
        
        // Panel that contains the border so that borders are not drawn on our
        // canvas, but just outside it.
        Box borderPanel = new Box(BoxLayout.LINE_AXIS);
        borderPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        borderPanel.add(worldCanvas);
        
        JPanel canvasPanel = new JPanel(new CenterLayout());        
        canvasPanel.add(borderPanel, BorderLayout.CENTER);
        JScrollPane worldScrollPane = new JScrollPane(canvasPanel);
        worldScrollPane.setOpaque(false);
        // Why are these not opaque? Maybe they have to be on some platforms? looks fine on Mac OS X Leopard.
        worldScrollPane.getViewport().setOpaque(false);
        worldScrollPane.setBorder(null);
        
       
        worldPanel.add(worldHandlerDelegate.getWorldTitle(), BorderLayout.NORTH);
        worldPanel.add(worldScrollPane, BorderLayout.CENTER);

        centrePanel.add(worldPanel, BorderLayout.CENTER);
        
        // the control panel
        
        controlPanel = new ControlPanel(sim, true);
        controlPanel.setBorder(BorderFactory.createEtchedBorder());

        centrePanel.add(controlPanel, BorderLayout.SOUTH);

        
        // EAST side: project info button and class browser
        
        JPanel eastPanel = new JPanel(new BorderLayout(12, 12));

        JButton readMeButton = GreenfootUtil.createButton(showReadMeAction); 
        readMeButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource(readMeIconFile)));
        eastPanel.add(readMeButton, BorderLayout.NORTH);
        
        // the class browser 
        
        classScrollPane = new JScrollPane(classBrowser);
        classScrollPane.setOpaque(false);
        classScrollPane.getViewport().setOpaque(false);
        classScrollPane.setBorder(BorderFactory.createEtchedBorder());
        eastPanel.add(classScrollPane, BorderLayout.CENTER);

        // the compile button at the bottom
        
        JButton button = GreenfootUtil.createButton(compileAllAction);
        button.setFocusable(false);
        // set the icon image: currently empty, but used to force same button look as readme button
        button.setIcon(new ImageIcon(getClass().getClassLoader().getResource(compileIconFile)));
        eastPanel.add(button, BorderLayout.SOUTH);

        
        // arrange the major components in the content pane
        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        contentPane.setLayout(new BorderLayout(12, 12));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        contentPane.add(centrePanel, BorderLayout.CENTER);
        contentPane.add(eastPanel, BorderLayout.EAST);

        pack();
        worldDimensions = worldCanvas.getPreferredSize();        
    }

    /**
     * Pack the components in this frame.
     * As part of this, try to make sure that the frame does not get too big.
     * If necessary, make it smaller to fit on screen.
     * 
     * <p>Call on event thread only.
     */
    public void pack()
    {
        super.pack();
        
        int width = getSize().width;
        int height = getSize().height;
        boolean change = false;
        
        if (width > getMaximumSize().width) {
            width = getMaximumSize().width;
            change = true;
        }
        if (height > getMaximumSize().height) {
            height = getMaximumSize().height;
            change = true;
        }
        if (change) {
            setSize(width, height);
        }
    }
    
    /**
     * Return the preferred size for the frame. The preferred size adds a bit of
     * spacing to the default size to get a margin around the world display.
     */
    public  Dimension getPreferredSize() {
        Dimension dim = super.getPreferredSize();
        dim.setSize(dim.width + WORLD_MARGIN, dim.height + WORLD_MARGIN);
        return dim;
    }

    /**
	 * Build a new (empty) class browser.
	 */
    private void buildClassBrowser()
    {
        classBrowser = new ClassBrowser(project, this);
        classBrowser.getSelectionManager().addSelectionChangeListener(this);
        DragGlassPane.getInstance().setSelectionManager(classBrowser.getSelectionManager());
    }

    /**
     * Read the classes from a given project and display them in the class browser.
     * <br>
     * Call only from the event thread.
     */
    private void populateClassBrowser(ClassBrowser classBrowser, GProject project)
    {
    	if (project != null) {
    		try {
    			GPackage pkg = project.getDefaultPackage();

    			GClass[] classes = pkg.getClasses();
    			//add the system classes
    			classBrowser.quickAddClass(new ClassView(classBrowser, new GCoreClass(World.class, project)));
    			classBrowser.quickAddClass(new ClassView(classBrowser, new GCoreClass(Actor.class, project)));
    			
    			for (int i = 0; i < classes.length; i++) {
    				GClass gClass = classes[i];
    				classBrowser.quickAddClass(new ClassView(classBrowser, gClass));
    			}

    			classBrowser.updateLayout();
    		}
    		catch (Exception exc) {
    			//Debug.reportError("Could not open classes in scenario", exc);
    			exc.printStackTrace();
    		}
    	}
    }

    private void setupActions()
    {
    	newClassAction = new NewClassAction(this);
    	saveProjectAction = new SaveProjectAction(this);
    	saveCopyAction = new SaveCopyAction(this);
    	showReadMeAction = new ShowReadMeAction(this);
    	exportProjectAction = new ExportProjectAction(this);
    	closeProjectAction = new CloseProjectAction(this);
    	removeSelectedClassAction = new RemoveSelectedClassAction(this);
    	removeSelectedClassAction.setEnabled(false);
    	compileAllAction = new CompileAllAction(project);
    }
    
    /**
     * Build the menu bar.
     */
    private JMenuBar buildMenu()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu projectMenu = addMenu(Config.getString("menu.scenario"), menuBar, 's');
        
        addMenuItem(NewProjectAction.getInstance(), projectMenu, -1, false, KeyEvent.VK_N);
        addMenuItem(OpenProjectAction.getInstance(), projectMenu, KeyEvent.VK_O, false, KeyEvent.VK_O);
        
        recentProjectsMenu = new JMenu(Config.getString("menu.openRecent"));
        projectMenu.add(recentProjectsMenu);
        updateRecentProjects();
        
        addMenuItem(closeProjectAction, projectMenu, KeyEvent.VK_W, false, KeyEvent.VK_C);
        addMenuItem(saveProjectAction, projectMenu, KeyEvent.VK_S, false, KeyEvent.VK_S);
        addMenuItem(saveCopyAction, projectMenu, -1, false, -1);
        projectMenu.addSeparator();
        addMenuItem(exportProjectAction, projectMenu, KeyEvent.VK_E, false, KeyEvent.VK_E);

//        addMenuItem(new NYIAction("Save As...", this), projectMenu, KeyEvent.VK_S, true, -1);
//        projectMenu.addSeparator();
//        addMenuItem(new NYIAction("Page Setup...", this), projectMenu, KeyEvent.VK_P, true, -1);
//        addMenuItem(new NYIAction("Print...", this), projectMenu, KeyEvent.VK_P, false, KeyEvent.VK_P);
        if(! Config.isMacOS()) {
            projectMenu.addSeparator();
            addMenuItem(QuitAction.getInstance(), projectMenu, KeyEvent.VK_Q, false, KeyEvent.VK_Q);
        }
        
        JMenu editMenu = addMenu(Config.getString("menu.edit"), menuBar, 'e');
        
        addMenuItem(newClassAction, editMenu, KeyEvent.VK_N, false, KeyEvent.VK_N);
        addMenuItem(removeSelectedClassAction, editMenu, KeyEvent.VK_D, false, KeyEvent.VK_R);
        
        JMenu ctrlMenu = addMenu(Config.getString("menu.controls"), menuBar, 'c');
        
        addMenuItem(RunOnceSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_A, false, KeyEvent.VK_A);
        addMenuItem(RunSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_R, false, KeyEvent.VK_R);
        addMenuItem(PauseSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_R, true, KeyEvent.VK_P);
        addMenuItem(ResetWorldAction.getInstance(), ctrlMenu, KeyEvent.VK_T, false, KeyEvent.VK_T);
//        addMenuItem(new NYIAction("Increase Speed", this), ctrlMenu, KeyEvent.VK_PLUS, false, KeyEvent.VK_PLUS);
//        addMenuItem(new NYIAction("Decrease Speed", this), ctrlMenu, KeyEvent.VK_MINUS, false, KeyEvent.VK_MINUS);
        ctrlMenu.addSeparator();
        addMenuItem(compileAllAction, ctrlMenu, KeyEvent.VK_K, false, -1);
        
        JMenu helpMenu = addMenu(Config.getString("menu.help"), menuBar, 'h');
        
        if(! Config.isMacOS()) {
            addMenuItem(AboutGreenfootAction.getInstance(this), helpMenu, -1, false, KeyEvent.VK_A);
        }
        addMenuItem(ShowCopyrightAction.getInstance(this), helpMenu, -1, false, -1);
        helpMenu.addSeparator();
        addMenuItem(new ShowApiDocAction(Config.getString("menu.help.classDoc")), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.javadoc"), Config.getPropString("greenfoot.url.javaStdLib")), helpMenu, -1, false, -1);
        helpMenu.addSeparator();
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.tutorial"), Config.getPropString("greenfoot.url.tutorial")), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.website"), Config.getPropString("greenfoot.url.greenfoot")), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction(Config.getString("menu.help.moreScenarios"), Config.getPropString("greenfoot.url.scenarios")), helpMenu, -1, false, -1);
        helpMenu.addSeparator();
        addMenuItem(new ShowWebsiteAction(Config.getPropString("greenfoot.gameserver.name"), Config.getPropString("greenfoot.gameserver.address")), helpMenu, -1, false, -1);
        
        return menuBar;
    }

    /** 
     * Add a menu to a menu bar.
     */
    private JMenu addMenu(String name, JMenuBar menubar, char mnemonic)
    {
        JMenu menu = new JMenu(name);
        if(!Config.isMacOS())
            menu.setMnemonic(mnemonic);
        menubar.add(menu);
        return menu;
    }

    /** 
     * Add a menu item to a menu.
     */
    private void addMenuItem(Action action, JMenu menu, int accelKey, boolean shift, int mnemonicKey)
    {
        if(accelKey != -1) {
            if(shift)
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelKey, shiftAccelModifier));
            else
                action.putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(accelKey, accelModifier));
        }
        if(!Config.isMacOS() && mnemonicKey != -1)
            action.putValue(Action.MNEMONIC_KEY, Integer.valueOf(mnemonicKey));
        menu.add(action);
    }
    
    /**
     * Update the 'Open Recent' menu
     */
    private void updateRecentProjects()
    {
        List<?> projects = PrefMgr.getRecentProjects();
        for (Iterator<?> it = projects.iterator(); it.hasNext();) {
            JMenuItem item = new JMenuItem((String)it.next());
            item.addActionListener(OpenRecentProjectAction.getInstance());
            recentProjectsMenu.add(item);
        }
    }


    /**
     * Enable/disable the project specific actions, depending on whether a
     * project is currently open.
     */
    private void enableProjectActions() 
    {
    	boolean state = (project != null);
    	
        closeProjectAction.setEnabled(state);
        saveProjectAction.setEnabled(state);
        saveCopyAction.setEnabled(state);
        newClassAction.setEnabled(state);
        showReadMeAction.setEnabled(state);
        exportProjectAction.setEnabled(state);
        
        // Disable simulation buttons
        if (state == false) {
        	WorldHandler.getInstance().discardWorld();
            removeSelectedClassAction.setEnabled(false);
        }
        
        compileAllAction.setProject(project);
    }

    /**
     * Quit Greenfoot.
     */
    private void exit()
    {
        super.dispose();
        GreenfootMain.closeAll();
    }

    /**
     * This frame should never be disposed (at least not until the program is
     * closed). BlueJ disposes all windows when compiling, so dispose is
     * overridden to avoid it for this frame. Be aware of this when it should
     * really shut down!
     * 
     * @see java.awt.Window#dispose()
     * @see #exit()
     */
    public void dispose()
    {
        // I will not close :-)
    }

    /**
     * Returns the maximum size, which is the size of the screen.
     */
    public Dimension getMaximumSize()
    {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }
    
    /**
     * Returns true if we need to resize the frame. Based on whether the world
     * has changed size or we have specifically asked for a resize by setting resizeWhenPossible.
     * 
     * @see #setResizeWhenPossible(boolean)
     * @return true, if we need a resize.
     */
    private boolean needsResize()
    {
        Dimension dim = worldCanvas.getPreferredSize();
        if (resizeWhenPossible) {
            return true;
        }
        else if (worldDimensions == null) {
            // If the worldDimensions are null here, it means that we set the
            // size specifically when we created the frame.
            return false;
        }
        else if (!dim.equals(worldDimensions)) {
            return true;
        }
        else {
            return false;
        }
    }
    
    /**
     * Resizes the frame to its preferred size by running pack().
     * <p>
     * Should be run on the event thread.
     */
    private void resize() {
        setResizeWhenPossible(false);
        pack();
    }

    /**
     * Indicate whether we want to resize the next time we get new information
     * about the size, and hence might want to do a resize of the entire frame.
     */
    public void setResizeWhenPossible(boolean b)
    {
        worldDimensions = null;
        this.resizeWhenPossible = b;
    }
    
    // ----------- WindowListener interface -----------
    
    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
        GreenfootMain.closeProject(this, true);
    }

    public void windowClosed(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowActivated(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    // ----------- CompileListener interface -----------
    
    public void compileStarted(RCompileEvent event)
    {
        WorldHandler.getInstance().discardWorld();
    }

    public void compileError(RCompileEvent event)
    {

    }

    public void compileWarning(RCompileEvent event)
    {

    }

    public void compileSucceeded(RCompileEvent event)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                WorldHandler.getInstance().instantiateNewWorld();
                classBrowser.repaint();
                compileAllAction.setEnabled(project != null);
            }
        });
    }

    public void compileFailed(RCompileEvent event)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                compileAllAction.setEnabled(project != null);
            }
        });
    }
    
    // ----------- end of WindowListener interface -----------
    
    // ----------- WorldListener interface -------------
    
    public void worldCreated(WorldEvent e)
    {
        World newWorld = worldHandler.getWorld();
        if (needsResize() && newWorld != null) {
            resize();
        }
        worldDimensions = worldCanvas.getPreferredSize();
    }
    
    public void worldRemoved(WorldEvent e)
    {
        removeAllInspectors();
    }

    // ------------- end of WorldListener interface ------------
    
    // ------------- SelectionListener interface ---------------
    
    public void selectionChange(Selectable source)
    {
    	if (source instanceof ClassView) {
    		removeSelectedClassAction.setEnabled(true);
    	}
    	else {
    		removeSelectedClassAction.setEnabled(false);
    	}
    }
    
    // ------------- end of SelectionListener interface --------
    
    // ------------- InspectorManager interface ----------------
    
    public ClassInspector getClassInspectorInstance(DebuggerClass clss, Package pkg, JFrame parent)
    {
        ClassInspector inspector = (ClassInspector) classInspectors.get(clss.getName());

        if (inspector == null) {
            ClassInspectInvokerRecord ir = new ClassInspectInvokerRecord(clss.getName());
            inspector = new GreenfootClassInspector(clss, this, pkg, ir, parent);
            classInspectors.put(clss.getName(), inspector);
        }

        final Inspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    insp.update();
                    insp.updateLayout();
                    insp.setVisible(true);
                    insp.bringToFront();
                }
            });

        return inspector;
    }
    
    public ObjectInspector getInspectorInstance(DebuggerObject obj, String name, Package pkg, InvokerRecord ir, JFrame parent)
    {
        ObjectInspector inspector = (ObjectInspector) objectInspectors.get(obj);
        
        if (inspector == null) {
            inspector = new GreenfootObjectInspector(obj, this, name, pkg, ir, parent);
            objectInspectors.put(obj, inspector);
        }
        
        final ObjectInspector insp = inspector;
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                insp.update();
                insp.updateLayout();
                insp.setVisible(true);
                insp.bringToFront();
            }
        });
        
        return inspector;
    }
    
    public ResultInspector getResultInspectorInstance(DebuggerObject obj, String name, Package pkg, InvokerRecord ir, ExpressionInformation info, JFrame parent)
    {
        ResultInspector inspector = (ResultInspector) objectInspectors.get(obj);
        
        if (inspector == null) {
            inspector = new GreenfootResultInspector(obj, this, name, pkg, ir, info, parent);
            objectInspectors.put(obj, inspector);
        }

        final ResultInspector insp = inspector;
        insp.update();
        insp.updateLayout();
        EventQueue.invokeLater(new Runnable() {
                public void run() {
                    insp.setVisible(true);
                    insp.bringToFront();
        }
            });

        return inspector;
    }
    
    public boolean inTestMode()
    {
        //Greenfoot does not support testing:
        return false;
    }
    
    public void removeInspector(DebuggerClass cls)
    {
        classInspectors.remove(cls.getName());
    }
    
    public void removeInspector(DebuggerObject obj)
    {
        objectInspectors.remove(obj);
    }

    // ------------- end of InspectorManager interface ---------

    /**
     * Removes all inspector instances for this project.
     * This is used when VM is reset or the project is recompiled.
     */
    public void removeAllInspectors()
    {
        for (Iterator<Inspector> it = objectInspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = (Inspector) it.next();
            inspector.setVisible(false);
            inspector.dispose();
        }
        objectInspectors.clear();
        
        for (Iterator<Inspector> it = classInspectors.values().iterator(); it.hasNext();) {
            Inspector inspector = (Inspector) it.next();
            inspector.setVisible(false);
            inspector.dispose();
        }
        classInspectors.clear();
    }
}
