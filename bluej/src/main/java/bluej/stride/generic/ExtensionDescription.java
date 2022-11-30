/*
 This file is part of the BlueJ program. 
 Copyright (C) 2014,2015,2016,2020 Michael Kölling and John Rosenberg
 
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

import bluej.stride.operations.ToggleBooleanProperty;
import bluej.utility.Debug;
import bluej.utility.javafx.FXPlatformRunnable;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ExtensionDescription
{
    public enum ExtensionSource
    {
        BEFORE, AFTER, INSIDE_FIRST, INSIDE_LATER, MODIFIER, SELECTION;
    }

    // Note that the character may be '\b', for backspace.
    private final char shortcut;
    private final String description;
    private final boolean showInCatalogue;
    private final List<ExtensionSource> validSources;

    private FXPlatformRunnable action;
    private ToggleBooleanProperty operation;
    private Frame frame;

    public ExtensionDescription(char shortcut, String description, FXPlatformRunnable action, boolean showInCatalogue, ExtensionSource firstSrc, ExtensionSource... restSrc)
    {
        this(shortcut, description, showInCatalogue, firstSrc, restSrc);
        this.action = action;
    }

    public ExtensionDescription(ToggleBooleanProperty operation, Frame frame, boolean showInCatalogue, ExtensionSource firstSrc, ExtensionSource... restSrc)
    {
        this(operation.getKey(), operation.getLabel(), showInCatalogue, firstSrc, restSrc);
        this.operation = operation;
        this.frame = frame;
    }

    private ExtensionDescription(char shortcut, String description, boolean showInCatalogue, ExtensionSource firstSrc, ExtensionSource... restSrc)
    {
        this.shortcut = shortcut;
        this.description = description;
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

    @OnThread(Tag.FXPlatform)
    public void activate()
    {
        if (action != null) {
            action.run();
        }
        else if(operation != null) {
            operation.activate(frame);
        }
        else {
            Debug.reportError("Action and Operation shouldn't be both null in ExtensionDescription:: " + description);
        }
    }

    @OnThread(Tag.FXPlatform)
    public void activate(List<Frame> frames)
    {
        if(operation != null) {
            operation.activate(frames);
        }
        else {
            Debug.reportError("Operation shouldn't be null when calling activate(frames) in ExtensionDescription:: " + description);
        }
    }

    public boolean showInCatalogue() { return showInCatalogue; }

    public boolean validFor(ExtensionSource extensionSource)
    {
        return validSources.contains(extensionSource);
    }
}
