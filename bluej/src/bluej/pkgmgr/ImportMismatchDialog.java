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

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;

/**
 * Dialog for showing the user a list of files which
 * had mismatched package lines on an open non-BlueJ.
 *
 * @author  Andrew Patterson
 */
public class ImportMismatchDialog extends EscapeDialog
{
    private static final String dialogTitle = Config.getString("pkgmgr.importmismatch.title");

    private boolean result = false;

    public ImportMismatchDialog(JFrame parent, List<File> files,
                                 List<String> packageNamesOriginal, List<String> packageNamesChanged)
    {
        super(parent, dialogTitle, true);

        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

            JLabel helpText1 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine1"));
            helpText1.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText1);
            JLabel helpText2 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine2"));
            helpText2.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText2);
            JLabel helpText3 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine3"));
            helpText3.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText3);
            JLabel helpText4 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine4"));
            helpText4.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText4);
            JLabel helpText5 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine5"));
            helpText5.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText5);
            JLabel helpText6 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine6"));
            helpText6.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText6);
            JLabel helpText7 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine7"));
            helpText7.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText7);
            JLabel helpText8 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine8"));
            helpText8.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText8);
            JLabel helpText9 = new JLabel(Config.getString("pkgmgr.importmismatch.helpLine9"));
            helpText9.setAlignmentX(LEFT_ALIGNMENT);
            mainPanel.add(helpText9);

            Font smallFont = helpText1.getFont().deriveFont(10);
            helpText1.setFont(smallFont);
            helpText2.setFont(smallFont);
            helpText3.setFont(smallFont);
            helpText4.setFont(smallFont);
            helpText5.setFont(smallFont);
            helpText6.setFont(smallFont);
            helpText7.setFont(smallFont);
            helpText8.setFont(smallFont);
            helpText9.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            JList failedList = new JList(files.toArray());
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

                JButton continueButton = BlueJTheme.getContinueButton();
				continueButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) { doContinue(); }        		
				});

                JButton cancelButton = BlueJTheme.getCancelButton();
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) { doCancel(); }        		
				});

                DialogManager.addOKCancelButtons(buttonPanel, continueButton, cancelButton);

                getRootPane().setDefaultButton(cancelButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    public boolean getResult()
    {
        return result;
    }

	private void doContinue()
	{
		result = true;
		dispose();
	}
	
	private void doCancel()
	{
		result = false;
		dispose();
	}
}
