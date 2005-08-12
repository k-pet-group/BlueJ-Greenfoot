package greenfoot.gui.classbrowser;

import greenfoot.WorldInvokeListener;
import greenfoot.actions.NewSubclassAction;
import greenfoot.event.CompileListener;
import greenfoot.gui.classbrowser.role.ClassRole;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import rmiextension.wrappers.RClass;
import rmiextension.wrappers.RPackage;
import rmiextension.wrappers.event.RCompileEvent;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.prefmgr.PrefMgr;
import bluej.runtime.ExecServer;
import bluej.utility.Utility;
import bluej.views.ConstructorView;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassView.java 3512 2005-08-12 14:03:24Z polle $
 */
public class ClassView extends JToggleButton
    implements ChangeListener, Selectable, CompileListener, MouseListener
{
    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private RClass remoteClass;
    private Class realClass; // null if not compiled
    private ClassRole role;
    private final static int SHADOW = 2;
    private final static int SELECTED_BORDER = 3;
    private final static int BORDER = 1;
    private ClassBrowser classBrowser;
    public static final String newline = System.getProperty("line.separator");
    private static String imports = "import greenfoot.GreenfootWorld;" + newline + "import greenfoot.GreenfootObject;"
            + newline;
    private JPopupMenu popupMenu;

    public ClassView(ClassRole role, RClass rClass)
    {
        this.remoteClass = rClass;
        realClass = getClass(rClass);
        setRole(role);
        addChangeListener(this);
        this.setOpaque(false);
        try {
            logger.info("Creating view: " + role + " for " + rClass.getQualifiedName());
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        this.addMouseListener(this);
        this.setBorder(BorderFactory.createEmptyBorder(SELECTED_BORDER + 2, SELECTED_BORDER + 2, SELECTED_BORDER
                + SHADOW + 2, SELECTED_BORDER + SHADOW + 2));

    }

    public Class getRealClass()
    {
        return realClass;
    }

    /**
     * Get the real class that rClass represents.
     * 
     * @param class1
     * @return null if the class can't be loaded
     */
    private Class getClass(RClass rClass)
    {
        Class cls = null;
        try {
            if (!rClass.isCompiled()) {
                return cls;
            }
        }
        catch (ProjectNotOpenException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (PackageNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        try {
            String className = rClass.getQualifiedName();
            //it is important that we use the right classloader
            cls = ExecServer.loadAndInitClass(className);
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (NoClassDefFoundError e) {
            e.printStackTrace();
        }
        return cls;
    }

    void setClassBrowser(ClassBrowser classBrowser)
    {
        this.classBrowser = classBrowser;
        createPopupMenu();
    }

    public RClass getRClass()
    {
        return remoteClass;
    }

    private void createPopupMenu()
    {
        try {
            logger.info("Creating new popup for: " + remoteClass.getQualifiedName());
        }
        catch (RemoteException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }

        if (popupMenu != null) {
            remove(popupMenu);
        }
        // popupMenu = menu.getPopupMenu();
        popupMenu = new JPopupMenu();

        if (realClass != null) {

            if (!java.lang.reflect.Modifier.isAbstract(realClass.getModifiers())) {
                List constructorItems = role.createConstructorActions(realClass);

                boolean hasEntries = false;
                for (Iterator iter = constructorItems.iterator(); iter.hasNext();) {
                    Action callAction = (Action) iter.next();
                    JMenuItem item = popupMenu.add(callAction);
                    item.setFont(PrefMgr.getPopupMenuFont());
                    hasEntries = true;
                }

                if (hasEntries) {
                    popupMenu.addSeparator();
                }
            }

            ViewFilter filter = new ViewFilter(ViewFilter.STATIC | ViewFilter.PROTECTED);
            View view = View.getView(realClass);
            MethodView[] allMethods = view.getAllMethods();
            WorldInvokeListener invocListener = new WorldInvokeListener(realClass);

            if (bluej.pkgmgr.target.role.ClassRole.createMenuItems(popupMenu, allMethods, filter, "", invocListener))
                popupMenu.addSeparator();
        }

        popupMenu.setInvoker(this);

        popupMenu.add(new NewSubclassAction("New subclass", this, classBrowser));
        // popupMenu.insert( JPopupMenu.);
        add(popupMenu);
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e)
            {
                maybeShowPopup(e);
            }

            public void mouseReleased(MouseEvent e)
            {
                maybeShowPopup(e);
            }

            private void maybeShowPopup(MouseEvent e)
            {
                if (e.isPopupTrigger()) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });


    }


    /**
     * Sets the role of this ClassLabel. Updates the ui if the role has changed
     * 
     * @param role
     */
    private void setRole(ClassRole role)
    {

        if (this.role == null || role.getClass() != this.role.getClass()) {
            this.role = role;
            update();
        }
    }

    private void update()
    {
        clearUI();
        role.buildUI(this, remoteClass);
        revalidate();
        //       repaint();
    }

    /**
     *  
     */
    private void clearUI()
    {
        this.removeAll();
    }

    /**
     * @see javax.swing.JComponent#paintComponent(java.awt.Graphics)
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g.setColor(getParent().getBackground());
        g.fillRect(0, 0, getWidth(), getHeight());

        g.setColor(getBackground());
        g.fillRect(0, 0, getWidth() - SHADOW, getHeight() - SHADOW);

        drawShadow((Graphics2D) g);
        drawBorders((Graphics2D) g);

    }

    /**
     * Draw a 'shadow' appearance under and to the right of the target.
     */
    protected void drawShadow(Graphics2D g)
    {
        g.setColor(Color.GRAY);
        g.fillRect(SHADOW, getHeight() - SHADOW, getWidth() - SHADOW, SHADOW);
        g.fillRect(getWidth() - SHADOW, SHADOW, SHADOW, getHeight() - SHADOW);
    }

    /**
     * Draw the borders of this target.
     */
    protected void drawBorders(Graphics2D g)
    {
        g.setColor(Color.BLACK);
        int thickness = isSelected() ? SELECTED_BORDER : BORDER;
        Utility.drawThickRect(g, 0, 0, getWidth() - 1 - SHADOW, getHeight() - 1 - SHADOW, thickness);
    }

    /**
     *  
     */
    public ClassRole getRole()
    {
        return role;
    }

    public String getQualifiedClassName()
    {
        try {
            return remoteClass.getQualifiedName();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    public String getClassName()
    {
        try {
            String qName = remoteClass.getQualifiedName();
            String name = qName;
            int index = qName.lastIndexOf('.');
            if (index >= 0) {
                name = qName.substring(index + 1);
            }
            return name;
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    public void stateChanged(ChangeEvent e)
    {
        //repaint();
        fireSelectionChangeEvent();
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.ui.classbrowser.Selectable#select()
     */
    public void select()
    {
        this.setSelected(true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see dk.sdu.mip.dit.ui.classbrowser.Selectable#deselect()
     */
    public boolean deselect()
    {
        if (isSelected()) {
            setSelected(false);
            return true;
        }
        return false;
    }

    protected void fireSelectionChangeEvent()
    {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SelectionListener.class) {
                ((SelectionListener) listeners[i + 1]).selectionChange(this);
            }
        }
    }

    /**
     * Add a changeListener to listen for changes in this LanguagePack.
     * ChangeEvents are fired when the file is saved.
     * 
     * @param l
     *            Listener to add
     */
    public void addSelectionChangeListener(SelectionListener l)
    {
        listenerList.add(SelectionListener.class, l);
    }

    /**
     * Creates an instance of this class. The default constructor is used. This
     * method is used for creating instances when clicking on the world.
     * 
     * @return The Object that has been created
     */
    public Object createInstance()
    {
        try {
            Class cls = getRealClass();
            logger.info("real class: " + cls);
            logger.info("*** Class LOADER1: " + this.getClass().getClassLoader());

            if (cls == null) {
                return null;
            }
            Constructor constructor = cls.getConstructor(new Class[]{});
            logger.info("*** Class LOADER2: " + cls.getClassLoader());

            Object newObject = constructor.newInstance(new Object[]{});
            logger.info("new Obejct: " + newObject);
            return newObject;
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch (InstantiationException e) {
            e.printStackTrace();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                // Filter the stack trace. Take it from the first point
                // at which code from this class was being executed.
                StackTraceElement [] strace = cause.getStackTrace();
                for (int i = strace.length; i > 0; i--) {
                    if (strace[i-1].getClassName().equals(realClass.getName())) {
                        StackTraceElement [] newStrace = new StackTraceElement[i];
                        System.arraycopy(strace, 0, newStrace, 0, i);
                        cause.setStackTrace(newStrace);
                        break;
                    }
                }
                cause.printStackTrace();
            }
            else
                e.printStackTrace();
        }

        return null;

    }

    public RClass createSubclass(String className)
    {
        FileWriter writer = null;
        try {
            //get the default package which is the one containing the user
            // code.
            RPackage pkg = remoteClass.getPackage().getProject().getPackage("");
            //write the java file as this is required to exist
            File dir = pkg.getProject().getDir();

            File newJavaFile = new File(dir, className + ".java");
            /*
             * try { boolean fileExist = newJavaFile.createNewFile();
             * if(fileExist) { logger.info("File exist: " + newJavaFile); //TODO
             * Do something if java file exist return; } else {
             * logger.info("File created: " + newJavaFile); } } catch
             * (IOException e1) { // TODO Auto-generated catch block
             * e1.printStackTrace(); return; }
             */
            writer = new FileWriter(newJavaFile);
            String packageName = "";
            String superClassName = getClassName();
            if (pkg != null) {
                packageName = pkg.getName();
            }
            writer.write(imports);
            role.createSkeleton(className, superClassName, writer);
            RClass newClass = pkg.newClass(className);
            return newClass;
        }
        catch (ProjectNotOpenException e) {
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            e.printStackTrace();
        }
        catch (RemoteException e) {
            e.printStackTrace();
        }
        catch (IOException e3) {
            // TODO Auto-generated catch block
            e3.printStackTrace();
        }
        catch (MissingJavaFileException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;

    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.CompileListener#compileStarted(greenfoot.remote.RCompileEvent)
     */
    public void compileStarted(RCompileEvent event)
    {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.CompileListener#compileError(greenfoot.remote.RCompileEvent)
     */
    public void compileError(RCompileEvent event)
    {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.CompileListener#compileWarning(greenfoot.remote.RCompileEvent)
     */
    public void compileWarning(RCompileEvent event)
    {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.CompileListener#compileSucceeded(greenfoot.remote.RCompileEvent)
     */
    public void compileSucceeded(RCompileEvent event)
    {
        reloadClass();
    }

    public void reloadClass()
    {
        realClass = getClass(remoteClass);
        createPopupMenu();
        update();
    }

    /*
     * (non-Javadoc)
     * 
     * @see greenfoot.CompileListener#compileFailed(greenfoot.remote.RCompileEvent)
     */
    public void compileFailed(RCompileEvent event)
    {
    // TODO Auto-generated method stub

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseClicked(java.awt.event.MouseEvent)
     */
    public void mouseClicked(MouseEvent e)
    {
        //int clicks = e.getClickCount();
        if (e.getClickCount() > 1 && ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            try {
                remoteClass.edit();
            }
            catch (ProjectNotOpenException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (PackageNotFoundException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            catch (RemoteException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseEntered(java.awt.event.MouseEvent)
     */
    public void mouseEntered(MouseEvent e)
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e)
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e)
    {

    }

    /*
     * (non-Javadoc)
     * 
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e)
    {

    }
}
