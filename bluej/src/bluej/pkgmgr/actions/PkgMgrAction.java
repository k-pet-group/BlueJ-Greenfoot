/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2016,2017,2019  Michael Kolling and John Rosenberg 
 
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

import bluej.Config;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.utility.javafx.FXAbstractAction;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyCombination.Modifier;

/**
 * This class is intended to act as a base class for actions which require
 * a reference to the PkgMgrFrame object.<p>
 * 
 * It translates the "name" of the action in the sub-class automatically.
 * It can also set-up an accelerator key.
 * 
 * @author Davin McCall
 * @author Amjad Altadmri
 */
public abstract class PkgMgrAction extends FXAbstractAction
{
    // --------- CLASS VARIABLES ----------

    protected PkgMgrFrame pmf;
    // JavaFX doesn't support tooltips on menu items unless we use custom items,
    // so this is somewhat redundant now unless you have a button showing for the action
    protected String shortDescription;

    // --------- INSTANCE METHODS ----------

    public PkgMgrAction(PkgMgrFrame pmf, String s)
    {
    	this(pmf, s, false);
    }

    public PkgMgrAction(PkgMgrFrame pmf, String s, boolean showsDialog)
    {
        super(Config.getString(s) + (showsDialog ? "..." : ""), Config.getAcceleratorKeyFX(s));
        this.pmf = pmf;
    }

    /**
     * Constructor for an action with an accelerator key. The default shift
     * modifiers are used.
     * @param s         the untranslated action "name" (label)
     * @param keycode       the keycode of the accelerator key (one of
     *                          KeyEvent.*)
     */
    public PkgMgrAction(PkgMgrFrame pmf, String s, KeyCode keycode)
    {
        super(Config.getString(s), new KeyCodeCombination(keycode, KeyCombination.SHORTCUT_DOWN));
        this.pmf = pmf;
    }

    /**
     * Constructor for an action with an accelerator key, not using the default modifiers.
     * @param s         the untranslated action "name" (menu label)
     * @param keycode       the keycode of the accelerator key (one of KeyEvent.*)
     * @param modifiers     the shift modifiers for the accelerator key (Event.*)
     */
    public PkgMgrAction(PkgMgrFrame pmf, String s, KeyCode keycode, Modifier modifiers)
    {
        super(Config.getString(s), new KeyCodeCombination(keycode, modifiers));
        this.pmf = pmf;
    }
    
    /**
     * Set the frame to which this action will apply.
     * @param pmf  The frame to which this action will apply.
     */
    public void setFrame(PkgMgrFrame pmf)
    {
		this.pmf = pmf;
	}
        
    @Override
	public final void actionPerformed(boolean viaContextMenu)
    {
        actionPerformed(pmf);
    }
        
    public abstract void actionPerformed(PkgMgrFrame pmf);

    @Override
    public Button makeButton()
    {
        // PkgMgrFrame buttons don't take focus:
        Button b = super.makeButton();
        b.setFocusTraversable(false);
        if (shortDescription != null)
        {
            Tooltip.install(b, new Tooltip(shortDescription));
        }
        return b;
    }
}