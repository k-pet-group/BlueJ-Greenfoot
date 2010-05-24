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
package bluej.editor.moe;

import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.BlueJFileReader;

import bluej.utility.Utility;
import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model
import javax.swing.*;		// all the GUI components
import java.io.*;
import javax.swing.border.EmptyBorder;

/**
 *
 * @author Michael Kolling
 */
public final class Info extends JPanel
    implements ActionListener
{
    static final ImageIcon helpImage = Config.getFixedImageAsIcon("help.png");

    public static Font infoFont = new Font("SansSerif", Font.BOLD, 10);

    // -------- INSTANCE VARIABLES --------

    private JLabel line1;
    private JLabel line2;
    String originalMsg;
    boolean isClear;
    JButton helpButton;
    String helpGroup;

    // ------------- METHODS --------------

    public Info()
    {
        super();
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createLineBorder(Color.black));
        setFont(infoFont);

        JPanel body = new JPanel(new GridLayout(0, 1));	// one col, many rows
        body.setBackground(MoeEditor.infoColor);
        body.setBorder(new EmptyBorder(0,6,0,4));
        line1 = new JLabel();
        line2 = new JLabel();
        body.add(line1);
        body.add(line2);
        body.setOpaque(false);
        add(body, BorderLayout.CENTER);

        helpButton = new JButton(helpImage);
        if (!Config.isMacOS()) {
            helpButton.setMargin(new Insets(0,0,0,0));
        }
        else {
            Utility.changeToMacButton(helpButton);
        }
        helpButton.addActionListener(this);
        helpButton.setRequestFocusEnabled(false);   // never get focus
        add(helpButton, BorderLayout.EAST);
        helpButton.setVisible(false);

        isClear = true;
        helpGroup = "";
    }

    /**
     * display a one line message
     */
    public void message(String msg)
    {
        originalMsg = msg;
        int newline = msg.indexOf('\n');
        if (newline == -1)
            if(msg.length() <= 81)
                message (msg, "");
            else
                message (msg.substring(0, 80), msg.substring(80));
        else
            message (msg.substring(0, newline), msg.substring(newline+1));
    }


    /**
     * display a two line message
     */
    public void message(String msg1, String msg2)
    {
        line1.setText(msg1);
        line2.setText(msg2.replace('\n', ' '));

        isClear = false;

        hideHelp();
    }


    /**
     * display a one line warning (message with beep)
     */
    public void warning(String msg)
    {
        message (msg);
        MoeEditorManager.editorManager.beep();
    }


    /**
     * display a two line warning (message with beep)
     */
    public void warning(String msg1, String msg2)
    {
        message (msg1, msg2);
        MoeEditorManager.editorManager.beep();
    }


    /**
     * clear the display
     */
    public void clear()
    {
        if (!isClear) {
            message (" ", " ");
            isClear = true;
        }
    }


    /**
     *
     */
    public void setHelp(String helpGroup)
    {
        this.helpGroup = helpGroup;
        helpButton.setVisible(true);
    }

    /**
     *
     */
    public void hideHelp()
    {
        helpButton.setVisible(false);
    }

    // ---- ActionListener interface ----

    public void actionPerformed(ActionEvent evt)
    {
        displayHelp(helpGroup);
    }

    private void displayHelp(String helpGroup)
    {
        File fileName = Config.getLanguageFile(helpGroup + ".help");
        int i = originalMsg.indexOf('\n');
        
        // fix for newline bug #386 with jdk1.4.0
        String line;
        if (i<0) {
            line = originalMsg;
        } else {
            line = originalMsg.substring(0,i);   
        }
        
        String helpText = BlueJFileReader.readHelpText(fileName, line.trim(),
                                                       false);
//         if(originalMsg.length() > 60) {
//             int half = originalMsg.length() / 2;
//             originalMsg = originalMsg.substring(0, half) + "\n" +
//                          originalMsg.substring(half);
//         }

        if(helpText == null)
            DialogManager.showMessageWithText(null, "no-help",
                                              "\n" + originalMsg);
        else
            DialogManager.showText(null, originalMsg + "\n\n" + helpText);
    }

}  // end class Info
