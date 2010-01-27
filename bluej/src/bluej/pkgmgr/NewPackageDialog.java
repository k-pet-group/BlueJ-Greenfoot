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
import bluej.utility.EscapeDialog;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog for creating a new Package
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 * @version $Id: NewPackageDialog.java 7055 2010-01-27 13:58:55Z plcs $
 */
class NewPackageDialog extends EscapeDialog
{
    private String newPackageName = "";

    private JTextField textFld;

    private boolean ok;		// result: which button?

	public NewPackageDialog(JFrame parent)
	{
		super(parent, Config.getString("pkgmgr.newPackage.title"), true);

		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent E)
			{
				ok = false;
				setVisible(false);
			}
		});

		JPanel mainPanel = new JPanel();
		{
			mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
			mainPanel.setBorder(BlueJTheme.dialogBorder);

			JLabel newclassTag = new JLabel(Config.getString("pkgmgr.newPackage.label"));
			{
				newclassTag.setAlignmentX(LEFT_ALIGNMENT);
			}

			textFld = new JTextField(24);
			{
				textFld.setAlignmentX(LEFT_ALIGNMENT);
			}

			mainPanel.add(newclassTag);
			mainPanel.add(textFld);
			mainPanel.add(Box.createVerticalStrut(5));

			mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			{
				buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

				JButton okButton = BlueJTheme.getOkButton();
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) { doOK(); }        		
				});

				JButton cancelButton = BlueJTheme.getCancelButton();
				cancelButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) { doCancel(); }        		
				});

                                DialogManager.addOKCancelButtons(buttonPanel, okButton, cancelButton);

				getRootPane().setDefaultButton(okButton);
			}

			mainPanel.add(buttonPanel);
		}

		getContentPane().add(mainPanel);
		pack();

		DialogManager.centreDialog(this);
	}

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
        ok = false;
        textFld.requestFocus();
        setVisible(true);
        return ok;
    }

    public String getPackageName()
    {
        return newPackageName;
    }

    /**
     * Close action when OK is pressed.
     */
    public void doOK()
    {
        newPackageName = textFld.getText().trim();

        if (JavaNames.isQualifiedIdentifier(newPackageName)) {
            ok = true;
            setVisible(false);
        }
        else {
            DialogManager.showError((JFrame)this.getParent(), "invalid-package-name");
            textFld.selectAll();
            textFld.requestFocus();
        }
    }

    /**
     * Close action when Cancel is pressed.
     */
    public void doCancel()
    {
        ok = false;
        setVisible(false);
    }
}
