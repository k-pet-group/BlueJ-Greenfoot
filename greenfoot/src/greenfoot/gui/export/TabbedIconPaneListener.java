/*
 * TabbedIconPaneListener - a listener to tab selection changes in the
 * TabbedIconPane.
 *
 * @author Michael Kolling
 * @version $Id: TabbedIconPaneListener.java 4979 2007-04-19 20:42:52Z mik $
 */

package greenfoot.gui.export;

public interface TabbedIconPaneListener 
{
    /** 
     * Called when the selection of the tabs changes.
     * 'name' is the NAME of the selected tab.
     */
    void tabSelected(String name);
}
