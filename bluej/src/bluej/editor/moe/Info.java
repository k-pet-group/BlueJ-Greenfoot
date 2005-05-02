// Copyright (c) 2000, 2005 BlueJ Group, Deakin University
//
// This software is made available under the terms of the "MIT License"
// A copy of this license is included with this source distribution
// in "license.txt" and is also available at:
// http://www.opensource.org/licenses/mit-license.html 
// Any queries should be directed to Michael Kolling mik@bluej.org

package bluej.editor.moe;

import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.BlueJFileReader;

import java.awt.*;              // MenuBar, MenuItem, Menu, Button, etc.
import java.awt.event.*;        // New Event model
import javax.swing.*;		// all the GUI components
import java.io.*;

/**
 *
 * @author Michael Kolling
 */
public final class Info extends JPanel
    implements ActionListener
{
    static final ImageIcon helpImage = Config.getImageAsIcon("image.editor.help");

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
        line1 = new JLabel();
        line2 = new JLabel();
        body.add(line1);
        body.add(line2);
        add(body, BorderLayout.CENTER);

        helpButton = new JButton(helpImage);
        helpButton.setMargin(new Insets(0,0,0,0));
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
