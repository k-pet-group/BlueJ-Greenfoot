/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
package bluej.pkgmgr.actions;

import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.*;

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;

/**
 * This class is intended to act as a base class for actions which require
 * a reference to the PkgMgrFrame object.<p>
 * 
 * It translates the "name" of the action in the sub-class automatically.
 * It can also set-up an accelerator key.
 * 
 * @author Davin McCall
 * @version $Id: PkgMgrAction.java 6215 2009-03-30 13:28:25Z polle $
 */
public abstract class PkgMgrAction extends AbstractAction {
        
    // --------- CLASS VARIABLES ----------

    protected static final int SHORTCUT_MASK =
        Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    
    // --------- STATIC METHODS ------------
    
    /**
     * From some event go up the component hiearchy from the source of the event until the
     * PkgMgrFrame is found. This also handles the case where the source is a menu item on
     * a popup menu.
     * 
     * @param event     The event for which to find the frame.
     * @return      The discovered frame (or null).
     */
    public static PkgMgrFrame frameFromEvent(ActionEvent event)
    {
        Component jc = (Component)event.getSource();
        while( jc != null ) {            
             //System.out.println("hello " + jc.getClass().getName());
             //System.out.flush();
            
            if( jc instanceof PkgMgrFrame ) {
                break;
            }
            
            if( jc instanceof JPopupMenu ) {
                jc = ((JPopupMenu)jc).getInvoker();
            }
            else {
                jc = jc.getParent();
            }
        }                
        return (PkgMgrFrame)jc;
    }
    
    
    // --------- INSTANCE METHODS ----------
    
    public PkgMgrAction(String s)
    { 
        super(Config.getString(s)); 
        if (!Config.isMacOS()){
        	// Mnemonic keys are against the apple gui guidelines.
        	putValue(MNEMONIC_KEY, new Integer(Config.getMnemonicKey(s)));
        }
        if (Config.hasAcceleratorKey(s)){
            putValue(ACCELERATOR_KEY, Config.getAcceleratorKey(s));
        }
    }
    
    /**
     * Constructor for an action with an accelerator key. The default shift
     * modifiers are used.
     * @param s         the untranslated action "name" (label)
     * @param keycode       the keycode of the accelerator key (one of
     *                          KeyEvent.*)
     */
    public PkgMgrAction(String s, int keycode)
    {
        super(Config.getString(s));
        KeyStroke ks = KeyStroke.getKeyStroke(keycode, SHORTCUT_MASK);
        putValue(ACCELERATOR_KEY, ks);
    }
    
    /**
     * Constructor for an action with an accelerator key, not using the default modifiers.
     * @param s         the untranslated action "name" (menu label)
     * @param keycode       the keycode of the accelerator key (one of KeyEvent.*)
     * @param modifiers     the shift modifiers for the accelerator key (Event.*)
     */
    public PkgMgrAction(String s, int keycode, int modifiers)
    {

        super(Config.getString(s));
        KeyStroke ks = KeyStroke.getKeyStroke(keycode, modifiers);
        putValue(ACCELERATOR_KEY, ks);
    }
    
    /**
     * Retrieve the "toggle model" if any of an action. An action only has a toggle
     * model if it has an assosciated boolean state which should be displayed as a check
     * box.
     * 
     * By default there is no toggle model.
     * 
     * @return the toggle model for this action (or null).
     */
    public ButtonModel getToggleModel(PkgMgrFrame pmf)
    {
        return null;
    }
        
    final public void actionPerformed(ActionEvent event)
    {
        PkgMgrFrame pmf = frameFromEvent(event);
        actionPerformed(pmf);
    }
        
    public void actionPerformed(PkgMgrFrame pmf)
    {
        // default is to do nothing.
    }
}
    