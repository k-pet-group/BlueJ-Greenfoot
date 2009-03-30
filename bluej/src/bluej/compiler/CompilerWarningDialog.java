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
package bluej.compiler;

import bluej.*;
import bluej.Config;

import bluej.utility.MultiLineLabel;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;


/**
 * Dialog for Compiler Warning messages.  Should be used as a Singleton.  
 * The dialog is non-modal, allowing minimisation to ignore further warnings.
 * 
 * @version $Id: CompilerWarningDialog.java 6215 2009-03-30 13:28:25Z polle $
 * @author Bruce Quig
 */
public class CompilerWarningDialog extends JFrame implements ActionListener
{
    // Internationalisation
    static final String close = Config.getString("close");
    static final String dialogTitle = Config.getString("compiler.warningDialog.title");
    static final String subTitle = Config.getString("compiler.warningDialog.label");
    static final String noWarnings = Config.getString("compiler.warningDialog.noWarnings");
    
    private MultiLineLabel warningLabel;
    private boolean isEmpty;  // true if there's is (logically) no text in the dialog box
    
    // singleton
    private static CompilerWarningDialog dialog;
    
    /**
     * Creates a new CompilerWarningDialog object.  Needs to be accessed through
     *  static factory method, getDialog()
     * 
     * @param parent the frame that called the print dialog
     */
    private CompilerWarningDialog()
    {
        super(dialogTitle);
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                doClose();
            }
        });

        JPanel mainPanel = new JPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5,12,12,40));
        mainPanel.add(Box.createVerticalStrut(
                              BlueJTheme.dialogCommandButtonsVertical));

        JLabel subTitleLabel = new JLabel(subTitle);
        mainPanel.add(subTitleLabel);
        mainPanel.add(Box.createVerticalStrut(10));
                
        warningLabel = new MultiLineLabel();
        mainPanel.add(warningLabel);
     
        mainPanel.add(Box.createVerticalStrut(5));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setAlignmentX(CENTER_ALIGNMENT);

        JButton closeButton = new JButton(close);
        closeButton.addActionListener(this);
        buttonPanel.add(closeButton);
        getRootPane().setDefaultButton(closeButton);

        mainPanel.add(buttonPanel);

        getContentPane().add(mainPanel);
        reset();

    }
    
    public static CompilerWarningDialog getDialog()
    {
        if(dialog==null)
            dialog = new CompilerWarningDialog();
        return dialog;
    }


    /**
     * ActionListener for buttons
     * 
     * @param evt button event (Cancel or OK)
     */
    public void actionPerformed(ActionEvent evt)
    {
        String cmd = evt.getActionCommand();

        if (close.equals(cmd)) {
            doClose();
        }
      
    }

  
    /**
     * Close action when Cancel is pressed.
     */
    public void doClose()
    {
        reset();   // clear-down any outstanding messages
        setVisible(false);
    }
    
    /**
     * add a warning message component to the dialog
     * If it's the first such component, overwrite any text associated
     * with the dialog's "empty" state, otherwise, just append
     */
    public void addWarningMessage(String warning)
    {
        if(isEmpty) {
            warningLabel.setText(warning);
            isEmpty = false;
        } else {
            warningLabel.addText(warning);
        }
        pack();
        if(!isVisible()) {
            setVisible(true);            
        }
    }
    
    public void reset()
    {
        warningLabel.setText(noWarnings);
        isEmpty = true;
        pack();    
    }
    
}
  
