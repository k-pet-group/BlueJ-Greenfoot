/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2011  Michael Kolling and John Rosenberg 
 
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

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.javafx.JavaFXUtil;
import javafx.scene.control.Label;
import threadchecker.OnThread;
import threadchecker.Tag;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Status label for the Moe editor.
 * 
 * @author Michael Kolling
 */
@OnThread(Tag.FXPlatform)
public final class StatusLabel extends Label
{
    public static enum Status
    {
        READONLY("editor.state.readOnly"),
        SAVED("editor.state.saved"),
        CHANGED("editor.state.changed");

        private final String displayText;

        private Status(String displayKey)
        {
            this.displayText = Config.getString(displayKey);
        }

        public String getDisplayText()
        {
            return displayText;
        }
    }

    private Status state;

    public StatusLabel(Status initialState)
    {
        JavaFXUtil.addStyleClass(this, "moe-status-label");
        styleProperty().bind(PrefMgr.getEditorFontCSS(false));
        setText(initialState.getDisplayText());
        state = initialState;
    }

    // ------------- PUBLIC METHODS ---------------

    public boolean isSaved() 
    {
        return (state != Status.CHANGED);
    }

    public boolean isChanged() 
    {
        return (state == Status.CHANGED);
    }

    public boolean isReadOnly() 
    {
        return (state == Status.READONLY);
    }

    public void setState(Status newState)
    {
        state = newState;
        String newText = state.getDisplayText();
        // Make it always be two lines tall:
        if (!newText.contains("\n"))
            newText += "\n ";
        setText(newText);
    }
}
