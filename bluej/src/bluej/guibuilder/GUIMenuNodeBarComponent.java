package javablue.GUIBuilder;

import java.awt.*;
import java.util.Vector;

/**
 * Defines the interface for the GUIMenuBar.
 *
 * Created: Oct 1, 1998.
 *
 * @author Morten Knudsen & Kent Hansen
 * @version 1.0
 * @see javablue.GUIMenuBar
 */
public interface GUIMenuNodeBarComponent extends GUIMenuNodeComponent
{
    /**
     * Checks if this menu bar has any menus associated.
     *
     * @returns false if the menu bar is empty, true otherwise.
     */
    public boolean containItems ();
}
