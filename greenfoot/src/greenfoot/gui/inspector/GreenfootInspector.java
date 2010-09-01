/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2010  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.inspector;

import greenfoot.Actor;
import greenfoot.ActorVisitor;
import greenfoot.core.WorldHandler;
import greenfoot.gui.input.InputManager;
import greenfoot.localdebugger.LocalObject;

/**
 * Contains methods used by the inspector in greenfoot.
 * 
 * @author Poul Henriksen
 */
public class GreenfootInspector
{
    /**
     * Whether the Get button should be enabled.
     * @return True if the selected object is an actor
     */
    static boolean isGetEnabled(Object selectedObject)
    {
        if (selectedObject != null && selectedObject instanceof LocalObject) {
            Object obj = ((LocalObject) selectedObject).getObject();
            if (obj != null && obj instanceof Actor) {
                return true;
            }
        }
        return false;
    }

    /**
     * The "Get" button was pressed. Start dragging the selected object.
     */
    static void doGet(Object selectedObject)
    {
        Object obj = ((LocalObject) selectedObject).getObject();
        InputManager inputManager = WorldHandler.getInstance().getInputManager();
        Actor actor = (Actor) obj;
        if(ActorVisitor.getWorld(actor) != null) {
            inputManager.objectMoved(actor);
        }
        else {
            inputManager.objectCreated(actor);
        }
    }
}
