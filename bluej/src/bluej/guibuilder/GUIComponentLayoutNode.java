package bluej.guibuilder;

/**
 * This interface is used byte the varous layouts.
 */
public interface GUIComponentLayoutNode extends GUIComponentNode
{
    /**
     * Sets the higlight of all children of the layout. Used when a whole layout is selected.
     *
     * @param state The state to be set.
     */
    public abstract void setHighlight (boolean state);

    /**
     * Add a new component to the layout with the specified constraints.
     * 
     * @param component   The component to be added.
     * @param component   The constraints to be used.
     */
    public abstract void addGUIComponent (Object component, Object constraints);


    /**
     * Add a new component to the layout. The component will be placed in the "Center" position. 
     * 
     * @param component    The component to be added.
     */
    public abstract void addGUIComponent (GUIComponent component);


    /**
     * Returns whether the number of components is fixed in the layout.
     * Some for some layouts e.g. BorderLayout this is the case.
     *
     * @return	true, if number of components is fixed, false otherwise.
     */
    public abstract boolean hasFixedSize ();


    /**
    * Returns the number of the components in the layout.
    *
    * @return number of components.
    */
    public abstract int getGUIComponentCount();


    /**
    * Returns an array of the GUIComponents in the layout.
    *
    * @return Array of GUIComponents.
    */
    public abstract GUIComponent[] getGUIComponents();


    /**
     * This method initializes the GUIContainer on which components can be placed.
     * This method should be called when a GUIComponentNode changes layout.
     *
     */
    public abstract void initContainers ();
}
