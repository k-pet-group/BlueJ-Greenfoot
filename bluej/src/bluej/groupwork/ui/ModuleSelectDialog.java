/*
 This file is part of the BlueJ program. 
 Copyright (C) 1999-2009,2010  Michael Kolling and John Rosenberg 
 
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
package bluej.groupwork.ui;

import java.awt.Container;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import bluej.BlueJTheme;
import bluej.Config;
import bluej.groupwork.Repository;
import bluej.groupwork.TeamUtils;
import bluej.groupwork.TeamworkCommand;
import bluej.groupwork.TeamworkCommandResult;
import bluej.utility.EscapeDialog;
import bluej.utility.SwingWorker;

/**
 * A dialog for selecting a module to checkout.
 * 
 * @author Davin McCall
 */
public class ModuleSelectDialog extends EscapeDialog implements ListSelectionListener
{
    private Repository repository;
    
    private ActivityIndicator progressBar;
    private JTextField moduleField;
    private JButton okButton;
    private JList moduleList;
    private ModuleListerThread worker;
    
    private boolean wasOk;
    
    public ModuleSelectDialog(Frame owner, Repository repository)
    {
        super(owner, Config.getString("team.moduleselect.title"), true);
        this.repository = repository;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        pack();
    }
    
    /**
     * Get the selected module name, or null if no module was selected
     * (dialog was cancelled).
     */
    public String getModuleName()
    {
        if (wasOk) {
            return moduleField.getText();
        }
        else {
            return null;
        }
    }
    
    /**
     * Start the progress bar. Safe to call from any thread.
     */
    private void startProgressBar()
    {
        progressBar.setRunning(true);
    }
    
    /**
     * Stop the progress bar. Safe to call from any thread.
     */
    private void stopProgressBar()
    {
        progressBar.setRunning(false);
    }
    
    private void setModuleList(List<String> modules)
    {
        Object [] listData = modules.toArray();
        moduleList.setListData(listData);
    }
    
    private void buildUI()
    {
        // Content pane
        JPanel contentPane = new JPanel();
        BoxLayout layout = new BoxLayout(contentPane, BoxLayout.Y_AXIS);
        contentPane.setLayout(layout);    
        contentPane.setBorder(BlueJTheme.dialogBorder);
        setContentPane(contentPane);
        
        // Module text field
        Box moduleBox = new Box(BoxLayout.X_AXIS);
        moduleBox.add(new JLabel(Config.getString("team.moduleselect.label")));
        moduleBox.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
        moduleField = new JTextField(20);
        moduleField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e)
            {
                resetOk();
            }
            
            public void insertUpdate(DocumentEvent e)
            {
                resetOk();
            }
            
            public void removeUpdate(DocumentEvent e)
            {
                resetOk();
            }
            
            public void resetOk()
            {
                okButton.setEnabled(moduleField.getText().length() != 0);
            }
        });
        moduleBox.add(moduleField);
        addXAligned(contentPane, moduleBox, 0.0f);
        contentPane.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));
        
        addXAligned(contentPane, new JSeparator(), 0.0f);
        contentPane.add(Box.createVerticalStrut(BlueJTheme.generalSpacingWidth));

        // Modules list
        addXAligned(contentPane, new JLabel(Config.getString("team.moduleselect.available")), 0f);
        
        Box moduleListBox = new Box(BoxLayout.X_AXIS);
        moduleList = new JList();
        moduleList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        moduleList.getSelectionModel().addListSelectionListener(this);
        moduleList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e)
            {
                if (e.getButton() == MouseEvent.BUTTON1 && e.getClickCount() == 2) {
                    int index = moduleList.locationToIndex(e.getPoint());
                    if (index != -1) {
                        wasOk = true;
                        dispose();
                    }
                }
            }
        });
        moduleList.setAlignmentY(0f);
        JScrollPane moduleListSP = new JScrollPane(moduleList);
        moduleListSP.setAlignmentY(0f);
        moduleListBox.add(moduleListSP);
        moduleListBox.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
        final JButton listButton = new JButton(Config.getString("team.moduleselect.show"));
        listButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                listButton.setEnabled(false);
                startProgressBar();
                worker = new ModuleListerThread();
                worker.start();
            }
        });
        listButton.setAlignmentY(0f);
        moduleListBox.add(listButton);
        
        addXAligned(contentPane, moduleListBox, 0f);

        contentPane.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));
        
        // Button box
        Box buttonBox = new Box(BoxLayout.X_AXIS);
        progressBar = new ActivityIndicator();
        buttonBox.add(progressBar);
        buttonBox.add(Box.createHorizontalGlue());
        addXAligned(contentPane, buttonBox, 0.0f);
        
        // Ok button
        buttonBox.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
        okButton = BlueJTheme.getOkButton();
        getRootPane().setDefaultButton(okButton);
        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                wasOk = true;
                dispose();
            }
        });
        okButton.setEnabled(false);
        buttonBox.add(okButton);
        
        // Cancel button
        buttonBox.add(Box.createHorizontalStrut(BlueJTheme.generalSpacingWidth));
        JButton cancelButton = BlueJTheme.getCancelButton();
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e)
            {
                if (worker != null) {
                    worker.cancel();
                }
                dispose();
            }
        });
        buttonBox.add(cancelButton);
    }
    
    private void addXAligned(Container parent, JComponent child, float alignment)
    {
        child.setAlignmentX(alignment);
        parent.add(child);
    }
    
    // ---- ListSelectionListener interface ----
    
    public void valueChanged(ListSelectionEvent e)
    {
        if (! e.getValueIsAdjusting()) {
            int selected = moduleList.getSelectedIndex();
            if (selected != -1) {
                String module = moduleList.getModel().getElementAt(selected).toString();
                moduleField.setText(module);
            }
        }
    }
    
    /**
     * A thread to find the available modules in the background.
     * 
     * @author Davin McCall
     */
    private class ModuleListerThread extends SwingWorker
    {
        private TeamworkCommand command;
        private TeamworkCommandResult result;
        private List<String> modules;
        
        public ModuleListerThread()
        {
            modules = new ArrayList<String>();
            command = repository.getModules(modules);
        }
        
        public Object construct()
        {
            result = command.getResult();
            return result;
        }
        
        public void finished()
        {
            stopProgressBar();
            if (command != null) {
                if (result != null && ! result.isError()) {
                    setModuleList(modules);
                }
                else {
                    TeamUtils.handleServerResponse(result, ModuleSelectDialog.this);
                }
            }
        }
        
        public void cancel()
        {
            if (command != null) {
                command.cancel();
                command = null;
            }
        }
    }
}
