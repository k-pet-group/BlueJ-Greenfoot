package javablue.GUIBuilder;

/**
 *  This interface is used by all classes that can contain GUIComponents
 *  These include subclasses of java.awt.Container and subclasses of any layout.
 *
 * @see java.awt.Container
 */
public interface GUIComponentNode extends GUIComponent
{


    /*
     * Removes the specified GUIComponent from the component.
     *
     * @param component The GUIComponent to be removed.
     */
    public abstract void deleteChild(GUIComponent component);
    
}
