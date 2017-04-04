/*
 This file is part of the BlueJ program.
 Copyright (C) 2017 Michael Kölling and John Rosenberg

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
package bluej.editor.moe;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * An abstract action which delegates to a sub-action, and which
 * mirrors the "enabled" state of the sub-action. This allows having
 * actions with alternative labels.
 *
 * @author Davin McCall
 * @author Amjad Altadmri
 */
public class ToolbarAction extends AbstractAction implements PropertyChangeListener
{
    private final Action subAction;

    public ToolbarAction(Action subAction, String label)
    {
        super(label);
        this.subAction = subAction;
        subAction.addPropertyChangeListener(this);
        setEnabled(subAction.isEnabled());
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        subAction.actionPerformed(e);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        // If the enabled state of the sub-action changed,
        // then we should change our own state.
        if (evt.getPropertyName().equals("enabled")) {
            Object newVal = evt.getNewValue();
            if (newVal instanceof Boolean) {
                boolean state = ((Boolean) newVal);
                setEnabled(state);
            }
        }
    }
}