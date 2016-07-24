/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016 Michael Kölling and John Rosenberg 
 
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
package bluej.stride.framedjava.ast;

import bluej.stride.framedjava.elements.CodeElement;
import bluej.stride.framedjava.elements.LocatableElement.LocationMap;
import bluej.stride.framedjava.errors.CodeError;
import bluej.stride.framedjava.errors.DirectSlotError;
import bluej.stride.generic.InteractionManager;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.List;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Created by neil on 20/02/2015.
 */
public abstract class SlotFragment extends JavaFragment
{
    /*
    public final void showError(CodeError codeError)
    {
        getSlot().addError(codeError);
    }
    */

    /**
     * Finds errors that do not prevent compilation.  Often these errors
     * overlap javac errors, but add more information or suggested fixes
     * @param editor
     * @return Null if no future, otherwise a future to complete for errors
     */
    @OnThread(Tag.FXPlatform)
    public Future<List<DirectSlotError>> findLateErrors(InteractionManager editor, CodeElement parent, LocationMap rootPathMap)
    {
        return null;
    }
}
