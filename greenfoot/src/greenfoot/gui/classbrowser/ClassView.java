package greenfoot.gui.classbrowser;

import bluej.Config;
import greenfoot.actions.EditClassAction;
import greenfoot.actions.NewSubclassAction;
import greenfoot.actions.RemoveClassAction;
import greenfoot.core.GClass;
import greenfoot.core.GPackage;
import greenfoot.core.Greenfoot;
import greenfoot.core.WorldInvokeListener;
import greenfoot.event.CompileListener;
import greenfoot.event.ActorInstantiationListener;
import greenfoot.gui.classbrowser.role.ClassRole;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
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

import rmiextension.wrappers.event.RCompileEvent;
import bluej.extensions.ClassNotFoundException;
import bluej.extensions.MissingJavaFileException;
import bluej.extensions.PackageNotFoundException;
import bluej.extensions.ProjectNotOpenException;
import bluej.prefmgr.PrefMgr;
import bluej.runtime.ExecServer;
import bluej.utility.Utility;
import bluej.views.MethodView;
import bluej.views.View;
import bluej.views.ViewFilter;
import java.awt.Dimension;
import java.awt.Font;

/**
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: ClassView.java 4012 2006-04-25 14:38:06Z mik $
 */
public class ClassView extends JToggleButton
    implements ChangeListener, Selectable, CompileListener, MouseListener
{
    private final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    public static final Color[] shadowColours = { new Color(242, 242, 242), 
                                                  new Color(211, 211, 211),
                                                  new Color(189, 189, 189),
                                                  new Color(83, 83, 83)
                                                };

    private static final int SHADOW = 4;    // thickness of shadow
    private static final int GAP = 2;       // spacing between classes
    private static final int SELECTED_BORDER = 3;
    private static final int BORDER = 1;

    private transient final static Logger logger = Logger.getLogger("greenfoot");
    private static final String newline = System.getProperty("line.separator");
    private static final String imports = "import greenfoot.World;" + newline 
                                         + "import greenfoot.Actor;" + newline;

    private static final Dimension minimumSize = new Dimension(60, 20);
    private static final Dimension preferredSize = new Dimension(60, 20);
    
    private GClass gClass;
    private Class realClass; // null if not compiled
    private ClassRole role;
    private ClassBrowser classBrowser;
    private JPopupMenu popupMenu;

    
    public ClassView(ClassRole role, GClass gClass)
    {
        this.gClass = gClass;
        realClass = getClass(gClass);
        setRole(role);
        addChangeListener(this);
        this.setOpaque(false);
        this.addMouseListener(this);
        this.setBorder(BorderFactory.createEmptyBorder(7, 8, 10, 11)); //top,left,bottom,right
        Font font = getFont();
        font = font.deriveFont(13.0f);
        this.setFont(font);
//        this.setFont(PrefMgr.getTargetFont());

//        setBackground(new Color(245, 204, 155));
}

        
    /**
     * Return the real Java class that this class view represents.
     */
    public Class getRealClass()
    {
        return realClass;
    }

    
    /**
     * Get the real class that this view represents.
     * 
     * @param class1
     * @return null if the class can't be loaded
     */
    private Class getClass(GClass gClass)
    {
        Class cls = null;
        if (!gClass.isCompiled()) {
            return cls;
        }
        try {
            String className = gClass.getQualifiedName();
            //it is important that we use the right classloader
            cls = ExecServer.loadAndInitClass(className);
        }
        catch (LinkageError e) {
            // TODO log this properly? It can happen for various reasons, not
            // necessarily a real error.
            e.printStackTrace();
        }
        return cls;
    }

    void setClassBrowser(ClassBrowser classBrowser)
    {
        this.classBrowser = classBrowser;
        createPopupMenu();
    }

    public GClass getGClass()
    {
        return gClass;
    }

    private void createPopupMenu()
    {
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
            if (bluej.pkgmgr.target.role.ClassRole.createMenuItems(popupMenu, allMethods, filter, 0, allMethods.length, "", invocListener))
                popupMenu.addSeparator();
        }

        popupMenu.setInvoker(this);

        JMenuItem item;
        item = new JMenuItem(new EditClassAction(gClass));
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
        popupMenu.add(item);

        role.addPopupMenuItems(popupMenu);

        item = new JMenuItem(new RemoveClassAction(this));
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
        popupMenu.add(item);

        popupMenu.addSeparator();

        item = new JMenuItem(new NewSubclassAction(this, classBrowser));
        item.setFont(PrefMgr.getPopupMenuFont());
        item.setForeground(envOpColour);
        popupMenu.add(item);
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
        role.buildUI(this, gClass);
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
        int height = getHeight() - SHADOW;
        int width = getWidth() - 4;
        
        //TODO get this color from the bluej config
        g.setColor(new Color(245, 204, 155));
        g.fillRect(0, GAP, width, height - GAP);   // blank for gap above class

        super.paintComponent(g);
        
        drawShadow((Graphics2D) g);
        drawBorders((Graphics2D) g);

    }

    /**
     * Draw a 'shadow' appearance under and to the right of the target.
     */
    protected void drawShadow(Graphics2D g)
    {
        int height = getHeight() - SHADOW;
        int width = getWidth() - 4;
        
//        g.setColor(Color.WHITE);
//        g.fillRect(0, 0, width + 4, GAP);   // blank for gap above class
//        g.fillRect(0, height, 6, height + SHADOW);
//        g.fillRect(width, 0, width + 3, 10);
        
        // colorchange is expensive on mac, so draworder is by color, not position
        g.setColor(shadowColours[3]);
        g.drawLine(3, height, width, height);//bottom

        g.setColor(shadowColours[2]);
        g.drawLine(4, height + 1, width, height + 1);//bottom
        g.drawLine(width + 1, height + 2, width + 1, 3 + GAP);//right

        g.setColor(shadowColours[1]);
        g.drawLine(5, height + 2, width + 1, height + 2);//bottom
        g.drawLine(width + 2, height + 3, width + 2, 4 + GAP);//right

        g.setColor(shadowColours[0]);
        g.drawLine(6, height + 3, width + 2, height + 3); //bottom
        g.drawLine(width + 3, height + 3, width + 3, 5 + GAP); // right
    }

    /**
     * Draw the borders of this target.
     */
    protected void drawBorders(Graphics2D g)
    {
        g.setColor(Color.BLACK);
        int thickness = isSelected() ? SELECTED_BORDER : BORDER;
        Utility.drawThickRect(g, 0, GAP, getWidth() - 4, getHeight() - SHADOW - GAP - 1, thickness);
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
        return gClass.getQualifiedName();
    }

    public String getClassName()
    {
        return gClass.getName();
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
     * Add a changeListener to listen for changes.
     * 
     * @param l
     *            Listener to add
     */
    public void addSelectionChangeListener(SelectionListener l)
    {
        listenerList.add(SelectionListener.class, l);
    }
    
    /**
     * Remove a changeListener.
     * 
     * @param l
     *            Listener to remove
     */
    public void removeSelectionChangeListener(SelectionListener l)
    {
        if(isSelected()) {
            deselect();
        }
        listenerList.remove(SelectionListener.class, l);
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
            ActorInstantiationListener invocationListener = Greenfoot.getInstance().getInvocationListener();
            if(invocationListener != null) {
                invocationListener.localObjectCreated(newObject);
            }
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

    public GClass createSubclass(String className)
    {
        FileWriter writer = null;
        try {
            //get the default package which is the one containing the user
            // code.
            GPackage pkg = gClass.getPackage().getProject().getDefaultPackage();
            //write the java file as this is required to exist
            File dir = pkg.getProject().getDir();
            File newJavaFile = new File(dir, className + ".java");
            writer = new FileWriter(newJavaFile);
            String superClassName = getClassName();            
            writer.write(imports);
            role.createSkeleton(className, superClassName, writer);
            writer.close();
            GClass newClass = pkg.newClass(className);
            //We know what the superclass should be, so we set it.
            newClass.setSuperclassGuess(this.getQualifiedClassName());
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

    // ----- CompileListener interface -----
    
    /*
     */
    public void compileStarted(RCompileEvent event) { }

    /*
     */
    public void compileError(RCompileEvent event) { }

    /*
     */
    public void compileWarning(RCompileEvent event) { }

    /**
     * After a successful compile, reload the Java class associated with this class view.
     */
    public void compileSucceeded(RCompileEvent event)
    {
        reloadClass();
    }

    public void reloadClass()
    {
        realClass = getClass(gClass);
        createPopupMenu();
        update();
    }

    /*
     */
    public void compileFailed(RCompileEvent event) { }


    // ----- MouseListener interface -----
    
    /**
     * Mouse-click on this class view. Chek for double-click and handle.
     */
    public void mouseClicked(MouseEvent e)
    {
        if (e.getClickCount() > 1 && ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0)) {
            try {
                gClass.edit();
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
     */
    public void mouseEntered(MouseEvent e) { }

    /*
     */
    public void mouseExited(MouseEvent e) { }
    
    /**
     * The mouse was pressed on the component. Do what you have to do.
     */
    public void mousePressed(MouseEvent e)
    {
        select();
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

    public void remove()
    {
        
        classBrowser.removeClass(this);
        removeChangeListener(this);
        try {
            gClass.remove();
        }
        catch (ProjectNotOpenException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (PackageNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (ClassNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch (RemoteException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
