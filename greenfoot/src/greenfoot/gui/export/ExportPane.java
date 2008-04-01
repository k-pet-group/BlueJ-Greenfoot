/*
 * ExportPane is a superclass for all changing panes that can appear 
 * in the Export dialogue.
 *
 * @author Michael Kolling
 * @version $Id: ExportPane.java 5661 2008-04-01 17:21:53Z polle $
 */

package greenfoot.gui.export;

import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;


import bluej.Config;

public abstract class ExportPane extends JPanel
{
    private static final String lockText = Config.getString("export.lock.label");
    private static final String lockDescription = Config.getString("export.lock.description");

    protected JCheckBox lockScenario;
    protected JLabel lockScenarioDescription;
        
    /** 
     * Create a an export pane for export to web pages.
     */
    public ExportPane() 
    {
        lockScenario = new JCheckBox(lockText, false);
        lockScenario.setSelected(false);
        lockScenario.setAlignmentX(LEFT_ALIGNMENT);
        
        lockScenarioDescription = new JLabel(lockDescription); 
        Font smallFont = lockScenarioDescription.getFont().deriveFont(Font.ITALIC, 11.0f);
        lockScenarioDescription.setFont(smallFont);
        
    }
    

    /**
     * Return true if the user wants to lock the scenario.
     */
    public boolean lockScenario()
    {
        return lockScenario.isSelected();
    }
}
