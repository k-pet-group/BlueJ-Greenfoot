/*
 * ExportPane is a superclass for all changing panes that can appear 
 * in the Export dialogue.
 *
 * @author Michael Kolling
 * @version $Id: ExportPane.java 4978 2007-04-19 18:56:45Z mik $
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
    private static final String worldSelectLabelText = "World Class: ";

    protected JCheckBox extraControls;    
    protected JComboBox worldSelectComboBox;
    protected JPanel mainClassPanel;
    
    /** 
     * Create a an export pane for export to web pages.
     */
    public ExportPane(List<String> worlds) 
    {
        mainClassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        {
            worldSelectComboBox = makeWorldClassPopup(worlds);
            JLabel classLabel = new JLabel(worldSelectLabelText);
            mainClassPanel.add(classLabel);
            mainClassPanel.add(worldSelectComboBox);
            mainClassPanel.setAlignmentX(LEFT_ALIGNMENT);
        }

        extraControls = new JCheckBox(extraControlsLabelText, false);
        extraControls.setSelected(true);
        extraControls.setAlignmentX(LEFT_ALIGNMENT);
    }
    
    /**
     * Return the name of the world class that should be instantiated.
     */
    public String getWorldClassName()
    {
        return (String) worldSelectComboBox.getSelectedItem();
    }

  
    /**
     * Return true if user wants to include the source.
     */
    public boolean includeExtraControls()
    {
        return extraControls.isSelected();
    }

    /**
     * Fill the world class popup selector with all the worlds in the list.
     */
    private JComboBox makeWorldClassPopup(List<String> worlds)
    {
        JComboBox popup = new JComboBox();

        popup.setFont(PrefMgr.getPopupMenuFont());

        for (String world : worlds)
            popup.addItem(world);
        
        return popup;
    }
}
