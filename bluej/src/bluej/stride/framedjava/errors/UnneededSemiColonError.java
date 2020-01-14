/*
 This file is part of the BlueJ program. 
 Copyright (C) 2015,2016,2019,2020  Michael Kolling and John Rosenberg

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
package bluej.stride.framedjava.errors;

import java.util.Collections;
import java.util.List;

import bluej.editor.fixes.FixSuggestion;
import bluej.stride.framedjava.ast.StringSlotFragment;
import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.utility.javafx.FXRunnable;

/**
 * An error which indicates an extra semi-colon at the end of a slot.
 * We now prevent semi-colons in most slots, so this should rarely (or never?)
 * occur.
 */
public class UnneededSemiColonError extends SyntaxCodeError
{
    private final FXRunnable fix;

    /**
     * Creates an error about a semi-colon at the end of a slot, with a quick fix to remove it.
     *
     * @param slot The slot with the semi-colon
     * @param fix An action which will remove the semi-colon
     */
    @OnThread(Tag.Any)
    public UnneededSemiColonError(StringSlotFragment slot, FXRunnable fix)
    {
        super(slot, "Unnecessary semi-colon at end of slot");
        this.fix = fix;
    }

    @Override
    @OnThread(value = Tag.FXPlatform, ignoreParent = true)
    public List<FixSuggestion> getFixSuggestions()
    {
        return Collections.singletonList(new FixSuggestion() {
            
            @Override
            @OnThread(Tag.Any)
            public String getDescription()
            {
                return "Remove semi-colon";
            }
            
            @Override
            public void execute()
            {
                fix.run();
            }
        });
    }
}
