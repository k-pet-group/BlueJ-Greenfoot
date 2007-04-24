/*
 * ExportPane is a superclass for all changing panes that can appear 
 * in the Export dialogue.
 *
 * @author Michael Kolling
 * @version $Id: ExportPane.java 5000 2007-04-24 17:04:35Z mik $
 */

package greenfoot.gui.export;

import bluej.prefmgr.PrefMgr;
import java.awt.FlowLayout;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

public abstract class ExportPane extends JPanel
{
    private static final String extraControlsLabelText = "Allow speed change and 'Act'";

    protected JCheckBox extraControls;
        
    /** 
     * Create a an export pane for export to web pages.
     */
    public ExportPane() 
    {
        extraControls = new JCheckBox(extraControlsLabelText, false);
        extraControls.setSelected(true);
        extraControls.setAlignmentX(LEFT_ALIGNMENT);
    }
    

    /**
     * Return true if user wants to include the source.
     */
    public boolean includeExtraControls()
    {
        return extraControls.isSelected();
    }
}
