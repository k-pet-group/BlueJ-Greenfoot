/*
 * ExportAppPane.java
 *
 * Created on April 19, 2007, 6:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.

 * @author Michael Kolling
 * @version $Id: ExportAppPane.java 5776 2008-06-19 17:19:36Z !Snabe23 $
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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import bluej.BlueJTheme;
import bluej.Config;

public class ExportAppPane extends ExportPane
{
    public static final String FUNCTION = "APP";
    
    private static final String helpLine1 = Config.getString("export.app.help");
    private static final String exportLocationLabelText = Config.getString("export.app.location");

    private JFileChooser fileChooser;
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

        targetDirField = new JTextField(targetFile.toString(), 26);
        targetDirField.setEditable(false);

        JLabel helpText1 = new JLabel(helpLine1);
        add(helpText1);

        add(Box.createVerticalStrut(10));

        JPanel inputPanel = new JPanel();
        {
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
            inputPanel.setAlignmentX(LEFT_ALIGNMENT);

            inputPanel.add(Box.createVerticalStrut(5));

            JPanel exportLocationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            {
                JLabel exportLocationLabel = new JLabel(exportLocationLabelText);
                exportLocationPanel.add(exportLocationLabel);

                exportLocationPanel.add(targetDirField);

                JButton browse = new JButton(Config.getString("export.app.browse"));
                exportLocationPanel.add(browse);
                browse.addActionListener(new ActionListener() {
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
        File file = FileChoosers.getFileName(this, targetFile,
                                             "Save executable jar file");
        if(file != null) {
            String newName = file.getPath();
            if(!newName.endsWith(".jar")) {
                newName += ".jar";
            }
            targetDirField.setText(newName);
        }
    }

    @Override
    public void activated(JButton continueButton)
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
