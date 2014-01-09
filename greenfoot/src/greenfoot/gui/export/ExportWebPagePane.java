/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2005-2009,2013  Poul Henriksen and Michael Kolling 
 
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

/**
 * ExportWebPagePane.java
 *
 * @author Michael Kolling
 */
public class ExportWebPagePane extends ExportPane
{
    public static final String FUNCTION = "WEB";
    
    private static final String helpLine1 = Config.getString("export.web.help");
    private static final String exportLocationLabelText = Config.getString("export.web.exportLocation");

    private JTextField targetDirField;

    /** 
     * Create a an export pane for export to web pages.
     */
    public ExportWebPagePane(String scenarioName, File defaultExportDir) 
    {
        super();
        File exportDir = new File(defaultExportDir, scenarioName + "-export");

        if (exportDir.exists()) {
            exportDir.delete();
        }
        
        makePane(exportDir);
    }
    
    /**
     * Return the directory where the scenario should be exported.
     */
    public String getExportLocation()
    {
        return targetDirField.getText();
    }

    /**
     * Build the component.
     */
    private void makePane(final File defaultDir)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.dialogBorder);
        setBackground(backgroundColor);

        targetDirField = new JTextField(defaultDir.toString(), 24);
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

                JButton browse = new JButton(Config.getString("export.web.browse"));
                exportLocationPanel.add(browse);
                browse.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e)
                    {
                        File file = FileChoosers.getFileName(ExportWebPagePane.this, defaultDir,
                                                              Config.getString("export.web.choose"));
                        if(file != null) {
                            targetDirField.setText(file.getPath());
                        }
                    }
                });                    
            }
            exportLocationPanel.setAlignmentX(LEFT_ALIGNMENT);
            inputPanel.add(exportLocationPanel);
            inputPanel.add(Box.createVerticalStrut(4));

            inputPanel.add(lockScenario);
        }

        add(inputPanel);
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
