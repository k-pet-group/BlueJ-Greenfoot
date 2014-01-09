/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2011,2013  Poul Henriksen and Michael Kolling 
 
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

import greenfoot.util.FileChoosers;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;
import javax.swing.JOptionPane;

/**
 * Export dialog pane for exporting to a standalone application.
 * 
 * @author Michael Kolling
 */
public class ExportAppPane extends ExportPane
{
    public static final String FUNCTION = "APP";
    
    private static final String helpLine1 = Config.getString("export.app.help");
    private static final String exportLocationLabelText = Config.getString("export.app.location");
    
    private JTextField targetDirField;
    
    /** Creates a new instance of ExportAppPane */
    public ExportAppPane(String scenarioName, File defaultExportDir) 
    {
        super();
        File targetFile = new File(defaultExportDir, scenarioName + ".jar");
        makePane(targetFile);
    }
    
    /**
     * Return the directory where the scenario should be exported.
     */
    public String getExportName()
    {
        return targetDirField.getText();
    }
    
    /**
     * Build the component.
     */
    private void makePane(final File targetFile)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.dialogBorder);
        setBackground(backgroundColor);


        targetDirField = new JTextField(targetFile.toString(), 26);
        targetDirField.setEditable(false);

        JLabel helpText1 = new JLabel(helpLine1);
        add(helpText1);

        add(Box.createVerticalStrut(10));

        JPanel inputPanel = new JPanel();
        {
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
            inputPanel.setAlignmentX(LEFT_ALIGNMENT);
            inputPanel.setBackground(backgroundColor);

            inputPanel.add(Box.createVerticalStrut(5));

            JPanel exportLocationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            {
                exportLocationPanel.setBackground(backgroundColor);
                JLabel exportLocationLabel = new JLabel(exportLocationLabelText);
                exportLocationPanel.add(exportLocationLabel);

                exportLocationPanel.add(targetDirField);

                JButton browse = new JButton(Config.getString("export.app.browse"));
                exportLocationPanel.add(browse);
                browse.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) { getFileName(targetFile); }
                });                    
            }
            exportLocationPanel.setAlignmentX(LEFT_ALIGNMENT);
            inputPanel.add(exportLocationPanel);
            inputPanel.add(Box.createVerticalStrut(5));

            inputPanel.add(lockScenario);
        }

        add(inputPanel);
    }
    
    /**
     * Get a user-chosen file name via a file system browser.
     * Set the pane's text field to the selected file.
     */
    private void getFileName(File targetFile)
    {
        File file = FileChoosers.getFileName(this, targetFile, Config.getString("export.app.choose"));
        if(file != null) {
            String newName = file.getPath();
            if(!newName.endsWith(".jar")) {
                if(! newName.toLowerCase().endsWith(".jar")) {
                    newName += ".jar";
                }
                else {
                    newName = newName.substring(0, newName.length()-".jar".length());
                    newName += ".jar";
                }
            }
            targetDirField.setText(newName);
            if(file.exists()) {
                String message = newName + " " + Config.getString("export.fileExists.message");
                String title = "Warning";
                int result = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.YES_NO_OPTION);
                if(result==JOptionPane.NO_OPTION) {
                    getFileName(targetFile);
                    return;
                }
            }
        }
    }

    @Override
    public void activated()
    {
        // Nothing special to do here
    }    
    
    @Override
    public boolean prePublish()
    {
        // Nothing special to do here   
        return true;
    }
    
    @Override
    public void postPublish(boolean success)
    {
        // Nothing special to do here       
    }
}
