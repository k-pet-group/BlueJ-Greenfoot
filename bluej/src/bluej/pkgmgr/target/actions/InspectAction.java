/*
 This file is part of the BlueJ program. 
 Copyright (C) 2020 Michael Kölling and John Rosenberg 
 
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
package bluej.pkgmgr.target.actions;

import bluej.pkgmgr.target.ClassTarget;
import bluej.pkgmgr.target.EditableTarget;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.stage.Window;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * Action to inspect the static members of a class
 */
@OnThread(Tag.FXPlatform)
public class InspectAction extends ClassTargetOperation
{
    private final Node animateFromCentreOverride;

    /**
     * Create an action to inspect a class (i.e. static members, not inspecting an instance)
     * 
     * @param animateFromCentreOverride If non-null, animate from centre of this node.  If null, use ClassTarget's GUI node
     */
    public InspectAction(Node animateFromCentreOverride)
    {
        super("inspectClass", Combine.ONE, null, ClassTarget.inspectStr, MenuItemOrder.INSPECT, EditableTarget.MENU_STYLE_INBUILT);
        this.animateFromCentreOverride = animateFromCentreOverride;
    }

    @Override
    protected void execute(ClassTarget target)
    {
        if (target.checkDebuggerState())
        {
            Window parent = target.getPackage().getUI().getStage();
            Node animateFromCentre = animateFromCentreOverride != null ? animateFromCentreOverride : target.getNode();

            target.inspect(parent, animateFromCentre);
        }
    }
}
