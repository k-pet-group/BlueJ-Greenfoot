/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016 Michael Kölling and John Rosenberg
 
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
package bluej.stride.slots;

import bluej.utility.javafx.FXRunnable;
import bluej.utility.javafx.JavaFXUtil;
import threadchecker.OnThread;
import threadchecker.Tag;

import javafx.application.Platform;

public class SlotTraversalChars implements SlotValueListener
{
    private final char[] endings;
    private FXRunnable callback;
    
    public SlotTraversalChars(char... endings)
    {
        this.endings = endings;
        callback = null;
    }
    
    public SlotTraversalChars(FXRunnable callback, char... endings)
    {
        this.endings = endings;
        this.callback = callback;
    }
    
    @Override
    public boolean valueChanged(HeaderItem slot, String oldValue, String newValue, FocusParent<HeaderItem> parent)
    {
        //Disallow space on some slots
        for (char ending : endings) {
            if (newValue.contains("" + ending)) {
                //Reject - replace with old one
                //If ends in a space, treat as space key and move focus to next (if available)
                if ((newValue.charAt(newValue.length() - 1) == ending))
                {
                    // Space is only a valid traversal char if we're not otherwise blank:
                    if (ending != ' ' || newValue.length() > 1)
                    {
                        if (callback != null)
                            callback.run();
                        else
                        {
                            // Proxy for whether the user altered the text; are we on FX thread?
                            if (Platform.isFxApplicationThread())
                                JavaFXUtil.runPlatformLater(() -> parent.focusRight(slot));
                        }
                    }
                }
                return false;
            }
        }
        return true;
    }
    
    public static final SlotTraversalChars ASSIGN_LHS = new SlotTraversalChars('=');
    public static final SlotTraversalChars METHOD_NAME = new SlotTraversalChars(' ', '(');
    public static final SlotTraversalChars IDENTIFIER = new SlotTraversalChars(' ');

    public char[] getChars()
    {
        return endings;
    }
    
    @Override
    @OnThread(Tag.FXPlatform)
    public void backSpacePressedAtStart(HeaderItem slot) { }

    @Override
    @OnThread(Tag.FXPlatform)
    public void deletePressedAtEnd(HeaderItem slot) { }
}
