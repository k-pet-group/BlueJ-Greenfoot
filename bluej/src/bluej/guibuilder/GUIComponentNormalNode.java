package javablue.GUIBuilder;

/**
 * This interface should be used byte all subclasses of java.awt.Container.
 * It is for concrete components that can contain other GUIComponents.
 * Example: GUIPanel.
 *
 * @see java.awt.Container
 */
public interface GUIComponentNormalNode extends GUIComponentNode, GUIConcreteComponent
{


    /*
     * Returns the qualifier of the container, that is the name of the container.
     * It is used when the code is generated, so that the components can be added to the
     * right container.
     *
     * @return The qualifier of the container.
     */
    public abstract String getQualifier();


    /**
     * Adds a GUIComponent to the container, furthermore the GUIComponent is stored as
     * a child of the current GUIComponentLayoutNode, if any.
     *
     * @param component The GUIComponent to be added to the container.
     *
     * @see GUIBuilder.GUIComponentLayoutNode#addGUIComponent(GUIComponent component)
     */
    public abstract void add (GUIComponent component);


    /**
     * Adds a GUIComponent to the container with the specified constraints.
     * Furthermore the GUIComponent is stored as a child
     * of the current GUIComponentLayoutNode, if any, with the specified constraints.
     *
     * @param component The GUIComponent to be added to the container.
     * @param constraints The constraints to be used.
     *
     * @see GUIBuilder.GUIComponentLayoutNode#addGUIComponent(GUIComponent component, Object constraints)
     */
    public abstract void add (GUIComponent component, Object constraints);


    /*
     * This method is used to set the Layout of the container. The initContainers method
     * in the layout is invoked from here.
     * @param layout The GUIComponentLayoutNode to be used.
     */
    public abstract void setGUILayout (GUIComponentLayoutNode layout);


    /*
     * Returns the layout currently used in the container.
     * @return The GUIComponentLayoutNode currently used.
     */
    public abstract GUIComponentLayoutNode getGUILayout ();


    /**
     * Removes the GUIComponentLayoutNode used as layout in the container. Furthermore
     * removeAll from the super class Contaiener is called.
     *
     * @see java.awt.Container#removeAll()
     */
    public abstract void removeLayout();
}
