package greenfoot.gui;

import greenfoot.World;
import greenfoot.actions.AboutGreenfootAction;
import greenfoot.actions.CloseProjectAction;
import greenfoot.actions.CompileAllAction;
import greenfoot.actions.CompileClassAction;
import greenfoot.actions.EditClassAction;
import greenfoot.actions.NYIAction;
import greenfoot.actions.NewClassAction;
import greenfoot.actions.NewProjectAction;
import greenfoot.actions.OpenProjectAction;
import greenfoot.actions.PauseSimulationAction;
import greenfoot.actions.RemoveSelectedClassAction;
import greenfoot.actions.RunOnceSimulationAction;
import greenfoot.actions.RunSimulationAction;
import greenfoot.actions.SaveProjectAction;
import greenfoot.actions.ShowCopyrightAction;
import greenfoot.actions.ShowReadMeAction;
import greenfoot.actions.ShowWebsiteAction;
import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.GProject;
import greenfoot.core.GreenfootMain;
import greenfoot.core.LocationTracker;
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
import greenfoot.gui.classbrowser.SelectionManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.HeadlessException;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Iterator;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.Config;
import bluej.extensions.ProjectNotOpenException;
import bluej.utility.Debug;

import com.apple.eawt.Application;
import com.apple.eawt.ApplicationAdapter;
import com.apple.eawt.ApplicationEvent;

/**
 * The main frame for a Greenfoot project (one per project)
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @author mik
 *
 * @version $Id: GreenfootFrame.java 4178 2006-05-09 20:24:57Z mik $
 */
