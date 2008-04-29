/*
 * ExportPane is a superclass for all changing panes that can appear 
 * in the Export dialogue.
 *
 * @author Michael Kolling
 * @version $Id: ExportPane.java 5718 2008-04-29 17:48:33Z polle $
 */

package greenfoot.gui.export;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import bluej.Config;

public abstract class ExportPane extends JPanel
{
    private static final String lockText = Config.getString("export.lock.label");
    private static final String lockDescription = Config.getString("export.lock.description");

    protected JCheckBox lockScenario;

    /**
     * Create a an export pane for export to web pages.
     */
    public ExportPane()
    {
        lockScenario = new JCheckBox(lockText, false);
        lockScenario.setSelected(true);
        lockScenario.setAlignmentX(LEFT_ALIGNMENT);
        lockScenario.setToolTipText(lockDescription);
        lockScenario.setOpaque(false);
    }

    /**
     * This method will be called when this pane is activated (about to be
     * shown/visible)
     */
    public abstract void activated();

    /**
     * Return true if the user wants to lock the scenario.
     */
    public boolean lockScenario()
    {
        return lockScenario.isSelected();
    }
}