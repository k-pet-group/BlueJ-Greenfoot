package rmiextension;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.*;

import rmiextension.wrappers.event.RActionListenerImpl;
import rmiextension.wrappers.event.RActionListenerWrapper;

/**
 * Utility class for transporting menus via serializations. <br>
 * 
 * The method writeObject() is inspired by:
 * http://www.smotricz.com/kabutz/Issue013a.html This material from The Java(tm)
 * Specialists' Newsletter by Maximum Solutions (South Africa). Please contact
 * Maximum Solutions for more information.
 * 
 * @author Poul Henriksen <polle@mip.sdu.dk>
 * @version $Id: MenuSerializer.java 3262 2005-01-12 03:30:49Z davmac $
 * 
 * TODO this class is no longer needed and can be removed
 */
public class MenuSerializer
    implements Serializable
{
    private IOException defaultWriteException;
    private SMenuElement menu;

    private static interface SMenuElement
        extends Serializable
    {
        /**
         * Should return the component that this elements corresponds to in the
         * menu hiearachy of Swing
         * 
         * @return a component
         */
        Component getComponent();
    }

    private static class SMenuSeparator
        implements SMenuElement
    {
        public Component getComponent()
        {
            return new JSeparator();
        }
    }

    private static class SMenu
        implements SMenuElement
    {
        private List menuElements = new ArrayList();
        private String label;

        public SMenu(JMenu menu)
        {
            label = menu.getText();
            createMenu(menu.getPopupMenu());
        }

        public SMenu(JPopupMenu menu)
        {
            label = menu.getLabel();
            createMenu(menu);
        }

        private void createMenu(JPopupMenu menu)
        {
            Component[] comps = menu.getComponents();
            for (int i = 0; i < comps.length; i++) {
                Component component = comps[i];
                if (component instanceof JMenu) {
                    add(new SMenu(((JMenu) component)));
                }
                else if (component instanceof JMenuItem) {
                    add(new SMenuAction((JMenuItem) component));
                }
                else if (component instanceof JSeparator) {
                    add(new SMenuSeparator());
                }
            }
        }

        /**
         * @param action
         */
        private void add(SMenuElement action)
        {
            menuElements.add(action);
        }

        /*
         * (non-Javadoc)
         * 
         * @see greenfoot.remote.MenuSerializer.SMenuElement#getComponent()
         */
        public Component getComponent()
        {
            JMenu menu = new JMenu(label);
            for (Iterator iter = menuElements.iterator(); iter.hasNext();) {
                SMenuElement element = (SMenuElement) iter.next();
                menu.add(element.getComponent());
            }
            return menu;
        }
    }

    private static class SMenuAction
        implements SMenuElement
    {
        private RActionListenerWrapper actionListener;
        private String label;

        public SMenuAction(JMenuItem menuItem)
        {
            ActionListener[] listeners = menuItem.getActionListeners();
            label = menuItem.getText();
            try {
                RActionListenerImpl impl = new RActionListenerImpl(listeners, menuItem);
                actionListener = new RActionListenerWrapper(impl);
            }
            catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        /*
         * (non-Javadoc)
         * 
         * @see greenfoot.remote.MenuSerializer.SMenuElement#getComponent()
         */
        public Component getComponent()
        {
            JMenuItem menuItem = new JMenuItem(label);
            menuItem.addActionListener(actionListener);
            return menuItem;
        }
    }

    public MenuSerializer(MenuElement menu)
    {
        if (menu instanceof JMenu) {
            this.menu = new SMenu((JMenu) menu);
        }
        else if (menu instanceof JPopupMenu) {
            this.menu = new SMenu(((JPopupMenu) menu));
        }
    }

    public JMenu getMenu()
    {
        return (JMenu) menu.getComponent();
    }

    private void writeObject(final ObjectOutputStream out)
        throws IOException
    {
        if (SwingUtilities.isEventDispatchThread()) {
            // This is all that is necessary if we are already in
            // the event dispatch thread, e.g. a user clicked a
            // button which caused the object to be serialized
            out.defaultWriteObject();
        }
        else {
            try {
                // we want to wait until the object has been written
                // before continuing. If we called this from the
                // event dispatch thread we would get an exception
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run()
                    {
                        try {
                            // easiest way to indicate to the enclosing class
                            // that an exception occurred is to have a member
                            // which keeps the IOException
                            defaultWriteException = null;
                            // we call the actual write object method
                            out.defaultWriteObject();
                        }
                        catch (IOException ex) {
                            // oops, an exception occurred, remember the
                            // exception object
                            defaultWriteException = ex;
                        }
                    }
                });
                if (defaultWriteException != null) {
                    // an exception occurred in the code above, throw it!
                    throw defaultWriteException;
                }
            }
            catch (InterruptedException ex) {
                // I'm not quite sure what do here, perhaps:
                Thread.currentThread().interrupt();
                return;
            }
            catch (InvocationTargetException ex) {
                // This can actually only be a RuntimeException or an
                // Error - in either case we want to rethrow them
                Throwable target = ex.getTargetException();
                if (target instanceof RuntimeException) {
                    throw (RuntimeException) target;
                }
                else if (target instanceof Error) {
                    throw (Error) target;
                }
                ex.printStackTrace(); // this should not happen!
                throw new RuntimeException(ex.toString());
            }
        }
    }

    /**
     * @param e
     * @return
     */
    public JPopupMenu getPopupMenu()
    {
        JPopupMenu popup = ((JMenu) menu.getComponent()).getPopupMenu();
        //Component source = (Component) e.getSource();
        //Point locOnScr = source.getLocationOnScreen();
        //popup.setInvoker(source);
        //popup.setLocation((int) locOnScr.getX() + e.getX(), (int) locOnScr.getY() + e.getY());
        return popup;
    }

}