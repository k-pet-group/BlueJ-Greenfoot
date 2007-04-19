/*
 * ExportAppPane.java
 *
 * Created on April 19, 2007, 6:15 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.

 * @author Michael Kolling
 * @version $Id: ExportAppPane.java 4980 2007-04-19 20:43:26Z mik $
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
    public static final String NAME = "APP";
    
    private static final String helpLine1 = "Create an executable jar file that can be run on its own.";
    private static final String exportLcoationLabelText = "Export location: ";

    private JFileChooser fileChooser;
    private JTextField targetDirField;
    
    /** Creates a new instance of ExportAppPane */
    public ExportAppPane(List<String> worlds, File defaultExportDir) 
    {
        super(worlds);
        makePane(worlds, defaultExportDir);
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
    private void makePane(List<String> worlds, File defaultExportDir)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.dialogBorder);

        targetDirField = new JTextField(defaultExportDir.toString(), 20);
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
                        File file = FileChoosers.getExportFile(ExportAppPane.this);
                        if(file != null) {
                            targetDirField.setText(file.getPath());
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
