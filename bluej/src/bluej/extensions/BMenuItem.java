package bluej.extensions;

import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.swing.JMenuItem;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * An implementation of an item in a menu. A menu item is essentially a button sitting in a list. 
 * When the user selects the "button", the action associated with the menu item is performed. 
 *
 * @author Clive Miller
 * @version $Id: BMenuItem.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class BMenuItem
{
    private String text;
    private boolean enabled = true;
    private boolean autoEnable;

    private final Collection menuListeners; // of MenuListener
    private final Map issuedJMenuItems; // of PkgMgrFrame => JMenuItem
    
    /**
     * Creates a menuItem with text.
     * @param text the text of the MenuItem.
     */
    public BMenuItem (String text)
    {
        this.text = text;
        menuListeners = new ArrayList();
        issuedJMenuItems = Collections.synchronizedMap (new HashMap());
    }
    
    /**
     * Creates a menuItem
     * @param text the text of the MenuItem
     * @param autoEnable if <code>true</code> this item will be disabled
     * when the frame is empty, and enabled when it is not
     */
    public BMenuItem (String text, boolean autoEnable)
    {
        this (text);
        this.autoEnable = autoEnable;
    }
    
    /** Internal use
    */
    public JMenuItem getJMenuItem (final PkgMgrFrame pmf)
    {
        JMenuItem mi;
        mi = (JMenuItem)issuedJMenuItems.get (pmf);
        if (mi == null) {
            mi = createJMenuItem (pmf);
            mi.setEnabled (enabled);
            for (Iterator it=menuListeners.iterator(); it.hasNext();) {
                MenuListener ml = (MenuListener)it.next();
                addActionListener (mi, ml, pmf);
            }
            issuedJMenuItems.put (pmf, mi);
            pmf.addWindowListener (new WindowAdapter() {
                public void windowClosed (WindowEvent we) {
                    issuedJMenuItems.remove (pmf);
                }
            });
        }
        if (autoEnable) pmf.disableOnEmpty (mi);
        return mi;
    }

    /** Internal use
    */
    public void removeMenuItems()
    {
        for (Iterator it=issuedFramesIterator(); it.hasNext(); ) {
            PkgMgrFrame pmf = (PkgMgrFrame)it.next();
            JMenuItem item = (JMenuItem)getIssuedJMenuItem (pmf);
            if (item.getParent() != null) item.getParent().remove (item);
        }
    }
        
    /**
     * This creates the actual JMenuItem that is to be used for this particular class
     */
    JMenuItem createJMenuItem (PkgMgrFrame pmf)
    {
        return new JMenuItem (getText());
    }
    
    JMenuItem getIssuedJMenuItem (PkgMgrFrame pmf)
    {
        return (JMenuItem)issuedJMenuItems.get (pmf);
    }
    
    Iterator issuedFramesIterator()
    {
        return issuedJMenuItems.keySet().iterator();
    }
    
    private void addActionListener (JMenuItem mi, final MenuListener ml, final PkgMgrFrame pmf) {
        mi.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e)
            {
                BPackage pkg = null;
                Package realPkg = pmf.getPackage();
                if (realPkg != null) pkg = new BPackage (realPkg);
                else pkg = new BPackage (pmf);
                ml.menuInvoked (BMenuItem.this, pkg);
            }
        });
    }
        
    /**
     * Adds a menu listener to the menu item
     * @param ml the MenuListener to be added
     */
    public void addMenuListener (final MenuListener ml)
    {
        menuListeners.add (ml);
        for (Iterator it=issuedJMenuItems.keySet().iterator(); it.hasNext();) {
            PkgMgrFrame pmf = (PkgMgrFrame)it.next();
            JMenuItem mi = (JMenuItem)issuedJMenuItems.get (pmf);
            addActionListener (mi, ml, pmf);
        }
    }
    
    /**
     * Enables or disables all instances of the menu item
     * @param enabled <code>true</code> to enable the item
     */
    public void setEnabled (boolean enabled)
    {
        this.enabled = enabled;
        for (Iterator it=issuedJMenuItems.keySet().iterator(); it.hasNext();) {
            PkgMgrFrame pmf = (PkgMgrFrame)it.next();
            JMenuItem mi = (JMenuItem)issuedJMenuItems.get (pmf);
            mi.setEnabled (enabled);
        }
    }

    /**
     * Enables or disables the menu item, either on all instances
     * or only on a specific package frame. Note that applying
     * this where pkg is <code>null</code> will result in all
     * future newly-created frames will reflect the change.
     * @param enabled <code>true</code> to enable the item
     * @param pkg which package frame to apply this to. Use
     * <code>null</code> to indicate all frames.
     */
    public void setEnabled (boolean enabled, BPackage pkg)
    {
        if (pkg == null) {
            setEnabled (enabled);
        } else {
            PkgMgrFrame pmf = PkgMgrFrame.findFrame (pkg.getRealPackage());
            JMenuItem mi = (JMenuItem)issuedJMenuItems.get (pmf);
            if (mi != null) {
                mi.setEnabled (enabled);
            }
        }
    }

    /**
     * Gets the current global menu item enabled status
     * @return the status of the menu enablement for new menu items
     */
    public boolean getEnabled()
    {
        return enabled;
    }

    /**
     * Sets the menu item text. All menus will be updated.
     * @param text the text to set the menu item to
     */
    public void setText (String text)
    {
        this.text = text;
        for (Iterator it=issuedJMenuItems.keySet().iterator(); it.hasNext();) {
            PkgMgrFrame pmf = (PkgMgrFrame)it.next();
            JMenuItem mi = (JMenuItem)issuedJMenuItems.get (pmf);
            mi.setText (text);
        }
    }

    /**
     * Gets the menu item text
     * @return the text of the menu item
     */
    public String getText ()
    {
        return text;
    }
}