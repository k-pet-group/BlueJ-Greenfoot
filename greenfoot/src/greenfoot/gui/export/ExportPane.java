/*
 * ExportPane is a superclass for all changing panes that can appear 
 * in the Export dialogue.
 *
 * @author Michael Kolling
 * @version $Id: ExportPane.java 4998 2007-04-24 11:39:23Z mik $
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
    protected JPanel worldClassPanel;
    protected JComboBox worldClassPopup;
        
    /** 
     * Create a an export pane for export to web pages.
     */
    public ExportPane() 
    {
        worldClassPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        {
            worldClassPopup = new JComboBox();
            worldClassPopup.setFont(PrefMgr.getPopupMenuFont());
            JLabel classLabel = new JLabel(worldSelectLabelText);
            worldClassPanel.add(classLabel);
            worldClassPanel.add(worldClassPopup);
            worldClassPanel.setAlignmentX(LEFT_ALIGNMENT);
//            worldClassPanel.setVisible(false);
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
        System.out.println("selected: " + (String) worldClassPopup.getSelectedItem());
        return (String) worldClassPopup.getSelectedItem();
    }

    /**
     * Update the pane to reflect the current project state.
     * Return true if the components in the pane have changed.
     */
    public boolean updatePane(List<String> worlds)
    {
        boolean makeVisible = worlds.size() > 1;
        boolean changed = worldClassPanel.isVisible() != makeVisible;
        
        updateWorldClassPopup(worlds);
        if (changed) {
            worldClassPanel.setVisible(makeVisible);
        }
        return changed;
    }

    /**
     * Return true if user wants to include the source.
     */
    public boolean includeExtraControls()
    {
        return extraControls.isSelected();
    }

    /**
     * Update the world classes in the class popup.
     */
    protected void updateWorldClassPopup(List<String> worlds)
    {
        worldClassPopup.removeAllItems();
        for (String world : worlds)
            worldClassPopup.addItem(world);
    }
}
