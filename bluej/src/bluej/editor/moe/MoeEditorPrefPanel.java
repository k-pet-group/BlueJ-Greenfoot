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
* @version $Id: MoeEditorPrefPanel.java 3357 2005-05-02 03:23:33Z davmac $
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

