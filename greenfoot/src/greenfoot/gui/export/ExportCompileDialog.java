/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011  Poul Henriksen and Michael Kolling 
 
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
package greenfoot.gui.export;

import greenfoot.actions.CompileAllAction;
import greenfoot.core.GProject;
import greenfoot.event.CompileListener;
import greenfoot.gui.*;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import rmiextension.wrappers.event.RCompileEvent;
import bluej.BlueJTheme;
import bluej.Config;
import bluej.utility.DialogManager;
import bluej.utility.EscapeDialog;
import java.awt.Dialog;
import java.awt.Frame;

/**
 * Dialog to be used when the project is not compiled and an export is
 * attempted. The dialog will ask the user to compile all classes. 
 * 
 * @author Poul Henriksen
 * 
 */
public class ExportCompileDialog extends EscapeDialog 
        implements CompileListener
{
    private String helpLine = Config.getString("export.compile.help");
    private boolean ok;
    private GProject project;
    
    /**
     * Creates a new dialog. This dialog should listen for compile events.
     */
    public ExportCompileDialog(Dialog parent, GProject project)
    {
        super(parent, Config.getString("export.compile.notCompiled"), true);
        this.project = project;
        makeDialog();
    }
    

    /**
     * Creates a new dialog. This dialog should listen for compile events.
     */
    public ExportCompileDialog(Frame parent, GProject project)
    {
        super(parent, Config.getString("export.compile.notCompiled"), true);
        this.project = project;
        makeDialog();
    }
    

    /**
     * Show this dialog and return true if everything was compiled, false otherwise.
     */
    public boolean display()
    {
        ok = false;
        setVisible(true);  // returns after Compiled All or Cancel, which set 'ok'
        return ok;
    }
    
    /**
     * Create the dialog interface.
     */
    private void makeDialog()
    {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

            WrappingMultiLineLabel helpText = new WrappingMultiLineLabel(helpLine, 60);
            mainPanel.add(helpText);


            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));


            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton compileButton = new JButton(new CompileAllAction(project));

                JButton cancelButton = BlueJTheme.getCancelButton();
                cancelButton.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) { doCancel(); }                
                });

                buttonPanel.add(compileButton);
                buttonPanel.add(cancelButton);

                getRootPane().setDefaultButton(compileButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }
    

    /**
     * Close action when Cancel is pressed.
     */
    private void doCancel()
    {
        ok = false;
        dispose();
    }
    
    
    /**
     * Close action when everything is compiled.
     */
    private void doOk()
    {
        ok = true;
        dispose();        
    }
    
    @Override
    public void compileError(RCompileEvent event)
    {
    }
    
    @Override
    public void compileFailed(RCompileEvent event)
    {
        doCancel();
    }

    @Override
    public void compileStarted(RCompileEvent event)
    {

    }

    @Override
    public void compileSucceeded(RCompileEvent event)
    {
        if(project.isCompiled()) {
            doOk();
        }
    }
    
    @Override
    public void compileWarning(RCompileEvent event)
    {
        
    }
}
