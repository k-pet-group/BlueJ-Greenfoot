/*
 This file is part of the BlueJ program. 
 Copyright (C) 2018  Michael Kolling and John Rosenberg
 
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
package bluej.pkgmgr;

import bluej.debugger.DebuggerObject;
import bluej.views.CallableView;
import javafx.stage.Stage;
import threadchecker.OnThread;
import threadchecker.Tag;

/**
 * An interface for UI actions on a package.
 * 
 * @author Davin McCall
 */
@OnThread(Tag.FXPlatform)
public interface PackageUI
{
    /**
     * Get the Stage associated with the Package UI.
     */
    public Stage getStage();
    
    /**
     * Initiate an interactive call to a static method or a constructor. The UI should prompt for
     * call parameters (if applicable) and then execute the call.
     * 
     * @param view  the view representing the method/constructor to call.
     */
    public void callStaticMethodOrConstructor(CallableView view);

    /**
     * Highlights the given object, and clears highlights on all
     * other objects.
     * 
     * @param currentObject The object to highlight (may be null,
     *                      to just clear all existing highlights)
     */
    public void highlightObject(DebuggerObject currentObject);
}
