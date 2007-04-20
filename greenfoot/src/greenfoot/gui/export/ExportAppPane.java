/*
 * ExportAppPane.java
 *
 * Created on April 19, 2007, 6:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.

 * @author Michael Kolling
 * @version $Id: ExportAppPane.java 4984 2007-04-20 09:26:46Z mik $
 */

package greenfoot.gui.export;

import bluej.BlueJTheme;
import greenfoot.util.FileChoosers;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ExportAppPane extends ExportPane
{
    public static final String FUNCTION = "APP";
    
    private static final String helpLine1 = "Create an executable jar file that can be run on its own.";
    private static final String exportLcoationLabelText = "Save to: ";

    private JFileChooser fileChooser;
    private JTextField targetDirField;
    
    /** Creates a new instance of ExportAppPane */
    public ExportAppPane(List<String> worlds, String scenarioName, File defaultExportDir) 
    {
        super(worlds);
        File targetFile = new File(defaultExportDir, scenarioName + ".jar");
        makePane(worlds, targetFile);
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
    private void makePane(List<String> worlds, final File targetFile)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.dialogBorder);

        targetDirField = new JTextField(targetFile.toString(), 20);
        targetDirField.setEditable(false);

        JLabel helpText1 = new JLabel(helpLine1);
        add(helpText1);


        Font smallFont = helpText1.getFont().deriveFont(Font.ITALIC, 11.0f);
        helpText1.setFont(smallFont);

        add(Box.createVerticalStrut(10));

        JPanel inputPanel = new JPanel();
        {
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
            inputPanel.setAlignmentX(LEFT_ALIGNMENT);

            if (worlds.size() > 1) {  // only if there is more than one world
                inputPanel.add(mainClassPanel);
            }
            inputPanel.add(Box.createVerticalStrut(5));

            JPanel exportLocationPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            {
                JLabel exportLocationLabel = new JLabel(exportLcoationLabelText);
                exportLocationPanel.add(exportLocationLabel);

                exportLocationPanel.add(targetDirField);

                JButton browse = new JButton("Browse");
                exportLocationPanel.add(browse);
                browse.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        File file = FileChoosers.getFileName(ExportAppPane.this, targetFile,
                                                             "Save executable jar file");
                        if(file != null) {
                            String newName = file.getPath();
                            if(!newName.endsWith(".jar")) {
                                newName += ".jar";
                            }
                            targetDirField.setText(newName);
                        }
                    }
                });                    
            }
            exportLocationPanel.setAlignmentX(LEFT_ALIGNMENT);
            inputPanel.add(exportLocationPanel);
            inputPanel.add(Box.createVerticalStrut(5));

            inputPanel.add(extraControls);
        }

        add(inputPanel);
    }
}
