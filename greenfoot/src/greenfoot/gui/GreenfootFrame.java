package greenfoot.gui;

import greenfoot.Greenfoot;
import greenfoot.GreenfootWorld;
import greenfoot.Simulation;
import greenfoot.WorldHandler;
import greenfoot.actions.CompileAllAction;
import greenfoot.actions.CompileClassAction;
import greenfoot.actions.EditClassAction;
import greenfoot.actions.NewProjectAction;
import greenfoot.actions.OpenProjectAction;
import greenfoot.event.CompileListener;
import greenfoot.gui.classbrowser.ClassBrowser;
import greenfoot.gui.classbrowser.ClassView;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.border.Border;

import rmiextension.wrappers.RBlueJ;
import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.RProject;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.Config;
import bluej.extensions.PackageAlreadyExistsException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;

/**
 * The main frame of the greenfoot application
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: GreenfootFrame.java 3165 2004-11-25 02:07:14Z davmac $
 */
public class GreenfootFrame extends JFrame
    implements WindowListener, CompileListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");

    private RBlueJ blueJ;
    private CompileClassAction compileClassAction = new CompileClassAction("Compile");
    private EditClassAction editClassAction = new EditClassAction("Edit");
    private ClassBrowser classBrowser;
    private WorldHandler worldHandler;
    private JSplitPane splitPane;
    private final static Dimension MAX_SIZE = new Dimension(800, 600);

    /**
     * Creates a new frame with all the basic components (menus...)
     *  
     */
    public GreenfootFrame(RBlueJ blueJ, RProject project)
        throws HeadlessException, ProjectNotOpenException, RemoteException
    {
        super("greenfoot:" + project.getName());
        try {
            //HACK to avoid error in class diagram (getPreferredSize stuff) on
            // windows, we use cross platform look and feel
            if (Config.isWinOS()) {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
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
        this.blueJ = blueJ;
        setSize(400, 300);
        buildUI();
        addWindowListener(this);
        Greenfoot.getInstance().addCompileListener(this);
        openProject(project);

    }
    
    public WorldHandler getWorldHandler()
    {
        return worldHandler;
    }

    private void buildUI()
    {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setLayout(new BorderLayout());
        setJMenuBar(buildMenu());
        setGlassPane(DragGlassPane.getInstance());
    }

    private void openProject(RProject project)
    {
        classBrowser = buildClassBrowser();
        classBrowser.setBackground(Color.WHITE);
        JScrollPane classScrollPane = new JScrollPane(classBrowser);
        worldHandler = buildWorld();
        Simulation.initialize(worldHandler);
        Simulation sim = Simulation.getInstance();
        JScrollPane worldScrollPane = new JScrollPane(worldHandler.getWorldCanvas());
        JPanel worldPanel = new JPanel(new BorderLayout(4, 4));
        Border empty = BorderFactory.createEmptyBorder(4, 4, 4, 4);
        worldPanel.setBorder(empty);

        ControlPanel controlPanel = new ControlPanel(sim);
        controlPanel.setBorder(BorderFactory.createEtchedBorder());
        controlPanel.addChangeListener(sim);

        worldHandler.setDelay(controlPanel.getDelay());

        //todo this should be moved to WorldCanvas becuase it changes when new
        // worlds are created
        worldPanel.add(worldHandler.getWorldTitle(), BorderLayout.NORTH);
        worldPanel.add(worldScrollPane, BorderLayout.CENTER);
        worldPanel.add(controlPanel, BorderLayout.SOUTH);

        JPanel rightPane = new JPanel(new BorderLayout());
        rightPane.add(classScrollPane, BorderLayout.CENTER);
        Box buttonPanel = new Box(BoxLayout.Y_AXIS);
        buttonPanel.setAlignmentX(Box.CENTER_ALIGNMENT);

        JButton button = new JButton(new CompileAllAction("Compile"));
        Dimension pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
        buttonPanel.add(button);

        buttonPanel.add(Box.createVerticalStrut(5));

        button = new JButton("New Class...");
        pref = button.getMinimumSize();
        pref.width = Integer.MAX_VALUE;
        button.setMaximumSize(pref);
        buttonPanel.add(button);

        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        rightPane.add(buttonPanel, BorderLayout.SOUTH);

        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true);
        splitPane.setLeftComponent(worldPanel);
        splitPane.setRightComponent(rightPane);
        splitPane.setResizeWeight(.9);
        splitPane.resetToPreferredSizes();

        getContentPane().add(splitPane, BorderLayout.CENTER);

        instantiateNewWorld(classBrowser);
        pack();

        worldHandler.setSelectionManager(classBrowser.getSelectionManager());

    }

    public void pack()
    {
        super.pack();
        splitPane.resetToPreferredSizes();
        super.pack();
        int width = getSize().width;
        int height = getSize().height;

        if (width > getMaximumSize().width) {
            width = getMaximumSize().width;
        }
        if (height > getMaximumSize().height) {
            height = getMaximumSize().height;
        }
        setSize(width, height);
        this.validateTree();
    }

    /**
     * Tries to instantiate a new world
     * 
     * @param classBrowser
     */
    private GreenfootWorld instantiateNewWorld(ClassBrowser classBrowser)
    {
        //init a random world
        Iterator worldClasses = classBrowser.getWorldClasses();

        while (worldClasses.hasNext()) {
            ClassView classView = (ClassView) worldClasses.next();
            if (!classView.getClassName().equals("GreenfootWorld")) {
                Object o = classView.createInstance();
                if (o instanceof GreenfootWorld) {
                    GreenfootWorld world = (GreenfootWorld) o;
                    if (world != null) {
                        worldHandler.setWorld(world);
                    }
                    return world;
                }
            }
        }

        return null;
    }

    /**
     *  
     */
    private WorldHandler buildWorld()
    {
        GreenfootWorld world = null;
        WorldCanvas worldCanvas = new WorldCanvas(world);

        WorldHandler worldHandler = new WorldHandler(worldCanvas, world);
        return worldHandler;
    }

    private ClassBrowser buildClassBrowser()
    {
        ClassBrowser classBrowser = new ClassBrowser();
        classBrowser.addCompileClassAction(compileClassAction);
        classBrowser.addEditClassAction(editClassAction);

        RPackage pkg;
        try {
            //pkg = Greenfoot.getInstance().getCurrentPackage();
            pkg = Greenfoot.getInstance().getPackage();
            //	TODO when project is empty (a new project) the systemclasses get
            // loaded twice
            pkg = pkg.getProject().getPackage("");

            RClass[] classes = pkg.getRClasses();
            //add the system classes
            RProject prj = pkg.getProject();

            RPackage sysPkg = prj.getPackage("greenfoot");
            if (sysPkg == null) {
                sysPkg = prj.newPackage("greenfoot");
            }

            RClass[] rClasses = sysPkg.getRClasses();
            for (int i = 0; i < rClasses.length; i++) {
                RClass rClass = rClasses[i];
                classBrowser.addClass(rClass);
            }

            for (int i = 0; i < classes.length; i++) {
                RClass rClass = classes[i];
                classBrowser.addClass(rClass);
            }

        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        catch (PackageAlreadyExistsException e) {
            e.printStackTrace();
        }

        Border insideBorder = BorderFactory.createEmptyBorder(3, 3, 3, 3);
        classBrowser.setBorder(insideBorder);
        return classBrowser;
    }

    private JMenuBar buildMenu()
    {
        JMenuBar menuBar = new JMenuBar();

        JMenu projectMenu = new JMenu("Project");
        menuBar.add(projectMenu);

        projectMenu.add(new NewProjectAction("New"));
        projectMenu.add(new OpenProjectAction("Open"));

        JMenu classMenu = new JMenu("Class");
        menuBar.add(classMenu);
        classMenu.add(compileClassAction);
        classMenu.add(editClassAction);

        return menuBar;
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
    //I will not close :-)
    }

    public void windowOpened(WindowEvent e)
    {}

    public void windowClosing(WindowEvent e)
    {
        logger.info("WindowClosing");
        exit();
    }

    private void exit()
    {
        super.dispose();
        Greenfoot.getInstance().closeThisInstance();
    }

    public void windowClosed(WindowEvent e)
    {
        logger.info("WindowClosed");
    }

    public void windowIconified(WindowEvent e)
    {}

    public void windowDeiconified(WindowEvent e)
    {}

    public void windowActivated(WindowEvent e)
    {}

    public void windowDeactivated(WindowEvent e)
    {}

    public void compileStarted(RCompileEvent event)
    {
        worldHandler.setWorld(null);
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
    }

    public void compileFailed(RCompileEvent event)
    {}

    public Dimension getMaximumSize()
    {
        return MAX_SIZE;
    }
}