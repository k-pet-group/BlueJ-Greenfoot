package bluej.guibuilder;

import java.awt.*;
import java.util.Vector;


/**
 * Defines the interface for those GUIMenuComponents that contain components.
 * Those are GUIMenuBar and GUIMenu.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 * @see javablue.GUIMenuBar
 * @see javablue.GUIMenu
 */
public interface GUIMenuNodeComponent extends GUIMenuComponent
{
    /**
     * Removes the specified menu component from this component.
     *
     * @param menuComponent The menu component to be deleted.
     */
    public void deleteChild (GUIMenuComponent menuComponent);

    /**
     * Adds the specified menu component to this component.
     *
     * @param menuComponent The menu component to be added.
     */
    public void addGUIMenuComponent (GUIMenuComponent menuComponent);

    /**
     * Inserts the specified menu component at the specified position in this
     * component.
     *
     * @param menuComponent	The menu component to be inserted.
     * @param beforeComponent	Insert the component just before this component.
     */
    public void insertGUIMenuComponent (GUIMenuComponent menuComponent, GUIMenuComponent beforeComponent);
}
