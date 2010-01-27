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
 * Dialog for creating a new Package
 * 
 * @version $Id: ProjectPrintDialog.java 7055 2010-01-27 13:58:55Z plcs $
 * @author Bruce Quig
 */
public class ProjectPrintDialog extends EscapeDialog
{
    private boolean ok; // result: which button?
    private JCheckBox printDiagram;
    private JCheckBox printSource;
    private JCheckBox printReadme;

    /**
     * Creates a new ProjectPrintDialog object.
     * 
     * @param parent the frame that called the print dialog
     */
    public ProjectPrintDialog(PkgMgrFrame parent)
    {
        super(parent, Config.getString("pkgmgr.printDialog.title"), true);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent E)
            {
                ok = false;
                setVisible(false);
            }
        });

        JPanel mainPanel = new JPanel();

        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BlueJTheme.dialogBorder);
        mainPanel.add(Box.createVerticalStrut(
                              BlueJTheme.dialogCommandButtonsVertical));

        printDiagram = new JCheckBox(Config.getString("pkgmgr.printDialog.printDiagram"));
        printDiagram.setSelected(true);
        mainPanel.add(printDiagram);
                
        printSource = new JCheckBox(Config.getString("pkgmgr.printDialog.printSource"));
        mainPanel.add(printSource);
                
        if(parent.getPackage().isUnnamedPackage()) {
            printReadme = new JCheckBox(Config.getString("pkgmgr.printDialog.printReadme"));
            mainPanel.add(printReadme);
        }
        mainPanel.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
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

        mainPanel.add(buttonPanel);

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     * 
     * @return the status of the print job, proceed if true, cancel if false
     */
    public boolean display()
    {
        ok = false;
        setVisible(true);

        return ok;
    }

    /**
     * Close action called when OK button is pressed.  It only sets ok boolean
     * flag to true as long as one of the check boxes is selected
     */
    public void doOK()
    {
        ok = (printDiagram() || printSource() || printReadme());
        setVisible(false);
    }

    /**
     * Close action when Cancel is pressed.
     */
    public void doCancel()
    {
        ok = false;
        setVisible(false);
    }

    /**
     * Print class diagram selection status
     * 
     * @return true if radio button is selected meaning  diagram should be
     *         printed
     */
    public boolean printDiagram()
    {
        return printDiagram.isSelected();
    }

    /**
     * Print all source code selection status
     * 
     * @return true if radio button is selected meaning  source code should be
     *         printed
     */
    public boolean printSource()
    {
        return printSource.isSelected();
    }

    /**
     * Print project's readme selection status
     * 
     * @return true if radio button is selected meaning  readme should be
     *         printed
     */
    public boolean printReadme()
    {
        return (printReadme != null && printReadme.isSelected());
    }
}
