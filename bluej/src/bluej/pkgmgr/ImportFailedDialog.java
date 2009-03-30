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
package bluej.pkgmgr;

import bluej.*;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog for showing the user a list of files which failed
 * an import.
 *
 * @author  Andrew Patterson
 * @version $Id: ImportFailedDialog.java 6215 2009-03-30 13:28:25Z polle $
 */
public class ImportFailedDialog extends EscapeDialog
    implements ActionListener
{
    private static final String cont = Config.getString("continue");

    private static final String dialogTitle = Config.getString("pkgmgr.importfailed.title");
    private static final String helpLine1 = Config.getString("pkgmgr.importfailed.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.importfailed.helpLine2");
    private static final String helpLine3 = Config.getString("pkgmgr.importfailed.helpLine3");

    public ImportFailedDialog(JFrame parent, Object[] objects)
    {
        super(parent, dialogTitle, true);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

            JLabel helpText1 = new JLabel(helpLine1);
            helpText1.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText1);

            JLabel helpText2 = new JLabel(helpLine2);
            helpText2.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText2);

            JLabel helpText3 = new JLabel(helpLine3);
            helpText3.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText3);

            Font smallFont = helpText1.getFont().deriveFont(10);
            helpText1.setFont(smallFont);
            helpText2.setFont(smallFont);
            helpText3.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            JList failedList = new JList(objects);
            {
                failedList.setAlignmentX(LEFT_ALIGNMENT);
            }

            JScrollPane scrolly = new JScrollPane(failedList);
            scrolly.setAlignmentX(LEFT_ALIGNMENT);

            mainPanel.add(scrolly);
            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton contButton = new JButton(cont);
                {
                    contButton.addActionListener(this);
                }

                buttonPanel.add(contButton);

                getRootPane().setDefaultButton(contButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    public void actionPerformed(ActionEvent evt)
    {
        dispose();
    }
}
