/*
 * ExportPublishPane.java
 *
 * @author Michael Kolling
 * @version $Id: ExportPublishPane.java 4984 2007-04-20 09:26:46Z mik $
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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class ExportPublishPane extends ExportPane
{
    public static final String FUNCTION = "PUBLISH";
    
    private static final String helpLine1 = "Publish the scenario to mygame.java.sum.com.";
    
    /** Creates a new instance of ExportPublishPane */
    public ExportPublishPane(List<String> worlds, String scenarioName) 
    {
        super(worlds);
        makePane(worlds);
    }
    
    /**
     * Build the component.
     */
    private void makePane(List<String> worlds)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BlueJTheme.dialogBorder);

        JLabel helpText1 = new JLabel(helpLine1);
        add(helpText1);


        Font smallFont = helpText1.getFont().deriveFont(Font.ITALIC, 11.0f);
        helpText1.setFont(smallFont);

        add(Box.createVerticalStrut(5));

        JPanel inputPanel = new JPanel();
        {
            inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
            inputPanel.setAlignmentX(LEFT_ALIGNMENT);

            if (worlds.size() > 1) {  // only if there is more than one world
                inputPanel.add(mainClassPanel);
            }
            inputPanel.add(Box.createVerticalStrut(5));
            inputPanel.add(extraControls);
        }

        add(inputPanel);
        add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));
    }
}
