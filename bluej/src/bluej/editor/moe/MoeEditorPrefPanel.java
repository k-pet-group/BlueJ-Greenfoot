/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009  Michael Kolling and John Rosenberg 
 
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
//Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import javax.swing.*;

import bluej.*;
import bluej.prefmgr.*;

/**
* A PrefPanel subclass to allow the user to interactively add a new library
* to the browser.  The new library can be specified as a file (ZIP or JAR
* archive) with an associated description.
* 
* @author Andrew Patterson
* @version $Id: MoeEditorPrefPanel.java 6215 2009-03-30 13:28:25Z polle $
*/
public class MoeEditorPrefPanel extends JPanel implements PrefPanelListener {

    private JTextField sizeField;

    /**
     * Setup the UI for the dialog and event handlers for the dialog's buttons.
     * 
     * @param title the title of the dialog
     */
    public MoeEditorPrefPanel() {

        JLabel fontsizeTag = new JLabel("Font size");
        {
            fontsizeTag.setAlignmentX(LEFT_ALIGNMENT);
        }

        sizeField = new JTextField(4);
        {
            sizeField.setAlignmentX(LEFT_ALIGNMENT);
        }

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.generalBorder);

        add(fontsizeTag);
        add(sizeField);
        add(Box.createGlue());
    }

    public void beginEditing()
    {
        sizeField.setText("10");            
    }

    public void revertEditing()
    {
    }

    public void commitEditing()
    {
    }
}

