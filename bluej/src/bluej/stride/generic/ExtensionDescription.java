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
package bluej.stride.generic;

import bluej.utility.javafx.FXRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtensionDescription
{
    public static enum ExtensionSource
    {
        BEFORE, AFTER, INSIDE_FIRST, INSIDE_LATER, MODIFIER, SELECTION;
    }

    // Note that the character may be '\b', for backspace.
    private final char shortcut;
    private final String description;
    private final FXRunnable action;
    private final boolean showInCatalogue;
    private final List<ExtensionSource> validSources;
/*
    public ExtensionDescription(char shortcut, String description, FXRunnable action)
    {
        this(shortcut, description, action, false, true);
    }*/

    public ExtensionDescription(char shortcut, String description, FXRunnable action, boolean showInCatalogue, ExtensionSource firstSrc, ExtensionSource... restSrc)
    {
        this.shortcut = shortcut;
        this.description = description;
        this.action = action;
        this.showInCatalogue = showInCatalogue;
        validSources = new ArrayList<>();
        validSources.add(firstSrc);
        validSources.addAll(Arrays.asList(restSrc));
    }

    public char getShortcutKey()
    {
        return shortcut;
    }
    
    public String getDescription()
    {
        return description;
    }
    
    /**
     * @return true if the key was allowed in this context, and has been processed,
     *          false if the key should be dealt with by the caller instead.
     */
    public void activate()
    {
        action.run();
    }

    public boolean showInCatalogue() { return showInCatalogue; }

    public boolean validFor(ExtensionSource extensionSource)
    {
        return validSources.contains(extensionSource);
    }
}