public class GreenfootFrame extends JFrame
    implements WindowListener, CompileListener, WorldListener
{
    private static final String readMeIconFile = "readme.png";
    private static final String compileIconFile = "compile.png";
    private static final int WORLD_MARGIN = 40;

    private static final int accelModifier = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    private static final int shiftAccelModifier = accelModifier | KeyEvent.SHIFT_MASK;

    private WorldCanvas worldCanvas;
    private WorldHandler worldHandler;
    private Dimension worldDimensions;
    private ClassBrowser classBrowser;
    private ControlPanel controlPanel;
    
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
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run()
                {
                    try {
                        instance = new GreenfootFrame(blueJ);                        
                    }
                    catch (ProjectNotOpenException pnoe) {
                        pnoe.printStackTrace();
                    }
                    catch (RemoteException re) {
                        re.printStackTrace();
                    }
                }
            });
            return instance;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a new top level frame with all the GUI components.
     */
    private GreenfootFrame(RBlueJ blueJ)
        throws HeadlessException, ProjectNotOpenException, RemoteException
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
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        LocationTracker.instance(); //force initialisation
        
        URL iconFile = this.getClass().getClassLoader().getResource("greenfoot-icon.gif");
        ImageIcon icon = new ImageIcon(iconFile);
        setIconImage(icon.getImage());

        makeFrame();
        addWindowListener(this);
        GreenfootMain.getInstance().addCompileListener(this);
        
        restoreFrameState();

        prepareMacOSApp();
        
        setVisible(true);
    }
    
    /**
     * Restore the current main window size from the project properties.
     */
    private void restoreFrameState()
    {
        ProjectProperties projectProperties = GreenfootMain.getProjectProperties();

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
            setBounds(40, 40, 600, 500);
            setResizeWhenPossible(true);
        }
        
        try {
            int speed = projectProperties.getInt("simulation.speed");
            Simulation.getInstance().setSpeed(speed);
        } 
        catch (NumberFormatException ecx) {
            //simulation.speed not found
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
               // PreferencesAction.getInstance().actionPerformed(getMostRecent());
               // e.setHandled(true);
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
     * 
     */
    public void openProject(final GProject project)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                setTitle("Greenfoot: " + project.getName());
                populateClassBrowser(classBrowser, project);
                enableProjectActions();
                instantiateNewWorld(classBrowser);
            }
        });
    }    
    
    /**
     * Create the GUI components for this project in the top level frame.
     * This includes opening the project and displaying the project classes.
     */
    private void makeFrame()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        // Build the class browser before building the menu, because
        // some menu actions work on the class browser.
        classBrowser = buildClassBrowser();
        setJMenuBar(buildMenu());
        setGlassPane(DragGlassPane.getInstance());

        // build the centre panel. this includes the world and the controls
        
        JPanel centrePanel = new JPanel(new BorderLayout(4, 4));

        // the world panel. this includes the world title and world
        
        JPanel worldPanel = new JPanel(new BorderLayout());
        worldPanel.setBorder(BorderFactory.createEtchedBorder());        

        worldCanvas = new WorldCanvas(null);
        worldCanvas.setBorder(BorderFactory.createLineBorder(Color.BLACK));
        
        WorldHandler.initialise(worldCanvas);
        worldHandler = WorldHandler.getInstance();
        worldHandler.addWorldListener(this);
        Simulation.initialize(worldHandler);
        Simulation sim = Simulation.getInstance();
        
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
        
        JPanel canvasPanel = new JPanel(new CenterLayout());
        canvasPanel.add(worldCanvas, BorderLayout.CENTER);
        JScrollPane worldScrollPane = new JScrollPane(canvasPanel);
        worldScrollPane.setOpaque(false);
        worldScrollPane.getViewport().setOpaque(false);
        worldScrollPane.setBorder(null);
        
        worldPanel.add(worldHandler.getWorldTitle(), BorderLayout.NORTH);
        worldPanel.add(worldScrollPane, BorderLayout.CENTER);

        centrePanel.add(worldPanel, BorderLayout.CENTER);
        
        // the control panel
        
        controlPanel = new ControlPanel(sim);
        controlPanel.setBorder(BorderFactory.createEtchedBorder());

        centrePanel.add(controlPanel, BorderLayout.SOUTH);

        
        // EAST side: project info button and class browser
        
        JPanel eastPanel = new JPanel(new BorderLayout(12, 12));

        JButton readMeButton = new JButton(ShowReadMeAction.getInstance()); 
        readMeButton.setIcon(new ImageIcon(getClass().getClassLoader().getResource(readMeIconFile)));
        eastPanel.add(readMeButton, BorderLayout.NORTH);
        
        // the class browser 
        
        JScrollPane classScrollPane = new JScrollPane(classBrowser);
        classScrollPane.setOpaque(false);
        classScrollPane.getViewport().setOpaque(false);
        classScrollPane.setBorder(BorderFactory.createEtchedBorder());
        eastPanel.add(classScrollPane, BorderLayout.CENTER);

        // the compile button at the bottom
        
        JButton button = new JButton(CompileAllAction.getInstance());
        // set the icon image: currently empty, but used to force same button look as readme button
        button.setIcon(new ImageIcon(getClass().getClassLoader().getResource(compileIconFile)));
        eastPanel.add(button, BorderLayout.SOUTH);

        
        // arrange the major components in the content pane
        
        JPanel contentPane = (JPanel)getContentPane();
        contentPane.setLayout(new BorderLayout(12, 12));
        contentPane.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        contentPane.add(centrePanel, BorderLayout.CENTER);
        contentPane.add(eastPanel, BorderLayout.EAST);

        pack();
        worldDimensions = worldCanvas.getPreferredSize();
        
        worldHandler.setSelectionManager(classBrowser.getSelectionManager());
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
     * Tries to instantiate a new world. This may fail (if, for example, the
     * world is not compiled. If multiple world classes exist, a random one
     * will be instantiated.
     */
    private void instantiateNewWorld(ClassBrowser classBrowser)
    {
        //init a random world
        Iterator worldClasses = classBrowser.getWorldClasses();

        while (worldClasses.hasNext()) {
            ClassView classView = (ClassView) worldClasses.next();
            if (!classView.getClassName().equals("World")) {
                classView.reloadClass();
                classView.createInstance();
            }
        }
    }


    /**
     * Build the class browser.
     */
    private ClassBrowser buildClassBrowser()
    {
        ClassBrowser classBrowser = new ClassBrowser();
        SelectionManager selectionManager = classBrowser.getSelectionManager();
        selectionManager.addSelectionChangeListener(CompileClassAction.getInstance());
        selectionManager.addSelectionChangeListener(EditClassAction.getInstance());

        return classBrowser;
    }

    /**
     * Read the classes from a given project and display them in the class browser.
     */
    private void populateClassBrowser(ClassBrowser classBrowser, GProject project)
    {
        try {
            GPackage pkg = project.getDefaultPackage();

            GClass[] classes = pkg.getClasses();
            //add the system classes
            GPackage sysPkg = project.getGreenfootPackage();
            if (sysPkg == null) {
                sysPkg = project.newPackage("greenfoot");
            }

            GClass[] gClasses = sysPkg.getClasses();
            for (int i = 0; i < gClasses.length; i++) {
                GClass gClass = gClasses[i];
                classBrowser.quickAddClass(gClass);
            }

            for (int i = 0; i < classes.length; i++) {
                GClass gClass = classes[i];
                classBrowser.quickAddClass(gClass);
            }
            
            classBrowser.updateLayout();
            classBrowser.revalidate();
        }
        catch (Exception exc) {
            Debug.reportError("Could not open classes in project", exc);
        }
    }

    /**
     * Build the menu bar.
     */
    private JMenuBar buildMenu()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu projectMenu = addMenu("Project", menuBar, 'p');
        
        addMenuItem(NewProjectAction.getInstance(), projectMenu, -1, false, KeyEvent.VK_N);
        addMenuItem(OpenProjectAction.getInstance(), projectMenu, KeyEvent.VK_O, false, KeyEvent.VK_O);
//        addMenuItem(new NYIAction("Open Recent...", this), projectMenu, -1, false, -1);
        addMenuItem(CloseProjectAction.getInstance(), projectMenu, KeyEvent.VK_W, false, KeyEvent.VK_C);
        addMenuItem(SaveProjectAction.getInstance(), projectMenu, KeyEvent.VK_S, false, KeyEvent.VK_S);
        addMenuItem(new NYIAction("Save As...", this), projectMenu, KeyEvent.VK_S, true, -1);
        projectMenu.addSeparator();
        addMenuItem(new NYIAction("Page Setup...", this), projectMenu, KeyEvent.VK_P, true, -1);
        addMenuItem(new NYIAction("Print...", this), projectMenu, KeyEvent.VK_P, false, KeyEvent.VK_P);
        
        JMenu editMenu = addMenu("Edit", menuBar, 'e');
        
        addMenuItem(NewClassAction.getInstance(classBrowser), editMenu, KeyEvent.VK_N, false, KeyEvent.VK_N);
        RemoveSelectedClassAction removeClassAction = RemoveSelectedClassAction.getInstance();
        classBrowser.getSelectionManager().addSelectionChangeListener(removeClassAction);
        addMenuItem(removeClassAction, editMenu, KeyEvent.VK_D, false, KeyEvent.VK_R);
        
        JMenu ctrlMenu = addMenu("Controls", menuBar, 'c');
        
        addMenuItem(RunOnceSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_A, false, KeyEvent.VK_A);
        addMenuItem(RunSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_R, false, KeyEvent.VK_R);
        addMenuItem(PauseSimulationAction.getInstance(), ctrlMenu, KeyEvent.VK_R, true, KeyEvent.VK_P);
        addMenuItem(new NYIAction("Increase Speed", this), ctrlMenu, KeyEvent.VK_PLUS, false, KeyEvent.VK_PLUS);
        addMenuItem(new NYIAction("Decrease Speed", this), ctrlMenu, KeyEvent.VK_MINUS, false, KeyEvent.VK_MINUS);
        ctrlMenu.addSeparator();
        addMenuItem(CompileAllAction.getInstance(), ctrlMenu, KeyEvent.VK_K, false, -1);
        
        JMenu helpMenu = addMenu("Help", menuBar, 'h');
        
        addMenuItem(AboutGreenfootAction.getInstance(this), helpMenu, -1, false, KeyEvent.VK_A);
        addMenuItem(ShowCopyrightAction.getInstance(this), helpMenu, -1, false, -1);
        helpMenu.addSeparator();
        addMenuItem(new ShowWebsiteAction("Greenfoot Web Site", "http://www.greenfoot.org"), helpMenu, -1, false, -1);
        addMenuItem(new ShowWebsiteAction("Greenfoot Tutorial", "http://www.greenfoot.org/doc/tutorial.html"), helpMenu, -1, false, -1);
        
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
            action.putValue(Action.MNEMONIC_KEY, new Integer(mnemonicKey));
        menu.add(action);
    }


    /**
     * Enable the actions that were disabled when no project is open.
     */
    private void enableProjectActions() 
    {
        CloseProjectAction.getInstance().setEnabled(true);
        SaveProjectAction.getInstance().setEnabled(true);
        NewClassAction.getInstance().setEnabled(true);
        RemoveSelectedClassAction.getInstance().setEnabled(true);
        CompileAllAction.getInstance().setEnabled(true);
        ShowReadMeAction.getInstance().setEnabled(true);
        
        controlPanel.setSliderEnabled(true);
    }

    /**
     * Close this project.
     */
    private void exit()
    {
        super.dispose();
        GreenfootMain.getInstance().closeThisInstance();
    }


    /**
     * 
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

    // ----------- WindowListener interface -----------
    
    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
        exit();
    }

    public void windowClosed(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowActivated(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    // ----------- CompileListener interface -----------
    
    public void compileStarted(RCompileEvent event)
    {        
        WorldHandler.getInstance().reset();
    }

    public void compileError(RCompileEvent event)
    {

    }

    public void compileWarning(RCompileEvent event)
    {

    }

    public void compileSucceeded(RCompileEvent event)
    {
        instantiateNewWorld(classBrowser);
        classBrowser.repaint();
    }

    public void compileFailed(RCompileEvent event)
    {}

    
    // ----------- end of WindowListener interface -----------
    
    // ----------- WorldListener interface -------------
    
    public void worldCreated(WorldEvent e)
    {
        // TODO Auto-generated method stub
        // from openProject
        World newWorld = worldHandler.getWorld();
        if (needsResize() && newWorld != null) {
            resize();
        }

    }
    
    public void worldRemoved(WorldEvent e)
    {
        // TODO Auto-generated method stub
        
    }
    
    // ------------- end of WorldListener interface ------------
    
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
        worldDimensions = worldCanvas.getPreferredSize();
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
}