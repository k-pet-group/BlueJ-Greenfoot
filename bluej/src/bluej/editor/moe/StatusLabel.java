// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import bluej.Config;
import java.awt.*;              // New Event model    
import javax.swing.*;

/**
** @author Michael Kolling
**
**/

public final class StatusLabel extends JLabel
{
    // ---------------- CONSTANTS -----------------

    public static Font statusFont = new Font("SansSerif", Font.BOLD | Font.ITALIC, 11);

    // current save state
    static final int READONLY = 0;
    static final int SAVED = 1;   
    static final int CHANGED = 2; 

    private final String[] stateString = { 
        Config.getString("editor.state.readOnly"), 
        Config.getString("editor.state.saved"),
        Config.getString("editor.state.changed")
    };


    // ------------ INSTANCE VARIABLES ------------

    private int state;


    // -------------- CONSTRUCTORS ----------------

    public StatusLabel(int initialState)
    {
        super("", JLabel.CENTER);
        setText(stateString[initialState]);
        setFont(statusFont);
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        state = initialState;
    }

    // ------------- PUBLIC METHODS ---------------

    public boolean isSaved() 
    {
        return (state != CHANGED);
    }

    public boolean isChanged() 
    {
        return (state == CHANGED);
    }

    public boolean isReadOnly() 
    {
        return (state == READONLY);
    }

    public void setState(int newState)
    {
        state = newState;
        setText(stateString[state]);
    }

}  // end class StatusLabel
