/*
 * ExportWebPagePane.java
 *
 * @author Michael Kolling
 * @version $Id: ExportWebPagePane.java 5661 2008-04-01 17:21:53Z polle $
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

public class ExportWebPagePane extends ExportPane
{
    public static final String FUNCTION = "WEB";
    
    private static final String helpLine1 = Config.getString("export.web.help");
    private static final String exportLocationLabelText = Config.getString("export.web.exportLocation");

    private JFileChooser fileChooser;
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

        targetDirField = new JTextField(defaultDir.toString(), 24);
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

                JButton browse = new JButton(Config.getString("export.web.browse"));
                exportLocationPanel.add(browse);
                browse.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e)
                    {
                        File file = FileChoosers.getExportDir(ExportWebPagePane.this, defaultDir,
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
            inputPanel.add(lockScenarioDescription);
        }

        add(inputPanel);
    }
}
