package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;
/**
 * Defines the interface for those GUIMenuComponents that do not contain
 * components. Those are GUIMenuItem, GUIMenuCheckboxItem and GUIMenuSeparator.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 * @see javablue.GUIMenuItem
 * @see javablue.GUIMenuCheckboxItem
 * @see javablue.GUIMenuSeparator
 */
public interface GUIMenuLeafComponent extends GUIMenuComponent
{
    /**
     * Return a vector with the names of all listeners accociated with this
     * menu component.
     *
     * @return String vector with the names of accociated listeners.
     */
    public Vector getListenerVector();
}
