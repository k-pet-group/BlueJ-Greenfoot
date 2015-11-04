/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2014  Michael Kolling and John Rosenberg 
 
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import threadchecker.OnThread;
import threadchecker.Tag;
import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;


/**
 * Dialog for creating a new Package
 * 
 * @version $Id: ProjectPrintDialog.java 12519 2014-10-09 11:58:21Z nccb $
 * @author Bruce Quig
 */
public class ProjectPrintDialog extends EscapeDialog
{
    private boolean ok; // result: which button?
    private JCheckBox printDiagram;
    private JCheckBox printSource;
    private JCheckBox printReadme;
    
    private JCheckBox printLineNumbers;
    private JCheckBox printHighlighting;
    
    // We store these values for use afterwards, off the Swing thread:
    private boolean printDiagramSelected;
    private boolean printSourceSelected;
    private boolean printReadmeSelected;
    
    private boolean printLineNumbersSelected;
    private boolean printHighlightingSelected;

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
        
        printLineNumbers = new JCheckBox(Config.getString("editor.printDialog.printLineNumbers"));
        printLineNumbers.setSelected(true);
        mainPanel.add(printLineNumbers);
                
        printHighlighting = new JCheckBox(Config.getString("editor.printDialog.printHighlighting"));
        mainPanel.add(printHighlighting);
        
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
        storeValues();
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
    @OnThread(Tag.Any)
    public boolean printDiagram()
    {
        return printDiagramSelected;
    }

    /**
     * Print all source code selection status
     * 
     * @return true if radio button is selected meaning  source code should be
     *         printed
     */
    @OnThread(Tag.Any)
    public boolean printSource()
    {
        return printSourceSelected;
    }

    /**
     * Print project's readme selection status
     * 
     * @return true if radio button is selected meaning  readme should be
     *         printed
     */
    @OnThread(Tag.Any)
    public boolean printReadme()
    {
        return printReadmeSelected;
    }
                
    // While on Swing thread, store selections ready for later retrieval from another thread:
    private void storeValues()
    {
        printDiagramSelected = printDiagram.isSelected();
        printSourceSelected = printSource.isSelected();
        printReadmeSelected = (printReadme != null && printReadme.isSelected());
        printLineNumbersSelected = printLineNumbers.isSelected();
        printHighlightingSelected = printHighlighting.isSelected();
    }
    
    @OnThread(Tag.Any)
    public boolean printLineNumbers()
    {
        return printLineNumbersSelected;
    }
    
    @OnThread(Tag.Any)
    public boolean printHighlighting()
    {
        return printHighlightingSelected;
    }
}
